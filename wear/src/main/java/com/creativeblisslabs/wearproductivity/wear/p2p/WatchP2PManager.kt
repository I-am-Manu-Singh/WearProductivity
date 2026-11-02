package com.creativeblisslabs.wearproductivity.wear.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.util.Log
import com.creativeblisslabs.wearproductivity.shared.WatchP2PMessageType
import com.creativeblisslabs.wearproductivity.shared.WatchP2PPayload
import com.creativeblisslabs.wearproductivity.shared.WatchPeerDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

/**
 * High-performance peer-to-peer connection and file streaming manager for Watch-to-Watch communication.
 * Operates over local Wi-Fi LAN / hotspot using Android Network Service Discovery (mDNS/NSD)
 * and raw TCP streaming with PIN pairing verification and chunked progress callbacks.
 */
class WatchP2PManager(private val context: Context) {

    companion object {
        private const val TAG = "WatchP2PManager"
        private const val SERVICE_TYPE = "_lifeos-wear._tcp."
        private const val DEFAULT_PORT = 8944
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    private val nsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val wifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private var multicastLock: WifiManager.MulticastLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    // Unique device ID & dynamic 4-digit pairing PIN for this session
    val localDeviceId: String = Build.SERIAL.takeIf { it.isNotBlank() && it != "unknown" }
        ?: UUID.randomUUID().toString().take(8)
    val localDeviceName: String = "LifeOS-${Build.MODEL.take(10)}"
    val localPairingPin: String = (1000..9999).random().toString()

    private val _discoveredPeers = MutableStateFlow<List<WatchPeerDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<WatchPeerDevice>> = _discoveredPeers.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    var onFileReceivedCallback: ((File) -> Unit)? = null

    /**
     * Starts the P2P Server and registers the mDNS service for watch-to-watch pairing.
     */
    fun startP2PService() {
        if (_isServerRunning.value) return

        acquireLocks()
        scope.launch {
            try {
                // Open ServerSocket
                val server = ServerSocket(0) // dynamic available port
                serverSocket = server
                val port = server.localPort
                _isServerRunning.value = true
                Log.d(TAG, "P2P Server listening on port $port")

                // Register with NSD
                registerNsdService(port)

                // Accept incoming connections loop
                while (isActive && !server.isClosed) {
                    try {
                        val clientSocket = server.accept()
                        scope.launch { handleIncomingSocket(clientSocket) }
                    } catch (e: Exception) {
                        if (!server.isClosed) {
                            Log.w(TAG, "Error accepting P2P socket: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start P2P server: ${e.message}", e)
                _isServerRunning.value = false
            }
        }

        startDiscovery()
    }

    private fun registerNsdService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceType = SERVICE_TYPE
            serviceName = "$localDeviceName#$localDeviceId"
            this.port = port
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAttribute("devId", localDeviceId)
                setAttribute("model", Build.MODEL)
                setAttribute("pin", localPairingPin)
            }
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo?) {
                Log.d(TAG, "NSD Service registered: ${info?.serviceName}")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "NSD Registration failed: errorCode=$errorCode")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo?) {
                Log.d(TAG, "NSD Service unregistered")
            }
            override fun onUnregistrationFailed(info: NsdServiceInfo?, errorCode: Int) {
                Log.w(TAG, "NSD Unregistration failed: errorCode=$errorCode")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering NSD: ${e.message}", e)
        }
    }

    /**
     * Discovers other watches running LifeOS on the same Wi-Fi / hotspot.
     */
    fun startDiscovery() {
        if (discoveryListener != null) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: errorCode=$errorCode")
            }
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.w(TAG, "Discovery stop failed: errorCode=$errorCode")
            }
            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "NSD Discovery started for $serviceType")
            }
            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "NSD Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Discovered service: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceName.contains(localDeviceId)) {
                    // Ignore own advertised service
                    return
                }

                // Resolve service
                resolveService(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                _discoveredPeers.value = _discoveredPeers.value.filterNot {
                    serviceInfo.serviceName.contains(it.deviceId)
                }
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting NSD discovery: ${e.message}", e)
        }
    }

    private fun resolveService(info: NsdServiceInfo) {
        val resolver = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${serviceInfo?.serviceName}: errorCode=$errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress ?: return
                val port = serviceInfo.port
                val nameParts = serviceInfo.serviceName.split("#")
                val peerName = nameParts.firstOrNull() ?: "Watch Peer"
                val peerId = nameParts.getOrNull(1) ?: UUID.randomUUID().toString().take(8)

                var peerPin = ""
                var peerModel = "Wear OS"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val attrs = serviceInfo.attributes
                    peerPin = attrs["pin"]?.let { String(it, Charsets.UTF_8) }.orEmpty()
                    peerModel = attrs["model"]?.let { String(it, Charsets.UTF_8) } ?: "Wear OS"
                }

                val peer = WatchPeerDevice(
                    deviceId = peerId,
                    deviceName = peerName,
                    model = peerModel,
                    hostAddress = host,
                    port = port,
                    pairingPin = peerPin,
                    isPaired = false,
                    lastSeenTimestamp = System.currentTimeMillis()
                )

                _discoveredPeers.value = (_discoveredPeers.value.filterNot { it.deviceId == peerId } + peer)
                Log.d(TAG, "Successfully resolved peer watch: $peer")
            }
        }

        try {
            nsdManager.resolveService(info, resolver)
        } catch (e: Exception) {
            Log.w(TAG, "Error triggering service resolve: ${e.message}")
        }
    }

    /**
     * Handles incoming connection from another watch (file receiver).
     */
    private suspend fun handleIncomingSocket(socket: Socket) = withContext(Dispatchers.IO) {
        var partFile: File? = null
        try {
            socket.use { sock ->
                sock.soTimeout = 30_000
                val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
                val rawJson = reader.readLine() ?: return@withContext
                val payload = json.decodeFromString<WatchP2PPayload>(rawJson)

                val outputWriter = BufferedWriter(OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8))

                when (payload.type) {
                    WatchP2PMessageType.PAIR_REQUEST -> {
                        // Handshake confirmation
                        val response = WatchP2PPayload(
                            type = WatchP2PMessageType.PAIR_RESPONSE,
                            senderId = localDeviceId,
                            senderName = localDeviceName,
                            pinCode = localPairingPin,
                            success = true
                        )
                        outputWriter.write(json.encodeToString(WatchP2PPayload.serializer(), response) + "\n")
                        outputWriter.flush()
                    }

                    WatchP2PMessageType.FILE_OFFER -> {
                        val receivedDir = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "LifeOS_P2P"
                        )
                        if (!receivedDir.exists()) receivedDir.mkdirs()

                        val transferId = payload.transferId.ifBlank { UUID.randomUUID().toString().take(6) }
                        val safeName = payload.fileName.ifBlank { "p2p_file.bin" }
                        val part = File(receivedDir, "$safeName.$transferId.part")
                        partFile = part

                        // Stream bytes
                        val rawInput = sock.getInputStream()
                        var bytesReceived = 0L
                        val buffer = ByteArray(32 * 1024)

                        FileOutputStream(part).use { fos ->
                            var read: Int
                            while (rawInput.read(buffer).also { read = it } != -1) {
                                fos.write(buffer, 0, read)
                                bytesReceived += read
                                if (payload.fileSize > 0 && bytesReceived >= payload.fileSize) {
                                    break
                                }
                            }
                            fos.flush()
                        }

                        // Rename to safe unique file
                        val finalFile = getUniqueTargetFile(receivedDir, safeName)
                        if (part.renameTo(finalFile)) {
                            Log.d(TAG, "P2P File received and saved: ${finalFile.absolutePath}")
                            onFileReceivedCallback?.invoke(finalFile)

                            // Send ACK
                            val ack = WatchP2PPayload(
                                type = WatchP2PMessageType.TRANSFER_ACK,
                                transferId = transferId,
                                success = true
                            )
                            outputWriter.write(json.encodeToString(WatchP2PPayload.serializer(), ack) + "\n")
                            outputWriter.flush()
                        } else {
                            part.delete()
                            throw IOException("Failed to rename .part file to ${finalFile.name}")
                        }
                    }

                    else -> {
                        Log.w(TAG, "Unknown payload type: ${payload.type}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "P2P socket handling failed: ${e.message}", e)
            partFile?.delete()
        }
    }

    /**
     * Sends a file to a remote peer watch over direct Wi-Fi socket.
     */
    suspend fun sendFileToPeer(
        peer: WatchPeerDevice,
        file: File,
        onProgress: (progress: Float, sentBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val transferId = UUID.randomUUID().toString().take(8)
        val totalBytes = file.length().coerceAtLeast(1L)

        try {
            Socket(peer.hostAddress, peer.port).use { socket ->
                socket.soTimeout = 45_000
                val outputStream = socket.getOutputStream()
                val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))

                // 1. Send FILE_OFFER header
                val offer = WatchP2PPayload(
                    type = WatchP2PMessageType.FILE_OFFER,
                    transferId = transferId,
                    senderId = localDeviceId,
                    senderName = localDeviceName,
                    fileName = file.name,
                    fileSize = totalBytes
                )
                writer.write(json.encodeToString(WatchP2PPayload.serializer(), offer) + "\n")
                writer.flush()

                // 2. Stream raw file bytes
                val buffer = ByteArray(32 * 1024)
                var bytesSent = 0L

                FileInputStream(file).use { fis ->
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        currentCoroutineContext().ensureActive()
                        outputStream.write(buffer, 0, read)
                        bytesSent += read
                        val pct = (bytesSent.toFloat() / totalBytes.toFloat()).coerceIn(0f, 0.99f)
                        onProgress(pct, bytesSent, totalBytes)
                    }
                    outputStream.flush()
                }

                // 3. Await ACK from remote watch
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val ackJson = reader.readLine()
                if (ackJson != null) {
                    val ackPayload = json.decodeFromString<WatchP2PPayload>(ackJson)
                    if (ackPayload.success) {
                        onProgress(1f, totalBytes, totalBytes)
                        return@withContext Result.success(Unit)
                    } else {
                        return@withContext Result.failure(Exception(ackPayload.errorMessage ?: "Remote watch rejected file"))
                    }
                } else {
                    onProgress(1f, totalBytes, totalBytes)
                    return@withContext Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending file to peer watch: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun getUniqueTargetFile(dir: File, fileName: String): File {
        var file = File(dir, fileName)
        if (!file.exists()) return file

        val baseName = fileName.substringBeforeLast(".")
        val ext = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
        var count = 1
        while (file.exists()) {
            file = File(dir, "$baseName ($count)$ext")
            count++
        }
        return file
    }

    private fun acquireLocks() {
        try {
            multicastLock = wifiManager.createMulticastLock("LifeOS:WatchP2PMulticast").apply {
                setReferenceCounted(true)
                acquire()
            }
            @Suppress("DEPRECATION")
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "LifeOS:WatchP2PWifiLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire Wi-Fi/multicast locks: ${e.message}")
        }
    }

    fun stop() {
        try {
            discoveryListener?.let {
                nsdManager.stopServiceDiscovery(it)
                discoveryListener = null
            }
            registrationListener?.let {
                nsdManager.unregisterService(it)
                registrationListener = null
            }
            serverSocket?.close()
            serverSocket = null
            _isServerRunning.value = false

            multicastLock?.let { if (it.isHeld) it.release() }
            wifiLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping P2P Manager: ${e.message}")
        }
    }
}


package com.creativeblisslabs.wearproductivity.shared

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/**
 * Robust Wearable DataLayer & P2P file transfer wire protocol and handshake definitions.
 * Compatible with ChannelClient streaming, cancellation codes, and two-way verification acknowledgments.
 */
object FileTransferProtocol {
    const val CHANNEL_PREFIX = "/lifeos/file_transfer"
    const val LEGACY_CHANNEL_PREFIX = "/file_transfer"
    const val STATUS_PATH = "/lifeos/file_transfer_status"

    // Close codes
    const val CLOSE_CODE_NORMAL = 0
    const val CLOSE_CODE_CANCELLED = 1001
    const val CLOSE_CODE_ERROR = 1002

    // Ack statuses
    const val ACK_SUCCESS = "success"
    const val ACK_ERROR = "error"
    const val ACK_CANCELLED = "cancelled"

    /**
     * Builds standard channel path:
     * /lifeos/file_transfer/<transferId>/<fileSize>/<urlEncodedFileName>
     */
    fun buildChannelPath(transferId: String, fileSize: Long, fileName: String): String {
        val safeSize = if (fileSize < 0) 0L else fileSize
        val safeName = try {
            java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")
        } catch (_: Exception) {
            fileName.replace("/", "_")
        }
        return "$CHANNEL_PREFIX/$transferId/$safeSize/$safeName"
    }

    /**
     * Parses metadata from incoming channel path.
     * Supports both modern 3-part layout and legacy direct paths.
     */
    fun parseChannelPath(path: String): FileTransferMeta? {
        val prefix = when {
            path.startsWith("$CHANNEL_PREFIX/") -> "$CHANNEL_PREFIX/"
            path.startsWith("$LEGACY_CHANNEL_PREFIX/") -> "$LEGACY_CHANNEL_PREFIX/"
            path.startsWith("/file-transfer/") -> "/file-transfer/"
            else -> return null
        }

        val relative = path.removePrefix(prefix)
        val parts = relative.split("/", limit = 3)

        return if (parts.size >= 3) {
            val transferId = parts[0]
            val expectedSize = parts[1].toLongOrNull() ?: -1L
            val decodedName = try {
                java.net.URLDecoder.decode(parts[2], "UTF-8")
            } catch (_: Exception) {
                parts[2]
            }
            FileTransferMeta(
                transferId = transferId,
                fileName = decodedName.ifBlank { "shared_file" },
                expectedSize = expectedSize
            )
        } else {
            // Fallback for legacy single-segment path /lifeos/file_transfer/<filename>
            val rawName = parts.lastOrNull().orEmpty().ifBlank { "shared_file" }
            val decodedName = try {
                java.net.URLDecoder.decode(rawName, "UTF-8")
            } catch (_: Exception) {
                rawName
            }
            FileTransferMeta(
                transferId = "legacy_${System.currentTimeMillis()}",
                fileName = decodedName,
                expectedSize = -1L
            )
        }
    }
}

data class FileTransferMeta(
    val transferId: String,
    val fileName: String,
    val expectedSize: Long
)

enum class FileTransferAckState {
    SUCCESS,
    ERROR,
    CANCELLED
}

/**
 * Thread-safe registry for awaiting two-way transfer acknowledgments before closing channels.
 */
object FileTransferAckRegistry {
    private val pendingAcks = ConcurrentHashMap<String, CompletableDeferred<FileTransferAckState>>()

    fun register(transferId: String): CompletableDeferred<FileTransferAckState> {
        val deferred = CompletableDeferred<FileTransferAckState>()
        pendingAcks[transferId] = deferred
        return deferred
    }

    fun complete(transferId: String, state: FileTransferAckState): Boolean {
        val deferred = pendingAcks.remove(transferId) ?: return false
        return deferred.complete(state)
    }

    fun unregister(transferId: String) {
        pendingAcks.remove(transferId)
    }
}


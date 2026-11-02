package com.creativeblisslabs.wearproductivity.shared

import kotlinx.serialization.Serializable

/**
 * Data structures for offline Watch-to-Watch (peer-to-peer) Wi-Fi/LAN discovery,
 * PIN pairing verification, and streaming file transfers.
 */
@Serializable
data class WatchPeerDevice(
    val deviceId: String,
    val deviceName: String,
    val model: String,
    val hostAddress: String,
    val port: Int,
    val pairingPin: String = "",
    val isPaired: Boolean = false,
    val lastSeenTimestamp: Long = 0L
)

@Serializable
enum class WatchP2PMessageType {
    PAIR_REQUEST,
    PAIR_RESPONSE,
    FILE_OFFER,
    FILE_CHUNK,
    TRANSFER_ACK,
    TRANSFER_CANCEL
}

@Serializable
data class WatchP2PPayload(
    val type: WatchP2PMessageType,
    val transferId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val pinCode: String = "",
    val fileName: String = "",
    val fileSize: Long = 0L,
    val success: Boolean = true,
    val errorMessage: String? = null
)


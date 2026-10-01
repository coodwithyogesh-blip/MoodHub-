package com.example.mesh

import java.util.UUID

/**
 * Routing envelope. The relay layer forwards the opaque payload and does not render
 * message/audio/video content. Payload encryption/identity handshake is a separate layer.
 */
data class MeshPacket(
    val id: String = UUID.randomUUID().toString(),
    val originId: String,
    val destinationId: String?,
    val ttl: Int = 8,
    val payloadType: String,
    val encryptedPayload: ByteArray
)

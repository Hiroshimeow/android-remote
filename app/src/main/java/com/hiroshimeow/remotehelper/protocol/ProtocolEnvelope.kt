package com.hiroshimeow.remotehelper.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ProtocolEnvelope(
    val v: Int = 1,
    val id: String,
    val type: String,
    val sessionId: String? = null,
    val ts: Long,
    val payload: JsonElement? = null
)

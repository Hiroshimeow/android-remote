package com.example.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class BaseMessage(
    val type: String,
    val data: JsonElement? = null,
    val v: Int? = null,
    // Used for auth
    val pin: String? = null
)

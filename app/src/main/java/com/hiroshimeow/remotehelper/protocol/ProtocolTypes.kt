package com.hiroshimeow.remotehelper.protocol

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(val pin: String)

@Serializable
data class AuthResponse(val status: String, val message: String? = null, val token: String? = null)

@Serializable
data class ErrorResponse(val code: String, val message: String)

@Serializable
data class ControlAck(val success: Boolean, val error: String? = null)

@Serializable
data class TapMessagePayload(val x: Float, val y: Float)

@Serializable
data class SwipeMessagePayload(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val durationMs: Long)

@Serializable
data class SdpMessagePayload(val type: String, val sdp: String)

@Serializable
data class IceMessagePayload(val sdpMid: String, val sdpMLineIndex: Int, val candidate: String)

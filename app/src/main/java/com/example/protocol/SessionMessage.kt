package com.example.protocol

import kotlinx.serialization.Serializable

@Serializable
sealed class SignalingMessage {
    abstract val type: String
}

@Serializable
data class SdpOfferMessage(
    override val type: String = "sdp.offer",
    val sdp: String
) : SignalingMessage()

@Serializable
data class SdpAnswerMessage(
    override val type: String = "sdp.answer",
    val sdp: String
) : SignalingMessage()

@Serializable
data class IceCandidateMessage(
    override val type: String = "ice.candidate",
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val candidate: String
) : SignalingMessage()

@Serializable
data class SessionStateMessage(
    val v: Int = 1,
    val type: String = "session.state",
    val sessionId: String,
    val state: String,
    val controllerCount: Int,
    val streamProfile: String,
    val startedAt: Long
)


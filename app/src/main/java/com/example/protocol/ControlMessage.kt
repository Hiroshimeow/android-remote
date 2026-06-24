package com.example.protocol

import kotlinx.serialization.Serializable

@Serializable
sealed class ControlMessage {
    abstract val v: Int
    abstract val type: String
    abstract val sessionId: String?
}

@Serializable
data class TapMessage(
    override val v: Int = 1,
    override val type: String = "gesture.tap",
    override val sessionId: String? = null,
    val x: Float,
    val y: Float,
    val ts: Long = 0
) : ControlMessage()

@Serializable
data class SwipeMessage(
    override val v: Int = 1,
    override val type: String = "gesture.swipe",
    override val sessionId: String? = null,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val durationMs: Long
) : ControlMessage()

@Serializable
data class BackMessage(
    override val v: Int = 1,
    override val type: String = "system.back",
    override val sessionId: String? = null
) : ControlMessage()

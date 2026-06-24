package com.example.protocol

import kotlinx.serialization.Serializable

@Serializable
data class StreamConfig(
    val id: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int
) {
    companion object {
        val PROFILE_720P_30 = StreamConfig("720p30", 1280, 720, 30, 3000000)
        val PROFILE_720P_20 = StreamConfig("720p20", 1280, 720, 20, 2000000)
        val PROFILE_540P_30 = StreamConfig("540p30", 960, 540, 30, 1500000)
        val DEFAULT = PROFILE_720P_30
    }
}

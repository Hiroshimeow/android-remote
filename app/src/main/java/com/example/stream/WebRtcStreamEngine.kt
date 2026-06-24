package com.example.stream

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.protocol.StreamConfig

class WebRtcStreamEngine(private val context: Context) : StreamEngine {

    override fun start(config: StreamConfig, mediaProjectionPermissionResultData: Intent) {
        Log.d("WebRtcStreamEngine", "start stream with config: $config")
        // TODO: Initialize WebRTC PeerConnectionFactory
        // TODO: Create ScreenCapturerAndroid using mediaProjectionPermissionResultData
        // TODO: Create VideoTrack and add to PeerConnection
    }

    override fun stop() {
        Log.d("WebRtcStreamEngine", "stop stream")
        // TODO: Dispose PeerConnection and Capturer
    }

    override fun setQuality(profile: StreamConfig) {
        Log.d("WebRtcStreamEngine", "set quality: $profile")
    }
}

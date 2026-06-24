package com.example.stream

import android.content.Intent
import com.example.protocol.StreamConfig

interface StreamEngine {
    fun start(config: StreamConfig, mediaProjectionPermissionResultData: Intent)
    fun stop()
    fun setQuality(profile: StreamConfig)
}

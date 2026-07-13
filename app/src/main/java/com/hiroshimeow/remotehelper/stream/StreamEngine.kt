package com.hiroshimeow.remotehelper.stream

import android.content.Intent
import com.hiroshimeow.remotehelper.protocol.StreamConfig

interface StreamEngine {
    fun start(config: StreamConfig, mediaProjectionPermissionResultData: Intent)
    fun stop()
    fun setQuality(profile: StreamConfig)
}

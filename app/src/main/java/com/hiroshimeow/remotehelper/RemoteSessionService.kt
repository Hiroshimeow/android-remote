package com.hiroshimeow.remotehelper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hiroshimeow.remotehelper.protocol.*
import com.hiroshimeow.remotehelper.server.LocalWebServer
import com.hiroshimeow.remotehelper.session.SessionManager
import com.hiroshimeow.remotehelper.stream.WebRtcStreamEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class RemoteSessionService : Service() {
    private lateinit var webServer: LocalWebServer
    private var webRtcEngine: WebRtcStreamEngine? = null
    private var mediaProjection: MediaProjection? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaProjectionResultData: Intent? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "RemoteSessionChannel"
    }

    override fun onCreate() {
        super.onCreate()
        webServer = LocalWebServer(
            androidContext = this,
            onControllerReady = {
                startWebRtc()
            },
            onWebRtcMessage = { type, payload ->
                handleWebRtcMessage(type, payload)
            }
        )
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra("RESULT_CODE", android.app.Activity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>("RESULT_DATA")
                
                if (resultCode == android.app.Activity.RESULT_OK && resultData != null) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    mediaProjectionResultData = resultData
                    webServer.start()
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopSession()
            }
        }
        return START_NOT_STICKY
    }
    
    private fun startWebRtc() {
        val resultData = mediaProjectionResultData ?: return
        
        webRtcEngine = WebRtcStreamEngine(
            context = this,
            sendSdp = { sdp ->
                serviceScope.launch {
                    val payload = protocolJson.encodeToJsonElement(
                        SdpMessagePayload.serializer(),
                        SdpMessagePayload(type = sdp.type.canonicalForm(), sdp = sdp.description)
                    )
                    webServer.sendMessageToController("webrtc.offer", payload)
                }
            },
            sendIce = { ice ->
                serviceScope.launch {
                    val payload = protocolJson.encodeToJsonElement(
                        IceMessagePayload.serializer(),
                        IceMessagePayload(sdpMid = ice.sdpMid, sdpMLineIndex = ice.sdpMLineIndex, candidate = ice.sdp)
                    )
                    webServer.sendMessageToController("webrtc.ice", payload)
                }
            }
        )
        
        webRtcEngine?.start(StreamConfig.DEFAULT, resultData)
    }

    private fun handleWebRtcMessage(type: String, payload: kotlinx.serialization.json.JsonElement) {
        try {
            if (type == "webrtc.answer") {
                val sdpPayload = protocolJson.decodeFromJsonElement<SdpMessagePayload>(payload)
                val sdp = SessionDescription(SessionDescription.Type.fromCanonicalForm(sdpPayload.type), sdpPayload.sdp)
                webRtcEngine?.handleRemoteDescription(sdp)
            } else if (type == "webrtc.ice") {
                val icePayload = protocolJson.decodeFromJsonElement<IceMessagePayload>(payload)
                val candidate = IceCandidate(icePayload.sdpMid, icePayload.sdpMLineIndex, icePayload.candidate)
                webRtcEngine?.handleRemoteIceCandidate(candidate)
            }
        } catch (e: Exception) {
            Log.e("RemoteSessionService", "Error handling WebRTC message", e)
        }
    }

    private fun stopSession() {
        SessionManager.stopSession()
        webServer.stop()
        webRtcEngine?.stop()
        webRtcEngine = null
        mediaProjectionResultData = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Remote Control Session",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, RemoteSessionService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Remote Helper Active")
            .setContentText("Screen is being captured for remote control.")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopSession()
    }
}

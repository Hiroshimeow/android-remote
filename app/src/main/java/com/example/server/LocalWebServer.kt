package com.example.server

import android.content.Context
import android.util.Log
import com.example.input.BasicCoordinateMapper
import com.example.input.RemoteAccessibilityService
import com.example.protocol.*
import com.example.session.SessionManager
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.decodeFromJsonElement
import java.time.Duration

class LocalWebServer(
    private val androidContext: Context,
    private val sessionManager: SessionManager
) {
    private var server: ApplicationEngine? = null
    private val coordinateMapper = BasicCoordinateMapper(androidContext)

    fun start() {
        if (server != null) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                server = embeddedServer(CIO, port = 8080) {
                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(15)
                        timeout = Duration.ofSeconds(15)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }
                    routing {
                        get("/") {
                            val html = androidContext.assets.open("index.html").bufferedReader().use { it.readText() }
                            call.respondText(html, io.ktor.http.ContentType.Text.Html)
                        }
                        
                        webSocket("/ws") {
                            Log.d("LocalWebServer", "WebSocket connection opened")
                            sessionManager.onControllerConnected()
                            try {
                                for (frame in incoming) {
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        Log.d("LocalWebServer", "Received: $text")
                                        try {
                                            val message = protocolJson.decodeFromString<BaseMessage>(text)
                                            if (message.type == "control_mock" && message.data != null) {
                                                val controlMessage = protocolJson.decodeFromJsonElement<ControlMessage>(message.data)
                                                handleControlMessage(controlMessage)
                                            } else if (message.type == "auth" && message.pin != null) {
                                                val isValid = sessionManager.validatePin(message.pin)
                                                Log.d("LocalWebServer", "Auth attempt with pin ${message.pin}: $isValid")
                                                // TODO: send auth response
                                            }
                                        } catch (e: Exception) {
                                            Log.e("LocalWebServer", "Failed to parse message", e)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("LocalWebServer", "WebSocket error", e)
                            } finally {
                                sessionManager.onControllerDisconnected()
                                Log.d("LocalWebServer", "WebSocket connection closed")
                            }
                        }
                    }
                }.start(wait = false)
                Log.d("LocalWebServer", "Server started on port 8080")
            } catch (e: Exception) {
                Log.e("LocalWebServer", "Failed to start server", e)
            }
        }
    }

    private fun handleControlMessage(message: ControlMessage) {
        val inputEngine = RemoteAccessibilityService.instance ?: return
        when (message) {
            is TapMessage -> {
                val point = coordinateMapper.mapNormalizedToScreen(message.x, message.y)
                inputEngine.tap(point.x, point.y)
            }
            is SwipeMessage -> {
                val start = coordinateMapper.mapNormalizedToScreen(message.x1, message.y1)
                val end = coordinateMapper.mapNormalizedToScreen(message.x2, message.y2)
                inputEngine.swipe(start.x, start.y, end.x, end.y, message.durationMs)
            }
            is BackMessage -> {
                inputEngine.back()
            }
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        Log.d("LocalWebServer", "Server stopped")
    }
}

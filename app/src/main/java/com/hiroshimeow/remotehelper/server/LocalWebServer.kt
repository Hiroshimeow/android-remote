package com.hiroshimeow.remotehelper.server

import android.content.Context
import android.util.Log
import com.hiroshimeow.remotehelper.input.BasicCoordinateMapper
import com.hiroshimeow.remotehelper.input.RemoteAccessibilityService
import com.hiroshimeow.remotehelper.protocol.*
import com.hiroshimeow.remotehelper.session.SessionManager
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.time.Duration
import java.util.UUID

class LocalWebServer(
    private val androidContext: Context,
    private val onControllerReady: () -> Unit,
    private val onWebRtcMessage: (String, kotlinx.serialization.json.JsonElement) -> Unit
) {
    private var server: ApplicationEngine? = null
    private val coordinateMapper = BasicCoordinateMapper(androidContext)
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeSession: WebSocketSession? = null

    fun start() {
        if (server != null) return
        
        serverScope.launch {
            try {
                server = embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(15)
                        timeout = Duration.ofSeconds(15)
                        maxFrameSize = 65536
                        masking = false
                    }
                    routing {
                        get("/") {
                            val html = androidContext.assets.open("index.html").bufferedReader().use { it.readText() }
                            call.respondText(html, io.ktor.http.ContentType.Text.Html)
                        }
                        
                        get("/health") {
                            call.respondText("OK")
                        }
                        
                        webSocket("/ws") {
                            val client = ClientConnection(this)
                            client.authState = AuthState.UNAUTHENTICATED
                            Log.d("LocalWebServer", "WebSocket connection opened")
                            
                            try {
                                for (frame in incoming) {
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        handleIncomingMessage(client, text)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("LocalWebServer", "WebSocket error", e)
                            } finally {
                                if (client.isAuthenticated) {
                                    if (activeSession == client.session) {
                                        activeSession = null
                                    }
                                    SessionManager.onControllerDisconnected()
                                }
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

    suspend fun sendMessageToController(type: String, payload: kotlinx.serialization.json.JsonElement) {
        val session = activeSession ?: return
        val envelope = ProtocolEnvelope(
            id = UUID.randomUUID().toString(),
            type = type,
            ts = System.currentTimeMillis(),
            payload = payload
        )
        try {
            session.send(Frame.Text(protocolJson.encodeToString(envelope)))
        } catch (e: Exception) {
            Log.e("LocalWebServer", "Failed to send message to controller", e)
        }
    }

    private suspend fun handleIncomingMessage(client: ClientConnection, text: String) {
        try {
            val envelope = protocolJson.decodeFromString<ProtocolEnvelope>(text)
            
            if (envelope.type == "auth.request") {
                if (client.failedAttempts.get() >= 5) {
                    sendError(client.session, envelope.id, "rate_limited", "Too many failed attempts")
                    client.session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Too many failed attempts"))
                    return
                }
                
                if (envelope.payload != null) {
                    val authPayload = protocolJson.decodeFromJsonElement<AuthRequest>(envelope.payload)
                    val token = SessionManager.validatePinAndGenerateToken(authPayload.pin)
                    if (token != null) {
                        // Enforce single controller
                        if (SessionManager.sessionState.value.controllerCount >= 1 && activeSession != null) {
                            sendError(client.session, envelope.id, "controller_busy", "Session already has an active controller")
                            client.session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Controller busy"))
                            return
                        }
                        
                        client.isAuthenticated = true
                        client.authState = AuthState.AUTHENTICATED
                        client.sessionToken = token
                        client.failedAttempts.set(0)
                        activeSession = client.session
                        
                        SessionManager.onControllerConnected()
                        
                        val responsePayload = protocolJson.encodeToJsonElement(
                            AuthResponse.serializer(),
                            AuthResponse(status = "ok", token = token)
                        )
                        val responseEnvelope = ProtocolEnvelope(
                            id = UUID.randomUUID().toString(),
                            type = "auth.ok",
                            ts = System.currentTimeMillis(),
                            payload = responsePayload
                        )
                        client.session.send(Frame.Text(protocolJson.encodeToString(responseEnvelope)))
                        
                        // Notify that controller is ready for WebRTC offer
                        onControllerReady()
                    } else {
                        client.failedAttempts.incrementAndGet()
                        sendError(client.session, envelope.id, "auth_failed", "Invalid PIN")
                    }
                }
                return
            }
            
            if (!client.isAuthenticated) {
                sendError(client.session, envelope.id, "unauthorized", "Not authenticated")
                return
            }
            
            when (envelope.type) {
                "gesture.tap" -> {
                    if (envelope.payload != null) {
                        val tap = protocolJson.decodeFromJsonElement<TapMessagePayload>(envelope.payload)
                        handleTap(tap)
                    }
                }
                "gesture.swipe" -> {
                    if (envelope.payload != null) {
                        val swipe = protocolJson.decodeFromJsonElement<SwipeMessagePayload>(envelope.payload)
                        handleSwipe(swipe)
                    }
                }
                "system.back" -> {
                    RemoteAccessibilityService.instance?.back()
                }
                "webrtc.answer", "webrtc.ice" -> {
                    if (envelope.payload != null) {
                        onWebRtcMessage(envelope.type, envelope.payload)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e("LocalWebServer", "Message parse error", e)
        }
    }
    
    private suspend fun sendError(session: WebSocketSession, refId: String, code: String, message: String) {
        val errorPayload = protocolJson.encodeToJsonElement(
            ErrorResponse.serializer(),
            ErrorResponse(code, message)
        )
        val envelope = ProtocolEnvelope(
            id = UUID.randomUUID().toString(),
            type = "error",
            ts = System.currentTimeMillis(),
            payload = errorPayload
        )
        session.send(Frame.Text(protocolJson.encodeToString(envelope)))
    }

    private fun handleTap(message: TapMessagePayload) {
        val inputEngine = RemoteAccessibilityService.instance ?: return
        val point = coordinateMapper.mapNormalizedToScreen(message.x, message.y)
        inputEngine.tap(point.x, point.y)
    }

    private fun handleSwipe(message: SwipeMessagePayload) {
        val inputEngine = RemoteAccessibilityService.instance ?: return
        val start = coordinateMapper.mapNormalizedToScreen(message.x1, message.y1)
        val end = coordinateMapper.mapNormalizedToScreen(message.x2, message.y2)
        inputEngine.swipe(start.x, start.y, end.x, end.y, message.durationMs)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        activeSession = null
        Log.d("LocalWebServer", "Server stopped")
    }
}

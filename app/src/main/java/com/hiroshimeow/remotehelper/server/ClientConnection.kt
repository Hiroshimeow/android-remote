package com.hiroshimeow.remotehelper.server

import io.ktor.websocket.WebSocketSession
import java.util.concurrent.atomic.AtomicInteger

enum class AuthState {
    SOCKET_OPEN,
    UNAUTHENTICATED,
    AUTHENTICATED
}

class ClientConnection(val session: WebSocketSession) {
    var authState: AuthState = AuthState.SOCKET_OPEN
    val failedAttempts = AtomicInteger(0)
    var isAuthenticated = false
    var sessionToken: String? = null
}

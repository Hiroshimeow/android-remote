package com.hiroshimeow.remotehelper.session

import com.hiroshimeow.remotehelper.protocol.StreamConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import java.util.UUID

data class SessionInfo(
    val sessionId: String,
    val pin: String,
    val state: SessionState,
    val controllerCount: Int,
    val streamProfile: StreamConfig,
    val startedAt: Long,
    val sessionToken: String? = null
)

object SessionManager {
    private val secureRandom = SecureRandom()
    private val _sessionState = MutableStateFlow(
        SessionInfo(
            sessionId = "",
            pin = "",
            state = SessionState.Idle,
            controllerCount = 0,
            streamProfile = StreamConfig.DEFAULT,
            startedAt = 0
        )
    )
    val sessionState: StateFlow<SessionInfo> = _sessionState.asStateFlow()

    fun startSession() {
        val pin = String.format("%06d", secureRandom.nextInt(1000000))
        val sessionId = UUID.randomUUID().toString()
        _sessionState.value = _sessionState.value.copy(
            sessionId = sessionId,
            pin = pin,
            state = SessionState.WaitingForScreenConsent,
            startedAt = System.currentTimeMillis(),
            sessionToken = null
        )
    }

    fun onScreenConsentGranted() {
        if (_sessionState.value.state == SessionState.WaitingForScreenConsent) {
            _sessionState.value = _sessionState.value.copy(state = SessionState.WaitingForController)
        }
    }

    fun onControllerConnected() {
        val currentCount = _sessionState.value.controllerCount
        _sessionState.value = _sessionState.value.copy(
            state = SessionState.Connected,
            controllerCount = currentCount + 1
        )
    }

    fun onControllerDisconnected() {
        val currentCount = (_sessionState.value.controllerCount - 1).coerceAtLeast(0)
        _sessionState.value = _sessionState.value.copy(
            controllerCount = currentCount,
            state = if (currentCount == 0) SessionState.WaitingForController else SessionState.Connected
        )
    }

    fun stopSession() {
        _sessionState.value = _sessionState.value.copy(
            state = SessionState.Stopped,
            controllerCount = 0,
            pin = "",
            sessionToken = null
        )
    }

    fun validatePinAndGenerateToken(pin: String): String? {
        val state = _sessionState.value
        if (state.state == SessionState.Idle || state.state == SessionState.Stopped) return null
        
        val now = System.currentTimeMillis()
        if (now - state.startedAt > 10 * 60 * 1000 && state.controllerCount == 0) {
            return null // PIN expired
        }

        if (pin == state.pin) {
            val token = UUID.randomUUID().toString()
            _sessionState.value = state.copy(sessionToken = token)
            return token
        }
        return null
    }
    
    fun validateToken(token: String): Boolean {
        val state = _sessionState.value
        return state.sessionToken != null && state.sessionToken == token && 
               state.state != SessionState.Idle && state.state != SessionState.Stopped
    }
}

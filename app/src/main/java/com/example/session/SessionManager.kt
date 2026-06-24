package com.example.session

import com.example.protocol.StreamConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class SessionInfo(
    val sessionId: String,
    val pin: String,
    val state: SessionState,
    val controllerCount: Int,
    val streamProfile: StreamConfig,
    val startedAt: Long
)

class SessionManager {
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
        val pin = String.format("%06d", Random.nextInt(1000000))
        val sessionId = java.util.UUID.randomUUID().toString()
        _sessionState.value = _sessionState.value.copy(
            sessionId = sessionId,
            pin = pin,
            state = SessionState.WaitingForScreenConsent,
            startedAt = System.currentTimeMillis()
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
            controllerCount = 0
        )
    }

    fun validatePin(pin: String): Boolean {
        return pin == _sessionState.value.pin && _sessionState.value.state != SessionState.Idle && _sessionState.value.state != SessionState.Stopped
    }
}

package com.example.session

enum class SessionState {
    Idle,
    WaitingForScreenConsent,
    WaitingForController,
    Connected,
    Reconnecting,
    Stopped,
    Error
}

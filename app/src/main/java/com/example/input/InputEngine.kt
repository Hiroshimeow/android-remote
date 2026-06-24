package com.example.input

interface InputEngine {
    fun tap(x: Float, y: Float)
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long)
    fun back()
}

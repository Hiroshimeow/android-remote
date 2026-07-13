package com.hiroshimeow.remotehelper.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class RemoteAccessibilityService : AccessibilityService(), InputEngine {

    companion object {
        var instance: RemoteAccessibilityService? = null
            private set
    }

    private var isDispatching = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("RemoteAccessibility", "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.d("RemoteAccessibility", "Service interrupted")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }
    
    private val gestureCallback = object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            isDispatching = false
        }
        override fun onCancelled(gestureDescription: GestureDescription?) {
            isDispatching = false
        }
    }

    override fun tap(x: Float, y: Float) {
        if (isDispatching) return
        isDispatching = true
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()
        
        dispatchGesture(gesture, gestureCallback, null)
    }

    override fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long) {
        if (isDispatching) return
        isDispatching = true
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        
        dispatchGesture(gesture, gestureCallback, null)
    }

    override fun back() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }
}

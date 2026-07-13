package com.hiroshimeow.remotehelper.input

import android.content.Context
import android.graphics.PointF
import android.util.DisplayMetrics
import android.view.WindowManager

class BasicCoordinateMapper(private val context: Context) : CoordinateMapper {
    override fun mapNormalizedToScreen(x: Float, y: Float): PointF {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val bounds = windowManager.maximumWindowMetrics.bounds
        
        val screenX = x * bounds.width()
        val screenY = y * bounds.height()
        
        return PointF(screenX, screenY)
    }
}

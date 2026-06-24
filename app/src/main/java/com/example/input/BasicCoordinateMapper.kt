package com.example.input

import android.content.Context
import android.graphics.PointF
import android.util.DisplayMetrics
import android.view.WindowManager

class BasicCoordinateMapper(private val context: Context) : CoordinateMapper {
    override fun mapNormalizedToScreen(x: Float, y: Float): PointF {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        // Use getRealMetrics to include the area of navigation bars and status bar
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        
        val screenX = x * displayMetrics.widthPixels
        val screenY = y * displayMetrics.heightPixels
        
        return PointF(screenX, screenY)
    }
}

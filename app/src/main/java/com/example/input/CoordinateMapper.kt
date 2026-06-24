package com.example.input

import android.graphics.PointF

interface CoordinateMapper {
    /**
     * Map normalized coordinates (0.0 to 1.0) to actual physical screen coordinates.
     */
    fun mapNormalizedToScreen(x: Float, y: Float): PointF
}

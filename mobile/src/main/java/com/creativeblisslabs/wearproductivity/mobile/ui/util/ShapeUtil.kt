package com.creativeblisslabs.wearproductivity.mobile.ui.util

import android.graphics.Matrix
import android.graphics.RectF
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.transformed
import kotlin.math.min

fun normalize(
    shape: RoundedPolygon,
    radial: Boolean,
    dstBounds: RectF,
): RoundedPolygon {
    val srcBoundsArray = FloatArray(4)
    if (radial) {
        shape.calculateMaxBounds(srcBoundsArray)
    } else {
        shape.calculateBounds(srcBoundsArray)
    }

    val srcWidth = srcBoundsArray[2] - srcBoundsArray[0]
    val srcHeight = srcBoundsArray[3] - srcBoundsArray[1]
    val dstWidth = dstBounds.width()
    val dstHeight = dstBounds.height()

    if (srcWidth <= 0f || srcHeight <= 0f || dstWidth <= 0f || dstHeight <= 0f) {
        return shape
    }

    val scale = min(
        dstWidth / srcWidth,
        dstHeight / srcHeight,
    )
    val srcCenterX = (srcBoundsArray[0] + srcBoundsArray[2]) / 2f
    val srcCenterY = (srcBoundsArray[1] + srcBoundsArray[3]) / 2f

    val transform = Matrix().apply {
        setScale(scale, scale)
        preTranslate(-srcCenterX, -srcCenterY)
        postTranslate(dstBounds.centerX(), dstBounds.centerY())
    }
    return shape.transformed(transform)
}

package com.pocket.ui.util

import android.graphics.*
import android.view.View
import kotlin.math.min

object ViewMaskPaints {
    /** 
     * Paint to use for a layer that masks the previously drawn [Canvas] content. 
     */
    val maskLayerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    /** 
     * Paint to use for a layer that cuts something out from the previously drawn [Canvas] content. 
     */
    val cutoutLayerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }
}

/** Paint used to draw shapes on the masking layer. */
private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.BLACK
}
/**
 * Masks the corners of a canvas so they are transparent and appear rounded.
 * This should be called at the end of [View.onDraw] after all the content you want inside
 * the rounded rect has already been drawn to the canvas.
 */
fun View.roundCanvasCorners(canvas: Canvas, cornerRadius: Float) {
    // Assumes the content to mask with this rounded rectangle was drawn before calling this method.
    maskCanvas(canvas) {
        drawRoundRect(0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            cornerRadius,
            cornerRadius,
            paint)
    }
}

/**
 * Masks a circle around the canvas so the view appears to be a circle.
 * This should be called at the end of [View.onDraw] after all the content you want inside
 * the circle has already been drawn to the canvas.
 *
 * @param maskModifier should be used to draw an extra, optional mask
 */
fun View.makeCircle(canvas: Canvas, maskModifier: (Canvas.() -> Unit)? = null) {
    maskCanvas(canvas) {
        val cx = width / 2f
        val cy = height / 2f
        drawCircle(cx, cy, min(cx, cy), paint)
        maskModifier?.invoke(this)
    }
}

/**
 * Masks the current canvas content with a [mask].
 *
 * Or said differently, sets pixels outside the [mask] to transparent and keeps whatever
 * has already been drawn on the canvas inside the [mask].
 *
 * @sample roundCanvasCorners
 */
inline fun View.maskCanvas(canvas: Canvas, mask: Canvas.() -> Unit) {
    // Make sure the view is in its own layer,
    // so we don't accidentally mask out something we're drawing on top of.
    setLayerType(View.LAYER_TYPE_HARDWARE, null)

    // Create a new layer to draw the mask in. We're passing in a paint with a Porter-Duff mode
    // that's going to be applied when the mask is composited with the canvas content during
    // `canvas.restore()`. The mode treats canvas content as "destination" and the mask as "source".
    // `PorterDuff.Mode.DST_IN` means we're going to keep the destination inside the drawn source.
    canvas.saveLayer(0F, 0F, width.toFloat(), height.toFloat(), ViewMaskPaints.maskLayerPaint)
    // Draw the mask.
    canvas.mask()
    // Composite the layer with the mask with the previously drawn canvas content.
    canvas.restore()
}

/**
 * Cuts out a [cutout] from the current canvas content,
 * or punches through the current canvas content with whatever [cutout] draws.
 *
 * Or said differently, sets the pixels drawn by [cutout] to transparent and keeps whatever
 * has already been draw on the canvas outside the [cutout].
 */
inline fun View.cutoutFromCanvas(canvas: Canvas, cutout: Canvas.() -> Unit) {
    // Make sure the view is in its own layer,
    // so we don't accidentally cut something out of what we're drawing on top of.
    setLayerType(View.LAYER_TYPE_HARDWARE, null)

    // Create a new layer to draw the cutout in. We're passing in a paint with a Porter-Duff mode
    // that's going to be applied when the mask is composited with the canvas content during
    // `canvas.restore()`. The mode treats canvas content as "destination" and the mask as "source".
    // `PorterDuff.Mode.DST_OUT` means we're going to keep the destination outside the drawn source.
    canvas.saveLayer(0F, 0F, width.toFloat(), height.toFloat(), ViewMaskPaints.cutoutLayerPaint)
    // Draw the cutout.
    canvas.cutout()
    // Composite the layer with the cutout with the previously drawn canvas content.
    canvas.restore()
}

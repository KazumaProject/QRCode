package com.journeyapps.barcodescanner

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class TargetView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : View(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context): this(context, null, 0, 0)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): this(context, attrs, defStyleAttr, 0)

    private var objectAnimator: ObjectAnimator ?= null

    private val c: Context by lazy {
        context
    }

    private val margin: Float = 50f

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawLeftTopLine(canvas, setupPaint())
        drawLeftBottomLine(canvas, setupPaint())
        drawRightTopLine(canvas, setupPaint())
        drawRightBottomLine(canvas, setupPaint())
        expandedAnimation()
        drawCrossLine(canvas,setupCrossLinePaint())
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        objectAnimator?.cancel()
        objectAnimator = null
    }

    private fun drawLeftTopLine(canvas: Canvas?, paint: Paint) {
        canvas?.drawLine(0.0f, 0.0f, margin, 0.0f, paint)
        canvas?.drawLine(0.0f, 0.0f, 0.0f, margin, paint)
    }

    private fun drawLeftBottomLine(canvas: Canvas?, paint: Paint) {
        canvas?.drawLine(0.0f, height.toFloat(), margin, height.toFloat(), paint)
        canvas?.drawLine(0.0f, height.toFloat() - margin, 0.0f, height.toFloat(), paint)
    }

    private fun drawRightTopLine(canvas: Canvas?, paint: Paint) {
        canvas?.drawLine(width.toFloat() - margin, 0.0f, width.toFloat(), 0.0f, paint)
        canvas?.drawLine(width.toFloat(), 0.0f, width.toFloat(), margin, paint)
    }

    private fun drawRightBottomLine(canvas: Canvas?, paint: Paint) {
        canvas?.drawLine(width.toFloat() - margin, height.toFloat(), width.toFloat(), height.toFloat(), paint)
        canvas?.drawLine(width.toFloat(), height.toFloat() - margin, width.toFloat(), height.toFloat(), paint)
    }

    private fun setupPaint(): Paint {
        return Paint().apply {
            isAntiAlias = true
            color = ContextCompat.getColor(c, android.R.color.holo_green_dark)
            strokeWidth = 24f
        }
    }

    private fun expandedAnimation() {
        val expandedScaleX = PropertyValuesHolder.ofFloat("scaleX", 1.05f, 0.98f)
        val expandedScaleY = PropertyValuesHolder.ofFloat("scaleY", 1.05f, 0.98f)

        objectAnimator = ObjectAnimator.ofPropertyValuesHolder(this, expandedScaleX, expandedScaleY).apply {
            duration = 1100
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        objectAnimator?.start()
    }

    private fun drawCrossLine(canvas: Canvas?, paint: Paint) {
        val halfWidth = (width / 2).toFloat()
        val halfHeight= (height / 2).toFloat()
        val margin = 25f
        canvas?.drawLine(halfWidth - margin, halfHeight, halfWidth + margin, halfHeight, paint)
        canvas?.drawLine(halfWidth, halfHeight - margin, halfWidth, halfHeight + margin, paint)
    }

    private fun setupCrossLinePaint(): Paint {
        return Paint().apply {
            isAntiAlias = true
            color = ContextCompat.getColor(c, android.R.color.holo_green_dark)
            strokeWidth = 6f
        }
    }

}
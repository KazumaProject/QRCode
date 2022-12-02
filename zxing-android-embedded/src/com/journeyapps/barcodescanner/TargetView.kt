package com.journeyapps.barcodescanner

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.zxing.client.android.R

open class TargetView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : View(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context): this(context, null, 0, 0){
        expandedAnimation()
    }
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0, 0){
        expandedAnimation()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): this(context, attrs, defStyleAttr, 0){
        expandedAnimation()
    }

    private lateinit var objectAnimator: ObjectAnimator

    var isCrossLineVisible = false

    var targetMaskVisibility = false

    var isBarcodeModeOn = false

    private val c: Context by lazy {
        context
    }

    private val margin: Float = 200f
    private val marginB: Float = 100f

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if(isBarcodeModeOn){
            drawLeftTopLineB(canvas, setupPaint(),getPath())
            drawRightTopLineB(canvas, setupPaint(),getPath())
            drawLeftBottomLineB(canvas, setupPaint(),getPath())
            drawRightBottomLineB(canvas, setupPaint(),getPath())
        }else{
            drawLeftTopLine(canvas, setupPaint(),getPath())
            drawLeftBottomLine(canvas, setupPaint(),getPath())
            drawRightTopLine(canvas, setupPaint(),getPath())
            drawRightBottomLine(canvas, setupPaint(),getPath())
        }
        if (isCrossLineVisible)
            drawCrossLine(canvas,setupCrossLinePaint())
        if (targetMaskVisibility){
            objectAnimator.cancel()
        }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        objectAnimator.cancel()
    }
    private fun drawLeftTopLine(canvas: Canvas?, paint: Paint, path: Path) {
        path.rewind()
        path.moveTo(margin,30f)
        path.lineTo(10f,30f)
        path.lineTo(10f,margin + 30)
        paint.style = Paint.Style.STROKE
        paint.pathEffect = CornerPathEffect(90f)
        paint.strokeCap = Paint.Cap.ROUND
        canvas?.drawPath(path,paint)
        path.close()
    }

    private fun drawLeftBottomLine(canvas: Canvas?, paint: Paint, path: Path) {
        path.rewind()
        path.moveTo(margin,height.toFloat() - 30f)
        path.lineTo(10f,height.toFloat() - 30f)
        path.lineTo(10f,height.toFloat() - margin - 30)
        paint.style = Paint.Style.STROKE
        paint.pathEffect = CornerPathEffect(90f)
        paint.strokeCap = Paint.Cap.ROUND
        canvas?.drawPath(path,paint)
        path.close()
        /*canvas?.drawLine(0.0f, height.toFloat(), margin, height.toFloat(), paint)
        canvas?.drawLine(0.0f, height.toFloat() - margin, 0.0f, height.toFloat(), paint)*/
    }

    private fun drawRightTopLine(canvas: Canvas?, paint: Paint, path: Path) {
        path.rewind()
        path.moveTo(width.toFloat() - margin ,30f)
        path.lineTo(width.toFloat() - 10f,30f)
        path.lineTo(width.toFloat() - 10f,margin + 30)
        paint.style = Paint.Style.STROKE
        paint.pathEffect = CornerPathEffect(90f)
        paint.strokeCap = Paint.Cap.ROUND
        canvas?.drawPath(path,paint)
        path.close()
    }

    private fun drawRightBottomLine(canvas: Canvas?, paint: Paint, path: Path) {
        path.rewind()
        path.moveTo(width.toFloat() - margin,height.toFloat() - 30f)
        path.lineTo(width.toFloat() - 10f,height.toFloat() - 30f)
        path.lineTo(width.toFloat() - 10f,height.toFloat() - margin - 30f)
        paint.style = Paint.Style.STROKE
        paint.pathEffect = CornerPathEffect(90f)
        paint.strokeCap = Paint.Cap.ROUND
        canvas?.drawPath(path,paint)
        path.close()
    }

    private fun drawLeftTopLineB(canvas: Canvas?, paint: Paint, path: Path) {
        path.rewind()
        path.moveTo(marginB,330f)
        path.lineTo(10f,330f)
        path.lineTo(10f,marginB + 330f)
        paint.style = Paint.Style.STROKE
        paint.pathEffect = CornerPathEffect(90f)
        paint.strokeCap = Paint.Cap.ROUND
        canvas?.drawPath(path,paint)
        path.close()
    }

    private fun drawLeftBottomLineB(canvas: Canvas?, paint: Paint, path: Path) {
        path.rewind()
        path.moveTo(marginB,height.toFloat() - 330f)
        path.lineTo(10f,height.toFloat() - 330f)
        path.lineTo(10f,height.toFloat() - 330f - marginB)
        paint.style = Paint.Style.STROKE
        paint.pathEffect = CornerPathEffect(90f)
        paint.strokeCap = Paint.Cap.ROUND
        canvas?.drawPath(path,paint)
        path.close()
    }

    private fun drawRightTopLineB(canvas: Canvas?, paint: Paint, path: Path) {
        path.rewind()
        path.moveTo(width.toFloat() - marginB ,330f)
        path.lineTo(width.toFloat() - 10f,330f)
        path.lineTo(width.toFloat() - 10f,marginB + 330)
        paint.style = Paint.Style.STROKE
        paint.pathEffect = CornerPathEffect(90f)
        paint.strokeCap = Paint.Cap.ROUND
        canvas?.drawPath(path,paint)
        path.close()
    }

    private fun drawRightBottomLineB(canvas: Canvas?, paint: Paint, path: Path) {
        path.rewind()
        path.moveTo(width.toFloat() - marginB,height.toFloat() - 330f)
        path.lineTo(width.toFloat() - 10f,height.toFloat() - 330f)
        path.lineTo(width.toFloat() - 10f,height.toFloat() - 330f -  marginB)
        paint.style = Paint.Style.STROKE
        paint.pathEffect = CornerPathEffect(90f)
        paint.strokeCap = Paint.Cap.ROUND
        canvas?.drawPath(path,paint)
        path.close()
    }

    private fun setupPaint(): Paint {
        return Paint().apply {
            isAntiAlias = true
            color = ContextCompat.getColor(c, R.color.zxing_off_white)
            strokeWidth = 8f
        }
    }

    private fun setupPaintRectangle(): Paint {
        return Paint().apply {
            isAntiAlias = true
            color = ContextCompat.getColor(c, R.color.zxing_viewfinder_mask)
            alpha = 60
            strokeWidth = 12f
        }
    }

    private fun getPath(): Path{
        return Path()
    }

    private fun drawCenterRectangle(canvas: Canvas?, paint: Paint) {
        val rect = Rect().apply {
            set(64, 64, width - 64, height - 64)
        }
        canvas?.drawRoundRect(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat(),
            40f,
            40f,
            paint
        )
    }

    private fun expandedAnimation() {
        val expandedScaleX = PropertyValuesHolder.ofFloat("scaleX", 1.03f, 0.98f)
        val expandedScaleY = PropertyValuesHolder.ofFloat("scaleY", 1.03f, 0.98f)

        objectAnimator = ObjectAnimator.ofPropertyValuesHolder(this, expandedScaleX, expandedScaleY).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        objectAnimator.start()
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
            color = ContextCompat.getColor(c, android.R.color.holo_orange_light)
            strokeWidth = 2f
        }
    }

}
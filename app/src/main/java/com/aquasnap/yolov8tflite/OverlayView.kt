package com.aquasnap.yolov8tflite

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var bounds = Rect()

    private var boundingBoxClickListener: ((BoundingBox) -> Unit)? = null

    init {
        initPaints()
    }

    fun clear() {
        results = emptyList()  // Clear the bounding boxes
        textPaint.reset()
        textBackgroundPaint.reset()
        invalidate()  // Force a redraw with no bounding boxes
        initPaints()  // Reinitialize the paints
    }


    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach {
            val left = it.x1 * width
            val top = it.y1 * height
            val right = it.x2 * width
            val bottom = it.y2 * height

            val boxPaint = Paint().apply {
                color = when (it.cls) {
                    0 -> Color.RED
                    1 -> Color.GREEN
                    2 -> Color.BLUE
                    else -> Color.YELLOW
                }
                strokeWidth = 8F
                style = Paint.Style.STROKE
            }

            canvas.drawRect(left, top, right, bottom, boxPaint)

            // Format the text to show class name and confidence percentage
            val drawableText = "${it.clsName} (${it.confidencePercentage}%)"  // Include confidence percentage

            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()

            // Draw the background rectangle for text
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw the class name and confidence percentage
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }
    }





    fun setResults(boundingBoxes: List<BoundingBox>) {
        if (boundingBoxes.isEmpty()) {
            results = emptyList()  // Ensure results are cleared if no bounding boxes are detected
        } else {
            results = boundingBoxes
        }
        invalidate()  // Redraw the view with the updated results
    }


    fun setOnBoundingBoxClickListener(listener: (BoundingBox) -> Unit) {
        boundingBoxClickListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            // Check if touch is within any bounding box
            results.forEach { box ->
                val left = box.x1 * width
                val top = box.y1 * height
                val right = box.x2 * width
                val bottom = box.y2 * height

                if (x in left..right && y in top..bottom) {
                    // Call listener with the clicked bounding box
                    boundingBoxClickListener?.invoke(box)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun getResults(): List<BoundingBox> {
        return results
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}

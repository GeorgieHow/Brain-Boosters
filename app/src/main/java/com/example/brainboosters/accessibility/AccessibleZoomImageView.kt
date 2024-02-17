package com.example.brainboosters.accessibility

import android.content.Context
import android.graphics.*

import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener

class AccessibleZoomImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {


    private val zoomMatrix = Matrix()
    private var zoomInProgress = false

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && !zoomInProgress) {
            // Before zooming, change the scale type to MATRIX to allow for zoom manipulation
            scaleType = ScaleType.MATRIX
            zoomToPoint(event.x, event.y)
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun zoomToPoint(x: Float, y: Float) {
        // Assume scaleFactor is determined dynamically or fixed as per your requirement
        val scaleFactor = 2f

        // Adjust the touch coordinates to account for the image's scale and position
        val adjustedCoords = adjustTouchCoordinates(x, y)
        val adjustedX = adjustedCoords.first
        val adjustedY = adjustedCoords.second

        // Apply zoom using adjusted coordinates
        zoomMatrix.reset() // Reset the matrix to apply a new scale
        zoomMatrix.postScale(scaleFactor, scaleFactor, adjustedX, adjustedY)
        imageMatrix = zoomMatrix // Correctly assign the modified matrix to imageMatrix
        zoomInProgress = true

        // Schedule the resetZoom to run after a delay
        postDelayed({ resetZoom() }, 2000) // Delay for 2 seconds before resetting
    }
    private fun adjustTouchCoordinates(x: Float, y: Float): Pair<Float, Float> {
        val drawable = drawable ?: return Pair(x, y) // Ensure there is a drawable

        val imageViewWidth = width
        val imageViewHeight = height

        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight

        val scale: Float
        val dx: Float
        val dy: Float

        // Assuming the image is centered in the ImageView
        if (imageViewWidth * drawableHeight > imageViewHeight * drawableWidth) {
            // Image is limited by height, calculate vertical margins
            scale = imageViewHeight.toFloat() / drawableHeight.toFloat()
            dx = (imageViewWidth - drawableWidth * scale) * 0.5f
            dy = 0f
        } else {
            // Image is limited by width, calculate horizontal margins
            scale = imageViewWidth.toFloat() / drawableWidth.toFloat()
            dx = 0f
            dy = (imageViewHeight - drawableHeight * scale) * 0.5f
        }

        // Adjust the touch coordinates
        val adjustedX = (x - dx) / scale
        val adjustedY = (y - dy) / scale

        return Pair(adjustedX, adjustedY)
    }

    fun resetZoom() {
        // After zooming out, reset the scale type to one that fits the image without zoom
        scaleType = ScaleType.FIT_CENTER

        // Reset the zoom matrix
        imageMatrix = Matrix()

        // Set zoomInProgress to false to allow for new zoom operations
        zoomInProgress = false

        // Request layout to apply the new scale type and matrix
        requestLayout()
    }

}
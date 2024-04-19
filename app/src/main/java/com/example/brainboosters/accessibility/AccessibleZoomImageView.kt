package com.example.brainboosters.accessibility

import android.content.Context
import android.graphics.*

import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView

/**
 * A custom ImageView class that lets you zoom a specific point on the image, based on
 * where you click it. Used for making the pictures in the quiz more accessible and easier to
 * see if there are smaller details in the background.
 *
 * @param context The Context the view is running in.
 * @param attrs The attributes of the XML tag.
 * @param defStyleAttr A style attribute from the current theme applied to this view.
 */
class AccessibleZoomImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    // Sets up a matrix, and a boolean to help with the zoom
    private val zoomMatrix = Matrix()
    private var zoomInProgress = false

    // On initialisation, the scale is set to a Matrix
    init {
        scaleType = ScaleType.MATRIX
    }

    /**
     * Overrides the onTouchEvent, so when the image is touched, it will zoom. Wherever the user
     * touches is where it will zoom into.
     *
     * @param event MotionEvent instance containing full information about the event.
     * @return Boolean value, which tells whether the event was carried out or not. True if done,
     * false if not.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && !zoomInProgress) {
            scaleType = ScaleType.MATRIX
            zoomToPoint(event.x, event.y)
            return true
        }
        return super.onTouchEvent(event)
    }

    /**
    * Zooms to the specific point touched.
    *
    * @param x The x-coordinate of where the user touches.
    * @param y The y-coordinate of where the user touches.
    */
    private fun zoomToPoint(x: Float, y: Float) {

        // Scale of the zoom
        val scaleFactor = 2f
        val adjustedCoords = adjustTouchCoordinates(x, y)
        val adjustedX = adjustedCoords.first
        val adjustedY = adjustedCoords.second

        // Resets the Matrix, and scales it to the right x and y coordinates.
        zoomMatrix.reset()
        zoomMatrix.postScale(scaleFactor, scaleFactor, adjustedX, adjustedY)
        imageMatrix = zoomMatrix
        zoomInProgress = true

        // Automatically reset zoom after 2 seconds
        postDelayed({ resetZoom() }, 2000)
    }

    /**
     * Converts coordinates of where the user touched on the screen, to match the
     * coordinates on the image clicked.
     *
     * @param x The x-coordinate of the touch.
     * @param y The y-coordinate of the touch.
     * @return A Pair containing the adjusted x and y coordinates.
     */
    private fun adjustTouchCoordinates(x: Float, y: Float): Pair<Float, Float> {
        val drawable = drawable ?: return Pair(x, y)

        val imageViewWidth = width
        val imageViewHeight = height
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight

        val scale: Float
        val dx: Float
        val dy: Float

        // Checks aspect ratio of the ImageView XML against the image inside it.
        if (imageViewWidth * drawableHeight > imageViewHeight * drawableWidth) {

            // Scales it to make sure it fits horizontally, and maintains its aspect ratio.
            scale = imageViewHeight.toFloat() / drawableHeight.toFloat()
            dx = (imageViewWidth - drawableWidth * scale) * 0.5f
            dy = 0f
        } else {

            // Scales it to make sure it fits vertically, and maintains its aspect ratio
            scale = imageViewWidth.toFloat() / drawableWidth.toFloat()
            dx = 0f
            dy = (imageViewHeight - drawableHeight * scale) * 0.5f
        }

        // Adjusts them with the scale being used
        val adjustedX = (x - dx) / scale
        val adjustedY = (y - dy) / scale

        return Pair(adjustedX, adjustedY)
    }

    /**
     * Resets the zoom level to the original scale and layout of the ImageView.
     */
    fun resetZoom() {

        scaleType = ScaleType.FIT_CENTER
        imageMatrix = Matrix()
        zoomInProgress = false

        // Redo layout with the original pictures dimensions
        requestLayout()
    }

}
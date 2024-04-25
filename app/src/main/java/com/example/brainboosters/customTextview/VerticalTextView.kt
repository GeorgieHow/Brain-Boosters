package com.example.brainboosters.customTextview

import android.content.Context
import android.graphics.Canvas
import android.text.BoringLayout
import android.text.Layout
import android.text.TextUtils.TruncateAt
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.withSave

/**
 * A custom text view created to draw sideways text boxes. Mainly used to create labels for
 * graphs on the statistics page, so they display better.
 *
 * @property context The Context the view is running in.
 * @property attrs The attributes of the XML tag.
 * */
class VerticalTextView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    init {
        if (layout == null) {
            setWillNotDraw(false)
        }
    }

    // Works out if the text should be drawn from top to bottom based on the gravity.
    private val isTopDown: Boolean
        get() = !(Gravity.isVertical(gravity) && gravity.and(Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM)

    /**
     * Sets the text for the view and invalidates the previous layout.
     *
     * @param text The text to display.
     * @param type If the text is editable or not.
     */
    override fun setText(text: CharSequence, type: BufferType) {
        super.setText(text, type)
        requestLayout()
        invalidate()
    }

    /**
     * Gets the existing layout for text drawing, so it can draw it sideways.
     */
    private val textLayout: Layout by lazy {
        BoringLayout.Metrics().also { metrics ->
            metrics.width = height
        }.let { metrics ->
            BoringLayout.make(
                text, paint, metrics.width, Layout.Alignment.ALIGN_NORMAL,
                2f, 0f, metrics, false, TruncateAt.END,
                height - compoundPaddingLeft - compoundPaddingRight
            )
        }
    }

    /**
     * Draws text vertically.
     *
     * @param canvas The canvas on which the view draws its content.
     */
    override fun onDraw(canvas: Canvas) {
        canvas.withSave {
            if (isTopDown) {
                val textShift = textSize - (paint.fontMetrics.bottom + paint.fontMetrics.descent)
                translate(textShift, 0f)
                rotate(90f)
            } else {
                translate(textSize, height.toFloat())
                rotate(-90f)
            }
            translate(compoundPaddingLeft.toFloat(), extendedPaddingTop.toFloat())
            textLayout.draw(this)
        }
    }
}
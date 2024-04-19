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

    // Works out if the text should be drawn from top to bottom based on the gravity.
    private val topDown = gravity.let { g ->
        !(Gravity.isVertical(g) && g.and(Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM)
    }

    // Metrics used to measure the width and height of the view's text content that is
    // currently in it.
    private val metrics = BoringLayout.Metrics()
    private var padLeft = 0
    private var padTop = 0

    // Keeps layout stored so no need to recalculate it if not needed
    private var layout1: Layout? = null

    /**
     * Sets the text for the view and invalidates the previous layout.
     *
     * @param text The text to display.
     * @param type If the text is editable or not.
     */
    override fun setText(text: CharSequence, type: BufferType) {
        super.setText(text, type)
        layout1 = null
    }

    /**
     * Gets the existing layout for text drawing, so it can draw it sideways.
     */
    private fun makeLayout(): Layout {
        if (layout1 == null) {
            metrics.width = height
            paint.color = currentTextColor
            paint.drawableState = drawableState
            layout1 = BoringLayout.make(text, paint, metrics.width, Layout.Alignment.ALIGN_NORMAL, 2f, 0f, metrics, false, TruncateAt.END, height - compoundPaddingLeft - compoundPaddingRight)
            padLeft = compoundPaddingLeft
            padTop = extendedPaddingTop
        }
        return layout1!!
    }

    /**
     * Draws text vertically.
     *
     * @param c The canvas on which the view draws its content.
     */
    override fun onDraw(c: Canvas) {
        if (layout == null)
            return
        c.withSave {
            // Depending on gravity set at start, rotates the textview
            if (topDown) {
                val fm = paint.fontMetrics
                // Translates it 90 clockwise
                translate(textSize - (fm.bottom + fm.descent), 0f)
                rotate(90f)
            } else {
                // Translates it 90 counter clockwise
                translate(textSize, height.toFloat())
                rotate(-90f)
            }
            translate(padLeft.toFloat(), padTop.toFloat())
            makeLayout().draw(this)
        }
    }
}
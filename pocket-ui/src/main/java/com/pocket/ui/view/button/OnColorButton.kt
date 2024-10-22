package com.pocket.ui.view.button

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import com.pocket.ui.R
import com.pocket.ui.util.cutoutFromCanvas

/**
 * A button white button with a foreground color the same as the real background color
 */
internal class OnColorButton : BoxButtonBase {
    private var mBackground: ButtonBoxDrawable =
        ButtonBoxDrawable(context, R.color.pkt_button_box_oncolor_fill, 0).apply {
            callback = this@OnColorButton
        }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context,
        attrs,
        defStyle) {
        init()
    }

    private fun init() {
        setTextColor(Color.BLACK)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBackground.setBounds(0, 0, w, h)
    }

    override fun drawableStateChanged() {
        mBackground.alpha = (255 * if (isPressed) 0.5f else 1.0f).toInt()
        super.drawableStateChanged()
    }

    override fun onDraw(canvas: Canvas) {
        // Draw the button background first.
        mBackground.draw(canvas)
        // Cut out the text drawn by the base class which is a TextView in this case.
        cutoutFromCanvas(canvas) {
            super.onDraw(canvas)
        }
    }
}
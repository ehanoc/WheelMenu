package com.ehanoc.menuwheel.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Created by bruno.
 */
class CustomCircleImageView : CircleImageView {

    /**
     *
     * @param context
     */
    constructor(context: Context) : super(context) {}

    /**
     *
     * @param context
     * @param attrs
     */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    /**
     *
     * @param context
     * @param attrs
     * @param defStyle
     */
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {}

    /**
     *
     * @param event
     * @return
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }

    /**
     *
     */
    protected fun init() {

    }
}

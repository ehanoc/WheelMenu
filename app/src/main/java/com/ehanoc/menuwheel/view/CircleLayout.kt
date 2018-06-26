package com.ehanoc.menuwheel.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.*
import android.widget.LinearLayout
import com.ehanoc.menuwheel.R
import java.util.HashSet

/*
 * Copyright dmitry.zaicew@gmail.com Dmitry Zaitsev
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

// Changed the original class to fit the requirements and to Kotlin language
class CircleLayout @SuppressLint("NewApi")
constructor(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {

    private var mLayoutMode = LAYOUT_NORMAL

    private var mInnerCircle: Drawable? = null

    private var mAngleOffset: Float = 0.toFloat()
    private var mAngleRange: Float = 0.toFloat()

    private var mDividerWidth: Float = 0.toFloat()
    private var mInnerRadius: Int = 0

    private val mDividerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mCirclePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mBounds = RectF()

    private var mDst: Bitmap? = null
    private var mSrc: Bitmap? = null
    private var mSrcCanvas: Canvas? = null
    private var mDstCanvas: Canvas? = null
    private val mXfer: Xfermode
    private val mXferPaint: Paint

    private val mMotionTarget: View? = null

    private var mDrawingCache: Bitmap? = null
    private var mCachedCanvas: Canvas? = null
    private val mDirtyViews = HashSet<View>()
    private var mCached = false

    val radius: Int
        get() {
            val width = width
            val height = height

            val minDimen = (if (width > height) height else width).toFloat()

            val radius = (minDimen - mInnerRadius) / 2f

            return radius.toInt()
        }

    var angleOffset: Float
        get() = mAngleOffset
        set(offset) {
            mAngleOffset = offset
            requestLayout()
            invalidate()
        }

    var innerRadius: Int
        get() = mInnerRadius
        set(radius) {
            mInnerRadius = radius
            requestLayout()
            invalidate()
        }

    var innerCircle: Drawable?
        get() = mInnerCircle
        set(d) {
            mInnerCircle = d
            requestLayout()
            invalidate()
        }

    constructor(context: Context) : this(context, null) {}

    init {

        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleLayout, 0, 0)

        val res = resources

        try {
            val dividerColor = a.getColor(R.styleable.CircleLayout_sliceDivider, res.getColor(android.R.color.transparent))
            mInnerCircle = a.getDrawable(R.styleable.CircleLayout_innerCircle)

            if (mInnerCircle is ColorDrawable) {
                val innerColor = a.getColor(R.styleable.CircleLayout_innerCircle, res.getColor(android.R.color.white))
                mCirclePaint.color = innerColor
            }

            mDividerPaint.color = dividerColor

            mAngleOffset = a.getFloat(R.styleable.CircleLayout_angleOffset, 90f)
            mAngleRange = a.getFloat(R.styleable.CircleLayout_angleRange, 360f)
            mDividerWidth = a.getDimensionPixelSize(R.styleable.CircleLayout_dividerWidth, 1).toFloat()
            mInnerRadius = a.getDimensionPixelSize(R.styleable.CircleLayout_innerRadius, 80)

            mLayoutMode = a.getColor(R.styleable.CircleLayout_layoutMode, LAYOUT_PIE)
        } finally {
            a.recycle()
        }

        mDividerPaint.strokeWidth = mDividerWidth

        mXfer = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        mXferPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        //Turn off hardware acceleration if possible
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

    }


    override fun setLayoutMode(mode: Int) {
        mLayoutMode = mode
        requestLayout()
        invalidate()
    }

    override fun getLayoutMode(): Int {
        return mLayoutMode
    }

    fun getCenter(p: PointF) {
        p.set(width / 2f, (height / 2).toFloat())
    }

    fun setInnerCircle(res: Int) {
        mInnerCircle = context.resources.getDrawable(res)
        requestLayout()
        invalidate()
    }

    fun setInnerCircleColor(color: Int) {
        mInnerCircle = ColorDrawable(color)
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val count = childCount

        var maxHeight = 0
        var maxWidth = 0

        // Find rightmost and bottommost child
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
                maxWidth = Math.max(maxWidth, child.measuredWidth)
                maxHeight = Math.max(maxHeight, child.measuredHeight)
            }
        }

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, suggestedMinimumHeight)
        maxWidth = Math.max(maxWidth, suggestedMinimumWidth)

        val width = View.resolveSize(maxWidth, widthMeasureSpec)
        val height = View.resolveSize(maxHeight, heightMeasureSpec)

        setMeasuredDimension(width, height)

        if (mSrc != null && (mSrc!!.width != width || mSrc!!.height != height)) {
            mDst!!.recycle()
            mSrc!!.recycle()
            mDrawingCache!!.recycle()

            mDst = null
            mSrc = null
            mDrawingCache = null
        }

        if (mSrc == null) {
            mSrc = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            mDst = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            mDrawingCache = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            mSrcCanvas = Canvas(mSrc!!)
            mDstCanvas = Canvas(mDst!!)
            mCachedCanvas = Canvas(mDrawingCache!!)
        }
    }

    private fun layoutParams(child: View): LayoutParams {
        return child.layoutParams as LayoutParams
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val childs = childCount

        var totalWeight = 0f

        for (i in 0 until childs) {
            val child = getChildAt(i)

            val lp = layoutParams(child)

            totalWeight += lp.weight
        }

        val width = width
        val height = height

        val minDimen = (if (width > height) height else width).toFloat()
        val radius = (minDimen - mInnerRadius) / 2f

        mBounds.set(width / 2 - minDimen / 2, height / 2 - minDimen / 2, width / 2 + minDimen / 2, height / 2 + minDimen / 2)

        var startAngle = mAngleOffset

        for (i in 0 until childs) {
            val child = getChildAt(i)

            val lp = layoutParams(child)

            val angle = mAngleRange / totalWeight * lp.weight

            val centerAngle = startAngle + angle / 2f
            val x: Int
            val y: Int

            if (childs > 1) {
                x = (radius * Math.cos(Math.toRadians(centerAngle.toDouble()))).toInt() + width / 2
                y = (radius * Math.sin(Math.toRadians(centerAngle.toDouble()))).toInt() + height / 2
            } else {
                x = width / 2
                y = height / 2
            }

            val halfChildWidth = child.measuredWidth / 2
            val halfChildHeight = child.measuredHeight / 2

            val left = if (lp.width != MATCH_PARENT) x - halfChildWidth else 0
            val top = if (lp.height != MATCH_PARENT) y - halfChildHeight else 0
            val right = if (lp.width != MATCH_PARENT) x + halfChildWidth else width
            val bottom = if (lp.height != MATCH_PARENT) y + halfChildHeight else height

            child.layout(left, top, right, bottom)

            if (left != child.left || top != child.top
                    || right != child.right || bottom != child.bottom
                    || lp.startAngle != startAngle
                    || lp.endAngle != startAngle + angle) {
                mCached = false
            }

            lp.startAngle = startAngle

            startAngle += angle

            lp.endAngle = startAngle
        }

        invalidate()
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
        val lp = LayoutParams(p.width, p.height)

        if (p is LinearLayout.LayoutParams) {
            lp.weight = p.weight
        }

        return lp
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    private fun drawChild(canvas: Canvas, child: View, lp: LayoutParams) {
        mSrcCanvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        mDstCanvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        mSrcCanvas!!.save()

        val childLeft = child.left
        val childTop = child.top
        val childRight = child.right
        val childBottom = child.bottom

        mSrcCanvas!!.clipRect(childLeft.toFloat(), childTop.toFloat(), childRight.toFloat(), childBottom.toFloat(), Region.Op.REPLACE)
        mSrcCanvas!!.translate(childLeft.toFloat(), childTop.toFloat())

        child.draw(mSrcCanvas)

        mSrcCanvas!!.restore()

        mXferPaint.xfermode = null
        mXferPaint.color = Color.BLACK

        val sweepAngle = (lp.endAngle - lp.startAngle) % 361

        mDstCanvas!!.drawArc(mBounds, lp.startAngle, sweepAngle, true, mXferPaint)
        mXferPaint.xfermode = mXfer
        mDstCanvas!!.drawBitmap(mSrc!!, 0f, 0f, mXferPaint)

        canvas.drawBitmap(mDst!!, 0f, 0f, null)
    }

    private fun redrawDirty(canvas: Canvas) {
        for (child in mDirtyViews) {
            drawChild(canvas, child, layoutParams(child))
        }

        if (mMotionTarget != null) {
            drawChild(canvas, mMotionTarget, layoutParams(mMotionTarget))
        }
    }

    private fun drawDividers(canvas: Canvas, halfWidth: Float, halfHeight: Float, radius: Float) {
        val childs = childCount

        if (childs < 2) {
            return
        }

        for (i in 0 until childs) {
            val child = getChildAt(i)
            val lp = layoutParams(child)

            canvas.drawLine(halfWidth, halfHeight,
                    radius * Math.cos(Math.toRadians(lp.startAngle.toDouble())).toFloat() + halfWidth,
                    radius * Math.sin(Math.toRadians(lp.startAngle.toDouble())).toFloat() + halfHeight,
                    mDividerPaint)

            if (i == childs - 1) {
                canvas.drawLine(halfWidth, halfHeight,
                        radius * Math.cos(Math.toRadians(lp.endAngle.toDouble())).toFloat() + halfWidth,
                        radius * Math.sin(Math.toRadians(lp.endAngle.toDouble())).toFloat() + halfHeight,
                        mDividerPaint)
            }
        }
    }

    private fun drawInnerCircle(canvas: Canvas, halfWidth: Float, halfHeight: Float) {
        if (mInnerCircle != null) {
            if (mInnerCircle !is ColorDrawable) {
                mInnerCircle!!.setBounds(
                        halfWidth.toInt() - mInnerRadius,
                        halfHeight.toInt() - mInnerRadius,
                        halfWidth.toInt() + mInnerRadius,
                        halfHeight.toInt() + mInnerRadius)

                mInnerCircle!!.draw(canvas)
            } else {
                canvas.drawCircle(halfWidth, halfHeight, mInnerRadius.toFloat(), mCirclePaint)
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        var _canvas = canvas
        if (mLayoutMode == LAYOUT_NORMAL) {
            super.dispatchDraw(_canvas)
            return
        }

        if (mSrc == null || mDst == null || mSrc!!.isRecycled || mDst!!.isRecycled) {
            return
        }

        val childs = childCount

        val halfWidth = width / 2f
        val halfHeight = height / 2f

        val radius = if (halfWidth > halfHeight) halfHeight else halfWidth

        if (mCached && mDrawingCache != null && !mDrawingCache!!.isRecycled && mDirtyViews.size < childs / 2) {
            _canvas.drawBitmap(mDrawingCache!!, 0f, 0f, null)

            redrawDirty(_canvas)

            drawDividers(_canvas, halfWidth, halfHeight, radius)

            drawInnerCircle(_canvas, halfWidth, halfHeight)

            return
        } else {
            mCached = false
        }

        var sCanvas: Canvas? = null

        if (mCachedCanvas != null) {
            sCanvas = _canvas
            _canvas = mCachedCanvas as Canvas
        }

        val bkg = background
        bkg?.draw(_canvas)

        for (i in 0 until childs) {
            val child = getChildAt(i)
            val lp = layoutParams(child)

            drawChild(_canvas, child, lp)
        }

        drawDividers(_canvas, halfWidth, halfHeight, radius)

        drawInnerCircle(_canvas, halfWidth, halfHeight)

        if (mCachedCanvas != null) {
            sCanvas!!.drawBitmap(mDrawingCache!!, 0f, 0f, null)
            mDirtyViews.clear()
            mCached = true
        }
    }

    class LayoutParams : ViewGroup.LayoutParams {

        var startAngle: Float = 0.toFloat()
        var endAngle: Float = 0.toFloat()

        var weight = 1f

        constructor(width: Int, height: Int) : super(width, height) {}

        constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    }

    companion object {

        val LAYOUT_NORMAL = 1
        val LAYOUT_PIE = 2
    }

}
package com.ehanoc.menuwheel.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.ehanoc.menuwheel.R
import com.nightonke.boommenu.BoomButtons.BoomButton
import com.nightonke.boommenu.OnBoomListener


class WheelMenuLayout(context: Context, attrs: AttributeSet): ConstraintLayout(context, attrs) {

    private var imageOriginal: Bitmap? = null
    private var imageScaled: Bitmap? = null     //variables for original and re-sized image
    private var _matrix: Matrix? = null                         //Matrix used to perform rotations
    private var wheelHeight:Int = 0
    private var wheelWidth = 0           //height and width of the view
    private var _top:Int = 0                               //the current _top of the wheel (calculated in
     // wheel divs)
        private var totalRotation:Double = 0.toDouble()                  //variable that counts the total rotation
     // during a given rotation of the wheel by the
        // user (from ACTION_DOWN to ACTION_UP)
        private var divCount:Int = 0                          //no of divisions in the wheel
    private var divAngle:Int = 0                          //angle of each division
    private var selectedPosition:Int = 0                  //the section currently selected by the user.
    private var snapToCenterFlag = true       //variable that determines whether to snap the

     private var _context: Context? = null
    private var wheelChangeListener:WheelChangeListener? = null

     //
        private val gestureDetector: GestureDetector? = null
    private val isRotating:Boolean = false

    private var mCircleLayout:CircleLayout? = null
    private var mBackgroundMenu: ImageView? = null

    init{
        init(context, attrs)
    }

     //initializations
     private fun init(context: Context, attrs: AttributeSet) {
         this._context = context
         selectedPosition = 0

         val a = getContext().obtainStyledAttributes(attrs, R.styleable.WheelMenuLayout)

         val divInt = a.getInt(R.styleable.WheelMenuLayout_dividers, 12)
         setDivCount(divInt)

         if (_matrix == null)
        {
            _matrix = Matrix()
        } else {
            _matrix?.reset()
        }

         setOnTouchListener(WheelTouchListener())
    }

    override fun onViewAdded(view: View?) {
        super.onViewAdded(view)
        prepareWheelUIElements()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

//        val boom: BoomMenuButton = rootView.findViewById(R.id.bmb)
//        boom.onBoomListener = EmptyBoomListener()
    }

    /**
     * Add a new listener to observe user selection changes.
     *
     * @param wheelChangeListener
     */
    fun setWheelChangeListener(wheelChangeListener:WheelChangeListener) {
        this.wheelChangeListener = wheelChangeListener
    }

    /**
     * Returns the position currently selected by the user.
     *
     * @return the currently selected position between 1 and divCount.
     */
    fun getSelectedPosition():Int {
        return selectedPosition
    }

    /**
     * Set no of divisions in the wheel menu.
     *
     * @param divCount no of divisions.
     */
    fun setDivCount(divCount:Int) {
        this.divCount = divCount

        divAngle = 360 / divCount
        totalRotation = (-1 * (divAngle / 2)).toDouble()

        mCircleLayout?.rotation = (360 / divCount).toFloat()
    }

    /**
     * Set the snap to center flag. If true, wheel will always snap to center of current section.
     *
     * @param snapToCenterFlag
     */
    fun setSnapToCenterFlag(snapToCenterFlag:Boolean) {
        this.snapToCenterFlag = snapToCenterFlag
    }

    /**
     * Set a different _top position. Default _top position is 0.
     * Should be set after {#setDivCount(int) setDivCount} method and the value should be greater
     * than 0 and lesser
     * than divCount, otherwise the provided value will be ignored.
     *
     * @param newTopDiv
     */
    fun setAlternateTopDiv(newTopDiv:Int) {
        if (newTopDiv < 0 || newTopDiv >= divCount)
            return
        else
            _top = newTopDiv

        selectedPosition = _top
    }

    /**
     * Set the wheel image.
     *
     * @param bitmap the bitmap
     */
    fun setWheelImage(bitmap: Bitmap) {
        imageOriginal = bitmap
        mBackgroundMenu!!.scaleType = ImageView.ScaleType.MATRIX
        mBackgroundMenu!!.setImageBitmap(imageOriginal)
    }

    /**
     *
     * @param circuleLayout
     * @param mWheelBackgroundMenu
     */
    fun prepareWheelUIElements() {
        mCircleLayout = findViewById(R.id.circle_layout_id)
        mBackgroundMenu = findViewById(R.id.wheelmenu_background_menu)

        val bm = (mBackgroundMenu?.drawable as BitmapDrawable).bitmap
        setWheelImage(bm)
    }

     /*
      * We need this to get the dimensions of the view. Once we get those,
      * We can scale the image to make sure it's proper,
      * Initialize the _matrix and align it with the views center.
       */
     override fun onSizeChanged(w:Int, h:Int, oldw:Int, oldh:Int) {
        super.onSizeChanged(w, h, oldw, oldh)

         if (imageOriginal == null) return


         // method called multiple times but initialized just once
         if (wheelHeight == 0 || wheelWidth == 0)
         {
            wheelHeight = h
            wheelWidth = w
             // resize the image
             val resize = Matrix()
            resize.postScale(Math.min(wheelWidth, wheelHeight).toFloat() / imageOriginal!!.width.toFloat(),
                    Math.min(wheelWidth,
                    wheelHeight).toFloat() / imageOriginal!!.height.toFloat())

            imageScaled = Bitmap.createBitmap(imageOriginal!!, 0, 0, imageOriginal!!.width,
            imageOriginal!!.height, resize, false)

             // translate the _matrix to the image view's center
             val translateX = (wheelWidth / 2 - imageScaled!!.width / 2).toFloat()
             val translateY = (wheelHeight / 2 - imageScaled!!.height / 2).toFloat()
             _matrix?.postTranslate(translateX, translateY)

             mBackgroundMenu?.setImageBitmap(imageScaled)
             mBackgroundMenu?.imageMatrix = _matrix
         }
     }

    /**
     * get the angle of a touch event.
     */
    private fun getAngle(touchX:Double, touchY:Double):Double {
        var x = touchX
        var y = touchY
        x -= (wheelWidth / 2.0)
        y = wheelHeight.toDouble() - y - (wheelHeight / 2.0)

        return when (getQuadrant(x, y)) {
            1 -> Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI
            2 -> 180 - Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI
            3 -> 180 + (-1.0 * Math.asin(y / Math.hypot(x, y)) * 180.0 / Math.PI)
            4 -> 360 + Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI
            else -> 0.0
        }
    }

    /**
     * get the quadrant of the wheel which contains the touch point (x,y)
     *
     * @return quadrant 1,2,3 or 4
     */
    private fun getQuadrant(x:Double, y:Double):Int {
        return if (x >= 0) {
            if (y >= 0) 1 else 4
        } else {
            if (y >= 0) 2 else 3
        }
    }

    /**
     * rotate the wheel by the given angle
     *
     * @param degrees
     */
    private fun rotateWheel(degrees:Float) {
        _matrix?.postRotate(degrees, (wheelWidth / 2).toFloat(), (wheelHeight / 2).toFloat())
        mBackgroundMenu?.imageMatrix = _matrix

        //add the rotation to the total rotation
        totalRotation += degrees

        mCircleLayout?.rotation = mCircleLayout?.rotation!! + degrees
    }

    /**
     * Interface to to observe user selection changes.
     */
    interface WheelChangeListener {
        /**
     * Called when user selects a new position in the wheel menu.
     *
     * @param selectedPosition the new position selected.
     */
        fun onSelectionChange(selectedPosition:Int)
    }

     //listener for touch events on the wheel
     private inner class WheelTouchListener: View.OnTouchListener {
         private var startAngle:Double = 0.toDouble()

        override fun onTouch(v: View, event: MotionEvent):Boolean {

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    //get the start angle for the current move event
                    startAngle = getAngle(event.x.toDouble(), event.y.toDouble())
                }


                MotionEvent.ACTION_MOVE -> {
                    //get the current angle for the current move event
                    val currentAngle = getAngle(event.x.toDouble(), event.y.toDouble())

                    //rotate the wheel by the difference
                    rotateWheel((startAngle - currentAngle).toFloat())

                    //current angle becomes start angle for the next motion
                    startAngle = currentAngle
                }


                MotionEvent.ACTION_UP -> {
                     //get the total angle rotated in 360 degrees
                    totalRotation %= 360

                    //represent total rotation in positive value
                    if (totalRotation < 0)
                    {
                        totalRotation += 360
                    }

                     //calculate the no of divs the rotation has crossed
                    val nrDivsCrossed = ((totalRotation) / divAngle).toInt()

                    //calculate current _top
                    _top = (divCount + _top - nrDivsCrossed) % divCount

                    //for next rotation, the initial total rotation will be the no of degrees
                    // inside the current _top
                    totalRotation %= divAngle

                    //snapping to the _top's center
                    if (snapToCenterFlag)
                    {
                        //calculate the angle to be rotated to reach the _top's center.
                        val leftover = divAngle / 2 - totalRotation

                        rotateWheel((leftover).toFloat())

                        //re-initialize total rotation
                        totalRotation = (divAngle / 2).toDouble()
                    }

                    val newPosition:Int = if (_top == 0) {
                            divCount - 1//loop around the array
                        } else {
                            _top - 1
                        } //set the currently selected option

                    if (newPosition == selectedPosition) return false

                    selectedPosition = newPosition

                    wheelChangeListener?.onSelectionChange(getSelectedPosition())
                }
        }
        return true
        }
    }

//    public fun initCircleMenuBoom(drawables: IntArray, labels: Array<String>, listener: OnBoomListener) {
//        val boom: BoomMenuButton = rootView.findViewById(R.id.bmb)
//
//        for (i in 0 until drawables.size) {
//            boom.addBuilder(TextInsideCircleButton.Builder()
//                    .normalImageRes(drawables[i])
//                    .normalText(labels[i])
//                    .imageRect(Rect(Util.dp2px(25f), Util.dp2px(20f), Util.dp2px(55f), Util.dp2px(55f))))
//        }
//
//        boom.onBoomListener = listener
//    }

    private class EmptyBoomListener : OnBoomListener {
        override fun onBoomDidShow() {
        }

        override fun onBackgroundClick() {
        }

        override fun onClicked(index: Int, boomButton: BoomButton?) {
        }

        override fun onBoomDidHide() {
        }

        override fun onBoomWillHide() {
        }

        override fun onBoomWillShow() {
        }

    }
}
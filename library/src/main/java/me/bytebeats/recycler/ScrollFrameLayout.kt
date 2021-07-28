package me.bytebeats.recycler

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.MotionEventCompat
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.pow


/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created on 2021/7/28 15:07
 * @Version 1.0
 * @Description TO-DO
 */

class ScrollFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (mScrollableView?.left ?: 0 != 0) {
                mOnPositionChangeListener?.apply {
                    mScrollableView?.offsetTopAndBottom(getOffsetX() - mScrollableView!!.left)
                    mScrollableView?.offsetTopAndBottom(getOffsetY() - mScrollableView!!.top)
                }
            }
        }
    }

    var mScrollAxis = SCROLL_AXIS_HORIZONTAL

    private var mScrollableView: View? = null

    var mOnPositionChangeListener: OnPositionChangeListener? = null

    private val mDragHelper by quickLazy {
        ViewDragHelper.create(this, sensitivity, mViewDragCallback)
    }

    private val mViewDragCallback by quickLazy {
        object : ViewDragHelper.Callback() {
            override fun onViewDragStateChanged(state: Int) {
                super.onViewDragStateChanged(state)
                mDragState = state
            }

            override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
                super.onViewPositionChanged(changedView, left, top, dx, dy)
                mOnPositionChangeListener?.onPositionChanged(
                    this@ScrollFrameLayout,
                    changedView,
                    left,
                    top,
                    dx,
                    dy
                )
            }

            override fun getViewHorizontalDragRange(child: View): Int {
                return child.measuredWidth
            }

            override fun tryCaptureView(child: View, pointerId: Int): Boolean = child.id == mScrollableViewId

            override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
                return max(minOf(left, mContentOffsetX), -child.measuredWidth + getScreenWidth(context) + dragClip)
            }

            override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
                super.onViewReleased(releasedChild, xvel, yvel)
                flingView(xvel, yvel)
            }
        }
    }

    private val mTouchSlop by quickLazy {
        (ViewConfiguration.get(context).scaledTouchSlop / sensitivity).toInt()
    }

    var mContentOffsetX = 0
    var mScrollableViewId = 0
        set(value) {
            field = value
            mScrollableView = findViewById(field)
        }
    private var mDragState = ViewDragHelper.STATE_IDLE

    var dragClip = 0
    var isDragEnable = true

    interface OnPositionChangeListener {
        fun getOffsetX(): Int
        fun getOffsetY(): Int
        fun onPositionChanged(layout: ScrollFrameLayout, view: View, left: Int, top: Int, dx: Int, dy: Int)
    }


    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        val shouldInterceptTouchEvent = ev?.let { mDragHelper.shouldInterceptTouchEvent(it) } ?: false
        val isDragging = isDragging(ev)
        if (isDragging) {
            requestDisallowInterceptTouchEvent(true)
        }
        return shouldInterceptTouchEvent or isDragging
    }

    private var initialX = 0.0F
    private var initialY = 0.0F
    private fun isDragging(mv: MotionEvent?): Boolean {
        return when (MotionEventCompat.getActionMasked(mv)) {
            MotionEvent.ACTION_DOWN -> {
                initialX = mv?.x ?: 0.0F
                initialY = mv?.y ?: 0.0F
                false
            }
            MotionEvent.ACTION_MOVE -> {
                val x = MotionEventCompat.getX(mv, 0)
                val y = MotionEventCompat.getY(mv, 0)
                return checkTouchSlop(mScrollableView, x, y)
            }
            else -> false
        }
    }

    private fun checkTouchSlop(view: View?, dx: Float, dy: Float): Boolean {
        if (view != null) {
            val horizontalScroll = mViewDragCallback.getViewHorizontalDragRange(view) > 0
            val verticalScroll = mViewDragCallback.getViewVerticalDragRange(view) > 0
            if (horizontalScroll && verticalScroll) {
                return dx.pow(2) + dy.pow(2) > mTouchSlop.toFloat().pow(2)
            } else if (horizontalScroll) {
                val b = dx.absoluteValue > mTouchSlop
                if (b) {
                    mDragState = ViewDragHelper.STATE_DRAGGING
                }
                return b
            } else if (verticalScroll) {
                return dy.absoluteValue > mTouchSlop
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var result = false
        if (isScrolling() && isDragEnable) {
            event?.let {
                mDragHelper.processTouchEvent(it)
            }
            result = true
        } else {
            result = super.onTouchEvent(event)
        }
        return result
    }

    override fun computeScroll() {
        if (mDragHelper.continueSettling(false)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun isHorizontalScroll(): Boolean = mScrollAxis == SCROLL_AXIS_HORIZONTAL

    fun scrollItem(dx: Int, dy: Int) {
        mScrollableView?.let {
            if (isHorizontalScroll()) {
                val oldX = it.left
                val clampedX = mViewDragCallback.clampViewPositionHorizontal(it, oldX + dx, dx)
                val newDx = clampedX - oldX
                it.offsetLeftAndRight(newDx)
            } else {
                val oldY = it.top
                val clampedY = mViewDragCallback.clampViewPositionVertical(it, oldY + dy, dy)
                val newDy = clampedY - oldY
                it.offsetTopAndBottom(newDy)
            }
        }
    }

    fun scrollItemBy(dx: Int, dy: Int, listener: OnPositionChangeListener?) {
        mScrollableView?.let {
            val oldX = it.left
            val oldY = it.top
            val clampedX = mViewDragCallback.clampViewPositionHorizontal(it, oldX + dx, dx)
            val clampedY = mViewDragCallback.clampViewPositionVertical(it, oldY + dy, dy)
            mDragHelper.smoothSlideViewTo(it, clampedX, clampedY)
            invalidate()
            listener?.onPositionChanged(this, it, clampedX, clampedY, dx, dy)
        }
    }

    private fun isScrolling(): Boolean {
        return mDragState == ViewDragHelper.STATE_DRAGGING || mDragState == ViewDragHelper.STATE_SETTLING
    }

    private fun flingView(xvel: Float, yvel: Float) {
        mScrollableView?.let {
            val left = it.left + convertVelocityToDistance(xvel)
            val top = it.top + convertVelocityToDistance(yvel)
            val clampedX = mViewDragCallback.clampViewPositionHorizontal(it, left, xvel.toInt())
            val clampedY = mViewDragCallback.clampViewPositionVertical(it, top, yvel.toInt())
            mDragHelper.settleCapturedViewAt(clampedX, clampedY)
            invalidate()
        }
    }

    private fun convertVelocityToDistance(velocity: Float): Int = (velocity * scroll_factor).toInt()

    private fun <T> quickLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

    enum class ScrollAxis {
        HORIZONTAL, VERTICAL
    }

    companion object {
        private const val scroll_factor = 0.1
        private const val sensitivity = 2.0f

        fun getScreenWidth(context: Context): Int {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val dm = DisplayMetrics()
            wm.defaultDisplay.getMetrics(dm)
            return if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                dm.heightPixels
            } else {
                dm.widthPixels
            }
        }
    }
}
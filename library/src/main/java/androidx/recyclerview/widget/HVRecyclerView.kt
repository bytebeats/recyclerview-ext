package androidx.recyclerview.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.math.abs

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created on 2021/7/27 14:24
 * @Version 1.0
 * @Description 如果RecyclerView既支持水平滚动，也支持垂直滚动，那么，沿着45度方向滑动列表，会同时触发两个方向的滚动，这个限制HVRecyclerView限制列表同一次只能向一个方向滚动
 */

class HVRecyclerView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleRes: Int = 0
) : RecyclerView(context, attributeSet, defStyleRes) {
    companion object {
        private const val ORIENTATION_IDLE = 0
        private const val ORIENTATION_HORIZONTAL = 1
        private const val ORIENTATION_VERTICAL = 2
    }

    var orientation = ORIENTATION_IDLE
    private var isTouching = false

    init {
        setScrollingTouchSlop(TOUCH_SLOP_PAGING)
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == SCROLL_STATE_IDLE) {
                    orientation = ORIENTATION_IDLE
                }
            }
        })
    }

    override fun scrollStep(dx: Int, dy: Int, consumed: IntArray?) {

        if (orientation == ORIENTATION_HORIZONTAL) {
            super.scrollStep(dx, 0, consumed)
            consumed?.set(1, dy)
        } else if (orientation == ORIENTATION_VERTICAL) {
            super.scrollStep(0, dy, consumed)
            consumed?.set(0, dx)
        } else {
            if (abs(dx) > abs(dy)) {
                orientation = ORIENTATION_HORIZONTAL
                super.scrollStep(dx, 0, consumed)
                consumed?.set(1, dy)
            } else {
                orientation = ORIENTATION_VERTICAL
                super.scrollStep(0, dy, consumed)
                consumed?.set(0, dx)
            }
        }
    }

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?,
        offsetInWindow: IntArray?, type: Int
    ): Boolean {
        var dxUnconsumed = dx
        var dyUnConsumed = dy
        if (orientation != ORIENTATION_VERTICAL) {
            dyUnConsumed = 0
        }
        if (orientation != ORIENTATION_HORIZONTAL) {
            dxUnconsumed = 0
        }

        return super.dispatchNestedPreScroll(dxUnconsumed, dyUnConsumed, consumed, offsetInWindow, type)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                isTouching = false
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
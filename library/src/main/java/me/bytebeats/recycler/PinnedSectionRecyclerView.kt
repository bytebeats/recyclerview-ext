package me.bytebeats.recycler

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.absoluteValue

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created on 2021/7/22 21:23
 * @Version 1.0
 * @Description TO-DO
 */

class PinnedSectionRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {
    private var pinnedSection: PinnedSection? = null
    private var recycledSection: PinnedSection? = null

    private var translateY: Int = 0
    private var shadowDrawable: GradientDrawable? = null
    private var sectionDistanceY = 0
    private var shadowHeight = 0

    private val touchRect = Rect()
    private val touchPoint = PointF()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var touchTarget: View? = null
    private var downEvent: MotionEvent? = null

    private val rect = Rect()
    private var touchTargetItem: View? = null

    private val gestureDetector by lazy {
        GestureDetector(context.applicationContext, object : GestureDetector.OnGestureListener {
            override fun onDown(e: MotionEvent?): Boolean = true

            override fun onShowPress(e: MotionEvent?) {
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val child = findChildViewUnder(e.x, e.y)
                if (child != null) {
                    val position = getChildLayoutPosition(child)
                    onPinnedItemClickListener?.onClick(child, position, e.x)
                }
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean =
                true

            override fun onLongPress(e: MotionEvent) {
                findChildViewUnder(e.x, e.y)?.apply {
                    onPinnedItemClickListener?.onLongClick(this, getChildLayoutPosition(this))
                }
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean = true

        })
    }


    private val onScrollListener = object : OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            checkOnScroll()
            if (dy.absoluteValue >= 5.0) {
                onPinnedItemClickListener?.onCancel()
            }
        }
    }
    var onPinnedItemClickListener: OnPinnedItemClickListener? = null

    var shadowVisible: Boolean = false
        set(value) {
            field = value
            if (field) {
                if (shadowDrawable == null) {
                    shadowDrawable = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(Color.parseColor("#50000000"), Color.parseColor("#00000000"))
                    )
                }
                shadowHeight = (8.0 * resources.displayMetrics.density).toInt()
            } else {
                shadowDrawable = null
                shadowHeight = 0
            }
            pinnedSection?.apply {
                val v = holder.itemView
                invalidate(v.left, v.top, v.right, v.bottom + shadowHeight)
            }
        }

    init {
        addOnScrollListener(onScrollListener)
    }

    private fun isTopReachedOrBeyond(): Boolean {
        return if (layoutManager == null) false
        else if ((layoutManager is LinearLayoutManager)) {
            (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0
        } else {
            false
        }
    }

    private fun checkOnScroll() {
        if (adapter is PinedSectionAdapter) {
            updatePinnedSection(findFirstVisibleItemPosition(), findFirstCompletelyVisibleItemPosition())
        } else {
            throw IllegalStateException(ILLEGAL_STATE_MESSAGE)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        pinnedSection?.apply {
            canvas.save()

            val view = holder.itemView
            val clipHeight =
                view.height + if (shadowDrawable == null) 0 else shadowHeight.coerceAtMost(sectionDistanceY)
            canvas.clipRect(0, 0, view.width, clipHeight)
            canvas.translate(paddingLeft.toFloat(), (paddingTop + translateY).toFloat())
            drawChild(canvas, view, drawingTime)

            if (sectionDistanceY > 0) {
                shadowDrawable?.setBounds(view.left, view.bottom, view.right, view.bottom + shadowHeight)
                shadowDrawable?.draw(canvas)
            }

            canvas.restore()
        }
    }

    fun updatePinnedSectionData() {
        if (pinnedSection == null || adapter == null) {
            return
        }
        pinnedSection?.apply {
            adapter?.bindViewHolder(holder, position)
        }
    }

    private fun createPinnedSection(position: Int) {
        removePinnedSection()
        recycledSection = null
        sectionDistanceY = 0
        translateY = 0

        adapter?.apply {
            val holder = createViewHolder(this@PinnedSectionRecyclerView, getItemViewType(position))
            bindViewHolder(holder, position)
            var layoutParams = holder.itemView.layoutParams
            if (layoutParams == null) {
                layoutParams = generateDefaultLayoutParams()
                holder.itemView.layoutParams = layoutParams
            }
            val widthMeasureSpec = MeasureSpec.makeMeasureSpec(width - paddingLeft - paddingRight, MeasureSpec.EXACTLY)
            val heightMeasureSpec =
                MeasureSpec.makeMeasureSpec(height - paddingTop - paddingBottom, MeasureSpec.AT_MOST)
            holder.itemView.measure(widthMeasureSpec, heightMeasureSpec)
            holder.itemView.layout(0, 0, holder.itemView.measuredWidth, holder.itemView.measuredHeight)
            val pinnedShadow = PinnedSection(holder, position)
            pinnedSection = pinnedShadow
            updatePinnedSectionLocation()
        }
    }

    private fun updatePinnedSection(firstVisibleItemPosition: Int, firstCompletelyVisibleItemPosition: Int) {
        val pinnedSectionPosition = findPinnedSectionPosition(firstVisibleItemPosition)
        if (pinnedSectionPosition == NO_PINNED) {
            removePinnedSection()
            return
        }
        if (firstCompletelyVisibleItemPosition == pinnedSection?.position) {
            removePinnedSection()
        }
        if (pinnedSectionPosition == pinnedSection?.position) {
            updatePinnedSectionLocation()
        } else {
            createPinnedSection(pinnedSectionPosition)
        }
    }

    private fun findPinnedSectionPosition(position: Int): Int {
        val pinnedSectionAdapter = adapter as PinedSectionAdapter
        if (position in 0 until (adapter?.itemCount ?: 0)) {
            for (i in position downTo 0) {
                if (pinnedSectionAdapter.isPinnedSectionItem(i)) {
                    return i
                }
            }
        }
        return NO_PINNED
    }

    private fun updatePinnedSectionLocation() {
        val nextPinnedSectionPosition = findNextPinnedSectionPositionByPinnedSection()
        val linearLayoutManager = layoutManager as LinearLayoutManager
        val nextPinnedView = linearLayoutManager.findViewByPosition(nextPinnedSectionPosition)
        if (nextPinnedView == null) {
            val firstCompletelyVisibleItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
            val firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition()
            sectionDistanceY = if (firstVisibleItemPosition == firstCompletelyVisibleItemPosition) 0 else shadowHeight
            return
        }
        val currentBottom = pinnedSection?.holder?.itemView?.bottom ?: 0
        val nextTop = nextPinnedView.top
        sectionDistanceY = nextTop - currentBottom
        translateY = if (sectionDistanceY < 0) {
            sectionDistanceY
        } else {
            0
        }
    }

    private fun findNextPinnedSectionPositionByPinnedSection(): Int {
        val pinnedSectionAdapter = adapter as PinedSectionAdapter
        for (i in pinnedSection!!.position + 1 until adapter!!.itemCount) {
            if (pinnedSectionAdapter.isPinnedSectionItem(i)) {
                return i
            }
        }
        return NO_PINNED
    }

    private fun removePinnedSection() {
        sectionDistanceY = 0
        recycledSection = pinnedSection
        pinnedSection = null
    }

    fun updatePinnedSection() {
        pinnedSection?.apply {
            createPinnedSection(position)
        }
    }

    fun collapsePinnedSection(position: Int) {
        if (position == pinnedSection?.position) {
            removePinnedSection()
            smoothScrollToPosition(position)
        }
    }

    private fun findFirstVisibleItemPosition(): Int {
        return if (layoutManager is LinearLayoutManager) (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() else 0
    }

    private fun findFirstCompletelyVisibleItemPosition(): Int {
        return if (layoutManager is LinearLayoutManager) (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() else 0
    }

    private fun clearTouchTarget() {
        touchTarget = null
        if (downEvent != null) {
            downEvent?.recycle()
            downEvent = null
        }
    }

    private fun isViewTouched(view: View?, x: Float, y: Float): Boolean {
        if (view == null) return false
        view.getHitRect(touchRect)
        touchRect.top += translateY
        touchRect.bottom += translateY + paddingTop
        touchRect.left += paddingLeft
        touchRect.right -= paddingRight
        return touchRect.contains(x.toInt(), y.toInt())
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y
        val action = ev.action
        if (action == MotionEvent.ACTION_DOWN && touchTarget == null && pinnedSection != null
            && isViewTouched(pinnedSection!!.holder.itemView, x, y)) {
            touchTarget = pinnedSection!!.holder.itemView
            touchPoint.x = x
            touchPoint.y = y
            downEvent = MotionEvent.obtain(ev)
        }
        if (touchTarget != null) {
            if (isViewTouched(touchTarget, x, y)) {
                touchTarget!!.dispatchTouchEvent(ev)
            }
            if (action == MotionEvent.ACTION_UP) {
                clearTouchTarget()
                super.dispatchTouchEvent(ev)
            } else if (action == MotionEvent.ACTION_CANCEL) {
                clearTouchTarget()
            } else if (action == MotionEvent.ACTION_MOVE) {
                if ((y - touchPoint.y).absoluteValue > touchSlop) {
                    val event = MotionEvent.obtain(ev)
                    event.action = MotionEvent.ACTION_CANCEL
                    touchTarget!!.dispatchTouchEvent(ev)
                    event.recycle()

                    super.dispatchTouchEvent(downEvent)
                    super.dispatchTouchEvent(ev)
                    clearTouchTarget()
                }
            }
            return true
        } else {
            gestureDetector.onTouchEvent(ev)
        }

        val viewUnderTouch = findViewUnderTouchEvent(ev)
        if (viewUnderTouch != null) {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchTargetItem = viewUnderTouch
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    touchTargetItem = null
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun findViewUnderTouchEvent(ev: MotionEvent): View? {
        if (touchTargetItem != null) {
            touchTargetItem!!.getGlobalVisibleRect(rect)
            if (rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                return touchTargetItem
            }
        }
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.getGlobalVisibleRect(rect)
            if (rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                return child
            }
        }
        return null
    }

    override fun setAdapter(adapter: Adapter<ViewHolder>?) {
        if (adapter != null) {
            if (adapter !is PinedSectionAdapter) {
                throw IllegalStateException(ILLEGAL_STATE_MESSAGE)
            }
        }

        if (getAdapter() != adapter) removePinnedSection()

        super.setAdapter(adapter)
    }

    internal class PinnedSection(val holder: ViewHolder, val position: Int)

    interface PinedSectionAdapter {
        fun isPinnedSectionItem(position: Int): Boolean
    }

    interface OnPinnedItemClickListener {
        fun onClick(pinnedView: View, position: Int, eventX: Float)
        fun onLongClick(pinnedView: View, position: Int)
        fun onCancel()
    }

    companion object {
        private const val NO_PINNED = -1
        private const val ILLEGAL_STATE_MESSAGE =
            "Does your Adapter implement PinnedSectionRecyclerView.PinedSectionAdapter?"
    }
}
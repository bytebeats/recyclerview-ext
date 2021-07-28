package me.bytebeats.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.customview.widget.ViewDragHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created on 2021/7/28 20:00
 * @Version 1.0
 * @Description TO-DO
 */

abstract class PartialItemScrollAdapter(val recyclerView: RecyclerView) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        ViewDragHelper.create(recyclerView, object : ViewDragHelper.Callback() {
            override fun tryCaptureView(child: View, pointerId: Int): Boolean = false
        })
    }

    private var scrollDistance = (recyclerView.context.resources.displayMetrics.density * item_scroll_distance).toInt()

    private val mItemScrollFrameLayouts = mutableSetOf<WeakReference<ScrollFrameLayout>>()

    private var offsetX = 0
    private var offsetY = 0
    var headerWidth = 0

    private val mOnPositionChangeListener = object : ScrollFrameLayout.OnPositionChangeListener {
        override fun getOffsetX(): Int = offsetX

        override fun getOffsetY(): Int = offsetY

        override fun onPositionChanged(layout: ScrollFrameLayout, view: View, left: Int, top: Int, dx: Int, dy: Int) {
            offsetX = left
            offsetY = top
            tryToScroll(layout, view, left, top, dx, dy)
        }
    }

    private fun tryToScroll(
        layout: ScrollFrameLayout,
        changedView: View,
        left: Int,
        top: Int,
        dx: Int,
        dy: Int
    ) {
        if (recyclerView.layoutManager != null && recyclerView.layoutManager is LinearLayoutManager) {
            if ((recyclerView.layoutManager as LinearLayoutManager).orientation == LinearLayoutManager.VERTICAL && mItemScrollFrameLayouts.all {
                    it.get()?.isHorizontalScroll() == true
                } || (recyclerView.layoutManager as LinearLayoutManager).orientation == LinearLayoutManager.HORIZONTAL && mItemScrollFrameLayouts.all {
                    it.get()?.isHorizontalScroll() == false
                }) {
                mItemScrollFrameLayouts.forEach {
                    it.get()?.scrollItem(dx, dy)
                }
                recyclerView.invalidate()
                onPositionChanged(layout, changedView, left, top, dx, dy)
            }
        } else {
            throw IllegalStateException("RecyclerView and its item ScrollFrameLayouts has the same scroll axis!")
        }
    }

    abstract fun initHeaderView(viewType: Int, header: ViewGroup, layout: ScrollFrameLayout)
    abstract fun initContentView(viewType: Int, header: ViewGroup, layout: ScrollFrameLayout)
    abstract fun updateHeaderView(position: Int, headerVH: ViewHolder)
    abstract fun updateContentView(position: Int, contentVH: ViewHolder)

    protected open fun onPositionChanged(
        layout: ScrollFrameLayout,
        changedView: View,
        left: Int,
        top: Int,
        dx: Int,
        dy: Int
    ) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_draggable, parent, false)
        val holder = object : ViewHolder(itemView) {
            override fun getScrollLayoutId(): Int = R.id.scroll_frame_layout
            override fun getHeaderId(): Int = R.id.draggable_header
            override fun getContentId(): Int = R.id.draggable_content
        }
//        itemView.tag = holder
        holder.scrollFrameLayout.let {
            it.mOnPositionChangeListener = this.mOnPositionChangeListener
            it.mContentOffsetX = headerWidth
            it.mScrollableViewId = R.id.draggable_content
        }

        initHeaderView(viewType, holder.header, holder.scrollFrameLayout)
        initContentView(viewType, holder.content, holder.scrollFrameLayout)

        mItemScrollFrameLayouts.add(WeakReference(holder.scrollFrameLayout))
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        updateHeaderView(position, holder as ViewHolder)
        updateContentView(position, holder as ViewHolder)
    }

    protected fun scrollToLeft(dx: Int) {
        scrollDistance = dx
        offsetX = dx
        recyclerView.invalidate()
    }

    protected fun scrollToRight() {
        scrollToLeft(-scrollDistance)
    }

    private fun scrollItemsBy(dx: Int) {
        mItemScrollFrameLayouts.forEach {
            it.get()?.scrollItemBy(dx, 0, object : ScrollFrameLayout.OnPositionChangeListener {
                override fun getOffsetX(): Int = offsetX
                override fun getOffsetY(): Int = offsetY
                override fun onPositionChanged(
                    layout: ScrollFrameLayout,
                    view: View,
                    left: Int,
                    top: Int,
                    dx: Int,
                    dy: Int
                ) {
                    offsetX = left
                    offsetY = top
                    tryToScroll(layout, view, left, top, dx, dy)
                }
            })
        }
    }

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val scrollFrameLayout by lazyFind<ScrollFrameLayout>(getScrollLayoutId())
        val header by lazyFind<ViewGroup>(getHeaderId())
        val content by lazyFind<ViewGroup>(getContentId())

        @IdRes
        abstract fun getScrollLayoutId(): Int

        @IdRes
        abstract fun getHeaderId(): Int

        @IdRes
        abstract fun getContentId(): Int
    }


    companion object {
        private const val item_scroll_distance = 100

        private fun <T : View> RecyclerView.ViewHolder.lazyFind(@IdRes idRes: Int): Lazy<T> =
            lazy(LazyThreadSafetyMode.NONE) {
                itemView.findViewById<T>(idRes)
            }
    }
}
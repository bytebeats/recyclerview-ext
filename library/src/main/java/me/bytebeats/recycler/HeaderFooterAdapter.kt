package me.bytebeats.recycler

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created on 2021/7/22 21:25
 * @Version 1.0
 * @Description Adapter supported headers and footers
 */

class HeaderFooterAdapter(
    val wrappedAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
    val headers: MutableList<View> = mutableListOf(),
    val footers: MutableList<View> = mutableListOf(),
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        wrappedAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                notifyDataSetChanged()
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                notifyItemRangeChanged(getHeaderCount() + positionStart, itemCount)
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                super.onItemRangeChanged(positionStart, itemCount, payload)
                notifyItemRangeChanged(getHeaderCount() + positionStart, itemCount, payload)
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                notifyItemRangeInserted(getHeaderCount() + positionStart, itemCount)
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                notifyItemRangeRemoved(getHeaderCount() + positionStart, itemCount)
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                notifyItemMoved(getHeaderCount() + fromPosition, getHeaderCount() + toPosition)
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType in VIEW_TYPE_HEADER until 0) {
            if (viewType in VIEW_TYPE_HEADER until VIEW_TYPE_FOOTER) {
                return SimpleViewHolder(headers[viewType - VIEW_TYPE_HEADER])
            } else {
                return SimpleViewHolder(footers[viewType - VIEW_TYPE_FOOTER])
            }
        } else {
            val wrappedViewHolder = wrappedAdapter.onCreateViewHolder(parent, viewType)
            if (wrappedViewHolder.itemView.parent != null) {
                throw IllegalStateException("ViewHolder parent should be null: ViewHolder=${wrappedViewHolder.javaClass.simpleName}, wrappedAdapter=${wrappedAdapter.javaClass.simpleName},  viewType=${viewType}")
            }
            return wrappedViewHolder
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position in getHeaderCount() until getHeaderCount() + getWrappedItemCount()) {
            wrappedAdapter.onBindViewHolder(holder, position)
        }
    }

    override fun getItemCount(): Int {
        return getHeaderCount() + wrappedAdapter.itemCount + getFooterCount()
    }

    override fun getItemViewType(position: Int): Int {
        if (hasHeaders() && position < getHeaderCount()) {
            return VIEW_TYPE_HEADER + position
        } else if (hasFooters() && position >= getHeaderCount() + getWrappedItemCount()) {
            return VIEW_TYPE_FOOTER + position - getHeaderCount() - getWrappedItemCount()
        } else {
            return wrappedAdapter.getItemViewType(position - getHeaderCount())
        }
    }

    private fun getWrappedItemCount(): Int = wrappedAdapter.itemCount

    private fun hasHeaders(): Boolean = headers.isEmpty()
    private fun hasFooters(): Boolean = footers.isEmpty()

    fun getHeaderCount(): Int = headers.size
    fun getFooterCount(): Int = footers.size

    fun addHeader(header: View) {
        headers.add(header)
        notifyItemChanged(headers.lastIndex)
    }

    fun addHeaders(headers: Collection<View>) {
        this.headers.addAll(headers)
        notifyDataSetChanged()
    }

    fun insertHeader(position: Int, header: View) {
        headers.add(position, header)
        notifyItemInserted(position)
    }

    fun removeHeader(header: View): Boolean {
        val idx = headers.indexOf(header)
        if (idx > -1) {
            val result = headers.remove(header)
            if (result) {
                notifyItemRemoved(idx)
            }
            return result
        }
        return false
    }

    fun addFooter(footer: View) {
        footers.add(footer)
        notifyItemChanged(footers.lastIndex + getHeaderCount() + wrappedAdapter.itemCount)
    }

    fun addFooters(footers: Collection<View>) {
        this.footers.addAll(footers)
        notifyDataSetChanged()
    }

    fun removeFooter(footer: View): Boolean {
        val idx = footers.indexOf(footer)
        if (idx > -1) {
            val result = footers.remove(footer)
            if (result) {
                notifyItemRemoved(idx + getHeaderCount() + wrappedAdapter.itemCount)
            }
            return result
        }
        return false
    }

    companion object {
        /**
         * 100 view types for headers and footers respectively is ENOUGH!
         */
        private const val VIEW_TYPE_HEADER = -200
        private const val VIEW_TYPE_FOOTER = -100
    }
}
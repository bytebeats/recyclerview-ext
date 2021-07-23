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
    val headers: List<View>? = null,
    val footers: List<View>? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mHeaders = mutableListOf<View>()
    private val mFooters = mutableListOf<View>()

    init {
        headers?.toCollection(mHeaders)
        footers?.toCollection(mFooters)
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
                return SimpleViewHolder(mHeaders[viewType - VIEW_TYPE_HEADER])
            } else {
                return SimpleViewHolder(mFooters[viewType - VIEW_TYPE_FOOTER])
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

    private fun hasHeaders(): Boolean = mHeaders.isEmpty()
    private fun hasFooters(): Boolean = mFooters.isEmpty()

    fun getHeaderCount(): Int = mHeaders.size
    fun getFooterCount(): Int = mFooters.size

    fun addHeader(header: View) {
        mHeaders.add(header)
        notifyItemChanged(mHeaders.lastIndex)
    }

    fun addHeaders(headers: Collection<View>) {
        this.mHeaders.addAll(headers)
        notifyDataSetChanged()
    }

    fun insertHeader(position: Int, header: View) {
        mHeaders.add(position, header)
        notifyItemInserted(position)
    }

    fun removeHeader(header: View): Boolean {
        val idx = mHeaders.indexOf(header)
        if (idx > -1) {
            val result = mHeaders.remove(header)
            if (result) {
                notifyItemRemoved(idx)
            }
            return result
        }
        return false
    }

    fun addFooter(footer: View) {
        mFooters.add(footer)
        notifyItemChanged(mFooters.lastIndex + getHeaderCount() + wrappedAdapter.itemCount)
    }

    fun addFooter(position: Int, footer: View) {
        mFooters.add(position, footer)
        notifyItemChanged(position + getHeaderCount() + wrappedAdapter.itemCount)
    }

    fun addFooters(footers: Collection<View>) {
        this.mFooters.addAll(footers)
        notifyDataSetChanged()
    }

    fun removeFooter(footer: View): Boolean {
        val idx = mFooters.indexOf(footer)
        if (idx > -1) {
            val result = mFooters.remove(footer)
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
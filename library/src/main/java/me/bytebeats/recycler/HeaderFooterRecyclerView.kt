package me.bytebeats.recycler

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created on 2021/7/23 11:24
 * @Version 1.0
 * @Description RecyclerView can add headers and footers
 */

class HeaderFooterRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {
    private var headers = mutableListOf<View>()
    private var footers = mutableListOf<View>()

    private var wrappedAdapter: HeaderFooterAdapter? = null

    override fun getAdapter(): HeaderFooterAdapter? {
        return wrappedAdapter
    }

    override fun setAdapter(adapter: Adapter<ViewHolder>?) {
        if (adapter == null) {
            super.setAdapter(adapter)
            return
        }
        wrappedAdapter = if (adapter is HeaderFooterAdapter) {
            adapter
        } else {
            HeaderFooterAdapter(adapter, headers, footers)
        }
        headers.clear()
        footers.clear()
        super.setAdapter(wrappedAdapter)
    }

    fun addHeader(header: View) {
        if (wrappedAdapter != null) {
            wrappedAdapter!!.addHeader(header)
        } else {
            headers.add(header)
        }
    }

    fun addHeader(position: Int, header: View) {
        if (wrappedAdapter != null) {
            wrappedAdapter!!.insertHeader(position, header)
        } else {
            headers.add(position, header)
        }
    }

    fun addHeaders(headers: Collection<View>) {
        if (wrappedAdapter != null) {
            wrappedAdapter!!.addHeaders(headers)
        } else {
            this.headers.addAll(headers)
        }
    }

    fun removeHeader(header: View): Boolean {
        return if (wrappedAdapter != null) {
            wrappedAdapter!!.removeHeader(header)
        } else {
            headers.remove(header)
        }
    }

    fun addFooter(footer: View) {
        if (wrappedAdapter != null) {
            wrappedAdapter!!.addFooter(footer)
        } else {
            footers.add(footer)
        }
    }

    fun addFooter(position: Int, footer: View) {
        if (wrappedAdapter != null) {
            wrappedAdapter!!.addFooter(position, footer)
        } else {
            footers.add(position, footer)
        }
    }

    fun addFooters(footers: Collection<View>) {
        if (wrappedAdapter != null) {
            wrappedAdapter!!.addFooters(footers)
        } else {
            this.footers.addAll(footers)
        }
    }

    fun removeFooter(footer: View): Boolean {
        return if (wrappedAdapter != null) {
            wrappedAdapter!!.removeFooter(footer)
        } else {
            this.footers.remove(footer)
        }
    }
}
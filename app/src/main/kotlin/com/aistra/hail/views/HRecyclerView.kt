package com.aistra.hail.views

import android.content.Context
import android.util.AttributeSet
import android.view.ContextMenu.ContextMenuInfo
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Customized RecyclerView with ContextMenu support.
 * */
class HRecyclerView : RecyclerView {
    private var contextMenuInfo: RecyclerViewContextMenuInfo? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun getContextMenuInfo(): ContextMenuInfo? {
        return contextMenuInfo
    }

    override fun showContextMenuForChild(originalView: View?): Boolean {
        if (originalView == null || adapter == null) return false

        val longPressPosition = getChildAdapterPosition(originalView)
        if (longPressPosition >= 0) {
            contextMenuInfo = RecyclerViewContextMenuInfo(longPressPosition, getChildViewHolder(originalView))
            return super.showContextMenuForChild(originalView)
        }
        return false
    }

    class RecyclerViewContextMenuInfo(val position: Int, val viewHolder: ViewHolder) : ContextMenuInfo
}
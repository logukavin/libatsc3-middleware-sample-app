package org.ngbp.jsonrpc4jtestharness.useragent

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.progress_view.view.*
import org.ngbp.jsonrpc4jtestharness.R

class ProgressView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.progress_view, this)
        progress_bar.visibility = View.GONE
    }


    fun show() {
        progress_bar.visibility = View.VISIBLE
    }

    fun hide() {
        progress_bar.visibility = View.GONE
    }
}
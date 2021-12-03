package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.philips.jhdr.SlhdrPlayerView

class Atsc3SlhdrPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SlhdrPlayerView(context, attrs), Atsc3PlayerView {

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isInEditMode) return

    }

    companion object {
        val TAG: String = Atsc3SlhdrPlayerView::class.java.simpleName

        fun inflate(context: Context, root: ViewGroup): Atsc3SlhdrPlayerView {
            return LayoutInflater.from(context).inflate(R.layout.atsc3_slhdr_player_view, root, false) as Atsc3SlhdrPlayerView
        }
    }
}
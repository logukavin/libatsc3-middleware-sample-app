package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.nextgenbroadcast.mobile.mmt.atsc3.media.MMTDataSource
import kotlinx.android.synthetic.embedded.receiver_player_layout.view.*

class ReceiverPlayerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseReceiverPlayerView(context, attrs, defStyleAttr) {

    override fun isPlaying(): Boolean {
        return super.isPlaying() || mmt_player_view.isPlaying
    }

    override fun startPlayback(mmtSource: Any) {
        super.startPlayback(Any())

        mmt_player_view.visibility = View.VISIBLE
        (mmtSource as? MMTDataSource)?.let { source ->
            mmt_player_view.start(source)
        }
    }

    override fun stopPlayback() {
        super.stopPlayback()

        mmt_player_view.stop()
    }
}
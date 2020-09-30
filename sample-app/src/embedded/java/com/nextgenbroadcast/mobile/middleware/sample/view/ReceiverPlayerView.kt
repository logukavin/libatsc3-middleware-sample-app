package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.util.AttributeSet
import com.nextgenbroadcast.mobile.mmt.atsc3.media.MMTDataBuffer
import kotlinx.android.synthetic.main.receiver_player_layout.view.*

class ReceiverPlayerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseReceiverPlayerView(context, attrs, defStyleAttr) {

    override fun startPlayback(mmtSource: Any) {
        super.startPlayback(Any())

        (mmtSource as? MMTDataBuffer)?.let { mmtBuffer ->
            receiver_media_player.play(mmtBuffer)
        }
    }
}
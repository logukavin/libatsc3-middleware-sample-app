package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.util.AttributeSet
import com.nextgenbroadcast.mobile.permission.UriPermissionProvider
import kotlinx.android.synthetic.main.receiver_player_layout.view.*

class ReceiverPlayerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseReceiverPlayerView(context, attrs, defStyleAttr) {

    fun setUriPermissionProvider(uriPermissionProvider: UriPermissionProvider?) {
        receiver_media_player.setUriPermissionProvider(uriPermissionProvider)
    }

}
package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.player.Atsc3MediaPlayer
import com.philips.jhdr.ISlhdrOperatingModeNtf
import com.philips.jhdr.SlhdrPlayerView
import com.philips.jhdr.SlhdrRenderersFactory

class Atsc3SlhdrPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SlhdrPlayerView(context, attrs), Atsc3PlayerView, ISlhdrOperatingModeNtf {

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isInEditMode) return

        setSlhdrModeNtf(this)
    }

    override fun play(atsc3Player: Atsc3MediaPlayer, mediaUri: Uri, mimeType: String?) {
        val renderersFactory = SlhdrRenderersFactory(context, getRendererConnectNtf())
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        atsc3Player.play(renderersFactory, mediaUri, mimeType)
    }

    override fun OnProcessingModeChanged(SlhdrMode: Int, OutputMode: Int, SplitScreenMode: Int) {
        val processingSlhdr = (SlhdrMode > ISlhdrOperatingModeNtf.Processing_Mode_None)
        LOG.d(TAG, "OnProcessingModeChanged - SlhdrMode: $SlhdrMode, OutputMode: $OutputMode, SplitScreenMode: $SplitScreenMode -> processingSlhdr: $processingSlhdr")
    }

    override fun OnDaChanged(onOff: Boolean, level: Int) {
        LOG.d(TAG, "OnDaChanged - onOff: $onOff, level: $level")
    }

    companion object {
        val TAG: String = Atsc3SlhdrPlayerView::class.java.simpleName

        fun inflate(context: Context, root: ViewGroup): Atsc3SlhdrPlayerView {
            return LayoutInflater.from(context).inflate(R.layout.atsc3_slhdr_player_view, root, false) as Atsc3SlhdrPlayerView
        }
    }
}
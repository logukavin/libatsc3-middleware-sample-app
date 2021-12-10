package com.nextgenbroadcast.mobile.middleware.sample.exoplayer

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.video.VideoRendererEventListener
import com.nextgenbroadcast.mobile.player.MMTConstants
import com.philips.jhdr.ISlhdrMediaCodecConstructed

class SlhdrAtsc3RenderersFactory(
    context: Context,
    slhdrMediaCodecConstructed: ISlhdrMediaCodecConstructed? = null,
    mimeType: String? = null
) : RenderersFactory {
    private val factory = if (mimeType == MMTConstants.MIME_MMT_VIDEO || mimeType == MMTConstants.MIME_MMT_AUDIO) {
        SlhdrMMTRenderersFactory(context, slhdrMediaCodecConstructed)
    } else {
        SlhdrRouteDASHRenderersFactory(context, slhdrMediaCodecConstructed)
    }

    override fun createRenderers(eventHandler: Handler, videoRendererEventListener: VideoRendererEventListener, audioRendererEventListener: AudioRendererEventListener, textRendererOutput: TextOutput, metadataRendererOutput: MetadataOutput, drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?): Array<Renderer> {
        return factory.createRenderers(eventHandler, videoRendererEventListener, audioRendererEventListener, textRendererOutput, metadataRendererOutput, drmSessionManager)
    }
}
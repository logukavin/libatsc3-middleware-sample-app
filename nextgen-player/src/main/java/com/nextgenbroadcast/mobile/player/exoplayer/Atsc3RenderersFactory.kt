package com.nextgenbroadcast.mobile.player.exoplayer

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.video.VideoRendererEventListener
import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTRenderersFactory
import com.nextgenbroadcast.mobile.player.MMTConstants

class Atsc3RenderersFactory(
    context: Context,
    mimeType: String? = null
) : RenderersFactory {
    val factory = if (mimeType == MMTConstants.MIME_MMT_VIDEO || mimeType == MMTConstants.MIME_MMT_AUDIO) {
        MMTRenderersFactory(context)
    } else {
        //jjustman-2021-09-08 - TODO - refactor DefaultRenderersFactory out into RouteDASHRenderersFactory for AC-4 and MPEGH support
        DefaultRenderersFactory(context)
    }

    override fun createRenderers(eventHandler: Handler, videoRendererEventListener: VideoRendererEventListener, audioRendererEventListener: AudioRendererEventListener, textRendererOutput: TextOutput, metadataRendererOutput: MetadataOutput, drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?): Array<Renderer> {
        return factory.createRenderers(eventHandler, videoRendererEventListener, audioRendererEventListener, textRendererOutput, metadataRendererOutput, drmSessionManager)
    }
}
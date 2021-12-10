package com.nextgenbroadcast.mobile.middleware.sample.exoplayer;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.nextgenbroadcast.mobile.player.exoplayer.RouteDASHRenderersFactory;
import com.philips.jhdr.ISlhdrMediaCodecConstructed;
import com.philips.jhdr.SlhdrMediaCodecVideoRenderer;

import java.util.ArrayList;

// based on SlhdrRenderersFactory
public class SlhdrRouteDASHRenderersFactory extends RouteDASHRenderersFactory {
    private final Context context;
    @Nullable
    private DrmSessionManager<FrameworkMediaCrypto> drmSessionManager;
    private int extensionRendererMode;
    private long allowedVideoJoiningTimeMs;
    private boolean playClearSamplesWithoutKeys;
    private MediaCodecSelector mediaCodecSelector;

    private ISlhdrMediaCodecConstructed slhdrMediaCodecConstructed;

    public SlhdrRouteDASHRenderersFactory(Context context, @Nullable ISlhdrMediaCodecConstructed slhdrMediaCodecConstructed) {
        super(context);

        this.context = context;
        this.extensionRendererMode = EXTENSION_RENDERER_MODE_ON; //jjustman-2020-08-25 - enable this for DAA?
        this.allowedVideoJoiningTimeMs = DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS;
        this.mediaCodecSelector = MediaCodecSelector.DEFAULT;
        this.slhdrMediaCodecConstructed = slhdrMediaCodecConstructed;
    }

    @Override
    public DefaultRenderersFactory setExtensionRendererMode(int extensionRendererMode) {
        this.extensionRendererMode = extensionRendererMode;
        return super.setExtensionRendererMode(extensionRendererMode);
    }

    @Override
    public DefaultRenderersFactory setPlayClearSamplesWithoutKeys(boolean playClearSamplesWithoutKeys) {
        this.playClearSamplesWithoutKeys = playClearSamplesWithoutKeys;
        return super.setPlayClearSamplesWithoutKeys(playClearSamplesWithoutKeys);
    }

    @Override
    public DefaultRenderersFactory setMediaCodecSelector(MediaCodecSelector mediaCodecSelector) {
        this.mediaCodecSelector = mediaCodecSelector;
        return super.setMediaCodecSelector(mediaCodecSelector);
    }

    @Override
    public DefaultRenderersFactory setAllowedVideoJoiningTimeMs(long allowedVideoJoiningTimeMs) {
        this.allowedVideoJoiningTimeMs = allowedVideoJoiningTimeMs;
        return super.setAllowedVideoJoiningTimeMs(allowedVideoJoiningTimeMs);
    }

    public Renderer[] createRenderers(Handler eventHandler, VideoRendererEventListener videoRendererEventListener, AudioRendererEventListener audioRendererEventListener, TextOutput textRendererOutput, MetadataOutput metadataRendererOutput, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager) {
        if (drmSessionManager == null) {
            drmSessionManager = this.drmSessionManager;
        }

        ArrayList<Renderer> renderersList = new ArrayList<>();
        buildVideoRenderers(context, mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, videoRendererEventListener, allowedVideoJoiningTimeMs, renderersList);
        buildAudioRenderers(context, extensionRendererMode, mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, false, buildAudioProcessors(), eventHandler, audioRendererEventListener, renderersList);
        buildTextRenderers(context, textRendererOutput, eventHandler.getLooper(), extensionRendererMode, renderersList);
        buildMetadataRenderers(context, metadataRendererOutput, eventHandler.getLooper(), extensionRendererMode, renderersList);
        buildCameraMotionRenderers(context, extensionRendererMode, renderersList);
        buildMiscellaneousRenderers(context, eventHandler, this.extensionRendererMode, renderersList);
        return renderersList.toArray(new Renderer[0]);
    }

    protected void buildVideoRenderers(Context context, MediaCodecSelector mediaCodecSelector, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, VideoRendererEventListener eventListener, long allowedVideoJoiningTimeMs, ArrayList<Renderer> out) {
        out.add(new SlhdrMediaCodecVideoRenderer(context, mediaCodecSelector, allowedVideoJoiningTimeMs, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, 50, slhdrMediaCodecConstructed));
    }

}

package com.nextgenbroadcast.mobile.middleware.sample.openSlhdr;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.nio.ByteBuffer;

public class OpenSlhdrMediaCodecVideoRenderer extends MediaCodecVideoRenderer {
    private OpenSlhdr openSlhdr = new OpenSlhdr();

    public OpenSlhdrMediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector) {
        super(context, mediaCodecSelector);
    }

    public OpenSlhdrMediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs) {
        super(context, mediaCodecSelector, allowedJoiningTimeMs);
    }

    public OpenSlhdrMediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        super(context, mediaCodecSelector, allowedJoiningTimeMs, eventHandler, eventListener, maxDroppedFramesToNotify);
    }

    public OpenSlhdrMediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, boolean enableDecoderFallback, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        super(context, mediaCodecSelector, allowedJoiningTimeMs, enableDecoderFallback, eventHandler, eventListener, maxDroppedFramesToNotify);
    }

    @Override
    protected void onQueueInputBuffer(@NonNull DecoderInputBuffer buffer) {
//        ByteBuffer data = buffer.data;
//        if (data != null && data.isDirect()) {
//            //TODO: data.position() + data.arrayOffset() ??
//            openSlhdr.decodeMetadata(data, data.position(), data.limit());
//        }

        super.onQueueInputBuffer(buffer);
    }

    //TODO: maybe use this instead onQueueInputBuffer?
    @Override
    protected void handleInputBufferSupplementalData(@NonNull DecoderInputBuffer buffer) throws ExoPlaybackException {
        super.handleInputBufferSupplementalData(buffer);
    }
}

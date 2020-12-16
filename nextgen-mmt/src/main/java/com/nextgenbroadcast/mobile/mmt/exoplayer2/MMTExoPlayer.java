package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsCollector;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.Util;

public class MMTExoPlayer extends SimpleExoPlayer implements DefaultClockWrapper.HandlerCallback {
    private static final int MSG_DO_SOME_WORK = 2;

    public MMTExoPlayer(Context context) {
        this(context,
                new DefaultRenderersFactory(context),
                new DefaultTrackSelector(),
                new MMTLoadControl(),
                null,
                new DefaultBandwidthMeter.Builder(context).build(),
                new AnalyticsCollector.Factory(),
                new DefaultClockWrapper(),
                Util.getLooper());
    }

    public MMTExoPlayer(Context context, RenderersFactory renderersFactory, TrackSelector trackSelector, LoadControl loadControl, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, BandwidthMeter bandwidthMeter, AnalyticsCollector.Factory analyticsCollectorFactory, DefaultClockWrapper clock, Looper looper) {
        super(context, renderersFactory, trackSelector, loadControl, drmSessionManager, bandwidthMeter, analyticsCollectorFactory, clock, looper);
        clock.setHandlerCallback(this);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == MSG_DO_SOME_WORK) {
            Log.d("!!!", "Do some work");
        }

        return false;
    }
}

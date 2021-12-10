package com.nextgenbroadcast.mobile.player.exoplayer

import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.Allocator
import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTLoadControl
import com.nextgenbroadcast.mobile.player.MMTConstants

class Atsc3LoadControl(
    mimeType: String? = null
) : LoadControl {
    private val loadControl = if (mimeType == MMTConstants.MIME_MMT_VIDEO || mimeType == MMTConstants.MIME_MMT_AUDIO) {
        MMTLoadControl()
    } else {
        RouteDASHLoadControl()
    }

    override fun onPrepared() {
        loadControl.onPrepared()
    }

    override fun onTracksSelected(renderers: Array<out Renderer>, trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
        loadControl.onTracksSelected(renderers, trackGroups, trackSelections)
    }

    override fun onStopped() {
        loadControl.onStopped()
    }

    override fun onReleased() {
        loadControl.onReleased()
    }

    override fun getAllocator(): Allocator {
        return loadControl.allocator
    }

    override fun getBackBufferDurationUs(): Long {
        return loadControl.backBufferDurationUs
    }

    override fun retainBackBufferFromKeyframe(): Boolean {
        return loadControl.retainBackBufferFromKeyframe()
    }

    override fun shouldContinueLoading(bufferedDurationUs: Long, playbackSpeed: Float): Boolean {
        return loadControl.shouldContinueLoading(bufferedDurationUs, playbackSpeed)
    }

    override fun shouldStartPlayback(bufferedDurationUs: Long, playbackSpeed: Float, rebuffering: Boolean): Boolean {
        return loadControl.shouldStartPlayback(bufferedDurationUs, playbackSpeed, rebuffering)
    }
}
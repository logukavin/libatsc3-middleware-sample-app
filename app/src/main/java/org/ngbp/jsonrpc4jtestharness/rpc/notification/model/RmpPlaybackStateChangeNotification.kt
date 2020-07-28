package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType

data class RmpPlaybackStateChangeNotification (
        var msgType: String = NotificationType.RMP_PLAYBACK_STATE_CHANGE.value,
        var playbackState: PlaybackState? = null
) : RPCNotification
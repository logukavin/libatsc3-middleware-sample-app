package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType

data class RmpPlaybackRateChangeNotification(
        var msgType: String = NotificationType.RMP_PLAYBACK_RATE_CHANGE.value,
        var playbackRate: Float? = null
): RPCNotification
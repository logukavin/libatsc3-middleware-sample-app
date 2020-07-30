package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType

data class RmpPlaybackRateChangeNotification(
        var playbackRate: Float? = null
): RPCNotification(NotificationType.RMP_PLAYBACK_RATE_CHANGE)
package org.ngbp.jsonrpc4jtestharness.rpc.asynchronousNotificationsofChanges.model

import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.VideoDescriptionService

data class AudioAccessibilityPreferenceChangeNotification (
    var msgType: String? = null,
    var videoDescriptionService: VideoDescriptionService? = null
)
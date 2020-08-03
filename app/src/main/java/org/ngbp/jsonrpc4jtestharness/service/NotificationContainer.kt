package org.ngbp.jsonrpc4jtestharness.service

import com.nextgenbroadcast.mobile.core.model.PlaybackState

data class NotificationContainer(val title: String, val message: String, val id: Int, val state: PlaybackState)
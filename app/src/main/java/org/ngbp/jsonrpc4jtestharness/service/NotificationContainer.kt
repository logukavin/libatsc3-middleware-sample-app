package org.ngbp.jsonrpc4jtestharness.service

import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState

data class NotificationContainer(val title: String, val message: String, val id: Int, val state: PlaybackState)
package org.ngbp.jsonrpc4jtestharness.controller

import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType

interface IRPCController {
    val language: String
    val queryServiceId: String?
    val mediaUrl: String?
    val playbackState: PlaybackState

    fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double)
    fun updateRMPState(state: PlaybackState)

    fun requestMediaPlay(mediaUrl: String? = null, delay: Long)
    fun requestMediaStop(delay: Long)

    fun subscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType>
    fun unsubscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType>
}
package org.ngbp.jsonrpc4jtestharness.service

import android.app.Notification
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState

class NotificationContainer(val notification: Notification, val id: Int, val state: PlaybackState) {

    override fun equals(other: Any?): Boolean {
        return (this.id == (other as NotificationContainer).id) && (this.state == (other as NotificationContainer).state)
    }

    override fun hashCode(): Int {
        var result = notification.hashCode()
        result = 31 * result + id
        result = 31 * result + state.hashCode()
        return result
    }

    fun copyObject(): NotificationContainer {
        return NotificationContainer(notification, id, state)
    }
}
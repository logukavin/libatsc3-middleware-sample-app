package org.ngbp.jsonrpc4jtestharness.service

import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState

class NotificationContainer(val title: String, val message: String, val id: Int, val state: PlaybackState) {

    override fun equals(other: Any?): Boolean {
        return (this.id == (other as NotificationContainer).id) && (this.state == (other as NotificationContainer).state) &&
                this.title == (other as NotificationContainer).title &&
                this.message == (other as NotificationContainer).message
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + id
        result = 31 * result + state.hashCode()
        return result
    }

}
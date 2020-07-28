package org.ngbp.jsonrpc4jtestharness.service

interface INotificationManager {
    fun addNotification(notification: NotificationContainer)
    fun removePendingNotification()
}
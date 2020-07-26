package org.ngbp.jsonrpc4jtestharness.service

import android.app.Notification

interface INotificationManager {
    fun addNotification(notification: NotificationContainer)
    fun removePendingNotification(id: Int)
    fun removeAllPendingNotifications()
    fun getCountOfPendingNotification(): Int
}
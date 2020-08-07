package com.nextgenbroadcast.mobile.middleware.notification

interface INotificationManager {
    fun addNotification(notification: NotificationContainer)
    fun removePendingNotification()
}
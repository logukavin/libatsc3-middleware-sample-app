package com.nextgenbroadcast.mobile.middleware.sample.service

interface INotificationManager {
    fun addNotification(notification: NotificationContainer)
    fun removePendingNotification()
}
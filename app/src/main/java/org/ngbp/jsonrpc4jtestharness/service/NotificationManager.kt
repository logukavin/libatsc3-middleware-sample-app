package org.ngbp.jsonrpc4jtestharness.service

import android.os.Handler

class NotificationManager(private val notificationHelper: NotificationHelper) : INotificationManager {

    private val ACTION_DELAY = 500L
    private val notificationHandler = Handler()

    @Volatile
    var currentNotification: NotificationContainer? = null

    @Volatile
    var newNotification: NotificationContainer? = null

    private val notificationRunnable = Runnable {
        val localCurrentNotification = currentNotification
        val localNewNotification = newNotification
        if (localNewNotification != null) {
            if (localCurrentNotification == null) {
                currentNotification = localNewNotification
                notificationHelper.notify(localNewNotification.id, notificationHelper.createMediaNotification(localNewNotification))
            } else if (localCurrentNotification != localNewNotification) {
                currentNotification = localNewNotification
                notificationHelper.notify(localNewNotification.id, notificationHelper.createMediaNotification(localNewNotification))
            }
        }
    }


    override fun addNotification(notification: NotificationContainer) {
        newNotification = notification
        showNotification()
    }

    override fun removePendingNotification() {
        notificationHandler.removeCallbacks(notificationRunnable)
    }

    private fun showNotification() {
        notificationHandler.removeCallbacks(notificationRunnable)
        notificationHandler.postDelayed(notificationRunnable, ACTION_DELAY)
    }
}

package org.ngbp.jsonrpc4jtestharness.service

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.*

class NotificationManager(val context: Context) : INotificationManager {
    private var notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)
    private val ACTION_DELAY = 500L
    private var notificationMap = ConcurrentHashMap<Int, Notification>()
    private val service: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var pendingNotificationScheduler: ScheduledFuture<*>? = null

    @Volatile
    var currentNotification: NotificationContainer? = null
    @Volatile
    var newNotification: NotificationContainer? = null
    override fun addNotification(notification: NotificationContainer) {
        newNotification = notification.copyObject()
        showNotification()
    }


    override fun removePendingNotification(id: Int) {
        notificationManager.cancel(id)
    }

    override fun removeAllPendingNotifications() {
        pendingNotificationScheduler?.cancel(true)
        pendingNotificationScheduler = null
        notificationManager.cancelAll()
    }

    override fun getCountOfPendingNotification(): Int = notificationMap.size

    private fun showAllPendingNotification() {
        if (currentNotification == null) {
            newNotification?.let {
                currentNotification = it.copyObject()
                notificationManager.notify(it.id, it.notification)
            }
        } else
            if (currentNotification?.equals(newNotification) == false) {
                newNotification?.let {
                    notificationManager.notify(it.id, it.notification)
                }
            }
        pendingNotificationScheduler = null
    }

    private fun showNotification() {
        if (pendingNotificationScheduler == null) {
            startExecution()
        } else {
            pendingNotificationScheduler?.cancel(true)
            pendingNotificationScheduler = null
            showNotification()
        }
    }

    private fun startExecution() {
        pendingNotificationScheduler = service.schedule({
            showAllPendingNotification()
        }, ACTION_DELAY, TimeUnit.MILLISECONDS)
    }
}

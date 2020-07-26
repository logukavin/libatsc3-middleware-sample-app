package org.ngbp.jsonrpc4jtestharness.service

import android.app.Notification
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.*

class NotificationManager(val context: Context) : INotificationManager {
    private var notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)
    private val ACTION_DELAY = 2000L
    private var notificationMap = ConcurrentHashMap<Int, Notification>()
    private val service: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var run: ScheduledFuture<*>? = null

    var currentNotification: NotificationContainer? = null
    var newNotification: NotificationContainer? = null
    override fun addNotification(notification: NotificationContainer) {
        newNotification = notification.copyObject()
        showNotification()
    }


    override fun removePendingNotification(id: Int) {
        notificationManager.cancel(id)
    }

    override fun removeAllPendingNotifications() {
        run?.cancel(true)
        run = null
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
        run = null
    }

    private fun showNotification() {
        if (run == null) {
            startExecution()
        } else {
            run?.cancel(true)
            run = null
            showNotification()
        }
    }

    private fun startExecution() {
        run = service.schedule({
            showAllPendingNotification()
        }, ACTION_DELAY, TimeUnit.MILLISECONDS)
    }
}

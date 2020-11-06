package com.nextgenbroadcast.mobile.middleware.sample

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService
import com.nextgenbroadcast.mobile.middleware.service.StandaloneAtsc3Service
import com.nextgenbroadcast.mobile.service.binder.InterprocessServiceBinder

abstract class BaseActivity : AppCompatActivity() {
    var isBound: Boolean = false
        private set

    fun bindService() {
        if (isBound) return

        Intent(this, StandaloneAtsc3Service::class.java).also { intent ->
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    fun unbindService() {
        if (!isBound) return

        unbindService(connection)
        isBound = false
        onUnbind()
    }

    abstract fun onBind(binder: IServiceBinder)
    abstract fun onUnbind()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            onBind(InterprocessServiceBinder(service, BuildConfig.APPLICATION_ID))
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }
}

fun openRoute(context: Context, path: String) {
    Atsc3ForegroundService.openRoute(context, path)
}
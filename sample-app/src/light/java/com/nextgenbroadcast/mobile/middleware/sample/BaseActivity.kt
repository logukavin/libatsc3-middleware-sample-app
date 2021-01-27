package com.nextgenbroadcast.mobile.middleware.sample

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.service.binder.InterprocessServiceBinder

abstract class BaseActivity : AppCompatActivity() {
    var isBound: Boolean = false
        private set

    fun bindService() {
        if (isBound) return

        newServiceIntent().also { intent ->
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
        private var binder: InterprocessServiceBinder? = null

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            binder = InterprocessServiceBinder(service).also {
                onBind(it)
            }
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            if (isBound) {
                binder?.close()
                binder = null

                onUnbind()
            }

            isBound = false
        }
    }
}

fun openRoute(context: Context, path: String) {
    newServiceIntent().apply {
        action = /*Atsc3ForegroundService.ACTION_OPEN_ROUTE*/ "com.nextgenbroadcast.mobile.middleware.intent.action.OPEN_ROUTE"
        putExtra(/*BindableForegroundService.EXTRA_FOREGROUND*/"foreground", true)
        putExtra(/*Atsc3ForegroundService.EXTRA_ROUTE_PATH*/ "route_path", path)
    }.also { intent ->
        ContextCompat.startForegroundService(context, intent)
    }
}

private fun newServiceIntent() = Intent().apply {
    component = ComponentName(
            "com.nextgenbroadcast.mobile.middleware.sample.standalone",
            "com.nextgenbroadcast.mobile.middleware.service.StandaloneAtsc3Service"
    )
}
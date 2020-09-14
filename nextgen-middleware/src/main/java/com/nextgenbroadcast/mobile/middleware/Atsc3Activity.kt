package com.nextgenbroadcast.mobile.middleware

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService
import com.nextgenbroadcast.mobile.middleware.service.EmbeddedAtsc3Service
import com.nextgenbroadcast.mobile.middleware.service.StandaloneAtsc3Service
import com.nextgenbroadcast.mobile.middleware.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.service.binder.InterprocessServiceBinder

abstract class Atsc3Activity : AppCompatActivity() {
    var isBound: Boolean = false
        private set

    override fun onStart() {
        super.onStart()
        Intent().apply {
            component = ComponentName(
                    "${BuildConfig.LIBRARY_PACKAGE_NAME}.sample.standalone",
                    "${Atsc3ForegroundService.clazz.canonicalName}"
            )
        }.also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()

        unbindService(connection)
        isBound = false
        onUnbind()
    }

    abstract fun onBind(binder: IServiceBinder)
    abstract fun onUnbind()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {

            val binder = when (Atsc3ForegroundService.clazz) {
                EmbeddedAtsc3Service::class.java -> {
                    service as? IServiceBinder
                }
                StandaloneAtsc3Service::class.java -> {
                    InterprocessServiceBinder(service)
                }
                else -> null
            } ?: return

            onBind(binder)
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }
}
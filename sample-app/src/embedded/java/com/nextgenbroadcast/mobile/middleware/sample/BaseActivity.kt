package com.nextgenbroadcast.mobile.middleware.sample

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.R
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService
import com.nextgenbroadcast.mobile.middleware.service.EmbeddedAtsc3Service

abstract class BaseActivity : AppCompatActivity() {
    var isBound: Boolean = false
        private set

    fun bindService() {
        if (isBound) return

        Intent(this, EmbeddedAtsc3Service::class.java).also { intent ->
            intent.action = ACTION_BIND_FROM_ACTIVITY
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
            val binder = service as? IServiceBinder ?: run {
                Toast.makeText(this@BaseActivity, R.string.service_action_disconnect, Toast.LENGTH_LONG).show()
                return
            }

            onBind(binder)
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    companion object {
        const val ACTION_BIND_FROM_ACTIVITY = "activity"
    }
}

fun openRoute(context: Context, path: String) {
    Atsc3ForegroundService.openRoute(context, path)
}
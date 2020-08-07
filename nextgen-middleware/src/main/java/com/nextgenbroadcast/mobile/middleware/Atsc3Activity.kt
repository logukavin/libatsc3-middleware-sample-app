package com.nextgenbroadcast.mobile.middleware

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity

abstract class Atsc3Activity : AppCompatActivity() {
    var isBound: Boolean = false
        private set

    override fun onStart() {
        super.onStart()

        Intent(this, Atsc3ForegroundService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()

        unbindService(connection)
        isBound = false
        onUnbind()
    }

    abstract fun onBind(binder: Atsc3ForegroundService.ServiceBinder)
    abstract fun onUnbind()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as? Atsc3ForegroundService.ServiceBinder ?: return
            onBind(binder)
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }
}
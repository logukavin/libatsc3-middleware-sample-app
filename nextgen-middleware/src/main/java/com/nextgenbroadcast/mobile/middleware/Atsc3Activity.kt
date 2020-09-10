package com.nextgenbroadcast.mobile.middleware

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity

abstract class Atsc3Activity : AppCompatActivity() {
    var isBound: Boolean = false
        private set

    override fun onStart() {
        super.onStart()

        Intent(this, SERVICE_CLASS).also { intent ->
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
            when(SERVICE_CLASS) {
                EmbeddedAtsc3Service::class.java -> {
                    val binder = service as? IServiceBinder ?: return
                    isBound = true
                    onBind(binder)
                }
                StandaloneAtsc3Service::class.java -> {
                    isBound = true
                    onBind(InterprocessServiceBinder(service))
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }


    companion object {
        //val SERVICE_CLASS = StandaloneAtsc3Service::class.java
        val SERVICE_CLASS = EmbeddedAtsc3Service::class.java
    }
}
package com.nextgenbroadcast.mobile.middleware.sample

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.R
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService
import com.nextgenbroadcast.mobile.middleware.service.EmbeddedAtsc3Service

abstract class BaseActivity : AppCompatActivity() {
    private lateinit var mediaBrowser: MediaBrowserCompat

    var isBound: Boolean = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaBrowser = MediaBrowserCompat(
                this,
                ComponentName(this, EmbeddedAtsc3Service::class.java),
                connectionCallbacks,
                null
        )
    }

    fun bindService() {
        // The media session connection will start ForegroundService that requires permission.
        // This method called after permission request, that's why it should be here.
        if (!mediaBrowser.isConnected) {
            mediaBrowser.connect()
        }

        if (isBound) return

        Intent(this, EmbeddedAtsc3Service::class.java).apply {
            action = EmbeddedAtsc3Service.SERVICE_INTERFACE
            putExtra(Atsc3ForegroundService.EXTRA_PLAY_AUDIO_ON_BOARD, true)
        }.also { intent ->
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    fun unbindService() {
        mediaBrowser.disconnect()

        if (!isBound) return

        unbindService(connection)

        isBound = false
        onUnbind()
    }

    abstract fun onBind(binder: IServiceBinder)
    abstract fun onUnbind()

    abstract fun onSourcesAvailable(sources: List<MediaBrowserCompat.MediaItem>)
    abstract fun onMediaSessionCreated()

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            val mediaController = MediaControllerCompat(this@BaseActivity, mediaBrowser.sessionToken)
            MediaControllerCompat.setMediaController(this@BaseActivity, mediaController)

            mediaBrowser.subscribe(mediaBrowser.root, object : MediaBrowserCompat.SubscriptionCallback() {
                override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) {
                    onSourcesAvailable(children)
                }
            })

            onMediaSessionCreated()
        }

        override fun onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }
    }

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
}

fun openRoute(context: Context, path: String) {
    Atsc3ForegroundService.openRoute(context, path)
}
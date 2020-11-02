package com.nextgenbroadcast.mobile.middleware.sample

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nextgenbroadcast.mobile.permission.UriPermissionProvider
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.service.binder.InterprocessServiceBinder

abstract class BaseActivity : AppCompatActivity() {

    var isBound: Boolean = false
        private set

    private val uriPermissionProvider = UriPermissionProvider(BuildConfig.APPLICATION_ID)

    override fun onStart() {
        super.onStart()

        newServiceIntent().also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()

        unbindService(connection)
        isBound = false
        onUnbind()
    }

    override fun onAttachFragment(fragment: Fragment) {
        if (fragment is BaseFragment) {
            fragment.uriPermissionProvider = uriPermissionProvider
            fragment.newServiceIntent = newServiceIntent()
        }
    }

    abstract fun onBind(binder: IServiceBinder)
    abstract fun onUnbind()

    private val connection = object : ServiceConnection {
        private var binder: InterprocessServiceBinder? = null

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            binder = InterprocessServiceBinder(service, BuildConfig.APPLICATION_ID, uriPermissionProvider).also {
                onBind(it)
                uriPermissionProvider.setPermissionRequester(it)
            }
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            if (isBound) {
                binder?.close()
                binder = null

                onUnbind()
            }

            uriPermissionProvider.setPermissionRequester(null)

            isBound = false
        }
    }

    private fun newServiceIntent() = Intent().apply {
        component = ComponentName(
                "com.nextgenbroadcast.mobile.middleware.sample.standalone",
                "com.nextgenbroadcast.mobile.middleware.service.StandaloneAtsc3Service"
        )
    }
}
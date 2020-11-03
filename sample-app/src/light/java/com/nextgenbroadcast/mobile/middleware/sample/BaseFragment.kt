package com.nextgenbroadcast.mobile.middleware.sample

import androidx.fragment.app.Fragment
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.sample.view.ReceiverPlayerView
import com.nextgenbroadcast.mobile.permission.UriPermissionProvider
import com.nextgenbroadcast.mobile.service.binder.InterprocessServiceBinder

open class BaseFragment : Fragment() {

    private val uriPermissionProvider = UriPermissionProvider(BuildConfig.APPLICATION_ID)

    open fun onBind(binder: IServiceBinder) {
        if (binder is InterprocessServiceBinder) {
            binder.setPermissionProvider(uriPermissionProvider)
        }
    }

    open fun onUnbind() {
        uriPermissionProvider.setPermissionRequester(null)
    }

    fun preparePlayerView(playerView: ReceiverPlayerView) {
        playerView.setUriPermissionProvider(uriPermissionProvider)
    }
}
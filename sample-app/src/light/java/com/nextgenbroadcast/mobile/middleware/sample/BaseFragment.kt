package com.nextgenbroadcast.mobile.middleware.sample

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.nextgenbroadcast.mobile.middleware.sample.view.ReceiverPlayerView
import com.nextgenbroadcast.mobile.permission.UriPermissionProvider

open class BaseFragment : Fragment() {

    var uriPermissionProvider: UriPermissionProvider? = null
    var newServiceIntent: Intent? = null

    fun openRoute(path: String) {
        newServiceIntent?.apply {
            action = /*Atsc3ForegroundService.ACTION_OPEN_ROUTE*/ "com.nextgenbroadcast.mobile.middleware.intent.action.OPEN_ROUTE"
            putExtra(/*BindableForegroundService.EXTRA_FOREGROUND*/"foreground", true)
            putExtra(/*Atsc3ForegroundService.EXTRA_ROUTE_PATH*/ "route_path", path)
        }.also { intent ->
            intent?.let { ContextCompat.startForegroundService(requireActivity(), it) }
        }
    }

    fun preparePlayerView(playerView: ReceiverPlayerView) {
        playerView.setUriPermissionProvider(uriPermissionProvider)
    }
}
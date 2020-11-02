package com.nextgenbroadcast.mobile.middleware.sample

import androidx.fragment.app.Fragment
import com.nextgenbroadcast.mobile.middleware.sample.view.ReceiverPlayerView
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService

open class BaseFragment : Fragment() {
    fun openRoute(path: String) {
        Atsc3ForegroundService.openRoute(requireContext(), path)
    }

    fun preparePlayerView(playerView: ReceiverPlayerView) {}
}
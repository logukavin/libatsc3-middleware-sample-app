package com.nextgenbroadcast.mobile.core.presentation

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.AppData

interface IUserAgentPresenter {
    val appData: LiveData<AppData?>
    val appState: LiveData<Int>

    fun setState(state: Int)

    companion object {
        const val STATE_UNAVAILABLE = 0
        const val STATE_LOADED = 1
        const val STATE_OPENED = 2
    }
}
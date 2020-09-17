package com.nextgenbroadcast.mobile.core.presentation

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.AppData

interface IUserAgentPresenter {
    val appData: LiveData<AppData?>
}
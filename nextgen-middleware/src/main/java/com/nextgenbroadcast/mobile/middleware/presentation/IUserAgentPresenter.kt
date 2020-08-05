package com.nextgenbroadcast.mobile.middleware.presentation

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.AppData

interface IUserAgentPresenter {
    val appData: LiveData<AppData?>
}
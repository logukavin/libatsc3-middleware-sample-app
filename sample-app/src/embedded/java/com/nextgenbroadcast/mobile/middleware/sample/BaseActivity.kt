package com.nextgenbroadcast.mobile.middleware.sample

import com.nextgenbroadcast.mobile.middleware.Atsc3Activity
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService

abstract class BaseActivity : Atsc3Activity() {

    fun openRoute(path: String) {
        Atsc3ForegroundService.openRoute(this, path)
    }
}
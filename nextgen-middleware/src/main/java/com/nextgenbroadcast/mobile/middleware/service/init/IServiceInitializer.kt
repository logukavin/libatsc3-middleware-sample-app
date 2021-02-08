package com.nextgenbroadcast.mobile.middleware.service.init

import android.content.Context

internal interface IServiceInitializer {
    fun initialize(context: Context, components: Map<Class<*>, Pair<Int, String>>): Boolean
    fun cancel()
}
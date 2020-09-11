package com.nextgenbroadcast.mobile.middleware

import android.content.Context
import androidx.startup.Initializer
import com.nextgenbroadcast.mobile.middleware.service.StandaloneAtsc3Service

class StandaloneServiceInitializer : Initializer<StandaloneAtsc3Service> {
    override fun create(context: Context) = StandaloneAtsc3Service()

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = arrayListOf()
}
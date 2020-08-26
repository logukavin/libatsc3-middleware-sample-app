package com.nextgenbroadcast.mobile.middleware.settings

import android.content.Context
import androidx.core.content.edit
import com.nextgenbroadcast.mobile.middleware.BuildConfig
import com.nextgenbroadcast.mobile.middleware.server.ServerConstants
import java.util.*

internal class MiddlewareSettingsImpl(context: Context) : IMiddlewareSettings {
    private val preferences = context.getSharedPreferences(REPOSITORY_PREFERENCE, Context.MODE_PRIVATE)

    override val deviceId: String
        get() = requireString(DEVICE_ID) {
            UUID.randomUUID().toString()
        }

    override val advertisingId: String
        get() = requireString(ADVERTISING_ID) {
            UUID.randomUUID().toString()
        }

    override val hostName = ServerConstants.HOST_NAME
    override val httpPort = ServerConstants.HTTP_PORT
    override val httpsPort = ServerConstants.HTTPS_PORT
    override val wsPort = ServerConstants.WS_PORT
    override val wssPort = ServerConstants.WSS_PORT

    private fun saveString(key: String, value: String): String {
        preferences.edit { putString(key, value) }
        return value
    }

    private fun requireString(key: String, action: () -> String): String {
        return preferences.getString(key, null) ?: saveString(key, action.invoke())
    }

    companion object {
        const val REPOSITORY_PREFERENCE = "${BuildConfig.LIBRARY_PACKAGE_NAME}.preference"
        const val DEVICE_ID = "device_id"
        const val ADVERTISING_ID = "advertising_id"
    }
}
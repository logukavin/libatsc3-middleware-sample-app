package com.nextgenbroadcast.mobile.middleware.repository

import android.content.Context
import androidx.core.content.edit
import com.nextgenbroadcast.mobile.middleware.BuildConfig
import java.util.*

class PreferenceHelperImpl(context: Context) : IPreferenceHelper {
    private val preferences = context.getSharedPreferences(REPOSITORY_PREFERENCE, Context.MODE_PRIVATE)

    override val deviceId: String
        get() = requireString(DEVICE_ID) {
            UUID.randomUUID().toString()
        }

    override val advertisingId: String
        get() = requireString(ADVERTISING_ID) {
            UUID.randomUUID().toString()
        }

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
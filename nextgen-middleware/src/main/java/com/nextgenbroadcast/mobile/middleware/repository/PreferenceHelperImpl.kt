package com.nextgenbroadcast.mobile.middleware.repository

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelperImpl(private var applicationContext: Context) : PreferenceHelper {
    companion object {
        const val REPOSITORY_PREFERENCE = "com.nextgenbroadcast.mobile.middleware.repository"
        const val DEVICE_ID = "device_id"
        const val ADVERTISING_ID = "advertising_id"
    }

    lateinit var preferences: SharedPreferences

    init {
        setUpIDGeneration()
    }

    private fun setUpIDGeneration() {
        preferences = applicationContext.getSharedPreferences(REPOSITORY_PREFERENCE, Context.MODE_PRIVATE)
        val deviceId = preferences.getString(DEVICE_ID, "")
        val advertisingId = preferences.getString(ADVERTISING_ID, "")
        if (deviceId == "" || advertisingId == "") {
            with(preferences.edit()) {
                putString(DEVICE_ID, java.util.UUID.randomUUID().toString())
                putString(ADVERTISING_ID, java.util.UUID.randomUUID().toString())
                commit()
            }
        }
    }

    override fun getDeviceID(): String {
        return preferences.getString(DEVICE_ID, "")?:""
    }
    override fun getAdvertisingId(): String {
        return preferences.getString(ADVERTISING_ID, "") ?: ""
    }
}
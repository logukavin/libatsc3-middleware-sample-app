package com.nextgenbroadcast.mobile.middleware.settings

import android.content.Context
import android.location.Location
import androidx.core.content.edit
import com.nextgenbroadcast.mobile.middleware.BuildConfig
import com.nextgenbroadcast.mobile.middleware.server.ServerConstants
import org.json.JSONObject
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

    override var location: Location?
        get() = preferences.getString(LOCATION, null)?.let { locationJsonStr ->
            val locationJson = JSONObject(locationJsonStr)
            Location(locationJson.getString(LOCATION_PROVIDER)).apply {
                latitude = locationJson.getDouble(LOCATION_LATITUDE)
                longitude = locationJson.getDouble(LOCATION_LONGITUDE)
            }
        }
        set(value) {
            value?.let { location ->
                preferences.edit {
                    putString(LOCATION, JSONObject().apply {
                        put(LOCATION_PROVIDER, location.provider)
                        put(LOCATION_LATITUDE, location.latitude)
                        put(LOCATION_LONGITUDE, location.longitude)
                    }.toString())
                }
            }
        }

    override var frequencyList: List<Int>?
        get() = preferences.getStringSet(FREQUENCY_LIST, null)?.let { frequencyArrayStr ->
            frequencyArrayStr.map { it.toInt() }
        }
        set(value) {
            preferences.edit {
                putStringSet(FREQUENCY_LIST, value?.map { it.toString() }?.toSet())
            }
        }

    override val hostName = ServerConstants.HOST_NAME
    override var httpPort = ServerConstants.PORT_AUTOFIT
    override var httpsPort = ServerConstants.PORT_AUTOFIT
    override var wsPort = ServerConstants.PORT_AUTOFIT
    override var wssPort = ServerConstants.PORT_AUTOFIT

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
        private const val LOCATION = "location"
        private const val LOCATION_PROVIDER = "location_provider"
        private const val LOCATION_LATITUDE = "location_latitude"
        private const val LOCATION_LONGITUDE = "location_longitude"
        private const val FREQUENCY_LIST = "frequency_list"
    }
}
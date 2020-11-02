package com.nextgenbroadcast.mobile.middleware.settings

import android.content.Context
import android.location.Location
import androidx.core.content.edit
import com.nextgenbroadcast.mobile.middleware.BuildConfig
import com.nextgenbroadcast.mobile.middleware.location.FrequencyLocation
import com.nextgenbroadcast.mobile.middleware.server.ServerConstants
import org.json.JSONArray
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

    override var frequencyLocation: FrequencyLocation?
        get() = loadString(FREQUENCY_LOCATION)?.let {
            stringToFrequencyLocation(it)
        }
        set(value) {
            frequencyLocationToString(value)?.let {
                saveString(FREQUENCY_LOCATION, it)
            }
        }

    override var lastFrequency: Int
        get() = loadInt(FREQUENCY_USER)
        set(value) {
            saveInt(FREQUENCY_USER, value)
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

    private fun loadString(key: String): String? {
        return preferences.getString(key, null)
    }

    private fun requireString(key: String, action: () -> String): String {
        return loadString(key) ?: saveString(key, action.invoke())
    }

    private fun saveInt(key: String, value: Int): Int {
        preferences.edit { putInt(key, value) }
        return value
    }

    private fun loadInt(key: String): Int {
        return preferences.getInt(key, 0)
    }

    private fun stringToFrequencyLocation(flJsonStr: String): FrequencyLocation? {
        val frequencyLocationJson = JSONObject(flJsonStr)

        val frequencyJSONArray = frequencyLocationJson.optJSONArray(FREQUENCY_LIST)
        val frequencyList = mutableListOf<Int>().apply {
            frequencyJSONArray?.let {
                for (index in 0 until frequencyJSONArray.length()) {
                    add(index, frequencyJSONArray.get(index) as Int)
                }
            }
        }

        val location = if (frequencyLocationJson.has(LOCATION_PROVIDER)) {
            Location(frequencyLocationJson.getString(LOCATION_PROVIDER)).apply {
                latitude = frequencyLocationJson.optDouble(LOCATION_LATITUDE, 0.0)
                longitude = frequencyLocationJson.optDouble(LOCATION_LONGITUDE, 0.0)
            }
        } else {
            return null
        }

        return FrequencyLocation(location, frequencyList)
    }

    private fun frequencyLocationToString(frequencyLocation: FrequencyLocation?): String? {
        return frequencyLocation?.let {
            JSONObject().apply {
                put(LOCATION_PROVIDER, frequencyLocation.location.provider)
                put(LOCATION_LATITUDE, frequencyLocation.location.latitude)
                put(LOCATION_LONGITUDE, frequencyLocation.location.longitude)
                put(FREQUENCY_LIST, JSONArray(frequencyLocation.frequencyList))
            }.toString()
        }
    }

    companion object {
        private const val REPOSITORY_PREFERENCE = "${BuildConfig.LIBRARY_PACKAGE_NAME}.preference"
        private const val DEVICE_ID = "device_id"
        private const val ADVERTISING_ID = "advertising_id"
        private const val FREQUENCY_LOCATION = "frequency_location"
        private const val LOCATION_PROVIDER = "location_provider"
        private const val LOCATION_LATITUDE = "location_latitude"
        private const val LOCATION_LONGITUDE = "location_longitude"
        private const val FREQUENCY_LIST = "frequency_list"
        private const val FREQUENCY_USER = "frequency_user"
    }
}
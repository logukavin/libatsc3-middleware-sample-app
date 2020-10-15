package com.nextgenbroadcast.mobile.middleware.service.init

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService

internal object MetadataReader {

    fun discoverMetadata(context: Context): HashMap<Class<*>, Pair<Int, String>> {
        val discovered = HashMap<Class<*>, Pair<Int, String>>()

        try {
            val service = ComponentName(context.packageName, Atsc3ForegroundService.clazz.name)
            val providerInfo = context.packageManager.getServiceInfo(service, PackageManager.GET_META_DATA)
            providerInfo.metaData?.let { metadata ->
                val keys = metadata.keySet()
                for (key in keys) {
                    val clazz = Class.forName(key)
                    discovered[clazz] = getData(metadata, key)
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }

        return discovered
    }

    private fun getData(metadata: Bundle, key: String?): Pair<Int, String> {
        return when (val value = metadata.get(key)) {
            is Int -> Pair(value, "")
            is String -> Pair(0, value)
            else -> Pair(0, "")
        }
    }

}
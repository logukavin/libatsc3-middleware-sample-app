package com.nextgenbroadcast.mobile.middleware.service.init

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService

internal object MetadataReader {
    internal fun discoverMetadata(context: Context): HashMap<Class<*>, Pair<Int, String>> {
        val discovered = HashMap<Class<*>, Pair<Int, String>>()

        try {
            val service = ComponentName(context.packageName, Atsc3ForegroundService.clazz.name)
            val providerInfo = context.packageManager.getServiceInfo(service, PackageManager.GET_META_DATA)
            providerInfo.metaData?.let { metadata ->
                val keys = metadata.keySet()
                for (key in keys) {
                    val resource = metadata.getInt(key)
                    val value = metadata.getString(key, null)
                    val clazz = Class.forName(key)

                    discovered[clazz] = Pair(resource, value)
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            //throw StartupException(exception)
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            //throw StartupException(exception)
            e.printStackTrace()
        }

        return discovered
    }

}
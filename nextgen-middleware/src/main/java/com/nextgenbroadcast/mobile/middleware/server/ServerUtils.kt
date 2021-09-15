package com.nextgenbroadcast.mobile.middleware.server

import android.net.Uri
import com.nextgenbroadcast.mobile.core.md5
import com.nextgenbroadcast.mobile.middleware.settings.IClientSettings
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.apache.commons.lang3.StringUtils

object ServerUtils {

    fun generateAppContextCachePath(appContextId: String): String {
        return appContextId.md5()
    }

    fun createUrl(entryPageUrl: String, settings: IClientSettings) = Uri.Builder()
            .scheme("https")
            .encodedAuthority("${settings.hostName}:${settings.httpsPort}")
            .appendEncodedPath(entryPageUrl)
            .build()
            .toString()


    //jjustman--2021-09-14 - establish our baseUri context either both bcast and bbandEntryPage, BA consumes this value via org.atsc.query.baseURI jsonrpc method used for=
    fun createBasePathFromClientSettings(settings: IClientSettings, appContextId: String)= Uri.Builder()
            .scheme("https")
            .encodedAuthority("${settings.hostName}:${settings.httpsPort}")
            .appendEncodedPath(generateAppContextCachePath(appContextId))
            .build()
            .toString()

    //jjustman-2021-09-14 - hack: if we don't have our IClientSettings, try and chomp it from our appContextId
    fun createBasePathFromBcastEntryPageUrl(appEntryPoint: String, appContextId: String): String {
        return StringUtils.chop(appEntryPoint.toHttpUrlOrNull()?.resolve("/" + generateAppContextCachePath(appContextId)).toString())
    }

    fun createEntryPoint(entryPageUrl: String, appContextId: String, settings: IClientSettings) = Uri.Builder()
            .scheme("https")
            .encodedAuthority("${settings.hostName}:${settings.httpsPort}")
            .appendEncodedPath(generateAppContextCachePath(appContextId))
            .appendEncodedPath(entryPageUrl)
            .build()
            .toString()

    fun addSocketPath(entryPoint: String, settings: IClientSettings) = Uri.parse(entryPoint)
            .buildUpon()
            .appendQueryParameter("wsURL", "wss://${settings.hostName}:${settings.wssPort}")
            .appendQueryParameter("rev", ServerConstants.REVISION)
            .build()
            .toString()


    fun multipartFilePath(fileName: String, index: Int) = "$fileName?index=$index"
}
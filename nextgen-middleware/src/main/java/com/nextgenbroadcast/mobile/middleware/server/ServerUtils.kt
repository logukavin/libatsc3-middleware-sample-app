package com.nextgenbroadcast.mobile.middleware.server

import android.net.Uri
import com.nextgenbroadcast.mobile.core.md5
import com.nextgenbroadcast.mobile.middleware.settings.IClientSettings

internal object ServerUtils {

    fun createUrl(entryPageUrl: String, settings: IClientSettings) =
        createEntryPoint(null, entryPageUrl, settings)

    fun createEntryPoint(appContextId: String?, entryPageUrl: String?, settings: IClientSettings) = Uri.Builder()
        .scheme("https")
        .encodedAuthority("${settings.hostName}:${settings.httpsPort}")
        .apply { appContextId?.let { appendEncodedPath(appContextId.md5()) } }
        .apply { entryPageUrl?.let { appendEncodedPath(entryPageUrl) } }
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
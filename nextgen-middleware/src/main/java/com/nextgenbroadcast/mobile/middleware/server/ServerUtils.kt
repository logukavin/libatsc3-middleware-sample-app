package com.nextgenbroadcast.mobile.middleware.server

import android.net.Uri
import com.nextgenbroadcast.mobile.core.md5
import com.nextgenbroadcast.mobile.middleware.settings.IClientSettings

object ServerUtils {

    fun createEntryPoint(entryPageUrl: String, appContextId: String, settings: IClientSettings) = Uri.Builder()
            .scheme("https")
            .encodedAuthority("${settings.hostName}:${settings.httpsPort}")
            .appendEncodedPath(appContextId.md5())
            .appendEncodedPath(entryPageUrl)
            .build()
            .toString()

    fun addSocketPath(entryPoint: String, settings: IClientSettings) = Uri.parse(entryPoint)
            .buildUpon()
            .appendQueryParameter("wsURL", "wss://${settings.hostName}:${settings.wssPort}")
            .appendQueryParameter("rev", "${ServerConstants.REVISION}")
            .build()
            .toString()

}
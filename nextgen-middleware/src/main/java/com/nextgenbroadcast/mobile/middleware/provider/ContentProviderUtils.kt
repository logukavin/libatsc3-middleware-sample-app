@file:JvmName("ContentProviderUtils")
package com.nextgenbroadcast.mobile.middleware.provider

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build

internal const val ROUTE_CONTENT_SL_HDR1_PRESENT = "routeSlHdr1Present"

internal fun ContentProvider.requireAppContext(): Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    requireContext()
} else {
    context ?: throw IllegalStateException("Provider $this not attached to a context.")
}.applicationContext

internal fun ContentProvider.requireAppContextOrNull(): Context? = try {
    requireAppContext()
} catch (e: IllegalStateException) {
    null
}

internal fun getReceiverUriForPath(authority: String, path: String) = Uri.Builder()
    .scheme(ContentResolver.SCHEME_CONTENT)
    .authority(authority)
    .encodedPath(path)
    .build()

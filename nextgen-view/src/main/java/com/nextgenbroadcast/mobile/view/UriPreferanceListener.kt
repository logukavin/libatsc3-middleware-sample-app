package com.nextgenbroadcast.mobile.view

import android.net.Uri

interface UriPermissionsListener {
    fun requestUriPermissions(uri: Uri): Object?
}
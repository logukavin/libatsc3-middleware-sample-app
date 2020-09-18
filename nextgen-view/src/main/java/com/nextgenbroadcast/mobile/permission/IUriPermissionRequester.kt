package com.nextgenbroadcast.mobile.permission

import android.net.Uri

interface IUriPermissionRequester {
    fun requestUriPermission(uri: Uri)
}
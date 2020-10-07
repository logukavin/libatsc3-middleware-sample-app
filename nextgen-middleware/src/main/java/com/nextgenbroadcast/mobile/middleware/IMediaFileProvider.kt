package com.nextgenbroadcast.mobile.middleware

import android.net.Uri

interface IMediaFileProvider {
    fun getFileProviderUri(path: String): Uri
    fun grantUriPermission(toPackage: String, uri: Uri, isTemporarily: Boolean = true)
    fun revokeAllUriPermissions()
}
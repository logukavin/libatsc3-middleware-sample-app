package com.nextgenbroadcast.mobile.middleware.service.provider

import android.net.Uri

interface IStandaloneMediaFileProvider : IMediaFileProvider {
    fun grantUriPermission(toPackage: String, uri: Uri, isTemporarily: Boolean = true)
    fun revokeAllUriPermissions()
}
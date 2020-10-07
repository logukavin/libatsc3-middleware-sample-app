package com.nextgenbroadcast.mobile.middleware

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class MediaFileProvider(
        private val context: Context,
        private val mediaFileSweeper: MediaFileSweeper,
) : IMediaFileProvider {
    private val authority = context.getString(R.string.nextgenMediaFileProvider)

    override fun getFileProviderUri(path: String): Uri = FileProvider.getUriForFile(
            context,
            authority,
            File(path)
    )

    override fun grantUriPermission(toPackage: String, uri: Uri, isTemporarily: Boolean) {
        context.grantUriPermission(toPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if(isTemporarily)
            mediaFileSweeper.sweepLater(toPackage, uri)
    }

    override fun revokeAllUriPermissions() {
        mediaFileSweeper.sweepEverything()
    }
}
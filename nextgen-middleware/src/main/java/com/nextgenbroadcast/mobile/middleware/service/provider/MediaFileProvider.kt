package com.nextgenbroadcast.mobile.middleware.service.provider

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.nextgenbroadcast.mobile.middleware.R
import java.io.File

open class MediaFileProvider(
        private val context: Context
) : IMediaFileProvider {
    private val authority = context.getString(R.string.nextgenMediaFileProvider)

    override fun getFileProviderUri(path: String): Uri = FileProvider.getUriForFile(
            context,
            authority,
            File(path)
    )

}
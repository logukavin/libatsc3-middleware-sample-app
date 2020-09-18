package com.nextgenbroadcast.mobile.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.google.android.exoplayer2.upstream.ContentDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.TransferListener
import java.io.IOException

class RemoteContentDataSource(
        private val context: Context,
        private val uriPermissionProvider: UriPermissionProvider
) : DataSource {

    private val contentDataSource = ContentDataSource(context)

    override fun addTransferListener(transferListener: TransferListener) {
        contentDataSource.addTransferListener(transferListener)
    }

    @Throws(ContentDataSource.ContentDataSourceException::class)
    override fun open(dataSpec: DataSpec): Long {
        if (context.checkCallingOrSelfUriPermission(dataSpec.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_DENIED) {
            try {
                uriPermissionProvider.requestPermission(dataSpec.uri)
            } catch (e: InterruptedException) {
                e.printStackTrace()
                throw ContentDataSource.ContentDataSourceException(IOException(e))
            }
        }
        return contentDataSource.open(dataSpec)
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        return contentDataSource.read(buffer, offset, readLength)
    }

    override fun getUri(): Uri? = contentDataSource.uri

    override fun getResponseHeaders(): Map<String, List<String>> = contentDataSource.responseHeaders

    @Throws(IOException::class)
    override fun close() {
        contentDataSource.close()
    }
}
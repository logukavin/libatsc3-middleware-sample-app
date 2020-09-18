package com.nextgenbroadcast.mobile.permission

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.TransferListener
import com.google.android.exoplayer2.util.Assertions
import java.io.IOException

class AlterDefaultDataSource(
        private val context: Context,
        private val baseDataSource: DataSource,
        private val uriPermissionProvider: UriPermissionProvider?
) : DataSource {

    private var dataSource: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        dataSource?.addTransferListener(transferListener)
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        Assertions.checkState(dataSource == null)
        val scheme = dataSpec.uri.scheme
        dataSource = if (null != uriPermissionProvider && SCHEME_CONTENT == scheme) {
            RemoteContentDataSource(context, uriPermissionProvider)
        } else {
            DefaultDataSource(context, baseDataSource)
        }
        return dataSource?.open(dataSpec)?:0
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        return Assertions.checkNotNull(dataSource).read(buffer, offset, readLength)
    }

    override fun getUri(): Uri? = dataSource?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = dataSource?.responseHeaders ?: emptyMap()

    @Throws(IOException::class)
    override fun close() {
        dataSource?.let {
            try {
                it.close()
            } finally {
                dataSource = null
            }
        }
    }

    companion object {
        private const val SCHEME_CONTENT = "content"
    }
}
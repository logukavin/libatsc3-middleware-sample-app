package com.nextgenbroadcast.mobile.permission

import android.content.Context
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory

class AlterDataSourceFactory(
        private val context: Context,
        private val userAgent: String,
        private val uriPermissionProvider: UriPermissionProvider?
) : DataSource.Factory {

    override fun createDataSource(): DataSource = AlterDefaultDataSource(
            context,
            DefaultHttpDataSourceFactory(userAgent, null).createDataSource(),
            uriPermissionProvider)
}
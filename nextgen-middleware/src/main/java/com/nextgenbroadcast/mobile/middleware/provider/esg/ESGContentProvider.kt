package com.nextgenbroadcast.mobile.middleware.provider.esg

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.serviceGuide.SGProgram
import androidx.lifecycle.distinctUntilChanged
import com.nextgenbroadcast.mobile.middleware.R
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.SGDataBase
import java.lang.Integer.parseInt
import java.util.*


class ESGContentProvider : ContentProvider(), LifecycleOwner {

    private lateinit var AUTHORITY: String
    private lateinit var SERVICE_CONTENT_URI: Uri
    private lateinit var PROGRAM_CONTENT_URI: Uri
    private lateinit var SERVICES_CONTENT_TYPE: String
    private lateinit var SERVICE_CONTENT_ITEM_TYPE: String
    private lateinit var PROGRAMS_CONTENT_TYPE: String
    private lateinit var PROGRAM_CONTENT_ITEM_TYPE: String

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    private lateinit var db: SGDataBase

    private fun initializeUriMatching() {
        uriMatcher.addURI(AUTHORITY, "$SERVICE_CONTENT_PATH/#", URI_SERVICE_BY_ID)
        uriMatcher.addURI(AUTHORITY, SERVICE_CONTENT_PATH, URI_ALL_SERVICES)

        uriMatcher.addURI(AUTHORITY, "$PROGRAM_CONTENT_PATH/#", URI_PROGRAM_BY_ID)
        uriMatcher.addURI(AUTHORITY, PROGRAM_CONTENT_PATH, URI_ALL_PROGRAMS)
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    override fun onCreate(): Boolean {
        AUTHORITY = context?.getString(R.string.nextgenServicesGuideProvider).toString()
        SERVICE_CONTENT_URI = Uri.parse("content://$AUTHORITY/$SERVICE_CONTENT_PATH")
        PROGRAM_CONTENT_URI = Uri.parse("content://$AUTHORITY/$PROGRAM_CONTENT_PATH")

        SERVICES_CONTENT_TYPE = ("vnd.android.cursor.dir/vnd.$AUTHORITY.$SERVICE_CONTENT_PATH")
        SERVICE_CONTENT_ITEM_TYPE = ("vnd.android.cursor.item/vnd.$AUTHORITY.$SERVICE_CONTENT_PATH")

        PROGRAMS_CONTENT_TYPE = ("vnd.android.cursor.dir/vnd.$AUTHORITY.$PROGRAM_CONTENT_PATH")
        PROGRAM_CONTENT_ITEM_TYPE = ("vnd.android.cursor.item/vnd.$AUTHORITY.$PROGRAM_CONTENT_PATH")

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        initializeUriMatching()

        val appContext = context?.applicationContext ?: return false

        db = SGDataBase.getDatabase(appContext).apply {
            contentDAO().getAllLD().distinctUntilChanged().observe(this@ESGContentProvider) {
                appContext.contentResolver.notifyChange(SERVICE_CONTENT_URI, null)
            }
        }

        return true
    }

    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            URI_ALL_SERVICES -> SERVICES_CONTENT_TYPE
            URI_SERVICE_BY_ID -> SERVICE_CONTENT_ITEM_TYPE
            URI_ALL_PROGRAMS -> PROGRAMS_CONTENT_TYPE
            URI_PROGRAM_BY_ID -> PROGRAM_CONTENT_ITEM_TYPE
            else -> throw IllegalArgumentException("Wrong URI: $uri")
        }
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor {

        val context = context ?: throw IllegalArgumentException("Wrong context")
        val contentResolver = context.contentResolver

        val cursor = fillCursor(uri, selectionArgs, selection)

        cursor.setNotificationUri(contentResolver, SERVICE_CONTENT_URI)

        return cursor
    }

    private fun fillCursor(uri: Uri, selectionArgs: Array<String>?, selection: String?): Cursor {

        when (uriMatcher.match(uri)) {
            URI_ALL_SERVICES -> {
                return db.sgScheduleMapDAO().getAllService(0, 1)
            }

            URI_SERVICE_BY_ID -> {
                var id = -1
                uri.lastPathSegment?.let {
                    id = parseInt(it)
                }
                return db.sgScheduleMapDAO().getServiceById(id, 0, 1)
            }

            URI_ALL_PROGRAMS -> {
                if (selectionArgs.isNullOrEmpty()) throw IllegalArgumentException("Wrong selectionArgs: $selectionArgs")

                val selectionColumns = selection?.split(" AND ")
                var serviceId: Int = -1
                var startTime: Long = 0
                var endTime: Long = Long.MAX_VALUE

                selectionColumns?.forEachIndexed { index, columnName ->
                    when (columnName) {
                        SERVICE_COLUMN_ID -> {
                            serviceId = selectionArgs[index].toInt()
                        }

                        PROGRAM_COLUMN_START_TIME -> {
                            startTime = selectionArgs[index].toLong()
                        }

                        PROGRAM_COLUMN_END_TIME -> {
                            endTime = selectionArgs[index].toLong()
                        }

                        else -> {
                            // ignore
                        }
                    }
                } ?: throw IllegalArgumentException("You missed add some column name")

                if (serviceId < 0) throw IllegalArgumentException("You missed add contentIds arg")

                //TODO: use language from settings
                return db.sgScheduleMapDAO().getContentBy(serviceId, startTime, endTime, Locale.getDefault().language)
            }

            URI_PROGRAM_BY_ID -> {
                //TODO: add implementation
            }

            else -> throw IllegalArgumentException("Wrong URI: $uri")
        }
        throw throw IllegalArgumentException("URI not matched")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun shutdown() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        super.shutdown()
    }

    companion object {
        const val SERVICE_CONTENT_PATH = "services_data"
        const val PROGRAM_CONTENT_PATH = "programs_data"

        const val SERVICE_COLUMN_BSID = "bsid"
        const val SERVICE_COLUMN_ID = "serviceId"
        const val SERVICE_COLUMN_SHORT_NAME = "shortServiceName"
        const val SERVICE_COLUMN_GLOBAL_ID = "globalServiceId"
        const val SERVICE_COLUMN_MAJOR_CHANNEL_NO = "majorChannelNo"
        const val SERVICE_COLUMN_MINOR_CHANNEL_NO = "minorChannelNo"
        const val SERVICE_COLUMN_CATEGORY = "category"

        const val PROGRAM_COLUMN_START_TIME = "startTime"
        const val PROGRAM_COLUMN_END_TIME = "endTime"
        const val PROGRAM_COLUMN_DURATION = "duration"

        const val PROGRAM_COLUMN_CONTENT_ID = "id"
        const val PROGRAM_COLUMN_CONTENT_VERSION = "version"
        const val PROGRAM_COLUMN_CONTENT_ICON = "icon"
        const val PROGRAM_COLUMN_CONTENT_NAME = "name"
        const val PROGRAM_COLUMN_CONTENT_DESCRIPTION = "description"

        const val URI_ALL_SERVICES = 1
        const val URI_SERVICE_BY_ID = 2
        const val URI_ALL_PROGRAMS = 3
        const val URI_PROGRAM_BY_ID = 4

        const val ACTION_BIND_FROM_PROVIDER = "provider"
    }
}
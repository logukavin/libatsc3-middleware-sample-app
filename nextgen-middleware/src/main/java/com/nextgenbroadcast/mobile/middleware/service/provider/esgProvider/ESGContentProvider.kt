package com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider

import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.distinctUntilChanged
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.serviceGuide.SGProgram
import com.nextgenbroadcast.mobile.middleware.R
import com.nextgenbroadcast.mobile.middleware.service.EmbeddedAtsc3Service
import java.lang.Integer.parseInt


class ESGContentProvider : ContentProvider(), LifecycleOwner {

    private lateinit var AUTHORITY: String
    private lateinit var SERVICE_CONTENT_URI: Uri
    private lateinit var PROGRAM_CONTENT_URI: Uri
    private lateinit var SERVICES_CONTENT_TYPE: String
    private lateinit var SERVICE_CONTENT_ITEM_TYPE: String
    private lateinit var PROGRAMS_CONTENT_TYPE: String
    private lateinit var PROGRAM_CONTENT_ITEM_TYPE: String

    private var data: Map<AVService, List<SGProgram>> = emptyMap()
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    var isBound: Boolean = false
        private set

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as? EmbeddedAtsc3Service.ProviderServiceBinder ?: run {
                return
            }

            onBind(binder)

            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

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

        bindService()

        return true
    }

    private fun bindService() {
        if (isBound) return

        Intent(context, EmbeddedAtsc3Service::class.java).also { intent ->
            intent.action = ACTION_BIND_FROM_PROVIDER
            context?.bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    private fun onBind(binder: EmbeddedAtsc3Service.ProviderServiceBinder) {
        binder.serviceController.schedule.distinctUntilChanged().observe(this, { schedule ->
            data = schedule?.toMutableMap() ?: emptyMap()
            context?.contentResolver?.notifyChange(SERVICE_CONTENT_URI, null)
        })
    }

    private fun unbindService() {
        if (!isBound) return

        context?.unbindService(connection)
        isBound = false
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
        val cursor = MatrixCursor(projection)

        val contentResolver = context?.contentResolver ?: return cursor

        when (uriMatcher.match(uri)) {
            URI_ALL_SERVICES -> {
                data.forEach { (avService, _) ->
                    fillServiceRow(cursor, avService)
                }
            }

            URI_SERVICE_BY_ID -> {
                var id = -1
                uri.lastPathSegment?.let {
                    id = parseInt(it)
                }

                if (id >= 0) {
                    data.keys.find { it.id == id }?.let {
                        fillServiceRow(cursor, it)
                    }
                }
            }

            URI_ALL_PROGRAMS -> {
                if (selectionArgs.isNullOrEmpty()) return cursor

                val selectionColumns = selection?.split(" AND ")
                var serviceId: Int = -1
                var startTime: Long = 0
                var endTime: Long = 0

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

                if (serviceId < 0) throw IllegalArgumentException("You missed add service_id arg")

                var filteredData = data.filterKeys {
                    it.id == serviceId
                }.flatMap { it.value }

                if (startTime > 0) {
                    filteredData = filteredData.filter {
                         startTime < it.endTime
                    }
                }

                if (endTime > 0) {
                    filteredData = filteredData.filter {
                        it.endTime <= endTime
                    }
                }

                filteredData.forEach { program ->
                    fillProgramRow(cursor, program)
                }
            }

            URI_PROGRAM_BY_ID -> {
                //TODO: add implementation
            }

            else -> throw IllegalArgumentException("Wrong URI: $uri")
        }

        cursor.setNotificationUri(contentResolver, SERVICE_CONTENT_URI)

        return cursor
    }

    private fun fillServiceRow(cursor: MatrixCursor, avService: AVService) {
        cursor.newRow()
            .add(SERVICE_COLUMN_BSID, avService.bsid)
            .add(SERVICE_COLUMN_ID, avService.id)
            .add(SERVICE_COLUMN_SHORT_NAME, avService.shortName)
            .add(SERVICE_COLUMN_GLOBAL_ID, avService.globalId)
            .add(SERVICE_COLUMN_MAJOR_CHANNEL_NO, avService.majorChannelNo)
            .add(SERVICE_COLUMN_MINOR_CHANNEL_NO, avService.minorChannelNo)
            .add(SERVICE_COLUMN_CATEGORY, avService.category)
    }

    private fun fillProgramRow(cursor: MatrixCursor, program: SGProgram) {
        cursor.newRow()
            .add(PROGRAM_COLUMN_START_TIME, program.startTime)
            .add(PROGRAM_COLUMN_END_TIME, program.endTime)
            .add(PROGRAM_COLUMN_DURATION, program.duration)
            .add(PROGRAM_COLUMN_CONTENT_ID, program.content?.id)
            .add(PROGRAM_COLUMN_CONTENT_VERSION, program.content?.version)
            .add(PROGRAM_COLUMN_CONTENT_ICON, program.content?.icon)
            .add(PROGRAM_COLUMN_CONTENT_NAME, program.content?.name)
            .add(PROGRAM_COLUMN_CONTENT_DESCRIPTION, program.content?.description)
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

        unbindService()
    }

    companion object {
        const val SERVICE_CONTENT_PATH = "services_data"
        const val PROGRAM_CONTENT_PATH = "programs_data"

        const val SERVICE_COLUMN_BSID = "bsid"
        const val SERVICE_COLUMN_ID = "id"
        const val SERVICE_COLUMN_SHORT_NAME = "shortName"
        const val SERVICE_COLUMN_GLOBAL_ID = "globalId"
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
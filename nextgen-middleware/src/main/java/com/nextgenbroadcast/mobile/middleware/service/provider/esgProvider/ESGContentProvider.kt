package com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider

import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.IBinder
import androidx.lifecycle.*
import com.nextgenbroadcast.mobile.core.mapWith
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.serviceGuide.SGProgram
import com.nextgenbroadcast.mobile.middleware.R
import com.nextgenbroadcast.mobile.middleware.service.EmbeddedAtsc3Service
import java.lang.Integer.parseInt
import java.util.*


class ESGContentProvider: ContentProvider(), LifecycleOwner {

    private lateinit var AUTHORITY: String
    private lateinit var SERVICE_CONTENT_URI: Uri
    private lateinit var PROGRAM_CONTENT_URI: Uri
    private lateinit var SERVICES_CONTENT_TYPE: String
    private lateinit var SERVICE_CONTENT_ITEM_TYPE: String
    private lateinit var PROGRAMS_CONTENT_TYPE: String
    private lateinit var PROGRAM_CONTENT_ITEM_TYPE: String

    private var data: MutableMap<AVService, List<SGProgram>> = mutableMapOf()
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
        Transformations.distinctUntilChanged(binder.selectorPresenter.sltServices)
                .mapWith(binder.selectorPresenter.schedule) { (services, schedule) ->
                    schedule?.toMutableMap()?.also { map ->
                        val serviceIds = map.keys.map { it.id }
                        services?.forEach { service ->
                            if (!serviceIds.contains(service.id)) {
                                map[service] = emptyList()
                            }
                        }
                    } ?: services?.associate {
                        it to emptyList()
                    } ?: emptyMap()
                }.observe(this, { schedule ->
                    data = schedule.toMutableMap()
                    context?.contentResolver?.notifyChange(SERVICE_CONTENT_URI, null)
                })
    }

    private fun unbindService() {
        if (!isBound) return

        context?.unbindService(connection)
        isBound = false
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            URI_ALL_SERVICES -> SERVICES_CONTENT_TYPE
            URI_SERVICE_BY_ID -> SERVICE_CONTENT_ITEM_TYPE
            URI_ALL_PROGRAMS -> PROGRAMS_CONTENT_TYPE
            URI_PROGRAM_BY_ID -> PROGRAM_CONTENT_ITEM_TYPE
            else -> throw IllegalArgumentException("Wrong URI: $uri")
        }
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val cursor = MatrixCursor(projection)

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

                data.keys.find {it.id == id}?.let {
                    fillServiceRow(cursor, it)
                }
            }
            URI_ALL_PROGRAMS -> {

                val selectionColumns = selection?.split(" AND ")
                var filteredData = mutableMapOf<AVService, List<SGProgram>>()

                selectionColumns?.forEachIndexed { index, columnName ->

                    val selectionArg = selectionArgs?.get(index)

                    filteredData = when(columnName) {
                        SERVICE_COLUMN_ID -> {
                            selectionArg ?: throw IllegalArgumentException("You missed add service_id arg")

                            data.filterKeys { it.id == selectionArg.toInt() } as MutableMap<AVService, List<SGProgram>>
                        }
                        PROGRAM_COLUMN_START_TIME -> {
                            selectionArg ?: throw IllegalArgumentException("You missed add start_time arg")

                            mutableMapOf<AVService, List<SGProgram>>().apply {
                                filteredData.forEach { (key, list) ->
                                    put(key, list.filter {
                                        (it.startTime <= selectionArg.toLong() && it.endTime > selectionArg.toLong())
                                                || it.startTime >= selectionArg.toLong()
                                    })
                                }
                            }
                        }
                        else -> data
                        //TODO: add implementation for other columns
                    }

                    filteredData.values.forEach { list ->
                        list.forEach { program ->
                            fillProgramRow(cursor, program)
                        }
                    }
                } ?: throw IllegalArgumentException("You missed add some column name")
            }
            URI_PROGRAM_BY_ID -> {
                //TODO: add implementation
            }
            else -> throw IllegalArgumentException("Wrong URI: $uri")
        }

        cursor.setNotificationUri(context?.contentResolver, SERVICE_CONTENT_URI)

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
            .add(PROGRAM_COLUMN_CONTENT_NAME, program.content?.getName(Locale.ENGLISH))
            .add(PROGRAM_COLUMN_CONTENT_DESCRIPTION, program.content?.getDescription(Locale.ENGLISH))
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
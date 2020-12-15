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
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.PROGRAMS_CONTENT_TYPE
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.PROGRAM_COLUMN_CONTENT_DESCRIPTION
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.PROGRAM_COLUMN_CONTENT_ICON
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.PROGRAM_COLUMN_CONTENT_ID
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.PROGRAM_COLUMN_CONTENT_NAME
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.PROGRAM_COLUMN_CONTENT_VERSION
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.PROGRAM_COLUMN_DURATION
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.PROGRAM_COLUMN_END_TIME
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.PROGRAM_COLUMN_START_TIME
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.PROGRAM_CONTENT_ITEM_TYPE
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.PROGRAM_CONTENT_PATH
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.SERVICES_CONTENT_TYPE
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.SERVICE_COLUMN_BSID
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.SERVICE_COLUMN_CATEGORY
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.SERVICE_COLUMN_GLOBAL_ID
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.SERVICE_COLUMN_ID
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.SERVICE_COLUMN_MAJOR_CHANNEL_NO
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.SERVICE_COLUMN_MINOR_CHANNEL_NO
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.SERVICE_COLUMN_SHORT_NAME
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.SERVICE_CONTENT_ITEM_TYPE
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.SERVICE_CONTENT_PATH
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.SERVICE_CONTENT_URI
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.URI_ALL_PROGRAMS
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.URI_ALL_SERVICES
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.URI_PROGRAM_BY_ID
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.Contract.Companion.URI_SERVICE_BY_ID
import com.nextgenbroadcast.mobile.middleware.service.EmbeddedAtsc3Service
import java.lang.Integer.parseInt
import java.lang.NullPointerException
import java.util.*
import java.util.concurrent.TimeUnit


class ESGProvider: ContentProvider(), LifecycleOwner {

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
        uriMatcher.addURI(Contract.AUTHORITY, "$SERVICE_CONTENT_PATH/#", URI_SERVICE_BY_ID)
        uriMatcher.addURI(Contract.AUTHORITY, SERVICE_CONTENT_PATH, URI_ALL_SERVICES)

        uriMatcher.addURI(Contract.AUTHORITY, "$PROGRAM_CONTENT_PATH/#", URI_PROGRAM_BY_ID)
        uriMatcher.addURI(Contract.AUTHORITY, PROGRAM_CONTENT_PATH, URI_ALL_PROGRAMS)
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    override fun onCreate(): Boolean {
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
                uri.lastPathSegment?.let { parseInt(it) }?.let { id = it }
                fillServiceRow(cursor, data.keys.toTypedArray()[id])
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

                            val tempFilteredData = mutableMapOf<AVService, List<SGProgram>>()

                            filteredData.forEach { (key, list) ->
                                tempFilteredData[key] = list.filter {
                                    (it.startTime <= selectionArg.toLong() && it.endTime > selectionArg.toLong())
                                            || it.startTime >= selectionArg.toLong()
                                }
                            }
                            tempFilteredData
                        }
                        else -> data
//                        Todo: add implementation for other columns
                    }

                    filteredData.values.forEach { list ->
                        list.forEach { program ->
                            fillProgramRow(cursor, program)
                        }
                    }
                } ?: throw IllegalArgumentException("You missed add some column name")
            }
            URI_PROGRAM_BY_ID -> {
//                Todo: add implementation
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
        const val ACTION_BIND_FROM_PROVIDER = "provider"
    }
}
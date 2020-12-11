package com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider

import android.net.Uri

open class Contract {

    companion object {
        const val AUTHORITY = "com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.ESGProvider"

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


        val SERVICE_CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$SERVICE_CONTENT_PATH")
        val PROGRAM_CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PROGRAM_CONTENT_PATH")

        const val URI_ALL_SERVICES = 1
        const val URI_SERVICE_BY_ID = 2
        const val URI_ALL_PROGRAMS = 3
        const val URI_PROGRAM_BY_ID = 4

        const val SERVICES_CONTENT_TYPE = ("vnd.android.cursor.dir/vnd.$AUTHORITY.$SERVICE_CONTENT_PATH")
        const val SERVICE_CONTENT_ITEM_TYPE = ("vnd.android.cursor.item/vnd.$AUTHORITY.$SERVICE_CONTENT_PATH")

        const val PROGRAMS_CONTENT_TYPE = ("vnd.android.cursor.dir/vnd.$AUTHORITY.$PROGRAM_CONTENT_PATH")
        const val PROGRAM_CONTENT_ITEM_TYPE = ("vnd.android.cursor.item/vnd.$AUTHORITY.$PROGRAM_CONTENT_PATH")
    }
}
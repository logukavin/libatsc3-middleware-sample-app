package com.nextgenbroadcast.mobile.core.model

import android.os.Bundle
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AVService(
        val bsid: Int,
        val id: Int,
        val shortName: String?,
        val globalId: String?,
        val majorChannelNo: Int,
        val minorChannelNo: Int,
        val category: Int
) : Parcelable {

    fun toBundle(): Bundle {
        return Bundle().apply {
            putInt(FIELD_BSID, bsid)
            putInt(FIELD_ID, id)
            putString(FIELD_SHORT_NAME, shortName)
            putString(FIELD_GLOBAL_ID, globalId)
            putInt(FIELD_MAJOR_CHANNEL_NO, majorChannelNo)
            putInt(FIELD_MINOR_CHANNEL_NO, minorChannelNo)
            putInt(FIELD_CATEGORY, category)
        }
    }

    companion object {
        private const val FIELD_BSID = "bsid"
        private const val FIELD_ID = "id"
        private const val FIELD_SHORT_NAME = "shortName"
        private const val FIELD_GLOBAL_ID = "globalId"
        private const val FIELD_MAJOR_CHANNEL_NO = "majorChannelNo"
        private const val FIELD_MINOR_CHANNEL_NO = "minorChannelNo"
        private const val FIELD_CATEGORY = "category"

        fun fromBundle(bundle: Bundle): AVService {
            return AVService(
                    bundle.getInt(FIELD_BSID),
                    bundle.getInt(FIELD_ID),
                    bundle.getString(FIELD_SHORT_NAME),
                    bundle.getString(FIELD_GLOBAL_ID),
                    bundle.getInt(FIELD_MAJOR_CHANNEL_NO),
                    bundle.getInt(FIELD_MINOR_CHANNEL_NO),
                    bundle.getInt(FIELD_CATEGORY)
            )
        }
    }
}

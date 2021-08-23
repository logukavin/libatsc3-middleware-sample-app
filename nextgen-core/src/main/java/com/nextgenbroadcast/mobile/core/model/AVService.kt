package com.nextgenbroadcast.mobile.core.model

import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AVService(
        val bsid: Int,
        val id: Int,
        val shortName: String?,
        val globalId: String?,
        val majorChannelNo: Int,
        val minorChannelNo: Int,
        val category: Int,
        val hidden: Boolean = false,
        val default: Boolean = false
) : Parcelable {

    fun uniqueId(): Long {
        return (bsid.toLong() shl Int.SIZE_BITS) or id.toLong()
    }

    fun toBundle(): Bundle {
        return Bundle().apply {
            putInt(FIELD_BSID, bsid)
            putInt(FIELD_ID, id)
            putString(FIELD_SHORT_NAME, shortName)
            putString(FIELD_GLOBAL_ID, globalId)
            putInt(FIELD_MAJOR_CHANNEL_NO, majorChannelNo)
            putInt(FIELD_MINOR_CHANNEL_NO, minorChannelNo)
            putInt(FIELD_CATEGORY, category)
            putBoolean(FIELD_HIDDEN, hidden)
            putBoolean(FIELD_DEFAULT, default)
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
        private const val FIELD_HIDDEN = "hidden"

        private const val FIELD_DEFAULT = "default"

        fun fromBundle(bundle: Bundle): AVService {
            return AVService(
                    bundle.getInt(FIELD_BSID),
                    bundle.getInt(FIELD_ID),
                    bundle.getString(FIELD_SHORT_NAME),
                    bundle.getString(FIELD_GLOBAL_ID),
                    bundle.getInt(FIELD_MAJOR_CHANNEL_NO),
                    bundle.getInt(FIELD_MINOR_CHANNEL_NO),
                    bundle.getInt(FIELD_CATEGORY),
                    bundle.getBoolean(FIELD_HIDDEN),
                    bundle.getBoolean(FIELD_DEFAULT)
            )
        }
    }
}

package com.nextgenbroadcast.mobile.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SLSService(
        val bsid: Int,
        val id: Int,
        val shortName: String?,
        val globalId: String?,
        val majorChannelNo: Int,
        val minorChannelNo: Int,
        val category: Int
) : Parcelable

package com.nextgenbroadcast.mobile.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class ReceiverState : Parcelable {
    OPENED, PAUSED, IDLE
}
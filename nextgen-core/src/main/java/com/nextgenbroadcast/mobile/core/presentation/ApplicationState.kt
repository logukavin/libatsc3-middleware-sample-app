package com.nextgenbroadcast.mobile.core.presentation

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class ApplicationState : Parcelable {
    STATE_UNAVAILABLE,
    STATE_LOADED,
    STATE_OPENED
}
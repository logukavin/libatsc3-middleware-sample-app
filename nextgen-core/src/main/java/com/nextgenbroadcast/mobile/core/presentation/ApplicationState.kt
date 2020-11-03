package com.nextgenbroadcast.mobile.core.presentation

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class ApplicationState : Parcelable {
    UNAVAILABLE,
    LOADED,
    OPENED
}
package com.nextgenbroadcast.mobile.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class ApplicationState : Parcelable {
    UNAVAILABLE,
    LOADED,
    OPENED
}
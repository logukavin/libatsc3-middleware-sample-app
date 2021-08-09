package com.nextgenbroadcast.mobile.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RPMParams(
        var scale: Double = 100.0,
        var x: Int = 0,
        var y: Int = 0
) : Parcelable
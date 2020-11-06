package com.nextgenbroadcast.mobile.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlin.math.max

@Parcelize
class PhyFrequency(
        private val _frequency: Int,
        val source: Source
) : Parcelable {
    val frequency: Int
        get() = max(0, _frequency)
    val useDefault: Boolean
        get() = _frequency == LAST_SAVED_FREQUENCY

    enum class Source {
        USER, AUTO
    }

    companion object {
        private const val LAST_SAVED_FREQUENCY = -2

        fun auto(frequency: Int) = PhyFrequency(frequency, Source.AUTO)
        fun user(frequency: Int) = PhyFrequency(frequency, Source.USER)
        fun default(source: Source) = PhyFrequency(LAST_SAVED_FREQUENCY, source)
    }
}
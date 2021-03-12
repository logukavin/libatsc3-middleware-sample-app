package com.nextgenbroadcast.mobile.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ReceiverState(
        val state: State,
        val configIndex: Int,
        val configCount: Int
) : Parcelable {

    enum class State {
        IDLE, SCANNING, TUNING, CONNECTED
    }

    companion object {
        fun idle() = ReceiverState(State.IDLE, -1, -1)
        fun scanning(configIndex: Int, configCount: Int) = ReceiverState(State.SCANNING, configIndex, configCount)
        fun tuning(configIndex: Int, configCount: Int) = ReceiverState(State.TUNING, configIndex, configCount)
        fun connected(configIndex: Int, configCount: Int) = ReceiverState(State.CONNECTED, configIndex, configCount)
    }
}
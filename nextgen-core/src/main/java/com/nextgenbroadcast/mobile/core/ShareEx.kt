package com.nextgenbroadcast.mobile.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun <T> StateFlow<T>.asReadOnly(): StateFlow<T> {
    return if (this is MutableStateFlow<T>) {
        this.asStateFlow()
    } else {
        this
    }
}

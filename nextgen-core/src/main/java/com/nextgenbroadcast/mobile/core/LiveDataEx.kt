package com.nextgenbroadcast.mobile.core

import androidx.annotation.MainThread
import androidx.lifecycle.*

@MainThread
inline fun <X, Y, R> mapPair(sourceA: LiveData<X>, sourceB: LiveData<Y>, crossinline mapFunction: (Pair<X?, Y?>) -> R): LiveData<R> {
    val lastX = Variable<X>()
    val lastY = Variable<Y>()
    return MediatorLiveData<R>().apply {
        addSource(sourceA) { x ->
            lastX.value = x
            setValue(mapFunction(Pair(x, lastY.value)))
        }
        addSource(sourceB) { y ->
            lastY.value = y
            setValue(mapFunction(Pair(lastX.value, y)))
        }
    }
}

class Variable<T> {
    var value: T? = null
}

inline fun <X, Y, R> LiveData<X>.mapWith(second: LiveData<Y>, crossinline transform: (Pair<X?, Y?>) -> R): LiveData<R> =
        mapPair(this, second) { transform(it) }

fun <X, Y> LiveData<X>.unite(second: LiveData<Y>): LiveData<Pair<X?, Y?>> =
        mapPair(this, second) { it }

package com.nextgenbroadcast.mobile.core

import android.os.Bundle
import android.os.Parcelable


fun <T : Parcelable> Bundle?.getParcelable(clazz: Class<T>, key: String): T? {
    if (this == null) return null
    classLoader = clazz.classLoader
    return getParcelable(key)
}

fun <T : Parcelable> Bundle?.getParcelableArrayList(clazz: Class<T>, key: String): ArrayList<T>? {
    if (this == null) return null
    classLoader = clazz.classLoader
    return getParcelableArrayList(key)
}

package com.nextgenbroadcast.mobile.core

import android.location.Location
import java.math.BigInteger
import java.security.MessageDigest

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

fun Double.toDegrees(): String {
    return Location.convert(this, Location.FORMAT_DEGREES).replace(",", ".")
}

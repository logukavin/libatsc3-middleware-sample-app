package org.ngbp.jsonrpc4jtestharness.utils

fun Double?.toMilliSec(): Double? {
    return if (this == null) {
        this
    } else {
        this * 1000
    }
}
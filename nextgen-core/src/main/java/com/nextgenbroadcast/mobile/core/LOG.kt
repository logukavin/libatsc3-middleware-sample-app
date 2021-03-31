package com.nextgenbroadcast.mobile.core

import android.util.Log

object LOG {

    @JvmStatic
    fun v(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.v(tag, msg)
        } else {
            Log.v(tag, msg, tr)
        }
    }

    @JvmStatic
    fun d(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.d(tag, msg)
        } else {
            Log.d(tag, msg, tr)
        }
    }

    @JvmStatic
    fun i(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.i(tag, msg)
        } else {
            Log.i(tag, msg, tr)
        }
    }

    @JvmStatic
    fun w(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.w(tag, msg)
        } else {
            Log.w(tag, msg, tr)
        }
    }

    @JvmStatic
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.e(tag, msg)
        } else {
            Log.e(tag, msg, tr)
        }
    }

}
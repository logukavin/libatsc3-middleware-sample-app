package com.nextgenbroadcast.mobile.middleware.atsc3.source

interface IAtsc3Source {
    fun open(): Int
    fun close()
    fun stop()

    companion object {
        const val CONFIG_DEFAULT = -1

        const val RESULT_ERROR = -1
        const val RESULT_OK = 0
    }
}
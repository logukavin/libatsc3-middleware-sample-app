package com.nextgenbroadcast.mobile.core.serviceGuide

import java.util.*

interface ISGContent {
    val id: String?
    val version: Long
    val icon: String?

    fun getName(local: Locale): String?
    fun getDescription(local: Locale): String?
}
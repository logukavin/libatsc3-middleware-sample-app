package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide

interface IServiceGuideDeliveryUnitReader {
    fun clearAll()
    fun readDeliveryUnit(filePath: String, serviceBsid: Int)
    fun readXml(filePath: String, index: Int): String?
}
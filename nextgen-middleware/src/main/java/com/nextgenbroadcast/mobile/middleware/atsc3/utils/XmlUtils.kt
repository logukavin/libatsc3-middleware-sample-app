package com.nextgenbroadcast.mobile.middleware.atsc3.utils

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

object XmlUtils {

    @Throws(XmlPullParserException::class)
    fun newParser(cmlPayload: String): XmlPullParser {
        return Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(ByteArrayInputStream(cmlPayload.toByteArray()), null)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun skip(parser: XmlPullParser) {
        check(parser.eventType == XmlPullParser.START_TAG)
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    fun strToBool(value: String?): Boolean {
        return java.lang.Boolean.parseBoolean(value)
    }

    fun strToInt(value: String): Int {
        try {
            return value.toInt()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return -1
    }

    fun strToLong(value: String): Long {
        try {
            return value.toLong(16)
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return -1
    }

    fun strToListOfInt(value: String): List<Int> {
        return value.split(" ").map { strToInt(it) }
    }

    fun strToDate(value: String): ZonedDateTime? {
        return try {
            ZonedDateTime.parse(value)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
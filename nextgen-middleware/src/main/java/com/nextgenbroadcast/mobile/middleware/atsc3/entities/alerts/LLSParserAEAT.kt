package com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts

import android.util.Log
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.entities.*
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.*
import kotlin.jvm.Throws

class LLSParserAEAT {

    fun parseAeaTable(xmlPayload: String): ArrayList<AeaTable> {
        val aeaList = ArrayList<AeaTable>()
        try {
            val parser = XmlUtils.newParser(xmlPayload)
            parser.nextTag()
            parser.require(XmlPullParser.START_TAG, null, "AEAT")

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }

                when (parser.name) {
                    "AEA" -> aeaList.add(readAEA(parser))
                    else -> XmlUtils.skip(parser)
                }
            }
        } catch (e: XmlPullParserException) {
            Log.e("LLSParserAEAT", "exception in parsing: $e")
        } catch (e: IOException) {
            Log.e("LLSParserAEAT", "exception in parsing: $e")
        }

        val startIndxs = getAEATagsIdx(xmlPayload, "<AEA ")
        val endIndxs = getAEATagsIdx(xmlPayload, "</AEA>")

        aeaList.forEachIndexed { index, aeaStore ->
            aeaStore.xml = xmlPayload.substring(startIndxs[index], endIndxs[index] + "</AEA>".length)
        }

        return aeaList
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readAEA(parser: XmlPullParser): AeaTable {
        val aea = AeaTable()

        val attrCount = parser.attributeCount
        for (i in 0 until attrCount) {
            when (parser.getAttributeName(i)) {
                "aeaId" -> aea.id = parser.getAttributeValue(i)
                "aeaType" -> aea.type = parser.getAttributeValue(i)
                "refAEAid" -> aea.refId = parser.getAttributeValue(i)
//                "audience" -> aea.audience = parser.getAttributeValue(i) //TODO: uncomment all for full parsing
//                "issuer" -> aea.issuer = parser.getAttributeValue(i)
//                "priority" -> aea.priority = XmlUtils.strToInt(parser.getAttributeValue(i))
//                "wakeup" -> aea.wakeup = XmlUtils.strToBool(parser.getAttributeValue(i))
                else -> {
                    // skip attribute
                }
            }
        }

        while (parser.next() != XmlPullParser.END_DOCUMENT && parser.name != "AEA") {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                "Header" -> {
                    val header = readHeader(parser)
                    aea.effective = header.effective
                    aea.expires = header.expires
                }
                "AEAText" -> aea.messages?.add(readAEAText(parser).message)
                else -> XmlUtils.skip(parser)
            }

//            when (parser.name) {
//                "Header" -> aea.header = readHeader(parser)
//                "AEAText" -> aea.listAeaText.add(readAEAText(parser))
//                "Media" -> aea.media.add(readMedia(parser))
//                "LiveMedia" -> aea.liveMedia = readLiveMedia(parser)
//                else -> XmlUtils.skip(parser)
//            }
        }

        return aea
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readHeader(parser: XmlPullParser): Header {
        val header = Header()

        val attrCount = parser.attributeCount
        for (i in 0 until attrCount) {
            when (parser.getAttributeName(i)) {
                "effective" -> header.effective = parser.getAttributeValue(i)
                "expires" -> header.expires = parser.getAttributeValue(i)
                else -> {
                    // skip attribute
                }
            }
        }

//        while (parser.next() != XmlPullParser.END_DOCUMENT && parser.name != "Header") {
//            if (parser.eventType != XmlPullParser.START_TAG) {
//                continue
//            }
//
//            when (parser.name) {
//                "EventCode" -> header.eventCode = readEventCode(parser)
//                "EventDesc" -> header.listEventDesc?.add(readEventDecs(parser))
//                "Location" -> header.listLocation?.add(readLocation(parser))
//                else -> XmlUtils.skip(parser)
//            }
//        }

        return header
    }

    private fun getAEATagsIdx(str : String, substr: String): ArrayList<Int> {
        val listIdx = arrayListOf<Int>()
        var lastIndex = -1
        while (str.indexOf(substr, lastIndex + 1).also { lastIndex = it } != -1) {
            listIdx.add(lastIndex)
        }
        return listIdx
    }


    @Throws(IOException::class, XmlPullParserException::class)
    private fun readMedia(parser: XmlPullParser): Media {
        val media = Media()

        val attrCount = parser.attributeCount
        for (i in 0 until attrCount) {
            when (parser.getAttributeName(i)) {
                "contentType" -> media.contentType = parser.getAttributeValue(i)
                "alternateUrl" -> media.alternateUrl = parser.getAttributeValue(i)
                "contentLength" -> media.contentLength = XmlUtils.strToLong(parser.getAttributeValue(i))
                "mediaAssoc" -> media.mediaAssoc = parser.getAttributeValue(i)
                "mediaDesc" -> media.mediaDesc = parser.getAttributeValue(i)
                "mediaType" -> media.mediaType = parser.getAttributeValue(i)
                "url" -> media.url = parser.getAttributeValue(i)
                else -> {
                    // skip attribute
                }
            }
        }

        return media
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLiveMedia(parser: XmlPullParser): LiveMedia {
        val liveMedia = LiveMedia()

        val attrCount = parser.attributeCount
        for (i in 0 until attrCount) {
            when (parser.getAttributeName(i)) {
                "contentType" -> liveMedia.bsid = XmlUtils.strToInt(parser.getAttributeValue(i))
                "serviceId" -> liveMedia.serviceId = XmlUtils.strToInt(parser.getAttributeValue(i))
                else -> {
                    // skip attribute
                }
            }
        }

        while (parser.next() != XmlPullParser.END_DOCUMENT && parser.name != "LiveMedia") {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                "ServiceName" -> liveMedia.serviceNames?.add(readServiceNames(parser))
                else -> XmlUtils.skip(parser)
            }
        }

        return liveMedia
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readAEAText(parser: XmlPullParser): AeaText {
        val aeaText = AeaText()

        aeaText.lang = readOneAttribute(parser, "xml:lang")
        aeaText.message = readText(parser, "AEAText")

        return aeaText
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readServiceNames(parser: XmlPullParser): ServiceName {
        val name = ServiceName()

        name.lang = readOneAttribute(parser, "xml:lang")
        name.message = readText(parser, "ServiceName")

        return name
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readEventCode(parser: XmlPullParser): EventCode {
        val code = EventCode()

        code.type = readOneAttribute(parser, "type")
        code.message = readText(parser, "EventCode")

        return code
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readEventDecs(parser: XmlPullParser): EventDesc {
        val eventDesc = EventDesc()

        eventDesc.lang = readOneAttribute(parser, "xml:lang")
        eventDesc.message = readText(parser, "EventDesc")

        return eventDesc
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLocation(parser: XmlPullParser): Location {
        val location = Location()

        location.type = readOneAttribute(parser, "type")
        location.message = readText(parser, "Location")

        return location
    }

    private fun readOneAttribute(parser: XmlPullParser, attrName: String): String {
        var result = ""
        val attrCount = parser.attributeCount
        for (i in 0 until attrCount) {
            when (parser.getAttributeName(i)) {
                attrName -> result = parser.getAttributeValue(i)
                else -> {
                    // skip attribute
                }
            }
        }
        return result
    }

    private fun readText(parser: XmlPullParser, tagName: String): String {
        var result = ""
        while (parser.next() != XmlPullParser.END_DOCUMENT && parser.name != tagName) {
            if (parser.eventType == XmlPullParser.TEXT) {
                result = parser.text
            } else {
                XmlUtils.skip(parser)
            }
        }
        return result
    }
}
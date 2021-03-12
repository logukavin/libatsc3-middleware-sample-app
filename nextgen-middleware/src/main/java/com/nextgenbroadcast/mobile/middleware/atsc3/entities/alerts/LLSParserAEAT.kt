package com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts

import android.util.Log
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
    private fun readAEAText(parser: XmlPullParser): AeaText {
        val aeaText = AeaText()

        val attrCount = parser.attributeCount
        for (i in 0 until attrCount) {
            when (parser.getAttributeName(i)) {
                "xml:lang" -> aeaText.lang = parser.getAttributeValue(i)
                else -> {
                    // skip attribute
                }
            }
        }

        while (parser.next() != XmlPullParser.END_DOCUMENT && parser.name != "AEAText") {
            if (parser.eventType == XmlPullParser.TEXT) {
                aeaText.message = parser.text
            } else {
                XmlUtils.skip(parser)
            }
        }

        return aeaText
    }
}
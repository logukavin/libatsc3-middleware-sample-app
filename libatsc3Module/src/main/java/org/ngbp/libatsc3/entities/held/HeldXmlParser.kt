package org.ngbp.libatsc3.entities.held

import android.util.Log
import org.ngbp.libatsc3.utils.XmlUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

class HeldXmlParser {
    fun parseXML(xmlPayload: String): Held? {
        try {
            val parser = XmlUtils.newParser(xmlPayload)
            parser.nextTag()
            parser.require(XmlPullParser.START_TAG, null, "HELD")
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                val name = parser.name
                if (name == ENTRY_HELD) {
                    return readHeld(parser)
                } else {
                    XmlUtils.skip(parser)
                }
            }
        } catch (e: XmlPullParserException) {
            Log.e("HeldXmlParser", "exception in parsing: $e")
        } catch (e: IOException) {
            Log.e("HeldXmlParser", "exception in parsing: $e")
        }
        return null
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readHeld(parser: XmlPullParser): Held {
        parser.require(XmlPullParser.START_TAG, null, ENTRY_HELD)
        val held = Held()
        val attrCount = parser.attributeCount
        for (i in 0 until attrCount) {
            when (parser.getAttributeName(i)) {
                "appContextId" -> held.appContextId = parser.getAttributeValue(i)
                "appRendering" -> held.appRendering = XmlUtils.strToBool(parser.getAttributeValue(i))
                "bcastEntryPackageUrl" -> held.bcastEntryPackageUrl = parser.getAttributeValue(i)
                "bcastEntryPageUrl" -> held.bcastEntryPageUrl = parser.getAttributeValue(i)
                "coupledServices" -> held.coupledServices = XmlUtils.strToInt(parser.getAttributeValue(i))
                else -> {
                    // skip
                }
            }
        }
        return held
    }

    companion object {
        private const val ENTRY_HELD = "HTMLEntryPackage"
    }
}
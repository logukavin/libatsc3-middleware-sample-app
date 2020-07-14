package org.ngbp.libatsc3.entities.service

import android.util.Log
import org.ngbp.libatsc3.utils.XmlUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.*

class LLSParserSLT {
    fun parseXML(xmlPayload: String): ArrayList<Service> {
        val services = ArrayList<Service>()
        try {
            val parser = XmlUtils.newParser(xmlPayload)
            parser.nextTag()
            parser.require(XmlPullParser.START_TAG, null, "SLT")

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                val name = parser.name
                if (name == ENTRY_SERVICE) {
                    services.add(readService(parser))
                } else {
                    XmlUtils.skip(parser)
                }
            }
        } catch (e: XmlPullParserException) {
            Log.e("LLSParserSLT", "exception in parsing: $e")
        } catch (e: IOException) {
            Log.e("LLSParserSLT", "exception in parsing: $e")
        }
        return services
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readService(parser: XmlPullParser): Service {
        val currentService = Service()

        val attrCount = parser.attributeCount
        for (i in 0 until attrCount) {
            when (parser.getAttributeName(i)) {
                "serviceId" -> currentService.serviceId = XmlUtils.strToInt(parser.getAttributeValue(i))
                "globalServiceID" -> currentService.globalServiceId = parser.getAttributeValue(i)
                "majorChannelNo" -> currentService.majorChannelNo = XmlUtils.strToInt(parser.getAttributeValue(i))
                "minorChannelNo" -> currentService.minorChannelNo = XmlUtils.strToInt(parser.getAttributeValue(i))
                "shortServiceName" -> currentService.shortServiceName = parser.getAttributeValue(i)
                else -> {
                    // skip attribute
                }
            }
        }

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            val name = parser.name
            if (name == "BroadcastSvcSignaling") {
                currentService.broadcastSvcSignalingCollection.add(readBroadcastSvcSignaling(parser))
            } else {
                XmlUtils.skip(parser)
            }
        }

        return currentService
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readBroadcastSvcSignaling(parser: XmlPullParser): BroadcastSvcSignaling {
        val broadcastSvcSignaling = BroadcastSvcSignaling()

        val attrCount = parser.attributeCount
        for (i in 0 until attrCount) {
            when (parser.getAttributeName(i)) {
                "slsProtocol" -> broadcastSvcSignaling.slsProtocol = XmlUtils.strToInt(parser.getAttributeValue(i))
                "slsMajorProtocolVersion" -> broadcastSvcSignaling.slsMajorProtocolVersion = XmlUtils.strToInt(parser.getAttributeValue(i))
                "slsMinorProtocolVersion" -> broadcastSvcSignaling.slsMinorProtocolVersion = XmlUtils.strToInt(parser.getAttributeValue(i))
                "slsDestinationIpAddress" -> broadcastSvcSignaling.slsDestinationIpAddress = parser.getAttributeValue(i)
                "slsDestinationUdpPort" -> broadcastSvcSignaling.slsDestinationUdpPort = parser.getAttributeValue(i)
                "slsSourceIpAddress" -> broadcastSvcSignaling.slsSourceIpAddress = parser.getAttributeValue(i)
                else -> {
                    // skip attribute
                }
            }
        }

        return broadcastSvcSignaling
    }

    companion object {
        private const val ENTRY_SERVICE = "Service"
    }
}
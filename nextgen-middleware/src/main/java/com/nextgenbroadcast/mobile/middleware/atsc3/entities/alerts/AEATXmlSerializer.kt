package com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts

import android.util.Log
import android.util.Xml
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.entities.Aea
import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter


class AEATXmlSerializer {
    fun writeXml(aeaList: List<Aea>): String? {
        Log.d("RRR","writeXml")
        val serializer: XmlSerializer = Xml.newSerializer()
        val writer = StringWriter()
        val xml = try {
            serializer.setOutput(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "AEAT")
            serializer.attribute("", "number", aeaList.size.toString())
            for (aea in aeaList) {
                serializer.startTag("", "AEA")
                serializer.attribute("", "aeaId", aea.aeaId)
                serializer.attribute("", "aeaType", aea.aeaType)
                serializer.attribute("", "audience", aea.audience)
                serializer.attribute("", "issuer", aea.issuer)
                serializer.attribute("", "priority", aea.priority.toString())
                serializer.attribute("", "wakeup", aea.wakeup.toString())

                val header = aea.header
                if (header != null) {
                    serializer.startTag("", "Header")
                    serializer.attribute("", "effective", header.effective)
                    serializer.attribute("", "expires", header.expires)

                    //TODO: serialize EventCode, EventDesc, Location

                    serializer.endTag("","Header")
                }

                val listAeaText = aea.listAeaText
                if (listAeaText.isNotEmpty()) {
                    for (aeaText in listAeaText) {
                        serializer.startTag("", "AEAText")
                        serializer.attribute("", "xml:lang", aeaText.lang)
                        serializer.text(aeaText.message)
                        serializer.endTag("", "AEAText")
                    }
                }

                //TODO: serialize LiveMedia, Media

                serializer.endTag("", "AEA")
            }
            serializer.endTag("", "AEAT")
            serializer.endDocument()
            writer.toString()
        } catch (e: Exception) {
            Log.d("RRR","Exception $e")
            throw RuntimeException(e)
        }
        return xml
    }
}
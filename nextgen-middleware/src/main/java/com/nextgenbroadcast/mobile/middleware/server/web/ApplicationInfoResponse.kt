package com.nextgenbroadcast.mobile.middleware.server.web

import android.util.Xml
import java.io.StringWriter

data class ApplicationInfoResponse(
    val name: String = "ATSC",
    val optionsAllowStop: Boolean = false,
    val state: String = "running",
    val X_ATSC_App2AppURL: String = "",
    val X_ATSC_WSURL: String = "",
    val X_ATSC_UserAgent: String = "Value of ATSC UA header"
) {

    fun getXML(): String {
        val xmlSerializer = Xml.newSerializer()
        val stringWriter = StringWriter()
        xmlSerializer.setOutput(stringWriter)
        xmlSerializer.startDocument("UTF-8", null)

        xmlSerializer.startTag("", "service")
        xmlSerializer.attribute("", "xmlns", "urn:dialmultiscreenorg:schemas:dial")
        xmlSerializer.attribute("", "dialVer", "1.7")

        xmlSerializer.startTag("", "name")
        xmlSerializer.text(name)
        xmlSerializer.endTag("", "name")

        xmlSerializer.startTag("", "options")
        xmlSerializer.attribute("", "allowStop", optionsAllowStop.toString())
        xmlSerializer.endTag("", "options")

        xmlSerializer.startTag("", "state")
        xmlSerializer.text(state)
        xmlSerializer.endTag("", "state")

        xmlSerializer.startTag("", "additionalData")

        xmlSerializer.startTag("", "X_ATSC_App2AppURL")
        xmlSerializer.text(X_ATSC_App2AppURL)
        xmlSerializer.endTag("", "X_ATSC_App2AppURL")

        xmlSerializer.startTag("", "X_ATSC_WSURL")
        xmlSerializer.text(X_ATSC_WSURL)
        xmlSerializer.endTag("", "X_ATSC_WSURL")

        xmlSerializer.startTag("", "X_ATSC_UserAgent")
        xmlSerializer.text(X_ATSC_UserAgent)
        xmlSerializer.endTag("", "X_ATSC_UserAgent")

        xmlSerializer.endTag("", "additionalData")
        xmlSerializer.endTag("", "service")

        xmlSerializer.endDocument()

        return stringWriter.toString()
    }

}
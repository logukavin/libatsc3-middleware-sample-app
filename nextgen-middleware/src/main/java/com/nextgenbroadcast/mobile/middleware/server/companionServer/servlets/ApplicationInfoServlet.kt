package com.nextgenbroadcast.mobile.middleware.server.companionServer.servlets

import android.util.Xml
import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter
import java.net.HttpURLConnection
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ApplicationInfoServlet : HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.status = HttpURLConnection.HTTP_OK
        resp.writer.println(getXML("ATSC", false, "running", "", "", ""))
    }

    private fun getXML(name: String,
                       optionsAllowStop: Boolean,
                       state: String,
                       appToAppUrl: String,
                       wsUrl: String,
                       userAgent: String): String {
        val xmlSerializer = Xml.newSerializer()
        val stringWriter = StringWriter()
        with(xmlSerializer) {
            setOutput(stringWriter)
            startDocument("UTF-8", null)

            startTagWithEmptyNameSpace("service")
            attribute("", "xmlns", "urn:dialmultiscreenorg:schemas:dial")
            attribute("", "dialVer", "1.7")

            startTagWithEmptyNameSpace("name")
            text(name)
            endTagWithEmptyNameSpace("name")

            startTagWithEmptyNameSpace("options")
            attribute("", "allowStop", optionsAllowStop.toString())
            endTagWithEmptyNameSpace("options")

            startTagWithEmptyNameSpace("state")
            text(state)
            endTagWithEmptyNameSpace("state")

            startTagWithEmptyNameSpace("additionalData")

            startTagWithEmptyNameSpace("X_ATSC_App2AppURL")
            text(appToAppUrl)
            endTagWithEmptyNameSpace("X_ATSC_App2AppURL")

            startTagWithEmptyNameSpace("X_ATSC_WSURL")
            text(wsUrl)
            endTagWithEmptyNameSpace("X_ATSC_WSURL")

            startTagWithEmptyNameSpace("X_ATSC_UserAgent")
            text(userAgent)
            endTagWithEmptyNameSpace("X_ATSC_UserAgent")

            endTagWithEmptyNameSpace("additionalData")
            endTagWithEmptyNameSpace("service")

            xmlSerializer.endDocument()
        }

        return stringWriter.toString()
    }

    private fun XmlSerializer.startTagWithEmptyNameSpace(tagName: String) {
        this.startTag("", tagName)
    }

    private fun XmlSerializer.endTagWithEmptyNameSpace(tagName: String) {
        this.endTag("", tagName)
    }
}
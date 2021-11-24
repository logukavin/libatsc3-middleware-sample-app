package com.nextgenbroadcast.mobile.middleware.server.servlets

import android.util.Xml
import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter
import java.net.HttpURLConnection
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CDApplicationInfoServlet : HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.status = HttpURLConnection.HTTP_OK
        resp.contentType = "text/xml"
        val applicationInfoXml = getXML("ATSC", false, ApplicationState.RUNNING, "", "", "")
        resp.writer.println(applicationInfoXml)
    }

    private fun getXML(name: String, optionsAllowStop: Boolean, state: ApplicationState, appToAppUrl: String, wsUrl: String, userAgent: String): String {
        val xmlSerializer = Xml.newSerializer()
        val stringWriter = StringWriter()

        with(xmlSerializer) {
            setOutput(stringWriter)
            startDocument("UTF-8", null)

            addTag("service") {
                addTag("name") {
                    text(name)
                }
                addTag("options") {
                    attribute("", "allowStop", optionsAllowStop.toString())
                }
                addTag("state") {
                    text(state.name)
                }
                addTag("additionalData") {
                    addTag("X_ATSC_App2AppURL") {
                        text(appToAppUrl)
                    }
                    addTag("X_ATSC_WSURL") {
                        text(wsUrl)
                    }
                    addTag("X_ATSC_UserAgent") {
                        text(userAgent)
                    }
                }
            }

            xmlSerializer.endDocument()
        }

        return stringWriter.toString()
    }

    private fun XmlSerializer.addTag(name: String, block: () -> Unit) {
        startTag("", name)
        block()
        endTag("", name)
    }

    enum class ApplicationState {
        RUNNING;

        override fun toString(): String {
            return name.lowercase()
        }
    }
}
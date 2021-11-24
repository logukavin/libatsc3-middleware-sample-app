package com.nextgenbroadcast.mobile.middleware.server.servlets

import java.net.HttpURLConnection
import java.util.*
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CDDescriptionServlet(
    private val applicationUrl: String
) : HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        with(resp) {
            status = HttpURLConnection.HTTP_OK
            contentType = "text/xml; charset=utf-8"
            setHeader("CONTENT-LANGUAGE", Locale.ENGLISH.language)
            setHeader("Application-URL", applicationUrl)
            setHeader("Access-Control-Allow-Origin", "*")
        }
    }
}
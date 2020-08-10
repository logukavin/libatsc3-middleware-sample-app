package com.nextgenbroadcast.mobile.middleware.server

import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class MiddlewareWebServerTestServlet : HttpServlet() {
    @Throws(ServletException::class, IOException::class)
    override fun doGet(request: HttpServletRequest,
                       response: HttpServletResponse) {
        response.contentType = "text/html"
        response.status = HttpServletResponse.SC_OK
        response.writer.print(serverMessage)
    }

    companion object {
        private const val serialVersionUID = -6154475799000019575L
        const val serverMessage = "Hello World"
    }
}
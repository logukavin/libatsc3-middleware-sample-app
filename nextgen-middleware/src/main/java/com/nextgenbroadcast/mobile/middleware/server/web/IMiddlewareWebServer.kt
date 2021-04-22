package com.nextgenbroadcast.mobile.middleware.server.web

import com.nextgenbroadcast.mobile.middleware.gateway.web.ConnectionType
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

interface IMiddlewareWebServer {
    fun addConnection(type: ConnectionType, host: String, port: Int)
    fun addHandler(path: String, onGet: (req: HttpServletRequest, resp: HttpServletResponse) -> Unit): Boolean
    fun removeHandler(path: String)
}
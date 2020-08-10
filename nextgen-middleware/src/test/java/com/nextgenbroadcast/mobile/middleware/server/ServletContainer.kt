package com.nextgenbroadcast.mobile.middleware.server

import javax.servlet.http.HttpServlet

data class ServletContainer(val servlet: HttpServlet, val path: String)
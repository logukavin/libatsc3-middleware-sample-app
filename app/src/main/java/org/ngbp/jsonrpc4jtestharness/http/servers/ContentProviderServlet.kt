package org.ngbp.jsonrpc4jtestharness.http.servers

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ContentProviderServlet internal constructor(context: Context) : HttpServlet() {
    private val content: String?

    @Throws(IOException::class)
    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        val out = response.getWriter()
        out.println(content)
    }

    init {

        //        Read web content from assets folder
        val sb = StringBuilder()
        try {
            var content: String?
            val `is` = context.getAssets().open("GitHub.htm")
            val br = BufferedReader(InputStreamReader(`is`, StandardCharsets.UTF_8))
            while (br.readLine().also { content = it } != null) {
                sb.append(content)
            }
            br.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        content = sb.toString()
    }
}
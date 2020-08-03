package org.ngbp.jsonrpc4jtestharness.view

import android.webkit.WebView

object BANavController {

    fun navigateUp(webView: WebView) = execute(webView, JS_NAVIGATE_UP)
    fun navigateDown(webView: WebView) = execute(webView, JS_NAVIGATE_DOWN)
    fun navigateNext(webView: WebView) = execute(webView, JS_NAVIGATE_NEXT)
    fun navigateBack(webView: WebView) = execute(webView, JS_NAVIGATE_BACK)
    fun navigateExit(webView: WebView) = execute(webView, JS_NAVIGATE_EXIT)
    fun navigateEnter(webView: WebView) = execute(webView, JS_NAVIGATE_ENTER)

    private fun execute(webView: WebView, script: String) {
        webView.evaluateJavascript(script, null)
    }

    private const val JS_ENTRY_POINT = "__ANDROID_BRIDGE"

    private const val JS_NAVIGATE_UP = "$JS_ENTRY_POINT.up();"
    private const val JS_NAVIGATE_DOWN = "$JS_ENTRY_POINT.down();"
    private const val JS_NAVIGATE_NEXT = "$JS_ENTRY_POINT.next();"
    private const val JS_NAVIGATE_BACK = "$JS_ENTRY_POINT.back();"
    private const val JS_NAVIGATE_EXIT = "$JS_ENTRY_POINT.exit();"
    private const val JS_NAVIGATE_ENTER = "$JS_ENTRY_POINT.enter();"
}
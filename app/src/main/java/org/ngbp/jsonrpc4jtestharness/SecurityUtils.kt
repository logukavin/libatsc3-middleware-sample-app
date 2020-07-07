package org.ngbp.jsonrpc4jtestharness

import android.content.Context
import android.net.http.SslError
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

object SecurityUtils {

    private var certificates: Array<X509Certificate?>? = null
    private var privateKey: PrivateKey? = null

    private fun loadCertificateAndPrivateKey(context: Context) {
        try {
            val certificateFileStream = context.resources.openRawResource(R.raw.mykey)
            val keyStore = KeyStore.getInstance("PKCS12")
            val password = "MY_PASSWORD"
            keyStore.load(certificateFileStream, password.toCharArray())
            val aliases = keyStore.aliases()
            val alias = aliases.nextElement()
            val key = keyStore.getKey(alias, password.toCharArray())
            if (key is PrivateKey) {
                privateKey = key
                val cert = keyStore.getCertificate(alias)
                certificates = arrayOfNulls(1)
                certificates?.let{
                    it[0] = cert as X509Certificate
                }
            }
            certificateFileStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val trustedWebViewClient: WebViewClient = object : WebViewClient() {
        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.proceed()
        }

        override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
            if (certificates == null || privateKey == null) {
                loadCertificateAndPrivateKey(view.context)
            }
            request.proceed(privateKey, certificates)
        }
    }

}
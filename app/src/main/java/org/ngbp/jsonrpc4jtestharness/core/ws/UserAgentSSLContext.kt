package org.ngbp.jsonrpc4jtestharness.core.ws

import android.content.Context
import org.ngbp.jsonrpc4jtestharness.R
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class UserAgentSSLContext(private val context: Context) : IUserAgentSSLContext {

    @Throws(GeneralSecurityException::class, IOException::class)
    override fun getInitializedSSLContext(password: String): SSLContext {
        val inputStream = context.resources.openRawResource(R.raw.mykey)
        var keystore: KeyStore? = null
        inputStream.use {
            keystore = KeyStore.getInstance("PKCS12")
            keystore?.run { load(it, password.toCharArray()) }
        }
        val keyManagerFactory = KeyManagerFactory.getInstance("X509")
        keyManagerFactory.init(keystore, password.toCharArray())
        val tmf = TrustManagerFactory.getInstance("X509")
        tmf.init(keystore)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, tmf.trustManagers, null)
        return sslContext
    }
}
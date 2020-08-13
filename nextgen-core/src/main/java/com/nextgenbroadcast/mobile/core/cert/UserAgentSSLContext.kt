package com.nextgenbroadcast.mobile.core.cert

import android.content.Context
import java.io.IOException
import java.security.GeneralSecurityException
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class UserAgentSSLContext(private val context: Context) : IUserAgentSSLContext {
    @Throws(GeneralSecurityException::class, IOException::class)
    override fun getInitializedSSLContext(password: String): SSLContext {
        val keystore = CertificateUtils.loadKeystore(context, password)
        val keyManagerFactory = KeyManagerFactory.getInstance(CertificateUtils.KEY_MANAGER_ALGORITHM).apply {
            init(keystore, password.toCharArray())
        }
        val tmf = TrustManagerFactory.getInstance(CertificateUtils.KEY_MANAGER_ALGORITHM).apply {
            init(keystore)
        }

        return SSLContext.getInstance("TLS").apply {
            init(keyManagerFactory.keyManagers, tmf.trustManagers, null)
        }
    }
}
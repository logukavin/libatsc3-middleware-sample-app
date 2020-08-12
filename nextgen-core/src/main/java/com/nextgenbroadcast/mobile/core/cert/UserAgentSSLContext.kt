package com.nextgenbroadcast.mobile.core.cert

import android.content.Context
import com.nextgenbroadcast.mobile.core.R
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class UserAgentSSLContext(private val inputStream: InputStream?) : IUserAgentSSLContext {
    @Throws(GeneralSecurityException::class, IOException::class)
    override fun getInitializedSSLContext(password: String): SSLContext {
        val keystore = CertificateUtils.loadKeystore(inputStream, password)
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
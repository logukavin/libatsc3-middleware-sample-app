package com.nextgenbroadcast.mobile.core.cert

import android.content.Context
import com.nextgenbroadcast.mobile.core.R
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class UserAgentSSLContext(
        private val context: Context
) : IUserAgentSSLContext {

    private fun openCertStream(): InputStream {
        return context.resources.openRawResource(R.raw.mykey)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    override fun getInitializedSSLContext(password: String): SSLContext {
        val keystore = CertificateUtils.loadKeystore(openCertStream(), password)
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

    fun loadCertificateAndPrivateKey(password: String = "MY_PASSWORD"): Pair<PrivateKey, X509Certificate>? {
        return CertificateUtils.loadCertificateAndPrivateKey(openCertStream(), password)
    }
}
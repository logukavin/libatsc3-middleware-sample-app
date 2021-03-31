package com.nextgenbroadcast.mobile.core.cert

import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

object CertificateUtils {
    @JvmStatic
    val KEY_MANAGER_ALGORITHM = "X509"
    const val KEY_STORE_TYPE = "PKCS12"

    fun loadKeystore(stream: InputStream, password: String): KeyStore {
        stream.use { inputStream ->
            return KeyStore.getInstance(KEY_STORE_TYPE).also {
                it.load(inputStream, password.toCharArray())
            }
        }
    }

    fun loadCertificateAndPrivateKey(stream: InputStream, password: String): Pair<PrivateKey, X509Certificate>? {
        try {
            val keyStore = loadKeystore(stream, password)
            val alias = keyStore.aliases().nextElement()
            val key = keyStore.getKey(alias, password.toCharArray())
            if (key is PrivateKey) {
                val cert = keyStore.getCertificate(alias)
                if (cert is X509Certificate) {
                    return Pair(key, cert)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
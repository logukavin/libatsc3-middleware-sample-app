package com.nextgenbroadcast.mobile.core.cert

import android.content.Context
import com.nextgenbroadcast.mobile.core.R
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

object CertificateUtils {
    const val KEY_MANAGER_ALGORITHM = "X509"
    const val KEY_STORE_TYPE = "PKCS12"

    fun loadKeystore(context: Context, password: String): KeyStore {
        context.resources.openRawResource(R.raw.mykey).use { inputStream ->
            return KeyStore.getInstance(KEY_STORE_TYPE).also {
                it.load(inputStream, password.toCharArray())
            }
        }
    }

    fun loadCertificateAndPrivateKey(context: Context, password: String = "MY_PASSWORD"): Pair<PrivateKey, X509Certificate>? {
        try {
            val keyStore = loadKeystore(context, password)
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
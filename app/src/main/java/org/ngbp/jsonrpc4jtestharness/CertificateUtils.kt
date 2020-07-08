package org.ngbp.jsonrpc4jtestharness

import android.content.Context
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

object CertificateUtils {

    var certificates: Array<X509Certificate?>? = null
    var privateKey: PrivateKey? = null

    fun loadCertificateAndPrivateKey(context: Context) {
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
}
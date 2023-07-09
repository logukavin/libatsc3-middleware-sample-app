package com.nextgenbroadcast.mobile.middleware.dev.telemetry

import android.content.Context
import com.nextgenbroadcast.mobile.core.cert.CertificateUtils

object CertificateStore {
    fun getReceiverCert(context: Context, keyPassword: String) = CertificateUtils.createKeyStore(
        context.assets.open("0d76690b26ece2cb93e90b2fd3d5e4605fae83b1ff76a3aa2f55db8284e2bba4-certificate.pem.crt"),
        context.assets.open("0d76690b26ece2cb93e90b2fd3d5e4605fae83b1ff76a3aa2f55db8284e2bba4-private.pem.key"),
        keyPassword
    )

    //libatsc3_manager_provisioning
    fun getManagerCert(context: Context, keyPassword: String) = CertificateUtils.createKeyStore(
        context.assets.open("30028412ef206c96b3169e0321db7746bfdec41ed4c321cf97c998449cf288bf-certificate.pem.crt"),
        context.assets.open("30028412ef206c96b3169e0321db7746bfdec41ed4c321cf97c998449cf288bf-private.pem.key"),
        keyPassword
    )
}
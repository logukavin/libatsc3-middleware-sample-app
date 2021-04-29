package com.nextgenbroadcast.mobile.middleware.atsc3.source

import com.nextgenbroadcast.mobile.core.LOG
import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase
import org.ngbp.libatsc3.middleware.android.phy.SaankhyaPHYAndroid
import java.lang.IllegalStateException

abstract class Atsc3Source : IAtsc3Source {
    private var atsc3NdkPHYClientInstance: Atsc3NdkPHYClientBase? = null

    @Synchronized
    override fun open(): Int {
        if (atsc3NdkPHYClientInstance != null) throw IllegalStateException("Source already opened")

        atsc3NdkPHYClientInstance = openPhyClient()

        return if (atsc3NdkPHYClientInstance != null) IAtsc3Source.RESULT_OK else IAtsc3Source.RESULT_ERROR
    }

    @Synchronized
    override fun close() {
        atsc3NdkPHYClientInstance?.let { client ->
            closePhyClient(client)
            atsc3NdkPHYClientInstance = null
        }
    }

    @Synchronized
    override fun stop() {
        atsc3NdkPHYClientInstance?.stop()
    }

    @Synchronized
    fun tune(freqKhz: Int) {
        atsc3NdkPHYClientInstance?.tune(freqKhz, 0)
    }

    protected abstract fun openPhyClient(): Atsc3NdkPHYClientBase?

    protected open fun closePhyClient(client: Atsc3NdkPHYClientBase) {
        LOG.d(TAG, "close source -- calling client.deinit")

        client.deinit()
    }

    companion object {
        private val TAG: String = Atsc3Source::class.java.simpleName

        const val DEVICE_TYPE_AUTO = 0 // use for SaankhyaPHYAndroid.DEVICE_TYPE_MARKONE
        const val DEVICE_TYPE_KAILASH = SaankhyaPHYAndroid.DEVICE_TYPE_FX3_KAILASH
        const val DEVICE_TYPE_YOGA = SaankhyaPHYAndroid.DEVICE_TYPE_FX3_YOGA

        fun isSaankhyaDevice(vendorId: Int, productId: Int): Boolean {
            return vendorId == SaankhyaPHYAndroid.CYPRESS_VENDOR_ID && productId == SaankhyaPHYAndroid.KAILASH_PRODUCT_ID
        }
    }
}
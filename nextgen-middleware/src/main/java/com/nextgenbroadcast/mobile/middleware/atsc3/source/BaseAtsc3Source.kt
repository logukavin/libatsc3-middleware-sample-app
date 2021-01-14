package com.nextgenbroadcast.mobile.middleware.atsc3.source

import android.util.Log
import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase
import java.lang.IllegalStateException

abstract class BaseAtsc3Source : IAtsc3Source {
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
        Log.d(TAG, "close source -- calling client.deinit")

        client.deinit()
    }

    companion object {
        private val TAG: String = BaseAtsc3Source::class.java.simpleName
    }
}
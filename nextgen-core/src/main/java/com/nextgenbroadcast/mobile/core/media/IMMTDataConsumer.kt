package com.nextgenbroadcast.mobile.core.media

import java.io.IOException

interface IMMTDataConsumer<P, F> {
    @Throws(IOException::class)
    fun open()
    fun release()
    fun isActive(): Boolean

    fun InitMpuMetadata_HEVC_NAL_Payload(payload: P)
    fun PushMfuByteBufferFragment(mfuByteBufferFragment: F)
}
package com.nextgenbroadcast.mobile.core.media

interface IMMTDataConsumer<P, F> {
    fun open()
    fun release()
    fun isActive(): Boolean

    fun InitMpuMetadata_HEVC_NAL_Payload(payload: P)
    fun PushMfuByteBufferFragment(mfuByteBufferFragment: F)
}
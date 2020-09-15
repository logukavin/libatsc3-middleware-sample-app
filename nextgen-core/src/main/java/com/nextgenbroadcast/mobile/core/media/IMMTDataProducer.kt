package com.nextgenbroadcast.mobile.core.media

interface IMMTDataProducer<P, F> {
    fun setMMTSource(source: IMMTDataConsumer<P, F>)
    fun resetMMTSource(source: IMMTDataConsumer<P, F>)
}
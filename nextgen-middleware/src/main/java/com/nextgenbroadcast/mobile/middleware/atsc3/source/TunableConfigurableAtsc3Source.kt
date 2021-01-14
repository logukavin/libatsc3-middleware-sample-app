package com.nextgenbroadcast.mobile.middleware.atsc3.source

abstract class TunableConfigurableAtsc3Source : ConfigurableAtsc3Source<Int>(emptyList()), ITunableSource {
    override fun applyConfig(config: Int): Int {
        tune(getConfig(config))
        return config
    }
}
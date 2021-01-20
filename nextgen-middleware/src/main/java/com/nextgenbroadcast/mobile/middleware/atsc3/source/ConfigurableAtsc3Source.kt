package com.nextgenbroadcast.mobile.middleware.atsc3.source

import java.util.*
import kotlin.collections.ArrayList

abstract class ConfigurableAtsc3Source<T>(
        configs: List<T>
): BaseAtsc3Source() {
    private val configList = ArrayList(configs)
    private var configIndex: Int = configs.size - 1
    private var scanning: Boolean = configs.size > 1

    fun configure(configIndex: Int): Int {
        if (configList.isEmpty() || configIndex >= configList.size) return IAtsc3Source.RESULT_ERROR

        if (configIndex >= 0) {
            scanning = false
            this.configIndex = configIndex
        }

        var result = applyConfig(this.configIndex)
        if (result != IAtsc3Source.RESULT_ERROR && this.configIndex >= 0) {
            result = this.configIndex

            if (scanning) {
                if (this.configIndex == 0) {
                    scanning = false
                } else {
                    this.configIndex--
                }
            }
        }

        return result
    }

    fun getAllConfigs(): List<T> {
        return Collections.unmodifiableList(configList)
    }

    fun setConfigs(configs: List<T>) {
        configList.clear()
        configList.addAll(configs)
        configIndex = configs.size - 1
        scanning = configIndex > 0
    }

    fun getConfig(configIndex: Int): T {
        return configList[configIndex]
    }

    fun getConfigIndex() = configIndex

    protected abstract fun applyConfig(configIndex: Int): Int
}
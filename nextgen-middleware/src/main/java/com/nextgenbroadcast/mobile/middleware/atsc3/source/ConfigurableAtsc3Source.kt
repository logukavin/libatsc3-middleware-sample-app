package com.nextgenbroadcast.mobile.middleware.atsc3.source

import java.util.*
import kotlin.collections.ArrayList

abstract class ConfigurableAtsc3Source<T>(
        configs: List<T>
): BaseAtsc3Source() {
    private val configList = ArrayList(configs)
    private var configIndex: Int = configs.size - 1
    private var scanning: Boolean = configs.size > 1

    override fun open(): Int {
        var result = super.open()

        if (result != IAtsc3Source.RESULT_ERROR && configIndex >= 0) {
            result = configIndex

            if (scanning) {
                if (configIndex == 0) {
                    scanning = false
                } else {
                    configIndex--
                }
            }
        }

        return result
    }

    fun configure(config: Int): Int {
        if (configList.isEmpty() || config >= configList.size) return IAtsc3Source.RESULT_ERROR

        if (config >= 0) {
            scanning = false
            configIndex = config
        }

        return applyConfig(configIndex)
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

    fun getConfig(index: Int): T {
        return configList[index]
    }

    fun getConfigIndex() = configIndex

    protected abstract fun applyConfig(config: Int): Int
}
package com.nextgenbroadcast.mobile.middleware.service.init

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils
import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase
import org.xmlpull.v1.XmlPullParser

internal class PhyInitializer : IServiceInitializer {

    private var isActive = true

    override fun initialize(context: Context, components: HashMap<Class<*>, Pair<Int, String>>): Boolean {
        components.filter { (clazz, _) ->
            Atsc3NdkPHYClientBase::class.java.isAssignableFrom(clazz)
        }.map { (clazz, data) ->
            val (resource) = data
            Pair(clazz as Class<out Atsc3NdkPHYClientBase>, resource)
        }.forEach { (component, resource) ->
            try {
                val parser = context.resources.getXml(resource)
                readPhyAttributes(parser).forEach { (fd, devicePath, freqKhz) ->
                    if (initializePHY(component, fd, devicePath, freqKhz)) {
                        return true
                    }
                }
            } catch (e: Resources.NotFoundException) {
                e.printStackTrace()
            }

            if (!isActive) return@forEach
        }

        return false
    }

    override fun cancel() {
        isActive = false
    }

    private fun readPhyAttributes(parser: XmlResourceParser): List<Triple<Int, String?, Int>> {
        val result = ArrayList<Triple<Int, String?, Int>>()

        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }

                if (parser.name == "resources") {
                    while (parser.next() != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType != XmlPullParser.START_TAG) {
                            continue
                        }

                        if (parser.name == "phy-device") {
                            var fd = -1
                            var path: String? = null
                            var freqKhz = -1

                            for (i in 0 until parser.attributeCount) {
                                when (parser.getAttributeName(i)) {
                                    "fd" -> fd = parser.getAttributeIntValue(i, -1)
                                    "path" -> path = parser.getAttributeValue(i)
                                    "freqKhz" -> freqKhz = parser.getAttributeIntValue(i, -1)
                                }
                            }

                            result.add(Triple(fd, path, freqKhz))
                        } else {
                            XmlUtils.skip(parser)
                        }
                    }
                } else {
                    XmlUtils.skip(parser)
                }
            }
        } finally {
            parser.close()
            return result
        }
    }

    private fun initializePHY(component: Class<out Atsc3NdkPHYClientBase>, fd: Int, devicePath: String?, freqKhz: Int): Boolean {
        try {
            val instance: Any = component.getDeclaredConstructor().newInstance()
            val initializer = instance as Atsc3NdkPHYClientBase

            if (initializer.init() == 0) {
                if (initializer.open(fd, devicePath) == 0) {
                    if (freqKhz > 0) {
                        initializer.tune(freqKhz, 0)
                    }

                    return true
                }
            }
        } catch (t: Throwable) {
            //throw StartupException(throwable)
            t.printStackTrace()
        }

        return false
    }
}
package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide

import android.util.Log
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGContent
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.*
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.*
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.TimeUtils
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException

internal class SGDUReader(
        private val services: MutableMap<String, SGService>,
        private val contents: MutableMap<String, SGContent>
) {

    fun readFromFile(file: File, isActive: () -> Boolean, bsid: Int) {
        val fileName = file.name

        SGDUFile.open(file).use { reader ->
            var index = 0;
            while (isActive() && reader.hasNext()) {
                reader.next()?.let { (fragmentType, xml) ->
                    when (fragmentType) {
                        SGFragmentType.SERVICE -> parseServiceXML(xml)?.let { service ->
                            service.bsid = bsid
                            service.duFileName = fileName
                            service.duIndex = index
                        }

                        SGFragmentType.SCHEDULE -> parseScheduleXML(xml)?.let { schedule ->
                            schedule.duFileName = fileName
                            schedule.duIndex = index
                        }

                        SGFragmentType.CONTENT -> {
                            val content = parseContentXML(xml).apply {
                                duFileName = fileName
                                duIndex = index
                            }
                            content.id?.let { contentId ->
                                contents[contentId] = content
                            }
                        }

                        else -> {
                            // ignore
                        }
                    }
                }

                index++
            }
        }
    }

    private fun getOrCreateService(serviceId: String): SGService {
        return services[serviceId] ?: SGService(serviceId).also {
            services[serviceId] = it
        }
    }

    private fun parseServiceXML(xmlPayload: String): SGService? {
        try {
            val parser = XmlUtils.newParser(xmlPayload)

            parser.nextTag()
            parser.require(XmlPullParser.START_TAG, null, "Service")

            var serviceId: String = ""
            var globalServiceId: String? = null
            var version: Long = 0

            parser.iterateAttrs { name, value ->
                // serviceId = XmlUtils.strToInt(value)
                when (name) {
                    "id" -> serviceId = value
                    "globalServiceID" -> globalServiceId = value
                    "version" -> version = XmlUtils.strToLong(value)
                }
            }

            val service = getOrCreateService(serviceId)

            if (service.version > version) {
                return service
            } else {
                service.version = version
            }

            if (service.globalServiceId != globalServiceId) {
                service.globalServiceId = globalServiceId
            }

            parser.iterateDocument { tagName ->
                when (tagName) {
                    "Name" -> readTextAttr(parser) {
                        service.shortServiceName = it
                    }

                    "PrivateExt" -> parser.iterateSubTags { name ->
                        if (name == "sa:ATSC3ServiceExtension") {
                            parser.iterateSubTags { textName ->
                                when (textName) {
                                    "sa:MajorChannelNum" -> parser.readTextTag()?.let { text ->
                                        service.majorChannelNo = XmlUtils.strToInt(text)
                                    }
                                    "sa:MinorChannelNum" -> parser.readTextTag()?.let { text ->
                                        service.minorChannelNo = XmlUtils.strToInt(text)
                                    }
                                    else -> parser.skipTag()
                                }
                            }
                        } else {
                            parser.skipTag()
                        }
                    }

                    else -> parser.skipTag()
                }
            }

            return service
        } catch (e: XmlPullParserException) {
            Log.e(SGDDReader.TAG, "exception in parsing: $e")
        } catch (e: IOException) {
            Log.e(SGDDReader.TAG, "exception in parsing: $e")
        }
        return null
    }

    private fun parseScheduleXML(xmlPayload: String): SGSchedule? {
        try {
            val parser = XmlUtils.newParser(xmlPayload)

            parser.nextTag()
            parser.require(XmlPullParser.START_TAG, null, "Schedule")

            var scheduleId: String? = null
            var scheduleVersion: Long = 0

            parser.iterateAttrs { name, value ->
                when (name) {
                    "id" -> scheduleId = value
                    "version" -> scheduleVersion = XmlUtils.strToLong(value)
                }
            }

            if (scheduleId == null) return null

            var schedule: SGSchedule? = null

            parser.iterateDocument { tagName ->
                when (tagName) {
                    "ServiceReference" -> readIdRefTag(parser) { serviceId ->
                        schedule = getOrCreateService(serviceId).let { service ->
                            service.scheduleMap?.get(scheduleId)
                                    ?: SGSchedule(scheduleId, serviceId, scheduleVersion).also {
                                        service.addSchedule(it)
                                    }
                        }
                    }

                    "ContentReference" -> SGScheduleContent().apply {
                        parser.iterateAttrs { name, value ->
                            if (name == "idRef") {
                                contentId = value
                            }
                        }.iterateSubTags { name ->
                            if (name == "PresentationWindow") {
                                var startTime = 0L
                                var endTime = 0L
                                var duration = 0
                                parser.iterateAttrs { attrName, value ->
                                    when (attrName) {
                                        "startTime" -> startTime = XmlUtils.strToLong(value)
                                        "endTime" -> endTime = XmlUtils.strToLong(value)
                                        "duration" -> duration = XmlUtils.strToInt(value)
                                    }
                                }.skipSubTags()

                                if (startTime > 0) startTime = TimeUtils.ntpSecondsToUtc(startTime)
                                if (endTime > 0) endTime = TimeUtils.ntpSecondsToUtc(endTime)

                                addPresentation(startTime, endTime, duration)
                            } else {
                                parser.skipTag()
                            }
                        }
                    }.also {
                        schedule?.addContent(it)
                    }
                }
            }

            return schedule
        } catch (e: XmlPullParserException) {
            Log.e(SGDDReader.TAG, "exception in parsing: $e")
        } catch (e: IOException) {
            Log.e(SGDDReader.TAG, "exception in parsing: $e")
        }

        return null
    }

    private fun parseContentXML(xmlPayload: String): SGContent {
        return SGContent().apply {
            try {
                val parser = XmlUtils.newParser(xmlPayload)

                parser.nextTag()
                parser.require(XmlPullParser.START_TAG, null, "Content")

                parser.iterateAttrs { name, value ->
                    when (name) {
                        "id" -> id = value
                        "version" -> version = XmlUtils.strToLong(value)
                    }
                }

                parser.iterateDocument { tagName ->
                    when (tagName) {
                        "ServiceReference" -> readIdRefTag(parser) {
                            addServiceId(it)
                        }

                        "Name" -> readLangStringAttr(parser) { text, lang ->
                            addName(text, lang)
                        }

                        "Description" -> readLangStringAttr(parser) { text, lang ->
                            addDescription(text, lang)
                        }

                        "PrivateExt" -> parser.iterateSubTags { name ->
                            when (name) {
                                "sa:ContentIcon" -> parser.readTextTag()?.let { text -> icon = text }
                                else -> parser.skipTag()
                            }
                        }

                        else -> parser.skipTag()
                    }
                }

            } catch (e: XmlPullParserException) {
                Log.e(SGDDReader.TAG, "exception in parsing: $e")
            } catch (e: IOException) {
                Log.e(SGDDReader.TAG, "exception in parsing: $e")
            }
        }
    }

    private fun readTextAttr(parser: XmlPullParser, action: (String) -> Unit) {
        parser.iterateAttrs { name, value ->
            if (name == "text") {
                action(value)
            }
        }.skipSubTags()
    }

    private fun readLangStringAttr(parser: XmlPullParser, action: (text: String, lang: String) -> Unit) {
        var text: String? = null
        var lang: String? = null
        parser.iterateAttrs { name, value ->
            when (name) {
                "text" -> text = value
                "xml:lang" -> lang = value
            }
        }.skipSubTags()

        text?.let { txt ->
            lang?.let { lng ->
                action(txt, lng)
            }
        }
    }

    /*
    jjustman-2022-07-25 - fix

2022-07-25 00:35:04.521 26889-27576/com.nextgenbroadcast.mobile.middleware.sample W/System.err: java.lang.NumberFormatException: For input string: "bcast://trivenidigital.com/Service-8"
2022-07-25 00:35:04.521 26889-27576/com.nextgenbroadcast.mobile.middleware.sample W/System.err:     at java.lang.Integer.parseInt(Integer.java:747)
2022-07-25 00:35:04.521 26889-27576/com.nextgenbroadcast.mobile.middleware.sample W/System.err:     at java.lang.Integer.parseInt(Integer.java:865)
2022-07-25 00:35:04.521 26889-27576/com.nextgenbroadcast.mobile.middleware.sample W/System.err:     at com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils.strToInt(XmlUtils.kt:39)
2022-07-25 00:35:04.521 26889-27576/com.nextgenbroadcast.mobile.middleware.sample W/System.err:     at com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils.strToInt$default(XmlUtils.kt:37)
2022-07-25 00:35:04.521 26889-27576/com.nextgenbroadcast.mobile.middleware.sample W/System.err:     at com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGDUReader.readIdRefTag(SGDUReader.kt:283)
2022-07-25 00:35:04.522 26889-27576/com.nextgenbroadcast.mobile.middleware.sample W/System.err:     at com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGDUReader.parseContentXML(SGDUReader.kt:224)
2022-07-25 00:35:04.522 26889-27576/com.nextgenbroadcast.mobile.middleware.sample W/System.err:     at com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGDUReader.readFromFile(SGDUReader.kt:39)
2022-07-25 00:35:04.522 26889-27576/com.nextgenbroadcast.mobile.middleware.sample W/System.err:     at com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.ServiceGuideDeliveryUnitReader$readDeliveryUnit$1.invokeSuspend(ServiceGuideDeliveryUnitReader.kt:40)
2022-07-25 00:35:04.522 26889-27576/com.nextgenbroadcast.mobile.middleware.sample W/System.err:     at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
     */

    private fun readIdRefTag(parser: XmlPullParser, action: (String) -> Unit) {
        parser.iterateAttrs { name, value ->
            if (name == "idRef") {
                //XmlUtils.strToInt(value);
                action(value);
            }
        }.skipSubTags()
    }
}
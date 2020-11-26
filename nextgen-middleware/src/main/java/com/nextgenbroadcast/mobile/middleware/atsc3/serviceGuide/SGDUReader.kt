package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide

import android.util.Log
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.TimeUtils
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGContentImpl
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.*
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils.iterateAttrs
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils.iterateDocument
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils.iterateSubTags
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils.readTextTag
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils.skipSubTags
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils.skipTag
import okio.ByteString.Companion.readByteString
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.DataInputStream
import java.io.File
import java.io.IOException

@ExperimentalUnsignedTypes
internal class SGDUReader {

    private val readBuffer = ByteArray(8)

    private fun DataInputStream.readUInt32(): UInt {
        readFully(readBuffer, 0, 4)
        return ((readBuffer[0].toUInt() and 0xFFu) shl 24) or
                ((readBuffer[1].toUInt() and 0xFFu) shl 16) or
                ((readBuffer[2].toUInt() and 0xFFu) shl 8) or
                (readBuffer[3].toUInt() and 0xFFu)
    }

    private fun DataInputStream.readUInt24(): UInt {
        readFully(readBuffer, 0, 3)
        return ((readBuffer[0].toUInt() and 0xFFu) shl 16) or
                ((readBuffer[1].toUInt() and 0xFFu) shl 8) or
                (readBuffer[2].toUInt() and 0xFFu)
    }

    private fun DataInputStream.readUByte8(): UByte {
        readFully(readBuffer, 0, 1)
        return readBuffer[0].toUByte()
    }

    fun readFromFile(file: File, services: MutableMap<Int, SGService>, contents: MutableMap<String, SGContentImpl>) {
        DataInputStream(file.inputStream()).use { reader ->
            // Unit_Header
            val extensionOffset = reader.readUInt32()
            reader.skipBytes(2) // reserved
            val fragmentsCount = reader.readUInt24().toInt()
            val offsets = UIntArray(fragmentsCount)
            for (i in 0 until fragmentsCount) {
                // skip fragmentTransportID[i] + fragmentVersion[i]
                reader.skipBytes(UInt.SIZE_BYTES + UInt.SIZE_BYTES)
                offsets[i] = reader.readUInt32()
            }

            // Unit_Payload
            val getSize = { i: Int ->
                (offsets.getOrNull(i + 1)?.let { prevOffset ->
                    prevOffset - offsets[i]
                } ?: if (extensionOffset > 0uL) {
                    extensionOffset - offsets[i]
                } else {
                    reader.available().toUInt()
                }).toInt()
            }

            for (i in 0 until fragmentsCount) {
                when (reader.readUByte8().toInt()) {
                    SGFragmentEncoding.XML_OMA -> {
                        readOMAXml(reader, getSize(i), services, contents)
                    }

                    else -> {
                        reader.skipBytes(getSize(i) - GENERAL_OFFSET)
                    }
                }
            }
        }
    }

    private fun readOMAXml(reader: DataInputStream, size: Int, services: MutableMap<Int, SGService>, contents: MutableMap<String, SGContentImpl>) {
        val readString = { offset: Int ->
            reader.readByteString(size - offset).utf8()
        }

        val getOrCreateService = { serviceId: Int ->
            services[serviceId] ?: SGService(serviceId).also {
                services[serviceId] = it
            }
        }

        when (reader.readUByte8().toInt()) {
            SGFragmentType.SERVICE -> parseServiceXML(readString(OMA_XML_OFFSET), getOrCreateService)

            SGFragmentType.SCHEDULE -> parseScheduleXML(readString(OMA_XML_OFFSET), getOrCreateService)

            SGFragmentType.CONTENT -> {
                val content = parseContentXML(readString(OMA_XML_OFFSET))
                val contentId = content.id
                if (contentId != null) {
                    contents[contentId] = content
                }
//                val contentId = content.id
//                if (contentId != null) {
//                    content.serviceIdList?.forEach { serviceId ->
//                        val service = getOrCreateService(serviceId)
//                        val scheduleContent = service.scheduleMap?.let {
//                            it[contentId]
//                        } ?: SGScheduleContent(null, serviceId, 0, contentId).also {
//                            if (service.scheduleMap != null) {
//                                service.scheduleMap?.put(contentId, it)
//                            } else {
//                                service.scheduleMap = mutableMapOf(Pair(contentId, it))
//                            }
//                        }
//
//                        scheduleContent.content = content
//                    }
//                }
            }

            else -> reader.skipBytes(size - OMA_XML_OFFSET)
        }
    }

    private fun parseServiceXML(xmlPayload: String, getService: (Int) -> SGService): SGService? {
        try {
            val parser = XmlUtils.newParser(xmlPayload)

            parser.nextTag()
            parser.require(XmlPullParser.START_TAG, null, "Service")

            var serviceId: Int = -1
            var globalServiceId: String? = null
            var version: Long = 0

            parser.iterateAttrs { name, value ->
                when (name) {
                    "id" -> serviceId = XmlUtils.strToInt(value)
                    "globalServiceID" -> globalServiceId = value
                    "version" -> version = XmlUtils.strToLong(value)
                }
            }

            val service = getService(serviceId)

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

    private fun parseScheduleXML(xmlPayload: String, getService: (Int) -> SGService): SGSchedule? {
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

            val getOrCreateSchedule = { serviceId: Int ->
                getService(serviceId).let { service ->
                    service.scheduleMap?.get(scheduleId)
                            ?: SGSchedule(scheduleId, serviceId, scheduleVersion).also {
                                service.addSchedule(it)
                            }
                }
            }

            var schedule: SGSchedule? = null

            parser.iterateDocument { tagName ->
                when (tagName) {
                    "ServiceReference" -> readIdRefTag(parser) {
                        schedule = getOrCreateSchedule(it)
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

                                if (startTime > 0) TimeUtils.ntpSecondsToUtc(startTime)
                                if (endTime > 0) TimeUtils.ntpSecondsToUtc(endTime)

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

    private fun parseContentXML(xmlPayload: String): SGContentImpl {
        return SGContentImpl().apply {
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

    private fun readIdRefTag(parser: XmlPullParser, action: (Int) -> Unit) {
        parser.iterateAttrs { name, value ->
            if (name == "idRef") {
                action(XmlUtils.strToInt(value))
            }
        }.skipSubTags()
    }

    companion object {
        private val GENERAL_OFFSET = UByte.SIZE_BYTES /* fragmentEncoding */
        private val OMA_XML_OFFSET = GENERAL_OFFSET + UByte.SIZE_BYTES  /* fragmentEncoding + fragmentType */
    }
}
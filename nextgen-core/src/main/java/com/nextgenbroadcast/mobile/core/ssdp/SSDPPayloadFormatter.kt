package com.nextgenbroadcast.mobile.core.ssdp

import java.util.*

class SSDPPayloadFormatter(
    private val address: String,
    private val port: Int,
) {

    private fun StringBuilder.appendRNLine(content: String) {
        if (content.isNotEmpty()) {
            append(content)
        }
        append("\r\n")
    }

    fun parseMethodOrNull(data: String): String? {
        return runCatching {
            Scanner(data).nextLine()
        }.getOrNull()
    }

    fun parseHeadersOrNull(data: String): Map<String, String>? {
        return runCatching {
            val headers = hashMapOf<String, String>()
            val scanner = Scanner(data)
            scanner.nextLine() // skip method
            while (scanner.hasNext()) {
                val headerPlainText = scanner.nextLine()
                val dividerIndex = headerPlainText.indexOf(":")
                if (dividerIndex == -1) continue
                val headerName = headerPlainText.substring(0, dividerIndex).trim()
                headers[headerName] = headerPlainText.substring(startIndex = dividerIndex + 1).trim()
            }
            headers
        }.getOrNull()
    }

    fun parseDeviceInfoOrNull(data: String): SSDPDeviceInfo? {
        return parseHeadersOrNull(data)?.let { headers ->
            SSDPDeviceInfo(
                id = headers[SERVER] ?: return null,
                location = headers[LOCATION] ?: return null
            )
        }
    }

    fun formatSearchPayload(searchTarget: String, mx: Int): String = buildString {
        appendRNLine(M_SEARCH)
        appendRNLine("$HOST: $address:$port")
        appendRNLine("$ST: $searchTarget")
        appendRNLine("$MX: $mx")
        appendRNLine("$MAN: $DISCOVER")
        appendRNLine("")
    }

    fun formatSearchResponsePayload(
        searchTarget: String,
        location: String,
        deviceId: String,
        date: String,
        maxAge: Int,
    ): String = buildString {
        appendRNLine(HTTP_OK)
        appendRNLine("$CACHE_CONTROL = $maxAge")
        appendRNLine("$DATE: $date")
        appendRNLine("$LOCATION: $location")
        appendRNLine("$SERVER: $deviceId")
        appendRNLine("$ST: $searchTarget")
        appendRNLine("$USN: uuid:$deviceId:$searchTarget")
        appendRNLine("")
    }

    fun formatAdvertisePayload(
        notificationType: String,
        location: String,
        deviceId: String,
        maxAge: Int,
    ): String = buildString {
        appendRNLine(M_NOTIFY)
        appendRNLine("$HOST: $address:$port")
        appendRNLine("$CACHE_CONTROL = $maxAge")
        appendRNLine("$LOCATION: $location")
        appendRNLine("$NT: $notificationType")
        appendRNLine("$NTS: $ALIVE")
        appendRNLine("$SERVER: $deviceId")
        appendRNLine("$USN: uuid:$deviceId:${notificationType}")
        appendRNLine("")
    }

    companion object {
        private const val HOST = "HOST"
        private const val CACHE_CONTROL = "CACHE-CONTROL: max-age"
        internal const val ST = "ST"
        internal const val SERVER = "SERVER"
        internal const val LOCATION = "LOCATION"
        internal const val NT = "NT"
        private const val NTS = "NTS"
        private const val USN = "USN"
        private const val DATE = "DATE"
        private const val MX = "MX"
        private const val MAN = "MAN"

        internal const val M_NOTIFY = "NOTIFY * HTTP/1.1"
        internal const val M_SEARCH = "M-SEARCH * HTTP/1.1"
        internal const val HTTP_OK = "HTTP/1.1 200 OK"

        private const val ALIVE = "ssdp:alive"
        private const val DISCOVER = "ssdp:discover"
    }

}
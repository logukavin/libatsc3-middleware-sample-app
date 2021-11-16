package com.nextgenbroadcast.mobile.core.ssdp

import android.util.Log
import java.net.*
import java.util.*

internal object SSDPNetworkUtils {

    fun getActiveNetworkInterfaceOrNull(): NetworkInterface? {
        val interfaces: Enumeration<NetworkInterface>
        try {
            interfaces = NetworkInterface.getNetworkInterfaces()
        } catch (e: SocketException) {
            Log.e("NetworkUtils", "getActiveNetworkInterfaceOrNull returning null", e)
            return null
        }

        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val inetAddresses = networkInterface.inetAddresses
            /* Check if we have a non-local address. If so, this is the active
             * interface.
             *
             * This isn't a perfect heuristic: I have devices which this will
             * still detect the wrong interface on, but it will handle the
             * common cases of wifi-only and Ethernet-only.
             */
            while (inetAddresses.hasMoreElements()) {
                val addr = inetAddresses.nextElement()
                if (!(addr.isLoopbackAddress || addr.isLinkLocalAddress)) {
                    return networkInterface
                }
            }
        }
        return null
    }

    fun getLocalIPv4Address(networkInterface: NetworkInterface): InetAddress? {
        val addresses = networkInterface.inetAddresses
        while (addresses.hasMoreElements()) {
            val address = addresses.nextElement()
            if (address is Inet4Address && !address.isLoopbackAddress) {
                return address
            }
        }
        return null
    }

    val DatagramPacket.stringData: String
        get() = String(data)

}
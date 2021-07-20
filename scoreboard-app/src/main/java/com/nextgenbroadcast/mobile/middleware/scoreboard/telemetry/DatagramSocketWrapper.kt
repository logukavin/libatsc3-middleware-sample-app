package com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DatagramSocketWrapper(private val context: Context) {
    private val socket: DatagramSocket = DatagramSocket()
    private val address: InetAddress? by lazy {
        getBroadcastAddress()
    }

    fun sendUdpMessage(message: String) {
        val buf = message.toByteArray()
        val packet = DatagramPacket(buf, buf.size, address, SOCKET_PORT)
        CoroutineScope(Dispatchers.IO).launch {
            socket.send(packet)
        }
    }

    @Throws(IOException::class)
    private fun getBroadcastAddress(): InetAddress? {
        val wifi = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        // handle null somehow
        val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3) quads[k] = (broadcast shr k * 8 and 0xFF).toByte()
        return InetAddress.getByAddress(quads)
    }

    companion object {
        const val SOCKET_PORT = 6969
    }
}
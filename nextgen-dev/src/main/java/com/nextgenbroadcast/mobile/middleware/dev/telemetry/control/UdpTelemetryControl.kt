package com.nextgenbroadcast.mobile.middleware.dev.telemetry.control

import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryControl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpTelemetryControl(
        private val host: String,
        private val port: Int
) : ITelemetryControl {
    override suspend fun subscribe(commandFlow: MutableSharedFlow<TelemetryControl>) {
        callbackFlow {
            val listener = UdpListener { message ->
                sendBlocking(TelemetryControl().apply {
                    action = message.trim()
                })
            }.apply {
                start()
            }

            awaitClose {
                listener.close()
            }
        }.buffer(Channel.CONFLATED) // To avoid blocking
                .collect { data ->
                    commandFlow.emit(data)
                }
    }

    inner class UdpListener(
            private val block: (message: String) -> Unit
    ) : Thread(TAG) {
        private val buffer = ByteArray(1024)
        private val packet = DatagramPacket(buffer, buffer.size)

        private var socket: DatagramSocket? = null
        private var running = true

        override fun run() {
            try {
                DatagramSocket(port, InetAddress.getByName(host)).also {
                    socket = it
                }.use { socket ->
                    while (running) {
                        socket.receive(packet)

                        block(String(packet.data, 0, packet.length))
                    }
                }
            } catch (e: Exception) {
                LOG.d(TAG, "Failed reading UDP socket: $host/$port", e)
            }
        }

        override fun interrupt() {
            super.interrupt()
            socket?.close()
        }

        fun close() {
            running = false
            interrupt()
        }
    }

    companion object {
        val TAG: String = UdpTelemetryControl::class.java.simpleName
    }
}

/*
Sending Broadcast Udp with socket:

                val buf = "test".toByteArray()
                val address = getBroadcastAddress()
                val packet = DatagramPacket(buf, buf.size, address, 8081)
                CoroutineScope(Dispatchers.IO).launch {
                    socket.send(packet)
                }

    private val socket = DatagramSocket(/*8081, InetAddress.getByName("0.0.0.0")*/).apply {
        broadcast = true
    }

    @Throws(IOException::class)
    fun getBroadcastAddress(): InetAddress? {
        val wifi = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        // handle null somehow
        val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3) quads[k] = (broadcast shr k * 8 and 0xFF).toByte()
        return InetAddress.getByAddress(quads)
    }
 */
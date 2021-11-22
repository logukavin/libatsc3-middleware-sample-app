package com.nextgenbroadcast.mobile.core.ssdp

import android.util.Log
import com.nextgenbroadcast.mobile.core.ssdp.SSDPNetworkUtils.stringData
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

abstract class SocketAwareThread(
    private val socket: DatagramSocket,
    private val tag: String,
    datagramBufferSize: Int = 1024
) : Thread(tag) {

    private val datagramPacket: DatagramPacket by lazy {
        val buffer = ByteArray(size = datagramBufferSize)
        DatagramPacket(buffer, buffer.size)
    }

    @Volatile
    private var isRunning: Boolean = false

    abstract fun handlePacket(data: String, address: InetAddress, port: Int)

    override fun start() {
        isRunning = true
        super.start()
    }

    override fun run() {
        Log.d(tag, "Started")
        while (isRunning) {
            try {
                socket.receive(datagramPacket)
                handlePacket(datagramPacket.stringData, datagramPacket.address, datagramPacket.port)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        Log.d(tag, "Ended")
    }

    fun stopExecution() {
        isRunning = false
    }

}
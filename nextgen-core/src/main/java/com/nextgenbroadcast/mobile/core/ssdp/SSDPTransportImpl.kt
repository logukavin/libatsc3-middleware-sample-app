package com.nextgenbroadcast.mobile.core.ssdp

import android.os.*
import android.util.Log
import com.nextgenbroadcast.mobile.core.ssdp.*
import com.nextgenbroadcast.mobile.core.ssdp.SSDPNetworkUtils.getActiveNetworkInterfaceOrNull
import com.nextgenbroadcast.mobile.core.ssdp.SSDPNetworkUtils.getLocalIPv4Address
import com.nextgenbroadcast.mobile.core.ssdp.SSDPPayloadFormatter.Companion.HTTP_OK
import com.nextgenbroadcast.mobile.core.ssdp.SSDPPayloadFormatter.Companion.M_NOTIFY
import com.nextgenbroadcast.mobile.core.ssdp.SSDPPayloadFormatter.Companion.M_SEARCH
import com.nextgenbroadcast.mobile.core.ssdp.SSDPPayloadFormatter.Companion.NT
import com.nextgenbroadcast.mobile.core.ssdp.SSDPPayloadFormatter.Companion.ST
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.*
import java.util.*

class SSDPTransportImpl(
    private val role: SSDPRole,
    private val address: String,
    private val port: Int,
    private val deviceId: String,
    private val ssdpDeviceInfoFlow: MutableStateFlow<Set<SSDPDeviceInfo>>,
    private val payloadFormatter: SSDPPayloadFormatter = SSDPPayloadFormatter(address, port),
    private val logger: (String) -> Unit = { Log.d(TAG, it) },
) : ISSDPTransport {

    private var multicastSocket: MulticastSocket? = null
    private var unicastSocket: DatagramSocket? = null
    private var actionHandlerThread: ActionHandlerThread? = null
    private var multicastThread: MulticastThread? = null
    private var unicastThread: UnicastThread? = null

    @Volatile
    private var location: String = ""

    private val multicastGroupAddress: InetSocketAddress by lazy {
        InetSocketAddress(address, port)
    }

    override var isRunning: Boolean = false
        private set

    @Synchronized
    override fun start() {
        logger("Starting")
        actionHandlerThread = ActionHandlerThread().apply {
            start()
        }
    }

    override fun advertise(location: String) {
        this.location = location
        actionHandlerThread?.advertiseAsync(location)
    }

    override fun search(searchTarget: String) {
        actionHandlerThread?.searchAsync(searchTarget)
    }

    override fun shutdown() {
        actionHandlerThread?.shutdownAsync()
    }

    private fun initializeSockets() {
        val networkInterface = getActiveNetworkInterfaceOrNull() ?: return
        multicastSocket = createMulticastSocket(networkInterface)
        unicastSocket = createUnicastSocket(networkInterface)
    }

    private fun createMulticastSocket(networkInterface: NetworkInterface): MulticastSocket {
        return MulticastSocket(port).apply {
            loopbackMode = true
            joinGroup(multicastGroupAddress, networkInterface)
        }
    }

    private fun createUnicastSocket(networkInterface: NetworkInterface): DatagramSocket {
        return DatagramSocket(null).apply {
            reuseAddress = true
            bind(InetSocketAddress(getLocalIPv4Address(networkInterface), this@SSDPTransportImpl.port))
        }
    }

    private fun multicast(payload: String) {
        try {
            logger("Multicasting:\n$payload")
            multicastSocket?.send(DatagramPacket(payload.toByteArray(), payload.length, multicastGroupAddress))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun unicast(payload: String, address: InetAddress, port: Int) {
        val response = DatagramPacket(
            payload.toByteArray(),
            payload.length,
            InetSocketAddress(address, port)
        )
        try {
            logger("Unicasting to ip:$address, port: $port:\n$payload")
            unicastSocket?.send(response)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun parseAndNotifyListener(data: String) {
        payloadFormatter.parseDeviceInfoOrNull(data)?.let { device ->
            logger("Adding device $device")
            ssdpDeviceInfoFlow.value = ssdpDeviceInfoFlow.value.toMutableSet().apply { add(device) }
        }
    }

    private inner class ActionHandlerThread : HandlerThread(TAG) {

        private val handler: Handler by lazy { Handler(looper, ::handleMessage) }

        override fun start() {
            super.start()
            handler.sendMessage(Message.obtain(handler, ACTION_INITIALIZE))
        }

        fun advertiseAsync(location: String) {
            handler.sendMessage(Message.obtain(handler, ACTION_NOTIFY, location))
        }

        fun searchAsync(searchTarget: String) {
            handler.sendMessage(Message.obtain(handler, ACTION_SEARCH, searchTarget))
        }

        fun shutdownAsync() {
            handler.sendMessage(Message.obtain(handler, ACTION_STOP))
        }

        private fun handleMessage(message: Message): Boolean {
            runCatching {
                when (message.what) {
                    ACTION_NOTIFY -> executeAdvertise(message.obj as String)
                    ACTION_SEARCH -> executeSearch(message.obj as String)
                    ACTION_STOP -> executeStop()
                    ACTION_INITIALIZE -> executeInitialize()
                }
            }
            return true
        }

        private fun executeAdvertise(location: String) {
            logger("Executing advertise")
            multicast(
                payload = payloadFormatter.formatAdvertisePayload(
                    notificationType = role.data,
                    location = location,
                    deviceId = deviceId,
                    maxAge = MAX_AGE
                )
            )
        }

        private fun executeSearch(searchTarget: String) {
            logger("Executing search, target: $searchTarget")
            multicast(
                payload = payloadFormatter.formatSearchPayload(
                    searchTarget = searchTarget,
                    mx = MAX_AGE
                )
            )
        }

        private fun executeStop() {
            logger("shutdown")
            multicastThread?.stopExecution()
            multicastThread = null
            unicastThread?.stopExecution()
            unicastThread = null
            multicastSocket?.close()
            multicastSocket = null
            unicastSocket?.close()
            unicastSocket = null
            isRunning = false
            quitSafely()
        }

        private fun executeInitialize() {
            logger("initialize")
            initializeSockets()
            multicastThread = MulticastThread(requireNotNull(multicastSocket))
            unicastThread = UnicastThread(requireNotNull(unicastSocket))
            multicastThread?.start()
            unicastThread?.start()
            isRunning = true
        }
    }

    private inner class UnicastThread(
        unicastSocket: DatagramSocket
    ) : SocketAwareThread(unicastSocket, UNICAST_TAG) {

        override fun handlePacket(data: String, address: InetAddress, port: Int) {
            logger("Unicast datagram received:\n$data")
            when (payloadFormatter.parseMethodOrNull(data)) {
                HTTP_OK -> {
                    parseAndNotifyListener(data)
                    // TODO should do some action regarding `location` field of the device
                }
            }
        }

    }

    private inner class MulticastThread(
        multicastSocket: MulticastSocket
    ) : SocketAwareThread(multicastSocket, MULTICAST_TAG) {

        override fun handlePacket(data: String, address: InetAddress, port: Int) {
            when (payloadFormatter.parseMethodOrNull(data)) {
                M_SEARCH -> {
                    val headers = payloadFormatter.parseHeadersOrNull(data)
                    if (headers?.get(ST) != role.data) return
                    logger("Multicast datagram received:\n$data")
                    unicast(
                        payload = payloadFormatter.formatSearchResponsePayload(
                            searchTarget = role.data,
                            date = Date().toString(),
                            deviceId = deviceId,
                            location = location,
                            maxAge = MAX_AGE
                        ),
                        address = address,
                        port = port
                    )
                }
                M_NOTIFY -> {
                    val headers = payloadFormatter.parseHeadersOrNull(data)
                    if (headers?.get(NT) == role.oppositeRole().data) {
                        logger("Multicast datagram received:\n$data")
                        parseAndNotifyListener(data)
                    }
                }
            }
        }
    }

    companion object {
        private const val MAX_AGE = 1800

        // LOG constants
        private const val TAG = "SSDPTransportImpl"
        private const val MULTICAST_TAG = "SSDPTransportImpl:Multicast"
        private const val UNICAST_TAG = "SSDPTransportImpl:Unicast"

        // Message constants
        private const val ACTION_NOTIFY = 1
        private const val ACTION_SEARCH = 2
        private const val ACTION_STOP = 3
        private const val ACTION_INITIALIZE = 4
    }

}
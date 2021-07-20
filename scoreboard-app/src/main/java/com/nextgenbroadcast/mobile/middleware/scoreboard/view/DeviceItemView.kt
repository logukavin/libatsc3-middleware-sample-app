package com.nextgenbroadcast.mobile.middleware.scoreboard.view

import android.content.Context
import android.net.wifi.WifiManager
import android.util.AttributeSet
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.ClientTelemetryEvent
import com.nextgenbroadcast.mobile.middleware.scoreboard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DeviceItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val gson = Gson()
    private val phyType = object : TypeToken<PhyPayload>() {}.type

    lateinit var title: TextView
    lateinit var lostLabel: TextView
    lateinit var phyChart: PhyChart
    lateinit var removeBtn: Button
    private val socket: DatagramSocket = DatagramSocket()
    private var selectedDeviceId:String? = null
    private val address: InetAddress? by lazy {
        getBroadcastAddress()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        title = findViewById(R.id.device_name_view)
        lostLabel = findViewById(R.id.device_lost_label)
        phyChart = findViewById(R.id.device_phy_chart)
        removeBtn = findViewById(R.id.device_remove_btn)
    }

    fun observeSelectedDeviceId(selectedDeviceIdFlow:Flow<String?>){
       CoroutineScope(Dispatchers.IO).launch {
           selectedDeviceIdFlow.collect {
               selectedDeviceId = it
           }
       }
    }

    fun observe(flow: Flow<ClientTelemetryEvent>?) {
        phyChart.setDataSource(
            flow?.mapNotNull { event ->
                try {
                    val payload = gson.fromJson<PhyPayload>(event.payload, phyType)
                    Pair(payload.timeStamp, payload.snr1000.toDouble() / 1000)
                    val timeStamp = payload.timeStamp
                    val payloadValue = payload.snr1000.toDouble() / 1000
                    if (title.text.equals(selectedDeviceId)) {
                        sendUdpMessage("${title.text},${payload.timeStamp},$payloadValue")
                    }
                    Pair(timeStamp, payloadValue)
                } catch (e: Exception) {
                    LOG.w(TAG, "Can't parse telemetry event payload", e)
                    null
                }
            }?.let {
                PhyChart.DataSource(it)
            }
        )
    }

    private fun sendUdpMessage(message:String) {
        val buf = message.toByteArray()
        val packet = DatagramPacket(buf, buf.size, address, SOCKET_PORT)
        CoroutineScope(Dispatchers.IO).launch {
            socket.send(packet)
        }
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

    data class PhyPayload(
        val snr1000: Int,
        val timeStamp: Long
    )

    companion object {
        val TAG: String = DeviceItemView::class.java.simpleName
        const val SOCKET_PORT = 6969
    }
}
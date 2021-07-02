package com.nextgenbroadcast.mobile.middleware.scoreboard.view

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.ClientTelemetryEvent
import com.nextgenbroadcast.mobile.middleware.scoreboard.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class DeviceItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val gson = Gson()
    private val phyType = object : TypeToken<PhyPayload>() {}.type

    lateinit var title: TextView
    lateinit var lostLabel: TextView
    lateinit var phyChart: PhyChart
    lateinit var removeBtn: Button

    override fun onFinishInflate() {
        super.onFinishInflate()

        title = findViewById(R.id.device_name_view)
        lostLabel = findViewById(R.id.device_lost_label)
        phyChart = findViewById(R.id.device_phy_chart)
        removeBtn = findViewById(R.id.device_remove_btn)
    }

    fun observe(flow: Flow<ClientTelemetryEvent>?) {
        phyChart.setDataSource(
            flow?.mapNotNull { event ->
                try {
                    val payload = gson.fromJson<PhyPayload>(event.payload, phyType)
                    Pair(payload.timeStamp, payload.snr1000.toDouble() / 1000)
                } catch (e: Exception) {
                    LOG.w(TAG, "Can't parse telemetry event payload", e)
                    null
                }
            }?.let {
                PhyChart.DataSource(it)
            }
        )
    }

    data class PhyPayload(
        val snr1000: Int,
        val timeStamp: Long
    )

    companion object {
        val TAG: String = DeviceItemView::class.java.simpleName
    }
}
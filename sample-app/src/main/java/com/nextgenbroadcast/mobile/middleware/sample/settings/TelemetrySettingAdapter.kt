package com.nextgenbroadcast.mobile.middleware.sample.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryEvent.Companion.EVENT_TOPIC_LOCATION
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryEvent.Companion.EVENT_TOPIC_SENSORS
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.LocationFrequencyType
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.SensorFrequencyType
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.SensorTelemetryReader
import com.nextgenbroadcast.mobile.middleware.sample.R

class TelemetrySettingAdapter(
    val layoutInflater: LayoutInflater,
    val settingListener: TelemetrySettingsFragment.TelemetrySettingsListener
) : ListAdapter<SensorState, TelemetrySettingAdapter.TelemetrySettingViewHolder>(sensorStateDifUtil) {
    private val sensorFrequencyTypeList = SensorFrequencyType.values().toList()
    private val locationFrequencyTypeList = LocationFrequencyType.values().toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TelemetrySettingViewHolder {
        return TelemetrySettingViewHolder(layoutInflater.inflate(R.layout.telemetry_setting_item, parent, false))
    }

    override fun onBindViewHolder(holder: TelemetrySettingViewHolder, position: Int) {
      getItem(position).let { (telemetryItem, enable, delayFrequency) ->
            with(holder.sensorSwitch) {
                isChecked = enable
                val sensorType = telemetryItem.substringAfter("$EVENT_TOPIC_SENSORS:").toIntOrNull()

                text = if (sensorType != null) {
                    SensorTelemetryReader.getSensorNameBySensorType(sensorType)
                } else {
                    telemetryItem
                }

                setOnClickListener {
                    settingListener.enableCollectData(telemetryItem, isChecked)
                }
            }

            with(holder.frequencySpinner) {
                val typeList = if (telemetryItem == EVENT_TOPIC_LOCATION) {
                    locationFrequencyTypeList
                } else {
                    sensorFrequencyTypeList
                }

                val spinnerAdapter = ArrayAdapter(context, R.layout.custom_spinner_view, typeList)

                val selectedPosition = if (telemetryItem == EVENT_TOPIC_LOCATION) {
                    spinnerAdapter.getPosition(getLocationFrequencyType(delayFrequency))
                } else {
                    spinnerAdapter.getPosition(getSensorFrequencyType(delayFrequency))
                }

                adapter = spinnerAdapter

                setSelection(selectedPosition)

                onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {}

                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val frequencyDelay = if (telemetryItem == EVENT_TOPIC_LOCATION) {
                                locationFrequencyTypeList[position].delay()
                            } else {
                                sensorFrequencyTypeList[position].delayMils
                            }
                            settingListener.changeSensorFrequencyDelay(telemetryItem, frequencyDelay)
                        }
                    }

            }

        }
    }

    private fun getSensorFrequencyType(sensorDelay: Long?): SensorFrequencyType {
        var frequencyType = SensorFrequencyType.MEDIUM
        sensorDelay?.let { delay ->
            valueOfSensorFrequencyType(delay)?.let {
                frequencyType = it
            }
        }

        return frequencyType
    }

    private fun getLocationFrequencyType(locationDelay: Long?): LocationFrequencyType {
        var frequencyType = LocationFrequencyType.MEDIUM
        locationDelay?.let { delay ->
            valueOfLocationFrequencyType(delay)?.let {
                frequencyType = it
            }
        }

        return frequencyType
    }

    class TelemetrySettingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sensorSwitch: SwitchCompat = itemView.findViewById(R.id.switchTelemetrySetting)
        val frequencySpinner: Spinner = itemView.findViewById(R.id.spinnerTelemetryFrequency)
    }

    companion object {
        fun valueOfSensorFrequencyType(value: Long): SensorFrequencyType? =
            SensorFrequencyType.values().find { it.delayMils == value }

        fun valueOfLocationFrequencyType(value: Long): LocationFrequencyType? =
            LocationFrequencyType.values().find { it.delay() == value }

        private val sensorStateDifUtil = object : DiffUtil.ItemCallback<SensorState>() {
            override fun areItemsTheSame(oldItem: SensorState, newItem: SensorState): Boolean {
                return oldItem.sensorName == newItem.sensorName
            }

            override fun areContentsTheSame(oldItem: SensorState, newItem: SensorState): Boolean {
                return oldItem.sensorEnable == newItem.sensorEnable
                        && oldItem.sensorDelay == newItem.sensorDelay
            }
        }
    }

}
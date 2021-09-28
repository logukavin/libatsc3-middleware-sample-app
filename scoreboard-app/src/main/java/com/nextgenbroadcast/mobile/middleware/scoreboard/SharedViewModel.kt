package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.location.Location
import androidx.lifecycle.*
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.LocationData
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.CommandTarget
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.DeviceScoreboardInfo
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.LocationDataAndTime
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TDataPoint
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryEntity
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.TelemetryManager
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.floor

class SharedViewModel : ViewModel() {
    private val _deviceList: MutableLiveData<List<TelemetryDevice>> = MutableLiveData(emptyList())
    private val _chartDevices = MutableLiveData<List<String>>(emptyList())
    private val _deviceFlowMap = MutableLiveData<Map<String, Flow<TDataPoint>>>(emptyMap())
    private val _locationData = MutableLiveData<Map<String, LocationDataAndTime>>(emptyMap())
    val currentDeviceLiveData = MutableLiveData<LocationData?>(null)
    val selectedCommandTarget = MutableLiveData<CommandTarget?>(null)
    val targetCommandLiveData: MediatorLiveData<List<CommandTarget>> = MediatorLiveData()
    val telemetryCommands = MutableLiveData(
        listOf(
            TelemetryEntity("acceleration"),
            TelemetryEntity("gyroscope"),
            TelemetryEntity("motion"),
            TelemetryEntity("step_detector"),
            TelemetryEntity("step_counter"),
            TelemetryEntity("rotation"),
            TelemetryEntity(TelemetryEvent.EVENT_TOPIC_BATTERY),
            TelemetryEntity(TelemetryEvent.EVENT_TOPIC_LOCATION),
            TelemetryEntity(TelemetryEvent.EVENT_TOPIC_PHY)
        )
    )

    private val locationCalculationResult = FloatArray(1)

    private val _distanceLiveData: LiveData<Map<String, Float?>?> = currentDeviceLiveData
        .mapWithCallback(
            second = _locationData,
            onFirstChanged = { distanceMap, oldLocation, newLocation, devicesLocationMap ->
                calculateDistance(
                    distanceMap = distanceMap?.toMutableMap(),
                    oldMyLocation = oldLocation,
                    newMyLocation = newLocation ?: return@mapWithCallback distanceMap,
                    newDevicesLocation = devicesLocationMap ?: return@mapWithCallback distanceMap,
                    oldDevicesLocation = devicesLocationMap
                )
            },
            onSecondChanged = { distanceMap, oldDevicesLocation, newDevicesLocation, currentLocation ->
                calculateDistance(
                    distanceMap = distanceMap?.toMutableMap(),
                    oldMyLocation = currentLocation ?: return@mapWithCallback distanceMap,
                    newMyLocation = currentLocation,
                    newDevicesLocation = newDevicesLocation ?: return@mapWithCallback distanceMap,
                    oldDevicesLocation = oldDevicesLocation
                )
            },
            onBothChanged = { distanceMap, oldLocation, newLocation, oldDevicesLocation, newDevicesLocation ->
                calculateDistance(
                    distanceMap = distanceMap?.toMutableMap(),
                    oldMyLocation = oldLocation,
                    newMyLocation = newLocation ?: return@mapWithCallback distanceMap,
                    newDevicesLocation = newDevicesLocation ?: return@mapWithCallback distanceMap,
                    oldDevicesLocation = oldDevicesLocation
                )
            }
        )

    val devicesToAdd = _chartDevices.mapWith(_deviceFlowMap) { (devices, deviceToFlow) ->
        devices?.subtract(deviceToFlow?.keys ?: emptyList())
    }.distinctUntilChanged()

    val devicesToRemove = _chartDevices.mapWith(_deviceFlowMap) { (devices, deviceFlows) ->
        deviceFlows?.keys?.subtract(devices ?: emptyList())
    }.distinctUntilChanged()

    val deviceInfoList: LiveData<List<DeviceScoreboardInfo>> =
        _deviceList.mapWith(_deviceFlowMap, _distanceLiveData) { (list, flowMap, distanceMap) ->
            list?.map { device ->
                DeviceScoreboardInfo(
                    device = device,
                    selected = flowMap?.containsKey(device.id) ?: false,
                    distance = distanceMap?.get(device.id)
                )
            } ?: emptyList()
        }.distinctUntilChanged()

    val chartDevices = _chartDevices.mapWith(_deviceList) { (chartList, deviceList) ->
        deviceList?.filter { chartList?.contains(it.id) ?: false }
    }.distinctUntilChanged()

    val chartDevicesWithFlow = chartDevices.mapWith(_deviceFlowMap, _distanceLiveData) { (chartList, flowMap, distance) ->
        chartList?.filter { flowMap?.containsKey(it.id) ?: false }?.map { device ->
            DeviceScoreboardInfo(
                device = device,
                distance = distance?.get(device.id),
                selected = true
            )
        }
    }.distinctUntilChanged()

    val selectedDeviceId: MutableLiveData<String?> = MutableLiveData()

    val selectionState: LiveData<Pair<Boolean, Boolean>> = deviceInfoList.map { deviceList ->
        val deviceWithFlowCount = deviceList.filter { (_, hasFlow) -> hasFlow }.size
        Pair(deviceWithFlowCount > 0, deviceWithFlowCount == deviceList.size)
    }.distinctUntilChanged()

    init {
        targetCommandLiveData.value = initialTargetSetup()
        targetCommandLiveData.addSource(deviceInfoList) { deviceInfoList ->
            targetCommandLiveData.value = mutableListOf(
                CommandTarget.SelectedDevices(
                    deviceIdList = deviceInfoList
                        .filter { it.selected }
                        .map { it.device.id }
                ),
                CommandTarget.Broadcast
            ).apply {
                deviceInfoList.map { deviceInfo ->
                    CommandTarget.Device(deviceInfo.device.id)
                }.let { commands ->
                    addAll(commands)
                }
            }.also {
                // update value in case devices has been selected
                if (selectedCommandTarget.value is CommandTarget.SelectedDevices) {
                    selectedCommandTarget.value = it.first()
                }
            }
        }
    }

    private fun initialTargetSetup(): List<CommandTarget> = listOf(
        CommandTarget.SelectedDevices(
            deviceIdList = deviceInfoList.value
                ?.filter { it.selected }
                ?.map { it.device.id }
                ?: emptyList()
        ),
        CommandTarget.Broadcast
    )

    private fun calculateDistance(
        distanceMap: MutableMap<String, Float?>?,
        oldMyLocation: LocationData?,
        newMyLocation: LocationData,
        newDevicesLocation: Map<String, LocationDataAndTime>,
        oldDevicesLocation: Map<String, LocationDataAndTime>?
    ): Map<String, Float?> {
        return if (distanceMap == null || oldMyLocation != newMyLocation) {

            // we should recalculate all records:
            // 1. When it's first calculation (aka. distanceMap is equals to null)
            // 2. When location of the host device is changed, so all data is invalid

            if (distanceMap != null && oldMyLocation != null
                && !isLocationDataChangedSignificantly(newMyLocation, oldMyLocation)
                && newDevicesLocation.keys == oldDevicesLocation?.keys
            ) {
                // if current device location changed not significantly we should
                // check if devices location is still valid
                calculateDevicesLocation(distanceMap, newMyLocation, newDevicesLocation, oldDevicesLocation)
                return distanceMap
            }
            // Current device location is changed and we should recalculate all data
            val map = distanceMap ?: mutableMapOf()
            val devicesLocationMap = newDevicesLocation.toMutableMap()
            invalidateOldLocationDevices(map, devicesLocationMap)
            calculateDistance(map, devicesLocationMap, newMyLocation)
            return map
        } else {
            // means that current device location is not changed, so we should check if
            // devices location is still valid
            calculateDevicesLocation(distanceMap, newMyLocation, newDevicesLocation, oldDevicesLocation)
            distanceMap
        }
    }

    private fun calculateDevicesLocation(
        distanceMap: MutableMap<String, Float?>,
        newMyLocation: LocationData,
        newDevicesLocation: Map<String, LocationDataAndTime>,
        oldDevicesLocation: Map<String, LocationDataAndTime>?
    ) {
        val diff = newDevicesLocation.toMutableMap()
        invalidateOldLocationDevices(distanceMap, diff)
        // finding diff in devices location
        oldDevicesLocation?.forEach { (id, locationData) ->
            // remove devices that has same location as in prev. calculation
            val newLocationData = diff[id]
            if (newLocationData != null && !isLocationDataChangedSignificantly(
                    newLocationData.location,
                    locationData.location
                )
            ) {
                diff.remove(id)
            }
        }

        // now diff contains only locations that should be recalculated
        if (diff.isNotEmpty()) {
            calculateDistance(distanceMap, diff, newMyLocation)
        }
    }

    /**
     * Removes device from the [devicesLocationMap] and its distance form the [distanceMap]
     * in case its last location update was more than [INVALIDATE_LOCATION_TIME_DIFF] before
     */
    private fun invalidateOldLocationDevices(
        distanceMap: MutableMap<String, Float?>,
        devicesLocationMap: MutableMap<String, LocationDataAndTime>
    ) {
        devicesLocationMap
            .filter { (_, locationData) ->
                System.currentTimeMillis() - locationData.timestamp >= INVALIDATE_LOCATION_TIME_DIFF
            }
            .map { it.key }
            .forEach { id ->
                devicesLocationMap.remove(id)
                distanceMap.remove(id)
            }
    }

    /**
     * Compares two coordinates.
     *
     * 1second it is ~ 31m, so if changes is in second and it's less or equals to 0.1 second we could
     * ignore those changes and don't recalculate all distances
     */
    private fun isCoordinateChangedSignificantly(first: Double, second: Double): Boolean {
        var firstCoordinate = abs(first)
        var secondCoordinate = abs(second)
        val firstDegrees = floor(firstCoordinate)
        val secondDegrees = floor(secondCoordinate)

        if (firstDegrees != secondDegrees) return true

        firstCoordinate -= firstDegrees
        firstCoordinate *= 60
        secondCoordinate -= secondDegrees
        secondCoordinate *= 60
        val firstMinutes = floor(firstCoordinate)
        val secondMinutes = floor(secondCoordinate)

        if (firstMinutes != secondMinutes) return true

        firstCoordinate -= firstMinutes
        firstCoordinate *= 60
        secondCoordinate -= secondMinutes
        secondCoordinate *= 60


        return abs(firstCoordinate - secondCoordinate) > SKIP_SECONDS
    }

    private fun isLocationDataChangedSignificantly(first: LocationData, second: LocationData): Boolean {
        return isCoordinateChangedSignificantly(first.lat, second.lat)
            || isCoordinateChangedSignificantly(first.lng, second.lng)
    }

    private fun calculateDistance(
        holder: MutableMap<String, Float?>,
        devicesData: Map<String, LocationDataAndTime>,
        currentLocation: LocationData
    ) {
        devicesData.forEach { (id, locationAndTime) ->
            Location.distanceBetween(
                locationAndTime.location.lat,
                locationAndTime.location.lng,
                currentLocation.lat,
                currentLocation.lng,
                locationCalculationResult
            )
            holder[id] = locationCalculationResult.first()
        }
    }

    fun addDeviceLocation(location: TelemetryEvent) {
        val payload = location.payload
        if (payload !is LocationData) return
        val id = TelemetryManager.extractClientId(location.topic)
        _locationData.value = _locationData.value?.toMutableMap()?.apply {
            put(id, LocationDataAndTime(payload))
        } ?: mapOf(id to LocationDataAndTime(payload))
    }

    fun setDeviceSelection(deviceId: String?) {
        selectedDeviceId.value = deviceId
    }

    fun setDevicesList(deviceIds: List<TelemetryDevice>) {
        _deviceList.value = deviceIds
    }

    fun addDeviceChart(deviceId: String) {
        val list = _chartDevices.value?.toMutableList() ?: mutableListOf()
        if (!list.contains(deviceId)) {
            list.add(deviceId)
            _chartDevices.value = list
        }
    }

    fun removeDeviceChart(deviceId: String) {
        val list = _chartDevices.value?.toMutableList() ?: return
        list.remove(deviceId)
        _chartDevices.value = list
        synchronizeChartSelection(deviceId)
    }

    private fun synchronizeChartSelection(deviceId: String) {
        if (selectedDeviceId.value == deviceId) {
            selectedDeviceId.value = null
        }
    }

    fun addFlows(list: List<Pair<String, Flow<TDataPoint>>>) {
        val map = _deviceFlowMap.value?.toMutableMap() ?: mutableMapOf()
        list.forEach { (deviceId, flow) ->
            map[deviceId] = flow
        }
        _deviceFlowMap.postValue(map)
    }

    fun addFlow(deviceId: String, flow: Flow<TDataPoint>) {
        val map = _deviceFlowMap.value?.toMutableMap() ?: mutableMapOf()
        map[deviceId] = flow
        _deviceFlowMap.postValue(map)
    }

    fun removeFlows(list: List<String>) {
        val map = _deviceFlowMap.value?.toMutableMap() ?: return
        list.forEach { deviceId ->
            map.remove(deviceId)
        }
        _deviceFlowMap.postValue(map)
    }

    fun removeFlow(deviceId: String) {
        val map = _deviceFlowMap.value?.toMutableMap() ?: return
        map.remove(deviceId)
        _deviceFlowMap.postValue(map)
    }

    fun getDeviceFlow(deviceId: String): Flow<TDataPoint>? {
        return _deviceFlowMap.value?.get(deviceId)
    }

    fun selectAllDevices(isChecked: Boolean) {
        val list = _chartDevices.value?.toMutableList() ?: mutableListOf()
        deviceInfoList.value?.forEach { (device) ->
            if (isChecked) {
                if (!list.contains(device.id)) {
                    list.add(device.id)
                }
            } else {
                list.remove(device.id)
                synchronizeChartSelection(device.id)
            }
        }
        _chartDevices.value = list
    }

    fun selectCommand(position: Int, selected: Boolean) {
        telemetryCommands.value = telemetryCommands.value?.toMutableList()?.apply {
            set(position, get(position).copy(checked = selected))
        }
    }

    fun restoreTelemetryCommands(data: String) {
        val commands = data.split(",")
        telemetryCommands.value = telemetryCommands.value?.toMutableList()?.apply {
            commands.forEach { command ->
                val index = indexOfFirst { it.name.contentEquals(command, true) }
                if (index != -1) {
                    set(index, get(index).copy(checked = true))
                }
            }
        }
    }

    companion object {
        private const val SKIP_SECONDS = 0.1
        private val INVALIDATE_LOCATION_TIME_DIFF = TimeUnit.MINUTES.toMillis(5)
    }

}
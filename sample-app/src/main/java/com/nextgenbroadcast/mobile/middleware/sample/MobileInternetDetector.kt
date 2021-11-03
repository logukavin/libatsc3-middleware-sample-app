package com.nextgenbroadcast.mobile.middleware.sample

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.PhoneStateListener.*
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.nextgenbroadcast.mobile.middleware.sample.MobileInternetDetector.CellularNetworkState.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MobileInternetDetector(
    private val context: Context,
) {

    private val mState: MutableStateFlow<CellularNetworkState> = MutableStateFlow(IDLE)
    private val mNetworkCapabilities: MutableStateFlow<NetworkCapabilities?> =
        MutableStateFlow(null)

    private val telephonyManager: TelephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }
    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    val state: StateFlow<CellularNetworkState> = mState
    val networkCapabilities: StateFlow<NetworkCapabilities?> = mNetworkCapabilities

    var registered: Boolean = false
        private set

    private val internetPhoneStateListener: PhoneStateListener by lazy {
        object : PhoneStateListener() {

            private var lastCellularNetworkState: CellularNetworkState = IDLE

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                Log.d(TAG, "$telephonyDisplayInfo")
                lastCellularNetworkState = telephonyDisplayInfo.toCellularNetworkState()
                mState.value = lastCellularNetworkState
            }

            override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    lastCellularNetworkState = fromNetworkType(networkType)
                    Log.d(TAG, "onDataConnectionStateChanged: $lastCellularNetworkState")
                    mState.value = lastCellularNetworkState
                }
                if (state == TelephonyManager.DATA_CONNECTED) {
                    mState.value = lastCellularNetworkState
                } else {
                    mState.value = IDLE
                }
            }
        }
    }

    private val networkCallback: ConnectivityManager.NetworkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                Log.d(TAG, "linkDownstreamBandwidthKbps: ${networkCapabilities.linkDownstreamBandwidthKbps}")
                Log.d(TAG, "linkUpstreamBandwidthKbps: ${networkCapabilities.linkUpstreamBandwidthKbps}")
                mNetworkCapabilities.value = networkCapabilities
            }
        }
    }

    @Synchronized
    fun register() {
        if (registered) return
        registered = true
        var events = LISTEN_DATA_CONNECTION_STATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            events = events or LISTEN_DISPLAY_INFO_CHANGED
        }
        telephonyManager.listen(internetPhoneStateListener, events)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    @Synchronized
    fun unregister() {
        if (!registered) return
        telephonyManager.listen(internetPhoneStateListener, LISTEN_NONE)
        connectivityManager.unregisterNetworkCallback(networkCallback)
        registered = false
    }

    private fun fromNetworkType(
        networkType: Int,
        additionalCheck: (CellularNetworkState) -> CellularNetworkState = { it },
    ): CellularNetworkState = when (networkType) {
        TelephonyManager.NETWORK_TYPE_LTE -> additionalCheck(LTE)
        TelephonyManager.NETWORK_TYPE_NR -> additionalCheck(NR)

        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_CDMA,
        TelephonyManager.NETWORK_TYPE_1xRTT,
        TelephonyManager.NETWORK_TYPE_IDEN -> TWO_G

        TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_EVDO_B,
        TelephonyManager.NETWORK_TYPE_EHRPD,
        TelephonyManager.NETWORK_TYPE_HSPAP -> THREE_G

        else -> UNDEFINED
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun TelephonyDisplayInfo.toCellularNetworkState(): CellularNetworkState =
        fromNetworkType(
            networkType = networkType,
            additionalCheck = { type ->
                when (type) {
                    LTE, NR -> when (overrideNetworkType) {
                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> LTE_ADVANCED_PRO // Advanced pro LTE (5Ge)
                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> NR_NSA // NR (5G) for 5G Sub-6 networks
                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE -> NR_NSA_MMWAVE // (5G+/5G UW) for 5G mmWave networks
                        else -> type
                    }
                    else -> type
                }
            }
        )

    enum class CellularNetworkState {
        IDLE,
        UNDEFINED,
        TWO_G,
        THREE_G,
        LTE,
        LTE_ADVANCED_PRO,
        NR,
        NR_NSA,
        NR_NSA_MMWAVE
    }

    companion object {
        private const val TAG = "MobileInternetDetector"
    }

}

fun MobileInternetDetector.CellularNetworkState.asString() = when (this) {
    IDLE -> "NOT CONNECTED"
    TWO_G -> "G"
    THREE_G -> "3G"
    LTE -> "4G"
    LTE_ADVANCED_PRO -> "5Ge"
    NR -> "5G"
    NR_NSA -> "5G NR"
    NR_NSA_MMWAVE -> "5G+/5G"
    else -> "UNDEFINED"
}


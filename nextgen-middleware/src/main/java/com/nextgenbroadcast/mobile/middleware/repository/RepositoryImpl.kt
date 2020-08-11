package com.nextgenbroadcast.mobile.middleware.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class RepositoryImpl(private var applicationContext: Context) : IRepository {
    private val _applications = ConcurrentHashMap<String, Atsc3Application>()

    init {
        setUpIDGeneration()
    }

    companion object {
        const val REPOSITORY_PREFERENCE = "com.nextgenbroadcast.mobile.middleware.repository"
        const val DEVICE_ID = "device_id"
        const val ADVERTISING_ID = "advertising_id"
    }

    lateinit var preferences: SharedPreferences

    override val hostName = "localHost"
    override val httpPort = 8080
    override val httpsPort = 8443
    override val wsPort = 9998
    override val wssPort = 9999

    override val selectedService = MutableLiveData<SLSService>()
    override val serviceGuideUrls = MutableLiveData<List<Urls>>()

    override val routeMediaUrl = MutableLiveData<String>()

    override val applications = MutableLiveData<List<Atsc3Application>?>()
    override val services = MutableLiveData<List<SLSService>>()
    override val heldPackage = MutableLiveData<Atsc3HeldPackage?>()

    override fun addOrUpdateApplication(application: Atsc3Application) {
        _applications[application.cachePath] = application
        applications.postValue(_applications.values.toList())
    }

    override fun findApplication(appContextId: String): Atsc3Application? {
        return _applications.elements().toList().firstOrNull { app ->
            app.appContextIdList.contains(appContextId)
        }
    }

    override fun setServices(services: List<SLSService>) {
        this.services.postValue(services)
    }

    override fun setSelectedService(service: SLSService?) {
        selectedService.postValue(service)
    }

    override fun setHeldPackage(data: Atsc3HeldPackage?) {
        heldPackage.postValue(data)
    }

    override fun setMediaUrl(mediaUrl: String?) {
        routeMediaUrl.postValue(mediaUrl)
    }

    override fun reset() {
        selectedService.postValue(null)
        serviceGuideUrls.postValue(emptyList())
        services.postValue(emptyList())
        heldPackage.postValue(null)
        routeMediaUrl.postValue(null)
    }

    private fun setUpIDGeneration() {
        preferences = applicationContext.getSharedPreferences(REPOSITORY_PREFERENCE, Context.MODE_PRIVATE)
        val deviceId = preferences.getString(DEVICE_ID, "")
        val advertisingId = preferences.getString(ADVERTISING_ID, "")
        if (deviceId == "" || advertisingId == "") {
            with(preferences.edit()) {
                putString(DEVICE_ID, UUID.randomUUID().toString())
                putString(ADVERTISING_ID, UUID.randomUUID().toString())
                commit()
            }
        }
    }

    override fun getDeviceId(): String {
        return preferences.getString(DEVICE_ID, "") ?: ""
    }

    override fun getAdvertisingId(): String {
        return preferences.getString(ADVERTISING_ID, "") ?: ""
    }
}
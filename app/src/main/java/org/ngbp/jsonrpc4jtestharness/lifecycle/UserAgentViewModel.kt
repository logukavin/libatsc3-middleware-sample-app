package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.ngbp.jsonrpc4jtestharness.controller.IReceiverController
import org.ngbp.jsonrpc4jtestharness.controller.model.AppData

class UserAgentViewModel(
        private val controller: IReceiverController
) : ViewModel() {

    val services = Transformations.distinctUntilChanged(controller.sltServices)
    val isReady = MutableLiveData<Boolean>().apply { value = true } //TODO: Transformations.map(controller.appData) { isAppReady(it ) }
    val appData = Transformations.distinctUntilChanged(controller.appData)

    fun selectService(serviceId: Int) {
        services.value?.let { serviceList ->
            val service = serviceList.firstOrNull { it.id == serviceId }

            service?.let {
                controller.selectService(service)
            }
        }
    }

    private fun isAppReady(appData: AppData?): Boolean {
        return appData?.let {
            !it.appContextId.isNullOrEmpty() && !it.appEntryPage.isNullOrEmpty()
        } ?: false
    }
}
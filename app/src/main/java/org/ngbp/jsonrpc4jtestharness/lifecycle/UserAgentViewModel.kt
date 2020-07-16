package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.ngbp.jsonrpc4jtestharness.controller.IUserAgentController
import org.ngbp.jsonrpc4jtestharness.controller.model.AppData

class UserAgentViewModel(
        private val agentController: IUserAgentController
) : ViewModel() {

    val services = Transformations.distinctUntilChanged(agentController.sltServices)
    val isReady = /*MutableLiveData<Boolean>().apply { value = true } //TODO:*/ Transformations.map(agentController.appData) { isAppReady(it ) }
    val appData = Transformations.distinctUntilChanged(agentController.appData)

    fun selectService(serviceId: Int) {
        services.value?.let { serviceList ->
            val service = serviceList.firstOrNull { it.id == serviceId }

            service?.let {
                agentController.selectService(service)
            }
        }
    }

    private fun isAppReady(appData: AppData?): Boolean {
        return appData?.let {
            !it.appContextId.isNullOrEmpty() && !it.appEntryPage.isNullOrEmpty()
        } ?: false
    }
}
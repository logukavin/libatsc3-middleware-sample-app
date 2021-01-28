package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import com.nextgenbroadcast.mobile.core.presentation.ISelectorPresenter

class SelectorViewModel(
        private val presenter: ISelectorPresenter
) : ViewModel() {
    val services = presenter.sltServices.distinctUntilChanged()
    val selectedService = presenter.selectedService.distinctUntilChanged()

    fun selectService(bsid: Int, serviceId: Int): Boolean {
        return services.value?.let { serviceList ->
            val service = serviceList.firstOrNull { it.bsid == bsid && it.id == serviceId }

            service?.let {
                presenter.selectService(service)
            }
        } ?: false
    }

    fun getSelectedServiceId() = presenter.selectedService.value?.id
}
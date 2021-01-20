package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.nextgenbroadcast.mobile.core.presentation.ISelectorPresenter

class SelectorViewModel(
        private val presenter: ISelectorPresenter
) : ViewModel() {
    val services = Transformations.distinctUntilChanged(presenter.sltServices)

    fun selectService(bsid: Int, serviceId: Int): Boolean {
        services.value?.let { serviceList ->
            val service = serviceList.firstOrNull { it.bsid == bsid && it.id == serviceId }

            service?.let {
                presenter.selectService(service)
            }
        }

        return true
    }

    fun getSelectedServiceId() = presenter.selectedService.value?.id
}
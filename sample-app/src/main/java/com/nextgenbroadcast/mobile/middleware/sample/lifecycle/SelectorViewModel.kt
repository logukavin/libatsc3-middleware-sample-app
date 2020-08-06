package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter

class SelectorViewModel(
        private val presenter: ISelectorPresenter
) : ViewModel() {
    val services = Transformations.distinctUntilChanged(presenter.sltServices)

    fun selectService(serviceId: Int) {
        services.value?.let { serviceList ->
            val service = serviceList.firstOrNull { it.id == serviceId }

            service?.let {
                presenter.selectService(service)
            }
        }
    }

    fun getSelectedServiceId(): Int? {
        return presenter.selectedService.value?.id
    }
}
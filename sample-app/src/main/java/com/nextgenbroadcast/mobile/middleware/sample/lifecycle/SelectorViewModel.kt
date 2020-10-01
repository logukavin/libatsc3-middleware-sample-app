package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.core.presentation.ISelectorPresenter

class SelectorViewModel(
        private val presenter: ISelectorPresenter
) : ViewModel() {
    val services = Transformations.distinctUntilChanged(presenter.sltServices)

    fun selectService(serviceId: Int): Boolean {
        if (presenter.selectedService.value?.id == serviceId) return false

        services.value?.let { serviceList ->
            val service = serviceList.firstOrNull { it.id == serviceId }

            service?.let {
                presenter.selectService(service)
            }
        }

        return true
    }

    fun getSelectedService(): LiveData<SLSService?> {
        return presenter.selectedService
    }
}
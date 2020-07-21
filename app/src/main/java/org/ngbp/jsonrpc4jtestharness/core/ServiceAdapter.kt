package org.ngbp.jsonrpc4jtestharness.core

import android.content.Context
import android.widget.ArrayAdapter
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService

class ServiceAdapter(
        context: Context,
        private val items: List<SLSService>
) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item) {

    init {
        if (items.isEmpty()) {
            add("No service available")
        } else {
            addAll(items.map { it.shortName })
        }
    }

    override fun getItemId(position: Int): Long {
        return items.getOrNull(position)?.id?.toLong() ?: -1
    }
}
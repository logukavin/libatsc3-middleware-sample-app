package org.ngbp.jsonrpc4jtestharness.core

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.ngbp.jsonrpc4jtestharness.R
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService

class ServiceAdapter(context: Context) : ArrayAdapter<SLSService>(context, R.layout.list_item_simple_spinner) {
    private var inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id?.toLong() ?: -1
    }

    override fun getCount(): Int {
        val count = super.getCount()
        return if (count == 0) -1 else count
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(convertView, parent, position)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(convertView, parent, position)
    }

    private fun createView(convertView: View?, parent: ViewGroup, position: Int): View {
        val view = convertView ?: inflater.inflate(R.layout.list_item_simple_spinner, parent, false)

        val serviceName: TextView = view.findViewById<View>(R.id.service_name) as TextView

        if (count > 0) {
            val slsServiceName = getItem(position)?.shortName
            serviceName.text = slsServiceName
        } else serviceName.text = context.getString(R.string.no_service_available)

        return view
    }

    fun setServices(data: List<SLSService>) {
        clear()
        addAll(data)
    }
}
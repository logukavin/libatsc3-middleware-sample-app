package org.ngbp.jsonrpc4jtestharness.core

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.ngbp.jsonrpc4jtestharness.R
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService

class ServiceAdapter(
        context: Context,
        services: List<SLSService>
) : ArrayAdapter<String>(context, R.layout.list_item_simple_spinner) {

    private var items: List<SLSService> = services
    private var inflater: LayoutInflater = LayoutInflater.from(context)

    init {
        setData(items)
    }

    override fun getItemId(position: Int): Long {
        return items.getOrNull(position)?.id?.toLong() ?: -1
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = inflater.inflate(R.layout.list_item_simple_spinner, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
            view = convertView
        }

        val slsServiceName = getItem(position)
        viewHolder.serviceName.text = slsServiceName

        return view
    }

    private class ViewHolder internal constructor(view: View?) {
        val serviceName: TextView = view?.findViewById<View>(R.id.service_name) as TextView
    }

    fun setServices(services: List<SLSService>) {
        clear()
        items = services
        setData(items)
    }

    private fun setData(data: List<SLSService>) {
        if (data.isEmpty()) {
            add(context.getString(R.string.no_service_available))
        } else {
            addAll(data.map { it.shortName })
        }
    }
}
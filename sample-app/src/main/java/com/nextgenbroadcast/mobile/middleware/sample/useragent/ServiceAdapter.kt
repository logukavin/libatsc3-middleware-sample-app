package com.nextgenbroadcast.mobile.middleware.sample.useragent

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.middleware.sample.R

class ServiceAdapter(context: Context) : ArrayAdapter<SLSService>(context, 0) {
    private var inflater = LayoutInflater.from(context)

    override fun getItem(position: Int): SLSService? {
        return if (isEmptyState()) null else super.getItem(position)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id?.toLong() ?: -1
    }

    override fun getCount(): Int {
        return if (isEmptyState()) 1 else super.getCount()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(convertView, parent, position)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(convertView, parent, position)
    }

    private fun isEmptyState() = super.getCount() == 0

    private fun createView(convertView: View?, parent: ViewGroup, position: Int): View {
        val view = (convertView ?: inflater.inflate(R.layout.service_list_item, parent, false)) as TextView

        view.text = if (!isEmptyState()) {
            getItem(position)?.shortName
        } else {
            context.getString(R.string.no_service_available)
        }

        return view
    }

    fun setServices(data: List<SLSService>) {
        clear()
        addAll(data)
    }
}
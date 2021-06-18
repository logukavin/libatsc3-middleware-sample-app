package com.nextgenbroadcast.mobile.view

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_about.view.*


class AboutDialog(
    private val phyInfo: Triple<String?, String?, String?>?,
    private val frequency: Int?
) : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_about, container, false)
        view.but_ok_about.setOnClickListener {
            dismiss()
        }
        view.tv_about_content.text = getInfo()
        return view
    }

    @SuppressLint("MissingPermission")
    private fun getInfo(): CharSequence {
        val context = requireContext()
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        val stringBuilder = StringBuilder().apply {
            packageInfo?.versionName?.let {
                appendBoldTitle(getString(R.string.version_name)).append(it)
            }

            packageInfo?.longVersionCode?.let {
                if (isNotEmpty()) {
                    append(DOUBLE_LINE_BREAK)
                }
                appendBoldTitle(getString(R.string.version_code)).append(it)
            }

            try {
                Build.getSerial()?.let {
                    if (isNotEmpty()) {
                        append(DOUBLE_LINE_BREAK)
                    }
                    appendBoldTitle(getString(R.string.android_id)).append(it)
                }
            } catch (e: SecurityException) {
            }

            phyInfo?.let { info ->
                val (sdkVersion, firmwareVersion, demodeVersion) = info
                sdkVersion?.let {
                    if (isNotEmpty()) {
                        append(DOUBLE_LINE_BREAK)
                        appendBoldTitle(getString(R.string.phy_sdk_version)).append(it)
                    }
                }

                demodeVersion?.let {
                    if (isNotEmpty()) {
                        append(DOUBLE_LINE_BREAK)
                        appendBoldTitle(getString(R.string.demode_version)).append(it)
                    }
                }

                firmwareVersion?.let {
                    if (isNotEmpty()) {
                        append(DOUBLE_LINE_BREAK)
                        appendBoldTitle(getString(R.string.firmware_version)).append(it)
                    }
                }
            }

            frequency?.let {
                append(DOUBLE_LINE_BREAK)
                appendBoldTitle(getString(R.string.frequency)).append(it)
            }
        }

        return Html.fromHtml(stringBuilder.toString(), Html.FROM_HTML_MODE_LEGACY)
    }

    private fun StringBuilder.appendBoldTitle(title: String): StringBuilder {
        append("<b>").append(title).append(" ").append("</b>")
        return this
    }

    companion object {
        private const val DOUBLE_LINE_BREAK = "<br><br>"
    }
}
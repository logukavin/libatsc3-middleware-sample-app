package com.nextgenbroadcast.mobile.view

import android.os.Bundle
import android.provider.Settings.Secure.ANDROID_ID
import android.provider.Settings.Secure.getString
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_about.view.*


class AboutDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_about, null, false)
        view.but_ok_about.setOnClickListener {
            dismiss()
        }
        view.tv_about_content.text = getInfo()
        return view
    }

    private fun getInfo(): Spanned {
        val stringBuilder = StringBuilder()
        val doubleNewLine = "<br><br>"
        requireContext().let { it ->
            val packageInfo = it.packageManager?.getPackageInfo(it.packageName, 0)
            packageInfo?.applicationInfo?.uid

            packageInfo?.versionName?.let {
                addBoldTitle(stringBuilder, getString(R.string.version_name))
                stringBuilder
                        .append(it)

            }

            packageInfo?.longVersionCode?.let {
                if (stringBuilder.isNotEmpty()) {
                    stringBuilder.append(doubleNewLine)
                }
                addBoldTitle(stringBuilder, getString(R.string.version_code))
                stringBuilder
                        .append(it)
            }

            getString(it.contentResolver, ANDROID_ID)?.let {
                if (stringBuilder.isNotEmpty()) {
                    stringBuilder.append(doubleNewLine)
                }
                addBoldTitle(stringBuilder, getString(R.string.android_id))
                stringBuilder
                        .append(it)
            }
        }
        return Html.fromHtml(stringBuilder.toString()) as Spanned
    }

    private fun addBoldTitle(stringBuilder: StringBuilder, title: String) {
        stringBuilder.append("<b>")
                .append(title)
                .append(" ")
                .append("</b>")
    }


}
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
        val doubleNewLine = getString(R.string.line_break)

        val stringBuilder = StringBuilder()
        context?.let { it ->
            val packageInfo = it.packageManager?.getPackageInfo(it.packageName, 0)
            packageInfo?.applicationInfo?.uid

            packageInfo?.versionName?.let {
                stringBuilder
                        .append(getString(R.string.version_name))
                        .append(it)

            }


            packageInfo?.longVersionCode?.let {
                if (stringBuilder.isNotEmpty()) {
                    stringBuilder.append(doubleNewLine)
                }
                stringBuilder.append(getString(R.string.version_code))
                        .append(it)
            }

            getString(it.contentResolver, ANDROID_ID)?.let {
                if (stringBuilder.isNotEmpty()) {
                    stringBuilder.append(doubleNewLine)
                }
                stringBuilder
                        .append(getString(R.string.android_id))
                        .append(it)
            }
        }

        view.tv_about_content.text = Html.fromHtml(stringBuilder.toString()) as Spanned

        return view
    }
}
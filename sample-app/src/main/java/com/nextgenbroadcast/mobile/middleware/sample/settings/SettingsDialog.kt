package com.nextgenbroadcast.mobile.middleware.sample.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.*
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.middleware.sample.databinding.DialogSettingsBinding
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ViewViewModel


class SettingsDialog : DialogFragment() {

    private val viewViewModel: ViewViewModel by activityViewModels()

    private lateinit var binding: DialogSettingsBinding
    private lateinit var pagerAdapter: SettingsPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.SettingsDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogSettingsBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = viewViewModel
        }
        dialog?.setTitle(R.string.settings_title)
        pagerAdapter = SettingsPagerAdapter()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = pagerAdapter.getTabName(position)
        }.attach()
        setChildFragmentResultListener(REQUEST_KEY_SCAN_RANGE, ::setFragmentResultAndDismiss)
        setChildFragmentResultListener(REQUEST_KEY_FREQUENCY, ::setFragmentResultAndDismiss)
    }

    private fun setChildFragmentResultListener(key: String, listener: (String, Bundle) -> Unit) {
        childFragmentManager.setFragmentResultListener(key, viewLifecycleOwner, listener)
    }

    private fun setFragmentResultAndDismiss(key: String, result: Bundle) {
        setFragmentResult(key, result)
        dismiss()
    }

    private inner class SettingsPagerAdapter : FragmentStateAdapter(this) {

        override fun getItemCount(): Int = DIALOG_TAB_COUNT

        fun getTabName(position: Int): String = when (position) {
            0 -> getString(R.string.settings_tab_tune)
            1 -> getString(R.string.settings_tab_ui)
            2 -> getString(R.string.settings_tab_telemetry)
            3 -> getString(R.string.settings_tab_logs)
            else -> throw IllegalArgumentException("Unknown position $position")
        }

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> TuneSettingsFragment().apply { arguments = this@SettingsDialog.arguments }
            1 -> UISettingsFragment()
            2 -> TelemetrySettingsFragment()
            3 -> LogsSettingsFragment()
            else -> throw IllegalArgumentException(
                "Unable to create instance of Fragment for position $position"
            )
        }

    }

    companion object {
        val TAG: String = SettingsDialog::class.java.simpleName

        const val PARAM_FREQUENCY = "param_frequency"

        const val REQUEST_KEY_FREQUENCY = "requestKey_frequency"
        const val REQUEST_KEY_SCAN_RANGE = "requestKey_scan_range"

        private const val DIALOG_TAB_COUNT = 4

        fun newInstance(freqKhz: Int?): SettingsDialog {
            return SettingsDialog().apply {
                if (freqKhz != null) {
                    arguments = Bundle().apply {
                        putInt(PARAM_FREQUENCY, freqKhz)
                    }
                }
            }
        }
    }
}
package com.nextgenbroadcast.mobile.middleware.sample.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nextgenbroadcast.mobile.middleware.sample.databinding.FragmentLogSettingsBinding

class LogsSettingsFragment : Fragment() {

    private lateinit var binding: FragmentLogSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentLogSettingsBinding.inflate(inflater, container, false).apply {
        lifecycleOwner = viewLifecycleOwner
        binding = this
    }.root


}
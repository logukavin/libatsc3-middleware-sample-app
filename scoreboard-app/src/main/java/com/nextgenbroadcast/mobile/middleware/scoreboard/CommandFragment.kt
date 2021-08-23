package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nextgenbroadcast.mobile.middleware.scoreboard.ScoreboardService.Companion.ACTION_COMMANDS
import com.nextgenbroadcast.mobile.middleware.scoreboard.ScoreboardService.Companion.COMMAND_EXTRAS
import com.nextgenbroadcast.mobile.middleware.scoreboard.ScoreboardService.Companion.DEVICES_EXTRAS
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.FragmentCommandBinding
import org.json.JSONObject

class CommandFragment : Fragment(), View.OnClickListener {
    lateinit var binding: FragmentCommandBinding
    private val sharedViewModel by activityViewModels<SharedViewModel>()
    var isGlobalCommand = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCommandBinding.inflate(inflater, container, false)
        binding.buttonPing.setOnClickListener(this)
        return binding.root
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.button_ping -> sendPingCommand()
        }
    }

    private fun sendPingCommand() {
        if (!isGlobalCommand) {
            with(Intent(context, ScoreboardService::class.java)) {
                action = ACTION_COMMANDS

                putExtra(COMMAND_EXTRAS, JSONObject().apply {
                    put(ACTION, ACTION_PING)
                }.toString())

                sharedViewModel.chartDevicesWithFlow.value?.map { device ->
                    device.id
                }?.let { deviceIdList ->
                    putStringArrayListExtra(DEVICES_EXTRAS, ArrayList(deviceIdList))
                }

                context?.startService(this)
            }
        }
    }

    companion object {
        val TAG = CommandFragment::class.java.simpleName
        const val ACTION = "action"
        const val ACTION_PING = "ping"
    }
}
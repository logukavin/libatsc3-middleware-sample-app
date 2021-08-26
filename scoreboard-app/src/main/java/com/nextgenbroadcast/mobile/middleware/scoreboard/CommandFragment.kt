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
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.CommandTuneViewBinding
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.FragmentCommandBinding
import org.json.JSONObject

class CommandFragment : Fragment(), View.OnClickListener {
    private val sharedViewModel by activityViewModels<SharedViewModel>()
    lateinit var binding: FragmentCommandBinding
    lateinit var tuneBinding: CommandTuneViewBinding
    private var isGlobalCommand = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCommandBinding.inflate(inflater, container, false)
        tuneBinding = CommandTuneViewBinding.bind(binding.root)
        binding.buttonPing.setOnClickListener(this)
        tuneBinding.buttonTune.setOnClickListener(this)

        return binding.root
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.buttonPing.id -> sendPingCommand()
            tuneBinding.buttonTune.id -> sendTuneCommand()
        }
    }

    private fun sendTuneCommand() {
        val frequencyValues = tuneBinding.editTextTune.text.toString()
        val arguments = JSONObject().apply {
            put("frequency", frequencyValues + "000")
        }

        sendCommand("tune", arguments)

        tuneBinding.editTextTune.setText("")
        tuneBinding.editTextTune.clearFocus()
    }

    private fun sendPingCommand() {
        sendCommand("ping", null)
    }

    private fun sendCommand(command: String, arguments: JSONObject?) {
        if (!isGlobalCommand) {
            with(Intent(context, ScoreboardService::class.java)) {
                action = ACTION_COMMANDS
                val jsonObject = JSONObject().apply {
                    put(ACTION, command)
                    put(CONTROL_ARGUMENTS, arguments)
                }

                putExtra(COMMAND_EXTRAS, jsonObject.toString())

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
        val TAG: String = CommandFragment::class.java.simpleName
        const val ACTION = "action"
        const val CONTROL_ARGUMENTS = "arguments"
//        const val FREQUENCY_ARG = "frequency"
//        const val CONTROL_ACTION_TUNE = "tune"
//        const val CONTROL_ACTION_PING = "ping"
//        const val CONTROL_ACTION_ACQUIRE_SERVICE = "acquireService"
//        const val CONTROL_ACTION_SET_TEST_CASE = "setTestCase"
//        const val CONTROL_ACTION_RESTART_APP = "restartApp"
//        const val CONTROL_ACTION_REBOOT_DEVICE = "rebootDevice"
//        const val CONTROL_ACTION_TELEMETRY_ENABLE = "enableTelemetry"
//        const val CONTROL_ACTION_VOLUME = "volume"
//        const val CONTROL_ACTION_WIFI_INFO = "networkInfo"
//        const val CONTROL_ACTION_FILE_WRITER = "fileWriter"
//        const val CONTROL_ACTION_RESET_RECEIVER_DEMODE = "reset"
    }
}
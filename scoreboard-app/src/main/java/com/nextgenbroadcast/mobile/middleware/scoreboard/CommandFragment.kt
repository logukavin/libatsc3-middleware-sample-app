package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nextgenbroadcast.mobile.middleware.scoreboard.ScoreboardService.Companion.ACTION_COMMANDS
import com.nextgenbroadcast.mobile.middleware.scoreboard.ScoreboardService.Companion.COMMAND_EXTRAS
import com.nextgenbroadcast.mobile.middleware.scoreboard.ScoreboardService.Companion.DEVICES_EXTRAS
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.CommandSelectServiceViewBinding
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.CommandTuneViewBinding
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.FragmentCommandBinding
import org.json.JSONObject

class CommandFragment : Fragment(), View.OnClickListener {
    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private lateinit var binding: FragmentCommandBinding
    private lateinit var tuneBinding: CommandTuneViewBinding
    private lateinit var selectServiceBinding: CommandSelectServiceViewBinding
    private var isGlobalCommand = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCommandBinding.inflate(inflater, container, false)
        tuneBinding = CommandTuneViewBinding.bind(binding.root)
        selectServiceBinding = CommandSelectServiceViewBinding.bind(binding.root)
        binding.buttonPing.setOnClickListener(this)
        tuneBinding.buttonTune.setOnClickListener(this)
        selectServiceBinding.buttonSelectService.setOnClickListener(this)

        return binding.root
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.buttonPing.id -> sendPingCommand()
            tuneBinding.buttonTune.id -> sendTuneCommand()
            selectServiceBinding.buttonSelectService.id -> sendSelectServiceCommand()
        }
    }

    private fun sendSelectServiceCommand() {
        val serviceName = selectServiceBinding.editTextServiceName.text.toString()
        val serviceId = serviceName.toIntOrNull()
        val serviceBsid = selectServiceBinding.editTextBsId.text.toString().toIntOrNull()

        val arguments = JSONObject().apply {
            if (serviceId == null) {
                put("serviceName", serviceName)
            } else {
                put("serviceId", serviceId)
            }

            serviceBsid?.let { bsId ->
                put("serviceBsid", bsId)
            }
        }

        sendCommand("acquireService", arguments)
    }

    private fun sendTuneCommand() {
        tuneBinding.editTextTune.text.toString().toIntOrNull()?.let { frequencyValues ->
            val arguments = JSONObject().apply {
                put("frequency", frequencyValues * 1000)
            }
            sendCommand("tune", arguments)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun sendPingCommand() {
        sendCommand("ping", null)
    }

    private fun sendCommand(command: String, arguments: JSONObject?) {
        if (!isGlobalCommand) {
            (Intent(context, ScoreboardService::class.java)).apply {
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

                showToast(getString(R.string.command_has_been_sent))
            }
        }
    }

    companion object {
        val TAG: String = CommandFragment::class.java.simpleName
        const val ACTION = "action"
        const val CONTROL_ARGUMENTS = "arguments"
    }
}
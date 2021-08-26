package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.CommandSelectServiceViewBinding
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.CommandTuneViewBinding
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.FragmentCommandBinding
import org.json.JSONObject

class CommandFragment : Fragment(), View.OnClickListener {
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var binding: FragmentCommandBinding
    private lateinit var tuneBinding: CommandTuneViewBinding
    private lateinit var selectServiceBinding: CommandSelectServiceViewBinding

    private var isGlobalCommand = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCommandBinding.inflate(inflater, container, false)

        tuneBinding = CommandTuneViewBinding.bind(binding.root)
        selectServiceBinding = CommandSelectServiceViewBinding.bind(binding.root)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            buttonPing.setOnClickListener(this@CommandFragment)
            tuneBinding.buttonTune.setOnClickListener(this@CommandFragment)
            selectServiceBinding.buttonSelectService.setOnClickListener(this@CommandFragment)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.buttonPing.id -> sendPingCommand()
            tuneBinding.buttonTune.id -> sendTuneCommand()
            selectServiceBinding.buttonSelectService.id -> sendSelectServiceCommand()
        }
    }

    private fun sendSelectServiceCommand() {
        with(selectServiceBinding) {
            val serviceName = editTextServiceName.text?.toString() ?: return
            val serviceBsid = editTextBsId.text?.toString()?.toIntOrNull()

            val arguments = JSONObject().apply {
                val serviceId = serviceName.toIntOrNull()
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
    }

    private fun sendTuneCommand() {
        val frequency = with(tuneBinding) {
            editTextTune.text?.toString()?.toIntOrNull()
        } ?: return

        val arguments = JSONObject().apply {
            put("frequency", frequency * 1000)
        }
        sendCommand("tune", arguments)
    }

    private fun sendPingCommand() {
        sendCommand("ping", null)
    }

    private fun sendCommand(command: String, arguments: JSONObject?) {
        val devices = sharedViewModel.chartDevicesWithFlow.value?.map { device ->
            device.id
        } ?: return

        if (isGlobalCommand) {
            //TODO: implements
        } else {
            Intent(context, ScoreboardService::class.java).apply {
                action = ScoreboardService.ACTION_COMMANDS
                val jsonObject = JSONObject().apply {
                    put(ACTION, command)
                    put(CONTROL_ARGUMENTS, arguments)
                }

                putExtra(ScoreboardService.COMMAND_EXTRAS, jsonObject.toString())
                putStringArrayListExtra(ScoreboardService.DEVICES_EXTRAS, arrayListOf(*devices.toTypedArray()))
            }.run {
                requireContext().startService(this)
            }

            showToast(getString(R.string.command_has_been_sent))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        val TAG: String = CommandFragment::class.java.simpleName

        private const val ACTION = "action"
        private const val CONTROL_ARGUMENTS = "arguments"
    }
}
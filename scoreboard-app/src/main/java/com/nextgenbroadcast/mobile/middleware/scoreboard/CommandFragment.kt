package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.*
import org.json.JSONObject

class CommandFragment : Fragment(), View.OnClickListener {
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var binding: FragmentCommandBinding
    private lateinit var tuneBinding: CommandTuneViewBinding
    private lateinit var selectServiceBinding: CommandSelectServiceViewBinding
    private lateinit var setTestCaseBinding: CommandTestcaseViewBinding
    private lateinit var setVolumeViewBinding: CommandVolumeViewBinding

    private var isGlobalCommand = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCommandBinding.inflate(inflater, container, false)

        tuneBinding = CommandTuneViewBinding.bind(binding.root)
        selectServiceBinding = CommandSelectServiceViewBinding.bind(binding.root)
        setTestCaseBinding = CommandTestcaseViewBinding.bind(binding.root)
        setVolumeViewBinding = CommandVolumeViewBinding.bind(binding.root)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            buttonPing.setOnClickListener(this@CommandFragment)
            tuneBinding.buttonTune.setOnClickListener(this@CommandFragment)
            selectServiceBinding.buttonSelectService.setOnClickListener(this@CommandFragment)
            setTestCaseBinding.buttonApplyTest.setOnClickListener(this@CommandFragment)
            setTestCaseBinding.buttonClearTest.setOnClickListener(this@CommandFragment)
            setVolumeViewBinding.buttonVolume.setOnClickListener(this@CommandFragment)
            buttonRebootDevice.setOnClickListener(this@CommandFragment)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.buttonPing.id -> sendPingCommand()
            tuneBinding.buttonTune.id -> sendTuneCommand()
            selectServiceBinding.buttonSelectService.id -> sendSelectServiceCommand()
            setTestCaseBinding.buttonApplyTest.id -> sendTestCaseCommand()
            setTestCaseBinding.buttonClearTest.id -> clearTestCaseCommand()
            setVolumeViewBinding.buttonVolume.id -> sendVolumeCommand()
            binding.buttonRebootDevice.id -> showRebootDeviceDialog(R.string.reboot_device_warning) { sendRebootDeviceCommand() }
        }
    }

    private fun showRebootDeviceDialog(messageId:Int, action: () -> Unit) {
        AlertDialog.Builder(requireContext()).apply {
            setMessage(getString(messageId, sharedViewModel.chartDevicesWithFlow.value?.size))
            setPositiveButton(getString(R.string.dialog_ok)) { _, _ -> action()}
            setNegativeButton(getString(R.string.dialog_cancel)) { _, _ -> }
        }.show()
    }

    private fun sendRebootDeviceCommand() {
        sendCommand("rebootDevice", null)
    }

    private fun sendVolumeCommand() {
        val arguments = JSONObject().apply {
            put("value", setVolumeViewBinding.seekBarVolume.progress)
        }
        sendCommand("volume", arguments)
    }

    private fun clearTestCaseCommand() {
        setTestCaseBinding.editTextTestCase.setText("")
        sendTestCaseCommand()
    }

    private fun sendTestCaseCommand() {
        val testCase = setTestCaseBinding.editTextTestCase.text?.toString()
        val arguments = JSONObject().apply {
            put("case", testCase)
        }
        sendCommand("setTestCase", arguments)
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
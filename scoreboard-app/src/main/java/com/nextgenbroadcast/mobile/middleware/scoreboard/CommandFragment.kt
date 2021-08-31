package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CommandFragment : Fragment(), View.OnClickListener {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val sharedPreferences: SharedPreferences by lazy {
        requireContext().getSharedPreferences("command_preferences", Context.MODE_PRIVATE)
    }
    private lateinit var binding: FragmentCommandBinding
    private lateinit var tuneBinding: CommandTuneViewBinding
    private lateinit var selectServiceBinding: CommandSelectServiceViewBinding
    private lateinit var setTestCaseBinding: CommandTestcaseViewBinding
    private lateinit var setVolumeViewBinding: CommandVolumeViewBinding
    private lateinit var restartAppViewBinding: CommandRestartAppViewBinding
    private lateinit var showDebugInfoBinding: CommandDebugInfoBinding
    private lateinit var saveFileViewBinding: CommandSaveFileViewBinding
    private lateinit var telemetryViewBinding: CommandTelemetryViewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCommandBinding.inflate(inflater, container, false)
        val view = binding.root

        with(view) {
            tuneBinding = CommandTuneViewBinding.bind(this)
            selectServiceBinding = CommandSelectServiceViewBinding.bind(this)
            setTestCaseBinding = CommandTestcaseViewBinding.bind(this)
            setVolumeViewBinding = CommandVolumeViewBinding.bind(this)
            restartAppViewBinding = CommandRestartAppViewBinding.bind(this)
            showDebugInfoBinding = CommandDebugInfoBinding.bind(this)
            saveFileViewBinding = CommandSaveFileViewBinding.bind(this)
            telemetryViewBinding = CommandTelemetryViewBinding.bind(this)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            initialSavedViewStates()
        }

        with(binding) {
            buttonPing.setOnClickListener(this@CommandFragment)
            tuneBinding.buttonTune.setOnClickListener(this@CommandFragment)
            selectServiceBinding.buttonSelectService.setOnClickListener(this@CommandFragment)
            setTestCaseBinding.buttonApplyTest.setOnClickListener(this@CommandFragment)
            setTestCaseBinding.buttonClearTest.setOnClickListener(this@CommandFragment)
            setVolumeViewBinding.buttonVolume.setOnClickListener(this@CommandFragment)
            buttonRebootDevice.setOnClickListener(this@CommandFragment)
            restartAppViewBinding.buttonRestartApp.setOnClickListener(this@CommandFragment)
            showDebugInfoBinding.buttonShowDebugInfo.setOnClickListener(this@CommandFragment)
            buttonShowNetworkInfo.setOnClickListener(this@CommandFragment)
            saveFileViewBinding.buttonWriteToFile.setOnClickListener(this@CommandFragment)
            telemetryViewBinding.buttonSetTelemetry.setOnClickListener(this@CommandFragment)
        }

    }

    private fun saveViewStates() {
        sharedPreferences.edit().apply {
            binding.checkboxGlobalCommand.let { checkBox ->
                putBoolean(checkBox.strId, checkBox.isChecked)
            }

            saveEditTextInput(this, tuneBinding.editTextTune)
            saveEditTextInput(this, selectServiceBinding.editTextServiceName)
            saveEditTextInput(this, selectServiceBinding.editTextBsId)
            saveEditTextInput(this, setTestCaseBinding.editTextTestCase)

            setVolumeViewBinding.seekBarVolume.let { seekBar ->
                putInt(seekBar.strId, seekBar.progress)
            }

            saveEditTextInput(this, restartAppViewBinding.editTextRestartApp)

            showDebugInfoBinding.let {
                putBoolean(it.checkboxDebug.strId, it.checkboxDebug.isChecked)
                putBoolean(it.checkboxPhy.strId, it.checkboxPhy.isChecked)
            }

            saveEditTextInput(this, saveFileViewBinding.editTextFileName)
            saveEditTextInput(this, saveFileViewBinding.editTextWritingDuration)
            saveEditTextInput(this, telemetryViewBinding.editTextTelemetryNames)
            saveEditTextInput(this, telemetryViewBinding.editTextTelemetryDelay)

            telemetryViewBinding.checkboxTelemetryEnable.let {
                putBoolean(it.strId, it.isChecked)
            }

        }.apply()
    }

    private fun initialSavedViewStates() {
        binding.checkboxGlobalCommand.apply {
            isChecked = sharedPreferences.getBoolean(id.toString(), false)
        }

        restoreEditTextInputValue(tuneBinding.editTextTune)
        restoreEditTextInputValue(selectServiceBinding.editTextServiceName)
        restoreEditTextInputValue(selectServiceBinding.editTextBsId)
        restoreEditTextInputValue(setTestCaseBinding.editTextTestCase)

        setVolumeViewBinding.seekBarVolume.apply {
            progress = sharedPreferences.getInt(id.toString(), DEFAULT_VALUE)
        }

        restoreEditTextInputValue(restartAppViewBinding.editTextRestartApp)

        showDebugInfoBinding.checkboxDebug.apply {
            isChecked = sharedPreferences.getBoolean(id.toString(), false)
        }
        showDebugInfoBinding.checkboxPhy.apply {
            isChecked = sharedPreferences.getBoolean(id.toString(), false)
        }

        restoreEditTextInputValue(saveFileViewBinding.editTextFileName)
        restoreEditTextInputValue(saveFileViewBinding.editTextWritingDuration)

        telemetryViewBinding.checkboxTelemetryEnable.apply {
            isChecked = sharedPreferences.getBoolean(id.toString(), false)
        }
        restoreEditTextInputValue(telemetryViewBinding.editTextTelemetryNames)
        restoreEditTextInputValue(telemetryViewBinding.editTextTelemetryDelay)

    }

    private fun saveEditTextInput(editor: SharedPreferences.Editor, editText: EditText) {
        editor.putString(editText.strId, editText.text.toString())
    }

    private fun restoreEditTextInputValue(editText: EditText) {
        editText.setText(sharedPreferences.getString(editText.strId, ""))
    }

    override fun onStop() {
        saveViewStates()
        super.onStop()
    }

    override fun onClick(view: View) {
        if (!binding.checkboxGlobalCommand.isChecked && sharedViewModel.chartDevicesWithFlow.value.isNullOrEmpty()) {
            showToast(getString(R.string.no_selected_devices))
            return
        }

        when (view.id) {
            binding.buttonPing.id -> sendPingCommand()
            tuneBinding.buttonTune.id -> sendTuneCommand()
            selectServiceBinding.buttonSelectService.id -> sendSelectServiceCommand()
            setTestCaseBinding.buttonApplyTest.id -> sendTestCaseCommand()
            setTestCaseBinding.buttonClearTest.id -> clearTestCaseCommand()
            setVolumeViewBinding.buttonVolume.id -> sendVolumeCommand()
            binding.buttonRebootDevice.id -> sendRebootDeviceCommand()
            restartAppViewBinding.buttonRestartApp.id -> sendRestartAppCommand()
            showDebugInfoBinding.buttonShowDebugInfo.id -> sendShowDebugInfoCommand()
            binding.buttonShowNetworkInfo.id -> sendShowNetworkInfoCommand()
            saveFileViewBinding.buttonWriteToFile.id -> sendSaveToFileCommand()
            telemetryViewBinding.buttonSetTelemetry.id -> sendSetTelemetryCommand()
        }
    }

    private fun sendPingCommand() {
        sendCommand("ping", null)
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

    private fun sendTestCaseCommand() {
        val testCase = setTestCaseBinding.editTextTestCase.text?.toString()
        val arguments = JSONObject().apply {
            put("case", testCase)
        }
        sendCommand("setTestCase", arguments)
    }

    private fun clearTestCaseCommand() {
        setTestCaseBinding.editTextTestCase.setText("")
        sendTestCaseCommand()
    }

    private fun sendVolumeCommand() {
        val arguments = JSONObject().apply {
            put("value", setVolumeViewBinding.seekBarVolume.progress)
        }
        sendCommand("volume", arguments)
    }

    private fun showWarningDialog(messageId: Int, action: () -> Unit) {
        AlertDialog.Builder(requireContext()).apply {
            val devices = getDevicesCount()
            setMessage(getString(messageId, devices))
            setPositiveButton(getString(R.string.dialog_ok)) { _, _ -> action() }
            setNegativeButton(getString(R.string.dialog_cancel)) { _, _ -> }
        }.show()
    }

    private fun getDevicesCount(): Int {
        return if (binding.checkboxGlobalCommand.isChecked) {
            sharedViewModel.deviceIdList.value?.size ?: 0
        } else {
            sharedViewModel.chartDevicesWithFlow.value?.size ?: 0
        }
    }

    private fun sendRebootDeviceCommand() {
        showWarningDialog(R.string.reboot_device_warning) {
            sendCommand("rebootDevice", null)
        }
    }

    private fun sendRestartAppCommand() {
        showWarningDialog(R.string.restart_app_warning) {
            val arguments =
                restartAppViewBinding.editTextRestartApp.text?.toString()?.toLongOrNull()?.let { seconds ->
                    JSONObject().apply {
                        put("startDelay", TimeUnit.SECONDS.toMillis(seconds))
                    }
                }
            sendCommand("restartApp", arguments)
        }
    }

    private fun sendShowDebugInfoCommand() {
        val arguments = JSONObject().apply {
            put("debug", showDebugInfoBinding.checkboxDebug.isChecked)
            put("phy", showDebugInfoBinding.checkboxPhy.isChecked)
        }
        sendCommand("showDebugInfo", arguments)
    }

    private fun sendShowNetworkInfoCommand() {
        sendCommand("networkInfo", null)
    }

    private fun sendSaveToFileCommand() {
        val fileName = saveFileViewBinding.editTextFileName.text?.toString()
        if (fileName.isNullOrBlank()) return
        val arguments = JSONObject().apply {
            put("name", fileName)
            saveFileViewBinding.editTextWritingDuration.text?.toString()?.toIntOrNull()?.let { duration ->
                put("duration", duration)
            }
        }
        sendCommand("fileWriter", arguments)
    }

    private fun sendSetTelemetryCommand() {
        val arguments = JSONObject().apply {
            put("enable", telemetryViewBinding.checkboxTelemetryEnable.isChecked)
            telemetryViewBinding.editTextTelemetryDelay.text?.toString()?.toIntOrNull()?.let { telemetryDelay ->
                put("delay", telemetryDelay)
            }
            telemetryViewBinding.editTextTelemetryNames.text?.let { sensors ->
                put("name", sensors)
            }
        }

        sendCommand("enableTelemetry", arguments)
    }

    private fun sendCommand(command: String, arguments: JSONObject?) {
        if (binding.checkboxGlobalCommand.isChecked) {
            showWarningDialog(R.string.you_send_command_to_all_devices) {
                Intent(context, ScoreboardService::class.java).apply {
                    action = ScoreboardService.ACTION_GLOBAL_COMMANDS
                    putExtra(TOPIC, command)
                    val payload = arguments ?: JSONObject()
                    putExtra(ScoreboardService.COMMAND_EXTRAS, payload.toString())
                }.run {
                    sendToService(this)
                }
            }
        } else {
            val devices = sharedViewModel.chartDevicesWithFlow.value?.map { device ->
                device.id
            } ?: return

            Intent(context, ScoreboardService::class.java).apply {
                action = ScoreboardService.ACTION_COMMANDS
                val jsonObject = JSONObject().apply {
                    put(ACTION, command)
                    put(CONTROL_ARGUMENTS, arguments)
                }

                putExtra(ScoreboardService.COMMAND_EXTRAS, jsonObject.toString())
                putStringArrayListExtra(ScoreboardService.DEVICES_EXTRAS, arrayListOf(*devices.toTypedArray()))
            }.run {
                sendToService(this)
            }
        }

    }

    private fun sendToService(intent: Intent) {
        requireContext().startService(intent)
        showToast(getString(R.string.command_has_been_sent))
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ACTION = "action"
        private const val DEFAULT_VALUE = 50
        private const val CONTROL_ARGUMENTS = "arguments"
        const val TOPIC = "topic"
    }

    private val View.strId: String
        get() {
            return id.toString()
        }
}
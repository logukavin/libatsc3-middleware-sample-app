package com.nextgenbroadcast.mobile.view

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.get
import androidx.core.view.size
import androidx.fragment.app.DialogFragment
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider
import com.google.android.exoplayer2.ui.TrackNameProvider
import com.nextgenbroadcast.mobile.player.MediaTrackDescription
import com.nextgenbroadcast.mobile.player.MediaRendererType

class TrackSelectionDialog : DialogFragment() {
    private var titleStr = ""
    private var onClickListener: DialogInterface.OnClickListener? = null
    private var onDismissListener: DialogInterface.OnDismissListener? = null

    private var trackFormats: Map<MediaRendererType, List<MediaTrackDescription>>? = null

    private var trackNameProvider: TrackNameProvider? = null
    private var radioGroupMap = mutableMapOf<Int, RadioGroup>()
    private var overridesMap = mutableListOf<Int>()
    private var disabledRendersSet: MutableSet<Int> = mutableSetOf()

    fun init(titleStr: String, formats: Map<MediaRendererType, List<MediaTrackDescription>>,
             onClickListener: DialogInterface.OnClickListener, onDismissListener: DialogInterface.OnDismissListener
    ) {
        this.titleStr = titleStr
        this.onClickListener = onClickListener
        this.onDismissListener = onDismissListener

        trackFormats = formats
    }

    fun isDisabled(trackIndex: Int): Boolean {
        return disabledRendersSet.contains(trackIndex)
    }

    fun isSelected(trackIndex: Int): Boolean {
        return overridesMap.contains(trackIndex)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AppCompatDialog(activity, R.style.TrackSelectionDialogThemeOverlay).apply {
            setTitle(titleStr)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.onDismiss(dialog)
    }

    private fun showTrackSelections() {
        val formats = trackFormats ?: return
        val layoutInflater = LayoutInflater.from(context)
        val llTrackSelectionContainer = requireView().findViewById<ViewGroup>(R.id.llTrackSelectionContainer)

        formats.forEach { (type, renderers) ->
            var hasSelection = false
            val trackType = type.value

            var radioButtonGroup: RadioGroup? = radioGroupMap[trackType]
            if (radioButtonGroup == null) {
                val titleTextView = layoutInflater.inflate(R.layout.track_text_view, llTrackSelectionContainer, false) as TextView
                titleTextView.text = getTrackTypeString(trackType)
                llTrackSelectionContainer.addView(titleTextView)
                radioButtonGroup = layoutInflater.inflate(R.layout.track_radio_group, llTrackSelectionContainer, false) as RadioGroup
                val radioButton = layoutInflater.inflateRadioButton(llTrackSelectionContainer, getString(R.string.exo_track_selection_none), 0)
                radioButtonGroup.addView(radioButton)
                radioGroupMap[trackType] = radioButtonGroup
            }

            renderers.forEach { (trackFormat, isSelected, trackIndex) ->
                val radioButton = layoutInflater.inflateRadioButton(llTrackSelectionContainer, trackNameProvider?.getTrackName(trackFormat), trackIndex)
                radioButtonGroup.addView(radioButton)

                /** set selection in radioButton  */
                if (!disabledRendersSet.contains(trackIndex) && isSelected) {
                    hasSelection = true
                    radioButtonGroup.check(radioButton.id)
                }

                if (radioButtonGroup.parent == null) {
                    llTrackSelectionContainer.addView(radioButtonGroup)
                }
            }

            if (!hasSelection) {
                radioButtonGroup.check(radioButtonGroup[0].id)
            }
        }
    }

    private fun LayoutInflater.inflateRadioButton(root: ViewGroup, title: String?, trackIndex: Int): RadioButton {
        return (inflate(R.layout.track_radio_button, root, false) as RadioButton).apply {
            text = title
            tag = trackIndex
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val dialogView = inflater.inflate(R.layout.track_selection_dialog, container, false)

        val cancelButton = dialogView.findViewById<Button>(R.id.track_selection_dialog_cancel_button)
        val okButton = dialogView.findViewById<Button>(R.id.track_selection_dialog_ok_button)
        cancelButton.setOnClickListener { view: View? -> dismiss() }
        okButton.setOnClickListener {
            onClickListener?.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
            dismiss()
        }

        return dialogView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackNameProvider = DefaultTrackNameProvider(resources)
        showTrackSelections()
    }
    private fun getTrackTypeString(trackType: Int): String {
        return when (trackType) {
            C.TRACK_TYPE_AUDIO -> resources.getString(R.string.exo_track_selection_title_audio)
            C.TRACK_TYPE_TEXT -> resources.getString(R.string.exo_track_selection_title_text)
            else -> throw IllegalArgumentException()
        }
    }

    fun updateOverrides() {
        overridesMap.clear()

        radioGroupMap.values.forEach { radioGroup ->
            val isNoneSelected = (radioGroup[0] as RadioButton).isChecked
            val hasSelection = radioGroup.checkedRadioButtonId >= 0
            for (i in 1 until radioGroup.size) {
                val radioButton = radioGroup[i] as RadioButton

                if (radioButton.tag is Int) {
                    val trackIndex = radioButton.tag as Int
                    if (isNoneSelected) {
                        disabledRendersSet.add(trackIndex)
                    } else if (hasSelection) {
                        if (radioButton.isChecked) {
                            disabledRendersSet.remove(trackIndex)
                            overridesMap.add(trackIndex)
                        } else {
                            disabledRendersSet.add(trackIndex)
                        }
                    } else {
                        disabledRendersSet.remove(trackIndex)
                    }
                }
            }
        }
    }

    init {
        // Retain instance across activity re-creation to prevent losing access to init data.
        retainInstance = true
    }
}
package com.nextgenbroadcast.mobile.middleware.service.holder

import android.content.Context
import android.net.Uri
import com.nextgenbroadcast.mobile.middleware.service.SrtConfigReader
import java.io.File
import java.util.*

class SrtListHolder(
        private val context: Context,
) {
    private val externalSrtServices: MutableList<Triple<String, String, Boolean>> = mutableListOf()

    fun open() {
        context.contentResolver.openAssetFileDescriptor(Uri.fromFile(File(EXTERNAL_FILE_PATH)), "r")?.let { file ->
            externalSrtServices.addAll(
                    SrtConfigReader.readSrtListFromFile(file)
            )
        }
    }

    fun getDefaultRoute(): String? {
        val list = externalSrtServices.filter { (_, _, default) ->
            default
        }
        return if (list.isNotEmpty()) {
            list.joinToString("\n") { (_, path, _) ->
                path
            }
        } else null
    }

    fun getFullSrtList(): List<Triple<String, String, String>> {
        val additional = externalSrtServices.map { (title, path, _) ->
            Triple(title, path, UUID.randomUUID().toString())
        }

        return sourceList + additional
    }

    companion object {
        const val EXTERNAL_FILE_PATH = "/sdcard/srt.conf"

        val sourceList = listOf(
                Triple("las", "srt://las.srt.atsc3.com:31350?passphrase=A166AC45-DB7C-4B68-B957-09B8452C76A4", "A166AC45-DB7C-4B68-B957-09B8452C76A4"),
                Triple("bna", "srt://bna.srt.atsc3.com:31347?passphrase=88731837-0EB5-4951-83AA-F515B3BEBC20", "88731837-0EB5-4951-83AA-F515B3BEBC20"),
                Triple("slc", "srt://slc.srt.atsc3.com:31341?passphrase=B9E4F7B8-3CDD-4BA2-ACA6-13088AB855C0", "B9E4F7B8-3CDD-4BA2-ACA6-13088AB855C0"),
                Triple("lab", "srt://lab.srt.atsc3.com:31340?passphrase=03760631-667B-4ADB-9E04-E4491B0A7CF1", "03760631-667B-4ADB-9E04-E4491B0A7CF1"),
                Triple("qa", "srt://lab.srt.atsc3.com:31347?passphrase=f51e5a22-9b73-4ec8-be84-e4c173f1d913", "f51e5a22-9b73-4ec8-be84-e4c173f1d913"),
                Triple("labJJ", "srt://lab.srt.atsc3.com:31346?passphrase=055E0771-97B2-4447-8B5C-3B2497D0DE32", "055E0771-97B2-4447-8B5C-3B2497D0DE32"),
                Triple("labJJPixel5", "srt://lab.srt.atsc3.com:31348?passphrase=3D5E5ED2-700D-443B-968F-598DB9A2750D&packetfilter=fec", "3D5E5ED2-700D-443B-968F-598DB9A2750D"),
                Triple("seaJJAndroid", "srt://sea.srt.atsc3.com:31346?passphrase=055E0771-97B2-4447-8B5C-3B2497D0DE32", "055E0771-97B2-4447-8B5C-3B2497D0DE32")
        )

    }
}
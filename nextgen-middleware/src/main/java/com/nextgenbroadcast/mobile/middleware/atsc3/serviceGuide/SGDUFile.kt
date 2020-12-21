package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide

import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGFragmentEncoding
import okio.ByteString.Companion.toByteString
import java.io.File
import java.io.RandomAccessFile

@ExperimentalUnsignedTypes
internal class SGDUFile {
    private val readBuffer = ByteArray(8)

    private var duOffsets: UIntArray? = null

    private fun RandomAccessFile.readUInt32(): UInt {
        readFully(readBuffer, 0, 4)
        return ((readBuffer[0].toUInt() and 0xFFu) shl 24) or
                ((readBuffer[1].toUInt() and 0xFFu) shl 16) or
                ((readBuffer[2].toUInt() and 0xFFu) shl 8) or
                (readBuffer[3].toUInt() and 0xFFu)
    }

    private fun RandomAccessFile.readUInt24(): UInt {
        readFully(readBuffer, 0, 3)
        return ((readBuffer[0].toUInt() and 0xFFu) shl 16) or
                ((readBuffer[1].toUInt() and 0xFFu) shl 8) or
                (readBuffer[2].toUInt() and 0xFFu)
    }

    fun readXml(file: File, index: Int): String? {
        if (!file.exists() || index < 0) return null

        RandomAccessFile(file, "r").use { randomFile ->
            // Unit_Header
            val extensionOffset = randomFile.readUInt32()
            randomFile.skipBytes(2) // reserved
            val fragmentsCount = randomFile.readUInt24().toInt()

            if (index >= fragmentsCount) return null

            val offsets = UIntArray(fragmentsCount).also {
                duOffsets = it
            }
            for (i in 0 until fragmentsCount) {
                // skip fragmentTransportID[i] + fragmentVersion[i]
                randomFile.skipBytes(UInt.SIZE_BYTES + UInt.SIZE_BYTES)
                offsets[i] = randomFile.readUInt32()
            }

            randomFile.skipBytes(offsets[index].toInt())

            val fragmentEncoding = randomFile.readUnsignedByte()
            if (fragmentEncoding != SGFragmentEncoding.XML_OMA) return null

            randomFile.skipBytes(1) // skip fragmentType

            val size = if (index == 0) {
                offsets[index]
            } else if (index + 1 < offsets.size) {
                offsets[index + 1] - offsets[index]
            } else if (extensionOffset > 0uL) {
                extensionOffset - offsets[index]
            } else {
                randomFile.length().toUInt() - offsets[index]
            }

            val ba = ByteArray(size.toInt() - SGDUReader.OMA_XML_OFFSET)
            randomFile.read(ba)

            return ba.toByteString().utf8()
        }
    }
}
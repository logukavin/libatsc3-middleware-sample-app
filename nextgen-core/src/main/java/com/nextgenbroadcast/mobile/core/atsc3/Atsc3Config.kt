package com.nextgenbroadcast.mobile.core.atsc3

object Atsc3Config {
    const val A300_YEAR =   "2020"
    const val A300_MONTH =  "01"
    const val A300_DAY =    "07"

    val CAPABILITIES = listOf(
        // RMP
        0x0509, // ATSC 3.0 HEVC Video
        0x050B, // Dolby® AC4 Audio
        0x050D, // IMSC1 Text Profile
        0x0511, // ATSC 3.0 SDR with SL-HDR1 SEI
        // AMP
        0x0589, // ATSC 3.0 HEVC Video
        0x058B, // Dolby® AC4 Audio
        0x058D, // IMSC1 Text Profile
        0x0591, // ATSC 3.0 SDR with SL-HDR1 SEI
        // Interactive
        0x0700 // Interactive Content Environment
    )
}
package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import com.google.android.exoplayer2.util.Util;

public final class MMTDef {
    static final int SIZE_SAMPLE_HEADER = Byte.BYTES /*sampleType*/
            + Integer.BYTES /*sampleSize*/
            + Long.BYTES /*sampleTime*/;

    static final int SIZE_HEADER = Byte.BYTES /*video type*/
            + Byte.BYTES /*audio type*/
            + Byte.BYTES /*text type*/
            + Integer.BYTES /*video width*/
            + Integer.BYTES /*video height*/
            + Float.BYTES /*video frame rate*/
            + Integer.BYTES /*video initial data length*/
            + Integer.BYTES /*audio channel count*/
            + Integer.BYTES /*audio sample rate*/;

    static final byte TRACK_UNDEFINED = 0;
    static final byte TRACK_VIDEO_HEVC = 1;
    static final byte TRACK_AUDIO_AC4 = 1;
    static final byte TRACK_TEXT_TTML = 1;

    static final byte[] mmtSignature = Util.getUtf8Bytes("#!MMT\n");
}

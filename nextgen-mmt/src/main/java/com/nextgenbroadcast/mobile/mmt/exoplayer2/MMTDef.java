package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import com.google.android.exoplayer2.util.Util;

public final class MMTDef {
    static final int SIZE_LENGTH = Integer.BYTES;
    static final int TIME_LENGTH = Long.BYTES;
    static final int SIZE_SAMPLE_HEADER = SIZE_LENGTH + TIME_LENGTH;
    static final int SIZE_HEADER = Integer.BYTES /*videoWidth*/ + Integer.BYTES /*videoHeight*/ + Float.BYTES /*frameRate*/ + Integer.BYTES /*initial data length*/;

    static final byte[] mmtSignature = Util.getUtf8Bytes("#!MMT\n");
}

package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import com.google.android.exoplayer2.util.Util;

public final class MMTDef {
    static final int SIZE_SAMPLE_HEADER =
            Byte.BYTES          /* sample type */
            + Integer.BYTES     /* sample size */
            + Long.BYTES        /* sample presentation time */
            + Byte.BYTES;       /* is key frame */

    static final int SIZE_HEADER =
            Integer.BYTES       /* video type - see Atom.java*/
            + Integer.BYTES     /* audio type - see Atom.java - Util.getIntegerCodeForString("ac-4");*/
            + Integer.BYTES     /* text type - see Atom.java - Util.getIntegerCodeForString("ac-4"); */
            + Integer.BYTES     /* video width */
            + Integer.BYTES     /* video height */
            + Float.BYTES       /* video frame rate */
            + Integer.BYTES     /* video initial data length */
            + Integer.BYTES     /* audio channel count */
            + Integer.BYTES     /* audio sample rate */
            + Long.BYTES;       /* default sample duration */

    static final byte TRACK_UNDEFINED = 0;
    static final byte TRACK_VIDEO_HEVC = 1;
    static final byte TRACK_TEXT_TTML = 1;

    static final byte[] mmtSignature = Util.getUtf8Bytes("#!MMT\n");
}

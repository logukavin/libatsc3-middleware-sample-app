package com.nextgenbroadcast.mobile.core.atsc3.mmt;

import java.nio.charset.StandardCharsets;

public final class MMTConstants {
    public static final String MIME_MMT_VIDEO = "video/mmt";
    public static final String MIME_MMT_AUDIO = "audio/mmt";

    public static final int SIZE_SAMPLE_HEADER =
            Byte.BYTES          /* sample type */
            + Integer.BYTES     /* sample size */
            + Long.BYTES        /* sample presentation time */
            + Byte.BYTES;       /* is key frame */

    public static final int SIZE_HEADER =
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

    public static final byte[] mmtSignature = "#!MMT\n".getBytes(StandardCharsets.UTF_8);

    public static final int TRACK_TYPE_UNKNOWN = -1;
    public static final int TRACK_TYPE_AUDIO = 1;
    public static final int TRACK_TYPE_VIDEO = 2;
    public static final int TRACK_TYPE_TEXT = 3;
}

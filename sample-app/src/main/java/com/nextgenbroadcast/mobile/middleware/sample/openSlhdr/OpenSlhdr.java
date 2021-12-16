package com.nextgenbroadcast.mobile.middleware.sample.openSlhdr;

import java.nio.ByteBuffer;

class OpenSlhdr {
    static {
        System.loadLibrary("openslhdr");
    }

    public native int decodeMetadata(ByteBuffer data, int position, int size);
}
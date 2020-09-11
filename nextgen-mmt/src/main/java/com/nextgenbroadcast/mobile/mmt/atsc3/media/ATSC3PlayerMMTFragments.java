package com.nextgenbroadcast.mobile.mmt.atsc3.media;

import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MfuByteBufferFragment;
import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MpuMetadata_HEVC_NAL_Payload;

import java.util.concurrent.LinkedBlockingDeque;

public class ATSC3PlayerMMTFragments {

    public static MpuMetadata_HEVC_NAL_Payload InitMpuMetadata_HEVC_NAL_Payload = null;

    public static LinkedBlockingDeque<MfuByteBufferFragment> mfuBufferQueueVideo = new LinkedBlockingDeque<>(); //Collections.synchronizedList(new ArrayList<MfuByteBufferFragment>());
    public static LinkedBlockingDeque<MfuByteBufferFragment> mfuBufferQueueAudio = new LinkedBlockingDeque<>(); //Collections.synchronizedList(new ArrayList<MfuByteBufferFragment>());
    public static LinkedBlockingDeque<MfuByteBufferFragment> mfuBufferQueueStpp = new LinkedBlockingDeque<>(); //Collections.synchronizedList(new ArrayList<MfuByteBufferFragment>());

}

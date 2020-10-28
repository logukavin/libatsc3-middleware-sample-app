package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.upstream.DefaultAllocator;

public class MMTLoadControl extends DefaultLoadControl {
    /* jjustman-2020-10-20 -

        testing:
            allow a 10s "MMT" back-buffer to avoid A/V skew failure in which a PTS discontinuty gap will
            prevent a/v sync from re-stabilizing and remain in "buffering" mode

            2020-10-20 18:50:59.027 8149-8293/com.nextgenbroadcast.mobile.middleware.sample E/AudioTrack: Discontinuity detected [expected 3203544229, got 3208049188]
            2020-10-20 18:50:59.027 8149-8293/com.nextgenbroadcast.mobile.middleware.sample W/AudioTimestampPoller: reset with updateState(STATE_INITIALZING)
            2020-10-20 18:50:59.036 8149-8293/com.nextgenbroadcast.mobile.middleware.sample D/AudioTrack: getTimestamp_l(60): device stall time corrected using current time 15290874930751
            ...

            2020-10-20 18:50:59.108 8149-8251/com.nextgenbroadcast.mobile.middleware.sample D/PushMfuByteBufferFragment:INFO:  packet_id: 200, mpu_sequence_number: 1601643412, setting audio_packet_statistics.extracted_sample_duration_us to follow video: 16683 * 2
            2020-10-20 18:50:59.158 8149-8293/com.nextgenbroadcast.mobile.middleware.sample W/AudioTrack: getTimestamp_l(60): retrograde timestamp time corrected, 15290967755396 < 15290986968158
            2020-10-20 18:51:00.090 8149-8251/com.nextgenbroadcast.mobile.middleware.sample D/PushMfuByteBufferFragment:INFO:  packet_id: 200, mpu_sequence_number: 1601643413, setting audio_packet_statistics.extracted_sample_duration_us to follow video: 16683 * 2
            2020-10-20 18:51:01.090 8149-8251/com.nextgenbroadcast.mobile.middleware.sample D/PushMfuByteBufferFragment:INFO:  packet_id: 200, mpu_sequence_number: 1601643414, setting audio_packet_statistics.extracted_sample_duration_us to follow video: 16683 * 2
            2020-10-20 18:51:02.100 8149-8251/com.nextgenbroadcast.mobile.middleware.sample D/PushMfuByteBufferFragment:INFO:  packet_id: 200, mpu_sequence_number: 1601643415, setting audio_packet_statistics.extracted_sample_duration_us to follow video: 16683 * 2
            2020-10-20 18:51:03.073 8149-8251/com.nextgenbroadcast.mobile.middleware.sample D/pushMfuByteBufferFragment: A: appending MFU: mpu_sequence_number: 1601643415, sampleNumber: 30, size: 393, mpuPresentationTimeUs: 3706582830, queueSize: 0
            2020-10-20 18:51:03.092 8149-8251/com.nextgenbroadcast.mobile.middleware.sample D/PushMfuByteBufferFragment:INFO:  packet_id: 200, mpu_sequence_number: 1601643416, setting audio_packet_statistics.extracted_sample_duration_us to follow video: 16683 * 2
            2020-10-20 18:51:03.313 8149-8251/com.nextgenbroadcast.mobile.middleware.sample D/pushMfuByteBufferFragment: V: appending MFU: mpu_sequence_number: 1601643416, sampleNumber: 12, size: 13575, mpuPresentationTimeUs: 3706799728, queueSize: 0
            2020-10-20 18:51:03.314 8149-8251/com.nextgenbroadcast.mobile.middleware.sample D/intf: atsc3_mmt_mpu_mfu_on_sample_complete_ndk: total mfu count: 192001, packet_id: 100, mpu: 1601643416, sample: 13, orig len: 95, len: 95
       */
    public MMTLoadControl() {
        super(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                /*DEFAULT_MIN_BUFFER_MS*/ 250,

                /*DEFAULT_MAX_BUFFER_MS*/ 250,

                /*DEFAULT_MAX_BUFFER_MS*/ 2000,

                /*DEFAULT_BUFFER_FOR_PLAYBACK_MS*/ 250,

                /*DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS*/ 250,

                /*DEFAULT_TARGET_BUFFER_BYTES*/ 1024,

                DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS,

                /*DEFAULT_BACK_BUFFER_DURATION_MS*/ 10000,

                /*DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME*/ true);
    }
}

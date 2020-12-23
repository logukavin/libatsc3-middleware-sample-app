package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Util;
import com.nextgenbroadcast.mobile.mmt.atsc3.media.MMTDataBuffer;

import org.ngbp.libatsc3.middleware.android.mmt.MfuByteBufferFragment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ConcurrentModificationException;

public class Atsc3MMTDataSource extends BaseDataSource {

    public static class MMTDataSourceException extends IOException {

        public MMTDataSourceException(Exception cause) {
            super(cause);
        }

    }

    private final MMTDataBuffer inputSource;
    private final ByteBuffer sampleHeaderBuffer = ByteBuffer.allocate(MMTDef.SIZE_SAMPLE_HEADER);

    @Nullable
    private Uri uri;
    private long bytesRemaining;
    private boolean opened;
    private boolean readSignature = true;
    private boolean readHeader = true;
    private boolean readSampleHeader = false;
    private MfuByteBufferFragment currentSample = null;

    public static int DEBUG_COUNTER = 0;
    private long lastComputedPresentationTimestampUs = 0;

    public Atsc3MMTDataSource(MMTDataBuffer dataSource) {
        super(/* isNetwork= */ false);
        inputSource = dataSource;
    }

    @Override
    public long open(DataSpec dataSpec) throws MMTDataSourceException {
        uri = dataSpec.uri;
        transferInitializing(dataSpec);

        try {
            inputSource.open();
        } catch (ConcurrentModificationException e) {
            throw new MMTDataSourceException(e);
        }

        if (dataSpec.length != C.LENGTH_UNSET) {
            bytesRemaining = dataSpec.length;
        } else {
            bytesRemaining = C.LENGTH_UNSET;
        }

        opened = true;
        transferStarted(dataSpec);

        return bytesRemaining;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) {
        /*
         * Before reading of sample data next steps MUST be performed
         * 1. once read and check source the signature
         * 2. once read the stream format initialization data
         * 3. read sample header before every sample
         */

        if (!opened || !inputSource.isActive()) {
            return C.LENGTH_UNSET;
        }

        // Read identification header
        if (readSignature) {
            ByteBuffer bb = ByteBuffer.allocate(MMTDef.mmtSignature.length);
            bb.put(MMTDef.mmtSignature);
            bb.rewind();

            int bytesToRead = Math.min(bb.remaining() - offset, readLength);
            bb.get(buffer, offset, bytesToRead);
            readSignature = bb.remaining() != 0;

            return bytesToRead;
        }

        // read stream initialization header
        if (readHeader) {
            // wait for initial MFU Metadata
            if (!inputSource.hasMpuMetadata()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    return 0;
                }

                if (!inputSource.hasMpuMetadata()) {
                    return 0;
                }
            }

            // wait for video or audio key frame
            if (!inputSource.skipUntilKeyFrame()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    return 0;
                }

                if (!inputSource.skipUntilKeyFrame()) {
                    return 0;
                }
            }

            if (offset == 0 && readLength == MMTDef.SIZE_HEADER) {
                // write stream Header data
                ByteBuffer bb = ByteBuffer.allocate(MMTDef.SIZE_HEADER);

                //jjustman-2020-12-02 - TODO - pass-thru fourcc from
                Integer videoFormat = Util.getIntegerCodeForString("hev1");
                Integer audioFormat = Util.getIntegerCodeForString("ac-4");
//                if(true) {
//                    audioFormat = Util.getIntegerCodeForString("mp4a");
//                    /* jjustman-2020-11-30 - needs special audio codec specific metadata */
//                }
                Integer ttmlFormat = Util.getIntegerCodeForString("stpp");
                bb.putInt(videoFormat)
                        .putInt(audioFormat)
                        .putInt(ttmlFormat)
                        .putInt(inputSource.getVideoWidth())
                        .putInt(inputSource.getVideoHeight())
                        .putFloat(inputSource.getVideoFrameRate())
                        .putInt(inputSource.getMpuMetadataSize())
                        .putInt(inputSource.getAudioChannelCount())
                        .putInt(inputSource.getAudioSampleRate())
                        .putLong(inputSource.ptsOffsetUs());
                bb.rewind();
                bb.get(buffer, 0, MMTDef.SIZE_HEADER);

                return MMTDef.SIZE_HEADER;
            } else {
                // write initial MFU Metadata
                int bytesToRead = inputSource.readMpuMetadata(buffer, offset, readLength);
                readHeader = bytesToRead != readLength;

                return bytesToRead;
            }
        }

        if (readLength == 0) {
            return 0;
        } else if (bytesRemaining == 0) {
            return C.RESULT_END_OF_INPUT;
        }

        // get next sample and fill it's header
        if (currentSample == null) {
            try {
                if ((currentSample = inputSource.poll(10)) == null) {
                    return 0;
                }
            } catch (InterruptedException e) {
                return 0;
            }

            byte sampleType = C.TRACK_TYPE_UNKNOWN;
            if (inputSource.isVideoSample(currentSample)) {
                sampleType = C.TRACK_TYPE_VIDEO;
            } else if (inputSource.isAudioSample(currentSample)) {
                sampleType = C.TRACK_TYPE_AUDIO;
            } else if (inputSource.isTextSample(currentSample)) {
                sampleType = C.TRACK_TYPE_TEXT;
            }

            long computedPresentationTimestampUs = inputSource.getPresentationTimestampUs(currentSample);

            if(lastComputedPresentationTimestampUs - computedPresentationTimestampUs > 10000000) {
                Log.w("MMTDataSource", "JJ: >>> packetId had timestamp wraparound!");
            }
            lastComputedPresentationTimestampUs = computedPresentationTimestampUs;

            if((DEBUG_COUNTER++) % 10 == 0) {
                Log.d("MMTDataSource", String.format("JJ: >>> packet_id: %d, computedPresentationTimestampUs: %d (%d) mpu_sequence number: %d, sample number: %d, debug_counter: %d",
                        currentSample.packet_id,
                        computedPresentationTimestampUs,
                        currentSample.get_safe_mfu_presentation_time_uS_computed(),
                        currentSample.mpu_sequence_number,
                        currentSample.sample_number,
                        DEBUG_COUNTER));
            }

//            Log.d("!!!", ">>> sample TimeUs: " + computedPresentationTimestampUs
//                    + ",  sample size: " + toProcessMfuByteBufferFragment.bytebuffer_length
//                    + ",  sample type: " + sampleType
//                    + ", sequence number: " + toProcessMfuByteBufferFragment.mpu_sequence_number);
            sampleHeaderBuffer.clear();
            sampleHeaderBuffer
                    .put(sampleType)
                    .putInt(currentSample.bytebuffer_length)
                    .putLong(computedPresentationTimestampUs)
                    .put(currentSample.sample_number == 1 ? (byte) 1 : (byte) 0);
            sampleHeaderBuffer.rewind();

            readSampleHeader = true;
        }

        // read the sample header
        if (readSampleHeader) {
            int bytesToRead = Math.min(sampleHeaderBuffer.remaining(), readLength);
            sampleHeaderBuffer.get(buffer, offset, bytesToRead);
            readSampleHeader = sampleHeaderBuffer.remaining() != 0;
            return bytesToRead;
        }

        // read the sample buffer
        int bytesToRead = Math.min(currentSample.myByteBuffer.remaining(), readLength);
        if (bytesToRead == 0) {
            currentSample = null;
            return C.RESULT_END_OF_INPUT;
        }

        currentSample.myByteBuffer.get(buffer, offset, bytesToRead);

        if (currentSample.myByteBuffer.remaining() == 0) {
            currentSample.unreferenceByteBuffer();
            currentSample = null;
        }

        bytesTransferred(bytesToRead);

        return bytesToRead;
    }

    @Nullable
    @Override
    public Uri getUri() {
        return uri;
    }

    @SuppressWarnings("Finally")
    @Override
    public void close() {
        uri = null;

        inputSource.close();

        if (currentSample != null) {
            currentSample.unreferenceByteBuffer();
            currentSample = null;
        }

        readSignature = true;
        readHeader = true;
        readSampleHeader = false;

        if (opened) {
            opened = false;
            transferEnded();
        }
    }

}

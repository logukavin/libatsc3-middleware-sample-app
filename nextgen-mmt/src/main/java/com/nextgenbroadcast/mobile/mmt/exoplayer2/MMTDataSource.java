package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Log;
import com.nextgenbroadcast.mobile.mmt.atsc3.media.MMTDataBuffer;

import org.ngbp.libatsc3.middleware.android.ATSC3PlayerFlags;
import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MfuByteBufferFragment;
import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MmtPacketIdContext;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class MMTDataSource extends BaseDataSource {

    public static class TestExtractedMMTDataSourceException extends IOException {

        public TestExtractedMMTDataSourceException(IOException cause) {
            super(cause);
        }

    }

    private final MMTDataBuffer inputSource;

    @Nullable
    private Uri uri;
    private long bytesRemaining;
    private boolean opened;
    private boolean readSignature = true;
    private boolean readHeader = true;
    private boolean readSampleHeader = false;
    private ByteBuffer sampleHeaderBuffer = ByteBuffer.allocate(MMTDef.SIZE_SAMPLE_HEADER);
    private MfuByteBufferFragment toProcessMfuByteBufferFragment = null;

    public MMTDataSource(MMTDataBuffer dataSource) {
        super(/* isNetwork= */ false);
        inputSource = dataSource;
    }

    @Override
    public long open(DataSpec dataSpec) throws MMTDataSource.TestExtractedMMTDataSourceException {
        try {
            uri = dataSpec.uri;
            transferInitializing(dataSpec);

            ATSC3PlayerFlags.ATSC3PlayerStartPlayback = true;
            ATSC3PlayerFlags.ATSC3PlayerStopPlayback = false;
            ATSC3PlayerFlags.FirstMfuBufferVideoKeyframeSent = false;
            ATSC3PlayerFlags.FirstMfuBuffer_presentation_time_us_mpu = 0;

            //TODO: remove this...spinlock...
            while (!ATSC3PlayerFlags.ATSC3PlayerStopPlayback && !inputSource.hasMpuMetadata()) {
                Log.d("createMfuOuterMediaCodec", "waiting for initMpuMetadata_HEVC_NAL_Payload != null");
                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                    //
                }
            }

            //spin for at least one video and one audio frame
            while (!ATSC3PlayerFlags.ATSC3PlayerStopPlayback && (inputSource.mfuBufferQueueVideo.peek() == null || inputSource.mfuBufferQueueAudio.peek() == null)) {
                //Log.d("createMfuOuterMediaCodec", String.format("waiting for mfuBufferQueueVideo, size: %d, mfuBufferQueueAudio, size: %d", source.mfuBufferQueueVideo.size, dataSource.mfuBufferQueueAudio.size))
                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                    //
                }
            }

            //bail early
            if (ATSC3PlayerFlags.ATSC3PlayerStopPlayback) {
                throw new EOFException();
            }

            if (dataSpec.length != C.LENGTH_UNSET) {
                bytesRemaining = dataSpec.length;
            } else {
                bytesRemaining = C.LENGTH_UNSET;
            }

        } catch (IOException e) {
            throw new MMTDataSource.TestExtractedMMTDataSourceException(e);
        }

        opened = true;
        transferStarted(dataSpec);

        return bytesRemaining;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) {
        if (readSignature) {
            ByteBuffer bb = ByteBuffer.allocate(MMTDef.mmtSignature.length);
            bb.put(MMTDef.mmtSignature);
            bb.rewind();

            int bytesToRead = (int) Math.min(bb.remaining() - offset, readLength);
            bb.get(buffer, offset, bytesToRead);
            readSignature = bb.remaining() != 0;

            return bytesToRead;
        }

        if (readHeader) {
            ByteBuffer initBuffer = inputSource.InitMpuMetadata_HEVC_NAL_Payload.myByteBuffer;

            if (offset == 0 && readLength == MMTDef.SIZE_HEADER) {
                initBuffer.rewind();
                int size = initBuffer.remaining();

                int videoWidth = MmtPacketIdContext.video_packet_statistics.width > 0 ? MmtPacketIdContext.video_packet_statistics.width : MmtPacketIdContext.MmtMfuStatistics.FALLBACK_WIDTH;
                int videoHeight = MmtPacketIdContext.video_packet_statistics.height > 0 ? MmtPacketIdContext.video_packet_statistics.height : MmtPacketIdContext.MmtMfuStatistics.FALLBACK_HEIGHT;
                float frameRate = (float) 1000000.0 / MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us;

                ByteBuffer bb = ByteBuffer.allocate(MMTDef.SIZE_HEADER);
                bb.putInt(videoWidth).putInt(videoHeight).putFloat(frameRate).putInt(size);
                bb.rewind();
                bb.get(buffer, 0, MMTDef.SIZE_HEADER);

                return MMTDef.SIZE_HEADER;
            } else {
                int bytesToRead = (int) Math.min(initBuffer.remaining(), readLength);
                initBuffer.get(buffer, offset, bytesToRead);
                readHeader = initBuffer.remaining() != 0;

                return bytesToRead;
            }
        }

        if (readLength == 0) {
            return 0;
        } else if (bytesRemaining == 0) {
            return C.RESULT_END_OF_INPUT;
        }

        int bytesRead;

        inputSource.mfuBufferQueueAudio.clear();
        inputSource.mfuBufferQueueStpp.clear();

        if (toProcessMfuByteBufferFragment == null || toProcessMfuByteBufferFragment.myByteBuffer.remaining() == 0) {
            if (toProcessMfuByteBufferFragment != null) {
                toProcessMfuByteBufferFragment.unreferenceByteBuffer();
            }

            try {
                if ((toProcessMfuByteBufferFragment = inputSource.mfuBufferQueueVideo.poll(10, TimeUnit.MILLISECONDS)) == null) {
                    return 0;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (toProcessMfuByteBufferFragment != null) {
                long computedPresentationTimestampUs = inputSource.getPresentationTimestampUs(toProcessMfuByteBufferFragment);

//                Log.d("!!!", ">>> sample TimeUs: " + computedPresentationTimestampUs
//                        + ",  sample size: " + toProcessMfuByteBufferFragment.bytebuffer_length
//                        + ",  sample remaining: " + toProcessMfuByteBufferFragment.myByteBuffer.remaining()
//                        + ", sequence number: " + toProcessMfuByteBufferFragment.mpu_sequence_number);
                sampleHeaderBuffer.clear();
                sampleHeaderBuffer.putInt(toProcessMfuByteBufferFragment.bytebuffer_length).putLong(computedPresentationTimestampUs);
                sampleHeaderBuffer.rewind();

                readSampleHeader = true;
            }
        }

        if (!opened) return C.LENGTH_UNSET;

        if (readSampleHeader) {
            int bytesToRead = (int) Math.min(sampleHeaderBuffer.remaining(), readLength);
            sampleHeaderBuffer.get(buffer, offset, bytesToRead);
            readSampleHeader = sampleHeaderBuffer.remaining() != 0;
            return bytesToRead;
        }

        int bytesToRead = (int) Math.min(toProcessMfuByteBufferFragment.myByteBuffer.remaining(), readLength);
        if (bytesToRead == 0) {
            toProcessMfuByteBufferFragment = null;
            return C.RESULT_END_OF_INPUT;
        }

        toProcessMfuByteBufferFragment.myByteBuffer.get(buffer, offset, bytesToRead);
        bytesRead = bytesToRead;

        bytesTransferred(bytesRead);

        return bytesRead;
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

        ATSC3PlayerFlags.ATSC3PlayerStartPlayback = false;
        ATSC3PlayerFlags.ATSC3PlayerStopPlayback = true;
        inputSource.release();

        if (toProcessMfuByteBufferFragment != null) {
            toProcessMfuByteBufferFragment.unreferenceByteBuffer();
            toProcessMfuByteBufferFragment = null;
        }

        if (opened) {
            opened = false;
            transferEnded();
        }
    }

}

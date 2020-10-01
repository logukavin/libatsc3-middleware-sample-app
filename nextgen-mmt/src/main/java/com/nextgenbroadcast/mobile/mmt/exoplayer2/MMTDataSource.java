package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.nextgenbroadcast.mobile.mmt.atsc3.media.MMTDataBuffer;

import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MfuByteBufferFragment;

import java.nio.ByteBuffer;

public class MMTDataSource extends BaseDataSource {

    private final MMTDataBuffer inputSource;

    @Nullable
    private Uri uri;
    private long bytesRemaining;
    private boolean opened;
    private boolean readSignature = true;
    private boolean readHeader = true;
    private boolean readSampleHeader = false;
    private ByteBuffer sampleHeaderBuffer = ByteBuffer.allocate(MMTDef.SIZE_SAMPLE_HEADER);
    private MfuByteBufferFragment currentSample = null;

    public MMTDataSource(MMTDataBuffer dataSource) {
        super(/* isNetwork= */ false);
        inputSource = dataSource;
    }

    @Override
    public long open(DataSpec dataSpec) {
        uri = dataSpec.uri;
        transferInitializing(dataSpec);

        inputSource.open();

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
            if (!inputSource.hasMpuMetadata()) {
                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                    //
                }

                if (!inputSource.hasMpuMetadata()) {
                    return 0;
                }
            }

            ByteBuffer initBuffer = inputSource.InitMpuMetadata_HEVC_NAL_Payload.myByteBuffer;

            if (offset == 0 && readLength == MMTDef.SIZE_HEADER) {
                initBuffer.rewind();
                int initDataSize = initBuffer.remaining();

                ByteBuffer bb = ByteBuffer.allocate(MMTDef.SIZE_HEADER);
                bb.put(MMTDef.TRACK_VIDEO_HEVC)
                        .put(MMTDef.TRACK_AUDIO_AC4)
                        .put(MMTDef.TRACK_TEXT_TTML)
                        .putInt(inputSource.getVideoWidth())
                        .putInt(inputSource.getVideoHeight())
                        .putFloat(inputSource.getVideoFrameRate())
                        .putInt(initDataSize)
                        .putInt(inputSource.getAudioChannelCount())
                        .putInt(inputSource.getAudioSampleRate());
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

        if (currentSample == null) {
            try {
                if ((currentSample = inputSource.poll(10)) == null) {
                    return 0;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (currentSample != null) {
                byte sampleType = C.TRACK_TYPE_UNKNOWN;
                if (inputSource.isVideoSample(currentSample)) {
                    sampleType = C.TRACK_TYPE_VIDEO;
                } else if (inputSource.isAudioSample(currentSample)) {
                    sampleType = C.TRACK_TYPE_AUDIO;
                } else if (inputSource.isTextSample(currentSample)) {
                    sampleType = C.TRACK_TYPE_TEXT;
                }

                long computedPresentationTimestampUs = inputSource.getPresentationTimestampUs(currentSample);

//                Log.d("!!!", ">>> sample TimeUs: " + computedPresentationTimestampUs
//                        + ",  sample size: " + toProcessMfuByteBufferFragment.bytebuffer_length
//                        + ",  sample type: " + sampleType
//                        + ", sequence number: " + toProcessMfuByteBufferFragment.mpu_sequence_number);
                sampleHeaderBuffer.clear();
                sampleHeaderBuffer
                        .put(sampleType)
                        .putInt(currentSample.bytebuffer_length)
                        .putLong(computedPresentationTimestampUs)
                        .put(currentSample.sample_number == 1 ? (byte) 1 : (byte) 0);
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

        int bytesToRead = (int) Math.min(currentSample.myByteBuffer.remaining(), readLength);
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

        inputSource.release();

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

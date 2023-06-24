package com.nextgenbroadcast.mobile.middleware.provider.mmt;


import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.MMTClockAnchor;
import com.google.protobuf.Parser;
import com.nextgenbroadcast.mobile.core.LOG;
import com.nextgenbroadcast.mobile.middleware.BuildConfig;
import com.nextgenbroadcast.mobile.middleware.atsc3.buffer.Atsc3RingBuffer;
import com.nextgenbroadcast.mobile.player.MMTConstants;

import org.bouncycastle.util.encoders.Hex;
import org.ngbp.libatsc3.middleware.Atsc3NdkMediaMMTBridge;
import org.ngbp.libatsc3.middleware.android.mmt.MmtPacketIdContext;
import org.ngbp.libatsc3.middleware.android.mmt.MpuMetadata_HEVC_NAL_Payload;
import org.ngbp.libatsc3.middleware.android.mmt.models.MMTAudioDecoderConfigurationRecord;
import org.ngbp.libatsc3.middleware.mmt.pb.MmtAudioProperties;
import org.ngbp.libatsc3.middleware.mmt.pb.MmtCaptionProperties;
import org.ngbp.libatsc3.middleware.mmt.pb.MmtMpTable;
import org.ngbp.libatsc3.middleware.mmt.pb.MmtRingBufferHeaders;
import org.ngbp.libatsc3.middleware.mmt.pb.MmtVideoProperties;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MMTFragmentWriter {
    public static final String TAG = MMTFragmentWriter.class.getSimpleName();
    private static final boolean MMT_DEBUG_LOGGING_ENABLED = BuildConfig.MMTDebugLoggingEnabled;
    private static final int MAX_FIRST_MFU_WAIT_TIME = 5000;

    public static final String AC_4_ID = MMTAudioDecoderConfigurationRecord.AC_4_ID;
    private static final int AC_4_CODE = getIntegerCodeForString(MMTAudioDecoderConfigurationRecord.AC_4_ID);

    private static final int TRACK_HEADER_BUFFER_SIZE_DEFAULT = 512;

    //ac-4 sync frame header
    private final byte[] ac4header = {(byte) 0xAC, 0x40, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00};

    // SIZE_SAMPLE_HEADER
    private final byte[] emptyFragmentHeader = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    private final int MAX_SAMPLE_HEADER_SIZE = MMTConstants.SIZE_SAMPLE_HEADER + ac4header.length;
    private final int SAMPLE_HEADER_OFFSET = MAX_SAMPLE_HEADER_SIZE - Atsc3RingBuffer.RING_BUFFER_PAGE_HEADER_SIZE;

    private final Boolean audioOnly;
    private final int serviceId;
    private final Atsc3RingBuffer ringBuffer;
    private final ByteBuffer fragmentBuffer = ByteBuffer.allocate(1024 * 1024); //jjustman-2022-02-16 - was 1024 * 1024);

    private final ArrayMap<Integer, MMTAudioDecoderConfigurationRecord> audioConfigurationMap = new ArrayMap<>();
    private final ArrayMap<Integer, Long> audioPacketIdMpuSequenceNumberMap = new ArrayMap<>();

    private final ArrayMap<Integer, MmtMpTable.MmtAssetRow> assetMapping = new ArrayMap<>();

    private final ArrayMap<String, MmtVideoProperties.MmtVideoPropertiesAsset> videoAssetMap = new ArrayMap<>();
    private final ArrayMap<String, MmtAudioProperties.MmtAudioPropertiesAsset> audioAssetMap = new ArrayMap<>();
    private final ArrayMap<String, MmtCaptionProperties.MmtCaptionPropertiesAsset> captionAssetMap = new ArrayMap<>();

    private ByteBuffer InitMpuMetadata_HEVC_NAL_Payload = null;
    private ByteBuffer headerBuffer;

    private long videoMfuPresentationTimestampUs = Long.MAX_VALUE;
    private final ArrayMap<Integer, Long> audioMfuPresentationTimestampMap = new ArrayMap<>();
    private long stppMfuPresentationTimestampUs = Long.MAX_VALUE;

    private long videoMfuPresentationTimestampUsMaxWraparoundValue = Long.MAX_VALUE;
    private long audioMfuPresentationTimestampUsMaxWraparoundValue = Long.MAX_VALUE;
    private long stppMfuPresentationTimestampUsMaxWraparoundValue  = Long.MAX_VALUE;

    private long minMfuPresentationTimestampUsMaxWraparoundValue = Long.MAX_VALUE;


    private volatile boolean isActive = true;
    private volatile boolean mpTableComplete = false;
    private boolean sendFileHeader = true;
    private boolean firstKeyFrameReceived = false;

    private long mpuWaitingStartTime;

    public MMTFragmentWriter(int serviceId, Atsc3RingBuffer fragmentBuffer, boolean audioOnly) {
        this.serviceId = serviceId;
        this.ringBuffer = fragmentBuffer;
        this.audioOnly = audioOnly;

        this.fragmentBuffer.limit(0);
    }

    public int getServiceId() {
        return serviceId;
    }

    public boolean isActive() {
        return isActive;
    }

    public int write(FileOutputStream out) throws IOException {
        if (!isActive) throw new IOException("Reader is not active");

        int bytesRead = 0;
        if (sendFileHeader) {
            if (!audioOnly && !hasMpuMetadata()) {
                if (mpuWaitingStartTime == 0) {
                    mpuWaitingStartTime = System.currentTimeMillis();
                }

                scanMpuMetadata(fragmentBuffer);

                if (!hasMpuMetadata()) {
                    if ((System.currentTimeMillis() - mpuWaitingStartTime) < MAX_FIRST_MFU_WAIT_TIME) {
                        return 0;
                    } else {
                        throw new IOException("Can't get MPU Metadata");
                    }
                }
            }

            if (!mpTableComplete) {
                return 0;
            }

            if (headerBuffer == null) {
                headerBuffer = createFileHeader();
            }

            bytesRead += writeBuffer(out, headerBuffer);
            out.flush();

            sendFileHeader = false;
        }

        bytesRead += writeQueue(out);

        return bytesRead;
    }

    public void close() {
        isActive = false;
    }

    private ByteBuffer createFileHeader() {
        final byte[] EMPTY_ARRAY = new byte[0];

        int mpuMetadataSize = getMpuMetadataSize();

        ByteBuffer audioHeaderBuffer = ByteBuffer.allocate(TRACK_HEADER_BUFFER_SIZE_DEFAULT);
        ByteBuffer videoHeaderBuffer = ByteBuffer.allocate(audioOnly ? 0 : TRACK_HEADER_BUFFER_SIZE_DEFAULT);
        ByteBuffer captionHeaderBuffer = ByteBuffer.allocate(audioOnly ? 0 : TRACK_HEADER_BUFFER_SIZE_DEFAULT);

        for (MmtMpTable.MmtAssetRow asset : assetMapping.values()) {
            int packetId = asset.getPacketId();

            if (isAudioSample(packetId)) {
                int audioFormat = getIntegerCodeForString(asset.getType());
                MMTAudioDecoderConfigurationRecord info = audioConfigurationMap.get(packetId);
                MmtAudioProperties.MmtAudioPropertiesAsset properties = audioAssetMap.get(asset.getId());

                byte[] languageBytes = EMPTY_ARRAY;
                //TODO: implement multi-language support
                if (properties != null && properties.getPresentationCount() > 0) {
                    MmtAudioProperties.MmtAudioPropertiesPresentation presentation = properties.getPresentation(0);
                    if (presentation.getLanguageCount() > 0) {
                        languageBytes = presentation.getLanguage(0).getBytes(StandardCharsets.UTF_8);
                    }
                }

                int headerSize = MMTConstants.AUDIO_TRACK_HEADER_SIZE + languageBytes.length;
                audioHeaderBuffer = ensureBufferSize(audioHeaderBuffer, headerSize);
                audioHeaderBuffer
                        .putInt(headerSize)
                        .put((byte) MMTConstants.TRACK_TYPE_AUDIO)
                        .putInt(audioFormat)
                        .putInt(packetId)
                        .putInt(info != null ? info.channel_count : 0)
                        .putInt(info != null ? info.sample_rate : 0)
                        .putInt(languageBytes.length);
                if (languageBytes.length > 0) {
                    audioHeaderBuffer.put(languageBytes);
                }
            } else if (!audioOnly) {
                if (isVideoSample(packetId)) {
                    int videoFormat = getIntegerCodeForString(asset.getType());

                    // write initial MFU Metadata
                    InitMpuMetadata_HEVC_NAL_Payload.rewind();

                    int headerSize = MMTConstants.VIDEO_TRACK_HEADER_SIZE + mpuMetadataSize;
                    videoHeaderBuffer = ensureBufferSize(videoHeaderBuffer, headerSize);
                    videoHeaderBuffer
                            .putInt(headerSize)
                            .put((byte) MMTConstants.TRACK_TYPE_VIDEO)
                            .putInt(videoFormat)
                            .putInt(packetId)
                            .putInt(getVideoWidth()) //TODO: get from asset
                            .putInt(getVideoHeight()) //TODO: get from asset
                            .putFloat(getVideoFrameRate()) //TODO: get from asset
                            .putInt(mpuMetadataSize)
                            .put(InitMpuMetadata_HEVC_NAL_Payload);
                } else if (isTextSample(packetId)) {
                    int ttmlFormat = getIntegerCodeForString(asset.getType());
                    MmtCaptionProperties.MmtCaptionPropertiesAsset properties = captionAssetMap.get(asset.getId());

                    byte[] languageBytes = EMPTY_ARRAY;
                    if (properties != null) {
                        languageBytes = properties.getLanguage().getBytes(StandardCharsets.UTF_8);
                    }

                    int headerSize = MMTConstants.CC_TRACK_HEADER_SIZE;
                    captionHeaderBuffer = ensureBufferSize(captionHeaderBuffer, headerSize);
                    captionHeaderBuffer
                            .putInt(headerSize)
                            .put((byte) MMTConstants.TRACK_TYPE_TEXT)
                            .putInt(ttmlFormat)
                            .putInt(packetId)
                            .putInt(languageBytes.length);
                    if (languageBytes.length > 0) {
                        captionHeaderBuffer.put(languageBytes);
                    }
                }
            }
        }

        audioAssetMap.clear();
        videoAssetMap.clear();
        captionAssetMap.clear();

        audioHeaderBuffer.limit(audioHeaderBuffer.position());
        audioHeaderBuffer.rewind();
        videoHeaderBuffer.limit(videoHeaderBuffer.position());
        videoHeaderBuffer.rewind();
        captionHeaderBuffer.limit(captionHeaderBuffer.position());
        captionHeaderBuffer.rewind();

        int headerSize = MMTConstants.HEADER_SIZE
                + audioHeaderBuffer.limit()
                + videoHeaderBuffer.limit()
                + captionHeaderBuffer.limit();

        ByteBuffer fileHeaderBuffer = ByteBuffer.allocate(MMTConstants.mmtSignature.length + headerSize);
        fileHeaderBuffer.put(MMTConstants.mmtSignature);
        fileHeaderBuffer.putInt(headerSize);
        fileHeaderBuffer.put(audioHeaderBuffer);
        fileHeaderBuffer.put(videoHeaderBuffer);
        fileHeaderBuffer.put(captionHeaderBuffer);
        fileHeaderBuffer.rewind();

        return fileHeaderBuffer;
    }

    private ByteBuffer ensureBufferSize(ByteBuffer buff, int size) {
        if (buff.remaining() < size) {
            ByteBuffer newBuff = ByteBuffer.allocate(buff.capacity() + TRACK_HEADER_BUFFER_SIZE_DEFAULT);
            newBuff.put(buff.array(), 0, buff.position());
            return newBuff;
        }
        return buff;
    }

    private void writeEmptyFragment(FileOutputStream out, int fragmentType) throws IOException {
        emptyFragmentHeader[0] = (byte) fragmentType;
        out.write(emptyFragmentHeader,0 , emptyFragmentHeader.length);
        out.flush();
    }

    private int writeQueue(FileOutputStream out) throws IOException {
        // write empty fragment to buffer to check stream is still alive
        writeEmptyFragment(out, MMTConstants.TRACK_TYPE_EMPTY);

        int bytesRead = 0;
        while (isActive) {
            if (fragmentBuffer.remaining() == 0) {
                readFragment(fragmentBuffer);
                if(MMT_DEBUG_LOGGING_ENABLED) {
                    Log.d(TAG, String.format("writeQueue - calling readFragment(fragmentBuffer) -  fragmentBuffer.remaining: %d", fragmentBuffer.remaining()));
                }
            }

            // read the sample buffer
            int bytesToRead = fragmentBuffer.remaining();
            if (bytesToRead == 0) {
                break;
            }

            if(MMT_DEBUG_LOGGING_ENABLED) {
                Log.d(TAG, String.format("writeQueue - writeBuffer - with fragmentBuffer.remaining: %d", fragmentBuffer.remaining()));
            }
            bytesRead += writeBuffer(out, fragmentBuffer);

        }

        if(MMT_DEBUG_LOGGING_ENABLED) {
            Log.d(TAG, String.format("writeQueue - calling out.flush, total bytesRead: %d", bytesRead));
        }
        out.flush();

        return bytesRead;
    }

    private int writeBuffer(FileOutputStream out, ByteBuffer buffer) throws IOException {
        int bytesToWrite = buffer.remaining();
        //jjustman-2021-09-01-do we need to .sync() out?
        out.write(buffer.array(), buffer.position(), bytesToWrite);
        buffer.limit(0);
        return bytesToWrite;
    }

    private void readFragment(ByteBuffer buffer) {
        int retryCount = 5;
        while (true) {
            buffer.clear();
            buffer.position(SAMPLE_HEADER_OFFSET);
            int payloadSize = ringBuffer.readNextPage(Atsc3RingBuffer.RING_BUFFER_PAGE_FRAGMENT, buffer);

            if (payloadSize == Atsc3RingBuffer.RESULT_RETRY) {
                if (--retryCount >= 0) {
                    continue; // we skipped page in some reason, let's try again
                } else {
                    Log.w(TAG,"readFragment - failed retryCount with RESULT_RETRY");

                    buffer.limit(0);
                    Thread.yield();
                    return;
                }
            }

            if (payloadSize <= 0) {
                buffer.limit(0);
                if(MMT_DEBUG_LOGGING_ENABLED) {
                    Log.d(TAG,"readFragment - failed payloadSize <= 0");
                }
                Thread.yield();
                return;
            }

            int headerSize = ringBuffer.getInt(buffer);
            payloadSize -= Integer.BYTES; // skip headerSize
            MmtRingBufferHeaders.MmtFragmentHeader fragmentHeader = readFragmentHeader(buffer, headerSize);
            if (fragmentHeader == null) return;
            payloadSize -= headerSize; // skip header

            int service_id = fragmentHeader.getServiceId();
            if (service_id != serviceId) {
                // it's a bad sign, probably receiver switched to another Service or we read a fragment from the previous session
                buffer.limit(0);
                return;
            }

            int packet_id = fragmentHeader.getPacketId();
            int sample_number = fragmentHeader.getSampleNumber();
            long mpu_presentation_time_uS_from_SI = fragmentHeader.getPresentationUs();

            byte sampleType = MMTConstants.TRACK_TYPE_UNKNOWN;
            if (isVideoSample(packet_id)) {
                sampleType = MMTConstants.TRACK_TYPE_VIDEO;
            } else if (isAudioSample(packet_id)) {
                sampleType = MMTConstants.TRACK_TYPE_AUDIO;
            } else if (isTextSample(packet_id)) {
                sampleType = MMTConstants.TRACK_TYPE_TEXT;
            }

            // If it's AV stream skip all fragments except audio till video key frame received
            if (!audioOnly && !firstKeyFrameReceived) {
                if (sampleType == MMTConstants.TRACK_TYPE_VIDEO && isKeySample(sample_number)) {
                    firstKeyFrameReceived = true;
                } else {
                    //jjustman-2023-04-26 - hack! discard all samples until first video keyframe received
                    buffer.limit(0);
                    continue;
                }
//
//                else if (sampleType != MMTConstants.TRACK_TYPE_AUDIO) {
//                    buffer.limit(0);
//                    continue;
//                }
            }

            Long computedPresentationTimestampUs = getPresentationTimestampUs(packet_id, fragmentHeader.getSequenceNumber(), sample_number, mpu_presentation_time_uS_from_SI);
            //jjustman-2023-04-21 - provide computedPresentationTimestampUs from our mpu_presentation_time_uS_from_SI
            //Long computedPresentationTimestampUs = getPresentationTimestampUsNoAnchor(packet_id, fragmentHeader.getSequenceNumber(), sample_number, mpu_presentation_time_uS_from_SI);
            if(MMT_DEBUG_LOGGING_ENABLED) {
                Log.d(TAG, String.format("computedPresentationTimestampUs\tpacket_id\t%d\tmpu_sequence_number\t%d\tsample_number\t%d\tmpu_presentation_time_uS_from_SI\t%d\tcomputedPresentationTimestampUs\t%d",
                        packet_id, fragmentHeader.getSequenceNumber(), sample_number, mpu_presentation_time_uS_from_SI, computedPresentationTimestampUs));
            }

            if(computedPresentationTimestampUs == null) {
                //bail
                buffer.limit(0);
                return;
            }
            int headerDiff = buffer.position() - MMTConstants.SIZE_SAMPLE_HEADER;
            if (sampleType == MMTConstants.TRACK_TYPE_AUDIO) {
                MmtMpTable.MmtAssetRow asset = assetMapping.get(packet_id);

                //check if our packet_id flow is ac-4, and prepend sync frame header as needed
                if (asset != null && AC_4_ID.equals(asset.getType())) {
                    headerDiff -= ac4header.length;

                    ac4header[4] = (byte) (payloadSize >> 16 & 0xFF);
                    ac4header[5] = (byte) (payloadSize >> 8 & 0xFF);
                    ac4header[6] = (byte) (payloadSize & 0xFF);

                    buffer.position(headerDiff + MMTConstants.SIZE_SAMPLE_HEADER);
                    buffer.put(ac4header);

                    payloadSize += ac4header.length;
                }
            }

            buffer.position(headerDiff);
            buffer.put(sampleType)
                    .putInt(payloadSize)
                    .putInt(packet_id)
                    .putLong(computedPresentationTimestampUs)
                    .put(isKeySample(sample_number) ? (byte) 1 : (byte) 0);

            int dataSize = headerDiff + MMTConstants.SIZE_SAMPLE_HEADER + payloadSize;

            if(MMT_DEBUG_LOGGING_ENABLED) {
                Log.d(TAG, String.format("onmfu\tsampleType\t%d\tpacketId\t%d\tsampleNumber\t%d\tpresentationTimeUs\t%d",
                        sampleType, packet_id, sample_number, computedPresentationTimestampUs));
            }

            int limit = Math.max(dataSize, 0);
            buffer.limit(limit);
            buffer.position(headerDiff);

            return;
        }
    }

    private boolean hasMpuMetadata() {
        return InitMpuMetadata_HEVC_NAL_Payload != null;
    }

    private void scanMpuMetadata(ByteBuffer buffer) {
        int retryCount = 50;
        while (true) {
            buffer.clear();
            int payloadSize = ringBuffer.readNextPage(Atsc3RingBuffer.RING_BUFFER_PAGE_INIT, buffer);

            if (payloadSize == Atsc3RingBuffer.RESULT_RETRY) {
                if (--retryCount >= 0) {
                    continue; // we skipped page in some reason, let's try again
                } else {
                    buffer.limit(0);
                    return;
                }
            }

            if (payloadSize <= 0) {
                buffer.limit(0);
                return;
            }

            int headerSize = ringBuffer.getInt(buffer);
            MmtRingBufferHeaders.MmtFragmentHeader fragmentHeader = readFragmentHeader(buffer, headerSize);
            if (fragmentHeader == null) return;

            int service_id = fragmentHeader.getServiceId();
            // we read a fragment from the previous session, skip it
            if (service_id != serviceId) continue;

            if (InitMpuMetadata_HEVC_NAL_Payload == null) {
                int nalPayloadSize = payloadSize - headerSize - Integer.BYTES /* headerSize value */;
                ByteBuffer init = ByteBuffer.allocate(nalPayloadSize);
                buffer.get(init.array(), 0, nalPayloadSize);
                InitMpuMetadata_HEVC_NAL_Payload = init;
                LOG.e(TAG, String.format(Locale.US, "scanMpuMetadata: InitMpuMetadata_HEVC_NAL_Payload len: %d, is: %s", nalPayloadSize, Hex.toHexString(InitMpuMetadata_HEVC_NAL_Payload.array())));
            }

            buffer.limit(0);

            return;
        }
    }

    @Nullable
    private MmtRingBufferHeaders.MmtFragmentHeader readFragmentHeader(ByteBuffer buffer, int headerSize) {
        MmtRingBufferHeaders.MmtFragmentHeader fragmentHeader;
        try {
            // next method looks faster fragmentHeader = MmtRingBufferHeaders.MmtFragmentHeader.parseFrom(CodedInputStream.newInstance(buffer.array(), buffer.position(), headerSize));
            Parser<MmtRingBufferHeaders.MmtFragmentHeader> parser = MmtRingBufferHeaders.MmtFragmentHeader.getDefaultInstance().getParserForType();
            fragmentHeader = parser.parseFrom(buffer.array(), buffer.position(), headerSize);
            buffer.position(buffer.position() + headerSize);
        } catch (Exception e) {
            LOG.e(TAG, "Failed to parse Fragment header", e);
            buffer.limit(0);
            return null;
        }
        return fragmentHeader;
    }

    private int getVideoWidth() {
        return MmtPacketIdContext.video_packet_statistics.width > 0 ? MmtPacketIdContext.video_packet_statistics.width : MmtPacketIdContext.MmtMfuStatistics.FALLBACK_WIDTH;
    }

    //TODO: refactor to get rid of MmtPacketIdContext
    private int getVideoHeight() {
        return MmtPacketIdContext.video_packet_statistics.height > 0 ? MmtPacketIdContext.video_packet_statistics.height : MmtPacketIdContext.MmtMfuStatistics.FALLBACK_HEIGHT;
    }

    private float getVideoFrameRate() {
        return (float) 1000000.0 / MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us;
    }

    private boolean isVideoSample(int packet_id) {
        return MmtPacketIdContext.video_packet_id == packet_id;
    }

    private boolean isAudioSample(int packet_id) {
        return audioConfigurationMap.containsKey(packet_id);
    }

    private boolean isTextSample(int packet_id) {
        return MmtPacketIdContext.stpp_packet_id == packet_id;
    }

    private boolean isKeySample(int sample_number) {
        return sample_number == 1;
    }

    private int getMpuMetadataSize() {
        if (InitMpuMetadata_HEVC_NAL_Payload == null) return 0;
        return InitMpuMetadata_HEVC_NAL_Payload.limit();
    }

    //jjustman-2020-12-22 - TODO: handle when mfu_presentation_time_uS_computed - push last value?
    private Long getPresentationTimestampUs(int packet_id, int mpu_sequence_number, int sample_number, long mpu_presentation_time_uS_from_SI) {
        if (mpu_presentation_time_uS_from_SI > 0) {
            long mfu_presentation_time_uS_computed = 0;
            long extracted_sample_duration_us;

            long track_anchor_timestamp_us = 0;
            if (isVideoSample(packet_id)) {
                if (MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us > 0) {
                    mfu_presentation_time_uS_computed = mpu_presentation_time_uS_from_SI + (sample_number - 1) * MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us;
                }
            } else if (isAudioSample(packet_id)) {
                if ((extracted_sample_duration_us = MmtPacketIdContext.getAudioPacketStatistic(packet_id).extracted_sample_duration_us) > 0) {
//jjustman-2023-04-15 - super super super hack!
                    mfu_presentation_time_uS_computed = mpu_presentation_time_uS_from_SI + (sample_number - 1) * extracted_sample_duration_us;
//                    mfu_presentation_time_uS_computed += 30000000;
                }
            } else if (isTextSample(packet_id)) {
                if (MmtPacketIdContext.stpp_packet_statistics.extracted_sample_duration_us > 0) {
                    mfu_presentation_time_uS_computed = mpu_presentation_time_uS_from_SI + (sample_number - 1) * MmtPacketIdContext.stpp_packet_statistics.extracted_sample_duration_us;
                }
            }

            if (mfu_presentation_time_uS_computed > 0) {
                //todo: expand size as needed, every ~ mfu_presentation_time_uS_computed 1000000uS
                if (isVideoSample(packet_id)) {
                    if(videoMfuPresentationTimestampUsMaxWraparoundValue != Long.MAX_VALUE && mfu_presentation_time_uS_computed >= videoMfuPresentationTimestampUsMaxWraparoundValue) {
                        //drop this, as we haven't seen a sane time yet since our wraparound
                        return null;
                    } else {
                        videoMfuPresentationTimestampUsMaxWraparoundValue = Long.MAX_VALUE;
                    }

                    if (videoMfuPresentationTimestampUs == Long.MAX_VALUE) {
                        videoMfuPresentationTimestampUs = mfu_presentation_time_uS_computed;
                    }
                    track_anchor_timestamp_us = videoMfuPresentationTimestampUs;
                } else if (isAudioSample(packet_id)) {

                    if(audioMfuPresentationTimestampUsMaxWraparoundValue != Long.MAX_VALUE && mfu_presentation_time_uS_computed >= audioMfuPresentationTimestampUsMaxWraparoundValue) {
                        //drop this, as we haven't seen a sane time yet since our wraparound
                        return null;
                    } else {
                        audioMfuPresentationTimestampUsMaxWraparoundValue = Long.MAX_VALUE;
                    }

                    if (!audioMfuPresentationTimestampMap.containsKey(packet_id)) {
                        audioMfuPresentationTimestampMap.put(packet_id, mfu_presentation_time_uS_computed);
                    }
                    track_anchor_timestamp_us = audioMfuPresentationTimestampMap.get(packet_id);
                } else if (isTextSample(packet_id)) {
                    if(stppMfuPresentationTimestampUsMaxWraparoundValue != Long.MAX_VALUE && mfu_presentation_time_uS_computed >= stppMfuPresentationTimestampUsMaxWraparoundValue) {
                        //drop this, as we haven't seen a sane time yet since our wraparound
                        return null;
                    } else {
                        stppMfuPresentationTimestampUsMaxWraparoundValue = Long.MAX_VALUE;
                    }
                    if (stppMfuPresentationTimestampUs == Long.MAX_VALUE) {
                        stppMfuPresentationTimestampUs = mfu_presentation_time_uS_computed;
                    }
                    track_anchor_timestamp_us = stppMfuPresentationTimestampUs;
                }

                //long anchorMfuPresentationTimestampUs = getMinNonZeroMfuPresentationTimestampForAnchor(packet_id);
                long mpuPresentationTimestampDeltaUs = mfu_presentation_time_uS_computed - track_anchor_timestamp_us;

                return mpuPresentationTimestampDeltaUs + MMTClockAnchor.MPU_PRESENTATION_DELTA_PTS_OFFSET_US;
            }
        }

        return null;
    }


    //jjustman-2023-04-21 - don't rebase from anchor?
    private Long getPresentationTimestampUsNoAnchor(int packet_id, int mpu_sequence_number, int sample_number, long mpu_presentation_time_uS_from_SI) {
        if (mpu_presentation_time_uS_from_SI > 0) {
            long mfu_presentation_time_uS_computed = 0;
            long extracted_sample_duration_us;

            long track_anchor_timestamp_us = 0;
            if (isVideoSample(packet_id)) {
                if (MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us > 0) {
                    mfu_presentation_time_uS_computed = mpu_presentation_time_uS_from_SI + (sample_number - 1) * MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us;
                }
            } else if (isAudioSample(packet_id)) {
                if ((extracted_sample_duration_us = MmtPacketIdContext.getAudioPacketStatistic(packet_id).extracted_sample_duration_us) > 0) {
//jjustman-2023-04-15 - super super super hack!
                    mfu_presentation_time_uS_computed = mpu_presentation_time_uS_from_SI + (sample_number - 1) * extracted_sample_duration_us;
//                    mfu_presentation_time_uS_computed += 30000000;
                }
            } else if (isTextSample(packet_id)) {
                if (MmtPacketIdContext.stpp_packet_statistics.extracted_sample_duration_us > 0) {
                    mfu_presentation_time_uS_computed = mpu_presentation_time_uS_from_SI + (sample_number - 1) * MmtPacketIdContext.stpp_packet_statistics.extracted_sample_duration_us;
                }
            }

            if (mfu_presentation_time_uS_computed > 0) {
                //todo: expand size as needed, every ~ mfu_presentation_time_uS_computed 1000000uS
                if (isVideoSample(packet_id)) {
                    if(videoMfuPresentationTimestampUsMaxWraparoundValue != Long.MAX_VALUE && mfu_presentation_time_uS_computed >= videoMfuPresentationTimestampUsMaxWraparoundValue) {
                        //drop this, as we haven't seen a sane time yet since our wraparound
                        minMfuPresentationTimestampUsMaxWraparoundValue = Long.MAX_VALUE;

                        return null;
                    } else {
                        videoMfuPresentationTimestampUsMaxWraparoundValue = Long.MAX_VALUE;
                    }

                    if (videoMfuPresentationTimestampUs == Long.MAX_VALUE) {
                        videoMfuPresentationTimestampUs = mfu_presentation_time_uS_computed;
                    }
                    track_anchor_timestamp_us = videoMfuPresentationTimestampUs;
                } else if (isAudioSample(packet_id)) {

                    if(audioMfuPresentationTimestampUsMaxWraparoundValue != Long.MAX_VALUE && mfu_presentation_time_uS_computed >= audioMfuPresentationTimestampUsMaxWraparoundValue) {
                        //drop this, as we haven't seen a sane time yet since our wraparound
                        return null;
                    } else {
                        audioMfuPresentationTimestampUsMaxWraparoundValue = Long.MAX_VALUE;
                    }

                    if (!audioMfuPresentationTimestampMap.containsKey(packet_id)) {
                        audioMfuPresentationTimestampMap.put(packet_id, mfu_presentation_time_uS_computed);
                    }
                    track_anchor_timestamp_us = audioMfuPresentationTimestampMap.get(packet_id);
                } else if (isTextSample(packet_id)) {
                    if(stppMfuPresentationTimestampUsMaxWraparoundValue != Long.MAX_VALUE && mfu_presentation_time_uS_computed >= stppMfuPresentationTimestampUsMaxWraparoundValue) {
                        //drop this, as we haven't seen a sane time yet since our wraparound
                        return null;
                    } else {
                        stppMfuPresentationTimestampUsMaxWraparoundValue = Long.MAX_VALUE;
                    }
                    if (stppMfuPresentationTimestampUs == Long.MAX_VALUE) {
                        stppMfuPresentationTimestampUs = mfu_presentation_time_uS_computed;
                    }
                    track_anchor_timestamp_us = stppMfuPresentationTimestampUs;
                }


                if(minMfuPresentationTimestampUsMaxWraparoundValue == Long.MAX_VALUE) {
                    minMfuPresentationTimestampUsMaxWraparoundValue = mpu_presentation_time_uS_from_SI;
                }


//                //long anchorMfuPresentationTimestampUs = getMinNonZeroMfuPresentationTimestampForAnchor(packet_id);
//                long mpuPresentationTimestampDeltaUs = mfu_presentation_time_uS_computed - track_anchor_timestamp_us;

                return mfu_presentation_time_uS_computed + MMTClockAnchor.MPU_PRESENTATION_DELTA_PTS_OFFSET_US - minMfuPresentationTimestampUsMaxWraparoundValue;
            }
        }

        return null;
    }

    private long getMinNonZeroMfuPresentationTimestampForAnchor(int packet_id) {
        long minNonZeroMfuPresentationTimestampForAnchor = Long.MAX_VALUE;

        Long audioMfuPresentationTimestampUs = audioMfuPresentationTimestampMap.get(packet_id);
        if (videoMfuPresentationTimestampUs != Long.MAX_VALUE) {
            minNonZeroMfuPresentationTimestampForAnchor = videoMfuPresentationTimestampUs;
            if (audioMfuPresentationTimestampUs != null) {
                minNonZeroMfuPresentationTimestampForAnchor = Math.min(minNonZeroMfuPresentationTimestampForAnchor, audioMfuPresentationTimestampUs);
                if (stppMfuPresentationTimestampUs != Long.MAX_VALUE) {
                    minNonZeroMfuPresentationTimestampForAnchor = Math.min(minNonZeroMfuPresentationTimestampForAnchor, stppMfuPresentationTimestampUs);
                }
            }
        } else if (audioMfuPresentationTimestampUs != null) {
            minNonZeroMfuPresentationTimestampForAnchor = audioMfuPresentationTimestampUs;
            if (stppMfuPresentationTimestampUs != Long.MAX_VALUE) {
                minNonZeroMfuPresentationTimestampForAnchor = Math.min(minNonZeroMfuPresentationTimestampForAnchor, stppMfuPresentationTimestampUs);
            }
        } else if (stppMfuPresentationTimestampUs != Long.MAX_VALUE) {
            minNonZeroMfuPresentationTimestampForAnchor = stppMfuPresentationTimestampUs;
        }

        return minNonZeroMfuPresentationTimestampForAnchor;
    }

    public void InitMpuMetadata_HEVC_NAL_Payload(MpuMetadata_HEVC_NAL_Payload payload) {
        if (!isActive) return;

        if (InitMpuMetadata_HEVC_NAL_Payload == null) {
            InitMpuMetadata_HEVC_NAL_Payload = payload.myByteBuffer.duplicate();
        } /*else {
            //Do not free it here payload.releaseByteBuffer();
        }*/
    }

    public void pushAudioDecoderConfigurationRecord(MMTAudioDecoderConfigurationRecord mmtAudioDecoderConfigurationRecord, Atsc3NdkMediaMMTBridge atsc3NdkMediaMMTBridge) {
        audioConfigurationMap.put(mmtAudioDecoderConfigurationRecord.packet_id, mmtAudioDecoderConfigurationRecord);
        if(audioPacketIdMpuSequenceNumberMap.containsKey(mmtAudioDecoderConfigurationRecord.packet_id)) {
            Long lastAudioPacketMpuSequenceNumber = audioPacketIdMpuSequenceNumberMap.get(mmtAudioDecoderConfigurationRecord.packet_id);

            if(mmtAudioDecoderConfigurationRecord.mpu_sequence_number < lastAudioPacketMpuSequenceNumber) {
                Log.w(TAG, String.format("pushAudioDecoderConfigurationRecord - detected mpu_sequence_number wraparound with packet_id: %d, lastAudioPacketMpuSequenceNumber: %d, current mpuSequenceNumber: %d - resetting MMTClockAnchor.SystemClockAnchor to 0!",
                        mmtAudioDecoderConfigurationRecord.packet_id,
                        lastAudioPacketMpuSequenceNumber,
                        mmtAudioDecoderConfigurationRecord.mpu_sequence_number));

                resetMfuPresentationTimestampAnchorsAndMMTSystemClockAnchor(atsc3NdkMediaMMTBridge, mmtAudioDecoderConfigurationRecord);
            }
        }
        audioPacketIdMpuSequenceNumberMap.put(mmtAudioDecoderConfigurationRecord.packet_id, mmtAudioDecoderConfigurationRecord.mpu_sequence_number);
    }

    private void resetMfuPresentationTimestampAnchorsAndMMTSystemClockAnchor(Atsc3NdkMediaMMTBridge atsc3NdkMediaMMTBridge, MMTAudioDecoderConfigurationRecord mmtAudioDecoderConfigurationRecord) {
        MMTClockAnchor.SystemClockAnchor = Long.MIN_VALUE;
        MMTClockAnchor.SystemClockAnchorResetFromTimestampNegativeDiscontinuity = true;

        videoMfuPresentationTimestampUsMaxWraparoundValue = videoMfuPresentationTimestampUs;
        videoMfuPresentationTimestampUs = Long.MAX_VALUE;

        audioMfuPresentationTimestampUsMaxWraparoundValue = audioMfuPresentationTimestampMap.getOrDefault(mmtAudioDecoderConfigurationRecord.packet_id, Long.MAX_VALUE);
        audioMfuPresentationTimestampMap.clear();
        //jjustman-2023-06-10 - fixup for mmt wraparound?
        audioPacketIdMpuSequenceNumberMap.clear();

        stppMfuPresentationTimestampUsMaxWraparoundValue = stppMfuPresentationTimestampUs;
        stppMfuPresentationTimestampUs = Long.MAX_VALUE;
        //atsc3NdkMediaMMTBridge.rewindBuffer();
    }

    public void pushVideoStreamProperties(MmtVideoProperties.MmtVideoPropertiesDescriptor descriptor) {
        if (sendFileHeader) {
            for (MmtVideoProperties.MmtVideoPropertiesAsset asset : descriptor.getAssetList()) {
                videoAssetMap.put(asset.getId(), asset);
            }
        }
    }

    public void pushAudioStreamProperties(MmtAudioProperties.MmtAudioPropertiesDescriptor descriptor) {
        if (sendFileHeader) {
            for (MmtAudioProperties.MmtAudioPropertiesAsset asset : descriptor.getAssetList()) {
                audioAssetMap.put(asset.getId(), asset);
            }
        }
    }

    public void pushCaptionAssetProperties(MmtCaptionProperties.MmtCaptionPropertiesDescriptor descriptor) {
        if (sendFileHeader) {
            for (MmtCaptionProperties.MmtCaptionPropertiesAsset asset : descriptor.getAssetList()) {
                captionAssetMap.put(asset.getId(), asset);
            }
        }
    }

    public void pushAssetMappingTable(MmtMpTable.MmtAssetTable assets) {
        if (!mpTableComplete) {
            for (MmtMpTable.MmtAssetRow asset : assets.getAssetList()) {
                int packetId = asset.getPacketId();
                if (isVideoSample(packetId) || isAudioSample(packetId) || isTextSample(packetId)) {
                    assetMapping.put(packetId, asset);
                }
            }
            //jjustman-2023-03-06 - hack for triveni packager that doesn't emit mp_table_complete
            if(assetMapping.size() >= 2 && MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us > 0) {
                mpTableComplete = true;
            }
        }
    }

    public void completeAssetMappingTable(MmtMpTable.MmtAssetTable assets) {
        if (!mpTableComplete) {
            // Check is all known
            for (MmtMpTable.MmtAssetRow asset : assets.getAssetList()) {
                int packetId = asset.getPacketId();
                if (!isVideoSample(packetId) && !isAudioSample(packetId) && !isTextSample(packetId)) {
                    return;
                }
            }

            for (MmtMpTable.MmtAssetRow asset : assets.getAssetList()) {
                assetMapping.put(asset.getPacketId(), asset);
            }

            if(MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us > 0){
                mpTableComplete = true;
            }
        }
    }

    /**
     * Copied from ExoPlayer/Utils.java
     */
    private static int getIntegerCodeForString(String string) {
        int length = string.length();
        if (length > 4) throw new IllegalArgumentException();
        int result = 0;
        for (int i = 0; i < length; i++) {
            result <<= 8;
            result |= string.charAt(i);
        }
        return result;
    }
}

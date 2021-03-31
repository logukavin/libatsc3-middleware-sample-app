package com.nextgenbroadcast.mobile.middleware.provider.mmt;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTClockAnchor;
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverStandalone;
import com.nextgenbroadcast.mobile.player.MMTConstants;
import com.nextgenbroadcast.mobile.core.model.AVService;
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverCore;
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants;

import org.ngbp.libatsc3.middleware.Atsc3NdkMediaMMTBridge;
import org.ngbp.libatsc3.middleware.android.ATSC3PlayerFlags;
import org.ngbp.libatsc3.middleware.android.DebuggingFlags;
import org.ngbp.libatsc3.middleware.android.application.interfaces.IAtsc3NdkMediaMMTBridgeCallbacks;
import org.ngbp.libatsc3.middleware.android.mmt.MfuByteBufferFragment;
import org.ngbp.libatsc3.middleware.android.mmt.MmtPacketIdContext;
import org.ngbp.libatsc3.middleware.android.mmt.MpuMetadata_HEVC_NAL_Payload;
import org.ngbp.libatsc3.middleware.android.mmt.models.MMTAudioDecoderConfigurationRecord;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class MMTContentProvider extends ContentProvider implements IAtsc3NdkMediaMMTBridgeCallbacks {
    public static final String TAG = MMTContentProvider.class.getSimpleName();

    //jjustman-2020-12-23 - give the a/v/s decoder some time to decode frames, otherwise we will stall at startup
    //jjustman-2021-01-13 - TODO: remove me
    public static final long PTS_OFFSET_US = 266000L;

    private static final String[] COLUMNS = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};

    private final AtomicInteger mSessionCount = new AtomicInteger();
    private final ConcurrentLinkedDeque<MMTFileDescriptor> descriptors = new ConcurrentLinkedDeque<>();

    private Atsc3ReceiverCore atsc3ReceiverCore;
    private Atsc3NdkMediaMMTBridge atsc3NdkMediaMMTBridge;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private StorageManager mStorageManager;

    private boolean FirstMfuBufferVideoKeyframeSent = false;

    @Override
    public boolean onCreate() {
        MmtPacketIdContext.Initialize();

        atsc3ReceiverCore = Atsc3ReceiverStandalone.get(getContext());
        atsc3NdkMediaMMTBridge = new Atsc3NdkMediaMMTBridge(this);

        mHandlerThread = new HandlerThread("mmt-content-provider");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper());

        return true;
    }

    @Override
    public void attachInfo(@NonNull Context context, @NonNull ProviderInfo info) {
        super.attachInfo(context, info);

        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
    }

    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        if (projection == null) {
            projection = COLUMNS;
        }

        String[] cols = new String[projection.length];
        Object[] values = new Object[projection.length];
        int i = 0;
        for (String col : projection) {
            if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                cols[i] = OpenableColumns.DISPLAY_NAME;
                values[i++] = ContentUris.parseId(uri) + ".mmt";
            } else if (OpenableColumns.SIZE.equals(col)) {
                cols[i] = OpenableColumns.SIZE;
                values[i++] = Long.MAX_VALUE;
            }
        }

        cols = copyOf(cols, i);
        values = copyOf(values, i);

        final MatrixCursor cursor = new MatrixCursor(cols, 1);
        cursor.addRow(values);
        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        // ContentProvider has already checked granted permissions

        AVService service = getServiceForUri(uri);
        if (service == null) {
            throw new IllegalArgumentException("Unable to find service for " + uri);
        }

        boolean audioOnly = service.getCategory() == SLTConstants.SERVICE_CATEGORY_AO;
        return audioOnly ? MMTConstants.MIME_MMT_AUDIO : MMTConstants.MIME_MMT_VIDEO;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("No external inserts");
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("No external updates");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("No external deletes");
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        // ContentProvider has already checked granted permissions

        AVService service = getServiceForUri(uri);
        if (service == null) {
            throw new FileNotFoundException("Unable to find service for " + uri);
        }

        final int fileMode = modeToMode(mode);
        try {
            mSessionCount.incrementAndGet();

            boolean audioOnly = service.getCategory() == SLTConstants.SERVICE_CATEGORY_AO;
            MMTFileDescriptor descriptor = new MMTFileDescriptor(audioOnly) {
                @Override
                public void onRelease() {
                    super.onRelease();

                    mSessionCount.decrementAndGet();
                    descriptors.remove(this);
                }
            };

            descriptors.add(descriptor);

            // hack to force an audio playback because we don't get an MFU initialization data that usually rise it
            if (audioOnly) {
                ATSC3PlayerFlags.ATSC3PlayerStartPlayback = true;
            }

            return mStorageManager.openProxyFileDescriptor(fileMode, descriptor, mHandler);
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    private AVService getServiceForUri(@NonNull Uri uri) {
        return atsc3ReceiverCore.findActiveServiceById((int) ContentUris.parseId(uri));
    }

    public static Uri getUriForService(@NonNull Context context, @NonNull String authority, @NonNull String serviceId) {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority).encodedPath(serviceId).build();
    }

    /**
     * Copied from ContentResolver.java
     */
    private static int modeToMode(String mode) {
        int modeBits;
        if ("r".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("wa".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_APPEND;
        } else if ("rw".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE;
        } else if ("rwt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }
        return modeBits;
    }

    private static String[] copyOf(String[] original, int newLength) {
        final String[] result = new String[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }

    private static Object[] copyOf(Object[] original, int newLength) {
        final Object[] result = new Object[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }

    @Override
    public void showMsgFromNative(String message) {
        Log.d(TAG, message);
    }

    @Override
    public void pushMfuByteBufferFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        if (isActive()) {
            if (!ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
                ATSC3PlayerFlags.ATSC3PlayerStartPlayback = true;

                //jjustman-2021-01-13 - HACK
                MMTClockAnchor.SystemClockAnchor = 0;
                MMTClockAnchor.MfuClockAnchor = 0;
            }

            PushMfuByteBufferFragment(mfuByteBufferFragment);
        } else if (ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
            ATSC3PlayerFlags.ATSC3PlayerStartPlayback = false;
            mfuByteBufferFragment.unreferenceByteBuffer();
        }
    }

    @Override
    public void pushMpuMetadata_HEVC_NAL_Payload(MpuMetadata_HEVC_NAL_Payload mpuMetadata_hevc_nal_payload) {
        if (isActive()) {
            if (!ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
                ATSC3PlayerFlags.ATSC3PlayerStartPlayback = true;
            }

            InitMpuMetadata_HEVC_NAL_Payload(mpuMetadata_hevc_nal_payload);

        } else if (ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
            ATSC3PlayerFlags.ATSC3PlayerStartPlayback = false;
            mpuMetadata_hevc_nal_payload.releaseByteBuffer();
        }
    }

    @Override
    public void pushAudioDecoderConfigurationRecord(MMTAudioDecoderConfigurationRecord mmtAudioDecoderConfigurationRecord) {
        descriptors.forEach(descriptor -> {
            descriptor.pushAudioDecoderConfigurationRecord(mmtAudioDecoderConfigurationRecord);
        });
    }

    private boolean isActive() {
        return mSessionCount.get() > 0;
    }

    private void InitMpuMetadata_HEVC_NAL_Payload(MpuMetadata_HEVC_NAL_Payload payload) {
        descriptors.forEach(descriptor -> {
            descriptor.InitMpuMetadata_HEVC_NAL_Payload(payload);
        });
    }

    private void PushMfuByteBufferFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        descriptors.forEach(descriptor -> {
            descriptor.PushMfuByteBufferFragment(mfuByteBufferFragment);
        });

        if (MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us == 0 || MmtPacketIdContext.audio_packet_statistics.extracted_sample_duration_us == 0) {
            Log.d("MMTDataBuffer", String.format("PushMfuByteBufferFragment:WARN:packet_id: %d, mpu_sequence_number: %d, video.duration_us: %d, audio.duration_us: %d, missing extracted_sample_duration",
                    mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number,
                    MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us,
                    MmtPacketIdContext.audio_packet_statistics.extracted_sample_duration_us));
        }

        //jjustman-2020-12-02 - TODO: fix me
        if (MmtPacketIdContext.video_packet_id == mfuByteBufferFragment.packet_id) {
            addVideoFragment(mfuByteBufferFragment);
            if (mfuByteBufferFragment.sample_number == 1) {
                Log.d("MMTContentProvider", String.format("PushMfuByteBufferFragment:\tV\tpacket_id\t%d\tmpu_sequence_number\t%d\tduration_us\t%d\tsafe_mfu_presentation_time_us_computed\t%d\tmfuBufferQueue size\t%d",
                        mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number,
                        MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us,
                        mfuByteBufferFragment.get_safe_mfu_presentation_time_uS_computed(),
                        maxQueueSize()/*mfuBufferQueue.size()*/));
            }
        } else if (MmtPacketIdContext.audio_packet_id == mfuByteBufferFragment.packet_id) {
            addAudioFragment(mfuByteBufferFragment);
            if (mfuByteBufferFragment.sample_number == 1) {
                Log.d("MMTContentProvider", String.format("PushMfuByteBufferFragment:\tA\tpacket_id\t%d\tmpu_sequence_number\t%d\tduration_us\t%d\tsafe_mfu_presentation_time_us_computed\t%d\tmfuBufferQueue size\t%d",
                        mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number,
                        MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us,
                        mfuByteBufferFragment.get_safe_mfu_presentation_time_uS_computed(),
                        maxQueueSize()/*mfuBufferQueue.size()*/));
            }
        } else if (MmtPacketIdContext.stpp_packet_id == mfuByteBufferFragment.packet_id) {
            addSubtitleFragment(mfuByteBufferFragment);
            Log.d("MMTContentProvider", String.format("PushMfuByteBufferFragment:\tS\tpacket_id\t%d\tmpu_sequence_number\t%d\tduration_us\t%d\tsafe_mfu_presentation_time_us_computed\t%d\tmfuBufferQueue size\t%d",
                    mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number,
                    MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us,
                    mfuByteBufferFragment.get_safe_mfu_presentation_time_uS_computed(),
                    maxQueueSize()/*mfuBufferQueue.size()*/));
        }
    }

    private void addVideoFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        //jjustman-2020-12-09 - hacks to make sure we don't fall too far behind wall-clock
//        if(mfuBufferQueue.size() > 120) {
//            Log.w("MMTDataBuffer", String.format("addVideoFragment: V: clearing queue, length: %d", mfuBufferQueue.size()));
//            mfuBufferQueue.clear();
//        }

        if (isKeySample(mfuByteBufferFragment)) {
            if (!FirstMfuBufferVideoKeyframeSent) {
                Log.d("MMTContentProvider", String.format("addVideoFragment: V: pushing FIRST: queueSize: %d, sampleNumber: %d, size: %d, mpuPresentationTimeUs: %d",
                        maxQueueSize()/*mfuBufferQueue.size()*/,
                        mfuByteBufferFragment.sample_number,
                        mfuByteBufferFragment.bytebuffer_length,
                        mfuByteBufferFragment.mpu_presentation_time_uS_from_SI));
            }
            FirstMfuBufferVideoKeyframeSent = true;

            MmtPacketIdContext.video_packet_statistics.video_mfu_i_frame_count++;
        } else {
            MmtPacketIdContext.video_packet_statistics.video_mfu_pb_frame_count++;
        }

        if (mfuByteBufferFragment.mfu_fragment_count_expected == mfuByteBufferFragment.mfu_fragment_count_rebuilt) {
            MmtPacketIdContext.video_packet_statistics.complete_mfu_samples_count++;
        } else {
            MmtPacketIdContext.video_packet_statistics.corrupt_mfu_samples_count++;
        }

        //TODO: jjustman-2019-10-23: manual missing statistics, context callback doesn't compute this properly yet.
        if (MmtPacketIdContext.video_packet_statistics.last_mpu_sequence_number != mfuByteBufferFragment.mpu_sequence_number) {
            MmtPacketIdContext.video_packet_statistics.total_mpu_count++;
            //compute trailing mfu's missing

            //compute leading mfu's missing
            if (mfuByteBufferFragment.sample_number > 1) {
                MmtPacketIdContext.video_packet_statistics.missing_mfu_samples_count += (mfuByteBufferFragment.sample_number - 1);
            }
        } else {
            MmtPacketIdContext.video_packet_statistics.missing_mfu_samples_count += mfuByteBufferFragment.sample_number - (1 + MmtPacketIdContext.video_packet_statistics.last_mfu_sample_number);
        }

        MmtPacketIdContext.video_packet_statistics.last_mfu_sample_number = mfuByteBufferFragment.sample_number;
        MmtPacketIdContext.video_packet_statistics.last_mpu_sequence_number = mfuByteBufferFragment.mpu_sequence_number;

        //todo - build mpu stats from tail of mfuBufferQueueVideo

        MmtPacketIdContext.video_packet_statistics.total_mfu_samples_count++;

        if ((MmtPacketIdContext.video_packet_statistics.total_mfu_samples_count % DebuggingFlags.DEBUG_LOG_MFU_STATS_FRAME_COUNT) == 0) {
            Log.d("MMTDataBuffer",
                    String.format("pushMfuByteBufferFragment: V: appending MFU: mpu_sequence_number: %d, sampleNumber: %d, size: %d, mpuPresentationTimeUs: %d, queueSize: %d",
                            mfuByteBufferFragment.mpu_sequence_number,
                            mfuByteBufferFragment.sample_number,
                            mfuByteBufferFragment.bytebuffer_length,
                            mfuByteBufferFragment.get_safe_mfu_presentation_time_uS_computed(),
                            maxQueueSize()/*mfuBufferQueue.size()*/));
        }
    }

    private void addAudioFragment(MfuByteBufferFragment mfuByteBufferFragment) {

        //jjustman-2020-12-09 - hacks to make sure we don't fall too far behind wall-clock
//        if(mfuBufferQueue.size() > 120) {
//            Log.w("MMTDataBuffer", String.format("addAudioFragment: A: clearing queue, length: %d",mfuBufferQueue.size()));
//            mfuBufferQueue.clear();
//        }

        if (mfuByteBufferFragment.mfu_fragment_count_expected == mfuByteBufferFragment.mfu_fragment_count_rebuilt) {
            MmtPacketIdContext.audio_packet_statistics.complete_mfu_samples_count++;
        } else {
            MmtPacketIdContext.audio_packet_statistics.corrupt_mfu_samples_count++;
        }

        //todo - build mpu stats from tail of mfuBufferQueueVideo

        MmtPacketIdContext.audio_packet_statistics.total_mfu_samples_count++;

        if (MmtPacketIdContext.audio_packet_statistics.last_mpu_sequence_number != mfuByteBufferFragment.mpu_sequence_number) {
            MmtPacketIdContext.audio_packet_statistics.total_mpu_count++;
            //compute trailing mfu's missing

            //compute leading mfu's missing
            if (mfuByteBufferFragment.sample_number > 1) {
                MmtPacketIdContext.audio_packet_statistics.missing_mfu_samples_count += (mfuByteBufferFragment.sample_number - 1);
            }
        } else {
            MmtPacketIdContext.audio_packet_statistics.missing_mfu_samples_count += mfuByteBufferFragment.sample_number - (1 + MmtPacketIdContext.audio_packet_statistics.last_mfu_sample_number);
        }

        MmtPacketIdContext.audio_packet_statistics.last_mfu_sample_number = mfuByteBufferFragment.sample_number;
        MmtPacketIdContext.audio_packet_statistics.last_mpu_sequence_number = mfuByteBufferFragment.mpu_sequence_number;


        if ((MmtPacketIdContext.audio_packet_statistics.total_mfu_samples_count % DebuggingFlags.DEBUG_LOG_MFU_STATS_FRAME_COUNT) == 0) {

            Log.d("MMTDataBuffer", String.format("pushMfuByteBufferFragment: A: appending MFU: mpu_sequence_number: %d, sampleNumber: %d, size: %d, mpuPresentationTimeUs: %d, queueSize: %d",
                    mfuByteBufferFragment.mpu_sequence_number,
                    mfuByteBufferFragment.sample_number,
                    mfuByteBufferFragment.bytebuffer_length,
                    mfuByteBufferFragment.get_safe_mfu_presentation_time_uS_computed(),
                    maxQueueSize()/*mfuBufferQueue.size()*/));
        }
    }

    private void addSubtitleFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        if (!FirstMfuBufferVideoKeyframeSent) {
            return;
        }

        MmtPacketIdContext.stpp_packet_statistics.total_mfu_samples_count++;
    }

    private boolean isKeySample(MfuByteBufferFragment fragment) {
        return fragment.sample_number == 1;
    }

    private int maxQueueSize() {
        final AtomicInteger maxSize = new AtomicInteger(0);
        descriptors.forEach(descriptor -> {
            int size = descriptor.getQueueSize();
            if (size > maxSize.get()) maxSize.set(size);
        });

        return maxSize.get();
    }
}

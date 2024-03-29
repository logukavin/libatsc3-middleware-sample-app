package com.nextgenbroadcast.mobile.middleware.provider.mmt;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.MMTClockAnchor;
import com.nextgenbroadcast.mobile.core.LOG;
import com.nextgenbroadcast.mobile.core.exception.ServiceNotFoundException;
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverStandalone;

import static com.nextgenbroadcast.mobile.middleware.provider.ContentProviderUtils.*;

import com.nextgenbroadcast.mobile.middleware.R;
import com.nextgenbroadcast.mobile.player.MMTConstants;
import com.nextgenbroadcast.mobile.core.model.AVService;
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverCore;
import com.nextgenbroadcast.mobile.core.atsc3.SLTConstants;
import com.nextgenbroadcast.mobile.middleware.atsc3.buffer.Atsc3RingBuffer;

import org.ngbp.libatsc3.middleware.Atsc3NdkMediaMMTBridge;
import org.ngbp.libatsc3.middleware.android.ATSC3PlayerFlags;
import org.ngbp.libatsc3.middleware.android.application.interfaces.IAtsc3NdkMediaMMTBridgeCallbacks;
import org.ngbp.libatsc3.middleware.android.mmt.MfuByteBufferFragment;
import org.ngbp.libatsc3.middleware.android.mmt.MmtPacketIdContext;
import org.ngbp.libatsc3.middleware.android.mmt.MpuMetadata_HEVC_NAL_Payload;
import org.ngbp.libatsc3.middleware.android.mmt.models.MMTAudioDecoderConfigurationRecord;
import org.ngbp.libatsc3.middleware.mmt.pb.MmtAudioProperties;
import org.ngbp.libatsc3.middleware.mmt.pb.MmtCaptionProperties;
import org.ngbp.libatsc3.middleware.mmt.pb.MmtMpTable;
import org.ngbp.libatsc3.middleware.mmt.pb.MmtVideoProperties;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MMTContentProvider extends ContentProvider implements IAtsc3NdkMediaMMTBridgeCallbacks {
    public static final String TAG = MMTContentProvider.class.getSimpleName();
    private static final boolean TRACE_ENABLED = false;


    //jjustman-2021-11-11, old values: 320 and 16*1024
    private static final int RING_BUFFER_MAX_PAGE_COUNT = 320 * 16;
    private static final int RING_BUFFER_PAGE_SIZE = 512; // it should be bigger then AC-4 audio frame that takes around ~450 bytes to prevent multiple RB read requests and buffer joint for video frames
    private static final int RING_BUFFER_SIZE = RING_BUFFER_MAX_PAGE_COUNT * RING_BUFFER_PAGE_SIZE;

    private static final String[] COLUMNS = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 5;
    private static final int KEEP_ALIVE_SECONDS = 3;

    private final ThreadFactory threadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        public Thread newThread(Runnable r) {
            return new Thread(r, "MMT Pipe #" + mCount.getAndIncrement());
        }
    };

    private final CopyOnWriteArrayList<MMTFragmentWriter> descriptors = new CopyOnWriteArrayList<>();
    //TODO: used as temporary solution and must be refactored
    private final CopyOnWriteArrayList<Integer> slHdr1Services = new CopyOnWriteArrayList<>();

    private Executor threadPoolExecutor;
    private Atsc3ReceiverCore receiver;
    private Atsc3NdkMediaMMTBridge atsc3NdkMediaMMTBridge;

//    private boolean FirstMfuBufferVideoKeyframeSent = false;

    private final ByteBuffer fragmentBuffer = ByteBuffer.allocateDirect(RING_BUFFER_SIZE);

    private Context appContext;
    private Uri slHdr1PresentUri;

    @Override
    public boolean onCreate() {
        appContext = requireAppContextOrNull(this);
        if (appContext == null) return false;

        slHdr1PresentUri = getUriForPath(appContext, ROUTE_CONTENT_SL_HDR1_PRESENT);

        MmtPacketIdContext.Initialize();

        threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue(), threadFactory);

        receiver = Atsc3ReceiverStandalone.get(getContext());
        atsc3NdkMediaMMTBridge = new Atsc3NdkMediaMMTBridge(this, fragmentBuffer, RING_BUFFER_MAX_PAGE_COUNT);

        return true;
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
        if(TRACE_ENABLED) {
            Log.i(TAG, String.format("openFile with uri: %s, mode: %s", uri.toString(), mode));
        }
        final AVService service = getServiceForUri(uri);
        if (service == null) {
            throw new ServiceNotFoundException("Unable to find service for " + uri);
        }

        boolean audioOnly = service.getCategory() == SLTConstants.SERVICE_CATEGORY_AO;

        try {
            final ParcelFileDescriptor[] fds = ParcelFileDescriptor.createPipe();
            final Atsc3RingBuffer fragmentBuff = new Atsc3RingBuffer(fragmentBuffer.duplicate(), RING_BUFFER_PAGE_SIZE);
            final MMTFragmentWriter writer = new MMTFragmentWriter(service.getId(), fragmentBuff, audioOnly);

            synchronized (descriptors) {
                // close all sessions of wrong service
                int serviceId = service.getId();
                descriptors.removeIf(w -> {
                    if (w.getServiceId() != serviceId) {
                        w.close();
                        return true;
                    } else {
                        return false;
                    }
                });

                descriptors.add(writer);

                //TODO: used as temporary solution and must be refactored
                if (slHdr1Services.contains(serviceId)) {
                    notifySlHdr1Present();
                }

                // reset with first descriptor only
                if (descriptors.size() == 1) {
                    atsc3NdkMediaMMTBridge.rewindBuffer();
                }
            }

            //TODO: temporary solution, requared for atsc3_onExtractedSampleDuration()
            if (!ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
                ATSC3PlayerFlags.ATSC3PlayerStartPlayback = true;

                // vmatiash - reset time for first session only
                if (descriptors.size() == 1) {
                    //jjustman-2021-01-13 - HACK
                    MMTClockAnchor.SystemClockAnchor = Long.MIN_VALUE;
                    MMTClockAnchor.SystemClockAnchorResetFromTimestampNegativeDiscontinuity = false;
                    MMTClockAnchor.MfuAudioClockAnchorNtp64Min = null;
                    MMTClockAnchor.MfuAudioClockAnchorMpuSequenceMin = null;
                }
            }

            threadPoolExecutor.execute(() -> {
                writeToFile(fds[1], writer, service);

                synchronized (descriptors) {
                    descriptors.remove(writer);

                    if (descriptors.isEmpty()) {
                        ATSC3PlayerFlags.ATSC3PlayerStartPlayback = false;
                    }
                }
            });

            return fds[0];
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    private void writeToFile(ParcelFileDescriptor fd, MMTFragmentWriter writer, AVService service) {
        try (FileOutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(fd)) {
            /*
             * Read MMT data until requested service is selected. Check selected service
             * every 5 seconds if no MMT data available. That could be if another service
             * selected, route closed or some other problems happened. Short retry loop tries
             * to receive MMT data during 500 mills if writer didn't write anything to output stream.
             */
            while (receiver.getSelectedService() == service) {
                int counter = -50; // 5 sec
                while (counter < 5 && writer.isActive()) {
                    int bytesRead = writer.write(out);

                    if(TRACE_ENABLED) {
                        LOG.d(TAG, String.format("writeToFile:: after writer.write, counter: %d, bytesRead: %d", counter, bytesRead));
                    }

                    if (bytesRead > 0) {
                        counter = 0;
                    } else {
                        counter++;
                        if (counter < 0) {
                            //jjustman-2021-09-01 - was 100ms, changing to 16ms
                            //LOG.d(TAG, "writeToFile:: sleeping for 16ms, counter: " + counter);
                            //noinspection BusyWait
                            if(TRACE_ENABLED) {
                                LOG.d(TAG, String.format("writeToFile:: calling thread.sleep(8)"));
                            }
                            Thread.sleep(8);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.e(TAG, "Failed to read MMT media stream", e);
        } finally {
            writer.close();
        }
    }

    private AVService getServiceForUri(@NonNull Uri uri) {
        return receiver.findActiveServiceById((int) ContentUris.parseId(uri));
    }

    public static Uri getUriForService(@NonNull Context context, @NonNull String authority, @NonNull String serviceId) {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority).encodedPath(serviceId).build();
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
//        if (isActive()) {
//            if (!ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
//                ATSC3PlayerFlags.ATSC3PlayerStartPlayback = true;
//
//                //jjustman-2021-01-13 - HACK
//                MMTClockAnchor.SystemClockAnchor = 0;
//                MMTClockAnchor.MfuClockAnchor = 0;
//            }
//
//            PushMfuByteBufferFragment(mfuByteBufferFragment);
//        } else if (ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
//            ATSC3PlayerFlags.ATSC3PlayerStartPlayback = false;
//            mfuByteBufferFragment.unreferenceByteBuffer();
//        }
    }

    @Override
    public void pushMpuMetadata_HEVC_NAL_Payload(MpuMetadata_HEVC_NAL_Payload mpuMetadata_hevc_nal_payload) {
//        if (isActive()) {
//            if (!ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
//                ATSC3PlayerFlags.ATSC3PlayerStartPlayback = true;
//            }
//
//            InitMpuMetadata_HEVC_NAL_Payload(mpuMetadata_hevc_nal_payload);
//
//        } else if (ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
//            ATSC3PlayerFlags.ATSC3PlayerStartPlayback = false;
//            mpuMetadata_hevc_nal_payload.releaseByteBuffer();
//        }
    }

    private boolean isActive() {
        return !descriptors.isEmpty();
    }

    //TODO: rewrite with ring-buffer?
    @Override
    public void pushAudioDecoderConfigurationRecord(MMTAudioDecoderConfigurationRecord mmtAudioDecoderConfigurationRecord) {
        if(MmtPacketIdContext.selected_audio_packet_id == -1) {
            MmtPacketIdContext.selected_audio_packet_id = mmtAudioDecoderConfigurationRecord.packet_id; //jjustman-2021-05-24 - todo: pick min()
        }
        descriptors.forEach(descriptor -> {
            descriptor.pushAudioDecoderConfigurationRecord(mmtAudioDecoderConfigurationRecord, atsc3NdkMediaMMTBridge);
        });
    }

    @Override
    public void atsc3_signallingContext_notify_audio_packet_id_and_mpu_timestamp_descriptor(int audio_packet_id, long mpu_sequence_number, long mpu_presentation_time_ntp64, long mpu_presentation_time_seconds, int mpu_presentation_time_microseconds) {
//        if(MMTClockAnchor.SystemClockAnchorResetFromTimestampNegativeDiscontinuity) {
//            if(mpu_sequence_number >= MMTClockAnchor.MfuAudioClockAnchorMpuSequenceMin) {
//                //we can safely set our audio ntp64 as the mfu timing anchor minimum after wraparound
//                MMTClockAnchor.MfuAudioClockAnchorNtp64Min = mpu_presentation_time_ntp64;
//            }
//        }
    }

    @Override
    public void onVideoStreamProperties(MmtVideoProperties.MmtVideoPropertiesDescriptor properties) {
        descriptors.forEach(descriptor -> {
            descriptor.pushVideoStreamProperties(properties);
        });
    }

    @Override
    public void onCaptionAssetProperties(MmtCaptionProperties.MmtCaptionPropertiesDescriptor properties) {
        descriptors.forEach(descriptor -> {
            descriptor.pushCaptionAssetProperties(properties);
        });
    }

    @Override
    public void onAudioStreamProperties(MmtAudioProperties.MmtAudioPropertiesDescriptor properties) {
        descriptors.forEach(descriptor -> {
            descriptor.pushAudioStreamProperties(properties);
        });
    }

    @Override
    public void onMpTableSubset(MmtMpTable.MmtAssetTable assetTable) {
        descriptors.forEach(descriptor -> {
            descriptor.pushAssetMappingTable(assetTable);
        });
    }

    @Override
    public void onMpTableComplete(MmtMpTable.MmtAssetTable assetTable) {
        descriptors.forEach(descriptor -> {
            descriptor.completeAssetMappingTable(assetTable);
        });
    }

    @Override
    public void notifySlHdr1Present(int service_id, int packet_id) {
        slHdr1Services.add(service_id);

        notifySlHdr1Present();
    }

    private void notifySlHdr1Present() {
        appContext.getContentResolver().notifyChange(slHdr1PresentUri, null);
    }

    private Uri getUriForPath(Context context, String path) {
        return getReceiverUriForPath(context.getString(R.string.nextgenMMTContentProvider), path);
    }

    //TODO: do we need this?
//    private void PushMfuByteBufferFragment(MfuByteBufferFragment mfuByteBufferFragment) {
////        descriptors.forEach(descriptor -> {
////            descriptor.PushMfuByteBufferFragment(mfuByteBufferFragment);
////        });
//
//        if (MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us == 0) {
//            Log.d("MMTContentProvider", String.format("PushMfuByteBufferFragment:WARN V: packet_id: %d, mpu_sequence_number: %d, video.duration_us: %d, missing extracted_sample_duration",
//                    mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number,
//                    MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us));
//        } else if (MmtPacketIdContext.isAudioPacket(mfuByteBufferFragment.packet_id)) {
//            MmtPacketIdContext.MmtMfuStatistics audioPacketStatistic = MmtPacketIdContext.getAudioPacketStatistic(mfuByteBufferFragment.packet_id);
//            if (audioPacketStatistic != null && audioPacketStatistic.extracted_sample_duration_us == 0) {
//                Log.d("MMTContentProvider", String.format("PushMfuByteBufferFragment:WARN A: packet_id: %d, mpu_sequence_number: %d, audio.duration_us: %d, missing extracted_sample_duration",
//                        mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number,
//                        audioPacketStatistic.extracted_sample_duration_us)); //jjustman-2021-06-02: make this...not dumb? :)
//            }
//        }
//
//        //jjustman-2020-12-02 - TODO: fix me
//        if (MmtPacketIdContext.video_packet_id == mfuByteBufferFragment.packet_id) {
//            addVideoFragment(mfuByteBufferFragment);
//            if (mfuByteBufferFragment.sample_number == 1) {
//                Log.d("MMTContentProvider", String.format("PushMfuByteBufferFragment:\tV\tpacket_id\t%d\tmpu_sequence_number\t%d\tduration_us\t%d\tsafe_mfu_presentation_time_us_computed\t%d\tmfuBufferQueue size\t%d",
//                        mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number,
//                        MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us,
//                        mfuByteBufferFragment.get_safe_mfu_presentation_time_uS_computed(),
//                        maxQueueSize()/*mfuBufferQueue.size()*/));
//            }
//        } else if (MmtPacketIdContext.selected_audio_packet_id == mfuByteBufferFragment.packet_id) {
//            addAudioFragment(mfuByteBufferFragment);
//            if (mfuByteBufferFragment.sample_number == 1) {
//                Log.d("MMTContentProvider", String.format("PushMfuByteBufferFragment:\tA\tpacket_id\t%d\tmpu_sequence_number\t%d\tduration_us\t%d\tsafe_mfu_presentation_time_us_computed\t%d\tmfuBufferQueue size\t%d",
//                        mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number,
//                        MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us,
//                        mfuByteBufferFragment.get_safe_mfu_presentation_time_uS_computed(),
//                        maxQueueSize()/*mfuBufferQueue.size()*/));
//            }
//        } else if (MmtPacketIdContext.stpp_packet_id == mfuByteBufferFragment.packet_id) {
//            addSubtitleFragment(mfuByteBufferFragment);
//            Log.d("MMTContentProvider", String.format("PushMfuByteBufferFragment:\tS\tpacket_id\t%d\tmpu_sequence_number\t%d\tduration_us\t%d\tsafe_mfu_presentation_time_us_computed\t%d\tmfuBufferQueue size\t%d",
//                    mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number,
//                    MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us,
//                    mfuByteBufferFragment.get_safe_mfu_presentation_time_uS_computed(),
//                    maxQueueSize()/*mfuBufferQueue.size()*/));
//        }
//    }
//
//    private void addVideoFragment(MfuByteBufferFragment mfuByteBufferFragment) {
//        //jjustman-2020-12-09 - hacks to make sure we don't fall too far behind wall-clock
////        if(mfuBufferQueue.size() > 120) {
////            Log.w("MMTDataBuffer", String.format("addVideoFragment: V: clearing queue, length: %d", mfuBufferQueue.size()));
////            mfuBufferQueue.clear();
////        }
//
//        if (isKeySample(mfuByteBufferFragment)) {
//            if (!FirstMfuBufferVideoKeyframeSent) {
//                Log.d("MMTContentProvider", String.format("addVideoFragment: V: pushing FIRST: queueSize: %d, sampleNumber: %d, size: %d, mpuPresentationTimeUs: %d",
//                        maxQueueSize()/*mfuBufferQueue.size()*/,
//                        mfuByteBufferFragment.sample_number,
//                        mfuByteBufferFragment.bytebuffer_length,
//                        mfuByteBufferFragment.mpu_presentation_time_uS_from_SI));
//            }
//            FirstMfuBufferVideoKeyframeSent = true;
//
//            MmtPacketIdContext.video_packet_statistics.video_mfu_i_frame_count++;
//        } else {
//            MmtPacketIdContext.video_packet_statistics.video_mfu_pb_frame_count++;
//        }
//
//        if (mfuByteBufferFragment.mfu_fragment_count_expected == mfuByteBufferFragment.mfu_fragment_count_rebuilt) {
//            MmtPacketIdContext.video_packet_statistics.complete_mfu_samples_count++;
//        } else {
//            MmtPacketIdContext.video_packet_statistics.corrupt_mfu_samples_count++;
//        }
//
//        //TODO: jjustman-2019-10-23: manual missing statistics, context callback doesn't compute this properly yet.
//        if (MmtPacketIdContext.video_packet_statistics.last_mpu_sequence_number != mfuByteBufferFragment.mpu_sequence_number) {
//            MmtPacketIdContext.video_packet_statistics.total_mpu_count++;
//            //compute trailing mfu's missing
//
//            //compute leading mfu's missing
//            if (mfuByteBufferFragment.sample_number > 1) {
//                MmtPacketIdContext.video_packet_statistics.missing_mfu_samples_count += (mfuByteBufferFragment.sample_number - 1);
//            }
//        } else {
//            MmtPacketIdContext.video_packet_statistics.missing_mfu_samples_count += mfuByteBufferFragment.sample_number - (1 + MmtPacketIdContext.video_packet_statistics.last_mfu_sample_number);
//        }
//
//        MmtPacketIdContext.video_packet_statistics.last_mfu_sample_number = mfuByteBufferFragment.sample_number;
//        MmtPacketIdContext.video_packet_statistics.last_mpu_sequence_number = mfuByteBufferFragment.mpu_sequence_number;
//
//        //todo - build mpu stats from tail of mfuBufferQueueVideo
//
//        MmtPacketIdContext.video_packet_statistics.total_mfu_samples_count++;
//
//        if ((MmtPacketIdContext.video_packet_statistics.total_mfu_samples_count % DebuggingFlags.DEBUG_LOG_MFU_STATS_FRAME_COUNT) == 0) {
//            Log.d("MMTDataBuffer",
//                    String.format("pushMfuByteBufferFragment: V: appending MFU: mpu_sequence_number: %d, sampleNumber: %d, size: %d, mpuPresentationTimeUs: %d, queueSize: %d",
//                            mfuByteBufferFragment.mpu_sequence_number,
//                            mfuByteBufferFragment.sample_number,
//                            mfuByteBufferFragment.bytebuffer_length,
//                            mfuByteBufferFragment.get_safe_mfu_presentation_time_uS_computed(),
//                            maxQueueSize()/*mfuBufferQueue.size()*/));
//        }
//    }
//
//    private void addAudioFragment(MfuByteBufferFragment mfuByteBufferFragment) {
//
//        if(!MmtPacketIdContext.isAudioPacket(mfuByteBufferFragment.packet_id)) {
//           Log.w(TAG, String.format("addAudioFragment: attempted to add packet_id: %d but MmtPacketIdContext.isAudioPacket returned false!", mfuByteBufferFragment.packet_id));
//           return;
//        }
//
//        //jjustman-2020-12-09 - hacks to make sure we don't fall too far behind wall-clock
//        //jjustman-2021-06-02 - TODO: refactor out into MMTFileDescriptor::PushMfuByteBufferFragment
//
//        //if(mfuBufferQueue.size() > 120) {
//        //    Log.w("MMTDataBuffer", String.format("addAudioFragment: A: clearing queue, length: %d",mfuBufferQueue.size()));
//        //      mfuBufferQueue.clear();
//        //    }
//
//        MmtPacketIdContext.MmtMfuStatistics audioPacketStatistic = MmtPacketIdContext.getAudioPacketStatistic(mfuByteBufferFragment.packet_id);
//        if (audioPacketStatistic == null) return;
//
//        if (mfuByteBufferFragment.mfu_fragment_count_expected == mfuByteBufferFragment.mfu_fragment_count_rebuilt) {
//            audioPacketStatistic.complete_mfu_samples_count++;
//        } else {
//            audioPacketStatistic.corrupt_mfu_samples_count++;
//        }
//
//        //todo - build mpu stats from tail of mfuBufferQueueVideo
//
//        audioPacketStatistic.total_mfu_samples_count++;
//
//        if (audioPacketStatistic.last_mpu_sequence_number != mfuByteBufferFragment.mpu_sequence_number) {
//            audioPacketStatistic.total_mpu_count++;
//            //compute trailing mfu's missing
//
//            //compute leading mfu's missing
//            if (mfuByteBufferFragment.sample_number > 1) {
//                audioPacketStatistic.missing_mfu_samples_count += (mfuByteBufferFragment.sample_number - 1);
//            }
//        } else {
//            audioPacketStatistic.missing_mfu_samples_count += mfuByteBufferFragment.sample_number - (1 + audioPacketStatistic.last_mfu_sample_number);
//        }
//
//        audioPacketStatistic.last_mfu_sample_number = mfuByteBufferFragment.sample_number;
//        audioPacketStatistic.last_mpu_sequence_number = mfuByteBufferFragment.mpu_sequence_number;
//
//        if ((audioPacketStatistic.total_mfu_samples_count % DebuggingFlags.DEBUG_LOG_MFU_STATS_FRAME_COUNT) == 0) {
//
//            Log.d("MMTDataBuffer", String.format("pushMfuByteBufferFragment: A: appending MFU: mpu_sequence_number: %d, sampleNumber: %d, size: %d, mpuPresentationTimeUs: %d, queueSize: %d",
//                    mfuByteBufferFragment.mpu_sequence_number,
//                    mfuByteBufferFragment.sample_number,
//                    mfuByteBufferFragment.bytebuffer_length,
//                    mfuByteBufferFragment.get_safe_mfu_presentation_time_uS_computed(),
//                    maxQueueSize()));
//        }
//    }
//
//    private void addSubtitleFragment(MfuByteBufferFragment mfuByteBufferFragment) {
//        if (!FirstMfuBufferVideoKeyframeSent) {
//            return;
//        }
//
//        MmtPacketIdContext.stpp_packet_statistics.total_mfu_samples_count++;
//    }
//
//    private boolean isKeySample(MfuByteBufferFragment fragment) {
//        return fragment.sample_number == 1;
//    }
//
//    private int maxQueueSize() {
//        int maxSize = 0;
//
//        return maxSize;
//    }
}

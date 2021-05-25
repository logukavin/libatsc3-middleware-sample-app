package com.nextgenbroadcast.mobile.player.exoplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;

/*
    based on ContentDataSource
 */
public class Atsc3ContentDataSource extends BaseDataSource {

    /**
     * Thrown when an {@link IOException} is encountered reading from a content URI.
     */
    public static class Atsc3ContentDataSourceException extends IOException {

        public Atsc3ContentDataSourceException(IOException cause) {
            super(cause);
        }

    }

    private final ContentResolver resolver;

    private @Nullable
    Uri uri;
    private @Nullable
    AssetFileDescriptor assetFileDescriptor;
    private @Nullable
    FileInputStream inputStream;
    private long bytesRemaining;
    private boolean opened;

    /**
     * @param context A context.
     */
    public Atsc3ContentDataSource(Context context) {
        super(/* isNetwork= */ false);
        this.resolver = context.getContentResolver();
    }

    /**
     * @param context  A context.
     * @param listener An optional listener.
     * @deprecated Use {@link #Atsc3ContentDataSource(Context)} and {@link
     * #addTransferListener(TransferListener)}.
     */
    @Deprecated
    public Atsc3ContentDataSource(Context context, @Nullable TransferListener listener) {
        this(context);
        if (listener != null) {
            addTransferListener(listener);
        }
    }

    @Override
    public long open(DataSpec dataSpec) throws Atsc3ContentDataSource.Atsc3ContentDataSourceException {
        try {
            uri = dataSpec.uri;
            transferInitializing(dataSpec);
            assetFileDescriptor = resolver.openAssetFileDescriptor(uri, "r");
            if (assetFileDescriptor == null) {
                throw new FileNotFoundException("Could not open file descriptor for: " + uri);
            }
            inputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
            long assetStartOffset = assetFileDescriptor.getStartOffset();
            long skipped = inputStream.skip(assetStartOffset + dataSpec.position) - assetStartOffset;
            if (skipped != dataSpec.position) {
                // We expect the skip to be satisfied in full. If it isn't then we're probably trying to
                // skip beyond the end of the data.
                throw new EOFException();
            }
            if (dataSpec.length != C.LENGTH_UNSET) {
                bytesRemaining = dataSpec.length;
            } else {
                long assetFileDescriptorLength = assetFileDescriptor.getLength();
                if (assetFileDescriptorLength == AssetFileDescriptor.UNKNOWN_LENGTH) {
                    // The asset must extend to the end of the file. If FileInputStream.getChannel().size()
                    // returns 0 then the remaining length cannot be determined.
                    FileChannel channel = inputStream.getChannel();
                    long channelSize = channel.size();
                    bytesRemaining = channelSize == 0 ? C.LENGTH_UNSET : channelSize - channel.position();
                } else {
                    bytesRemaining = assetFileDescriptorLength - skipped;
                }
            }
        } catch (IOException e) {
            throw new Atsc3ContentDataSource.Atsc3ContentDataSourceException(e);
        }

        opened = true;
        transferStarted(dataSpec);

        return bytesRemaining;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws Atsc3ContentDataSource.Atsc3ContentDataSourceException {
        //Log.d("Atsc3ContentDataSource",String.format("read with offset: %d, readLength: %d, bytesRemaining: %d", offset, readLength, bytesRemaining));


        if (readLength == 0) {
            return 0;
        } else if (bytesRemaining == 0) {
            return C.RESULT_END_OF_INPUT;
        }

        int bytesRead;
        try {
            int bytesToRead = bytesRemaining == C.LENGTH_UNSET ? readLength
                    : (int) Math.min(bytesRemaining, readLength);
            bytesRead = inputStream.read(buffer, offset, bytesToRead);
        } catch (IOException e) {
            throw new Atsc3ContentDataSource.Atsc3ContentDataSourceException(e);
        }

        if (bytesRead == -1) {
            // we expect empty result when data is not ready
//            if (bytesRemaining != C.LENGTH_UNSET) {
//                // End of stream reached having not read sufficient data.
//                throw new Atsc3ContentDataSource.Atsc3ContentDataSourceException(new EOFException());
//            }
//            return C.RESULT_END_OF_INPUT;

            //Log.d("Atsc3ContentDataSource",String.format("after read wtih bytesRead == -1, returning 0"));

            return 0;
        }
        if (bytesRemaining != C.LENGTH_UNSET) {
            bytesRemaining -= bytesRead;
        }
        bytesTransferred(bytesRead);

        //Log.d("Atsc3ContentDataSource",String.format("read: exit with bytesRead: %d", bytesRead));

        return bytesRead;
    }

    @Override
    public @Nullable
    Uri getUri() {
        return uri;
    }

    @SuppressWarnings("Finally")
    @Override
    public void close() throws Atsc3ContentDataSource.Atsc3ContentDataSourceException {
        uri = null;
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new Atsc3ContentDataSource.Atsc3ContentDataSourceException(e);
        } finally {
            inputStream = null;
            try {
                if (assetFileDescriptor != null) {
                    assetFileDescriptor.close();
                }
            } catch (IOException e) {
                throw new Atsc3ContentDataSource.Atsc3ContentDataSourceException(e);
            } finally {
                assetFileDescriptor = null;
                if (opened) {
                    opened = false;
                    transferEnded();
                }
            }
        }
    }

}
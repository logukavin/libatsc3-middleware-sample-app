package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.FileDataSource;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class PcapUdpDataSource extends BaseDataSource {
    private final static long MAGIC_IDENTICAL = 0xa1b2c3d4;
    private final static long MAGIC_SWAPPED = 0xd4c3b2a1;
    private final static long MAGIC_IDENTICAL_NANO = 0xa1b23c4d;
    private final static long MAGIC_SWAPPED_NANO = 0x4d3cb2a1;

    private final static long ETHERTYPE_IP = 0x0800;

    @Nullable
    private RandomAccessFile file;
    @Nullable
    private Uri uri;
    private long bytesRemaining;
    private boolean opened;

    private final byte[] readBuffer = new byte[4];
    private boolean isSwapped;
    private int packetRemaining;

    private long ts_sec;
    private long ts_usec;
    private long incl_len;
    private long orig_len;

    public PcapUdpDataSource() {
        super(false);
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        try {
            uri = dataSpec.uri;
            transferInitializing(dataSpec);

            file = new RandomAccessFile(dataSpec.uri.getPath(), "r");
            int headerSize = readPcapHeader();
            file.seek(Math.max(headerSize, dataSpec.position));

            bytesRemaining = dataSpec.length == -1L ? file.length() - dataSpec.position - headerSize : dataSpec.length;
            if (bytesRemaining < 0L) {
                throw new EOFException();
            }
        } catch (IOException e) {
            throw new FileDataSource.FileDataSourceException(e);
        }

        opened = true;
        transferStarted(dataSpec);

        return bytesRemaining;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        if (file == null) return C.LENGTH_UNSET;

        if (readLength == 0) {
            return 0;
        } else if (this.bytesRemaining == 0L) {
            return C.LENGTH_UNSET;
        } else {
            int bytesRead;
            try {
                if (packetRemaining == 0) {
                    while (opened) {
                        readPcapRecordHeader();

                        if (incl_len == orig_len) {
                            /*
                             * struct	ether_header {
                             *  	u_char	ether_dhost[6];
                             * 	    u_char	ether_shost[6];
                             *  	u_short	ether_type;
                             * };
                             *
                             */
                            skipFully(6 + 6 /* ether_dhost + ether_shost */);
                            int ether_type = readUInt16(false);
                            if (ether_type != ETHERTYPE_IP) {
                                skipFully((int) incl_len - 14 /* ether_header */);
                                continue;
                            }

                            break;
                        } else {
                            skipFully((int) incl_len);
                        }
                    }

                    packetRemaining = (int) incl_len - 14; /* ether_header */
                }

                bytesRead = file.read(buffer, offset, (int)Math.min(bytesRemaining, Math.min(packetRemaining, readLength)));
            } catch (IOException e) {
                throw new FileDataSource.FileDataSourceException(e);
            }

            if (bytesRead > 0) {
                bytesRemaining -= bytesRead;
                packetRemaining -= bytesRead;
                bytesTransferred(bytesRead);
            }

            return bytesRead;
        }
    }

    @Nullable
    @Override
    public Uri getUri() {
        return uri;
    }

    @Override
    public void close() throws IOException {
        this.uri = null;

        try {
            if (this.file != null) {
                this.file.close();
            }
        } catch (IOException e) {
            throw new FileDataSource.FileDataSourceException(e);
        } finally {
            this.file = null;
            if (this.opened) {
                this.opened = false;
                this.transferEnded();
            }
        }
    }

    private int readPcapHeader() throws IOException {
        long magic_number = readUInt32(false);
        if (magic_number == MAGIC_IDENTICAL || magic_number == MAGIC_IDENTICAL_NANO) {
            isSwapped = false;
        } else if (magic_number == MAGIC_SWAPPED || magic_number == MAGIC_SWAPPED_NANO) {
            isSwapped = true;
        } else {
            throw new UnsupportedOperationException("Unsupported format exception");
        }

        // pcap_hdr_s
        //version_major = readUInt16(isSwapped)
        //version_minor = readUInt16(isSwapped)
        //thiszone = readInt32(isSwapped)
        //sigfigs = readUInt32(isSwapped)
        //snaplen = readUInt32(isSwapped)
        //network = readUInt32(isSwapped)
        return 4 + 2 + 2 + 4 + 4 + 4 + 4;
    }

    private void readPcapRecordHeader() throws IOException {
        ts_sec = readUInt32(isSwapped);
        ts_usec = readUInt32(isSwapped);
        incl_len = readUInt32(isSwapped);
        orig_len = readUInt32(isSwapped);
    }

    private int readInt32(boolean swapped) throws IOException {
        file.read(readBuffer, 0, 4);
        if (swapped) {
            return ((readBuffer[3] & 0xFF) << 24) |
                    ((readBuffer[2] & 0xFF) << 16) |
                    ((readBuffer[1] & 0xFF) << 8) |
                    (readBuffer[0] & 0xFF);
        } else {
            return ((readBuffer[0] & 0xFF) << 24) |
                    ((readBuffer[1] & 0xFF) << 16) |
                    ((readBuffer[2] & 0xFF) << 8) |
                    (readBuffer[3] & 0xFF);
        }
    }

    private long readUInt32(boolean swapped) throws IOException {
        file.read(readBuffer, 0, 4);
        if (swapped) {
            return ((readBuffer[3] & 0xFF) << 24) |
                    ((readBuffer[2] & 0xFF) << 16) |
                    ((readBuffer[1] & 0xFF) << 8) |
                    (readBuffer[0] & 0xFF);
        } else {
            return ((readBuffer[0] & 0xFF) << 24) |
                    ((readBuffer[1] & 0xFF) << 16) |
                    ((readBuffer[2] & 0xFF) << 8) |
                    (readBuffer[3] & 0xFF);
        }
    }

    private int readUInt16(boolean swapped) throws IOException {
        file.read(readBuffer, 0, 2);
        if (swapped) {
            return ((readBuffer[1] & 0xFF) << 8) |
                    (readBuffer[0] & 0xFF);
        } else {
            return ((readBuffer[0] & 0xFF) << 8) |
                    ((readBuffer[1] & 0xFF));
        }
    }

    private void skipFully(int n) throws IOException {
        int remaining = n;
        while (opened && remaining > 0) {
            remaining -= file.skipBytes(n);
        }
    }
}

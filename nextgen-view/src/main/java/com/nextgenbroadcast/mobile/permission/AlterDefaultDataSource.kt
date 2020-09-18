package com.nextgenbroadcast.mobile.view;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;
import com.nextgenbroadcast.mobile.permission.UriPermissionProvider;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CustomDefaultDataSource implements DataSource {
    private static final String SCHEME_CONTENT = "content";

    private final Context context;
    private final DataSource baseDataSource;
    private final UriPermissionProvider uriPermissionProvider;

    private DataSource dataSource;

    /**
     * Constructs a new instance that delegates to a provided {@link DataSource} for URI schemes other
     * than file, asset and content.
     *
     * @param context A context.
     * @param baseDataSource A {@link DataSource} to use for URI schemes other than file, asset and
     *     content. This {@link DataSource} should normally support at least http(s).
     */
    public CustomDefaultDataSource(Context context, DataSource baseDataSource, UriPermissionProvider uriPermissionProvider) {
        this.context = context.getApplicationContext();
        this.baseDataSource = baseDataSource;
        this.uriPermissionProvider = uriPermissionProvider;
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {
        dataSource.addTransferListener(transferListener);
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        Assertions.checkState(dataSource == null);
        // Choose the correct source for the scheme.
        String scheme = dataSpec.uri.getScheme();
        if (SCHEME_CONTENT.equals(scheme)) {
            dataSource = new CustomContentDataSource(context, uriPermissionProvider);
        } else {
            dataSource = new DefaultDataSource(context, baseDataSource);
        }
        // Open the source and return.
        return dataSource.open(dataSpec);
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return Assertions.checkNotNull(dataSource).read(buffer, offset, readLength);
    }

    @Override
    public @Nullable Uri getUri() {
        return dataSource == null ? null : dataSource.getUri();
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return dataSource == null ? Collections.emptyMap() : dataSource.getResponseHeaders();
    }

    @Override
    public void close() throws IOException {
        if (dataSource != null) {
            try {
                dataSource.close();
            } finally {
                dataSource = null;
            }
        }
    }
}

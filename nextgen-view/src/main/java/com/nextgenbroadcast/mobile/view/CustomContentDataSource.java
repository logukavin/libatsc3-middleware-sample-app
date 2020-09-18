package com.nextgenbroadcast.mobile.view;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.nextgenbroadcast.mobile.UriPermissionProvider;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CustomContentDataSource implements DataSource {

    private final Context context;
    private final UriPermissionProvider uriPermissionProvider;
    private final DefaultDataSource defaultDataSource;

    public CustomContentDataSource(Context context, DataSource baseDataSource, UriPermissionProvider uriPermissionProvider) {
        this.context = context;
        this.uriPermissionProvider = uriPermissionProvider;
        this.defaultDataSource = new DefaultDataSource(context, baseDataSource);
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {
        defaultDataSource.addTransferListener(transferListener);
    }

    public long open(DataSpec dataSpec) throws IOException {
        if (context.checkCallingOrSelfUriPermission(dataSpec.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_DENIED) {
            uriPermissionProvider.requestPermission(dataSpec.uri);
        }
        return defaultDataSource.open(dataSpec);
    }

    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return defaultDataSource.read(buffer, offset, readLength);
    }

    @Nullable
    public Uri getUri() {
        return defaultDataSource.getUri();
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return defaultDataSource.getResponseHeaders();
    }

    public void close() throws IOException {
        defaultDataSource.close();
    }
}

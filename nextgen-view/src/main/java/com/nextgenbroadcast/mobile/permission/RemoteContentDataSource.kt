package com.nextgenbroadcast.mobile.view;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.ContentDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.nextgenbroadcast.mobile.permission.UriPermissionProvider;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CustomContentDataSource implements DataSource {

    private final Context context;
    private final UriPermissionProvider uriPermissionProvider;
    private final ContentDataSource contentDataSource;

    public CustomContentDataSource(Context context, UriPermissionProvider uriPermissionProvider) {
        this.context = context;
        this.uriPermissionProvider = uriPermissionProvider;
        this.contentDataSource = new ContentDataSource(context);
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {
        contentDataSource.addTransferListener(transferListener);
    }

    public long open(DataSpec dataSpec) throws ContentDataSource.ContentDataSourceException {
        if (context.checkCallingOrSelfUriPermission(dataSpec.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_DENIED) {
            try {
                uriPermissionProvider.requestPermission(dataSpec.uri);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new ContentDataSource.ContentDataSourceException(new IOException(e));
            }
        }
        return contentDataSource.open(dataSpec);
    }

    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return contentDataSource.read(buffer, offset, readLength);
    }

    @Nullable
    public Uri getUri() {
        return contentDataSource.getUri();
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return contentDataSource.getResponseHeaders();
    }

    public void close() throws IOException {
        contentDataSource.close();
    }
}

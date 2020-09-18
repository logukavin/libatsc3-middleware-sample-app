package com.nextgenbroadcast.mobile.view;

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.nextgenbroadcast.mobile.permission.UriPermissionProvider;

public final class CustomDataSourceFactory implements Factory {

  private final Context context;
  private final @Nullable TransferListener listener;
  private UriPermissionProvider uriPermissionProvider;

  public CustomDataSourceFactory(Context context, UriPermissionProvider uriPermissionProvider) {
    this.context = context.getApplicationContext();
    this.listener = null;
    this.uriPermissionProvider = uriPermissionProvider;
  }

  @Override
  public DataSource createDataSource() {
    DataSource dataSource = new CustomContentDataSource(context, uriPermissionProvider);
    if (listener != null) {
      dataSource.addTransferListener(listener);
    }
    return dataSource;
  }
}

package com.nextgenbroadcast.mobile.view;

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.nextgenbroadcast.mobile.UriPermissionProvider;

public final class CustomDataSourceFactory implements Factory {

  private final Context context;
  private final @Nullable TransferListener listener;
  private final DataSource.Factory baseDataSourceFactory;
  private UriPermissionProvider uriPermissionProvider;

  public CustomDataSourceFactory(Context context, UriPermissionProvider uriPermissionProvider, String userAgent) {
    this(context, userAgent, /* listener= */ null);
    this.uriPermissionProvider = uriPermissionProvider;
  }

  public CustomDataSourceFactory(
      Context context, String userAgent, @Nullable TransferListener listener) {
    this(context, listener, new DefaultHttpDataSourceFactory(userAgent, listener));
  }

  public CustomDataSourceFactory(Context context, Factory baseDataSourceFactory) {
    this(context, /* listener= */ null, baseDataSourceFactory);
  }

  public CustomDataSourceFactory(
      Context context,
      @Nullable TransferListener listener,
      Factory baseDataSourceFactory) {
    this.context = context.getApplicationContext();
    this.listener = listener;
    this.baseDataSourceFactory = baseDataSourceFactory;
  }

  @Override
  public DataSource createDataSource() {
    DataSource dataSource = new CustomContentDataSource(context, baseDataSourceFactory.createDataSource(), uriPermissionProvider);
    if (listener != null) {
      dataSource.addTransferListener(listener);
    }
    return dataSource;
  }
}

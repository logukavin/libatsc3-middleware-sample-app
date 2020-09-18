/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nextgenbroadcast.mobile.view;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;

/**
 * A {@link Factory} that produces {@link DefaultDataSource} instances that delegate to
 * {@link DefaultHttpDataSource}s for non-file/asset/content URIs.
 */
public final class CustomDataSourceFactory implements Factory {

  private final Context context;
  private final @Nullable TransferListener listener;
  private final Factory baseDataSourceFactory;
  private UriPermissionsListener uriPermissionsListener;

  /**
   * @param context A context.
   * @param userAgent The User-Agent string that should be used.
   */
  public CustomDataSourceFactory(Context context, UriPermissionsListener uriPermissionsListener, String userAgent) {
    this(context, userAgent, /* listener= */ null);
    this.uriPermissionsListener = uriPermissionsListener;
  }

  /**
   * @param context A context.
   * @param userAgent The User-Agent string that should be used.
   * @param listener An optional listener.
   */
  public CustomDataSourceFactory(
      Context context, String userAgent, @Nullable TransferListener listener) {
    this(context, listener, new DefaultHttpDataSourceFactory(userAgent, listener));
  }

  /**
   * @param context A context.
   * @param baseDataSourceFactory A {@link Factory} to be used to create a base {@link DataSource}
   *     for {@link DefaultDataSource}.
   * @see DefaultDataSource#DefaultDataSource(Context, TransferListener, DataSource)
   */
  public CustomDataSourceFactory(Context context, Factory baseDataSourceFactory) {
    this(context, /* listener= */ null, baseDataSourceFactory);
  }

  /**
   * @param context A context.
   * @param listener An optional listener.
   * @param baseDataSourceFactory A {@link Factory} to be used to create a base {@link DataSource}
   *     for {@link DefaultDataSource}.
   * @see DefaultDataSource#DefaultDataSource(Context, TransferListener, DataSource)
   */
  public CustomDataSourceFactory(
      Context context,
      @Nullable TransferListener listener,
      Factory baseDataSourceFactory) {
    this.context = context.getApplicationContext();
    this.listener = listener;
    this.baseDataSourceFactory = baseDataSourceFactory;
  }

  @Override
  public CustomDataSource createDataSource() {
    CustomDataSource dataSource =
        new CustomDataSource(context, baseDataSourceFactory.createDataSource(), uriPermissionsListener);
    if (listener != null) {
      dataSource.addTransferListener(listener);
    }
    return dataSource;
  }
}

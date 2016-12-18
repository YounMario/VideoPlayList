/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.player.DataSource;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.player.CacheGlue;
import com.android.player.RangeFile;
import com.android.player.okhttp.OkHttpDataSource;
import com.google.android.exoplayer.upstream.AssetDataSource;
import com.google.android.exoplayer.upstream.ContentDataSource;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.FileDataSource;
import com.google.android.exoplayer.upstream.HttpDataSource;
import com.google.android.exoplayer.upstream.TransferListener;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.Assertions;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * A {@link OkUriDataSource} that supports multiple URI schemes. The supported schemes are:
 *
 * <ul>
 * <li>http(s): For fetching data over HTTP and HTTPS (e.g. https://www.something.com/media.mp4).
 * <li>file: For fetching data from a local file (e.g. file:///path/to/media/media.mp4, or just
 *     /path/to/media/media.mp4 because the implementation assumes that a URI without a scheme is a
 *     local file URI).
 * <li>asset: For fetching data from an asset in the application's apk (e.g. asset:///media.mp4).
 * <li>content: For fetching data from a content URI (e.g. content://authority/path/123).
 * </ul>
 */
public final class OkUriDataSource implements UriDataSource {

  private static final String SCHEME_FILE = "file";
  private static final String SCHEME_ASSET = "asset";
  private static final String SCHEME_CONTENT = "content";
  private static final String TAG = "OkUriDataSource";

  private final UriDataSource httpDataSource;
  private final UriDataSource fileDataSource;
  private final UriDataSource assetDataSource;
  private final UriDataSource contentDataSource;
  private static final OkHttpClient sOkHttpClient = new OkHttpClient();

  /**
   * {@code null} if no data source is open. Otherwise, equal to {@link #fileDataSource} if the open
   * data source is a file, or {@link #httpDataSource} otherwise.
   */
  private UriDataSource dataSource;
  private CacheGlue glue;

  /**
   * Constructs a new instance.
   * <p>
   * The constructed instance will not follow cross-protocol redirects (i.e. redirects from HTTP to
   * HTTPS or vice versa) when fetching remote data. Cross-protocol redirects can be enabled by
   * using {@link #OkUriDataSource(Context, TransferListener, String, boolean)} and passing
   * {@code true} as the final argument.
   *
   * @param context A context.
   * @param userAgent The User-Agent string that should be used when requesting remote data.
   */
  public OkUriDataSource(Context context, String userAgent) {
    this(context, null, userAgent, true);
  }

  /**
   * Constructs a new instance.
   * <p>
   * The constructed instance will not follow cross-protocol redirects (i.e. redirects from HTTP to
   * HTTPS or vice versa) when fetching remote data. Cross-protocol redirects can be enabled by
   * using {@link #OkUriDataSource(Context, TransferListener, String, boolean)} and passing
   * {@code true} as the final argument.
   *
   * @param context A context.
   * @param listener An optional {@link TransferListener}.
   * @param userAgent The User-Agent string that should be used when requesting remote data.
   */
  public OkUriDataSource(Context context, TransferListener listener, String userAgent) {
    this(context, listener, userAgent, true);
  }

  public OkUriDataSource(Context context, TransferListener listener, String userAgent,
                         CacheGlue glue, boolean needEncrypty) {
    this(context, listener,
            new OkHttpDataSource(getDownloadOkHttpClient(glue),
                    userAgent,
                    null,
                    listener,
                    needEncrypty,
                    new CacheControl.Builder().noCache().build()));
    this.glue = glue;
  }


  /**
   * Constructs a new instance, optionally configured to follow cross-protocol redirects.
   *
   * @param context A context.
   * @param listener An optional {@link TransferListener}.
   * @param userAgent The User-Agent string that should be used when requesting remote data.
   * @param allowCrossProtocolRedirects Whether cross-protocol redirects (i.e. redirects from HTTP
   *     to HTTPS and vice versa) are enabled when fetching remote data..
   */
  public OkUriDataSource(Context context, TransferListener listener, String userAgent,
                         boolean allowCrossProtocolRedirects) {
    this(context, listener,
        new OkHttpDataSource(getDownloadOkHttpClient(null),
                userAgent,
                null,
                listener,
                true,
                new CacheControl.Builder().noCache().build()));
  }

  private static final String sPath = "/sdcard/VidTrim/";
  private static OkHttpClient getDownloadOkHttpClient(final CacheGlue glue) {
    Interceptor interceptor = new Interceptor() {
      @Override
      public Response intercept(Chain chain) throws IOException {
        Log.d(TAG, "request: " + chain.request().toString());
        Log.d(TAG, "request: " + chain.request().headers().toString());
        Response originalResponse = chain.proceed(chain.request());

        String md5 = glue.getMd5();//chain.request().httpUrl().;
        String range = chain.request().headers().get("range");
        if (!TextUtils.isEmpty(range) && range.startsWith("bytes=")) {
          range = range.substring(6);
          range = range.substring(0, range.indexOf("-"));
        } else {
          range = "0";
          glue.setLength(originalResponse.body().contentLength());
        }
        RangeFile rangeFile = glue.addCache(md5, Long.valueOf(range));
        return originalResponse.newBuilder()
                .body(new DeepCopyResponseBody(originalResponse.body(), rangeFile))
                .build();
      }
    };
    OkHttpClient okHttpClient = new OkHttpClient.Builder().addNetworkInterceptor(interceptor).build();
    return okHttpClient;
  }

  public static class DeepCopyResponseBody extends ResponseBody {

    private final ResponseBody responseBody;
    private BufferedSource bufferedSource;
    private BufferedSink fileSink;
    private RangeFile rangefile;

    public DeepCopyResponseBody(ResponseBody responseBody, RangeFile rangeFile) {
      this.responseBody = responseBody;
      this.rangefile = rangeFile;
      this.fileSink = rangeFile.fileSink;
    }

    public void closeFileSink() {
      try {
        Log.d("DeepCopyResponseBody", "filesink size: " + fileSink.buffer().size());
        fileSink.close();

      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    @Override
    public MediaType contentType() {
      return responseBody.contentType();
    }

    @Override
    public long contentLength() {
      return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
      Log.d("DeepCopyResponseBody", "length :" + responseBody.contentLength());
      Log.d("DeepCopyResponseBody", "type :" + responseBody.contentType());
//      sink.write(responseBody.source().buffer().clone());
      if (bufferedSource == null) {
        bufferedSource = Okio.buffer(source(responseBody.source()));
      }
      return bufferedSource;
    }

    private Source source(Source source) {
      return new ForwardingSource(source) {
        long totalBytesRead = 0L;

        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
          long bytesRead = super.read(sink, byteCount);
          // read() returns the number of bytes read, or -1 if this source is exhausted.
          totalBytesRead += bytesRead != -1 ? bytesRead : 0;
          Buffer clone = sink.clone();
          long size = fileSink.writeAll(clone);
          rangefile.end += size;
//           Log.d("DeepCopyResponseBody", "size: " + size);
          return bytesRead;
        }
      };
    }
  }
  /**
   * Constructs a new instance, using a provided {@link HttpDataSource} for fetching remote data.
   *
   * @param context A context.
   * @param listener An optional {@link TransferListener}.
   * @param httpDataSource {@link UriDataSource} to use for non-file URIs.
   */
  public OkUriDataSource(Context context, TransferListener listener,
                         UriDataSource httpDataSource) {
    this.httpDataSource = Assertions.checkNotNull(httpDataSource);
    this.fileDataSource = new FileDataSource(listener);
    this.assetDataSource = new AssetDataSource(context, listener);
    this.contentDataSource = new ContentDataSource(context, listener);
  }

  @Override
  public long open(DataSpec dataSpec) throws IOException {
    Assertions.checkState(dataSource == null);
    // Choose the correct source for the scheme.
    String scheme = dataSpec.uri.getScheme();
    if (SCHEME_FILE.equals(scheme) || TextUtils.isEmpty(scheme)) {
      if (dataSpec.uri.getPath().startsWith("/android_asset/")) {
        dataSource = assetDataSource;
      } else {
        dataSource = fileDataSource;
      }
    } else if (SCHEME_ASSET.equals(scheme)) {
      dataSource = assetDataSource;
    } else if (SCHEME_CONTENT.equals(scheme)) {
      dataSource = contentDataSource;
    } else {
      dataSource = httpDataSource;
    }
    // Open the source and return.
    return dataSource.open(dataSpec);
  }

  @Override
  public int read(byte[] buffer, int offset, int readLength) throws IOException {
    return dataSource.read(buffer, offset, readLength);
  }

  @Override
  public String getUri() {
    return dataSource == null ? null : dataSource.getUri();
  }

  @Override
  public void close() throws IOException {
    if (dataSource != null) {
      try {
        if (dataSource instanceof OkHttpDataSource) {
          Log.d("DeepCopyResponseBody", "close fileSink");
          glue.glueAll();
        }
        dataSource.close();
      } finally {
        dataSource = null;
      }
    }
  }

}

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
package com.android.player;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Build;

import com.android.player.DataSource.AesDataSource;
import com.android.player.DataSource.EncryptedVideoDataSource;
import com.android.player.DemoPlayer.RendererBuilder;
import com.android.player.util.Constans;
import com.android.player.util.RotAlgo;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

/**
 * A {@link RendererBuilder} for streams that can be read using an {@link Extractor}.
 */
public class ExtractorRendererBuilder implements RendererBuilder {

  private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
  private static final int BUFFER_SEGMENT_COUNT = 256;

  private final Context context;
  private final String userAgent;
  private final Uri uri;
  private final int encryptedVersion;
  private final int start;
  private final int end;

  public ExtractorRendererBuilder(Context context, String userAgent, Uri uri) {
    this(context, userAgent, uri, Constans.ENCRYPTED_NONE);
  }

  public ExtractorRendererBuilder(Context context, String userAgent, Uri uri, int encryptedVersion) {
    this.context = context;
    this.userAgent = userAgent;
    this.uri = uri;
    this.encryptedVersion = encryptedVersion;
    this.start = 0;
    this.end = 0;
  }

  private static final byte[] AES_KEY = hexStringToByteArray(RotAlgo.rotate("_hgueqrscea`sbfbrpstctgbaeafqcue"));
  private static final byte[] AES_IV = hexStringToByteArray(RotAlgo.rotate("_ph`faf`eptecagc_hggdqgqgahrrq_d"));

  private static byte[] hexStringToByteArray(String s) {
    s = s.toLowerCase();
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
              + Character.digit(s.charAt(i+1), 16));
    }
    return data;
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  @Override
  public void buildRenderers(final DemoPlayer player) {
    Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);

    // Build the video and audio renderers.
    MediaCodecVideoTrackRenderer videoRenderer;
    DataSource dataSource;
    switch (encryptedVersion) {
      case Constans.ENCRYPTED_NONE:
        dataSource = new DefaultUriDataSource(context, userAgent);
        break;
      case Constans.ENCRYPTED_VER_1:
        dataSource = new EncryptedVideoDataSource(uri);
        break;
      case Constans.ENCRYPTED_VER_2:
      default:
        dataSource = new AesDataSource(uri);
        break;
    }
    ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, allocator,
            BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
    videoRenderer =new MediaCodecVideoTrackRenderer(context,
            sampleSource, MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING, 5000,
            player.getMainHandler(), player, 50);

    MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
            MediaCodecSelector.DEFAULT, null, true, player.getMainHandler(), player,
            AudioCapabilities.getCapabilities(context), AudioManager.STREAM_MUSIC);

//    TrackRenderer textRenderer = new TextTrackRenderer(sampleSource, player,
//            player.getMainHandler().getLooper());

    TrackRenderer textRenderer = null;


    // Invoke the callback.
    TrackRenderer[] renderers = new TrackRenderer[DemoPlayer.RENDERER_COUNT];
    renderers[DemoPlayer.TYPE_VIDEO] = videoRenderer;
    renderers[DemoPlayer.TYPE_AUDIO] = audioRenderer;
    if (textRenderer != null) {
      renderers[DemoPlayer.TYPE_TEXT] = textRenderer;
    }
//    player.setSelectedTrack(DemoPlayer.TYPE_TEXT, 0);
    player.onRenderers(renderers, null);
  }

  @Override
  public void cancel() {
    // Do nothing.
  }

  @Override
  public int getStartMs() {
    return start;
  }

  @Override
  public int getEndMs() {
    return end;
  }

}

package com.android.player;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.Uri;

import com.android.player.DataSource.OkUriDataSource;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;

/**
 * Created by wangye on 16-10-18.
 */

public class OkHttpExtractorRendererBuilder implements DemoPlayer.RendererBuilder {
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private final Context context;
    private final String userAgent;
    private final Uri uri;

    private boolean needEncrypty;

    public OkHttpExtractorRendererBuilder(Context context, String userAgent, Uri uri, boolean needEncrypt) {
        this.context = context;
        this.userAgent = userAgent;
        this.uri = uri;
        this.needEncrypty = needEncrypt;
    }

    public OkHttpExtractorRendererBuilder(Context context, String userAgent, Uri uri) {
        this.context = context;
        this.userAgent = userAgent;
        this.uri = uri;
        this.needEncrypty = false;
    }

    @Override
    public void buildRenderers(DemoPlayer player) {
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);

        // Build the video and audio renderers.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(),
                null);
        CacheGlue glue = new CacheGlue(uri);
        DataSource dataSource = new OkUriDataSource(context, bandwidthMeter, userAgent, glue, needEncrypty);

        ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context,
                sampleSource, MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000,
                player.getMainHandler(), player, 50);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                MediaCodecSelector.DEFAULT, null, true, player.getMainHandler(), player,
                AudioCapabilities.getCapabilities(context), AudioManager.STREAM_MUSIC);
        TrackRenderer textRenderer = new TextTrackRenderer(sampleSource, player,
                player.getMainHandler().getLooper());

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[DemoPlayer.RENDERER_COUNT];
        renderers[DemoPlayer.TYPE_VIDEO] = videoRenderer;
        renderers[DemoPlayer.TYPE_AUDIO] = audioRenderer;
        renderers[DemoPlayer.TYPE_TEXT] = textRenderer;
        player.onRenderers(renderers, bandwidthMeter);
    }

    @Override
    public void cancel() {

    }

    @Override
    public int getStartMs() {
        return 0;
    }

    @Override
    public int getEndMs() {
        return 0;
    }
}

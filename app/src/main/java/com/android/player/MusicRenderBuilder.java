package com.android.player;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.Util;

/**
 * Created by wangye on 16-10-10.
 */

public class MusicRenderBuilder implements DemoPlayer.RendererBuilder {
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private final Context context;
    private final String userAgent;
    private final Uri uri;

    public MusicRenderBuilder(Context context, String userAgent, Uri uri) {
        this.context = context;
        this.userAgent = userAgent;
        this.uri = uri;
    }

    @Override
    public void buildRenderers(DemoPlayer player) {
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        String userAgent = Util.getUserAgent(context, "ExoPlayerDemo");
        DataSource dataSource = new DefaultUriDataSource(context, null, userAgent);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                MediaCodecSelector.DEFAULT, null, true, player.getMainHandler(), player,
                AudioCapabilities.getCapabilities(context), AudioManager.STREAM_MUSIC);

        TrackRenderer[] renderers = new TrackRenderer[DemoPlayer.RENDERER_COUNT];
        renderers[DemoPlayer.TYPE_AUDIO] = audioRenderer;
        player.onRenderers(renderers, null);
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

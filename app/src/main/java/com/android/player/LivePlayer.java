package com.android.player;

import com.google.android.exoplayer.ExoPlayer;

/**
 * Created by wangye on 16-8-12.
 */
public class LivePlayer {

    // Constants pulled into this class for convenience.
    public static final int STATE_IDLE = ExoPlayer.STATE_IDLE;
    public static final int STATE_PREPARING = ExoPlayer.STATE_PREPARING;
    public static final int STATE_BUFFERING = ExoPlayer.STATE_BUFFERING;
    public static final int STATE_READY = ExoPlayer.STATE_READY;
    public static final int STATE_ENDED = ExoPlayer.STATE_ENDED;
    public static final int TRACK_DISABLED = ExoPlayer.TRACK_DISABLED;
    public static final int TRACK_DEFAULT = ExoPlayer.TRACK_DEFAULT;


    private static final int RENDERER_BUILDING_STATE_IDLE = 1;
    private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
    private static final int RENDERER_BUILDING_STATE_BUILT = 3;
    /**
     * Builds renderers for the player.
     */
    public interface RendererBuilder {
        /**
         * Builds renderers for playback.
         *
         * @param player The player for which renderers are being built. {@link DemoPlayer#onRenderers}
         *     should be invoked once the renderers have been built. If building fails,
         *     {@link DemoPlayer#onRenderersError} should be invoked.
         */
        void buildRenderers(DemoPlayer player);
        /**
         * Cancels the current build operation, if there is one. Else does nothing.
         * <p>
         * A canceled build operation must not invoke {@link DemoPlayer#onRenderers} or
         * {@link DemoPlayer#onRenderersError} on the player, which may have been released.
         */
        void cancel();
    }

    public LivePlayer() {

    }

    public void prepare() {

    }
    public void start() {

    }
}

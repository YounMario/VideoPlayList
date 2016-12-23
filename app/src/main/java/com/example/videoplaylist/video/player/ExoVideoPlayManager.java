package com.example.videoplaylist.video.player;

import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.android.player.DemoPlayer;
import com.android.player.ExtractorRendererBuilder;
import com.android.player.OkHttpExtractorRendererBuilder;
import com.example.videoplaylist.App;
import com.example.videoplaylist.R;
import com.example.videoplaylist.video.listener.VideoPlayListener;
import com.example.videoplaylist.video.utils.ClipHelper;
import com.example.videoplaylist.video.widget.VideoPlayerBottomBar;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.util.Util;

import java.lang.ref.WeakReference;

/**
 * Created by 龙泉 on 2016/10/19.
 */

public class ExoVideoPlayManager implements DemoPlayer.Listener {

    private static final String TAG = "ExoVideoPlayManager";
    private static final String TAG_PLAY_STATE = "ExoPlayState";
    private DemoPlayer mExoPlayer;
    private String playUrl;
    private PlayableWindow currentWindow;
    private Handler handler;
    private boolean mNeedCache = true;
    private boolean startPlay;

    private VideoPlayListener videoPlayListener;


    public ExoVideoPlayManager() {
        handler = new InnerHandler(this);
    }

    public void setMute(boolean volumeMute) {
        if (mExoPlayer != null) {
            mExoPlayer.setMute(volumeMute);
        }
    }


    public int getTrackCount(int typeAudio) {
        if (mExoPlayer == null) return 0;
        return mExoPlayer.getTrackCount(typeAudio);
    }

    public void onFocus() {
        if (currentWindow != null) {
            currentWindow.updateUiToFocusState();
        }
    }


    private static class InnerHandler extends Handler {
        private WeakReference<ExoVideoPlayManager> mExoVideoPlayManagerRef;

        public InnerHandler(ExoVideoPlayManager playManager) {
            mExoVideoPlayManagerRef = new WeakReference<>(playManager);
        }


        public void handleMessage(Message msg) {
            if (msg == null) {
                return;
            }

            final ExoVideoPlayManager exoVideoPlayManager = mExoVideoPlayManagerRef.get();
            if (exoVideoPlayManager == null) {
                return;
            }

            exoVideoPlayManager.handleMessage(msg);
        }
    }

    private void handleMessage(Message msg) {

    }


    public void play() {
        play(false);
    }

    public void play(boolean isMute) {
        Log.i(TAG, "play videoPlayer");
        prePare(isMute);
        mExoPlayer.setPlayWhenReady(true);
        mExoPlayer.seekTo(currentWindow.getCurrentSeek());
        currentWindow.updateUiToPrepare();
        startPlay = true;
        if (currentWindow.getPlayerView() != null) {
            VideoPlayerBottomBar bottomBar = (VideoPlayerBottomBar) currentWindow.getPlayerView().findViewById(R.id.video_play_bottom_bar);
            if (bottomBar != null) {
                bottomBar.setupPlayer(mExoPlayer);
                bottomBar.setBufferBarVisible(mNeedCache);
            }
        }
    }


    public void stopPlay() {
        Log.i(TAG_PLAY_STATE, "stop videoPlayer");
        if (mExoPlayer != null) {
            mExoPlayer.release();
            mExoPlayer = null;
        }
        if (currentWindow != null) {
            currentWindow.updateUiToNormalState();
            if (currentWindow.getPlayerView() != null) {
                VideoPlayerBottomBar bottomBar = (VideoPlayerBottomBar) currentWindow.getPlayerView().findViewById(R.id.video_play_bottom_bar);
                if (bottomBar != null) {
                    bottomBar.release();
                }
            }
        }
    }


    public void pause() {
        Log.i(TAG_PLAY_STATE, "pause videoPlayer");
        if (mExoPlayer != null) {
            mExoPlayer.setPlayWhenReady(false);
        }

        handler.removeCallbacks(updateProgressAction);
        if (currentWindow != null) {
            currentWindow.updateUiToPauseState();
        }
    }


    public void seekTo(int position) {
        if (mExoPlayer != null) {
            mExoPlayer.seekTo(position);
        }
    }


    public void setPlayableWindow(PlayableWindow window) {
        this.currentWindow = window;
    }


    public void resume() {
        if (mExoPlayer != null) {
            mExoPlayer.setPlayWhenReady(true);
        }
        if (currentWindow != null) {
            currentWindow.updateUiToPlayState();
        }
    }


    public void setPlayListener(VideoPlayListener listener) {
        this.videoPlayListener = listener;
    }


    public void setUrl(String url) {
        playUrl = url;
    }


    public String getCurrentUrl() {
        return playUrl;
    }


    public long getCurrentSeek() {
        return mExoPlayer == null ? 0 : mExoPlayer.getCurrentPosition();
    }


    public long getDuration() {
        return mExoPlayer == null ? 0 : mExoPlayer.getDuration();
    }

    public boolean isPlaying() {
        return mExoPlayer != null && mExoPlayer.getPlayWhenReady();
    }

    private void prePare(boolean isMute) {
        if (mExoPlayer != null) {
            mExoPlayer.release();
        }
        Log.i(TAG, "prepare playUrl: " + playUrl);
        mExoPlayer = new DemoPlayer(getVideoRenderBuilder(playUrl), true, true);
        mExoPlayer.addListener(this);
        mExoPlayer.prepare();


        if (currentWindow != null) {
            currentWindow.showCover();
            Surface surface = currentWindow.getPlayableSurface();
            mExoPlayer.setSurface(surface);
        }
        //mExoPlayer.setSurface();
        mExoPlayer.setMute(isMute);
        mExoPlayer.setPlayWhenReady(false);

    }

    private DemoPlayer.RendererBuilder getVideoRenderBuilder(String playUrl) {
        String userAgent = Util.getUserAgent(App.getInstance(), "livelocker-ExoPlayer");
        Uri uri = Uri.parse(playUrl);
        mNeedCache = "http".equalsIgnoreCase(uri.getScheme());
        DemoPlayer.RendererBuilder render = mNeedCache ?
                new OkHttpExtractorRendererBuilder(App.getInstance(), userAgent, uri) :
                new ExtractorRendererBuilder(App.getInstance(), userAgent, uri);
        return render;
    }

    private void reset() {
        if (mExoPlayer != null) {
            mExoPlayer.setPlayWhenReady(false);
            mExoPlayer.release();
        }
    }


    public void onStateChanged(boolean playWhenReady, int playbackState) {
        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                text += "buffering";
                onBuffering(playWhenReady);
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";
                onVideoPlayFinished(playWhenReady);
                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_PREPARING:
                text += "preparing";
                onPreparing();
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";
                onPlayerReady(playWhenReady);
                break;
            default:
                text += "unknown";
                break;
        }
        if (mExoPlayer != null) {
            Log.i(TAG, "onStateChanged: " + text);
            Log.i(TAG, "buffered: " + mExoPlayer.getBufferedPercentage()
                    + ", current: " + mExoPlayer.getCurrentPosition()
                    + ", duration: " + mExoPlayer.getDuration());
            updateProgressBar();
        }
    }

    private void onPreparing() {
        if (currentWindow != null) {
            currentWindow.showLoading();
        }
    }

    private void onBuffering(boolean isPlayWhenReady) {
        if (currentWindow != null && isPlayWhenReady && mNeedCache) {
            currentWindow.showLoading();
        }
    }

    private void onPlayerReady(boolean playWhenReady) {
        if (videoPlayListener != null) {
            videoPlayListener.playerReady(playWhenReady);
        }
        if (currentWindow != null) {
            currentWindow.hideLoading();
            if (playWhenReady) {
                currentWindow.updateUiToPlayState();
            }
        }
    }

    private void onVideoPlayFinished(boolean playWhenReady) {
        if (videoPlayListener != null) {
            videoPlayListener.playFinished(playWhenReady);
        }
    }


    public void onError(Exception e) {
        if (videoPlayListener != null) {
            videoPlayListener.onError(e);
        }
    }


    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        if (currentWindow == null) {
            return;
        }
        TextureView textureView = (TextureView) currentWindow.getVideoView();

        if (textureView == null) return;
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        float scaledX = 1.f * width / viewWidth;
        float scaledY = 1.f * height / viewHeight;
        float scale = Math.max(1 / scaledX, 1 / scaledY);

        // Calculate pivot points, in our case crop from center
        int pivotPointX = viewWidth / 2;
        int pivotPointY = viewHeight / 2;

        Matrix matrix = new Matrix();
        matrix.setScale(scaledX * scale, scaledY * scale, pivotPointX, pivotPointY);
        float offset = ClipHelper.getTranslateOffset(currentWindow.getClipType(), width, height, viewWidth, viewHeight);
        matrix.postTranslate(0, offset);
        textureView.setTransform(matrix);
    }

    private void updateProgressBar() {
        if (mExoPlayer == null || currentWindow == null) {
            return;
        }
        long duration = mExoPlayer.getDuration();
        long position = mExoPlayer.getCurrentPosition();

        float percent = (float) (position * 1.0 * 100 / duration);
        long bufferedPercent = mExoPlayer == null ? 0 : mExoPlayer.getBufferedPercentage();
        Log.i(TAG, "buffering :" + bufferedPercent + " play position:" + position);
        if (mExoPlayer.getPlayerControl() != null && mExoPlayer.getPlayerControl().isPlaying()) {
            if (startPlay && percent > 0.1) {
                startPlay = false;
                if (videoPlayListener != null) {
                    videoPlayListener.playStarted();
                }
            }
            if (percent > 0.1) {
                currentWindow.hideCover();
            }
        } else {
            startPlay = false;
        }

        // Remove scheduled updates.
        handler.removeCallbacks(updateProgressAction);
        // Schedule an update if necessary.
        int playbackState = mExoPlayer == null ? ExoPlayer.STATE_IDLE : mExoPlayer.getPlaybackState();
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            long delayMs;
            if (mExoPlayer.getPlayWhenReady() && playbackState == ExoPlayer.STATE_READY) {
                delayMs = 1000 - (position % 1000);
                if (delayMs < 200) {
                    delayMs += 1000;
                }
            } else {
                delayMs = 1000;
            }
            handler.postDelayed(updateProgressAction, delayMs);
        }
    }

    private boolean needShowBufferBar(long bufferedPercent) {
        return bufferedPercent == 100 || mExoPlayer.getPlaybackState() == ExoPlayer.STATE_PREPARING;
    }


    private final Runnable updateProgressAction = new Runnable() {

        public void run() {
            updateProgressBar();
        }
    };


    public void release() {
        Log.i(TAG_PLAY_STATE, "relase videoPlayer");
        handler.removeCallbacks(updateProgressAction);
        if (mExoPlayer != null) {
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }


    public void setSurface(Surface surface) {
        if (mExoPlayer != null) {
            mExoPlayer.setSurface(surface);
        }
    }


}

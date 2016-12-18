package com.example.videoplaylist.video.video.player;

import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.view.TextureView;

import com.android.player.DemoPlayer;
import com.android.player.ExtractorRendererBuilder;
import com.android.player.OkHttpExtractorRendererBuilder;
import com.cleanmaster.util.CMLog;
import com.cmcm.locker.R;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.util.Util;
import com.keniu.security.MoSecurityApplication;
import com.locker.newscard.video.listener.VideoPlayListener;
import com.locker.newscard.video.utils.ClipHelper;
import com.locker.newscard.video.widget.VideoPlayerBottomBar;

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
    private long startBufferring = -1;
    private CheckFileRunnable checkFileRunnable;

    private VideoPlayListener videoPlayListener;
    private boolean needEncrypt;

    public ExoVideoPlayManager() {
        this(true);
    }

    public ExoVideoPlayManager(boolean needEncrypt) {
        handler = new InnerHandler(this);
        this.needEncrypt = needEncrypt;
    }

    public void setMute(boolean volumeMute) {
        if (mExoPlayer != null) {
            mExoPlayer.setMute(volumeMute);
        }
    }

    public void setNotifyListener(DemoPlayer.NotifyListener mNotifyListener) {
        if (mExoPlayer != null) {
            mExoPlayer.setNotifyListener(mNotifyListener);
        }
    }

    public int getTrackCount(int typeAudio) {
        if (mExoPlayer == null) return 0;
        return mExoPlayer.getTrackCount(typeAudio);
    }


    private static class InnerHandler extends Handler {
        private WeakReference<ExoVideoPlayManager> mExoVideoPlayManagerRef;

        public InnerHandler(ExoVideoPlayManager playManager) {
            mExoVideoPlayManagerRef = new WeakReference<ExoVideoPlayManager>(playManager);
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
        CMLog.i(TAG, "play videoPlayer");
        prePare(isMute);
        mExoPlayer.setPlayWhenReady(true);
        mExoPlayer.seekTo( currentWindow.getCurrentSeek());
        currentWindow.updateUiToPlayState();
        startPlay = true;
        if (currentWindow.getPlayerView() != null) {
            VideoPlayerBottomBar bottomBar = (VideoPlayerBottomBar) currentWindow.getPlayerView().findViewById(R.id.video_play_bottom_bar);
            if (bottomBar != null) {
                bottomBar.setupPlayer(mExoPlayer);
            }
        }
    }


    public void stopPlay() {
        CMLog.i(TAG_PLAY_STATE, "stop videoPlayer");
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
        CMLog.i(TAG_PLAY_STATE, "pause videoPlayer");
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
            if (position == 0) {
                reportPlayVideo();
            }
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
            currentWindow.updateUiToResumeState();
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
        if (checkFileRunnable != null) {
            handler.removeCallbacks(checkFileRunnable);
            checkFileRunnable = null;
        }
        CMLog.i(TAG, "prepare playUrl: " + playUrl);
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
        String userAgent = Util.getUserAgent(MoSecurityApplication.getInstance(), "livelocker-ExoPlayer");
        Uri uri = Uri.parse(playUrl);
        mNeedCache = "http".equalsIgnoreCase(uri.getScheme());
        DemoPlayer.RendererBuilder render = mNeedCache ?
                new OkHttpExtractorRendererBuilder(MoSecurityApplication.getInstance(), userAgent, uri) :
                new ExtractorRendererBuilder(MoSecurityApplication.getInstance(), userAgent, uri);
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
                startBufferring = System.currentTimeMillis();
                runCheckDownload();
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";
                reportPlayFinished();
                onVideoPlayFinished(playWhenReady);
                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_PREPARING:
                text += "preparing";
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";
                reportBufferTime();
                onPlayerReady(playWhenReady);
                break;
            default:
                text += "unknown";
                break;
        }
        CMLog.i(TAG, "onStateChanged: " + text);
        CMLog.i(TAG, "buffered: " + mExoPlayer.getBufferedPercentage()
                + ", current: " + mExoPlayer.getCurrentPosition()
                + ", duration: " + mExoPlayer.getDuration());
        updateProgressBar();
    }

    private void onPlayerReady(boolean playWhenReady) {
        if (videoPlayListener != null) {
            videoPlayListener.playerReady(playWhenReady);
        }
    }

    private void onVideoPlayFinished(boolean playWhenReady) {
        if (videoPlayListener != null) {
            videoPlayListener.playFinished(playWhenReady);
        }
    }

    private void runCheckDownload() {
        if (!mNeedCache) {
            return;
        }
        if (checkFileRunnable != null) {
            handler.removeCallbacks(checkFileRunnable);
            checkFileRunnable = null;
        }
        checkFileRunnable = new CheckFileRunnable();
        handler.post(checkFileRunnable);
    }

    private void reportBufferTime() {
        if (!mNeedCache) {
            return;
        }
        if (mExoPlayer != null) {
            int time = (int) mExoPlayer.getDuration();
            if (startBufferring != -1) {
                long during = System.currentTimeMillis() - startBufferring;
                startBufferring = -1;
            }
        }
    }

    private void reportPlayFinished() {
        if (mExoPlayer != null) {
            int time = (int) mExoPlayer.getDuration();
        }
    }

    private void reportPlayVideo() {
        if (mExoPlayer != null) {
            int time = (int) mExoPlayer.getDuration();
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
        TextureView textureView =  (TextureView) currentWindow.getVideoView();

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
        CMLog.i(TAG, "buffering :" + bufferedPercent + " play position:" + position);
        if (bufferedPercent == 100 || !mNeedCache) {
            currentWindow.hideBufferBar();
        } else {
            currentWindow.showBufferBar();
        }
        if (mExoPlayer.getPlayerControl() != null && mExoPlayer.getPlayerControl().isPlaying()) {
            if (startPlay && percent > 0.1) {
                startPlay = false;
                if (videoPlayListener != null) {
                    videoPlayListener.playStarted();
                }
                reportPlayVideo();
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
        CMLog.i(TAG_PLAY_STATE, "relase videoPlayer");
        handler.removeCallbacks(updateProgressAction);
        if (checkFileRunnable != null) {
            handler.removeCallbacks(checkFileRunnable);
            checkFileRunnable = null;
        }
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

    private class CheckFileRunnable implements Runnable {

        public CheckFileRunnable() {
        }


        public void run() {
            if (mExoPlayer != null && mExoPlayer.getBufferedPercentage() == 100) {
            } else {
                handler.postDelayed(this, 1000);
            }
        }
    }

}

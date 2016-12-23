package com.example.videoplaylist.video.player.manager;

import android.text.TextUtils;
import android.util.Log;

import com.example.videoplaylist.utils.FileUtils;
import com.example.videoplaylist.video.bean.VideoInfo;
import com.example.videoplaylist.video.player.PlayableWindow;

import java.io.File;

/**
 * Created by 龙泉 on 2016/12/23.
 */

public class DefaultVideoPlayManager implements VideoPlayManager {


    private static final String TAG = "DefaultVideoPlayManager";

    private static final int STATE_INIT = -1;
    private static final int STATE_INITED = 0;
    private static final int STATE_NORMAL = 1;
    private static final int STATE_PLAY = 2;
    private static final int STATE_PAUSE = 3;


    private int mCurrentState;
    private PlayableWindow mCurrentWindow;


    @Override
    public void play() {
        int currentPlayingPosition = mCurrentWindow.getWindowIndex();
        Log.i(TAG, "playing position:" + currentPlayingPosition);
        String url = getPlayAblePath();
        if (url == null) {
            return;
        }
        setCurrentState(STATE_PLAY);
        mCurrentWindow.setUrl(url);
        mCurrentWindow.setPlayActive(true);
        mCurrentWindow.play();
    }

    @Override
    public void stopPlay() {
        if (mCurrentWindow == null) {
            return;
        }
        Log.i(TAG, "stop play position:" + mCurrentWindow.getWindowIndex());
        setCurrentState(STATE_NORMAL);
        if (mCurrentWindow != null) {
            mCurrentWindow.stopPlay();
            mCurrentWindow.setPlayActive(false);
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void setPlayableWindow(PlayableWindow window) {
        this.mCurrentWindow = window;
    }

    @Override
    public PlayableWindow getCurrentPlayableWindow() {
        return mCurrentWindow;
    }

    @Override
    public void resume() {
        if (mCurrentState == STATE_NORMAL || mCurrentState == STATE_PAUSE) {
            play();
        }
    }

    @Override
    public void onScrollFinished(boolean isUp) {

    }

    @Override
    public void release() {
        stopPlay();
        if (mCurrentWindow != null) {
            mCurrentWindow.onRelease();
            mCurrentWindow = null;
        }
    }

    private String getPlayAblePath() {
        if (mCurrentWindow == null) {
            return null;
        }
        final VideoInfo itemVideoInfo = (VideoInfo) mCurrentWindow.getVideoItem();
        String localPath = itemVideoInfo.getVideoLocalPath();
        String webPath = itemVideoInfo.getVideoUrl();

        if (TextUtils.isEmpty(localPath) && !TextUtils.isEmpty(webPath)) {
            localPath = FileUtils.convertUrlToLocalPath(webPath);
        }
        File f = new File(localPath);
        if (f.exists()) {
            return localPath;
        } else {
            if (TextUtils.isEmpty(webPath)) {
                throw new RuntimeException("error url");
            }
            return webPath;
        }
    }

    private void setCurrentState(int currentState) {
        this.mCurrentState = currentState;
    }
}

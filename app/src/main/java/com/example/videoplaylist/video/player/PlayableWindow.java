package com.example.videoplaylist.video.player;

import android.view.Surface;
import android.view.View;

import com.example.videoplaylist.video.bean.VideoInfo;
import com.example.videoplaylist.video.player.manager.VideoPlayManager;

/**
 * Created by 龙泉 on 2016/10/19.
 */

public interface PlayableWindow<T> {

    boolean canPlay();

    View getPlayerView();

    View getVideoView();

    View getVideoPlayBtn();

    Surface getPlayableSurface();

    void updateUiToPlayState();

    void updateUiToPauseState();

    void updateUiToResumeState();

    void updateUiToNormalState();

    void updateUiToFocusState();

    void updateUiToPrepare();

    void showLoading();

    void hideLoading();

    void showCover();

    void hideCover();

    void setClipType(int type);

    int getClipType();

    float getWindowLastCalculateArea();

    void setCurrentSeek(long playTime);

    long getCurrentSeek();

    void setWindowIndex(int index);

    int getWindowIndex();

    void onRelease();

    //----new --------
    void stopPlay();

    boolean isPlaying();

    void setUrl(String url);

    void play();

    void pause();

    void resume();

    void onFocus();

    void setSurface(Surface mSurface);

    void setVideoPlayManager(VideoPlayManager videoPlayManager);

    T getVideoItem();

    void updateVideoItem(T videoItem);

    boolean playActive();

    void setPlayActive(boolean playActive);
}

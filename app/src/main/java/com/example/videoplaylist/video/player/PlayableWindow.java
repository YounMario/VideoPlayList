package com.example.videoplaylist.video.player;

import android.view.Surface;
import android.view.View;

/**
 * Created by 龙泉 on 2016/10/19.
 */

public interface PlayableWindow {

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
}

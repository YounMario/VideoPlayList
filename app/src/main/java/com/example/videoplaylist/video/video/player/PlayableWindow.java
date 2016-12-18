package com.example.videoplaylist.video.video.player;

import android.view.Surface;
import android.view.View;

/**
 * Created by 龙泉 on 2016/10/19.
 */

public interface PlayableWindow {

    boolean canPlay();

    View getPlayerView();

    View getVideoView();

    Surface getPlayableSurface();

    void updateBufferProgress(int progress);

    void updateUiToPlayState();

    void updateUiToPauseState();

    void updateUiToResumeState();

    void updateUiToNormalState();

    void showBufferBar();

    void showPlayBar();

    void hideBufferBar();

    void hidePlayBuffer();

    void showCover();

    void hideCover();

    void setClipType(int type);

    int getClipType();

    float getWindowLastCalculateArea();

    void setCurrentSeek(long playTime);

    long getCurrentSeek();

    void setWindowIndex(int index);

    int getWindowIndex();

}

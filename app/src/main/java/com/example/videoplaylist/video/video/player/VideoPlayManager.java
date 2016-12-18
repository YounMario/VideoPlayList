package com.example.videoplaylist.video.video.player;


/**
 * Created by 龙泉 on 2016/10/19.
 */

public interface VideoPlayManager {

    void play();

    void stopPlay();

    void pause();

    void setPlayableWindow(PlayableWindow window);

    PlayableWindow getCurrentPlayableWindow();

    void resume();

    void onScrollFinished(boolean isUp);

}

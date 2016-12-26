package com.example.videoplaylist.video.player.manager;


import com.example.videoplaylist.video.player.PlayableWindow;

/**
 * Created by 龙泉 on 2016/10/19.
 */

public interface VideoPlayManager {

    void play();

    void stopPlay();

    void pause();

    PlayableWindow getCurrentPlayableWindow();

    void resume();

    void onScrollFinished(boolean isUp);

    void release();

    void onAttach(PlayableWindow needPlayWindow);

    void onDetach(PlayableWindow currentPlayableWindow);
}

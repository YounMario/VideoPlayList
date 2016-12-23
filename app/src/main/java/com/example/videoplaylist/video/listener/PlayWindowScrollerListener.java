package com.example.videoplaylist.video.listener;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;


import com.example.videoplaylist.video.player.PlayableWindow;
import com.example.videoplaylist.video.player.manager.VideoPlayManager;

import java.util.ArrayList;

/**
 * Created by 龙泉 on 2016/10/19.
 */

public class PlayWindowScrollerListener extends RecyclerView.OnScrollListener {

    private static final String TAG = "ScrollerListener";

    private VideoPlayManager playManager;
    private ArrayList<PlayableWindow> playableWindows;
    private boolean mLastScroll;

    public PlayWindowScrollerListener(VideoPlayManager playManager) {
        this.playManager = playManager;
        playableWindows = new ArrayList<>();
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        mLastScroll = dy > 0;
    }


    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (newState != RecyclerView.SCROLL_STATE_IDLE) {
            return;
        }
        playManager.onScrollFinished(mLastScroll);

        playableWindows.clear();
        PlayableWindow currentPlayableWindow = playManager.getCurrentPlayableWindow();

        int firstPosition = RecyclerView.NO_POSITION;
        int lastPosition = RecyclerView.NO_POSITION;

        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            firstPosition = linearLayoutManager.findFirstVisibleItemPosition();
            lastPosition = linearLayoutManager.findLastVisibleItemPosition();
        }
        //枚举第一次展现的item 到最后一次展现的item的播放权重
        for (int i = firstPosition; i <= lastPosition; i++) {
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
            if (holder instanceof PlayableWindow) {
                PlayableWindow playerWindow = (PlayableWindow) holder;
                //update all visible item position
                playerWindow.setWindowIndex(i);
                if (playerWindow.canPlay()) {
                    playableWindows.add(playerWindow);
                }
            }
        }


        PlayableWindow needPlayWindow = findNeedPlay(lastPosition, recyclerView.getAdapter().getItemCount());


        if (isWindowIndexNotChanged(needPlayWindow,currentPlayableWindow) && isPlayWindowInstanceNotChanged(needPlayWindow,currentPlayableWindow)) {
            playManager.resume();
            return;
        }

        if (currentPlayableWindow != null) {
            playManager.stopPlay();
        }

        if (needPlayWindow == null) {
            return;
        }
        playManager.setPlayableWindow(needPlayWindow);
        playManager.play();
    }


    private PlayableWindow findNeedPlay(int lastPosition, int recycleViewItemCount) {
        //not found playable window
        if (playableWindows.size() == 0) {
            return null;
        }
        //last item invisible return firstPlayable Position
        int lastIndex = playableWindows.size() - 1;
        PlayableWindow lastPlayableWindow = playableWindows.get(lastIndex);

        //if lastPosition is not the last of recycleView item
        if (lastPosition != recycleViewItemCount - 1 || lastPlayableWindow.getWindowIndex() < lastPosition) {
            return playableWindows.get(0);
        }

        //last visible item must be not null here

        PlayableWindow secondLastPlayableWindow = playableWindows.get(lastIndex - 1);

        //if secondLastPlayableWindow's area larger than LastPlayableWindow return secondLastPlayableWindow
        if (secondLastPlayableWindow != null) {
            //indexListSize is at less 2 here
            if (lastPlayableWindow.getWindowLastCalculateArea() < secondLastPlayableWindow.getWindowLastCalculateArea()) {
                return secondLastPlayableWindow;
            }
        }
        return lastPlayableWindow;
    }

    private boolean isWindowIndexNotChanged(PlayableWindow needPlayWindow, PlayableWindow currentPlayableWindow) {
        return needPlayWindow != null && currentPlayableWindow != null && needPlayWindow.getWindowIndex() == currentPlayableWindow.getWindowIndex();
    }

    private boolean isPlayWindowInstanceNotChanged(PlayableWindow window1, PlayableWindow window2) {
        return window1 == window2;
    }
}
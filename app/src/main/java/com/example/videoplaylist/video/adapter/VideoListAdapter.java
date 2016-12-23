package com.example.videoplaylist.video.adapter;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.videoplaylist.App;
import com.example.videoplaylist.R;
import com.example.videoplaylist.video.bean.VideoInfo;
import com.example.videoplaylist.video.holder.VideoItemHolder;
import com.example.videoplaylist.video.player.PlayableWindow;
import com.example.videoplaylist.video.player.manager.VideoPlayManager;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 龙泉 on 2016/12/15.
 */

public class VideoListAdapter extends RecyclerView.Adapter {


    private static final String TAG = "VideoListAdapter";

    private static final String TAG_ITEM_STATE = "video_item_state";
    private static final String TAG_SAVE_CURRENT = "save_current_seek";
    private int currentState;

    private PlayableWindow currentWindow;
    private RecyclerView mRecycleView;

    private Surface mSurface;


    private List<VideoInfo> data;
    private SparseArray<Long> playSeekMap;

    private LinearLayoutManager mLinearLayoutManager;

    private static final int STATE_INIT = -1;
    private static final int STATE_INITED = 0;
    private static final int STATE_NORMAL = 1;
    private static final int STATE_PLAY = 2;
    private static final int STATE_PAUSE = 3;


    private VideoPlayManager mVideoPlayManager;


//    public void onResume() {
//        if (currentState == STATE_PAUSE || currentState == STATE_INITED) {
//            Log.i(TAG_ITEM_STATE, "on onResumePlay : " + this);
//            playVideo(true);
//        }
//    }

//    private void onScrollTo(int position, boolean needClearSeek) {
//        if (!currentPositionIsIllegal() && currentWindow.getWindowIndex() != position && !positionIsIllegal(position)) {
//            stopPlay();
//            if (needClearSeek) {
//                playSeekMap.put(currentWindow.getWindowIndex(), 0L);
//            }
//            int offset = currentWindow.getPlayerView().getTop();
//            PlayableWindow newWindow = (PlayableWindow) mRecycleView.findViewHolderForAdapterPosition(position);
//            newWindow.setWindowIndex(position);
//            setPlayableWindow(newWindow);
//            mLinearLayoutManager.scrollToPositionWithOffset(position, offset);
//            playVideo(false);
//        }
//    }

    private boolean positionIsIllegal(int position) {
        return position < 0 || position >= getItemCount();
    }


    public void release() {
        Log.i(TAG_ITEM_STATE, "release player:" + this);
        mVideoPlayManager.release();
    }


    public void setData(List<VideoInfo> videos) {
        data = videos;
        notifyDataSetChanged();
    }


    public void setLinearLayout(LinearLayoutManager linearLayoutManager) {
        this.mLinearLayoutManager = linearLayoutManager;
    }


    public VideoListAdapter(RecyclerView recyclerView) {
        mRecycleView = recyclerView;
        setCurrentState(STATE_INIT);

        data = new ArrayList<>();
//        mVideoPlayerManager.setPlayListener(new VideoPlayListener() {
//            @Override
//            public void playFinished(boolean playWhenReady) {
//                if (currentPositionIsIllegal()) {
//                    return;
//                }
//                onScrollTo(currentWindow.getWindowIndex() + 1, true);
//                if (lastPlayVideoUrlChanged()) {
//                    playVideo(false);
//                } else {
//                    mVideoPlayerManager.seekTo(0);
//                }
//                setExpired();
//            }
//
//            @Override
//            public void playStarted() {
//            }
//
//            @Override
//            public void playerReady(boolean playWhenReady) {
//
//            }
//
//            @Override
//            public void onError(Exception ex) {
//
//            }
//        });

        playSeekMap = new SparseArray<>();
    }


    private boolean currentPositionIsIllegal() {
        return currentWindow == null || positionIsIllegal(currentWindow.getWindowIndex());
    }

//    private boolean lastPlayVideoUrlChanged() {
//        String currentPlayUrl = getPlayAblePath();
//        if (currentPlayUrl == null) {
//            return false;
//        }
//
//        if (!currentPlayUrl.equals(mVideoPlayerManager.getCurrentUrl())) {
//            return true;
//        }
//        return false;
//    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_list, parent, false);
        VideoItemHolder videoItemHolder = new VideoItemHolder(itemView);
        videoItemHolder.setVideoPlayManager(mVideoPlayManager);
        return videoItemHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        VideoInfo videoInfo = getItem(position);
        final VideoItemHolder window = (VideoItemHolder) holder;
        window.updateVideoItem(videoInfo);

        if (currentState == STATE_INIT) {
            currentWindow = window;
            currentWindow.setWindowIndex(position);
            playSeekMap.put(position, 0L);
            setCurrentState(STATE_INITED);
        }

        final ImageView playBtn = (ImageView) window.getVideoPlayBtn();
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentWindow.getWindowIndex() == position) {
                    if (currentState == STATE_PLAY) {
                        setCurrentState(STATE_PAUSE);
                        currentWindow.pause();
                        playBtn.setImageResource(R.drawable.icon_play);
                    } else if (currentState == STATE_PAUSE) {
                        setCurrentState(STATE_PLAY);
                        currentWindow.resume();
                        playBtn.setImageResource(R.drawable.icon_pause);
                    }
                }
            }
        });

        window.getVideoView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentWindow.getWindowIndex() == position) {
                    currentWindow.onFocus();
                } else {
                    Log.i(TAG, "scroll to next");
                    //onScrollTo(position, false);
                }
            }
        });

        final ImageView imageView = (ImageView) window.getView(R.id.cover_image_view);
        Picasso.with(App.getInstance())
                .load(videoInfo.getThumbnailUrl())
                .fit()
                .into(imageView);



        TextView descText = (TextView) window.getView(R.id.txt_desc);
        String desc = videoInfo.getDesc();
        descText.setText(desc);


        if (currentWindow != null && currentWindow.getWindowIndex() != position) {
            playBtn.setImageResource(R.drawable.icon_pause);
            window.updateUiToNormalState();
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public VideoInfo getItem(int position) {
        return data.get(position);
    }


    private void setCurrentState(int currentState) {
        Log.i("aStateChanged", " currentState:" + currentState);
        this.currentState = currentState;
    }

    public void setVideoPlayManager(VideoPlayManager videoPlayManager){
        this.mVideoPlayManager = videoPlayManager;
    }


    private void saveCurrentPlayTime(PlayableWindow currentPlayingWindow) {
        if (currentPlayingWindow != null && currentPlayingWindow.isPlaying()) {
            long currentSeek = currentPlayingWindow.getCurrentSeek();
            Log.i(TAG_SAVE_CURRENT, " save current play time position :" + currentPlayingWindow.getWindowIndex() + "  current seek:" + currentSeek);
            playSeekMap.put(currentPlayingWindow.getWindowIndex(), currentSeek);
        }
    }

    private void applyCurrentPlayTime(PlayableWindow playableWindow) {
        Log.i(TAG_SAVE_CURRENT, " apply current play time");
        int position = playableWindow.getWindowIndex();
        if (playableWindow != null) {
            Long currentSeek = playSeekMap.get(position);
            if (currentSeek != null) {
                playableWindow.setCurrentSeek(currentSeek);
            } else {
                playableWindow.setCurrentSeek(0);
            }
            Log.i(TAG_SAVE_CURRENT, " apply current playTime position:" + position + " current seek:" + currentSeek);
        }

    }

}

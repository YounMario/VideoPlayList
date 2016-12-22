package com.example.videoplaylist.video.adapter;

import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.videoplaylist.App;
import com.example.videoplaylist.R;
import com.example.videoplaylist.utils.FileUtils;
import com.example.videoplaylist.video.bean.VideoInfo;
import com.example.videoplaylist.video.holder.VideoItemHolder;
import com.example.videoplaylist.video.listener.VideoPlayListener;
import com.example.videoplaylist.video.player.ExoVideoPlayManager;
import com.example.videoplaylist.video.player.PlayableWindow;
import com.example.videoplaylist.video.player.VideoPlayManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 龙泉 on 2016/12/15.
 */

public class VideoListAdapter extends RecyclerView.Adapter implements VideoPlayManager {



    private static final String TAG = "VideoListAdapter";
    private static final String TAG_TEXTURE = "texture";

    private static final String TAG_ITEM_STATE = "video_item_state";
    private static final String TAG_SAVE_CURRENT = "save_current_seek";
    private final Drawable mPauseIconDrawable;
    private final Drawable mPlayIconDrawable;
    private ItemState currentState;

    private ExoVideoPlayManager mVideoPlayerManager;
    private PlayableWindow currentWindow;
    private RecyclerView mRecycleView;

    private Surface mSurface;


    private List<VideoInfo> data;
    private SparseArray<Long> playSeekMap;

    private long mFirstItemDefaultSeek;
    private LinearLayoutManager mLinearLayoutManager;



    public void onResume() {
        if (checkStateEquals(currentState, ItemState.PAUSE) || checkStateEquals(currentState, ItemState.INITED)) {
            Log.i(TAG_ITEM_STATE, "on onResumePlay : " + this);
            playVideo(true);
        }
    }

    private void onScrollTo(int position, boolean needClearSeek) {
        if (!currentPositionIsIllegal() && currentWindow.getWindowIndex() != position && !positionIsIllegal(position)) {
            stopPlay();
            if (needClearSeek) {
                playSeekMap.put(currentWindow.getWindowIndex(), 0L);
            }
            int offset = currentWindow.getPlayerView().getTop();
            PlayableWindow newWindow = (PlayableWindow) mRecycleView.findViewHolderForAdapterPosition(position);
            newWindow.setWindowIndex(position);
            setPlayableWindow(newWindow);
            mLinearLayoutManager.scrollToPositionWithOffset(position, offset);
            playVideo(false);
        }
    }

    private boolean positionIsIllegal(int position){
        return position < 0 || position >= getItemCount();
    }


    public void release() {
        if (mVideoPlayerManager != null) {
            Log.i(TAG_ITEM_STATE, "release player:" + this);
            stopPlay();
            if (currentWindow != null) {
                currentWindow.onRelease();
                currentWindow = null;
            }
            mVideoPlayerManager.release();
        }
    }

    private static boolean checkStateEquals(ItemState one, ItemState other) {
        return !(one == null || other == null) && one.equals(other);
    }

    public void setData(List<VideoInfo> videos) {
        data = videos;
        notifyDataSetChanged();
    }

    public void setFirstItemSeek(long firstItemSeek) {
        mFirstItemDefaultSeek = firstItemSeek;
    }



    public void setFirstItemDuring(long mVideoDuring) {

    }

    public void setLinearLayout(LinearLayoutManager linearLayoutManager) {
        this.mLinearLayoutManager = linearLayoutManager;
    }


    private enum ItemState {
        NOMAL, PLAY, PAUSE, INIT, INITED;
    }

    public VideoListAdapter(RecyclerView recyclerView) {
        mRecycleView = recyclerView;
        mVideoPlayerManager = new ExoVideoPlayManager();
        currentState = ItemState.INIT;

        data = new ArrayList<>();
        mVideoPlayerManager.setPlayListener(new VideoPlayListener() {
            @Override
            public void playFinished(boolean playWhenReady) {
                if (currentPositionIsIllegal()) {
                    return;
                }
                onScrollTo(currentWindow.getWindowIndex() + 1, true);
                if (lastPlayVideoUrlChanged()) {
                    playVideo(false);
                } else {
                    mVideoPlayerManager.seekTo(0);
                }
                setExpired();
            }

            @Override
            public void playStarted() {
            }

            @Override
            public void playerReady(boolean playWhenReady) {

            }

            @Override
            public void onError(Exception ex) {

            }
        });

        playSeekMap = new SparseArray<>();

        //Log.i("locker_news_videoplay", "onCreate ：mStartTime 被置为了  = " + mStartTime );
        Resources resources = App.getInstance().getResources();
        mPauseIconDrawable = resources.getDrawable(R.drawable.icon_pause);
        mPlayIconDrawable = resources.getDrawable(R.drawable.icon_play);
    }



    private boolean currentPositionIsIllegal() {
        return currentWindow == null || positionIsIllegal(currentWindow.getWindowIndex());
    }

    private boolean lastPlayVideoUrlChanged() {
        String currentPlayUrl = getPlayAblePath();
        if (currentPlayUrl == null) {
            return false;
        }

        if (!currentPlayUrl.equals(mVideoPlayerManager.getCurrentUrl())) {
            return true;
        }
        return false;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_list, parent, false);
        return new VideoItemHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        VideoInfo videoInfo = getItem(position);
        final VideoItemHolder window = (VideoItemHolder) holder;

        if (checkStateEquals(currentState, ItemState.INIT)) {
            currentWindow = window;
            currentWindow.setWindowIndex(position);
            currentWindow.setCurrentSeek(mFirstItemDefaultSeek);
            playSeekMap.put(position, mFirstItemDefaultSeek);
            setCurrentState(ItemState.INITED);
        }

        final ImageView playBtn = (ImageView) window.getVideoPlayBtn();
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentWindow.getWindowIndex() == position) {
                    if (checkStateEquals(currentState, ItemState.PLAY)) {
                        window.updateUiToPauseState();
                        setCurrentState(ItemState.PAUSE);
                        mVideoPlayerManager.pause();
                        playBtn.setImageDrawable(mPlayIconDrawable);
                    } else if (checkStateEquals(currentState, ItemState.PAUSE)) {
                        setCurrentState(ItemState.PLAY);
                        window.updateUiToPlayState();
                        mVideoPlayerManager.resume();
                        playBtn.setImageDrawable(mPauseIconDrawable);
                    }
                }
            }
        });

        window.getVideoView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentWindow.getWindowIndex() == position) {
                    currentWindow.updateUiToFocusState();
                } else {
                    Log.i(TAG, "scroll to next");
                    onScrollTo(position, false);
                }
            }
        });

        final ImageView imageView = (ImageView) window.getView(R.id.cover_image_view);

        final TextureView textureView = (TextureView) window.getView(R.id.textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.i(TAG_TEXTURE, "is available position:" + position + " view:" + textureView);
                if (currentWindow != null && position == currentWindow.getWindowIndex()) {
                    if (mSurface != null) {
                        mSurface.release();
                    }
                    mSurface = new Surface(surface);

                    mVideoPlayerManager.setSurface(mSurface);
                    if (!checkStateEquals(currentState, ItemState.PLAY)) {
                        Log.i(TAG_ITEM_STATE, "first show and play  position:" + position + " object:" + VideoListAdapter.this);
                        playVideo(false);
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if(surface != null){
                    surface.release();
                }
                if (currentWindow != null && position == currentWindow.getWindowIndex()) {
                    Log.i(TAG_TEXTURE, "is surface texture disdroyed:" + position + " view:" + textureView);
                    if (mSurface != null) {
                        mSurface.release();
                        mSurface = null;
                    }
                    Log.i("locker_news_videoplay", "on pause.......");
                    onVideoChange();
                    pause();
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//                Log.i(TAG_TEXTURE, "is surface texture update:" + position);
            }
        });


        TextView descText = (TextView) window.getView(R.id.txt_desc);
        String desc = videoInfo.getDesc();
        descText.setText(desc);

        if (window.getWindowIndex() != position) {
            window.updateUiToNormalState();
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public VideoInfo getItem(int position){
        return data.get(position);
    }


    @Override
    public void play() {
        playVideo(true);
    }

    private String getPlayAblePath() {
        if(currentWindow == null){
            return null;
        }
        final VideoInfo itemVideoInfo = getItem(currentWindow.getWindowIndex());
        String localPath = itemVideoInfo.getVideoLocalPath();
        String webPath = itemVideoInfo.getVideoUrl();

        if (TextUtils.isEmpty(localPath) && !TextUtils.isEmpty(webPath)) {
            localPath = FileUtils.convertUrlToLocalPath(webPath);
        }
        File f = new File(localPath);
        if(f.exists()){
            return localPath;
        }else{
            if(TextUtils.isEmpty(webPath)){
                throw new RuntimeException("error url");
            }
            return webPath;
        }
    }

    private void setCurrentState(ItemState currentState) {
        Log.i("aStateChanged", " currentState:" + currentState.toString());
        this.currentState = currentState;
    }


    @Override
    public void stopPlay() {
        if (currentWindow == null){
            return;
        }
        saveCurrentPlayTime(currentWindow);
        Log.i(TAG, "stop play position:" + currentWindow.getWindowIndex());
        stopPlayer();
        notifyItemChanged(currentWindow.getWindowIndex());
    }

    @Override
    public void pause() {
        if (mVideoPlayerManager != null && currentState == ItemState.PLAY) {
            setCurrentState(ItemState.PAUSE);
            saveCurrentPlayTime(currentWindow);
            mVideoPlayerManager.stopPlay();
        }
    }

    @Override
    public void setPlayableWindow(PlayableWindow window) {
        this.currentWindow = window;
    }

    @Override
    public PlayableWindow getCurrentPlayableWindow() {
        return currentWindow;
    }

    @Override
    public void resume() {
        if (checkStateEquals(currentState, ItemState.NOMAL) || checkStateEquals(currentState, ItemState.PAUSE)) {
            play();
        }
    }

    @Override
    public void onScrollFinished(boolean isUp) {
    }


    private void onVideoChange() {

    }

    private void playVideo(boolean needNotifyItemChanged) {
        if (currentPositionIsIllegal()) {
            return;
        }
        int currentPlayingPosition = currentWindow.getWindowIndex();
        Log.i(TAG, "playing position:" + currentPlayingPosition);
        String url = getPlayAblePath();
        if (url == null) {
            return;
        }
        setCurrentState(ItemState.PLAY);

        if (needNotifyItemChanged) {
            notifyItemChanged(currentPlayingPosition);
        }
        applyCurrentPlayTime(currentWindow);
        mVideoPlayerManager.setPlayableWindow(currentWindow);
        mVideoPlayerManager.setUrl(url);
        mVideoPlayerManager.play();
    }

    private void setExpired() {
        //视频播放时，就将视频置为已看过
        int currentPlayingPosition = currentWindow.getWindowIndex();
    }

    private void stopPlayer() {
        if (mVideoPlayerManager != null) {
            setCurrentState(ItemState.NOMAL);
            mVideoPlayerManager.stopPlay();
        }
    }

    private void saveCurrentPlayTime(PlayableWindow currentPlayingWindow) {
        if (currentPlayingWindow != null && mVideoPlayerManager.isPlaying()) {
            long currentSeek = mVideoPlayerManager.getCurrentSeek();
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

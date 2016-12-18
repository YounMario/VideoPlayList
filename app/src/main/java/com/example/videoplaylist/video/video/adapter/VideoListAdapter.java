package com.example.videoplaylist.video.video.adapter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.extra.VolleyImageView;
import com.cleanmaster.util.CMLog;
import com.cleanmaster.util.DLog;
import com.cleanmaster.util.LockerFileUtils;
import com.cmcm.locker.R;
import com.locker.newscard.video.api.VideoInfo;
import com.locker.newscard.video.api.VideoInfoModel;
import com.locker.newscard.video.holder.VideoItemHolder;
import com.locker.newscard.video.listener.VideoPlayListener;
import com.locker.newscard.video.player.ExoVideoPlayManager;
import com.locker.newscard.video.player.PlayableWindow;
import com.locker.newscard.video.player.VideoPlayManager;
import com.locker.newscard.video.report.locker_news_videopage;

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
    private ItemState currentState;

    private ExoVideoPlayManager mVideoPlayerManager;
    private PlayableWindow currentWindow;
    private final Context mContext;

    private boolean hasFocus = false;
    private Surface mSurface;


    private List<VideoInfo> data;
    private SparseArray<Long> playSeekMap;

    private long mFirstItemDefaultSeek;
    private long mStartEnterTime;
    private long mVideoItemDuring;


    public void onResume() {
        if (checkStateEquals(currentState, ItemState.PAUSE) || checkStateEquals(currentState, ItemState.INITED)) {
            CMLog.i(TAG_ITEM_STATE, "on onResumePlay : " + this);
            playVideo(true);
        }
    }



    public void release() {
        if (mVideoPlayerManager != null) {
            CMLog.i(TAG_ITEM_STATE, "release player:" + this);
            stopPlay();
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

    public void onHide() {
        int mStayTime = (int) ((System.currentTimeMillis() - mStartEnterTime) / 1000);
        locker_news_videopage.reportAction(locker_news_videopage.ACTION_EXIT, "", 0, locker_news_videopage.DEFAULT_BYTE, mStayTime);
    }

    public void onShow() {
        mStartEnterTime = System.currentTimeMillis();
        if (!currentPositionIsIllegal()) {
            VideoInfo videoInfo = getItem(currentWindow.getWindowIndex());
            locker_news_videopage.reportAction(locker_news_videopage.ACTION_ENTER_DETAIL, videoInfo.getVideoId(), (int) mVideoItemDuring, (byte) videoInfo.getCategoryId(), 0);
        } else {
            locker_news_videopage.reportAction(locker_news_videopage.ACTION_ENTER_DETAIL, "", 0, locker_news_videopage.DEFAULT_BYTE, 0);
        }
    }

    public void setFirstItemDuring(long mVideoDuring) {
        this.mVideoItemDuring = mVideoDuring;
    }


    private enum ItemState {
        NOMAL, PLAY, PAUSE, INIT, INITED;
    }

    public VideoListAdapter(Context context) {
        mContext = context;
        mVideoPlayerManager = new ExoVideoPlayManager();
        currentState = ItemState.INIT;

        data = new ArrayList<>();
        mVideoPlayerManager.setPlayListener(new VideoPlayListener() {
            @Override
            public void playFinished(boolean playWhenReady) {
                reportPlayFinished();
                if (currentPositionIsIllegal()) {
                    return;
                }
                if (lastPlayVideoUrlChanged()) {
                    playVideo(false);
                } else {
                    mVideoPlayerManager.seekTo(0);
                }
            }

            @Override
            public void playStarted() {
                reportPlayStarted();
            }

            @Override
            public void playerReady(boolean playWhenReady) {

            }

            @Override
            public void onError(Exception ex) {

            }
        });

        playSeekMap = new SparseArray<>();
    }

    private void reportPlayStarted() {
        if (!currentPositionIsIllegal()) {
            int videoTime = mVideoPlayerManager != null ? (int) mVideoPlayerManager.getDuration()/1000 : 0;
            VideoInfo videoInfo = getItem(currentWindow.getWindowIndex());
            locker_news_videopage.reportAction(locker_news_videopage.ACTION_START_PLAY, videoInfo.getVideoId(), videoTime, (byte) videoInfo.getCategoryId(), 0);
        } else {
            locker_news_videopage.reportAction(locker_news_videopage.ACTION_START_PLAY, "", 0, locker_news_videopage.DEFAULT_BYTE, 0);
        }
    }

    private void reportPlayFinished() {
        if (!currentPositionIsIllegal()) {
            int videoTime = mVideoPlayerManager != null ? (int) mVideoPlayerManager.getDuration()/1000 : 0;
            VideoInfo videoInfo = getItem(currentWindow.getWindowIndex());
            locker_news_videopage.reportAction(locker_news_videopage.ACTION_FINISH_PLAY, videoInfo.getVideoId(), videoTime, (byte) videoInfo.getCategoryId(), 0);
        } else {
            locker_news_videopage.reportAction(locker_news_videopage.ACTION_FINISH_PLAY, "", 0, locker_news_videopage.DEFAULT_BYTE, 0);
        }
    }

    private boolean currentPositionIsIllegal() {
        return currentWindow == null || currentWindow.getWindowIndex() < 0 || currentWindow.getWindowIndex() >= getItemCount();
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

        window.getVideoView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (window.getWindowIndex() == position) {
                    if (checkStateEquals(currentState, ItemState.PLAY)) {
                        setCurrentState(ItemState.PAUSE);
                        window.updateUiToPauseState();
                        mVideoPlayerManager.pause();
                        reportPause();
                    } else if (checkStateEquals(currentState, ItemState.PAUSE)) {
                        setCurrentState(ItemState.PLAY);
                        window.updateUiToResumeState();
                        mVideoPlayerManager.resume();
                        reportResume();
                    }
                }
            }
        });

        final VolleyImageView imageView = (VolleyImageView) window.getView(R.id.cover_image_view);
        imageView.setImageUrl(videoInfo.getThumbnailUrl());

        final TextureView textureView = (TextureView) window.getView(R.id.textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                CMLog.i(TAG_TEXTURE, "is available position:" + position + " view:" + textureView);
                if (window.getWindowIndex() == position) {
                    if (mSurface != null) {
                        mSurface.release();
                    }
                    mSurface = new Surface(surface);

                    mVideoPlayerManager.setSurface(mSurface);
                    if (!checkStateEquals(currentState, ItemState.PLAY) && hasFocus) {
                        CMLog.i(TAG_ITEM_STATE, "first show and play  position:" + position + " object:" + VideoListAdapter.this);
                        playVideo(false);
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (position == window.getWindowIndex()) {
                    CMLog.i(TAG_TEXTURE, "is surface texture disdroyed:" + position + " view:" + textureView);
                    if (mSurface != null) {
                        mSurface.release();
                        mSurface = null;
                    }
                    pause();
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//                CMLog.i(TAG_TEXTURE, "is surface texture update:" + position);
            }
        });


        TextView descText = (TextView) window.getView(R.id.txt_desc);
        String desc = videoInfo.getDesc();
        if (DLog.isDebugMode()) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("(");
            stringBuilder.append("cId_" + videoInfo.getCategoryId());
            stringBuilder.append(" ,vId_" + videoInfo.getVideoId());
            stringBuilder.append("）");
            stringBuilder.append(desc);
            desc = stringBuilder.toString();
        }
        descText.setText(desc);

        if (window.getWindowIndex() != position) {
            window.updateUiToNormalState();
        }

    }

    private void reportResume() {
        locker_news_videopage.reportAction(locker_news_videopage.ACTION_RESUME, "", 0, locker_news_videopage.DEFAULT_BYTE, 0);
    }

    private void reportPause() {
        locker_news_videopage.reportAction(locker_news_videopage.ACTION_PAUSE, "", 0, locker_news_videopage.DEFAULT_BYTE, 0);
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
            localPath = LockerFileUtils.convertUrlToLocalPath(webPath);
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
        this.currentState = currentState;
    }


    @Override
    public void stopPlay() {
        if (currentWindow == null){
            return;
        }
        saveCurrentPlayTime(currentWindow);
        CMLog.i(TAG, "stop play position:" + currentWindow.getWindowIndex());
        notifyItemChanged(currentWindow.getWindowIndex());
        stopPlayer();
    }

    @Override
    public void pause() {
        if (mVideoPlayerManager != null) {
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
        locker_news_videopage.reportAction(locker_news_videopage.ACTION_SCROLL_UP_MANUAL, "", 0, locker_news_videopage.DEFAULT_BYTE, 0);
    }

    private void playVideo(boolean needNotifyItemChanged) {
        if (currentPositionIsIllegal()) {
            return;
        }
        int currentPlayingPosition = currentWindow.getWindowIndex();
        CMLog.i(TAG, "playing position:" + currentPlayingPosition);
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

        //视频播放时，就将视频置为已看过
        final VideoInfo videoInfo = getItem(currentPlayingPosition);
        VideoInfoModel.get().setVideoExpired(videoInfo);
    }

    private void stopPlayer() {
        if (mVideoPlayerManager != null) {
            setCurrentState(ItemState.NOMAL);
            currentWindow = null;
            mVideoPlayerManager.stopPlay();
        }
    }

    public void saveCurrentPlayTime(PlayableWindow currentPlayingWindow) {
        if (currentPlayingWindow != null && mVideoPlayerManager.isPlaying()) {
            long currentSeek = mVideoPlayerManager.getCurrentSeek();
            CMLog.i(TAG_SAVE_CURRENT, " save current play time position :" + currentPlayingWindow.getWindowIndex() + "  current seek:" + currentSeek);
            playSeekMap.put(currentPlayingWindow.getWindowIndex(), currentSeek);
        }
    }

    public void applyCurrentPlayTime(PlayableWindow playableWindow) {
        CMLog.i(TAG_SAVE_CURRENT, " apply current play time");
        int position = playableWindow.getWindowIndex();
        if (playableWindow != null) {
            Long currentSeek = playSeekMap.get(position);
            if (currentSeek != null) {
                playableWindow.setCurrentSeek(currentSeek);
            } else {
                playableWindow.setCurrentSeek(0);
            }
            CMLog.i(TAG_SAVE_CURRENT, " apply current playTime position:" + position + " current seek:" + currentSeek);
        }

    }

}

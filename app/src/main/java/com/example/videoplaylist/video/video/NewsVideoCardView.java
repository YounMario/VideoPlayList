package com.example.videoplaylist.video.video;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.extra.VolleyImageView;
import com.cleanmaster.popwindow.PopWindowListener;
import com.cleanmaster.ui.cover.CoverConstValue;
import com.cleanmaster.ui.cover.GlobalEvent;
import com.cleanmaster.util.CMLog;
import com.cleanmaster.util.MemoryUtils;
import com.cmcm.locker.R;
import com.locker.newscard.ItemScrollListener;
import com.locker.newscard.bean.LockNewsDao;
import com.locker.newscard.ui.NewsLockItemView;
import com.locker.newscard.video.api.VideoInfo;
import com.locker.newscard.video.api.VideoInfoModel;
import com.locker.newscard.video.api.VideoPreLoaderService;
import com.locker.newscard.video.listener.VideoPlayListener;
import com.locker.newscard.video.player.ExoVideoPlayManager;
import com.locker.newscard.video.player.PlayableWindow;

import java.lang.ref.WeakReference;

/**
 * Created by zhangbo on 2016/12/14.
 */
public class NewsVideoCardView extends CardView implements ItemScrollListener {
    private static final int WHAT_UPDATE_REMAIN_TIME = 1;
    private static final int WHAT_RECOVER_CLICK = 2;

    private Surface mSurface;
    private TextView mDescribeView;
    private TextureView mTextureView;
    private VolleyImageView mImageView;

    private TextView mTotalTime;
    private ImageView mPlayStatueImage;
    private AnimationDrawable playStateDrawable;

    private PlayableWindow playableWindow;
    private ExoVideoPlayManager mPlayManager;
    private VideoPlayListener mVideoPlayListener = new VideoCardPlayListener(this);

    private boolean mIsEnter;
    private boolean mIsPlayManagerInit = false;
    private VideoInfo mVideoInfo;
    private VideoCardHandler mVideoHandler;
    private boolean mForibidClick = false;

    public NewsVideoCardView(Context context) {
        super(context);
        initView(context);
    }

    public NewsVideoCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public NewsVideoCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.layout_news_video_card_view, this);
        mTextureView = (TextureView) findViewById(R.id.video_card_player_view);
        mImageView = (VolleyImageView) findViewById(R.id.video_card_image_view);
        mDescribeView = (TextView) findViewById(R.id.video_card_describe);
        mPlayStatueImage = (ImageView) findViewById(R.id.video_card_voice_img);
        mTotalTime = (TextView) findViewById(R.id.video_card_total_time);

        mPlayStatueImage.setBackgroundResource(R.drawable.voice_frame_animation);
        playStateDrawable = (AnimationDrawable) mPlayStatueImage.getBackground();

        mVideoHandler = new VideoCardHandler(this);

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mForibidClick) {
                    return;
                }
                mForibidClick = true;
                mVideoHandler.sendEmptyMessageDelayed(WHAT_RECOVER_CLICK, 500);

                if (mPlayManager != null && mVideoInfo != null) {
                    VideoDetailView.start(mVideoInfo, mPlayManager.getCurrentSeek(),mPlayManager.getDuration(), new PopWindowListener() {
                        @Override
                        public void onCreate() {
                        }

                        @Override
                        public void onDestroy() {
                            if (mIsEnter && mPlayManager != null) {
                                mPlayManager.resume();
                            }
                        }

                        @Override
                        public void onShow() {
                            pausePlay();
                        }

                        @Override
                        public void onHide() {
                            if (mIsEnter && mPlayManager != null) {
                                mPlayManager.resume();
                            }
                        }
                    });
                }
            }
        });

        mPlayManager = new ExoVideoPlayManager();

        playableWindow = new PlayableWindow() {
            @Override
            public boolean canPlay() {
                return true;
            }

            @Override
            public View getPlayerView() {
                return NewsVideoCardView.this;
            }

            @Override
            public View getVideoView() {
                return mTextureView;
            }

            @Override
            public Surface getPlayableSurface() {
                if (mTextureView != null && mTextureView.getSurfaceTexture() != null) {
                    if (mSurface != null) {
                        mSurface.release();
                    }
                    mSurface = new Surface(mTextureView.getSurfaceTexture());
                    return mSurface;
                }
                return null;
            }


            @Override
            public void updateBufferProgress(int progress) {
            }

            @Override
            public void updateUiToPlayState() {
                mImageView.setVisibility(View.INVISIBLE);
                if (playStateDrawable == null) {
                    mPlayStatueImage.setBackgroundResource(R.drawable.voice_frame_animation);
                    playStateDrawable = (AnimationDrawable) mPlayStatueImage.getBackground();
                }
                playStateDrawable.start();
                mVideoHandler.sendEmptyMessage(WHAT_UPDATE_REMAIN_TIME);
            }

            @Override
            public void updateUiToPauseState() {
                if (playStateDrawable != null) {
                    playStateDrawable.stop();
                }
                mVideoHandler.removeMessages(WHAT_UPDATE_REMAIN_TIME);
            }

            @Override
            public void updateUiToResumeState() {
                if (playStateDrawable != null) {
                    playStateDrawable.start();
                }
                mVideoHandler.sendEmptyMessage(WHAT_UPDATE_REMAIN_TIME);
            }

            @Override
            public void updateUiToNormalState() {
                if (playStateDrawable != null) {
                    playStateDrawable.stop();
                }
                mVideoHandler.removeMessages(WHAT_UPDATE_REMAIN_TIME);
            }

            @Override
            public void showBufferBar() {
            }

            @Override
            public void showPlayBar() {
            }

            @Override
            public void hideBufferBar() {
            }

            @Override
            public void hidePlayBuffer() {
            }

            @Override
            public void showCover() {
            }

            @Override
            public void hideCover() {
            }

            @Override
            public void setClipType(int type) {
            }

            @Override
            public int getClipType() {
                return 0;
            }

            @Override
            public float getWindowLastCalculateArea() {
                return 0;
            }

            @Override
            public void setCurrentSeek(long playTime) {

            }

            @Override
            public long getCurrentSeek() {
                return 0;
            }


            @Override
            public void setWindowIndex(int index) {

            }

            @Override
            public int getWindowIndex() {
                return 0;
            }
        };
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                if (mIsEnter) {
                    if (mSurface != null) {
                        mSurface.release();
                    }
                    mSurface = new Surface(surface);
                    mPlayManager.setSurface(mSurface);
                    startPlay();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (mSurface != null) {
                    mSurface.release();
                    mSurface = null;
                }
                mIsPlayManagerInit = false;
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }


    public void loadData(final NewsLockItemView.OnRemoveItemListener listener, final LockNewsDao dao) {
        CMLog.i("test", "loadData");
        VideoInfoModel.get().getOneCachedNotPlayVideoAsync(new VideoInfoModel.VideoLoadListener() {
            @Override
            public void onSuccess(VideoInfo videoInfo) {
                mVideoInfo = videoInfo;
                mImageView.setVisibility(View.VISIBLE);
                mImageView.setImageUrl(mVideoInfo.getThumbnailUrl());
                String describe = mVideoInfo.getDesc();
                if (!TextUtils.isEmpty(describe)) {
                    mDescribeView.setText(describe);
                }
                startPlay();
            }

            @Override
            public void onFailed() {
                mVideoInfo = null;
                listener.onRemove(dao);
            }
        });
    }

    private void initPlayer() {
        mPlayManager.setPlayableWindow(playableWindow);
        mPlayManager.setUrl(mVideoInfo.getVideoLocalPath());
        mPlayManager.setPlayListener(mVideoPlayListener);
        mIsPlayManagerInit = true;
    }

    private void releasePlayer() {
        if (mPlayManager != null) {
            mPlayManager.release();
            mIsPlayManagerInit = false;
        }
        if (playStateDrawable != null) {
            mPlayStatueImage.setImageResource(0);
            playStateDrawable.stop();
            playStateDrawable = null;
        }
    }

    private void startPlay() {
        if (mIsEnter && mVideoInfo != null && mPlayManager != null && playableWindow != null) {
            if (mIsPlayManagerInit) {
                mPlayManager.resume();
            } else {
                mPlayManager.stopPlay();
                initPlayer();
                mPlayManager.play(true);
            }
        }
    }

    private void pausePlay() {
        if (mPlayManager != null) {
            mPlayManager.pause();
        }
    }

    @Override
    public void enter() {
        mIsEnter = true;
        startPlay();
        VideoInfoModel.get().setVideoExpired(mVideoInfo);
        VideoPreLoaderService.sendStartPreload();
        setScreenOn(true);
    }

    @Override
    public void quit() {
        mIsEnter = false;
        pausePlay();
        setScreenOn(false);
    }

    private static void setScreenOn(boolean needKeep) {
        Message message = Message.obtain();
        message.what = CoverConstValue.WHAT_UPDATE_SCREEN_ON_SETTING;
        Bundle data = new Bundle();
        data.putBoolean(CoverConstValue.BUNDLE_KEY_KEEP_SCREEN_ON, needKeep);
        message.setData(data);
        GlobalEvent.get().sendMessage(message);
    }

    public void onCoverRemove() {
        releasePlayer();
        MemoryUtils.recycleView(mImageView);
    }

    public void onCoverStopShow() {
        pausePlay();
    }

    public void onCoverStartShow() {
        startPlay();
    }

    private String formatTime(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        int minutes = (int) totalSeconds / 60;
        int seconds = (int) totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    //---------------unUsed method end -----
    private static class VideoCardPlayListener implements VideoPlayListener {
        private final WeakReference<NewsVideoCardView> videoCardViewWeakReference;

        public VideoCardPlayListener(NewsVideoCardView newsVideoCardView) {
            videoCardViewWeakReference = new WeakReference<>(newsVideoCardView);
        }

        @Override
        public void playFinished(boolean playWhenReady) {
            NewsVideoCardView videoCardView = videoCardViewWeakReference.get();
            if (videoCardView != null && videoCardView.mIsEnter) {
                videoCardView.mPlayManager.seekTo(0);
                videoCardView.startPlay();
            }

            if (videoCardView != null) {
                setScreenOn(false);
            }
        }

        @Override
        public void playStarted() {
            CMLog.i("test", "playStarted");
        }

        @Override
        public void playerReady(boolean playWhenReady) {
            CMLog.i("test", "playerReady :" + playWhenReady);
        }

        @Override
        public void onError(Exception ex) {

        }
    }

    private static class VideoCardHandler extends Handler {
        public static final int REMAIN_TIME_UPDATE_INTERVAL = 1000;
        private final WeakReference<NewsVideoCardView> mVideoCardView;

        public VideoCardHandler(NewsVideoCardView videoCardView) {
            mVideoCardView = new WeakReference<>(videoCardView);
        }

        @Override
        public void handleMessage(Message msg) {
            NewsVideoCardView videoCardView = mVideoCardView.get();
            if (videoCardView == null) {
                return;
            }

            switch (msg.what) {
                case WHAT_UPDATE_REMAIN_TIME:
                    if (videoCardView.mPlayManager != null && videoCardView.mPlayManager.isPlaying()) {
                        long duration = videoCardView.mPlayManager.getDuration();
                        long currentPosition = videoCardView.mPlayManager.getCurrentSeek();
                        if (duration >= currentPosition) {
                            if (videoCardView.mTotalTime.getVisibility() != View.VISIBLE) {
                                videoCardView.mTotalTime.setVisibility(View.VISIBLE);
                            }
                            videoCardView.mTotalTime.setText(videoCardView.formatTime(duration - currentPosition));
                        }

                        sendEmptyMessageDelayed(WHAT_UPDATE_REMAIN_TIME, REMAIN_TIME_UPDATE_INTERVAL);
                    }
                    break;
                case WHAT_RECOVER_CLICK:
                    videoCardView.mForibidClick = false;
                    break;
            }
        }
    }

}

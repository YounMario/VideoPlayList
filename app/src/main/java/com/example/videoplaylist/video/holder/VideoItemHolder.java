package com.example.videoplaylist.video.holder;

import android.animation.ObjectAnimator;
import android.graphics.SurfaceTexture;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.videoplaylist.App;
import com.example.videoplaylist.R;
import com.example.videoplaylist.video.anim.AnimationUtils;
import com.example.videoplaylist.video.bean.VideoInfo;
import com.example.videoplaylist.video.player.ExoVideoPlayManager;
import com.example.videoplaylist.video.player.PlayableWindow;
import com.example.videoplaylist.video.player.manager.VideoPlayManager;
import com.example.videoplaylist.video.utils.ThreadUtils;
import com.example.videoplaylist.video.utils.ViewUitls;
import com.example.videoplaylist.video.widget.VideoPlayerBottomBar;
import com.squareup.picasso.Picasso;


/**
 * Created by 龙泉 on 2016/12/15.
 */

public class VideoItemHolder extends RecyclerView.ViewHolder implements PlayableWindow<VideoInfo> {

    private static final String TAG = "VideoItemHolder";

    private View mItemView;
    private TextureView textureView;
    private final float DEFAULT_OFFSET = 0.90f;
    private Surface mSurface;

    private ImageView btnPlay;
    private ImageView ivLoading;
    private ImageView coverImageView;
    private VideoPlayerBottomBar videoPlayerBottomBar;
    private FrameLayout frameCover;
    private TextView mTxtDescription;

    private ImageView btnFullScreen;

    private float mLastVisibleArea;
    private int mCurrentIndex;
    private long mCurrentPlaySeek;

    private ObjectAnimator roatingAnimation;
    private ObjectAnimator videoBottomBarAnimation;
    private ObjectAnimator playButtonAnimation;
    private Runnable mDelayHideRunnable;

    private ExoVideoPlayManager mExoPlayer;
    private VideoInfo mVideoItem;
    private boolean mPlayActive;
    private VideoPlayManager mVideoPlayManager;

    public VideoItemHolder(View itemView) {
        super(itemView);
        mItemView = itemView;
        initView();
    }

    private void initView() {
        textureView = (TextureView) itemView.findViewById(R.id.textureView);
        btnPlay = (ImageView) itemView.findViewById(R.id.btn_pause);
        coverImageView = (ImageView) itemView.findViewById(R.id.cover_image_view);
        videoPlayerBottomBar = (VideoPlayerBottomBar) itemView.findViewById(R.id.video_play_bottom_bar);
        frameCover = (FrameLayout) itemView.findViewById(R.id.frame_cover);
        ivLoading = (ImageView) itemView.findViewById(R.id.img_buffering);
        mTxtDescription = (TextView) itemView.findViewById(R.id.txt_desc);
        btnFullScreen = (ImageView) itemView.findViewById(R.id.btn_full);
        mExoPlayer = new ExoVideoPlayManager();
        mExoPlayer.setPlayableWindow(this);

        btnFullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                if (mVideoPlayManager.getCurrentPlayableWindow() == VideoItemHolder.this) {
                    if (mSurface != null) {
                        mSurface.release();
                    }
                    mSurface = new Surface(surface);
                    VideoItemHolder.this.setSurface(mSurface);
                    //只是为了第一次播放？
                    if (mVideoPlayManager.getCurrentPlayableWindow().playActive()) {
                        mVideoPlayManager.play();
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (surface != null) {
                    surface.release();
                }
                PlayableWindow currentWindow = mVideoPlayManager.getCurrentPlayableWindow();
                if (currentWindow == VideoItemHolder.this) {
                    Log.i(TAG, "is surface texture disdroyed:" + currentWindow.getWindowIndex() + " view:" + textureView);
                    if (mSurface != null) {
                        mSurface.release();
                        mSurface = null;
                    }
                    Log.i("locker_news_videoplay", "on pause.......");
                    if (currentWindow.isPlaying()) {
                        mVideoPlayManager.stopPlay();
                    }
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//                Log.i(TAG_TEXTURE, "is surface texture update:" + position);
            }
        });


        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoPlayManager.getCurrentPlayableWindow() == VideoItemHolder.this) {
                    if (mVideoPlayManager.isPlaying()) {
                        mVideoPlayManager.pause();
                        btnPlay.setImageResource(R.drawable.icon_play);
                    } else if (mVideoPlayManager.isPlaying()) {
                        mVideoPlayManager.resume();
                        btnPlay.setImageResource(R.drawable.icon_pause);
                    }
                }
            }
        });

        getVideoView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoPlayManager.getCurrentPlayableWindow() == VideoItemHolder.this) {
                    onFocus();
                }
            }
        });
    }

    public View getView(int resId) {
        return itemView.findViewById(resId);
    }

    @Override
    public boolean canPlay() {
        mLastVisibleArea = ViewUitls.visibleArea(this, mItemView.getParent());
        return mLastVisibleArea > DEFAULT_OFFSET;
    }

    @Override
    public View getPlayerView() {
        return itemView;
    }

    @Override
    public View getVideoView() {
        return textureView;
    }


    @Override
    public Surface getPlayableSurface() {
        TextureView textureView = (TextureView) getView(R.id.textureView);
        if (textureView != null && textureView.getSurfaceTexture() != null) {
            if (mSurface != null) {
                mSurface.release();
            }
            mSurface = new Surface(textureView.getSurfaceTexture());
            return mSurface;
        }
        return null;
    }


    @Override
    public void updateUiToPlayState() {
        btnPlay.setVisibility(View.GONE);
        videoPlayerBottomBar.setVisibility(View.VISIBLE);
        if (roatingAnimation != null) {
            ivLoading.setVisibility(View.VISIBLE);
        }
        delayHideControlBar();
    }


    @Override
    public void updateUiToPauseState() {
        cancelDelayHideControlBar();
        btnPlay.setVisibility(View.VISIBLE);
        if (ivLoading.getVisibility() == View.VISIBLE) {
            ivLoading.setVisibility(View.INVISIBLE);
        }
        videoPlayerBottomBar.setVisibility(View.VISIBLE);
    }


    @Override
    public void updateUiToNormalState() {
        frameCover.setVisibility(View.VISIBLE);
        coverImageView.setVisibility(View.VISIBLE);
        btnPlay.setVisibility(View.GONE);
        videoPlayerBottomBar.setVisibility(View.GONE);
        stopLoading();
        cancelDelayHideControlBar();
    }

    @Override
    public void updateUiToFocusState() {
        if (ivLoading.getVisibility() == View.VISIBLE) {
            return;
        }
        if (videoPlayerBottomBar.getVisibility() != View.VISIBLE) {
            showPlayButtonWithAnimation();
            showBottomBarWithAnimation();
            delayHideControlBar();
        } else {
            hidePlayButtonWithAnimation();
            hideBottomBarWithAnimation();
        }
    }

    private void hidePlayButtonWithAnimation() {
        if (playButtonAnimation != null) {
            playButtonAnimation.cancel();
            playButtonAnimation = null;
        }
        playButtonAnimation = AnimationUtils.createHideGradientAnimation(btnPlay);
        playButtonAnimation.start();
    }

    private void showPlayButtonWithAnimation() {
        if (playButtonAnimation != null) {
            playButtonAnimation.cancel();
            playButtonAnimation = null;
        }
        btnPlay.setVisibility(View.VISIBLE);
        playButtonAnimation = AnimationUtils.createShowGradientAnimation(btnPlay);
        playButtonAnimation.start();
    }

    @Override
    public void updateUiToPrepare() {
        frameCover.setVisibility(View.GONE);
    }

    @Override
    public void showLoading() {
        if (btnPlay.getVisibility() == View.VISIBLE) {
            btnPlay.setVisibility(View.GONE);
        }
        startLoading();
    }

    @Override
    public void hideLoading() {
        stopLoading();
    }

    @Override
    public void showCover() {
        coverImageView.setVisibility(View.VISIBLE);

    }

    @Override
    public void hideCover() {
        coverImageView.setVisibility(View.GONE);
    }


    @Override
    public float getWindowLastCalculateArea() {
        return mLastVisibleArea;
    }

    @Override
    public void setCurrentSeek(long seek) {
        mCurrentPlaySeek = seek;
    }

    @Override
    public long getCurrentSeek() {
        return mExoPlayer.getCurrentSeek();
    }


    @Override
    public void setWindowIndex(int index) {
        mCurrentIndex = index;
    }

    @Override
    public int getWindowIndex() {
        return mCurrentIndex;
    }

    @Override
    public void onRelease() {
        cancelDelayHideControlBar();
        stopRoationAnimation();
        stopBottomBarAnimation();
        stopPlayButtonAnimation();
        releasePlayer();
    }

    private void releasePlayer() {
        if (mExoPlayer != null) {
            mExoPlayer.release();
        }
    }

    @Override
    public void stopPlay() {
        mExoPlayer.stopPlay();
    }

    @Override
    public boolean isPlaying() {
        return mExoPlayer.isPlaying();
    }

    @Override
    public void setUrl(String url) {
        mExoPlayer.setUrl(url);
    }

    @Override
    public void play() {
        mExoPlayer.play();
    }

    @Override
    public void pause() {
        mExoPlayer.pause();
    }

    @Override
    public void resume() {
        mExoPlayer.resume();
    }

    @Override
    public void onFocus() {
        mExoPlayer.onFocus();
    }

    @Override
    public void setSurface(Surface mSurface) {
        mExoPlayer.setSurface(mSurface);
    }

    @Override
    public void setVideoPlayManager(VideoPlayManager videoPlayManager) {
        this.mVideoPlayManager = videoPlayManager;
    }

    @Override
    public VideoInfo getVideoItem() {
        return mVideoItem;
    }

    @Override
    public void updateVideoItem(VideoInfo videoItem) {
        this.mVideoItem = videoItem;
        if (mVideoPlayManager.getCurrentPlayableWindow() != this) {
            btnPlay.setImageResource(R.drawable.icon_pause);
            updateUiToNormalState();
        }
        Picasso.with(App.getInstance())
                .load(videoItem.getThumbnailUrl())
                .fit()
                .into(coverImageView);

        mTxtDescription.setText(videoItem.getDesc());
    }

    @Override
    public boolean playActive() {
        return mPlayActive;
    }

    @Override
    public void setPlayActive(boolean playActive) {
        this.mPlayActive = playActive;
    }

    private void stopPlayButtonAnimation() {
        if (playButtonAnimation != null) {
            playButtonAnimation.cancel();
            playButtonAnimation = null;
        }
    }

    private void stopBottomBarAnimation() {
        if (videoBottomBarAnimation != null) {
            videoBottomBarAnimation.cancel();
            videoBottomBarAnimation = null;
        }
    }

    private void startLoading() {
        if (ivLoading != null) {
            ivLoading.setVisibility(View.VISIBLE);
            startRoationAnimation(ivLoading);
        }
    }

    private void stopLoading() {
        if (ivLoading != null) {
            ivLoading.setVisibility(View.GONE);
            stopRoationAnimation();
        }
    }


    private void startRoationAnimation(View imageView) {
        if (roatingAnimation != null) {
            roatingAnimation.cancel();
            roatingAnimation = null;
        }
        roatingAnimation = AnimationUtils.createRotationAnimation(imageView);
        roatingAnimation.start();
    }

    private void stopRoationAnimation() {
        if (roatingAnimation != null) {
            roatingAnimation.cancel();
            roatingAnimation = null;
        }
    }

    private void delayHideControlBar() {
        cancelDelayHideControlBar();
        mDelayHideRunnable = new Runnable() {
            @Override
            public void run() {
                hidePlayButtonWithAnimation();
                hideBottomBarWithAnimation();
            }
        };
        ThreadUtils.postOnUiThreadDelayed(mDelayHideRunnable, 5000);
    }

    private void cancelDelayHideControlBar() {
        if (mDelayHideRunnable != null) {
            ThreadUtils.removeUiRunnable(mDelayHideRunnable);
        }
    }

    private void hideBottomBarWithAnimation() {
        if (videoBottomBarAnimation != null) {
            videoBottomBarAnimation.cancel();
            videoBottomBarAnimation = null;
        }
        videoBottomBarAnimation = AnimationUtils.createHideGradientAnimation(videoPlayerBottomBar);
        videoBottomBarAnimation.start();
    }

    private void showBottomBarWithAnimation() {
        if (videoBottomBarAnimation != null) {
            videoBottomBarAnimation.cancel();
            videoBottomBarAnimation = null;
        }
        videoPlayerBottomBar.setVisibility(View.VISIBLE);
        videoBottomBarAnimation = AnimationUtils.createShowGradientAnimation(videoPlayerBottomBar);
        videoBottomBarAnimation.start();
    }


}

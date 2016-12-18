package com.example.videoplaylist.video.video.holder;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.volley.extra.VolleyImageView;
import com.cmcm.locker.R;
import com.locker.newscard.video.player.PlayableWindow;
import com.locker.newscard.video.utils.ViewUitls;
import com.locker.newscard.video.widget.VideoPlayerBottomBar;

/**
 * Created by 龙泉 on 2016/12/15.
 */

public class VideoItemHolder extends RecyclerView.ViewHolder implements PlayableWindow {

    private View mItemView;
    private TextureView textureView;
    private final float DEFAULT_OFFSET = 0.90f;
    private Surface mSurface;

    private ImageView btnPause;
    private ImageView ivLoading;
    private VolleyImageView coverImageView;
    private VideoPlayerBottomBar videoPlayerBottomBar;
    private FrameLayout frameCover;


    private float mLastVisibleArea;
    private int mCurrentIndex;
    private long mCurrentPlaySeek;

    private ObjectAnimator roatingAnimation;




    public VideoItemHolder(View itemView) {
        super(itemView);
        mItemView = itemView;
        initView();
    }

    private void initView() {
        textureView = (TextureView) itemView.findViewById(R.id.textureView);
        btnPause  = (ImageView) itemView.findViewById(R.id.btn_pause);
        coverImageView = (VolleyImageView) itemView.findViewById(R.id.cover_image_view);
        videoPlayerBottomBar = (VideoPlayerBottomBar) itemView.findViewById(R.id.video_play_bottom_bar);
        frameCover = (FrameLayout) itemView.findViewById(R.id.frame_cover);
        ivLoading = (ImageView) itemView.findViewById(R.id.img_buffering);
    }

    public View getView(int resId) {
        return itemView.findViewById(resId);
    }

    @Override
    public boolean canPlay() {
        mLastVisibleArea = ViewUitls.visibleArea(this, mItemView.getParent());
        return  mLastVisibleArea > DEFAULT_OFFSET;
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
    public void updateBufferProgress(int progress) {

    }

    @Override
    public void updateUiToPlayState() {
        btnPause.setVisibility(View.GONE);
        videoPlayerBottomBar.setVisibility(View.VISIBLE);
        frameCover.setVisibility(View.GONE);
    }


    @Override
    public void updateUiToPauseState() {
        btnPause.setVisibility(View.VISIBLE);
    }

    @Override
    public void updateUiToResumeState() {
        btnPause.setVisibility(View.GONE);
    }

    @Override
    public void updateUiToNormalState() {
        frameCover.setVisibility(View.VISIBLE);
        coverImageView.setVisibility(View.VISIBLE);
        stopLoading();
        btnPause.setVisibility(View.GONE);
        videoPlayerBottomBar.setVisibility(View.GONE);
    }

    @Override
    public void showBufferBar() {
        //startLoading();
    }

    @Override
    public void showPlayBar() {

    }

    @Override
    public void hideBufferBar() {
        //stopLoading();
    }

    @Override
    public void hidePlayBuffer() {

    }

    @Override
    public void showCover() {
        coverImageView.setVisibility(View.VISIBLE);
        startLoading();
    }

    @Override
    public void hideCover() {
        coverImageView.setVisibility(View.GONE);
        stopLoading();
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
        return mLastVisibleArea;
    }

    @Override
    public void setCurrentSeek(long seek) {
        mCurrentPlaySeek = seek;
    }

    @Override
    public long getCurrentSeek() {
        return mCurrentPlaySeek;
    }


    @Override
    public void setWindowIndex(int index) {
        mCurrentIndex = index;
    }

    @Override
    public int getWindowIndex() {
        return mCurrentIndex;
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
        roatingAnimation = ObjectAnimator.ofFloat(imageView, View.ROTATION, 0, 360);
        roatingAnimation.setDuration(1000);
        roatingAnimation.setInterpolator(new LinearInterpolator());
        roatingAnimation.setRepeatCount(ValueAnimator.INFINITE);
        roatingAnimation.setRepeatMode(ValueAnimator.RESTART);
        roatingAnimation.start();
    }

    private void stopRoationAnimation() {
        if (roatingAnimation != null) {
            roatingAnimation.cancel();
            roatingAnimation = null;
        }
    }
}

package com.example.videoplaylist.video.video.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.player.DemoPlayer;
import com.cmcm.locker.R;

import java.lang.ref.WeakReference;

/**
 * Created by 龙泉 on 2016/12/16.
 */

public class VideoPlayerBottomBar extends RelativeLayout {

    private VideoProgressBar mVideoProgressBar;
    private TextView mStartTime;
    private TextView mEndTime;

    private WeakReference<DemoPlayer> mPlayerReference;

    public VideoPlayerBottomBar(Context context) {
        this(context, null);
    }

    public VideoPlayerBottomBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayerBottomBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mStartTime = (TextView) findViewById(R.id.txt_play_time);
        mEndTime = (TextView) findViewById(R.id.txt_end_time);
        mVideoProgressBar = (VideoProgressBar) findViewById(R.id.progress_bar);
        init();
    }

    public void setupPlayer(DemoPlayer player) {
        mPlayerReference = new WeakReference<>(player);
        mVideoProgressBar.setController(mPlayerReference);
    }

    public void release() {
        if (mPlayerReference != null) {
            mPlayerReference.clear();
            mPlayerReference = null;
        }
    }


    private void init() {
        mVideoProgressBar.setProgressListener(new VideoProgressBar.ProgressListener() {
            @Override
            public void updateProgress(int progress) {
                updateTextView();
            }
        });
        mEndTime.setText(timeToString(0));
        mStartTime.setText(timeToString(0));
    }


    private void updateTextView() {
        if (mPlayerReference != null && mPlayerReference.get() != null) {
            long duringTime = mPlayerReference.get().getDuration();
            long currentPos = mPlayerReference.get().getCurrentPosition();
            mEndTime.setText(timeToString(duringTime));
            mStartTime.setText(timeToString(currentPos));
        }
    }

    private String timeToString(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        int minutes = (int) totalSeconds / 60;
        int seconds = (int) totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}

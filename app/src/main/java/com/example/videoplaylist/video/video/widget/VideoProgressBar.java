package com.example.videoplaylist.video.video.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import com.android.player.DemoPlayer;
import com.cleanmaster.ui.cover.GlobalEvent;
import com.cmcm.locker.R;

import java.lang.ref.WeakReference;

/**
 * Created by 龙泉 on 2016/12/15.
 */
public class VideoProgressBar extends View {


    private Paint mPaint;
    private Paint mBackGroundPaint;
    private WeakReference<DemoPlayer> weakReference;


    private ProgressListener mProgressListener;
    private final String colorBlue = "#3488EB";

    public VideoProgressBar(Context context) {
        this(context, null);
    }

    public VideoProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackGroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mPaint.setColor(Color.parseColor(colorBlue));
        mBackGroundPaint.setColor(ContextCompat.getColor(context, R.color.light_gray_hint));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int currentProgress = getProgress();
        final float width = getWidth() * currentProgress / 100.f;
        canvas.drawRect(0, 0, getWidth(), getHeight(), mBackGroundPaint);
        canvas.drawRect(0, 0, width, getHeight(), mPaint);


        mProgressListener.updateProgress(currentProgress);

        if (GlobalEvent.get().isShowing() && currentProgress <= 100) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private int getProgress() {
        if (weakReference == null || weakReference.get() == null) return 0;
        long duration = weakReference.get().getDuration();
        if (duration == 0) return 0;
        return (int) (100.f * weakReference.get().getCurrentPosition() / duration);
    }

    public void setController(WeakReference<DemoPlayer> playerControlReference) {
        weakReference = playerControlReference;
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void releseController() {
        if (weakReference != null && weakReference.get() != null) {
            weakReference.clear();
        }
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.mProgressListener = progressListener;
    }

    public interface ProgressListener {
        void updateProgress(int progress);
    }
}

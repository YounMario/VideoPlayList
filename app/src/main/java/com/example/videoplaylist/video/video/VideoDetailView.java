package com.example.videoplaylist.video.video;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.cleanmaster.popwindow.PopWindow;
import com.cleanmaster.popwindow.PopWindowLauncher;
import com.cleanmaster.popwindow.PopWindowListener;
import com.cleanmaster.util.CMLog;
import com.cleanmaster.util.DLog;
import com.cleanmaster.util.DimenUtils;
import com.cleanmaster.util.KLockerConfigMgr;
import com.cmcm.locker.R;
import com.locker.newscard.video.adapter.VideoListAdapter;
import com.locker.newscard.video.api.VideoInfo;
import com.locker.newscard.video.api.VideoInfoModel;
import com.locker.newscard.video.api.VideoPreLoaderService;
import com.locker.newscard.video.listener.PlayWindowScrollerListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 龙泉 on 2016/12/15.
 */

public class VideoDetailView extends PopWindow{
    private static String KEY_VIDEO_INFO = "key_video_info";
    private static String KEY_VIDEO_CURRENT_POSITION = "key_video_current_position";
    private static String KEY_VIDEO_TIME = "key_video_time";
    private RecyclerView mRecyclerView;
    private VideoListAdapter mAdapter;

    private FrameLayout mBtnBack;
    private VideoInfo videoInfo;
    private long mFirstItemSeek;

    private static final String TAG = "VideoDetailView";
    private long mVideoDuring = 0;

    @Override
    protected void onCreate() {
        initData();
        setContentView(R.layout.layout_video_detail);
        KLockerConfigMgr.getInstance().setCoverVideoListPageShow(true);
        VideoPreLoaderService.sendStopPreload();
    }

    public static void start(VideoInfo videoInfo, long videoPosition, long videoDuring, PopWindowListener listener) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_VIDEO_INFO, videoInfo);
        bundle.putLong(KEY_VIDEO_CURRENT_POSITION, videoPosition);
        bundle.putLong(KEY_VIDEO_TIME, videoDuring);
        PopWindowLauncher.getInstance().startPopWindow(VideoDetailView.class, true, bundle, null, listener);
    }

    private void initData() {
        Bundle params = getParams();
        if (params != null) {
            Object value = params.get(KEY_VIDEO_INFO);
            if (value != null) {
                videoInfo = (VideoInfo) value;
            }
            mFirstItemSeek = params.getLong(KEY_VIDEO_CURRENT_POSITION, 0);
            mVideoDuring = params.getLong(KEY_VIDEO_TIME, 0);
            mVideoDuring /= 1000;
        }
    }

    @Override
    protected void initView() {
        super.initView();
        mRecyclerView = (RecyclerView) mView.findViewById(R.id.recycleView);
        mBtnBack = (FrameLayout) mView.findViewById(R.id.btn_back);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new VideoListAdapter(getContext());
        CMLog.i(TAG, " my first item default seek");
        mAdapter.setFirstItemSeek(mFirstItemSeek);
        mAdapter.setFirstItemDuring(mVideoDuring);
        mRecyclerView.addOnScrollListener(new PlayWindowScrollerListener(mAdapter));
        ((SimpleItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        if (videoInfo == null) {
            DLog.toFile(TAG, "ERROR ! videoInfo is Null!");
            hideView();
            return;
        }
        final List<String> notVideoIds = new ArrayList<>();
        notVideoIds.add(videoInfo.getVideoId());
        List<VideoInfo> videos = VideoInfoModel.get().getNotPlayVideosByCategoryId(videoInfo.getCategoryId(), notVideoIds);
        CMLog.i(TAG, " my url :" + videoInfo.getVideoUrl() + "  my local path:" + videoInfo.getVideoLocalPath());
        videos.add(0, videoInfo);
        mAdapter.setData(videos);
        mRecyclerView.setAdapter(mAdapter);
        initTitleBar();
        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoDetailView.this.hideView();
            }
        });

    }

    private void initTitleBar() {
        int stateBarHeight = DimenUtils.getStatusBarHeight(this.getContext());

        final int l = mBtnBack.getPaddingLeft();
        final int t = mBtnBack.getPaddingTop();
        final int r = mBtnBack.getPaddingRight();
        final int b = mBtnBack.getPaddingBottom();


        mBtnBack.setPadding(l, t + stateBarHeight, r, b);
        ViewGroup.LayoutParams params = mBtnBack.getLayoutParams();
        mBtnBack.setLayoutParams(params);
    }


    @Override
    protected void onDestroy() {
        if (mAdapter != null) {
            mAdapter.release();
        }
        KLockerConfigMgr.getInstance().setCoverVideoListPageShow(false);
        VideoPreLoaderService.sendStartPreload();
    }

    @Override
    protected void onShow() {
        if (mAdapter != null) {
            mAdapter.play();
            mAdapter.onShow();
        }
    }

    @Override
    protected void onHide() {
        if (mAdapter != null) {
            mAdapter.release();
            mAdapter.onHide();
        }
    }
}

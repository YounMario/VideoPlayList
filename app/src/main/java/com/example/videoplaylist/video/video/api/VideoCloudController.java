package com.example.videoplaylist.video.video.api;


import android.content.Context;

import com.cleanmaster.util.BatteryStatusUtil;
import com.cleanmaster.util.CMLog;
import com.cleanmaster.util.DLog;
import com.cleanmaster.util.KLockerConfigMgr;
import com.cmcm.kinfoc2.NetworkType;
import com.cmcm.kinfoc2.Utils;
import com.keniu.security.MoSecurityApplication;
import com.locker.net.ThreadManager;


/**
 * 云控Video 插电、WIFI 下载
 */
public class VideoCloudController {

    private String TAG = "VideoCloudController";
    //最多缓存条数
    private static final int MAX_CACHE_COUNT = 5;

    //最低电量--非充电状态
    private static final int MIN_BATTERY_LEVEL_NO_PLUG = 20;
    //最低电量--充电状态
    private static final int MIN_BATTERY_LEVEL_PLUGGING = 10;

    private boolean mIsPlugIn;

    private final Context mContext;

    public VideoCloudController() {
        mContext = MoSecurityApplication.getInstance();
    }


    public void setIsPlugIn(boolean in) {
        this.mIsPlugIn = in;
        if (mIsPlugIn) {
            DLog.toFile(TAG, "插电，尝试预拉。");
            preloadVideos();
        }
    }

    public void preloadVideos() {

        if (KLockerConfigMgr.getInstance().isCoverVideoListPageShow()) {
            DLog.toFile(TAG, " 锁屏的视频列表页 已经启动，不下载");
            return;
        }

        NetworkType netType = getCurrentNetWorkType();
        if (!isNetAllow(netType)) {
            DLog.toFile(TAG, " 不是 WIFI ，不允许下载");
            return;
        }
        if (isBatteryLow()) {
            DLog.toFile(TAG, "电量太低，不允许下载");
            return;
        }
        DLog.toFile(TAG, " 达到补充缓存条件");
        handleDownload(MAX_CACHE_COUNT);
    }

    private boolean isNetAllow(NetworkType netType) {
        if (NetworkType.WIFI.equals(netType)) {
            DLog.toFile(TAG, "Wifi 已经开启");
            return true;
        }
        return false;
    }

    private void handleDownload(final int minVideoCacheCount) {
        ThreadManager.getInstance().executeBL(new Runnable() {
            @Override
            public void run() {
                final int availableVideoCacheCount = VideoInfoModel.get().getCachedNotPlayVideosSync().size(); //获取可播放视频(缓冲池)总数
                int supplementCacheCount = minVideoCacheCount - availableVideoCacheCount;
                supplementCacheCount = supplementCacheCount > 0 ? supplementCacheCount : 0;

                DLog.toFile(TAG, "缓存池大小 :" + minVideoCacheCount + "，已缓存数：" + availableVideoCacheCount +"，本次需补充数：" + supplementCacheCount);

                if (supplementCacheCount > 0) {
                    downloadVideo(supplementCacheCount);
                }
            }
        });
    }

    private void downloadVideo(final int limitCount) {
        DLog.toFile(TAG, " ----> downloadVideos, count = " + limitCount);
        VideoInfoModel.get().downLoadVideos(limitCount, new MultiDownloadOption(), new VideoInfoModel.VideoInfoMultiDownLoadListener() {
            @Override
            public void onDownLoadOneSuccess(int remain) {
                if (remain == 0) {
                    CMLog.i(TAG, "[multi_download] onDownLoadOneSuccess : remain = " + remain);
                }
            }

            @Override
            public void onDownLoadOneFail(int remain) {
                CMLog.i(TAG, "[multi_download] onDownLoadOneFail : remain = " + remain);
            }

            @Override
            public void onFinished(int succeedCount) {
                DLog.toFile(TAG, "[multi_download] onFinished: succeedCount = " + succeedCount);
                //这里又加了一遍的原因是：
                // 快速观看一个视频，又切换另一个视频。这两次都会触发预加载，但是若第二次触发时，第一次的下载还未完成，那么第二次的触发就被取消了，所以这里会尝试补充一次。
                preloadVideos();
            }

            @Override
            public void onDownLoadStart() {
                CMLog.i(TAG, "[multi_download] onDownLoadStart");
            }
        });
    }

    private boolean isAllowedNetWorkType(NetworkType type) {
        return isNetAllow(type);
    }

    private boolean isBatteryLow() {
        int minBatteryLevel = mIsPlugIn ? MIN_BATTERY_LEVEL_PLUGGING : MIN_BATTERY_LEVEL_NO_PLUG;
        return BatteryStatusUtil.getBatteryLevel() < minBatteryLevel;
    }

    private NetworkType getCurrentNetWorkType() {
        return Utils.getNetworkType(mContext);
    }

    private class MultiDownloadOption extends DownloadOption {
        private static final int RETRY_TIMES = 2;

        private MultiDownloadOption() {
            super(false, VideoInfoModel.TAG_MULTI_DOWNLOAD, RETRY_TIMES);
        }

        @Override
        public boolean canRetry(DownloadResponse failedResponse) {
            return failedResponse != null && failedResponse.getResult() != ResultCode.SUCCESS;
        }

        @Override
        public boolean canRequest() {
            if (isBatteryLow()) {
                DLog.toFile(TAG, "电量低无法下载");
                return false;
            }
            return isAllowedNetWorkType(getCurrentNetWorkType());
        }
    }
}

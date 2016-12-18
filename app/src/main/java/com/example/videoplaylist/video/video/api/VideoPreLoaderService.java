package com.example.videoplaylist.video.video.api;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.cleanmaster.common.KCommons;
import com.cleanmaster.util.CMLog;
import com.cleanmaster.util.OpLog;
import com.keniu.security.MoSecurityApplication;
import com.locker.newscard.video.NewsCardVideoCloudConfig;


/**
 * Created by 龙泉 on 2016/11/22.
 */
public class VideoPreLoaderService extends Service {

    private static final String TAG = "VideoPreLoaderService";
    private static final String KEY_COMMAND = "key_load_command";

    private static final int NONE = 0;
    private static final int START_PRELOAD = 1;
    private static final int STOP_PRELOAD = 2;
    private static final int NET_WORK_CHANGED = 3;

    public static void sendStopPreload() {
        startService(STOP_PRELOAD);
    }

    public static void sendStartPreload() {
        if (NewsCardVideoCloudConfig.isNewsCardVideoSwitchOn()) {
            startService(START_PRELOAD);
        }
    }

    public static void startOnNetworkStateChanged() {
        if (NewsCardVideoCloudConfig.isNewsCardVideoSwitchOn()) {
            startService(NET_WORK_CHANGED);
        }
    }

    private static void startService(int command) {
        try {
            Context context = MoSecurityApplication.getAppContext();
            Intent intent = new Intent(context, VideoPreLoaderService.class);
            intent.putExtra(KEY_COMMAND, command);
            context.startService(intent);
        } catch (Exception e) {
            OpLog.toFile(TAG, "startService exception : " + e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CMLog.i(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int command = intent.getIntExtra(KEY_COMMAND, NONE);
            switch (command) {
                case START_PRELOAD:
                    startPreLoad();
                    break;
                case STOP_PRELOAD:
                    stopPreLoad();
                    break;
                case NET_WORK_CHANGED:
                    onNetWorkEnable(KCommons.isWifiNetworkUp(MoSecurityApplication.getAppContext()));
                    break;
                default:
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    private void startPreLoad() {
        CMLog.i(TAG, "startPreLoad");
        VideoInfoModel.get().preLoadVideos();
    }

    private void stopPreLoad() {
        CMLog.i(TAG, "stopPreLoad");
        VideoInfoModel.get().cancelMultiDownload();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void onNetWorkEnable(boolean isWifi) {
        CMLog.i(TAG, "network changed ： isWifi = " + isWifi);
        if (isWifi) {
            startPreLoad();
        } else {
            stopPreLoad();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CMLog.i(TAG, "onDestroy");
    }

}

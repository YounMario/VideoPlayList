package com.example.videoplaylist.video.video.api;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cleanmaster.util.OpLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by longquan
 */

public class HttpManager {

    private static String TAG = "HttpManager";

    private static HttpManager mInstance;
    private ThreadPoolExecutor mExecutor;

    private BlockingQueue<String> mDownloadingUrls;
    private ConcurrentHashMap<String, DownLoadTask> mDownLoadQueue;
    private ConcurrentHashMap<String, LinkedBlockingQueue<String>> mTagList;

    private final Object lock = new Object();

    private HttpManager() {
        mDownLoadQueue = new ConcurrentHashMap<>();
        mTagList = new ConcurrentHashMap<>();
        mDownloadingUrls = new LinkedBlockingDeque<>();
        mExecutor = new ThreadPoolExecutor(1, 1, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public static HttpManager getInstance() {
        if (mInstance == null) {
            mInstance = new HttpManager();
        }
        return mInstance;
    }

    public void downLoad(final String url, String md5, boolean runInNew, final String tag, final String savePath, DownLoadCallBack callBack) {
        if (TextUtils.isEmpty(url) || mDownLoadQueue.containsKey(url)) {
            return;
        }
        synchronized (lock) {
            mDownloadingUrls.add(url);
        }

        DownLoadTask task = new DownLoadTask(new DownLoadRunnable(tag, url, md5, savePath, callBack, new DownLoadRunnable.TaskEndListener() {

            @Override
            public void onComplete() {
                mDownLoadQueue.remove(url);
                removeFromTagList(tag, url);
                mDownloadingUrls.remove(url);
                OpLog.toFile(TAG, "task end url :" + url);
            }
        }));
        if (runInNew) {
            new Thread(task).start();
        } else {
            mExecutor.submit(task);
        }
        addToTagList(tag, url);
        mDownLoadQueue.put(url, task);
    }

    public void cancel(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        if (!mDownLoadQueue.containsKey(url)) {
            return;
        }
        OpLog.toFile(TAG, "cancel url:" + url);
        mDownLoadQueue.get(url).cancel(true);
    }

    public int getTaskSize(String tag) {
        if (TextUtils.isEmpty(tag)) {
            return 0;
        }
        if (mTagList.containsKey(tag)) {
            return mTagList.get(tag).size();
        }
        return 0;
    }

    private void addToTagList(String tag, String url) {
        synchronized (lock) {
            if (TextUtils.isEmpty(tag)) {
                return;
            }
            if (!mTagList.containsKey(tag)) {
                mTagList.put(tag, new LinkedBlockingQueue<String>());
            }
            mTagList.get(tag).add(url);
        }
    }

    private void removeFromTagList(String tag, String url) {
        BlockingQueue<String> list = mTagList.get(tag);
        if (list != null) {
            list.remove(url);
        }
    }

    public List<String> getDownloadingList() {
        ArrayList<String> list;
        synchronized (lock) {
            list = new ArrayList<>(mDownloadingUrls);
        }
        return list;
    }

    public void cancelDownloading() {
        synchronized (lock) {
            for (String url : mDownloadingUrls) {
                if (TextUtils.isEmpty(url)) {
                    continue;
                }
                if (mDownLoadQueue.containsKey(url)) {
                    mDownLoadQueue.get(url).cancel(true);
                    mDownloadingUrls.remove(url);
                }
            }
        }
    }

    public void cancelTaskByTag(String tag) {
        synchronized (lock) {
            if (mTagList.containsKey(tag)) {
                BlockingQueue<String> downloadingQueue = mTagList.get(tag);
                for (String url :
                        downloadingQueue) {
                    OpLog.toFile(TAG, "cancel by tag  : " + tag + " current url :" + url);
                    cancel(url);
                    mDownloadingUrls.remove(url);
                    mDownLoadQueue.remove(url);
                }
            }
        }
    }


    public interface DownLoadCallBack {

        void onProgress(int percent);

        void onSuccess(@NonNull DownloadResponse response);

        void onFailed(@NonNull DownloadResponse response);

        void onStart();
    }
}

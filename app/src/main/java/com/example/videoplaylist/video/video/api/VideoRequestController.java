package com.example.videoplaylist.video.video.api;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cleanmaster.util.CMLog;
import com.cleanmaster.util.DLog;
import com.cleanmaster.util.LockerFileUtils;
import com.cleanmaster.util.OpLog;
import com.cmcm.locker.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 龙泉 on 2016/8/18.
 */
public class VideoRequestController {

    private static final String TAG = "VideoRequestController";
    private volatile boolean isMultiTaskRunning = false;

    /**
     * 批量下载
     *
     * @param list                  下载列表
     * @param option                下载选项
     * @param multiDownloadListener 下载回调
     */
    public void downloadVideoFiles(List<VideoInfo> list, DownloadOption option, final VideoInfoModel.VideoInfoMultiDownLoadListener multiDownloadListener) {
        if (isMultiTaskRunning) {
            DLog.toFile(TAG, " 当前存在多任务下载，本次不下载");
            return;
        }
        if (list == null || list.size() == 0) {
            DLog.toFile(TAG, " 队列为空不需要下载");
            return;
        }
        isMultiTaskRunning = true;
        final AtomicInteger count = new AtomicInteger(list.size());
        final AtomicInteger successCount = new AtomicInteger(0);
        if (multiDownloadListener != null) {
            multiDownloadListener.onDownLoadStart();
        }
        DLog.toFile(TAG, " 本次多任务下载个数:" + list.size());
        for (final VideoInfo info : list) {

            final DownloadOption copyOption = DownloadOption.copyOne(option);
            downloadVideoFile(info, copyOption, null, new VideoInfoModel.VideoDownLoadListener() {

                @Override
                public void onStart() {
                    CMLog.i(TAG, "multi_download onStart:" + info.getVideoUrl());
                }

                @Override
                public void onUpdate(int percent) {
                    CMLog.i(TAG, "multi_download onUpdate :" + percent);
                }

                @Override
                public void onSuccess(@NonNull VideoInfo info, @NonNull DownloadResponse response) {
                    int remain = count.decrementAndGet();
                    int succeedCount = successCount.incrementAndGet();
                    CMLog.i(TAG, "multi_download onSuccess one : id = " + info.getVideoId() + ", remain = " + remain + ", succeed count = " + succeedCount);
                    if (multiDownloadListener != null) {
                        multiDownloadListener.onDownLoadOneSuccess(remain);
                    }
                    if (remain == 0) {
                        if (multiDownloadListener != null) {
                            multiDownloadListener.onFinished(succeedCount);
                        }
                        isMultiTaskRunning = false;
                        CMLog.i(TAG, "multi_download success!");
                    }
                }

                @Override
                public void onFailed(@NonNull DownloadResponse response) {
                    int remain = count.decrementAndGet();
                    CMLog.i(TAG, "multi_download failed download one remain:" + remain);
                    if (multiDownloadListener != null) {
                        multiDownloadListener.onDownLoadOneFail(remain);
                    }
                    if (remain == 0) {
                        if (multiDownloadListener != null) {
                            multiDownloadListener.onFinished(successCount.get());
                        }
                        isMultiTaskRunning = false;
                        CMLog.i(TAG, "multi_download failed!");
                    }
                }
            });
        }
    }

    public void cancelMultiDownload() {
        isMultiTaskRunning = false;
        HttpManager.getInstance().cancelTaskByTag(VideoInfoModel.TAG_MULTI_DOWNLOAD);
    }

    private void downloadVideoFile(final VideoInfo bean, final DownloadOption downloadOption, final Handler handler, final VideoInfoModel.VideoDownLoadListener listener) {

        if (bean == null) {
            asyncFailed(handler, DownloadResponse.paramsError(), listener);
            return;
        }

        final String videoDownloadUrl = bean.getVideoUrl();
        if (TextUtils.isEmpty(videoDownloadUrl)) {
            asyncFailed(handler, DownloadResponse.paramsError(), listener);
            return;
        }

        final String videoLocalPath = bean.getVideoLocalPath();
        if (TextUtils.isEmpty(videoLocalPath)) {
            asyncFailed(handler, DownloadResponse.paramsError(), listener);
            return;
        }

        if (BuildConfig.DEBUG) {
            OpLog.toFile(TAG, "downloading Url :" + videoDownloadUrl);
        }

        if (!downloadOption.canRequest()) {
            asyncFailed(handler, new DownloadResponse(0, ResultCode.ERROR_REQUEST_NOT_ALLOWED), listener);
            return;
        }

        HttpManager.getInstance().downLoad(videoDownloadUrl, bean.getMd5(), downloadOption.isInNewThread(),
                downloadOption.getTag(), videoLocalPath, new HttpManager.DownLoadCallBack() {

                    @Override
                    public void onStart() {
                        asyncStart(handler, listener);
                    }

                    @Override
                    public void onProgress(int percent) {
                        asyncUpdate(handler, listener, percent);
                    }

                    @Override
                    public void onSuccess(@NonNull DownloadResponse response) {
                        CMLog.i(TAG, "on success");
                        asyncSuccess(handler, listener, bean, response);
                    }

                    @Override
                    public void onFailed(@NonNull DownloadResponse response) {
                        int retryTimes = downloadOption.getRetryTimes();
                        if (retryTimes <= 0 || !downloadOption.canRetry(response)) {
                            asyncFailed(handler, response, listener);
                            CMLog.i(TAG, "on failed.");
                        } else {
                            CMLog.i(TAG, "retry..." + retryTimes);
                            downloadOption.setRetryTimes(retryTimes - 1);
                            downloadVideoFile(bean, downloadOption, handler, listener);
                        }
                    }
                });
    }

    private void asyncStart(Handler handler, final VideoInfoModel.VideoDownLoadListener listener) {
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) {
                        listener.onStart();
                    }
                }
            });
        } else {
            if (listener != null) {
                listener.onStart();
            }
        }
    }


    private void asyncUpdate(Handler handler, final VideoInfoModel.VideoDownLoadListener videoDownLoadListener, final int percent) {
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (videoDownLoadListener != null) {
                        videoDownLoadListener.onUpdate(percent);
                    }
                }
            });
        } else {
            if (videoDownLoadListener != null) {
                videoDownLoadListener.onUpdate(percent);
            }
        }
    }

    private void asyncSuccess(Handler handler, final VideoInfoModel.VideoDownLoadListener videoDownLoadListener, final VideoInfo info, final DownloadResponse response) {
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (videoDownLoadListener != null) {
                        videoDownLoadListener.onSuccess(info, response);
                    }
                }
            });
        } else {
            if (videoDownLoadListener != null) {
                videoDownLoadListener.onSuccess(info, response);
            }
        }
    }

    private void asyncFailed(Handler handler, final DownloadResponse response, final VideoInfoModel.VideoDownLoadListener videoDownLoadListener) {
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    videoDownLoadListener.onFailed(response);
                }
            });
        } else {
            videoDownLoadListener.onFailed(response);
        }
    }

    public static List<VideoInfo> parseVideoJson(JSONObject jsonString) {
        List<VideoInfo> beans = new ArrayList<>();
        try {
            JSONArray dataJson = jsonString.optJSONArray("data");
            if (dataJson == null) return beans;
            List<VideoInfo> normalVideos = getVideoArray(dataJson);

            if (normalVideos != null) {
                beans.addAll(normalVideos);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return beans;
    }

    private static List<VideoInfo> getVideoArray(JSONArray arrayJson) throws JSONException {
        List<VideoInfo> beans = new ArrayList<>();
        if (arrayJson == null) return beans;
        for (int i = 0; i < arrayJson.length(); i++) {
            try {
                JSONObject beanJson = arrayJson.getJSONObject(i);
                if (!isValid(beanJson)) {
                    continue;
                }
                VideoInfo bean = new VideoInfo();
                bean.setVideoId(beanJson.optString("id"));
                final String videoUrl = beanJson.optString("video_url");
                bean.setVideoUrl(videoUrl);
                bean.setThumbnailUrl(beanJson.optString("thumbnail_url"));
                bean.setMd5(beanJson.optString("md5"));
                bean.setTitle(beanJson.optString("title"));
                bean.setDesc(beanJson.optString("desc"));
                bean.setType(beanJson.optInt("type"));
                bean.setCategoryId(beanJson.optInt("cate_id"));
                bean.setVideoLocalPath(LockerFileUtils.convertUrlToLocalPath(videoUrl));
                beans.add(bean);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return beans;
    }


    private static boolean isValid(JSONObject object) {
        if (object == null) return false;
        if (!object.has("id")) return false;
        if (!object.has("video_url")) return false;
        if (!object.has("thumbnail_url")) return false;
        if (!object.has("md5")) return false;
        if (!object.has("cate_id")) return false;
        return true;
    }

    public void cancel(String url) {
        HttpManager.getInstance().cancel(url);
    }

    List<String> getDownLoadingList() {
        return HttpManager.getInstance().getDownloadingList();
    }

    public int getTagCount(String tag) {
        return HttpManager.getInstance().getTaskSize(tag);
    }


    public void cancelDownload() {
        isMultiTaskRunning = false;
        HttpManager.getInstance().cancelDownloading();
    }
}

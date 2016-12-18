package com.example.videoplaylist.video.video.api;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.cleanmaster.dao.DaoFactory;
import com.cleanmaster.dao.VideoInfoDao;
import com.cleanmaster.mutual.BackgroundThread;
import com.cleanmaster.util.BatteryStatusUtil;
import com.cleanmaster.util.CMLog;
import com.cleanmaster.util.DLog;
import com.cleanmaster.util.OpLog;
import com.keniu.security.MoSecurityApplication;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class VideoInfoModel {
    private static final String TAG = "VideoInfoModel";
    public static final String TAG_MULTI_DOWNLOAD = "multi_down_load";

    private final Context mContext;

    private final VideoInfoDao mVideoInfoDao;
    private final Handler mHandler;
    private final VideoRequestController mVideoRequestController;
    private final VideoCloudController mVideoCloudController;


    private VideoInfoModel() {
        mContext = MoSecurityApplication.getAppContext();
        mHandler = new Handler(Looper.getMainLooper());
        mVideoInfoDao = DaoFactory.getVideoInfoDao(mContext);
        mVideoRequestController = new VideoRequestController();
        mVideoCloudController = new VideoCloudController();
    }

    protected void cancelMultiDownload() {
        mVideoRequestController.cancelMultiDownload();
        HttpManager.getInstance().cancelTaskByTag(VideoInfoModel.TAG_MULTI_DOWNLOAD);
    }

    private static final class Holder {
        private static final VideoInfoModel sInstance = new VideoInfoModel();
    }

    public static VideoInfoModel get() {
        return Holder.sInstance;
    }

    private static JSONObject convert(InputStream inputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = reader.readLine()) != null) {
                responseStrBuilder.append(inputStr);
            }
            return new JSONObject(responseStrBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getJsonObject() {
        final AssetManager assets = mContext.getResources().getAssets();
        try {
            return convert(assets.open("video_info_list.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 初始化DB，只在程序启动时，初始化一次。
     * <p>
     * 暂时逻辑，后期应会改成从接口获取。
     */
    public void init() {
        final List<VideoInfo> all = mVideoInfoDao.findAll();
        if (all == null || all.isEmpty()) {
            final JSONObject jsonObject = getJsonObject();
            if (jsonObject == null) {
                return;
            }
            List<VideoInfo> infoList = VideoRequestController.parseVideoJson(jsonObject);
            removeDuplicateWithOrder(infoList);
            OpLog.toFile(TAG, "first init video_info dao。videoInfoList size = " + infoList.size());
            saveVideoInfos(infoList);
        }
        mVideoCloudController.setIsPlugIn(BatteryStatusUtil.isPlugged());
    }

    public void onPowerConnected() {
        CMLog.i(TAG, "on Power connect");
        mVideoCloudController.setIsPlugIn(true);
    }

    public void onPowerDisConnected() {
        CMLog.i(TAG, "on Power disconnect");
        mVideoCloudController.setIsPlugIn(false);
    }

    public List<VideoInfo> findAll(){
        return mVideoInfoDao.findAll();
    }

    /**
     * 该方法 只能由VideoPreLoaderService来触发，其他严禁调用！
     */
    protected void preLoadVideos(){
        mVideoCloudController.preloadVideos();
    }

    public long saveVideoInfos(final List<VideoInfo> videoInfos) {
        return mVideoInfoDao.saveAll(videoInfos);
    }

    public void setVideoExpired(VideoInfo info) {
        if (info != null && mVideoInfoDao.setVideoExpired(info.getVideoId())) {
            info.setState(VideoInfo.STATE_PLAYED);
        }
    }

    public void getOneCachedNotPlayVideoAsync(final VideoLoadListener listener) {
        BackgroundThread.post(new Runnable() {
            @Override
            public void run() {
                final List<VideoInfo> cachedNotPlayVideos = getCachedNotPlayVideosSync();
                if (!cachedNotPlayVideos.isEmpty()) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onSuccess(cachedNotPlayVideos.get(0));
                        }
                    });
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onFailed();
                        }
                    });
                }
            }
        });
    }


    /**
     * 下载好的，未播过的视频
     *
     * @return
     */
    public List<VideoInfo> getCachedNotPlayVideosSync() {
        return pickDownloadedVideoList(getAllNotPlayVideos());
    }

    /**
     * 点击视频卡片后进入列表页，数据准备策略
     * 1.取出同类型的视频，最多4个；
     * 2.取出剩余的视频，顺序随机。
     */
   public List<VideoInfo> getNotPlayVideosByCategoryId(int cateId, List<String> notInList) {
       final List<VideoInfo> returnList = new ArrayList<>();
       //1.取出4个同类型视频，过滤掉当前视频
       List<VideoInfo> categoryList = mVideoInfoDao.findLimitNotPlayDataByCategoryId(cateId, notInList, 4);
       if (categoryList != null) {
           returnList.addAll(categoryList);
           //2.得到要过滤掉的ids
           notInList = (notInList == null ? new ArrayList<String>(): notInList);
           for (VideoInfo info : categoryList) {
               notInList.add(info.getVideoId());
           }
       }
       //3.取出要过滤的ids外的其余数据。
       List<VideoInfo> filterList = mVideoInfoDao.findNotPlayData(notInList);
       if (filterList != null && !filterList.isEmpty()) {
           returnList.addAll(filterList);
       }
       return returnList;
   }

    //预加载策略：进入视频列表页的时候，就停掉 多任务下载；离开视频列表的时候，再开始预加载。
    public void downLoadVideos(final int limit, final DownloadOption option, final VideoInfoMultiDownLoadListener multiDownLoadListener) {
        final List<VideoInfo> list = getDownLoadAvailableInfosUrlSync(limit);
        //final int allNotPlayVideosSize = getAllNotPlayVideos().size();

        DLog.toFile(TAG, "downloadVideos : 需补充数目 = " + limit + ", 可下载数 = " + list.size() /*+ ", 当前数据库中所有未播过的 VideoInfo 条目为 = " + allNotPlayVideosSize*/);

        //// TODO: 2016/12/15 后期版本 增加网络请求接口后，再放开注释的逻辑。因为现在数据在本地，有多少请求多少，没有就不请求。
//        if (limit <= allNotPlayVideosSize) {
            if (list.isEmpty()) {
                CMLog.i(TAG, "list 为空，本次不下载");
            } else {
                CMLog.i(TAG, "开始多任务下载");
                mVideoRequestController.downloadVideoFiles(list, option, multiDownLoadListener);
            }
//        } else {
//            requestVideoInfo(new VideoInfoModel.VideoInfoDownloadListener() {
//                @Override
//                public void onSuccess(final List<VideoInfo> result) {
//                    OpLog.toFile(TAG, "下载URL成功, 大小:" + result.size());
//                    ThreadManager.getInstance().executeBL(new Runnable() {
//                        @Override
//                        public void run() {
//                            List<VideoInfo> regetResult = getDownLoadAvailableInfosUrlSync(limit);
//                            OpLog.toFile(TAG, "再次获取：未播过的且可下载的新视频的数目 = " + list.size());
//                            videoRequestController.downloadVideoFiles(regetResult, option, multiDownLoadListener);
//                        }
//                    });
//                }
//
//                @Override
//                public void onFailed() {
//                    OpLog.toFile(TAG, "下载URL 失败");
//                }
//            });
//        }
    }

    /**
     * 未播过的，未下载的新视频。
     */
    private List<VideoInfo> getDownLoadAvailableInfosUrlSync(int limit) {
        final List<String> downLoadingList = mVideoRequestController.getDownLoadingList();
        CMLog.i(TAG, "Downloading urls :" + downLoadingList);
        return pickNotDownloadVideoList(limit, mVideoInfoDao.getNotPlayVideos(downLoadingList));
    }

    /**
     * 所有未播过的视频：包括缓存的，与未缓存的。
     *
     * @return
     */
    private List<VideoInfo> getAllNotPlayVideos() {
        return mVideoInfoDao.getNotPlayVideos();
    }

    private List<VideoInfo> findByCategoryId(int cateid) {
        return mVideoInfoDao.findDataByCategoryId(cateid);
    }

    private List<VideoInfo> pickDownloadedVideoList(List<VideoInfo> list) {
        List<VideoInfo> temp = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return temp;
        }
        for (VideoInfo info : list) {
            if (info.isDownloaded()) {
                temp.add(info);
            }
        }
        return temp;
    }

    private List<VideoInfo> pickNotDownloadVideoList(int limitSize, List<VideoInfo> list) {
        List<VideoInfo> temp = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return temp;
        }
        for (VideoInfo info : list) {
            if (!info.isDownloaded()) {
                temp.add(info);
                if (temp.size() == limitSize) {
                    return temp;
                }
            }
        }
        return temp;
    }

    private static void removeDuplicateWithOrder(List<VideoInfo> list) {
        DLog.toFile(TAG, "____________ original size = " + list.size());
        long time = System.currentTimeMillis();

        final Set<VideoInfo> set = new HashSet<>();
        final List<VideoInfo> newList = new ArrayList<>();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            VideoInfo element = (VideoInfo) iter.next();
            if (set.add(element)) {
                newList.add(element);
            } else {
                DLog.toFile(TAG, "duplicate id = " + element.getVideoId());
            }
        }
        list.clear();
        list.addAll(newList);

        long dt = System.currentTimeMillis() - time;
        DLog.toFile(TAG, "_____________ cost time = " + dt + ", result size = " + list.size());
    }

    public interface VideoLoadListener {
        void onSuccess(VideoInfo videoInfo);
        void onFailed();
    }

    public interface VideoDownLoadListener {

        void onStart();

        void onUpdate(int percent);

        void onSuccess(@NonNull VideoInfo info, @NonNull DownloadResponse response);

        void onFailed(@NonNull DownloadResponse response);
    }

    public interface VideoInfoMultiDownLoadListener {
        void onDownLoadStart();

        void onDownLoadOneSuccess(int remain);

        void onDownLoadOneFail(int remain);

        void onFinished(int succeedCount);
    }

}

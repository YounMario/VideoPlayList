package com.example.videoplaylist.video.video;

import com.cleanmaster.util.DLog;
import com.cmcm.onews.model.ONews;
import com.locker.newscard.video.api.VideoInfo;
import com.locker.newscard.video.api.VideoInfoModel;
import com.locker.newscard.video.api.VideoPreLoaderService;

import java.util.List;

/**
 * Created by zhangbo on 2016/12/14.
 */

public class NewsCardVideoUtils {
    public static final String CTYPE_VIDEO = "0x123456789";

   private static boolean hasCachedVideos() {
        List<VideoInfo> cachedNotPlayVideos = VideoInfoModel.get().getCachedNotPlayVideosSync();
        int size = cachedNotPlayVideos.size();
        DLog.toFile("NewsCardVideo", "缓存池未播放数 ：" + size /*+ ", videos = " + cachedNotPlayVideos*/);
        return size > 0;
    }

    public static void insertVideoNews(List<ONews> list) {
        if (list == null) {
            return;
        }

        if (!NewsCardVideoCloudConfig.isNewsCardVideoSwitchOn()) {
            return;
        }

        if (!hasCachedVideos()) {
            //显示视频卡的外部条件已满足，但是还没有已缓存的视频，这时应该触发一次预加载。
            VideoPreLoaderService.sendStartPreload();
            return;
        }

        ONews news = new ONews();
        news.ctype(CTYPE_VIDEO);
        list.add(news);
    }
}

package com.example.videoplaylist.video.video;

import com.cleanmaster.util.DLog;
import com.cmcm.cmlocker.business.cube.CubeUtil;
import com.ijinshan.cloudconfig.deepcloudconfig.CloudConfigExtra;
import com.locker.newscard.utils.NewsCardUtils;

/**
 * Created by luojianhui on 2016/12/17.
 */

public class NewsCardVideoCloudConfig {
    private static final int SWITCH_ON = 1;
    private static final int SWITCH_OFF = 0;
    private static final String SECTION_NEWS_CARD_VIDEO = "section_news_card_video";
    private static final String KEY_SWITCH = "key_switch";

    public static boolean isNewsCardVideoSwitchOn() {
        if (DLog.isDebugMode()) {
            return true;
        }
        if (!NewsCardUtils.isNewsCardEnable()) {
            return false;
        }

        final int switchValue = CloudConfigExtra.getIntValue(CubeUtil.KEY_FUNC, SECTION_NEWS_CARD_VIDEO, KEY_SWITCH, SWITCH_ON);
        return switchValue == SWITCH_ON;
    }
}

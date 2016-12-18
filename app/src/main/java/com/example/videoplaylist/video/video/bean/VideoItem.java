package com.example.videoplaylist.video.video.bean;

/**
 * Created by 龙泉 on 2016/12/15.
 */

public class VideoItem {

    private String mLocalPath;
    private String mWebPath;

    public VideoItem() {

    }


    public String getLocalPath() {
        return mLocalPath;
    }

    public void setLocalPath(String localPath) {
        this.mLocalPath = localPath;
    }

    public String getWebPath() {
        return mWebPath;
    }

    public void setWebPath(String webPath) {
        this.mWebPath = webPath;
    }
}

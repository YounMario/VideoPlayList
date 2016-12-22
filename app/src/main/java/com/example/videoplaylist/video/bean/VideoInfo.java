package com.example.videoplaylist.video.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.File;

public class VideoInfo implements Parcelable {

    public static final int STATE_NOT_PLAY = 0;
    public static final int STATE_PLAYED = 1;


    private long id;
    private String videoId;
    private String videoUrl;//视频的url
    private String thumbnailUrl;//视频缩略图 url
    private String md5;
    private String title;
    private String desc;
    private int type;//视频类型 ：0 普通 1 广告
    private int categoryId;//视频分类 默认0
    private String videoLocalPath;//【本地字段】视频缓存路径

    public VideoInfo() {
    }

    private VideoInfo(Parcel in) {
        this.id = in.readLong();
        this.videoId = in.readString();
        this.videoUrl = in.readString();
        this.thumbnailUrl = in.readString();
        this.md5 = in.readString();
        this.title = in.readString();
        this.desc = in.readString();
        this.type = in.readInt();
        this.categoryId = in.readInt();
        this.videoLocalPath = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(videoId);
        dest.writeString(videoUrl);
        dest.writeString(thumbnailUrl);
        dest.writeString(md5);
        dest.writeString(title);
        dest.writeString(desc);
        dest.writeInt(type);
        dest.writeInt(categoryId);
        dest.writeString(videoLocalPath);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getVideoLocalPath() {
        return videoLocalPath;
    }

    public void setVideoLocalPath(String videoLocalPath) {
        this.videoLocalPath = videoLocalPath;
    }

    @Override
    public String toString() {
        return "VideoInfo{" +
                "id=" + id +
                ", videoId =" + videoId +
                ", videoDownloadUrl = " + videoUrl +
                ", thumbnailUrl =" + thumbnailUrl +
                ", md5='" + md5 +
                ", title = " + title +
                ", desc = " + desc +
                ", type = " + type +
                ", categoryId =" + categoryId +
                ", videoLocalPath='" + videoLocalPath +
                '}';
    }

    public static final Creator<VideoInfo> CREATOR = new Creator<VideoInfo>() {
        @Override
        public VideoInfo createFromParcel(Parcel in) {
            return new VideoInfo(in);
        }

        @Override
        public VideoInfo[] newArray(int size) {
            return new VideoInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        VideoInfo temp = (VideoInfo) obj;
        return videoId.equalsIgnoreCase(temp.getVideoId());
    }

    @Override
    public int hashCode() {
        return videoId.hashCode();
    }

}

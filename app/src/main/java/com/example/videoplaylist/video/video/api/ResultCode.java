package com.example.videoplaylist.video.video.api;

/**
 * Created by 龙泉 on 2016/8/31.
 */
public class ResultCode {


    //did request
    public final static int SUCCESS = 10000;
    public final static int REQUEST_URL_FROM_SERVER_FAILED = 10001;
    public final static int REQUEST_TIME_OUT = 10002;
    public final static int USER_CANCELED = 10003;
    public final static int FILE_NOT_COMPLETE = 10005;
    public final static int REQUEST_FAILED = 10006;

    //didn't request
    public final static int BATTERY_LOW = 20001;
    public final static int NET_WORK_NOT_AVALIBLE = 20003;
    public final static int ERROR_PARAMS = 20004;
    public final static int GET_DOWNLOAD_LOCAL_PATH_ERROR = 20005;
    public final static int USER_CANCELED_BEFORE_REQUESTING = 20006;
    public final static int NOT_RECOMMEND_VIDEO = 20007;
    public final static int ERROR_REQUEST_NOT_ALLOWED = 20008;
    public final static int SUCCESS_WITH_NOT_REQUEST_NET = 20009;
    public final static int OUTPUT_PATH_IS_EMPTY = 20010;
    public final static int RENAME_FILE_FAILED = 20011;
    public final static int COLLECTION_TYPE_NO_NEED_TO_PULL_LIST = 20012;

    //[audio] didn't request
    public static final int AUDIO_URL_IS_EMPTY_ERROR = 21001;
    public final static int AUDIO_GET_DOWNLOAD_LOCAL_PATH_ERROR = 21002;



}

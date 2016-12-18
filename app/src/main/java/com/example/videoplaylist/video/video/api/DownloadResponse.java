package com.example.videoplaylist.video.video.api;


/**
 * Created by 龙泉 on 2016/9/5.
 */
public class DownloadResponse {

    private long duringTime;
    private int result;


    private String msg;

    public DownloadResponse(long duringTime, int result) {
        this(duringTime, result, "");
    }

    public DownloadResponse(long duringTime, int result, String msg) {
        this.duringTime = duringTime;
        this.result = result;
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public long getDuringTime() {
        return duringTime;
    }

    public void setDuringTime(long duringTime) {
        this.duringTime = duringTime;
    }

    public static DownloadResponse success(long duringTime){
        return new DownloadResponse(duringTime,ResultCode.SUCCESS);
    }

    public static DownloadResponse cancelResponse() {
        return new DownloadResponse(0, ResultCode.USER_CANCELED);
    }

    public static DownloadResponse netWorkNotAvalible() {
        return new DownloadResponse(0, ResultCode.NET_WORK_NOT_AVALIBLE);
    }

    public static DownloadResponse paramsError() {
        return new DownloadResponse(0, ResultCode.ERROR_PARAMS);
    }

    public static DownloadResponse localOutPutPathError() {
        return new DownloadResponse(0, ResultCode.GET_DOWNLOAD_LOCAL_PATH_ERROR);
    }

    public static DownloadResponse cancelResponseBeforeRequest() {
        return new DownloadResponse(0, ResultCode.USER_CANCELED_BEFORE_REQUESTING);
    }

    //----------Audio Download Error Start-----------//
    public static DownloadResponse audioEmptyUrlError() {
        return new DownloadResponse(0, ResultCode.AUDIO_URL_IS_EMPTY_ERROR);
    }

    public static DownloadResponse audioLocalOutPutPathError() {
        return new DownloadResponse(0, ResultCode.AUDIO_GET_DOWNLOAD_LOCAL_PATH_ERROR);
    }
    //----------Audio Download Error End-----------//

}

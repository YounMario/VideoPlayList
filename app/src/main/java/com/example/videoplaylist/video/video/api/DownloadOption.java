package com.example.videoplaylist.video.video.api;

/**
 * Created by 龙泉 on 2016/9/5.
 */
public abstract class DownloadOption {

    private boolean inNewThread;
    private String tag;
    private int retryTimes;

    public DownloadOption(boolean inNewThread, String tag) {
        this.inNewThread = inNewThread;
        this.tag = tag;
    }

    public DownloadOption(boolean inNewThread, String tag, int retryTimes) {
        this.inNewThread = inNewThread;
        this.tag = tag;
        this.retryTimes = retryTimes;
    }

    public void setInNewThread(boolean in) {
        this.inNewThread = in;
    }

    public boolean isInNewThread() {
        return inNewThread;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag){
       this.tag = tag;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public abstract boolean canRequest();

    public abstract boolean canRetry(DownloadResponse failedResponse);

    /**
     * copy一个副本：防止批量下载时使用同一个option，影响同批次的其他下载。
     * @param originOption
     * @return
     */
    public static DownloadOption copyOne(final DownloadOption originOption) {
        return new DownloadOption(originOption.inNewThread, originOption.tag, originOption.retryTimes) {
            @Override
            public boolean canRequest() {
                return originOption.canRequest();
            }

            @Override
            public boolean canRetry(DownloadResponse failedResponse) {
                return originOption.canRetry(failedResponse);
            }
        };
    }
}

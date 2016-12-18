package com.example.videoplaylist.video.video.api;

import java.util.concurrent.FutureTask;

/**
 * Created by longquan on 2016/8/28.
 */

public class DownLoadTask extends FutureTask<Boolean> {

    private DownLoadRunnable downLoadRunable;
    private String tag;

    DownLoadTask(DownLoadRunnable downLoadRunable) {
        super(downLoadRunable);
        this.downLoadRunable = downLoadRunable;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        downLoadRunable.cancel();
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    protected void done() {
        super.done();
    }
}

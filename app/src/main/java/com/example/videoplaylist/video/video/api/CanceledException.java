package com.example.videoplaylist.video.video.api;

/**
 * Created by 龙泉 on 2016/9/5.
 */
public class CanceledException extends RuntimeException {


    public CanceledException() {
        super("canceled download");
    }
}

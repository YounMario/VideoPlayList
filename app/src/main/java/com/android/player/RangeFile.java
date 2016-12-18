package com.android.player;

import com.example.videoplaylist.App;

import java.io.File;
import java.io.FileNotFoundException;

import okio.BufferedSink;
import okio.Okio;

/**
 * Created by wangye on 16-1-25.
 */
public class RangeFile {
    private static final String sPath = App.getInstance().getFilesDir().getAbsolutePath();

    public BufferedSink fileSink;
    public File file;
    public String path;
    public long start;
    public long end;
    public RangeFile(String md5, long range) {
        try {
            start = range;
            end = range;
            path = sPath + File.separator + md5 + "_glue_range_" + range;
            file = new File(path);
            fileSink = Okio.buffer(Okio.sink(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
package com.example.videoplaylist.video.video.api;

import android.text.TextUtils;

import com.cleanmaster.util.Md5Util;
import com.cleanmaster.util.OpLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * Created by longquan on 2016/8/28.
 */

public class DownLoadRunnable implements Callable<Boolean> {

    private static final String TAG = "DownLoadRunnable";
    private String url;
    private HttpManager.DownLoadCallBack callBack;

    private String outputPath;
    private String tag;

    private volatile boolean cancel;
    private TaskEndListener taskEndListener;
    private String fileMd5;


    public DownLoadRunnable(String tag, String url, String md5 , String outputPath, HttpManager.DownLoadCallBack callBack, TaskEndListener taskEndListener) {
        this.url = url;
        this.callBack = callBack;
        this.outputPath = outputPath;
        this.tag = tag;
        cancel = false;
        this.fileMd5 = md5;
        this.taskEndListener = taskEndListener;
    }


    private void closeReadStream(InputStream in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void closeWriteStream(OutputStream out) {
        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onStart(){
        if(callBack != null){
            callBack.onStart();
        }
    }

    private void onFail(int responseCode, long duringTime) {
        onFail(responseCode, duringTime, "");
    }

    private void onFail(int responseCode, long duringTime, String msg) {
        if (callBack != null) {
            if (!cancel) {
                callBack.onFailed(new DownloadResponse(duringTime, responseCode, msg));
            }
        }
    }

    private void onSuccess(long duringTime) {
        if (callBack != null) {
            if(!cancel){
                callBack.onSuccess(DownloadResponse.success(duringTime));
            }
        }
    }

    private void onUpdate(int percent) {
        if (callBack != null) {
            if (!cancel) {
                callBack.onProgress(percent);
            }
        }
    }


    @Override
    public Boolean call() throws Exception {
        HttpURLConnection connection = null;
        InputStream in = null;
        FileOutputStream out = null;
        File tempFile = null;
        long duringTime = 0;
        try {
            long startTime = System.currentTimeMillis();
            URL httpUrl = new URL(url);
            connection = (HttpURLConnection) httpUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            int responseCode = connection.getResponseCode();
            onStart();
            if(cancel){
                throw new CanceledException();
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                int contentLength = connection.getContentLength();
                connection.getContentType();
                connection.getContentLength();

                byte[] read = new byte[8192];
                int readcount;
                int totalread = 0;
                in = connection.getInputStream();


                if (TextUtils.isEmpty(outputPath)) {
                    onFail(ResultCode.OUTPUT_PATH_IS_EMPTY, 0);
                    return true;
                }


                String tempPath = outputPath + ".temp";
                tempFile = new File(tempPath);
                File outPut = new File(outputPath);
                out = new FileOutputStream(tempFile);

                int lastProcess = 0;
                while (!cancel && (readcount = in.read(read, 0, read.length)) != -1) {
                    totalread += readcount;
                    int process = (int) (totalread * 1.0f * 100 / contentLength);
                    if (lastProcess != process) {
                        onUpdate(process);
                    }
                    lastProcess = process;
                    out.write(read, 0, readcount);
                }
                duringTime = System.currentTimeMillis() - startTime;
                if (cancel) {
                    throw new CanceledException();
                } else {
                    if (!TextUtils.isEmpty(fileMd5)) {
                        if (fileMd5.equals(Md5Util.getFileMD5(tempFile))) {
                            if (tempFile.renameTo(outPut)) {
                                onSuccess(duringTime);
                            } else {
                                tempFile.delete();
                                onFail(ResultCode.FILE_NOT_COMPLETE, duringTime);
                            }
                        } else {
                            tempFile.delete();
                            onFail(ResultCode.FILE_NOT_COMPLETE, duringTime);
                        }
                    } else {
                        if (tempFile.renameTo(outPut)) {
                            onSuccess(duringTime);
                        } else {
                            tempFile.delete();
                            onFail(ResultCode.RENAME_FILE_FAILED, duringTime);
                        }
                    }
                }
            } else {
                onFail(responseCode, duringTime);
            }
        } catch (SocketTimeoutException timeException) {
            onFail(ResultCode.REQUEST_TIME_OUT, duringTime);
        } catch (CanceledException cancelException) {
            if (tempFile != null && tempFile.exists()) {
                closeWriteStream(out);
                tempFile.delete();
            }
            return true;
        } catch (Exception ex) {
            OpLog.toFile(TAG, ex.getMessage());
            onFail(ResultCode.REQUEST_FAILED, duringTime, ex.getMessage());
        } finally {
            if (taskEndListener != null) {
                taskEndListener.onComplete();
            }
            if (connection != null) {
                connection.disconnect();
            }
            closeReadStream(in);
            closeWriteStream(out);
        }
        return true;
    }

    public void cancel() {
        cancel = true;
    }

    public interface TaskEndListener {
        void onComplete();
    }

}

package com.example.videoplaylist;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.videoplaylist.video.adapter.VideoListAdapter;
import com.example.videoplaylist.video.bean.VideoInfo;
import com.example.videoplaylist.video.listener.PlayWindowScrollerListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private VideoListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycle_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        ArrayList<VideoInfo> videoInfos = new ArrayList<>();
        for (int i=0;i<10;i++){
            VideoInfo info = new VideoInfo();
            info.setTitle("dss");
            info.setVideoUrl("http://img.locker.cmcm.com/livelock/uservideo/90f1353176bc83dffe2f246eba496c7a");
            videoInfos.add(info);
        }
        mAdapter = new VideoListAdapter(mRecyclerView);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setData(videoInfos);
        mAdapter.setLinearLayout(linearLayoutManager);
        mRecyclerView.addOnScrollListener(new PlayWindowScrollerListener(mAdapter));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.release();
        }
    }
}

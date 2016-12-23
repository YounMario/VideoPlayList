package com.example.videoplaylist;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;

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
            info.setDesc("des");
            info.setVideoUrl("http://clips.vorwaerts-gmbh.de/VfE_html5.mp4");
            videoInfos.add(info);
        }
        mAdapter = new VideoListAdapter(mRecyclerView);
        ((SimpleItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setData(videoInfos);
        mAdapter.setLinearLayout(linearLayoutManager);
        mRecyclerView.addOnScrollListener(new PlayWindowScrollerListener(mAdapter));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.play();
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

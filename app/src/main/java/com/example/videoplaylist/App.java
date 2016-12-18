package com.example.videoplaylist;

import android.app.Application;

/**
 * Created by 龙泉 on 2016/12/18.
 */

public class App extends Application{

    private static App instance;

    public App() {
        instance = this;
    }

    public static App getInstance(){
        return instance;
    }
}

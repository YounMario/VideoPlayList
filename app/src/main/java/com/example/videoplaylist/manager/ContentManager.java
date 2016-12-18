package com.example.videoplaylist.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.videoplaylist.App;

/**
 * Created by 龙泉 on 2016/12/18.
 */

public class ContentManager {

    private static ContentManager mContentManager;
    private SharedPreferences mShardPreferences;


    public static ContentManager getInstance() {
        if (mContentManager == null) {
            mContentManager = new ContentManager();
        }
        return mContentManager;
    }

    public ContentManager() {
        String spName = App.getInstance().getPackageName() + "_preferences";
        mShardPreferences = App.getInstance().getSharedPreferences(spName, Context.MODE_PRIVATE);
    }

    public void putLong(String key, long value) {
        SharedPreferences.Editor editor = mShardPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public void putInt(String key, int value) {
        SharedPreferences.Editor editor = mShardPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public void putString(String key, String value) {
        SharedPreferences.Editor editor = mShardPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void putFloat(String key, float value) {
        SharedPreferences.Editor editor = mShardPreferences.edit();
        editor.putFloat(key, value);
        editor.apply();
    }


}

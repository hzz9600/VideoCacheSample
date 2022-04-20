package com.danikula.videocache.preload;

import android.util.Log;

import com.danikula.videocache.BuildConfig;

public class PreloadLog {
    private static final String TAG = "PreloadLog";

    public static void info(String s) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, s);
        }
    }

    public static void debug(String s) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, s);
        }
    }
}

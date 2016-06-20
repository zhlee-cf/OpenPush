package com.im.openpush.utils;

import android.app.Application;
import android.content.Context;

/**
 * Created by lzh
 */
public class MyApp extends Application {

    public static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }
}

package com.example.usbeacon;

import android.app.Application;

/**
 * Created by 良柏 on 2018/3/14.
 */

public class THLApp extends Application {
    public static THLApp App		= null;
    public static THLConfig Config	= null;

    /** ================================================ */
    public static THLApp getApp()
    {
        return App;
    }

    /** ================================================ */
    @Override
    public void onCreate()
    {
        super.onCreate();

        App		= this;
        Config	= new THLConfig(this);

        Config.loadSettings();
    }

    /** ================================================ */
    @Override
    public void onTerminate()
    {
        Config.saveSettings();

        super.onTerminate();
    }
}

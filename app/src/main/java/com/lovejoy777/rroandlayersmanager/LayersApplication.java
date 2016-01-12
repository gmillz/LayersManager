package com.lovejoy777.rroandlayersmanager;

import android.app.Application;

import com.lovejoy777.rroandlayersmanager.helper.ThemeLoader;

public class LayersApplication extends Application {

    private ThemeLoader mThemeLoader;

    @Override
    public void onCreate() {
        super.onCreate();
        mThemeLoader = ThemeLoader.getInstance(getApplicationContext());
    }
}

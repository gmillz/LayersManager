package com.lovejoy777.rroandlayersmanager.helper;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;

import java.io.IOException;
import java.util.ArrayList;

public class Theme {

    public static final boolean SHOW_CM_THEMES = false;

    private Context mContext;
    private Context mThemeContext;

    private String mName;
    private String mDeveloper;
    private String mPackageName;
    private Drawable mIcon;

    private boolean mCMTETheme = false;

    // boot animation support
    private boolean mHasBootAnimations = false;
    private boolean mSingleBootAnimation = true;

    // wallpapers
    private boolean mHasWallpapers = false;
    private ArrayList<String> mWallpapers = new ArrayList<>();

    public Theme(Context context, String packageName, boolean isCMTheme) {
        mContext = context;

        mCMTETheme = isCMTheme;
        mPackageName = packageName;

        try {
            mThemeContext = context.createPackageContext(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        loadTheme();
    }

    public String getName() {
        return mName;
    }

    public String getDeveloper() {
        return mDeveloper;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setDeveloper(String dev) {
        mDeveloper = dev;
    }

    public boolean containsBootAnimations() {
        return mHasBootAnimations;
    }

    private void loadTheme() {
        AssetManager am = mThemeContext.getAssets();

        try {
            String[] assets = am.list("");

            for (String asset : assets) {
                if (asset.equals("bootanimation")) {
                    for (String b : am.list(asset)) {
                        if (am.list(b).length == 0) {
                            mSingleBootAnimation = true;
                        }
                    }
                    mHasBootAnimations = true;
                } else if (asset.equals("wallpaper")) {
                    for (String w : am.list("wallpaper")) {
                        mWallpapers.add(w);
                    }
                    if (mWallpapers.size() > 0) {
                        mHasWallpapers = true;
                    }
                }
            }
        } catch (IOException e) {
            //ignore
        }

        mIcon = mThemeContext.getApplicationInfo().loadIcon(mThemeContext.getPackageManager());
    }
}

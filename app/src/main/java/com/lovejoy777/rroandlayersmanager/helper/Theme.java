package com.lovejoy777.rroandlayersmanager.helper;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.lovejoy777.rroandlayersmanager.fragments.WallpaperFragment;
import com.lovejoy777.rroandlayersmanager.utils.IconUtils;
import com.lovejoy777.rroandlayersmanager.utils.ThemeUtils;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class Theme {

    public static final boolean SHOW_CM_THEMES = false;

    public static final String SYSTEM_THEME = "system";

    private Context mContext;
    private Context mThemeContext;

    private String mName;
    private String mDeveloper;
    private String mPackageName;
    private Drawable mIcon;

    private Point mDisplaySize = new Point();

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

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getSize(mDisplaySize);

        try {
            if (isSystemTheme()) {
                mThemeContext = mContext;
            } else {
                mThemeContext = context.createPackageContext(packageName, 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        loadTheme();
    }

    public boolean isSystemTheme() {
        return SYSTEM_THEME.equals(mPackageName);
    }

    public String getPackageName() {
        return mPackageName;
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

    public ArrayList<String> getWallpapers() {
        return mWallpapers;
    }

    public AssetManager getAssets() {
        return mThemeContext.getAssets();
    }

    public Bitmap getWallpaperFromAssets(String name) {
        try {
            if (isSystemTheme() && name.equals("default")) {
                WallpaperManager wm = WallpaperManager.getInstance(mContext);
                return IconUtils.drawableToBitmap(wm.getBuiltInDrawable());
            }
            Log.d("TEST", "wallpaper=" + name);
            AssetManager am = mThemeContext.getAssets();
            InputStream is = am.open(ThemeUtils.THEME_WALLPAPER_PATH + "/" + name);

            // Determine insample size
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opts);
            opts.inSampleSize = Utils.calculateInSampleSize(opts, mDisplaySize.x, mDisplaySize.y);
            is.close();

            // Decode the bitmap, regionally if neccessary
            is = am.open(ThemeUtils.THEME_WALLPAPER_PATH + "/" + name);
            opts.inJustDecodeBounds = false;
            Rect rect = Utils.getCropRectIfNecessary(opts, mDisplaySize.x, mDisplaySize.y);
            if (rect != null) {
                BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);
                // Check if we can downsample a little more now that we cropped
                opts.inSampleSize = Utils.calculateInSampleSize(rect.width(), rect.height(),
                        mDisplaySize.x, mDisplaySize.y);
                return decoder.decodeRegion(rect, opts);
            } else {
                return BitmapFactory.decodeStream(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public InputStream getBootAnimationStream(String name) {
        if (TextUtils.isEmpty(name)) {
            name = "bootanimation.zip";
        }
        Log.d("TEST", "getBootAnimationStream : " + mPackageName);
        if (isSystemTheme()) {
            Log.d("TEST", "isSystemTheme");
            if (ThemeUtils.STOCK_BOOT_BACKUP.exists()) {
                try {
                    return new FileInputStream(ThemeUtils.STOCK_BOOT_BACKUP);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    // ignore
                }
            }
            try {
                return new FileInputStream(new File(ThemeUtils.STOCK_BOOTANIMATION));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
        try {
            return mThemeContext.getAssets().open(ThemeUtils.THEME_BOOTANIMATION_PATH + "/" + name);
        } catch (IOException e) {
            return null;
        }
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
        if (isSystemTheme()) {
            mHasBootAnimations = true;
            mSingleBootAnimation = true;
            mHasWallpapers = true;
            mWallpapers.add("default");
            mName = "System";
            mIcon = mContext.getDrawable(android.R.drawable.sym_def_app_icon);
            return;
        }
        AssetManager am = mThemeContext.getAssets();

        try {
            String[] assets = am.list("");

            for (String asset : assets) {
                Log.d("TEST", asset);
                if (asset.equals(ThemeUtils.THEME_BOOTANIMATION_PATH)) {
                    Log.d("TEST", "has bootanimation");
                    for (String b : am.list(asset)) {
                        if (am.list(b).length == 0) {
                            mSingleBootAnimation = true;
                        }
                    }
                    mHasBootAnimations = true;
                } else if (asset.equals(ThemeUtils.THEME_WALLPAPER_PATH)) {
                    Log.d("TEST", "has wallpapers : " + mPackageName);
                    mWallpapers.addAll(Arrays.asList(am.list(ThemeUtils.THEME_WALLPAPER_PATH)));
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

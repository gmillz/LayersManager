package com.lovejoy777.rroandlayersmanager.activities;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.lovejoy777.rroandlayersmanager.R;
import com.lovejoy777.rroandlayersmanager.helper.Theme;
import com.lovejoy777.rroandlayersmanager.helper.ThemeLoader;
import com.lovejoy777.rroandlayersmanager.utils.ThemeUtils;
import com.lovejoy777.rroandlayersmanager.utils.ThemesContract;
import com.lovejoy777.rroandlayersmanager.views.BootAniImageView;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipFile;

public class BootAnimPreviewActivity extends Activity {

    private static final String TAG = BootAnimPreviewActivity.class.getName();

    private static final String CACHED_SUFFIX = "_bootanimation.zip";

    private BootAniImageView mBootAnimationPreview;
    private String mBootAnimationName;

    private Theme mTheme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.boot_animation_preview);

        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        mBootAnimationPreview = (BootAniImageView) findViewById(R.id.preview);

        String pkgName = getIntent().getStringExtra("packagename");
        mBootAnimationName = getIntent().getStringExtra("bootanimation");
        if (TextUtils.isEmpty(pkgName)) finish();

        List<Theme> themes = ThemeLoader.getInstance(this).getThemes();
        for (Theme theme : themes) {
            if (theme.getPackageName().equals(pkgName)) {
                mTheme = theme;
            }
        }

        new AnimationLoader(this, pkgName).execute();

        Button apply = (Button) findViewById(R.id.apply);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThemesContract contract = new ThemesContract();
                contract.setBootanimation(mTheme.getBootAnimationStream(mBootAnimationName));
                ThemeUtils.install(BootAnimPreviewActivity.this, contract);
                finish();
            }
        });

        Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    class AnimationLoader extends AsyncTask<Void, Void, ZipFile> {
        Context mContext;
        String mPkgName;

        public AnimationLoader(Context context, String pkgName) {
            mContext = context;
            mPkgName = pkgName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mBootAnimationPreview.setImageDrawable(null);
            //mLoadingProgress.setVisibility(View.VISIBLE);
            //mNoPreviewTextView.setVisibility(View.INVISIBLE);
        }

        @Override
        protected ZipFile doInBackground(Void... params) {
            if (mContext == null) {
                return null;
            }
            ZipFile zip;
            // check if the bootanimation is cached
            File f = new File(mContext.getCacheDir(), mPkgName + CACHED_SUFFIX);
            if (!f.exists()) {
                // go easy on cache storage and clear out any previous boot animations
                clearBootAnimationCache();
                try {
                    Context themeContext = mContext.createPackageContext(mPkgName, 0);
                    InputStream is = mTheme.getBootAnimationStream(mBootAnimationName);
                    //Utils.copyAsset(am, "bootanimation/bootanimation.zip", f.getAbsolutePath());
                    FileOutputStream fos = new FileOutputStream(f);
                    IOUtils.copy(is, fos);
                    fos.close();
                    //FileUtils.copyToFile(is, f);
                    is.close();
                } catch (Exception e) {
                    //Log.w(TAG, "Unable to load boot animation", e);
                    return null;
                }
            }
            try {
                zip = new ZipFile(f);
            } catch (IOException e) {
                //Log.w(TAG, "Unable to load boot animation", e);
                return null;
            }
            return zip;
        }

        @Override
        protected void onPostExecute(ZipFile zip) {
            super.onPostExecute(zip);
            if (zip == null) return;
            mBootAnimationPreview.setBootAnimation(zip);
            mBootAnimationPreview.start();
        }
    }

    private void clearBootAnimationCache() {
        File cache = getCacheDir();
        if (cache.exists()) {
            for(File f : cache.listFiles()) {
                // volley stores stuff in cache so don't delete the volley directory
                if(!f.isDirectory() && f.getName().endsWith(CACHED_SUFFIX)) {
                    if (!f.delete()) {
                        Log.e(TAG, "Can't delete " + f.getAbsolutePath());
                    }
                }
            }
        }
    }
}

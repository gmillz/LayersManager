package com.lovejoy777.rroandlayersmanager.installer;

import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.bitsyko.liblayers.layerfiles.LayerFile;
import com.lovejoy777.rroandlayersmanager.commands.Commands;
import com.lovejoy777.rroandlayersmanager.utils.ThemeUtils;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class BaseInstaller {

    protected Context mContext;

    protected ProgressDialog mProgessDialog;

    protected Executor mExecutor = Executors.newFixedThreadPool(4);

    public abstract void installOverlays(List<LayerFile> layers);

    public BaseInstaller(Context context) {
        mContext = context;

        mProgessDialog = new ProgressDialog(mContext);
        mProgessDialog.setTitle("Installing");
        mProgessDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgessDialog.setIndeterminate(true);
        mProgessDialog.setCancelable(false);
    }

    protected void setMax(int i) {
        mProgessDialog.setMax(i);
    }

    public void showProgressDialog() {
        mProgessDialog.show();
    }

    protected void updateProgress() {
        int progress = mProgessDialog.getProgress() + 1;
        mProgessDialog.setProgress(progress);
        if (mProgessDialog.getMax() == mProgessDialog.getProgress()) {
            hideProgressDialog();
            Commands.sendFinishedBroadcast(mContext);
        }
        mProgessDialog.setProgress(progress);
    }

    public void hideProgressDialog() {
        mProgessDialog.hide();
    }

    public void installBootAnimation(final InputStream is) {
        new AsyncTask<Void, Void, Void>() {
            protected void onPreExecute() {
                mProgessDialog.show();
            }
            protected Void doInBackground(Void... v) {
                ThemeUtils.backupStockBoot();
                File temp = new File(mContext.getCacheDir() + "/temp/bootanimation.zip");
                try {
                    ThemeUtils.copyAndScaleBootAnimation(mContext, is, temp.getAbsolutePath());
                } catch (IOException e) {
                    return null;
                }
                Utils.remount("rw", "/system");
                Utils.copyFile(temp.getAbsolutePath(), ThemeUtils.STOCK_BOOTANIMATION);
                Utils.remount("ro", "/system");

                return null;
            }
            protected void onPostExecute(Void v) {
                mProgessDialog.hide();
            }
        }.execute();
    }

    public void installWallpaper(Bitmap bitmap) {
        WallpaperManager manager = WallpaperManager.getInstance(mContext);
        try {
            manager.setBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

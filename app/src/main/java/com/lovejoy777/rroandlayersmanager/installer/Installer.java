package com.lovejoy777.rroandlayersmanager.installer;

import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.AsyncTask;

import com.bitsyko.liblayers.layerfiles.LayerFile;
import com.lovejoy777.rroandlayersmanager.AsyncResponse;
import com.lovejoy777.rroandlayersmanager.commands.Commands;
import com.lovejoy777.rroandlayersmanager.utils.ThemeUtils;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Installer extends BaseInstaller {

    public Installer(Context context) {
        super(context);
    }

    @Override
    public void installOverlays(List<LayerFile> layers) {
        new Commands.InstallOverlaysBetterWay(layers, mContext).execute();
    }

    public void installWallpaperFromAssets(final AssetManager am,
                                           final String name, final int x, final int y) {
        new AsyncTask<Void, Void, Bitmap>() {
            protected void onPreExecute() {
                mProgessDialog.show();
            }
            protected Bitmap doInBackground(Void... v) {
                try {
                    InputStream is =
                            am.open(ThemeUtils.THEME_WALLPAPER_PATH + File.separator + name);

                    // Determine insample size
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(is, null, opts);
                    opts.inSampleSize = Utils.calculateInSampleSize(opts, x, y);
                    is.close();

                    // Decode the bitmap, regionally if neccessary
                    is = am.open(ThemeUtils.THEME_WALLPAPER_PATH + "/" + name);
                    opts.inJustDecodeBounds = false;
                    Rect rect = Utils.getCropRectIfNecessary(opts, x, y);
                    if (rect != null) {
                        BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);
                        // Check if we can downsample a little more now that we cropped
                        opts.inSampleSize = Utils.calculateInSampleSize(rect.width(), rect.height(),
                                x, y);
                        return decoder.decodeRegion(rect, opts);
                    } else {
                        return BitmapFactory.decodeStream(is);
                    }
                } catch (IOException e) {
                    // ignore
                    return null;
                }
            }

            protected void onPostExecute(Bitmap b) {
                installWallpaper(b);
                mProgessDialog.dismiss();
            }
        }.execute();
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
}

package com.lovejoy777.rroandlayersmanager.utils;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.util.Log;

import com.bitsyko.libicons.AppIcon;
import com.bitsyko.libicons.IconPack;
import com.lovejoy777.rroandlayersmanager.DeviceSingleton;
import com.lovejoy777.rroandlayersmanager.R;
import com.lovejoy777.rroandlayersmanager.fragments.IconFragment;
import com.lovejoy777.rroandlayersmanager.overlaycreator.Overlay;

import org.adw.launcher.IconShader;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class IconUtils {

    public interface Callback {
        void onInstallFinish();
    }

    private static ProgressDialog mProgressDialog;
    private static Executor mExecutor = Executors.newFixedThreadPool(4);
    private static Callback sCallback;

    private static boolean sInstalling = false;

    public static boolean isInstalling() {
        return sInstalling;
    }

    public static void setInstalling(boolean installing) {
        sInstalling = installing;
    }

    public static void installIcons(Context context, List<AppIcon> list, Callback callback) {
        if (callback != null) sCallback = callback;
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setTitle(R.string.installingOverlays);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setProgress(0);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMax(list.size());
        mProgressDialog.show();
        Utils.remount("rw");
        for (AppIcon app : list) {
            new InstallAppIcon(app).executeOnExecutor(mExecutor);
        }
    }

    private static void progressUpdate() {
        if (mProgressDialog != null) {
            int progress = mProgressDialog.getProgress() + 1;
            mProgressDialog.setProgress(progress);
            if (mProgressDialog.getMax() == mProgressDialog.getProgress()) {
                Utils.remount("ro");
                mProgressDialog.dismiss();
                if (sCallback != null) sCallback.onInstallFinish();
            }
        }
    }

    public static class InstallAppIcon extends AsyncTask<Void, String, Void> {
        private AppIcon appIcon;

        public InstallAppIcon(AppIcon appIcon) {
            this.appIcon = appIcon;
        }

        @Override
        protected Void doInBackground(Void... a) {
            try {
                if (appIcon.getUseDefault()) {
                    File apk = appIcon.overlay.getVendorApp();
                    if (apk.exists()) {
                        if (!apk.delete()) {
                            throw new RuntimeException("Cannot delete apk");
                        }
                    }
                } else if (appIcon.mCustomBitmap != null) {
                    appIcon.install();
                    appIcon.overlay.createAndInstall();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Void v) {
            progressUpdate();
        }
    }

    public static Bitmap createIconBitmap(Drawable icon, Context context,
                                          IconPack iconPack) {
        final Canvas canvas = new Canvas();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final int iconBitmapSize = am.getLauncherLargeIconSize();

        Drawable iconMask = null;
        Drawable iconBack = null;
        Drawable iconPaletteBack = null;
        Drawable iconUpon = null;
        float scale = 1f;
        float angle = 0;
        float translationX = 0;
        float translationY = 0;
        int defaultSwatchColor = 0;
        int backTintColor = 0;
        IconPack.SwatchType swatchType = IconPack.SwatchType.None;
        float[] colorFilter = null;

        if (iconPack != null) {
            iconMask = iconPack.getIconMask();
            iconBack = iconPack.getIconBack();
            iconPaletteBack = iconPack.getIconPaletteBack();
            iconUpon = iconPack.getIconUpon();
            scale = iconPack.getIconScale();
            angle = iconPack.getIconAngle();
            translationX = iconPack.getTranslationX();
            translationY = iconPack.getTranslationY();
            swatchType = iconPack.getSwatchType();
            colorFilter = iconPack.getColorFilter();
            defaultSwatchColor = iconPack.getDefaultSwatchColor();
        }

        Log.d("TEST", "iconUpon is null ? " + String.valueOf(iconUpon == null));
        Log.d("TEST", "iconMask is null ? " + String.valueOf(iconMask == null));
        Log.d("TEST", "iconSize == " + iconPack.getIconSize());

        icon = resize(context.getResources(), icon, iconPack.getIconSize());
        icon = shadeIcon(context.getResources(), icon, iconPack.getIconShader());

        int width = iconBitmapSize;
        int height = iconBitmapSize;

        if (icon instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) icon;
            painter.setIntrinsicWidth(width);
            painter.setIntrinsicHeight(height);
        } else if (icon instanceof BitmapDrawable) {
            // Ensure the bitmap has a density.
            BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
            }

            if (swatchType != null && swatchType != IconPack.SwatchType.None) {
                Palette palette = Palette.from(bitmap).generate();
                switch (swatchType) {
                    case Vibrant:
                        backTintColor = palette.getVibrantColor(defaultSwatchColor);
                        break;
                    case VibrantLight:
                        backTintColor = palette.getLightVibrantColor(defaultSwatchColor);
                        break;
                    case VibrantDark:
                        backTintColor = palette.getDarkVibrantColor(defaultSwatchColor);
                        break;
                    case Muted:
                        backTintColor = palette.getMutedColor(defaultSwatchColor);
                        break;
                    case MutedLight:
                        backTintColor = palette.getLightMutedColor(defaultSwatchColor);
                        break;
                    case MutedDark:
                        backTintColor = palette.getDarkMutedColor(defaultSwatchColor);
                        break;
                }
            }
        }
        int sourceWidth = icon.getIntrinsicWidth();
        int sourceHeight = icon.getIntrinsicHeight();
        if (sourceWidth > 0 && sourceHeight > 0) {
            // Scale the icon proportionally to the icon dimensions
            final float ratio = (float) sourceWidth / sourceHeight;
            if (sourceWidth > sourceHeight) {
                height = (int) (width / ratio);
            } else if (sourceHeight > sourceWidth) {
                width = (int) (height * ratio);
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(iconBitmapSize, iconBitmapSize,
                Bitmap.Config.ARGB_8888);

        canvas.setBitmap(bitmap);

        // Scale the original
        Rect oldBounds = new Rect();
        oldBounds.set(icon.getBounds());
        icon.setBounds(0, 0, width, height);
        canvas.save();
        final float halfWidth = width / 2f;
        final float halfHeight = height / 2f;
        canvas.scale(scale, scale, halfWidth, halfHeight);
        canvas.translate(translationX, translationY);
        canvas.rotate(angle, halfWidth, halfHeight);
        if (colorFilter != null) {
            Paint p = null;
            if (icon instanceof BitmapDrawable) {
                p = ((BitmapDrawable) icon).getPaint();
            } else if (icon instanceof PaintDrawable) {
                p = ((PaintDrawable) icon).getPaint();
            }
            if (p != null) {
                p.setColorFilter(new ColorMatrixColorFilter(colorFilter));
            }
        }
        icon.draw(canvas);
        canvas.restore();
        if (iconMask != null) {
            iconMask.setBounds(icon.getBounds());
            ((BitmapDrawable) iconMask).getPaint().setXfermode(
                    new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            iconMask.draw(canvas);
        }
        Drawable back = null;
        if (swatchType != null && swatchType != IconPack.SwatchType.None) {
            back = iconPaletteBack;
        } else if (iconBack != null) {
            back = iconBack;
        }
        if (back != null) {
            back.setBounds(icon.getBounds());
            Paint paint = ((BitmapDrawable) back).getPaint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
            if (backTintColor != 0) {
                paint.setColorFilter(new PorterDuffColorFilter(backTintColor,
                        PorterDuff.Mode.MULTIPLY));
            }
            back.draw(canvas);
        }
        if (iconUpon != null) {
            iconUpon.setBounds(icon.getBounds());
            iconUpon.draw(canvas);
        }
        icon.setBounds(oldBounds);
        bitmap.setDensity(canvas.getDensity());

        return bitmap;
    }

    public static Drawable shadeIcon(Resources res,
                                     Drawable drawable, IconShader.CompiledIconShader shader) {
        if (shader == null) return drawable;
        Bitmap b = drawableToBitmap(drawable);
        b = IconShader.processIcon(b, shader);
        Log.d("TEST", "b == null ? " + String.valueOf(b == null));
        return new BitmapDrawable(res, b);
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static ArrayList<IconFragment.Item> getAllIcons(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
        ArrayList<IconFragment.Item> items = new ArrayList<>();
        for (ResolveInfo info : apps) {
            items.add(new IconFragment.Item(context, info));
        }
        return items;
    }

    public static float dpiFromPx(int size, DisplayMetrics metrics) {
        float densityRatio = (float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT;
        return (size / densityRatio);
    }

    public static Drawable resize(Resources res, Drawable drawable, int size) {
        Bitmap b = drawableToBitmap(drawable);
        Bitmap bitmapResize = Bitmap.createScaledBitmap(b, size, size, false);
        return new BitmapDrawable(res, bitmapResize);
    }

    public static Bitmap resize(Bitmap b, int size) {
        return Bitmap.createScaledBitmap(b, size, size, false);
    }

    public static void saveBitmapForActivityInfo(
            Context context, ActivityInfo aInfo, Bitmap bitmap) {
        try {
            Resources appResources =
                    context.getPackageManager().getResourcesForApplication(aInfo.applicationInfo);

            Overlay overlay = new Overlay(context, aInfo.packageName);

            String name = appResources.getResourceName(aInfo.getIconResource());
            String f = StringUtils.substringAfter(StringUtils.substringBefore(name, "/"), ":");
            String n = StringUtils.substringAfter(name, "/");

            for (String den : Utils.getDensityBuckets()) {
                String location = f + "-" + den + "/" + n;

                File destFile = new File(overlay.getResDir() + File.separator
                        + location + ".png");
                if (!destFile.getParentFile().exists() && !destFile.getParentFile().mkdirs()) {
                    throw new RuntimeException("cannot create directory");
                }

                FileOutputStream out = new FileOutputStream(destFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void putIconInCache(Context context, String packageName, Drawable d) {
        Bitmap b = drawableToBitmap(d);
        try {
            File f = new File(context.getFilesDir()
                    + File.separator + "icons" + File.separator + packageName + ".png");
            if (!f.getParentFile().exists()) {
                if (!f.getParentFile().mkdirs()) {
                    throw new RuntimeException();
                }
            }
            FileOutputStream out = new FileOutputStream(f);
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public static Drawable getIconFromCache(Context context, String packageName) {
        String file = context.getFilesDir().getAbsolutePath()
                + File.separator + "icons" + File.separator + packageName + ".png";
        Bitmap b = BitmapFactory.decodeFile(file);
        return new BitmapDrawable(context.getResources(), b);
    }

    public static void uninstallIcons(Context context) {
        String d = DeviceSingleton.getInstance().getOverlayFolder();
        File overlay = new File(d);
        Utils.remount("rw");
        for (File file : overlay.listFiles()) {
            if (file.getName().contains("icon")) {
                Utils.deleteFile(file.getAbsolutePath());
            }
        }
        Utils.remount("ro");
    }
}

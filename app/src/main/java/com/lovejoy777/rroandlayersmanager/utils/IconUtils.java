package com.lovejoy777.rroandlayersmanager.utils;

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

import com.bitsyko.libicons.AppIcon;
import com.bitsyko.libicons.IconPack;
import com.lovejoy777.rroandlayersmanager.R;
import com.lovejoy777.rroandlayersmanager.fragments.IconFragment;
import com.lovejoy777.rroandlayersmanager.overlaycreator.Overlay;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class IconUtils {

    private static final Canvas sCanvas = new Canvas();
    private static final Rect sOldBounds = new Rect();

    public interface Callback {
        void onInstallFinish();
    }

    private static ProgressDialog mProgressDialog;
    private static Executor mExecutor = Executors.newFixedThreadPool(8);
    private static Callback sCallback;

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
        Utils.remount("ro");
    }

    private static void progressUpdate() {
        if (mProgressDialog != null) {
            int progress = mProgressDialog.getProgress() + 1;
            mProgressDialog.setProgress(progress);
            if (mProgressDialog.getMax() == mProgressDialog.getProgress()) {
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
                        apk.delete();
                    }
                } else {
                    //appIcon.install();
                    appIcon.overlay.create();
                    appIcon.overlay.install();
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
        synchronized (sCanvas) {
            final int iconBitmapSize = icon.getMinimumWidth();

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
            }

            int width = icon.getIntrinsicWidth();
            int height = icon.getIntrinsicHeight();

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

            // no intrinsic size --> use default size
            int textureWidth = iconBitmapSize;
            int textureHeight = iconBitmapSize;

            Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);

            final int left = (textureWidth - width) / 2;
            final int top = (textureHeight - height) / 2;

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

            sOldBounds.set(icon.getBounds());
            icon.setBounds(0, 0, width, height);
            canvas.save();
            final float halfWidth = width / 2f;
            final float halfHeight = height / 2f;
            canvas.rotate(angle, halfWidth, halfHeight);
            canvas.scale(scale, scale, halfWidth, halfHeight);
            canvas.translate(translationX, translationY);
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
                defaultSwatchColor = iconPack.getDefaultSwatchColor();
            } else if (iconBack != null) {
                back = iconBack;
            }
            if (back != null) {
                canvas.setBitmap(null);
                Bitmap finalBitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                        Bitmap.Config.ARGB_8888);
                canvas.setBitmap(finalBitmap);
                back.setBounds(icon.getBounds());
                Paint paint = ((BitmapDrawable) back).getPaint();
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
                if (backTintColor != 0) {
                    paint.setColorFilter(new PorterDuffColorFilter(backTintColor,
                            PorterDuff.Mode.MULTIPLY));
                }
                back.draw(canvas);
                canvas.drawBitmap(bitmap, null, icon.getBounds(), null);
                bitmap.recycle();
                bitmap = finalBitmap.copy(Bitmap.Config.ARGB_8888,true);
                finalBitmap.recycle();
            }
            if (iconUpon != null) {
                iconUpon.draw(canvas);
            }
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);

            return bitmap;
        }
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

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

    public static void saveBitmapForActivityInfo(
            Context context, ActivityInfo aInfo, Bitmap bitmap) {
        try {
            List<String> list = getApplicationIcons(context, aInfo.applicationInfo);

            Resources appResources =
                    context.getPackageManager().getResourcesForApplication(aInfo.applicationInfo);

            Overlay overlay = new Overlay(context, aInfo.packageName);

            String drawableName = StringUtils.substringAfter(
                    appResources.getResourceName(aInfo.getIconResource()), "/");

            if (list.isEmpty()) {
                throw new RuntimeException("No application icon");
            }

            List<String> iconLocation = new ArrayList<>();

            for (String string : list) {
                iconLocation.add(new File(string).getParent());
            }

            for (String location : iconLocation) {
                File destFile = new File(overlay.path + File.separator
                        + location + File.separator + drawableName + ".png");
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

    private static List<String> getApplicationIcons(Context context, ApplicationInfo applicationInfo)
            throws IOException, InterruptedException {

        String apkLocation = applicationInfo.sourceDir;
        File appt = new File(context.getCacheDir() + "/aapt");

        Utils.CommandOutput commandOutput = Utils.runCommand(appt.getAbsolutePath() + " dump badging " +
                apkLocation, false);

        if (commandOutput == null || !StringUtils.isEmpty(commandOutput.error)) {
            throw new RuntimeException(commandOutput == null ? "error" : commandOutput.error);
        }


        String[] lines = commandOutput.output.split(System.getProperty("line.separator"));

        List<String> list = new ArrayList<>();

        for (String string : lines) {
            if (string.contains("application-icon-")) {
                list.add(StringUtils.substringBetween(string, "'"));
            }
        }

        return list;
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
}

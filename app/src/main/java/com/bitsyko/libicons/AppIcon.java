package com.bitsyko.libicons;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.Pair;

import com.lovejoy777.rroandlayersmanager.overlaycreator.Overlay;
import com.lovejoy777.rroandlayersmanager.utils.IconUtils;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import org.adw.launcher.IconShader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppIcon {

    private static final String TAG = "AppIcon";

    public Overlay overlay;
    private String mIcon;
    private ApplicationInfo applicationInfo;
    private Resources mAppResources;
    private Context context;
    private IconPackHelper iconPack;
    private boolean inPack;

    public AppIcon(Context context, ComponentName cmp, IconPackHelper iconPack, boolean inPack) throws PackageManager.NameNotFoundException {
        this.context = context;
        this.applicationInfo = context.getPackageManager().getApplicationInfo(cmp.getPackageName(), PackageManager.GET_ACTIVITIES);
        mAppResources = context.getPackageManager().getResourcesForApplication(applicationInfo);
        this.iconPack = iconPack;
        this.inPack = inPack;
        overlay = new Overlay(context, cmp.getPackageName());
    }

    public AppIcon(Context context, ComponentName cmp, IconPackHelper iconPack, boolean inPack, String icon) throws PackageManager.NameNotFoundException {
        this(context, cmp, iconPack, inPack);
        mIcon = icon;
    }

    public String getPackageName() {
        return applicationInfo.packageName;
    }

    public String getName() {
        return String.valueOf(applicationInfo.loadLabel(context.getPackageManager()));
    }


    public void install() throws Exception {
        if (inPack) {
            //Icon is in pack
            Log.d("InPack", applicationInfo.packageName);
            installInPack();
        } else {
            //Icon isn't in pack (apply masks and backs)
            Log.d("NotInPack", applicationInfo.packageName);
            installNotInPack();
        }
    }

    public void installNotInPack() throws Exception {


        List<String> list = getApplicationIcons();

        if (list.isEmpty()) {
            throw new RuntimeException("No application icon");
        }

        List<String> iconLocation = new ArrayList<>();

        for (String string : list) {
            iconLocation.add(new File(string).getParent());
        }

        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(applicationInfo.packageName,
                PackageManager.GET_ACTIVITIES);

        for (android.content.pm.ActivityInfo a : packageInfo.activities) {
            String drawableName = StringUtils.substringAfter(mAppResources.getResourceName(a.getIconResource()), "/");
            Drawable iconDrawable = mAppResources.getDrawable(a.getIconResource(), null);

            Bitmap icon = IconUtils.createIconBitmap(iconDrawable, context, iconPack);

            for (String location : iconLocation) {
                Log.d("TEST", "location=" + location);
                File destFile = new File(overlay.path + "/" + location + "/" + drawableName + ".png");

                destFile.getParentFile().mkdirs();

                FileOutputStream out = new FileOutputStream(destFile);
                icon.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
            }
        }

        overlay.create();
    }

    public void installInPack() throws Exception {
        List<String> list = getApplicationIcons();

        if (list.isEmpty()) {
            throw new RuntimeException("No application icon");
        }

        String appIcon = new File(list.get(0)).getName().replace(".png", "");

        List<String> iconLocation = new ArrayList<>();

        for (String string : list) {
            iconLocation.add(new File(string).getParent());
        }

        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(getPackageName(),
                PackageManager.GET_ACTIVITIES);


        Map<String, String> activitiesWithIcons = new HashMap<>();

        Resources appResources = context.getPackageManager().getResourcesForApplication(getPackageName());
        Resources iconPackResources = context.getPackageManager().getResourcesForApplication(iconPack.getPackageName());

        if (packageInfo.activities == null || packageInfo.activities.length == 0) {
            Log.e(getPackageName(), "No activities");
            return;
        }

        for (android.content.pm.ActivityInfo a : packageInfo.activities) {
            Log.d("TEST", "iconResource=" + appResources.getResourceName(a.getIconResource()));
            activitiesWithIcons.put(a.name, StringUtils.substringAfter(appResources.getResourceName(a.getIconResource()), "/"));
        }

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(getPackageName());
        ActivityInfo aInfo = context.getPackageManager().getActivityInfo(intent.getComponent(), 0);


        Log.d("App Icon", appIcon);
        Log.d("Icon locations", String.valueOf(iconLocation));
        Log.d("Activities", String.valueOf(activitiesWithIcons));

        int iconId = iconPack.getResourceIdForActivityIcon(aInfo);

            BitmapDrawable icon = null;

            try {
                icon = (BitmapDrawable) iconPackResources.getDrawable(iconId, null);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Vector", "Not supported");
            }

            if (icon == null) {
                Log.d("Missing resource", mIcon);
            }


            for (String location : iconLocation) {

                File destFile = new File(overlay.path + "/" + location + "/" + appIcon + ".png");

                if (!destFile.getParentFile().exists()) {
                    if (!destFile.getParentFile().mkdirs()) {
                        Log.d(TAG, "cannot create: " + destFile.getAbsolutePath());
                    }
                }

                if (icon != null) {
                    FileOutputStream out = new FileOutputStream(destFile);
                    icon.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.close();
                }

            }

        if (!(new File(context.getCacheDir() + "/tempFolder/"
                + getPackageName() + "/" + list.get(0)).exists())) {
            Log.d("Fallback for: ", getPackageName());
            installNotInPack();
        } else {
            overlay.create();
        }

    }

    private List<String> getApplicationIcons() throws IOException, InterruptedException {

        String apkLocation = applicationInfo.sourceDir;
        File appt = new File(context.getCacheDir() + "/aapt");

        Process nativeApp = Runtime.getRuntime().exec(new String[]{
                appt.getAbsolutePath(),
                "dump", "badging",
                apkLocation});

        String data = IOUtils.toString(nativeApp.getInputStream());
        String error = IOUtils.toString(nativeApp.getErrorStream());

        nativeApp.waitFor();

        if (!StringUtils.isEmpty(error)) {
            throw new RuntimeException(error);
        }


        String[] lines = data.split(System.getProperty("line.separator"));

        List<String> list = new ArrayList<>();

        for (String string : lines) {
            if (string.contains("application-icon-")) {
                list.add(StringUtils.substringBetween(string, "'"));
            }
        }

        return list;

    }


    public boolean isInPack() {
        return inPack;
    }
}

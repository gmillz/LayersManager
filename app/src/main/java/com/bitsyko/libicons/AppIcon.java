package com.bitsyko.libicons;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;

import com.lovejoy777.rroandlayersmanager.overlaycreator.Overlay;
import com.lovejoy777.rroandlayersmanager.utils.IconUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AppIcon {

    private static final String TAG = "AppIcon";

    public Overlay overlay;
    private ApplicationInfo applicationInfo;
    private Resources mAppResources;
    private Context context;
    private IconPackHelper iconPack;
    private boolean inPack;

    public AppIcon(Context context, ComponentName cmp, IconPackHelper iconPack, boolean inPack)
            throws PackageManager.NameNotFoundException {
        this.context = context;
        this.applicationInfo = context.getPackageManager().getApplicationInfo(
                cmp.getPackageName(), PackageManager.GET_ACTIVITIES);
        mAppResources = context.getPackageManager().getResourcesForApplication(applicationInfo);
        this.iconPack = iconPack;
        this.inPack = inPack;
        overlay = new Overlay(context, cmp.getPackageName());
    }

    public String getPackageName() {
        return applicationInfo.packageName;
    }

    public String getName() {
        return String.valueOf(applicationInfo.loadLabel(context.getPackageManager()));
    }


    public void install() throws Exception {

        List<String> list = getApplicationIcons();

        if (list.isEmpty()) {
            throw new RuntimeException("No application icon");
        }

        List<String> iconLocation = new ArrayList<>();

        for (String string : list) {
            iconLocation.add(new File(string).getParent());
        }

        PackageInfo packageInfo =
                context.getPackageManager().getPackageInfo(
                        applicationInfo.packageName, PackageManager.GET_ACTIVITIES);

        for (ActivityInfo aInfo : packageInfo.activities) {
            String drawableName = StringUtils.substringAfter(
                    mAppResources.getResourceName(aInfo.getIconResource()), "/");

            Bitmap icon;

            int iconId = iconPack.getResourceIdForActivityIcon(aInfo);
            if (iconId == 0) {
                Log.d("TEST", "ICON NOT IN PACK = " + aInfo.toString());
                icon = IconUtils.createIconBitmap(
                        mAppResources.getDrawable(
                                aInfo.getIconResource(), null), context, iconPack);
            } else {
                Log.d("TEST", "ICON IN PACK = " + aInfo.toString());
                icon = IconUtils.drawableToBitmap(
                        iconPack.getIconPackResources().getDrawable(iconId, null));
            }

            for (String location : iconLocation) {
                File destFile = new File(overlay.path + File.separator
                        + location + File.separator + drawableName + ".png");
                if (!destFile.getParentFile().exists() && !destFile.getParentFile().mkdirs()) {
                    throw new RuntimeException("cannot create directory");
                }

                FileOutputStream out = new FileOutputStream(destFile);
                icon.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
            }
        }
        overlay.create();
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

package com.bitsyko.libicons;

import android.content.Context;
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

import org.adw.launcher.*;

public class AppIcon {

    private ApplicationInfo applicationInfo;
    private Resources res;
    private Context context;
    private IconPack iconPack;
    public Overlay overlay;
    private boolean inPack;

    //ClassName:Drawable
    List<Pair<String, String>> iconList = new ArrayList<>();

    public AppIcon(Context context, String packageName, IconPack iconPack, boolean inPack) throws PackageManager.NameNotFoundException {
        this.context = context;
        this.applicationInfo = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
        this.res = context.getPackageManager().getResourcesForApplication(applicationInfo);
        this.iconPack = iconPack;
        this.inPack = inPack;
        overlay = new Overlay(context, packageName);
    }

    public AppIcon(Context context, String packageName, IconPack iconPack, boolean inPack, Collection<Pair<String, String>> iconList) throws PackageManager.NameNotFoundException {
        this(context, packageName, iconPack, inPack);
        this.iconList.addAll(iconList);
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


        Map<String, Collection<String>> installedAppsAndTheirLauncherActivities =
                SystemApplicationHelper.getInstance(context).getInstalledAppsAndTheirLauncherActivities();


        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(applicationInfo.packageName,
                PackageManager.GET_ACTIVITIES);

        Collection<String> launcherActivities = installedAppsAndTheirLauncherActivities.get(applicationInfo.packageName);

        Map<String, String> iconOverlaysData = iconPack.getIconOverlaysData();


        for (android.content.pm.ActivityInfo a : packageInfo.activities) {

            if (!(res.getDrawable(a.getIconResource(), null) instanceof BitmapDrawable)) {
                continue;
            }

            if (!launcherActivities.contains(a.name)) {
                continue;
            }

            String drawableName = StringUtils.substringAfter(res.getResourceName(a.getIconResource()), "/");
            //Bitmap icon = ((BitmapDrawable) res.getDrawable(a.getIconResource(), null)).getBitmap().copy(Bitmap.Config.ARGB_8888, true);

            Drawable iconDrawable = res.getDrawable(a.getIconResource(), null);

            XmlPullParser shaders = iconPack.getShader();


            IconShader.CompiledIconShader compiledIconShader = IconShader.parseXml(shaders);

            Bitmap icon = ((BitmapDrawable) (iconDrawable)).getBitmap();

            icon = Bitmap.createScaledBitmap(icon, 72, 72, true);

            icon = IconShader.processIcon(icon, compiledIconShader);

            String scale = iconOverlaysData.get("scale");

            Bitmap backPicture = iconPack.getBitmapFromDrawable(iconOverlaysData.get("iconback"));
            Bitmap uponPicture = iconPack.getBitmapFromDrawable(iconOverlaysData.get("iconupon"));
            Bitmap maskPicture = iconPack.getBitmapFromDrawable(iconOverlaysData.get("iconmask"));

            icon = icon.copy(Bitmap.Config.ARGB_8888, true);

            //Scale down picture


            int destSize = (int) (backPicture.getWidth() * Float.parseFloat(scale));

            Bitmap bitmap2scaled = Bitmap.createScaledBitmap(icon, destSize, destSize, false);

            Bitmap bitmap2moved = Bitmap.createBitmap(backPicture.getWidth(), backPicture.getHeight(), backPicture.getConfig());
            Canvas canvas = new Canvas(bitmap2moved);


            int margin = (backPicture.getWidth() - bitmap2scaled.getWidth()) / 2;
            canvas.drawBitmap(bitmap2scaled, margin, margin, null);

            icon = bitmap2moved;

            if (maskPicture != null) {
                icon = Bitmap.createScaledBitmap(icon, maskPicture.getWidth(), maskPicture.getHeight(), true);
                icon = overlay(icon, maskPicture);
                icon = icon.copy(Bitmap.Config.ARGB_8888, true);
            } else {
                Log.d("No picture", "Mask");
            }


            //Mask
            for (int x = 0; x < icon.getWidth(); x++) {
                for (int y = 0; y < icon.getHeight(); y++) {

                    int color = icon.getPixel(x, y);

                    int r = Color.red(color);
                    int g = Color.green(color);
                    int b = Color.blue(color);


                    if (r == 0 && g == 0 && b == 0) {
                        icon.setPixel(x, y, Color.argb(0, 0, 0, 0));
                    }


                }
            }

            icon = overlay(backPicture, icon).copy(Bitmap.Config.ARGB_8888, true);

            //Clear rest of icon
            for (int x = 0; x < icon.getWidth(); x++) {
                for (int y = 0; y < icon.getHeight(); y++) {

                    int alpha = Color.alpha(backPicture.getPixel(x, y));

                    if (alpha == 0) {
                        icon.setPixel(x, y, Color.argb(0, 0, 0, 0));
                    }


                }
            }


            if (uponPicture != null) {
                icon = overlay(icon, uponPicture);
            } else {
                Log.d("No picture", "Upon");
            }


            // backPicture;

            for (String location : iconLocation) {

                File destFile = new File(context.getCacheDir() + "/tempFolder/" + applicationInfo.packageName + "/" + location + "/" + drawableName + ".png");

                destFile.getParentFile().mkdirs();

                FileOutputStream out = new FileOutputStream(destFile);
                icon.compress(Bitmap.CompressFormat.PNG, 100, out);

                out.close();

            }


        }

        overlay.create();
    }

    private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        return bmOverlay;
    }


    public void installInPack() throws Exception {

        //Assumption: Icons are in the same folder as launcher icon


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
            activitiesWithIcons.put(a.name, StringUtils.substringAfter(appResources.getResourceName(a.getIconResource()), "/"));
        }


        Log.d("App Icon", appIcon);
        Log.d("Icon locations", String.valueOf(iconLocation));
        Log.d("Activities", String.valueOf(activitiesWithIcons));


        for (Pair<String, String> activityWithIcon : iconList) {

            if (!activitiesWithIcons.keySet().contains(activityWithIcon.first)) {
                continue;
            }

            String iconName = activitiesWithIcons.get(activityWithIcon.first);

            int drawableIconID = iconPackResources.getIdentifier(activityWithIcon.second, "drawable", iconPack.getPackageName());
            int mipmapIconID = iconPackResources.getIdentifier(activityWithIcon.second, "mipmap", iconPack.getPackageName());

            if (drawableIconID == 0 && mipmapIconID == 0) {
                Log.e("No icon in iconpack", activityWithIcon.second);
                continue;
            }

            int finalIconID = drawableIconID == 0 ? mipmapIconID : drawableIconID;

            BitmapDrawable icon;

            try {
                icon = (BitmapDrawable) iconPackResources.getDrawable(finalIconID, null);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("No icon for", activityWithIcon.first + " " + activityWithIcon.second);
                Log.e("Vector", "Not supported");
                continue;
            }

            if (icon == null) {
                Log.d("Missing resource", activityWithIcon.second);
                continue;
            }


            for (String location : iconLocation) {

                File destFile = new File(context.getCacheDir() + "/tempFolder/" + getPackageName() + "/" + location + "/" + iconName + ".png");

                destFile.getParentFile().mkdirs();

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

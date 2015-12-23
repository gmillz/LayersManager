package com.bitsyko.libicons;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.lovejoy777.rroandlayersmanager.overlaycreator.Overlay;
import com.lovejoy777.rroandlayersmanager.utils.IconUtils;

public class AppIcon {

    public Overlay overlay;
    private ApplicationInfo applicationInfo;
    private Context context;
    private ComponentName cmp;
    private Resources mAppResources;
    private boolean useDefault = false;

    public AppIcon(Context context, ComponentName cmp) throws PackageManager.NameNotFoundException {
        this.context = context;
        this.applicationInfo = context.getPackageManager().getApplicationInfo(
                cmp.getPackageName(), PackageManager.GET_ACTIVITIES);
        overlay = new Overlay(context, cmp.getPackageName());
        this.cmp = cmp;
        mAppResources = context.getPackageManager().getResourcesForApplication(applicationInfo);
    }

    public String getPackageName() {
        return applicationInfo.packageName;
    }

    public String getName() {
        return String.valueOf(applicationInfo.loadLabel(context.getPackageManager()));
    }

    public ComponentName getComponentName() {
        return cmp;
    }

    public boolean hasOverlay() {
        return overlay.getVendorApp().exists();
    }

    public Drawable getDefaultIcon() {
        return IconUtils.getIconFromCache(context, getPackageName());
    }

    public Bitmap getIcon(IconPack iconPack, ActivityInfo aInfo) {
        Bitmap icon;

        Log.d("TEST", "packageName=" + aInfo.packageName);

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
        return icon;
    }

    public void setUseDefault(boolean b) {
        useDefault = b;
    }

    public boolean getUseDefault() {
        return useDefault;
    }
}

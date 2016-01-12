package com.bitsyko.libicons;

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
    private ActivityInfo activityInfo;
    private Context context;
    private Resources mAppResources;
    private boolean useDefault = false;

    public Bitmap mCustomBitmap;

    public AppIcon(Context context, ActivityInfo info) throws PackageManager.NameNotFoundException {
        this.context = context;
        this.applicationInfo = context.getPackageManager().getApplicationInfo(
                info.packageName, PackageManager.GET_ACTIVITIES);
        activityInfo = info;
        overlay = new Overlay(context, info.packageName, "icon_" + info.packageName + ".overlay");
        mAppResources = context.getPackageManager().getResourcesForApplication(applicationInfo);
    }

    public String getPackageName() {
        return applicationInfo.packageName;
    }

    public String getName() {
        return String.valueOf(applicationInfo.loadLabel(context.getPackageManager()));
    }

    public boolean hasOverlay() {
        return overlay.getVendorApp().exists();
    }

    public Drawable getDefaultIcon() {
        return IconUtils.getIconFromCache(context, getPackageName());
    }

    public Bitmap getIcon(IconPack iconPack, ActivityInfo aInfo) {
        Bitmap icon = null;

        int iconId = iconPack.getResourceIdForActivityIcon(aInfo);
        if (iconId > 0) {
            Log.d("TEST", "pName=" + getPackageName() + " : icon in pack");
            icon = IconUtils.drawableToBitmap(
                    iconPack.getIconPackResources().getDrawable(iconId, null));
        } else if (iconPack.shouldComposeIcon()) {
            Log.d("TEST", "pName=" + getPackageName() + " : icon not in pack, composing");
            Drawable d = mAppResources.getDrawable(aInfo.getIconResource(), null);
            Log.d("TEST", "d is null ? " + String.valueOf(d == null));
            icon = IconUtils.createIconBitmap(
                    mAppResources.getDrawable(
                            aInfo.getIconResource(), null), context, iconPack);
        }
        return icon;
    }

    public void setUseDefault(boolean b) {
        useDefault = b;
    }

    public boolean getUseDefault() {
        return useDefault;
    }

    public void install() {
        IconUtils.saveBitmapForActivityInfo(context, activityInfo, mCustomBitmap);
    }
}

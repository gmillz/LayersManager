package com.lovejoy777.rroandlayersmanager.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * Created by gmillz on 1/19/16.
 */
public class NotificationHelper {

    public static void showThemeInstalledNotification(Context context, String pkgName) {
        ApplicationInfo info = null;
        try {
            info = context.getPackageManager().getApplicationInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        if (info == null) return;

        CharSequence appName = info.loadLabel(context.getPackageManager());

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder notif = new Notification.Builder(context);
    }
}

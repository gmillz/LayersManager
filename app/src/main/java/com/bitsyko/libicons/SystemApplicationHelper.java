package com.bitsyko.libicons;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class SystemApplicationHelper {

    private static SystemApplicationHelper instance;
    private Context context;
    private Collection<String> installedActivities;

    //No instances
    private SystemApplicationHelper(Context context) {
        this.context = context;
    }

    public static SystemApplicationHelper getInstance(Context context) {
        if (instance == null) {
            if (context == null) {
                throw new RuntimeException("Can't create instance without context");
            }
            instance = new SystemApplicationHelper(context);
        }
        return instance;
    }

    public Collection<String> getInstalledAppsWithLauncherActivities() {
        if (installedActivities == null) {
            installedActivities = new HashSet<>();

            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            final List<ResolveInfo> apps = context.getPackageManager().queryIntentActivities(mainIntent, 0);

            for (ResolveInfo info : apps) {
                installedActivities.add(info.activityInfo.packageName);
            }
        }
        return installedActivities;
    }
}

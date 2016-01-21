package com.lovejoy777.rroandlayersmanager.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lovejoy777.rroandlayersmanager.notification.NotificationHelper;

public class AppReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
            NotificationHelper.showThemeInstalledNotification(context, "");
        }
    }
}

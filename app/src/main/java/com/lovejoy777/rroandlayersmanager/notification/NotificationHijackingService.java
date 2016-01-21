/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lovejoy777.rroandlayersmanager.notification;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import com.bitsyko.libicons.IconPack;
import com.bitsyko.liblayers.Layer;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NotificationHijackingService extends NotificationListenerService {
    private static final String TAG = NotificationHijackingService.class.getName();
    private static final String GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending";
    private static final String ACTION_INSTALLED =
            "com.android.vending.SUCCESSFULLY_INSTALLED_CLICKED";
    private static final String EXTRA_PACKAGE_NAME = "package_name";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (GOOGLE_PLAY_PACKAGE_NAME.equals(sbn.getPackageName())) {
            PendingIntent contentIntent = sbn.getNotification().contentIntent;
            if (contentIntent == null) return;
            Class pendingIntent = contentIntent.getClass();
            Intent intent = null;
            try {
                Method getIntent = pendingIntent.getMethod("getIntent");
                getIntent.setAccessible(true);
                Object o = getIntent.invoke(contentIntent);
                if (o instanceof Intent) {
                    intent = (Intent) o;
                }
            } catch (NoSuchMethodException|InvocationTargetException|IllegalAccessException e) {
                // ignore
            }
            if (intent == null) return;
            String action = intent.getAction();
            if (ACTION_INSTALLED.equals(action)) {
                String pkgName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
                try {
                    if (IconPack.isIconPack(getApplicationContext(), pkgName)
                            || Layer.isLayer(getApplicationContext(), pkgName)) {
                        cancelNotification(sbn.getKey());
                        return;
                    }
                    Utils.CMPackageInfo info = Utils.getMetaData(getApplicationContext(),
                            getPackageManager().getApplicationInfo(pkgName, 0));
                    if (info.cmTheme) {
                        cancelNotification(sbn.getKey());
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // ignore
                }
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    // ensure that this notification listener is enabled.
    // the service watches for google play notifications
    public static void ensureEnabled(Context context) {
        ComponentName me = new ComponentName(context, NotificationHijackingService.class);
        String meFlattened = me.flattenToString();

        String existingListeners = Settings.Secure.getString(context.getContentResolver(),
                getEnabledNotificationListenersString());

        if (!TextUtils.isEmpty(existingListeners)) {
            if (existingListeners.contains(meFlattened)) {
                return;
            } else {
                existingListeners += ":" + meFlattened;
            }
        } else {
            existingListeners = meFlattened;
        }

        putStringSecure(context.getContentResolver(),
                getEnabledNotificationListenersString(),
                existingListeners);
    }

    public static void putIntSecure(ContentResolver resolver, String key, int value) {
        try {
            Class c = Settings.Secure.class;

            Class[] paramTypes = new Class[3];
            paramTypes[0] = ContentResolver.class;
            paramTypes[1] = String.class;
            paramTypes[2] = Integer.class;

            Object[] args = new Object[3];
            args[0] = resolver;
            args[1] = key;
            args[2] = value;

            Method m = c.getMethod("putInt", paramTypes);
            m.setAccessible(true);
            m.invoke(c, args);
        } catch (NoSuchMethodException|InvocationTargetException|IllegalAccessException e) {
            // ignore
        }
    }

    public static void putStringSecure(ContentResolver resolver, String key, String value) {
        try {
            Class c = Settings.Secure.class;

            Class[] paramTypes = new Class[3];
            paramTypes[0] = ContentResolver.class;
            paramTypes[1] = String.class;
            paramTypes[2] = String.class;

            Object[] args = new Object[3];
            args[0] = resolver;
            args[1] = key;
            args[2] = value;

            Method m = c.getMethod("putString", paramTypes);
            m.setAccessible(true);
            m.invoke(c, args);
        } catch (NoSuchMethodException|InvocationTargetException|IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static String getEnabledNotificationListenersString() {
        try {
            Class c = Settings.Secure.class;
            for (Field f : c.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getName().equals("ENABLED_NOTIFICATION_LISTENERS")) {
                    return (String) f.get(null);
                }
            }
        } catch (IllegalAccessException e) {
            // ignore
        }
        return null;
    }
}
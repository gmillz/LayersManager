package com.lovejoy777.rroandlayersmanager;

import android.app.Application;
import android.util.Log;

import com.lovejoy777.rroandlayersmanager.helper.ThemeLoader;
import com.lovejoy777.rroandlayersmanager.notification.NotificationHijackingService;
import com.lovejoy777.rroandlayersmanager.utils.OmsUtils;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

public class LayersApplication extends Application {

    private ThemeLoader mThemeLoader;

    @Override
    public void onCreate() {
        super.onCreate();
        mThemeLoader = ThemeLoader.getInstance(getApplicationContext());
        NotificationHijackingService.ensureEnabled(this);

        if (Utils.omsExists()) {
            Log.d("TEST", "OMS exists");
        } else {
            Log.d("TEST", "OMS does not exist");
        }

        NotificationHijackingService.putIntSecure(getContentResolver(), "advanced_reboot", 2);

        new OmsUtils(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mThemeLoader.unregisterPackageReceiver();
    }
}

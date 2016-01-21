package com.lovejoy777.rroandlayersmanager.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.bitsyko.liblayers.Layer;
import com.lovejoy777.rroandlayersmanager.fragments.IconFragment;
import com.lovejoy777.rroandlayersmanager.utils.IconUtils;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ThemeLoader {

    public static final String LAYERS_LOADED = "layers_loaded";
    public static final String ICONS_LOADED = "icons_loaded";
    public static final String THEMES_LOADED = "themes_loaded";

    private static final Intent sLayersLoadedIntent = new Intent(LAYERS_LOADED);
    private static final Intent sIconsLoadedIntent = new Intent(ICONS_LOADED);
    private static final Intent sThemesLoadedIntent = new Intent(THEMES_LOADED);

    private Context mContext;

    private List<Layer> mLayers = new ArrayList<>();
    private List<IconFragment.Item> mIcons = new ArrayList<>();
    private List<Theme> mThemes = new ArrayList<>();

    private static ThemeLoader INSTANCE;
    private Executor mExecutor = Executors.newFixedThreadPool(2);

    public static ThemeLoader getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ThemeLoader(context);
        }
        INSTANCE.setupPackageReceiver();
        return INSTANCE;
    }

    public ThemeLoader(Context context) {
        mContext = context;
        populateThemes();
        populateLayers();
        populateIcons();
    }

    private void setupPackageReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        mContext.registerReceiver(mPackageReceiver, filter);
    }

    public void unregisterPackageReceiver() {
        mContext.unregisterReceiver(mPackageReceiver);
    }

    public List<Layer> getLayers() {
        return mLayers;
    }

    public List<IconFragment.Item> getIcons() {
        return mIcons;
    }

    public List<Theme> getThemes() {
        return mThemes;
    }

    private void populateLayers() {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... v) {
                mLayers = Layer.getLayersInSystem(mContext);
                return null;
            }
            protected void onPostExecute(Void v) {
                mContext.sendBroadcast(sLayersLoadedIntent);
            }
        }.executeOnExecutor(mExecutor);
    }

    private void populateIcons() {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... v) {
                mIcons = IconUtils.getAllIcons(mContext);
                Collections.sort(mIcons, ICON_COMPARATOR);
                return null;
            }
            protected void onPostExecute(Void v) {
                mContext.sendBroadcast(sIconsLoadedIntent);
            }
        }.executeOnExecutor(mExecutor);
    }

    private void populateThemes() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                mThemes = Utils.getThemes(mContext);
                Collections.sort(mThemes, THEME_COMPARATOR);
                return null;
            }
            protected void onPostExecute(Void v) {
                mContext.sendBroadcast(sThemesLoadedIntent);
            }
        }.executeOnExecutor(mExecutor);
    }

    private static Comparator<Theme> THEME_COMPARATOR = new Comparator<Theme>() {
        @Override
        public int compare(Theme theme1, Theme theme2) {
            return theme1.getName().compareTo(theme2.getName());
        }
    };

    private static Comparator<IconFragment.Item> ICON_COMPARATOR =
            new Comparator<IconFragment.Item>() {
        @Override
        public int compare(IconFragment.Item item, IconFragment.Item t1) {
            return item.getTitle().compareTo(t1.getTitle());
        }
    };

    private BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) return;

            if (action.equals(Intent.ACTION_PACKAGE_ADDED)
                    || action.equals(Intent.ACTION_PACKAGE_CHANGED)
                    || action.equals(Intent.ACTION_PACKAGE_REMOVED)
                    || action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                if (TextUtils.isEmpty(packageName)) return;
                if (Layer.isLayer(context, packageName)) {
                    populateLayers();
                } else {
                    populateThemes();
                    populateIcons();
                }
            }
        }
    };
}

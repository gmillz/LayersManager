package com.lovejoy777.rroandlayersmanager.helper;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.bitsyko.libicons.IconPack;
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

    public static final String ICON_PACKS_LOADED = "icon_packs_loaded";
    public static final String ICONS_LOADED = "icons_loaded";

    public static final Intent sIconPacksLoadedIntent = new Intent(ICON_PACKS_LOADED);
    public static final Intent sIconsLoadedIntent = new Intent(ICONS_LOADED);

    private Context mContext;

    private List<Layer> mLayers = new ArrayList<>();
    private List<IconPack> mIconPacks = new ArrayList<>();
    private List<IconFragment.Item> mIcons = new ArrayList<>();
    private List<Theme> mThemes = new ArrayList<>();

    public static ThemeLoader INSTANCE;
    private Executor mExecutor = Executors.newFixedThreadPool(2);

    public static ThemeLoader getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ThemeLoader(context);
        }
        return INSTANCE;
    }

    public ThemeLoader(Context context) {
        mContext = context;
        populateLayers();
        populateIconPacks();
        populateIcons();
        populateThemes();
    }

    public List<Layer> getLayers() {
        return mLayers;
    }

    public List<IconPack> getIconPacks() {
        return mIconPacks;
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
                //mContext.sendBroadcast();
            }
        }.executeOnExecutor(mExecutor);
    }

    private void populateIconPacks() {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... v) {
                mIconPacks = IconPack.getIconPacks(mContext);
                IconPack defaultPack = new IconPack(mContext, "default");
                mIconPacks.add(defaultPack);
                Collections.sort(mIconPacks, ICON_PACK_COMPARATOR);
                return null;
            }
            protected void onPostExecute(Void v) {
                mContext.sendBroadcast(sIconPacksLoadedIntent);
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
                Utils.getThemes(mContext);
                return null;
            }
        }.executeOnExecutor(mExecutor);
    }

    private static Comparator<IconPack> ICON_PACK_COMPARATOR = new Comparator<IconPack>() {
        @Override
        public int compare(IconPack iconPack, IconPack t1) {
            return iconPack.getName().compareTo(t1.getName());
        }
    };

    private static Comparator<IconFragment.Item> ICON_COMPARATOR =
            new Comparator<IconFragment.Item>() {
        @Override
        public int compare(IconFragment.Item item, IconFragment.Item t1) {
            return item.getTitle().compareTo(t1.getTitle());
        }
    };
}

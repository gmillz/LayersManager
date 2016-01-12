package com.bitsyko.liblayers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import com.bitsyko.LayerInfo;
import com.bitsyko.liblayers.layerfiles.CMTEOverlay;
import com.bitsyko.liblayers.layerfiles.ColorOverlay;
import com.bitsyko.liblayers.layerfiles.CustomStyleOverlay;
import com.bitsyko.liblayers.layerfiles.GeneralOverlay;
import com.bitsyko.liblayers.layerfiles.LayerFile;
import com.lovejoy777.rroandlayersmanager.helper.AndroidXMLDecompress;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Layer implements Closeable, LayerInfo {
    private static final String ACTION_PICK_PLUGIN = "com.layers.plugins.PICK_OVERLAYS";

    // Theme asset paths
    private static final String COMMON_FOLDER = "overlays/common";
    private static final String CM_THEME_NAME_TAG = "org.cyanogenmod.theme.name";
    private static final String CM_THEME_AUTHOR_TAG = "org.cyanogenmod.theme.author";
    private final String name;
    private final String packageName;
    private final String developer;
    private final ApplicationInfo applicationInfo;
    private final Resources resources;
    private final Context context;
    private final Drawable icon;
    public boolean isCMTETheme;
    private Context layerContext;
    private List<Drawable> screenShots;
    private Drawable promo;
    private List<Color> colors = new ArrayList<>();
    private String generalZip;
    private List<LayerFile> layers;
    //Map for unpacked zipfiles from layer
    private Map<String, ZipFile> zipFileMap = new ArrayMap<>();
    private int screenShotId = 1;
    private int screenShotNumber = -1;

    public Layer(String name, String developer, Drawable icon) {
        this(name, developer, icon, null, null, null, null, false);
    }

    private Layer(String name, String developer, Drawable icon, String packageName,
                  Resources resources, ApplicationInfo applicationInfo, Context context, boolean isCMTETheme) {
        this.name = name;
        this.developer = developer;
        this.icon = icon;
        this.packageName = packageName;
        this.resources = resources;
        this.applicationInfo = applicationInfo;
        this.isCMTETheme = isCMTETheme;

        if (context != null) {
            this.context = context.getApplicationContext();
            try {
                layerContext = this.context.createPackageContext(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                // ignore
            }
        } else {
            this.context = null;
        }
    }

    public static Layer layerFromPackageName(String packageName, Context context)
            throws PackageManager.NameNotFoundException {

        ApplicationInfo applicationInfo = context.getApplicationContext().getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);

        Bundle bundle = applicationInfo.metaData;

        String name = bundle.getString("Layers_Name");
        String developer = bundle.getString("Layers_Developer");

        String mDrawableName = "icon";
        PackageManager manager = context.getApplicationContext().getPackageManager();

        Resources resources = manager.getResourcesForApplication(packageName);

        int iconID = resources.getIdentifier(mDrawableName, "drawable", packageName);

        Drawable icon = resources.getDrawable(iconID, null);

        return new Layer(name, developer, icon, packageName, resources, applicationInfo, context, false);
    }

    public static Layer layerFromCMTETheme(String packageName, Context context) {
        Context themeContext;
        try {
            themeContext = context.createPackageContext(packageName, 0);

            ApplicationInfo applicationInfo =
                    context.getApplicationContext().getPackageManager()
                            .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            CMPackageInfo packageInfo = getMetaData(context, applicationInfo);

            if (themeContext != null) {
                Drawable icon = context.getPackageManager().getApplicationIcon(packageName);


                return new Layer(packageInfo.name, packageInfo.dev, icon,
                        packageName, themeContext.getResources(), applicationInfo, context, true);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        return null;
    }

    public static List<Layer> getLayersInSystem(Context context) {

        List<Layer> layerList = new ArrayList<>();

        PackageManager packageManager = context.getPackageManager();
        Intent baseIntent = new Intent(ACTION_PICK_PLUGIN);
        baseIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
        ArrayList<ResolveInfo> list = (ArrayList<ResolveInfo>)
                packageManager.queryIntentServices(baseIntent, PackageManager.GET_RESOLVED_FILTER);

        for (ResolveInfo info : list) {
            ServiceInfo sinfo = info.serviceInfo;

            try {
                layerList.add(layerFromPackageName(sinfo.packageName, context));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

        }

        //getInstalledCMTEThemes(activity, layerList);

        return layerList;
    }

    private static CMPackageInfo getMetaData(Context context, ApplicationInfo info) {
        File file = new File(info.sourceDir);
        CMPackageInfo packageInfo = new CMPackageInfo();
        ZipFile zip;
        InputStream manifestInputStream;
        byte[] array;

        try {
            Context appContext = context.createPackageContext(info.packageName, 0);
            zip = new ZipFile(file);
            manifestInputStream = zip.getInputStream(zip.getEntry("AndroidManifest.xml"));
            array = IOUtils.toByteArray(manifestInputStream);
            String manifest = AndroidXMLDecompress.decompressXML(array);
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(IOUtils.toInputStream(manifest), null);
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                if (name.equals("meta-data")) {
                    int size = parser.getAttributeCount();
                    for (int i = 0; i < size; i++) {
                        String attrName = parser.getAttributeName(i);
                        String attrValue = parser.getAttributeValue(i);
                        if (attrName.equals("name")) {
                            if (attrValue.equals(CM_THEME_NAME_TAG)
                                    || attrValue.equals(CM_THEME_AUTHOR_TAG)) {
                                i++;
                                String v = parser.getAttributeValue(i);
                                if (attrValue.equals(CM_THEME_NAME_TAG)) {
                                    packageInfo.name = appContext.getResources()
                                            .getString(Integer.parseInt(v));
                                } else if (attrValue.equals(CM_THEME_AUTHOR_TAG)) {
                                    packageInfo.dev = appContext.getResources()
                                            .getString(Integer.parseInt(v));
                                }
                                packageInfo.cmTheme = true;
                            }
                        }
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException | IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException | XmlPullParserException e) {
            // ignore - causes spam
        }
        return packageInfo;
    }

    private static void getInstalledCMTEThemes(Context context, List<Layer> layers) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo info : apps) {
            CMPackageInfo packageInfo = getMetaData(context, info);
            if (packageInfo.cmTheme) {
                Layer layer = layerFromCMTETheme(info.packageName, context);
                if (layer != null) layers.add(layer);
            }
        }
    }

    public String getVersionCode() {
        try {
            return context.getApplicationContext().getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public String getName() {
        return name;
    }

    public String getDeveloper() {
        return developer;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<Drawable> getScreenShots() {
        return getScreenShots(new Callback<Drawable>() {
            @Override
            public void callback(Drawable object) {
            }
        });
    }

    public int getScreenShotsNumber() {

        if (screenShotNumber == -1) {

            int i = 0;

            int screenShotId = 0;

            do {
                i++;

                String drawableName = "screenshot" + i;

                screenShotId = resources.getIdentifier(drawableName, "drawable", packageName);

            } while (screenShotId != 0);

            screenShotNumber = i;

        }

        return screenShotNumber;

    }

    public Drawable getScreenshot(int id) {
        int resId = (resources.getIdentifier("screenshot" + id, "drawable", packageName));
        return resources.getDrawable(resId, null);
    }

    public Pair<Integer, Drawable> getNextScreenshot() {

        String drawableName = "screenshot" + screenShotId;

        int mDrawableResID = resources.getIdentifier(drawableName, "drawable", packageName);

        if (mDrawableResID == 0) {
            return new Pair<>(0, null);
        }

        Drawable drawable = resources.getDrawable(mDrawableResID, null);

        screenShotId++;

        return new Pair<>(screenShotId - 1, drawable);
    }

    public List<Drawable> getScreenShots(Callback<Drawable> callback) {

        if (screenShots == null) {

            screenShots = new ArrayList<>();

            int i = 1;

            while (true) {

                String drawableName = "screenshot" + i;

                int mDrawableResID = resources.getIdentifier(drawableName, "drawable", packageName);

                if (mDrawableResID == 0) {
                    break;
                }

                Drawable drawable = resources.getDrawable(mDrawableResID, null);
                screenShots.add(resources.getDrawable(mDrawableResID, null));
                i++;
                callback.callback(drawable);
            }

        }

        return screenShots;
    }

    public Drawable getPromo() {

        if (promo == null) {

            int promoID = resources.getIdentifier("heroimage", "drawable", packageName);
            if (promoID != 0) {
                promo = resources.getDrawable(promoID, null);
            }
        }
        return promo;
    }

    public List<LayerFile> getLayersInCMTETheme() {
        List<LayerFile> layers = new ArrayList<>();
        try {
            AssetManager am = getAssetManager();
            for (String name : am.list("overlays")) {
                PackageManager pm = context.getPackageManager();
                try {
                    String n = pm.getApplicationLabel(pm.getApplicationInfo(name, 0)).toString();
                    layers.add(new CMTEOverlay(context, this, n, name));
                } catch (PackageManager.NameNotFoundException e) {
                    // ignore
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return layers;
    }

    public String extractCommonResources() {
        String dir = getCacheDir() + File.separator + packageName + File.separator + "common";
        Utils.copyAssetFolder(getAssetManager(), COMMON_FOLDER, dir);
        return dir + File.separator + "res" + File.separator + "values";
    }

    public List<LayerFile> getLayersInPackage() {

        if (isCMTETheme) {
            return getLayersInCMTETheme();
        }

        if (layers == null) {

            layers = new ArrayList<>();

            AssetManager assetManager = getResources().getAssets();

            List<String> layerZips = new ArrayList<>();

            try {
                layerZips.addAll(Arrays.asList(assetManager.list("Files")));
            } catch (IOException e) {
                e.printStackTrace();
            }

            List<LayerFile> generalOverlays = new ArrayList<>();
            Set<LayerFile> colorOverlays = new HashSet<>();
            List<LayerFile> customStylesOverlay = new ArrayList<>();

            for (String overlayFile : layerZips) {

                boolean generalOverlay = false;

                if (StringUtils.endsWithIgnoreCase(overlayFile, "general.zip")) {
                    generalOverlay = true;
                }

                //Extracting zip
                File zipFile = new File(context.getCacheDir() + File.separator
                        + StringUtils.deleteWhitespace(getName()) + File.separator + overlayFile);

                Utils.copyAsset(assetManager,
                        "Files" + File.separator + overlayFile, zipFile.getAbsolutePath());

                //Checking zip content
                ArrayList<? extends ZipEntry> zipEntries = new ArrayList<>();

                try {
                    ZipFile zip = new ZipFile(zipFile);
                    zipEntries = Collections.list(zip.entries());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                boolean customStyle = false;
                List<Color> customStyles = new ArrayList<>();


                for (ZipEntry zipEntry : zipEntries) {

                    if (generalOverlay) {
                        generalZip = overlayFile;
                        generalOverlays.add(new GeneralOverlay(this, zipEntry.getName()));
                        continue;
                    }

                    //Contains folders <=> custom style overlay
                    if (zipEntry.getName().contains("/")) {

                        //If it's not folder
                        if (!zipEntry.getName().endsWith("/")) {
                            customStyle = true;
                            customStyles.add(new Style(zipEntry.getName(), this));
                        }

                    } else {
                        //Color overlay
                        colorOverlays.add(new ColorOverlay(this, zipEntry.getName()));
                        colors.add(new Color(overlayFile, this));
                    }


                }

                if (customStyle) {
                    customStylesOverlay.add(new CustomStyleOverlay(this, overlayFile, customStyles));
                }

            }


            //To remove duplicates
            //TODO: Make it better
            colors = new ArrayList<>(new HashSet<>(colors));
            Collections.sort(colors);

            List<LayerFile> colorOverlaysList = new ArrayList<>(colorOverlays);

            Collections.sort(generalOverlays);
            Collections.sort(customStylesOverlay);
            Collections.sort(colorOverlaysList);

            //We're showing custom styles overlay after general
            layers.addAll(generalOverlays);
            layers.addAll(customStylesOverlay);
            layers.addAll(colorOverlaysList);


        }

        return layers;
    }

    public int getPluginVersion() {
        int mPluginVersion = 2;
        Bundle bundle = applicationInfo.metaData;
        if (bundle != null && bundle.containsKey("Layers_PluginVersion")) {
            mPluginVersion = Integer.parseInt(bundle.getString("Layers_PluginVersion"));
        }
        return mPluginVersion;
    }

    public List<Color> getColors() {
        return new ArrayList<>(colors);
    }

    public Resources getResources() {
        return resources;
    }

    public String getWhatsNew() {
        if (applicationInfo.metaData != null) {
            return applicationInfo.metaData.getString("Layers_WhatsNew");
        }
        return null;
    }

    public String getDescription() {
        if (applicationInfo.metaData != null) {
            return applicationInfo.metaData.getString("Layers_Description");
        }
        return null;
    }

    public AssetManager getAssetManager() {
        return layerContext.getAssets();
    }

    public String getCacheDir() {
        return context.getCacheDir().getAbsolutePath();
    }

    public Context getRelatedContext() {
        return context;
    }

    public ZipFile getFileFromMap(String name) {
        return zipFileMap.get(name);
    }

    public ZipFile putFileToMap(ZipFile zipFile, String name) {
        return zipFileMap.put(name, zipFile);
    }

    public boolean mapHasFile(String name) {
        return zipFileMap.containsKey(name);
    }

    @Override
    public void close() throws IOException {
        if (new File(getCacheDir() + File.separator + getName()).exists()) {
            Utils.deleteFile(context.getCacheDir()
                    + File.separator + StringUtils.deleteWhitespace(getName()));
        }

        for (ZipFile zipFile : zipFileMap.values()) {
            zipFile.close();
        }

        zipFileMap.clear();
    }

    public String getGeneralZip() {
        return generalZip;
    }

    private static class CMPackageInfo {
        String name;
        String dev;
        boolean cmTheme;
    }
}

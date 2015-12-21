package com.lovejoy777.rroandlayersmanager.overlaycreator;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.lovejoy777.rroandlayersmanager.DeviceSingleton;
import com.lovejoy777.rroandlayersmanager.utils.ResourceParser;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;

import kellinwood.security.zipsigner.ZipSigner;

public class Overlay {

    private static final String TAG = "Overlay";

    private static final String TARGET_PACKAGE_TEMPLATE = "<<TARGET_PACKAGE>>";
    private static final String PACKAGE_NAME_TEMPLATE = "<<PACKAGE_NAME>>";

    private HashMap<String, HashMap<String, String>> mResources = new HashMap<>();
    private HashMap<String, ResourceParser.Style> mStyles = new HashMap<>();

    public String name;
    public String targetPackage;
    public String packageName;
    public File path;

    private File manifest;
    private File res;
    private File unsignedApp;
    private File signedApp;

    private Context context;
    private Creator creator;

    private boolean resourcesLoaded = false;

    public Overlay(Context context, String targetPackage) {
        initialize(context, targetPackage,
                context.getCacheDir() + "/temp/" + targetPackage);

    }

    /*public Overlay(Context context, String targetPackage, File path) {
        initialize(context, targetPackage, path.getAbsolutePath());
    }*/

    public void initialize(Context context, String targetPackage, String path) {

        this.context = context;
        this.targetPackage = targetPackage;
        packageName = targetPackage + ".overlay";
        this.creator = new Creator(context);

        this.path = new File(path);
        manifest = new File(path + File.separator + "AndroidManifest.xml");
        res = new File(path + File.separator + "res");
        unsignedApp = new File(path + File.separator + packageName + "_unsigned.apk");
        signedApp = new File(context.getCacheDir() + File.separator + packageName + "_signed.apk");

        if (!res.mkdirs()) {
            Log.e(TAG, "cannot create " + res.getAbsolutePath());
        }

        for (String re : ResourceParser.resourceTypes) {
            mResources.put(re, new HashMap<String, String>());
        }

        //Utils.applyPermissions(this.path, "666");

        //loadResources();
        //dereferenceResources();
    }

    public void loadResources() {
        Log.d("TEST", "targetPackage=" + targetPackage);
        for (File resdir : res.listFiles()) {
            if (resdir.getName().contains("values")) {
                for (File xml : resdir.listFiles()) {
                    if (xml.getName().contains("styles")) {
                        Log.d("TEST", "style found");
                        ResourceParser.parseStyleXML(xml, mStyles);
                    } else {
                        ResourceParser.parseXML(xml, mResources);
                    }
                }
            }
        }
        File current = new File(res + "/values-v" + Build.VERSION.SDK_INT);
        if (current.exists()) {
            ResourceParser.parseXML(current, mResources);
        }
        //loadDrawables();
        resourcesLoaded = true;
    }

    public void dereferenceResources() {
        loadResources();
        for (String type : mResources.keySet()) {
            HashMap<String, String> map = mResources.get(type);
            for (String name : map.keySet()) {
                String value = map.get(name);
                Log.d("TEST", "targetPackage=" + targetPackage);
                Log.d("TEST", "packageName=" + packageName);
                Log.d("TEST", "type=" + type + " : name=" + name + " : value=" + value);
                if (type.equals("drawable")) continue;
                if (value.contains("@")
                        && !value.contains("android:") && !value.contains("common:")) {
                    String[] split = value.split("/");
                    String t = split[0].substring(1);
                    String n = split[1];
                    if (t.equals("drawable")) continue;
                    Log.d("TEST", "type=" + t + " : name=" + n);
                    mResources.get(t).put(name, mResources.get(t).get(n));
                }
                //Log.d("TEST", name + "=" + value);
            }
        }
        if (mStyles.size() != 0) {
            for (String name : mStyles.keySet()) {
                ResourceParser.Style style = mStyles.get(name);
                for (String na : style.contents.keySet()) {
                    String value = style.contents.get(na);
                    if (value.contains("@")
                            && !value.contains("android:") && !value.contains("common:")) {
                        String[] split = value.split("/");
                        String t = split[0].substring(1);
                        String n = split[1];
                        Log.d("TEST", "type=" + t + " : name=" + n);
                        if (t.equals("drawable")) continue;
                        if (t.equals("style")) continue;
                        mStyles.get(name).contents.put(na, mResources.get(t).get(n));
                    }
                }
            }
        }
        loopResources();
    }

    public HashMap<String, HashMap<String, String>> getResources() {
        return mResources;
    }

    public HashMap<String, ResourceParser.Style> getStyles() {
        return mStyles;
    }

    public void loopResources() {
        for (String type : mResources.keySet()) {
            HashMap<String, String> map = mResources.get(type);
            for (String name : map.keySet()) {
                String value = map.get(name);
                Log.d("TEST", name + "=" + value);
            }
        }
        loopStyles();
        writeResources();
    }

    private void loopStyles() {
        for (String name : mStyles.keySet()) {
            ResourceParser.Style style = mStyles.get(name);
            Log.d("TEST-STYLE", "name=" + style.name);
            Log.d("TEST-STYLE", "parent=" + style.parent);
            for (String n : style.contents.keySet()) {
                String v = style.contents.get(n);
                Log.d("TEST-STYLE", "item = " + n + "=" + v);
            }
        }
    }

    public File getResDir() {
        return res;
    }

    public void create() {
        try {
            createManifest();
            createUnsignedPackage();
            signPackage();
            install();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createManifest() throws IOException {
        Log.d("TEST", "createManifest");
        String tempManifest = IOUtils.toString(context.getAssets().open("AndroidManifest.xml"))
                .replace(TARGET_PACKAGE_TEMPLATE, targetPackage)
                .replace(PACKAGE_NAME_TEMPLATE, targetPackage + ".overlay");

        FileUtils.writeStringToFile(manifest, tempManifest);
    }

    private void createUnsignedPackage() throws IOException, InterruptedException {
        Log.d("TEST", "createUnsignedPackage");
        unsignedApp.getParentFile().mkdirs();
        String command = creator.aapt.getAbsolutePath() + " package "
                + "-M " + manifest.getAbsolutePath()
                + " -S " + res.getAbsolutePath()
                + " -I /system/framework/framework-res.apk"
                + " -F " + unsignedApp.getAbsolutePath();
        unsignedApp.setWritable(true);
        unsignedApp.setReadable(true);
        /*Process app = Runtime.getRuntime().exec(new String[]{
                creator.aapt.getAbsolutePath(), "p",
                "-M", manifest.getAbsolutePath(),
                "-S", res.getAbsolutePath(),
                "-I", "system/framework/framework-res.apk",
                "-F", unsignedApp.getAbsolutePath()});*/

        Log.d("TEST", "command=" + command);
        Utils.runCommand(command);

        //IOUtils.toString(app.getInputStream());
        //IOUtils.toString(app.getErrorStream());

        //app.waitFor();
    }

    public void writeResources(){
        clearValues();
        for (String type : ResourceParser.resourceTypes) {
            File f = new File(res + "/values-v" + Build.VERSION.SDK_INT + "/" + type + ".xml");
            f.getParentFile().mkdirs();
            String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
            content += "<resources>\n";
            //content += "\n";
            if (mResources.get(type).keySet().size() == 0) continue;
            for (String name : mResources.get(type).keySet()) {
                String value = mResources.get(type).get(name);
                content += "    ";
                content += "<" + type + " name=\"" + name + "\">" + value + "</" + type + ">";
                content += "\n";
            }
            content += "</resources>";
            try {
                FileUtils.writeStringToFile(f, content);
            } catch (IOException e) {
                // ignore
            }
            Log.d("TEST", "content= \n" + content);
        }
    }

    private void clearValues() {
        for (File f : res.listFiles()) {
            if (f.getName().contains("values")) {
                if (!f.delete()) {
                    Log.d("TEST", "cannot delete " + f.getAbsolutePath());
                }
            }
        }
    }

    private void signPackage() throws ClassNotFoundException,
            IllegalAccessException, InstantiationException, GeneralSecurityException, IOException {

        signedApp.getParentFile().mkdirs();
        ZipSigner zipSigner = new ZipSigner();
        zipSigner.setKeymode("testkey");
        zipSigner.signZip(unsignedApp.getAbsolutePath(), signedApp.getAbsolutePath());
    }

    public void install() {
        Log.d("TEST", "installing overlay");
        Utils.moveFile(signedApp.getAbsolutePath(),
                DeviceSingleton.getInstance().getOverlayFolder()
                        + File.separator + signedApp.getName());

    }
}

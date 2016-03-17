package com.lovejoy777.rroandlayersmanager.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.bitsyko.libicons.IconPack;
import com.lovejoy777.rroandlayersmanager.DeviceSingleton;
import com.lovejoy777.rroandlayersmanager.helper.AndroidXMLDecompress;
import com.lovejoy777.rroandlayersmanager.helper.Theme;

import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

public class Utils {

    public static final String TAG = "Layers.Utils";

    private static final String CM_THEME_NAME_TAG = "org.cyanogenmod.theme.name";
    private static final String CM_THEME_AUTHOR_TAG = "org.cyanogenmod.theme.author";

    public static class CommandOutput {
        public String output;
        public String error;
        public int exitCode;
    }

    public static boolean deleteFile(String path) {
        if (!isRootAvailable()) return false;
        try {
            if (new File(path).isDirectory()) {
                runCommand("rm -rf '" + path + "'\n", true);
            } else {
                runCommand("rm -rf '" + path + "'\n", true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return !new File(path).exists();
    }

    public static boolean createFolder(File folder) {
        if (!isRootAvailable()) return false;
        runCommand("mkdir " + folder.getPath(), true);
        return true;
    }

    public static boolean applyPermissions(String file, String perms) {
        try {
            runCommand("chmod " + perms + " " + file, true);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean applyPermissionsRecursive(String file, String perms) {
        try {
            runCommand("chmod -R " + perms + " " + file, true);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean moveFile(String old, String newDir) {
        if (!isRootAvailable()) return false;
        try {
            runCommand("mv -f " + old + " " + newDir, true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return new File(newDir).exists();
    }

    public static boolean copyFile(String old, String newFile) {
        if (!isRootAvailable()) return false;
        try {
            runCommand("cp " + old + " " + newFile, true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return new File(newFile).exists();
    }

    public static CommandOutput runCommand(String cmd, boolean useRoot) {
        if (!isRootAvailable()) return null;
        CommandOutput output = new CommandOutput();
        try {
            Process process = Runtime.getRuntime().exec(useRoot ? "su" : "sh");
            DataOutputStream os = new DataOutputStream(
                    process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();

            output.exitCode = process.waitFor();
            output.output = IOUtils.toString(process.getInputStream());
            output.error = IOUtils.toString(process.getErrorStream());
            if (output.exitCode != 0 || (!"".equals(output.error) && null != output.error)) {
                Log.e("Root Error, cmd: " + cmd, output.error);
                return output;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return output;
        }
        return output;
    }

    public static boolean remount(String mountType) {
        String folder = DeviceSingleton.getInstance().getMountFolder();
        return remount(mountType, folder);
    }

    public static boolean remount(String mountType, String location) {
        if (!isRootAvailable()) return false;

        CommandOutput out = runCommand("mount -o "
                + mountType + ",remount " + location + "\n", true);

        return out != null && out.exitCode == 0;
    }

    public static boolean isRootAvailable() {
        return true;
    }

    public static boolean copyAssetFolder(AssetManager assetManager,
                                          String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            if (!new File(toPath).exists() && !new File(toPath).mkdirs()) {
                throw new RuntimeException("cannot create directory: " + toPath);
            }
            boolean res = true;
            for (String file : files) {
                if (assetManager.list(fromAssetPath + "/" + file).length == 0) {
                    res &= copyAsset(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
                } else {
                    res &= copyAssetFolder(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
                }
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean copyAsset(AssetManager assetManager,
                                    String fromAssetPath, String toPath) {
        InputStream in;

        File parent = new File(toPath).getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            Log.d(TAG, "Unable to create " + parent.getAbsolutePath());
        }

        try {
            in = assetManager.open(fromAssetPath);
            copyInputStreamToFile(in, new File(toPath));
            in.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static File getCacheDir() {
        return new File(Environment.getExternalStorageDirectory() + "/.layers-cache");
    }

    public static boolean copyInputStreamToFile(InputStream in, File file) {
        try {
            if (file.exists()) {
                if (!file.delete())
                    Log.e(TAG, "Unable to delete " + file.getAbsolutePath());
            }
            File parent = file.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs())
                    Log.e(TAG, "Unable to create " + parent.getAbsolutePath());
            }

            FileOutputStream out = new FileOutputStream(file);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                    // ignore
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static List<String> sDensityBuckets = new ArrayList<>();

    static {
        sDensityBuckets.add("xxxhdpi");
        sDensityBuckets.add("xxhdpi");
        sDensityBuckets.add("xhdpi");
        sDensityBuckets.add("hdpi");
        sDensityBuckets.add("mdpi");
        sDensityBuckets.add("ldpi");
    }

    public static List<String> getDensityBuckets() {
        return sDensityBuckets;
    }

    public static List<Theme> getThemes(Context context) {
        List<Theme> themes = new ArrayList<>();
        getSystemTheme(context, themes);
        getInstalledThemes(context, themes);
        return themes;
    }

    private static void getSystemTheme(Context context, List<Theme> themes) {
        themes.add(new Theme(context, Theme.SYSTEM_THEME));
    }

    private static void getInstalledThemes(Context context, List<Theme> themes) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo info : apps) {
            Theme theme = null;
            if (IconPack.isIconPack(context, info.packageName))  {
                theme = new Theme(context, info.packageName);
            } else {
                CMPackageInfo packageInfo = getMetaData(context, info);
                if (packageInfo.cmTheme) {
                    Log.d("TEST", info.packageName);
                    theme = new Theme(context, info.packageName);
                    theme.setName(packageInfo.name);
                }
            }
            if (theme != null) {
                themes.add(theme);
            }
        }
    }

    public static class CMPackageInfo {
        String name;
        String dev;
        public boolean cmTheme = false;
    }

    public static CMPackageInfo getMetaData(Context context, ApplicationInfo info) {
        File file = new File(info.sourceDir);
        CMPackageInfo packageInfo = new CMPackageInfo();
        ZipFile zip;
        InputStream manifestInputStream;
        byte[] array;

        try {
            Context appContext = context.createPackageContext(info.packageName, 0);
            // first check for any assets to speed this up
            AssetManager am = appContext.getAssets();
            if (!ThemeUtils.assetsContainThemeParts(am)) return packageInfo;
            zip = new ZipFile(file);
            manifestInputStream = zip.getInputStream(zip.getEntry("AndroidManifest.xml"));
            array = IOUtils.toByteArray(manifestInputStream);
            String manifest = AndroidXMLDecompress.decompressXML(array);
            if (!manifest.contains("cyanogenmod")) return packageInfo;
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

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        return calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight);
    }

    // Modified from original source:
    // http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
    public static int calculateInSampleSize(
            int decodeWidth, int decodeHeight, int reqWidth, int reqHeight) {
        // Raw height and width of image
        int inSampleSize = 1;

        if (decodeHeight > reqHeight || decodeWidth > reqWidth) {
            final int halfHeight = decodeHeight / 2;
            final int halfWidth = decodeWidth / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight &&
                    (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * For excessively large images with an awkward ratio we
     * will want to crop them
     * @return
     */
    public static Rect getCropRectIfNecessary(
            BitmapFactory.Options options,int reqWidth, int reqHeight) {
        Rect rect = null;
        // Determine downsampled size
        int width = options.outWidth / options.inSampleSize;
        int height = options.outHeight / options.inSampleSize;

        if ((reqHeight * 1.5 < height)) {
            int bottom = height/ 4;
            int top = bottom + height/2;
            rect = new Rect(0, bottom, width, top);
        } else if ((reqWidth * 1.5 < width)) {
            int left = width / 4;
            int right = left + height/2;
            rect = new Rect(left, 0, right, height);
        }
        return rect;
    }

    public static boolean omsExists() {
        return new File("/system/bin/om").exists();
    }

    public static String getStringFromFile(String path) {
        CommandOutput out = runCommand("cat " + path, true);
        if (out != null) Log.d("TEST", out.output);
        return out != null ? out.output : null;
    }
}

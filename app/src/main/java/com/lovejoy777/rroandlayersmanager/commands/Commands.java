package com.lovejoy777.rroandlayersmanager.commands;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.bitsyko.liblayers.layerfiles.LayerFile;
import com.lovejoy777.rroandlayersmanager.AsyncResponse;
import com.lovejoy777.rroandlayersmanager.DeviceSingleton;
import com.lovejoy777.rroandlayersmanager.R;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


public class Commands {

    private static final String[] aaptUrls = {
            "https://www.dropbox.com/s/au7ccu1gtroqvzt/aapt_x86?dl=1",
            "https://www.dropbox.com/s/5x2fpgw6ojyao2d/aapt_arm?dl=1"
    };

    public static ArrayList<String> loadFiles(String directory) {

        File f = new File(directory);
        ArrayList<String> files = new ArrayList<>();

        if (!f.exists() || !f.isDirectory()) {
            return files;
        }

        for (File file : f.listFiles()) {
            if (!file.isDirectory()) {
                files.add(file.getName());
            }
        }
        Collections.sort(files, String.CASE_INSENSITIVE_ORDER);
        return files;
    }

    public static ArrayList<String> loadFolders(String directory) {
        File f = new File(directory);
        ArrayList<String> folders = new ArrayList<>();

        if (!f.exists() || !f.isDirectory()) {
            return folders;
        }

        for (File file : f.listFiles()) {
            if (file.isDirectory()) {
                folders.add(file.getName());
            }
        }
        return folders;
    }

    public static void reboot(final Context context) {
        AlertDialog.Builder progressDialogReboot = new AlertDialog.Builder(context);
        progressDialogReboot.setTitle(R.string.Reboot);
        progressDialogReboot.setMessage(R.string.PreformReboot);
        progressDialogReboot.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            //when Cancel Button is clicked
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        progressDialogReboot.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            //when Cancel Button is clicked
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Runtime.getRuntime()
                            .exec(new String[]{"su", "-c", "am restart"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            }
        });
        progressDialogReboot.show();
    }

    public static ArrayList<String> fileNamesFromZip(File zip) throws IOException {
        ArrayList<String> files = new ArrayList<>();

        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)));
        ZipEntry ze;

        while ((ze = zis.getNextEntry()) != null) {
            files.add(ze.getName());
        }

        return files;
    }

    public static int getSortMode(Activity context) {
        if (context == null) return 1;
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("sortMode", 1);
    }

    public static void setSortMode(Activity context, int mode) {
        if (context == null) return;
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("sortMode", mode).commit();
    }

    public static void killLauncherIcon(Context context) {

        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, com.lovejoy777.rroandlayersmanager.MainActivity.class); // activity which is first time open in manifiest file which is declare as <category android:name="android.intent.category.LAUNCHER" />
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        Toast.makeText(context, context.getResources().getString(R.string.launcherIconRemoved), Toast.LENGTH_SHORT).show();

    }

    public static void ReviveLauncherIcon(Context context) {

        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, com.lovejoy777.rroandlayersmanager.MainActivity.class);
        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        Toast.makeText(context, context.getResources().getString(R.string.launcherIconRevived), Toast.LENGTH_SHORT).show();

    }

    public static boolean appInstalledOrNot(Context context, String uri) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    public static class InstallZipBetterWay extends AsyncTask<String, Void, Void> {

        Context context;
        ProgressDialog progressBackup;
        AsyncResponse callback;

        public InstallZipBetterWay(Context context, AsyncResponse callback) {
            this.context = context;
            this.callback = callback;
        }

        @Override
        protected void onPreExecute() {
            progressBackup = ProgressDialog.show(context, context.getString(R.string.installingOverlays),
                    context.getString(R.string.installing) + "...", true);
        }

        @Override
        protected Void doInBackground(String... files) {
            String tempDir = context.getCacheDir().getAbsolutePath() + File.separator + "zipCache/";

            try {
                FileUtils.deleteDirectory(new File(tempDir));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (!new File(tempDir).mkdirs()) {
                if (!new File(tempDir).exists()) {
                    throw new RuntimeException("Cannot create temp folder");
                }
            }

            for (String file : files) {
                if (file.endsWith(".zip")) {
                    Log.d("Extracting", file);
                    try {
                        ZipInputStream zis = new ZipInputStream(
                                new BufferedInputStream(new FileInputStream(file)));
                        ZipEntry ze;
                        ZipFile zipFile = new ZipFile(file);

                        while ((ze = zis.getNextEntry()) != null) {
                            FileUtils.copyInputStreamToFile(
                                    zipFile.getInputStream(ze), new File(tempDir + ze.getName()));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("Extracting failed", file);
                    }
                } else if (file.endsWith(".apk")) {
                    Log.d("Copying", file);

                    File apkFile = new File(file);

                    try {
                        FileUtils.copyFile(apkFile, new File(tempDir + apkFile.getName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("Copying failed", file);
                    }
                }
            }

            Utils.remount("rw");
            Utils.moveFile(tempDir + "*", DeviceSingleton.getInstance().getOverlayFolder() + "/");
            Utils.applyPermissionsRecursive(DeviceSingleton.getInstance().getOverlayFolder(), "644");
            Utils.applyPermissions(DeviceSingleton.getInstance().getOverlayFolder(), "755");
            Utils.remount("ro");
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            progressBackup.dismiss();
            if (callback != null) {
                callback.processFinish();
            }
        }
    }

    public static class UnInstallOverlays extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress;
        private ArrayList<String> paths;
        private Context Context;
        private AsyncResponse delegate;
        private int i = 0;

        public UnInstallOverlays(ArrayList<String> paths, Context context, AsyncResponse response) {
            this.paths = paths;
            this.Context = context;
            this.delegate = response;
        }

        protected void onPreExecute() {
            progress = new ProgressDialog(Context);
            progress.setTitle(R.string.uninstallingOverlays);
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setProgress(0);
            progress.show();
            progress.setCancelable(false);
            progress.setMax(paths.size());
        }

        @Override
        protected Void doInBackground(Void... params) {
            Utils.remount("rw");
            for (String path : paths) {
                Log.d("Removing: ", path);
                try {
                    Utils.deleteFile(path);
                } catch (Exception e) {
                    Log.w("Cannot remove: ", path);
                    e.printStackTrace();
                }
                publishProgress();
            }
            Utils.remount("ro");
            return null;
        }

        protected void onProgressUpdate(Void... progress2) {
            progress.setProgress(++i);
        }

        protected void onPostExecute(Void result) {
            progress.dismiss();
            if (delegate != null) {
                delegate.processFinish();
            }
        }

    }

    public static class InstallOverlaysBetterWay extends AsyncTask<Void, String, Void> {

        private ProgressDialog progress;
        private AsyncResponse delegate;
        private List<LayerFile> layersToInstall;
        private Context context;
        private int i = 0;

        public InstallOverlaysBetterWay(List<LayerFile> layersToInstall, Context context, AsyncResponse delegate) {
            this.layersToInstall = layersToInstall;
            this.context = context;
            this.delegate = delegate;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(context);
            progress.setTitle(R.string.installingOverlays);
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setProgress(i);
            progress.show();
            progress.setCancelable(false);
            progress.setMax(layersToInstall.size());
        }


        @Override
        protected Void doInBackground(Void... params) {
            Utils.remount("rw");
            for (LayerFile layerFile : layersToInstall) {
                try {
                    Utils.moveFile(layerFile.getFile(context).getAbsolutePath(),
                            DeviceSingleton.getInstance().getOverlayFolder() + "/" + layerFile.getFile(context).getName());
                    publishProgress();
                } catch (Exception e) {
                    e.printStackTrace();
                    publishProgress(e.getMessage());
                }
            }

            if (!layersToInstall.isEmpty()) {
                try {
                    layersToInstall.get(0).getLayer().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Utils.applyPermissionsRecursive(DeviceSingleton.getInstance().getOverlayFolder(), "644");
            Utils.applyPermissions(DeviceSingleton.getInstance().getOverlayFolder(), "755");
            Utils.remount("ro");
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (values.length != 0) {
                Toast.makeText(context, values[0], Toast.LENGTH_LONG).show();
            }
            progress.setProgress(++i);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progress.dismiss();
            if (delegate != null) {
                delegate.processFinish();
            }
        }

    }

    public static class CheckAapt extends AsyncTask<Void, Void, Void> {

        Context context;

        public CheckAapt(Context context) {
            this.context = context;
        }
        @Override
        protected Void doInBackground(Void... params) {
            try {
                FileUtils.deleteDirectory(new File(context.getCacheDir() + "/tempFolder/"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Downloading aapt
            File aapt = new File(context.getCacheDir() + "/aapt");

            if (!aapt.exists()) {
                for (String url : aaptUrls) {
                    try {
                        FileUtils.copyURLToFile(new URL(url), aapt);

                        Utils.CommandOutput output =
                                Utils.runCommand(aapt.getAbsolutePath() + " v", false);

                        if (output != null && StringUtils.isEmpty(output.error)) {
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Utils.applyPermissions(aapt.getAbsolutePath(), "700");

            return null;
        }
    }
}

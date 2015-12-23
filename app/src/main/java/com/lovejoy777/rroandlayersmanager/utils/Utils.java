package com.lovejoy777.rroandlayersmanager.utils;

import android.content.res.AssetManager;
import android.util.Log;

import com.lovejoy777.rroandlayersmanager.DeviceSingleton;

import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {

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
        return applyPermissions(new File(file), perms);
    }

    public static boolean applyPermissions(File file, String perms) {
        try {
            runCommand("chmod -R " + perms + " "
                    + file.getAbsolutePath(), true);
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
        if (!isRootAccessAvailable()) return false;
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
        if (!isRootAvailable()) return false;
        String folder = DeviceSingleton.getInstance().getMountFolder();
        runCommand("mount -o remount," + mountType + " " + folder + "\n", true);
        return true;
    }

    public static boolean isRootAccessAvailable() {
        if (!isRootAvailable()) return false;
        CommandOutput co = runCommand("echo \"\"", true);
        return co != null && co.exitCode == 0;
    }

    public static boolean isRootAvailable() {
        return findBinary("su");
    }

    public static boolean findBinary(String binaryName) {
        boolean found = false;
        String[] places = {"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
                "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
        for (String where : places) {
            if (new File(where + binaryName).exists()) {
                found = true;
                break;
            }
        }
        return found;
    }

    public static boolean copyAssetFolder(AssetManager assetManager,
                                          String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            if (!new File(toPath).mkdirs()) {
                throw new RuntimeException("cannot create directory: " + toPath);
            }
            boolean res = true;
            for (String file : files) {
                Log.d("TEST", "file=" + file);
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
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}

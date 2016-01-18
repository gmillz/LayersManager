package com.lovejoy777.rroandlayersmanager.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ThemeUtils {

    public static final String THEME_BOOTANIMATION_PATH = "bootanimation";
    public static final String THEME_WALLPAPER_PATH = "wallpapers";

    public static final String STOCK_BOOTANIMATION = "system/media/bootanimation.zip";

    public static final File STOCK_BOOT_BACKUP = new File("/system/media/bootbackup.zip");

    public static boolean assetsContainThemeParts(AssetManager am) {
        try {
            String[] assets = am.list("");
            List<String> list = Arrays.asList(assets);
            return list.size() != 0 &&
                    (list.contains(THEME_BOOTANIMATION_PATH)
                    || list.contains(THEME_WALLPAPER_PATH));
        } catch (IOException e) {
            return false;
        }
    }

    public static void backupStockBoot() {
        if (!STOCK_BOOT_BACKUP.exists()) {
            Utils.remount("rw", "/system");
            Utils.copyFile(STOCK_BOOTANIMATION, STOCK_BOOT_BACKUP.getAbsolutePath());
            Utils.applyPermissions(STOCK_BOOT_BACKUP.getAbsolutePath(), "644");
            Utils.remount("ro", "/system");
        }
    }

    /**
     * Scale the boot animation to better fit the device by editing the desc.txt found
     * in the bootanimation.zip
     * @param context Context to use for getting an instance of the WindowManager
     * @param input InputStream of the original bootanimation.zip
     * @param dst Path to store the newly created bootanimation.zip
     * @throws IOException
     */
    public static void copyAndScaleBootAnimation(Context context, InputStream input, String dst)
            throws IOException {
        final OutputStream os = new FileOutputStream(dst);
        final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));
        final ZipInputStream bootAni = new ZipInputStream(new BufferedInputStream(input));
        ZipEntry ze;

        zos.setMethod(ZipOutputStream.STORED);
        final byte[] bytes = new byte[4096];
        int len;
        while ((ze = bootAni.getNextEntry()) != null) {
            ZipEntry entry = new ZipEntry(ze.getName());
            entry.setMethod(ZipEntry.STORED);
            entry.setCrc(ze.getCrc());
            entry.setSize(ze.getSize());
            entry.setCompressedSize(ze.getSize());
            if (!ze.getName().equals("desc.txt")) {
                // just copy this entry straight over into the output zip
                zos.putNextEntry(entry);
                while ((len = bootAni.read(bytes)) > 0) {
                    zos.write(bytes, 0, len);
                }
            } else {
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(bootAni));
                final String[] info = reader.readLine().split(" ");

                int scaledWidth;
                int scaledHeight;
                WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
                DisplayMetrics dm = new DisplayMetrics();
                wm.getDefaultDisplay().getRealMetrics(dm);
                // just in case the device is in landscape orientation we will
                // swap the values since most (if not all) animations are portrait
                if (dm.widthPixels > dm.heightPixels) {
                    scaledWidth = dm.heightPixels;
                    scaledHeight = dm.widthPixels;
                } else {
                    scaledWidth = dm.widthPixels;
                    scaledHeight = dm.heightPixels;
                }

                int width = Integer.parseInt(info[0]);
                int height = Integer.parseInt(info[1]);

                if (width == height)
                    scaledHeight = scaledWidth;
                else {
                    // adjust scaledHeight to retain original aspect ratio
                    float scale = (float)scaledWidth / (float)width;
                    int newHeight = (int)((float)height * scale);
                    if (newHeight < scaledHeight)
                        scaledHeight = newHeight;
                }

                CRC32 crc32 = new CRC32();
                int size = 0;
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                line = String.format(Locale.ENGLISH,
                        "%d %d %s\n", scaledWidth, scaledHeight, info[2]);
                buffer.put(line.getBytes());
                size += line.getBytes().length;
                crc32.update(line.getBytes());
                while ((line = reader.readLine()) != null) {
                    line = String.format("%s\n", line);
                    buffer.put(line.getBytes());
                    size += line.getBytes().length;
                    crc32.update(line.getBytes());
                }
                entry.setCrc(crc32.getValue());
                entry.setSize(size);
                entry.setCompressedSize(size);
                zos.putNextEntry(entry);
                zos.write(buffer.array(), 0, size);
            }
            zos.closeEntry();
        }
        zos.close();
    }
}

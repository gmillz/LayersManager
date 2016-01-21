package com.bitsyko.liblayers.layerfiles;

import android.content.Context;

import com.bitsyko.liblayers.Color;
import com.bitsyko.liblayers.Layer;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

public class ColorOverlay extends LayerFile {

    private Color selectedColor;

    public ColorOverlay(Layer parentLayer, String name) {
        super(parentLayer, name);
    }

    @Override
    public File getFile(Context context) {

        if (file != null && file.exists()) return file;

        String cacheDir = Utils.getCacheDir() + File.separator
                + StringUtils.deleteWhitespace(parentLayer.getName()) + File.separator;

        if (!new File(cacheDir).exists()) {
            new File(cacheDir).mkdirs();
        }

        File colorZipFile = new File(cacheDir + selectedColor.getZip());

        if (!colorZipFile.exists()) {
            Utils.copyAsset(parentLayer.getAssetManager(), "Files" + File.separator
                    + selectedColor.getZip(), colorZipFile.getAbsolutePath());
        }

        File apkFile = new File(cacheDir + name);
        try {
            ZipFile generalZipFileAsZip = new ZipFile(colorZipFile);
            InputStream is = generalZipFileAsZip.getInputStream(generalZipFileAsZip.getEntry(name));
            Utils.copyInputStreamToFile(is, apkFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        apkFile.setReadable(true, false);

        file = apkFile;
        return apkFile;
    }

    public void setColor(Color color) {
        selectedColor = color;
    }


}

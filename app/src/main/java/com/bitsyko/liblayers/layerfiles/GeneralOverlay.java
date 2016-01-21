package com.bitsyko.liblayers.layerfiles;

import android.content.Context;

import com.bitsyko.liblayers.Layer;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

public class GeneralOverlay extends LayerFile {

    public GeneralOverlay(Layer parentLayer, String name) {
        super(parentLayer, name);
    }

    @Override
    public File getFile(Context context) {

        if (file != null && file.exists()) return file;

        String generalZip = parentLayer.getGeneralZip();

        String cacheDir = Utils.getCacheDir() + File.separator
                + StringUtils.deleteWhitespace(parentLayer.getName()) + File.separator;

        if (!new File(cacheDir).exists()) {
            new File(cacheDir).mkdirs();
        }

        File generalZipFile = new File(cacheDir + generalZip);

        if (generalZipFile.exists()) {
            generalZipFile.delete();
        }

        Utils.copyAsset(parentLayer.getAssetManager(), "Files" + File.separator
                + generalZip, generalZipFile.getAbsolutePath());

        File apkFile = new File(cacheDir + name);
        try {
            ZipFile general = new ZipFile(generalZipFile);
            InputStream is = general.getInputStream(general.getEntry(name));
            Utils.copyInputStreamToFile(is, apkFile);
            is.close();
            general.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        apkFile.setReadable(true, false);

        file = apkFile;

        return apkFile;
    }
}

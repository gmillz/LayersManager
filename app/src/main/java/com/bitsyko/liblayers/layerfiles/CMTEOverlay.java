package com.bitsyko.liblayers.layerfiles;

import android.content.Context;
import android.util.Log;

import com.bitsyko.liblayers.Layer;
import com.lovejoy777.rroandlayersmanager.overlaycreator.Overlay;
import com.lovejoy777.rroandlayersmanager.utils.OverlayParser;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import java.io.File;

public class CMTEOverlay extends LayerFile {

    private String mTargetPackage;
    private Overlay mOverlay;

    private File mPackageDir;

    public CMTEOverlay(Context context, Layer layer, String name, String targetPackage) {
        super(layer, name);

        mTargetPackage = targetPackage;
        mOverlay = new Overlay(context, targetPackage);
        mPackageDir = mOverlay.path;

    }

    public String getTargetPackage() {
        return mTargetPackage;
    }

    public String getPath() {
        return mPackageDir.getAbsolutePath();
    }

    public void dereferenceOverlay(OverlayParser parser) {
        mOverlay.dereferenceResources();
        parser.dereferenceCommonResources(mOverlay);
    }

    public void unpackOverlay() {
        Layer layer = getLayer();
        String targetPackage = getTargetPackage();
        File resDir = new File(mPackageDir + File.separator + "res");
        resDir.mkdirs();

        Utils.copyAssetFolder(layer.getAssetManager(), "overlays/"
                + targetPackage + "/res", resDir.getAbsolutePath());
    }

    public void createOverlay() {
        mOverlay.create();
    }

    public File getFile(Context context) {
        return null;
    }
}

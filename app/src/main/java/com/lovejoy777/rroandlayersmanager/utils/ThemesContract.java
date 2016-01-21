package com.lovejoy777.rroandlayersmanager.utils;

import android.graphics.Bitmap;

import com.bitsyko.liblayers.Layer;
import com.bitsyko.liblayers.layerfiles.LayerFile;
import com.lovejoy777.rroandlayersmanager.overlaycreator.Overlay;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ThemesContract {

    public static final String WALLPAPER = "wallpaper";
    public static final String BOOTANIMATION = "bootanimation";

    public boolean wallpaperChanged;
    public Bitmap wallpaper;

    public boolean bootAnimationChanged;
    public InputStream bootAnimation;

    public boolean overlaysChanged;
    public List<Overlay> overlays = new ArrayList<>();
    public List<LayerFile> layers = new ArrayList<>();

    public void setOverlays(List<Overlay> overlays) {
        this.overlays = overlays;
        overlaysChanged = true;
    }

    public void setLayers(List<LayerFile> layers) {
        this.layers = layers;
    }

    public void setWallpaper(Bitmap bitmap) {
        wallpaperChanged = true;
        wallpaper = bitmap;
    }

    public void setBootanimation(InputStream is) {
        bootAnimationChanged = true;
        bootAnimation = is;
    }
}

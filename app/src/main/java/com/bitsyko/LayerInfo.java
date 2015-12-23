package com.bitsyko;

import android.graphics.drawable.Drawable;

import java.util.Comparator;

public interface LayerInfo {

    Comparator<LayerInfo> compareName = new Comparator<LayerInfo>() {
        public int compare(LayerInfo layer1, LayerInfo layer2) {
            return layer1.getName().compareToIgnoreCase(layer2.getName());
        }
    };
    Comparator<LayerInfo> compareDev = new Comparator<LayerInfo>() {
        public int compare(LayerInfo layer1, LayerInfo layer2) {
            return layer1.getDeveloper().compareToIgnoreCase(layer2.getDeveloper());
        }
    };

    String getName();

    String getDeveloper();

    Drawable getIcon();

    String getPackageName();
}
package com.lovejoy777.rroandlayersmanager.utils;

import com.bitsyko.liblayers.Layer;
import com.bitsyko.liblayers.layerfiles.CMTEOverlay;
import com.lovejoy777.rroandlayersmanager.overlaycreator.Overlay;

import java.io.File;
import java.util.HashMap;

public class OverlayParser {

    CMTEOverlay mOverlay;
    HashMap<String, HashMap<String, String>> mCommonResources = new HashMap<>();
    private boolean resourcesFound = false;

    public OverlayParser(CMTEOverlay overlay) {
        mOverlay = overlay;

        for (String type : ResourceParser.resourceTypes) {
            mCommonResources.put(type, new HashMap<String, String>());
        }
    }

    public void parseStyles(Overlay overlay) {
        File styles = new File(overlay.getResDir() + "/values/styles.xml");
    }

    public void loadCommonResources() {
        Layer layer = mOverlay.getLayer();
        String commonDir = layer.extractCommonResources();
        for (File file : new File(commonDir).listFiles()) {
            if (!file.getName().contains("styles")) {
                ResourceParser.parseXML(file, mCommonResources);
            }
        }
    }

    public void dereferenceCommonResources(Overlay overlay) {
        overlay.loadResources();
        HashMap<String, HashMap<String, String>> resources = overlay.getResources();
        HashMap<String, ResourceParser.Style> styles = overlay.getStyles();
        for (String type : resources.keySet()) {
            HashMap<String, String> map = resources.get(type);
            for (String name : map.keySet()) {
                String value = map.get(name);
                if (value.contains("@")
                        && !value.contains("android:") && value.contains("common:")) {
                    String[] split = value.split("/");
                    String t = split[0].substring(1).split(":")[1];
                    String n = split[1];
                    resources.get(t).put(name, mCommonResources.get(t).get(n));
                }
            }
        }
        for (String name : styles.keySet()) {
            ResourceParser.Style style = styles.get(name);
            for (String na : style.contents.keySet()) {
                String value = style.contents.get(na);
                if (value.contains("@")
                        && !value.contains("android:") && value.contains("common:")) {
                    String[] split = value.split("/");
                    String t = split[0].substring(1);
                    String n = split[1];
                    if (t.equals("drawable")) continue;
                    styles.get(name).contents.put(na, mCommonResources.get(t).get(n));
                }
            }
        }
        overlay.loopResources();
    }

    public void findAndReplace(String path) {
        if (!resourcesFound) return;
        for (String key : mCommonResources.keySet()) {
            HashMap<String, String> map = mCommonResources.get(key);
            for (String s : map.keySet()) {
                String si = "@" + key + "/" + s;
            }
        }
    }
}

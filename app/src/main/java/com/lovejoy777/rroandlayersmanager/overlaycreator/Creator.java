package com.lovejoy777.rroandlayersmanager.overlaycreator;

import android.content.Context;

import java.io.File;

/**
 * Created by griff on 12/16/2015.
 */
public class Creator {

    File aapt;

    public Creator(Context context) {
        aapt = new File(context.getCacheDir() + "/aapt");
    }
}

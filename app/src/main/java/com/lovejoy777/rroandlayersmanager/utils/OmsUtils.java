package com.lovejoy777.rroandlayersmanager.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by gmillz on 1/21/16.
 */
public class OmsUtils {

    public static final String TAG = OmsUtils.class.getName();

    private static final String OVERLAY_DATA_FILE = "/data/system/overlays.xml";

    private static final String TAG_OVERLAYS = "overlays";
    private static final String TAG_OVERLAY = "overlay";
    private static final String TAG_TARGET = "target";
    private static final String TAG_USER = "user";

    private static final String ATTR_PATH = "path";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_STATE = "state";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_ID = "id";

    private static final int CURRENT_VERSION = 1;

    private Context mContext;

    private SparseArray<ArrayMap<String, ArrayList<OverlayInfo>>> mOverlays = new SparseArray<>();


    public OmsUtils(Context context) {
        mContext = context;
        parseOverlayFile();
    }

    public void parseOverlayFile() {
        XmlPullParser parser = Xml.newPullParser();
        String content = Utils.getStringFromFile(OVERLAY_DATA_FILE);
        if (TextUtils.isEmpty(content)) return;
        try {
            parser.setInput(IOUtils.toInputStream(content), "utf-8");
            XmlUtils.beginDocument(parser, TAG_OVERLAYS);
            int versionStr = XmlUtils.readIntAttribute(parser, ATTR_VERSION);
            switch (versionStr) {
                case CURRENT_VERSION:
                    break;
                default:
                    throw new XmlPullParserException("Unrecognized version " + versionStr);
            }

            int depth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, depth)) {
                switch (parser.getName()) {
                    case TAG_USER:
                        int userId = XmlUtils.readIntAttribute(parser, ATTR_ID);
                        ArrayMap<String, ArrayList<OverlayInfo>> userOverlays = new ArrayMap<>();
                        readTargetOverlays(parser, userId, userOverlays);
                        mOverlays.put(userId, userOverlays);
                        break;
                }
            }
        } catch (XmlPullParserException|ClassCastException|IOException e) {
            Log.e(TAG, "Failed to parse Xml");
        }
    }

    private void readTargetOverlays(XmlPullParser parser, int userId,
                                   ArrayMap<String, ArrayList<OverlayInfo>> overlays)
            throws IOException, XmlPullParserException {
        int depth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, depth)) {
            switch (parser.getName()) {
                case TAG_TARGET:
                    String targetPackage = XmlUtils.readStringAttribute(parser, ATTR_NAME);
                    overlays.put(targetPackage, readOverlays(targetPackage, parser, userId));
                    break;
            }
        }
    }

    private ArrayList<OverlayInfo> readOverlays(String targetPackage, XmlPullParser parser,
                                                int userId) throws IOException, XmlPullParserException {
        int depth = parser.getDepth();
        ArrayList<OverlayInfo> overlays = new ArrayList<>();
        while (XmlUtils.nextElementWithin(parser, depth)) {
            switch (parser.getName()) {
                case TAG_OVERLAY:
                    overlays.add(readOverlay(userId, targetPackage, parser));
            }
        }
        return overlays;
    }

    private OverlayInfo readOverlay(int userId, String targetPackage, XmlPullParser parser)
            throws IOException {
        String packageName = XmlUtils.readStringAttribute(parser, ATTR_NAME);
        String baseCodePath = XmlUtils.readStringAttribute(parser, ATTR_PATH);
        int state = XmlUtils.readIntAttribute(parser, ATTR_STATE);
        return new OverlayInfo(packageName, targetPackage, baseCodePath, state, userId);
    }
}

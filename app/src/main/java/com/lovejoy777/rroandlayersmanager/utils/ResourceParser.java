package com.lovejoy777.rroandlayersmanager.utils;

import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ResourceParser {

    public static final ArrayList<String> resourceTypes = new ArrayList<>();

    static {
        resourceTypes.add("color");
        resourceTypes.add("string");
        //resourceTypes.add("drawable");
        resourceTypes.add("dimen");
    }

    public static void parseXML(File file, HashMap<String, HashMap<String, String>> map) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new FileInputStream(file), null);

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                if (!TextUtils.isEmpty(name)) {
                    if (resourceTypes.contains(name)) {
                        parseResource(parser, name, map);
                    }
                }
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void parseStyleXML(File file, HashMap<String, Style> map) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new FileInputStream(file), null);

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                if (name != null && name.equals("style")) {
                    parseStyle(parser, map);
                }
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseStyle(XmlPullParser parser,
                                   HashMap<String, Style> map)
            throws XmlPullParserException, IOException {
        Style style = new Style();
        int size = parser.getAttributeCount();
        for (int i = 0; i < size; i++) {
            String attrName = parser.getAttributeName(i);
            String attrValue = parser.getAttributeValue(i);
            if (attrName.equals("name")) {
                style.name = attrValue;
            } else if (attrName.equals("parent")) {
                style.parent = attrValue;
            }
        }
        parser.next();
        String name = parser.getName();
        while (name == null) {
            parser.next();
            name = parser.getName();
        }
        while (!name.equals("style")) {
            if (name.equals("item")) {
                size = parser.getAttributeCount();
                for (int i = 0; i < size; i++) {
                    String attrName = parser.getAttributeName(i);
                    String attrValue = parser.getAttributeValue(i);
                    if (attrName.equals("name")) {
                        parser.next();
                        String v = parser.getText();
                        style.contents.put(attrValue, v);
                    }
                }
            }
            parser.next();
            name = parser.getName();
            while (name == null) {
                parser.next();
                name = parser.getName();
            }
        }
        map.put(style.name, style);
    }

    private static void parseResource(XmlPullParser parser, String resType,
                                      HashMap<String, HashMap<String, String>> map)
            throws XmlPullParserException, IOException {
        int size = parser.getAttributeCount();
        for (int i = 0; i < size; i++) {
            String attrName = parser.getAttributeName(i);
            String attrValue = parser.getAttributeValue(i);
            if (attrName.equals("name")) {
                int eventType = parser.next();
                while (eventType != XmlPullParser.END_TAG) {
                    if (eventType == XmlPullParser.TEXT) {
                        if (parser.getText() != null) {
                            map.get(resType).put(attrValue, parser.getText());
                            Log.d("TEST", "value=" + map.get(resType).get(attrName));
                        }
                    }

                    eventType = parser.next();
                }
            }
        }
    }

    public static class Style {
        public String name;
        public String parent;
        public HashMap<String, String> contents = new HashMap<>();
    }

}

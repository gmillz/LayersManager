package com.bitsyko.libicons;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.adw.launcher.IconShader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bitsyko.LayerInfo;
import com.lovejoy777.rroandlayersmanager.R;

public class IconPack {


    public  static final int REQUEST_PICK_ICON = 13;

    static final String ICON_MASK_TAG = "iconmask";
    static final String ICON_BACK_TAG = "iconback";
    static final String ICON_UPON_TAG = "iconupon";
    static final String ICON_SCALE_TAG = "scale";
    static final String ICON_ROTATE_TAG = "rotate";
    static final String ICON_TRANSLATE_TAG = "translate";
    private static final String ICON_BACK_FORMAT = "iconback%d";

    // Palettized icon background constants
    private static final String ICON_PALETTIZED_BACK_TAG = "paletteback";
    private static final String IMG_ATTR = "img";
    private static final String SWATCH_TYPE_ATTR = "swatchType";
    private static final String DEFAULT_SWATCH_COLOR_ATTR = "defaultSwatchColor";
    private static final String VIBRANT_VALUE = "vibrant";
    private static final String VIBRANT_LIGHT_VALUE = "vibrantLight";
    private static final String VIBRANT_DARK_VALUE = "vibrantDark";
    private static final String MUTED_VALUE = "muted";
    private static final String MUTED_LIGHT_VALUE = "mutedLight";
    private static final String MUTED_DARK_VALUE = "mutedDark";

    // Rotation and translation constants
    private static final String ANGLE_ATTR = "angle";
    private static final String ANGLE_VARIANCE = "plusMinus";
    private static final String TRANSLATE_X_ATTR = "xOffset";
    private static final String TRANSLATE_Y_ATTR = "yOffset";

    private static final ComponentName ICON_MASK_COMPONENT;
    private static final ComponentName ICON_UPON_COMPONENT;
    private static final ComponentName ICON_SCALE_COMPONENT;

    public final static String[] sSupportedActions = new String[] {
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme",
            "com.novalauncher.THEME"
    };

    public static final String[] sSupportedCategories = new String[] {
            "com.fede.launcher.THEME_ICONPACK",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME"
    };

    // Holds package/class -> drawable
    private Map<ComponentName, String> mIconPackResources;
    private final Context mContext;
    private String mIconPackName;
    private Resources mIconPackResource;
    private Drawable mIconUpon, mIconMask;
    private Drawable[] mIconBacks;
    private static int mIconBackCount;
    private float mIconScale;
    private Drawable mIconPaletteBack;
    private SwatchType mSwatchType;
    private int[] mDefaultSwatchColors;
    private float[] mColorFilter;
    private ColorFilterUtils.Builder mFilterBuilder;
    private float mIconRotation;
    private float mIconTranslationX;
    private float mIconTranslationY;
    private float mIconRotationVariance;
    private int mIconSize;

    private IconShader.CompiledIconShader mIconShader;

    private String mName = "";
    private String mDescription = "";
    private String mWhatsNew = "";
    private List<Drawable> mPreviews = new ArrayList<>();

    private static final Random sRandom = new Random();

    static {
        ICON_MASK_COMPONENT = new ComponentName(ICON_MASK_TAG, "");
        ICON_UPON_COMPONENT = new ComponentName(ICON_UPON_TAG, "");
        ICON_SCALE_COMPONENT = new ComponentName(ICON_SCALE_TAG, "");
    }

    public enum SwatchType {
        None,
        Vibrant,
        VibrantLight,
        VibrantDark,
        Muted,
        MutedLight,
        MutedDark
    }

    public Drawable getIconBack() {
        return mIconBacks[sRandom.nextInt(mIconBacks.length)];
    }

    public Drawable getIconPaletteBack() {
        return mIconPaletteBack;
    }

    public int getDefaultSwatchColor() {
        if (mDefaultSwatchColors != null && mDefaultSwatchColors.length >= 1)
            return mDefaultSwatchColors[sRandom.nextInt(mDefaultSwatchColors.length)];

        return 0;
    }

    public SwatchType getSwatchType() {
        return mSwatchType;
    }

    public Drawable getIconMask() {
        return mIconMask;
    }

    public Drawable getIconUpon() {
        return mIconUpon;
    }

    public float getIconScale() {
        return mIconScale;
    }

    public float[] getColorFilter() {
        return mColorFilter;
    }

    public float getIconAngle() {
        if (mIconRotationVariance != 0) {
            return (mIconRotation + (sRandom.nextFloat() * (mIconRotationVariance * 2))
                    - mIconRotationVariance);
        }
        return mIconRotation;
    }

    public float getTranslationX() {
        return mIconTranslationX;
    }

    public float getTranslationY() {
        return mIconTranslationY;
    }

    public String getName() {
        return mName;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getWhatsNew() {
        return mWhatsNew;
    }

    public Drawable getIcon() {
        if (mIconPackName.equals("default")) {
            return mContext.getResources().getDrawable(android.R.drawable.sym_def_app_icon, null);
        }
        try {
            return mContext.getPackageManager().getApplicationIcon(mIconPackName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Drawable> getPreviewImages() {
        return mPreviews;
    }

    public IconShader.CompiledIconShader getIconShader() {
        return mIconShader;
    }

    public int getIconSize() {
        return mIconSize;
    }

    public IconPack(Context context, String packageName) {
        mContext = context;
        mIconPackName = packageName;

        if (!packageName.equals("default")) {
            mIconPackResources = new HashMap<>();
            mFilterBuilder = new ColorFilterUtils.Builder();

            ActivityManager am =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            mIconSize = am.getLauncherLargeIconSize();

            try {
                mName = String.valueOf(context.getPackageManager().getApplicationLabel(
                        context.getPackageManager().getApplicationInfo(packageName, 0)));
            } catch (Exception e) {
                // ignore
            }
            loadIconPack(packageName);
            loadThemeDetails();
        } else {
            mName = "Default";
        }
    }

    private Drawable getDrawableForName(ComponentName name) {
        if (isIconPackLoaded()) {
            String item = mIconPackResources.get(name);
            if (!TextUtils.isEmpty(item)) {
                int id = getResourceIdForDrawable(item);
                if (id != 0) {
                    return mIconPackResource.getDrawable(id, null);
                }
            }
        }
        return null;
    }

    public static ArrayList<IconPack> getIconPacks(Context context) {
        ArrayList<IconPack> iconPacks = new ArrayList<>();
        Map<String, IconPackInfo> map = getSupportedPackages(context);
        for (String key : map.keySet()) {
            IconPackInfo info = map.get(key);
            iconPacks.add(new IconPack(context, info.packageName));
        }
        return iconPacks;
    }

    public static Map<String, IconPackInfo> getSupportedPackages(Context context) {
        Intent i = new Intent();
        Map<String, IconPackInfo> packages = new HashMap<>();
        PackageManager packageManager = context.getPackageManager();
        for (String action : sSupportedActions) {
            i.setAction(action);
            for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
                IconPackInfo info = new IconPackInfo(r, packageManager);
                packages.put(r.activityInfo.packageName, info);
            }
        }
        i = new Intent(Intent.ACTION_MAIN);
        for (String category : sSupportedCategories) {
            i.addCategory(category);
            for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
                IconPackInfo info = new IconPackInfo(r, packageManager);
                packages.put(r.activityInfo.packageName, info);
            }
            i.removeCategory(category);
        }
        return packages;
    }

    private void loadThemeDetails() {
        XmlPullParser parser = null;
        int resId = mIconPackResource.getIdentifier("themecfg", "xml", getPackageName());
        if (resId != 0) {
            parser = mIconPackResource.getXml(resId);
        } else {
            try {
                InputStream inputStream = mIconPackResource.getAssets().open("themecfg.xml");
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                parser = factory.newPullParser();
                parser.setInput(inputStream, null);
            } catch (Exception e) {
                // ignore
            }
        }

        if (parser != null) {
            try {
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    String name = parser.getName();
                    Log.d("TEST", "name=" + name);
                    if (name == null) continue;
                    switch (name) {
                        case "themeName":
                            mDescription = parser.nextText();
                            break;
                        case "themeInfo":
                            mWhatsNew = parser.nextText();
                            break;
                        case "preview":
                        int i = parser.getAttributeCount();
                        for (int in = 0; in < i; in++) {
                            String val = parser.getAttributeValue(in);
                            mPreviews.add(
                                    mIconPackResource.getDrawable(
                                            getResourceIdForDrawable(val), null));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void pickIconPack(final Fragment fragment) {
        final Context context = fragment.getActivity();
        final Map<String, IconPackInfo> supportedPackages = getSupportedPackages(context);
        if (supportedPackages.isEmpty()) {
            Toast.makeText(context, R.string.no_iconpacks_summary, Toast.LENGTH_SHORT).show();
            return;
        }

        final IconAdapter adapter;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.dialog_pick_iconpack_title);
            adapter = new IconAdapter(context, supportedPackages);
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String selectedPackage = adapter.getItem(which);
                    Intent i = new Intent();
                    i.setClass(context, IconPickerActivity.class);
                    i.putExtra("package", selectedPackage);
                    fragment.startActivityForResult(i, REQUEST_PICK_ICON);
                    dialog.dismiss();
                }
            });
        builder.show();
    }

    private static class IconAdapter extends BaseAdapter {
        ArrayList<IconPackInfo> mSupportedPackages;
        Context mContext;

        IconAdapter(Context ctx, Map<String, IconPackInfo> supportedPackages) {
            mContext = ctx;
            mSupportedPackages = new ArrayList<>(supportedPackages.values());
            Collections.sort(mSupportedPackages, new Comparator<IconPackInfo>() {
                @Override
                public int compare(IconPackInfo lhs, IconPackInfo rhs) {
                    return lhs.label.toString().compareToIgnoreCase(rhs.label.toString());
                }
            });
        }

        @Override
        public int getCount() {
            return mSupportedPackages.size();
        }

        @Override
        public String getItem(int position) {
            return mSupportedPackages.get(position).packageName;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(mContext, R.layout.iconpack_chooser, null);
            }
            IconPackInfo info = mSupportedPackages.get(position);
            TextView txtView = (TextView) convertView.findViewById(R.id.title);
            txtView.setText(info.label);
            ImageView imgView = (ImageView) convertView.findViewById(R.id.icon);
            imgView.setImageDrawable(info.icon);
            return convertView;
        }
    }

    public static ArrayList<IconPickerActivity.Item> getCustomIconPackResources(
            Context context, String packageName) {
        Resources res;
        try {
            res = context.getPackageManager().getResourcesForApplication(packageName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        XmlResourceParser parser = null;
        ArrayList<IconPickerActivity.Item> iconPackResources = new ArrayList<>();

        try {
            parser = res.getAssets().openXmlResourceParser("drawable.xml");
        } catch (IOException e) {
            int resId = res.getIdentifier("drawable", "xml", packageName);
            if (resId != 0) {
                parser = res.getXml(resId);
            }
        }

        if (parser != null) {
            try {
                loadCustomResourcesFromXmlParser(parser, iconPackResources);
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            } finally {
                parser.close();
            }
        }
        return iconPackResources;
    }

    private static void loadCustomResourcesFromXmlParser(
            XmlPullParser parser, ArrayList<IconPickerActivity.Item> iconPackResources)
            throws XmlPullParserException, IOException {

        int eventType = parser.getEventType();
        do {
            if (eventType != XmlPullParser.START_TAG) {
                continue;
            }

            if (parser.getName().equalsIgnoreCase("item")) {
                String drawable = parser.getAttributeValue(null, "drawable");
                if (TextUtils.isEmpty(drawable) || drawable.length() == 0) {
                    continue;
                }
                IconPickerActivity.Item item = new IconPickerActivity.Item();
                item.isIcon = true;
                item.title = drawable;
                iconPackResources.add(item);
            } else if (parser.getName().equalsIgnoreCase("category")) {
                String title = parser.getAttributeValue(null, "title");
                if (TextUtils.isEmpty(title) || title.length() == 0) {
                    continue;
                }
                IconPickerActivity.Item item = new IconPickerActivity.Item();
                item.isHeader = true;
                item.title = title;
                iconPackResources.add(item);
            }
        } while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT);

    }

    private void loadResourcesFromXmlParser(XmlPullParser parser,
                                            Map<ComponentName, String> iconPackResources)
            throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        do {

            if (eventType != XmlPullParser.START_TAG) {
                continue;
            }

            if (parser.getAttributeCount() >= 1) {
                if (parser.getName().equalsIgnoreCase(ICON_BACK_TAG)) {
                    mIconBackCount = parser.getAttributeCount();
                    for (int i = 0; i < mIconBackCount; i++) {
                        String tag = String.format(ICON_BACK_FORMAT, i);
                        String icon = parser.getAttributeValue(i);
                        iconPackResources.put(new ComponentName(tag, ""), icon);
                    }
                }
            }

            if (parser.getName().equalsIgnoreCase(ICON_MASK_TAG) ||
                    parser.getName().equalsIgnoreCase(ICON_UPON_TAG)) {
                String icon = parser.getAttributeValue(null, "img");
                if (icon == null) {
                    if (parser.getAttributeCount() == 1) {
                        icon = parser.getAttributeValue(0);
                    }
                }
                iconPackResources.put(new ComponentName(parser.getName().toLowerCase(), ""), icon);
                continue;
            }

            if (ColorFilterUtils.parseIconFilter(parser, mFilterBuilder)) {
                continue;
            }

            if (parseRotationComponent(parser)) {
                continue;
            }

            if (parseTranslationComponent(parser)) {
                continue;
            }

            if (ICON_PALETTIZED_BACK_TAG.equalsIgnoreCase(parser.getName())) {
                parsePalettizedBackground(parser);
            }

            if (parser.getName().equalsIgnoreCase(ICON_SCALE_TAG)) {
                String factor = parser.getAttributeValue(null, "factor");
                if (factor == null) {
                    if (parser.getAttributeCount() == 1) {
                        factor = parser.getAttributeValue(0);
                    }
                }
                iconPackResources.put(ICON_SCALE_COMPONENT, factor);
                continue;
            }

            if (!parser.getName().equalsIgnoreCase("item")) {
                continue;
            }

            String component = parser.getAttributeValue(null, "component");
            String drawable = parser.getAttributeValue(null, "drawable");

            // Validate component/drawable exist
            if (TextUtils.isEmpty(component) || TextUtils.isEmpty(drawable)) {
                continue;
            }

            // Validate format/length of component
            if (!component.startsWith("ComponentInfo{") || !component.endsWith("}")
                    || component.length() < 16) {
                continue;
            }

            // Sanitize stored value
            component = component.substring(14, component.length() - 1).toLowerCase();

            ComponentName name;
            if (!component.contains("/")) {
                // Package icon reference
                name = new ComponentName(component.toLowerCase(), "");
            } else {
                name = ComponentName.unflattenFromString(component);
            }

            if (name != null) {
                iconPackResources.put(name, drawable);
            }
        } while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT);
    }

    private void parsePalettizedBackground(XmlPullParser parser) {
        int attrCount = parser.getAttributeCount();
        ArrayList<Integer> convertedColors = new ArrayList<>();
        for (int i = 0; i < attrCount; i++) {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            if (IMG_ATTR.equalsIgnoreCase(name)) {
                mIconPaletteBack =
                        mIconPackResource.getDrawable(getResourceIdForDrawable(value), null);
            } else if (SWATCH_TYPE_ATTR.equalsIgnoreCase(name)) {
                SwatchType type = SwatchType.None;
                if (VIBRANT_VALUE.equalsIgnoreCase(value)) {
                    type = SwatchType.Vibrant;
                } else if (VIBRANT_LIGHT_VALUE.equalsIgnoreCase(value)) {
                    type = SwatchType.VibrantLight;
                } else if (VIBRANT_DARK_VALUE.equalsIgnoreCase(value)) {
                    type = SwatchType.VibrantDark;
                } else if (MUTED_VALUE.equalsIgnoreCase(value)) {
                    type = SwatchType.Muted;
                } else if (MUTED_LIGHT_VALUE.equalsIgnoreCase(value)) {
                    type = SwatchType.MutedLight;
                } else if (MUTED_DARK_VALUE.equalsIgnoreCase(value)) {
                    type = SwatchType.MutedDark;
                }
                mSwatchType = type;
            } else if (name.startsWith(DEFAULT_SWATCH_COLOR_ATTR)) {
                try {
                    // ensure alpha is always 0xff
                    convertedColors.add(Color.parseColor(value) | 0xff000000);
                } catch (IllegalArgumentException e) {
                    //ignore
                }
            }
            if (convertedColors.size() > 0) {
                mDefaultSwatchColors = new int[convertedColors.size()];
                for (int j = 0; j < convertedColors.size(); j++) {
                    mDefaultSwatchColors[j] = convertedColors.get(j);
                }
            }
        }
    }

    private boolean parseRotationComponent(XmlPullParser parser) {
        if (!parser.getName().equalsIgnoreCase(ICON_ROTATE_TAG)) return false;

        String angle = parser.getAttributeValue(null, ANGLE_ATTR);
        String variance = parser.getAttributeValue(null, ANGLE_VARIANCE);
        if (angle != null) {
            try {
                mIconRotation = Float.valueOf(angle);
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        if (variance != null) {
            try {
                mIconRotationVariance = Float.valueOf(variance);
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        return true;
    }

    public boolean shouldComposeIcon() {
        return mIconBacks != null ||
                mIconUpon != null ||
                mColorFilter != null ||
                mIconPaletteBack != null ||
                mIconRotation != 0 ||
                mIconRotationVariance != 0 ||
                mIconTranslationX != 0 ||
                mIconTranslationY != 0 ||
                mIconScale != 1f;

    }

    private boolean parseTranslationComponent(XmlPullParser parser) {
        if (!parser.getName().equalsIgnoreCase(ICON_TRANSLATE_TAG)) return false;

        final float density = mContext.getResources().getDisplayMetrics().density;
        String translateX = parser.getAttributeValue(null, TRANSLATE_X_ATTR);
        String translateY = parser.getAttributeValue(null, TRANSLATE_Y_ATTR);
        if (translateX != null) {
            try {
                mIconTranslationX = Float.valueOf(translateX) * density;
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        if (translateY != null) {
            try {
                mIconTranslationY = Float.valueOf(translateY) * density;
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        return true;
    }

    private static void loadApplicationResources(Context context,
                                                 Map<ComponentName, String> iconPackResources,
                                                 String packageName) {
        Field[] drawableItems;
        try {
            Context appContext = context.createPackageContext(packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            drawableItems = Class.forName(packageName+".R$drawable",
                    true, appContext.getClassLoader()).getFields();
        } catch (Exception e) {
            return;
        }

        ComponentName compName;
        for (Field f : drawableItems) {
            String name = f.getName();

            String icon = name.toLowerCase();
            name = name.replaceAll("_", ".");

            compName = new ComponentName(name, "");
            iconPackResources.put(compName, icon);

            int activityIndex = name.lastIndexOf(".");
            if (activityIndex <= 0 || activityIndex == name.length() - 1) {
                continue;
            }

            String iconPackage = name.substring(0, activityIndex);
            if (TextUtils.isEmpty(iconPackage)) {
                continue;
            }

            String iconActivity = name.substring(activityIndex + 1);
            if (TextUtils.isEmpty(iconActivity)) {
                continue;
            }

            // Store entries as lower case to ensure match
            iconPackage = iconPackage.toLowerCase();
            iconActivity = iconActivity.toLowerCase();

            iconActivity = iconPackage + "." + iconActivity;
            compName = new ComponentName(iconPackage, iconActivity);
            iconPackResources.put(compName, icon);
        }
    }

    public boolean loadIconPack(String packageName) {
        mIconPackResources = getIconPackResources(mContext, packageName);
        Resources res;
        try {
            res = mContext.getPackageManager().getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        mIconPackResource = res;
        mIconMask = getDrawableForName(ICON_MASK_COMPONENT);
        mIconUpon = getDrawableForName(ICON_UPON_COMPONENT);
        String scale = mIconPackResources.get(ICON_SCALE_COMPONENT);
        if (scale != null) {
            try {
                mIconScale = Float.valueOf(scale);
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        if (mIconBackCount > 0) {
            mIconBacks = new Drawable[mIconBackCount];
            for (int i = 0; i < mIconBacks.length; i++) {
                mIconBacks[i] = getDrawableForName(
                        new ComponentName(String.format(ICON_BACK_FORMAT, i), ""));
            }
        }
        ColorMatrix cm = mFilterBuilder.build();
        if (cm != null) {
            mColorFilter = cm.getArray().clone();
        }
        loadShader();
        return true;
    }

    public void loadShader() {
        int resId = mIconPackResource.getIdentifier("shader", "xml", getPackageName());
        if (resId != 0) {
            XmlPullParser parser = mIconPackResource.getXml(resId);
            mIconShader = IconShader.parseXml(parser, mIconSize * mIconSize);
        }
    }

    public Map<ComponentName, String> getIconPackResources(
            Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }

        Resources res;
        try {
            res = context.getPackageManager().getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        XmlPullParser parser = null;
        InputStream inputStream = null;
        Map<ComponentName, String> iconPackResources = new HashMap<>();

        try {
            inputStream = res.getAssets().open("appfilter.xml");
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            parser = factory.newPullParser();
            parser.setInput(inputStream, "UTF-8");
        } catch (Exception e) {
            // Catch any exception since we want to fall back to parsing the xml/
            // resource in all cases
            int resId = res.getIdentifier("appfilter", "xml", packageName);
            if (resId != 0) {
                parser = res.getXml(resId);
            }
        }

        if (parser != null) {
            try {
                loadResourcesFromXmlParser(parser, iconPackResources);
                return iconPackResources;
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            } finally {
                // Cleanup resources
                if (parser instanceof XmlResourceParser) {
                    ((XmlResourceParser) parser).close();
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
        }

        // Application uses a different theme format (most likely launcher pro)
        int arrayId = res.getIdentifier("theme_iconpack", "array", packageName);
        if (arrayId == 0) {
            arrayId = res.getIdentifier("icon_pack", "array", packageName);
        }

        if (arrayId != 0) {
            String[] iconPack = res.getStringArray(arrayId);
            ComponentName compName;
            for (String entry : iconPack) {

                if (TextUtils.isEmpty(entry)) {
                    continue;
                }

                String icon = entry.toLowerCase();
                entry = entry.replaceAll("_", ".");

                compName = new ComponentName(entry.toLowerCase(), "");
                iconPackResources.put(compName, icon);

                int activityIndex = entry.lastIndexOf(".");
                if (activityIndex <= 0 || activityIndex == entry.length() - 1) {
                    continue;
                }

                String iconPackage = entry.substring(0, activityIndex);
                if (TextUtils.isEmpty(iconPackage)) {
                    continue;
                }

                String iconActivity = entry.substring(activityIndex + 1);
                if (TextUtils.isEmpty(iconActivity)) {
                    continue;
                }

                // Store entries as lower case to ensure match
                iconPackage = iconPackage.toLowerCase();
                iconActivity = iconActivity.toLowerCase();

                iconActivity = iconPackage + "." + iconActivity;
                compName = new ComponentName(iconPackage, iconActivity);
                iconPackResources.put(compName, icon);
            }
        } else {
            loadApplicationResources(context, iconPackResources, packageName);
        }
        return iconPackResources;
    }

    boolean isIconPackLoaded() {
        return mIconPackResource != null &&
                mIconPackName != null &&
                mIconPackResources != null;
    }

    private int getResourceIdForDrawable(String resource) {
        return mIconPackResource.getIdentifier(resource, "drawable", mIconPackName);
    }

    public Resources getIconPackResources() {
        return mIconPackResource;
    }

    public String getPackageName() {
        return mIconPackName;
    }

    public int getResourceIdForActivityIcon(ActivityInfo info) {
        if (!isIconPackLoaded()) {
            return 0;
        }
        ComponentName cn = new ComponentName(info.packageName.toLowerCase(),
                info.name.toLowerCase());
        String drawable = mIconPackResources.get(cn);
        if (drawable == null) {
            // Icon pack doesn't have an icon for the activity, fallback to package icon
            cn = new ComponentName(info.packageName.toLowerCase(), "");
            drawable = mIconPackResources.get(cn);
            if (drawable == null) {
                return 0;
            }
        }
        return getResourceIdForDrawable(drawable);
    }

    public static class IconPackInfo implements LayerInfo {
        String packageName;
        CharSequence label;
        Drawable icon;

        IconPackInfo(ResolveInfo r, PackageManager packageManager) {
            packageName = r.activityInfo.packageName;
            icon = r.loadIcon(packageManager);
            label = r.loadLabel(packageManager);
        }

        public String getName() {
            return String.valueOf(label);
        }

        public Drawable getIcon() {
            return icon;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getDeveloper() {
            return "";
        }
    }

    public static class ColorFilterUtils {
        private static final String TAG_FILTER = "filter";
        private static final String FILTER_HUE = "hue";
        private static final String FILTER_SATURATION = "saturation";
        private static final String FILTER_INVERT = "invert";
        private static final String FILTER_BRIGHTNESS = "brightness";
        private static final String FILTER_CONTRAST = "contrast";
        private static final String FILTER_ALPHA = "alpha";
        private static final String FILTER_TINT = "tint";

        private static final int MIN_HUE = -180;
        private static final int MAX_HUE = 180;
        private static final int MIN_SATURATION = 0;
        private static final int MAX_SATURATION = 200;
        private static final int MIN_BRIGHTNESS = 0;
        private static final int MAX_BRIGHTNESS = 200;
        private static final int MIN_CONTRAST = -100;
        private static final int MAX_CONTRAST = 100;
        private static final int MIN_ALPHA = 0;
        private static final int MAX_ALPHA = 100;

        public static boolean parseIconFilter(XmlPullParser parser, Builder builder)
                throws IOException, XmlPullParserException {
            String tag = parser.getName();
            if (!TAG_FILTER.equals(tag)) return false;

            int attrCount = parser.getAttributeCount();
            String attrName;
            String attr = null;
            int intValue;
            while (attrCount-- > 0) {
                attrName = parser.getAttributeName(attrCount);
                if (attrName.equals("name")) {
                    attr = parser.getAttributeValue(attrCount);
                }
            }
            String content = parser.nextText();
            if (attr != null && content != null && content.length() > 0) {
                content = content.trim();
                if (FILTER_HUE.equalsIgnoreCase(attr)) {
                    intValue = clampValue(getInt(content, 0),MIN_HUE, MAX_HUE);
                    builder.hue(intValue);
                } else if (FILTER_SATURATION.equalsIgnoreCase(attr)) {
                    intValue = clampValue(getInt(content, 100),
                            MIN_SATURATION, MAX_SATURATION);
                    builder.saturate(intValue);
                } else if (FILTER_INVERT.equalsIgnoreCase(attr)) {
                    if ("true".equalsIgnoreCase(content)) {
                        builder.invertColors();
                    }
                } else if (FILTER_BRIGHTNESS.equalsIgnoreCase(attr)) {
                    intValue = clampValue(getInt(content, 100),
                            MIN_BRIGHTNESS, MAX_BRIGHTNESS);
                    builder.brightness(intValue);
                } else if (FILTER_CONTRAST.equalsIgnoreCase(attr)) {
                    intValue = clampValue(getInt(content, 0),
                            MIN_CONTRAST, MAX_CONTRAST);
                    builder.contrast(intValue);
                } else if (FILTER_ALPHA.equalsIgnoreCase(attr)) {
                    intValue = clampValue(getInt(content, 100), MIN_ALPHA, MAX_ALPHA);
                    builder.alpha(intValue);
                } else if (FILTER_TINT.equalsIgnoreCase(attr)) {
                    try {
                        intValue = Color.parseColor(content);
                        builder.tint(intValue);
                    } catch (IllegalArgumentException e) {
                        // ignore
                    }
                }
            }
            return true;
        }

        private static int getInt(String value, int defaultValue) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        private static int clampValue(int value, int min, int max) {
            return Math.min(max, Math.max(min, value));
        }

        /**
         * See the following links for reference
         * http://groups.google.com/group/android-developers/browse_thread/thread/9e215c83c3819953
         * http://gskinner.com/blog/archives/2007/12/colormatrix_cla.html
         */
        public static ColorMatrix adjustHue(float value) {
            ColorMatrix cm = new ColorMatrix();
            value = value / 180 * (float) Math.PI;
            if (value != 0) {
                float cosVal = (float) Math.cos(value);
                float sinVal = (float) Math.sin(value);
                float lumR = 0.213f;
                float lumG = 0.715f;
                float lumB = 0.072f;
                float[] mat = new float[]{
                        lumR + cosVal * (1 - lumR) + sinVal * (-lumR),
                        lumG + cosVal * (-lumG) + sinVal * (-lumG),
                        lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0, 0,
                        lumR + cosVal * (-lumR) + sinVal * (0.143f),
                        lumG + cosVal * (1 - lumG) + sinVal * (0.140f),
                        lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0, 0,
                        lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)),
                        lumG + cosVal * (-lumG) + sinVal * (lumG),
                        lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0, 0,
                        0, 0, 0, 1, 0,
                        0, 0, 0, 0, 1};
                cm.set(mat);
            }
            return cm;
        }

        public static ColorMatrix adjustSaturation(float saturation) {
            saturation = Math.min(Math.max(saturation / 100, 0), 2);
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(saturation);

            return cm;
        }

        public static ColorMatrix invertColors() {
            float[] matrix = {
                    -1, 0, 0, 0, 255, //red
                    0, -1, 0, 0, 255, //green
                    0, 0, -1, 0, 255, //blue
                    0, 0, 0, 1, 0 //alpha
            };

            return new ColorMatrix(matrix);
        }

        public static ColorMatrix adjustBrightness(float brightness) {
            brightness = Math.min(Math.max(brightness / 100, 0), 1);
            ColorMatrix cm = new ColorMatrix();
            cm.setScale(brightness, brightness, brightness, 1);

            return cm;
        }

        public static ColorMatrix adjustContrast(float contrast) {
            contrast = Math.min(Math.max(contrast / 100, 0), 1) + 1;
            float o = (-0.5f * contrast + 0.5f) * 255;
            float[] matrix = {
                    contrast, 0, 0, 0, o, //red
                    0, contrast, 0, 0, o, //green
                    0, 0, contrast, 0, o, //blue
                    0, 0, 0, 1, 0 //alpha
            };

            return new ColorMatrix(matrix);
        }

        public static ColorMatrix adjustAlpha(float alpha) {
            alpha = Math.min(Math.max(alpha / 100, 0), 1);
            ColorMatrix cm = new ColorMatrix();
            cm.setScale(1, 1, 1, alpha);

            return cm;
        }

        public static ColorMatrix applyTint(int color) {
            float alpha = Color.alpha(color) / 255f;
            float red = Color.red(color) * alpha;
            float green = Color.green(color) * alpha;
            float blue = Color.blue(color) * alpha;

            float[] matrix = {
                    1, 0, 0, 0, red, //red
                    0, 1, 0, 0, green, //green
                    0, 0, 1, 0, blue, //blue
                    0, 0, 0, 1, 0 //alpha
            };

            return new ColorMatrix(matrix);
        }

        public static class Builder {
            private List<ColorMatrix> mMatrixList;

            public Builder() {
                mMatrixList = new ArrayList<>();
            }

            public Builder hue(float value) {
                mMatrixList.add(adjustHue(value));
                return this;
            }

            public Builder saturate(float saturation) {
                mMatrixList.add(adjustSaturation(saturation));
                return this;
            }

            public Builder brightness(float brightness) {
                mMatrixList.add(adjustBrightness(brightness));
                return this;
            }

            public Builder contrast(float contrast) {
                mMatrixList.add(adjustContrast(contrast));
                return this;
            }

            public Builder alpha(float alpha) {
                mMatrixList.add(adjustAlpha(alpha));
                return this;
            }

            public Builder invertColors() {
                mMatrixList.add(ColorFilterUtils.invertColors());
                return this;
            }

            public Builder tint(int color) {
                mMatrixList.add(applyTint(color));
                return this;
            }

            public ColorMatrix build() {
                if (mMatrixList == null || mMatrixList.size() == 0) return null;

                ColorMatrix colorMatrix = new ColorMatrix();
                for (ColorMatrix cm : mMatrixList) {
                    colorMatrix.postConcat(cm);
                }
                return colorMatrix;
            }
        }
    }
}

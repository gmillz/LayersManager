package com.lovejoy777.rroandlayersmanager.fragments;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bitsyko.libicons.AppIcon;
import com.bitsyko.libicons.IconPack;
import com.bitsyko.libicons.IconPickerActivity;
import com.lovejoy777.rroandlayersmanager.R;
import com.lovejoy777.rroandlayersmanager.commands.Commands;
import com.lovejoy777.rroandlayersmanager.utils.IconUtils;

import java.util.ArrayList;
import java.util.List;

public class IconFragment extends Fragment implements
        AppBarLayout.OnOffsetChangedListener, IconUtils.Callback {

    private Item mPickedItem;

    private ImageAdapter mAdapter;

    private ArrayList<Item> mItems = new ArrayList<>();

    private List<Item> mModified = new ArrayList<>();

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.icon_fragment, container, false);

        ActivityManager activityManager =
                (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        int iconSize = activityManager.getLauncherLargeIconSize();
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        float dpWidth = metrics.widthPixels / metrics.density;
        int columns = Math.round(dpWidth / IconUtils.dpiFromPx(iconSize * 3, metrics));

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.app_icons);
        recyclerView.setHasFixedSize(true);
        //recyclerView.setBackgroundColor(Color.GRAY);

        mItems = IconUtils.getAllIcons(getActivity());

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), columns);
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new ImageAdapter();
        recyclerView.setAdapter(mAdapter);

        recyclerView.addItemDecoration(new GridSpacingItemDecoration(columns, 20, true));

        setHasOptionsMenu(true);

        ArrayList<IconPack> iconPacks = IconPack.getIconPacks(getActivity());
        IconPack defaultPack = new IconPack(getActivity(), "default");
        iconPacks.add(defaultPack);

        Spinner spinner = (Spinner) view.findViewById(R.id.icon_packs);
        final IconPackAdapter packAdapter = new IconPackAdapter(getActivity(), iconPacks);
        spinner.setAdapter(packAdapter);
        spinner.setSelection(iconPacks.indexOf(defaultPack));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                IconPack pack = packAdapter.getItem(position);
                boolean def = pack.getPackageName().equals("default");
                if (!def) mModified.addAll(mItems);
                getActivity().invalidateOptionsMenu();
                for (Item item : mItems) {
                    IconUtils.putIconInCache(getActivity(), item.packageName, item.drawable);
                    if (def) {
                        Log.d("TEST", "pName=" + item.packageName);
                        if (item.icon.hasOverlay()) {
                            item.setDefaultDrawable(item.icon.getDefaultIcon());
                        } else {
                            item.setDefaultDrawable(item.drawable);
                        }
                        item.icon.setUseDefault(true);
                    } else {
                        item.setCustomBitmap(item.icon.getIcon(pack, item.info.activityInfo));
                        item.icon.setUseDefault(false);
                    }
                }
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return view;
    }

    private void saveCustomIcons() {
        List<AppIcon> icons = new ArrayList<>();
        for (Item item : mItems) {
            if (item.customBitmap != null) {
                IconUtils.saveBitmapForActivityInfo(
                        getActivity(), item.info.activityInfo, item.customBitmap);
                icons.add(item.icon);
            }
        }
        if (icons.size() > 0) {
            IconUtils.installIcons(getActivity(), icons, null);
        }
    }

    @Override
    public void onInstallFinish() {
        View view = getView();
        if (view == null) return;
        Snackbar.make(view, "Icons installed!", Snackbar.LENGTH_INDEFINITE)
                .setAction("Reboot", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Commands.reboot(getActivity());
                    }
                });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.icon_fragment, menu);
        menu.findItem(R.id.install).setVisible(mModified.size() != 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.install) {
            saveCustomIcons();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public static class Item {
        String title;
        Drawable drawable;
        int resource_id;
        ResolveInfo info;
        ImageViewHolder vHolder;
        Bitmap customBitmap;
        String packageName;
        AppIcon icon;

        public Item(Context context, ResolveInfo info) {
            ActivityManager activityManager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            int iconSize = activityManager.getLauncherLargeIconSize();

            this.info = info;
            title = (String) context.getPackageManager().getApplicationLabel(
                    info.activityInfo.applicationInfo);
            drawable = IconUtils.resize(context.getResources(),
                    info.activityInfo.loadIcon(context.getPackageManager()), iconSize);
            resource_id = info.activityInfo.getIconResource();
            packageName = info.activityInfo.packageName.toLowerCase();
            ComponentName cmp = new ComponentName(info.activityInfo.packageName,
                    info.activityInfo.name);
            try {
                icon = new AppIcon(context, cmp);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        public void setCustomBitmap(Bitmap b) {
            customBitmap = b;
        }

        public void setDefaultDrawable(Drawable d) {
            setCustomBitmap(null);
            drawable = d;
        }
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView imageView;

        public ImageViewHolder(View view) {
            super(view);

            textView = (TextView) view.findViewById(R.id.name);
            imageView = (ImageView) view.findViewById(R.id.icon);
        }
    }

    public class ImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = View.inflate(getActivity(), R.layout.app_icon, null);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            ImageViewHolder vHolder = (ImageViewHolder) holder;
            if (mItems.get(position).customBitmap != null) {
                vHolder.imageView.setImageBitmap(mItems.get(position).customBitmap);
            } else {
                vHolder.imageView.setImageDrawable(mItems.get(position).drawable);
            }
            vHolder.textView.setText(mItems.get(position).title);
            mItems.get(position).vHolder = vHolder;
            vHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPickedItem = mItems.get(position);
                    IconPack.pickIconPack(IconFragment.this);
                    if (!mModified.contains(mPickedItem)) {
                        mModified.add(mPickedItem);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("TEST", "fragment onActivityResult");
        if (data != null) {
            data.getExtras().getString(IconPickerActivity.SELECTED_RESOURCE_EXTRA);
            Bitmap bitmap = data.getParcelableExtra(IconPickerActivity.SELECTED_BITMAP_EXTRA);
            if (bitmap != null) {
                if (mPickedItem != null) {
                    IconUtils.putIconInCache(
                            getActivity(), mPickedItem.packageName, mPickedItem.drawable);
                    mPickedItem.setCustomBitmap(bitmap);
                    if (!mModified.contains(mPickedItem)) {
                        mModified.add(mPickedItem);
                    }
                    getActivity().invalidateOptionsMenu();
                }
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        /*if (i == 0) {
            mSwipeRefresh.setEnabled(true);
        } else {
            mSwipeRefresh.setEnabled(false);
        }*/
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    public class IconPackAdapter extends ArrayAdapter<IconPack> {

        public IconPackAdapter(Context context, ArrayList<IconPack> iconPacks) {
            super(context, R.layout.iconpack_chooser, iconPacks);
        }

        @Override
        public View getDropDownView(int position, View convertView,ViewGroup parent) {
            return getCustomView(position, convertView);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView);
        }

        public View getCustomView(int position, View convertView) {
            if (convertView == null) {
                convertView = View.inflate(getContext(), R.layout.iconpack_chooser, null);
            }
            TextView tView = (TextView) convertView.findViewById(R.id.title);
            ImageView iView = (ImageView) convertView.findViewById(R.id.icon);
            tView.setText(getItem(position).getName());
            iView.setImageDrawable(getItem(position).getIcon());
            return convertView;
        }
    }
}

package com.lovejoy777.rroandlayersmanager.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.lovejoy777.rroandlayersmanager.helper.Theme;
import com.lovejoy777.rroandlayersmanager.helper.ThemeLoader;
import com.lovejoy777.rroandlayersmanager.installer.Installer;

import java.util.ArrayList;
import java.util.List;

public class WallpaperFragment extends Fragment {

    private Point mDisplaySIze = new Point();

    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        recyclerView.setLayoutManager(layoutManager);

        final WallpaperAdapter adapter = new WallpaperAdapter();
        recyclerView.setAdapter(adapter);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapter.updateItems();
            }
        };
        IntentFilter filter = new IntentFilter(ThemeLoader.THEMES_LOADED);
        getActivity().registerReceiver(receiver, filter);

        WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(mDisplaySIze);

        return recyclerView;
    }

    public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.ViewHolder> {

        private List<Item> mWallpapers = new ArrayList<>();

        public class Item {
            Bitmap bitmap;
            AssetManager am;
            String name;

            public Item(Bitmap bitmap, AssetManager am, String name) {
                this.bitmap = bitmap;
                this.am = am;
                this.name = name;
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public ViewHolder(View view) {
                super(view);
            }
        }

        public WallpaperAdapter() {
            updateItems();
        }

        public void updateItems() {
            for (Theme theme : ThemeLoader.getInstance(null).getThemes()) {
                for (String name : theme.getWallpapers()) {
                    Bitmap bitmap = theme.getWallpaperFromAssets(name);
                    if (bitmap != null) mWallpapers.add(new Item(bitmap, theme.getAssets(), name));
                }
            }
            notifyDataSetChanged();
        }

        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(getActivity());
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            ViewGroup.LayoutParams params =
                    new ViewGroup.LayoutParams(mDisplaySIze.x / 2, mDisplaySIze.x / 2);
            imageView.setBackgroundColor(Color.DKGRAY);
            imageView.setLayoutParams(params);
            return new ViewHolder(imageView);
        }

        public void onBindViewHolder(ViewHolder vHolder, int position) {
            final Item item = mWallpapers.get(position);
            ((ImageView) vHolder.itemView).setImageBitmap(item.bitmap);

            vHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage("Set as wallpaper?");
                    builder.setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    new Installer(getActivity()).installWallpaperFromAssets(
                                            item.am, item.name, mDisplaySIze.x, mDisplaySIze.y);
                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    builder.show();
                }
            });
        }

        public int getItemCount() {
            return mWallpapers.size();
        }
    }
}

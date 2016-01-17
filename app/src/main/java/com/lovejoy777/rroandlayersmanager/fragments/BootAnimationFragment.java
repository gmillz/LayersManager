package com.lovejoy777.rroandlayersmanager.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.lovejoy777.rroandlayersmanager.R;
import com.lovejoy777.rroandlayersmanager.helper.Theme;
import com.lovejoy777.rroandlayersmanager.helper.ThemeLoader;
import com.lovejoy777.rroandlayersmanager.utils.Utils;
import com.lovejoy777.rroandlayersmanager.views.BootAniImageView;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

@SuppressWarnings("deprecation")
public class BootAnimationFragment extends Fragment {

    private static final String CACHED_SUFFIX = "_bootanimation.zip";

    BootAniImageView mBootAnimationPreview;
    AlertDialog mPreviewDialog;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setHasFixedSize(true);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        final ItemAdapter adapter = new ItemAdapter(getActivity());
        recyclerView.setAdapter(adapter);
        if (adapter.getItemCount() == 0) {
            TextView textView = new TextView(getActivity());
            textView.setGravity(Gravity.CENTER);
            textView.setTextAppearance(getActivity(),
                    android.R.style.TextAppearance_DeviceDefault_Large);
            textView.setText("No Boot Animations!");
            //return textView;
        }
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapter.updateItems();
            }
        };
        IntentFilter filter = new IntentFilter(ThemeLoader.THEMES_LOADED);
        getActivity().registerReceiver(receiver, filter);
        mBootAnimationPreview = new BootAniImageView(getActivity());
        return recyclerView;
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(mBootAnimationPreview);
        builder.setPositiveButton("OK", null);
        mPreviewDialog = builder.create();
    }

    public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

        public class ItemViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView name;
            TextView dev;
            //String pkgName;

            public ItemViewHolder(View view) {
                super(view);
                icon = (ImageView) view.findViewById(R.id.iv_themeImage);
                name = (TextView) view.findViewById(R.id.txtName);
                dev = (TextView) view.findViewById(R.id.txtSurname);
            }

            public void setPackage(final String pkgName) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new AnimationLoader(mContext, pkgName).execute();
                        mPreviewDialog.show();
                    }
                });
            }
        }

        public class Item {
            String name;
            String developer;
            Drawable icon;
            String pkgName;

            public Item(String pkgName, String name, String dev, Drawable icon) {
                this.pkgName = pkgName;
                this.name = name;
                developer = dev;
                this.icon = icon;
            }
        }

        public Context mContext;

        private List<Theme> mThemes = new ArrayList<>();
        private ArrayList<Item> mItems = new ArrayList<>();

        public ItemAdapter(Context c) {
            mContext = c;
            updateItems();
        }

        public void updateItems() {
            mThemes = ThemeLoader.getInstance(null).getThemes();
            for (Theme theme : mThemes) {
                Log.d("TEST", "attempting to load theme : " + theme.getName());
                if (theme.containsBootAnimations()) {
                    Log.d("TEST", "theme with boot : " + theme.getName());
                    mItems.add(new Item(theme.getPackageName(),
                            theme.getName(), theme.getDeveloper(), theme.getIcon()));
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = View.inflate(mContext, R.layout.adapter_cardview, null);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ItemViewHolder holder, int position) {
            holder.name.setText(mItems.get(position).name);
            holder.dev.setText(mItems.get(position).developer);
            holder.icon.setImageDrawable(mItems.get(position).icon);
            holder.setPackage(mItems.get(position).pkgName);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    class AnimationLoader extends AsyncTask<Void, Void, ZipFile> {
        Context mContext;
        String mPkgName;

        public AnimationLoader(Context context, String pkgName) {
            mContext = context;
            mPkgName = pkgName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mBootAnimationPreview.setImageDrawable(null);
            //mLoadingProgress.setVisibility(View.VISIBLE);
            //mNoPreviewTextView.setVisibility(View.INVISIBLE);
        }

        @Override
        protected ZipFile doInBackground(Void... params) {
            if (mContext == null) {
                return null;
            }
            ZipFile zip;
            // check if the bootanimation is cached
            File f = new File(mContext.getCacheDir(), mPkgName + CACHED_SUFFIX);
            if (!f.exists()) {
                // go easy on cache storage and clear out any previous boot animations
                clearBootAnimationCache();
                try {
                    Context themeContext = mContext.createPackageContext(mPkgName, 0);
                    AssetManager am = themeContext.getAssets();
                    InputStream is = am.open("bootanimation/bootanimation.zip");
                    Utils.copyAsset(am, "bootanimation/bootanimation.zip", f.getAbsolutePath());
                    //FileUtils.copyToFile(is, f);
                    is.close();
                } catch (Exception e) {
                    //Log.w(TAG, "Unable to load boot animation", e);
                    return null;
                }
            }
            try {
                zip = new ZipFile(f);
            } catch (IOException e) {
                //Log.w(TAG, "Unable to load boot animation", e);
                return null;
            }
            if (zip != null) {
                return zip;
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(ZipFile isSuccess) {
            super.onPostExecute(isSuccess);
            mBootAnimationPreview.setBootAnimation(isSuccess);
            mBootAnimationPreview.start();
        }
    }

    private void clearBootAnimationCache() {
        File cache = getActivity().getCacheDir();
        if (cache.exists()) {
            for(File f : cache.listFiles()) {
                // volley stores stuff in cache so don't delete the volley directory
                if(!f.isDirectory() && f.getName().endsWith(CACHED_SUFFIX)) f.delete();
            }
        }
    }
}

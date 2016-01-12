package com.lovejoy777.rroandlayersmanager.fragments;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.lovejoy777.rroandlayersmanager.R;
import com.lovejoy777.rroandlayersmanager.helper.Theme;
import com.lovejoy777.rroandlayersmanager.helper.ThemeLoader;

import java.util.ArrayList;
import java.util.List;

public class BootAnimationFragment extends Fragment {

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setHasFixedSize(true);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        ItemAdapter adapter = new ItemAdapter(getActivity());
        recyclerView.setAdapter(adapter);
        return null;
    }

    public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

        public class ItemViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView name;
            TextView dev;
            public ItemViewHolder(View view) {
                super(view);
                icon = (ImageView) view.findViewById(R.id.iv_themeImage);
                name = (TextView) view.findViewById(R.id.txtName);
                dev = (TextView) view.findViewById(R.id.txtSurname);
            }
        }

        public class Item {
            String name;
            String developer;
            Drawable icon;

            public Item(String name, String dev, Drawable icon) {
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
            mThemes = ThemeLoader.getInstance(null).getThemes();
            for (Theme theme : mThemes) {
                if (theme.containsBootAnimations()) {
                    mItems.add(new Item(theme.getName(), theme.getDeveloper(), theme.getIcon()));
                }
            }
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
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }
}

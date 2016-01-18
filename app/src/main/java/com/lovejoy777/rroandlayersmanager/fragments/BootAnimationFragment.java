package com.lovejoy777.rroandlayersmanager.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lovejoy777.rroandlayersmanager.R;
import com.lovejoy777.rroandlayersmanager.activities.BootAnimPreviewActivity;
import com.lovejoy777.rroandlayersmanager.helper.Theme;
import com.lovejoy777.rroandlayersmanager.helper.ThemeLoader;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class BootAnimationFragment extends Fragment {

    private RecyclerView mList;
    private TextView mNoBootAnimation;
    private ItemAdapter mAdapter;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mAdapter != null) {
                mAdapter.updateItems();
            }
            if (mList != null && mNoBootAnimation != null) {
                mNoBootAnimation.setVisibility(View.GONE);
                mList.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        RelativeLayout layout = new RelativeLayout(getActivity());

        // params used for all views
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);

        layout.setLayoutParams(params);

        mList = new RecyclerView(getActivity());
        mList.setHasFixedSize(true);
        mList.setLayoutParams(params);
        layout.addView(mList);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mList.setLayoutManager(layoutManager);

        mAdapter = new ItemAdapter(getActivity());
        mList.setAdapter(mAdapter);
        mNoBootAnimation = new TextView(getActivity());
        mNoBootAnimation.setGravity(Gravity.CENTER);
        mNoBootAnimation.setLayoutParams(params);
        mNoBootAnimation.setTextAppearance(getActivity(),
                android.R.style.TextAppearance_DeviceDefault_Large);
        mNoBootAnimation.setVisibility(View.GONE);
        mNoBootAnimation.setText("No Boot Animations!");
        layout.addView(mNoBootAnimation);
        if (mAdapter.getItemCount() == 0) {
            mNoBootAnimation.setVisibility(View.VISIBLE);
            mList.setVisibility(View.GONE);
        }
        IntentFilter filter = new IntentFilter(ThemeLoader.THEMES_LOADED);
        getActivity().registerReceiver(mReceiver, filter);
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);
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

            public void setPackage(final String pkgName) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), BootAnimPreviewActivity.class);
                        intent.putExtra("packagename", pkgName);
                        getActivity().startActivity(intent);
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
}

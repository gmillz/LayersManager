package com.lovejoy777.rroandlayersmanager.fragments;

import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bitsyko.LayerInfo;
import com.bitsyko.Placeholder;
import com.bitsyko.liblayers.Layer;
import com.lovejoy777.rroandlayersmanager.R;
import com.lovejoy777.rroandlayersmanager.adapters.CardViewAdapter;
import com.lovejoy777.rroandlayersmanager.commands.Commands;
import com.lovejoy777.rroandlayersmanager.helper.RecyclerItemClickListener;
import com.lovejoy777.rroandlayersmanager.helper.ThemeLoader;
import com.lovejoy777.rroandlayersmanager.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PluginFragment extends Fragment {

    private int mSortMode;

    private RecyclerView mRecyclerView;
    private CardViewAdapter mCardAdapter;
    private Boolean mNoOverlays = false;
    private CoordinatorLayout mLayout = null;
    private SwipeRefreshLayout mSwipeRefresh;

    ItemTouchHelper.SimpleCallback mItemTouchCallback =
            new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView,
                              RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
            //Remove swiped item from list and notify the RecyclerView
            String packageName = mCardAdapter.getLayerFromPosition(
                    viewHolder.getAdapterPosition()).getPackageName();
            Uri packageURI = Uri.parse("package:" + packageName);
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
            startActivityForResult(uninstallIntent, 1);
        }
    };

    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mLayout =
                (CoordinatorLayout) inflater.inflate(R.layout.fragment_plugins, container, false);

        ((DrawerLayout) getActivity().findViewById(R.id.drawer_layout))
                .setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        ((NavigationView) getActivity().findViewById(R.id.nav_view))
                .getMenu().getItem(0).setChecked(true);
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.title2);
        toolbarTitle.setText(getString(R.string.themes_title));

        int elevation = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 0, getResources().getDisplayMetrics());
        int height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());

        AppBarLayout.LayoutParams layoutParams = new AppBarLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height);

        toolbar.setElevation(elevation);
        toolbar.setLayoutParams(layoutParams);
        toolbar.setNavigationIcon(R.drawable.ic_action_menu);

        LoadRecyclerViewFabToolbar();

        mSortMode = Commands.getSortMode(getActivity());

        refreshList();

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshList();
            }
        };
        IntentFilter filter = new IntentFilter(ThemeLoader.LAYERS_LOADED);
        getActivity().registerReceiver(receiver, filter);
        setHasOptionsMenu(true);

        return mLayout;
    }

    private void LoadRecyclerViewFabToolbar() {
        //create RecyclerView
        RecyclerView recyclerCardViewList = (RecyclerView) mLayout.findViewById(R.id.cardList);
        recyclerCardViewList.setHasFixedSize(true);
        recyclerCardViewList.addOnItemTouchListener(
                new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        onListItemClick(position);
                    }
                })
        );

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerCardViewList.setLayoutManager(llm);


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerCardViewList);

        //create FAB
        FloatingActionButton fab = (FloatingActionButton) mLayout.findViewById(R.id.fab3);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((menu) getActivity()).changeFragment(4);
            }
        });

        mSwipeRefresh = (SwipeRefreshLayout) mLayout.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefresh.setColorSchemeResources(R.color.accent);
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            }
        });
    }

    //create list if no plugins are installed
    private List<Placeholder> createList2() {

        List<Placeholder> result = new ArrayList<>();
        result.add(new Placeholder(getString(R.string.tooBad), getString(R.string.noPlugins),
                getResources().getDrawable(R.drawable.ic_noplugin, null)));
        result.add(new Placeholder(getString(R.string.Showcase),
                getString(R.string.ShowCaseMore),
                getResources().getDrawable(R.mipmap.ic_launcher, null)));
        result.add(new Placeholder(getString(R.string.PlayStore),
                getString(R.string.PlayStoreMore),
                getResources().getDrawable(R.drawable.playstore, null)));
        return result;
    }

    //open Plugin page after clicked on a cardview
    protected void onListItemClick(int position) {
        if (!mNoOverlays) {
            ((menu) getActivity()).openOverlayDetailActivity(
                    (Layer) mCardAdapter.getLayerFromPosition(position));
        } else {
            //PlayStore
            if (position == 2) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.PlaystoreSearch))));
            }
            //Showcase
            if (position == 1) {
                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(getActivity().getApplicationContext(), R.anim.anni1, R.anim.anni2).toBundle();
                boolean installed = Commands.appInstalledOrNot(getActivity(), "com.lovejoy777.showcase");
                if (installed) {
                    //This intent will help you to launch if the package is already installed
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.lovejoy777.showcase", "com.lovejoy777.showcase.MainActivity1"));
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), "Please install the layers showcase plugin", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.lovejoy777.showcase"));
                    intent.putExtras(bndlanimation);
                    startActivity(intent);
                }
            }

        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            refreshList();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_pluginlist, menu);
        switch (mSortMode) {
            default:
                menu.findItem(R.id.menu_sortName).setChecked(true);
                break;
            case 2:
                menu.findItem(R.id.menu_sortDeveloper).setChecked(true);
                break;
            case 3:
                menu.findItem(R.id.menu_sortRandom).setChecked(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reboot:
                Commands.reboot(getActivity());
                break;
            case R.id.menu_sortName:
                item.setChecked(true);
                Commands.setSortMode(getActivity(), 1);
                refreshList();
                break;
            case R.id.menu_sortDeveloper:
                item.setChecked(true);
                Commands.setSortMode(getActivity(), 2);
                refreshList();
                break;
            case R.id.menu_sortRandom:
                item.setChecked(true);
                Commands.setSortMode(getActivity(), 3);
                refreshList();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshList() {
        new FillPluginList().execute();
    }

    private abstract class LoadStuff extends AsyncTask<Void, Void, List<? extends LayerInfo>> {

        protected void onPreExecute() {
            mSwipeRefresh.setRefreshing(true);
        }


        protected void onPostExecute(List<? extends LayerInfo> result) {

            if (result.size() > 0) {
                mCardAdapter = new CardViewAdapter(result);
            } else {
                mCardAdapter = new CardViewAdapter(createList2());
                mNoOverlays = true;
            }

            mRecyclerView = (RecyclerView) mLayout.findViewById(R.id.cardList);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setAdapter(mCardAdapter);
            mSwipeRefresh.setRefreshing(false);
        }

    }


    private class FillPluginList extends LoadStuff {

        @Override
        protected List<? extends LayerInfo> doInBackground(Void... params) {

            List<Layer> layerList = ThemeLoader.getInstance(null).getLayers();

            mSortMode = Commands.getSortMode(getActivity());
            if (mSortMode == 1) {
                //Alphabetically NAME
                Collections.sort(layerList, LayerInfo.compareName);
            } else if (mSortMode == 2) {
                //Alphabetically DEVELOPER
                Collections.sort(layerList, LayerInfo.compareDev);
            } else if (mSortMode == 3) {
                //RANDOM
                Collections.shuffle(layerList, new Random());
            }

            return layerList;

        }

    }
}

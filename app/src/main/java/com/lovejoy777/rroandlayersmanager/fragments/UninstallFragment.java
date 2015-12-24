package com.lovejoy777.rroandlayersmanager.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.bitsyko.liblayers.layerfiles.SimpleOverlay;
import com.lovejoy777.rroandlayersmanager.AsyncResponse;
import com.lovejoy777.rroandlayersmanager.DeviceSingleton;
import com.lovejoy777.rroandlayersmanager.R;
import com.lovejoy777.rroandlayersmanager.beans.FileBean;
import com.lovejoy777.rroandlayersmanager.commands.Commands;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UninstallFragment extends android.support.v4.app.Fragment implements AsyncResponse {

    android.support.v7.widget.Toolbar toolbar;
    TextView toolbarTitle;
    private FloatingActionButton fab2;
    private LinearLayout mLinearLayout;
    private CoordinatorLayout cordLayout = null;
    private ArrayList<CheckBox> checkBoxes = new ArrayList<>();
    private int mode;
    private ActionMode mActionMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        cordLayout = (CoordinatorLayout) inflater.inflate(R.layout.fragment_delete, container, false);

        Bundle bundle = getArguments();
        mode = bundle.getInt("Mode");
        ((NavigationView) getActivity().findViewById(R.id.nav_view)).getMenu().getItem(1).setChecked(true);

        toolbar = (android.support.v7.widget.Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.UninstallOverlays));

        int elevation = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0, getResources().getDisplayMetrics());
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());
        toolbar.setNavigationIcon(R.drawable.ic_action_menu);

        toolbarTitle = (TextView) getActivity().findViewById(R.id.title2);
        toolbarTitle.setText("");

        AppBarLayout.LayoutParams layoutParams = new AppBarLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height
        );

        ViewPager viewPager = (ViewPager) getActivity().findViewById(R.id.tabanim_viewpager);
        TabLayout tabLayout = (TabLayout) getActivity().findViewById(R.id.tabs);
        viewPager.setVisibility(View.VISIBLE);
        tabLayout.setVisibility(View.VISIBLE);

        AppBarLayout appbar = (AppBarLayout) getActivity().findViewById(R.id.appBarlayout);
        appbar.setElevation(0);
        toolbar.setElevation(elevation);
        toolbar.setLayoutParams(layoutParams);


        setHasOptionsMenu(true);

        ActivityCompat.invalidateOptionsMenu(getActivity());

        loadToolbarRecylcerViewFab();

        new LoadAndSet().execute();

        return cordLayout;
    }

    @Override
    public void processFinish() {
        fab2.hide();
        fab2.setClickable(true);
        CoordinatorLayout coordinatorLayoutView = (CoordinatorLayout) cordLayout.findViewById(R.id.main_content3);
        Snackbar.make(coordinatorLayoutView, R.string.uninstalled, Snackbar.LENGTH_LONG)
                .setAction(R.string.Reboot, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Commands.reboot(getActivity());
                    }
                })
                .show();

        new LoadAndSet().execute();
        mActionMode.finish();
        mActionMode = null;
        ActivityCompat.invalidateOptionsMenu(getActivity());
    }

    private void loadToolbarRecylcerViewFab() {

        mLinearLayout = (LinearLayout) cordLayout.findViewById(R.id.cardList);

        fab2 = (android.support.design.widget.FloatingActionButton) cordLayout.findViewById(R.id.fab6);
        fab2.hide();
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fab2.setClickable(false);
                AsyncUninstallOverlays();
            }
        });
    }

    private void AsyncUninstallOverlays() {

        ArrayList<String> paths = new ArrayList<>();
        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isChecked()) {
                paths.add(DeviceSingleton.getInstance().getOverlayFolder() + "/" + ((FileBean) checkBox.getTag()).getLocation());
            }
        }

        Commands.UnInstallOverlays asyncTask = new Commands.UnInstallOverlays(paths, getActivity(), this);
        asyncTask.execute();
    }

    //Check and Uncheck all Checkboxes
    private void checkAll() {

        for (CheckBox checkBox : checkBoxes) {
            if (!checkBox.isChecked()) {
                checkBox.performClick();
            }
        }

        refreshFab();
    }

    private void UncheckAll(boolean calledFromCabFinished) {

        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isChecked()) {
                checkBox.performClick();
            }
        }

        if (calledFromCabFinished) {
            fab2.hide();
        } else {
            refreshFab();
        }

    }


    //Overflow Menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.overflow, menu);
        menu.removeItem(R.id.menu_sort);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_selectall:
                if (item.isChecked()) item.setChecked(false);
                else item.setChecked(true);
                checkAll();
                return true;
            case R.id.menu_reboot:
                Commands.reboot(getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshFab() {

        int checkedItems = 0;

        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isChecked()) {
                checkedItems++;
            }
        }

        if (checkedItems > 0) {
            if (mActionMode == null) {
                mActionMode = getActivity().startActionMode(new ActionBarCallBack());
            }
            fab2.show();
        } else {
            if (mActionMode != null) {
                mActionMode.finish();
                mActionMode = null;
            }

            fab2.hide();
        }
    }

    private class LoadAndSet extends AsyncTask<Void, Void, List<FileBean>> {


        @Override
        protected List<FileBean> doInBackground(Void... params) {

            Collection<File> files = FileUtils.listFiles(new File(
                    DeviceSingleton.getInstance().getOverlayFolder()), new String[]{"apk"}, false);

            List<FileBean> fileBeans = new ArrayList<>();

            System.out.println(mode);
            for (File file : files) {
                if (mode == 0) {
                    if (!file.getAbsolutePath().contains("signed.")) {
                        fileBeans.add(new FileBean(file));
                    }
                } else {
                    if (file.getAbsolutePath().contains("signed.")) {
                        fileBeans.add(new FileBean(file));
                    }
                }
            }

            Collections.sort(fileBeans, new Comparator<FileBean>() {
                @Override
                public int compare(FileBean lhs, FileBean rhs) {
                    return lhs.getName().compareTo(rhs.getName());
                }
            });

            return fileBeans;

        }

        @Override
        protected void onPostExecute(List<FileBean> result) {

            checkBoxes.clear();
            mLinearLayout.removeAllViews();

            TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);

            layoutParams.topMargin = 12;

            for (FileBean fileBean : result) {

                TableRow row = new TableRow(getActivity());
                row.setLayoutParams(layoutParams);

                // Implemented LinearLayout because a TableRow can't have more than one View added to it. LinearLayout can have, so we add a
                // LinearLayout to TableRow
                LinearLayout rowlayout = new LinearLayout(getActivity());
                rowlayout.setOrientation(LinearLayout.VERTICAL);

                CheckBox check = new CheckBox(getActivity());

                TextView summary = new TextView(getActivity());

                check.setText(fileBean.getName());
                summary.setText(new SimpleOverlay(fileBean.getFile()).getRelatedPackage());

                check.setTag(fileBean);
                check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
                check.setPadding(padding, 0, padding, 0);
                check.setMinHeight(0);
                summary.setPadding(5 * padding, 0, padding, 0);

                rowlayout.addView(check);
                rowlayout.addView(summary);

                row.addView(rowlayout);

                mLinearLayout.addView(row);

                final FileBean finalFileBean = fileBean;

                check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        finalFileBean.setChecked(!finalFileBean.isChecked());
                        refreshFab();
                    }
                });

                checkBoxes.add(check);

            }


            ImageView noOverlays = (ImageView) cordLayout.findViewById(R.id.imageView);
            TextView noOverlaysText = (TextView) cordLayout.findViewById(R.id.textView7);
            if (result.isEmpty()) {
                noOverlays.setVisibility(View.VISIBLE);
                noOverlaysText.setVisibility(View.VISIBLE);
            }

            refreshFab();
        }
    }

    class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // TODO Auto-generated method stub
            switch (item.getItemId()) {
                case R.id.menu_selectall:
                    if (item.isChecked()) item.setChecked(false);
                    else item.setChecked(true);
                    checkAll();
                    return true;
                case R.id.menu_reboot:
                    Commands.reboot(getActivity());
                    return true;
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            mode.getMenuInflater().inflate(R.menu.overflow, menu);
            getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // TODO Auto-generated method stub
            UncheckAll(true);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub

            mode.setTitle(getResources().getString(R.string.OverlaysSelected));
            return false;
        }
    }
}
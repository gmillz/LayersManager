package com.lovejoy777.rroandlayersmanager;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.bitsyko.libicons.IconPack;
import com.bitsyko.liblayers.Layer;
import com.lovejoy777.rroandlayersmanager.activities.AboutActivity;
import com.lovejoy777.rroandlayersmanager.activities.DetailedTutorialActivity;
import com.lovejoy777.rroandlayersmanager.activities.IconPackDetailActivity;
import com.lovejoy777.rroandlayersmanager.activities.OverlayDetailActivity;
import com.lovejoy777.rroandlayersmanager.activities.SettingsActivity;
import com.lovejoy777.rroandlayersmanager.commands.Commands;
import com.lovejoy777.rroandlayersmanager.fragments.BackupRestoreFragment;
import com.lovejoy777.rroandlayersmanager.fragments.InstallFragment;
import com.lovejoy777.rroandlayersmanager.fragments.PluginFragment;
import com.lovejoy777.rroandlayersmanager.fragments.UninstallFragment;
import com.lovejoy777.rroandlayersmanager.utils.Utils;
import com.rubengees.introduction.IntroductionActivity;
import com.rubengees.introduction.IntroductionBuilder;
import com.rubengees.introduction.entity.Option;
import com.rubengees.introduction.entity.Slide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class menu extends AppCompatActivity {

    public static final String PLAY_SHOWCASE_URI =
            "http://play.google.com/store/apps/details?id=com.lovejoy777.showcase";
    public static final String PLAY_LAYERS_THEMES_URI =
            "https://play.google.com/store/search?q=Layers+Theme&c=" +
                    "apps&docType=1&sp=CAFiDgoMTGF5ZXJzIFRoZW1legIYAIoBAggB:S:ANO1ljK_ZAY";
    private DrawerLayout mDrawerLayout;
    private ViewPagerAdapter adapter;

    public static void loadTutorial(final Activity context) {
        new IntroductionBuilder(context).withSlides(generateSlides()).introduceMyself();
    }

    public static List<Slide> generateSlides() {
        List<Slide> slides = new ArrayList<>();

        slides.add(new Slide()
                .withTitle(R.string.Slide1_Heading)
                .withDescription(R.string.Slide1_Text)
                .withColorResource(R.color.tutorial_background_1)
                .withImage(R.drawable.layersmanager));
        slides.add(new Slide()
                .withTitle(R.string.Slide2_Heading)
                .withDescription(R.string.Slide2_Text)
                .withColorResource(R.color.tutorial_background_2)
                .withImage(R.drawable.intro_2));
        slides.add(new Slide()
                .withTitle(R.string.Slide3_Heading)
                .withDescription(R.string.Slide3_Text)
                .withColorResource(R.color.tutorial_background_3)
                .withImage(R.drawable.intro_3));
        slides.add(new Slide()
                .withTitle(R.string.Slide4_Heading)
                .withDescription(R.string.Slide4_Text)
                .withColorResource(R.color.tutorial_background_4)
                .withImage(R.drawable.intro_4));
        slides.add(new Slide()
                .withTitle(R.string.Slide5_Heading)
                .withDescription(R.string.Slide5_Text)
                .withColorResource(R.color.tutorial_background_5)
                .withImage(R.drawable.intro_5));
        slides.add(new Slide()
                .withTitle(R.string.Slide6_Heading)
                .withOption(new Option(R.string.SettingLauncherIconDetail))
                .withColorResource(R.color.tutorial_background_6)
                .withImage(R.drawable.layersmanager_crossed));
        slides.add(new Slide()
                .withTitle(R.string.Slide7_Heading)
                .withOption(new Option(R.string.SettingsHideOverlays))
                .withColorResource(R.color.tutorial_background_6)
                .withImage(R.drawable.intro_7));
        return slides;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_container);

        loadToolbarNavDrawer();

        if (!Utils.isRootAvailable()) {
            Toast.makeText(this, getString(R.string.noRoot), Toast.LENGTH_LONG).show();
        } else {
            createImportantDirectories();
        }

        Boolean tutorialShown = PreferenceManager.getDefaultSharedPreferences(menu.this).getBoolean("tutorialShown", false);
        if (!tutorialShown) {
            loadTutorial(this);
        } else {
            changeFragment(1);
        }
    }

    private void setupViewPager(ViewPager viewPager, int mode) {
        viewPager.removeAllViews();
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        if (mode == 0) {

            PluginFragment overlayFragment = new PluginFragment();
            PluginFragment iconFragment = new PluginFragment();

            Bundle args1 = new Bundle();
            Bundle args2 = new Bundle();
            args1.putInt("Mode", 0);
            args2.putInt("Mode", 1);

            overlayFragment.setArguments(args1);
            iconFragment.setArguments(args2);

            adapter.addFrag(overlayFragment, "Overlays");
            adapter.addFrag(iconFragment, "Icon Overlays");
        } else {
            System.out.println("TEST");
            adapter.removeAllFrags();
            adapter.notifyDataSetChanged();

            UninstallFragment uninstallOverlays = new UninstallFragment();
            UninstallFragment uninstallOverlays2 = new UninstallFragment();
            Bundle args1 = new Bundle();
            Bundle args2 = new Bundle();
            args1.putInt("Mode", 0);
            uninstallOverlays.setArguments(args1);
            adapter.addFrag(uninstallOverlays, "Overlays");
            args2.putInt("Mode", 1);
            uninstallOverlays2.setArguments(args2);
            adapter.addFrag(uninstallOverlays2, "Icon Overlays");
            //adapter.addFrag(new UninstallFragment(), "Icon Overlays");
        }
        viewPager.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IntroductionBuilder.INTRODUCTION_REQUEST_CODE &&
                resultCode == RESULT_OK) {
            for (Option option : data.<Option>getParcelableArrayListExtra(
                    IntroductionActivity.OPTION_RESULT)) {
                if (option.getPosition() == 5 && option.isActivated()) {
                    SharedPreferences myprefs =
                            getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
                    myprefs.edit().putBoolean("switch1", true).apply();
                    Commands.killLauncherIcon(this);
                } else if (option.getPosition() == 6 && option.isActivated()) {
                    SharedPreferences myprefs =
                            getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
                    myprefs.edit().putBoolean("disableNotInstalledApps", true).apply();
                }
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putBoolean("tutorialShown", true).apply();
                changeFragment(1);
            }
        } else {
            if (resultCode == RESULT_CANCELED) {
                loadTutorial(this);
            }
        }
    }

    private void loadToolbarNavDrawer() {
        //set Toolbar
        final android.support.v7.widget.Toolbar toolbar =
                (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_menu);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //set NavigationDrawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }
    }

    //navigationDrawerIcon Onclick
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Fragment currentFragment =
                        menu.this.getFragmentManager().findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof InstallFragment) {
                    changeFragment(1);
                } else {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //set NavigationDrawerContent
    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        mDrawerLayout.closeDrawers();
                        Bundle bndlanimation =
                                ActivityOptions.makeCustomAnimation(getApplicationContext(),
                                        R.anim.anni1, R.anim.anni2).toBundle();
                        int id = menuItem.getItemId();
                        switch (id) {
                            //Home
                            case R.id.nav_home:
                                menuItem.setChecked(true);
                                changeFragment(1);
                                break;
                            //Uninstall
                            case R.id.nav_delete:
                                menuItem.setChecked(true);
                                changeFragment(2);
                                break;
                            //Backup & Restore
                            case R.id.nav_restore:
                                menuItem.setChecked(true);
                                changeFragment(3);
                                break;
                            //Showcase
                            case R.id.nav_showcase:
                                boolean installed = Commands.appInstalledOrNot(
                                        menu.this, "com.lovejoy777.showcase");
                                if (installed) {
                                    // This intent will help you
                                    // to launch if the package is already installed
                                    Intent intent = new Intent();
                                    intent.setComponent(new ComponentName(
                                            "com.lovejoy777.showcase",
                                            "com.lovejoy777.showcase.MainActivity1"));
                                    startActivity(intent);
                                    break;
                                } else {
                                    Toast.makeText(menu.this,
                                            "Please install the layers showcase plugin",
                                            Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                                            PLAY_SHOWCASE_URI)), bndlanimation);
                                    break;
                                }
                                //PlayStore
                            case R.id.nav_playStore:
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(PLAY_LAYERS_THEMES_URI)), bndlanimation);
                                break;
                            //Tutorial
                            case R.id.nav_tutorial:
                                Intent tutorial = new Intent(
                                        menu.this, DetailedTutorialActivity.class);
                                startActivity(tutorial, bndlanimation);
                                break;
                            //About
                            case R.id.nav_about:
                                Intent about = new Intent(menu.this, AboutActivity.class);
                                startActivity(about, bndlanimation);
                                break;
                            //Settings
                            case R.id.nav_settings:
                                Intent settings = new Intent(menu.this, SettingsActivity.class);
                                startActivity(settings, bndlanimation);
                                break;
                        }
                        return false;
                    }
                });
    }

    public void changeFragment(int position) {
        android.support.v7.widget.Toolbar toolbar =
                (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        Fragment fragment = null;
        FragmentManager fragmentManager = getFragmentManager();
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }
        ViewPager viewPager = (ViewPager) findViewById(R.id.tabanim_viewpager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        switch (position) {
            case 1:
                setupViewPager(viewPager, 0);
                tabLayout.setupWithViewPager(viewPager);
                break;
            case 2:
                setupViewPager(viewPager, 1);
                tabLayout.setupWithViewPager(viewPager);
                //fragment = new UninstallFragment();
                break;
            case 3:
                fragment = new BackupRestoreFragment();
                break;
            case 4:
                fragment = new InstallFragment();
                break;
        }


        if (position > 2) {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment, "TAG")
                    .addToBackStack(null)
                    .commit();
        } else {
            android.support.v4.app.Fragment test =
                    getSupportFragmentManager().findFragmentByTag("TAG");
            if (test != null) {
                fragmentManager
                        .beginTransaction()
                        .remove(getFragmentManager().findFragmentByTag("TAG"))
                        .commit();
            }
        }
    }

    public void openOverlayDetailActivity(Layer layer) {
        Bundle args = new Bundle();
        args.putString("PackageName", layer.getPackageName());
        args.putBoolean("CMTETheme", layer.isCMTETheme);

        Intent intent = new Intent(this, OverlayDetailActivity.class);

        intent.putExtra("PackageName", layer.getPackageName());
        intent.putExtra("CMTETheme", layer.isCMTETheme);

        startActivity(intent);
    }

    public void openIconPackDetailActivity(IconPack iconPack) {
        Bundle args = new Bundle();
        args.putString("PackageName", iconPack.getPackageName());

        Intent intent = new Intent(this, IconPackDetailActivity.class);

        intent.putExtra("PackageName", iconPack.getPackageName());

        startActivity(intent);
    }

    private void createImportantDirectories() {
        String sdOverlays1 = Environment.getExternalStorageDirectory() + "/Overlays/Backup";
        // CREATES /SDCARD/OVERLAYS/BACKUP
        File dir1 = new File(sdOverlays1);

        dir1.mkdirs();

        Utils.remount("rw");
        String vendover = DeviceSingleton.getInstance().getOverlayFolder();
        // CREATES /VENDOR/OVERLAY
        File dir2 = new File(vendover);
        if (!dir2.exists()) {
            Utils.createFolder(dir2);
        }
        Utils.remount("ro");
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment =
                menu.this.getFragmentManager().findFragmentById(R.id.fragment_container);

        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
            return;
        }

        if (currentFragment instanceof InstallFragment) {
            changeFragment(1);
        }
        super.onBackPressed();
    }

    class ViewPagerAdapter extends FragmentStatePagerAdapter {
        private final List<android.support.v4.app.Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(android.support.v4.app.FragmentManager manager) {
            super(manager);
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(android.support.v4.app.Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public void removeAllFrags() {
            mFragmentList.clear();
            mFragmentTitleList.clear();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}

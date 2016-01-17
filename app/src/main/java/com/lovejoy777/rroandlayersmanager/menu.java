package com.lovejoy777.rroandlayersmanager;

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
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.bitsyko.liblayers.Layer;
import com.lovejoy777.rroandlayersmanager.activities.AboutActivity;
import com.lovejoy777.rroandlayersmanager.activities.DetailedTutorialActivity;
import com.lovejoy777.rroandlayersmanager.activities.OverlayDetailActivity;
import com.lovejoy777.rroandlayersmanager.activities.SettingsActivity;
import com.lovejoy777.rroandlayersmanager.commands.Commands;
import com.lovejoy777.rroandlayersmanager.fragments.BackupRestoreFragment;
import com.lovejoy777.rroandlayersmanager.fragments.BootAnimationFragment;
import com.lovejoy777.rroandlayersmanager.fragments.IconFragment;
import com.lovejoy777.rroandlayersmanager.fragments.InstallFragment;
import com.lovejoy777.rroandlayersmanager.fragments.PluginFragment;
import com.lovejoy777.rroandlayersmanager.fragments.UninstallFragment;
import com.lovejoy777.rroandlayersmanager.helper.Tutorial;
import com.lovejoy777.rroandlayersmanager.utils.Utils;
import com.rubengees.introduction.IntroductionActivity;
import com.rubengees.introduction.IntroductionBuilder;
import com.rubengees.introduction.entity.Option;

import java.io.File;

public class menu extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String PLAY_SHOWCASE_URI =
            "http://play.google.com/store/apps/details?id=com.lovejoy777.showcase";
    public static final String PLAY_LAYERS_THEMES_URI =
            "https://play.google.com/store/search?q=Layers+Theme&c=" +
                    "apps&docType=1&sp=CAFiDgoMTGF5ZXJzIFRoZW1legIYAIoBAggB:S:ANO1ljK_ZAY";
    private DrawerLayout mDrawerLayout;

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
            Tutorial.loadTutorial(this);
        } else {
            changeFragment(R.id.nav_themes);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
        }
        super.onActivityResult(requestCode, resultCode, data);
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

    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        changeFragment(id);
        return true;
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
                            case R.id.nav_themes:
                                menuItem.setChecked(true);
                                changeFragment(id);
                                break;
                            // Icons
                            case R.id.nav_icons:
                                menuItem.setChecked(true);
                                changeFragment(id);
                                break;
                            // Boot Animations
                            case R.id.nav_bootanimation:
                                menuItem.setChecked(true);
                                changeFragment(id);
                                break;
                            //Uninstall
                            case R.id.nav_delete:
                                menuItem.setChecked(true);
                                changeFragment(id);
                                break;
                            //Backup & Restore
                            case R.id.nav_restore:
                                menuItem.setChecked(true);
                                changeFragment(id);
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

    public void changeFragment(int id) {
        Fragment fragment = null;
        FragmentManager fragmentManager = getFragmentManager();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }
        switch (id) {
            case R.id.nav_themes:
                fragment = new PluginFragment();
                break;
            case R.id.nav_icons:
                fragment = new IconFragment();
                break;
            case R.id.nav_bootanimation:
                fragment = new BootAnimationFragment();
                break;
            case R.id.nav_delete:
                fragment = new UninstallFragment();
                break;
            case R.id.nav_restore:
                fragment = new BackupRestoreFragment();
                break;
        }

        if (fragment != null) {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment, "TAG")
                    .addToBackStack(null)
                    .commit();
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
            changeFragment(R.id.nav_delete);
        }
        super.onBackPressed();
    }
}

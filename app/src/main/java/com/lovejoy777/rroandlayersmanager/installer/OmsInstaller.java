package com.lovejoy777.rroandlayersmanager.installer;

import android.content.Context;
import android.os.AsyncTask;

import com.bitsyko.liblayers.layerfiles.LayerFile;
import com.lovejoy777.rroandlayersmanager.utils.Utils;

import java.util.List;

public class OmsInstaller extends BaseInstaller {

    public OmsInstaller(Context context) {
        super(context);
    }

    @Override
    public void installOverlays(final List<LayerFile> layers) {
        setMax(layers.size());
        showProgressDialog();
        for (LayerFile layer : layers) {
            new InstallOverlay(layer).executeOnExecutor(mExecutor);
        }
    }

    private class InstallOverlay extends AsyncTask<Void, Void, Void> {

        LayerFile layer;

        private InstallOverlay(LayerFile layer) {
            this.layer = layer;
        }

        protected Void doInBackground(Void... v) {
            installPackage(layer.getFile(mContext).getAbsolutePath());
            enableTheme(layer.getPackageName(mContext));
            return null;
        }

        protected void onPostExecute(Void v) {
            updateProgress();
        }
    }

    private void installPackage(String path) {
        Utils.runCommand("pm install " + path, true);
    }

    private void enableTheme(String name) {
        Utils.runCommand("om enable " + name, true);
    }
}

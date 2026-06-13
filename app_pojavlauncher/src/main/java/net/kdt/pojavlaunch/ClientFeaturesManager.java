package net.kdt.pojavlaunch;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.kdt.pojavlaunch.modloaders.FabricVersion;
import net.kdt.pojavlaunch.modloaders.FabriclikeDownloadTask;
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils;
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClientFeaturesManager {

    private static final String PREFS_NAME = "cs_client_features";
    public static final String KEY_ENABLED = "client_features_enabled";
    public static final String KEY_VERSION_ID = "client_mod_version_id";
    public static final String KEY_FILENAME = "client_mod_filename";
    public static final String KEY_DOWNLOAD_URL = "client_mod_download_url";
    public static final String KEY_MC_VERSION = "client_mod_mc_version";

    private final Activity mActivity;
    private final SharedPreferences mPrefs;
    private final Gson mGson = new Gson();

    public ClientFeaturesManager(Activity activity) {
        mActivity = activity;
        mPrefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return mPrefs.getBoolean(KEY_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    @Deprecated
    public void showVersionSelector(final Runnable onInstallSuccess) {
        android.util.Log.w("ClientFeaturesManager", "showVersionSelector is deprecated. Use ClientFeaturesFragment instead.");
    }

    public void showEnablePrompt(final Runnable onMaybeLater) {
        new AlertDialog.Builder(mActivity)
                .setTitle("⚠ Client Features Disabled")
                .setMessage("Enable Client Features to use Skin Management with auto-launch.")
                .setPositiveButton("ENABLE NOW", (d, w) -> {
                    if (mActivity instanceof androidx.fragment.app.FragmentActivity) {
                        Tools.swapFragment((androidx.fragment.app.FragmentActivity) mActivity,
                                net.kdt.pojavlaunch.fragments.ClientFeaturesFragment.class,
                                net.kdt.pojavlaunch.fragments.ClientFeaturesFragment.TAG, null,
                                R.anim.fade_scale_in, R.anim.fade_scale_out,
                                R.anim.fade_scale_in, R.anim.fade_scale_out);
                    }
                })
                .setNegativeButton("MAYBE LATER", (d, w) -> {
                    if (onMaybeLater != null) onMaybeLater.run();
                })
                .show();
    }
}

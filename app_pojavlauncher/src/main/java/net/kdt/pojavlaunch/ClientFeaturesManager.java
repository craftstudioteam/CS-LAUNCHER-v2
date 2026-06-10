package net.kdt.pojavlaunch;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

    public void showVersionSelector(final Runnable onInstallSuccess) {
        final BottomSheetDialog dialog = new BottomSheetDialog(mActivity, R.style.Theme_AppCompat_Dialog);
        View view = mActivity.getLayoutInflater().inflate(R.layout.dialog_client_features, null);
        dialog.setContentView(view);

        RecyclerView rv = view.findViewById(R.id.rv_mod_versions);
        rv.setLayoutManager(new LinearLayoutManager(mActivity));
        final ModVersionAdapter adapter = new ModVersionAdapter(new ModVersionAdapter.OnVersionSelectedListener() {
            @Override
            public void onVersionSelected(ModVersionAdapter.ModrinthVersion version) {
                dialog.dismiss();
                startDownload(version, onInstallSuccess);
            }
        });
        rv.setAdapter(adapter);

        view.findViewById(R.id.btn_install_version).setVisibility(View.GONE);

        dialog.show();

        // Fetch versions from Modrinth
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.modrinth.com/v2/project/IpIMaYzj/version");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "CSLauncher/1.0 (contact@craftstudio.dev)");
                    
                    InputStream is = conn.getInputStream();
                    String json = Tools.read(is);
                    final List<ModVersionAdapter.ModrinthVersion> versions = mGson.fromJson(json, new TypeToken<List<ModVersionAdapter.ModrinthVersion>>(){}.getType());
                    
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.setVersions(versions);
                        }
                    });
                } catch (Exception e) {
                    Log.e("ClientFeatures", "Failed to fetch versions", e);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mActivity, "Failed to fetch versions from Modrinth", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void startDownload(final ModVersionAdapter.ModrinthVersion version, final Runnable onInstallSuccess) {
        if (version.files == null || version.files.isEmpty()) return;
        final ModVersionAdapter.ModrinthVersion.ModrinthFile file = version.files.get(0);

        final AlertDialog downloadDialog = new AlertDialog.Builder(mActivity)
                .setView(R.layout.dialog_downloading)
                .setCancelable(false)
                .create();
        downloadDialog.show();

        final ProgressBar pb = downloadDialog.findViewById(R.id.pb_download);
        final TextView tvPercentage = downloadDialog.findViewById(R.id.tv_download_percentage);
        final TextView tvFilename = downloadDialog.findViewById(R.id.tv_download_filename);
        final View ivAnim = downloadDialog.findViewById(R.id.iv_download_anim);

        if (tvFilename != null) tvFilename.setText(file.filename);

        // Simple bounce animation
        if (ivAnim != null) {
            ivAnim.animate().translationY(20).setDuration(300).withEndAction(new Runnable() {
                @Override
                public void run() {
                    ivAnim.animate().translationY(0).setDuration(300).withEndAction(this).start();
                }
            }).start();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String mcVersion = version.game_versions != null && !version.game_versions.isEmpty() ? version.game_versions.get(0) : "unknown";
                    
                    String selectedProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, "");
                    MinecraftProfile prof = LauncherProfiles.mainProfileJson.profiles.get(selectedProfile);
                    File modsDir = new File(Tools.getGameDirPath(prof), "mods");
                    if (!modsDir.exists()) modsDir.mkdirs();

                    File destFile = new File(modsDir, file.filename);

                    URL url = new URL(file.url);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("User-Agent", "CSLauncher/1.0 (contact@craftstudio.dev)");
                    final int totalBytes = connection.getContentLength();
                    InputStream inputStream = connection.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(destFile);

                    byte[] buffer = new byte[8192];
                    long downloaded = 0;
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                        downloaded += read;
                        final int progress = (int) ((downloaded * 100) / totalBytes);
                        final long finalDownloaded = downloaded;
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (pb != null) pb.setProgress(progress);
                                if (tvPercentage != null) tvPercentage.setText(progress + "%");
                            }
                        });
                    }
                    outputStream.close();
                    inputStream.close();

                    // Save info
                    mPrefs.edit()
                            .putString(KEY_VERSION_ID, version.id)
                            .putString(KEY_FILENAME, file.filename)
                            .putString(KEY_DOWNLOAD_URL, file.url)
                            .putString(KEY_MC_VERSION, mcVersion)
                            .putBoolean(KEY_ENABLED, true)
                            .apply();

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (ivAnim != null) ivAnim.setVisibility(View.GONE);
                            View ivSuccess = downloadDialog.findViewById(R.id.iv_download_success);
                            if (ivSuccess != null) ivSuccess.setVisibility(View.VISIBLE);
                            
                            Toast.makeText(mActivity, "✦ Client mod installed successfully!", Toast.LENGTH_SHORT).show();
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    downloadDialog.dismiss();
                                    if (onInstallSuccess != null) onInstallSuccess.run();
                                }
                            }, 1500);
                        }
                    });

                } catch (Exception e) {
                    Log.e("ClientFeatures", "Download failed", e);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            downloadDialog.dismiss();
                            Toast.makeText(mActivity, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
    
    public void showEnablePrompt(final Runnable onMaybeLater) {
        new AlertDialog.Builder(mActivity)
                .setTitle("⚠ Client Features Disabled")
                .setMessage("Enable Client Features to use Skin Management with auto-launch.")
                .setPositiveButton("ENABLE NOW", (d, w) -> showVersionSelector(null))
                .setNegativeButton("MAYBE LATER", (d, w) -> {
                    if (onMaybeLater != null) onMaybeLater.run();
                })
                .show();
    }
}

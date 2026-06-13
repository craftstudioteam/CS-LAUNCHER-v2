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

    public void showVersionSelector(final Runnable onInstallSuccess) {
        final BottomSheetDialog dialog = new BottomSheetDialog(mActivity);
        View view = mActivity.getLayoutInflater().inflate(R.layout.dialog_client_features, null);
        dialog.setContentView(view);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Handle active status card and glow pulse
        View cardActive = view.findViewById(R.id.container_active_card);
        View glowView = view.findViewById(R.id.v_active_glow);
        final android.animation.AnimatorSet animatorSet;
        if (isEnabled() && cardActive != null && glowView != null) {
            cardActive.setVisibility(View.VISIBLE);
            
            glowView.setScaleX(1.02f);
            glowView.setScaleY(1.05f);
            
            android.animation.ObjectAnimator alphaAnimator = android.animation.ObjectAnimator.ofFloat(glowView, "alpha", 0.1f, 0.7f);
            alphaAnimator.setDuration(1600);
            alphaAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            alphaAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            alphaAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

            android.animation.ObjectAnimator scaleXAnimator = android.animation.ObjectAnimator.ofFloat(glowView, "scaleX", 1.01f, 1.04f);
            scaleXAnimator.setDuration(1600);
            scaleXAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            scaleXAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            scaleXAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

            android.animation.ObjectAnimator scaleYAnimator = android.animation.ObjectAnimator.ofFloat(glowView, "scaleY", 1.02f, 1.08f);
            scaleYAnimator.setDuration(1600);
            scaleYAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            scaleYAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            scaleYAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

            animatorSet = new android.animation.AnimatorSet();
            animatorSet.playTogether(alphaAnimator, scaleXAnimator, scaleYAnimator);
            animatorSet.start();
        } else {
            animatorSet = null;
        }

        dialog.setOnDismissListener(dialogInterface -> {
            if (animatorSet != null) {
                animatorSet.cancel();
            }
        });

        RecyclerView rv = view.findViewById(R.id.rv_mod_versions);
        rv.setLayoutManager(new LinearLayoutManager(mActivity));
        final ModVersionAdapter adapter = new ModVersionAdapter(new ModVersionAdapter.OnVersionSelectedListener() {
            @Override
            public void onVersionSelected(ModVersionAdapter.ModrinthVersion version) {
                startInstallWorkflow(dialog, view, version, onInstallSuccess);
            }
        });
        rv.setAdapter(adapter);

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

    private void startInstallWorkflow(final BottomSheetDialog dialog, final View dialogView, final ModVersionAdapter.ModrinthVersion version, final Runnable onInstallSuccess) {
        final android.widget.ViewFlipper flipper = dialogView.findViewById(R.id.flipper_client_features);
        if (flipper != null) {
            flipper.setInAnimation(mActivity, android.R.anim.fade_in);
            flipper.setOutAnimation(mActivity, android.R.anim.fade_out);
            flipper.setDisplayedChild(1);
        }

        final View pulseRing = dialogView.findViewById(R.id.logo_pulse_ring);
        final View optixLogo = dialogView.findViewById(R.id.iv_optix_logo);
        final View successCheck = dialogView.findViewById(R.id.iv_success_check);
        final TextView tvStage = dialogView.findViewById(R.id.tv_progress_stage);
        final TextView tvSub = dialogView.findViewById(R.id.tv_progress_sub);
        final ProgressBar pb = dialogView.findViewById(R.id.pb_install_progress);

        if (successCheck != null) successCheck.setVisibility(View.GONE);
        if (optixLogo != null) {
            optixLogo.setVisibility(View.VISIBLE);
            optixLogo.setScaleX(0f);
            optixLogo.setScaleY(0f);
        }
        if (tvStage != null) tvStage.setText("Enabling Optix Client...");
        if (tvSub != null) tvSub.setText("Preparing...");

        // A. Green glow pulse animation
        if (pulseRing != null) {
            pulseRing.setScaleX(0.8f);
            pulseRing.setScaleY(0.8f);
            pulseRing.setAlpha(1f);
            pulseRing.animate()
                .scaleX(1.8f)
                .scaleY(1.8f)
                .alpha(0f)
                .setDuration(600)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
        }

        // B. Logo scales in with spring effect after 100ms
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (optixLogo != null) {
                optixLogo.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(2f))
                    .withEndAction(() -> {
                        optixLogo.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                    })
                    .start();
            }
        }, 100);

        // Start the actual background installation tasks after animation finishes (700ms)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (tvStage != null) tvStage.setText("Installing Client Features...");
            runInstallProcess(dialog, dialogView, version, onInstallSuccess);
        }, 700);
    }

    private void runInstallProcess(final BottomSheetDialog dialog, final View dialogView, final ModVersionAdapter.ModrinthVersion version, final Runnable onInstallSuccess) {
        final TextView tvSub = dialogView.findViewById(R.id.tv_progress_sub);
        final ProgressBar pb = dialogView.findViewById(R.id.pb_install_progress);
        final String mcVersion = version.game_versions != null && !version.game_versions.isEmpty() ? version.game_versions.get(0) : null;
        
        if (mcVersion == null) {
            showErrorAndDismiss(dialog, "Invalid version selected");
            return;
        }

        updateProgressUI(tvSub, pb, "Preparing Profile...", 15);

        new Thread(() -> {
            try {
                // Stage 1: Get Fabric loader versions
                FabricVersion[] loaders = FabriclikeUtils.FABRIC_UTILS.downloadLoaderVersions(mcVersion);
                if (loaders == null || loaders.length == 0) {
                    throw new IOException("No Fabric loader found for " + mcVersion);
                }
                String loaderVersion = loaders[0].version;

                // Stage 2: Downloading Files...
                mActivity.runOnUiThread(() -> updateProgressUI(tvSub, pb, "Downloading Files...", 30));

                // A. Download Fabric launcher json metadata files (mCreateProfile = false)
                FabriclikeDownloadTask task = new FabriclikeDownloadTask(new ModloaderDownloadListener() {
                    @Override
                    public void onDownloadFinished(File downloadedFile) {
                        // Fabric loader files prepared. Now download the Optix mod file!
                        downloadOptixJar(dialog, dialogView, version, mcVersion, loaderVersion, onInstallSuccess);
                    }

                    @Override
                    public void onDownloadError(Exception e) {
                        showErrorAndDismiss(dialog, "Fabric setup failed: " + e.getMessage());
                    }

                    @Override
                    public void onDataNotAvailable() {
                        showErrorAndDismiss(dialog, "Fabric data not available");
                    }
                }, FabriclikeUtils.FABRIC_UTILS, mcVersion, loaderVersion, false);

                task.run();
            } catch (Exception e) {
                Log.e("ClientFeatures", "Fabric setup failed", e);
                showErrorAndDismiss(dialog, "Setup failed: " + e.getMessage());
            }
        }).start();
    }

    private void downloadOptixJar(final BottomSheetDialog dialog, final View dialogView, final ModVersionAdapter.ModrinthVersion version, final String mcVersion, final String loaderVersion, final Runnable onInstallSuccess) {
        final TextView tvSub = dialogView.findViewById(R.id.tv_progress_sub);
        final ProgressBar pb = dialogView.findViewById(R.id.pb_install_progress);
        
        if (version.files == null || version.files.isEmpty()) {
            showErrorAndDismiss(dialog, "No files found for this version");
            return;
        }
        final ModVersionAdapter.ModrinthVersion.ModrinthFile file = version.files.get(0);

        new Thread(() -> {
            try {
                // Stage 3: Installing Features...
                mActivity.runOnUiThread(() -> updateProgressUI(tvSub, pb, "Installing Features...", 60));

                // A. Create Dedicated Profile
                MinecraftProfile optixProfile = new MinecraftProfile();
                optixProfile.name = "Optix Client " + version.version_number;
                optixProfile.lastVersionId = "fabric-loader-" + loaderVersion + "-" + mcVersion;
                
                // Set separate isolated directory path under custom_instances
                String profileName = "Optix_" + version.version_number.replace('.', '_') + "_" + mcVersion.replace('.', '_');
                optixProfile.gameDir = "./custom_instances/" + profileName.toLowerCase();
                optixProfile.type = "modpack";
                optixProfile.icon = "Fabric";

                LauncherProfiles.load();
                LauncherProfiles.mainProfileJson.profiles.put(profileName, optixProfile);
                LauncherProfiles.write();

                // Select the new profile in preferences
                LauncherPreferences.DEFAULT_PREF.edit()
                        .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileName)
                        .apply();
                LauncherProfiles.load();

                // B. Resolve isolated mods directory and create it
                File gamedir = Tools.getGameDirPath(optixProfile);
                File modsDir = new File(gamedir, "mods");
                if (!modsDir.exists()) modsDir.mkdirs();

                File destFile = new File(modsDir, file.filename);

                // C. Download the Mod Jar
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
                    final int downloadProgress = (int) ((downloaded * 30) / totalBytes); // map 30% of progress bar
                    final int overallProgress = 60 + downloadProgress; // 60% -> 90%
                    mActivity.runOnUiThread(() -> pb.setProgress(overallProgress));
                }
                outputStream.close();
                inputStream.close();

                // Stage 4: Applying Configurations...
                mActivity.runOnUiThread(() -> updateProgressUI(tvSub, pb, "Applying Configurations...", 95));

                // Create default config folder and files inside separate directory
                File configDir = new File(gamedir, "config");
                if (!configDir.exists()) configDir.mkdirs();
                Tools.disableSplash(gamedir);

                // Save info to prefs
                mPrefs.edit()
                        .putString(KEY_VERSION_ID, version.id)
                        .putString(KEY_FILENAME, file.filename)
                        .putString(KEY_DOWNLOAD_URL, file.url)
                        .putString(KEY_MC_VERSION, mcVersion)
                        .putBoolean(KEY_ENABLED, true)
                        .apply();

                // Stage 5: Done!
                mActivity.runOnUiThread(() -> {
                    pb.setProgress(100);
                    tvSub.setText("Done");
                    tvSub.setTextColor(0xFF39FF14);
                    
                    playSuccessAnimation(dialog, dialogView, onInstallSuccess);
                });

            } catch (Exception e) {
                Log.e("ClientFeatures", "Download failed", e);
                showErrorAndDismiss(dialog, "Download failed: " + e.getMessage());
            }
        }).start();
    }

    private void playSuccessAnimation(final BottomSheetDialog dialog, final View dialogView, final Runnable onInstallSuccess) {
        final View optixLogo = dialogView.findViewById(R.id.iv_optix_logo);
        final View successCheck = dialogView.findViewById(R.id.iv_success_check);
        final TextView tvStage = dialogView.findViewById(R.id.tv_progress_stage);

        // Transition from Logo to Checkmark
        if (optixLogo != null) {
            optixLogo.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(250).start();
        }
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (successCheck != null) {
                successCheck.setVisibility(View.VISIBLE);
                successCheck.setScaleX(0f);
                successCheck.setScaleY(0f);
                successCheck.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(2f))
                    .withEndAction(() -> {
                        successCheck.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                    })
                    .start();
            }
            if (tvStage != null) {
                tvStage.setText("Client Features Enabled");
                tvStage.setTextColor(0xFF39FF14);
            }

            // Dismiss dialog after 1.5 seconds and trigger callback
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                dialog.dismiss();
                if (onInstallSuccess != null) onInstallSuccess.run();
            }, 1500);
        }, 260);
    }

    private void updateProgressUI(final TextView tv, final ProgressBar pb, final String text, final int progress) {
        if (tv != null) tv.setText(text);
        if (pb != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                pb.setProgress(progress, true);
            } else {
                pb.setProgress(progress);
            }
        }
    }

    private void showErrorAndDismiss(final BottomSheetDialog dialog, final String message) {
        mActivity.runOnUiThread(() -> {
            Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });
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

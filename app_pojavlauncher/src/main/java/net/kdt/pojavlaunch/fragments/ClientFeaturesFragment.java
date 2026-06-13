package net.kdt.pojavlaunch.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.kdt.pojavlaunch.ClientFeaturesManager;
import net.kdt.pojavlaunch.ModVersionAdapter;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;
import net.kdt.pojavlaunch.modloaders.FabricVersion;
import net.kdt.pojavlaunch.modloaders.FabriclikeDownloadTask;
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils;
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClientFeaturesFragment extends Fragment {

    public static final String TAG = "ClientFeaturesFragment";

    private ClientFeaturesManager mManager;
    private android.animation.AnimatorSet mHeroAnimatorSet;
    private final Gson mGson = new Gson();

    public ClientFeaturesFragment() {
        super(R.layout.fragment_client_features);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mManager = new ClientFeaturesManager(requireActivity());

        // Back button
        view.findViewById(R.id.btn_features_back).setOnClickListener(v -> 
                getParentFragmentManager().popBackStack());

        // Update active card & status
        updateHeroCard(view);

        // Setup versions list
        RecyclerView rv = view.findViewById(R.id.rv_features_versions);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        final ModVersionAdapter adapter = new ModVersionAdapter(new ModVersionAdapter.OnVersionSelectedListener() {
            @Override
            public void onVersionSelected(ModVersionAdapter.ModrinthVersion version) {
                startInstallWorkflow(view, version);
            }
        });
        rv.setAdapter(adapter);

        // Fetch versions from Modrinth
        new Thread(() -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/IpIMaYzj/version");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "CSLauncher/1.0 (contact@craftstudio.dev)");
                
                InputStream is = conn.getInputStream();
                String json = Tools.read(is);
                final List<ModVersionAdapter.ModrinthVersion> versions = mGson.fromJson(json, new TypeToken<List<ModVersionAdapter.ModrinthVersion>>(){}.getType());
                
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> adapter.setVersions(versions));
                }
            } catch (Exception e) {
                Log.e("ClientFeatures", "Failed to fetch versions", e);
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "Failed to fetch versions from Modrinth", Toast.LENGTH_LONG).show());
                }
            }
        }).start();
    }

    private void updateHeroCard(View view) {
        View heroGlow = view.findViewById(R.id.hero_glow);
        TextView tvVersion = view.findViewById(R.id.tv_hero_version);
        TextView tvStatus = view.findViewById(R.id.tv_hero_status);

        if (mHeroAnimatorSet != null) {
            mHeroAnimatorSet.cancel();
            mHeroAnimatorSet = null;
        }

        if (mManager.isEnabled()) {
            tvStatus.setText("ACTIVE");
            tvStatus.setTextColor(0xFF39FF14); // neon green
            String filename = requireActivity().getSharedPreferences("cs_client_features", Context.MODE_PRIVATE)
                    .getString(ClientFeaturesManager.KEY_FILENAME, "--");
            tvVersion.setText("Active: " + filename);

            if (heroGlow != null) {
                heroGlow.setVisibility(View.VISIBLE);
                heroGlow.setScaleX(1.02f);
                heroGlow.setScaleY(1.05f);
                
                android.animation.ObjectAnimator alphaAnim = android.animation.ObjectAnimator.ofFloat(heroGlow, "alpha", 0.1f, 0.7f);
                alphaAnim.setDuration(1600);
                alphaAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                alphaAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                
                android.animation.ObjectAnimator scaleXAnim = android.animation.ObjectAnimator.ofFloat(heroGlow, "scaleX", 1.01f, 1.04f);
                scaleXAnim.setDuration(1600);
                scaleXAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                scaleXAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);

                android.animation.ObjectAnimator scaleYAnim = android.animation.ObjectAnimator.ofFloat(heroGlow, "scaleY", 1.02f, 1.08f);
                scaleYAnim.setDuration(1600);
                scaleYAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                scaleYAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);

                mHeroAnimatorSet = new android.animation.AnimatorSet();
                mHeroAnimatorSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim);
                mHeroAnimatorSet.start();
            }
        } else {
            tvStatus.setText("NOT INSTALLED");
            tvStatus.setTextColor(0xFFFF6B6B); // red
            tvVersion.setText("Select a version below to install");
            if (heroGlow != null) {
                heroGlow.setVisibility(View.GONE);
            }
        }
    }

    private void startInstallWorkflow(final View view, final ModVersionAdapter.ModrinthVersion version) {
        final android.widget.ViewFlipper flipper = view.findViewById(R.id.flipper_features_page);
        if (flipper != null) {
            flipper.setInAnimation(requireContext(), android.R.anim.fade_in);
            flipper.setOutAnimation(requireContext(), android.R.anim.fade_out);
            flipper.setDisplayedChild(1);
        }

        final View pulseRing = view.findViewById(R.id.progress_pulse_ring);
        final View optixLogo = view.findViewById(R.id.iv_progress_logo);
        final View successCheck = view.findViewById(R.id.iv_progress_check);
        final TextView tvTitle = view.findViewById(R.id.tv_progress_title);
        final TextView tvSub = view.findViewById(R.id.tv_progress_subtitle);
        final ProgressBar pb = view.findViewById(R.id.pb_features_progress);

        if (successCheck != null) successCheck.setVisibility(View.GONE);
        if (optixLogo != null) {
            optixLogo.setVisibility(View.VISIBLE);
            optixLogo.setScaleX(0f);
            optixLogo.setScaleY(0f);
        }
        if (tvTitle != null) tvTitle.setText("Enabling Optix Client...");
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
            if (tvTitle != null) tvTitle.setText("Installing Client Features...");
            runInstallProcess(view, version);
        }, 700);
    }

    private void runInstallProcess(final View view, final ModVersionAdapter.ModrinthVersion version) {
        final TextView tvSub = view.findViewById(R.id.tv_progress_subtitle);
        final ProgressBar pb = view.findViewById(R.id.pb_features_progress);
        final String mcVersion = version.game_versions != null && !version.game_versions.isEmpty() ? version.game_versions.get(0) : null;
        
        if (mcVersion == null) {
            showErrorAndGoBack("Invalid version selected");
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
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> updateProgressUI(tvSub, pb, "Downloading Files...", 30));
                }

                // A. Download Fabric launcher json metadata files (mCreateProfile = false)
                FabriclikeDownloadTask task = new FabriclikeDownloadTask(new ModloaderDownloadListener() {
                    @Override
                    public void onDownloadFinished(File downloadedFile) {
                        downloadOptixJar(view, version, mcVersion, loaderVersion);
                    }

                    @Override
                    public void onDownloadError(Exception e) {
                        showErrorAndGoBack("Fabric setup failed: " + e.getMessage());
                    }

                    @Override
                    public void onDataNotAvailable() {
                        showErrorAndGoBack("Fabric data not available");
                    }
                }, FabriclikeUtils.FABRIC_UTILS, mcVersion, loaderVersion, false);

                task.run();
            } catch (Exception e) {
                Log.e("ClientFeatures", "Fabric setup failed", e);
                showErrorAndGoBack("Setup failed: " + e.getMessage());
            }
        }).start();
    }

    private void downloadOptixJar(final View view, final ModVersionAdapter.ModrinthVersion version, final String mcVersion, final String loaderVersion) {
        final TextView tvSub = view.findViewById(R.id.tv_progress_subtitle);
        final ProgressBar pb = view.findViewById(R.id.pb_features_progress);
        
        if (version.files == null || version.files.isEmpty()) {
            showErrorAndGoBack("No files found for this version");
            return;
        }
        final ModVersionAdapter.ModrinthVersion.ModrinthFile file = version.files.get(0);

        new Thread(() -> {
            try {
                // Stage 3: Installing Features...
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> updateProgressUI(tvSub, pb, "Installing Features...", 60));
                }

                // A. Create Dedicated Profile
                MinecraftProfile optixProfile = new MinecraftProfile();
                optixProfile.name = "Optix Client " + version.version_number;
                optixProfile.lastVersionId = "fabric-loader-" + loaderVersion + "-" + mcVersion;
                
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
                    if (isAdded() && getContext() != null) {
                        requireActivity().runOnUiThread(() -> pb.setProgress(overallProgress));
                    }
                }
                outputStream.close();
                inputStream.close();

                // Stage 4: Applying Configurations...
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> updateProgressUI(tvSub, pb, "Applying Configurations...", 95));
                }

                // Create default config folder and files inside separate directory
                File configDir = new File(gamedir, "config");
                if (!configDir.exists()) configDir.mkdirs();
                Tools.disableSplash(gamedir);

                // Save info to prefs
                requireActivity().getSharedPreferences("cs_client_features", Context.MODE_PRIVATE).edit()
                        .putString(ClientFeaturesManager.KEY_VERSION_ID, version.id)
                        .putString(ClientFeaturesManager.KEY_FILENAME, file.filename)
                        .putString(ClientFeaturesManager.KEY_DOWNLOAD_URL, file.url)
                        .putString(ClientFeaturesManager.KEY_MC_VERSION, mcVersion)
                        .putBoolean(ClientFeaturesManager.KEY_ENABLED, true)
                        .apply();

                // Stage 5: Done!
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        pb.setProgress(100);
                        tvSub.setText("Done");
                        tvSub.setTextColor(0xFF39FF14);
                        
                        playSuccessAnimation(view);
                    });
                }

            } catch (Exception e) {
                Log.e("ClientFeatures", "Download failed", e);
                showErrorAndGoBack("Download failed: " + e.getMessage());
            }
        }).start();
    }

    private void playSuccessAnimation(final View view) {
        final View optixLogo = view.findViewById(R.id.iv_progress_logo);
        final View successCheck = view.findViewById(R.id.iv_progress_check);
        final TextView tvTitle = view.findViewById(R.id.tv_progress_title);

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
            if (tvTitle != null) {
                tvTitle.setText("Client Features Enabled");
                tvTitle.setTextColor(0xFF39FF14);
            }

            // Pop back stack after 1.5 seconds to return to Main Menu
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && getContext() != null) {
                    getParentFragmentManager().popBackStack();
                }
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

    private void showErrorAndGoBack(final String message) {
        if (isAdded() && getContext() != null) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                getParentFragmentManager().popBackStack();
            });
        }
    }

    @Override
    public void onDestroyView() {
        if (mHeroAnimatorSet != null) {
            mHeroAnimatorSet.cancel();
            mHeroAnimatorSet = null;
        }
        super.onDestroyView();
    }
}

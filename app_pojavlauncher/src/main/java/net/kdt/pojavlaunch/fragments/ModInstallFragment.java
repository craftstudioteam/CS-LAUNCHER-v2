package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.modloaders.modpacks.models.Constants.SOURCE_MODRINTH;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackApi;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModrinthApi;
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModInstallFragment extends Fragment {

    public static final String TAG = "ModInstallFragment";
    private static final String ARG_MOD_ITEM = "mod_item";
    private static final String ARG_MOD_DETAIL = "mod_detail";
    private static final String ARG_VERSION_INDEX = "version_index";
    private static final String ARG_PROFILE_KEY = "profile_key";

    private ModItem mModItem;
    private ModDetail mModDetail;
    private int mVersionIndex;
    private String mProfileKey;

    private ImageView mBackButton;
    private ImageView mModIcon;
    private TextView mModTitle;
    private TextView mVersionBadge;
    private TextView mFullDescription;
    private Button mInstallButton;

    // View references for animations
    private View mTopBar;
    private View mBottomBar;
    private View mScrollContent;

    public static ModInstallFragment newInstance(ModItem item, ModDetail detail,
                                                  int versionIndex, String profileKey) {
        ModInstallFragment f = new ModInstallFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MOD_ITEM, item);
        args.putSerializable(ARG_MOD_DETAIL, detail);
        args.putInt(ARG_VERSION_INDEX, versionIndex);
        args.putString(ARG_PROFILE_KEY, profileKey);
        f.setArguments(args);
        return f;
    }

    public ModInstallFragment() {
        super(R.layout.fragment_mod_install);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mModItem = (ModItem) getArguments().getSerializable(ARG_MOD_ITEM);
            mModDetail = (ModDetail) getArguments().getSerializable(ARG_MOD_DETAIL);
            mVersionIndex = getArguments().getInt(ARG_VERSION_INDEX);
            mProfileKey = getArguments().getString(ARG_PROFILE_KEY);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Bind premium ID references
        mTopBar = view.findViewById(R.id.install_top_bar);
        mBackButton = view.findViewById(R.id.install_back_button);
        mModIcon = view.findViewById(R.id.install_mod_icon);
        mModTitle = view.findViewById(R.id.install_mod_title);
        mVersionBadge = view.findViewById(R.id.install_selected_version_badge);
        mFullDescription = view.findViewById(R.id.install_full_description);
        mBottomBar = view.findViewById(R.id.install_bottom_bar);
        mInstallButton = view.findViewById(R.id.install_button);
        mScrollContent = view.findViewById(R.id.install_scroll_content);

        // Populate UI
        if (mModItem != null) {
            mModTitle.setText(mModItem.title);

            // Load icon asynchronously
            ModIconCache iconCache = new ModIconCache();
            iconCache.getImage(
                    bitmap -> {
                        if (bitmap != null && isAdded()) {
                            mModIcon.setImageBitmap(bitmap);
                        }
                    },
                    mModItem.getIconCacheTag(),
                    mModItem.imageUrl
            );
        }

        if (mModDetail != null) {
            // Show full description
            if (mModDetail.description != null && !mModDetail.description.isEmpty()) {
                mFullDescription.setText(mModDetail.description);
            }

            // Show selected version badge
            if (mVersionIndex >= 0 && mModDetail.versionNames != null
                    && mVersionIndex < mModDetail.versionNames.length) {
                mVersionBadge.setText(mModDetail.versionNames[mVersionIndex]);
            }

            // Determine the file name from version URL
            String versionUrl = (mModDetail.versionUrls != null
                    && mVersionIndex >= 0 && mVersionIndex < mModDetail.versionUrls.length)
                    ? mModDetail.versionUrls[mVersionIndex] : null;

            final String fileName;
            if (versionUrl != null && !versionUrl.isEmpty()) {
                String raw = versionUrl.substring(versionUrl.lastIndexOf('/') + 1);
                if (raw.contains("?")) raw = raw.substring(0, raw.indexOf('?'));
                fileName = raw;
            } else {
                fileName = (mModItem != null ? mModItem.title : "mod") + ".jar";
            }

            final String finalUrl = versionUrl;

            mInstallButton.setOnClickListener(v -> {
                if (finalUrl == null || finalUrl.isEmpty()) {
                    Toast.makeText(getContext(),
                            R.string.modpack_install_download_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                startDownload(finalUrl, fileName);
            });
        }

        // Back button — pop back to the mod detail / list
        mBackButton.setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        if (getView() != null) {
            getView().post(this::setupInstallAnimations);
        }
    }

    // ─── Premium Entry Animations ──────────────────────────────────────

    private void setupInstallAnimations() {
        if (mTopBar != null) {
            mTopBar.setTranslationY(-60f);
            mTopBar.setAlpha(0f);
            mTopBar.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(260)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        if (mBottomBar != null) {
            mBottomBar.setTranslationY(80f);
            mBottomBar.setAlpha(0f);
            mBottomBar.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(280)
                    .setStartDelay(60)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        // Start staggered content layout animation
        if (mScrollContent != null) {
            View content = mScrollContent;
            if (content instanceof ViewGroup) {
                ((ViewGroup) content).startLayoutAnimation();
            }
        }

        // Bounce animation on INSTALL button (on load)
        if (mInstallButton != null) {
            mInstallButton.setScaleX(0.8f);
            mInstallButton.setScaleY(0.8f);
            mInstallButton.postDelayed(() -> {
                if (!isAdded()) return;
                mInstallButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(400)
                        .setInterpolator(new OvershootInterpolator(1.5f))
                        .start();
            }, 200);
        }

        // Premium button press scale effect
        if (mInstallButton != null) {
            mInstallButton.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120)
                                .setInterpolator(new OvershootInterpolator(1.5f))
                                .start();
                        break;
                }
                return false;
            });
        }

        // Premium back button press scale effect
        if (mBackButton != null) {
            mBackButton.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(70).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        break;
                }
                return false;
            });
        }
    }

    // ─── Download & Dependency Logic ──────────────────────────────────

    private void startDownload(String url, String fileName) {
        Context ctx = getContext();
        if (ctx == null) return;

        // Modpack: use handleInstallation which creates a full instance
        if (mModItem != null && mModItem.isModpack) {
            mInstallButton.setEnabled(false);
            mInstallButton.setText("Installing modpack...");
            ModpackApi api;
            if (mModItem.apiSource == Constants.SOURCE_MODRINTH) {
                api = new ModrinthApi();
            } else {
                api = new CommonApi(requireContext().getString(R.string.curseforge_api_key));
            }
            api.handleInstallation(ctx, mModDetail, mVersionIndex);
            return;
        }

        // Individual mod: check for dependencies
        if (mModDetail != null && mModDetail.versionDependencyIds != null
                && mVersionIndex >= 0 && mVersionIndex < mModDetail.versionDependencyIds.length) {
            showDependencyDialog(ctx, url, fileName);
        } else {
            downloadMod(ctx, url, fileName,
                    new String[0], new String[0]);
        }
    }

    private void showDependencyDialog(Context ctx, String url, String fileName) {
        String[] depIds = mModDetail.versionDependencyIds[mVersionIndex];
        String[] depNames = new String[depIds != null ? depIds.length : 0];
        if (depIds != null) {
            for (int i = 0; i < depIds.length; i++) {
                depNames[i] = "Dependency: " + depIds[i];
            }
        }
        String[] depTypes = mModDetail.versionDependencyTypes[mVersionIndex];
        if (depIds == null || depIds.length == 0) {
            downloadMod(ctx, url, fileName, new String[0], new String[0]);
            return;
        }

        boolean[] selected = new boolean[depIds.length];
        for (int i = 0; i < depIds.length; i++) {
            selected[i] = true;
        }

        new androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle(R.string.mod_deps_title)
                .setMultiChoiceItems(depNames, selected,
                        (dialog, which, isChecked) -> selected[which] = isChecked)
                .setPositiveButton(R.string.mod_deps_install_selected, (d, w) -> {
                    List<String> list = new ArrayList<>();
                    for (int i = 0; i < depIds.length; i++) {
                        if (selected[i]) list.add(depIds[i]);
                    }
                    downloadMod(ctx, url, fileName,
                            list.toArray(new String[0]), depTypes);
                })
                .setNeutralButton(R.string.mod_deps_install_without,
                        (d, w) -> downloadMod(ctx, url, fileName,
                                new String[0], new String[0]))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void downloadMod(Context ctx, String url, String fileName,
                              String[] depIds, String[] depTypes) {
        File modsDir = getModsDir();
        if (!modsDir.exists()) modsDir.mkdirs();

        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.global_waiting);
        mInstallButton.setEnabled(false);
        mInstallButton.setText(R.string.mod_installing);

        PojavApplication.sExecutorService.execute(() -> {
            try {
                DownloadUtils.downloadFile(url, new File(modsDir, fileName));
                for (String depId : depIds) {
                    if (depId == null || depId.isEmpty()) continue;
                    downloadDependency(depId, modsDir);
                }
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
                Tools.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(ctx,
                            ctx.getString(R.string.mod_install_success, fileName),
                            Toast.LENGTH_LONG).show();
                    // Pop back stack to mod list
                    getParentFragmentManager().popBackStack(
                            ModsSearchFragment.TAG,
                            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                    );
                });
            } catch (Exception e) {
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
                Tools.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    mInstallButton.setEnabled(true);
                    mInstallButton.setText(R.string.mod_install_now);
                    Tools.showErrorRemote(ctx, R.string.modpack_install_download_failed, e);
                });
            }
        });
    }

    private void downloadDependency(String projectId, File modsDir) {
        try {
            ModrinthApi api = new ModrinthApi();
            ModItem depItem = new ModItem(SOURCE_MODRINTH, false,
                    projectId, projectId, "", "");
            ModDetail depDetail = api.getModDetails(depItem);
            if (depDetail == null || depDetail.versionUrls == null
                    || depDetail.versionUrls.length == 0) return;
            String depUrl = depDetail.versionUrls[0];
            String depName = depUrl.substring(depUrl.lastIndexOf('/') + 1);
            if (depName.contains("?")) depName = depName.substring(0, depName.indexOf('?'));
            if (!depName.endsWith(".jar")) depName += ".jar";
            DownloadUtils.downloadFile(depUrl, new File(modsDir, depName));
        } catch (Exception e) {
            Log.w(TAG, "Failed to download dependency " + projectId);
        }
    }

    private File getModsDir() {
        try {
            String key = mProfileKey != null ? mProfileKey
                    : LauncherPreferences.DEFAULT_PREF.getString(
                            LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            if (key != null && !key.isEmpty()) {
                LauncherProfiles.load();
                MinecraftProfile profile = LauncherProfiles.mainProfileJson.profiles.get(key);
                if (profile != null) return new File(Tools.getGameDirPath(profile), "mods");
            }
        } catch (Exception ignored) {}
        return new File(Tools.DIR_GAME_NEW, "mods");
    }
}

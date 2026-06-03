package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.modloaders.modpacks.models.Constants.SOURCE_MODRINTH;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
    private TextView mChangelogView;
    private Button mInstallButton;

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
        mBackButton = view.findViewById(R.id.install_back_button);
        mModIcon = view.findViewById(R.id.install_mod_icon);
        mModTitle = view.findViewById(R.id.install_mod_title);
        mVersionBadge = view.findViewById(R.id.install_selected_version_badge);
        mFullDescription = view.findViewById(R.id.install_full_description);
        mChangelogView = view.findViewById(R.id.install_changelog);
        mInstallButton = view.findViewById(R.id.install_button);

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

        if (mModDetail != null && mVersionIndex >= 0
                && mVersionIndex < mModDetail.versionNames.length) {
            String versionName = mModDetail.versionNames[mVersionIndex];
            String mcVer = (mModDetail.mcVersionNames != null
                    && mVersionIndex < mModDetail.mcVersionNames.length)
                    ? mModDetail.mcVersionNames[mVersionIndex] : "";
            String badge = mcVer.isEmpty() ? versionName : versionName + " — " + mcVer;
            mVersionBadge.setText(badge);
        }

        if (mModItem != null && mModItem.description != null) {
            mFullDescription.setText(mModItem.description);
        }

        // Back
        mBackButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Install button
        mInstallButton.setOnClickListener(v -> handleInstall());

        // Entrance animation
        view.findViewById(R.id.install_scroll_content).setAlpha(0f);
        view.findViewById(R.id.install_scroll_content).setTranslationY(40f);
        view.findViewById(R.id.install_scroll_content).animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .start();

        mInstallButton.setScaleX(0.9f);
        mInstallButton.setScaleY(0.9f);
        mInstallButton.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(350)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.3f))
                .start();
    }

    private void handleInstall() {
        if (mModDetail == null || mVersionIndex < 0
                || mVersionIndex >= mModDetail.versionUrls.length) return;

        String url = mModDetail.versionUrls[mVersionIndex];
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        if (fileName.contains("?")) fileName = fileName.substring(0, fileName.indexOf('?'));
        if (!fileName.endsWith(".jar")) fileName += ".jar";

        // Check deps
        String[] depIds = (mModDetail.versionDependencyIds != null
                && mVersionIndex < mModDetail.versionDependencyIds.length)
                ? mModDetail.versionDependencyIds[mVersionIndex] : null;
        String[] depTypes = (mModDetail.versionDependencyTypes != null
                && mVersionIndex < mModDetail.versionDependencyTypes.length)
                ? mModDetail.versionDependencyTypes[mVersionIndex] : null;

        boolean hasRequired = false;
        List<String> requiredIds = new ArrayList<>();
        if (depIds != null && depTypes != null) {
            for (int i = 0; i < depIds.length; i++) {
                if ("required".equals(depTypes[i])) {
                    hasRequired = true;
                    requiredIds.add(depIds[i]);
                }
            }
        }

        if (hasRequired && !requiredIds.isEmpty()) {
            showDependencyDialog(url, fileName);
        } else {
            downloadMod(requireContext(), url, fileName, new String[0], new String[0]);
        }
    }

    private void showDependencyDialog(String url, String fileName) {
        Context ctx = requireContext();
        if (mModDetail == null || mVersionIndex < 0) return;

        String[] depIds = mModDetail.versionDependencyIds[mVersionIndex];
        String[] depTypes = mModDetail.versionDependencyTypes[mVersionIndex];
        String[] depNames = new String[depIds.length];
        boolean[] selected = new boolean[depIds.length];
        for (int i = 0; i < depIds.length; i++) {
            selected[i] = true;
            depNames[i] = "Dependency: " + depIds[i];
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
                    downloadMod(ctx, url, fileName, list.toArray(new String[0]), depTypes);
                })
                .setNeutralButton(R.string.mod_deps_install_without,
                        (d, w) -> downloadMod(ctx, url, fileName, new String[0], new String[0]))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void downloadMod(Context ctx, String url, String fileName,
                              String[] depIds, String[] depTypes) {
        File modsDir = getModsDir();
        if (!modsDir.exists()) modsDir.mkdirs();

        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.global_waiting);
        mInstallButton.setEnabled(false);
        mInstallButton.setText("INSTALLING...");

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
                    mInstallButton.setText("INSTALL NOW");
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

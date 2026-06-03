package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.ProgressLayout;
import com.kdt.SimpleArrayAdapter;

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
import java.util.Collections;
import java.util.List;

public class ModDetailFragment extends Fragment {

    public static final String TAG = "ModDetailFragment";
    private static final String ARG_MOD_ITEM = "mod_item";

    private ModItem mModItem;
    private ModDetail mModDetail;
    private ModpackApi mModpackApi;

    // Views
    private ImageView mBackButton;
    private TextView mTopBarTitle;
    private ImageView mModIcon;
    private ImageView mSourceBadge;
    private TextView mModTitle;
    private TextView mModSubtitle;
    private TextView mFullDescription;
    private Spinner mVersionSpinner;
    private TextView mVersionError;
    private Button mDownloadButton;
    private View mBottomBar;
    private View mScrollContent;
    private LinearLayout mSpinnerContainer;

    private int mSelectedVersionIndex = -1;
    private boolean mSuppressSelectionCallback;
    private String mProfileKey;
    private ModIconCache mIconCache;

    public static ModDetailFragment newInstance(ModItem item) {
        ModDetailFragment fragment = new ModDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MOD_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    public ModDetailFragment() {
        super(R.layout.fragment_mod_detail);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mModItem = (ModItem) getArguments().getSerializable(ARG_MOD_ITEM);
        }
        mProfileKey = getArguments() != null
                ? getArguments().getString(ManageModsFragment.BUNDLE_PROFILE_KEY) : null;
        if (mProfileKey == null) {
            mProfileKey = LauncherPreferences.DEFAULT_PREF
                    .getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        }
        mIconCache = new ModIconCache();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Initialize views
        mBackButton = view.findViewById(R.id.detail_back_button);
        mTopBarTitle = view.findViewById(R.id.detail_title_textview);
        mModIcon = view.findViewById(R.id.detail_mod_icon);
        mSourceBadge = view.findViewById(R.id.detail_source_badge);
        mModTitle = view.findViewById(R.id.detail_mod_title);
        mModSubtitle = view.findViewById(R.id.detail_mod_subtitle);
        mFullDescription = view.findViewById(R.id.detail_full_description);
        mVersionSpinner = view.findViewById(R.id.detail_version_spinner);
        mVersionError = view.findViewById(R.id.detail_version_error);
        mDownloadButton = view.findViewById(R.id.detail_download_button);
        mBottomBar = view.findViewById(R.id.detail_bottom_bar);
        mScrollContent = view.findViewById(R.id.detail_scroll_content);
        mSpinnerContainer = view.findViewById(R.id.detail_spinner_container);

        // Set up ModpackApi — determine source from the ModItem
        if (mModItem != null && mModItem.apiSource == Constants.SOURCE_MODRINTH) {
            mModpackApi = new ModrinthApi();
        } else {
            mModpackApi = new CommonApi(requireContext().getString(R.string.curseforge_api_key));
        }

        populateModInfo();
        setupBackButton();
        setupSpinner();
        loadModDetails();

        // Entrance animation — slide up + fade for content below the top bar
        animateEntrance();
    }

    // ── Populate Mod Info ──────────────────────────────────────────────

    private void populateModInfo() {
        if (mModItem == null) return;

        mTopBarTitle.setText(mModItem.title);
        mModTitle.setText(mModItem.title);

        // Subtitle: author + downloads
        StringBuilder info = new StringBuilder();
        if (mModItem.author != null && !mModItem.author.isEmpty()) {
            info.append("by ").append(mModItem.author);
        }
        if (mModItem.downloads != null && !mModItem.downloads.isEmpty()) {
            if (info.length() > 0) info.append(" \u2022 ");
            info.append(formatDownloads(mModItem.downloads)).append(" Downloads");
        }
        mModSubtitle.setText(info.toString());

        // Description
        if (mModItem.description != null && !mModItem.description.isEmpty()) {
            mFullDescription.setText(mModItem.description);
        } else {
            mFullDescription.setText("");
        }

        // Source badge
        mSourceBadge.setImageResource(getSourceDrawable(mModItem.apiSource));

        // Load icon asynchronously
        mModIcon.setImageDrawable(null);
        mIconCache.getImage(
                bitmap -> {
                    if (bitmap != null && isAdded()) {
                        mModIcon.setImageBitmap(bitmap);
                    } else if (isAdded()) {
                        mModIcon.setImageResource(R.mipmap.ic_launcher_foreground);
                    }
                },
                mModItem.getIconCacheTag(),
                mModItem.imageUrl
        );
    }

    // ── Back Navigation ────────────────────────────────────────────────

    private void setupBackButton() {
        mBackButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    // ── Spinner Setup ──────────────────────────────────────────────────

    private void setupSpinner() {
        mVersionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mSuppressSelectionCallback) return;
                if (mModDetail != null && position < mModDetail.versionNames.length) {
                    mSelectedVersionIndex = position;
                    enableDownloadButton(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSelectedVersionIndex = -1;
                enableDownloadButton(false);
            }
        });
    }

    // ── Load Mod Details (Versions) ────────────────────────────────────

    private void loadModDetails() {
        mVersionSpinner.setAdapter(new SimpleArrayAdapter<>(
                Collections.singletonList("Loading versions...")));
        mVersionError.setVisibility(View.GONE);

        PojavApplication.sExecutorService.execute(() -> {
            try {
                ModDetail detail = mModpackApi.getModDetails(mModItem);
                Tools.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (detail != null && detail.versionNames != null && detail.versionNames.length > 0) {
                        mModDetail = detail;
                        populateVersions(detail);
                    } else {
                        mVersionError.setVisibility(View.VISIBLE);
                        mVersionError.setText(R.string.search_modpack_download_error);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Failed to load mod details", e);
                Tools.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    mVersionError.setVisibility(View.VISIBLE);
                    mVersionError.setText(getString(R.string.search_modpack_download_error));
                });
            }
        });
    }

    private void populateVersions(ModDetail detail) {
        // Use lightweight ArrayAdapter with dark theme dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                R.layout.item_spinner_dark,
                detail.versionNames
        );
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown_dark);
        mVersionSpinner.setAdapter(adapter);

        // Store current selection to avoid animation loop during setup
        // If there is only one version, auto-select it without firing animation
        if (detail.versionNames.length == 1) {
            // Suppress onItemSelected during initial setup
            mSuppressSelectionCallback = true;
            mVersionSpinner.setSelection(0, false);
            mSuppressSelectionCallback = false;
            // Manually set state without animation
            mSelectedVersionIndex = 0;
            mDownloadButton.setEnabled(true);
            mDownloadButton.setBackgroundResource(R.drawable.background_download_button_enabled);
            mDownloadButton.setText(R.string.detail_download_now);
            mDownloadButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.neon_green));
            mDownloadButton.setOnClickListener(v -> handleDownload());
        }
    }

    // ── Download Button State ──────────────────────────────────────────

    private void enableDownloadButton(boolean enabled) {
        boolean wasEnabled = mDownloadButton.isEnabled();
        mDownloadButton.setEnabled(enabled);
        if (enabled) {
            mDownloadButton.setBackgroundResource(R.drawable.background_download_button_enabled);
            mDownloadButton.setText(R.string.detail_download_now);
            mDownloadButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.neon_green));

            // Scale-up bounce animation only on transition from disabled→enabled
            if (!wasEnabled) {
                mDownloadButton.setScaleX(0.8f);
                mDownloadButton.setScaleY(0.8f);
                mDownloadButton.animate()
                        .cancel();
                mDownloadButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(400)
                        .setInterpolator(new OvershootInterpolator(1.5f))
                        .start();
            }

            mDownloadButton.setOnClickListener(v -> handleDownload());
        } else {
            mDownloadButton.setBackgroundResource(R.drawable.background_download_button_disabled);
            mDownloadButton.setText(R.string.detail_select_version_first);
            mDownloadButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary_dim));
            mDownloadButton.setOnClickListener(null);
        }
    }

    // ── Download / Install ─────────────────────────────────────────────

    private void handleDownload() {
        if (mModDetail == null || mSelectedVersionIndex < 0) return;

        String url = mModDetail.versionUrls[mSelectedVersionIndex];
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        if (fileName.contains("?")) fileName = fileName.substring(0, fileName.indexOf('?'));
        if (!fileName.endsWith(".jar")) fileName += ".jar";

        // Check for dependencies
        String[] depIds = (mModDetail.versionDependencyIds != null
                && mSelectedVersionIndex < mModDetail.versionDependencyIds.length)
                ? mModDetail.versionDependencyIds[mSelectedVersionIndex] : null;
        String[] depTypes = (mModDetail.versionDependencyTypes != null
                && mSelectedVersionIndex < mModDetail.versionDependencyTypes.length)
                ? mModDetail.versionDependencyTypes[mSelectedVersionIndex] : null;

        boolean hasRequiredDeps = false;
        List<String> requiredDepNames = new ArrayList<>();
        if (depIds != null && depTypes != null) {
            for (int i = 0; i < depIds.length; i++) {
                if (depTypes[i] != null && depTypes[i].equals("required")) {
                    hasRequiredDeps = true;
                    requiredDepNames.add(depIds[i]);
                }
            }
        }

        if (hasRequiredDeps && requiredDepNames.size() > 0) {
            String[] reqIds = requiredDepNames.toArray(new String[0]);
            String[] reqTypes = new String[reqIds.length];
            // Map back to original types for the required ones
            int idx = 0;
            for (int i = 0; i < depIds.length; i++) {
                if (depTypes[i] != null && depTypes[i].equals("required")) {
                    reqTypes[idx++] = depTypes[i];
                }
            }
            showDependencyDialog(reqIds, reqTypes, url, fileName);
        } else {
            downloadMod(requireContext(), url, fileName, new String[0], new String[0]);
        }
    }

    private void showDependencyDialog(String[] depIds, String[] depTypes,
                                       String url, String fileName) {
        Context context = requireContext();
        String[] depNames = new String[depIds.length];
        boolean[] selected = new boolean[depIds.length];
        for (int i = 0; i < depIds.length; i++) {
            selected[i] = true;
            depNames[i] = "Dependency: " + depIds[i];
        }

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(R.string.mod_deps_title)
                .setMultiChoiceItems(depNames, selected,
                        (dialog, which, isChecked) -> selected[which] = isChecked)
                .setPositiveButton(R.string.mod_deps_install_selected, (d, w) -> {
                    List<String> selectedIds = new ArrayList<>();
                    for (int i = 0; i < depIds.length; i++) {
                        if (selected[i]) selectedIds.add(depIds[i]);
                    }
                    downloadMod(context, url, fileName,
                            selectedIds.toArray(new String[0]), depTypes);
                })
                .setNeutralButton(R.string.mod_deps_install_without,
                        (d, w) -> downloadMod(context, url, fileName, new String[0], new String[0]))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void downloadMod(Context context, String url, String fileName,
                              String[] depIds, String[] depTypes) {
        File modsDir = getModsDir();
        if (!modsDir.exists()) modsDir.mkdirs();

        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.global_waiting);
        PojavApplication.sExecutorService.execute(() -> {
            try {
                DownloadUtils.downloadFile(url, new File(modsDir, fileName));

                // Download selected dependencies
                for (String depId : depIds) {
                    if (depId == null || depId.isEmpty()) continue;
                    downloadDependency(depId, modsDir);
                }

                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
                Tools.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(context,
                            context.getString(R.string.mod_install_success, fileName),
                            Toast.LENGTH_LONG).show();
                    // Pop back to the mod list
                    if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                        getParentFragmentManager().popBackStack();
                    }
                });
            } catch (Exception e) {
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
                Tools.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Tools.showErrorRemote(context, R.string.modpack_install_download_failed, e);
                });
            }
        });
    }

    private void downloadDependency(String projectId, File modsDir) {
        try {
            ModrinthApi modrinthApi = new ModrinthApi();
            ModItem depItem = new ModItem(
                    Constants.SOURCE_MODRINTH,
                    false, projectId, projectId, "", "");
            ModDetail depDetail = modrinthApi.getModDetails(depItem);
            if (depDetail == null || depDetail.versionUrls == null || depDetail.versionUrls.length == 0) return;

            String depUrl = depDetail.versionUrls[0];
            String depName = depUrl.substring(depUrl.lastIndexOf('/') + 1);
            if (depName.contains("?")) depName = depName.substring(0, depName.indexOf('?'));
            if (!depName.endsWith(".jar")) depName += ".jar";

            DownloadUtils.downloadFile(depUrl, new File(modsDir, depName));
        } catch (Exception e) {
            Log.w(TAG, "Failed to download dependency " + projectId + ": " + e.getMessage());
        }
    }

    // ── Animations ─────────────────────────────────────────────────────

    private void animateEntrance() {
        if (mScrollContent == null || mBottomBar == null) return;

        // Fade + slide up the scroll content (details below top bar)
        mScrollContent.setAlpha(0f);
        mScrollContent.setTranslationY(60f);
        mScrollContent.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Bottom bar slides up from bottom with a slight delay
        mBottomBar.setTranslationY(120f);
        mBottomBar.animate()
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setStartDelay(100)
                .start();
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private File getModsDir() {
        try {
            String key = mProfileKey != null ? mProfileKey : LauncherPreferences.DEFAULT_PREF
                    .getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            if (key != null && !key.isEmpty()) {
                LauncherProfiles.load();
                MinecraftProfile profile = LauncherProfiles.mainProfileJson.profiles.get(key);
                if (profile != null) return new File(Tools.getGameDirPath(profile), "mods");
            }
        } catch (Exception ignored) {}
        return new File(Tools.DIR_GAME_NEW, "mods");
    }

    private String formatDownloads(String downloads) {
        try {
            long d = Long.parseLong(downloads);
            if (d >= 1000000) return (d / 1000000) + "M";
            if (d >= 1000) return (d / 1000) + "K";
            return String.valueOf(d);
        } catch (Exception e) {
            return downloads;
        }
    }

    private int getSourceDrawable(int apiSource) {
        switch (apiSource) {
            case Constants.SOURCE_CURSEFORGE:
                return R.drawable.ic_curseforge;
            case Constants.SOURCE_MODRINTH:
                return R.drawable.ic_modrinth;
            default:
                return R.mipmap.ic_launcher_foreground;
        }
    }
}

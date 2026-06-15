package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.modloaders.modpacks.models.Constants.SOURCE_MODRINTH;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
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
    private static final String ARG_CONTENT_TYPE = "content_type";

    private ModItem mModItem;
    private ModDetail mModDetail;
    private int mVersionIndex;
    private String mProfileKey;
    private String mContentType;

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
                                                  int versionIndex, String profileKey,
                                                  String contentType) {
        ModInstallFragment f = new ModInstallFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MOD_ITEM, item);
        args.putSerializable(ARG_MOD_DETAIL, detail);
        args.putInt(ARG_VERSION_INDEX, versionIndex);
        args.putString(ARG_PROFILE_KEY, profileKey);
        args.putString(ARG_CONTENT_TYPE, contentType != null ? contentType : "mod");
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
            mContentType = getArguments().getString(ARG_CONTENT_TYPE, "mod");
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
            ModIconCache iconCache = ModIconCache.getInstance();
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

            switch (mContentType) {
                case "resourcepack":
                    mInstallButton.setText("DOWNLOAD PACK");
                    break;
                case "world":
                    mInstallButton.setText("DOWNLOAD WORLD");
                    break;
                default:
                    mInstallButton.setText(R.string.mod_install_now);
                    break;
            }

            mInstallButton.setOnClickListener(v -> {
                if (finalUrl == null || finalUrl.isEmpty()) {
                    Toast.makeText(getContext(),
                            R.string.modpack_install_download_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                showProfileSelectorAndDownload(finalUrl, fileName);
            });
        }

        // Back button — pop back to the mod detail / list
        mBackButton.setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        // Use the view parameter directly (avoids NullPointerException if fragment is detached)
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        view.post(() -> {
            setupInstallAnimations();
            // Restore software layer after animation completes
            view.postDelayed(() -> { if (isAdded()) view.setLayerType(View.LAYER_TYPE_NONE, null); }, 500);
        });
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

    private void showProfileSelectorAndDownload(String url, String fileName) {
        net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.load();
        java.util.Map<String, net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile> profiles = 
                net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.mainProfileJson.profiles;
                
        if (profiles == null || profiles.isEmpty()) {
            startDownload(url, fileName);
            return;
        }

        String currentKey = mProfileKey != null ? mProfileKey 
                : net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.getString(
                        net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
                        
        java.util.List<String> validKeys = new java.util.ArrayList<>();
        java.util.List<String> validNames = new java.util.ArrayList<>();
        int currentSelection = -1;

        for (String key : profiles.keySet()) {
            net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile p = profiles.get(key);
            if (p == null) continue;
            
            String profileVid = (p.lastVersionId != null) ? p.lastVersionId.toLowerCase() : "";
            
            if ("mod".equals(mContentType)) {
                boolean isCompatible = false;
                if (mModDetail != null && mModDetail.versionLoaders != null && mModDetail.versionLoaders.length > mVersionIndex) {
                    String[] modLoaders = mModDetail.versionLoaders[mVersionIndex];
                    if (modLoaders != null && modLoaders.length > 0) {
                        for (String loader : modLoaders) {
                            if (net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, loader)) {
                                isCompatible = true;
                                break;
                            }
                        }
                    } else {
                        isCompatible = net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "fabric") || 
                                       net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "forge") || 
                                       net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "quilt") || 
                                       net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "neoforge") || 
                                       net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "liteloader") || 
                                       net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "optifine");
                    }
                } else {
                    isCompatible = net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "fabric") || 
                                   net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "forge") || 
                                   net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "quilt") || 
                                   net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "neoforge") || 
                                   net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "liteloader") || 
                                   net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "optifine");
                }
                
                if (!isCompatible) continue;
                
                
                if (mModDetail != null && mModDetail.mcVersionNames != null && mModDetail.mcVersionNames.length > mVersionIndex) {
                    String modMcVer = mModDetail.mcVersionNames[mVersionIndex];
                    if (modMcVer != null && !modMcVer.isEmpty()) {
                        String pmcVer = net.kdt.pojavlaunch.utils.ProfileDetection.getMcVersion(p);
                        if (!net.kdt.pojavlaunch.utils.ProfileDetection.isVersionCompatible(pmcVer, modMcVer)) {
                            continue;
                        }
                    }
                }
            } else if ("modpack".equals(mContentType)) {
                boolean isCompatible = false;
                if (mModDetail != null && mModDetail.versionLoaders != null && mModDetail.versionLoaders.length > mVersionIndex) {
                    String[] modLoaders = mModDetail.versionLoaders[mVersionIndex];
                    if (modLoaders != null && modLoaders.length > 0) {
                        for (String loader : modLoaders) {
                            if (net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, loader)) {
                                isCompatible = true;
                                break;
                            }
                        }
                    } else {
                        isCompatible = net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "fabric") || 
                                       net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "forge") || 
                                       net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "quilt") || 
                                       net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "neoforge") || 
                                       net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "liteloader") || 
                                       net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "optifine");
                    }
                } else {
                    isCompatible = net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "fabric") || 
                                   net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "forge") || 
                                   net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "quilt") || 
                                   net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "neoforge") || 
                                   net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "liteloader") || 
                                   net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "optifine");
                }
                if (!isCompatible) continue;
                
                if (mModDetail != null && mModDetail.mcVersionNames != null && mModDetail.mcVersionNames.length > mVersionIndex) {
                    String modMcVer = mModDetail.mcVersionNames[mVersionIndex];
                    if (modMcVer != null && !modMcVer.isEmpty()) {
                        String pmcVer = net.kdt.pojavlaunch.utils.ProfileDetection.getMcVersion(p);
                        if (!net.kdt.pojavlaunch.utils.ProfileDetection.isVersionCompatible(pmcVer, modMcVer)) {
                            continue;
                        }
                    }
                }
            } else if ("shader".equals(mContentType)) {
                boolean hasOptiOrLoader = net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "optifine") || 
                                          net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "fabric") || 
                                          net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "forge") || 
                                          net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "quilt") || 
                                          net.kdt.pojavlaunch.utils.ProfileDetection.hasLoader(p, "neoforge");
                if (!hasOptiOrLoader) continue;
            }
            
            validKeys.add(key);
            String safeName = (p.name != null && !p.name.isEmpty()) ? p.name : "Unnamed Profile";
            validNames.add(safeName);
            if (key.equals(currentKey)) {
                currentSelection = validKeys.size() - 1;
            }
        }

        // Try to automatically highlight/select a profile that exactly matches modMcVer
        if (mModDetail != null && mModDetail.mcVersionNames != null && mModDetail.mcVersionNames.length > mVersionIndex) {
            String modMcVer = mModDetail.mcVersionNames[mVersionIndex];
            if (modMcVer != null && !modMcVer.isEmpty()) {
                for (int i = 0; i < validKeys.size(); i++) {
                    net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile p = profiles.get(validKeys.get(i));
                    String pmcVer = net.kdt.pojavlaunch.utils.ProfileDetection.getMcVersion(p);
                    if (pmcVer.equalsIgnoreCase(modMcVer)) {
                        currentSelection = i;
                        break;
                    }
                }
            }
        }
        
        Log.i("ModProfileFilter", "Detected Profiles: " + profiles.size());
        for (String key : profiles.keySet()) {
            net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile p = profiles.get(key);
            if (p != null) {
                Log.i("ModProfileFilter", "Profile Name: " + p.name + " | Loader Type/Vid: " + p.lastVersionId);
            }
        }
        Log.i("ModProfileFilter", "Compatible Profiles: " + validKeys.size());
        
        if (validKeys.isEmpty() && !"modpack".equals(mContentType)) {
            Toast.makeText(getContext(), "No compatible mod-loader profiles found for this version.\nInstall Fabric/Forge/Quilt to this MC version first.", Toast.LENGTH_LONG).show();
            return;
        }

        if (validKeys.size() == 1 && !"modpack".equals(mContentType)) {
            mProfileKey = validKeys.get(0);
            startDownload(url, fileName);
            return;
        }

        String[] profileNames = validNames.toArray(new String[0]);
        String[] finalKeys = validKeys.toArray(new String[0]);
        
        int listSize = validNames.size();
        if ("modpack".equals(mContentType)) {
            listSize += 1;
        }
        
        CharSequence[] displayItems = new CharSequence[listSize];
        for (int i = 0; i < validNames.size(); i++) {
            String key = finalKeys[i];
            net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile p = profiles.get(key);
            String mcVer = net.kdt.pojavlaunch.utils.ProfileDetection.getMcVersion(p);
            String loaderInfo = "";
            if (p.lastVersionId != null) {
                String lower = p.lastVersionId.toLowerCase();
                if (lower.contains("fabric")) loaderInfo = " [Fabric]";
                else if (lower.contains("forge")) loaderInfo = " [Forge]";
                else if (lower.contains("neoforge")) loaderInfo = " [NeoForge]";
                else if (lower.contains("quilt")) loaderInfo = " [Quilt]";
            }
            String rawText;
            if (!mcVer.isEmpty()) {
                rawText = "✓ " + profileNames[i] + " (" + mcVer + loaderInfo + ")";
            } else {
                rawText = "✓ " + profileNames[i] + loaderInfo;
            }
            android.text.SpannableString span = new android.text.SpannableString(rawText);
            span.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.GREEN), 0, rawText.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            displayItems[i] = span;
        }
        
        if ("modpack".equals(mContentType)) {
            displayItems[listSize - 1] = "Create New Profile";
            if (validKeys.isEmpty()) {
                // If no compatible profile exists: Show: Create New Profile and automatically create one.
                Toast.makeText(getContext(), "No compatible profile exists. Creating a new one...", Toast.LENGTH_SHORT).show();
                mProfileKey = null;
                startDownload(url, fileName);
                return;
            }
        }
                        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Compatible Profiles")
                .setSingleChoiceItems(displayItems, currentSelection, (dialog, which) -> {
                    if ("modpack".equals(mContentType) && which == displayItems.length - 1) {
                        mProfileKey = null;
                    } else {
                        mProfileKey = finalKeys[which];
                    }
                    dialog.dismiss();
                    startDownload(url, fileName);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
        File targetDir = getContentDir();
        if (!targetDir.exists()) {
            boolean created = targetDir.mkdirs();
            if (created) Log.d("CS_LAUNCHER", "Created directory: " + targetDir.getAbsolutePath());
        }

        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.global_waiting);
        mInstallButton.setEnabled(false);
        mInstallButton.setText("Downloading...");

        PojavApplication.sExecutorService.execute(() -> {
            try {
                File targetFile = new File(targetDir, fileName);
                
                String title = mModItem != null ? mModItem.title : "Mod";
                String ver = (mModDetail != null && mModDetail.versionNames != null && mVersionIndex >= 0 && mVersionIndex < mModDetail.versionNames.length) ? mModDetail.versionNames[mVersionIndex] : "";
                String imgUrl = mModItem != null ? mModItem.imageUrl : null;
                
                net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper monitor = 
                    new net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper(
                        R.string.modpack_download_downloading_mods, 
                        ProgressLayout.INSTALL_MODPACK, 
                        title, ver, imgUrl, mContentType
                    );

                DownloadUtils.downloadFileMonitored(url, targetFile, null, monitor);

                // For worlds, extract ZIP and delete archive
                if ("world".equals(mContentType)) {
                    boolean ok = extractZip(targetFile, targetDir);
                    if (ok) targetFile.delete();
                }

                for (String depId : depIds) {
                    if (depId == null || depId.isEmpty()) continue;
                    downloadDependency(depId, targetDir);
                }
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
                Tools.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    String msg = ctx.getString(R.string.mod_install_success, fileName);
                    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
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

    private boolean extractZip(File zipFile, File destDir) {
        try (java.util.zip.ZipInputStream zis =
                     new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
            java.util.zip.ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract " + zipFile.getName(), e);
            return false;
        }
    }

    private void downloadDependency(String projectId, File depDir) {
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
            
            String depTitle = depItem.title != null && !depItem.title.isEmpty() ? depItem.title : projectId;
            net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper depMonitor = 
                new net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper(
                    R.string.modpack_download_downloading_mods, 
                    ProgressLayout.INSTALL_MODPACK, 
                    depTitle, "", depItem.imageUrl, mContentType
                );
            DownloadUtils.downloadFileMonitored(depUrl, new File(depDir, depName), null, depMonitor);
        } catch (Exception e) {
            Log.w(TAG, "Failed to download dependency " + projectId);
        }
    }

    private File getContentDir() {
        try {
            String key = mProfileKey != null ? mProfileKey
                    : LauncherPreferences.DEFAULT_PREF.getString(
                            LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            File profileDir = null;
            if (key != null && !key.isEmpty()) {
                LauncherProfiles.load();
                MinecraftProfile profile = LauncherProfiles.mainProfileJson.profiles.get(key);
                if (profile != null) profileDir = Tools.getGameDirPath(profile);
            }
            File baseDir = profileDir != null ? profileDir : new File(Tools.DIR_GAME_NEW);
            return ModDownloadHelper.getDestinationDir(baseDir, mContentType);
        } catch (Exception ignored) {}
        return ModDownloadHelper.getDestinationDir(new File(Tools.DIR_GAME_NEW), mContentType);
    }
}

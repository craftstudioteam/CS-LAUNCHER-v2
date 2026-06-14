package net.kdt.pojavlaunch.modloaders;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class FabriclikeDownloadTask implements Runnable, Tools.DownloaderFeedback{
    private final ModloaderDownloadListener mModloaderDownloadListener;
    private final FabriclikeUtils mUtils;
    private final String mGameVersion;
    private final String mLoaderVersion;
    private final boolean mCreateProfile;
    private net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper mProgressWrapper;
    public FabriclikeDownloadTask(ModloaderDownloadListener modloaderDownloadListener, FabriclikeUtils utils, String mGameVersion, String mLoaderVersion, boolean mCreateProfile) {
        this.mModloaderDownloadListener = modloaderDownloadListener;
        this.mUtils = utils;
        this.mGameVersion = mGameVersion;
        this.mLoaderVersion = mLoaderVersion;
        this.mCreateProfile = mCreateProfile;
    }

    @Override
    public void run() {
        android.util.Log.d("FabricInstall", "STEP 5: Downloading loader");
        mProgressWrapper = new net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper(R.string.fabric_dl_progress, ProgressLayout.INSTALL_MODPACK);
        mProgressWrapper.extraString = mUtils.getName();
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.fabric_dl_progress, mUtils.getName());
        try {
            if(runCatching()) mModloaderDownloadListener.onDownloadFinished(null);
            else mModloaderDownloadListener.onDataNotAvailable();
        }catch (IOException e) {
            mModloaderDownloadListener.onDownloadError(e);
        }
        ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
    }

    private boolean runCatching() throws IOException{
        String fabricJson = DownloadUtils.downloadString(mUtils.createJsonDownloadUrl(mGameVersion, mLoaderVersion));
        String versionId;
        try {
            JSONObject fabricJsonObject = new JSONObject(fabricJson);
            versionId = fabricJsonObject.getString("id");
        }catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        File versionJsonDir = new File(Tools.DIR_HOME_VERSION, versionId);
        File versionJsonFile = new File(versionJsonDir, versionId+".json");
        FileUtils.ensureDirectory(versionJsonDir);
        Tools.write(versionJsonFile.getAbsolutePath(), fabricJson);
        
        // FABRIC/LOADER REWORK: Do NOT create a separate profile.
        // Instead, update the existing profile's lastVersionId to use the loader.
        if(mCreateProfile) {
            LauncherProfiles.load();
            String loaderName = mUtils.getName(); // "Fabric" or "Quilt"
            
            // 1) Try the currently selected profile first
            MinecraftProfile profileToUpdate = null;
            String profileKeyToUpdate = null;
            String currentKey = LauncherPreferences.DEFAULT_PREF.getString(
                    LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            if (currentKey != null && LauncherProfiles.mainProfileJson.profiles.containsKey(currentKey)) {
                MinecraftProfile currentProfile = LauncherProfiles.mainProfileJson.profiles.get(currentKey);
                if (currentProfile != null && isProfileForGameVersion(currentProfile, mGameVersion)) {
                    profileToUpdate = currentProfile;
                    profileKeyToUpdate = currentKey;
                }
            }

            // 2) If current profile doesn't match, scan for any profile matching the game version
            if (profileToUpdate == null) {
                for (Map.Entry<String, MinecraftProfile> entry : LauncherProfiles.mainProfileJson.profiles.entrySet()) {
                    MinecraftProfile p = entry.getValue();
                    if (p != null && isProfileForGameVersion(p, mGameVersion)) {
                        profileToUpdate = p;
                        profileKeyToUpdate = entry.getKey();
                        break;
                    }
                }
            }

            // 3) If still no match, update the current profile anyway (best effort)
            if (profileToUpdate == null && currentKey != null) {
                profileToUpdate = LauncherProfiles.mainProfileJson.profiles.get(currentKey);
                profileKeyToUpdate = currentKey;
            }

            // 4) Clean up old-style duplicate profiles created by previous versions
            //    E.g. "Fabric fabric-loader-0.19.3-1.21.10" — these were created as standalone
            //    profiles before the rework. Remove them if we're updating/changing a profile now.
            java.util.ArrayList<String> keysToRemove = new java.util.ArrayList<>();
            String loaderLower = loaderName.toLowerCase();
            for (Map.Entry<String, MinecraftProfile> entry : LauncherProfiles.mainProfileJson.profiles.entrySet()) {
                MinecraftProfile p = entry.getValue();
                if (p == null) continue;
                String lvId = p.lastVersionId != null ? p.lastVersionId.toLowerCase() : "";
                // Skip the profile we're updating
                if (entry.getKey().equals(profileKeyToUpdate)) continue;
                // Match: lastVersionId contains the loader name AND the MC version
                if (lvId.contains(loaderLower) && lvId.contains(mGameVersion)) {
                    // Check if this is a standalone loader profile (not a user profile that happens to use this loader)
                    String pName = p.name != null ? p.name.toLowerCase() : "";
                    if (pName.contains(loaderLower) || pName.contains("minecraft " + mGameVersion) || pName.equals(mGameVersion)) {
                        keysToRemove.add(entry.getKey());
                    }
                }
            }

            if (profileToUpdate != null && profileKeyToUpdate != null) {
                // Update the existing profile's lastVersionId to the loader version
                profileToUpdate.lastVersionId = versionId;
                profileToUpdate.icon = mUtils.getIconName();
                // Clean profile name: show "Minecraft 1.21.10" with Fabric as loader,
                // NOT "Fabric fabric-loader-0.19.3-1.21.10"
                String lowerName = profileToUpdate.name != null ? profileToUpdate.name.toLowerCase() : "";
                // Only rename if it was a generic name or old-style loader name
                if (lowerName.isEmpty() || lowerName.startsWith("minecraft ") || lowerName.startsWith("default") ||
                        lowerName.equals(mGameVersion) || lowerName.contains("fabric-loader") || lowerName.contains("quilt-loader")) {
                    profileToUpdate.name = "Minecraft " + mGameVersion;
                }
                LauncherProfiles.mainProfileJson.profiles.put(profileKeyToUpdate, profileToUpdate);
                // Update the current profile preference
                LauncherPreferences.DEFAULT_PREF.edit()
                        .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKeyToUpdate)
                        .apply();
            } else {
                // Absolute fallback: create a new profile (should rarely happen)
                MinecraftProfile fabricProfile = new MinecraftProfile();
                fabricProfile.lastVersionId = versionId;
                fabricProfile.name = "Minecraft " + mGameVersion;
                fabricProfile.icon = mUtils.getIconName();
                LauncherProfiles.insertMinecraftProfile(fabricProfile);
            }

            // Remove old duplicate profiles
            for (String key : keysToRemove) {
                LauncherProfiles.mainProfileJson.profiles.remove(key);
            }

            LauncherProfiles.write();
        }
        return true;
    }

    /**
     * Checks whether the given profile is associated with the specified game version.
     * Compares the resolved MC version (via inheritsFrom chain) against the target.
     */
    private boolean isProfileForGameVersion(MinecraftProfile profile, String gameVersion) {
        if (profile == null || profile.lastVersionId == null) return false;
        String pmcVer = net.kdt.pojavlaunch.utils.ProfileDetection.getMcVersion(profile);
        return pmcVer != null && pmcVer.equals(gameVersion);
    }

    @Override
    public void updateProgress(int curr, int max) {
        if (mProgressWrapper != null) mProgressWrapper.updateProgress(curr, max);
    }
}

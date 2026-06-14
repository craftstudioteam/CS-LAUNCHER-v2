package net.kdt.pojavlaunch.value.launcherprofiles;

import android.util.Log;

import androidx.annotation.NonNull;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LauncherProfiles {
    public static MinecraftLauncherProfiles mainProfileJson;
    private static final File launcherProfilesFile = new File(Tools.DIR_GAME_NEW, "launcher_profiles.json");

    public static final java.util.List<Runnable> sProfileUpdateListeners = new java.util.ArrayList<>();

    public static void addUpdateListener(Runnable listener) {
        if (!sProfileUpdateListeners.contains(listener)) {
            sProfileUpdateListeners.add(listener);
        }
    }

    public static void removeUpdateListener(Runnable listener) {
        sProfileUpdateListeners.remove(listener);
    }

    /** Reload the profile from the file, creating a default one if necessary */
    public static void load(){
        if (launcherProfilesFile.exists()) {
            try {
                mainProfileJson = Tools.GLOBAL_GSON.fromJson(Tools.read(launcherProfilesFile.getAbsolutePath()), MinecraftLauncherProfiles.class);
            } catch (IOException e) {
                Log.e(LauncherProfiles.class.toString(), "Failed to load file: ", e);
                throw new RuntimeException(e);
            }
        }

        // Fill with default
        if (mainProfileJson == null) mainProfileJson = new MinecraftLauncherProfiles();
        if (mainProfileJson.profiles == null) mainProfileJson.profiles = new HashMap<>();
        if (mainProfileJson.profiles.size() == 0)
            mainProfileJson.profiles.put(UUID.randomUUID().toString(), MinecraftProfile.getDefaultProfile());

        autoScanProfiles();

        // Normalize profile names from mod installers
        if(normalizeProfileIds(mainProfileJson)){
            write();
            load();
        } else {
            // Notify listeners when profiles are reloaded/rescanned
            for (Runnable listener : new java.util.ArrayList<>(sProfileUpdateListeners)) {
                if (listener != null) Tools.runOnUiThread(listener);
            }
        }
    }

    public static void autoScanProfiles() {
        if (mainProfileJson == null || mainProfileJson.profiles == null) return;
        boolean changed = false;

        // Helper: check if a version ID looks like a loader version (not a raw MC version)
        java.util.regex.Pattern loaderPattern = java.util.regex.Pattern.compile(
                "(?i)(fabric|forge|neoforge|quilt|optifine|optix|liteloader)");

        // 1. Scan versions directory
        File versionsDir = new File(Tools.DIR_GAME_NEW, "versions");
        if (versionsDir.exists() && versionsDir.isDirectory()) {
            File[] files = versionsDir.listFiles(File::isDirectory);
            if (files != null) {
                for (File dir : files) {
                    String versionId = dir.getName();
                    String lower = versionId.toLowerCase();

                    // FABRIC/LOADER REWORK: Skip auto-creating profiles for loader versions.
                    // Fabric, Forge, NeoForge, Quilt, OptiFine etc. are loaders, not standalone
                    // game versions. They should already be handled by the installer which updates
                    // an existing profile. If there's no corresponding profile, check if any
                    // existing profile's resolved MC version matches.
                    boolean isLoaderVersion = loaderPattern.matcher(lower).find();

                    // Check if any profile already uses this lastVersionId
                    boolean exists = false;
                    for (MinecraftProfile p : mainProfileJson.profiles.values()) {
                        if (versionId.equals(p.lastVersionId)) {
                            exists = true;
                            break;
                        }
                    }

                    if (isLoaderVersion) {
                        // For loader versions, check if the underlying MC version already has a profile
                        String mcVersion = net.kdt.pojavlaunch.utils.ProfileDetection.extractMcFromVersionId(versionId);
                        boolean hasMcProfile = false;
                        if (mcVersion != null && !mcVersion.isEmpty()) {
                            for (MinecraftProfile p : mainProfileJson.profiles.values()) {
                                String resolved = net.kdt.pojavlaunch.utils.ProfileDetection.getMcVersion(p);
                                if (mcVersion.equals(resolved) || mcVersion.equals(p.lastVersionId)) {
                                    hasMcProfile = true;
                                    break;
                                }
                            }
                        }
                        if (hasMcProfile && !exists) {
                            // There's already a profile for this MC version, skip auto-creation
                            continue;
                        }
                    }

                    if (!exists) {
                        // Create a profile for this version
                        MinecraftProfile profile = new MinecraftProfile();
                        profile.lastVersionId = versionId;

                        // BRANDING: Show clean "Minecraft X.Y.Z" names, never expose internal
                        // loader version strings like "fabric-loader-0.19.3-1.21.10" to users.
                        // Extract the MC version for a clean display name.
                        String mcVer = net.kdt.pojavlaunch.utils.ProfileDetection.extractMcFromVersionId(versionId);
                        if (mcVer == null || mcVer.isEmpty()) mcVer = versionId;

                        if (lower.contains("neoforge")) {
                            profile.name = "Minecraft " + mcVer + " (NeoForge)";
                            profile.icon = "Forge";
                            profile.type = "custom";
                        } else if (lower.contains("forge")) {
                            profile.name = "Minecraft " + mcVer + " (Forge)";
                            profile.icon = "Forge";
                            profile.type = "custom";
                        } else if (lower.contains("fabric")) {
                            profile.name = "Minecraft " + mcVer;
                            profile.icon = "Fabric";
                            profile.type = "custom";
                        } else if (lower.contains("optifine")) {
                            profile.name = "Minecraft " + mcVer + " (OptiFine)";
                            profile.icon = "OptiFine";
                            profile.type = "custom";
                        } else if (lower.contains("optix")) {
                            profile.name = "Minecraft " + mcVer + " (Optix)";
                            profile.icon = "Fabric";
                            profile.type = "custom";
                        } else if (lower.contains("quilt")) {
                            profile.name = "Minecraft " + mcVer + " (Quilt)";
                            profile.icon = "Fabric";
                            profile.type = "custom";
                        } else if (lower.contains("liteloader")) {
                            profile.name = "Minecraft " + mcVer + " (LiteLoader)";
                            profile.icon = "Grass";
                            profile.type = "custom";
                        } else {
                            profile.name = "Minecraft " + mcVer;
                            profile.icon = "Grass";
                            profile.type = "custom";
                        }

                        mainProfileJson.profiles.put(UUID.randomUUID().toString(), profile);
                        changed = true;
                    }
                }
            }
        }

        // 2. Scan custom_instances directory
        File instancesDir = new File(Tools.DIR_GAME_NEW, "custom_instances");
        if (instancesDir.exists() && instancesDir.isDirectory()) {
            File[] files = instancesDir.listFiles(File::isDirectory);
            if (files != null) {
                for (File dir : files) {
                    String instName = dir.getName();
                    String relativePath = "./custom_instances/" + instName;
                    // Check if any profile uses this gameDir
                    boolean exists = false;
                    for (MinecraftProfile p : mainProfileJson.profiles.values()) {
                        if (p.gameDir != null && (p.gameDir.equals(relativePath) || p.gameDir.toLowerCase().contains(instName.toLowerCase()))) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        // Create profile for this custom instance
                        MinecraftProfile profile = new MinecraftProfile();
                        profile.gameDir = relativePath;
                        profile.type = "modpack";

                        String lower = instName.toLowerCase();
                        if (lower.contains("optix")) {
                            profile.name = "Optix Client Instance";
                            profile.icon = "Fabric";
                            profile.lastVersionId = findBestVersionForInstance("fabric");
                        } else if (lower.contains("client")) {
                            profile.name = "Client Feature Instance";
                            profile.icon = "Fabric";
                            profile.lastVersionId = findBestVersionForInstance("fabric");
                        } else {
                            profile.name = "Modpack " + instName;
                            profile.icon = "Grass";
                            profile.lastVersionId = findBestVersionForInstance(null);
                        }

                        mainProfileJson.profiles.put(UUID.randomUUID().toString(), profile);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            write();
        }
    }

    private static String findBestVersionForInstance(String keyword) {
        File versionsDir = new File(Tools.DIR_GAME_NEW, "versions");
        if (versionsDir.exists() && versionsDir.isDirectory()) {
            File[] files = versionsDir.listFiles(File::isDirectory);
            if (files != null && files.length > 0) {
                if (keyword != null) {
                    for (File dir : files) {
                        if (dir.getName().toLowerCase().contains(keyword)) {
                            return dir.getName();
                        }
                    }
                }
                return files[0].getName();
            }
        }
        return "1.21.10";
    }

    /** Apply the current configuration into a file */
    public static void write() {
        try {
            Tools.write(launcherProfilesFile.getAbsolutePath(), mainProfileJson.toJson());
            for (Runnable listener : new java.util.ArrayList<>(sProfileUpdateListeners)) {
                if (listener != null) Tools.runOnUiThread(listener);
            }
        } catch (IOException e) {
            Log.e(LauncherProfiles.class.toString(), "Failed to write profile file", e);
            throw new RuntimeException(e);
        }
    }

    public static @NonNull MinecraftProfile getCurrentProfile() {
        if(mainProfileJson == null) LauncherProfiles.load();
        String defaultProfileName = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, "");
        MinecraftProfile profile = mainProfileJson.profiles.get(defaultProfileName);
        if(profile == null) throw new RuntimeException("The current profile stopped existing :(");
        return profile;
    }

    /**
     * Insert a new profile into the profile map
     * @param minecraftProfile the profile to insert
     */
    public static void insertMinecraftProfile(MinecraftProfile minecraftProfile) {
        mainProfileJson.profiles.put(getFreeProfileKey(), minecraftProfile);
    }

    /**
     * Pick an unused normalized key to store a new profile with
     * @return an unused key
     */
    public static String getFreeProfileKey() {
        Map<String, MinecraftProfile> profileMap = mainProfileJson.profiles;
        String freeKey = UUID.randomUUID().toString();
        while(profileMap.get(freeKey) != null) freeKey = UUID.randomUUID().toString();
        return freeKey;
    }

    /**
     * For all keys to be UUIDs, effectively isolating profile created by installers
     * This avoids certain profiles to be erased by the installer
     * @return Whether some profiles have been normalized
     */
    private static boolean normalizeProfileIds(MinecraftLauncherProfiles launcherProfiles){
        boolean hasNormalized = false;
        ArrayList<String> keys = new ArrayList<>();

        // Detect denormalized keys
        for(String profileKey : launcherProfiles.profiles.keySet()){
            try{
                if(!UUID.fromString(profileKey).toString().equals(profileKey)) keys.add(profileKey);
            }catch (IllegalArgumentException exception){
                keys.add(profileKey);
                Log.w(LauncherProfiles.class.toString(), "Illegal profile uuid: " + profileKey);
            }
        }

        // Swap the new keys
        for(String profileKey : keys){
            MinecraftProfile currentProfile = launcherProfiles.profiles.get(profileKey);
            insertMinecraftProfile(currentProfile);
            launcherProfiles.profiles.remove(profileKey);
            hasNormalized = true;
        }

        return hasNormalized;
    }
}

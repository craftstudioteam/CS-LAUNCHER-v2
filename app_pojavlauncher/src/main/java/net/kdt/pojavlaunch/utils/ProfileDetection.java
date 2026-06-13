package net.kdt.pojavlaunch.utils;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

public class ProfileDetection {
    public static String getMcVersion(MinecraftProfile profile) {
        if (profile == null || profile.lastVersionId == null) return "";
        try {
            JMinecraftVersionList.Version v = Tools.getVersion(profile.lastVersionId);
            if (v != null) {
                if (v.inheritsFrom != null && !v.inheritsFrom.isEmpty()) {
                    return v.inheritsFrom;
                }
                return v.id; // Vanilla version
            }
        } catch (Exception e) {}
        return "";
    }

    public static boolean hasLoader(MinecraftProfile profile, String loader) {
        if (profile == null || profile.lastVersionId == null || loader == null) return false;
        String vId = profile.lastVersionId.toLowerCase();
        String targetLoader = loader.toLowerCase();
        
        if (vId.contains(targetLoader)) return true;
        
        try {
            JMinecraftVersionList.Version v = Tools.getVersion(profile.lastVersionId);
            if (v != null) {
                if (v.id != null && v.id.toLowerCase().contains(targetLoader)) return true;
                if (v.inheritsFrom != null && v.inheritsFrom.toLowerCase().contains(targetLoader)) return true;
                if (v.mainClass != null && v.mainClass.toLowerCase().contains(targetLoader)) return true;
            }
        } catch (Exception e) {}
        return false;
    }
}

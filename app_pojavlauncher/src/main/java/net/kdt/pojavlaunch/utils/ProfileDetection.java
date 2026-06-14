package net.kdt.pojavlaunch.utils;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

public class ProfileDetection {
    public static String getMcVersion(MinecraftProfile profile) {
        if (profile == null || profile.lastVersionId == null) return "";
        try {
            JMinecraftVersionList.Version v = Tools.getVersionInfo(profile.lastVersionId);
            if (v != null) {
                if (v.inheritsFrom != null && !v.inheritsFrom.isEmpty()) {
                    return v.inheritsFrom;
                }
                return v.id; // Vanilla version
            }
        } catch (Exception e) {}

        // Fallback: extract MC version from lastVersionId
        String vId = profile.lastVersionId;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(1\\.[0-9]+(?:\\.[0-9]+)?)");
        java.util.regex.Matcher matcher = pattern.matcher(vId);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Try profile name as well
        if (profile.name != null) {
            matcher = pattern.matcher(profile.name);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    public static boolean hasLoader(MinecraftProfile profile, String loader) {
        if (profile == null || profile.lastVersionId == null || loader == null) return false;
        String vId = profile.lastVersionId.toLowerCase();
        String targetLoader = loader.toLowerCase();
        
        if (vId.contains(targetLoader)) return true;
        
        try {
            JMinecraftVersionList.Version v = Tools.getVersionInfo(profile.lastVersionId);
            if (v != null) {
                if (v.id != null && v.id.toLowerCase().contains(targetLoader)) return true;
                if (v.inheritsFrom != null && v.inheritsFrom.toLowerCase().contains(targetLoader)) return true;
                if (v.mainClass != null && v.mainClass.toLowerCase().contains(targetLoader)) return true;
            }
        } catch (Exception e) {}

        // Fallback: check profile name for loader name
        if (profile.name != null && profile.name.toLowerCase().contains(targetLoader)) return true;

        return false;
    }

    public static boolean isVersionCompatible(String pmcVer, String modMcVer) {
        if (pmcVer == null || modMcVer == null) return false;
        pmcVer = pmcVer.trim().toLowerCase();
        modMcVer = modMcVer.trim().toLowerCase();
        if (pmcVer.isEmpty() || modMcVer.isEmpty()) return false;
        
        // Exact match
        if (pmcVer.equals(modMcVer)) return true;
        
        // Contains match (either way)
        if (pmcVer.contains(modMcVer) || modMcVer.contains(pmcVer)) return true;
        
        // Handle wildcards or minor versions: e.g. "1.21.x" or "1.21"
        String normP = pmcVer.replaceAll("[x*]", "");
        String normM = modMcVer.replaceAll("[x*]", "");
        if (normP.endsWith(".")) normP = normP.substring(0, normP.length() - 1);
        if (normM.endsWith(".")) normM = normM.substring(0, normM.length() - 1);
        
        if (!normP.isEmpty() && !normM.isEmpty()) {
            if (normP.startsWith(normM) || normM.startsWith(normP)) return true;
        }
        
        return false;
    }
}

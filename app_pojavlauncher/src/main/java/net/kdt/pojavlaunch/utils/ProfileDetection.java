package net.kdt.pojavlaunch.utils;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileDetection {

    /** Regex to match a Minecraft version number like 1.21, 1.21.10, 1.20.6 */
    private static final Pattern MC_VERSION_PATTERN =
            Pattern.compile("(1\\.[0-9]+(?:\\.[0-9]+)?)");

    /** Loader-related keywords used to determine if a version is a loader version */
    private static final Pattern LOADER_PATTERN =
            Pattern.compile("(?i)(fabric|forge|neoforge|quilt|optifine|optix|liteloader)");

    /**
     * Extract the base Minecraft version from a profile.
     * Uses the version JSON inheritsFrom chain first, then falls back to
     * extracting from the versionId string.
     */
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
        String result = extractMcFromVersionId(profile.lastVersionId);
        if (!result.isEmpty()) return result;

        // Try profile name as well
        if (profile.name != null) {
            result = extractMcFromVersionId(profile.name);
            if (!result.isEmpty()) return result;
        }
        return "";
    }

    /**
     * Extract the base Minecraft version from a loader version ID string.
     * E.g. "fabric-loader-0.19.3-1.21.10" → "1.21.10"
     *      "forge-1.21.10-52.0.12" → "1.21.10"
     *      "1.21.10" → "1.21.10"
     */
    public static String extractMcFromVersionId(String versionId) {
        if (versionId == null || versionId.isEmpty()) return "";

        // Try to find the LAST occurrence of a version number (loaders append MC version at the end)
        Matcher matcher = MC_VERSION_PATTERN.matcher(versionId);
        String lastMatch = "";
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        return lastMatch;
    }

    /**
     * Check whether a profile uses the specified mod loader.
     * Checks multiple sources:
     * 1. lastVersionId string contains the loader name
     * 2. Version JSON id/inheritsFrom/mainClass contain the loader name
     * 3. Profile name contains the loader name
     * 4. The actual mods directory contains the loader's API mod (e.g. fabric-api)
     */
    public static boolean hasLoader(MinecraftProfile profile, String loader) {
        if (profile == null || profile.lastVersionId == null || loader == null) return false;
        String vId = profile.lastVersionId.toLowerCase();
        String targetLoader = loader.toLowerCase();

        // 1. Check lastVersionId
        if (vId.contains(targetLoader)) return true;

        // 2. Check version JSON chain
        try {
            JMinecraftVersionList.Version v = Tools.getVersionInfo(profile.lastVersionId);
            if (v != null) {
                if (v.id != null && v.id.toLowerCase().contains(targetLoader)) return true;
                if (v.inheritsFrom != null && v.inheritsFrom.toLowerCase().contains(targetLoader)) return true;
                if (v.mainClass != null && v.mainClass.toLowerCase().contains(targetLoader)) return true;
            }
        } catch (Exception e) {}

        // 3. Check profile name
        if (profile.name != null && profile.name.toLowerCase().contains(targetLoader)) return true;

        // 4. Check actual mods directory for known loader API files
        //    This catches cases where a loader was installed but the version ID doesn't contain its name
        try {
            File gameDir = net.kdt.pojavlaunch.Tools.getGameDirPath(profile);
            if (gameDir != null) {
                File modsDir = new File(gameDir, "mods");
                if (modsDir.exists() && modsDir.isDirectory()) {
                    File[] mods = modsDir.listFiles((dir, name) ->
                            name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".jar.disabled"));
                    if (mods != null) {
                        for (File mod : mods) {
                            String modName = mod.getName().toLowerCase();
                            // Check for loader-specific API mods
                            if (targetLoader.equals("fabric") && modName.contains("fabric-api")) return true;
                            if (targetLoader.equals("fabric") && modName.contains("fabric-loader")) return true;
                            if (targetLoader.equals("forge") && modName.contains("forge")) return true;
                            if (targetLoader.equals("neoforge") && modName.contains("neoforge")) return true;
                            if (targetLoader.equals("quilt") && modName.contains("quilt")) return true;
                            if (targetLoader.equals("liteloader") && modName.contains("liteloader")) return true;
                            if (targetLoader.equals("optifine") && (modName.contains("optifine") || modName.contains("optifabric"))) return true;
                        }
                    }
                }
            }
        } catch (Exception e) {}

        return false;
    }

    /**
     * Check if a profile's MC version is compatible with a mod's required MC version.
     * Supports exact match, prefix match, and wildcards.
     */
    public static boolean isVersionCompatible(String pmcVer, String modMcVer) {
        if (pmcVer == null || modMcVer == null) return false;
        pmcVer = pmcVer.trim().toLowerCase();
        modMcVer = modMcVer.trim().toLowerCase();
        if (pmcVer.isEmpty() || modMcVer.isEmpty()) return false;
        
        // Exact match
        if (pmcVer.equals(modMcVer)) return true;
        
        // Contains match (either way) - handles cases like "1.21" matching "1.21.10"
        if (pmcVer.contains(modMcVer) || modMcVer.contains(pmcVer)) return true;
        
        // Handle wildcards or minor versions: e.g. "1.21.x" or "1.21"
        String normP = pmcVer.replaceAll("[x*]", "");
        String normM = modMcVer.replaceAll("[x*]", "");
        if (normP.endsWith(".")) normP = normP.substring(0, normP.length() - 1);
        if (normM.endsWith(".")) normM = normM.substring(0, normM.length() - 1);
        
        if (!normP.isEmpty() && !normM.isEmpty()) {
            if (normP.startsWith(normM) || normM.startsWith(normP)) return true;
        }
        
        // Handle version ranges like ">=1.21" or "1.21-1.22"
        if (modMcVer.startsWith(">=") || modMcVer.startsWith("<=") || modMcVer.startsWith(">") || modMcVer.startsWith("<")) {
            return isVersionInRange(pmcVer, modMcVer);
        }
        if (modMcVer.contains("-") && !modMcVer.startsWith("-")) {
            String[] parts = modMcVer.split("-", 2);
            if (parts.length == 2) {
                return isVersionInRange(pmcVer, parts[0], parts[1]);
            }
        }
        
        return false;
    }

    private static boolean isVersionInRange(String version, String rangeExpr) {
        if (rangeExpr.startsWith(">=")) {
            return compareVersions(version, rangeExpr.substring(2)) >= 0;
        } else if (rangeExpr.startsWith("<=")) {
            return compareVersions(version, rangeExpr.substring(2)) <= 0;
        } else if (rangeExpr.startsWith(">")) {
            return compareVersions(version, rangeExpr.substring(1)) > 0;
        } else if (rangeExpr.startsWith("<")) {
            return compareVersions(version, rangeExpr.substring(1)) < 0;
        }
        return false;
    }

    private static boolean isVersionInRange(String version, String min, String max) {
        return compareVersions(version, min) >= 0 && compareVersions(version, max) <= 0;
    }

    /**
     * Compare two version strings numerically.
     * Returns negative if v1 < v2, positive if v1 > v2, 0 if equal.
     */
    private static int compareVersions(String v1, String v2) {
        String clean1 = v1.replaceAll("[^0-9.]", "").replaceAll("\\.$", "");
        String clean2 = v2.replaceAll("[^0-9.]", "").replaceAll("\\.$", "");
        String[] parts1 = clean1.isEmpty() ? new String[]{"0"} : clean1.split("\\.");
        String[] parts2 = clean2.isEmpty() ? new String[]{"0"} : clean2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int n1 = 0, n2 = 0;
            try { n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0; } catch (NumberFormatException ignored) {}
            try { n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0; } catch (NumberFormatException ignored) {}
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }
}

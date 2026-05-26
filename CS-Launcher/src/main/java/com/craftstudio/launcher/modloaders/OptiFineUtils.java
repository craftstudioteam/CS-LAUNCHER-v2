package com.craftstudio.launcher.modloaders;

import com.craftstudio.launcher.utils.DownloadUtils;

import java.util.List;

public class OptiFineUtils {

    public static OptiFineVersions downloadOptiFineVersions(boolean force) throws Exception {
        return DownloadUtils.downloadStringCached("https://optifine.net/downloads", "of_downloads_page", force, new OptiFineScraper());
    }

    public static class OptiFineVersions {
        public List<String> minecraftVersions;
        public List<List<OptiFineVersion>> optifineVersions;
    }
    public static class OptiFineVersion {
        public String minecraftVersion;
        public String versionName;
        public String downloadUrl;
    }
}

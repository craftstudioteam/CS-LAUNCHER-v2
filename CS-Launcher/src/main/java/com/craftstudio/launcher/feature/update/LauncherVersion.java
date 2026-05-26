package com.craftstudio.launcher.feature.update;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class LauncherVersion {
    @SerializedName("version_code")
    private final int versionCode;
    @SerializedName("version_name")
    private final String versionName;
    private final WhatsNew title;
    private final WhatsNew description;
    @SerializedName("published_at")
    private final String publishedAt;
    @SerializedName("file_size")
    private final FileSize fileSize;
    @SerializedName("pre_release")
    private final boolean isPreRelease;

    public LauncherVersion(int versionCode, String versionName, WhatsNew title, WhatsNew description, String publishedAt, FileSize fileSize, boolean isPreRelease) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.title = title;
        this.description = description;
        this.publishedAt = publishedAt;
        this.fileSize = fileSize;
        this.isPreRelease = isPreRelease;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public WhatsNew getTitle() {
        return title;
    }

    public WhatsNew getDescription() {
        return description;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public FileSize getFileSize() {
        return fileSize;
    }

    public boolean isPreRelease() {
        return isPreRelease;
    }

    @NonNull
    @Override
    public String toString() {
        return "Version{" +
                "versionCode=" + versionCode +
                ", versionName='" + versionName + '\'' +
                ", title=" + title +
                ", description=" + description +
                ", publishedAt='" + publishedAt + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }

    public static class WhatsNew {
        @SerializedName("en_us")
        private final String enUS;
        @SerializedName("zh_cn")
        private final String zhCN;
        @SerializedName("zh_tw")
        private final String zhTW;

        public WhatsNew(String enUS, String zhCN, String zhTW) {
            this.enUS = enUS;
            this.zhCN = zhCN;
            this.zhTW = zhTW;
        }

        public String getEnUS() {
            return enUS;
        }

        public String getZhCN() {
            return zhCN;
        }

        public String getZhTW() {
            return zhTW;
        }

        @NonNull
        @Override
        public String toString() {
            return "WhatsNew{" +
                    "enUS='" + enUS + '\'' +
                    ", zhCN='" + zhCN + '\'' +
                    ", zhTW='" + zhTW + '\'' +
                    '}';
        }
    }

    public static class FileSize {
        private final long all;
        private final long arm;
        private final long arm64;
        private final long x86;
        private final long x86_64;

        public FileSize(long all, long arm, long arm64, long x86, long x86_64) {
            this.all = all;
            this.arm = arm;
            this.arm64 = arm64;
            this.x86 = x86;
            this.x86_64 = x86_64;
        }

        public long getAll() {
            return all;
        }

        public long getArm() {
            return arm;
        }

        public long getArm64() {
            return arm64;
        }

        public long getX86() {
            return x86;
        }

        public long getX86_64() {
            return x86_64;
        }

        @NonNull
        @Override
        public String toString() {
            return "FileSize{" +
                    "all=" + all +
                    ", arm=" + arm +
                    ", arm64=" + arm64 +
                    ", x86=" + x86 +
                    ", x86_64=" + x86_64 +
                    '}';
        }
    }
}
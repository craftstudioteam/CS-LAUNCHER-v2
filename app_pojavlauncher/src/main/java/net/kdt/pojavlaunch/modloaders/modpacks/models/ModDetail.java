package net.kdt.pojavlaunch.modloaders.modpacks.models;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import java.util.Arrays;

public class ModDetail extends ModItem implements Parcelable {
    /* A cheap way to map from the front facing name to the underlying id */
    public String[] versionNames;
    public String [] mcVersionNames;
    public String[] versionUrls;
    /* SHA 1 hashes, null if a hash is unavailable */
    public String[] versionHashes;
    /* Per-version dependency project IDs */
    public String[][] versionDependencyIds;
    /* Per-version dependency types — "required" or "optional" */
    public String[][] versionDependencyTypes;
    /* Supported mod loaders for each version */
    public String[][] versionLoaders;
    /* Screenshot gallery URLs */
    public String[] screenshotUrls;

    public ModDetail(ModItem item, String[] versionNames, String[] mcVersionNames, String[] versionUrls, String[] hashes) {
        this(item, versionNames, mcVersionNames, versionUrls, hashes, null, null, null);
    }

    public ModDetail(ModItem item, String[] versionNames, String[] mcVersionNames, String[] versionUrls, String[] hashes,
                     String[][] depIds, String[][] depTypes) {
        this(item, versionNames, mcVersionNames, versionUrls, hashes, depIds, depTypes, null);
    }

    public ModDetail(ModItem item, String[] versionNames, String[] mcVersionNames, String[] versionUrls, String[] hashes,
                     String[][] depIds, String[][] depTypes, String[][] loaders) {
        super(item.apiSource, item.isModpack, item.id, item.title, item.description, item.imageUrl);
        this.isRestricted = item.isRestricted;
        this.websiteUrl = item.websiteUrl;
        this.versionNames = versionNames;
        this.mcVersionNames = mcVersionNames;
        this.versionUrls = versionUrls;
        this.versionHashes = hashes;
        this.versionDependencyIds = depIds;
        this.versionDependencyTypes = depTypes;
        this.versionLoaders = loaders;

        // Add the mc version to the version model
        for (int i=0; i<versionNames.length; i++){
            if (mcVersionNames[i] != null && !versionNames[i].contains(mcVersionNames[i]))
                versionNames[i] += " - " + mcVersionNames[i];
        }
    }

    public void setScreenshotUrls(String[] urls) {
        screenshotUrls = urls;
    }

    protected ModDetail(Parcel in) {
        super(in.readInt(), in.readByte() != 0, in.readString(), in.readString(), in.readString(), in.readString());
        isRestricted = in.readByte() != 0;
        websiteUrl = in.readString();
        author = in.readString();
        downloads = in.readString();

        versionNames = in.createStringArray();
        mcVersionNames = in.createStringArray();
        versionUrls = in.createStringArray();
        versionHashes = in.createStringArray();
        screenshotUrls = in.createStringArray();

        int depCount = in.readInt();
        if (depCount >= 0) {
            versionDependencyIds = new String[depCount][];
            for (int i = 0; i < depCount; i++) {
                versionDependencyIds[i] = in.createStringArray();
            }
        } else {
            versionDependencyIds = null;
        }

        int typeCount = in.readInt();
        if (typeCount >= 0) {
            versionDependencyTypes = new String[typeCount][];
            for (int i = 0; i < typeCount; i++) {
                versionDependencyTypes[i] = in.createStringArray();
            }
        } else {
            versionDependencyTypes = null;
        }

        int loadersCount = in.readInt();
        if (loadersCount >= 0) {
            versionLoaders = new String[loadersCount][];
            for (int i = 0; i < loadersCount; i++) {
                versionLoaders[i] = in.createStringArray();
            }
        } else {
            versionLoaders = null;
        }

        // Re-apply mc version suffix (same logic as main constructor)
        for (int i = 0; i < versionNames.length; i++) {
            if (mcVersionNames[i] != null && !versionNames[i].contains(mcVersionNames[i])) {
                versionNames[i] += " - " + mcVersionNames[i];
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ModDetail{" +
                "versionNames=" + Arrays.toString(versionNames) +
                ", mcVersionNames=" + Arrays.toString(mcVersionNames) +
                ", versionUrls=" + Arrays.toString(versionUrls) +
                ", id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", apiSource=" + apiSource +
                ", isModpack=" + isModpack +
                '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(apiSource);
        dest.writeByte((byte) (isModpack ? 1 : 0));
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(imageUrl);
        dest.writeByte((byte) (isRestricted ? 1 : 0));
        dest.writeString(websiteUrl);
        dest.writeString(author);
        dest.writeString(downloads);

        dest.writeStringArray(versionNames);
        dest.writeStringArray(mcVersionNames);
        dest.writeStringArray(versionUrls);
        dest.writeStringArray(versionHashes);
        dest.writeStringArray(screenshotUrls);

        dest.writeInt(versionDependencyIds != null ? versionDependencyIds.length : -1);
        if (versionDependencyIds != null) {
            for (String[] arr : versionDependencyIds) dest.writeStringArray(arr);
        }
        dest.writeInt(versionDependencyTypes != null ? versionDependencyTypes.length : -1);
        if (versionDependencyTypes != null) {
            for (String[] arr : versionDependencyTypes) dest.writeStringArray(arr);
        }
        dest.writeInt(versionLoaders != null ? versionLoaders.length : -1);
        if (versionLoaders != null) {
            for (String[] arr : versionLoaders) dest.writeStringArray(arr);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ModDetail> CREATOR = new Creator<ModDetail>() {
        @Override
        public ModDetail createFromParcel(Parcel in) {
            return new ModDetail(in);
        }

        @Override
        public ModDetail[] newArray(int size) {
            return new ModDetail[size];
        }
    };
}

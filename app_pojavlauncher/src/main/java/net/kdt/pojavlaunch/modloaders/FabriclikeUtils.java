package net.kdt.pojavlaunch.modloaders;

import com.google.gson.JsonSyntaxException;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import android.util.Log;

public class FabriclikeUtils {

    public static final FabriclikeUtils FABRIC_UTILS = new FabriclikeUtils("https://meta.fabricmc.net/v2", "fabric", "Fabric", "fabric");
    public static final FabriclikeUtils QUILT_UTILS = new FabriclikeUtils("https://meta.quiltmc.org/v3", "quilt", "Quilt", "quilt");

    private static final String TAG = "CSLauncher";
    private static final String LOADER_METADATA_URL = "%s/versions/loader/%s";
    private static final String LOADER_METADATA_ALL_URL = "%s/versions/loader";
    private static final String GAME_METADATA_URL = "%s/versions/game";

    private static final String JSON_DOWNLOAD_URL = "%s/versions/loader/%s/%s/profile/json";

    private final String mApiUrl;
    private final String mCachePrefix;
    private final String mName;
    private final String mIconName;

    private FabriclikeUtils(String mApiUrl, String cachePrefix, String mName, String iconName) {
        this.mApiUrl = mApiUrl;
        this.mCachePrefix = cachePrefix;
        this.mIconName = iconName;
        this.mName = mName;
    }

    public FabricVersion[] downloadGameVersions() throws IOException{
        try {
            return DownloadUtils.downloadStringCached(String.format(GAME_METADATA_URL, mApiUrl), mCachePrefix+"_game_versions",
                    FabriclikeUtils::deserializeRawVersions
            );
        }catch (DownloadUtils.ParseException ignored) {}
        return null;
    }

    public FabricVersion[] downloadLoaderVersions(String gameVersion) throws IOException{
        String urlEncodedGameVersion = URLEncoder.encode(gameVersion, "UTF-8");
        String gameSpecificUrl = String.format(LOADER_METADATA_URL, mApiUrl, urlEncodedGameVersion);
        Log.d(TAG, "Game Version = " + gameVersion);
        Log.d(TAG, "Fabric API URL = " + gameSpecificUrl);

        FabricVersion[] versions = null;
        try {
            versions = downloadLoaderVersionsFromUrl(gameSpecificUrl,
                    mCachePrefix+"_loader_versions."+urlEncodedGameVersion);
        } catch (IOException e) {
            Log.w(TAG, "Game-specific loader request failed, trying loader fallback", e);
        }

        if (versions == null || versions.length == 0) {
            String fallbackUrl = String.format(LOADER_METADATA_ALL_URL, mApiUrl);
            Log.d(TAG, "Fabric API URL = " + fallbackUrl);
            versions = downloadLoaderVersionsFromUrl(fallbackUrl, mCachePrefix+"_loader_versions.all");
        }

        Log.d(TAG, "Loader Count Returned = " + (versions == null ? 0 : versions.length));
        return versions;
    }

    private FabricVersion[] downloadLoaderVersionsFromUrl(String url, String cacheName) throws IOException {
        try {
            return DownloadUtils.downloadStringCached(url, cacheName,
                    (input)->{ try {
                        return deserializeLoaderVersions(input);
                    }catch (JSONException e) {
                        throw new DownloadUtils.ParseException(e);
                    }});
        }catch (DownloadUtils.ParseException e) {
            Log.w(TAG, "Failed to parse loader metadata from " + url, e);
        }
        return null;
    }

    public String createJsonDownloadUrl(String gameVersion, String loaderVersion) {
        try {
            gameVersion = URLEncoder.encode(gameVersion, "UTF-8");
            loaderVersion = URLEncoder.encode(loaderVersion, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return String.format(JSON_DOWNLOAD_URL, mApiUrl, gameVersion, loaderVersion);
    }

    public String getName() {
        return mName;
    }
    public String getIconName() {
        return mIconName;
    }

    private static FabricVersion[] deserializeLoaderVersions(String input) throws JSONException {
        JSONArray jsonArray = new JSONArray(input);
        FabricVersion[] fabricVersions = new FabricVersion[jsonArray.length()];
        for(int i = 0; i < jsonArray.length(); i++) {
            JSONObject entryObject = jsonArray.getJSONObject(i);
            JSONObject jsonObject = entryObject.has("loader") ? entryObject.getJSONObject("loader") : entryObject;
            FabricVersion fabricVersion = new FabricVersion();
            fabricVersion.version = jsonObject.getString("version");
            //Quilt has a skill issue and does not say which versions are stable or not
            if(jsonObject.has("stable")) {
                fabricVersion.stable = jsonObject.getBoolean("stable");
            } else {
                fabricVersion.stable = !fabricVersion.version.contains("beta");
            }
            fabricVersions[i] = fabricVersion;
        }
        return fabricVersions;
    }

    private static FabricVersion[] deserializeRawVersions(String jsonArrayIn) throws DownloadUtils.ParseException {
        try {
            return Tools.GLOBAL_GSON.fromJson(jsonArrayIn, FabricVersion[].class);
        }catch (JsonSyntaxException e) {
            e.printStackTrace();
            throw new DownloadUtils.ParseException(null);
        }
    }
}

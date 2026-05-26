package com.craftstudio.launcher.value;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;
import com.craftstudio.launcher.feature.accounts.AccountsManager;
import com.craftstudio.launcher.feature.log.Logging;
import com.craftstudio.launcher.utils.path.PathManager;
import com.craftstudio.launcher.utils.skin.SkinFileDownloader;
import com.craftstudio.launcher.utils.stringutils.StringUtilsKt;

import com.craftstudio.launcher.Tools;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Keep
public class MinecraftAccount {
    public String accessToken = "0"; // access token
    public String clientToken = "0"; // clientID: refresh and invalidate
    public String profileId = "00000000-0000-0000-0000-000000000000"; // profile UUID, for obtaining skin
    public String username = "Steve";
    public String msaRefreshToken = "0";
    public String xuid;
    public String otherBaseUrl;
    public String otherAccount;
    public String otherPassword;
    public String accountType;
    private final String uniqueUUID = UUID.randomUUID().toString().toLowerCase(Locale.ROOT);

    public void updateMicrosoftSkin() {
        updateSkin("https://sessionserver.mojang.com");
    }

    public void updateOtherSkin() {
        updateSkin(StringUtilsKt.removeSuffix(otherBaseUrl, "/") + "/sessionserver/");
    }

    private void updateSkin(String url) {
        File skinFile = new File(PathManager.DIR_USER_SKIN, uniqueUUID + ".png");
        if (skinFile.exists()) FileUtils.deleteQuietly(skinFile); //清除一次皮肤文件
        try {
            new SkinFileDownloader().yggdrasil(url, skinFile, profileId);
            Logging.i("SkinLoader", "Update skin success");
        } catch (Exception e) {
            Logging.i("SkinLoader", "Could not update skin\n" + Tools.printToString(e));
        }
    }

    public void save() throws IOException {
        Tools.write(PathManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID, Tools.GLOBAL_GSON.toJson(this));
    }
    
    public static MinecraftAccount parse(String content) throws JsonSyntaxException {
        return Tools.GLOBAL_GSON.fromJson(content, MinecraftAccount.class);
    }

    public static MinecraftAccount loadFromProfileID(String profileID) {
        for (MinecraftAccount account : AccountsManager.INSTANCE.getAllAccounts()) {
            if (Objects.equals(account.profileId, profileID)) return account;
        }
        return null;
    }

    public static MinecraftAccount loadFromUniqueUUID(String uniqueUUID) {
        if(!accountExists(uniqueUUID)) return null;
        try {
            MinecraftAccount acc = parse(Tools.read(PathManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID));
            if (acc.accessToken == null) {
                acc.accessToken = "0";
            }
            if (acc.clientToken == null) {
                acc.clientToken = "0";
            }
            if (acc.profileId == null) {
                acc.profileId = "00000000-0000-0000-0000-000000000000";
            }
            if (acc.username == null) {
                acc.username = "0";
            }
            if (acc.msaRefreshToken == null) {
                acc.msaRefreshToken = "0";
            }
            return acc;
        } catch(IOException | JsonSyntaxException e) {
            Logging.e(MinecraftAccount.class.getName(), "Caught an exception while loading the profile",e);
            return null;
        }
    }

    private static boolean accountExists(String uniqueUUID) {
        return !uniqueUUID.isEmpty() && new File(PathManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID).exists();
    }

    public String getUniqueUUID() {
        return this.uniqueUUID;
    }

    @NonNull
    @Override
    public String toString() {
        return "MinecraftAccount{" +
                "username='" + username + '\'' +
                ", accountType=" + accountType +
                '}';
    }
}

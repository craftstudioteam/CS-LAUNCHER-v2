package com.craftstudio.launcher.tasks;

import static com.craftstudio.launcher.utils.DownloadUtils.downloadString;

import androidx.annotation.Nullable;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.craftstudio.launcher.feature.log.Logging;
import com.craftstudio.launcher.task.Task;
import com.craftstudio.launcher.utils.path.PathManager;
import com.craftstudio.launcher.utils.ZHTools;
import com.craftstudio.launcher.utils.path.UrlManager;

import com.craftstudio.launcher.JMinecraftVersionList;
import com.craftstudio.launcher.Tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

/** Class getting the version list, and that's all really */
public class AsyncVersionList {

    public void getVersionList(@Nullable VersionDoneListener listener, boolean secondPass){
        Task.runTask(() -> {
            File versionFile = new File(PathManager.FILE_VERSION_LIST);
            JMinecraftVersionList versionList = null;
            try {
                if (!versionFile.exists() || (ZHTools.getCurrentTimeMillis() > versionFile.lastModified() + 86400000)) {
                    versionList = downloadVersionList(UrlManager.URL_MINECRAFT_VERSION_REPOS);
                }
            } catch (Exception e) {
                Logging.e("AsyncVersionList", "Refreshing version list failed :" + e);
                Logging.e("GetVersionList", Tools.printToString(e));
            }

            // Fallback when no network or not needed
            if (versionList == null) {
                try {
                    versionList = Tools.GLOBAL_GSON.fromJson(new JsonReader(new FileReader(versionFile)), JMinecraftVersionList.class);
                } catch (FileNotFoundException e) {
                    Logging.e("File Not Found", Tools.printToString(e));
                } catch (JsonIOException | JsonSyntaxException e) {
                    Logging.e("AsyncVersionList", Tools.printToString(e));
                    versionFile.delete();
                    if (!secondPass) getVersionList(listener, true);
                }
            }

            if (listener != null) listener.onVersionDone(versionList);
            return null;
        }).execute();
    }


    @SuppressWarnings("SameParameterValue")
    private JMinecraftVersionList downloadVersionList(String mirror) {
        JMinecraftVersionList list = null;
        try {
            Logging.i("ExtVL", "Syncing to external: " + mirror);
            String jsonString = downloadString(mirror);
            list = Tools.GLOBAL_GSON.fromJson(jsonString, JMinecraftVersionList.class);
            Logging.i("ExtVL", "Downloaded the version list, len=" + list.versions.length);

            // Then save the version list
            //TODO make it not save at times ?
            FileOutputStream fos = new FileOutputStream(PathManager.FILE_VERSION_LIST);
            fos.write(jsonString.getBytes());
            fos.close();
        } catch (IOException e) {
            Logging.e("AsyncVersionList", Tools.printToString(e));
        }
        return list;
    }

    /** Basic listener, acting as a callback */
    public interface VersionDoneListener{
        void onVersionDone(JMinecraftVersionList versions);
    }

}

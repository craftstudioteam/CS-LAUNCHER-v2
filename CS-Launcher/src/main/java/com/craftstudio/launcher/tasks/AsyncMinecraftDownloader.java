package com.craftstudio.launcher.tasks;

import com.craftstudio.launcher.event.sticky.MinecraftVersionValueEvent;

import com.craftstudio.launcher.JMinecraftVersionList;

import org.greenrobot.eventbus.EventBus;

public class AsyncMinecraftDownloader {
    public static JMinecraftVersionList.Version getListedVersion(String versionString) {
        JMinecraftVersionList versionList = getJMinecraftVersionList();
        if (versionList == null || versionList.versions == null) return null; // can't have listed versions if there's no list
        for (JMinecraftVersionList.Version version : versionList.versions) {
            if (version.id.equals(versionString)) return version;
        }
        return null;
    }

    private static JMinecraftVersionList getJMinecraftVersionList() {
        MinecraftVersionValueEvent event = EventBus.getDefault().getStickyEvent(MinecraftVersionValueEvent.class);

        if (event != null) return event.getList();
        else return null;
    }

    public interface DoneListener{
        void onDownloadDone();
        void onDownloadFailed(Throwable throwable);
    }
}

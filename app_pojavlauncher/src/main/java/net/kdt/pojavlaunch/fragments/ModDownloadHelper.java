package net.kdt.pojavlaunch.fragments;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModDownloadHelper {

    public static File getDestinationDir(File baseDir, String contentType) {
        switch (contentType) {
            case "mod":
                return new File(baseDir, "mods");
            case "resourcepack":
                return new File(baseDir, "resourcepacks");
            case "shader":
                return new File(baseDir, "shaderpacks");
            case "world":
                return new File(baseDir, "saves");
            default:
                return new File(baseDir, "downloads");
        }
    }

    public static File getDestinationDir(Context context, String contentType, String profileKey) {
        File mcDir = null;
        if (profileKey != null && !profileKey.isEmpty()) {
            try {
                net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.load();
                net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile profile = 
                        net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.mainProfileJson.profiles.get(profileKey);
                if (profile != null) mcDir = net.kdt.pojavlaunch.Tools.getGameDirPath(profile);
            } catch (Exception ignored) {}
        }
        if (mcDir == null) mcDir = new File(Tools.DIR_GAME_NEW);

        switch (contentType) {
            case "mod":
                return new File(mcDir, "mods");
            case "resourcepack":
                return new File(mcDir, "resourcepacks");
            case "shader":
                return new File(mcDir, "shaderpacks");
            case "world":
                return new File(mcDir, "saves");
            default:
                return new File(mcDir, "downloads");
        }
    }

    public static String getFileExtension(String contentType) {
        switch (contentType) {
            case "mod":
                return ".jar";
            case "resourcepack":
            case "shader":
            case "world":
                return ".zip";
            default:
                return ".zip";
        }
    }

    public static void downloadAndExtract(Context context, String name, String url, String contentType, String profileKey) {
        if (context == null || url == null || url.isEmpty()) return;

        File destDir = getDestinationDir(context, contentType, profileKey);
        if (!destDir.exists()) destDir.mkdirs();

        String filename = sanitizeName(name) + getFileExtension(contentType);
        File destFile = new File(destDir, filename);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(name);
        request.setDescription("Downloading " + contentType + "...");
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationUri(Uri.fromFile(destFile));
        request.allowScanningByMediaScanner();

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;
        long downloadId = dm.enqueue(request);

        Toast.makeText(context, name + " download started!", Toast.LENGTH_SHORT).show();

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != downloadId) return;

                if (contentType.equals("world")) {
                    new Thread(() -> {
                        boolean ok = extractZip(destFile, destDir);
                        if (ok) destFile.delete();
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(ctx, name + (ok ? " ready! \u2713" : " extract failed"),
                                        Toast.LENGTH_LONG).show());
                    }).start();
                } else {
                    Toast.makeText(ctx, name + " installed! \u2713",
                            Toast.LENGTH_LONG).show();
                }

                ctx.unregisterReceiver(this);
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
    }

    private static boolean extractZip(File zipFile, File destDir) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

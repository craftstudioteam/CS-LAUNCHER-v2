package net.kdt.pojavlaunch.fragments;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.widget.Toast;

import net.kdt.pojavlaunch.Tools;

import java.io.File;

public class ModDownloadHelper {

    public static File getDestinationDir(Context context, String contentType) {
        File mcDir = new File(Tools.DIR_GAME_NEW);

        switch (contentType) {
            case "mod":
                return new File(mcDir, "mods");
            case "modpack":
                return new File(mcDir, "modpacks");
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
            case "modpack":
                return ".mrpack";
            case "resourcepack":
            case "shader":
            case "world":
                return ".zip";
            default:
                return ".zip";
        }
    }

    public static void download(Context context, String name, String url, String contentType) {
        if (context == null || url == null || url.isEmpty()) return;

        File destDir = getDestinationDir(context, contentType);
        if (!destDir.exists()) destDir.mkdirs();

        String filename = name.replaceAll("[^a-zA-Z0-9._-]", "_")
                + getFileExtension(contentType);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(name);
        request.setDescription("Downloading " + contentType + "...");
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationUri(Uri.fromFile(new File(destDir, filename)));
        request.allowScanningByMediaScanner();

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;
        long downloadId = dm.enqueue(request);

        Toast.makeText(context,
                name + " download started!",
                Toast.LENGTH_SHORT).show();

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    Toast.makeText(ctx,
                            name + " installed! \u2713",
                            Toast.LENGTH_LONG).show();
                    ctx.unregisterReceiver(this);
                }
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
}

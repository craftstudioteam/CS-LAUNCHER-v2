package net.kdt.pojavlaunch.modloaders.modpacks.imagecache;

import android.util.Log;

import net.kdt.pojavlaunch.PojavApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This image is intended to keep the mod icon cache tidy (aka under 100 megabytes).
 * Runs non-blocking — never blocks image loading threads.
 */
public class IconCacheJanitor implements Runnable{
    public static final long CACHE_SIZE_LIMIT = 104857600; // The cache size limit, 100 megabytes
    public static final long CACHE_BRINGDOWN = 52428800; // The size to which the cache should be brought
    // in case of an overflow, 50 mb
    private static final AtomicBoolean sJanitorRunning = new AtomicBoolean(false);
    private static volatile boolean sJanitorRan = false;
    private IconCacheJanitor() {
        // don't allow others to create this
    }
    @Override
    public void run() {
        File modIconCachePath = ModIconCache.getImageCachePath();
        if(!modIconCachePath.isDirectory() || !modIconCachePath.canRead()) return;
        File[] modIconFiles = modIconCachePath.listFiles();
        if(modIconFiles == null) return;
        ArrayList<File> writableModIconFiles = new ArrayList<>(modIconFiles.length);
        long directoryFileSize = 0;
        for(File modIconFile : modIconFiles) {
            if(!modIconFile.isFile() || !modIconFile.canRead()) continue;
            directoryFileSize += modIconFile.length();
            if(!modIconFile.canWrite()) continue;
            writableModIconFiles.add(modIconFile);
        }
        if(directoryFileSize < CACHE_SIZE_LIMIT)  {
            Log.i("IconCacheJanitor", "Skipping cleanup because there's not enough to clean up");
            sJanitorRan = true;
            sJanitorRunning.set(false);
            return;
        }
        Arrays.sort(modIconFiles,
                (x,y)-> Long.compare(y.lastModified(), x.lastModified())
        );
        int filesCleanedUp = 0;
        for(File modFile : writableModIconFiles) {
            if(directoryFileSize < CACHE_BRINGDOWN) break;
            long modFileSize = modFile.length();
            if(modFile.delete()) {
                directoryFileSize -= modFileSize;
                filesCleanedUp++;
            }
        }
        Log.i("IconCacheJanitor", "Cleaned up "+filesCleanedUp+ " files");
        sJanitorRan = true;
        sJanitorRunning.set(false);
    }

    /**
     * Runs the janitor task in the background. Never blocks callers.
     * Safe to call multiple times — only one janitor run will execute.
     */
    public static void runJanitor() {
        if (sJanitorRan) return;
        if (sJanitorRunning.compareAndSet(false, true)) {
            PojavApplication.sExecutorService.submit(new IconCacheJanitor());
        }
    }

    /**
     * Non-blocking check — returns true once a janitor cycle has completed.
     * Image loading threads should NOT wait for the janitor.
     */
    public static boolean hasJanitorRun() {
        return sJanitorRan;
    }
}

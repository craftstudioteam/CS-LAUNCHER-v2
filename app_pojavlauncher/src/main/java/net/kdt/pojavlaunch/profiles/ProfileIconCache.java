package net.kdt.pojavlaunch.profiles;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import net.kdt.pojavlaunch.R;

import java.util.concurrent.ConcurrentHashMap;

public class ProfileIconCache {
    // Data header format: data:<mime>;<encoding>,<data>
    private static final String DATA_HEADER = "data:";
    private static final String FALLBACK_ICON_NAME = "default";

    // Bounded LRU cache for decoded data icons — prevents unbounded heap growth.
    // 32 entries is generous: most users have <10 profiles.
    private static final int MAX_ICON_CACHE_SIZE = 32;
    private static final LruCache<String, Drawable> sIconCache = new LruCache<String, Drawable>(MAX_ICON_CACHE_SIZE) {
        @Override
        protected void entryRemoved(boolean evicted, String key, Drawable oldValue, Drawable newValue) {
            recycleBitmapDrawable(oldValue);
        }
    };
    // Static icons (built-in resource drawables) are lightweight references; ConcurrentHashMap avoids
    // ConcurrentModificationException when accessed from multiple threads.
    private static final ConcurrentHashMap<String, Drawable> sStaticIconCache = new ConcurrentHashMap<>();

    /**
     * Fetch an icon from the cache, or load it if it's not cached.
     * @param resources the Resources object, used for creating drawables
     * @param key the profile key
     * @param icon the profile icon data (stored in the icon field of MinecraftProfile)
     * @return an icon drawable
     */
    public static @NonNull Drawable fetchIcon(Resources resources, @NonNull String key, @Nullable String icon) {
        Drawable cachedIcon = sIconCache.get(key);
        if(cachedIcon != null) return cachedIcon;
        if(icon != null && icon.startsWith(DATA_HEADER)) return fetchDataIcon(resources, key, icon);
        else return fetchStaticIcon(resources, key, icon);
    }

    /**
     * Drop an icon from the icon cache. When dropped, its Drawable will be re-read from the
     * data string (or re-fetched from the static cache). The underlying Bitmap is recycled
     * to free native memory promptly.
     * @param key the profile key
     */
    public static void dropIcon(@NonNull String key) {
        Drawable removed = sIconCache.remove(key);
        recycleBitmapDrawable(removed);
    }

    /** Recycle the backing Bitmap of a BitmapDrawable to free native memory. */
    private static void recycleBitmapDrawable(@Nullable Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    private static Drawable fetchDataIcon(Resources resources, String key, @NonNull String icon) {
        Drawable dataIcon = readDataIcon(resources, icon);
        if(dataIcon == null) {
            Log.w("ProfileIconCache", "fetchDataIcon decode failed for " + key + " (icon length=" + icon.length() + ")");
            dataIcon = fetchFallbackIcon(resources);
        }
        sIconCache.put(key, dataIcon);
        return dataIcon;
    }

    private static Drawable fetchStaticIcon(Resources resources, String key, @Nullable String icon) {
        if (icon == null) {
            Drawable fallback = fetchFallbackIcon(resources);
            sIconCache.put(key, fallback);
            return fallback;
        }

        Drawable staticIcon = sStaticIconCache.get(icon);
        if(staticIcon == null) {
            staticIcon = getStaticIcon(resources, icon);
            if(staticIcon == null) staticIcon = fetchFallbackIcon(resources);
            Drawable existing = sStaticIconCache.putIfAbsent(icon, staticIcon);
            if (existing != null) staticIcon = existing; // Another thread won the race, reuse.
        }
        sIconCache.put(key, staticIcon);
        return staticIcon;
    }

    private static @NonNull Drawable fetchFallbackIcon(Resources resources) {
        Drawable fallbackIcon = sStaticIconCache.get(FALLBACK_ICON_NAME);
        if(fallbackIcon == null) {
            fallbackIcon = androidx.core.util.ObjectsCompat.requireNonNull(getStaticIcon(resources, FALLBACK_ICON_NAME));
            Drawable existing = sStaticIconCache.putIfAbsent(FALLBACK_ICON_NAME, fallbackIcon);
            if (existing != null) fallbackIcon = existing;
        }
        return fallbackIcon;
    }

    private static Drawable getStaticIcon(Resources resources, @NonNull String icon) {
        int staticIconResource = getStaticIconResource(icon);
        if(staticIconResource == -1) return null;
        return ResourcesCompat.getDrawable(resources, staticIconResource, null);
    }

    private static int getStaticIconResource(String icon) {
        switch (icon) {
            case "default": return R.drawable.ic_pojav_full;
            case "fabric": return R.drawable.ic_fabric;
            case "quilt": return R.drawable.ic_quilt;
            default: return -1;
        }
    }

    private static Drawable readDataIcon(Resources resources, String icon) {
        byte[] iconData = extractIconData(icon);
        if(iconData == null) {
            Log.w("ProfileIconCache", "readDataIcon: extractIconData returned null (icon prefix=" +
                    (icon.length() > 50 ? icon.substring(0, 50) : icon) + ")");
            return null;
        }

        // Decode bounds first to compute inSampleSize for memory-efficient loading
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(iconData, 0, iconData.length, opts);

        int targetSize = 128; // Profile icons displayed at ~48dp; 128px is more than sufficient
        opts.inSampleSize = computeInSampleSize(opts, targetSize, targetSize);
        opts.inJustDecodeBounds = false;
        opts.inMutable = false; // Immutable bitmaps are more memory-efficient on some platforms
        opts.inPreferredConfig = Bitmap.Config.RGB_565; // No alpha needed for profile icons

        Bitmap iconBitmap = BitmapFactory.decodeByteArray(iconData, 0, iconData.length, opts);
        if(iconBitmap == null) {
            Log.w("ProfileIconCache", "readDataIcon: BitmapFactory.decodeByteArray returned null (bytes=" + iconData.length + ", inSampleSize=" + opts.inSampleSize + ")");
            return null;
        }
        return new BitmapDrawable(resources, iconBitmap);
    }

    /**
     * Compute a power-of-two inSampleSize that scales the image down to at most target dimensions.
     */
    private static int computeInSampleSize(BitmapFactory.Options options, int targetWidth, int targetHeight) {
        int rawWidth = options.outWidth;
        int rawHeight = options.outHeight;
        int inSampleSize = 1;
        if (rawHeight > targetHeight || rawWidth > targetWidth) {
            int halfHeight = rawHeight / 2;
            int halfWidth = rawWidth / 2;
            while ((halfHeight / inSampleSize) >= targetHeight
                    && (halfWidth / inSampleSize) >= targetWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static byte[] extractIconData(String inputString) {
        int firstSemicolon = inputString.indexOf(';');
        int commaAfterSemicolon = inputString.indexOf(',');
        if(firstSemicolon == -1 || commaAfterSemicolon == -1) return null;
        String dataEncoding = inputString.substring(firstSemicolon+1, commaAfterSemicolon);
        if(!dataEncoding.equals("base64")) return null;
        return Base64.decode(inputString.substring(commaAfterSemicolon+1), 0);
    }
}

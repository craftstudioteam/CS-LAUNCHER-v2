package net.kdt.pojavlaunch.customcontrols.mouse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CursorManager {
    private static final String TAG = "CursorManager";

    /**
     * Apply a glow effect to a bitmap.
     */
    public static Bitmap applyGlow(Bitmap src, int glowRadius, int glowColor) {
        if (glowRadius <= 0) return src;

        Bitmap out = Bitmap.createBitmap(src.getWidth() + glowRadius * 2, src.getHeight() + glowRadius * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(glowColor);
        // Outer blur for the glow
        paint.setMaskFilter(new BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.OUTER));

        // Draw the alpha mask of the original bitmap with the blur paint
        Bitmap alpha = src.extractAlpha();
        canvas.drawBitmap(alpha, glowRadius, glowRadius, paint);
        
        // Draw the original bitmap on top
        canvas.drawBitmap(src, glowRadius, glowRadius, null);
        
        return out;
    }

    /**
     * Load a cursor drawable from a path.
     */
    public static Drawable loadCursorDrawable(Context context, String path) {
        if (path == null) return null;
        File file = new File(path);
        if (!file.exists()) return null;

        try {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap != null) {
                return new BitmapDrawable(context.getResources(), bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load cursor from " + path, e);
        }
        return null;
    }

    /**
     * Get all custom cursors in the cursors directory.
     */
    public static List<File> getCustomCursors() {
        List<File> cursors = new ArrayList<>();
        File dir = new File(Tools.DIR_CURSORS);
        if (!dir.exists()) {
            dir.mkdirs();
            return cursors;
        }

        File[] files = dir.listFiles((f, name) -> name.endsWith(".png") || name.endsWith(".webp"));
        if (files != null) {
            for (File f : files) {
                cursors.add(f);
            }
        }
        return cursors;
    }

    /**
     * Save a bitmap as a custom cursor.
     */
    public static boolean saveCursor(Bitmap bitmap, String name) {
        File dir = new File(Tools.DIR_CURSORS);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, name + ".png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to save cursor " + name, e);
            return false;
        }
    }
}

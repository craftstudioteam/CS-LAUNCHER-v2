package net.kdt.pojavlaunch.customcontrols.mouse;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.os.Build;
import android.util.Log;
import android.view.PointerIcon;
import android.view.View;
import net.kdt.pojavlaunch.MainActivity;
import net.kdt.pojavlaunch.MinecraftGLSurface;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import org.lwjgl.glfw.CallbackBridge;

import java.io.File;

public class CustomCursorRenderer {
    private static final String TAG = "CustomCursorRenderer";
    
    private static Movie mMovie;
    private static Bitmap mGifBitmap;
    private static Canvas mGifCanvas;
    private static Bitmap mStaticBitmap;
    private static PointerIcon mCachedPointerIcon;
    private static String mLastLoadedPath = null;
    private static int mLastGlowRadius = -1;
    private static int mLastGlowColor = 0;
    private static int mLastHotspotX = -1;
    private static int mLastHotspotY = -1;
    
    private static long mAnimationStartTime = 0;
    private static boolean mIsRunning = false;
    
    private static final Runnable mAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsRunning) return;
            
            updateCursorFrame();
            
            // Render at ~60fps (16ms per frame)
            Tools.MAIN_HANDLER.postDelayed(this, 16);
        }
    };
    
    public static void startAnimation() {
        if (!LauncherPreferences.PREF_CUSTOM_CURSOR_ENABLED) return;
        String path = LauncherPreferences.PREF_CUSTOM_CURSOR_PATH;
        if (path == null || !path.toLowerCase().endsWith(".gif")) return;
        
        if (mIsRunning) return;
        mIsRunning = true;
        mAnimationStartTime = System.currentTimeMillis();
        Tools.MAIN_HANDLER.post(mAnimationRunnable);
    }
    
    public static void stopAnimation() {
        mIsRunning = false;
        Tools.MAIN_HANDLER.removeCallbacks(mAnimationRunnable);
    }
    
    public static void updateCursorFrame() {
        MinecraftGLSurface glSurface = CallbackBridge.getMinecraftGLSurface();
        if (glSurface == null) return;
        
        View targetView = glSurface.getSurfaceView();
        if (targetView == null) targetView = glSurface;
        
        PointerIcon icon = getActivePointerIcon();
        if (icon != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                if (targetView.getPointerIcon() != icon) {
                    targetView.setPointerIcon(icon);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to set pointer icon on view", e);
            }
        }
        
        // Invalidate touchpad if visible to force redraw for animated GIFs
        Touchpad touchpad = MainActivity.touchpad;
        if (touchpad != null && touchpad.getVisibility() == View.VISIBLE) {
            touchpad.postInvalidate();
        }
    }
    
    public static synchronized void reset() {
        stopAnimation();
        mMovie = null;
        mGifBitmap = null;
        mGifCanvas = null;
        mStaticBitmap = null;
        mCachedPointerIcon = null;
        mLastLoadedPath = null;
        mLastGlowRadius = -1;
        mLastGlowColor = 0;
        mLastHotspotX = -1;
        mLastHotspotY = -1;
    }
    
    public static synchronized PointerIcon getActivePointerIcon() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return null;
        }
        
        if (!LauncherPreferences.PREF_CUSTOM_CURSOR_ENABLED) {
            return null;
        }
        
        String path = LauncherPreferences.PREF_CUSTOM_CURSOR_PATH;
        if (path == null) {
            return null;
        }
        
        int glowRadius = LauncherPreferences.PREF_CUSTOM_CURSOR_GLOW_RADIUS;
        int glowColor = LauncherPreferences.PREF_CUSTOM_CURSOR_GLOW_COLOR;
        int hotspotX = LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_hotspot_x", 0);
        int hotspotY = LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_hotspot_y", 0);
        
        boolean pathChanged = !path.equals(mLastLoadedPath);
        boolean glowChanged = glowRadius != mLastGlowRadius || glowColor != mLastGlowColor;
        boolean hotspotChanged = hotspotX != mLastHotspotX || hotspotY != mLastHotspotY;
        
        if (pathChanged || glowChanged || hotspotChanged) {
            // Reset and reload custom cursor
            reset();
            mLastLoadedPath = path;
            mLastGlowRadius = glowRadius;
            mLastGlowColor = glowColor;
            mLastHotspotX = hotspotX;
            mLastHotspotY = hotspotY;
            
            File file = new File(path);
            if (!file.exists()) {
                Log.e(TAG, "Cursor file does not exist: " + path);
                return null;
            }
            
            try {
                if (path.toLowerCase().endsWith(".gif")) {
                    mMovie = Movie.decodeFile(path);
                    if (mMovie != null) {
                        int w = mMovie.width();
                        int h = mMovie.height();
                        if (w <= 0) w = 32;
                        if (h <= 0) h = 32;
                        
                        mGifBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                        mGifCanvas = new Canvas(mGifBitmap);
                        
                        startAnimation();
                    } else {
                        Log.e(TAG, "Failed to decode GIF movie from: " + path);
                    }
                } else {
                    // Static image
                    Bitmap src = BitmapFactory.decodeFile(path);
                    if (src != null) {
                        Bitmap bitmapToUse;
                        if (glowRadius > 0) {
                            bitmapToUse = CursorManager.applyGlow(src, glowRadius, glowColor);
                        } else {
                            bitmapToUse = src;
                        }
                        mStaticBitmap = bitmapToUse;
                        
                        int hX = Math.max(0, Math.min(mStaticBitmap.getWidth() - 1, hotspotX));
                        int hY = Math.max(0, Math.min(mStaticBitmap.getHeight() - 1, hotspotY));
                        
                        mCachedPointerIcon = PointerIcon.create(mStaticBitmap, hX, hY);
                    } else {
                        Log.e(TAG, "Failed to decode static bitmap from: " + path);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading custom cursor", e);
            }
        }
        
        if (mMovie != null && mGifBitmap != null) {
            int duration = mMovie.duration();
            if (duration <= 0) duration = 1000;
            long now = System.currentTimeMillis();
            int relTime = (int) ((now - mAnimationStartTime) % duration);
            
            mMovie.setTime(relTime);
            mGifBitmap.eraseColor(Color.TRANSPARENT);
            mMovie.draw(mGifCanvas, 0, 0);
            
            Bitmap frame = mGifBitmap;
            if (glowRadius > 0) {
                frame = CursorManager.applyGlow(mGifBitmap, glowRadius, glowColor);
            }
            
            int hX = Math.max(0, Math.min(frame.getWidth() - 1, hotspotX));
            int hY = Math.max(0, Math.min(frame.getHeight() - 1, hotspotY));
            
            return PointerIcon.create(frame, hX, hY);
        } else {
            return mCachedPointerIcon;
        }
    }
    
    public static synchronized Bitmap getCurrentFrameBitmap() {
        if (!LauncherPreferences.PREF_CUSTOM_CURSOR_ENABLED) {
            return null;
        }
        
        String path = LauncherPreferences.PREF_CUSTOM_CURSOR_PATH;
        if (path == null) {
            return null;
        }
        
        // Ensure initialized
        getActivePointerIcon();
        
        if (mMovie != null && mGifBitmap != null) {
            int glowRadius = LauncherPreferences.PREF_CUSTOM_CURSOR_GLOW_RADIUS;
            int glowColor = LauncherPreferences.PREF_CUSTOM_CURSOR_GLOW_COLOR;
            if (glowRadius > 0) {
                return CursorManager.applyGlow(mGifBitmap, glowRadius, glowColor);
            }
            return mGifBitmap;
        }
        
        return mStaticBitmap;
    }
}

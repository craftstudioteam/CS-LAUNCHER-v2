package net.kdt.pojavlaunch; // Adjust package name as necessary

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import android.graphics.Bitmap;
import android.util.Log;

/**
 * CustomCursorEngine
 * 
 * Handles static and animated cursor injection directly into the GLFW window surface
 * to bypass Android View clipping limitations on Mali/Adreno graphics drivers.
 */
public class CustomCursorEngine {
    private static final String TAG = "CustomCursorEngine";
    private static CustomCursorEngine instance;
    private long windowHandle;
    
    // Engine components for Animated Cursors
    private ScheduledExecutorService scheduler;
    private final List<Long> cursorFrames;
    private int currentFrameIndex;
    private volatile boolean isAnimated = false;
    private final AtomicInteger animationDelayMs = new AtomicInteger(100);

    private CustomCursorEngine() {
        cursorFrames = new ArrayList<>();
    }

    public static CustomCursorEngine getInstance() {
        if (instance == null) {
            instance = new CustomCursorEngine();
        }
        return instance;
    }

    /**
     * Bind the engine to the current GLFW window handle once it's created.
     */
    public void init(long windowHandle) {
        this.windowHandle = windowHandle;
        enforceCursorVisibility();
    }
    
    private void enforceCursorVisibility() {
        if (this.windowHandle != 0) {
            // Enforce normal cursor mode so Mali/Adreno drivers don't clip the viewport bounds
            try {
                GLFW.glfwSetInputMode(this.windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            } catch (Exception e) {
                Log.e(TAG, "Failed to enforce GLFW_CURSOR_NORMAL mode", e);
            }
        }
    }

    /**
     * Mounts a single static custom cursor.
     */
    public void setStaticCursor(Bitmap bitmap, int xHot, int yHot) {
        stopAnimation();
        long cursor = createGlfwCursor(bitmap, xHot, yHot);
        
        if (cursor != 0) {
            applyCursor(cursor);
        } else {
            fallbackToDefault();
        }
    }

    /**
     * Mounts a multi-frame sequence for an animated cursor.
     * Expects a list of pre-sliced Android Bitmaps.
     */
    public void setAnimatedCursor(List<Bitmap> frames, int xHot, int yHot, int delayMs) {
        stopAnimation();
        
        cursorFrames.clear();
        for (Bitmap frame : frames) {
            long cursor = createGlfwCursor(frame, xHot, yHot);
            if (cursor != 0) {
                cursorFrames.add(cursor);
            }
        }

        if (cursorFrames.isEmpty()) {
            Log.w(TAG, "No valid frames decoded for animated cursor, triggering fallback");
            fallbackToDefault();
            return;
        }

        animationDelayMs.set(delayMs);
        currentFrameIndex = 0;
        isAnimated = true;
        
        startAnimationLoop();
    }

    private void startAnimationLoop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (isAnimated && !cursorFrames.isEmpty()) {
                currentFrameIndex = (currentFrameIndex + 1) % cursorFrames.size();
                long currentCursor = cursorFrames.get(currentFrameIndex);
                applyCursor(currentCursor);
            }
        }, 0, animationDelayMs.get(), TimeUnit.MILLISECONDS);
    }

    private void stopAnimation() {
        isAnimated = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        // Cleanup native cursor memory for all old frames
        for (long cursor : cursorFrames) {
            try {
                GLFW.glfwDestroyCursor(cursor);
            } catch (Exception e) {
                Log.e(TAG, "Error destroying cursor frame", e);
            }
        }
        cursorFrames.clear();
    }

    /**
     * Compiles an Android Bitmap into raw RGBA pixels to register a native GLFW cursor.
     */
    private long createGlfwCursor(Bitmap bitmap, int xHot, int yHot) {
        try {
            if (bitmap == null || bitmap.isRecycled()) {
                return 0;
            }

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            // Extract raw pixels from the Bitmap
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            // Explicitly map ARGB to RGBA
            ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF));     // Red channel
                    buffer.put((byte) ((pixel >> 8) & 0xFF));      // Green channel
                    buffer.put((byte) (pixel & 0xFF));             // Blue channel
                    buffer.put((byte) ((pixel >> 24) & 0xFF));     // Alpha channel
                }
            }
            buffer.flip();

            // Structure to native image format
            GLFWImage glfwImage = GLFWImage.malloc();
            glfwImage.set(width, height, buffer);

            // Hook to GLFW core
            long cursor = GLFW.glfwCreateCursor(glfwImage, xHot, yHot);
            
            // Prevent memory leaks
            glfwImage.free();

            return cursor;
        } catch (Exception e) {
            Log.e(TAG, "Cursor bitmap parsing threw runtime exception", e);
            return 0; // Returning 0 trips the fallback guard
        }
    }

    /**
     * Dispatches the pointer update request to the native event loop.
     */
    private void applyCursor(long cursor) {
        if (windowHandle != 0 && cursor != 0) {
            try {
                enforceCursorVisibility(); // Double-check driver flags before updating
                GLFW.glfwSetCursor(windowHandle, cursor);
            } catch (Exception e) {
                Log.e(TAG, "Failed to swap active GLFW cursor", e);
            }
        }
    }

    /**
     * Fallback Guard: Catches failed texture loading and reverts seamlessly to OS standard
     * cursor without dropping game input.
     */
    public void fallbackToDefault() {
        Log.w(TAG, "Falling back to standard OS mouse pointer");
        stopAnimation();
        try {
            long standardCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
            if (standardCursor != 0) {
                applyCursor(standardCursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL: Standard cursor fallback also failed", e);
        }
    }

    /**
     * Call this when shutting down the launcher/game or when swapping contexts.
     */
    public void cleanup() {
        stopAnimation();
        if (windowHandle != 0) {
            try {
                // Remove injected cursor wrapper and free handle
                GLFW.glfwSetCursor(windowHandle, 0); 
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear GLFW cursor during cleanup", e);
            }
        }
    }
}

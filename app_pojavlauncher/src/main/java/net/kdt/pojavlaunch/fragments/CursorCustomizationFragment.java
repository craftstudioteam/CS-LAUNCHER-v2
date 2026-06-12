package net.kdt.pojavlaunch.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.*;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.customcontrols.mouse.CursorManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CursorCustomizationFragment extends Fragment {

    public static final String TAG = "CursorCustomizationFragment";
    private ImageView mPreviewImage;
    private View mUploadZone;
    private Uri mSelectedImageUri;
    private Bitmap mCurrentCursorBitmap;

    private int mHotspotX = 0;
    private int mHotspotY = 0;
    private int mGlowRadius = 0;
    private int mSizeScale = 100;

    // Activity result launcher for file picker
    private final ActivityResultLauncher<String> mFilePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImageSelected);

    public CursorCustomizationFragment() {
        super(R.layout.fragment_cursor_customization);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        mPreviewImage = view.findViewById(R.id.cursor_preview_image);
        mUploadZone = view.findViewById(R.id.upload_zone);
        View importButton = view.findViewById(R.id.btn_import_png);
        View saveButton = view.findViewById(R.id.btn_save_cursor);
        View backButton = view.findViewById(R.id.cursor_back_button);

        // Setup seekbars
        SeekBar scaleSeek = view.findViewById(R.id.seek_cursor_size);
        SeekBar glowSeek = view.findViewById(R.id.seek_glow_strength);
        SeekBar hotspotXSeek = view.findViewById(R.id.seek_hotspot_x);
        SeekBar hotspotYSeek = view.findViewById(R.id.seek_hotspot_y);

        TextView scaleText = view.findViewById(R.id.scale_value_text);
        TextView glowText = view.findViewById(R.id.glow_value_text);
        TextView hotspotXText = view.findViewById(R.id.hotspot_x_value_text);
        TextView hotspotYText = view.findViewById(R.id.hotspot_y_value_text);

        // Load existing preferences
        mGlowRadius = net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_glow_radius", 0);
        mHotspotX = net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_hotspot_x", 0);
        mHotspotY = net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_hotspot_y", 0);
        mSizeScale = (int) net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.getFloat("custom_cursor_scale", 100f);

        scaleSeek.setProgress(mSizeScale);
        scaleText.setText(mSizeScale + "%");

        glowSeek.setProgress(mGlowRadius);
        glowText.setText(mGlowRadius + "%");

        hotspotXSeek.setProgress(mHotspotX);
        hotspotXText.setText(mHotspotX + " px");

        hotspotYSeek.setProgress(mHotspotY);
        hotspotYText.setText(mHotspotY + " px");

        // Load and preview current cursor if it exists
        if (net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_PATH != null) {
            File file = new File(net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_PATH);
            if (file.exists()) {
                try {
                    Bitmap currentBmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                    if (currentBmp != null) {
                        mCurrentCursorBitmap = currentBmp;
                        mPreviewImage.setImageBitmap(mCurrentCursorBitmap);
                        mPreviewImage.setPadding(0, 0, 0, 0);
                        
                        TextView label = view.findViewById(R.id.cursor_preview_label);
                        if (label != null) {
                            label.setText("CUSTOM");
                        }

                        // Configure seekbars max bounds based on active bitmap size
                        hotspotXSeek.setMax(mCurrentCursorBitmap.getWidth());
                        hotspotYSeek.setMax(mCurrentCursorBitmap.getHeight());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Entrance animation
        animateEntry(view);

        // Upload zone click
        mUploadZone.setOnClickListener(v -> openFilePicker());
        importButton.setOnClickListener(v -> openFilePicker());

        // SeekBar listeners
        scaleSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 25) {
                    progress = 25;
                    if (fromUser) seekBar.setProgress(25);
                }
                mSizeScale = progress;
                scaleText.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        glowSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mGlowRadius = progress;
                glowText.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        hotspotXSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mHotspotX = progress;
                hotspotXText.setText(progress + " px");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        hotspotYSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mHotspotY = progress;
                hotspotYText.setText(progress + " px");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Save button
        saveButton.setOnClickListener(v -> saveCursor());

        // Back button
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Apply press animations to buttons
        applyPressAnimation(backButton);
        applyPressAnimation(importButton);
        applyPressAnimation(saveButton);
    }

    private void animateEntry(View root) {
        View topBar = root.findViewById(R.id.cursor_top_bar);
        View previewContainer = root.findViewById(R.id.cursor_preview_container);

        // Top bar slides down
        topBar.setTranslationY(-80f);
        topBar.setAlpha(0f);
        topBar.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(350)
            .setInterpolator(new DecelerateInterpolator(1.5f))
            .start();

        // Animate preview container
        if (previewContainer != null) {
            previewContainer.setAlpha(0f);
            previewContainer.setTranslationY(30f);
            previewContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(180)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
        }

        // Animate upload zone
        if (mUploadZone != null) {
            mUploadZone.setAlpha(0f);
            mUploadZone.setTranslationY(20f);
            mUploadZone.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(300)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
        }
    }

    private void openFilePicker() {
        mFilePickerLauncher.launch("image/*");
    }

    private boolean isGif(Uri uri) {
        if (uri == null) return false;
        try {
            String mimeType = requireContext().getContentResolver().getType(uri);
            if (mimeType != null && mimeType.toLowerCase().contains("gif")) {
                return true;
            }
            String path = uri.getPath();
            if (path != null && path.toLowerCase().endsWith(".gif")) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private File copyUriToFile(Uri uri, String destName) throws Exception {
        File dir = new File(net.kdt.pojavlaunch.Tools.DIR_CURSORS);
        if (!dir.exists()) dir.mkdirs();
        File destFile = new File(dir, destName);
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        return destFile;
    }

    private void onImageSelected(Uri uri) {
        if (uri == null) return;
        mSelectedImageUri = uri;

        try {
            // Load the image
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return;

            // Decode bitmap with size limits to avoid OOM
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Calculate sample size (max 128px for cursor)
            int maxSize = 128;
            int sampleSize = 1;
            while (options.outWidth / sampleSize > maxSize || options.outHeight / sampleSize > maxSize) {
                sampleSize *= 2;
            }

            // Load the scaled bitmap
            InputStream inputStream2 = requireContext().getContentResolver().openInputStream(uri);
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = sampleSize;
            mCurrentCursorBitmap = BitmapFactory.decodeStream(inputStream2, null, options2);
            inputStream2.close();

            if (mCurrentCursorBitmap != null) {
                // Update preview
                mPreviewImage.setImageBitmap(mCurrentCursorBitmap);
                mPreviewImage.setPadding(0, 0, 0, 0);

                // Update hotspot seekbars max limits based on loaded image dimensions
                if (getView() != null) {
                    SeekBar hotspotXSeek = getView().findViewById(R.id.seek_hotspot_x);
                    SeekBar hotspotYSeek = getView().findViewById(R.id.seek_hotspot_y);
                    if (hotspotXSeek != null) {
                        hotspotXSeek.setMax(mCurrentCursorBitmap.getWidth());
                        mHotspotX = Math.min(mHotspotX, mCurrentCursorBitmap.getWidth());
                        hotspotXSeek.setProgress(mHotspotX);
                    }
                    if (hotspotYSeek != null) {
                        hotspotYSeek.setMax(mCurrentCursorBitmap.getHeight());
                        mHotspotY = Math.min(mHotspotY, mCurrentCursorBitmap.getHeight());
                        hotspotYSeek.setProgress(mHotspotY);
                    }
                }

                // Update label
                TextView label = getView().findViewById(R.id.cursor_preview_label);
                if (label != null) {
                    String fileName = uri.getLastPathSegment();
                    if (fileName != null && fileName.length() > 16) {
                        fileName = fileName.substring(0, 14) + "..";
                    }
                    label.setText(fileName != null ? fileName.toUpperCase() : "CUSTOM");
                }

                Toast.makeText(getContext(), "Cursor loaded successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCursor() {
        if (mCurrentCursorBitmap == null || mSelectedImageUri == null) {
            Toast.makeText(getContext(), "Please select an image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            boolean isGif = isGif(mSelectedImageUri);
            String extension = isGif ? ".gif" : ".png";
            String name = "custom_cursor_" + System.currentTimeMillis() + extension;
            File savedFile = copyUriToFile(mSelectedImageUri, name);

            // Update preferences
            net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.edit()
                .putString("custom_cursor_path", savedFile.getAbsolutePath())
                .putBoolean("custom_cursor_enabled", true)
                .putInt("custom_cursor_hotspot_x", mHotspotX)
                .putInt("custom_cursor_hotspot_y", mHotspotY)
                .putFloat("custom_cursor_scale", (float) mSizeScale)
                .putInt("custom_cursor_glow_radius", mGlowRadius)
                .apply();

            // Load variables
            net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_PATH = savedFile.getAbsolutePath();
            net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_ENABLED = true;
            net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_GLOW_RADIUS = mGlowRadius;
            net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_SCALE = (float) mSizeScale;

            // Update/refresh cursor in touchpad if active
            net.kdt.pojavlaunch.extra.ExtraCore.setValue(net.kdt.pojavlaunch.extra.ExtraConstants.REFRESH_CURSOR, null);
            
            // Reapply renderer changes
            net.kdt.pojavlaunch.customcontrols.mouse.CustomCursorRenderer.reset();
            net.kdt.pojavlaunch.customcontrols.mouse.CustomCursorRenderer.updateCursorFrame();

            Toast.makeText(getContext(), "Cursor saved and applied successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void applyPressAnimation(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                        .scaleX(0.92f)
                        .scaleY(0.92f)
                        .setDuration(80)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(180)
                        .setInterpolator(new OvershootInterpolator(2.5f))
                        .start();
                    break;
            }
            return false;
        });
    }
}

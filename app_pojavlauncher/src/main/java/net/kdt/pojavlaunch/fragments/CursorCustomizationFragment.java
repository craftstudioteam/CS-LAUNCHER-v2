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
    private int mOpacity = 100;

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
        View resetButton = view.findViewById(R.id.btn_reset_cursor);
        View backButton = view.findViewById(R.id.cursor_back_button);

        // Setup seekbars
        SeekBar scaleSeek = view.findViewById(R.id.seek_cursor_size);
        SeekBar glowSeek = view.findViewById(R.id.seek_glow_strength);
        SeekBar hotspotXSeek = view.findViewById(R.id.seek_hotspot_x);
        SeekBar hotspotYSeek = view.findViewById(R.id.seek_hotspot_y);
        SeekBar opacitySeek = view.findViewById(R.id.seek_cursor_opacity);

        TextView scaleText = view.findViewById(R.id.scale_value_text);
        TextView glowText = view.findViewById(R.id.glow_value_text);
        TextView hotspotXText = view.findViewById(R.id.hotspot_x_value_text);
        TextView hotspotYText = view.findViewById(R.id.hotspot_y_value_text);
        TextView opacityText = view.findViewById(R.id.opacity_value_text);

        // Load existing preferences
        mGlowRadius = net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_glow_radius", 0);
        mHotspotX = net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_hotspot_x", 0);
        mHotspotY = net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_hotspot_y", 0);
        mSizeScale = (int) net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.getFloat("custom_cursor_scale", 100f);
        mOpacity = net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_opacity", 100);

        scaleSeek.setProgress(mSizeScale);
        scaleText.setText(mSizeScale + "%");

        glowSeek.setProgress(mGlowRadius);
        glowText.setText(mGlowRadius + "%");

        hotspotXSeek.setProgress(mHotspotX);
        hotspotXText.setText(mHotspotX + " px");

        hotspotYSeek.setProgress(mHotspotY);
        hotspotYText.setText(mHotspotY + " px");

        opacitySeek.setProgress(mOpacity);
        opacityText.setText(mOpacity + "%");

        // Setup initial preview scaling and alpha
        if (mPreviewImage != null) {
            mPreviewImage.setScaleX(mSizeScale / 100f);
            mPreviewImage.setScaleY(mSizeScale / 100f);
            mPreviewImage.setAlpha(mOpacity / 100f);
        }
        updatePreviewStatusText(view);

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
                if (mPreviewImage != null) {
                    mPreviewImage.setScaleX(progress / 100f);
                    mPreviewImage.setScaleY(progress / 100f);
                }
                updatePreviewStatusText(view);
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

        opacitySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mOpacity = progress;
                opacityText.setText(progress + "%");
                if (mPreviewImage != null) {
                    mPreviewImage.setAlpha(progress / 100f);
                }
                updatePreviewStatusText(view);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Save button
        saveButton.setOnClickListener(v -> saveCursor());

        // Reset button
        resetButton.setOnClickListener(v -> showResetMenu());

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
        applyPressAnimation(resetButton);
    }

    private void updatePreviewStatusText(View root) {
        TextView statusText = root.findViewById(R.id.cursor_preview_status);
        if (statusText != null) {
            statusText.setText("Scale: " + mSizeScale + "% | Opacity: " + mOpacity + "%");
        }
    }

    private void showResetMenu() {
        String[] options = {
            "Reset Size",
            "Reset Position",
            "Reset Transparency",
            "Reset Cursor Style",
            "Full Reset"
        };
        
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset Cursor Options")
            .setItems(options, (dialog, which) -> {
                SeekBar scaleSeek = getView().findViewById(R.id.seek_cursor_size);
                SeekBar glowSeek = getView().findViewById(R.id.seek_glow_strength);
                SeekBar hotspotXSeek = getView().findViewById(R.id.seek_hotspot_x);
                SeekBar hotspotYSeek = getView().findViewById(R.id.seek_hotspot_y);
                SeekBar opacitySeek = getView().findViewById(R.id.seek_cursor_opacity);
                
                TextView scaleText = getView().findViewById(R.id.scale_value_text);
                TextView glowText = getView().findViewById(R.id.glow_value_text);
                TextView hotspotXText = getView().findViewById(R.id.hotspot_x_value_text);
                TextView hotspotYText = getView().findViewById(R.id.hotspot_y_value_text);
                TextView opacityText = getView().findViewById(R.id.opacity_value_text);
                
                switch (which) {
                    case 0: // Reset Size
                        mSizeScale = 100;
                        if (scaleSeek != null) scaleSeek.setProgress(100);
                        if (scaleText != null) scaleText.setText("100%");
                        if (mPreviewImage != null) {
                            mPreviewImage.setScaleX(1.0f);
                            mPreviewImage.setScaleY(1.0f);
                        }
                        break;
                    case 1: // Reset Position
                        mHotspotX = 0;
                        mHotspotY = 0;
                        if (hotspotXSeek != null) hotspotXSeek.setProgress(0);
                        if (hotspotYSeek != null) hotspotYSeek.setProgress(0);
                        if (hotspotXText != null) hotspotXText.setText("0 px");
                        if (hotspotYText != null) hotspotYText.setText("0 px");
                        break;
                    case 2: // Reset Transparency
                        mOpacity = 100;
                        mGlowRadius = 0;
                        if (opacitySeek != null) opacitySeek.setProgress(100);
                        if (glowSeek != null) glowSeek.setProgress(0);
                        if (opacityText != null) opacityText.setText("100%");
                        if (glowText != null) glowText.setText("0%");
                        if (mPreviewImage != null) {
                            mPreviewImage.setAlpha(1.0f);
                        }
                        break;
                    case 3: // Reset Cursor Style
                        net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.edit()
                            .putBoolean("custom_cursor_enabled", false)
                            .apply();
                        net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_ENABLED = false;
                        if (mPreviewImage != null) {
                            mPreviewImage.setImageResource(R.drawable.ic_mouse_pointer);
                            mPreviewImage.setPadding(6, 6, 6, 6);
                        }
                        TextView label = getView().findViewById(R.id.cursor_preview_label);
                        if (label != null) label.setText("DEFAULT");
                        break;
                    case 4: // Full Reset
                        mSizeScale = 100;
                        mHotspotX = 0;
                        mHotspotY = 0;
                        mOpacity = 100;
                        mGlowRadius = 0;
                        
                        if (scaleSeek != null) scaleSeek.setProgress(100);
                        if (hotspotXSeek != null) hotspotXSeek.setProgress(0);
                        if (hotspotYSeek != null) hotspotYSeek.setProgress(0);
                        if (opacitySeek != null) opacitySeek.setProgress(100);
                        if (glowSeek != null) glowSeek.setProgress(0);
                        
                        if (scaleText != null) scaleText.setText("100%");
                        if (hotspotXText != null) hotspotXText.setText("0 px");
                        if (hotspotYText != null) hotspotYText.setText("0 px");
                        if (opacityText != null) opacityText.setText("100%");
                        if (glowText != null) glowText.setText("0%");
                        
                        net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.edit()
                            .putBoolean("custom_cursor_enabled", false)
                            .putString("custom_cursor_path", null)
                            .putInt("custom_cursor_hotspot_x", 0)
                            .putInt("custom_cursor_hotspot_y", 0)
                            .putFloat("custom_cursor_scale", 100f)
                            .putInt("custom_cursor_glow_radius", 0)
                            .putInt("custom_cursor_opacity", 100)
                            .apply();
                        
                        net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_ENABLED = false;
                        net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_PATH = null;
                        net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_GLOW_RADIUS = 0;
                        net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_SCALE = 100f;
                        net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_OPACITY = 1f;
                        
                        if (mPreviewImage != null) {
                            mPreviewImage.setImageResource(R.drawable.ic_mouse_pointer);
                            mPreviewImage.setPadding(6, 6, 6, 6);
                            mPreviewImage.setScaleX(1.0f);
                            mPreviewImage.setScaleY(1.0f);
                            mPreviewImage.setAlpha(1.0f);
                        }
                        TextView lbl = getView().findViewById(R.id.cursor_preview_label);
                        if (lbl != null) lbl.setText("DEFAULT");
                        break;
                }
                updatePreviewStatusText(getView());
                Toast.makeText(getContext(), "Reset applied successfully!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
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
                .putInt("custom_cursor_opacity", mOpacity)
                .apply();

            // Load variables
            net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_PATH = savedFile.getAbsolutePath();
            net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_ENABLED = true;
            net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_GLOW_RADIUS = mGlowRadius;
            net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_SCALE = (float) mSizeScale;
            net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_CUSTOM_CURSOR_OPACITY = mOpacity / 100f;

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

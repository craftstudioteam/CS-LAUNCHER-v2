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
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

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
    
    // New variables for style and color
    private int mSelectedCursorStyleRes = R.drawable.ic_mouse_pointer;
    private boolean mUseCustomBitmap = false;
    private int mGlowColor = android.graphics.Color.parseColor("#A6FF3D"); // Default neon green

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
        View exportButton = view.findViewById(R.id.btn_export_cursor);
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

        // Style selector cards
        View cardClassic = view.findViewById(R.id.style_classic);
        View cardGamepad = view.findViewById(R.id.style_gamepad);
        View cardCustom = view.findViewById(R.id.style_custom);

        // Color preset ImageViews
        ImageView imgGreen = view.findViewById(R.id.color_green);
        ImageView imgCyan = view.findViewById(R.id.color_cyan);
        ImageView imgPurple = view.findViewById(R.id.color_purple);
        ImageView imgRed = view.findViewById(R.id.color_red);
        ImageView imgYellow = view.findViewById(R.id.color_yellow);
        ImageView imgWhite = view.findViewById(R.id.color_white);

        // Load existing preferences
        mGlowRadius = LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_glow_radius", 0);
        mHotspotX = LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_hotspot_x", 0);
        mHotspotY = LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_hotspot_y", 0);
        mSizeScale = (int) LauncherPreferences.DEFAULT_PREF.getFloat("custom_cursor_scale", 100f);
        mOpacity = LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_opacity", 100);
        mGlowColor = LauncherPreferences.DEFAULT_PREF.getInt("custom_cursor_glow_color", android.graphics.Color.parseColor("#A6FF3D"));

        // Set seekbars initial progress
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

        // Load and preview current cursor if it exists
        boolean enabled = LauncherPreferences.PREF_CUSTOM_CURSOR_ENABLED;
        String path = LauncherPreferences.PREF_CUSTOM_CURSOR_PATH;

        if (enabled && path != null) {
            File file = new File(path);
            if (file.exists()) {
                if (file.getName().contains("gamepad")) {
                    mSelectedCursorStyleRes = R.drawable.ic_gamepad_pointer;
                    mUseCustomBitmap = false;
                    
                    cardClassic.setBackgroundResource(R.drawable.background_card);
                    cardGamepad.setBackgroundResource(R.drawable.background_card_neon);
                    cardCustom.setBackgroundResource(R.drawable.background_card);
                } else {
                    try {
                        mCurrentCursorBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        if (mCurrentCursorBitmap != null) {
                            mUseCustomBitmap = true;
                            
                            cardClassic.setBackgroundResource(R.drawable.background_card);
                            cardGamepad.setBackgroundResource(R.drawable.background_card);
                            cardCustom.setBackgroundResource(R.drawable.background_card_neon);
                            
                            hotspotXSeek.setMax(mCurrentCursorBitmap.getWidth());
                            hotspotYSeek.setMax(mCurrentCursorBitmap.getHeight());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            mSelectedCursorStyleRes = R.drawable.ic_mouse_pointer;
            mUseCustomBitmap = false;
            
            cardClassic.setBackgroundResource(R.drawable.background_card_neon);
            cardGamepad.setBackgroundResource(R.drawable.background_card);
            cardCustom.setBackgroundResource(R.drawable.background_card);
        }

        // Initialize glow color circles selection state
        initColorSelection(mGlowColor, imgGreen, imgCyan, imgPurple, imgRed, imgYellow, imgWhite);

        // Update live preview initial state
        updateLivePreview();

        // Entrance animation
        animateEntry(view);

        // Upload zone & button clicks
        mUploadZone.setOnClickListener(v -> openFilePicker());
        importButton.setOnClickListener(v -> openFilePicker());
        exportButton.setOnClickListener(v -> exportCursor());

        // Style Selection Listeners
        cardClassic.setOnClickListener(v -> {
            mUseCustomBitmap = false;
            mSelectedCursorStyleRes = R.drawable.ic_mouse_pointer;
            cardClassic.setBackgroundResource(R.drawable.background_card_neon);
            cardGamepad.setBackgroundResource(R.drawable.background_card);
            cardCustom.setBackgroundResource(R.drawable.background_card);
            updateLivePreview();
        });

        cardGamepad.setOnClickListener(v -> {
            mUseCustomBitmap = false;
            mSelectedCursorStyleRes = R.drawable.ic_gamepad_pointer;
            cardClassic.setBackgroundResource(R.drawable.background_card);
            cardGamepad.setBackgroundResource(R.drawable.background_card_neon);
            cardCustom.setBackgroundResource(R.drawable.background_card);
            updateLivePreview();
        });

        cardCustom.setOnClickListener(v -> {
            if (mCurrentCursorBitmap == null) {
                openFilePicker();
            } else {
                mUseCustomBitmap = true;
                cardClassic.setBackgroundResource(R.drawable.background_card);
                cardGamepad.setBackgroundResource(R.drawable.background_card);
                cardCustom.setBackgroundResource(R.drawable.background_card_neon);
                updateLivePreview();
            }
        });

        // Color circle preset click listeners
        imgGreen.setOnClickListener(v -> selectGlowColor(0xFFA6FF3D, imgGreen));
        imgCyan.setOnClickListener(v -> selectGlowColor(0xFF00E5FF, imgCyan));
        imgPurple.setOnClickListener(v -> selectGlowColor(0xFFD500F9, imgPurple));
        imgRed.setOnClickListener(v -> selectGlowColor(0xFFFF3D00, imgRed));
        imgYellow.setOnClickListener(v -> selectGlowColor(0xFFFFEA00, imgYellow));
        imgWhite.setOnClickListener(v -> selectGlowColor(0xFFFFFFFF, imgWhite));

        // SeekBar listeners
        scaleSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 25) {
                    progress = 25;
                    if (fromUser) seekBar.setProgress(25);
                }
                mSizeScale = progress;
                scaleText.setText(progress + "%");
                updateLivePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        glowSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mGlowRadius = progress;
                glowText.setText(progress + "%");
                updateLivePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        hotspotXSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mHotspotX = progress;
                hotspotXText.setText(progress + " px");
                updateLivePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        hotspotYSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mHotspotY = progress;
                hotspotYText.setText(progress + " px");
                updateLivePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        opacitySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mOpacity = progress;
                opacityText.setText(progress + "%");
                updateLivePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Save & Reset actions
        saveButton.setOnClickListener(v -> saveCursor());
        resetButton.setOnClickListener(v -> resetToDefaultInstant());

        // Back button
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Apply press animations to buttons
        applyPressAnimation(backButton);
        applyPressAnimation(importButton);
        applyPressAnimation(exportButton);
        applyPressAnimation(saveButton);
        applyPressAnimation(resetButton);
    }

    private void updateLivePreview() {
        if (mPreviewImage == null) return;

        // 1. Get the base bitmap based on the selected style
        Bitmap baseBmp = null;
        try {
            if (mUseCustomBitmap && mCurrentCursorBitmap != null) {
                baseBmp = mCurrentCursorBitmap;
            } else {
                baseBmp = BitmapFactory.decodeResource(getResources(), mSelectedCursorStyleRes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (baseBmp == null) return;

        // 2. Apply the glow effect based on seekbar progress
        Bitmap processedBmp = baseBmp;
        if (mGlowRadius > 0) {
            try {
                processedBmp = CursorManager.applyGlow(baseBmp, mGlowRadius, mGlowColor);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 3. Set bitmap to the preview image view
        mPreviewImage.setImageBitmap(processedBmp);

        // For default pointer, if padding is needed, set 0 if glow is applied or 6dp if classic
        if (!mUseCustomBitmap && mGlowRadius == 0) {
            int padding = (int) (6 * getResources().getDisplayMetrics().density);
            mPreviewImage.setPadding(padding, padding, padding, padding);
        } else {
            mPreviewImage.setPadding(0, 0, 0, 0);
        }

        // 4. Update scales and alphas
        mPreviewImage.setScaleX(mSizeScale / 100f);
        mPreviewImage.setScaleY(mSizeScale / 100f);
        mPreviewImage.setAlpha(mOpacity / 100f);

        // 5. Update status labels
        View view = getView();
        if (view != null) {
            updatePreviewStatusText(view);

            TextView label = view.findViewById(R.id.cursor_preview_label);
            if (label != null) {
                if (mUseCustomBitmap) {
                    label.setText("CUSTOM");
                } else if (mSelectedCursorStyleRes == R.drawable.ic_gamepad_pointer) {
                    label.setText("GAMEPAD");
                } else {
                    label.setText("DEFAULT");
                }
            }
        }
    }

    private void updatePreviewStatusText(View root) {
        TextView statusText = root.findViewById(R.id.cursor_preview_status);
        if (statusText != null) {
            statusText.setText("Scale: " + mSizeScale + "% | Opacity: " + mOpacity + "%");
        }
    }

    private void initColorSelection(int color, ImageView... views) {
        ImageView selectView = views[0]; // Default green
        if (color == 0xFF00E5FF) selectView = views[1];
        else if (color == 0xFFD500F9) selectView = views[2];
        else if (color == 0xFFFF3D00) selectView = views[3];
        else if (color == 0xFFFFEA00) selectView = views[4];
        else if (color == 0xFFFFFFFF) selectView = views[5];

        selectGlowColor(color, selectView);
    }

    private void selectGlowColor(int color, ImageView selectedView) {
        mGlowColor = color;
        
        View root = getView();
        if (root == null) return;

        ImageView imgGreen = root.findViewById(R.id.color_green);
        ImageView imgCyan = root.findViewById(R.id.color_cyan);
        ImageView imgPurple = root.findViewById(R.id.color_purple);
        ImageView imgRed = root.findViewById(R.id.color_red);
        ImageView imgYellow = root.findViewById(R.id.color_yellow);
        ImageView imgWhite = root.findViewById(R.id.color_white);

        ImageView[] colorViews = {imgGreen, imgCyan, imgPurple, imgRed, imgYellow, imgWhite};
        for (ImageView v : colorViews) {
            if (v != null) {
                v.setImageDrawable(null);
            }
        }

        if (selectedView != null) {
            selectedView.setImageResource(R.drawable.ic_check_circle);
            selectedView.setPadding(2, 2, 2, 2);
            selectedView.setColorFilter(0xFF000000);
        }

        updateLivePreview();
    }

    private void resetToDefaultInstant() {
        mSizeScale = 100;
        mHotspotX = 0;
        mHotspotY = 0;
        mOpacity = 100;
        mGlowRadius = 0;
        mGlowColor = android.graphics.Color.parseColor("#A6FF3D");
        mUseCustomBitmap = false;
        mSelectedCursorStyleRes = R.drawable.ic_mouse_pointer;

        View root = getView();
        if (root == null) return;

        SeekBar scaleSeek = root.findViewById(R.id.seek_cursor_size);
        SeekBar glowSeek = root.findViewById(R.id.seek_glow_strength);
        SeekBar hotspotXSeek = root.findViewById(R.id.seek_hotspot_x);
        SeekBar hotspotYSeek = root.findViewById(R.id.seek_hotspot_y);
        SeekBar opacitySeek = root.findViewById(R.id.seek_cursor_opacity);

        TextView scaleText = root.findViewById(R.id.scale_value_text);
        TextView glowText = root.findViewById(R.id.glow_value_text);
        TextView hotspotXText = root.findViewById(R.id.hotspot_x_value_text);
        TextView hotspotYText = root.findViewById(R.id.hotspot_y_value_text);
        TextView opacityText = root.findViewById(R.id.opacity_value_text);

        if (scaleSeek != null) scaleSeek.setProgress(100);
        if (glowSeek != null) glowSeek.setProgress(0);
        if (hotspotXSeek != null) hotspotXSeek.setProgress(0);
        if (hotspotYSeek != null) hotspotYSeek.setProgress(0);
        if (opacitySeek != null) opacitySeek.setProgress(100);

        if (scaleText != null) scaleText.setText("100%");
        if (glowText != null) glowText.setText("0%");
        if (hotspotXText != null) hotspotXText.setText("0 px");
        if (hotspotYText != null) hotspotYText.setText("0 px");
        if (opacityText != null) opacityText.setText("100%");

        View cardClassic = root.findViewById(R.id.style_classic);
        View cardGamepad = root.findViewById(R.id.style_gamepad);
        View cardCustom = root.findViewById(R.id.style_custom);

        if (cardClassic != null) cardClassic.setBackgroundResource(R.drawable.background_card_neon);
        if (cardGamepad != null) cardGamepad.setBackgroundResource(R.drawable.background_card);
        if (cardCustom != null) cardCustom.setBackgroundResource(R.drawable.background_card);

        ImageView imgGreen = root.findViewById(R.id.color_green);
        selectGlowColor(mGlowColor, imgGreen);

        updateLivePreview();

        Toast.makeText(getContext(), "Cursor reset to default instantly!", Toast.LENGTH_SHORT).show();
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
            .setDuration(300)
            .setInterpolator(new DecelerateInterpolator(1.2f))
            .start();

        // Animate preview container
        if (previewContainer != null) {
            previewContainer.setAlpha(0f);
            previewContainer.setTranslationY(20f);
            previewContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setStartDelay(100)
                .setInterpolator(new DecelerateInterpolator(1.2f))
                .start();
        }

        // Animate upload zone
        if (mUploadZone != null) {
            mUploadZone.setAlpha(0f);
            mUploadZone.setTranslationY(15f);
            mUploadZone.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setStartDelay(200)
                .setInterpolator(new DecelerateInterpolator(1.2f))
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

    private File saveResourceToFile(int resId, String destName) throws Exception {
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), resId);
        File dir = new File(net.kdt.pojavlaunch.Tools.DIR_CURSORS);
        if (!dir.exists()) dir.mkdirs();
        File destFile = new File(dir, destName);
        try (FileOutputStream out = new FileOutputStream(destFile)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
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
                mUseCustomBitmap = true;

                View root = getView();
                if (root != null) {
                    View cardClassic = root.findViewById(R.id.style_classic);
                    View cardGamepad = root.findViewById(R.id.style_gamepad);
                    View cardCustom = root.findViewById(R.id.style_custom);

                    if (cardClassic != null) cardClassic.setBackgroundResource(R.drawable.background_card);
                    if (cardGamepad != null) cardGamepad.setBackgroundResource(R.drawable.background_card);
                    if (cardCustom != null) cardCustom.setBackgroundResource(R.drawable.background_card_neon);

                    // Update hotspot seekbars max limits based on loaded image dimensions
                    SeekBar hotspotXSeek = root.findViewById(R.id.seek_hotspot_x);
                    SeekBar hotspotYSeek = root.findViewById(R.id.seek_hotspot_y);
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

                // Update live preview
                updateLivePreview();

                Toast.makeText(getContext(), "Custom cursor loaded successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCursor() {
        try {
            boolean enabled = true;
            String path = null;

            if (mUseCustomBitmap) {
                if (mCurrentCursorBitmap == null || mSelectedImageUri == null) {
                    path = LauncherPreferences.PREF_CUSTOM_CURSOR_PATH;
                    if (path == null) {
                        Toast.makeText(getContext(), "Please select an image first!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    boolean isGif = isGif(mSelectedImageUri);
                    String extension = isGif ? ".gif" : ".png";
                    String name = "custom_cursor_" + System.currentTimeMillis() + extension;
                    File savedFile = copyUriToFile(mSelectedImageUri, name);
                    path = savedFile.getAbsolutePath();
                }
            } else if (mSelectedCursorStyleRes == R.drawable.ic_gamepad_pointer) {
                File savedFile = saveResourceToFile(R.drawable.ic_gamepad_pointer, "gamepad_cursor.png");
                path = savedFile.getAbsolutePath();
            } else {
                // Classic pointer
                enabled = false;
            }

            // Save preferences
            LauncherPreferences.DEFAULT_PREF.edit()
                .putString("custom_cursor_path", path)
                .putBoolean("custom_cursor_enabled", enabled)
                .putInt("custom_cursor_hotspot_x", mHotspotX)
                .putInt("custom_cursor_hotspot_y", mHotspotY)
                .putFloat("custom_cursor_scale", (float) mSizeScale)
                .putInt("custom_cursor_glow_radius", mGlowRadius)
                .putInt("custom_cursor_glow_color", mGlowColor)
                .putInt("custom_cursor_opacity", mOpacity)
                .apply();

            // Load variables in memory
            LauncherPreferences.PREF_CUSTOM_CURSOR_PATH = path;
            LauncherPreferences.PREF_CUSTOM_CURSOR_ENABLED = enabled;
            LauncherPreferences.PREF_CUSTOM_CURSOR_GLOW_RADIUS = mGlowRadius;
            LauncherPreferences.PREF_CUSTOM_CURSOR_GLOW_COLOR = mGlowColor;
            LauncherPreferences.PREF_CUSTOM_CURSOR_SCALE = (float) mSizeScale;
            LauncherPreferences.PREF_CUSTOM_CURSOR_OPACITY = mOpacity / 100f;

            // Reapply renderer changes
            net.kdt.pojavlaunch.extra.ExtraCore.setValue(net.kdt.pojavlaunch.extra.ExtraConstants.REFRESH_CURSOR, null);
            net.kdt.pojavlaunch.customcontrols.mouse.CustomCursorRenderer.reset();
            net.kdt.pojavlaunch.customcontrols.mouse.CustomCursorRenderer.updateCursorFrame();

            Toast.makeText(getContext(), "Cursor changes saved successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportCursor() {
        String currentPath = LauncherPreferences.PREF_CUSTOM_CURSOR_PATH;
        if (currentPath == null || !(new File(currentPath).exists())) {
            Toast.makeText(getContext(), "No customized cursor file found to export!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File srcFile = new File(currentPath);
            File exportDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            if (!exportDir.exists()) exportDir.mkdirs();
            File destFile = new File(exportDir, srcFile.getName());

            try (java.io.FileInputStream in = new java.io.FileInputStream(srcFile);
                 FileOutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            Toast.makeText(getContext(), "Cursor exported to Downloads: " + destFile.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void applyPressAnimation(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(80)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(new OvershootInterpolator(1.8f))
                        .start();
                    break;
            }
            return false;
        });
    }
}

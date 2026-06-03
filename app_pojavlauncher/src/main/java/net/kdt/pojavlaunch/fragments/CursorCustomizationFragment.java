package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
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
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.customcontrols.mouse.CursorDesignerView;
import net.kdt.pojavlaunch.customcontrols.mouse.CursorManager;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.File;
import java.io.InputStream;

public class CursorCustomizationFragment extends Fragment {
    public static final String TAG = "CursorCustomizationFragment";

    private View mPanelGeneric, mPanelCreate;
    private TextView mTabImport, mTabCreate, mTabMyCursors;
    private ImageView mPreviewNormal, mPreviewHover, mPreviewClick;
    private CursorDesignerView mDesigner;
    private SeekBar mSeekSize, mSeekGlow;
    private ImageButton mBtnPencil, mBtnEraser, mBtnFill;
    
    private Bitmap mCurrentBitmap;

    private final ActivityResultLauncher<String> mFilePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleImportedFile(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cursor_customization, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPanelGeneric = view.findViewById(R.id.panel_generic);
        mPanelCreate = view.findViewById(R.id.panel_create);

        mTabImport = view.findViewById(R.id.tab_import);
        mTabCreate = view.findViewById(R.id.tab_create);
        mTabMyCursors = view.findViewById(R.id.tab_my_cursors);

        mPreviewNormal = view.findViewById(R.id.cursor_preview_normal);
        mPreviewHover = view.findViewById(R.id.cursor_preview_hover);
        mPreviewClick = view.findViewById(R.id.cursor_preview_click);

        mDesigner = view.findViewById(R.id.cursor_designer);
        
        mSeekSize = view.findViewById(R.id.seek_cursor_size);
        mSeekGlow = view.findViewById(R.id.seek_glow_strength);

        mBtnPencil = view.findViewById(R.id.btn_tool_pencil);
        mBtnEraser = view.findViewById(R.id.btn_tool_eraser);
        mBtnFill = view.findViewById(R.id.btn_tool_fill);

        view.findViewById(R.id.cursor_back_button).setOnClickListener(v -> Tools.removeCurrentFragment(requireActivity()));

        mTabImport.setOnClickListener(v -> switchTab(0));
        mTabCreate.setOnClickListener(v -> switchTab(1));
        mTabMyCursors.setOnClickListener(v -> switchTab(2));

        view.findViewById(R.id.btn_import_png).setOnClickListener(v -> mFilePicker.launch("image/*"));
        
        // Editor Actions
        view.findViewById(R.id.btn_undo).setOnClickListener(v -> mDesigner.undo());
        view.findViewById(R.id.btn_redo).setOnClickListener(v -> mDesigner.redo());
        view.findViewById(R.id.btn_clear_canvas).setOnClickListener(v -> mDesigner.clear());
        
        // Tool Selectors
        mBtnPencil.setOnClickListener(v -> selectTool(CursorDesignerView.Tool.PENCIL));
        mBtnEraser.setOnClickListener(v -> selectTool(CursorDesignerView.Tool.ERASER));
        mBtnFill.setOnClickListener(v -> selectTool(CursorDesignerView.Tool.FILL));

        // Color Selectors
        view.findViewById(R.id.color_white).setOnClickListener(v -> mDesigner.setColor(Color.WHITE));
        view.findViewById(R.id.color_neon).setOnClickListener(v -> mDesigner.setColor(Color.parseColor("#A6FF3D")));
        view.findViewById(R.id.color_red).setOnClickListener(v -> mDesigner.setColor(Color.RED));
        view.findViewById(R.id.color_blue).setOnClickListener(v -> mDesigner.setColor(Color.BLUE));

        view.findViewById(R.id.btn_apply_cursor).setOnClickListener(v -> applyCursor());

        mSeekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mSeekGlow.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Initialize with default
        mCurrentBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_mouse_pointer);
        
        mSeekSize.setProgress((int) (LauncherPreferences.PREF_MOUSESCALE * 100));
        mSeekGlow.setProgress(LauncherPreferences.PREF_CUSTOM_CURSOR_GLOW_RADIUS);
        
        switchTab(0);
        startPreviewAnimations();
    }

    private void switchTab(int index) {
        mPanelGeneric.setVisibility(index != 1 ? View.VISIBLE : View.GONE);
        mPanelCreate.setVisibility(index == 1 ? View.VISIBLE : View.GONE);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(300);
        if (index == 1) mPanelCreate.startAnimation(fadeIn);
        else mPanelGeneric.startAnimation(fadeIn);

        mTabImport.setTextColor(index == 0 ? Color.parseColor("#A6FF3D") : Color.LTGRAY);
        mTabCreate.setTextColor(index == 1 ? Color.parseColor("#A6FF3D") : Color.LTGRAY);
        mTabMyCursors.setTextColor(index == 2 ? Color.parseColor("#A6FF3D") : Color.LTGRAY);
        
        if (index == 1) {
            mCurrentBitmap = mDesigner.getCursorBitmap();
        } else if (index == 2) {
            getView().findViewById(R.id.recycler_my_cursors).setVisibility(View.VISIBLE);
            getView().findViewById(R.id.btn_import_png).setVisibility(View.GONE);
        } else {
            getView().findViewById(R.id.recycler_my_cursors).setVisibility(View.GONE);
            getView().findViewById(R.id.btn_import_png).setVisibility(View.VISIBLE);
        }
        
        updatePreview();
    }

    private void selectTool(CursorDesignerView.Tool tool) {
        mDesigner.setTool(tool);
        mBtnPencil.setBackgroundColor(tool == CursorDesignerView.Tool.PENCIL ? Color.parseColor("#20A6FF3D") : Color.TRANSPARENT);
        mBtnEraser.setBackgroundColor(tool == CursorDesignerView.Tool.ERASER ? Color.parseColor("#20A6FF3D") : Color.TRANSPARENT);
        mBtnFill.setBackgroundColor(tool == CursorDesignerView.Tool.FILL ? Color.parseColor("#20A6FF3D") : Color.TRANSPARENT);
    }

    private void handleImportedFile(Uri uri) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            mCurrentBitmap = BitmapFactory.decodeStream(is);
            updatePreview();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to import image", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePreview() {
        if (mCurrentBitmap == null) return;
        Bitmap preview = mCurrentBitmap;
        int glow = mSeekGlow.getProgress();
        if (glow > 0) {
            preview = CursorManager.applyGlow(mCurrentBitmap, glow, Color.parseColor("#A6FF3D"));
        }
        
        mPreviewNormal.setImageBitmap(preview);
        mPreviewHover.setImageBitmap(preview);
        mPreviewClick.setImageBitmap(preview);
        
        float scale = mSeekSize.getProgress() / 100f;
        mPreviewNormal.setScaleX(scale); mPreviewNormal.setScaleY(scale);
        mPreviewHover.setScaleX(scale * 1.1f); mPreviewHover.setScaleY(scale * 1.1f);
        mPreviewClick.setScaleX(scale * 0.9f); mPreviewClick.setScaleY(scale * 0.9f);
    }

    private void applyCursor() {
        if (mCurrentBitmap == null) return;
        
        String fileName = "custom_cursor_" + System.currentTimeMillis();
        if (CursorManager.saveCursor(mCurrentBitmap, fileName)) {
            String path = new File(Tools.DIR_CURSORS, fileName + ".png").getAbsolutePath();
            LauncherPreferences.DEFAULT_PREF.edit()
                    .putString("custom_cursor_path", path)
                    .putBoolean("custom_cursor_enabled", true)
                    .putInt("mousescale", mSeekSize.getProgress())
                    .putInt("custom_cursor_glow_radius", mSeekGlow.getProgress())
                    .apply();
            
            LauncherPreferences.loadPreferences(requireContext());
            ExtraCore.setValue(ExtraConstants.REFRESH_CURSOR, true);
            Toast.makeText(getContext(), "Cursor Applied!", Toast.LENGTH_SHORT).show();
        }
    }

    private void startPreviewAnimations() {
        // Normal: Gentle Pulse
        ScaleAnimation pulse = new ScaleAnimation(0.98f, 1.02f, 0.98f, 1.02f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        pulse.setDuration(2000); pulse.setRepeatMode(Animation.REVERSE); pulse.setRepeatCount(Animation.INFINITE);
        mPreviewNormal.startAnimation(pulse);

        // Hover: Floating
        AlphaAnimation fade = new AlphaAnimation(0.7f, 1.0f);
        fade.setDuration(1000); fade.setRepeatMode(Animation.REVERSE); fade.setRepeatCount(Animation.INFINITE);
        mPreviewHover.startAnimation(fade);
    }
}

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
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CursorCustomizationFragment extends Fragment {
    public static final String TAG = "CursorCustomizationFragment";

    private View mPanelImport, mPanelCreate, mPanelMyCursors;
    private TextView mTabImport, mTabCreate, mTabMyCursors;
    private ImageView mLivePreview;
    private CursorDesignerView mDesigner;
    private SeekBar mSeekSize, mSeekGlow;
    
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

        mPanelImport = view.findViewById(R.id.panel_import);
        mPanelCreate = view.findViewById(R.id.panel_create);
        mPanelMyCursors = view.findViewById(R.id.panel_my_cursors);

        mTabImport = view.findViewById(R.id.tab_import);
        mTabCreate = view.findViewById(R.id.tab_create);
        mTabMyCursors = view.findViewById(R.id.tab_my_cursors);

        mLivePreview = view.findViewById(R.id.cursor_live_preview);
        mDesigner = view.findViewById(R.id.cursor_designer);
        
        mSeekSize = view.findViewById(R.id.seek_cursor_size);
        mSeekGlow = view.findViewById(R.id.seek_glow_strength);

        view.findViewById(R.id.cursor_back_button).setOnClickListener(v -> Tools.removeCurrentFragment(requireActivity()));

        mTabImport.setOnClickListener(v -> switchTab(0));
        mTabCreate.setOnClickListener(v -> switchTab(1));
        mTabMyCursors.setOnClickListener(v -> switchTab(2));

        view.findViewById(R.id.btn_import_png).setOnClickListener(v -> mFilePicker.launch("image/*"));
        view.findViewById(R.id.btn_clear_canvas).setOnClickListener(v -> mDesigner.clear());
        
        ImageButton btnPencil = view.findViewById(R.id.btn_tool_pencil);
        ImageButton btnEraser = view.findViewById(R.id.btn_tool_eraser);

        btnPencil.setOnClickListener(v -> {
            mDesigner.setTool(CursorDesignerView.Tool.PENCIL);
            btnPencil.setBackgroundResource(R.drawable.background_card_neon);
            btnEraser.setBackgroundResource(R.drawable.background_card);
        });

        btnEraser.setOnClickListener(v -> {
            mDesigner.setTool(CursorDesignerView.Tool.ERASER);
            btnPencil.setBackgroundResource(R.drawable.background_card);
            btnEraser.setBackgroundResource(R.drawable.background_card_neon);
        });

        view.findViewById(R.id.color_white).setOnClickListener(v -> mDesigner.setColor(Color.WHITE));
        view.findViewById(R.id.color_neon).setOnClickListener(v -> mDesigner.setColor(Color.parseColor("#A6FF3D")));
        view.findViewById(R.id.color_red).setOnClickListener(v -> mDesigner.setColor(Color.RED));
        view.findViewById(R.id.color_blue).setOnClickListener(v -> mDesigner.setColor(Color.BLUE));

        view.findViewById(R.id.btn_apply_cursor).setOnClickListener(v -> applyCursor());

        mSeekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = progress / 100f;
                mLivePreview.setScaleX(scale);
                mLivePreview.setScaleY(scale);
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
        
        updatePreview();
    }

    private void switchTab(int index) {
        mPanelImport.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        mPanelCreate.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        mPanelMyCursors.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        mTabImport.setTextColor(index == 0 ? Color.parseColor("#A6FF3D") : Color.LTGRAY);
        mTabCreate.setTextColor(index == 1 ? Color.parseColor("#A6FF3D") : Color.LTGRAY);
        mTabMyCursors.setTextColor(index == 2 ? Color.parseColor("#A6FF3D") : Color.LTGRAY);
        
        if (index == 1) {
            // Creation mode: Use designer's bitmap for preview
            mCurrentBitmap = mDesigner.getCursorBitmap();
            updatePreview();
        }
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
        mLivePreview.setImageBitmap(preview);
        
        float scale = mSeekSize.getProgress() / 100f;
        mLivePreview.setScaleX(scale);
        mLivePreview.setScaleY(scale);
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
}

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
import androidx.recyclerview.widget.RecyclerView;

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

    private View mPanelCreate, mPanelImport;
    private RecyclerView mPanelCollection;
    private TextView mTabImport, mTabCreate, mTabCollection;
    private ImageView mPreviewSmall, mPreviewMedium, mPreviewLarge;
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

        mPanelCreate = view.findViewById(R.id.panel_create);
        mPanelImport = view.findViewById(R.id.panel_import);
        mPanelCollection = view.findViewById(R.id.panel_collection);

        mTabImport = view.findViewById(R.id.tab_import);
        mTabCreate = view.findViewById(R.id.tab_create);
        mTabCollection = view.findViewById(R.id.tab_collection);

        mPreviewSmall = view.findViewById(R.id.preview_small);
        mPreviewMedium = view.findViewById(R.id.preview_medium);
        mPreviewLarge = view.findViewById(R.id.preview_large);
        
        mDesigner = view.findViewById(R.id.cursor_designer);
        
        mSeekSize = view.findViewById(R.id.seek_cursor_size);
        mSeekGlow = view.findViewById(R.id.seek_glow_strength);

        mBtnPencil = view.findViewById(R.id.btn_tool_pencil);
        mBtnFill = view.findViewById(R.id.btn_tool_fill);
        mBtnEraser = view.findViewById(R.id.btn_tool_eraser);

        view.findViewById(R.id.cursor_back_button).setOnClickListener(v -> Tools.removeCurrentFragment(requireActivity()));

        mTabImport.setOnClickListener(v -> switchTab(0));
        mTabCreate.setOnClickListener(v -> switchTab(1));
        mTabCollection.setOnClickListener(v -> switchTab(2));

        view.findViewById(R.id.btn_import_png).setOnClickListener(v -> mFilePicker.launch("image/*"));
        
        // Editor Actions
        view.findViewById(R.id.btn_undo).setOnClickListener(v -> mDesigner.undo());
        view.findViewById(R.id.btn_redo).setOnClickListener(v -> mDesigner.redo());
        view.findViewById(R.id.btn_clear_canvas).setOnClickListener(v -> mDesigner.clear());
        view.findViewById(R.id.btn_save_creation).setOnClickListener(v -> applyCursor());

        // Tool Selectors
        mBtnPencil.setOnClickListener(v -> selectTool(CursorDesignerView.Tool.PENCIL));
        mBtnFill.setOnClickListener(v -> selectTool(CursorDesignerView.Tool.FILL));
        mBtnEraser.setOnClickListener(v -> selectTool(CursorDesignerView.Tool.ERASER));

        // Color Selectors
        view.findViewById(R.id.color_white).setOnClickListener(v -> mDesigner.setColor(Color.WHITE));
        view.findViewById(R.id.color_neon).setOnClickListener(v -> mDesigner.setColor(Color.parseColor("#A6FF3D")));
        view.findViewById(R.id.color_red).setOnClickListener(v -> mDesigner.setColor(Color.RED));
        view.findViewById(R.id.color_blue).setOnClickListener(v -> mDesigner.setColor(Color.BLUE));
        view.findViewById(R.id.color_black).setOnClickListener(v -> mDesigner.setColor(Color.BLACK));
        view.findViewById(R.id.color_orange).setOnClickListener(v -> mDesigner.setColor(Color.parseColor("#FFBB33")));
        view.findViewById(R.id.color_purple).setOnClickListener(v -> mDesigner.setColor(Color.parseColor("#AA66CC")));

        view.findViewById(R.id.btn_apply_import).setOnClickListener(v -> applyCursor());

        mSeekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Live scale preview update if needed
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Real-time canvas listener
        mDesigner.setOnCanvasChangedListener(bitmap -> {
            mCurrentBitmap = bitmap;
            updatePreviews();
        });

        // Initialize with default
        mCurrentBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_mouse_pointer);
        
        mSeekSize.setProgress((int) (LauncherPreferences.PREF_MOUSESCALE * 100));
        mSeekGlow.setProgress(LauncherPreferences.PREF_CUSTOM_CURSOR_GLOW_RADIUS);
        
        switchTab(1); // Default to Create
        selectTool(CursorDesignerView.Tool.PENCIL);
    }

    private void switchTab(int index) {
        mPanelCreate.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        mPanelImport.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        mPanelCollection.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        mTabImport.setTextColor(index == 0 ? Color.parseColor("#A6FF3D") : Color.LTGRAY);
        mTabCreate.setTextColor(index == 1 ? Color.parseColor("#A6FF3D") : Color.LTGRAY);
        mTabCollection.setTextColor(index == 2 ? Color.parseColor("#A6FF3D") : Color.LTGRAY);
        
        mTabImport.setBackgroundResource(index == 0 ? R.drawable.bg_nav_item_active : 0);
        mTabCreate.setBackgroundResource(index == 1 ? R.drawable.bg_nav_item_active : 0);
        mTabCollection.setBackgroundResource(index == 2 ? R.drawable.bg_nav_item_active : 0);

        if (index == 1) {
            mCurrentBitmap = mDesigner.getCursorBitmap();
            updatePreviews();
        }
    }

    private void selectTool(CursorDesignerView.Tool tool) {
        mDesigner.setTool(tool);
        mBtnPencil.setBackgroundResource(tool == CursorDesignerView.Tool.PENCIL ? R.drawable.bg_nav_item_active : R.drawable.background_card);
        mBtnFill.setBackgroundResource(tool == CursorDesignerView.Tool.FILL ? R.drawable.bg_nav_item_active : R.drawable.background_card);
        mBtnEraser.setBackgroundResource(tool == CursorDesignerView.Tool.ERASER ? R.drawable.bg_nav_item_active : R.drawable.background_card);
        
        int neon = Color.parseColor("#A6FF3D");
        int gray = Color.LTGRAY;
        mBtnPencil.setImageTintList(android.content.res.ColorStateList.valueOf(tool == CursorDesignerView.Tool.PENCIL ? neon : gray));
        mBtnFill.setImageTintList(android.content.res.ColorStateList.valueOf(tool == CursorDesignerView.Tool.FILL ? neon : gray));
        mBtnEraser.setImageTintList(android.content.res.ColorStateList.valueOf(tool == CursorDesignerView.Tool.ERASER ? neon : gray));
    }

    private void handleImportedFile(Uri uri) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            mCurrentBitmap = BitmapFactory.decodeStream(is);
            updatePreviews();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to import image", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePreviews() {
        if (mCurrentBitmap != null) {
            mPreviewSmall.setImageBitmap(mCurrentBitmap);
            mPreviewMedium.setImageBitmap(mCurrentBitmap);
            mPreviewLarge.setImageBitmap(mCurrentBitmap);
        }
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

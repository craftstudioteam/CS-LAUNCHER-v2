package net.kdt.pojavlaunch.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

import java.io.InputStream;

public class SkinManagerFragment extends Fragment {

    public static final String TAG = "SKIN_MANAGER_FRAGMENT";
    private static final int REQUEST_CODE_SKIN = 1001;
    private static final int REQUEST_CODE_CAPE = 1002;

    private static final String PREF_NAME = "skin_manager_prefs";
    private static final String KEY_SKIN_URI = "skin_path";
    private static final String KEY_CAPE_URI = "cape_path";
    private static final String KEY_TEXTURE_MODEL = "texture_model";

    private GLSurfaceView mSkinPreviewSurface;
    private SwitchCompat mSwitchModelType;
    private TextView mTvSkinPath;
    private TextView mTvCapePath;

    // Placeholder for the custom SkinRenderer (integration required with provided classes)
    // private SkinRenderer mSkinRenderer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_skin_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSkinPreviewSurface = view.findViewById(R.id.skin_preview_surface);
        mSwitchModelType = view.findViewById(R.id.switch_model_type);
        mTvSkinPath = view.findViewById(R.id.tv_skin_path);
        mTvCapePath = view.findViewById(R.id.tv_cape_path);

        // Setup OpenGL Surface
        mSkinPreviewSurface.setEGLContextClientVersion(2);
        // mSkinRenderer = new SkinRenderer(requireContext());
        // mSkinPreviewSurface.setRenderer(mSkinRenderer);
        // mSkinPreviewSurface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Load Preferences
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String skinUriStr = prefs.getString(KEY_SKIN_URI, null);
        String capeUriStr = prefs.getString(KEY_CAPE_URI, null);
        String modelType = prefs.getString(KEY_TEXTURE_MODEL, "default");

        mSwitchModelType.setChecked("slim".equals(modelType));
        updatePathText(mTvSkinPath, skinUriStr, "Select PNG from storage");
        updatePathText(mTvCapePath, capeUriStr, "Select PNG from storage");

        // Model Type Toggle
        mSwitchModelType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putString(KEY_TEXTURE_MODEL, isChecked ? "slim" : "default").apply();
            updatePreview();
        });

        // Change Skin Button
        view.findViewById(R.id.btn_change_skin).setOnClickListener(v -> openFilePicker(REQUEST_CODE_SKIN));

        // Change Cape Button
        view.findViewById(R.id.btn_change_cape).setOnClickListener(v -> openFilePicker(REQUEST_CODE_CAPE));

        updatePreview();
    }

    private void openFilePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/png");
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            if (requestCode == REQUEST_CODE_SKIN) {
                prefs.edit().putString(KEY_SKIN_URI, uri.toString()).apply();
                updatePathText(mTvSkinPath, uri.toString(), "Select PNG from storage");
            } else if (requestCode == REQUEST_CODE_CAPE) {
                prefs.edit().putString(KEY_CAPE_URI, uri.toString()).apply();
                updatePathText(mTvCapePath, uri.toString(), "Select PNG from storage");
            }
            updatePreview();
        }
    }

    private void updatePathText(TextView textView, String uriStr, String defaultText) {
        if (uriStr != null) {
            Uri uri = Uri.parse(uriStr);
            textView.setText(uri.getLastPathSegment() != null ? uri.getLastPathSegment() : uriStr);
        } else {
            textView.setText(defaultText);
        }
    }

    private void updatePreview() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String skinUriStr = prefs.getString(KEY_SKIN_URI, null);
        String capeUriStr = prefs.getString(KEY_CAPE_URI, null);

        Bitmap skinBitmap = loadBitmapFromUri(skinUriStr);
        Bitmap capeBitmap = loadBitmapFromUri(capeUriStr);

        // if (mSkinRenderer != null && skinBitmap != null) {
        //     mSkinRenderer.setTexture(skinBitmap, capeBitmap);
        // }
    }

    private Bitmap loadBitmapFromUri(String uriStr) {
        if (uriStr == null) return null;
        try {
            Uri uri = Uri.parse(uriStr);
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                is.close();
                return bitmap;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load bitmap from URI: " + uriStr, e);
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSkinPreviewSurface != null) {
            try {
                mSkinPreviewSurface.onResume();
            } catch (Exception e) {
                Log.w(TAG, "GLSurfaceView onResume failed, renderer likely not set.", e);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSkinPreviewSurface != null) {
            try {
                mSkinPreviewSurface.onPause();
            } catch (Exception e) {
                Log.w(TAG, "GLSurfaceView onPause failed, renderer likely not set.", e);
            }
        }
    }
}

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

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.R;

import java.io.InputStream;
import java.io.File;
import net.kdt.pojavlaunch.value.MinecraftAccount;
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

    private String mPendingSkinUri;
    private String mPendingCapeUri;

    private SkinRenderer mSkinRenderer;

    private View mRemoveCapeButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_skin_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Authentication Guard
        MinecraftAccount activeAccount = net.kdt.pojavlaunch.PojavProfile.getCurrentProfileContent(requireContext(), null);
        if (activeAccount == null) {
            Tools.dialog(requireContext(), "Authentication Required", "Please log in or create an account first before managing textures.");
            getParentFragmentManager().popBackStack();
            return;
        }

        mSkinPreviewSurface = view.findViewById(R.id.skin_preview_surface);
        mSwitchModelType = view.findViewById(R.id.switch_model_type);
        mTvSkinPath = view.findViewById(R.id.tv_skin_path);
        mTvCapePath = view.findViewById(R.id.tv_cape_path);
        mRemoveCapeButton = view.findViewById(R.id.btn_remove_cape);

        // Setup OpenGL Surface
        mSkinPreviewSurface.setEGLContextClientVersion(2);
        // 2. Fix 3D Preview Container
        mSkinRenderer = new SkinRenderer(requireContext());
        mSkinPreviewSurface.setRenderer(mSkinRenderer);
        mSkinPreviewSurface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Load skin/cape and model type locally associated with the current profile
        File skinsDir = new File(Tools.DIR_DATA + "/skins");
        File capesDir = new File(Tools.DIR_DATA + "/capes");
        if (!skinsDir.exists()) skinsDir.mkdirs();
        if (!capesDir.exists()) capesDir.mkdirs();

        File localSkinFile = new File(skinsDir, activeAccount.username + "_skin.png");
        File localSkinMetadata = new File(skinsDir, activeAccount.username + "_metadata.json");
        File localCapeFile = new File(capesDir, activeAccount.username + "_cape.png");

        mPendingSkinUri = localSkinFile.exists() ? Uri.fromFile(localSkinFile).toString() : null;
        mPendingCapeUri = localCapeFile.exists() ? Uri.fromFile(localCapeFile).toString() : null;

        String modelType = "default";
        if (localSkinMetadata.exists()) {
            try {
                String metaContent = Tools.read(localSkinMetadata.getAbsolutePath());
                if (metaContent.contains("slim")) {
                    modelType = "slim";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mSwitchModelType.setChecked("slim".equals(modelType));
        updatePathText(mTvSkinPath, mPendingSkinUri, "Select PNG from storage");
        updatePathText(mTvCapePath, mPendingCapeUri, "Select PNG from storage");
        updateCapeButtonVisibility();

        // Model Type Toggle
        mSwitchModelType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updatePreview();
        });

        // Change Skin Button
        view.findViewById(R.id.btn_change_skin).setOnClickListener(v -> openFilePicker(REQUEST_CODE_SKIN));

        // Change Cape Button
        view.findViewById(R.id.btn_change_cape).setOnClickListener(v -> openFilePicker(REQUEST_CODE_CAPE));

        // Remove Cape Button
        if (mRemoveCapeButton != null) {
            mRemoveCapeButton.setOnClickListener(v -> {
                mPendingCapeUri = null;
                updatePathText(mTvCapePath, null, "Select PNG from storage");
                updateCapeButtonVisibility();
                updatePreview();
            });
        }

        // Save Changes Button
        view.findViewById(R.id.btn_save_skin_changes).setOnClickListener(v -> {
            MinecraftAccount acc = net.kdt.pojavlaunch.PojavProfile.getCurrentProfileContent(requireContext(), null);
            if (acc == null) return;

            try {
                // Save Skin
                if (mPendingSkinUri != null) {
                    File destSkin = new File(Tools.DIR_DATA + "/skins/" + acc.username + "_skin.png");
                    if (!mPendingSkinUri.equals(Uri.fromFile(destSkin).toString())) {
                        copyUriToFile(Uri.parse(mPendingSkinUri), destSkin);
                    }

                    File destSkinMeta = new File(Tools.DIR_DATA + "/skins/" + acc.username + "_metadata.json");
                    String model = mSwitchModelType.isChecked() ? "slim" : "default";
                    String metaContent = "{\n  \"model\": \"" + model + "\"\n}";
                    Tools.write(destSkinMeta.getAbsolutePath(), metaContent);
                } else {
                    new File(Tools.DIR_DATA + "/skins/" + acc.username + "_skin.png").delete();
                    new File(Tools.DIR_DATA + "/skins/" + acc.username + "_metadata.json").delete();
                }

                // Save Cape
                if (mPendingCapeUri != null) {
                    File destCape = new File(Tools.DIR_DATA + "/capes/" + acc.username + "_cape.png");
                    if (!mPendingCapeUri.equals(Uri.fromFile(destCape).toString())) {
                        copyUriToFile(Uri.parse(mPendingCapeUri), destCape);
                    }

                    File destCapeMeta = new File(Tools.DIR_DATA + "/capes/" + acc.username + "_metadata.json");
                    Tools.write(destCapeMeta.getAbsolutePath(), "{\n  \"enabled\": true\n}");
                } else {
                    new File(Tools.DIR_DATA + "/capes/" + acc.username + "_cape.png").delete();
                    new File(Tools.DIR_DATA + "/capes/" + acc.username + "_metadata.json").delete();
                }

                Toast.makeText(requireContext(), "Texture Saved Successfully!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Failed to save textures: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        updatePreview();
    }

    private void updateCapeButtonVisibility() {
        if (mRemoveCapeButton != null) {
            mRemoveCapeButton.setVisibility(mPendingCapeUri != null ? View.VISIBLE : View.GONE);
        }
    }

    private void copyUriToFile(Uri uri, File destFile) throws Exception {
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             java.io.FileOutputStream out = new java.io.FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
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

            if (requestCode == REQUEST_CODE_SKIN) {
                mPendingSkinUri = uri.toString();
                updatePathText(mTvSkinPath, mPendingSkinUri, "Select PNG from storage");
            } else if (requestCode == REQUEST_CODE_CAPE) {
                mPendingCapeUri = uri.toString();
                updatePathText(mTvCapePath, mPendingCapeUri, "Select PNG from storage");
            }
            updateCapeButtonVisibility();
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
        Bitmap skinBitmap = loadBitmapFromUri(mPendingSkinUri);
        Bitmap capeBitmap = loadBitmapFromUri(mPendingCapeUri);

        if (mSkinRenderer != null && skinBitmap != null) {
            mSkinRenderer.setTexture(skinBitmap, capeBitmap);
        }
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
                Log.w(TAG, "GLSurfaceView onResume failed", e);
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
                Log.w(TAG, "GLSurfaceView onPause failed", e);
            }
        }
    }

    /**
     * Internal Renderer for Skin Preview
     */
    private static class SkinRenderer implements GLSurfaceView.Renderer {
        private final Context mContext;
        private volatile Bitmap mSkinBitmap;
        private volatile Bitmap mCapeBitmap;

        public SkinRenderer(Context context) {
            this.mContext = context;
        }

        public void setTexture(Bitmap skin, Bitmap cape) {
            this.mSkinBitmap = skin;
            this.mCapeBitmap = cape;
        }

        @Override
        public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
            if (mSkinBitmap == null) mSkinBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_pojav_full);
            android.opengl.GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            android.opengl.GLES20.glEnable(android.opengl.GLES20.GL_DEPTH_TEST);
        }

        @Override
        public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
            android.opengl.GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
            android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT | android.opengl.GLES20.GL_DEPTH_BUFFER_BIT);
            // Rendering logic for 3D player model would go here
        }
    }
}

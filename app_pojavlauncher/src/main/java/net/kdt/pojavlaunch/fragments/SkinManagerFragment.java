package net.kdt.pojavlaunch.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.value.MinecraftAccount;
import net.kdt.pojavlaunch.yggdrasil.SkinAnalyzer;
import net.kdt.pojavlaunch.yggdrasil.SkinModelType;
import net.kdt.pojavlaunch.yggdrasil.PlayerSkin;
import net.kdt.pojavlaunch.yggdrasil.PlayerCape;
import net.kdt.pojavlaunch.yggdrasil.LocalUuidUtils;
import net.kdt.pojavlaunch.yggdrasil.LocalYggdrasilServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class SkinManagerFragment extends Fragment {

    public static final String TAG = "SKIN_MANAGER_FRAGMENT";
    private static final int REQUEST_CODE_SKIN = 1001;
    private static final int REQUEST_CODE_CAPE = 1002;

    private GLSurfaceView mSkinPreviewSurface;
    private SwitchCompat mSwitchModelType;
    private TextView mTvSkinPath;
    private TextView mTvCapePath;


    private String mPendingSkinUri;
    private String mPendingCapeUri;

    private SkinRenderer mSkinRenderer;

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


        // Setup OpenGL Surface
        mSkinPreviewSurface.setEGLContextClientVersion(2);
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

        boolean isSlim = false;
        if (localSkinMetadata.exists()) {
            try {
                String metaContent = Tools.read(localSkinMetadata.getAbsolutePath());
                if (metaContent.contains("slim")) {
                    isSlim = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mSwitchModelType.setChecked(isSlim);



        updatePathText(mTvSkinPath, mPendingSkinUri, "No custom skin selected");
        updatePathText(mTvCapePath, mPendingCapeUri, "No custom cape selected");
        updateAccountInfo();

        // Model Type Toggle
        mSwitchModelType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateAccountInfo();
            updatePreview();
        });

        // Change Skin Button
        view.findViewById(R.id.btn_change_skin).setOnClickListener(v -> openFilePicker(REQUEST_CODE_SKIN));

        // Remove Skin Button
        view.findViewById(R.id.btn_remove_skin).setOnClickListener(v -> {
            mPendingSkinUri = null;
            updatePathText(mTvSkinPath, null, "No custom skin selected");
            updateAccountInfo();
            updatePreview();
        });

        // Reset To Default Button
        view.findViewById(R.id.btn_reset_default).setOnClickListener(v -> {
            mPendingSkinUri = null;
            mPendingCapeUri = null;
            mSwitchModelType.setChecked(false);
            updatePathText(mTvSkinPath, null, "No custom skin selected");
            updatePathText(mTvCapePath, null, "No custom cape selected");
            updateAccountInfo();
            updatePreview();
        });

        // Change Cape Button
        view.findViewById(R.id.btn_change_cape).setOnClickListener(v -> openFilePicker(REQUEST_CODE_CAPE));

        // Remove Cape Button
        view.findViewById(R.id.btn_remove_cape).setOnClickListener(v -> {
            mPendingCapeUri = null;
            updatePathText(mTvCapePath, null, "No custom cape selected");
            updateAccountInfo();
            updatePreview();
        });

        // Touch listener for rotation control
        mSkinPreviewSurface.setOnTouchListener(new View.OnTouchListener() {
            private float previousX;
            private float previousY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        float dx = x - previousX;
                        float dy = y - previousY;
                        if (mSkinRenderer != null) {
                            mSkinRenderer.mAngleX += dx * 0.5f;
                            mSkinRenderer.mAngleY += dy * 0.5f;
                        }
                        break;
                }
                previousX = x;
                previousY = y;
                return true;
            }
        });

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
                } else {
                    new File(Tools.DIR_DATA + "/capes/" + acc.username + "_cape.png").delete();
                }

                // Update Yggdrasil server state immediately if active
                boolean isSlimModel = mSwitchModelType.isChecked();
                String finalSkin = mPendingSkinUri != null ? new File(Tools.DIR_DATA + "/skins/" + acc.username + "_skin.png").getAbsolutePath() : null;
                String finalCape = mPendingCapeUri != null ? new File(Tools.DIR_DATA + "/capes/" + acc.username + "_cape.png").getAbsolutePath() : null;
                String accUuid = LocalUuidUtils.generateProfileId(acc.username, isSlimModel ? SkinModelType.ALEX : SkinModelType.STEVE);
                
                LocalYggdrasilServer.registerProfile(acc.username, accUuid, finalSkin, finalCape, isSlimModel);

                acc.clearFaceCache();

                Toast.makeText(requireContext(), "Textures Saved Successfully!", Toast.LENGTH_SHORT).show();
                updateAccountInfo();

                // Refresh account spinner to update the skin head beside username
                if (getActivity() != null) {
                    com.kdt.mcgui.mcAccountSpinner spinner = getActivity().findViewById(R.id.account_spinner);
                    if (spinner != null) {
                        spinner.reloadAccounts(true, spinner.getSelectedItemPosition());
                    }
                    if (getActivity() instanceof LauncherActivity) {
                        ((LauncherActivity) getActivity()).updateNavSkinIcon();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Failed to save textures: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        updatePreview();
    }

    private void updateAccountInfo() {
        // Obsolete detail TextViews removed from layout
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

            byte[] bytes = readBytesFromUri(uri);
            if (bytes == null) {
                Toast.makeText(requireContext(), "Failed to load selected image file.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (requestCode == REQUEST_CODE_SKIN) {
                PlayerSkin prep = SkinAnalyzer.prepareSkin(bytes);
                if (prep == null) {
                    Toast.makeText(requireContext(), "Invalid skin! Must be 64x64 or 64x32 pixels.", Toast.LENGTH_LONG).show();
                    return;
                }
                mPendingSkinUri = uri.toString();
                mSwitchModelType.setChecked(prep.getModel() == SkinModelType.ALEX);
                updatePathText(mTvSkinPath, mPendingSkinUri, "No custom skin selected");
            } else if (requestCode == REQUEST_CODE_CAPE) {
                PlayerCape prep = SkinAnalyzer.prepareCape(bytes);
                if (prep == null) {
                    Toast.makeText(requireContext(), "Invalid cape size!", Toast.LENGTH_SHORT).show();
                    return;
                }
                mPendingCapeUri = uri.toString();
                updatePathText(mTvCapePath, mPendingCapeUri, "No custom cape selected");
            }
            updateAccountInfo();
            updatePreview();
        }
    }

    private byte[] readBytesFromUri(Uri uri) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read bytes from Uri: " + uri, e);
            return null;
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
        if (skinBitmap == null) {
            skinBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_steve);
        }
        Bitmap capeBitmap = loadBitmapFromUri(mPendingCapeUri);

        if (mSkinRenderer != null) {
            mSkinRenderer.mIsSlim = mSwitchModelType.isChecked();
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
        if (mSkinRenderer != null) {
            mSkinRenderer.onPause();
        }
    }

    /**
     * 3D Player Model OpenGL Renderer
     */
    private static class SkinRenderer implements GLSurfaceView.Renderer {
        private final Context mContext;
        
        public volatile float mAngleX = 0f;
        public volatile float mAngleY = 0f;
        public volatile boolean mIsSlim = false;

        private int mProgram;
        private int mPositionHandle;
        private int mTextureCoordHandle;
        private int mMVPMatrixHandle;
        private int mTextureUniformHandle;

        private final float[] mMVPMatrix = new float[16];
        private final float[] mProjectionMatrix = new float[16];
        private final float[] mViewMatrix = new float[16];
        private final float[] mModelMatrix = new float[16];

        private Cuboid mHead;
        private Cuboid mTorso;
        private Cuboid mSteveLeftArm;
        private Cuboid mSteveRightArm;
        private Cuboid mAlexLeftArm;
        private Cuboid mAlexRightArm;
        private Cuboid mLeftLeg;
        private Cuboid mRightLeg;
        private Cuboid mCape;

        private int mLastTexW = 0;
        private int mLastTexH = 0;
        private int mLastCapeW = 0;
        private int mLastCapeH = 0;

        private boolean mSkinTextureNeedsUpdate = false;
        private boolean mCapeTextureNeedsUpdate = false;
        private Bitmap mPendingSkinBitmap;
        private Bitmap mPendingCapeBitmap;
        private int mSkinTextureId = 0;
        private int mCapeTextureId = 0;

        private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "  gl_Position = uMVPMatrix * aPosition;\n" +
            "  vTextureCoord = aTextureCoord;\n" +
            "}\n";

        private final String fragmentShaderCode =
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform sampler2D sTexture;\n" +
            "void main() {\n" +
            "  vec4 color = texture2D(sTexture, vTextureCoord);\n" +
            "  if (color.a < 0.1) discard;\n" +
            "  gl_FragColor = color;\n" +
            "}\n";

        public SkinRenderer(Context context) {
            this.mContext = context;
        }

        public synchronized void setTexture(Bitmap skin, Bitmap cape) {
            this.mPendingSkinBitmap = skin;
            this.mPendingCapeBitmap = cape;
            this.mSkinTextureNeedsUpdate = true;
            this.mCapeTextureNeedsUpdate = true;
        }

        public void onPause() {
            mSkinTextureId = 0;
            mCapeTextureId = 0;
            mLastTexW = 0;
            mLastTexH = 0;
            mLastCapeW = 0;
            mLastCapeH = 0;
            mHead = null;
            mTorso = null;
            mSteveLeftArm = null;
            mSteveRightArm = null;
            mAlexLeftArm = null;
            mAlexRightArm = null;
            mRightLeg = null;
            mLeftLeg = null;
            mCape = null;
        }

        @Override
        public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
            GLES20.glClearColor(0.05f, 0.06f, 0.08f, 1.0f); // Dark background matches premium theme
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthFunc(GLES20.GL_LEQUAL);

            // Shader compiles
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "sTexture");
        }

        @Override
        public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float ratio = (float) width / height;
            // Use orthographic projection to match Minecraft's inventory preview
            Matrix.orthoM(mProjectionMatrix, 0, -ratio * 18f, ratio * 18f, -19f, 19f, 0.1f, 100.0f);
        }

        @Override
        public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            // Upload textures on GL thread
            synchronized (this) {
                if (mSkinTextureNeedsUpdate) {
                    if (mSkinTextureId != 0) {
                        GLES20.glDeleteTextures(1, new int[]{mSkinTextureId}, 0);
                        mSkinTextureId = 0;
                    }
                    if (mPendingSkinBitmap != null) {
                        mSkinTextureId = loadGLTexture(mPendingSkinBitmap);
                    }
                    mSkinTextureNeedsUpdate = false;
                }
                if (mCapeTextureNeedsUpdate) {
                    if (mCapeTextureId != 0) {
                        GLES20.glDeleteTextures(1, new int[]{mCapeTextureId}, 0);
                        mCapeTextureId = 0;
                    }
                    if (mPendingCapeBitmap != null) {
                        mCapeTextureId = loadGLTexture(mPendingCapeBitmap);
                    }
                    mCapeTextureNeedsUpdate = false;
                }
            }

            if (mSkinTextureId == 0) return;

            // Rebuild cuboids on texture size changes
            if (mPendingSkinBitmap != null) {
                checkRebuildCuboids(mPendingSkinBitmap.getWidth(), mPendingSkinBitmap.getHeight());
            }
            if (mCapeTextureId != 0 && mPendingCapeBitmap != null) {
                if (mCape == null || mLastCapeW != mPendingCapeBitmap.getWidth() || mLastCapeH != mPendingCapeBitmap.getHeight()) {
                    mLastCapeW = mPendingCapeBitmap.getWidth();
                    mLastCapeH = mPendingCapeBitmap.getHeight();
                    rebuildCape(mLastCapeW, mLastCapeH);
                }
            } else {
                mCape = null;
            }

            // Set camera (looking at center of player: Y = -4, moved back to Z = 40 for full body view)
            Matrix.setLookAtM(mViewMatrix, 0, 0f, -4f, 40f, 0f, -4f, 0f, 0f, 1.0f, 0f);

            // Rotations (pivoted around character center: Y = -4)
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, 0f, -4f, 0f);
            Matrix.rotateM(mModelMatrix, 0, mAngleY, 1f, 0f, 0f);
            Matrix.rotateM(mModelMatrix, 0, mAngleX, 0f, 1f, 0f);
            Matrix.translateM(mModelMatrix, 0, 0f, 4f, 0f);

            float[] mvMatrix = new float[16];
            Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mvMatrix, 0);

            GLES20.glUseProgram(mProgram);
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

            // Draw model
            draw(mHead, mSkinTextureId);
            draw(mTorso, mSkinTextureId);
            draw(mRightLeg, mSkinTextureId);
            draw(mLeftLeg, mSkinTextureId);

            if (mIsSlim) {
                draw(mAlexRightArm, mSkinTextureId);
                draw(mAlexLeftArm, mSkinTextureId);
            } else {
                draw(mSteveRightArm, mSkinTextureId);
                draw(mSteveLeftArm, mSkinTextureId);
            }

            // Draw Cape
            if (mCape != null && mCapeTextureId != 0) {
                float[] capeModelMatrix = new float[16];
                System.arraycopy(mModelMatrix, 0, capeModelMatrix, 0, 16);
                // Translate pivot (shoulder at Y=4, Z=2) to origin, rotate, translate back
                Matrix.translateM(capeModelMatrix, 0, 0f, 4f, 2f);
                Matrix.rotateM(capeModelMatrix, 0, 15.0f, 1f, 0f, 0f);
                Matrix.translateM(capeModelMatrix, 0, 0f, -4f, -2f);

                float[] capeMvMatrix = new float[16];
                float[] capeMvpMatrix = new float[16];
                Matrix.multiplyMM(capeMvMatrix, 0, mViewMatrix, 0, capeModelMatrix, 0);
                Matrix.multiplyMM(capeMvpMatrix, 0, mProjectionMatrix, 0, capeMvMatrix, 0);

                GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, capeMvpMatrix, 0);
                draw(mCape, mCapeTextureId);
            }
        }

        private void draw(Cuboid cuboid, int textureId) {
            if (cuboid == null || textureId == 0) return;

            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, cuboid.vertexBuffer);

            GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
            GLES20.glVertexAttribPointer(mTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, cuboid.uvBuffer);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mTextureUniformHandle, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, cuboid.vertexCount);

            GLES20.glDisableVertexAttribArray(mPositionHandle);
            GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        private int loadGLTexture(Bitmap bitmap) {
            int[] textureIds = new int[1];
            GLES20.glGenTextures(1, textureIds, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            return textureIds[0];
        }

        private void checkRebuildCuboids(int texW, int texH) {
            if (texW == mLastTexW && texH == mLastTexH && mHead != null) return;
            mLastTexW = texW;
            mLastTexH = texH;

            // Head: size 8x8x8. Bounds: X[-4, 4], Y[4, 12], Z[-4, 4]
            mHead = new Cuboid(-4, 4, 4, 12, -4, 4, 0, 0, 8, 8, 8, texW, texH, false);

            // Torso: size 8x12x4. Bounds: X[-4, 4], Y[-8, 4], Z[-2, 2]
            mTorso = new Cuboid(-4, 4, -8, 4, -2, 2, 16, 16, 8, 12, 4, texW, texH, false);

            // Steve Right Arm: size 4x12x4. Bounds: X[4, 8], Y[-8, 4], Z[-2, 2]
            mSteveRightArm = new Cuboid(4, 8, -8, 4, -2, 2, 40, 16, 4, 12, 4, texW, texH, false);

            // Steve Left Arm: mirrored from Right Arm if 64x32
            if (texH >= 64) {
                mSteveLeftArm = new Cuboid(-8, -4, -8, 4, -2, 2, 32, 48, 4, 12, 4, texW, texH, false);
            } else {
                mSteveLeftArm = new Cuboid(-8, -4, -8, 4, -2, 2, 40, 16, 4, 12, 4, texW, texH, true);
            }

            // Alex Right Arm: size 3x12x4. Bounds: X[4, 7], Y[-8, 4], Z[-2, 2]
            mAlexRightArm = new Cuboid(4, 7, -8, 4, -2, 2, 40, 16, 3, 12, 4, texW, texH, false);

            // Alex Left Arm: mirrored from Right Arm if 64x32
            if (texH >= 64) {
                mAlexLeftArm = new Cuboid(-7, -4, -8, 4, -2, 2, 32, 48, 3, 12, 4, texW, texH, false);
            } else {
                mAlexLeftArm = new Cuboid(-7, -4, -8, 4, -2, 2, 40, 16, 3, 12, 4, texW, texH, true);
            }

            // Right Leg: size 4x12x4. Bounds: X[0, 4], Y[-20, -8], Z[-2, 2]
            mRightLeg = new Cuboid(0, 4, -20, -8, -2, 2, 0, 16, 4, 12, 4, texW, texH, false);

            // Left Leg: mirrored from Right Leg if 64x32
            if (texH >= 64) {
                mLeftLeg = new Cuboid(-4, 0, -20, -8, -2, 2, 16, 48, 4, 12, 4, texW, texH, false);
            } else {
                mLeftLeg = new Cuboid(-4, 0, -20, -8, -2, 2, 0, 16, 4, 12, 4, texW, texH, true);
            }
        }

        private void rebuildCape(int capeW, int capeH) {
            // Cape: size 10x16x1. Bounds: X[-5, 5], Y[-12, 4], Z[2f, 3f]
            mCape = new Cuboid(-5, 5, -12, 4, 2f, 3f, 0, 0, 10, 16, 1, capeW, capeH, false);
        }

        private static class Cuboid {
            public FloatBuffer vertexBuffer;
            public FloatBuffer uvBuffer;
            public int vertexCount;

            public Cuboid(float x1, float x2, float y1, float y2, float z1, float z2,
                          int uStart, int vStart, int dx, int dy, int dz, int texW, int texH, boolean mirror) {
                
                float[] vertices = new float[36 * 3];
                float[] uvs = new float[36 * 2];

                // Front (Z = z2)
                addFace(vertices, uvs, 0, 0,
                        x1, y2, z2, x1, y1, z2, x2, y1, z2, x2, y2, z2,
                        uStart + dz, vStart + dz, dx, dy, texW, texH, mirror);

                // Back (Z = z1)
                addFace(vertices, uvs, 18, 12,
                        x2, y2, z1, x2, y1, z1, x1, y1, z1, x1, y2, z1,
                        uStart + dz + dx + dz, vStart + dz, dx, dy, texW, texH, mirror);

                // Left (X = x1) (Player's Right side)
                addFace(vertices, uvs, 36, 24,
                        x1, y2, z1, x1, y1, z1, x1, y1, z2, x1, y2, z2,
                        uStart, vStart + dz, dz, dy, texW, texH, mirror);

                // Right (X = x2) (Player's Left side)
                addFace(vertices, uvs, 54, 36,
                        x2, y2, z2, x2, y1, z2, x2, y1, z1, x2, y2, z1,
                        uStart + dz + dx, vStart + dz, dz, dy, texW, texH, mirror);

                // Top (Y = y2)
                addFace(vertices, uvs, 72, 48,
                        x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1,
                        uStart + dz, vStart, dx, dz, texW, texH, mirror);

                // Bottom (Y = y1)
                addFace(vertices, uvs, 90, 60,
                        x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2,
                        uStart + dz + dx, vStart, dx, dz, texW, texH, mirror);

                vertexCount = 36;

                ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
                byteBuf.order(ByteOrder.nativeOrder());
                vertexBuffer = byteBuf.asFloatBuffer();
                vertexBuffer.put(vertices);
                vertexBuffer.position(0);

                ByteBuffer uvBuf = ByteBuffer.allocateDirect(uvs.length * 4);
                uvBuf.order(ByteOrder.nativeOrder());
                uvBuffer = uvBuf.asFloatBuffer();
                uvBuffer.put(uvs);
                uvBuffer.position(0);
            }

            private void addFace(float[] vertices, float[] uvs, int vIdx, int uIdx,
                                 float xA, float yA, float zA,
                                 float xB, float yB, float zB,
                                 float xC, float yC, float zC,
                                 float xD, float yD, float zD,
                                 int uStart, int vStart, int dx, int dy, int texW, int texH, boolean mirror) {
                
                // Triangle 1
                vertices[vIdx] = xA; vertices[vIdx+1] = yA; vertices[vIdx+2] = zA;
                vertices[vIdx+3] = xB; vertices[vIdx+4] = yB; vertices[vIdx+5] = zB;
                vertices[vIdx+6] = xC; vertices[vIdx+7] = yC; vertices[vIdx+8] = zC;
                
                // Triangle 2
                vertices[vIdx+9] = xA; vertices[vIdx+10] = yA; vertices[vIdx+11] = zA;
                vertices[vIdx+12] = xC; vertices[vIdx+13] = yC; vertices[vIdx+14] = zC;
                vertices[vIdx+15] = xD; vertices[vIdx+16] = yD; vertices[vIdx+17] = zD;

                float u1 = (float) uStart / texW;
                float v1 = (float) vStart / texH;
                float u2 = (float) (uStart + dx) / texW;
                float v2 = (float) (vStart + dy) / texH;

                if (mirror) {
                    float temp = u1;
                    u1 = u2;
                    u2 = temp;
                }

                // UV assignments
                uvs[uIdx] = u1; uvs[uIdx+1] = v1;
                uvs[uIdx+2] = u1; uvs[uIdx+3] = v2;
                uvs[uIdx+4] = u2; uvs[uIdx+5] = v2;

                uvs[uIdx+6] = u1; uvs[uIdx+7] = v1;
                uvs[uIdx+8] = u2; uvs[uIdx+9] = v2;
                uvs[uIdx+10] = u2; uvs[uIdx+11] = v1;
            }
        }
    }
}

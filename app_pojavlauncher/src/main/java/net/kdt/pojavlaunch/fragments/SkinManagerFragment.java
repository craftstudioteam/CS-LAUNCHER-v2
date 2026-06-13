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

        private Cuboid mHead, mHeadLayer;
        private Cuboid mTorso, mTorsoLayer;
        private Cuboid mRightArm, mRightArmLayer;
        private Cuboid mLeftArm, mLeftArmLayer;
        private Cuboid mRightLeg, mRightLegLayer;
        private Cuboid mLeftLeg, mLeftLegLayer;
        private Cuboid mCape;

        private int mLastTexW = 0;
        private int mLastTexH = 0;
        private int mLastCapeW = 0;
        private int mLastCapeH = 0;
        private boolean mLastSlim = false;

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
            mLastTexW = 0; mLastTexH = 0;
            mLastCapeW = 0; mLastCapeH = 0;
            mHead = mHeadLayer = mTorso = mTorsoLayer = null;
            mRightArm = mRightArmLayer = mLeftArm = mLeftArmLayer = null;
            mRightLeg = mRightLegLayer = mLeftLeg = mLeftLegLayer = mCape = null;
        }

        @Override
        public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
            GLES20.glClearColor(0.05f, 0.06f, 0.08f, 1.0f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthFunc(GLES20.GL_LEQUAL);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);

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
            Matrix.orthoM(mProjectionMatrix, 0, -ratio * 18f, ratio * 18f, -19f, 19f, 0.1f, 100.0f);
        }

        @Override
        public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            synchronized (this) {
                if (mSkinTextureNeedsUpdate) {
                    if (mSkinTextureId != 0) GLES20.glDeleteTextures(1, new int[]{mSkinTextureId}, 0);
                    if (mPendingSkinBitmap != null) mSkinTextureId = loadGLTexture(mPendingSkinBitmap);
                    mSkinTextureNeedsUpdate = false;
                }
                if (mCapeTextureNeedsUpdate) {
                    if (mCapeTextureId != 0) GLES20.glDeleteTextures(1, new int[]{mCapeTextureId}, 0);
                    if (mPendingCapeBitmap != null) mCapeTextureId = loadGLTexture(mPendingCapeBitmap);
                    mCapeTextureNeedsUpdate = false;
                }
            }

            if (mSkinTextureId == 0) return;

            if (mPendingSkinBitmap != null) {
                checkRebuildCuboids(mPendingSkinBitmap.getWidth(), mPendingSkinBitmap.getHeight(), mIsSlim);
            }
            if (mCapeTextureId != 0 && mPendingCapeBitmap != null) {
                if (mCape == null || mLastCapeW != mPendingCapeBitmap.getWidth() || mLastCapeH != mPendingCapeBitmap.getHeight()) {
                    mLastCapeW = mPendingCapeBitmap.getWidth();
                    mLastCapeH = mPendingCapeBitmap.getHeight();
                    // Cape is authored relative to its own local origin so we can position it precisely
                    mCape = new Cuboid(0, 0, 0, -5, 5, -16, 0, 0, 1, 0, 0, 10, 16, 1, mLastCapeW, mLastCapeH, true, 0f);
                }
            } else {
                mCape = null;
            }

            Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 40f, 0f, 0f, 0f, 0f, 1.0f, 0f);
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.rotateM(mModelMatrix, 0, mAngleY, 1f, 0f, 0f);
            Matrix.rotateM(mModelMatrix, 0, mAngleX, 0f, 1f, 0f);

            GLES20.glUseProgram(mProgram);
            
            // Draw Passes
            GLES20.glDisable(GLES20.GL_BLEND);
            drawPart(mHead, mModelMatrix, mSkinTextureId);
            drawPart(mTorso, mModelMatrix, mSkinTextureId);
            drawPart(mRightArm, mModelMatrix, mSkinTextureId);
            drawPart(mLeftArm, mModelMatrix, mSkinTextureId);
            drawPart(mRightLeg, mModelMatrix, mSkinTextureId);
            drawPart(mLeftLeg, mModelMatrix, mSkinTextureId);

            // Draw Cape: attach to the torso/upper-back pivot and inherit the same body/model root.
            if (mCape != null && mCapeTextureId != 0) {
                float[] capeMatrix = new float[16];
                System.arraycopy(mModelMatrix, 0, capeMatrix, 0, 16);
                final float capePivotX = 0f;
                final float capePivotY = 8f;
                final float capePivotZ = -2.0f; // Attached to the back of the torso
                
                Matrix.translateM(capeMatrix, 0, capePivotX, capePivotY, capePivotZ);
                // Rotate 180 degrees so the visible face points outward
                Matrix.rotateM(capeMatrix, 0, 180f, 0f, 1f, 0f);
                // Swing the bottom of the cape outward
                Matrix.rotateM(capeMatrix, 0, -10f, 1f, 0f, 0f);
                
                drawPart(mCape, capeMatrix, mCapeTextureId);
            }

            // Draw Outer Layers (Alpha Blending)
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            drawPart(mHeadLayer, mModelMatrix, mSkinTextureId);
            drawPart(mTorsoLayer, mModelMatrix, mSkinTextureId);
            drawPart(mRightArmLayer, mModelMatrix, mSkinTextureId);
            drawPart(mLeftArmLayer, mModelMatrix, mSkinTextureId);
            drawPart(mRightLegLayer, mModelMatrix, mSkinTextureId);
            drawPart(mLeftLegLayer, mModelMatrix, mSkinTextureId);
        }

        private void drawPart(Cuboid cuboid, float[] baseMatrix, int textureId) {
            if (cuboid == null || textureId == 0) return;
            
            float[] finalMvp = new float[16];
            float[] partModel = new float[16];
            System.arraycopy(baseMatrix, 0, partModel, 0, 16);
            
            Matrix.translateM(partModel, 0, cuboid.pX, cuboid.pY, cuboid.pZ);
            // Apply walking/swinging angles here if animated later
            
            float[] mv = new float[16];
            Matrix.multiplyMM(mv, 0, mViewMatrix, 0, partModel, 0);
            Matrix.multiplyMM(finalMvp, 0, mProjectionMatrix, 0, mv, 0);

            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, cuboid.vertexBuffer);

            GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
            GLES20.glVertexAttribPointer(mTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, cuboid.uvBuffer);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mTextureUniformHandle, 0);

            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, finalMvp, 0);
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

        private void checkRebuildCuboids(int texW, int texH, boolean slim) {
            if (texW == mLastTexW && texH == mLastTexH && slim == mLastSlim && mHead != null) return;
            mLastTexW = texW; mLastTexH = texH; mLastSlim = slim;
            boolean is64 = (texH >= 64);
            
            mHead = new Cuboid(0, 8, 0, -4, 4, 0, 8, -4, 4, 0, 0, 8, 8, 8, texW, texH, false, 0f);
            mHeadLayer = new Cuboid(0, 8, 0, -4, 4, 0, 8, -4, 4, 32, 0, 8, 8, 8, texW, texH, false, 0.5f);
            
            mTorso = new Cuboid(0, 8, 0, -4, 4, -12, 0, -2, 2, 16, 16, 8, 12, 4, texW, texH, false, 0f);
            mTorsoLayer = is64 ? new Cuboid(0, 8, 0, -4, 4, -12, 0, -2, 2, 16, 32, 8, 12, 4, texW, texH, false, 0.25f) : null;
            
            float armW = slim ? 3 : 4;
            float rArmPx = slim ? -5.5f : -6f;
            float lArmPx = slim ? 5.5f : 6f;
            
            mRightArm = new Cuboid(rArmPx, 8, 0, -armW/2, armW/2, -12, 0, -2, 2, 40, 16, (int)armW, 12, 4, texW, texH, false, 0f);
            mRightArmLayer = is64 ? new Cuboid(rArmPx, 8, 0, -armW/2, armW/2, -12, 0, -2, 2, 40, 32, (int)armW, 12, 4, texW, texH, false, 0.25f) : null;
            
            if (is64) {
                mLeftArm = new Cuboid(lArmPx, 8, 0, -armW/2, armW/2, -12, 0, -2, 2, 32, 48, (int)armW, 12, 4, texW, texH, false, 0f);
                mLeftArmLayer = new Cuboid(lArmPx, 8, 0, -armW/2, armW/2, -12, 0, -2, 2, 48, 48, (int)armW, 12, 4, texW, texH, false, 0.25f);
                
                mRightLeg = new Cuboid(-2, -4, 0, -2, 2, -12, 0, -2, 2, 0, 16, 4, 12, 4, texW, texH, false, 0f);
                mRightLegLayer = new Cuboid(-2, -4, 0, -2, 2, -12, 0, -2, 2, 0, 32, 4, 12, 4, texW, texH, false, 0.25f);
                
                mLeftLeg = new Cuboid(2, -4, 0, -2, 2, -12, 0, -2, 2, 16, 48, 4, 12, 4, texW, texH, false, 0f);
                mLeftLegLayer = new Cuboid(2, -4, 0, -2, 2, -12, 0, -2, 2, 0, 48, 4, 12, 4, texW, texH, false, 0.25f);
            } else {
                mLeftArm = new Cuboid(lArmPx, 8, 0, -armW/2, armW/2, -12, 0, -2, 2, 40, 16, (int)armW, 12, 4, texW, texH, true, 0f);
                mRightLeg = new Cuboid(-2, -4, 0, -2, 2, -12, 0, -2, 2, 0, 16, 4, 12, 4, texW, texH, false, 0f);
                mLeftLeg = new Cuboid(2, -4, 0, -2, 2, -12, 0, -2, 2, 0, 16, 4, 12, 4, texW, texH, true, 0f);
                mLeftArmLayer = mRightLegLayer = mLeftLegLayer = null;
            }
        }

        private static class Cuboid {
            public FloatBuffer vertexBuffer;
            public FloatBuffer uvBuffer;
            public int vertexCount = 36;
            public float pX, pY, pZ;

            public Cuboid(float pX, float pY, float pZ, float x1, float x2, float y1, float y2, float z1, float z2,
                          int uStart, int vStart, int dx, int dy, int dz, int texW, int texH, boolean mirror, float expand) {
                this.pX = pX; this.pY = pY; this.pZ = pZ;
                x1 -= expand; x2 += expand;
                y1 -= expand; y2 += expand;
                z1 -= expand; z2 += expand;
                
                float[] v = new float[36 * 3];
                float[] u = new float[36 * 2];

                // Front (Z = z2)
                addFace(v, u, 0, 0, x1, y2, z2, x1, y1, z2, x2, y1, z2, x2, y2, z2, uStart+dz, vStart+dz, dx, dy, texW, texH, mirror);
                // Back (Z = z1) (Left and Right x-coords swapped to face outward correctly)
                addFace(v, u, 18, 12, x2, y2, z1, x2, y1, z1, x1, y1, z1, x1, y2, z1, uStart+dz+dx+dz, vStart+dz, dx, dy, texW, texH, mirror);
                // Left side (X = x1)
                addFace(v, u, 36, 24, x1, y2, z1, x1, y1, z1, x1, y1, z2, x1, y2, z2, uStart, vStart+dz, dz, dy, texW, texH, mirror);
                // Right side (X = x2)
                addFace(v, u, 54, 36, x2, y2, z2, x2, y1, z2, x2, y1, z1, x2, y2, z1, uStart+dz+dx, vStart+dz, dz, dy, texW, texH, mirror);
                // Top (Y = y2)
                addFace(v, u, 72, 48, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, uStart+dz, vStart, dx, dz, texW, texH, mirror);
                // Bottom (Y = y1)
                addFace(v, u, 90, 60, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, uStart+dz+dx, vStart, dx, dz, texW, texH, mirror);

                ByteBuffer bb = ByteBuffer.allocateDirect(v.length * 4);
                bb.order(ByteOrder.nativeOrder());
                vertexBuffer = bb.asFloatBuffer();
                vertexBuffer.put(v).position(0);

                ByteBuffer ub = ByteBuffer.allocateDirect(u.length * 4);
                ub.order(ByteOrder.nativeOrder());
                uvBuffer = ub.asFloatBuffer();
                uvBuffer.put(u).position(0);
            }

            private void addFace(float[] v, float[] u, int vi, int ui,
                                 float xA, float yA, float zA, float xB, float yB, float zB,
                                 float xC, float yC, float zC, float xD, float yD, float zD,
                                 int us, int vs, int dx, int dy, int tw, int th, boolean mirror) {
                v[vi]=xA; v[vi+1]=yA; v[vi+2]=zA; v[vi+3]=xB; v[vi+4]=yB; v[vi+5]=zB; v[vi+6]=xC; v[vi+7]=yC; v[vi+8]=zC;
                v[vi+9]=xA; v[vi+10]=yA; v[vi+11]=zA; v[vi+12]=xC; v[vi+13]=yC; v[vi+14]=zC; v[vi+15]=xD; v[vi+16]=yD; v[vi+17]=zD;

                float u1 = (float)us/tw, v1 = (float)vs/th, u2 = (float)(us+dx)/tw, v2 = (float)(vs+dy)/th;
                if (mirror) { float t=u1; u1=u2; u2=t; }
                u[ui]=u1; u[ui+1]=v1; u[ui+2]=u1; u[ui+3]=v2; u[ui+4]=u2; u[ui+5]=v2;
                u[ui+6]=u1; u[ui+7]=v1; u[ui+8]=u2; u[ui+9]=v2; u[ui+10]=u2; u[ui+11]=v1;
            }
        }
    }
}

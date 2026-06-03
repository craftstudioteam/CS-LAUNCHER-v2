package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.modloaders.InstalledModAdapter;
import net.kdt.pojavlaunch.modloaders.LocalPackAdapter;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.RTSpinnerAdapter;
import net.kdt.pojavlaunch.multirt.Runtime;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.profiles.ProfileIconCache;
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog;
import net.kdt.pojavlaunch.utils.CropperUtils;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.net.Uri;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import org.apache.commons.io.IOUtils;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;

public class ProfileEditorFragment extends Fragment implements CropperUtils.CropperListener{
    public static final String TAG = "ProfileEditorFragment";
    public static final String DELETED_PROFILE = "deleted_profile";

    private String mProfileKey;
    private MinecraftProfile mTempProfile = null;
    private String mValueToConsume = "";
    private Button mSaveButton, mDeleteButton, mControlSelectButton, mGameDirButton, mVersionSelectButton;
    private ImageButton mModsAddButton, mResourcePacksFolder, mShaderPacksFolder, mResourcePacksImport, mShaderPacksImport;
    private RecyclerView mModsRecycler, mResourcePacksRecycler, mShaderPacksRecycler;
    private TextView mModsEmpty, mResourcePacksEmpty, mShaderPacksEmpty;
    private Spinner mDefaultRuntime, mDefaultRenderer;
    private EditText mDefaultName, mDefaultJvmArgument;
    private TextView mDefaultPath, mDefaultVersion, mDefaultControl;
    private ImageView mProfileIcon;
    private final ActivityResultLauncher<?> mCropperLauncher = CropperUtils.registerCropper(this, this);

    private final ActivityResultLauncher<String> mResourcePackPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> handleImport(uri, "resourcepacks")
    );

    private final ActivityResultLauncher<String> mShaderPackPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> handleImport(uri, "shaderpacks")
    );

    private List<String> mRenderNames;

    public ProfileEditorFragment(){
        super(R.layout.fragment_profile_editor);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Paths, which can be changed
        String value = (String) ExtraCore.consumeValue(ExtraConstants.FILE_SELECTOR);
        if(value != null){
            if(mValueToConsume.equals(FileSelectorFragment.BUNDLE_SELECT_FOLDER)){
                mTempProfile.gameDir = value;
            }else{
                mTempProfile.controlFile = value;
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bindViews(view);

        Tools.RenderersList renderersList = Tools.getCompatibleRenderers(view.getContext());
        mRenderNames = renderersList.rendererIds;
        List<String> renderList = new ArrayList<>(renderersList.rendererDisplayNames.length + 1);
        renderList.addAll(Arrays.asList(renderersList.rendererDisplayNames));
        renderList.add(view.getContext().getString(R.string.global_default));
        mDefaultRenderer.setAdapter(new ArrayAdapter<>(getContext(), R.layout.item_simple_list_1, renderList));

        // Set up behaviors
        mSaveButton.setOnClickListener(v -> {
            ProfileIconCache.dropIcon(mProfileKey);
            save();
            Fragment parentFrag = getParentFragment();
            if (parentFrag instanceof MainMenuFragment) {
                MainMenuFragment mmf = (MainMenuFragment) parentFrag;
                mmf.clearRightPane();
                mmf.reloadSpinner();
            } else {
                Tools.backToMainMenu(requireActivity());
            }
        });

        mDeleteButton.setOnClickListener(v -> {
            if(LauncherProfiles.mainProfileJson.profiles.size() > 1){
                ProfileIconCache.dropIcon(mProfileKey);
                LauncherProfiles.mainProfileJson.profiles.remove(mProfileKey);
                LauncherProfiles.write();
                ExtraCore.setValue(ExtraConstants.REFRESH_VERSION_SPINNER, ProfileEditorFragment.DELETED_PROFILE);
            }
            Fragment parentFrag = getParentFragment();
            if (parentFrag instanceof MainMenuFragment) {
                MainMenuFragment mmf = (MainMenuFragment) parentFrag;
                mmf.clearRightPane();
                // Reload spinner now so deleted profile is gone immediately
                mmf.reloadSpinner();
            } else {
                Tools.removeCurrentFragment(requireActivity());
            }
        });


        View.OnClickListener gameDirListener = getGameDirListener();
        mGameDirButton.setOnClickListener(gameDirListener);
        mDefaultPath.setOnClickListener(gameDirListener);

        View.OnClickListener controlSelectListener = getControlSelectListener();
        mControlSelectButton.setOnClickListener(controlSelectListener);
        mDefaultControl.setOnClickListener(controlSelectListener);

        // Setup the expendable list behavior
        View.OnClickListener versionSelectListener = getVersionSelectListener();
        mVersionSelectButton.setOnClickListener(versionSelectListener);
        mDefaultVersion.setOnClickListener(versionSelectListener);

        // Set up the icon change click listener
        mProfileIcon.setOnClickListener(v -> CropperUtils.startCropper(mCropperLauncher));

        mModsAddButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(ManageModsFragment.BUNDLE_PROFILE_KEY, mProfileKey);
            navigateToFragment(ManageModsFragment.class, ManageModsFragment.TAG, args);
        });

        mResourcePacksFolder.setOnClickListener(v -> {
            File gameDir = Tools.getGameDirPath(mTempProfile);
            Tools.openPath(v.getContext(), new File(gameDir, "resourcepacks"), false);
        });

        mShaderPacksFolder.setOnClickListener(v -> {
            File gameDir = Tools.getGameDirPath(mTempProfile);
            Tools.openPath(v.getContext(), new File(gameDir, "shaderpacks"), false);
        });

        mResourcePacksImport.setOnClickListener(v -> mResourcePackPicker.launch("*/*"));
        mShaderPacksImport.setOnClickListener(v -> mShaderPackPicker.launch("*/*"));

        loadValues(LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, ""), view.getContext());
    }

    private void handleImport(Uri uri, String subDir) {
        if (uri == null) return;
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            File gameDir = Tools.getGameDirPath(mTempProfile);
            File destDir = new File(gameDir, subDir);
            if (!destDir.exists()) destDir.mkdirs();

            String fileName = Tools.getFileName(requireContext(), uri);
            if (fileName == null) fileName = "imported_" + System.currentTimeMillis() + ".zip";
            
            File destFile = new File(destDir, fileName);
            try (FileOutputStream os = new FileOutputStream(destFile)) {
                IOUtils.copy(is, os);
            }
            Toast.makeText(getContext(), "Imported successfully!", Toast.LENGTH_SHORT).show();
            setupPacksLists(); // Refresh
        } catch (Exception e) {
            Toast.makeText(getContext(), "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Navigate to a fragment — stays inside the right pane when running as a child fragment. */
    private void navigateToFragment(Class<? extends Fragment> fragmentClass, String tag, Bundle args) {
        Fragment parent = getParentFragment();
        if (parent instanceof MainMenuFragment) {
            ((MainMenuFragment) parent).openChildPane(fragmentClass, tag, args);
        } else {
            Tools.swapFragment(requireActivity(), fragmentClass, tag, args);
        }
    }

    private View.OnClickListener getGameDirListener() {
        return v -> {
            Bundle bundle = new Bundle(2);
            bundle.putBoolean(FileSelectorFragment.BUNDLE_SELECT_FOLDER, true);
            bundle.putString(FileSelectorFragment.BUNDLE_ROOT_PATH, Tools.DIR_GAME_HOME);
            bundle.putBoolean(FileSelectorFragment.BUNDLE_SHOW_FILE, false);
            mValueToConsume = FileSelectorFragment.BUNDLE_SELECT_FOLDER;

            navigateToFragment(FileSelectorFragment.class, FileSelectorFragment.TAG, bundle);
        };
    }

    private View.OnClickListener getControlSelectListener() {
        return v -> {
            Bundle bundle = new Bundle(3);
            bundle.putBoolean(FileSelectorFragment.BUNDLE_SELECT_FOLDER, false);
            bundle.putString(FileSelectorFragment.BUNDLE_ROOT_PATH, Tools.CTRLMAP_PATH);
            mValueToConsume = FileSelectorFragment.BUNDLE_SELECT_FILE;

            navigateToFragment(FileSelectorFragment.class, FileSelectorFragment.TAG, bundle);
        };
    }

    private View.OnClickListener getVersionSelectListener() {
        return v -> VersionSelectorDialog.open(v.getContext(), false, (id, snapshot)-> {
            mTempProfile.lastVersionId = id;
            mDefaultVersion.setText(id);
        });
    }


    private void loadValues(@NonNull String profile, @NonNull Context context){
        if(mTempProfile == null){
            mTempProfile = getProfile(profile);
        }
        // TODO: Remove this jank when it's not relevant anymore
        // Shitty hack to make OSMZink smoothly transition into kopper
        if ("vulkan_zink".equals(mTempProfile.pojavRendererName)) mTempProfile.pojavRendererName = "opengles3_desktopgl_zink_kopper";
        mProfileIcon.setImageDrawable(
                ProfileIconCache.fetchIcon(getResources(), mProfileKey, mTempProfile.icon)
        );

        // Runtime spinner
        List<Runtime> runtimes = MultiRTUtils.getInstalledRuntimes();
        int jvmIndex = runtimes.indexOf(new Runtime("<Default>"));
        if (mTempProfile.javaDir != null) {
            String selectedRuntime = mTempProfile.javaDir.substring(Tools.LAUNCHERPROFILES_RTPREFIX.length());
            int nindex = runtimes.indexOf(new Runtime(selectedRuntime));
            if (nindex != -1) jvmIndex = nindex;
        }
        mDefaultRuntime.setAdapter(new RTSpinnerAdapter(context, runtimes));
        if(jvmIndex == -1) jvmIndex = runtimes.size() - 1;
        mDefaultRuntime.setSelection(jvmIndex);

        // Renderer spinner
        int rendererIndex = mDefaultRenderer.getAdapter().getCount() - 1;
        if(mTempProfile.pojavRendererName != null) {
            int nindex = mRenderNames.indexOf(mTempProfile.pojavRendererName);
            if(nindex != -1) rendererIndex = nindex;
        }
        mDefaultRenderer.setSelection(rendererIndex);

        mDefaultVersion.setText(mTempProfile.lastVersionId);
        mDefaultJvmArgument.setText(mTempProfile.javaArgs == null ? "" : mTempProfile.javaArgs);
        mDefaultName.setText(mTempProfile.name);
        mDefaultPath.setText(mTempProfile.gameDir == null ? "" : mTempProfile.gameDir);
        mDefaultControl.setText(mTempProfile.controlFile == null ? "" : mTempProfile.controlFile);

        setupPacksLists();
    }

    private void setupPacksLists() {
        File gameDir = Tools.getGameDirPath(mTempProfile);
        File modsDir = new File(gameDir, "mods");
        File resourcePacksDir = new File(gameDir, "resourcepacks");
        File shaderPacksDir = new File(gameDir, "shaderpacks");

        mModsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mModsRecycler.setAdapter(new InstalledModAdapter(modsDir, isEmpty -> 
            mModsEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE)));

        mResourcePacksRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mResourcePacksRecycler.setAdapter(new LocalPackAdapter(resourcePacksDir, isEmpty -> 
            mResourcePacksEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE)));

        mShaderPacksRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mShaderPacksRecycler.setAdapter(new LocalPackAdapter(shaderPacksDir, isEmpty -> 
            mShaderPacksEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE)));
    }

    private MinecraftProfile getProfile(@NonNull String profile){
        MinecraftProfile minecraftProfile;
        if(getArguments() == null) {
            // EDGE CASE: User leaves Pojav in background. Pojav gets terminated in the background.
            // Current selected fragment and its arguments are saved.
            // User returns to Pojav. Android restarts process and reinitializes fragment without
            // going to the main screen. mainProfileJson and profiles left uninitialized, which
            // results in a crash.
            // Reload the profiles to avoid this edge case.
            LauncherProfiles.load();
            MinecraftProfile originalProfile = LauncherProfiles.mainProfileJson.profiles.get(profile);
            // EDGE CASE: User edits the JSON, so the profile that was edited no longer exists.
            // Create a brand new profile as a fallback for this case.
            if(originalProfile != null) minecraftProfile = new MinecraftProfile(originalProfile);
            else minecraftProfile = MinecraftProfile.createTemplate();
            mProfileKey = profile;
        }else{
            minecraftProfile = MinecraftProfile.createTemplate();
            mProfileKey = LauncherProfiles.getFreeProfileKey();
        }
        return minecraftProfile;
    }


    private void bindViews(@NonNull View view){
        mDefaultControl = view.findViewById(R.id.vprof_editor_ctrl_spinner);
        mDefaultRuntime = view.findViewById(R.id.vprof_editor_spinner_runtime);
        mDefaultRenderer = view.findViewById(R.id.vprof_editor_profile_renderer);
        mDefaultVersion = view.findViewById(R.id.vprof_editor_version_spinner);

        mDefaultPath = view.findViewById(R.id.vprof_editor_path);
        mDefaultName = view.findViewById(R.id.vprof_editor_profile_name);
        mDefaultJvmArgument = view.findViewById(R.id.vprof_editor_jre_args);

        mSaveButton = view.findViewById(R.id.vprof_editor_save_button);
        mDeleteButton = view.findViewById(R.id.vprof_editor_delete_button);
        mControlSelectButton = view.findViewById(R.id.vprof_editor_ctrl_button);
        mVersionSelectButton = view.findViewById(R.id.vprof_editor_version_button);
        mGameDirButton = view.findViewById(R.id.vprof_editor_path_button);
        mProfileIcon = view.findViewById(R.id.vprof_editor_profile_icon);

        mModsAddButton = view.findViewById(R.id.vprof_editor_mods_add);
        mResourcePacksFolder = view.findViewById(R.id.vprof_editor_resource_packs_folder);
        mShaderPacksFolder = view.findViewById(R.id.vprof_editor_shader_packs_folder);
        mModsRecycler = view.findViewById(R.id.vprof_editor_mods_recycler);
        mResourcePacksRecycler = view.findViewById(R.id.vprof_editor_resource_packs_recycler);
        mShaderPacksRecycler = view.findViewById(R.id.vprof_editor_shader_packs_recycler);
        mModsEmpty = view.findViewById(R.id.vprof_editor_mods_empty);
        mResourcePacksEmpty = view.findViewById(R.id.vprof_editor_resource_packs_empty);
        mShaderPacksEmpty = view.findViewById(R.id.vprof_editor_shader_packs_empty);
        mResourcePacksImport = view.findViewById(R.id.vprof_editor_resource_packs_import);
        mShaderPacksImport = view.findViewById(R.id.vprof_editor_shader_packs_import);
    }

    private void save(){
        //First, check for potential issues in the inputs
        mTempProfile.lastVersionId = mDefaultVersion.getText().toString();
        mTempProfile.controlFile = mDefaultControl.getText().toString();
        mTempProfile.name = mDefaultName.getText().toString();
        mTempProfile.javaArgs = mDefaultJvmArgument.getText().toString()
                .replaceAll("[\r\n]+", " ")
                .trim();
        mTempProfile.gameDir = mDefaultPath.getText().toString();

        if(mTempProfile.controlFile.isEmpty()) mTempProfile.controlFile = null;
        if(mTempProfile.javaArgs.isEmpty()) mTempProfile.javaArgs = null;
        if(mTempProfile.gameDir.isEmpty()) mTempProfile.gameDir = null;

        Runtime selectedRuntime = (Runtime) mDefaultRuntime.getSelectedItem();
        mTempProfile.javaDir = (selectedRuntime.name.equals("<Default>") || selectedRuntime.versionString == null)
                ? null : Tools.LAUNCHERPROFILES_RTPREFIX + selectedRuntime.name;

        if(mDefaultRenderer.getSelectedItemPosition() == mRenderNames.size()) mTempProfile.pojavRendererName = null;
        else mTempProfile.pojavRendererName = mRenderNames.get(mDefaultRenderer.getSelectedItemPosition());


        LauncherProfiles.mainProfileJson.profiles.put(mProfileKey, mTempProfile);
        LauncherProfiles.write();
        ExtraCore.setValue(ExtraConstants.REFRESH_VERSION_SPINNER, mProfileKey);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Always drop the icon cache when leaving, even without saving,
        // so the next reloadProfiles() re-fetches with correct bounds.
        if (mProfileKey != null) {
            ProfileIconCache.dropIcon(mProfileKey);
            // Reload the spinner so the icon redraws at correct size immediately
            // (covers Android back button path where reloadSpinner() isn't called explicitly)
            Fragment parent = getParentFragment();
            if (parent instanceof MainMenuFragment) {
                ((MainMenuFragment) parent).reloadSpinner();
            }
        }
    }

    @Override
    public void onCropped(Bitmap contentBitmap) {
        mProfileIcon.setImageBitmap(contentBitmap);
        Log.i("bitmap", "w="+contentBitmap.getWidth() +" h="+contentBitmap.getHeight());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (Base64OutputStream base64OutputStream = new Base64OutputStream(byteArrayOutputStream, Base64.NO_WRAP)) {
            contentBitmap.compress(
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R ?
                    // On Android < 30, there was no distinction between "lossy" and "lossless",
                    // and the type is picked by the quality parameter. We set the quality to 60.
                    // so it should be lossy,
                    Bitmap.CompressFormat.WEBP:
                    // On Android >= 30, we can explicitly specify that we want lossy compression
                    // with the visual quality of 60.
                    Bitmap.CompressFormat.WEBP_LOSSY,
                60,
                base64OutputStream
            );
            base64OutputStream.flush();
            byteArrayOutputStream.flush();
        }catch (IOException e) {
            Tools.showErrorRemote(e);
            return;
        }
        String iconLine = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
        mTempProfile.icon = "data:image/webp;base64," + iconLine;
    }

    @Override
    public void onFailed(Exception exception) {
        Tools.showErrorRemote(exception);
    }
}
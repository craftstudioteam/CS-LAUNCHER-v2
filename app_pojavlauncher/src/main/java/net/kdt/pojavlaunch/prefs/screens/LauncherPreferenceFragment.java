package net.kdt.pojavlaunch.prefs.screens;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
import static net.kdt.pojavlaunch.Tools.getTotalDeviceMemory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.FastClientHelper;
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.PojavProfile;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.fragments.GamepadMapperFragment;
import net.kdt.pojavlaunch.fragments.MainMenuFragment;
import net.kdt.pojavlaunch.fragments.RightPaneHomeFragment;
import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog;
import net.kdt.pojavlaunch.prefs.CustomToggleView;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.prefs.SettingsSaveManager;
import net.kdt.pojavlaunch.theme.ThemeManager;
import net.kdt.pojavlaunch.utils.GLInfoUtils;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fr.spse.gamepad_remapper.Remapper;

public class LauncherPreferenceFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private SettingsAdapter mAdapter;
    private SharedPreferences mDraftPrefs;
    private boolean mIsDirty = false;
    private String mCategoryName = null;

    // JRE result launcher
    private final ActivityResultLauncher<Object> mVmInstallLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("xz"), (data) -> {
                if (data != null) Tools.installRuntimeFromUri(getContext(), data);
            });

    // Custom background picker launcher
    private final ActivityResultLauncher<String> mImagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) copyImageToBgFile(uri);
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mCategoryName = args.getString("category", null);
        }

        // Copy main preferences into draft preference storage on root page load
        if (savedInstanceState == null && mCategoryName == null) {
            initializeDraft(requireContext());
        }
        mDraftPrefs = SettingsSaveManager.getDraftPrefs(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_custom_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ThemeManager.applyToPrefView(view);
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = view.findViewById(R.id.settings_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Dynamic Header titles
        TextView tvTitle = view.findViewById(R.id.settings_title);
        TextView tvSubtitle = view.findViewById(R.id.settings_subtitle);
        if (tvTitle != null) {
            tvTitle.setText(mCategoryName != null ? mCategoryName : "Settings");
        }
        if (tvSubtitle != null) {
            tvSubtitle.setText(mCategoryName != null ? getCategorySubtitle(mCategoryName) : "Customize your launcher experience");
        }

        // Header Back Navigation
        View backButton = view.findViewById(R.id.settings_back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.onBackPressed();
                }
            });
        }

        // Float Save Changes Bar Click Handler
        Button saveBtn = view.findViewById(R.id.btn_save_settings);
        if (saveBtn != null) {
            saveBtn.setOnClickListener(v -> {
                saveChanges();
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                        .start();
            });
        }

        setupSettingsList();
        updateSaveBar();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        updateSaveBar();
    }

    @Override
    public void onDestroyView() {
        if (mAdapter != null) {
            mAdapter.cleanup();
        }
        // Save pending changes on exit
        if (mIsDirty) {
            SettingsSaveManager.commitChanges(getContext());
            LauncherPreferences.loadPreferences(getContext());
            mIsDirty = false;
        }
        super.onDestroyView();
    }

    private void initializeDraft(Context context) {
        SharedPreferences mainPrefs = context.getSharedPreferences("cslauncher_settings", Context.MODE_PRIVATE);
        SharedPreferences draftPrefs = SettingsSaveManager.getDraftPrefs(context);
        SharedPreferences.Editor editor = draftPrefs.edit();
        editor.clear();
        for (Map.Entry<String, ?> entry : mainPrefs.getAll().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            }
        }
        editor.commit();
    }

    private String getCategorySubtitle(String category) {
        switch (category) {
            case "Launcher Settings": return "Configure language, updates, and downloads";
            case "Video & Graphics": return "Configure renderers, resolution, and VSync options";
            case "Controls": return "Customize touch overlays, cursors, and gyroscope controls";
            case "Java Runtime": return "Manage memory allocations, JREs, and Java options";
            case "Audio": return "Adjust volume levels and sound output parameters";
            case "Account": return "Check active profile accounts and skin logs";
            case "Experimental": return "Test launcher orientations, wall-papers, and colors";
            case "Advanced": return "Perform debug clears and database resets";
            case "Miscellaneous": return "Library verifications, system drivers, and in-game capes";
            default: return "Settings options";
        }
    }

    private void setupSettingsList() {
        List<SettingCategory> categories = new ArrayList<>();

        if (mCategoryName == null) {
            // Root Menu Categories List
            List<SettingItem> rootItems = new ArrayList<>();
            rootItems.add(new SettingItem("cat_launcher", SettingItem.TYPE_CATEGORY_LINK, "Launcher Settings", "Configure language, updates, and downloads", "Launcher Settings"));
            rootItems.add(new SettingItem("cat_video", SettingItem.TYPE_CATEGORY_LINK, "Video & Graphics", "Configure renderers, resolution, and VSync options", "Video & Graphics"));
            rootItems.add(new SettingItem("cat_controls", SettingItem.TYPE_CATEGORY_LINK, "Controls", "Customize touch overlays, cursors, and gyroscope controls", "Controls"));
            rootItems.add(new SettingItem("cat_java", SettingItem.TYPE_CATEGORY_LINK, "Java Runtime", "Manage memory allocations, JREs, and Java options", "Java Runtime"));
            rootItems.add(new SettingItem("cat_audio", SettingItem.TYPE_CATEGORY_LINK, "Audio", "Adjust volume levels and sound output parameters", "Audio"));
            rootItems.add(new SettingItem("cat_account", SettingItem.TYPE_CATEGORY_LINK, "Account", "Check active profile accounts and skin logs", "Account"));
            rootItems.add(new SettingItem("cat_experimental", SettingItem.TYPE_CATEGORY_LINK, "Experimental", "Test launcher orientations, wall-papers, and colors", "Experimental"));
            rootItems.add(new SettingItem("cat_advanced", SettingItem.TYPE_CATEGORY_LINK, "Advanced", "Perform debug clears and database resets", "Advanced"));
            rootItems.add(new SettingItem("cat_misc", SettingItem.TYPE_CATEGORY_LINK, "Miscellaneous", "Library verifications, system drivers, and in-game capes", "Miscellaneous"));
            categories.add(new SettingCategory("Settings Categories", rootItems));
        } else {
            // Subcategory Pages Detail
            switch (mCategoryName) {
                case "Launcher Settings":
                    List<SettingItem> launcherItems = new ArrayList<>();
                    launcherItems.add(new SettingItem("fastclient_preference", SettingItem.TYPE_CUSTOM_FASTCLIENT, "", "", null));
                    launcherItems.add(new SettingItem("force_english", SettingItem.TYPE_SWITCH, getString(R.string.preference_force_english_title), getString(R.string.preference_force_english_description), false));
                    launcherItems.add(new SettingItem("notification_permission_request", SettingItem.TYPE_SWITCH, getString(R.string.preference_ask_for_notification_title), getString(R.string.preference_ask_for_notification_description), false));
                    launcherItems.add(new SettingItem("microphone_permission_request", SettingItem.TYPE_SWITCH, getString(R.string.preference_ask_for_microphone_title), getString(R.string.preference_ask_for_microphone_description), false));
                    launcherItems.add(new SettingItem("downloadSource", SettingItem.TYPE_DROPDOWN, getString(R.string.preference_download_source_title), getString(R.string.preference_download_source_description), "default").setDropdownOptions(new String[]{"Default", "Mirror (China)"}, new String[]{"default", "china"}));
                    launcherItems.add(new SettingItem("verifyManifest", SettingItem.TYPE_SWITCH, getString(R.string.preference_verify_manifest_title), getString(R.string.preference_verify_manifest_description), true));
                    categories.add(new SettingCategory("Launcher Configurations", launcherItems));
                    break;

                case "Video & Graphics":
                    List<SettingItem> graphicsItems = new ArrayList<>();
                    graphicsItems.add(new SettingItem("mg_renderer_setting_angle", SettingItem.TYPE_DROPDOWN, getString(R.string.mg_renderer_angle), "Select backend ANGLE configuration", "1").setDropdownOptions(new String[]{"Vulkan", "OpenGL (System)", "OpenGLES"}, new String[]{"1", "2", "3"}));
                    graphicsItems.add(new SettingItem("mg_renderer_setting_multidraw", SettingItem.TYPE_DROPDOWN, getString(R.string.mg_renderer_multidraw), "Select multidraw emulation style", "0").setDropdownOptions(new String[]{"None", "Emulate glMultiDraw", "glMultiDraw Indirect"}, new String[]{"0", "1", "2"}));
                    graphicsItems.add(new SettingItem("mg_renderer_setting_fsr", SettingItem.TYPE_DROPDOWN, getString(R.string.mg_renderer_title_fsr), "Enable AMD FSR scaler", "0").setDropdownOptions(new String[]{"Disabled", "25%", "50%", "75%", "100%"}, new String[]{"0", "1", "2", "3", "4"}));
                    graphicsItems.add(new SettingItem("mg_renderer_setting_errorSetting", SettingItem.TYPE_DROPDOWN, getString(R.string.mg_renderer_title_errorSetting), "Configure error handling in GL", "0").setDropdownOptions(new String[]{"Report", "Ignore"}, new String[]{"0", "1"}));
                    graphicsItems.add(new SettingItem("mg_renderer_setting_timerQueryExt", SettingItem.TYPE_SWITCH, getString(R.string.mg_renderer_title_timerQueryExt), getString(R.string.mg_renderer_summary_timerQueryExt), false));
                    graphicsItems.add(new SettingItem("mg_renderer_setting_angleDepthClearFixMode", SettingItem.TYPE_SWITCH, getString(R.string.mg_renderer_title_angleDepthClearFixMode), getString(R.string.mg_renderer_summary_angleDepthClearFixMode), false));
                    graphicsItems.add(new SettingItem("mg_renderer_setting_gl43exts", SettingItem.TYPE_SWITCH, getString(R.string.mg_renderer_title_gl43exts), getString(R.string.mg_renderer_summary_gl43exts), false));
                    graphicsItems.add(new SettingItem("mg_renderer_computeShaderext", SettingItem.TYPE_SWITCH, getString(R.string.mg_renderer_title_computeShaderext), getString(R.string.mg_renderer_summary_computeShaderext), false));
                    graphicsItems.add(new SettingItem("mg_renderer_dsaExt", SettingItem.TYPE_SWITCH, getString(R.string.mg_renderer_title_dsaExt), getString(R.string.mg_renderer_summary_dsaExt), false));
                    graphicsItems.add(new SettingItem("mg_renderer_multidrawCompute", SettingItem.TYPE_SWITCH, getString(R.string.mg_renderer_title_multidrawCompute), getString(R.string.mg_renderer_summary_multidrawCompute), false));
                    graphicsItems.add(new SettingItem("mg_renderer_setting_glsl_cache_size", SettingItem.TYPE_INPUT, getString(R.string.mg_renderer_glsl_cache), "Input cache size limit", "128"));
                    graphicsItems.add(new SettingItem("ignoreNotch", SettingItem.TYPE_SWITCH, getString(R.string.mcl_setting_title_ignore_notch), getString(R.string.mcl_setting_subtitle_ignore_notch), false));
                    graphicsItems.add(new SettingItem("resolutionRatio", SettingItem.TYPE_SLIDER, getString(R.string.mcl_setting_title_resolution_scaler), getString(R.string.mcl_setting_subtitle_resolution_scaler), 100).setSliderConfig(25, 100, 5, " %"));
                    graphicsItems.add(new SettingItem("sustainedPerformance", SettingItem.TYPE_SWITCH, getString(R.string.preference_sustained_performance_title), getString(R.string.preference_sustained_performance_description), false));
                    graphicsItems.add(new SettingItem("alternate_surface", SettingItem.TYPE_SWITCH, getString(R.string.mcl_setting_title_use_surface_view), getString(R.string.mcl_setting_subtitle_use_surface_view), true));
                    graphicsItems.add(new SettingItem("force_vsync", SettingItem.TYPE_SWITCH, getString(R.string.preference_force_vsync_title), getString(R.string.preference_force_vsync_description), false));
                    graphicsItems.add(new SettingItem("vsync_in_zink", SettingItem.TYPE_SWITCH, getString(R.string.preference_vsync_in_zink_title), getString(R.string.preference_vsync_in_zink_description), true));
                    categories.add(new SettingCategory("Graphics Settings", graphicsItems));
                    break;

                case "Controls":
                    List<SettingItem> controlsItems = new ArrayList<>();
                    controlsItems.add(new SettingItem("buttonscale", SettingItem.TYPE_SLIDER, "Button Scale", "Adjust sized layout overlays", 100).setSliderConfig(20, 200, 10, " %"));
                    controlsItems.add(new SettingItem("mousescale", SettingItem.TYPE_SLIDER, "Mouse Cursor Scale", "Adjust virtual mouse cursor size", 100).setSliderConfig(25, 300, 25, " %"));
                    controlsItems.add(new SettingItem("mousespeed", SettingItem.TYPE_SLIDER, "Mouse Speed", "Adjust virtual cursor movement sensitivity", 100).setSliderConfig(25, 300, 25, " %"));
                    controlsItems.add(new SettingItem("disableGestures", SettingItem.TYPE_SWITCH, "Disable Gestures", "Disable gesture-based navigation overrides", false));
                    controlsItems.add(new SettingItem("timeLongPressTrigger", SettingItem.TYPE_SLIDER, "Long Press Trigger Delay", "Hold duration for virtual clicks", 300).setSliderConfig(100, 1000, 50, " ms"));
                    controlsItems.add(new SettingItem("disableDoubleTap", SettingItem.TYPE_SWITCH, "Swipe to Swap Hand", "Quick swipe swaps secondary item", false));
                    controlsItems.add(new SettingItem("mouse_start", SettingItem.TYPE_SWITCH, "Virtual Mouse Auto-Start", "Turn pointer cursor on automatically on game start", false));
                    controlsItems.add(new SettingItem("always_grab_mouse", SettingItem.TYPE_SWITCH, "Always Grab Mouse", "Keep mouse focus locked inside the window", false));
                    controlsItems.add(new SettingItem("enableGyro", SettingItem.TYPE_SWITCH, "Enable Gyroscope Sensor", "Use device motion sensors for controls", false));
                    controlsItems.add(new SettingItem("gyroSensitivity", SettingItem.TYPE_SLIDER, "Gyro Sensitivity", "Adjust motion sensitivity scaling", 100).setSliderConfig(10, 500, 10, " %"));
                    controlsItems.add(new SettingItem("gyroSampleRate", SettingItem.TYPE_SLIDER, "Gyro Sample Rate", "Adjust sensor update delay", 16).setSliderConfig(10, 100, 2, " ms"));
                    controlsItems.add(new SettingItem("gyroSmoothing", SettingItem.TYPE_SWITCH, "Gyro Smoothing", "Filter out tiny micro-shakes", true));
                    controlsItems.add(new SettingItem("gyroInvertX", SettingItem.TYPE_SWITCH, "Gyro Invert X Axis", "Invert horizontal rotation controls", false));
                    controlsItems.add(new SettingItem("gyroInvertY", SettingItem.TYPE_SWITCH, "Gyro Invert Y Axis", "Invert vertical rotation controls", false));
                    controlsItems.add(new SettingItem("gamepad_deadzone_scale", SettingItem.TYPE_SLIDER, "Gamepad Deadzone", "Analog joystick drift ignore range", 100).setSliderConfig(0, 100, 5, " %"));
                    controlsItems.add(new SettingItem("gamepadPassthru", SettingItem.TYPE_SWITCH, "Gamepad SDL Passthrough", "Route controller input directly through SDL", true));
                    controlsItems.add(new SettingItem("gamepadPassthruForced", SettingItem.TYPE_SWITCH, "Force Gamepad SDL Passthrough", "Detect unsupported controllers", false));
                    controlsItems.add(new SettingItem("forceEnableTouchController", SettingItem.TYPE_SWITCH, "Force Touch Controls", "Force visual control pads", false));
                    controlsItems.add(new SettingItem("touchControllerVibrateLength", SettingItem.TYPE_SLIDER, "Vibration length", "Haptic duration on touch presses", 100).setSliderConfig(0, 500, 10, " ms"));
                    controlsItems.add(new SettingItem("gamepad_remap_action", SettingItem.TYPE_ACTION, "Remap Gamepad Controller", "Map gamepad key layouts", null).setAction(() -> Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, "GAMEPAD_MAPPER", null)));
                    controlsItems.add(new SettingItem("gamepad_wipe_action", SettingItem.TYPE_ACTION, "Wipe Controller Map", "Clear custom gamepad configurations", null).setAction(() -> {
                        Remapper.wipePreferences(getContext());
                        Toast.makeText(getContext(), R.string.preference_controller_map_wiped, Toast.LENGTH_SHORT).show();
                    }));
                    categories.add(new SettingCategory("Control Settings", controlsItems));
                    break;

                case "Java Runtime":
                    List<SettingItem> javaItems = new ArrayList<>();
                    javaItems.add(new SettingItem("install_jre", SettingItem.TYPE_ACTION, getString(R.string.multirt_title), getString(R.string.multirt_subtitle), null).setAction(this::openMultiRTDialog));
                    javaItems.add(new SettingItem("javaArgs", SettingItem.TYPE_INPUT, getString(R.string.mcl_setting_title_javaargs), getString(R.string.mcl_setting_subtitle_javaargs), ""));

                    int deviceRam = getTotalDeviceMemory(requireContext());
                    int maxRAM = (Architecture.is32BitsDevice() || deviceRam < 2048) ? Math.min(1024, deviceRam) : deviceRam - (deviceRam < 3064 ? 800 : 1024);
                    javaItems.add(new SettingItem("allocation", SettingItem.TYPE_SLIDER, getString(R.string.mcl_memory_allocation), getString(R.string.mcl_memory_allocation_subtitle), 1024).setSliderConfig(256, maxRAM, 128, " MB"));

                    javaItems.add(new SettingItem("disable_autojre_select", SettingItem.TYPE_SWITCH, "Disable automatic JRE selection", "Stops automatic selection of which runtime to use", false));
                    javaItems.add(new SettingItem("java_sandbox", SettingItem.TYPE_SWITCH, getString(R.string.mcl_setting_java_sandbox), getString(R.string.mcl_setting_java_sandbox_subtitle), true));
                    categories.add(new SettingCategory("Java Configurations", javaItems));
                    break;

                case "Audio":
                    List<SettingItem> audioItems = new ArrayList<>();
                    audioItems.add(new SettingItem("enable_audio", SettingItem.TYPE_SWITCH, "Enable Game Sound", "Allow game instances to play audio", true));
                    audioItems.add(new SettingItem("launcher_volume", SettingItem.TYPE_SLIDER, "Launcher Master Volume", "Default volume control for launched games", 80).setSliderConfig(0, 100, 5, " %"));
                    audioItems.add(new SettingItem("use_opensles", SettingItem.TYPE_SWITCH, "Use OpenSL ES Backend", "Enable low-latency high performance sound library", false));
                    categories.add(new SettingCategory("Audio Settings", audioItems));
                    break;

                case "Account":
                    List<SettingItem> accountItems = new ArrayList<>();
                    MinecraftAccount activeAccount = PojavProfile.getCurrentProfileContent(requireContext(), null);
                    String accountName = activeAccount != null ? activeAccount.username : "None";
                    accountItems.add(new SettingItem("active_profile_info", SettingItem.TYPE_INFO, "Active Account Profile", accountName, null));
                    categories.add(new SettingCategory("Account Configurations", accountItems));
                    break;

                case "Experimental":
                    List<SettingItem> expItems = new ArrayList<>();
                    expItems.add(new SettingItem("dump_shaders", SettingItem.TYPE_SWITCH, getString(R.string.preference_shader_dump_title), getString(R.string.preference_shader_dump_description), false));
                    expItems.add(new SettingItem("bigCoreAffinity", SettingItem.TYPE_SWITCH, getString(R.string.preference_force_big_core_title), getString(R.string.preference_force_big_core_desc), false));
                    expItems.add(new SettingItem("force_landscape", SettingItem.TYPE_SWITCH, getString(R.string.preference_force_landscape_title), getString(R.string.preference_force_landscape_summary), false));
                    expItems.add(new SettingItem("enable_bg_gradient", SettingItem.TYPE_SWITCH, getString(R.string.preference_bg_gradient_title), getString(R.string.preference_bg_gradient_summary), false));
                    expItems.add(new SettingItem("set_custom_launcher_bg", SettingItem.TYPE_ACTION, getString(R.string.preference_set_custom_bg_title), getString(R.string.preference_set_custom_bg_summary), null).setAction(() -> mImagePickerLauncher.launch("image/*")));
                    expItems.add(new SettingItem("remove_custom_launcher_bg", SettingItem.TYPE_ACTION, getString(R.string.preference_remove_custom_bg_title), getString(R.string.preference_remove_custom_bg_summary), null).setAction(() -> {
                        File bgFile = new File(RightPaneHomeFragment.CUSTOM_BG_PATH);
                        if (bgFile.exists()) bgFile.delete();
                        notifyHomeFragmentBgChanged();
                        Toast.makeText(requireContext(), R.string.preference_custom_bg_removed, Toast.LENGTH_SHORT).show();
                    }));
                    expItems.add(new SettingItem("colour_theme_presets", SettingItem.TYPE_ACTION, getString(R.string.preference_colour_presets_title), getString(R.string.preference_colour_presets_summary), null).setAction(this::showPresetDialog));
                    categories.add(new SettingCategory("Experimental Settings", expItems));
                    break;

                case "Advanced":
                    List<SettingItem> advItems = new ArrayList<>();
                    advItems.add(new SettingItem("clear_cache_files", SettingItem.TYPE_ACTION, "Clear Shader & Temporary Caches", "Free up storage by deleting temporary rendering files", null).setAction(() -> {
                        try {
                            clearCacheLocal(requireContext());
                            Toast.makeText(getContext(), "Caches cleared successfully", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Failed to clear cache", Toast.LENGTH_SHORT).show();
                        }
                    }));
                    advItems.add(new SettingItem("reset_all_settings", SettingItem.TYPE_ACTION, "Reset Launcher Settings", "Restore factory defaults for all configuration profiles", null).setAction(() -> {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Reset Settings")
                                .setMessage("Are you sure you want to reset all settings to defaults?")
                                .setPositiveButton("Reset", (dialog, which) -> {
                                    mDraftPrefs.edit().clear().commit();
                                    requireContext().getSharedPreferences("cslauncher_settings", Context.MODE_PRIVATE).edit().clear().commit();
                                    LauncherPreferences.loadPreferences(requireContext());
                                    Toast.makeText(requireContext(), "Settings reset successfully", Toast.LENGTH_SHORT).show();
                                    requireActivity().recreate();
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    }));
                    categories.add(new SettingCategory("Advanced Actions", advItems));
                    break;

                case "Miscellaneous":
                    List<SettingItem> miscItems = new ArrayList<>();
                    miscItems.add(new SettingItem("checkLibraries", SettingItem.TYPE_SWITCH, getString(R.string.mcl_setting_check_libraries), getString(R.string.mcl_setting_check_libraries_subtitle), true));
                    miscItems.add(new SettingItem("arc_capes", SettingItem.TYPE_SWITCH, getString(R.string.arc_capes_title), getString(R.string.arc_capes_desc), false));
                    miscItems.add(new SettingItem("zinkPreferSystemDriver", SettingItem.TYPE_SWITCH, getString(R.string.preference_vulkan_driver_system_title), getString(R.string.preference_vulkan_driver_system_description), false));
                    categories.add(new SettingCategory("Miscellaneous Settings", miscItems));
                    break;
            }
        }

        mAdapter = new SettingsAdapter(categories, mDraftPrefs);
        mRecyclerView.setAdapter(mAdapter);
    }

    private boolean isItemVisible(SettingItem item, SharedPreferences draftPrefs) {
        if ("ignoreNotch".equals(item.key)) {
            return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && LauncherPreferences.PREF_NOTCH_SIZE > 0;
        }
        if ("sustainedPerformance".equals(item.key)) {
            return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N;
        }
        if ("force_vsync".equals(item.key)) {
            return draftPrefs.getBoolean("alternate_surface", true);
        }
        if ("timeLongPressTrigger".equals(item.key)) {
            return !draftPrefs.getBoolean("disableGestures", false);
        }
        if ("gyroSensitivity".equals(item.key) || "gyroSampleRate".equals(item.key) ||
                "gyroSmoothing".equals(item.key) || "gyroInvertX".equals(item.key) || "gyroInvertY".equals(item.key)) {
            return Tools.deviceSupportsGyro(getContext()) && draftPrefs.getBoolean("enableGyro", false);
        }
        if ("zinkPreferSystemDriver".equals(item.key)) {
            PackageManager pm = getContext().getPackageManager();
            boolean supportsTurnip = Tools.checkVulkanSupport(pm) && GLInfoUtils.getGlInfo().isAdreno();
            return supportsTurnip;
        }
        return true;
    }

    private void markDirty() {
        mIsDirty = true;
        updateSaveBar();
    }

    private void updateSaveBar() {
        if (getView() == null) return;
        View bar = getView().findViewById(R.id.unsaved_changes_bar);
        if (bar == null) return;
        Button saveBtn = getView().findViewById(R.id.btn_save_settings);
        TextView statusText = getView().findViewById(R.id.save_status_text);

        if (mIsDirty) {
            if (bar.getVisibility() != View.VISIBLE) {
                bar.setVisibility(View.VISIBLE);
                bar.setAlpha(0f);
                bar.setTranslationY(80f);
                bar.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                        .start();
            }
            if (saveBtn != null) {
                saveBtn.setEnabled(true);
                saveBtn.setAlpha(1f);
            }
            if (statusText != null) {
                statusText.setText("● Unsaved Changes");
                statusText.setTextColor(Color.parseColor("#FFA500"));
            }
        } else {
            if (bar.getVisibility() == View.VISIBLE) {
                bar.animate()
                        .alpha(0f)
                        .translationY(40f)
                        .setDuration(200)
                        .withEndAction(() -> bar.setVisibility(View.GONE))
                        .start();
            }
            if (saveBtn != null) {
                saveBtn.setEnabled(false);
                saveBtn.setAlpha(0.5f);
            }
            if (statusText != null) {
                statusText.setText("\u2713 Settings Saved");
                statusText.setTextColor(Color.parseColor("#39FF14"));
            }
        }
    }

    private void saveChanges() {
        if (!mIsDirty) return;

        Context context = getContext();
        if (context == null) return;

        SharedPreferences mainPrefs = context.getSharedPreferences("cslauncher_settings", Context.MODE_PRIVATE);

        boolean prevEnglish = mainPrefs.getBoolean("force_english", false);
        boolean prevGradient = mainPrefs.getBoolean(ThemeManager.KEY_GRADIENT, false);
        boolean prevLandscape = mainPrefs.getBoolean("force_landscape", false);

        // Commit modifications to main SharedPreferences
        SettingsSaveManager.commitChanges(context);

        // FORCE SAVE VERIFICATION
        boolean success = verifySavedSettings(context);

        if (success) {
            LauncherPreferences.loadPreferences(context);

            try {
                LauncherPreferences.writeMGRendererSettings();
            } catch (Exception e) {
                Log.e("LauncherPreferenceFragment", "Failed to write renderer configs", e);
            }

            mIsDirty = false;
            updateSaveBar();

            Toast.makeText(context, "\u2713 Settings Saved", Toast.LENGTH_SHORT).show();

            boolean newEnglish = mainPrefs.getBoolean("force_english", false);
            boolean newGradient = mainPrefs.getBoolean(ThemeManager.KEY_GRADIENT, false);
            boolean newLandscape = mainPrefs.getBoolean("force_landscape", false);

            if (prevEnglish != newEnglish || prevGradient != newGradient) {
                requireActivity().recreate();
            } else if (prevLandscape != newLandscape) {
                requireActivity().setRequestedOrientation(
                        newLandscape ? SCREEN_ORIENTATION_SENSOR_LANDSCAPE : SCREEN_ORIENTATION_UNSPECIFIED);
            }
        } else {
            Toast.makeText(context, "Failed to save settings", Toast.LENGTH_LONG).show();
        }
    }

    private boolean verifySavedSettings(Context context) {
        SharedPreferences mainPrefs = context.getSharedPreferences("cslauncher_settings", Context.MODE_PRIVATE);
        SharedPreferences draftPrefs = SettingsSaveManager.getDraftPrefs(context);
        Map<String, ?> mainMap = mainPrefs.getAll();
        Map<String, ?> draftMap = draftPrefs.getAll();

        boolean allMatch = true;
        for (Map.Entry<String, ?> entry : draftMap.entrySet()) {
            String key = entry.getKey();
            Object draftVal = entry.getValue();
            // Skip verification for link keys which aren't settings keys
            if (key.startsWith("cat_") || "gamepad_remap_action".equals(key) || "gamepad_wipe_action".equals(key) || "clear_cache_files".equals(key) || "reset_all_settings".equals(key) || "active_profile_info".equals(key) || "install_jre".equals(key) || "fastclient_preference".equals(key)) {
                continue;
            }
            if (!mainMap.containsKey(key)) {
                Log.e("SettingsVerification", "Failed to save settings: key " + key + " is missing from disk storage!");
                allMatch = false;
                continue;
            }
            Object mainVal = mainMap.get(key);
            if (draftVal == null) {
                if (mainVal != null) {
                    Log.e("SettingsVerification", "Failed to save settings: key " + key + " mismatch (draft=null, disk=" + mainVal + ")");
                    allMatch = false;
                }
            } else if (!draftVal.equals(mainVal)) {
                Log.e("SettingsVerification", "Failed to save settings: key " + key + " mismatch (draft=" + draftVal + ", disk=" + mainVal + ")");
                allMatch = false;
            }
        }
        return allMatch;
    }

    private void openMultiRTDialog() {
        MultiRTConfigDialog dialogScreen = new MultiRTConfigDialog();
        dialogScreen.prepare(getContext(), mVmInstallLauncher);
        dialogScreen.show();
    }

    private void copyImageToBgFile(@NonNull Uri uri) {
        File bgFile = new File(RightPaneHomeFragment.CUSTOM_BG_PATH);
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(bgFile)) {
            if (in == null) throw new Exception("Cannot open URI");
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            notifyHomeFragmentBgChanged();
            Toast.makeText(requireContext(), R.string.preference_custom_bg_set_success, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            if (bgFile.exists()) bgFile.delete();
            Toast.makeText(requireContext(), R.string.preference_custom_bg_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void showPresetDialog() {
        ThemeManager.Preset[] presets = ThemeManager.PRESETS;
        String[] labels = new String[presets.length + 1];
        for (int i = 0; i < presets.length; i++) labels[i] = presets[i].name;
        labels[presets.length] = getString(R.string.preference_colour_reset);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.preference_colour_presets_title)
                .setItems(labels, (dialog, which) -> {
                    if (which < presets.length) {
                        ThemeManager.applyPreset(presets[which]);
                    } else {
                        ThemeManager.resetToDefault();
                    }
                    requireActivity().recreate();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void notifyHomeFragmentBgChanged() {
        MainMenuFragment mmf = (MainMenuFragment) requireActivity()
                .getSupportFragmentManager()
                .findFragmentByTag("ROOT");
        if (mmf == null) return;
        RightPaneHomeFragment home = (RightPaneHomeFragment) mmf
                .getChildFragmentManager()
                .findFragmentByTag(RightPaneHomeFragment.TAG);
        if (home != null) home.reloadBackground();
    }

    private void clearCacheLocal(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteRecursive(dir);
        } catch (Exception e) {
            Log.e("LauncherPreferenceFragment", "Failed to clear cache", e);
        }
    }

    private void deleteRecursive(File file) {
        if (file == null) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    // ── RecyclerView Adapter & ViewHolder ─────────────────────────────────────

    private class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.CategoryViewHolder> {

        private final List<SettingCategory> mCategories;
        private final SharedPreferences mPrefs;
        private final SharedPreferences.OnSharedPreferenceChangeListener mListener;

        public SettingsAdapter(List<SettingCategory> categories, SharedPreferences prefs) {
            this.mCategories = categories;
            this.mPrefs = prefs;
            this.mListener = (p, key) -> {
                // Ignore key updates during dragging to prevent reconstruction lag
            };
            this.mPrefs.registerOnSharedPreferenceChangeListener(mListener);
        }

        public void cleanup() {
            mPrefs.unregisterOnSharedPreferenceChangeListener(mListener);
        }

        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_settings_category, parent, false);
            return new CategoryViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            SettingCategory cat = mCategories.get(position);
            holder.categoryTitle.setText(cat.title);
            holder.container.removeAllViews();

            boolean hasVisibleItems = false;
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

            for (SettingItem item : cat.items) {
                if (!isItemVisible(item, mPrefs)) {
                    continue;
                }
                hasVisibleItems = true;

                View itemView;
                if (item.type == SettingItem.TYPE_CUSTOM_FASTCLIENT) {
                    itemView = inflater.inflate(R.layout.fragment_settings_fastclient, holder.container, false);
                    FastClientHelper.setup(itemView, holder.itemView.getContext(), getChildFragmentManager());
                    holder.container.addView(itemView);
                    continue;
                }

                if (item.type == SettingItem.TYPE_CATEGORY_LINK) {
                    itemView = inflater.inflate(R.layout.item_setting_button, holder.container, false);
                    TextView tvTitle = itemView.findViewById(R.id.setting_title);
                    TextView tvSummary = itemView.findViewById(R.id.setting_summary);

                    tvTitle.setText(item.title);
                    tvSummary.setText(item.summary);

                    itemView.setOnClickListener(v -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("category", item.categoryLinkTarget);
                        Tools.swapFragment(
                                requireActivity(),
                                LauncherPreferenceFragment.class,
                                "SETTINGS_" + item.categoryLinkTarget,
                                bundle,
                                R.anim.slide_in_right,
                                R.anim.slide_out_left,
                                R.anim.slide_in_left,
                                R.anim.slide_out_right
                        );
                    });

                } else if (item.type == SettingItem.TYPE_SWITCH) {
                    itemView = inflater.inflate(R.layout.item_setting_toggle, holder.container, false);
                    TextView tvTitle = itemView.findViewById(R.id.setting_title);
                    TextView tvSummary = itemView.findViewById(R.id.setting_summary);
                    CustomToggleView toggle = itemView.findViewById(R.id.setting_toggle);

                    tvTitle.setText(item.title);
                    tvSummary.setText(item.summary);

                    boolean isChecked = mPrefs.getBoolean(item.key, (Boolean) item.defaultValue);
                    toggle.setChecked(isChecked, false);

                    itemView.setOnClickListener(v -> toggle.toggle());

                    toggle.setOnCheckedChangeListener((view, checkedVal) -> {
                        if ("notification_permission_request".equals(item.key)) {
                            Activity act = getActivity();
                            if (act instanceof LauncherActivity) {
                                LauncherActivity la = (LauncherActivity) act;
                                if (checkedVal) {
                                    la.askForNotificationPermission(() -> {
                                        toggle.setChecked(la.checkForNotificationPermission(), false);
                                    });
                                }
                            }
                        } else if ("microphone_permission_request".equals(item.key)) {
                            Activity act = getActivity();
                            if (act instanceof LauncherActivity) {
                                LauncherActivity la = (LauncherActivity) act;
                                if (checkedVal) {
                                    la.askForMicrophonePermission(() -> {
                                        toggle.setChecked(la.checkForMicrophonePermission(), false);
                                    });
                                }
                            }
                        } else {
                            mPrefs.edit().putBoolean(item.key, checkedVal).apply();
                            markDirty();
                        }

                        if (item.key.equals("enableGyro") || item.key.equals("disableGestures")
                                || item.key.equals("alternate_surface") || item.key.equals("mg_renderer_multidrawCompute")) {
                            mRecyclerView.post(() -> notifyDataSetChanged());
                        }
                    });

                } else if (item.type == SettingItem.TYPE_SLIDER) {
                    itemView = inflater.inflate(R.layout.item_setting_slider, holder.container, false);
                    TextView tvTitle = itemView.findViewById(R.id.setting_title);
                    TextView tvSummary = itemView.findViewById(R.id.setting_summary);
                    TextView tvVal = itemView.findViewById(R.id.setting_value_text);
                    SeekBar seekBar = itemView.findViewById(R.id.setting_seekbar);

                    tvTitle.setText(item.title);
                    tvSummary.setText(item.summary);

                    int curVal = mPrefs.getInt(item.key, (Integer) item.defaultValue);
                    tvVal.setText(curVal + item.unitSuffix);

                    seekBar.setMax((item.maxVal - item.minVal) / item.stepVal);
                    seekBar.setProgress((curVal - item.minVal) / item.stepVal);

                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                            int calculatedVal = item.minVal + progress * item.stepVal;
                            tvVal.setText(calculatedVal + item.unitSuffix);
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar sb) {}

                        @Override
                        public void onStopTrackingTouch(SeekBar sb) {
                            int finalVal = item.minVal + sb.getProgress() * item.stepVal;
                            mPrefs.edit().putInt(item.key, finalVal).apply();
                            markDirty();
                        }
                    });

                } else if (item.type == SettingItem.TYPE_DROPDOWN) {
                    itemView = inflater.inflate(R.layout.item_setting_dropdown, holder.container, false);
                    TextView tvTitle = itemView.findViewById(R.id.setting_title);
                    TextView tvSummary = itemView.findViewById(R.id.setting_summary);
                    TextView tvSpinner = itemView.findViewById(R.id.setting_spinner_text);

                    tvTitle.setText(item.title);
                    tvSummary.setText(item.summary);

                    String curVal = mPrefs.getString(item.key, (String) item.defaultValue);
                    int selIndex = 0;
                    for (int i = 0; i < item.dropdownValues.length; i++) {
                        if (Objects.equals(item.dropdownValues[i], curVal)) {
                            selIndex = i;
                            break;
                        }
                    }
                    tvSpinner.setText(item.dropdownEntries[selIndex]);

                    itemView.setOnClickListener(v -> {
                        new AlertDialog.Builder(holder.itemView.getContext())
                                .setTitle(item.title)
                                .setItems(item.dropdownEntries, (dialog, which) -> {
                                    String chosenVal = item.dropdownValues[which];
                                    mPrefs.edit().putString(item.key, chosenVal).apply();
                                    tvSpinner.setText(item.dropdownEntries[which]);
                                    markDirty();
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    });

                } else if (item.type == SettingItem.TYPE_INPUT) {
                    itemView = inflater.inflate(R.layout.item_setting_dropdown, holder.container, false);
                    TextView tvTitle = itemView.findViewById(R.id.setting_title);
                    TextView tvSummary = itemView.findViewById(R.id.setting_summary);
                    TextView tvSpinner = itemView.findViewById(R.id.setting_spinner_text);

                    tvTitle.setText(item.title);
                    tvSummary.setText(item.summary);

                    String curVal = mPrefs.getString(item.key, (String) item.defaultValue);
                    tvSpinner.setText(curVal != null && !curVal.isEmpty() ? curVal : "Default");

                    itemView.setOnClickListener(v -> {
                        Context ctx = holder.itemView.getContext();
                        EditText et = new EditText(ctx);
                        et.setInputType(InputType.TYPE_CLASS_TEXT);
                        et.setText(mPrefs.getString(item.key, (String) item.defaultValue));

                        new AlertDialog.Builder(ctx)
                                .setTitle(item.title)
                                .setView(et)
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                    String input = et.getText().toString();
                                    mPrefs.edit().putString(item.key, input).apply();
                                    tvSpinner.setText(input.isEmpty() ? "Default" : input);
                                    markDirty();
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    });

                } else if (item.type == SettingItem.TYPE_ACTION) {
                    itemView = inflater.inflate(R.layout.item_setting_button, holder.container, false);
                    TextView tvTitle = itemView.findViewById(R.id.setting_title);
                    TextView tvSummary = itemView.findViewById(R.id.setting_summary);

                    tvTitle.setText(item.title);
                    tvSummary.setText(item.summary);

                    itemView.setOnClickListener(v -> {
                        if (item.action != null) {
                            item.action.run();
                        }
                    });

                } else if (item.type == SettingItem.TYPE_INFO) {
                    itemView = inflater.inflate(R.layout.item_setting_dropdown, holder.container, false);
                    TextView tvTitle = itemView.findViewById(R.id.setting_title);
                    TextView tvSummary = itemView.findViewById(R.id.setting_summary);
                    TextView tvSpinner = itemView.findViewById(R.id.setting_spinner_text);

                    tvTitle.setText(item.title);
                    tvSummary.setText("");
                    tvSpinner.setText(String.valueOf(item.defaultValue));
                    tvSpinner.setBackground(null);
                    itemView.setClickable(false);
                    itemView.setFocusable(false);
                } else {
                    itemView = new View(holder.itemView.getContext());
                }

                holder.container.addView(itemView);
            }

            if (!hasVisibleItems) {
                holder.itemView.setVisibility(View.GONE);
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
                params.height = 0;
                params.topMargin = 0;
                params.bottomMargin = 0;
                holder.itemView.setLayoutParams(params);
            } else {
                holder.itemView.setVisibility(View.VISIBLE);
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.topMargin = (int) (8 * holder.itemView.getResources().getDisplayMetrics().density);
                params.bottomMargin = (int) (8 * holder.itemView.getResources().getDisplayMetrics().density);
                holder.itemView.setLayoutParams(params);
            }
        }

        @Override
        public int getItemCount() {
            return mCategories.size();
        }

        public class CategoryViewHolder extends RecyclerView.ViewHolder {
            TextView categoryTitle;
            LinearLayout container;

            public CategoryViewHolder(@NonNull View itemView) {
                super(itemView);
                categoryTitle = itemView.findViewById(R.id.category_title);
                container = itemView.findViewById(R.id.settings_list_container);
            }
        }
    }

    // ── Setting Data Models ───────────────────────────────────────────────────

    private static class SettingCategory {
        String title;
        List<SettingItem> items;

        public SettingCategory(String title, List<SettingItem> items) {
            this.title = title;
            this.items = items;
        }
    }

    private static class SettingItem {
        public static final int TYPE_SWITCH = 1;
        public static final int TYPE_SLIDER = 2;
        public static final int TYPE_DROPDOWN = 3;
        public static final int TYPE_ACTION = 4;
        public static final int TYPE_INFO = 5;
        public static final int TYPE_INPUT = 6;
        public static final int TYPE_CUSTOM_FASTCLIENT = 7;
        public static final int TYPE_CATEGORY_LINK = 8;

        String key;
        int type;
        String title;
        String summary;
        Object defaultValue;

        // Slider config
        int minVal;
        int maxVal;
        int stepVal;
        String unitSuffix;

        // Dropdown config
        String[] dropdownEntries;
        String[] dropdownValues;

        // Action config
        Runnable action;

        // Category link config
        String categoryLinkTarget;

        public SettingItem(String key, int type, String title, String summary, Object defaultValue) {
            this.key = key;
            this.type = type;
            this.title = title;
            this.summary = summary;
            this.defaultValue = defaultValue;
            if (type == TYPE_CATEGORY_LINK && defaultValue instanceof String) {
                this.categoryLinkTarget = (String) defaultValue;
            }
        }

        public SettingItem setSliderConfig(int min, int max, int step, String suffix) {
            this.minVal = min;
            this.maxVal = max;
            this.stepVal = step;
            this.unitSuffix = suffix;
            return this;
        }

        public SettingItem setDropdownOptions(String[] entries, String[] values) {
            this.dropdownEntries = entries;
            this.dropdownValues = values;
            return this;
        }

        public SettingItem setAction(Runnable r) {
            this.action = r;
            return this;
        }
    }
}
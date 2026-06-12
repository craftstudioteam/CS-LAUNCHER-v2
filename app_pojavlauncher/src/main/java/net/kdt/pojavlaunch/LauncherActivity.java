package net.kdt.pojavlaunch;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static net.kdt.pojavlaunch.Tools.hasNoOnlineProfileDialog;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import net.kdt.pojavlaunch.value.MinecraftAccount;
import net.kdt.pojavlaunch.fragments.ModsSearchFragment;
import net.kdt.pojavlaunch.fragments.CursorCustomizationFragment;
import net.kdt.pojavlaunch.fragments.SkinManagerFragment;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;

import com.kdt.mcgui.ProgressLayout;
import com.kdt.mcgui.mcAccountSpinner;

import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.extra.ExtraListener;
import net.kdt.pojavlaunch.fragments.MainMenuFragment;
import net.kdt.pojavlaunch.fragments.MicrosoftLoginFragment;
import net.kdt.pojavlaunch.fragments.SelectAuthFragment;
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.modloaders.modpacks.ModloaderInstallTracker;
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModLoader;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackInstaller;
import net.kdt.pojavlaunch.modloaders.modpacks.api.NotificationDownloadListener;
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.IconCacheJanitor;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;
import net.kdt.pojavlaunch.services.ProgressServiceKeeper;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.tasks.AsyncVersionList;
import net.kdt.pojavlaunch.tasks.MinecraftDownloader;
import net.kdt.pojavlaunch.utils.DateUtils;
import net.kdt.pojavlaunch.utils.NotificationUtils;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import android.view.Window;
import android.view.WindowManager;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LauncherActivity extends BaseActivity {
    public static final String SETTING_FRAGMENT_TAG = "SETTINGS_FRAGMENT";

    public final ActivityResultLauncher<Object> modInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data != null) Tools.launchModInstaller(this, data);
            });
    public final ActivityResultLauncher<Object> modpackImportLauncher =
            registerForActivityResult(new OpenDocumentWithExtension(new String[]{"zip", "mrpack"}), (data)->{
                if(data != null) {
                    PojavApplication.sExecutorService.execute(() -> {
                        try {
                            ModLoader loaderInfo = new CommonApi(getString(R.string.curseforge_api_key)).importModpack(this, data);
                            if (loaderInfo == null) return;
                            loaderInfo.getDownloadTask(new NotificationDownloadListener(this, loaderInfo)).run();
                        } catch (IOException e) {
                            Tools.showErrorRemote(this, R.string.modpack_install_download_failed, e);
                        } catch (IllegalArgumentException e) {
                            Tools.showError(this, R.string.not_modpack_file, e);
                        } catch (NoSuchAlgorithmException e) {
                            // Should literally never happen because SHA-1 is required Java spec
                            throw new RuntimeException(e);
                        }
                    });
                }
            });

    private mcAccountSpinner mAccountSpinner;
    private FragmentContainerView mFragmentView;
    private ImageButton mSettingsButton;
    private ProgressLayout mProgressLayout;
    private ProgressServiceKeeper mProgressServiceKeeper;
    private ModloaderInstallTracker mInstallTracker;
    private NotificationManager mNotificationManager;
    private ClientFeaturesManager mClientFeaturesManager;

    /* Allows to switch from one button "type" to another */
    private final FragmentManager.FragmentLifecycleCallbacks mFragmentCallbackListener = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            mSettingsButton.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), f instanceof MainMenuFragment
                    ? R.drawable.ic_menu_settings : R.drawable.ic_menu_home));
        }
    };

    /* Listener for the back button in settings */
    private final ExtraListener<String> mBackPreferenceListener = (key, value) -> {
        if(value.equals("true")) onBackPressed();
        return false;
    };

    /* Listener for the auth method selection screen */
    private final ExtraListener<Boolean> mSelectAuthMethod = (key, value) -> {
        Fragment fragment = getSupportFragmentManager().findFragmentById(mFragmentView.getId());
        // Allow starting the add account only from the main menu, should it be moved to fragment itself ?
        if(!(fragment instanceof MainMenuFragment)) return false;

        // In landscape two-pane mode, load into right pane; otherwise full-screen swap
        MainMenuFragment mmf = (MainMenuFragment) fragment;
        if (!mmf.tryOpenInRightPane(SelectAuthFragment.class, SelectAuthFragment.TAG, null)) {
            Tools.swapFragment(this, SelectAuthFragment.class, SelectAuthFragment.TAG, null);
        }
        return false;
    };

    /* Listener for the settings fragment */
    private final View.OnClickListener mSettingButtonListener = v -> {
        Fragment fragment = getSupportFragmentManager().findFragmentById(mFragmentView.getId());
        if (fragment instanceof MainMenuFragment) {
            MainMenuFragment mmf = (MainMenuFragment) fragment;
            // In two-pane landscape: if right pane already has content, pressing the
            // gear/home button pops back to home. If pane is at home, open settings.
            // Always open settings full-screen to match the new UI transformation
            Tools.swapFragment(this, LauncherPreferenceFragment.class, SETTING_FRAGMENT_TAG, null);
        } else {
            // Portrait: the settings button doubles as a home button when not on main menu
            Tools.backToMainMenu(this);
        }
    };

    private final ExtraListener<Boolean> mLaunchGameListener = (key, value) -> {
        if(mProgressLayout.hasProcesses()){
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return false;
        }

        String selectedProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE,"");
        if (LauncherProfiles.mainProfileJson == null || !LauncherProfiles.mainProfileJson.profiles.containsKey(selectedProfile)){
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show();
            return false;
        }
        MinecraftProfile prof = LauncherProfiles.mainProfileJson.profiles.get(selectedProfile);
        if (prof == null || prof.lastVersionId == null || "Unknown".equals(prof.lastVersionId)){
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show();
            return false;
        }

        if(mAccountSpinner.getSelectedAccount() == null){
            Toast.makeText(this, R.string.no_saved_accounts, Toast.LENGTH_LONG).show();
            ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true);
            return false;
        }
        String normalizedVersionId = AsyncMinecraftDownloader.normalizeVersionId(prof.lastVersionId);
        JMinecraftVersionList.Version mcVersion = AsyncMinecraftDownloader.getListedVersion(normalizedVersionId);

        // Do not load when is a modded version or older than minecraft 1.3 on demo account
        if (mAccountSpinner.getSelectedAccount().isDemo()) {
            boolean isOlderThan13 = true;

            if (mcVersion != null) {
                try {
                    isOlderThan13 = DateUtils.dateBefore(DateUtils.parseReleaseDate(mcVersion.releaseTime), 2012, 6, 22);
                } catch (ParseException ignored) {}
            }

            if (isOlderThan13) {
                hasNoOnlineProfileDialog(this, getString(R.string.global_error), getString(R.string.demo_versions_supported));
                return false;
            }
        }

        new MinecraftDownloader().start(
                this,
                mcVersion,
                normalizedVersionId,
                new ContextAwareDoneListener(this, normalizedVersionId)
        );
        return false;
    };

    private final TaskCountListener mDoubleLaunchPreventionListener = taskCount -> {
        // Hide the notification that starts the game if there are tasks executing.
        // Prevents the user from trying to launch the game with tasks ongoing.
        if(taskCount > 0) {
            Tools.runOnUiThread(() ->
                    mNotificationManager.cancel(NotificationUtils.NOTIFICATION_ID_GAME_START)
            );
        }
    };

    private ActivityResultLauncher<String> mRequestNotificationPermissionLauncher;
    private ActivityResultLauncher<String> mRequestMicrophonePermissionLauncher;
    private WeakReference<Runnable> mRequestNotificationPermissionRunnable;
    private WeakReference<Runnable> mRequestMicrophonePermissionRunnable;

    @Override
    protected boolean shouldIgnoreNotch() {
        return false;
    }

    @Override
    public boolean setFullscreen() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Edge-to-edge setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            );
        }

        // Apply saved colour theme before layout inflation
        setTheme(net.kdt.pojavlaunch.theme.ThemeManager.getSavedTheme());
        
        // Ensure landscape orientation
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        setContentView(R.layout.activity_pojav_launcher);
        
        // Handle window insets properly to prevent navigation bar space reservation
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content),
            (v, insets) -> WindowInsetsCompat.CONSUMED);
        FragmentManager fragmentManager = getSupportFragmentManager();
        // If we don't have a back stack root yet...
        if(fragmentManager.getBackStackEntryCount() < 1) {
            // Check if FastClient is enabled
            android.content.SharedPreferences p = getSharedPreferences("fastclient_prefs", android.content.Context.MODE_PRIVATE);
            boolean fcEnabled = p.getBoolean("fc_enabled", false);
            Class<? extends Fragment> rootFragment = fcEnabled ? net.kdt.pojavlaunch.fragments.FastClientHomeFragment.class : MainMenuFragment.class;

            // Manually add the first fragment to the backstack to get easily back to it
            fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .addToBackStack("ROOT")
                    .add(R.id.container_fragment, rootFragment, null, "ROOT").commit();
        }


        IconCacheJanitor.runJanitor();
        mRequestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isAllowed -> {
                    if(!isAllowed) handleNoNotificationPermission();
                    else {
                        Runnable runnable = Tools.getWeakReference(mRequestNotificationPermissionRunnable);
                        if(runnable != null) runnable.run();
                    }
                }
        );
        mRequestMicrophonePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isAllowed -> {
                    if(!isAllowed) handleNoNotificationPermission();
                    else {
                        Runnable runnable = Tools.getWeakReference(mRequestMicrophonePermissionRunnable);
                        if(runnable != null) runnable.run();
                    }
                }
        );
        getWindow().setBackgroundDrawable(null);
        bindViews();
        mClientFeaturesManager = new ClientFeaturesManager(this);
        setupNavButtons();
        checkNotificationPermission();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        ProgressKeeper.addTaskCountListener(mDoubleLaunchPreventionListener);
        ProgressKeeper.addTaskCountListener((mProgressServiceKeeper = new ProgressServiceKeeper(this)));

        mSettingsButton.setOnClickListener(mSettingButtonListener);
        ProgressKeeper.addTaskCountListener(mProgressLayout);
        ExtraCore.addExtraListener(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener);
        ExtraCore.addExtraListener(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod);

        ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME, mLaunchGameListener);

        new AsyncVersionList().getVersionList(versions -> ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versions), false);

        mInstallTracker = new ModloaderInstallTracker(this);

        mProgressLayout.observe(ProgressLayout.DOWNLOAD_MINECRAFT);
        mProgressLayout.observe(ProgressLayout.UNPACK_RUNTIME);
        mProgressLayout.observe(ProgressLayout.INSTALL_MODPACK);
        mProgressLayout.observe(ProgressLayout.AUTHENTICATE_MICROSOFT);
        mProgressLayout.observe(ProgressLayout.DOWNLOAD_VERSION_LIST);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextExecutor.setActivity(this);
        mInstallTracker.attach();
        updateNavSkinIcon();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ContextExecutor.clearActivity();
        mInstallTracker.detach();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(mFragmentCallbackListener, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgressLayout.cleanUpObservers();
        ProgressKeeper.removeTaskCountListener(mProgressLayout);
        ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.LAUNCH_GAME, mLaunchGameListener);

        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(mFragmentCallbackListener);
    }

    /** Custom implementation to feel more natural when a backstack isn't present */
    @Override
    public void onBackPressed() {
        MicrosoftLoginFragment fragment = (MicrosoftLoginFragment) getVisibleFragment(MicrosoftLoginFragment.TAG);
        if(fragment != null){
            if(fragment.canGoBack()){
                fragment.goBack();
                return;
            }
        }

        // If we are in settings, pop back to home
        if (getVisibleFragment(SETTING_FRAGMENT_TAG) != null) {
            getSupportFragmentManager().popBackStack();
            return;
        }

        // In landscape two-pane mode: if the right pane has content, pop it instead of exiting
        Fragment rootFrag = getVisibleFragment("ROOT");
        if (rootFrag instanceof MainMenuFragment) {
            MainMenuFragment mmf = (MainMenuFragment) rootFrag;
            if (mmf.isRightPaneActive()) {
                mmf.popRightPane();
                return;
            }
            finish();
            return;
        }

        // Default backstack behavior
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
            return;
        }

        finish();
    }

    @Override
    public void onAttachedToWindow() {
        LauncherPreferences.computeNotchSize(this);
    }

    @SuppressWarnings("SameParameterValue")
    private Fragment getVisibleFragment(String tag){
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if(fragment != null && fragment.isVisible()) {
            return fragment;
        }
        return null;
    }

    @SuppressWarnings("unused")
    private Fragment getVisibleFragment(int id){
        Fragment fragment = getSupportFragmentManager().findFragmentById(id);
        if(fragment != null && fragment.isVisible()) {
            return fragment;
        }
        return null;
    }

    private void checkNotificationPermission() {
        if(LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK ||
            checkForNotificationPermission()) {
            return;
        }

        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS)) {
            showNotificationPermissionReasoning();
            return;
        }
        askForNotificationPermission(null);
    }

    private void showNotificationPermissionReasoning() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notification_permission_dialog_title)
                .setMessage(R.string.notification_permission_dialog_text)
                .setPositiveButton(android.R.string.ok, (d, w) -> askForNotificationPermission(null))
                .setNegativeButton(android.R.string.cancel, (d, w)-> handleNoNotificationPermission())
                .show();
    }

    private void handleNoNotificationPermission() {
        LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK = true;
        LauncherPreferences.DEFAULT_PREF.edit()
                .putBoolean(LauncherPreferences.PREF_KEY_SKIP_NOTIFICATION_CHECK, true)
                .apply();
        Toast.makeText(this, R.string.notification_permission_toast, Toast.LENGTH_LONG).show();
    }

    public boolean checkForNotificationPermission() {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_DENIED;
    }
    public boolean checkForMicrophonePermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_DENIED;
    }

    public void askForNotificationPermission(Runnable onSuccessRunnable) {
        if(Build.VERSION.SDK_INT < 33) return;
        if(onSuccessRunnable != null) {
            mRequestNotificationPermissionRunnable = new WeakReference<>(onSuccessRunnable);
        }
        mRequestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    public void askForMicrophonePermission(Runnable onSuccessRunnable) {
        if(onSuccessRunnable != null) {
            mRequestMicrophonePermissionRunnable = new WeakReference<>(onSuccessRunnable);
        }
        mRequestMicrophonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }

    /** Stuff all the view boilerplate here */

    /** Wire up the landscape header bar navigation buttons. */
    private void setupNavButtons() {
        View navModStore       = findViewById(R.id.nav_mod_store);
        View navCustomControls  = findViewById(R.id.nav_custom_controls);
        View navCursor          = findViewById(R.id.nav_cursor);
        View navHome            = findViewById(R.id.nav_home);
        View btnHomeLogo        = findViewById(R.id.btn_home_logo);
        View tvLauncherTitle    = findViewById(R.id.tv_launcher_title);
        final android.widget.Button btnClientFeatures = findViewById(R.id.btn_client_features);

        View.OnClickListener homeListener = v -> {
            // Always pop to ROOT when clicking home
            getSupportFragmentManager().popBackStackImmediate("ROOT", 0);
            Fragment frag = getVisibleFragment("ROOT");
            if (frag instanceof MainMenuFragment) {
                ((MainMenuFragment) frag).refreshHomeState();
            }
        };

        if (navHome != null)         navHome.setOnClickListener(homeListener);
        if (btnHomeLogo != null)     btnHomeLogo.setOnClickListener(homeListener);
        if (tvLauncherTitle != null) tvLauncherTitle.setOnClickListener(homeListener);

        if (btnClientFeatures != null) {
            updateClientFeaturesButton(btnClientFeatures, mClientFeaturesManager.isEnabled());
            btnClientFeatures.setOnClickListener(v -> {
                boolean nextState = !mClientFeaturesManager.isEnabled();
                if (nextState) {
                    mClientFeaturesManager.showVersionSelector(() -> updateClientFeaturesButton(btnClientFeatures, true));
                } else {
                    mClientFeaturesManager.setEnabled(false);
                    updateClientFeaturesButton(btnClientFeatures, false);
                }
            });
        }

        if (navModStore != null) {
            navModStore.setOnClickListener(v -> {
                Fragment frag = getVisibleFragment("ROOT");
                if (frag instanceof MainMenuFragment) {
                    ((MainMenuFragment) frag).openChildPane(
                            ModsSearchFragment.class, ModsSearchFragment.TAG, null);
                } else {
                    Tools.swapFragment(this, ModsSearchFragment.class, ModsSearchFragment.TAG, null);
                }
            });
        }

        if (navCustomControls != null) {
            navCustomControls.setOnClickListener(v ->
                    startActivity(new Intent(this, CustomControlsActivity.class)));
        }

        if (navCursor != null) {
            navCursor.setOnClickListener(v ->
                    Tools.swapFragment(this, CursorCustomizationFragment.class,
                            CursorCustomizationFragment.TAG, null));
        }

        View navSkin = findViewById(R.id.nav_skin);
        if (navSkin != null) {
            navSkin.setOnClickListener(v -> {
                Fragment frag = getVisibleFragment("ROOT");
                if (frag instanceof MainMenuFragment) {
                    ((MainMenuFragment) frag).openChildPane(
                            SkinManagerFragment.class, SkinManagerFragment.TAG, null);
                } else {
                    Tools.swapFragment(this, SkinManagerFragment.class, SkinManagerFragment.TAG, null);
                }
            });
        }
        updateNavSkinIcon();
    }

    public void updateNavSkinIcon() {
        final ImageView navSkinIcon = findViewById(R.id.nav_skin_icon);
        if (navSkinIcon != null) {
            MinecraftAccount activeAccount = PojavProfile.getCurrentProfileContent(this, null);
            if (activeAccount != null) {
                Bitmap bitmap = activeAccount.getSkinFace();
                if (bitmap == null) {
                    bitmap = MinecraftAccount.getSkinFace(activeAccount.username);
                }
                if (bitmap != null) {
                    navSkinIcon.setImageBitmap(bitmap);
                    return;
                }
            }
            // Fallback to rounded default Steve
            Bitmap steve = BitmapFactory.decodeResource(getResources(), R.drawable.ic_steve);
            if (steve != null) {
                Bitmap roundedSteve = MinecraftAccount.roundBitmap(steve, 64, 10f);
                steve.recycle();
                navSkinIcon.setImageBitmap(roundedSteve);
            } else {
                navSkinIcon.setImageResource(R.drawable.ic_manage_skin);
            }
        }
    }

    private void updateClientFeaturesButton(android.widget.Button btn, boolean enabled) {
        if (enabled) {
            btn.setText("✦");
            btn.setBackgroundResource(R.drawable.bg_client_features_btn_filled);
            btn.setTextColor(android.graphics.Color.WHITE);
        } else {
            btn.setText("✦");
            btn.setBackgroundResource(R.drawable.bg_client_features_btn);
            btn.setTextColor(android.graphics.Color.parseColor("#B39DDB"));
        }
    }

    public ClientFeaturesManager getClientFeaturesManager() {
        return mClientFeaturesManager;
    }

    private void bindViews(){
        mFragmentView = findViewById(R.id.container_fragment);
        mSettingsButton = findViewById(R.id.setting_button);
        mAccountSpinner = findViewById(R.id.account_spinner);
        mProgressLayout = findViewById(R.id.progress_layout);
    }
}
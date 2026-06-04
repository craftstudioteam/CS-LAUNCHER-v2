package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.hasNoOnlineProfileDialog;
import static net.kdt.pojavlaunch.Tools.hasOnlineProfile;
import static net.kdt.pojavlaunch.Tools.openPath;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;
    private ViewGroup mRightPane;
    private View mPlayButton;
    private View mEditProfileButton;
    private View mBottomBar;
    private OnBackPressedCallback mRightPaneBackCallback;

    // ── Top bar state ──
    private int mCurrentNavTab = 0; // 0=Home, 1=ModStore, ...
    // Nav indicator views (in activity layout)
    private View mHomeIndicator, mModStoreIndicator, mControlsIndicator, mCursorIndicator, mToolsIndicator;
    private Interpolator mFastOutSlowIn = new AccelerateDecelerateInterpolator();

    // ─── Two-pane helpers ────────────────────────────────────────────────────

    /** True when the two-pane landscape layout is active. */
    private boolean isTwoPane() {
        // Landscape is now the default and only supported orientation for the launcher.
        return mRightPane != null;
    }

    /**
     * True when the right pane has a non-home fragment on the back stack.
     * Used by LauncherActivity to decide gear = home vs gear = settings.
     */
    public boolean isRightPaneActive() {
        return isTwoPane() && getChildFragmentManager().getBackStackEntryCount() > 0;
    }

    /**
     * Pops one entry off the right pane back stack.
     * Called from LauncherActivity.onBackPressed().
     */
    public void popRightPane() {
        if (!isTwoPane()) return;
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            getChildFragmentManager().popBackStack();
        }
    }

    /**
     * Pops everything off the right pane back stack so the home fragment shows again.
     * Safe to call even if back stack is empty.
     */
    public void clearRightPane() {
        if (!isTwoPane()) return;
        int count = getChildFragmentManager().getBackStackEntryCount();
        if (count > 0) {
            getChildFragmentManager().popBackStack(
                    getChildFragmentManager().getBackStackEntryAt(0).getName(),
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    /** Shows/hides the entire bottom bar. GONE collapses it so right pane fills full height. */
    private void setBottomBarVisible(boolean visible) {
        if (mBottomBar == null) return;
        if (visible) {
            if (mBottomBar.getVisibility() == View.VISIBLE) return;
            mBottomBar.setVisibility(View.VISIBLE);
            mBottomBar.setAlpha(0f);
            mBottomBar.setTranslationY(mBottomBar.getHeight() > 0
                    ? mBottomBar.getHeight() : 200f);
            mBottomBar.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(280)
                    .setInterpolator(mFastOutSlowIn)
                    .start();
        } else {
            if (mBottomBar.getVisibility() == View.GONE) return;
            mBottomBar.animate()
                    .alpha(0f)
                    .translationY(mBottomBar.getHeight() > 0
                            ? mBottomBar.getHeight() : 200f)
                    .setDuration(220)
                    .setInterpolator(mFastOutSlowIn)
                    .withEndAction(() -> mBottomBar.setVisibility(View.GONE))
                    .start();
        }
    }

    /** Explicitly clears the right pane and resets home UI state */
    public void refreshHomeState() {
        clearRightPane();
        setBottomBarVisible(true);
        if (mVersionSpinner != null) mVersionSpinner.reloadProfiles();
        updateSidebarStates(requireView(), R.id.home_button);
    }

    // Note: play button visibility during downloads is handled by the activity's
    // ProgressLayout — we do not need a separate TaskCountListener here.
    /**
     * Called by InstancePickerFragment after the user taps an instance.
     * Saves the selection, refreshes the spinner display, and pops back to home.
     */
    public void selectInstance(String profileKey) {
        LauncherPreferences.DEFAULT_PREF.edit()
                .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey)
                .apply();
        ExtraCore.setValue(ExtraConstants.REFRESH_VERSION_SPINNER, profileKey);
        clearRightPane();
        if (mVersionSpinner != null) mVersionSpinner.reloadProfiles();
    }

    /** Called externally (e.g. ProfileEditorFragment) to refresh the spinner display. */
    public void reloadSpinner() {
        if (mVersionSpinner != null) mVersionSpinner.reloadProfiles();
    }

    /**
     * Called by child fragments inside the right pane to navigate to another fragment
     * within the pane (landscape) or full-screen (portrait).
     * Use this instead of Tools.swapFragment(requireActivity(), ...) from child fragments.
     */
    public void openChildPane(Class<? extends Fragment> fragmentClass, String tag,
                              @Nullable Bundle args) {
        openPane(fragmentClass, tag, args);
    }

    /**
     * Returns true if the pane was used.
     */
    public boolean tryOpenInRightPane(Class<? extends Fragment> fragmentClass, String tag,
                                      @Nullable Bundle args) {
        if (!isTwoPane()) return false;
        openPane(fragmentClass, tag, args);
        return true;
    }

    /**
     * Internal navigation: right pane in landscape, full-screen swap in portrait.
     * Mod-store / mod-version-picker / mod-install fragments get the bottom bar
     * explicitly collapsed so the list container fills the full vertical space.
     */
    private void openPane(Class<? extends Fragment> fragmentClass, String tag,
                          @Nullable Bundle args) {
        if (isTwoPane()) {
            getChildFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.right_pane_container, fragmentClass, args, tag)
                    .addToBackStack(tag)
                    .commit();
            if (isModPaneFragment(fragmentClass)) {
                setBottomBarVisible(false);
            }
        } else {
            Tools.swapFragment(requireActivity(), fragmentClass, tag, args);
        }
    }

    /** Fragments that should consume the full height under the header (no bottom PLAY bar). */
    private boolean isModPaneFragment(Class<? extends Fragment> fragmentClass) {
        return fragmentClass == ModsSearchFragment.class
                || fragmentClass == ModVersionPickerFragment.class
                || fragmentClass == ModInstallFragment.class;
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    public MainMenuFragment() {
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create the callback once. Lifecycle owner = this fragment, so it is
        // automatically removed when the fragment is DESTROYED (not just view-destroyed).
        mRightPaneBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                // Guard: only act if view is still alive and back stack has entries
                if (mRightPane == null) return;
                if (getChildFragmentManager().getBackStackEntryCount() > 0) {
                    getChildFragmentManager().popBackStackImmediate();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(this, mRightPaneBackCallback);

        // Only register the back-stack listener once per fragment instance.
        // Using a member reference so we can remove it in onDestroyView if needed.
        getChildFragmentManager().addOnBackStackChangedListener(mBackStackListener);
    }

    /** Keeps a stable reference so we never register it twice. */
    private final androidx.fragment.app.FragmentManager.OnBackStackChangedListener
            mBackStackListener = () -> {
        mRightPaneBackCallback.setEnabled(isRightPaneActive());
        if (!isTwoPane()) return;
        // Show bottom bar ONLY on home (back stack empty). Hide on all other panes
        // including instance picker (it has its own back button in the header).
        boolean showBar = getChildFragmentManager().getBackStackEntryCount() == 0;
        setBottomBarVisible(showBar);
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // ─── Bottom bar views ───────────────────────────────────────
        ImageButton mEditProfileBtn = view.findViewById(R.id.edit_profile_button);
        Button mPlayBtn = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);
        mRightPane = view.findViewById(R.id.right_pane_container);
        mPlayButton = mPlayBtn;
        mEditProfileButton = mEditProfileBtn;
        mBottomBar = view.findViewById(R.id.bottom_bar);

        // ─── Horizontal Nav Rail (in activity layout) ─────────────────
        FrameLayout navHome = requireActivity().findViewById(R.id.nav_home);
        FrameLayout navModStore = requireActivity().findViewById(R.id.nav_mod_store);
        FrameLayout navControls = requireActivity().findViewById(R.id.nav_custom_controls);
        FrameLayout navCursor = requireActivity().findViewById(R.id.nav_cursor);
        FrameLayout navTools = requireActivity().findViewById(R.id.nav_instance_tools);

        // Indicator underlines (in activity layout)
        mHomeIndicator = requireActivity().findViewById(R.id.nav_home_indicator);
        mModStoreIndicator = requireActivity().findViewById(R.id.nav_mod_store_indicator);
        mControlsIndicator = requireActivity().findViewById(R.id.nav_controls_indicator);
        mCursorIndicator = requireActivity().findViewById(R.id.nav_cursor_indicator);
        mToolsIndicator = requireActivity().findViewById(R.id.nav_tools_indicator);

        // Load home fragment into right pane
        if (isTwoPane()) {
            Fragment existing = getChildFragmentManager()
                    .findFragmentById(R.id.right_pane_container);
            if (existing == null) {
                getChildFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.right_pane_container, RightPaneHomeFragment.class, null,
                                RightPaneHomeFragment.TAG)
                        .commit();
            }
        }

        // ─── Nav Tab Click Listeners ────────────────────────────────
        navHome.setOnClickListener(v -> {
            setActiveNavTab(0);
            clearRightPane();
            setBottomBarVisible(true);
            if (getChildFragmentManager().findFragmentById(R.id.right_pane_container) == null) {
                getChildFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.right_pane_container, RightPaneHomeFragment.class, null,
                                RightPaneHomeFragment.TAG)
                        .commit();
            }
        });

        navModStore.setOnClickListener(v -> {
            setActiveNavTab(1);
            Bundle args = new Bundle();
            args.putString(ManageModsFragment.BUNDLE_PROFILE_KEY,
                    LauncherPreferences.DEFAULT_PREF
                            .getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null));
            openPane(ModsSearchFragment.class, ModsSearchFragment.TAG, args);
        });

        navControls.setOnClickListener(v -> {
            setActiveNavTab(2);
            startActivity(new Intent(requireContext(), CustomControlsActivity.class));
        });

        navCursor.setOnClickListener(v -> {
            setActiveNavTab(3);
            Tools.swapFragment(requireActivity(), CursorCustomizationFragment.class,
                    CursorCustomizationFragment.TAG, null);
        });

        navTools.setOnClickListener(v -> {
            setActiveNavTab(4);
            if (Tools.isDemoProfile(v.getContext())) {
                hasNoOnlineProfileDialog(getActivity(),
                        getString(R.string.demo_unsupported),
                        getString(R.string.change_account));
            } else if (!hasOnlineProfile()) {
                hasNoOnlineProfileDialog(requireActivity());
            } else {
                openPath(v.getContext(), getCurrentProfileDirectory(), false);
            }
        });

        // Floating settings gear was removed from the top bar. Settings is reachable
        // via the dedicated settings click target on the main layout profile / core setup.


        // ─── Bottom bar listeners ────────────────────────────────────
        mEditProfileBtn.setOnClickListener(v ->
                mVersionSpinner.openProfileEditor(requireActivity()));

        if (isTwoPane()) {
            mVersionSpinner.setOnClickListener(v ->
                    openPane(InstancePickerFragment.class, InstancePickerFragment.TAG, null));
        }

        if (isTwoPane()) {
            setBottomBarVisible(getChildFragmentManager().getBackStackEntryCount() == 0);
        }

        // Neon green PLAY button
        mPlayBtn.setBackgroundResource(R.drawable.play_button_green);
        mPlayBtn.setTextColor(getResources().getColor(R.color.bg_primary));
        mPlayBtn.setOnClickListener(
                v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRightPane = null;
        mPlayButton = null;
        mEditProfileButton = null;
        mBottomBar = null;
        getChildFragmentManager().removeOnBackStackChangedListener(mBackStackListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVersionSpinner != null) {
            mVersionSpinner.post(() -> {
                if (mVersionSpinner != null) mVersionSpinner.reloadProfiles();
            });
        }
        
        // Force correct bar state on resume
        if (isTwoPane() && mBottomBar != null) {
            mBottomBar.post(() -> {
                boolean showBar = getChildFragmentManager().getBackStackEntryCount() == 0;
                setBottomBarVisible(showBar);
            });
        }
    }


    private void setActiveNavTab(int tabIndex) {
        mCurrentNavTab = tabIndex;
        // Reset all indicators
        if (mHomeIndicator != null) mHomeIndicator.setVisibility(tabIndex == 0 ? View.VISIBLE : View.INVISIBLE);
        if (mModStoreIndicator != null) mModStoreIndicator.setVisibility(tabIndex == 1 ? View.VISIBLE : View.INVISIBLE);
        if (mControlsIndicator != null) mControlsIndicator.setVisibility(tabIndex == 2 ? View.VISIBLE : View.INVISIBLE);
        if (mCursorIndicator != null) mCursorIndicator.setVisibility(tabIndex == 3 ? View.VISIBLE : View.INVISIBLE);
        if (mToolsIndicator != null) mToolsIndicator.setVisibility(tabIndex == 4 ? View.VISIBLE : View.INVISIBLE);
    }



    // ─── Private helpers ────────────────────────────────────────────────────

    private File getCurrentProfileDirectory() {
        String currentProfile = LauncherPreferences.DEFAULT_PREF
                .getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        if (!Tools.isValidString(currentProfile)) return new File(Tools.DIR_GAME_NEW);
        LauncherProfiles.load();
        MinecraftProfile profileObject =
                LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        if (profileObject == null) return new File(Tools.DIR_GAME_NEW);
        return Tools.getGameDirPath(profileObject);
    }

    private void runInstallerWithConfirmation(boolean isCustomArgs) {
        if (ProgressKeeper.getTaskCount() == 0)
            Tools.installMod(requireActivity(), isCustomArgs);
        else
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }

    /** Updates the visual state of sidebar buttons to highlight the active one. */
    private void updateSidebarStates(View root, int activeId) {
        int[] buttonIds = {R.id.home_button, R.id.mod_store_button};
        for (int id : buttonIds) {
            View btn = root.findViewById(id);
            if (btn == null) continue;
            boolean isActive = (id == activeId);
            btn.setBackgroundResource(isActive ? R.drawable.bg_nav_item_active : 0);
            if (btn instanceof Button) {
                ((Button) btn).setTextColor(isActive ? 
                    getResources().getColor(R.color.accent_primary) : 
                    getResources().getColor(R.color.text_secondary_light));
            }
        }
    }

    /** Applies a premium scale animation on touch for interactive elements. */
    private void applyPremiumTouchAnimation(View... views) {
        for (View v : views) {
            if (v == null) continue;
            v.setOnTouchListener((view, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.85f)
                            .setDuration(150)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1f).scaleY(1f).alpha(1f)
                            .setDuration(200)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                        break;
                }
                return false; // Let the standard click listener handle the click event
            });
        }
    }
}
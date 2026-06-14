package net.kdt.pojavlaunch.prefs.screens;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.prefs.SettingsSaveManager;

/**
 * Preference for the main screen, any sub-screen should inherit this class for consistent behavior,
 * overriding only onCreatePreferences
 */
public class LauncherPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsSaveManager.initDraft(getContext());
        // getPreferenceManager() is only valid AFTER super.onCreate()
        if (getPreferenceManager() != null) {
            getPreferenceManager().setSharedPreferencesName(SettingsSaveManager.DRAFT_PREFS_NAME);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        net.kdt.pojavlaunch.theme.ThemeManager.applyToPrefView(view);
        super.onViewCreated(view, savedInstanceState);
        RecyclerView list = getListView();
        if (list != null) {
            LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(
                    getContext(), R.anim.layout_fall_down);
            list.setLayoutAnimation(controller);
            list.scheduleLayoutAnimation();
            list.setItemAnimator(new DefaultItemAnimator());
            list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        View saveBtn = view.findViewById(R.id.btn_save_settings);
        if (saveBtn != null) {
            saveBtn.setOnClickListener(v -> {
                SettingsSaveManager.commitChanges(getContext());
                LauncherPreferences.loadPreferences(getContext());
                View bar = view.findViewById(R.id.unsaved_changes_bar);
                if (bar != null) bar.setVisibility(View.GONE);
            });
        }
        showSaveBarIfDirty(view);
    }

    private void showSaveBarIfDirty(View rootView) {
        if (rootView == null) return;
        View bar = rootView.findViewById(R.id.unsaved_changes_bar);
        if (bar == null) return;
        SharedPreferences p = getPreferenceManager().getSharedPreferences();
        // Default to true so toggle switches auto-save immediately
        boolean autoSave = p != null && p.getBoolean("auto_save_fallback", true);
        if (!autoSave && SettingsSaveManager.hasUnsavedChanges(getContext())) {
            bar.setVisibility(View.VISIBLE);
        } else {
            bar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_main);
        setupNotificationRequestPreference();
    }

    private void setupNotificationRequestPreference() {
        Preference mRequestNotificationPermissionPreference = findPreference("notification_permission_request");
        Preference mMicrophonePermissionPreference = findPreference("microphone_permission_request");
        Activity activity = getActivity();
        if(activity instanceof LauncherActivity) {
            LauncherActivity launcherActivity = (LauncherActivity)activity;
            if (mRequestNotificationPermissionPreference instanceof androidx.preference.TwoStatePreference) {
                androidx.preference.TwoStatePreference pref = (androidx.preference.TwoStatePreference) mRequestNotificationPermissionPreference;
                pref.setVisible(true);
                pref.setChecked(launcherActivity.checkForNotificationPermission());
                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean val = (Boolean) newValue;
                    if (val) {
                        launcherActivity.askForNotificationPermission(() -> {
                            pref.setChecked(launcherActivity.checkForNotificationPermission());
                        });
                    }
                    return false; // update checked state manually
                });
            }
            if (mMicrophonePermissionPreference instanceof androidx.preference.TwoStatePreference) {
                androidx.preference.TwoStatePreference pref = (androidx.preference.TwoStatePreference) mMicrophonePermissionPreference;
                pref.setVisible(true);
                pref.setChecked(launcherActivity.checkForMicrophonePermission());
                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean val = (Boolean) newValue;
                    if (val) {
                        launcherActivity.askForMicrophonePermission(() -> {
                            pref.setChecked(launcherActivity.checkForMicrophonePermission());
                        });
                    }
                    return false; // update checked state manually
                });
            }
        }else {
            if (mRequestNotificationPermissionPreference != null) {
                mRequestNotificationPermissionPreference.setVisible(false);
            }
            if (mMicrophonePermissionPreference != null) {
                mMicrophonePermissionPreference.setVisible(false);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if(sharedPreferences != null) sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        showSaveBarIfDirty(getView());
    }

    @Override
    public void onPause() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if(sharedPreferences != null) sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        // Always auto-save toggle/switch changes immediately so they survive restart
        // The auto_save_fallback pref defaults to true for first-time users
        boolean autoSave = p.getBoolean("auto_save_fallback", true);
        if (autoSave) {
            SettingsSaveManager.commitChanges(getContext());
            LauncherPreferences.loadPreferences(getContext());
            View bar = getView() != null ? getView().findViewById(R.id.unsaved_changes_bar) : null;
            if (bar != null) bar.setVisibility(View.GONE);
        } else {
            showSaveBarIfDirty(getView());
        }

        if ("force_english".equals(s)) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.recreate();
            }
        }
    }

    protected Preference requirePreference(CharSequence key) {
        Preference preference = findPreference(key);
        if(preference != null) return preference;
        throw new IllegalStateException("Preference "+key+" is null");
    }
    @SuppressWarnings("unchecked")
    protected <T extends Preference> T requirePreference(CharSequence key, Class<T> preferenceClass) {
        Preference preference = requirePreference(key);
        if(preferenceClass.isInstance(preference)) return (T)preference;
        throw new IllegalStateException("Preference "+key+" is not an instance of "+preferenceClass.getSimpleName());
    }
}
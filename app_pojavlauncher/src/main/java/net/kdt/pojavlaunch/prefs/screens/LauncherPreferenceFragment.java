package net.kdt.pojavlaunch.prefs.screens;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
 * overriding only onCreatePreferences.
 *
 * SETTINGS PAGE REWORK:
 * - Removed auto_save_fallback dependency
 * - Changes go to draft, Save button commits to main SharedPreferences
 * - Floating save bar with dirty-state indicator
 * - Auto-saves on fragment destroy to prevent data loss
 */
public class LauncherPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    /** Tracks whether any unsaved changes exist */
    private boolean mIsDirty = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsSaveManager.initDraft(getContext());
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

        setupSaveButton(view);
        updateSaveBar(view);
    }

    /** Wire up the modern floating SAVE CHANGES button */
    private void setupSaveButton(@NonNull View rootView) {
        Button saveBtn = rootView.findViewById(R.id.btn_save_settings);
        if (saveBtn == null) return;

        saveBtn.setOnClickListener(v -> {
            saveChanges();
            // Bounce animation on press
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                    .start();
        });
    }

    /** Persist all draft changes to main SharedPreferences */
    private void saveChanges() {
        if (!mIsDirty) return;
        SettingsSaveManager.commitChanges(getContext());
        LauncherPreferences.loadPreferences(getContext());
        mIsDirty = false;

        View root = getView();
        if (root != null) {
            updateSaveBar(root);
            // Show saved indicator briefly
            showSavedIndicator(root);
        }

        Toast.makeText(getContext(), "Settings saved successfully", Toast.LENGTH_SHORT).show();
    }

    /** Show a brief green "✓ Saved" toast on the save bar */
    private void showSavedIndicator(@NonNull View rootView) {
        View bar = rootView.findViewById(R.id.unsaved_changes_bar);
        if (bar == null) return;
        TextView statusText = bar.findViewById(R.id.save_status_text);
        if (statusText == null) return;

        final String originalText = statusText.getText().toString();
        statusText.setText("\u2713 Saved");
        statusText.setTextColor(android.graphics.Color.parseColor("#39FF14"));

        mHandler.postDelayed(() -> {
            if (!isAdded()) return;
            statusText.setText(originalText);
            statusText.setTextColor(android.graphics.Color.parseColor("#FFA500"));
        }, 2000);
    }

    /** Update save bar visibility and dirty indicator based on state */
    private void updateSaveBar(@NonNull View rootView) {
        View bar = rootView.findViewById(R.id.unsaved_changes_bar);
        if (bar == null) return;
        Button saveBtn = rootView.findViewById(R.id.btn_save_settings);
        TextView statusText = rootView.findViewById(R.id.save_status_text);

        boolean hasUnsaved = mIsDirty || SettingsSaveManager.hasUnsavedChanges(getContext());

        if (hasUnsaved) {
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
            if (saveBtn != null && !saveBtn.isEnabled()) {
                saveBtn.setEnabled(true);
                saveBtn.animate().alpha(1f).setDuration(200).start();
            }
            if (statusText != null) {
                statusText.setText("\u25CF Unsaved Changes");
                statusText.setTextColor(android.graphics.Color.parseColor("#FFA500"));
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
                saveBtn.animate().alpha(0.5f).setDuration(200).start();
            }
            if (statusText != null) {
                statusText.setText("\u2713 Saved");
                statusText.setTextColor(android.graphics.Color.parseColor("#39FF14"));
            }
        }
    }

    /** Mark settings as dirty and update the save bar */
    private void markDirty() {
        mIsDirty = true;
        View root = getView();
        if (root != null) {
            updateSaveBar(root);
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
                    markDirty();
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
                    markDirty();
                    return false; // update checked state manually
                });
            }
        } else {
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
        View root = getView();
        if (root != null) updateSaveBar(root);
    }

    @Override
    public void onPause() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if(sharedPreferences != null) sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        // Auto-save any pending changes to prevent data loss
        if (mIsDirty) {
            SettingsSaveManager.commitChanges(getContext());
            LauncherPreferences.loadPreferences(getContext());
            mIsDirty = false;
        }
        super.onDestroyView();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        // Any preference change marks the page as dirty
        markDirty();

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
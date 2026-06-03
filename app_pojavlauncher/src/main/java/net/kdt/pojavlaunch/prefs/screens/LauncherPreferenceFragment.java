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

/**
 * Preference for the main screen, any sub-screen should inherit this class for consistent behavior,
 * overriding only onCreatePreferences
 */
public class LauncherPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

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
            if (mRequestNotificationPermissionPreference != null) {
                mRequestNotificationPermissionPreference.setVisible(!launcherActivity.checkForNotificationPermission());
                mRequestNotificationPermissionPreference.setOnPreferenceClickListener(preference -> {
                    launcherActivity.askForNotificationPermission(()->mRequestNotificationPermissionPreference.setVisible(false));
                    return true;
                });
            }
            if (mMicrophonePermissionPreference != null) {
                mMicrophonePermissionPreference.setVisible(!launcherActivity.checkForMicrophonePermission());
                mMicrophonePermissionPreference.setOnPreferenceClickListener(preference -> {
                    launcherActivity.askForMicrophonePermission(()->mMicrophonePermissionPreference.setVisible(false));
                    return true;
                });
            }
        }else if (mRequestNotificationPermissionPreference != null) {
            mRequestNotificationPermissionPreference.setVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if(sharedPreferences != null) sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if(sharedPreferences != null) sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        LauncherPreferences.loadPreferences(getContext());
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
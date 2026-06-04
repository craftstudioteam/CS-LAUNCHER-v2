package net.kdt.pojavlaunch.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RightPaneHomeFragment extends Fragment {

    public static final String TAG = "RightPaneHomeFragment";
    public static final String CUSTOM_BG_PATH = Tools.DIR_DATA + "/custom_launcher_bg";

    private RecyclerView mRecyclerView;
    private HomeProfileAdapter mAdapter;

    public RightPaneHomeFragment() {
        super(R.layout.fragment_right_pane_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        loadBackground(view);

        view.setAlpha(0f);
        view.setTranslationX(60f);
        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(250)
            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
            .start();

        ImageView centerLogo = view.findViewById(R.id.iv_center_logo);
        if (centerLogo != null) {
            android.animation.ObjectAnimator floatAnim = android.animation.ObjectAnimator.ofFloat(
                    centerLogo, "translationY", 0f, -15f, 0f);
            floatAnim.setDuration(3500);
            floatAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            floatAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            floatAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            floatAnim.start();
        }

        ImageView heroBg = view.findViewById(R.id.iv_hero_bg);
        if (heroBg != null) {
            android.animation.PropertyValuesHolder scaleX =
                    android.animation.PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.05f, 1.0f);
            android.animation.PropertyValuesHolder scaleY =
                    android.animation.PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.05f, 1.0f);
            android.animation.ObjectAnimator panAnim =
                    android.animation.ObjectAnimator.ofPropertyValuesHolder(heroBg, scaleX, scaleY);
            panAnim.setDuration(20000);
            panAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            panAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            panAnim.setInterpolator(new android.view.animation.LinearInterpolator());
            panAnim.start();
        }

        mRecyclerView = view.findViewById(R.id.rv_home_profiles);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        setupProfileAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupProfileAdapter();
    }

    public void reloadBackground() {
        View v = getView();
        if (v != null) loadBackground(v);
    }

    private void setupProfileAdapter() {
        LauncherProfiles.load();
        Map<String, MinecraftProfile> profilesMap = LauncherProfiles.mainProfileJson != null
                ? LauncherProfiles.mainProfileJson.profiles : null;

        List<String> keys = new ArrayList<>();
        List<MinecraftProfile> profiles = new ArrayList<>();

        if (profilesMap != null) {
            List<Map.Entry<String, MinecraftProfile>> entries =
                    new ArrayList<>(profilesMap.entrySet());
            Collections.sort(entries, (a, b) -> {
                String ua = a.getValue().lastUsed != null ? a.getValue().lastUsed : "";
                String ub = b.getValue().lastUsed != null ? b.getValue().lastUsed : "";
                return ub.compareTo(ua);
            });
            for (Map.Entry<String, MinecraftProfile> entry : entries) {
                keys.add(entry.getKey());
                profiles.add(entry.getValue());
            }
        }

        mAdapter = new HomeProfileAdapter(keys, profiles,
                new HomeProfileAdapter.OnProfileActionListener() {
            @Override
            public void onProfilePlay(String profileKey, MinecraftProfile profile) {
                LauncherPreferences.DEFAULT_PREF.edit()
                        .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey)
                        .apply();
                ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
            }

            @Override
            public void onProfileEdit(String profileKey, MinecraftProfile profile) {
                LauncherPreferences.DEFAULT_PREF.edit()
                        .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, profileKey)
                        .apply();
                Tools.swapFragment(requireActivity(),
                        ProfileEditorFragment.class, ProfileEditorFragment.TAG, null);
            }
        });

        mRecyclerView.setAdapter(mAdapter);
    }

    private void loadBackground(@NonNull View view) {
        ImageView wallpaper = view.findViewById(R.id.right_pane_wallpaper);
        File bgFile = new File(CUSTOM_BG_PATH);
        if (bgFile.exists()) {
            Drawable d = Drawable.createFromPath(bgFile.getAbsolutePath());
            if (d != null) {
                wallpaper.setImageDrawable(d);
                wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
                wallpaper.setBackground(null);
                wallpaper.setVisibility(View.VISIBLE);
                return;
            }
        }
        wallpaper.setImageDrawable(null);
        TypedValue tv = new TypedValue();
        view.getContext().getTheme().resolveAttribute(R.attr.bgMainDrawable, tv, true);
        if (tv.resourceId != 0) {
            wallpaper.setBackgroundResource(tv.resourceId);
            wallpaper.setVisibility(View.VISIBLE);
        } else {
            wallpaper.setBackground(null);
            wallpaper.setVisibility(View.GONE);
        }
    }
}

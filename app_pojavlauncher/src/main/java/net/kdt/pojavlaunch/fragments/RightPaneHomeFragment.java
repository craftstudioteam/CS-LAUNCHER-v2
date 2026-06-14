package net.kdt.pojavlaunch.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

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
import net.kdt.pojavlaunch.profiles.ProfileIconCache;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
        loadCustomWallpaper(view);

        mRecyclerView = view.findViewById(R.id.rv_home_profiles);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        setupProfileAdapter();

        // Floating "+" FAB opens the Version Setup Hub (3-category grid)
        View fab = view.findViewById(R.id.fab_create_profile);
        if (fab != null) {
            // Apply 200ms scale-up reveal with DecelerateInterpolator
            fab.setScaleX(0.6f);
            fab.setScaleY(0.6f);
            fab.setAlpha(0f);
            fab.animate()
                    .scaleX(1f).scaleY(1f)
                    .alpha(1f)
                    .setDuration(200)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            fab.setOnClickListener(v -> {
                if (!isAdded() || getContext() == null) return;
                Fragment parent = getParentFragment();
                if (parent instanceof MainMenuFragment) {
                    ((MainMenuFragment) parent).openChildPane(
                            ProfileTypeSelectFragment.class,
                            ProfileTypeSelectFragment.TAG, null);
                } else if (getActivity() != null) {
                    Tools.swapFragment(getActivity(),
                            ProfileTypeSelectFragment.class,
                            ProfileTypeSelectFragment.TAG, null);
                }
            });
        }

        View refreshBtn = view.findViewById(R.id.btn_refresh_profiles);
        if (refreshBtn != null) {
            refreshBtn.setOnClickListener(v -> {
                setupProfileAdapter();
                Toast.makeText(getContext(), "Profiles refreshed", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupProfileAdapter();
    }

    public void reloadBackground() {
        View v = getView();
        if (v != null) loadCustomWallpaper(v);
    }

    private void setupProfileAdapter() {
        LauncherProfiles.load();
        Map<String, MinecraftProfile> profilesMap = LauncherProfiles.mainProfileJson != null
                ? LauncherProfiles.mainProfileJson.profiles : null;

        // Force icon cache refresh — guarantees fresh base64 decode for any
        // profile whose icon payload was updated since last reload.
        if (profilesMap != null) {
            for (String key : profilesMap.keySet()) {
                ProfileIconCache.dropIcon(key);
            }
        }

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

    private void loadCustomWallpaper(@NonNull View view) {
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
        wallpaper.setBackground(null);
        wallpaper.setVisibility(View.GONE);
    }
}

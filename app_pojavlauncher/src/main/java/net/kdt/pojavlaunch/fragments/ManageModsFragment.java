package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.fragments.ModsSearchFragment;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.InstalledModAdapter;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;

public class ManageModsFragment extends Fragment {

    public static final String TAG = "ManageModsFragment";
    public static final String BUNDLE_PROFILE_KEY = "profile_key";

    public ManageModsFragment() {
        super(R.layout.fragment_manage_mods);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ImageButton backButton  = view.findViewById(R.id.manage_mods_back);
        ImageButton addButton   = view.findViewById(R.id.manage_mods_add);
        TextView    title       = view.findViewById(R.id.manage_mods_title);
        RecyclerView recycler   = view.findViewById(R.id.manage_mods_recycler);
        View        emptyState  = view.findViewById(R.id.manage_mods_empty);

        // Back — delegate to the activity which handles both portrait (pop activity stack)
        // and landscape two-pane (pop right pane) in one reliable place.
        backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        // Resolve the active profile so we can apply version instance rules.
        MinecraftProfile profile = resolveActiveProfile();
        boolean modsBlocked = profile != null && (profile.isOptiFine() || profile.isVanilla());

        // Add → open mod store — stay in right pane if we're inside one.
        // For vanilla / OptiFine instances the mod store would crash the game,
        // so the entry point is hidden. Users can still browse packs/shaders/worlds
        // through the right-pane home mod-store tab if they need to.
        if (modsBlocked) {
            addButton.setVisibility(View.GONE);
        } else {
            addButton.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString(BUNDLE_PROFILE_KEY, getProfileKey());
                navigateToFragment(ModsSearchFragment.class, ModsSearchFragment.TAG, args);
            });
        }

        // Title: "ProfileName - Mods"
        String profileName = getProfileName();
        title.setText(profileName.isEmpty()
                ? getString(R.string.mcl_button_manage_mods)
                : profileName + " - Mods");

        // Build mod list
        File modsDir = getModsDir();
        InstalledModAdapter adapter = new InstalledModAdapter(modsDir, isEmpty -> {
            recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
    }

    /** Resolve the active profile either via the BUNDLE_PROFILE_KEY arg or the global pref. */
    private MinecraftProfile resolveActiveProfile() {
        try {
            String key = getProfileKey();
            if (key == null || key.isEmpty()) return null;
            LauncherProfiles.load();
            return LauncherProfiles.mainProfileJson.profiles.get(key);
        } catch (Throwable t) {
            return null;
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String getProfileName() {
        try {
            String key = getProfileKey();
            if (key == null || key.isEmpty()) return "";
            LauncherProfiles.load();
            MinecraftProfile profile = LauncherProfiles.mainProfileJson.profiles.get(key);
            if (profile == null) return "";
            return profile.name != null ? profile.name : key;
        } catch (Exception e) {
            return "";
        }
    }

    private File getModsDir() {
        try {
            String key = getProfileKey();
            if (key != null && !key.isEmpty()) {
                LauncherProfiles.load();
                MinecraftProfile profile = LauncherProfiles.mainProfileJson.profiles.get(key);
                if (profile != null) {
                    File gameDir = Tools.getGameDirPath(profile);
                    return new File(gameDir, "mods");
                }
            }
        } catch (Exception ignored) {}
        return new File(Tools.DIR_GAME_NEW, "mods");
    }

    private String getProfileKey() {
        Bundle args = getArguments();
        if (args != null && args.containsKey(BUNDLE_PROFILE_KEY)) {
            return args.getString(BUNDLE_PROFILE_KEY);
        }
        return LauncherPreferences.DEFAULT_PREF
                .getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
    }

    /** Go back — pops the parent's child stack synchronously when inside right pane. */
    private void navigateBack() {
        Fragment parent = getParentFragment();
        if (parent != null) {
            // Synchronous pop — no race condition with the view lifecycle
            parent.getChildFragmentManager().popBackStackImmediate();
        } else {
            Tools.removeCurrentFragment(requireActivity());
        }
    }

    /** Navigate to a fragment — stays inside the right pane when running as a child fragment. */
    private void navigateToFragment(Class<? extends Fragment> fragmentClass, String tag, Bundle args) {
        Fragment parent = getParentFragment();
        if (parent != null) {
            parent.getChildFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.right_pane_container, fragmentClass, args, tag)
                    .addToBackStack(tag)
                    .commit();
        } else {
            Tools.swapFragment(requireActivity(), fragmentClass, tag, args);
        }
    }
}
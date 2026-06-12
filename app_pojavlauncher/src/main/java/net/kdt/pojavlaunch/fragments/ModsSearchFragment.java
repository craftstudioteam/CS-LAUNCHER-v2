package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModrinthApi;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

public class ModsSearchFragment extends Fragment {

    public static final String TAG = "ModsSearchFragment";

    private static final String[] TAB_TITLES = {"Mods", "Resource Packs", "Shaders", "Worlds"};
    private static final String[] TAB_TYPES  = {"mod", "resourcepack", "shader", "world"};

    private EditText mSearchEditText;
    private ImageButton mFilterButton;
    private ViewPager2 mViewPager;
    private DownloadTabAdapter mTabAdapter;
    private LinearLayout mTabBar;
    private View mTabIndicator;
    private HorizontalScrollView mTabScroll;

    private int mCurrentTab = 0;
    private final SearchFilters mSearchFilters = new SearchFilters();
    private String mProfileKey;

    private final Handler mSearchHandler = new Handler(Looper.getMainLooper());

    public ModsSearchFragment() {
        super(R.layout.fragment_mod_search_tabbed);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mProfileKey = getArguments() != null
                ? getArguments().getString(ManageModsFragment.BUNDLE_PROFILE_KEY) : null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mSearchEditText = view.findViewById(R.id.search_mod_edittext);
        mFilterButton = view.findViewById(R.id.search_mod_filter);
        mViewPager = view.findViewById(R.id.download_view_pager);
        mTabBar = view.findViewById(R.id.tab_bar);
        mTabIndicator = view.findViewById(R.id.tab_indicator);
        mTabScroll = view.findViewById(R.id.tab_scroll);

        ImageButton backButton = view.findViewById(R.id.mod_store_back);
        backButton.setOnClickListener(v -> {
            Fragment parent = getParentFragment();
            if (parent instanceof MainMenuFragment) {
                ((MainMenuFragment) parent).refreshHomeState();
            } else if (parent != null) {
                parent.getChildFragmentManager().popBackStackImmediate();
            } else {
                Tools.removeCurrentFragment(requireActivity());
            }
        });

        setupTabs();

        mTabAdapter = new DownloadTabAdapter(this, TAB_TYPES);
        mViewPager.setAdapter(mTabAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                mCurrentTab = position;
                updateTabSelection(position);
                String query = mSearchEditText.getText().toString().trim();
                DownloadListFragment dlf = getListFragment(TAB_TYPES[position]);
                if (dlf != null && !query.isEmpty()) {
                    dlf.filter(query, mSearchFilters.mcVersion, mSearchFilters.modLoader);
                }
            }
        });

        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            private Runnable searchRunnable;
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) mSearchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    DownloadListFragment dlf = getListFragment(TAB_TYPES[mCurrentTab]);
                    if (dlf != null) {
                        dlf.filter(s.toString(), mSearchFilters.mcVersion, mSearchFilters.modLoader);
                    }
                };
                mSearchHandler.postDelayed(searchRunnable, 400);
            }
        });

        mSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
            mSearchHandler.removeCallbacksAndMessages(null);
            DownloadListFragment dlf = getListFragment(TAB_TYPES[mCurrentTab]);
            if (dlf != null) {
                dlf.filter(mSearchEditText.getText().toString(),
                        mSearchFilters.mcVersion, mSearchFilters.modLoader);
            }
            mSearchEditText.clearFocus();
            return false;
        });

        mFilterButton.setOnClickListener(v -> displayFilterDialog());
        mSearchEditText.setHint(R.string.hint_search_mod);

        // Wire up click listeners — handles fragment creation and recreation
        getChildFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                                                      @NonNull View v, @Nullable Bundle savedInstanceState) {
                        if (f instanceof DownloadListFragment) {
                            DownloadListFragment dlf = (DownloadListFragment) f;
                            String type = dlf.getContentType();
                            for (int i = 0; i < TAB_TYPES.length; i++) {
                                if (TAB_TYPES[i].equals(type)) {
                                    final int tabPos = i;
                                    dlf.setOnModItemClickListener(
                                            item -> onModItemClick(item, TAB_TYPES[tabPos]));
                                    break;
                                }
                            }
                        }
                    }
                }, true);
    }

    private void setupTabs() {
        mTabBar.removeAllViews();
        for (int i = 0; i < TAB_TITLES.length; i++) {
            TextView tab = new TextView(requireContext());
            tab.setText(TAB_TITLES[i]);
            tab.setTextSize(14);
            tab.setPadding(24, 8, 24, 8);
            tab.setGravity(android.view.Gravity.CENTER);
            tab.setTextColor(i == 0 ? Color.parseColor("#39FF14") : Color.parseColor("#9CA3AF"));
            tab.setTypeface(null, i == 0 ? Typeface.BOLD : Typeface.NORMAL);
            tab.setTag(i);
            tab.setOnClickListener(v -> mViewPager.setCurrentItem((int) v.getTag(), true));
            mTabBar.addView(tab, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
        mTabIndicator.post(() -> {
            if (mTabBar.getChildCount() > 0) {
                View firstTab = mTabBar.getChildAt(0);
                firstTab.post(() -> {
                    int w = firstTab.getWidth();
                    if (w > 0) {
                        mTabIndicator.getLayoutParams().width = w;
                        mTabIndicator.requestLayout();
                    }
                });
            }
        });
    }

    private void updateTabSelection(int position) {
        for (int i = 0; i < mTabBar.getChildCount(); i++) {
            TextView tab = (TextView) mTabBar.getChildAt(i);
            if (i == position) {
                tab.setTextColor(Color.parseColor("#39FF14"));
                tab.setTypeface(null, Typeface.BOLD);
            } else {
                tab.setTextColor(Color.parseColor("#9CA3AF"));
                tab.setTypeface(null, Typeface.NORMAL);
            }
        }
        View selectedTab = mTabBar.getChildAt(position);
        if (selectedTab != null) {
            selectedTab.post(() -> {
                int targetX = selectedTab.getLeft();
                int targetWidth = selectedTab.getWidth();
                if (targetWidth > 0) {
                    mTabIndicator.setTranslationX(targetX);
                    ViewGroup.LayoutParams lp = mTabIndicator.getLayoutParams();
                    lp.width = targetWidth;
                    mTabIndicator.requestLayout();
                }
            });
            mTabScroll.smoothScrollTo(selectedTab.getLeft() - 50, 0);
        }
    }

    private DownloadListFragment getListFragment(String contentType) {
        for (Fragment f : getChildFragmentManager().getFragments()) {
            if (f instanceof DownloadListFragment) {
                DownloadListFragment dlf = (DownloadListFragment) f;
                if (contentType.equals(dlf.getContentType())) {
                    return dlf;
                }
            }
        }
        return null;
    }

    private void onModItemClick(ModItem item, String contentType) {
        navigateToVersionPicker(item, contentType);
    }

    private void navigateToVersionPicker(ModItem item, String contentType) {
        Bundle args = new Bundle();
        args.putSerializable("mod_item", item);
        args.putString("content_type", contentType);
        args.putString(ManageModsFragment.BUNDLE_PROFILE_KEY, mProfileKey);

        Fragment parent = getParentFragment();
        if (parent instanceof MainMenuFragment) {
            ((MainMenuFragment) parent).openChildPane(
                    ModVersionPickerFragment.class, ModVersionPickerFragment.TAG, args);
        } else if (parent != null) {
            parent.getChildFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.fade_in_slide_up, R.anim.fade_out_slide_down,
                            R.anim.fade_in_slide_up, R.anim.fade_out_slide_down)
                    .setReorderingAllowed(true)
                    .replace(R.id.right_pane_container,
                            ModVersionPickerFragment.class, args, ModVersionPickerFragment.TAG)
                    .addToBackStack(ModVersionPickerFragment.TAG)
                    .commit();
        } else {
            Tools.swapFragment(requireActivity(),
                    ModVersionPickerFragment.class, ModVersionPickerFragment.TAG, args);
        }
    }

    private void downloadDirect(ModItem item, String contentType) {
        PojavApplication.sExecutorService.execute(() -> {
            try {
                ModrinthApi api = new ModrinthApi();
                ModDetail detail = api.getModDetails(item, null, null);
                if (detail != null && detail.versionUrls != null && detail.versionUrls.length > 0) {
                    String url = detail.versionUrls[0];
                    Context ctx = getContext();
                    if (ctx == null) return;
                    String name = item.title;
                    String finalContentType = contentType;
                    Tools.runOnUiThread(() -> showProfileSelectorAndDownloadDirect(item, finalContentType, url, name));
                } else {
                    Tools.runOnUiThread(() -> {
                        if (isAdded())
                            Toast.makeText(getContext(), "No download available", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Tools.runOnUiThread(() -> {
                    if (isAdded())
                        Toast.makeText(getContext(), "Download failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showProfileSelectorAndDownloadDirect(net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem item, String contentType, String url, String name) {
        net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.load();
        java.util.Map<String, net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile> profiles = 
                net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.mainProfileJson.profiles;
                
        if (profiles == null || profiles.isEmpty()) {
            net.kdt.pojavlaunch.fragments.ModDownloadHelper.downloadAndExtract(getContext(), name, url, contentType, null);
            return;
        }

        String[] profileKeys = profiles.keySet().toArray(new String[0]);
        String[] profileNames = new String[profileKeys.length];
        int currentSelection = -1;
        String currentKey = net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF.getString(
                        net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
                        
        for (int i = 0; i < profileKeys.length; i++) {
            net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile p = profiles.get(profileKeys[i]);
            profileNames[i] = (p != null && p.name != null && !p.name.isEmpty()) ? p.name : "Unnamed Profile";
            if (profileKeys[i].equals(currentKey)) {
                currentSelection = i;
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Profile to Install To")
                .setSingleChoiceItems(profileNames, currentSelection, (dialog, which) -> {
                    dialog.dismiss();
                    net.kdt.pojavlaunch.fragments.ModDownloadHelper.downloadAndExtract(getContext(), name, url, contentType, profileKeys[which]);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void displayFilterDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(R.layout.dialog_mod_filters)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            TextView mSelectedVersion = dialog.findViewById(R.id.search_mod_selected_mc_version_textview);
            Button mSelectVersionButton = dialog.findViewById(R.id.search_mod_mc_version_button);
            Button mApplyButton = dialog.findViewById(R.id.search_mod_apply_filters);
            Spinner mLoaderSpinner = dialog.findViewById(R.id.search_mod_loader_spinner);

            assert mSelectedVersion != null;
            assert mSelectVersionButton != null;
            assert mApplyButton != null;

            if (mLoaderSpinner != null) {
                String[] loaderLabels = {"Any loader", "Fabric", "Forge", "Quilt", "NeoForge"};
                final String[] loaderValues = {"", "fabric", "forge", "quilt", "neoforge"};
                android.widget.ArrayAdapter<String> loaderAdapter = new android.widget.ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_item, loaderLabels);
                loaderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mLoaderSpinner.setAdapter(loaderAdapter);

                String currentLoader = mSearchFilters.modLoader != null ? mSearchFilters.modLoader : "";
                for (int i = 0; i < loaderValues.length; i++) {
                    if (loaderValues[i].equals(currentLoader)) {
                        mLoaderSpinner.setSelection(i);
                        break;
                    }
                }

                mSelectVersionButton.setOnClickListener(v ->
                        VersionSelectorDialog.open(v.getContext(), true,
                                (id, snapshot) -> mSelectedVersion.setText(id)));

                mSelectedVersion.setText(mSearchFilters.mcVersion);

                mApplyButton.setOnClickListener(v -> {
                    mSearchFilters.mcVersion = mSelectedVersion.getText().toString();
                    int pos = mLoaderSpinner.getSelectedItemPosition();
                    mSearchFilters.modLoader = loaderValues[pos];
                    DownloadListFragment dlf = getListFragment(TAB_TYPES[mCurrentTab]);
                    if (dlf != null) {
                        dlf.filter(mSearchEditText.getText().toString(),
                                mSearchFilters.mcVersion, mSearchFilters.modLoader);
                    }
                    dialogInterface.dismiss();
                });
            } else {
                mSelectVersionButton.setOnClickListener(v ->
                        VersionSelectorDialog.open(v.getContext(), true,
                                (id, snapshot) -> mSelectedVersion.setText(id)));

                mSelectedVersion.setText(mSearchFilters.mcVersion);

                mApplyButton.setOnClickListener(v -> {
                    mSearchFilters.mcVersion = mSelectedVersion.getText().toString();
                    DownloadListFragment dlf = getListFragment(TAB_TYPES[mCurrentTab]);
                    if (dlf != null) {
                        dlf.filter(mSearchEditText.getText().toString(),
                                mSearchFilters.mcVersion, mSearchFilters.modLoader);
                    }
                    dialogInterface.dismiss();
                });
            }
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        applyInstanceRules();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * Apply version instance rules for the active profile:
     *  - vanilla profile → block the Mods tab; show only Resource Packs / Shaders / Worlds.
     *  - OptiFine profile → block the Mods tab; show only Resource Packs / Shaders / Worlds.
     *  - everything else → leave all four tabs.
     * If the user has the mod store already open and the rules now ban the Mods tab,
     * the ViewPager is moved to the first allowed tab.
     */
    private void applyInstanceRules() {
        if (!isAdded() || getView() == null) return;
        MinecraftProfile profile = resolveActiveProfile();
        if (profile == null) return;
        boolean isVanilla = profile.isVanilla();
        boolean isOptifine = profile.isOptiFine();
        if (!isVanilla && !isOptifine) return;

        // Mods tab is the first tab; force the ViewPager to the second one (Resource Packs).
        if (mCurrentTab == 0 && mViewPager != null) {
            mViewPager.setCurrentItem(1, false);
            mCurrentTab = 1;
            updateTabSelection(1);
        }
    }

    /** Resolve the active profile either via {@link #mProfileKey} arg or the global pref. */
    private MinecraftProfile resolveActiveProfile() {
        try {
            LauncherProfiles.load();
            String key = mProfileKey;
            if (key == null || key.isEmpty()) {
                key = net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF
                        .getString(net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            }
            if (key == null || key.isEmpty()) return null;
            return LauncherProfiles.mainProfileJson.profiles.get(key);
        } catch (Throwable t) {
            return null;
        }
    }
}

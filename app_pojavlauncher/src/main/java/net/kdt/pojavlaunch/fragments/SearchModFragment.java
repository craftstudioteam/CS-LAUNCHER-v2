package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.modpacks.ModItemAdapter;
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackApi;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModrinthApi;
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;

public class SearchModFragment extends Fragment implements ModItemAdapter.SearchResultCallback {

    public static final String TAG = "SearchModFragment";
    private View mOverlay;

    private EditText mSearchEditText;
    private ImageButton mFilterButton;
    private RecyclerView mRecyclerview;
    private ModItemAdapter mModItemAdapter;
    private ProgressBar mSearchProgressBar;
    private TextView mStatusTextView;
    private ColorStateList mDefaultTextColor;

    private ModpackApi modpackApi;

    private final SearchFilters mSearchFilters;

    public SearchModFragment(){
        super(R.layout.fragment_mod_search);
        mSearchFilters = new SearchFilters();
        mSearchFilters.isModpack = true;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        modpackApi = new ModpackSearchApi(context.getString(R.string.curseforge_api_key), mSearchFilters);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // You can only access resources after attaching to current context
        mModItemAdapter = new ModItemAdapter(getResources(), modpackApi, this);
        ProgressKeeper.addTaskCountListener(mModItemAdapter);

        mOverlay = view.findViewById(R.id.mod_store_header);
        mSearchEditText = view.findViewById(R.id.search_mod_edittext);
        mSearchProgressBar = view.findViewById(R.id.search_mod_progressbar);
        mRecyclerview = view.findViewById(R.id.search_mod_list);
        mStatusTextView = view.findViewById(R.id.search_mod_status_text);
        mFilterButton = view.findViewById(R.id.search_mod_filter);
        
        if (mSearchFilters.isModpack) {
            TextView title = view.findViewById(R.id.mod_store_title);
            if (title != null) title.setText("Download Modpacks");
            mSearchEditText.setHint("Search modpacks...");
        }

        mDefaultTextColor = mStatusTextView.getTextColors();

        mRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerview.setAdapter(mModItemAdapter);
        mModItemAdapter.setOnItemClickListener(item -> {
            Bundle args = new Bundle();
            args.putSerializable("mod_item", item);
            args.putString(ManageModsFragment.BUNDLE_PROFILE_KEY, null);
            if (mSearchFilters.isModpack) {
                args.putString("content_type", "modpack");
            }
            Fragment parent = getParentFragment();
            if (parent instanceof MainMenuFragment) {
                ((MainMenuFragment) parent).openChildPane(ModVersionPickerFragment.class, ModVersionPickerFragment.TAG, args);
            } else if (parent != null) {
                parent.getChildFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                R.anim.fade_in_slide_up, R.anim.fade_out_slide_down,
                                R.anim.fade_in_slide_up, R.anim.fade_out_slide_down)
                        .setReorderingAllowed(true)
                        .replace(R.id.right_pane_container, ModVersionPickerFragment.class, args, ModVersionPickerFragment.TAG)
                        .addToBackStack(ModVersionPickerFragment.TAG)
                        .commit();
            } else {
                Tools.swapFragment(requireActivity(), ModVersionPickerFragment.class, ModVersionPickerFragment.TAG, args);
            }
        });

        // Real-time search via TextWatcher with debounce
        Handler mSearchHandler = new Handler(Looper.getMainLooper());
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            private Runnable searchRunnable;
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) mSearchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> searchMods(s.toString());
                mSearchHandler.postDelayed(searchRunnable, 400);
            }
        });

        mSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
            mSearchHandler.removeCallbacksAndMessages(null);
            searchMods(mSearchEditText.getText().toString());
            mSearchEditText.clearFocus();
            return false;
        });

        mFilterButton.setOnClickListener(v -> displayFilterDialog());

        searchMods(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        hidePlayPanel(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        hidePlayPanel(false);
    }

    private void hidePlayPanel(boolean hide) {
        if (getActivity() == null) return;
        View bottomBar = getActivity().findViewById(R.id.bottom_bar);
        if (bottomBar != null) bottomBar.setVisibility(hide ? View.GONE : View.VISIBLE);
        View sidePanel = getActivity().findViewById(R.id.right_pane_container);
        if (sidePanel != null) {
            ViewGroup.LayoutParams lp = sidePanel.getLayoutParams();
            if (lp instanceof androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
                ((androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) lp).bottomToBottom = hide
                        ? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                        : R.id.bottom_bar;
                sidePanel.requestLayout();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ProgressKeeper.removeTaskCountListener(mModItemAdapter);
    }

    @Override
    public void onSearchFinished() {
        mSearchProgressBar.setVisibility(View.GONE);
        mStatusTextView.setVisibility(View.GONE);
    }

    @Override
    public void onSearchError(int error) {
        mSearchProgressBar.setVisibility(View.GONE);
        mStatusTextView.setVisibility(View.VISIBLE);
        switch (error) {
            case ERROR_INTERNAL:
                mStatusTextView.setTextColor(Color.RED);
                mStatusTextView.setText(R.string.search_modpack_error);
                break;
            case ERROR_NO_RESULTS:
                mStatusTextView.setTextColor(mDefaultTextColor);
                mStatusTextView.setText(R.string.search_modpack_no_result);
                break;
        }
    }

    private void searchMods(String name) {
        mSearchProgressBar.setVisibility(View.VISIBLE);
        mSearchFilters.name = name == null ? "" : name;
        mModItemAdapter.performSearchQuery(mSearchFilters);
    }

    private void displayFilterDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(R.layout.dialog_mod_filters)
                .create();

        // setup the view behavior
        dialog.setOnShowListener(dialogInterface -> {
            TextView mSelectedVersion = dialog.findViewById(R.id.search_mod_selected_mc_version_textview);
            Button mSelectVersionButton = dialog.findViewById(R.id.search_mod_mc_version_button);
            Button mApplyButton = dialog.findViewById(R.id.search_mod_apply_filters);
            Spinner mLoaderSpinner = dialog.findViewById(R.id.search_mod_loader_spinner);

            assert mSelectVersionButton != null;
            assert mSelectedVersion != null;
            assert mApplyButton != null;

            // Set up loader spinner
            if (mLoaderSpinner != null) {
                String[] loaderLabels = {"Any loader", "Fabric", "Forge", "Quilt", "NeoForge"};
                final String[] loaderValues = {"", "fabric", "forge", "quilt", "neoforge"};
                ArrayAdapter<String> loaderAdapter = new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_item, loaderLabels);
                loaderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mLoaderSpinner.setAdapter(loaderAdapter);

                // Restore current selection
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
                    searchMods(mSearchEditText.getText().toString());
                    dialogInterface.dismiss();
                });
            } else {
                mSelectVersionButton.setOnClickListener(v ->
                        VersionSelectorDialog.open(v.getContext(), true,
                                (id, snapshot) -> mSelectedVersion.setText(id)));
                mSelectedVersion.setText(mSearchFilters.mcVersion);
                mApplyButton.setOnClickListener(v -> {
                    mSearchFilters.mcVersion = mSelectedVersion.getText().toString();
                    searchMods(mSearchEditText.getText().toString());
                    dialogInterface.dismiss();
                });
            }
        });

        dialog.show();
    }

    // ── ModpackSearchApi ──────────────────────────────────────────────────────

    private static class ModpackSearchApi extends CommonApi {
        private final SearchFilters mFilters;
        private final ModrinthApi mModrinthApi = new ModrinthApi();

        ModpackSearchApi(String curseforgeApiKey, SearchFilters filters) {
            super(curseforgeApiKey);
            mFilters = filters;
        }

        /**
         * Override getModDetails so the version dropdown only shows versions
         * matching the selected MC version and loader filter.
         */
        @Override
        public ModDetail getModDetails(ModItem item) {
            if (item.apiSource == Constants.SOURCE_MODRINTH) {
                String filterVer = (mFilters.mcVersion != null && !mFilters.mcVersion.isEmpty())
                        ? mFilters.mcVersion : null;
                String filterLoader = (mFilters.modLoader != null && !mFilters.modLoader.isEmpty())
                        ? mFilters.modLoader : null;
                return mModrinthApi.getModDetails(item, filterVer, filterLoader);
            }
            // CurseForge: delegate normally (CF search already filters by version/loader)
            return super.getModDetails(item);
        }
    }
}
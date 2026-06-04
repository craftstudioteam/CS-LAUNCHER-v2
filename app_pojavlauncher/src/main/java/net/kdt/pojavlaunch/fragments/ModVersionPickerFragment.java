package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackApi;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModrinthApi;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModVersionPickerFragment extends Fragment {

    public static final String TAG = "ModVersionPickerFragment";
    private static final String ARG_MOD_ITEM = "mod_item";
    private static final String ARG_PROFILE_KEY = "profile_key";

    private static final int PAGE_SIZE = 15;

    private ModItem mModItem;
    private ModDetail mModDetail;
    private String mProfileKey;

    // Views
    private ImageButton mBackButton;
    private TextView mTitleView;
    private ProgressBar mLoadingView;
    private RecyclerView mVersionList;
    private View mPaginationFooter;
    private TextView mPaginationText;
    private ImageButton mPrevButton;
    private ImageButton mNextButton;
    private TextView mErrorView;

    private VersionAdapter mAdapter;
    private List<VersionEntry> mAllVersions = new ArrayList<>();
    private int mCurrentPage = 0;
    private int mTotalPages = 0;

    public static ModVersionPickerFragment newInstance(ModItem item, String profileKey) {
        ModVersionPickerFragment f = new ModVersionPickerFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MOD_ITEM, item);
        args.putString(ARG_PROFILE_KEY, profileKey);
        f.setArguments(args);
        return f;
    }

    public ModVersionPickerFragment() {
        super(R.layout.fragment_version_picker);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mModItem = (ModItem) getArguments().getSerializable(ARG_MOD_ITEM);
            mProfileKey = getArguments().getString(ARG_PROFILE_KEY);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mBackButton = view.findViewById(R.id.version_picker_back);
        mTitleView = view.findViewById(R.id.version_picker_title);
        mLoadingView = view.findViewById(R.id.version_picker_loading);
        mVersionList = view.findViewById(R.id.version_list);
        mPaginationFooter = view.findViewById(R.id.pagination_footer);
        mPaginationText = view.findViewById(R.id.pagination_text);
        mPrevButton = view.findViewById(R.id.pagination_prev);
        mNextButton = view.findViewById(R.id.pagination_next);
        mErrorView = view.findViewById(R.id.version_picker_error);

        if (mModItem != null) {
            mTitleView.setText(mModItem.title);
        }

        mBackButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        mVersionList.setLayoutManager(new LinearLayoutManager(requireContext()));
        mVersionList.addItemDecoration(new net.kdt.pojavlaunch.modloaders.modpacks.SpacesItemDecoration(8));
        mAdapter = new VersionAdapter();
        mVersionList.setAdapter(mAdapter);
        mVersionList.setItemAnimator(null); // Disable default animation, use custom

        mPrevButton.setOnClickListener(v -> goToPage(mCurrentPage - 1));
        mNextButton.setOnClickListener(v -> goToPage(mCurrentPage + 1));

        loadVersions();
    }

    private void loadVersions() {
        mLoadingView.setVisibility(View.VISIBLE);
        mErrorView.setVisibility(View.GONE);

        PojavApplication.sExecutorService.execute(() -> {
            try {
                ModpackApi api;
                if (mModItem.apiSource == Constants.SOURCE_MODRINTH) {
                    api = new ModrinthApi();
                } else {
                    api = new CommonApi(requireContext().getString(R.string.curseforge_api_key));
                }

                ModDetail detail = api.getModDetails(mModItem);
                Tools.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    mLoadingView.setVisibility(View.GONE);
                    if (detail != null && detail.versionNames != null && detail.versionNames.length > 0) {
                        mModDetail = detail;
                        buildVersionList(detail);
                    } else {
                        mErrorView.setVisibility(View.VISIBLE);
                        mErrorView.setText(R.string.search_modpack_download_error);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Failed to load versions", e);
                Tools.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    mLoadingView.setVisibility(View.GONE);
                    mErrorView.setVisibility(View.VISIBLE);
                    mErrorView.setText(getString(R.string.search_modpack_download_error));
                });
            }
        });
    }

    // Simple version comparator — prefers higher MC version strings first,
    // falls back to original index order for ties.
    private java.util.Comparator<VersionEntry> sLatestFirst = (a, b) -> {
        // Parse X.Y.Z numeric prefix from MC version for comparison
        String mcA = a.mcVersion.replaceAll("[^0-9.]", "");
        String mcB = b.mcVersion.replaceAll("[^0-9.]", "");
        String[] partsA = mcA.isEmpty() ? new String[]{"0"} : mcA.split("\\.");
        String[] partsB = mcB.isEmpty() ? new String[]{"0"} : mcB.split("\\.");
        for (int i = 0; i < Math.min(partsA.length, partsB.length); i++) {
            try {
                int cmp = Integer.compare(
                        Integer.parseInt(partsB[i]),
                        Integer.parseInt(partsA[i]));
                if (cmp != 0) return cmp;
            } catch (NumberFormatException ignored) {}
        }
        // If MC versions equal, compare the full version name descending
        return b.name.compareTo(a.name);
    };

    private void buildVersionList(ModDetail detail) {
        mAllVersions.clear();
        for (int i = 0; i < detail.versionNames.length; i++) {
            String mcVer = (detail.mcVersionNames != null && i < detail.mcVersionNames.length)
                    ? detail.mcVersionNames[i] : "";
            mAllVersions.add(new VersionEntry(
                    detail.versionNames[i],
                    mcVer,
                    detail.versionUrls[i],
                    i,
                    (detail.versionHashes != null && i < detail.versionHashes.length) ? detail.versionHashes[i] : null,
                    (detail.versionDependencyIds != null && i < detail.versionDependencyIds.length) ? detail.versionDependencyIds[i] : null,
                    (detail.versionDependencyTypes != null && i < detail.versionDependencyTypes.length) ? detail.versionDependencyTypes[i] : null
            ));
        }

        // Sort: latest MC version first
        java.util.Collections.sort(mAllVersions, sLatestFirst);

        mTotalPages = (int) Math.ceil((double) mAllVersions.size() / PAGE_SIZE);
        if (mTotalPages < 1) mTotalPages = 1;

        mPaginationFooter.setVisibility(mTotalPages > 1 ? View.VISIBLE : View.GONE);

        mCurrentPage = 0;
        showPage(0);
    }

    private void showPage(int page) {
        mCurrentPage = page;
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, mAllVersions.size());
        List<VersionEntry> pageItems = mAllVersions.subList(start, end);

        mAdapter.setData(pageItems);

        mPaginationText.setText("Page " + (page + 1) + " of " + mTotalPages);
        mPrevButton.setEnabled(page > 0);
        mNextButton.setEnabled(page < mTotalPages - 1);

        // Fade + translate animation on page change
        mVersionList.setAlpha(0.6f);
        mVersionList.setTranslationY(30f);
        mVersionList.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start();
    }

    private void goToPage(int page) {
        if (page < 0 || page >= mTotalPages) return;
        showPage(page);
    }

    private void openInstallScreen(VersionEntry entry) {
        ModInstallFragment fragment = ModInstallFragment.newInstance(
                mModItem, mModDetail, entry.index, mProfileKey);
        Fragment parent = getParentFragment();
        Bundle args = fragment.getArguments();
        if (parent instanceof MainMenuFragment) {
            ((MainMenuFragment) parent).openChildPane(ModInstallFragment.class, ModInstallFragment.TAG, args);
        } else if (parent != null) {
            parent.getChildFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.fade_in_slide_up, R.anim.fade_out_slide_down,
                            R.anim.fade_in_slide_up, R.anim.fade_out_slide_down)
                    .setReorderingAllowed(true)
                    .replace(R.id.right_pane_container, ModInstallFragment.class, args, ModInstallFragment.TAG)
                    .addToBackStack(ModInstallFragment.TAG)
                    .commit();
        } else if (getActivity() != null) {
            Tools.swapFragment(getActivity(), ModInstallFragment.class, ModInstallFragment.TAG, args);
        }
    }

    // ── Data class ────────────────────────────────────────────

    static class VersionEntry {
        final String name;
        final String mcVersion;
        final String url;
        final int index;
        final String hash;
        final String[] depIds;
        final String[] depTypes;

        VersionEntry(String name, String mcVersion, String url, int index,
                     String hash, String[] depIds, String[] depTypes) {
            this.name = name;
            this.mcVersion = mcVersion;
            this.url = url;
            this.index = index;
            this.hash = hash;
            this.depIds = depIds;
            this.depTypes = depTypes;
        }
    }

    // ── RecyclerView Adapter ──────────────────────────────────

    private class VersionAdapter extends RecyclerView.Adapter<VersionAdapter.VH> {

        private List<VersionEntry> mData = new ArrayList<>();

        void setData(List<VersionEntry> data) {
            mData = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_version_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            VersionEntry entry = mData.get(position);
            holder.nameView.setText(entry.name);
            if (entry.mcVersion != null && !entry.mcVersion.isEmpty()) {
                holder.mcBadge.setVisibility(View.VISIBLE);
                holder.mcBadge.setText(entry.mcVersion);
            } else {
                holder.mcBadge.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(v -> openInstallScreen(entry));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView nameView;
            final TextView mcBadge;

            VH(View itemView) {
                super(itemView);
                nameView = itemView.findViewById(R.id.version_name);
                mcBadge = itemView.findViewById(R.id.version_mc_badge);
            }
        }
    }
}

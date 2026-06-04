package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.modloaders.modpacks.ModItemAdapter;
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackApi;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModrinthApi;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;

public class DownloadListFragment extends Fragment implements ModItemAdapter.SearchResultCallback {

    private static final String ARG_TYPE = "content_type";

    private String mContentType;
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private TextView mStatusText;
    private ModItemAdapter mAdapter;
    private ModpackApi mApi;

    private OnModItemClickListener mItemClickListener;

    public interface OnModItemClickListener {
        void onItemClick(ModItem item);
    }

    public void setOnModItemClickListener(OnModItemClickListener listener) {
        mItemClickListener = listener;
    }

    public String getContentType() {
        return mContentType;
    }

    public static DownloadListFragment newInstance(String type) {
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        DownloadListFragment f = new DownloadListFragment();
        f.setArguments(args);
        return f;
    }

    public DownloadListFragment() {
        super(R.layout.fragment_download_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mContentType = getArguments() != null ? getArguments().getString(ARG_TYPE, "mod") : "mod";

        mRecyclerView = view.findViewById(R.id.download_list);
        mProgressBar = view.findViewById(R.id.download_list_progress);
        mStatusText = view.findViewById(R.id.download_list_status);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.addItemDecoration(new net.kdt.pojavlaunch.modloaders.modpacks.SpacesItemDecoration(12));

        // Use ModrinthApi directly for non-standard types (CF doesn't support them)
        if (mContentType.equals("mod") || mContentType.equals("modpack")) {
            mApi = new CommonApi(requireContext().getString(R.string.curseforge_api_key));
        } else {
            mApi = new ModrinthApi();
        }

        mAdapter = new ModItemAdapter(getResources(), mApi, this);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(item -> {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(item);
            }
        });

        loadContent();
    }

    private void loadContent() {
        SearchFilters filters = new SearchFilters();
        filters.projectType = mContentType;
        filters.isModpack = mContentType.equals("modpack");
        filters.name = "";

        mProgressBar.setVisibility(View.VISIBLE);
        mAdapter.performSearchQuery(filters);
    }

    public void filter(String query) {
        filter(query, null, null);
    }

    public void filter(String query, @Nullable String mcVersion, @Nullable String modLoader) {
        SearchFilters filters = new SearchFilters();
        filters.projectType = mContentType;
        filters.isModpack = mContentType.equals("modpack");
        filters.name = query != null ? query : "";
        filters.mcVersion = mcVersion != null && !mcVersion.isEmpty() ? mcVersion : null;
        filters.modLoader = modLoader != null && !modLoader.isEmpty() ? modLoader : null;

        mProgressBar.setVisibility(View.VISIBLE);
        mAdapter.performSearchQuery(filters);
    }

    @Override
    public void onSearchFinished() {
        mProgressBar.setVisibility(View.GONE);
        mStatusText.setVisibility(View.GONE);
    }

    @Override
    public void onSearchError(int error) {
        mProgressBar.setVisibility(View.GONE);
        mStatusText.setVisibility(View.VISIBLE);
        switch (error) {
            case ERROR_INTERNAL:
                mStatusText.setTextColor(android.graphics.Color.RED);
                mStatusText.setText(R.string.search_mod_error);
                break;
            case ERROR_NO_RESULTS:
                mStatusText.setTextColor(mStatusText.getTextColors().getDefaultColor());
                mStatusText.setText(R.string.search_mod_no_result);
                break;
        }
    }
}

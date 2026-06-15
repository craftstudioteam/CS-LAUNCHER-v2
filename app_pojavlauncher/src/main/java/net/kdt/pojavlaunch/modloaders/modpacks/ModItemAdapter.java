package net.kdt.pojavlaunch.modloaders.modpacks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackApi;
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;

import java.util.concurrent.Future;

public class ModItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements TaskCountListener {
    private static final ModItem[] MOD_ITEMS_EMPTY = new ModItem[0];
    private static final int VIEW_TYPE_MOD_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    // unused
    private final ModIconCache mIconCache = ModIconCache.getInstance();
    private final SearchResultCallback mSearchResultCallback;
    private ModItem[] mModItems;
    private final ModpackApi mModpackApi;

    private final float mCornerDimensionCache;
    private Future<?> mTaskInProgress;
    private SearchFilters mSearchFilters;
    private SearchResult mCurrentResult;
    private boolean mLastPage;
    private boolean mTasksRunning;

    private OnItemClickListener mOnItemClickListener;

    public ModItemAdapter(Resources resources, ModpackApi api, SearchResultCallback callback) {
        mCornerDimensionCache = resources.getDimension(R.dimen._1sdp) / 250;
        mModpackApi = api;
        mModItems = new ModItem[]{};
        mSearchResultCallback = callback;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    public void performSearchQuery(SearchFilters searchFilters) {
        if (mTaskInProgress != null) {
            mTaskInProgress.cancel(true);
            mTaskInProgress = null;
        }
        this.mSearchFilters = searchFilters;
        this.mLastPage = false;
        mTaskInProgress = new SelfReferencingFuture(new SearchApiTask(mSearchFilters, null))
                .startOnExecutor(PojavApplication.sExecutorService);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        switch (viewType) {
            case VIEW_TYPE_MOD_ITEM:
                View view = inflater.inflate(R.layout.item_mod_modern, viewGroup, false);
                return new ModItemViewHolder(view);
            case VIEW_TYPE_LOADING:
                view = inflater.inflate(R.layout.view_loading, viewGroup, false);
                return new LoadingViewHolder(view);
            default:
                throw new RuntimeException("Unimplemented view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_MOD_ITEM:
                ((ModItemViewHolder) holder).bind(mModItems[position]);
                break;
            case VIEW_TYPE_LOADING:
                loadMoreResults();
                break;
        }
    }

    @Override
    public int getItemCount() {
        if (mModItems.length == 0) return 0;
        return mLastPage ? mModItems.length : mModItems.length + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= mModItems.length) return VIEW_TYPE_LOADING;
        return VIEW_TYPE_MOD_ITEM;
    }

    private void loadMoreResults() {
        if (mTaskInProgress != null) return;
        mTaskInProgress = new SelfReferencingFuture(new SearchApiTask(mSearchFilters, mCurrentResult))
                .startOnExecutor(PojavApplication.sExecutorService);
    }

    @Override
    public void onUpdateTaskCount(int taskCount) {
        mTasksRunning = taskCount != 0;
    }

    private String formatDownloads(String downloads) {
        try {
            long d = Long.parseLong(downloads);
            if (d >= 1000000) return (d / 1000000) + "M";
            if (d >= 1000) return (d / 1000) + "K";
            return String.valueOf(d);
        } catch (Exception e) {
            return downloads;
        }
    }

    private int getSourceDrawable(int apiSource) {
        switch (apiSource) {
            case net.kdt.pojavlaunch.modloaders.modpacks.models.Constants.SOURCE_CURSEFORGE:
                return R.drawable.ic_curseforge;
            case net.kdt.pojavlaunch.modloaders.modpacks.models.Constants.SOURCE_MODRINTH:
                return R.drawable.ic_modrinth;
            default:
                throw new RuntimeException("Unknown API source");
        }
    }

    // ── ViewHolder for compact mod cards ──────────────────────────────────

    public class ModItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ImageView mIconView;
        private final ImageView mSourceIconView;
        private final TextView mTitleView;
        private final TextView mInfoView;
        private final TextView mDescriptionView;
        private final ImageButton mLikeButton;
        private final ImageButton mInstallButton;
        private ModItem mCurrentItem;
        private final SharedPreferences mLikedPrefs;

        public ModItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mLikedPrefs = itemView.getContext().getSharedPreferences("liked_mods", Context.MODE_PRIVATE);
            mIconView = itemView.findViewById(R.id.mod_thumbnail_imageview);
            mSourceIconView = itemView.findViewById(R.id.mod_source_imageview);
            mTitleView = itemView.findViewById(R.id.mod_title_textview);
            mInfoView = itemView.findViewById(R.id.mod_info_textview);
            mDescriptionView = itemView.findViewById(R.id.mod_body_textview);
            mLikeButton = itemView.findViewById(R.id.btn_like);
            mInstallButton = itemView.findViewById(R.id.btn_install);
            itemView.setOnClickListener(this);
        }

        public void bind(ModItem item) {
            mCurrentItem = item;
            mTitleView.setText(item.title);

            // Info line: author + downloads
            StringBuilder info = new StringBuilder();
            if (item.author != null && !item.author.isEmpty()) {
                info.append("by ").append(item.author);
            }
            if (item.downloads != null && !item.downloads.isEmpty()) {
                if (info.length() > 0) info.append(" \u2022 ");
                info.append(formatDownloads(item.downloads)).append(" Downloads");
            }
            mInfoView.setText(info.toString());

            // Description
            if (item.description != null && !item.description.isEmpty()) {
                mDescriptionView.setText(item.description);
                mDescriptionView.setVisibility(View.VISIBLE);
            } else {
                mDescriptionView.setVisibility(View.GONE);
            }

            // Source badge
            mSourceIconView.setImageResource(getSourceDrawable(item.apiSource));

            // Icon loading
            mIconView.setImageDrawable(null);
            mIconCache.getImage(
                    bitmap -> {
                        if (mCurrentItem == item) {
                            if (bitmap != null) {
                                mIconView.setImageBitmap(bitmap);
                            } else {
                                mIconView.setImageResource(R.mipmap.ic_launcher_foreground);
                            }
                        }
                    },
                    item.getIconCacheTag(),
                    item.imageUrl
            );

            // Like button — restore persisted state
            String modId = item.id;
            boolean isLiked = mLikedPrefs.getBoolean(modId, false);
            mLikeButton.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            mLikeButton.setColorFilter(isLiked ? Color.parseColor("#FF4444") : Color.parseColor("#9CA3AF"));
            mLikeButton.setOnClickListener(v -> {
                boolean nowLiked = !mLikedPrefs.getBoolean(modId, false);
                mLikedPrefs.edit().putBoolean(modId, nowLiked).apply();
                mLikeButton.setImageResource(nowLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                mLikeButton.setColorFilter(nowLiked ? Color.parseColor("#FF4444") : Color.parseColor("#9CA3AF"));
            });

            // Install button — triggers same navigation as card click
            mInstallButton.setOnClickListener(v -> {
                if (mOnItemClickListener != null && mCurrentItem != null) {
                    mOnItemClickListener.onItemClick(mCurrentItem);
                }
            });
        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener != null && mCurrentItem != null) {
                mOnItemClickListener.onItemClick(mCurrentItem);
            }
        }
    }

    private static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(View view) {
            super(view);
        }
    }

    private class SearchApiTask implements SelfReferencingFuture.FutureInterface {
        private final SearchFilters mSearchFilters;
        private final SearchResult mPreviousResult;

        private SearchApiTask(SearchFilters searchFilters, SearchResult previousResult) {
            this.mSearchFilters = searchFilters;
            this.mPreviousResult = previousResult;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void run(Future<?> myFuture) {
            SearchResult result = mModpackApi.searchMod(mSearchFilters, mPreviousResult);
            ModItem[] resultModItems = result != null ? result.results : null;
            if (resultModItems != null && resultModItems.length != 0 && mPreviousResult != null) {
                ModItem[] newModItems = new ModItem[resultModItems.length + mModItems.length];
                System.arraycopy(mModItems, 0, newModItems, 0, mModItems.length);
                System.arraycopy(resultModItems, 0, newModItems, mModItems.length, resultModItems.length);
                resultModItems = newModItems;
            }
            ModItem[] finalModItems = resultModItems;
            Tools.runOnUiThread(() -> {
                if (myFuture.isCancelled()) return;
                mTaskInProgress = null;
                if (finalModItems == null) {
                    mSearchResultCallback.onSearchError(SearchResultCallback.ERROR_INTERNAL);
                } else if (finalModItems.length == 0) {
                    if (mPreviousResult != null) {
                        mLastPage = true;
                        notifyItemChanged(mModItems.length);
                        mSearchResultCallback.onSearchFinished();
                        return;
                    }
                    mSearchResultCallback.onSearchError(SearchResultCallback.ERROR_NO_RESULTS);
                } else {
                    mSearchResultCallback.onSearchFinished();
                }
                mCurrentResult = result;
                if (finalModItems == null) {
                    mModItems = MOD_ITEMS_EMPTY;
                    notifyDataSetChanged();
                    return;
                }
                if (mPreviousResult != null) {
                    int prevLength = mModItems.length;
                    mModItems = finalModItems;
                    notifyItemChanged(prevLength);
                    notifyItemRangeInserted(prevLength + 1, mModItems.length);
                } else {
                    mModItems = finalModItems;
                    notifyDataSetChanged();
                }
            });
        }
    }

    public interface SearchResultCallback {
        int ERROR_INTERNAL = 0;
        int ERROR_NO_RESULTS = 1;
        void onSearchFinished();
        void onSearchError(int error);
    }

    public interface OnItemClickListener {
        void onItemClick(ModItem item);
    }
}

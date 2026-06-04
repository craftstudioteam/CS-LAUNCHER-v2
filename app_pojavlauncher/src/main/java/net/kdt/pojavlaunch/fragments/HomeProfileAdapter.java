package net.kdt.pojavlaunch.fragments;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.profiles.ProfileIconCache;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.util.List;

public class HomeProfileAdapter extends RecyclerView.Adapter<HomeProfileAdapter.ViewHolder> {

    private static final String TAG = "HomeProfileAdapter";

    private final List<MinecraftProfile> mProfileList;
    private final List<String> mProfileKeys;
    private final OnProfileActionListener mListener;

    public interface OnProfileActionListener {
        void onProfilePlay(String profileKey, MinecraftProfile profile);
        void onProfileEdit(String profileKey, MinecraftProfile profile);
    }

    public HomeProfileAdapter(List<String> profileKeys, List<MinecraftProfile> profiles,
                              OnProfileActionListener listener) {
        mProfileKeys = profileKeys;
        mProfileList = profiles;
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_profile_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MinecraftProfile profile = mProfileList.get(position);
        String profileKey = mProfileKeys.get(position);

        holder.tvName.setText(profile.name);

        StringBuilder meta = new StringBuilder();
        if (profile.lastVersionId != null && !profile.lastVersionId.isEmpty()) {
            meta.append(profile.lastVersionId);
        }
        if (profile.lastUsed != null && !profile.lastUsed.isEmpty()) {
            if (meta.length() > 0) meta.append(" \u2022 ");
            String date = profile.lastUsed.length() >= 10
                    ? profile.lastUsed.substring(0, 10) : profile.lastUsed;
            meta.append(date);
        }
        holder.tvMeta.setText(meta.toString());

        bindIcon(holder.imgIcon, profileKey, profile);

        holder.cardRoot.setOnClickListener(v -> {
            if (mListener != null) mListener.onProfileEdit(profileKey, profile);
        });

        holder.btnPlay.setOnClickListener(v -> {
            if (mListener != null) mListener.onProfilePlay(profileKey, profile);
        });
    }

    /**
     * Binds the graphical icon resource to the profile card image view.
     * Falls back to a typed icon (fabric / quilt / default) if the data icon
     * is missing or invalid, eliminating empty/hollow gray boxes.
     */
    private void bindIcon(ImageView target, String profileKey, MinecraftProfile profile) {
        String icon = profile.icon;
        Drawable drawable = null;

        try {
            drawable = ProfileIconCache.fetchIcon(target.getResources(), profileKey, icon);
        } catch (Exception e) {
            Log.w(TAG, "Icon load failed for " + profileKey, e);
        }

        if (drawable == null) {
            drawable = resolveTypeFallback(target, profile.lastVersionId);
        }
        if (drawable == null) {
            drawable = ContextCompat.getDrawable(target.getContext(), R.drawable.ic_pojav_full);
        }
        target.setImageDrawable(drawable);
    }

    /**
     * Picks a type-aware fallback icon based on the profile's MC version id
     * (e.g. "fabric-loader-1.20.1" → fabric icon). Avoids empty boxes when
     * the base64 icon payload is missing or corrupted.
     */
    private Drawable resolveTypeFallback(ImageView target, String lastVersionId) {
        if (lastVersionId == null) return null;
        String lower = lastVersionId.toLowerCase();
        int resId = -1;
        if (lower.contains("fabric")) resId = R.drawable.ic_fabric;
        else if (lower.contains("quilt")) resId = R.drawable.ic_quilt;
        else if (lower.contains("forge")) resId = R.drawable.ic_pojav_full;
        else if (lower.contains("neoforge")) resId = R.drawable.ic_pojav_full;
        if (resId == -1) return null;
        return ContextCompat.getDrawable(target.getContext(), resId);
    }

    @Override
    public int getItemCount() {
        return mProfileList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View cardRoot;
        final ImageView imgIcon;
        final TextView tvName;
        final TextView tvMeta;
        final FrameLayout btnPlay;

        ViewHolder(View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_profile_root);
            imgIcon = itemView.findViewById(R.id.img_profile_icon);
            tvName = itemView.findViewById(R.id.tv_profile_name);
            tvMeta = itemView.findViewById(R.id.tv_profile_meta);
            btnPlay = itemView.findViewById(R.id.btn_profile_play);
        }
    }
}


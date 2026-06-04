package net.kdt.pojavlaunch.fragments;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.profiles.ProfileIconCache;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.util.List;

public class HomeProfileAdapter extends RecyclerView.Adapter<HomeProfileAdapter.ViewHolder> {

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

        holder.imgIcon.setImageDrawable(ProfileIconCache.fetchIcon(
                holder.imgIcon.getResources(), profileKey, profile.icon));

        holder.cardRoot.setOnClickListener(v -> {
            if (mListener != null) mListener.onProfileEdit(profileKey, profile);
        });

        holder.btnPlay.setOnClickListener(v -> {
            if (mListener != null) mListener.onProfilePlay(profileKey, profile);
        });
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

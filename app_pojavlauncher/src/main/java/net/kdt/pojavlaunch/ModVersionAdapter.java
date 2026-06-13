package net.kdt.pojavlaunch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ModVersionAdapter extends RecyclerView.Adapter<ModVersionAdapter.ViewHolder> {

    public interface OnVersionSelectedListener {
        void onVersionSelected(ModrinthVersion version);
    }

    private List<ModrinthVersion> mVersions = new ArrayList<>();
    private final OnVersionSelectedListener mListener;
    private int mSelectedPosition = -1;

    public ModVersionAdapter(OnVersionSelectedListener listener) {
        mListener = listener;
    }

    public void setVersions(List<ModrinthVersion> versions) {
        mVersions = versions;
        notifyDataSetChanged();
    }

    public ModrinthVersion getSelectedVersion() {
        if (mSelectedPosition >= 0 && mSelectedPosition < mVersions.size()) {
            return mVersions.get(mSelectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mod_version, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModrinthVersion version = mVersions.get(position);
        holder.tvName.setText("Optix Client");
        
        String verNum = version.version_number != null ? version.version_number : "Unknown";
        holder.tvBadgeVersion.setText("v" + verNum);
        
        String mcVer = (version.game_versions != null && !version.game_versions.isEmpty() ? version.game_versions.get(0) : "Unknown MC");
        holder.tvBadgeMc.setText(mcVer);

        String loader = (version.loaders != null && !version.loaders.isEmpty() ? version.loaders.get(0) : "fabric");
        if (loader.length() > 0) {
            loader = loader.substring(0, 1).toUpperCase() + loader.substring(1);
        }
        holder.tvBadgeLoader.setText(loader);

        String type = version.version_type != null ? version.version_type : "release";
        if (type.equalsIgnoreCase("release")) {
            holder.tvBadgeStable.setText("Stable");
            holder.tvBadgeStable.setTextColor(0xFF39FF14);
            holder.tvBadgeStable.setBackgroundResource(R.drawable.bg_stable_pill);
        } else {
            holder.tvBadgeStable.setText(type.substring(0, 1).toUpperCase() + type.substring(1));
            holder.tvBadgeStable.setTextColor(0xFFFFC107);
            holder.tvBadgeStable.setBackgroundResource(R.drawable.bg_badge_pill);
        }

        View.OnClickListener clickListener = v -> {
            int oldPos = mSelectedPosition;
            mSelectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(mSelectedPosition);
            if (mListener != null) {
                mListener.onVersionSelected(version);
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        if (holder.btnInstall != null) {
            holder.btnInstall.setOnClickListener(clickListener);
            holder.btnInstall.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(120)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                        break;
                }
                return false;
            });
        }

        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(120)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                    break;
            }
            return false;
        });

        holder.itemView.setSelected(mSelectedPosition == position);
    }

    @Override
    public int getItemCount() {
        return mVersions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBadgeVersion, tvBadgeMc, tvBadgeLoader, tvBadgeStable;
        View btnInstall;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_version_name);
            tvBadgeVersion = itemView.findViewById(R.id.tv_badge_version);
            tvBadgeMc = itemView.findViewById(R.id.tv_badge_mc);
            tvBadgeLoader = itemView.findViewById(R.id.tv_badge_loader);
            tvBadgeStable = itemView.findViewById(R.id.tv_badge_stable);
            btnInstall = itemView.findViewById(R.id.btn_card_install);
        }
    }

    public static class ModrinthVersion {
        public String id;
        public String name;
        public String version_number;
        public String version_type;
        public List<String> game_versions;
        public List<String> loaders;
        public List<ModrinthFile> files;

        public static class ModrinthFile {
            public String url;
            public String filename;
        }
    }
}

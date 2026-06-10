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
        holder.tvName.setText(version.version_number + " | " + version.name);
        String info = (version.game_versions != null && !version.game_versions.isEmpty() ? version.game_versions.get(0) : "Unknown MC")
                + " [" + (version.loaders != null && !version.loaders.isEmpty() ? version.loaders.get(0) : "Unknown Loader") + "]";
        holder.tvInfo.setText(info);

        holder.itemView.setSelected(mSelectedPosition == position);
        holder.itemView.setOnClickListener(v -> {
            int oldPos = mSelectedPosition;
            mSelectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(mSelectedPosition);
            if (mListener != null) {
                mListener.onVersionSelected(version);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mVersions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_version_name);
            tvInfo = itemView.findViewById(R.id.tv_version_info);
        }
    }

    public static class ModrinthVersion {
        public String id;
        public String name;
        public String version_number;
        public List<String> game_versions;
        public List<String> loaders;
        public List<ModrinthFile> files;

        public static class ModrinthFile {
            public String url;
            public String filename;
        }
    }
}

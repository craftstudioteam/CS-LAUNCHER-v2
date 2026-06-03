package net.kdt.pojavlaunch.modloaders;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocalPackAdapter extends RecyclerView.Adapter<LocalPackAdapter.PackViewHolder> {

    public interface EmptyStateListener {
        void onEmptyStateChanged(boolean isEmpty);
    }

    private final List<File> mPacks = new ArrayList<>();
    private final EmptyStateListener mEmptyListener;

    public LocalPackAdapter(File packDir, EmptyStateListener listener) {
        mEmptyListener = listener;
        if (packDir != null && packDir.isDirectory()) {
            File[] files = packDir.listFiles(f -> {
                String name = f.getName();
                return name.endsWith(".zip") || (f.isDirectory() && !name.startsWith("."));
            });
            if (files != null) {
                Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                mPacks.addAll(Arrays.asList(files));
            }
        }
        notifyEmptyState();
    }

    @NonNull
    @Override
    public PackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_local_pack, parent, false);
        return new PackViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PackViewHolder holder, int position) {
        holder.bind(mPacks.get(position));
    }

    @Override
    public int getItemCount() { return mPacks.size(); }

    private void notifyEmptyState() {
        if (mEmptyListener != null) mEmptyListener.onEmptyStateChanged(mPacks.isEmpty());
    }

    class PackViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final ImageButton delete;

        PackViewHolder(@NonNull View itemView) {
            super(itemView);
            icon   = itemView.findViewById(R.id.local_pack_icon);
            name   = itemView.findViewById(R.id.local_pack_name);
            delete = itemView.findViewById(R.id.local_pack_delete);
        }

        void bind(File file) {
            String displayName = file.getName();
            if (displayName.endsWith(".zip")) displayName = displayName.substring(0, displayName.length() - 4);
            name.setText(displayName);
            
            icon.setImageResource(file.isDirectory() ? R.drawable.ic_folder : R.drawable.ic_folder_managed);

            delete.setOnClickListener(v -> {
                Context ctx = v.getContext();
                new AlertDialog.Builder(ctx)
                        .setTitle(ctx.getString(R.string.manage_mods_delete_confirm, displayName))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (d, i) -> {
                            org.apache.commons.io.FileUtils.deleteQuietly(file);
                            int p = getBindingAdapterPosition();
                            if (p != RecyclerView.NO_POSITION) {
                                mPacks.remove(p);
                                notifyItemRemoved(p);
                                notifyEmptyState();
                            }
                        })
                        .show();
            });
        }
    }
}

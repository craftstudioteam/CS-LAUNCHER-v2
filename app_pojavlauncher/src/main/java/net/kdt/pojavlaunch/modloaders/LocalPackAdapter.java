package net.kdt.pojavlaunch.modloaders;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LocalPackAdapter extends RecyclerView.Adapter<LocalPackAdapter.PackViewHolder> {

    public interface EmptyStateListener {
        void onEmptyStateChanged(boolean isEmpty);
    }

    private final List<File> mPacks = new ArrayList<>();
    private final EmptyStateListener mEmptyListener;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

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
            
            final String finalDisplayName = displayName;
            icon.setTag(file.getAbsolutePath());
            icon.setImageResource(file.isDirectory() ? R.drawable.ic_folder : R.drawable.ic_folder_managed);

            // Async Icon Loading
            final String expectedTag = file.getAbsolutePath();
            final WeakReference<ImageView> iconRef = new WeakReference<>(icon);
            PojavApplication.sExecutorService.execute(() -> {
                Bitmap bmp = extractPackIcon(file);
                if (bmp == null) return;
                mMainHandler.post(() -> {
                    ImageView iv = iconRef.get();
                    if (iv != null && expectedTag.equals(iv.getTag())) {
                        iv.setImageBitmap(bmp);
                    }
                });
            });

            delete.setOnClickListener(v -> {
                Context ctx = v.getContext();
                new AlertDialog.Builder(ctx)
                        .setTitle(ctx.getString(R.string.manage_mods_delete_confirm, finalDisplayName))
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

    @Nullable
    private Bitmap extractPackIcon(File file) {
        try {
            if (file.isDirectory()) {
                File iconFile = new File(file, "pack.png");
                if (iconFile.exists()) {
                    try (InputStream is = new FileInputStream(iconFile)) {
                        return BitmapFactory.decodeStream(is);
                    }
                }
            } else if (file.getName().endsWith(".zip")) {
                try (ZipFile zip = new ZipFile(file)) {
                    ZipEntry entry = zip.getEntry("pack.png");
                    if (entry != null) {
                        try (InputStream is = zip.getInputStream(entry)) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buf = new byte[8192];
                            int read;
                            while ((read = is.read(buf)) != -1) baos.write(buf, 0, read);
                            byte[] bytes = baos.toByteArray();
                            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}

package net.kdt.pojavlaunch.fragments;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ApiHandler;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CurseForgeWorldsFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private WorldsAdapter mAdapter;
    private final List<WorldEntry> mWorlds = new ArrayList<>();

    public static CurseForgeWorldsFragment newInstance() {
        return new CurseForgeWorldsFragment();
    }

    public CurseForgeWorldsFragment() {
        super(R.layout.fragment_download_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mRecyclerView = view.findViewById(R.id.download_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.addItemDecoration(new net.kdt.pojavlaunch.modloaders.modpacks.SpacesItemDecoration(12));

        mAdapter = new WorldsAdapter(mWorlds, world -> {
            if (world.websiteUrl != null && !world.websiteUrl.isEmpty()) {
                Tools.openURL(requireActivity(), world.websiteUrl);
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        loadWorlds();
    }

    private void loadWorlds() {
        String apiKey = getString(R.string.curseforge_api_key);
        ApiHandler api = new ApiHandler("https://api.curseforge.com/v1", apiKey);

        PojavApplication.sExecutorService.execute(() -> {
            try {
                HashMap<String, Object> params = new HashMap<>();
                params.put("gameId", 432);
                params.put("classId", 17);
                params.put("sortField", 2);
                params.put("sortOrder", "desc");
                params.put("pageSize", 20);
                params.put("index", 0);

                JsonObject response = api.get("mods/search", params, JsonObject.class);
                if (response == null) return;

                JsonArray data = response.getAsJsonArray("data");
                if (data == null) return;

                List<WorldEntry> results = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    JsonObject entry = data.get(i).getAsJsonObject();
                    WorldEntry w = new WorldEntry();
                    w.id = entry.get("id").getAsString();
                    w.name = entry.get("name").getAsString();
                    w.description = entry.has("summary") ? entry.get("summary").getAsString() : "";

                    if (entry.has("authors")) {
                        JsonArray authors = entry.getAsJsonArray("authors");
                        if (authors.size() > 0) {
                            w.author = authors.get(0).getAsJsonObject().get("name").getAsString();
                        }
                    }

                    w.downloads = entry.has("downloadCount") ? entry.get("downloadCount").getAsString() : "0";

                    if (entry.has("logo")) {
                        JsonObject logo = entry.getAsJsonObject("logo");
                        if (logo.has("thumbnailUrl") && !logo.get("thumbnailUrl").isJsonNull()) {
                            w.thumbnailUrl = logo.get("thumbnailUrl").getAsString();
                        }
                    }

                    if (entry.has("links")) {
                        JsonObject links = entry.getAsJsonObject("links");
                        if (links.has("websiteUrl") && !links.get("websiteUrl").isJsonNull()) {
                            w.websiteUrl = links.get("websiteUrl").getAsString();
                        }
                    }

                    if (w.websiteUrl == null) {
                        w.websiteUrl = "https://www.curseforge.com/minecraft/worlds/" + w.id;
                    }

                    results.add(w);
                }

                Tools.runOnUiThread(() -> {
                    mWorlds.clear();
                    mWorlds.addAll(results);
                    mAdapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    static class WorldEntry {
        String id, name, author, downloads, description, thumbnailUrl, websiteUrl;
    }

    static class WorldsAdapter extends RecyclerView.Adapter<WorldsAdapter.ViewHolder> {

        private final List<WorldEntry> mList;
        private final OnWorldClickListener mListener;

        interface OnWorldClickListener {
            void onWorldClick(WorldEntry world);
        }

        WorldsAdapter(List<WorldEntry> list, OnWorldClickListener listener) {
            mList = list;
            mListener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_curseforge_world, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WorldEntry world = mList.get(position);
            holder.nameView.setText(world.name);
            holder.authorView.setText(world.author != null ? world.author : "Unknown");
            holder.downloadsView.setText(formatDownloads(world.downloads));

            // Load thumbnail async
            if (world.thumbnailUrl != null && !world.thumbnailUrl.isEmpty()) {
                loadThumbnail(holder.thumbView, world.thumbnailUrl);
            } else {
                holder.thumbView.setImageResource(R.drawable.ic_mc_block);
            }

            holder.itemView.setOnClickListener(v -> {
                if (mListener != null) mListener.onWorldClick(world);
            });
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView thumbView;
            final TextView nameView;
            final TextView authorView;
            final TextView downloadsView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                thumbView = itemView.findViewById(R.id.world_thumbnail);
                nameView = itemView.findViewById(R.id.world_name);
                authorView = itemView.findViewById(R.id.world_author);
                downloadsView = itemView.findViewById(R.id.world_downloads);
            }
        }

        private static String formatDownloads(String dls) {
            try {
                long d = Long.parseLong(dls);
                if (d >= 1000000) return (d / 1000000) + "M";
                if (d >= 1000) return (d / 1000) + "K";
                return String.valueOf(d);
            } catch (Exception e) {
                return dls != null ? dls : "0";
            }
        }

        private static void loadThumbnail(ImageView imageView, String url) {
            PojavApplication.sExecutorService.execute(() -> {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setDoInput(true);
                    conn.connect();

                    try (InputStream is = conn.getInputStream()) {
                        Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
                        if (bitmap != null) {
                            Drawable drawable = new BitmapDrawable(imageView.getResources(), bitmap);
                            Tools.runOnUiThread(() -> imageView.setImageDrawable(drawable));
                        }
                    }
                    conn.disconnect();
                } catch (Exception ignored) {}
            });
        }
    }
}

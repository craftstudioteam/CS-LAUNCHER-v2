package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

public class FastClientHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_fastclient, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // PLAY button
        TextView btnPlay = view.findViewById(R.id.btn_play_main);
        btnPlay.setOnClickListener(v -> {
            // Existing launch logic should go here
        });

        // Folder row
        LinearLayout btnFolder = view.findViewById(R.id.btn_open_folder);
        btnFolder.setOnClickListener(v -> {
            // Folder open logic
        });

        // Server cards
        int[] serverCardIds = {
            R.id.card_server_1,
            R.id.card_server_2,
            R.id.card_server_3,
            R.id.card_server_4,
            R.id.card_server_5
        };
        String[] serverAddresses = {
            "bananasmp.net",
            "fast.ascendiamc.com",
            "play.happymc.fun",
            "insanesmp.net",
            "fast.eternalnetwork.club"
        };

        for (int i = 0; i < serverCardIds.length; i++) {
            final String address = serverAddresses[i];
            View card = view.findViewById(serverCardIds[i]);
            if (card != null) {
                card.setOnClickListener(v -> quickJoin(address));
            }
        }

        // View all servers
        TextView tvViewAll = view.findViewById(R.id.tv_view_all_servers);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                // All servers screen
            });
        }
    }

    private void quickJoin(String serverAddress) {
        // Implementation for quick join
    }
}

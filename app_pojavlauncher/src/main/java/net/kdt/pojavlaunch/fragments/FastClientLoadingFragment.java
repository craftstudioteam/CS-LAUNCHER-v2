package net.kdt.pojavlaunch.fragments;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

public class FastClientLoadingFragment extends Fragment {

    private Runnable onComplete;

    public static FastClientLoadingFragment newInstance(String version) {
        FastClientLoadingFragment f = new FastClientLoadingFragment();
        Bundle b = new Bundle();
        b.putString("ver", version);
        f.setArguments(b);
        return f;
    }

    public void setOnComplete(Runnable r) { this.onComplete = r; }

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        return i.inflate(R.layout.fragment_fastclient_loading, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String ver = getArguments() != null ? getArguments().getString("ver", "v1.6.2") : "v1.6.2";

        ProgressBar pb      = view.findViewById(R.id.pb_fastclient_progress);
        TextView tvPercent  = view.findViewById(R.id.tv_progress_percent);
        TextView tvStatus   = view.findViewById(R.id.tv_loading_status);
        TextView tvVerTag   = view.findViewById(R.id.tv_loading_version_tag);

        tvVerTag.setText(ver);

        String[] msgs  = {"Preparing installation...", "Downloading FastClient...",
                          "Verifying files...", "Applying patches...", "Done! Launching..."};
        int[]    progs = {0, 20, 60, 80, 100};
        int[]    delay = {0, 600, 1200, 1800, 2400};

        Handler h = new Handler(Looper.getMainLooper());

        for (int i = 0; i < msgs.length; i++) {
            final int idx = i;
            h.postDelayed(() -> {
                tvStatus.setText(msgs[idx]);
                tvPercent.setText(progs[idx] + "%");
                ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), progs[idx])
                              .setDuration(500)
                              .start();

                if (idx == msgs.length - 1) {
                    h.postDelayed(() -> {
                        if (onComplete != null) onComplete.run();
                        if (getParentFragmentManager().getBackStackEntryCount() > 0)
                            getParentFragmentManager().popBackStack();
                    }, 600);
                }
            }, delay[i]);
        }
    }
}

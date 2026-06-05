package net.kdt.pojavlaunch.fragments;

import android.widget.TextView;
import androidx.fragment.app.Fragment;
import net.kdt.pojavlaunch.R;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.view.animation.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CursorCustomizationFragment extends Fragment {

    public static final String TAG = "CursorCustomizationFragment";
    private View currentTab;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup entrance animations
        animateInitialEntry(view);

        // Setup tab switching
        setupTabAnimations(view);

        // Setup button press animations
        setupAllButtonAnimations(view);
    }

    /**
     * Smooth initial entrance animation
     */
    private void animateInitialEntry(View root) {
        View topBar = root.findViewById(R.id.cursor_top_bar);
        View importPanel = root.findViewById(R.id.panel_import);

        // Top bar slides down
        topBar.setTranslationY(-100f);
        topBar.setAlpha(0f);
        topBar.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(new DecelerateInterpolator(1.5f))
            .start();

        // Import panel fades in
        importPanel.setAlpha(0f);
        importPanel.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(150)
            .setInterpolator(new DecelerateInterpolator(1.5f))
            .start();
    }

    /**
     * Tab switching with smooth transitions
     */
    private void setupTabAnimations(View root) {
        View tabImport = root.findViewById(R.id.tab_import);
        View tabCollection = root.findViewById(R.id.tab_collection);

        View panelImport = root.findViewById(R.id.panel_import);
        View panelCollection = root.findViewById(R.id.panel_collection);

        // Default to Import tab
        currentTab = tabImport;
        panelImport.setVisibility(View.VISIBLE);

        tabImport.setOnClickListener(v -> {
            switchTab((TextView) v, (TextView) tabCollection);
            switchPanel(panelImport, panelCollection);
        });

        tabCollection.setOnClickListener(v -> {
            switchTab((TextView) v, (TextView) tabImport);
            switchPanel(panelCollection, panelImport);
        });
    }

    private void switchTab(TextView activeTab, TextView... otherTabs) {
        if (currentTab == activeTab) return;

        // Activate selected tab with smooth animation
        activeTab.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(150)
            .setInterpolator(new OvershootInterpolator(2f))
            .withEndAction(() -> {
                activeTab.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start();
            })
            .start();

        activeTab.setBackgroundResource(R.drawable.bg_tab_active);
        activeTab.setTextColor(0xFF000000);

        // Deactivate other tabs
        for (TextView tab : otherTabs) {
            tab.setBackground(null);
            tab.setTextColor(0xFF888888);
        }

        currentTab = activeTab;
    }

    private void switchPanel(View showPanel, View... hidePanels) {
        // Hide other panels with fade out
        for (View panel : hidePanels) {
            if (panel.getVisibility() == View.VISIBLE) {
                panel.animate()
                    .alpha(0f)
                    .translationY(20f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        panel.setVisibility(View.GONE);
                        panel.setTranslationY(0f);
                    })
                    .start();
            }
        }

        // Show new panel with fade in
        showPanel.setVisibility(View.VISIBLE);
        showPanel.setAlpha(0f);
        showPanel.setTranslationY(20f);

        showPanel.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setStartDelay(150)
            .setInterpolator(new DecelerateInterpolator(1.5f))
            .start();
    }

    /**
     * Button press animation with bounce
     */
    private void setupAllButtonAnimations(View root) {
        int[] buttonIds = {
            R.id.cursor_back_button,
            R.id.btn_import_png, R.id.btn_apply_import
        };

        for (int id : buttonIds) {
            View btn = root.findViewById(id);
            if (btn != null) {
                btn.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            v.animate()
                                .scaleX(0.90f)
                                .scaleY(0.90f)
                                .setDuration(80)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .start();
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .setInterpolator(new OvershootInterpolator(2.5f))
                                .start();
                            break;
                    }
                    return false;
                });
            }
        }
    }
}

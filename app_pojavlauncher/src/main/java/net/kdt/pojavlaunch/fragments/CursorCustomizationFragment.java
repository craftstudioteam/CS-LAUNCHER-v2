package net.kdt.pojavlaunch.fragments;

import android.animation.*;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CursorCustomizationFragment extends Fragment {

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
        
        // Setup color picker animations
        setupColorPickerAnimations(view);
    }

    /**
     * Smooth initial entrance animation
     */
    private void animateInitialEntry(View root) {
        View topBar = root.findViewById(R.id.cursor_top_bar);
        View createPanel = root.findViewById(R.id.panel_create);
        View bottomPanel = root.findViewById(R.id.tools_bottom_panel);
        
        // Top bar slides down
        topBar.setTranslationY(-100f);
        topBar.setAlpha(0f);
        topBar.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(new DecelerateInterpolator(1.5f))
            .start();
        
        // Create panel fades in with scale
        createPanel.setAlpha(0f);
        createPanel.setScaleX(0.95f);
        createPanel.setScaleY(0.95f);
        createPanel.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setStartDelay(150)
            .setInterpolator(new DecelerateInterpolator(1.5f))
            .start();
        
        // Bottom panel slides up
        bottomPanel.setTranslationY(100f);
        bottomPanel.setAlpha(0f);
        bottomPanel.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(450)
            .setStartDelay(200)
            .setInterpolator(new DecelerateInterpolator(1.5f))
            .start();
    }

    /**
     * Tab switching with smooth transitions
     */
    private void setupTabAnimations(View root) {
        View tabImport = root.findViewById(R.id.tab_import);
        View tabCreate = root.findViewById(R.id.tab_create);
        View tabCollection = root.findViewById(R.id.tab_collection);
        
        View panelCreate = root.findViewById(R.id.panel_create);
        View panelImport = root.findViewById(R.id.panel_import);
        View panelCollection = root.findViewById(R.id.panel_collection);
        
        currentTab = tabCreate;
        
        tabImport.setOnClickListener(v -> {
            switchTab((TextView) v, (TextView) tabCreate, (TextView) tabCollection);
            switchPanel(panelImport, panelCreate, panelCollection);
        });
        
        tabCreate.setOnClickListener(v -> {
            switchTab((TextView) v, (TextView) tabImport, (TextView) tabCollection);
            switchPanel(panelCreate, panelImport, panelCollection);
        });
        
        tabCollection.setOnClickListener(v -> {
            switchTab((TextView) v, (TextView) tabImport, (TextView) tabCreate);
            switchPanel(panelCollection, panelImport, panelCreate);
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
        
        // Show new panel with fade in + scale
        showPanel.setVisibility(View.VISIBLE);
        showPanel.setAlpha(0f);
        showPanel.setTranslationY(20f);
        showPanel.setScaleX(0.97f);
        showPanel.setScaleY(0.97f);
        
        showPanel.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(350)
            .setStartDelay(150)
            .setInterpolator(new DecelerateInterpolator(1.5f))
            .start();
    }

    /**
     * Smooth button press animation with bounce
     */
    private void setupAllButtonAnimations(View root) {
        int[] buttonIds = {
            R.id.cursor_back_button,
            R.id.btn_tool_pencil, R.id.btn_tool_eraser, R.id.btn_tool_fill,
            R.id.btn_undo, R.id.btn_redo, R.id.btn_clear_canvas,
            R.id.btn_save_creation, R.id.btn_import_png, R.id.btn_apply_import
        };
        
        for (int id : buttonIds) {
            View btn = root.findViewById(id);
            if (btn != null) {
                applyPressAnimation(btn);
            }
        }
    }

    private void applyPressAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
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

    /**
     * Color picker selection animation
     */
    private void setupColorPickerAnimations(View root) {
        int[] colorIds = {
            R.id.color_white, R.id.color_neon, R.id.color_red,
            R.id.color_blue, R.id.color_black, R.id.color_orange, R.id.color_purple
        };
        
        for (int id : colorIds) {
            View color = root.findViewById(id);
            if (color != null) {
                color.setOnClickListener(v -> {
                    // Selection pulse
                    AnimatorSet pulse = new AnimatorSet();
                    pulse.playTogether(
                        ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.3f, 1.1f),
                        ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.3f, 1.1f)
                    );
                    pulse.setDuration(300);
                    pulse.setInterpolator(new OvershootInterpolator(3f));
                    pulse.start();
                    
                    // Reset other colors
                    for (int otherId : colorIds) {
                        if (otherId != id) {
                            View other = root.findViewById(otherId);
                            if (other != null && other.getScaleX() != 1f) {
                                other.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(200)
                                    .start();
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * Tool selection animation
     */
    public void selectTool(View selectedTool, View... otherTools) {
        // Active tool animation
        selectedTool.setBackgroundResource(R.drawable.bg_tool_active);
        selectedTool.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(150)
            .setInterpolator(new OvershootInterpolator(2f))
            .withEndAction(() -> {
                selectedTool.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start();
            })
            .start();
        
        if (selectedTool instanceof ImageButton) {
            ((ImageButton) selectedTool).setColorFilter(0xFF000000);
        }
        
        // Inactive tools
        for (View tool : otherTools) {
            tool.setBackgroundResource(R.drawable.bg_tool_inactive);
            if (tool instanceof ImageButton) {
                ((ImageButton) tool).setColorFilter(0xFF888888);
            }
        }
    }
}

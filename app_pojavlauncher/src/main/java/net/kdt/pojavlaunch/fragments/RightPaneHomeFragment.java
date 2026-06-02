package net.kdt.pojavlaunch.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

import java.io.File;

/**
 * Default content of the right pane in landscape two-pane mode.
 * Shows a custom background (if set), otherwise a plain transparent pane.
 * Wiki and Discord buttons are pinned at the top.
 */
public class RightPaneHomeFragment extends Fragment {

    public static final String TAG = "RightPaneHomeFragment";
    /** File path where the custom launcher background image is stored. */
    public static final String CUSTOM_BG_PATH = Tools.DIR_DATA + "/custom_launcher_bg";

    public RightPaneHomeFragment() {
        super(R.layout.fragment_right_pane_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        loadBackground(view);

        // 1. Premium Fade and Slide In for the whole right pane
        view.setAlpha(0f);
        view.setTranslationX(60f);
        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(250)
            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
            .start();

        // 2. Floating effect for Center Logo
        ImageView centerLogo = view.findViewById(R.id.iv_center_logo);
        if (centerLogo != null) {
            android.animation.ObjectAnimator floatAnim = android.animation.ObjectAnimator.ofFloat(centerLogo, "translationY", 0f, -15f, 0f);
            floatAnim.setDuration(3500);
            floatAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            floatAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            floatAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            floatAnim.start();
        }

        // 3. Subtle Parallax/Ken Burns for Hero Background
        ImageView heroBg = view.findViewById(R.id.iv_hero_bg);
        if (heroBg != null) {
            android.animation.PropertyValuesHolder scaleX = android.animation.PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.05f, 1.0f);
            android.animation.PropertyValuesHolder scaleY = android.animation.PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.05f, 1.0f);
            android.animation.ObjectAnimator panAnim = android.animation.ObjectAnimator.ofPropertyValuesHolder(heroBg, scaleX, scaleY);
            panAnim.setDuration(20000);
            panAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            panAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            panAnim.setInterpolator(new android.view.animation.LinearInterpolator());
            panAnim.start();
        }
    }

    /**
     * Called after saving or removing a custom background so the pane
     * refreshes without needing a full fragment recreate.
     */
    public void reloadBackground() {
        View v = getView();
        if (v != null) loadBackground(v);
    }

    private void loadBackground(@NonNull View view) {
        ImageView wallpaper = view.findViewById(R.id.right_pane_wallpaper);
        File bgFile = new File(CUSTOM_BG_PATH);
        if (bgFile.exists()) {
            Drawable d = Drawable.createFromPath(bgFile.getAbsolutePath());
            if (d != null) {
                wallpaper.setImageDrawable(d);
                wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
                wallpaper.setBackground(null);
                wallpaper.setVisibility(View.VISIBLE);
                return;
            }
        }
        // No custom bg — show the gradient drawable as the pane background if gradient is on,
        // otherwise stay transparent (root fragment_launcher bg shows through).
        wallpaper.setImageDrawable(null);
        TypedValue tv = new TypedValue();
        view.getContext().getTheme().resolveAttribute(R.attr.bgMainDrawable, tv, true);
        if (tv.resourceId != 0) {
            wallpaper.setBackgroundResource(tv.resourceId);
            wallpaper.setVisibility(View.VISIBLE);
        } else {
            wallpaper.setBackground(null);
            wallpaper.setVisibility(View.GONE);
        }
    }
}
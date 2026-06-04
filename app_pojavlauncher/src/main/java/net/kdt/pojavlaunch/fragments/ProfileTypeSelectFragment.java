package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.hasNoOnlineProfileDialog;
import static net.kdt.pojavlaunch.Tools.hasOnlineProfile;

import android.animation.LayoutTransition;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import androidx.core.view.ViewCompat;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

public class ProfileTypeSelectFragment extends Fragment {
    public static final String TAG = "ProfileTypeSelectFragment";

    private LinearLayout mContent;

    public ProfileTypeSelectFragment() {
        super(R.layout.fragment_profile_type);
    }
    public ProfileTypeSelectFragment(int layout) {
        super(layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContent = view.findViewById(R.id.setup_hub_content);

        // Hardware acceleration
        if (getActivity() != null) {
            getActivity().getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        // Smooth layout transitions
        if (mContent != null) {
            LayoutTransition transition = new LayoutTransition();
            transition.enableTransitionType(LayoutTransition.CHANGING);
            transition.setDuration(LayoutTransition.APPEARING, 300);
            transition.setDuration(LayoutTransition.CHANGE_APPEARING, 300);
            transition.setInterpolator(LayoutTransition.APPEARING,
                    new DecelerateInterpolator());
            mContent.setLayoutTransition(transition);

            // Staggered entrance animation for buttons
            mContent.post(() -> animateButtonsEntry(mContent));
        }

        wireButton(view);
    }

    private void wireButton(@NonNull View view) {
        View b;
        b = view.findViewById(R.id.vanilla_profile);
        b.setOnClickListener(v -> navigateTo(ProfileEditorFragment.class, ProfileEditorFragment.TAG, new Bundle(1)));
        setupTouchAnimation(b);

        b = view.findViewById(R.id.optifine_profile);
        b.setOnClickListener(v -> tryInstall(OptiFineInstallFragment.class, OptiFineInstallFragment.TAG));
        setupTouchAnimation(b);

        b = view.findViewById(R.id.modded_profile_fabric);
        b.setOnClickListener(v -> tryInstall(FabricInstallFragment.class, FabricInstallFragment.TAG));
        setupTouchAnimation(b);

        b = view.findViewById(R.id.modded_profile_forge);
        b.setOnClickListener(v -> tryInstall(ForgeInstallFragment.class, ForgeInstallFragment.TAG));
        setupTouchAnimation(b);

        b = view.findViewById(R.id.modded_profile_neoforge);
        b.setOnClickListener(v -> tryInstall(NeoForgeInstallFragment.class, NeoForgeInstallFragment.TAG));
        setupTouchAnimation(b);

        b = view.findViewById(R.id.modded_profile_modpack);
        b.setOnClickListener(v -> tryInstall(ModpackCreateFragment.class, ModpackCreateFragment.TAG));
        setupTouchAnimation(b);

        b = view.findViewById(R.id.modded_profile_quilt);
        b.setOnClickListener(v -> tryInstall(QuiltInstallFragment.class, QuiltInstallFragment.TAG));
        setupTouchAnimation(b);

        b = view.findViewById(R.id.modded_profile_bta);
        b.setOnClickListener(v -> tryInstall(BTAInstallFragment.class, BTAInstallFragment.TAG));
        setupTouchAnimation(b);
    }

    /** Staggered entrance animation — buttons appear one by one smoothly */
    private void animateButtonsEntry(ViewGroup parent) {
        int delay = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(30f);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(350)
                    .setStartDelay(delay)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            delay += 50;
        }
    }

    /** Scale-press micro-interaction for all interactive buttons */
    private void setupTouchAnimation(View button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f)
                            .setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator(2f))
                            .start();
                    break;
            }
            return false;
        });
    }
    /** Navigate within right pane if inside MainMenuFragment, otherwise full-screen swap. */
    protected void navigateTo(Class<? extends Fragment> cls, String tag, Bundle args) {
        // Walk up to find MainMenuFragment (could be grandparent if nested)
        Fragment parent = getParentFragment();
        while (parent != null && !(parent instanceof MainMenuFragment)) {
            parent = parent.getParentFragment();
        }
        if (parent instanceof MainMenuFragment) {
            ((MainMenuFragment) parent).openChildPane(cls, tag, args);
        } else {
            Tools.swapFragment(requireActivity(), cls, tag, args);
        }
    }

    private void tryInstall(Class<? extends Fragment> fragmentClass, String tag){
        if(!hasOnlineProfile()){
            hasNoOnlineProfileDialog(requireActivity());
        } else {
            navigateTo(fragmentClass, tag, null);
        }
    }
}


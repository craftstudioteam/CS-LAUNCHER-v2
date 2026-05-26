package com.craftstudio.launcher.fragments;

import static com.craftstudio.launcher.event.single.RefreshVersionsEvent.MODE.END;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.craftstudio.launcher.anim.AnimPlayer;
import com.craftstudio.launcher.anim.animations.Animations;
import com.craftstudio.launcher.InfoCenter;
import com.craftstudio.launcher.R;
import com.craftstudio.launcher.databinding.FragmentLauncherBinding;
import com.craftstudio.launcher.event.single.AccountUpdateEvent;
import com.craftstudio.launcher.event.single.LaunchGameEvent;
import com.craftstudio.launcher.event.single.RefreshVersionsEvent;
import com.craftstudio.launcher.event.single.SwapToLoginEvent;
import com.craftstudio.launcher.feature.version.Version;
import com.craftstudio.launcher.feature.version.utils.VersionIconUtils;
import com.craftstudio.launcher.feature.version.VersionInfo;
import com.craftstudio.launcher.feature.version.VersionsManager;
import com.craftstudio.launcher.task.TaskExecutors;
import com.craftstudio.launcher.ui.dialog.EventPopupDialog;
import com.craftstudio.launcher.ui.dialog.TipDialog;
import com.craftstudio.launcher.ui.fragment.AboutFragment;
import com.craftstudio.launcher.ui.fragment.ControlButtonFragment;
import com.craftstudio.launcher.ui.fragment.DownloadFragment;
import com.craftstudio.launcher.ui.fragment.FilesFragment;
import com.craftstudio.launcher.ui.fragment.CursorStudioFragment;
import com.craftstudio.launcher.ui.fragment.FragmentWithAnim;
import com.craftstudio.launcher.ui.fragment.OfflineAccountSettingsFragment;
import com.craftstudio.launcher.ui.fragment.QuickSettingsFragment;
import com.craftstudio.launcher.ui.fragment.SettingsFragment;
import com.craftstudio.launcher.ui.fragment.VersionManagerFragment;
import com.craftstudio.launcher.ui.fragment.VersionsListFragment;
import com.craftstudio.launcher.ui.subassembly.account.AccountViewWrapper;
import com.craftstudio.launcher.utils.path.PathManager;
import com.craftstudio.launcher.utils.ZHTools;
import com.craftstudio.launcher.utils.anim.ViewAnimUtils;

import com.craftstudio.launcher.Tools;
import com.craftstudio.launcher.progresskeeper.ProgressKeeper;
import com.craftstudio.launcher.feature.accounts.AccountsManager;
import com.craftstudio.launcher.feature.accounts.AccountUtils;
import com.craftstudio.launcher.utils.skin.SkinLoader;
import com.craftstudio.launcher.value.MinecraftAccount;
import androidx.core.content.ContextCompat;
import android.graphics.drawable.Drawable;
import android.graphics.Color;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainMenuFragment extends FragmentWithAnim {
    public static final String TAG = "MainMenuFragment";
    private FragmentLauncherBinding binding;
    private AccountViewWrapper accountViewWrapper;

    public MainMenuFragment() {
        super(R.layout.fragment_launcher);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLauncherBinding.inflate(getLayoutInflater());
        accountViewWrapper = new AccountViewWrapper(this, binding.viewAccount);
        accountViewWrapper.refreshAccountInfo();
        refreshProfileCard();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.aboutText.setText(InfoCenter.replaceName(requireActivity(), R.string.about_tab));
        binding.aboutButton.setOnClickListener(v -> ZHTools.swapFragmentWithAnim(this, SettingsFragment.class, SettingsFragment.TAG, null));
        binding.aboutButtonBottom.setOnClickListener(v -> ZHTools.swapFragmentWithAnim(this, AboutFragment.class, AboutFragment.TAG, null));
        binding.modsButton.setOnClickListener(v -> {
            Version version = VersionsManager.INSTANCE.getCurrentVersion();
            if (version != null) {
                Bundle modsBundle = new Bundle();
                modsBundle.putString(com.craftstudio.launcher.ui.fragment.ManageModsFragment.BUNDLE_ROOT_PATH,
                        new java.io.File(version.getGameDir(), "mods").getAbsolutePath());
                ZHTools.swapFragmentWithAnim(this, com.craftstudio.launcher.ui.fragment.ManageModsFragment.class,
                        com.craftstudio.launcher.ui.fragment.ManageModsFragment.TAG, modsBundle);
            } else {
                Toast.makeText(requireContext(), R.string.version_manager_no_installed_version, Toast.LENGTH_SHORT).show();
            }
        });
        binding.customControlButton.setOnClickListener(v -> ZHTools.swapFragmentWithAnim(this, ControlButtonFragment.class, ControlButtonFragment.TAG, null));
        binding.openMainDirButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString(FilesFragment.BUNDLE_LIST_PATH, PathManager.DIR_GAME_HOME);
            ZHTools.swapFragmentWithAnim(this, FilesFragment.class, FilesFragment.TAG, bundle);
        });
        binding.installJarButton.setOnClickListener(v -> runInstallerWithConfirmation(false));
        binding.installJarButton.setOnLongClickListener(v -> {
            runInstallerWithConfirmation(true);
            return true;
        });
        binding.shareLogsButton.setOnClickListener(v -> ZHTools.shareLogs(requireActivity()));
        binding.premiumUpgradeCard.setOnClickListener(v -> openDownloadCenter());
        binding.upgradeNowButton.setOnClickListener(v -> openDownloadCenter());
        binding.btnManageMods.setOnClickListener(v -> {
            Version version = VersionsManager.INSTANCE.getCurrentVersion();
            if (version != null) {
                Bundle modsBundle = new Bundle();
                modsBundle.putString(com.craftstudio.launcher.ui.fragment.ManageModsFragment.BUNDLE_ROOT_PATH,
                        new java.io.File(version.getGameDir(), "mods").getAbsolutePath());
                ZHTools.swapFragmentWithAnim(this, com.craftstudio.launcher.ui.fragment.ManageModsFragment.class,
                        com.craftstudio.launcher.ui.fragment.ManageModsFragment.TAG, modsBundle);
            } else {
                Toast.makeText(requireContext(), R.string.version_manager_no_installed_version, Toast.LENGTH_SHORT).show();
            }
        });
        binding.newModsCard.setOnClickListener(v -> openDownloadCenter());
        binding.exploreModsButton.setOnClickListener(v -> openDownloadCenter());
        binding.manageFeaturesButton.setOnClickListener(v -> showClientFeaturesDialog());
        binding.profileSettingsButton.setOnClickListener(v -> 
            Toast.makeText(requireContext(), "Skin and Cape management coming soon", Toast.LENGTH_SHORT).show()
        );
        binding.logoutButton.setOnClickListener(v -> onProfileAuthButtonClick());

        binding.version.setOnClickListener(v -> {
            if (!isTaskRunning()) {
                ZHTools.swapFragmentWithAnim(this, VersionsListFragment.class, VersionsListFragment.TAG, null);
            } else {
                ViewAnimUtils.setViewAnim(binding.version, Animations.Shake);
                TaskExecutors.runInUIThread(() -> Toast.makeText(requireContext(), R.string.version_manager_task_in_progress, Toast.LENGTH_SHORT).show());
            }
        });
        binding.managerProfileButton.setOnClickListener(v -> {
            ViewAnimUtils.setViewAnim(binding.managerProfileButton, Animations.Pulse);
            ZHTools.swapFragmentWithAnim(this, QuickSettingsFragment.class, QuickSettingsFragment.TAG, null);
        });

        // Bell notification click listener (disabled — panel hidden)
        binding.bellNotificationFrame.setOnClickListener(v -> {});

        binding.playButton.setOnClickListener(v -> EventBus.getDefault().post(new LaunchGameEvent()));

        binding.versionName.setSelected(true);
        binding.versionInfo.setSelected(true);

        refreshCurrentVersion();

        // Show event popups on dashboard load
        if (EventPopupDialog.Companion.shouldShow(requireContext())) {
            EventPopupDialog.Companion.markShownThisSession();
            new EventPopupDialog(requireContext(), () -> {
                ZHTools.swapFragmentWithAnim(this,
                        CursorStudioFragment.class,
                        CursorStudioFragment.TAG,
                        null);
                return null;
            }).show();
        }
    }

    private void refreshProfileCard() {
        TaskExecutors.runInUIThread(() -> {
            MinecraftAccount account = AccountsManager.INSTANCE.getCurrentAccount();
            try {
                if (account != null) {
                    // try to refresh skin in background, then load avatar
                    TaskExecutors.getDefault().execute(() -> {
                        try {
                            if (AccountUtils.Companion.isMicrosoftAccount(account)) {
                                account.updateMicrosoftSkin();
                            } else if (AccountUtils.Companion.isOtherLoginAccount(account)) {
                                account.updateOtherSkin();
                            }
                        } catch (Exception ignored) {}

                        TaskExecutors.runInUIThread(() -> {
                            try {
                                int size = (int) Tools.dpToPx(getResources().getDimensionPixelSize(R.dimen._44sdp));
                                Drawable avatar = null;
                                try {
                                    avatar = SkinLoader.getAvatarDrawable(requireContext(), account, size);
                                } catch (Exception ignored) {}
                                if (avatar != null) binding.profileAvatar.setImageDrawable(avatar);
                                else binding.profileAvatar.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.default_head));
                            } catch (Exception ignored) {}
                        });
                    });

                    binding.profileUsername.setText(account.username);

                    if (AccountUtils.Companion.isNoLoginRequired(account)) {
                        binding.logoutText.setText("Log out");
                        binding.logoutIcon.setImageResource(R.drawable.ic_logout);
                        binding.premiumBadgeText.setText("Offline");
                        binding.premiumBadgeText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                        binding.premiumBadgeIcon.setImageResource(R.drawable.ic_logout);
                        binding.premiumBadgeIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                        binding.profileStatusText.setText("Offline");
                        binding.profileStatusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                    } else {
                        binding.logoutText.setText("Log out");
                        binding.logoutIcon.setImageResource(R.drawable.ic_logout);
                        binding.premiumBadgeText.setText("Premium User");
                        binding.premiumBadgeIcon.setImageResource(R.drawable.ic_crown);
                        binding.premiumBadgeIcon.setColorFilter(Color.parseColor("#FFB74D"));
                        binding.premiumBadgeText.setTextColor(Color.parseColor("#FFB74D"));
                        binding.profileStatusText.setText("Online");
                        binding.profileStatusText.setTextColor(Color.parseColor("#4CAF50"));
                    }
                } else {
                    binding.profileUsername.setText(R.string.generic_login);
                    binding.logoutText.setText(R.string.generic_login);
                    binding.logoutIcon.setImageResource(R.drawable.ic_add);
                    binding.premiumBadgeText.setText("Offline");
                    binding.premiumBadgeText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                    binding.premiumBadgeIcon.setImageResource(R.drawable.ic_logout);
                    binding.premiumBadgeIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                    binding.profileStatusText.setText("Offline");
                    binding.profileStatusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                    binding.profileAvatar.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.default_head));
                }

                // hide friends option as requested
                binding.friendsButton.setVisibility(View.GONE);
            } catch (Exception e) {
                // fail silently; keep defaults
            }
        });
    }

    private void refreshCurrentVersion() {
        Version version = VersionsManager.INSTANCE.getCurrentVersion();

        int versionInfoVisibility;
        if (version != null) {
            String versionName = version.getVersionName();
            binding.versionName.setText(versionName);
            binding.heroTitle.setText("Minecraft " + versionName);
            binding.playButton.setText("▶  PLAY  " + versionName);
            VersionInfo versionInfo = version.getVersionInfo();
            if (versionInfo != null) {
                binding.versionInfo.setText(versionInfo.getInfoString());
                versionInfoVisibility = View.VISIBLE;
                binding.modpackTagText.setText(resolveModpackTag(versionInfo));
            } else versionInfoVisibility = View.GONE;

            new VersionIconUtils(version).start(binding.versionIcon);
            binding.managerProfileButton.setVisibility(View.VISIBLE);
        } else {
            binding.versionName.setText(R.string.version_no_versions);
            binding.heroTitle.setText("Minecraft");
            binding.playButton.setText("▶  PLAY");
            binding.modpackTagText.setText("Vanilla");
            binding.managerProfileButton.setVisibility(View.GONE);
            versionInfoVisibility = View.GONE;
        }
        binding.versionInfo.setVisibility(versionInfoVisibility);
    }

    private void onProfileAuthButtonClick() {
        EventBus.getDefault().post(new SwapToLoginEvent());
    }

    private void openDownloadCenter() {
        ZHTools.swapFragmentWithAnim(this, DownloadFragment.class, DownloadFragment.TAG, null);
    }

    private void showClientFeaturesDialog() {
        String youtubeUrl = "https://youtube.com/@craft-studio-official";
        String message = youtubeUrl + "\n\nComing Soon. This feature will launch when we reach 4K subscribers!";

        new TipDialog.Builder(requireActivity())
                .setTitle(R.string.generic_tip)
                .setMessage(message)
                .setConfirm(R.string.generic_open)
                .setCancel(R.string.generic_close)
            .setConfirmClickListener(checked -> ZHTools.openLink(requireActivity(), youtubeUrl))
                .showDialog();
    }

    private String resolveModpackTag(VersionInfo versionInfo) {
        VersionInfo.LoaderInfo[] loaderInfoArray = versionInfo.getLoaderInfo();
        if (loaderInfoArray == null || loaderInfoArray.length == 0) {
            return "Vanilla";
        }

        for (VersionInfo.LoaderInfo loaderInfo : loaderInfoArray) {
            if (loaderInfo == null || loaderInfo.getName() == null) continue;
            String loaderName = loaderInfo.getName().trim();
            if (loaderName.isEmpty()) continue;
            if ("OptiFine".equalsIgnoreCase(loaderName)) return "OptiFine";
            return loaderName;
        }

        return "Vanilla";
    }

    @Subscribe()
    public void event(RefreshVersionsEvent event) {
        if (event.getMode() == END) {
            TaskExecutors.runInUIThread(this::refreshCurrentVersion);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(AccountUpdateEvent event) {
        if (accountViewWrapper != null) accountViewWrapper.refreshAccountInfo();
        refreshProfileCard();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    private void runInstallerWithConfirmation(boolean isCustomArgs) {
        if (ProgressKeeper.getTaskCount() == 0)
            Tools.installMod(requireActivity(), isCustomArgs);
        else
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }

    @Override
    public void slideIn(AnimPlayer animPlayer) {
        animPlayer.apply(new AnimPlayer.Entry(binding.launcherMenu, Animations.BounceInDown))
                .apply(new AnimPlayer.Entry(binding.playLayout, Animations.BounceInLeft))
                .apply(new AnimPlayer.Entry(binding.playButtonsLayout, Animations.BounceEnlarge));
    }

    @Override
    public void slideOut(AnimPlayer animPlayer) {
        animPlayer.apply(new AnimPlayer.Entry(binding.launcherMenu, Animations.FadeOutUp))
                .apply(new AnimPlayer.Entry(binding.playLayout, Animations.FadeOutRight))
                .apply(new AnimPlayer.Entry(binding.playButtonsLayout, Animations.BounceShrink));
    }
}

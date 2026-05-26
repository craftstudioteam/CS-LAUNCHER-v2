package com.craftstudio.launcher.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;

import com.craftstudio.launcher.context.ContextExecutor;
import com.craftstudio.launcher.context.LocaleHelper;
import com.craftstudio.launcher.event.single.LauncherIgnoreNotchEvent;
import com.craftstudio.launcher.feature.accounts.AccountsManager;
import com.craftstudio.launcher.feature.customprofilepath.ProfilePathManager;
import com.craftstudio.launcher.plugins.PluginLoader;
import com.craftstudio.launcher.renderer.Renderers;
import com.craftstudio.launcher.setting.AllSettings;
import com.craftstudio.launcher.ui.fragment.settings.AppearanceSettingsFragment;
import com.craftstudio.launcher.utils.StoragePermissionsUtils;

import com.craftstudio.launcher.MissingStorageActivity;
import com.craftstudio.launcher.R;
import com.craftstudio.launcher.Tools;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.Companion.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyAccentTheme();
        super.onCreate(savedInstanceState);
        LocaleHelper.Companion.setLocale(this);
        Tools.setFullscreen(this);
        Tools.updateWindowSize(this);

        checkStoragePermissions();
        //加载渲染器
        Renderers.INSTANCE.init(false);
        //加载插件
        PluginLoader.loadAllPlugins(this, false);
        //刷新游戏路径
        ProfilePathManager.INSTANCE.refreshPath();
    }

    private void applyAccentTheme() {
        String accent = AppearanceSettingsFragment.getAccentColor(this);
        int themeResId;
        switch (accent) {
            case "#FFA500":
                themeResId = R.style.AppTheme_ProOrange;
                break;
            case "#DC143C":
                themeResId = R.style.AppTheme_CrimsonRed;
                break;
            case "#00BFFF":
                themeResId = R.style.AppTheme_IceBlue;
                break;
            case "#8A2BE2":
                themeResId = R.style.AppTheme_RoyalPurple;
                break;
            default:
                themeResId = R.style.AppTheme_HackerGreen;
                break;
        }
        setTheme(themeResId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextExecutor.setActivity(this);
        if (!Tools.checkStorageRoot()) {
            startActivity(new Intent(this, MissingStorageActivity.class));
            finish();
        }

        checkStoragePermissions();

        AccountsManager.INSTANCE.reload();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Tools.setFullscreen(this);
        Tools.ignoreNotch(shouldIgnoreNotch(),this);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Tools.getDisplayMetrics(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void event(LauncherIgnoreNotchEvent event) {
        Tools.ignoreNotch(shouldIgnoreNotch(),this);
    }

    /** @return Whether or not the notch should be ignored */
    public boolean shouldIgnoreNotch() {
        return AllSettings.getIgnoreNotchLauncher().getValue();
    }

    private void checkStoragePermissions() {
        //检查所有文件管理权限
        StoragePermissionsUtils.checkPermissions(this);
    }
}

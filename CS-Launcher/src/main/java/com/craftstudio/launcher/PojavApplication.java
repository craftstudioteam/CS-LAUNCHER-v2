package com.craftstudio.launcher;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.craftstudio.launcher.utils.ZHTools.getVersionCode;
import static com.craftstudio.launcher.utils.ZHTools.getVersionName;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.craftstudio.launcher.InfoDistributor;
import com.craftstudio.launcher.context.ContextExecutor;
import com.craftstudio.launcher.context.LocaleHelper;
import com.craftstudio.launcher.feature.log.Logging;
import com.craftstudio.launcher.setting.AllSettings;
import com.craftstudio.launcher.ui.activity.ErrorActivity;
import com.craftstudio.launcher.utils.path.PathManager;
import com.craftstudio.launcher.utils.ZHTools;

import com.craftstudio.launcher.utils.FileUtils;

import java.io.File;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

public class PojavApplication extends Application {
	public static final String CRASH_REPORT_TAG = "CSLauncherCrashReport";

	@Override
	public void onCreate() {
		ContextExecutor.setApplication(this);

		Thread.setDefaultUncaughtExceptionHandler((thread, th) -> {
			boolean storagePermAllowed = (Build.VERSION.SDK_INT >= 29 || ActivityCompat.checkSelfPermission(PojavApplication.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) && Tools.checkStorageRoot();
			File crashFile = new File(storagePermAllowed ? PathManager.DIR_LAUNCHER_LOG : PathManager.DIR_DATA, "latestcrash.txt");
			try {
				// Write to file, since some devices may not able to show error
				FileUtils.ensureParentDirectory(crashFile);
				PrintStream crashStream = new PrintStream(crashFile);
				crashStream.append(InfoDistributor.APP_NAME + " crash report\n");
				crashStream.append(" - Time: ").append(DateFormat.getDateTimeInstance().format(new Date())).append("\n");
				crashStream.append(" - Device: ").append(Build.PRODUCT).append(" ").append(Build.MODEL).append("\n");
				crashStream.append(" - Android version: ").append(Build.VERSION.RELEASE).append("\n");
				crashStream.append(" - Launcher version: ").append(getVersionName()).append(" (").append(String.valueOf(getVersionCode())).append(")").append("\n");
				crashStream.append(" - Crash stack trace:\n");
				crashStream.append(Log.getStackTraceString(th));
				crashStream.close();
			} catch (Throwable throwable) {
				Logging.e(CRASH_REPORT_TAG, " - Exception attempt saving crash stack trace:", throwable);
				Logging.e(CRASH_REPORT_TAG, " - The crash stack trace was:", th);
			}

			ErrorActivity.showLauncherCrash(PojavApplication.this, crashFile.getAbsolutePath(), th);
			ZHTools.killProcess();
		});
		
		try {
			super.onCreate();
			PathManager.DIR_DATA = getDir("files", MODE_PRIVATE).getParent();
			PathManager.DIR_CACHE = getCacheDir();
			PathManager.DIR_ACCOUNT_NEW = PathManager.DIR_DATA + "/accounts";
			Tools.DEVICE_ARCHITECTURE = Architecture.getDeviceArchitecture();
			//Force x86 lib directory for Asus x86 based zenfones
			if(Architecture.isx86Device() && Architecture.is32BitsDevice()){
				String originalJNIDirectory = getApplicationInfo().nativeLibraryDir;
				getApplicationInfo().nativeLibraryDir = originalJNIDirectory.substring(0,
												originalJNIDirectory.lastIndexOf("/"))
												.concat("/x86");
			}
		} catch (Throwable throwable) {
			Intent ferrorIntent = new Intent(this, ErrorActivity.class);
			ferrorIntent.putExtra("throwable", throwable);
			ferrorIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
			startActivity(ferrorIntent);
		}

		//设置主题
		String launcherTheme = AllSettings.getLauncherTheme().getValue();
		if (!Objects.equals(launcherTheme, "system")) {
			switch (launcherTheme) {
				case "light" :
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
					break;
				case "dark" :
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
					break;
			}
		}
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		ContextExecutor.clearApplication();
	}

	@Override
    protected void attachBaseContext(Context base) {
		ContextExecutor.setApplication(this);
        super.attachBaseContext(LocaleHelper.Companion.setLocale(base));
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
		ContextExecutor.setApplication(this);
		LocaleHelper.Companion.setLocale(this);
    }
}

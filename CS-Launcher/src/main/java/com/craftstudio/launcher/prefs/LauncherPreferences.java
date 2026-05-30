package com.craftstudio.launcher.prefs;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.P;
import static com.craftstudio.launcher.Architecture.is32BitsDevice;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;

import com.craftstudio.launcher.feature.log.Logging;
import com.craftstudio.launcher.feature.unpack.Jre;
import com.craftstudio.launcher.setting.AllSettings;
import com.craftstudio.launcher.setting.AllStaticSettings;
import com.craftstudio.launcher.setting.Settings;
import com.craftstudio.launcher.ui.activity.BaseActivity;

import com.craftstudio.launcher.Tools;
import com.craftstudio.launcher.multirt.MultiRTUtils;
import com.craftstudio.launcher.utils.JREUtils;

public class LauncherPreferences {
    private static Boolean sIsDevicePowerful = null;

    public static void loadPreferences(Context ctx) {
        String argLwjglLibname = "-Dorg.lwjgl.opengl.libname=";
        String javaArgs = AllSettings.getJavaArgs().getValue();
        for (String arg : JREUtils.parseJavaArguments(javaArgs)) {
            if (arg.startsWith(argLwjglLibname)) {
                // purge arg
                AllSettings.getJavaArgs().put(javaArgs.replace(arg, "")).save();
            }
        }

        reloadRuntime();

        boolean powerful = isDevicePowerful(ctx);
        if (!Settings.Manager.contains("bigCoreAffinity"))
            AllSettings.getBigCoreAffinity().put(false).save();
        if (!Settings.Manager.contains("sustainedPerformance"))
            AllSettings.getSustainedPerformance().put(powerful).save();
        if (!Settings.Manager.contains("resolutionRatio"))
            AllSettings.getResolutionRatio().put(findBestResolution(ctx)).save();
    }

    /** Detect if the device is powerful enough for higher settings */
    public static boolean isDevicePowerful(Context context) {
        if (sIsDevicePowerful != null) return sIsDevicePowerful;
        
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return sIsDevicePowerful = false;
        if (Tools.getTotalDeviceMemory(context) <= 4096) return sIsDevicePowerful = false;
        
        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        if (Math.min(metrics.widthPixels, metrics.heightPixels) < 1080) return sIsDevicePowerful = false;
        
        if (Runtime.getRuntime().availableProcessors() <= 4) return sIsDevicePowerful = false;
        if (hasAllCoreSameFreq()) return sIsDevicePowerful = false;
        
        return sIsDevicePowerful = true;
    }

    /** Detect big.LITTLE architecture */
    private static boolean hasAllCoreSameFreq() {
        int coreCount = Runtime.getRuntime().availableProcessors();
        try {
            String freq0 = Tools.read("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
            String freqX = Tools.read("/sys/devices/system/cpu/cpu" + (coreCount - 1) + "/cpufreq/cpuinfo_max_freq");
            return freq0.equals(freqX);
        } catch (java.io.IOException e) {
            return false;
        }
    }

    /** Auto-detect best resolution ratio */
    public static int findBestResolution(Context context) {
        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int minSide = Math.min(metrics.widthPixels, metrics.heightPixels);
        int targetSide = isDevicePowerful(context) ? 1080 : 720;
        
        if (minSide <= targetSide) return 100;
        
        float ratio = (100f * targetSide / minSide);
        return (int) (Math.ceil(ratio / 25) * 25);
    }

    public static void reloadRuntime() {
        if (!Settings.Manager.contains("defaultRuntime") && !MultiRTUtils.getRuntimes().isEmpty()) {
            //设置默认运行环境
            AllSettings.getDefaultRuntime().put(Jre.JRE_8.getJreName()).save();
        }
    }

    /**
     * This functions aims at finding the best default RAM amount,
     * according to the RAM amount of the physical device.
     * Put not enough RAM ? Minecraft will lag and crash.
     * Put too much RAM ?
     * The GC will lag, android won't be able to breathe properly.
     * @param ctx Context needed to get the total memory of the device.
     * @return The best default value found.
     */
    public static int findBestRAMAllocation(Context ctx){
        int deviceRam = Tools.getTotalDeviceMemory(ctx);
        if (deviceRam < 1024) return 296;
        if (deviceRam < 1536) return 448;
        if (deviceRam < 2048) return 656;
        // Limit the max for 32 bits devices more harshly
        if (is32BitsDevice()) return 696;

        if (deviceRam < 3064) return 936;
        if (deviceRam < 4096) return 1144;
        if (deviceRam < 6144) return 1536;
        return 2048; //Default RAM allocation for 64 bits
    }

    /** Compute the notch size to avoid being out of bounds */
    public static void computeNotchSize(BaseActivity activity) {
        if (Build.VERSION.SDK_INT < P) return;
        try {
            final Rect cutout;
            if(SDK_INT >= Build.VERSION_CODES.S){
                cutout = activity.getWindowManager().getCurrentWindowMetrics().getWindowInsets().getDisplayCutout().getBoundingRects().get(0);
            } else {
                cutout = activity.getWindow().getDecorView().getRootWindowInsets().getDisplayCutout().getBoundingRects().get(0);
            }

            // Notch values are rotation sensitive, handle all cases
            int orientation = activity.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) AllStaticSettings.notchSize = cutout.height();
            else if (orientation == Configuration.ORIENTATION_LANDSCAPE) AllStaticSettings.notchSize = cutout.width();
            else AllStaticSettings.notchSize = Math.min(cutout.width(), cutout.height());

        }catch (Exception e){
            Logging.i("NOTCH DETECTION", "No notch detected, or the device if in split screen mode");
            AllStaticSettings.notchSize = -1;
        }
        Tools.updateWindowSize(activity);
    }
}

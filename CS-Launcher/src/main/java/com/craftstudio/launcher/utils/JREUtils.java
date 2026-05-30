package com.craftstudio.launcher.utils;

import static com.craftstudio.launcher.utils.path.PathManager.DIR_NATIVE_LIB;
import static com.craftstudio.launcher.Architecture.ARCH_X86;
import static com.craftstudio.launcher.Architecture.is64BitsDevice;
import static com.craftstudio.launcher.Tools.currentDisplayMetrics;

import android.content.Context;
import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.craftstudio.launcher.InfoDistributor;
import com.craftstudio.launcher.R;
import com.craftstudio.launcher.event.value.JvmExitEvent;
import com.craftstudio.launcher.feature.customprofilepath.ProfilePathHome;
import com.craftstudio.launcher.feature.customprofilepath.ProfilePathManager;
import com.craftstudio.launcher.feature.log.Logging;
import com.craftstudio.launcher.feature.version.Version;
import com.craftstudio.launcher.feature.version.VersionInfo;
import com.craftstudio.launcher.plugins.driver.DriverPluginManager;
import com.craftstudio.launcher.plugins.renderer.RendererPluginManager;
import com.craftstudio.launcher.plugins.renderer.RendererPlugin;
import com.craftstudio.launcher.renderer.RendererInterface;
import com.craftstudio.launcher.renderer.Renderers;
import com.craftstudio.launcher.setting.AllSettings;
import com.craftstudio.launcher.ui.activity.ErrorActivity;
import com.craftstudio.launcher.utils.ZHTools;
import com.craftstudio.launcher.utils.path.LibPath;
import com.craftstudio.launcher.utils.path.PathManager;
import com.oracle.dalvik.VMLauncher;

import com.craftstudio.launcher.Architecture;
import com.craftstudio.launcher.Logger;
import com.craftstudio.launcher.Tools;
import com.craftstudio.launcher.multirt.MultiRTUtils;
import com.craftstudio.launcher.multirt.Runtime;
import com.craftstudio.launcher.plugins.FFmpegPlugin;

import org.greenrobot.eventbus.EventBus;
import org.lwjgl.glfw.CallbackBridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public final class JREUtils {
    private JREUtils() {}

    public static String LD_LIBRARY_PATH;
    public static String jvmLibraryPath;

    public static String findInLdLibPath(String libName) {
        if (Os.getenv("LD_LIBRARY_PATH")==null) {
            try {
                if (LD_LIBRARY_PATH != null) {
                    Os.setenv("LD_LIBRARY_PATH", LD_LIBRARY_PATH, true);
                }
            }catch (ErrnoException e) {
                Logging.e("JREUtils", Tools.printToString(e));
            }
            return libName;
        }
        for (String libPath : Os.getenv("LD_LIBRARY_PATH").split(":")) {
            File f = new File(libPath, libName);
            if (f.exists() && f.isFile()) {
                return f.getAbsolutePath();
            }
        }
        return libName;
    }

    public static ArrayList<File> locateLibs(File path) {
        ArrayList<File> returnValue = new ArrayList<>();
        File[] list = path.listFiles();
        if (list != null) {
            for(File f : list) {
                if (f.isFile() && f.getName().endsWith(".so")) {
                    returnValue.add(f);
                }else if (f.isDirectory()) {
                    returnValue.addAll(locateLibs(f));
                }
            }
        }
        return returnValue;
    }

    public static void initJavaRuntime(String jreHome) {
        dlopen(findInLdLibPath("libjli.so"));
        if (!dlopen("libjvm.so")) {
            Logging.w("DynamicLoader","Failed to load with no path, trying with full path");
            dlopen(jvmLibraryPath+"/libjvm.so");
        }
        dlopen(findInLdLibPath("libverify.so"));
        dlopen(findInLdLibPath("libjava.so"));
        dlopen(findInLdLibPath("libnet.so"));
        dlopen(findInLdLibPath("libnio.so"));
        dlopen(findInLdLibPath("libawt.so"));
        dlopen(findInLdLibPath("libawt_headless.so"));
        dlopen(findInLdLibPath("libfreetype.so"));
        dlopen(findInLdLibPath("libfontmanager.so"));
        for (File f : locateLibs(new File(jreHome, Tools.DIRNAME_HOME_JRE))) {
            dlopen(f.getAbsolutePath());
        }
    }

    public static void redirectAndPrintJRELog() {
        Logging.v("jrelog","Log starts here");
        new Thread(new Runnable(){
            int failTime = 0;
            ProcessBuilder logcatPb;
            @Override
            public void run() {
                try {
                    if (logcatPb == null) {
                        logcatPb = new ProcessBuilder().command("logcat", "-v", "brief", "-s", "jrelog:I", "LIBGL:I", "NativeInput").redirectErrorStream(true);
                    }

                    Logging.i("jrelog-logcat","Clearing logcat");
                    new ProcessBuilder().command("logcat", "-c").redirectErrorStream(true).start();
                    Logging.i("jrelog-logcat","Starting logcat");
                    Process p = logcatPb.start();

                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = p.getInputStream().read(buf)) != -1) {
                        String currStr = new String(buf, 0, len);
                        Logger.appendToLog(currStr);
                    }

                    if (p.waitFor() != 0) {
                        Logging.e("jrelog-logcat", "Logcat exited with code " + p.exitValue());
                        failTime++;
                        Logging.i("jrelog-logcat", (failTime <= 10 ? "Restarting logcat" : "Too many restart fails") + " (attempt " + failTime + "/10");
                        if (failTime <= 10) {
                            run();
                        } else {
                            Logger.appendToLog("ERROR: Unable to get more Logging.");
                        }
                    }
                } catch (Throwable e) {
                    Logging.e("jrelog-logcat", "Exception on logging thread", e);
                    Logger.appendToLog("Exception on logging thread:\n" + Log.getStackTraceString(e));
                }
            }
        }).start();
        Logging.i("jrelog-logcat","Logcat thread started");

    }

    public static void relocateLibPath(Runtime runtime, String jreHome) {
        String JRE_ARCHITECTURE = runtime.arch;
        if (Architecture.archAsInt(JRE_ARCHITECTURE) == ARCH_X86){
            JRE_ARCHITECTURE = "i386/i486/i586";
        }

        for (String arch : JRE_ARCHITECTURE.split("/")) {
            File f = new File(jreHome, "lib/" + arch);
            if (f.exists() && f.isDirectory()) {
                Tools.DIRNAME_HOME_JRE = "lib/" + arch;
            }
        }

        String libName = is64BitsDevice() ? "lib64" : "lib";
        StringBuilder ldLibraryPath = new StringBuilder();

        // 1. JRE internal libs (Priority 1)
        File serverFile = new File(jreHome + "/" + Tools.DIRNAME_HOME_JRE + "/server/libjvm.so");
        String jvmServerPath = jreHome + "/" + Tools.DIRNAME_HOME_JRE + "/" + (serverFile.exists() ? "server" : "client");
        ldLibraryPath.append(jvmServerPath).append(":")
                .append(jreHome).append("/").append(Tools.DIRNAME_HOME_JRE).append(":")
                .append(jreHome).append("/lib:").append(jreHome).append("/").append(Tools.DIRNAME_HOME_JRE).append("/jli:");

        // 2. Plugins (FFmpeg, Renderer, Driver)
        if (FFmpegPlugin.isAvailable) {
            ldLibraryPath.append(FFmpegPlugin.libraryPath).append(":");
        }
        RendererPlugin customRenderer = RendererPluginManager.getSelectedRendererPlugin();
        if (customRenderer != null) {
            ldLibraryPath.append(customRenderer.getPath()).append(":");
        }
        String vulkanDriverPath = DriverPluginManager.getDriver().getPath();
        if (vulkanDriverPath != null && !vulkanDriverPath.isEmpty()) {
            ldLibraryPath.append(vulkanDriverPath).append(":");
        }

        // 3. App Native Libs (Priority 2)
        ldLibraryPath.append(DIR_NATIVE_LIB).append(":");

        // 4. System Libs (Priority 3)
        ldLibraryPath.append("/system/").append(libName).append(":")
                .append("/vendor/").append(libName).append(":")
                .append("/vendor/").append(libName).append("/hw:");

        // 5. Runtime Mod Dir
        File runtimeModDir = PathManager.DIR_RUNTIME_MOD;
        if (runtimeModDir != null) {
            ldLibraryPath.append(runtimeModDir.getAbsolutePath()).append(":");
        }

        String result = ldLibraryPath.toString();
        if (result.endsWith(":")) result = result.substring(0, result.length() - 1);
        LD_LIBRARY_PATH = result;
    }

    private static void initLdLibraryPath(String jreHome) {
        File serverFile = new File(jreHome + "/" + Tools.DIRNAME_HOME_JRE + "/server/libjvm.so");
        jvmLibraryPath = jreHome + "/" + Tools.DIRNAME_HOME_JRE + "/" + (serverFile.exists() ? "server" : "client");
        Logging.d("DynamicLoader","Base LD_LIBRARY_PATH: " + LD_LIBRARY_PATH);
        Logging.d("DynamicLoader","Internal LD_LIBRARY_PATH: "+jvmLibraryPath + ":" + LD_LIBRARY_PATH);
        setLdLibraryPath(jvmLibraryPath + ":" + LD_LIBRARY_PATH);
    }

    private static void setJavaEnv(Map<String, String> envMap, String jreHome, Version gameVersion, AppCompatActivity activity) {
        envMap.put("POJAV_NATIVEDIR", DIR_NATIVE_LIB);
        envMap.put("DRIVER_PATH", DriverPluginManager.getDriver().getPath());
        envMap.put("JAVA_HOME", jreHome);
        envMap.put("HOME", PathManager.DIR_GAME_HOME);
        envMap.put("TMPDIR", PathManager.DIR_CACHE.getAbsolutePath());
        envMap.put("LD_LIBRARY_PATH", LD_LIBRARY_PATH);
        envMap.put("PATH", jreHome + "/bin:" + Os.getenv("PATH"));
        envMap.put("FORCE_VSYNC", String.valueOf(AllSettings.getForceVsync().getValue()));

        // Performance Hacks
        envMap.put("vblank_mode", "0");
        envMap.put("mesa_glthread", "true");
        envMap.put("GALLIUM_THREAD", "1");

        // FIX: Huawei device fix
        if (Tools.deviceHasHangingLinker()) {
            envMap.put("POJAV_EMUI_ITERATOR_MITIGATE", "1");
        }

        // IMPROVEMENT 2: VulkanMod detection and support
        if (isVulkanModPresent(gameVersion)) {
            envMap.put("POJAV_RENDERER", "vulkan_zink");
            envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
            envMap.put("GALLIUM_DRIVER", "zink");
            envMap.put("MESA_GL_VERSION_OVERRIDE", "4.6");
            envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460");
            envMap.put("VK_ICD_FILENAMES", "");
            envMap.put("VULKAN_MOD_DETECTED", "1");

            if (activity != null) {
                activity.runOnUiThread(() -> Toast.makeText(activity, "VulkanMod detected! Vulkan renderer auto-enabled.", Toast.LENGTH_SHORT).show());
            }
            Logging.i("VulkanMod", "VulkanMod detected, forcing Vulkan environment");
        }

        float ratio = AllSettings.getResolutionRatio().getValue() / 100F;
        envMap.put("AWTSTUB_WIDTH", Integer.toString(Tools.getDisplayFriendlyRes(CallbackBridge.windowWidth > 0 ? CallbackBridge.windowWidth : CallbackBridge.physicalWidth, ratio)));
        envMap.put("AWTSTUB_HEIGHT", Integer.toString(Tools.getDisplayFriendlyRes(CallbackBridge.windowHeight > 0 ? CallbackBridge.windowHeight : CallbackBridge.physicalHeight, ratio)));
        envMap.put("MOD_ANDROID_RUNTIME", PathManager.DIR_RUNTIME_MOD != null ? PathManager.DIR_RUNTIME_MOD.getAbsolutePath() : "");

        if (AllSettings.getDumpShaders().getValue())
            envMap.put("LIBGL_VGPU_DUMP", "1");
        if (AllSettings.getZinkPreferSystemDriver().getValue())
            envMap.put("POJAV_ZINK_PREFER_SYSTEM_DRIVER", "1");
        if (AllSettings.getVsyncInZink().getValue())
            envMap.put("POJAV_VSYNC_IN_ZINK", "1");
        if (AllSettings.getBigCoreAffinity().getValue())
            envMap.put("POJAV_BIG_CORE_AFFINITY", "1");
        if (FFmpegPlugin.isAvailable)
            envMap.put("POJAV_FFMPEG_PATH", FFmpegPlugin.executablePath);
    }

    private static boolean isVulkanModPresent(Version gameVersion) {
        if (gameVersion == null) return false;
        File modsFolder = new File(gameVersion.getGameDir(), "mods");
        if (!modsFolder.exists()) return false;
        File[] mods = modsFolder.listFiles();
        if (mods == null) return false;
        for (File mod : mods) {
            if (mod.getName().toLowerCase(java.util.Locale.ROOT).contains("vulkanmod")) return true;
        }
        return false;
    }

    private static void setRendererEnv(Map<String, String> envMap) {
        RendererInterface currentRenderer = Renderers.INSTANCE.getCurrentRenderer();
        String rendererId = currentRenderer.getRendererId();

        // 1. Base / Common Environment Setup
        envMap.put("POJAV_RENDERER", rendererId);
        envMap.put("MESA_GLSL_CACHE_DIR", PathManager.DIR_CACHE.getAbsolutePath());

        // 2. Strict Renderer-Specific Isolation
        switch (rendererId) {
            case "opengles2": // Holy GL4ES
                envMap.put("LIBGL_ES", "2");
                envMap.put("LIBGL_GL", "21");
                envMap.put("LIBGL_MIPMAP", "3");
                envMap.put("LIBGL_NOERROR", "1");
                envMap.put("LIBGL_NOINTOVLHACK", "1");
                envMap.put("LIBGL_NORMALIZE", "1");
                envMap.put("LIBGL_FB", "1");
                break;

            case "angle": // OpenGL ES (Angle)
                envMap.put("LIBGL_ES", "3");
                envMap.put("LIBGL_GL", "32"); // Support modern Minecraft
                break;

            case "gallium_virgl": // Virgl (OpenGL)
                envMap.put("MESA_EXTENSION_OVERRIDE", "-GL_EXT_texture_sRGB");
                envMap.put("VIRGL_DEBUG", "x11");
                envMap.put("GALLIUM_DRIVER", "virpipe"); // Standard Virgl driver name
                envMap.put("LIB_MESA_NAME", loadGraphicsLibrary());
                break;

            case "gallium_panfrost": // Panfrost (OpenGL)
                envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "panfrost");
                envMap.put("GALLIUM_DRIVER", "panfrost");
                envMap.put("LIB_MESA_NAME", loadGraphicsLibrary());
                break;

            case "gallium_freedreno": // Freedreno (OpenGL)
                envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "freedreno");
                envMap.put("GALLIUM_DRIVER", "freedreno");
                envMap.put("LIB_MESA_NAME", loadGraphicsLibrary());
                break;

            case "ltw_render": // LTW (Hardware accelerated)
                envMap.put("LIBGL_ES", "3");
                envMap.put("LIBGL_GL", "30");
                envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "ltw");
                envMap.put("GALLIUM_DRIVER", "ltw");
                envMap.put("LIB_MESA_NAME", loadGraphicsLibrary());
                break;

            case "vulkan_zink": // Zink (Vulkan)
                envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
                envMap.put("GALLIUM_DRIVER", "zink");
                envMap.put("ZINK_DESCRIPTORS", "lazy");
                envMap.put("MESA_GL_VERSION_OVERRIDE", "4.6");
                envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460");
                envMap.put("force_glsl_extensions_warn", "true");
                envMap.put("allow_higher_compat_version", "true");
                envMap.put("allow_glsl_extension_directive_midshader", "true");
                envMap.put("LIB_MESA_NAME", loadGraphicsLibrary());
                if (!AllSettings.getZinkPreferSystemDriver().getValue() && com.craftstudio.launcher.utils.DeviceGPUDetector.INSTANCE.isAdreno()) {
                    envMap.put("POJAV_LOAD_TURNIP", "1");
                }
                break;
        }

        // 3. Merge environment variables defined within the Renderer implementation itself
        envMap.putAll(currentRenderer.getRendererEnv().getValue());

        // 4. EGL Context Override (if defined by the renderer)
        String eglName = currentRenderer.getRendererEGL();
        if (eglName != null) envMap.put("POJAVEXEC_EGL", eglName);

        // 5. Global Fallback for LIBGL_ES if not explicitly set
        if (!envMap.containsKey("LIBGL_ES")) {
            int glesMajor = getDetectedVersion();
            Logging.i("glesDetect", "GLES version detected: " + glesMajor);

            if (rendererId.startsWith("opengles")) {
                envMap.put("LIBGL_ES", rendererId.replace("opengles", "").replace("_5", ""));
            } else if (glesMajor >= 3) {
                envMap.put("LIBGL_ES", "3");
            } else {
                envMap.put("LIBGL_ES", "2");
            }
        }
    }

    private static void setCustomEnv(Map<String, String> envMap) throws Throwable {
        File customEnvFile = new File(PathManager.DIR_GAME_HOME, "custom_env.txt");
        if (customEnvFile.exists() && customEnvFile.isFile()) {
            BufferedReader reader = new BufferedReader(new FileReader(customEnvFile));
            String line;
            while ((line = reader.readLine()) != null) {
                int index = line.indexOf("=");
                envMap.put(line.substring(0, index), line.substring(index + 1));
            }
            reader.close();
        }
    }

    private static void checkAndUsedJSPH(Map<String, String> envMap, final Runtime runtime) {
        boolean onUseJSPH = runtime.javaVersion > 11;
        if (!onUseJSPH) return;
        File dir = new File(DIR_NATIVE_LIB);
        if (!dir.isDirectory()) return;
        String jsphName = runtime.javaVersion == 17 ? "libjsph17" : "libjsph21";
        File[] files = dir.listFiles((dir1, name) -> name.startsWith(jsphName));
        if (files != null && files.length > 0) {
            String libName = DIR_NATIVE_LIB + "/" + jsphName + ".so";
            envMap.put("JSP", libName);
        }
    }

    private static void setEnv(String jreHome, final Runtime runtime, Version gameVersion, AppCompatActivity activity) throws Throwable {
        Map<String, String> envMap = new ArrayMap<>();

        setJavaEnv(envMap, jreHome, gameVersion, activity);
        setCustomEnv(envMap);

        if (gameVersion != null) {
            checkAndUsedJSPH(envMap, runtime);

            VersionInfo versionInfo = gameVersion.getVersionInfo();
            if (versionInfo != null && versionInfo.getLoaderInfo() != null) {
                for (VersionInfo.LoaderInfo loaderInfo : versionInfo.getLoaderInfo()) {
                    if (loaderInfo.getLoaderEnvKey() != null) {
                        envMap.put(loaderInfo.getLoaderEnvKey(), "1");
                    }
                }
            }

            if (Renderers.INSTANCE.isCurrentRendererValid()) {
                setRendererEnv(envMap);
            }

            envMap.put("CS_VERSION_CODE", String.valueOf(ZHTools.getVersionCode()));
        }

        for (Map.Entry<String, String> env : envMap.entrySet()) {
            Logger.appendToLog("Added custom env: " + env.getKey() + "=" + env.getValue());
            try {
                Os.setenv(env.getKey(), env.getValue(), true);
            }catch (NullPointerException exception){
                Logging.e("JREUtils", exception.toString());
            }
        }
    }

    private static void initGraphicAndSoundEngine(boolean renderer) {
        try {
            dlopen(DIR_NATIVE_LIB + "/libopenal.so");

            if (!renderer) return;

            String rendererLib = loadGraphicsLibrary();
            RendererPlugin customRenderer = RendererPluginManager.getSelectedRendererPlugin();

            if (customRenderer != null) {
                customRenderer.getDlopen().forEach(lib -> dlopen(customRenderer.getPath() + "/" + lib));
            }

            if (!dlopen(rendererLib) && !dlopen(findInLdLibPath(rendererLib))) {
                Logging.e("RENDER_LIBRARY", "Failed to load renderer " + rendererLib);
            }
        } catch (Throwable e) {
            Logging.e("JREUtils", "Failed to initialize graphics or sound engine: " + e.getMessage());
        }
    }

    private static void launchJavaVM(
            final AppCompatActivity activity,
            String runtimeHome,
            Version gameVersion,
            final List<String> JVMArgs,
            final String userArgsString
    ) {
        List<String> userArgs = getJavaArgs(runtimeHome, userArgsString);
        purgeArg(userArgs, "-Xms");
        purgeArg(userArgs, "-Xmx");
        purgeArg(userArgs, "-d32");
        purgeArg(userArgs, "-d64");
        purgeArg(userArgs, "-Xint");
        purgeArg(userArgs, "-XX:+UseTransparentHugePages");
        purgeArg(userArgs, "-XX:+UseLargePagesInMetaspace");
        purgeArg(userArgs, "-XX:+UseLargePages");
        purgeArg(userArgs, "-Dorg.lwjgl.opengl.libname");
        purgeArg(userArgs, "-Dorg.lwjgl.freetype.libname");
        purgeArg(userArgs, "-XX:ActiveProcessorCount");
        purgeArg(userArgs, "-XX:+UseG1GC");
        purgeArg(userArgs, "-XX:+UseParallelGC");
        purgeArg(userArgs, "-XX:+UseConcMarkSweepGC");
        purgeArg(userArgs, "-XX:+UseSerialGC");
        purgeArg(userArgs, "-XX:+UseZGC");
        purgeArg(userArgs, "-XX:+UseShenandoahGC");
        purgeArg(userArgs, "-XX:+UnlockExperimentalVMOptions");
        purgeArg(userArgs, "-XX:+DisableExplicitGC");
        purgeArg(userArgs, "-XX:+UseStringDeduplication");
        purgeArg(userArgs, "-XX:MaxGCPauseMillis");
        purgeArg(userArgs, "-XX:G1NewSizePercent");
        purgeArg(userArgs, "-XX:G1ReservePercent");

        userArgs.add("-javaagent:" + LibPath.MIO_LIB_PATCHER.getAbsolutePath());

        userArgs.add("-XX:+UseG1GC");
        userArgs.add("-XX:+UnlockExperimentalVMOptions");
        userArgs.add("-XX:G1NewSizePercent=20");
        userArgs.add("-XX:G1ReservePercent=20");
        userArgs.add("-XX:MaxGCPauseMillis=20");
        userArgs.add("-XX:G1HeapRegionSize=8M");
        userArgs.add("-XX:+ParallelRefProcEnabled");
        userArgs.add("-XX:+DisableExplicitGC");
        userArgs.add("-XX:+AlwaysPreTouch");
        userArgs.add("-XX:+UseStringDeduplication");

        int ramMB = AllSettings.getRamAllocation().getValue().getValue();
        int minHeapMB = Math.max(ramMB / 4, 512);
        if (minHeapMB > ramMB) minHeapMB = ramMB;
        userArgs.add("-Xms" + minHeapMB + "M");
        userArgs.add("-Xmx" + ramMB + "M");
        if (Renderers.INSTANCE.isCurrentRendererValid()) userArgs.add("-Dorg.lwjgl.opengl.libname=" + loadGraphicsLibrary());

        userArgs.add("-XX:ActiveProcessorCount=" + java.lang.Runtime.getRuntime().availableProcessors());

        // IMPROVEMENT 2: VulkanMod JVM arguments
        if (isVulkanModPresent(gameVersion)) {
            userArgs.add("-Dvulkanmod.enabled=true");
            userArgs.add("-Dorg.lwjgl.vulkan.libname=libvulkan.so");
        }

        userArgs.addAll(JVMArgs);
        // Ensure JNA can unpack temporary files: override any previous -Djna.tmpdir
        purgeArg(userArgs, "-Djna.tmpdir");
        userArgs.add("-Djna.tmpdir=" + PathManager.DIR_CACHE.getAbsolutePath());
        
        activity.runOnUiThread(() -> Toast.makeText(activity, activity.getString(R.string.autoram_info_msg, AllSettings.getRamAllocation().getValue().getValue()), Toast.LENGTH_SHORT).show());
        System.out.println(JVMArgs);
        for (int i = 0; i < userArgs.size(); i++) {
            String arg = userArgs.get(i);
            if (arg.startsWith("--accessToken")) {
                i += 1;
            }
            Logger.appendToLog("JVMArg: " + arg);
        }

        setupExitMethod(activity.getApplication());
        initializeGameExitHook();
        chdir(gameVersion == null ? ProfilePathHome.getGameHome() : gameVersion.getGameDir().getAbsolutePath());
        userArgs.add(0,"java"); 

        activity.runOnUiThread(() -> activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        final int exitCode = VMLauncher.launchJVM(userArgs.toArray(new String[0]));
        Logger.appendToLog("Java Exit code: " + exitCode);
        if (exitCode != 0) {
            ErrorActivity.showExitMessage(activity, exitCode, false);
        }
        EventBus.getDefault().post(new JvmExitEvent(exitCode));
    }

    public static void launchWithUtils(
            final AppCompatActivity activity,
            final Runtime runtime,
            Version gameVersion,
            final List<String> JVMArgs,
            final String userArgsString
    ) throws Throwable {
        String runtimeHome = MultiRTUtils.getRuntimeHome(runtime.name).getAbsolutePath();

        try {
            relocateLibPath(runtime, runtimeHome);

            initLdLibraryPath(runtimeHome);

            setEnv(runtimeHome, runtime, gameVersion, activity);

            initJavaRuntime(runtimeHome);

            initGraphicAndSoundEngine(gameVersion != null);
        } catch (Throwable e) {
            Logging.e("JREUtils", "Critical failure during launch preparation", e);
            // We still attempt to launch Java VM, but it will likely fail or use system defaults
        }

        launchJavaVM(activity, runtimeHome, gameVersion, JVMArgs, userArgsString);
    }

    public static List<String> getJavaArgs(String runtimeHome, String userArgumentsString) {
        List<String> userArguments = parseJavaArguments(userArgumentsString);
        String resolvFile;
        resolvFile = new File(PathManager.DIR_DATA,"resolv.conf").getAbsolutePath();

        ArrayList<String> overridableArguments = new ArrayList<>(Arrays.asList(
                "-Djava.home=" + runtimeHome,
                "-Djava.io.tmpdir=" + PathManager.DIR_CACHE.getAbsolutePath(),
                "-Djna.boot.library.path=" + DIR_NATIVE_LIB,
                "-Duser.home=" + ProfilePathManager.INSTANCE.getCurrentPath(),
                "-Duser.language=" + System.getProperty("user.language"),
                "-Dos.name=Linux",
                "-Dos.version=Android-" + Build.VERSION.RELEASE,
                "-Dpojav.path.minecraft=" + ProfilePathHome.getGameHome(),
                "-Dpojav.path.private.account=" + PathManager.DIR_ACCOUNT_NEW,
                "-Duser.timezone=" + TimeZone.getDefault().getID(),

                "-Dorg.lwjgl.vulkan.libname=libvulkan.so",
                "-Dglfwstub.windowWidth=" + Tools.getDisplayFriendlyRes(currentDisplayMetrics.widthPixels, AllSettings.getResolutionRatio().getValue() / 100F),
                "-Dglfwstub.windowHeight=" + Tools.getDisplayFriendlyRes(currentDisplayMetrics.heightPixels, AllSettings.getResolutionRatio().getValue() / 100F),
                "-Dglfwstub.initEgl=false",
                "-Dext.net.resolvPath=" +resolvFile,
                "-Dlog4j2.formatMsgNoLookups=true",

                "-Dnet.minecraft.clientmodname=" + InfoDistributor.LAUNCHER_NAME,
                "-Dfml.earlyprogresswindow=false",
                "-Dloader.disable_forked_guis=true",
                "-Djdk.lang.Process.launchMechanism=FORK",
                "-Dsodium.checks.issue2561=false"
        ));

        List<String> additionalArguments = new ArrayList<>();
        for (String arg : overridableArguments) {
            String strippedArg = arg.substring(0,arg.indexOf('='));
            boolean add = true;
            for (String uarg : userArguments) {
                if (uarg.startsWith(strippedArg)) {
                    add = false;
                    break;
                }
            }
            if (add)
                additionalArguments.add(arg);
            else
                Logging.i("ArgProcessor","Arg skipped: "+arg);
        }

        userArguments.addAll(additionalArguments);
        return userArguments;
    }

    public static ArrayList<String> parseJavaArguments(String args){
        ArrayList<String> parsedArguments = new ArrayList<>(0);
        args = args.trim().replace(" ", "");
        String[] separators = new String[]{"-XX:-","-XX:+", "-XX:","--", "-D", "-X", "-javaagent:", "-verbose"};
        for (String prefix : separators){
            while (true) {
                int start = args.indexOf(prefix);
                if (start == -1) break;
                int end = -1;
                for (String separator: separators) {
                    int tempEnd = args.indexOf(separator, start + prefix.length());
                    if (tempEnd == -1) continue;
                    if (end == -1) {
                        end = tempEnd;
                        continue;
                    }
                    end = Math.min(end, tempEnd);
                }
                if (end == -1) end = args.length();

                String parsedSubString = args.substring(start, end);
                args = args.replace(parsedSubString, "");

                if (parsedSubString.indexOf('=') == parsedSubString.lastIndexOf('=')) {
                    int arraySize = parsedArguments.size();
                    if (arraySize > 0) {
                        String lastString = parsedArguments.get(arraySize - 1);
                        if (lastString.charAt(lastString.length() - 1) == ',' ||
                                parsedSubString.contains(",")) {
                            parsedArguments.set(arraySize - 1, lastString + parsedSubString);
                            continue;
                        }
                    }
                    parsedArguments.add(parsedSubString);
                }
                else Logging.w("JAVA ARGS PARSER", "Removed improper arguments: " + parsedSubString);
            }
        }
        return parsedArguments;
    }

    public static String loadGraphicsLibrary() {
        if (!Renderers.INSTANCE.isCurrentRendererValid()) return "libgl4es_114.so";
        else {
            RendererPlugin rendererPlugin = RendererPluginManager.getSelectedRendererPlugin();
            if (rendererPlugin != null) {
                return rendererPlugin.getPath() + "/" + rendererPlugin.getGlName();
            } else {
                String lib = Renderers.INSTANCE.getCurrentRenderer().getRendererLibrary();
                return lib != null ? lib : "libgl4es_114.so";
            }
        }
    }

    private static void purgeArg(List<String> argList, String argStart) {
        argList.removeIf(arg -> arg.startsWith(argStart));
    }
    private static final int EGL_OPENGL_ES_BIT = 0x0001;
    private static final int EGL_OPENGL_ES2_BIT = 0x0004;
    private static final int EGL_OPENGL_ES3_BIT_KHR = 0x0040;
    @SuppressWarnings("SameParameterValue")
    private static boolean hasExtension(String extensions, String name) {
        int start = extensions.indexOf(name);
        while (start >= 0) {
            int end = start + name.length();
            if (end == extensions.length() || extensions.charAt(end) == ' ') {
                return true;
            }
            start = extensions.indexOf(name, end);
        }
        return false;
    }

    public static int getDetectedVersion() {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        int[] numConfigs = new int[1];
        if (egl.eglInitialize(display, null)) {
            try {
                boolean checkES3 = hasExtension(egl.eglQueryString(display, EGL10.EGL_EXTENSIONS),
                        "EGL_KHR_create_context");
                if (egl.eglGetConfigs(display, null, 0, numConfigs)) {
                    EGLConfig[] configs = new EGLConfig[numConfigs[0]];
                    if (egl.eglGetConfigs(display, configs, numConfigs[0], numConfigs)) {
                        int highestEsVersion = 0;
                        int[] value = new int[1];
                        for (int i = 0; i < numConfigs[0]; i++) {
                            if (egl.eglGetConfigAttrib(display, configs[i],
                                    EGL10.EGL_RENDERABLE_TYPE, value)) {
                                if (checkES3 && ((value[0] & EGL_OPENGL_ES3_BIT_KHR) ==
                                        EGL_OPENGL_ES3_BIT_KHR)) {
                                    if (highestEsVersion < 3) highestEsVersion = 3;
                                } else if ((value[0] & EGL_OPENGL_ES2_BIT) == EGL_OPENGL_ES2_BIT) {
                                    if (highestEsVersion < 2) highestEsVersion = 2;
                                } else if ((value[0] & EGL_OPENGL_ES_BIT) == EGL_OPENGL_ES_BIT) {
                                    if (highestEsVersion < 1) highestEsVersion = 1;
                                }
                            } else {
                                Logging.w("glesDetect", "Getting config attribute with "
                                        + "EGL10#eglGetConfigAttrib failed "
                                        + "(" + i + "/" + numConfigs[0] + "): "
                                        + egl.eglGetError());
                            }
                        }
                        return highestEsVersion;
                    } else {
                        Logging.e("glesDetect", "Getting configs with EGL10#eglGetConfigs failed: "
                                + egl.eglGetError());
                        return -1;
                    }
                } else {
                    Logging.e("glesDetect", "Getting number of configs with EGL10#eglGetConfigs failed: "
                            + egl.eglGetError());
                    return -2;
                }
            } finally {
                egl.eglTerminate(display);
            }
        } else {
            Logging.e("glesDetect", "Couldn't initialize EGL.");
            return -3;
        }
    }
    public static native int chdir(String path);
    public static native boolean dlopen(String libPath);
    public static native void setLdLibraryPath(String ldLibraryPath);
    public static native void setupBridgeWindow(Object surface);
    public static native void releaseBridgeWindow();
    public static native void initializeGameExitHook();
    public static native void setupExitMethod(Context context);
    public static native int[] renderAWTScreenFrame();
    static {
        System.loadLibrary("exithook");
        System.loadLibrary("pojavexec");
        System.loadLibrary("pojavexec_awt");
    }
}

package com.craftstudio.launcher.support.touch_controller;

import android.content.Context;
import android.os.Vibrator;
import android.system.Os;

import com.craftstudio.launcher.InfoDistributor;
import com.craftstudio.launcher.feature.log.Logging;

import com.craftstudio.launcher.Logger;

import top.fifthlight.touchcontroller.proxy.client.LauncherProxyClient;
import top.fifthlight.touchcontroller.proxy.client.MessageTransport;
import top.fifthlight.touchcontroller.proxy.client.android.transport.UnixSocketTransportKt;

/**
 * 为适配 TouchController 模组
 * <a href="">https://modrinth.com/mod/touchcontroller</a>
 */
public final class ControllerProxy {
    private static LauncherProxyClient proxyClient;

    private ControllerProxy() {}

    /**
     * 启动控制代理客户端，目的是与 TouchController 模组进行通信
     */
    public static void startProxy(Context context) {
        if (proxyClient == null) {
            try {
                MessageTransport transport = UnixSocketTransportKt.UnixSocketTransport(InfoDistributor.LAUNCHER_NAME);
                Os.setenv("TOUCH_CONTROLLER_PROXY_SOCKET", InfoDistributor.LAUNCHER_NAME, true);
                LauncherProxyClient client = new LauncherProxyClient(transport);
                Vibrator vibrator = context.getSystemService(Vibrator.class);
                VibrationHandler handler = new VibrationHandler(vibrator);
                client.setVibrationHandler(handler);
                client.run();
                Logger.appendToLog("TouchController: TouchController Proxy Client has been created!");
                proxyClient = client;
            } catch (Throwable ex) {
                Logging.w("TouchController", "TouchController proxy client create failed", ex);
                proxyClient = null;
            }
        }
    }

    static LauncherProxyClient getProxyClient() {
        return proxyClient;
    }
}

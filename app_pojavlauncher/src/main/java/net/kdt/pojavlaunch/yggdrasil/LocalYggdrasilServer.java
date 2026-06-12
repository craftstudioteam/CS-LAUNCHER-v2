package net.kdt.pojavlaunch.yggdrasil;

import android.util.Log;
import java.io.File;

public class LocalYggdrasilServer {
    private static final String TAG = "LocalYggdrasilServer";
    private static final SkinManager mSkinManager = new SkinManager(SkinManager.androidSkinAnalyzerFacade);

    public static int getPort() {
        return mSkinManager.getPort();
    }

    public static synchronized void start() {
        int port = mSkinManager.startServer();
        if (port > 0) {
            System.setProperty("minecraft.api.auth.host", "http://127.0.0.1:" + port);
            System.setProperty("minecraft.api.account.host", "http://127.0.0.1:" + port);
            System.setProperty("minecraft.api.session.host", "http://127.0.0.1:" + port);
            System.setProperty("minecraft.api.services.host", "http://127.0.0.1:" + port);
            Log.i(TAG, "Local Yggdrasil Server started on port " + port);
        } else {
            Log.e(TAG, "Failed to start Local Yggdrasil Server");
        }
    }

    public static synchronized void stop() {
        mSkinManager.stopServer();
        Log.i(TAG, "Local Yggdrasil Server stopped");
    }

    public static synchronized void registerProfile(String username, String uuid, String skinPath, String capePath, boolean isSlim) {
        try {
            File skinFile = skinPath != null ? new File(skinPath) : null;
            File capeFile = capePath != null ? new File(capePath) : null;
            SkinModelType modelOverride = isSlim ? SkinModelType.ALEX : SkinModelType.STEVE;
            
            mSkinManager.prepareAccount(username, skinFile, capeFile, modelOverride);
            Log.i(TAG, "Registered profile for " + username);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register profile", e);
        }
    }
}

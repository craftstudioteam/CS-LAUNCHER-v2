package net.kdt.pojavlaunch.yggdrasil;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

public class SkinManager {
    private static final String TAG = "SkinManager";

    public interface SkinAnalyzerFacade {
        PlayerSkin prepareSkin(byte[] bytes);
        PlayerCape prepareCape(byte[] bytes);
    }

    private final SkinAnalyzerFacade analyzer;
    private final OfflineYggdrasilServer server = new OfflineYggdrasilServer();
    private int port = 0;

    public SkinManager(SkinAnalyzerFacade analyzer) {
        this.analyzer = analyzer;
    }

    private static byte[] readFileBytes(File file) {
        if (file == null || !file.exists()) return null;
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = fis.read(buf)) != -1) {
                bos.write(buf, 0, read);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Prepares an account using the account's actual UUID.
     * This ensures the UUID registered in the Yggdrasil server matches
     * the UUID that Minecraft uses in its session requests.
     */
    public void prepareAccountWithUuid(
            String username,
            String profileId,
            File skinFile,
            File capeFile,
            SkinModelType modelOverride
    ) throws InvalidSkinException {
        byte[] skinBytes = readFileBytes(skinFile);
        byte[] capeBytes = readFileBytes(capeFile);

        Log.i(TAG, "Preparing account with explicit UUID: " + profileId);
        Log.i(TAG, "Skin bytes: " + (skinBytes != null ? skinBytes.length + " bytes" : "null"));
        Log.i(TAG, "Cape bytes: " + (capeBytes != null ? capeBytes.length + " bytes" : "null"));

        PlayerSkin skin = null;
        if (skinBytes != null) {
            PlayerSkin base = analyzer.prepareSkin(skinBytes);
            if (base == null) {
                throw new InvalidSkinException((skinFile != null ? skinFile.getName() : "Skin file") + " must be 64x64 or 64x32 pixels");
            }
            if (modelOverride != null && modelOverride != base.getModel()) {
                skin = new PlayerSkin(base.getBytes(), base.getHash(), modelOverride);
            } else {
                skin = base;
            }
            Log.i(TAG, "Skin prepared: hash=" + skin.getHash() + " model=" + skin.getModel());
        }

        PlayerCape cape = null;
        if (capeBytes != null) {
            cape = analyzer.prepareCape(capeBytes);
            if (cape != null) {
                Log.i(TAG, "Cape prepared: hash=" + cape.getHash());
            }
        }

        // Use the provided profileId directly — do NOT regenerate it
        server.addCharacter(username, profileId, skin, cape);
        Log.i(TAG, "Character registered in server: " + username + " / " + profileId);
    }

    public PreparedAccount prepareAccount(
            String username,
            File skinFile,
            File capeFile,
            SkinModelType modelOverride
    ) throws InvalidSkinException {
        byte[] skinBytes = readFileBytes(skinFile);
        byte[] capeBytes = readFileBytes(capeFile);

        PlayerSkin skin = null;
        if (skinBytes != null) {
            PlayerSkin base = analyzer.prepareSkin(skinBytes);
            if (base == null) {
                throw new InvalidSkinException((skinFile != null ? skinFile.getName() : "Skin file") + " must be 64x64 or 64x32 pixels");
            }
            if (modelOverride != null && modelOverride != base.getModel()) {
                skin = new PlayerSkin(base.getBytes(), base.getHash(), modelOverride);
            } else {
                skin = base;
            }
        }

        PlayerCape cape = null;
        if (capeBytes != null) {
            cape = analyzer.prepareCape(capeBytes);
        }

        SkinModelType model = (skin != null) ? skin.getModel() : SkinModelType.NONE;
        String profileId = LocalUuidUtils.generateProfileId(username, model);

        server.addCharacter(username, profileId, skin, cape);

        return new PreparedAccount(
                username,
                profileId,
                LocalUuidUtils.toFormattedUuid(profileId),
                model
        );
    }

    public int startServer() {
        port = server.start();
        return port;
    }

    public void stopServer() {
        server.stop();
    }

    public String getAuthlibUrl() {
        return "http://127.0.0.1:" + port;
    }
    
    public int getPort() {
        return port;
    }

    public static final SkinAnalyzerFacade androidSkinAnalyzerFacade = new SkinAnalyzerFacade() {
        @Override
        public PlayerSkin prepareSkin(byte[] bytes) {
            return SkinAnalyzer.prepareSkin(bytes);
        }

        @Override
        public PlayerCape prepareCape(byte[] bytes) {
            return SkinAnalyzer.prepareCape(bytes);
        }
    };
}

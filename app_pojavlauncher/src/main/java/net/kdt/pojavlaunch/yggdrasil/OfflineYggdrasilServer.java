package net.kdt.pojavlaunch.yggdrasil;

import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

public class OfflineYggdrasilServer {
    private static final String TAG = "OfflineSkinServer";
    
    private final String serverName;
    private final String implName;
    private final String implVersion;

    private final Map<String, Character> byUuid = new ConcurrentHashMap<>();
    private final Map<String, Character> byName = new ConcurrentHashMap<>();
    private final Map<String, byte[]> textureStore = new ConcurrentHashMap<>();

    private KeyPair keyPair;
    // private HttpServer server;
    private int port = 0;
    private boolean running = false;

    public OfflineYggdrasilServer() {
        this("HyperLauncher", "drasl", "1.4");
    }

    public OfflineYggdrasilServer(String serverName, String implName, String implVersion) {
        this.serverName = serverName;
        this.implName = implName;
        this.implVersion = implVersion;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            this.keyPair = kpg.generateKeyPair();
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate RSA keypair", e);
        }
    }

    public void addCharacter(String username, String profileId, PlayerSkin skin, PlayerCape cape) {
        String uuid = profileId.replace("-", "").toLowerCase();
        Character character = new Character(uuid, username, skin, cape);
        byUuid.put(uuid, character);
        byName.put(username.toLowerCase(), character);
        if (skin != null) {
            textureStore.put(skin.getHash(), skin.getBytes());
        }
        if (cape != null) {
            textureStore.put(cape.getHash(), cape.getBytes());
        }
        Log.d(TAG, "Added character: " + username + " (" + uuid + ")");
    }

    public synchronized int start() {
        if (running) return port;
        // HTTP Server disabled due to Android incompatibility
        Log.w(TAG, "OfflineYggdrasilServer is disabled on Android. Returning port 0.");
        running = true;
        return 0;
    }

    public synchronized void stop() {
        if (running) {
            // server.stop(0);
            port = 0;
            running = false;
            Log.i(TAG, "OfflineYggdrasilServer stopped");
        }
    }

    public int getPort() {
        return port;
    }

    private String signRsa(String data) {
        try {
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initSign(keyPair.getPrivate());
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(sig.sign(), Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "RSA sign failed", e);
            return "";
        }
    }

    private String localBase() {
        return "http://127.0.0.1:" + port;
    }

    private String buildRoot() {
        try {
            JSONObject root = new JSONObject();
            JSONArray skinDomains = new JSONArray();
            skinDomains.put("127.0.0.1");
            skinDomains.put("localhost");
            root.put("skinDomains", skinDomains);

            JSONObject meta = new JSONObject();
            meta.put("serverName", serverName);
            meta.put("implementationName", implName);
            meta.put("implementationVersion", implVersion);
            meta.put("feature.non_email_login", true);
            meta.put("feature.legacy_skin_api", true);
            root.put("meta", meta);

            String publicKeyBase64 = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT).trim();
            root.put("signaturePublickey", "-----BEGIN PUBLIC KEY-----\n" + publicKeyBase64 + "\n-----END PUBLIC KEY-----");

            return root.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private static class Character {
        final String uuid;
        final String name;
        final PlayerSkin skin;
        final PlayerCape cape;

        Character(String uuid, String name, PlayerSkin skin, PlayerCape cape) {
            this.uuid = uuid;
            this.name = name;
            this.skin = skin;
            this.cape = cape;
        }

        String toProfileResponse(String baseUrl, Signer signer) {
            try {
                JSONObject texturesObj = new JSONObject();
                texturesObj.put("timestamp", System.currentTimeMillis());
                texturesObj.put("profileId", uuid);
                texturesObj.put("profileName", name);
                
                JSONObject textures = new JSONObject();
                if (skin != null) {
                    JSONObject skinObj = new JSONObject();
                    skinObj.put("url", baseUrl + "/textures/" + skin.getHash());
                    if (skin.getModel() == SkinModelType.ALEX) {
                        JSONObject metadata = new JSONObject();
                        metadata.put("model", "slim");
                        skinObj.put("metadata", metadata);
                    }
                    textures.put("SKIN", skinObj);
                }
                if (cape != null) {
                    JSONObject capeObj = new JSONObject();
                    capeObj.put("url", baseUrl + "/textures/" + cape.getHash());
                    textures.put("CAPE", capeObj);
                }
                texturesObj.put("textures", textures);

                String texturesJson = texturesObj.toString();
                String encoded = Base64.encodeToString(texturesJson.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                String signature = signer.sign(encoded);

                JSONObject response = new JSONObject();
                response.put("id", uuid);
                response.put("name", name);
                
                JSONArray properties = new JSONArray();
                JSONObject texturesProp = new JSONObject();
                texturesProp.put("name", "textures");
                texturesProp.put("value", encoded);
                texturesProp.put("signature", signature);
                properties.put(texturesProp);
                
                response.put("properties", properties);
                return response.toString();
            } catch (Exception e) {
                Log.e(TAG, "Failed to build profile response", e);
                return "{}";
            }
        }
    }

    private interface Signer {
        String sign(String data);
    }
}

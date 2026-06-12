package net.kdt.pojavlaunch.yggdrasil;

import android.util.Base64;
import android.util.Log;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
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
    private HttpServer server;
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
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            port = server.getAddress().getPort();
            
            server.createContext("/", new RootHandler());
            server.createContext("/api", new RootHandler());
            server.createContext("/api/profiles/minecraft", new ProfilesHandler());
            server.createContext("/sessionserver/session/minecraft/hasJoined", new HasJoinedHandler());
            server.createContext("/sessionserver/session/minecraft/join", new JoinHandler());
            server.createContext("/sessionserver/session/minecraft/profile/", new ProfileHandler());
            server.createContext("/textures/", new TexturesHandler());
            
            server.setExecutor(null);
            server.start();
            running = true;
            Log.i(TAG, "Server started on 127.0.0.1:" + port);
            return port;
        } catch (IOException e) {
            Log.e(TAG, "Failed to start OfflineYggdrasilServer", e);
            return 0;
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
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

    private static void sendJsonResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        exchange.close();
    }

    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendJsonResponse(exchange, buildRoot(), 200);
        }
    }

    private class ProfilesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJsonResponse(exchange, "{}", 405);
                return;
            }

            try {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                String body = bos.toString("UTF-8");
                JSONArray namesJson = new JSONArray(body);
                
                JSONArray result = new JSONArray();
                for (int i = 0; i < namesJson.length(); i++) {
                    String name = namesJson.getString(i);
                    Character c = byName.get(name.toLowerCase());
                    if (c != null) {
                        JSONObject profile = new JSONObject();
                        profile.put("id", c.uuid);
                        profile.put("name", c.name);
                        result.put(profile);
                    }
                }
                sendJsonResponse(exchange, result.toString(), 200);
            } catch (Exception e) {
                Log.e(TAG, "Profiles lookup failed", e);
                sendJsonResponse(exchange, "[]", 200);
            }
        }
    }

    private class HasJoinedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String username = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 0 && pair[0].equals("username")) {
                        username = pair.length > 1 ? pair[1] : "";
                        break;
                    }
                }
            }

            if (username == null) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }

            Character character = byName.get(username.toLowerCase());
            if (character == null) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            String response = character.toProfileResponse(localBase(), OfflineYggdrasilServer.this::signRsa);
            sendJsonResponse(exchange, response, 200);
        }
    }

    private class JoinHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        }
    }

    private class ProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String uuid = path.substring(path.lastIndexOf('/') + 1).toLowerCase().replace("-", "");
            
            Character character = byUuid.get(uuid);
            if (character == null) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            String response = character.toProfileResponse(localBase(), OfflineYggdrasilServer.this::signRsa);
            sendJsonResponse(exchange, response, 200);
        }
    }

    private class TexturesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String hash = path.substring(path.lastIndexOf('/') + 1);

            byte[] bytes = textureStore.get(hash);
            if (bytes == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.getResponseHeaders().set("Cache-Control", "max-age=2592000, public");
            exchange.getResponseHeaders().set("ETag", "\"" + hash + "\"");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            exchange.close();
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

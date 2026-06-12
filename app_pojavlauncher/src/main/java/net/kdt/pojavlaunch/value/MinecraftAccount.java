package net.kdt.pojavlaunch.value;


import android.graphics.BitmapFactory;
import android.util.Log;

import net.kdt.pojavlaunch.*;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.*;
import com.google.gson.*;
import android.graphics.Bitmap;
import android.util.Base64;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import org.apache.commons.io.IOUtils;

@SuppressWarnings("IOStreamConstructor")
@Keep
public class MinecraftAccount {
    public String accessToken = "0"; // access token
    public String clientToken = "0"; // clientID: refresh and invalidate
    public String profileId = "00000000-0000-0000-0000-000000000000"; // profile UUID, for obtaining skin
    public String username = "Steve";
    public String selectedVersion = "1.7.10";
    public boolean isMicrosoft = false;
    public String msaRefreshToken = "0";
    public String xuid;
    public long expiresAt;
    public String skinFaceBase64;
    private Bitmap mFaceCache;
    
    void updateSkinFace(String uuid) {
        try {
            File skinFile = getSkinFaceFile(username);
            Tools.downloadFile("https://mc-heads.net/head/" + uuid + "/100", skinFile.getAbsolutePath());
            
            Log.i("SkinLoader", "Update skin face success");
        } catch (IOException e) {
            // Skin refresh limit, no internet connection, etc...
            // Simply ignore updating skin face
            Log.w("SkinLoader", "Could not update skin face", e);
        }
    }

    public boolean isLocal(){
        return false;
    }

    public boolean isDemo(){
        return false;
    }
    
    public void updateSkinFace() {
        updateSkinFace(profileId);
    }
    
    public String save(String outPath) throws IOException {
        Tools.write(outPath, Tools.GLOBAL_GSON.toJson(this));
        return username;
    }
    
    public String save() throws IOException {
        return save(Tools.DIR_ACCOUNT_NEW + "/" + username + ".json");
    }
    
    public static MinecraftAccount parse(String content) throws JsonSyntaxException {
        return Tools.GLOBAL_GSON.fromJson(content, MinecraftAccount.class);
    }
    @Nullable
    public static MinecraftAccount load(String name) {
        if(!accountExists(name)) return null;
        try {
            MinecraftAccount acc = parse(Tools.read(Tools.DIR_ACCOUNT_NEW + "/" + name + ".json"));
            if (acc.accessToken == null) {
                acc.accessToken = "0";
            }
            if (acc.clientToken == null) {
                acc.clientToken = "0";
            }
            if (acc.username == null) {
                acc.username = "0";
            }
            if (!acc.isMicrosoft) {
                net.kdt.pojavlaunch.yggdrasil.SkinModelType model = net.kdt.pojavlaunch.yggdrasil.SkinModelType.NONE;
                File skinFile = new File(Tools.DIR_DATA + "/skins/" + acc.username + "_skin.png");
                if (skinFile.exists()) {
                    File skinMeta = new File(Tools.DIR_DATA + "/skins/" + acc.username + "_metadata.json");
                    if (skinMeta.exists()) {
                        try {
                            String metaContent = Tools.read(skinMeta.getAbsolutePath());
                            if (metaContent.contains("slim")) {
                                model = net.kdt.pojavlaunch.yggdrasil.SkinModelType.ALEX;
                            } else {
                                model = net.kdt.pojavlaunch.yggdrasil.SkinModelType.STEVE;
                            }
                        } catch (Exception e) {
                            model = net.kdt.pojavlaunch.yggdrasil.SkinModelType.STEVE;
                        }
                    } else {
                        try (FileInputStream fis = new java.io.FileInputStream(skinFile);
                             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = fis.read(buf)) != -1) {
                                bos.write(buf, 0, r);
                            }
                            model = net.kdt.pojavlaunch.yggdrasil.SkinAnalyzer.detectModel(bos.toByteArray());
                        } catch (Exception e) {
                            model = net.kdt.pojavlaunch.yggdrasil.SkinModelType.STEVE;
                        }
                    }
                }
                String rawUuid = net.kdt.pojavlaunch.yggdrasil.LocalUuidUtils.generateProfileId(acc.username, model);
                acc.profileId = net.kdt.pojavlaunch.yggdrasil.LocalUuidUtils.toFormattedUuid(rawUuid);
            } else if (acc.profileId == null) {
                acc.profileId = "00000000-0000-0000-0000-000000000000";
            }
            if (acc.selectedVersion == null) {
                acc.selectedVersion = "1.7.10";
            }
            if (acc.msaRefreshToken == null) {
                acc.msaRefreshToken = "0";
            }
            return acc;
        } catch(NullPointerException | IOException | JsonSyntaxException e) {
            Log.e(MinecraftAccount.class.getName(), "Caught an exception while loading the profile",e);
            return null;
        }
    }

    public void clearFaceCache() {
        mFaceCache = null;
    }

    public static Bitmap roundBitmap(Bitmap src, int size, float cornerRadius) {
        if (src == null) return null;
        try {
            Bitmap scaled = Bitmap.createScaledBitmap(src, size, size, false);
            Bitmap rounded = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(rounded);
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setAntiAlias(true);
            android.graphics.Rect rect = new android.graphics.Rect(0, 0, size, size);
            android.graphics.RectF rectF = new android.graphics.RectF(rect);
            paint.setColor(0xffffffff);
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint);
            paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(scaled, null, rect, paint);
            if (scaled != src) {
                scaled.recycle();
            }
            return rounded;
        } catch (Exception e) {
            Log.w("MinecraftAccount", "Error rounding bitmap", e);
            return src;
        }
    }

    public static Bitmap extractSkinHead(Bitmap fullSkin) {
        if (fullSkin == null) return null;
        try {
            Bitmap baseHead = Bitmap.createBitmap(fullSkin, 8, 8, 8, 8);
            Bitmap combined = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(combined);
            canvas.drawBitmap(baseHead, 0, 0, null);
            baseHead.recycle();
            
            if (fullSkin.getWidth() >= 48 && fullSkin.getHeight() >= 16) {
                Bitmap hat = Bitmap.createBitmap(fullSkin, 40, 8, 8, 8);
                canvas.drawBitmap(hat, 0, 0, null);
                hat.recycle();
            }
            
            Bitmap rounded = roundBitmap(combined, 64, 10f);
            combined.recycle();
            return rounded;
        } catch (Exception e) {
            Log.w("MinecraftAccount", "Error extracting skin head", e);
            return null;
        }
    }

    public Bitmap getSkinFace(){
        if(isLocal()) return null;

        File skinFaceFile = getSkinFaceFile(username);
        if (!skinFaceFile.exists()) {
            File customSkinFile = new File(Tools.DIR_DATA + "/skins/" + username + "_skin.png");
            if (customSkinFile.exists()) {
                try {
                    Bitmap fullSkin = BitmapFactory.decodeFile(customSkinFile.getAbsolutePath());
                    if (fullSkin != null) {
                        Bitmap head = extractSkinHead(fullSkin);
                        fullSkin.recycle();
                        if (head != null) return head;
                    }
                } catch (Exception e) {
                    Log.w("MinecraftAccount", "Failed to extract local skin face", e);
                }
            }

            // Legacy version, storing the head inside the json as base 64
            if(skinFaceBase64 == null) return null;
            byte[] faceIconBytes = Base64.decode(skinFaceBase64, Base64.DEFAULT);
            Bitmap base64Bitmap = BitmapFactory.decodeByteArray(faceIconBytes, 0, faceIconBytes.length);
            if (base64Bitmap != null) {
                Bitmap rounded = roundBitmap(base64Bitmap, 64, 10f);
                base64Bitmap.recycle();
                return rounded;
            }
            return null;
        } else {
            if(mFaceCache == null) {
                Bitmap cached = BitmapFactory.decodeFile(skinFaceFile.getAbsolutePath());
                if (cached != null) {
                    mFaceCache = roundBitmap(cached, 64, 10f);
                    cached.recycle();
                }
            }
        }

        return mFaceCache;
    }

    public static Bitmap getSkinFace(String username) {
        File customSkinFile = new File(Tools.DIR_DATA + "/skins/" + username + "_skin.png");
        if (customSkinFile.exists()) {
            try {
                Bitmap fullSkin = BitmapFactory.decodeFile(customSkinFile.getAbsolutePath());
                if (fullSkin != null) {
                    Bitmap head = extractSkinHead(fullSkin);
                    fullSkin.recycle();
                    if (head != null) {
                        return head;
                    }
                }
            } catch (Exception e) {
                Log.w("MinecraftAccount", "Failed to extract local skin face for " + username, e);
            }
        }
        File cachedHead = getSkinFaceFile(username);
        if (cachedHead.exists()) {
            try {
                Bitmap cached = BitmapFactory.decodeFile(cachedHead.getAbsolutePath());
                if (cached != null) {
                    Bitmap rounded = roundBitmap(cached, 64, 10f);
                    cached.recycle();
                    return rounded;
                }
            } catch (Exception e) {
                Log.w("MinecraftAccount", "Failed to decode cached skin face for " + username, e);
            }
        }
        return null;
    }

    private static File getSkinFaceFile(String username) {
        return new File(Tools.DIR_CACHE, username + ".png");
    }

    private static boolean accountExists(String username){
        return new File(Tools.DIR_ACCOUNT_NEW + "/" + username + ".json").exists();
    }
}

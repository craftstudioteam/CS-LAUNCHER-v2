package net.kdt.pojavlaunch.yggdrasil;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import java.security.MessageDigest;

public class SkinAnalyzer {

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static SkinModelType detectSkinModel(int imageHeight, PixelAlphaProvider provider) {
        if (imageHeight == 32) return SkinModelType.STEVE;

        if (allTransparent(50, 51, 16, 19, provider) &&
            allTransparent(54, 55, 20, 31, provider) &&
            allTransparent(42, 43, 48, 51, provider) &&
            allTransparent(46, 47, 52, 63, provider)) {
            return SkinModelType.ALEX;
        }
        return SkinModelType.STEVE;
    }

    private static boolean allTransparent(int xStart, int xEnd, int yStart, int yEnd, PixelAlphaProvider provider) {
        for (int x = xStart; x <= xEnd; x++) {
            for (int y = yStart; y <= yEnd; y++) {
                if (provider.getAlpha(x, y) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public interface PixelAlphaProvider {
        int getAlpha(int x, int y);
    }

    public static boolean validate(byte[] bytes) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        boolean valid = (opts.outWidth == 64 && opts.outHeight == 64) ||
               (opts.outWidth == 64 && opts.outHeight == 32);
        android.util.Log.i("SkinAnalyzer", "Skin PNG validation: " + valid + " (" + opts.outWidth + "x" + opts.outHeight + ")");
        return valid;
    }

    public static SkinModelType detectModel(byte[] bytes) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        android.util.Log.i("SkinAnalyzer", "BitmapFactory.decodeByteArray returned non-null: " + (bmp != null));
        if (bmp == null) {
            android.util.Log.e("SkinAnalyzer", "Failed to decode skin PNG (corrupted PNG)");
            return SkinModelType.STEVE;
        }
        try {
            SkinModelType model = detectSkinModel(opts.outHeight, (x, y) -> Color.alpha(bmp.getPixel(x, y)));
            android.util.Log.i("SkinAnalyzer", "Model detected: " + model.name());
            return model;
        } finally {
            if (!bmp.isRecycled()) {
                bmp.recycle();
            }
        }
    }

    public static PlayerSkin prepareSkin(byte[] bytes) {
        if (!validate(bytes)) return null;
        return new PlayerSkin(bytes, sha256Hex(bytes), detectModel(bytes));
    }

    public static PlayerCape prepareCape(byte[] bytes) {
        return new PlayerCape(bytes, sha256Hex(bytes));
    }
}

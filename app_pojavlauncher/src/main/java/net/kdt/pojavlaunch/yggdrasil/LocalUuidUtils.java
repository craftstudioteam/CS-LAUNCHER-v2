package net.kdt.pojavlaunch.yggdrasil;

import java.util.Locale;

public class LocalUuidUtils {
    private static String strFill(String str, char code, int length) {
        if (str.length() > length) {
            return str.substring(0, length);
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(code);
        }
        return sb.substring(str.length()) + str;
    }

    private static String baseUuid(String name) {
        String lengthPart = strFill(Integer.toHexString(name.length()), '0', 16);
        String hashPart = strFill(Long.toHexString(name.hashCode() & 0xFFFFFFFFL), '0', 16);
        
        StringBuilder sb = new StringBuilder(32);
        sb.append(lengthPart.substring(0, 12));
        sb.append('3');
        sb.append(lengthPart.substring(13, 16));
        sb.append('9');
        sb.append(hashPart.substring(0, 15));
        return sb.toString();
    }

    public static String generateProfileId(String username, SkinModelType model) {
        String base = baseUuid(username);
        if (model == SkinModelType.NONE) {
            return base;
        }

        String prefix = base.substring(0, 27);
        int a = Character.digit(base.charAt(7), 16);
        int b = Character.digit(base.charAt(15), 16);
        int c = Character.digit(base.charAt(23), 16);
        long maxSuffix = 0xFFFFFL;
        long suffix = Long.parseLong(base.substring(27), 16);

        for (long i = 0; i <= maxSuffix; i++) {
            int d = (int) (suffix & 0xF);

            if ((a ^ b ^ c ^ d) % 2 == model.getTargetParity()) {
                String suffixStr = Long.toHexString(suffix);
                while (suffixStr.length() < 5) {
                    suffixStr = "0" + suffixStr;
                }
                return prefix + suffixStr.toUpperCase(Locale.ROOT);
            }
            suffix = (suffix == maxSuffix) ? 0L : suffix + 1;
        }
        
        String suffixStr = Long.toHexString(suffix);
        while (suffixStr.length() < 5) {
            suffixStr = "0" + suffixStr;
        }
        return prefix + suffixStr.toUpperCase(Locale.ROOT);
    }

    public static String toFormattedUuid(String s) {
        if (s == null) return null;
        s = s.replace("-", "");
        if (s.length() != 32) return s;
        return s.substring(0, 8) + "-" +
               s.substring(8, 12) + "-" +
               s.substring(12, 16) + "-" +
               s.substring(16, 20) + "-" +
               s.substring(20);
    }
}

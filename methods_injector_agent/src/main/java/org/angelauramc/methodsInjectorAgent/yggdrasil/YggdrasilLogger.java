package org.angelauramc.methodsInjectorAgent.yggdrasil;

import java.lang.reflect.Method;
import java.util.Collection;

public class YggdrasilLogger {
    public static void logGameProfile(Object profile) {
        try {
            if (profile == null) {
                System.out.println("[YggdrasilLogger] GameProfile is null");
                return;
            }
            Class<?> profileClass = profile.getClass();
            Method getIdMethod = profileClass.getMethod("getId");
            Method getNameMethod = profileClass.getMethod("getName");
            Method getPropertiesMethod = profileClass.getMethod("getProperties");

            Object id = getIdMethod.invoke(profile);
            Object name = getNameMethod.invoke(profile);
            Object properties = getPropertiesMethod.invoke(profile);

            System.out.println("[YggdrasilLogger] === GAMEPROFILE INJECTION LOG ===");
            System.out.println("[YggdrasilLogger] ID: " + id);
            System.out.println("[YggdrasilLogger] Name: " + name);

            if (properties != null) {
                Method getMethod = properties.getClass().getMethod("get", Object.class);
                Collection<?> textures = (Collection<?>) getMethod.invoke(properties, "textures");
                if (textures != null) {
                    System.out.println("[YggdrasilLogger] Properties 'textures' count: " + textures.size());
                    for (Object prop : textures) {
                        Class<?> propClass = prop.getClass();
                        Method getValueMethod = propClass.getMethod("getValue");
                        Method getSignatureMethod = propClass.getMethod("getSignature");
                        
                        String value = (String) getValueMethod.invoke(prop);
                        String signature = (String) getSignatureMethod.invoke(prop);

                        System.out.println("[YggdrasilLogger] Property 'textures':");
                        System.out.println("[YggdrasilLogger]   Value: " + value);
                        System.out.println("[YggdrasilLogger]   Signature: " + signature);

                        if (value != null) {
                            try {
                                byte[] decodedBytes = java.util.Base64.getDecoder().decode(value);
                                String decoded = new String(decodedBytes, "UTF-8");
                                System.out.println("[YggdrasilLogger]   Decoded Value: " + decoded);
                            } catch (Throwable t) {
                                System.out.println("[YggdrasilLogger]   Failed to decode value: " + t.getMessage());
                            }
                        }
                    }
                } else {
                    System.out.println("[YggdrasilLogger] Properties 'textures' collection is null");
                }
            } else {
                System.out.println("[YggdrasilLogger] Properties map is null");
            }
            System.out.println("[YggdrasilLogger] =================================");
        } catch (Throwable t) {
            System.out.println("[YggdrasilLogger] Failed to log GameProfile: " + t.getMessage());
            t.printStackTrace();
        }
    }
}

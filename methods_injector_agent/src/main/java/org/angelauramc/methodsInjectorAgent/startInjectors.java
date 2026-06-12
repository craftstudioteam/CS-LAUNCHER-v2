package org.angelauramc.methodsInjectorAgent;

import org.angelauramc.methodsInjectorAgent.lwjgl2_methods_injector.ALC10Injector;
import org.angelauramc.methodsInjectorAgent.lwjgl2_methods_injector.ASM5OverrideInjector;
import org.angelauramc.methodsInjectorAgent.mods_compatibility_injector.VeilImguiOverrideDisable;

import java.lang.instrument.Instrumentation;

public class startInjectors {
    public static void premain(String args, Instrumentation inst) {
        try {
            // Check if we have the asm classes we need
            Class.forName("org.objectweb.asm.ClassReader");
            Class.forName("org.objectweb.asm.ClassVisitor");
            Class.forName("org.objectweb.asm.ClassWriter");
            Class.forName("org.objectweb.asm.MethodVisitor");
            Class.forName("org.objectweb.asm.Opcodes");
            Package asmPackage = org.objectweb.asm.Opcodes.class.getPackage();
            String implVersion = asmPackage.getImplementationVersion();
            if (implVersion == null) implVersion = "not found";
            System.out.println("Copper-Android: Detected ASM version: " + implVersion);
            ALC10Injector.premain(args, inst);
            VeilImguiOverrideDisable.premain(args, inst);
            // This is the version we override old asm vers with. So we add the patches
            // so the older version bugs are ported.
            if (implVersion.equals("5.0.4")) ASM5OverrideInjector.premain(args, inst);
            
            // Register Yggdrasil Authlib endpoint redirector
            inst.addTransformer(new java.lang.instrument.ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        java.security.ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                    if (className != null && className.startsWith("com/mojang/authlib/")) {
                        try {
                            return org.angelauramc.methodsInjectorAgent.yggdrasil.YggdrasilTransformer.transform(classfileBuffer);
                        } catch (Exception e) {
                            System.out.println("Failed to transform Authlib class: " + className);
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
            });
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
        }
    }
}

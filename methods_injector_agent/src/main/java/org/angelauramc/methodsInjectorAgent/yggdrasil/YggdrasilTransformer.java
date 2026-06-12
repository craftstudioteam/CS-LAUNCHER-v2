package org.angelauramc.methodsInjectorAgent.yggdrasil;

import org.objectweb.asm.*;

public class YggdrasilTransformer {
    public static byte[] transform(byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                
                // Patch isAllowedTextureDomain to always return true
                if (name.equals("isAllowedTextureDomain")) {
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            super.visitInsn(Opcodes.ICONST_1);
                            super.visitInsn(Opcodes.IRETURN);
                        }
                    };
                }
                
                // Redirect mojang domains in all method LDC instructions
                return new MethodVisitor(Opcodes.ASM5, mv) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof String) {
                            String s = (String) value;
                            if (s.startsWith("https://sessionserver.mojang.com")) {
                                String host = System.getProperty("minecraft.api.session.host");
                                if (host != null) {
                                    s = s.replace("https://sessionserver.mojang.com", host);
                                }
                            } else if (s.startsWith("https://authserver.mojang.com")) {
                                String host = System.getProperty("minecraft.api.auth.host");
                                if (host != null) {
                                    s = s.replace("https://authserver.mojang.com", host);
                                }
                            } else if (s.startsWith("https://api.mojang.com")) {
                                String host = System.getProperty("minecraft.api.account.host");
                                if (host != null) {
                                    s = s.replace("https://api.mojang.com", host);
                                }
                            } else if (s.startsWith("https://api.minecraftservices.com")) {
                                String host = System.getProperty("minecraft.api.services.host");
                                if (host != null) {
                                    s = s.replace("https://api.minecraftservices.com", host);
                                }
                            }
                            value = s;
                        }
                        super.visitLdcInsn(value);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }
}

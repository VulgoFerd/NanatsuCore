import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.zip.*;
import java.util.Enumeration;

// Custom ClassWriter that handles missing classes gracefully
class SafeClassWriter extends ClassWriter {
    public SafeClassWriter(int flags) {
        super(flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (Exception e) {
            // If we can't resolve the common superclass, fall back to Object
            return "java/lang/Object";
        }
    }
}

public class PatchBig {
    static final String IN_JAR  = "C:\\Users\\Fernando\\nntdarkmp\\mods\\JRMCore-v1.3.51.jar";
    static final String OUT_JAR = "C:\\Users\\Fernando\\Desktop\\NanatsuCore\\artifacts\\JRMCore-v1.3.51-bignum.jar";

    public static void main(String[] args) throws Exception {
        File in  = new File(IN_JAR);
        File out = new File(OUT_JAR);
        if (!in.exists()) throw new FileNotFoundException(IN_JAR);
        out.getParentFile().mkdirs();

        int patched = 0;
        try (ZipFile zip = new ZipFile(in); ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out))) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                String name = entry.getName();
                byte[] data = zip.getInputStream(entry).readAllBytes();

                if (name.equals("JinRyuu/JRMCore/JRMCoreH.class")) {
                    data = patchJRMCoreH(data);
                    patched++;
                    System.out.println("PATCHED " + entry.getName() + " (JRMCoreH)");
                } else if (name.equals("JinRyuu/JRMCore/ComJrmctp.class")) {
                    data = patchComJrmctp(data);
                    patched++;
                    System.out.println("PATCHED " + entry.getName() + " (ComJrmctp)");
                } else if (name.equals("JinRyuu/JRMCore/ComJrmcRacialSkill.class")) {
                    data = patchComJrmcRacialSkill(data);
                    patched++;
                    System.out.println("PATCHED " + entry.getName() + " (ComJrmcRacialSkill)");
                } else if (name.equals("JinRyuu/JRMCore/JRMCoreComTickH.class")) {
                    data = patchJRMCoreComTickH(data);
                    patched++;
                    System.out.println("PATCHED " + entry.getName() + " (JRMCoreComTickH)");
                } else if (name.equals("JinRyuu/JRMCore/JRMCorePacHanS.class")) {
                    data = patchJRMCorePacHanS(data);
                    patched++;
                    System.out.println("PATCHED " + entry.getName() + " (JRMCorePacHanS)");
                } else if (name.equals("JinRyuu/JRMCore/p/JRMCorePStats2.class")) {
                    data = patchJRMCorePStats2(data);
                    patched++;
                    System.out.println("PATCHED " + entry.getName() + " (JRMCorePStats2)");
                } else if (name.equals("JinRyuu/JRMCore/p/JRMCorePCost.class")) {
                    data = patchJRMCorePCost(data);
                    patched++;
                    System.out.println("PATCHED " + entry.getName() + " (JRMCorePCost)");
                } else if (name.equals("JinRyuu/JRMCore/JRMCorePacHanC.class")) {
                    data = patchJRMCorePacHanC(data);
                    patched++;
                    System.out.println("PATCHED " + entry.getName() + " (JRMCorePacHanC)");
                }

                ZipEntry ne = new ZipEntry(name);
                ne.setTime(entry.getTime());
                zos.putNextEntry(ne);
                zos.write(data);
                zos.closeEntry();
            }
        }
        System.out.println("\nPatched " + patched + " classes.");
        System.out.println("Output: " + out.getAbsolutePath());
    }

    // ============================================================
    // JRMCoreH - Add curTPLong field, modify getInt/setInt, add helpers
    // ============================================================
    static byte[] patchJRMCoreH(byte[] data) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(data).accept(cn, 0);

        // 1. Ensure getMaxTP returns Integer.MAX_VALUE
        for (MethodNode m : cn.methods) {
            if (m.name.equals("getMaxTP") && m.desc.equals("()I")) {
                m.instructions.clear();
                m.instructions.add(new LdcInsnNode(Integer.MAX_VALUE));
                m.instructions.add(new InsnNode(Opcodes.IRETURN));
                System.out.println("  -> getMaxTP confirmed: Integer.MAX_VALUE");
            }
        }

        // 2. Add curTPLong static field after curTP field
        boolean hasCurTPLong = false;
        for (FieldNode f : cn.fields) {
            if ("curTPLong".equals(f.name) && "J".equals(f.desc)) {
                hasCurTPLong = true;
                break;
            }
        }
        if (!hasCurTPLong) {
            for (int i = 0; i < cn.fields.size(); i++) {
                FieldNode f = cn.fields.get(i);
                if ("curTP".equals(f.name) && "I".equals(f.desc)) {
                    FieldNode fnLong = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "curTPLong", "J", null, null);
                    cn.fields.add(i + 1, fnLong);
                    System.out.println("  -> Added curTPLong static long field");
                    break;
                }
            }
        }

        // 3. Rewrite getInt to prefer jrmcTplong, saturate safely, fallback to jrmcTpint
        for (MethodNode m : cn.methods) {
            if (m.name.equals("getInt") && m.desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)I")) {
                m.instructions.clear();
                m.maxStack = Math.max(m.maxStack, 4);
                m.maxLocals = Math.max(m.maxLocals, 4);

                LabelNode lTpLong = new LabelNode();
                LabelNode lReturnMirror = new LabelNode();
                LabelNode lReturnSaturated = new LabelNode();

                // Get NBT "pres"
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                m.instructions.add(new LdcInsnNode("pres"));
                m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "nbt", "(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Lnet/minecraft/nbt/NBTTagCompound;", false));
                m.instructions.add(new InsnNode(Opcodes.DUP));
                m.instructions.add(new VarInsnNode(Opcodes.ASTORE, 2)); // local 2 = NBTTagCompound

                // Read jrmcTplong
                m.instructions.add(new LdcInsnNode("jrmcTplong"));
                m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74763_f", "(Ljava/lang/String;)J", false));
                m.instructions.add(new VarInsnNode(Opcodes.LSTORE, 3)); // local 3-4 = long

                // If long != 0 -> use long path (authoritative)
                m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 3));
                m.instructions.add(new InsnNode(Opcodes.LCONST_0));
                m.instructions.add(new InsnNode(Opcodes.LCMP));
                m.instructions.add(new JumpInsnNode(Opcodes.IFNE, lTpLong));

                // long == 0: fallback to jrmcTpint
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74762_e", "(Ljava/lang/String;)I", false));
                m.instructions.add(new JumpInsnNode(Opcodes.GOTO, lReturnMirror));

                // long != 0: saturate
                m.instructions.add(lTpLong);
                m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 3));
                m.instructions.add(new InsnNode(Opcodes.LCONST_0));
                m.instructions.add(new InsnNode(Opcodes.LCMP));
                // if <= 0 -> return 0
                LabelNode lGtZero = new LabelNode();
                m.instructions.add(new JumpInsnNode(Opcodes.IFGT, lGtZero));
                m.instructions.add(new InsnNode(Opcodes.ICONST_0));
                m.instructions.add(new JumpInsnNode(Opcodes.GOTO, lReturnMirror));

                // > 0 -> if >= MAX_VALUE return MAX_VALUE else (int)value
                m.instructions.add(lGtZero);
                m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 3));
                m.instructions.add(new LdcInsnNode((long) Integer.MAX_VALUE));
                m.instructions.add(new InsnNode(Opcodes.LCMP));
                // if < MAX_VALUE go to exact
                LabelNode lExact = new LabelNode();
                m.instructions.add(new JumpInsnNode(Opcodes.IFLT, lExact));
                // >= MAX_VALUE
                m.instructions.add(new LdcInsnNode(Integer.MAX_VALUE));
                m.instructions.add(new JumpInsnNode(Opcodes.GOTO, lReturnMirror));

                // exact
                m.instructions.add(lExact);
                m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 3));
                m.instructions.add(new InsnNode(Opcodes.L2I));
                m.instructions.add(new JumpInsnNode(Opcodes.GOTO, lReturnMirror));

                m.instructions.add(lReturnMirror);
                m.instructions.add(new InsnNode(Opcodes.IRETURN));

                System.out.println("  -> getInt rewritten: prefers jrmcTplong with safe saturation (<=0->0, 0< v <= MAX->v, >MAX->MAX), no Math.toIntExact on big values");
            }
        }

        // 4. Rewrite setInt(int, EntityPlayer, String):
        //    - key "jrmcTpint" -> route to setTPFromMirror(NBT, int):
        //        * long authority <= Integer.MAX_VALUE: exact int value (original semantics)
        //        * long authority >  Integer.MAX_VALUE: value is a saturated mirror, so apply
        //          delta = value - Integer.MAX_VALUE to the long authority (never truncate it)
        //    - any other key -> original behavior (write int to NBT)
        for (MethodNode m : cn.methods) {
            if (m.name.equals("setInt") && m.desc.equals("(ILnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)V")) {
                m.instructions.clear();
                m.maxStack = Math.max(m.maxStack, 3);
                m.maxLocals = Math.max(m.maxLocals, 4);

                LabelNode lOtherKey = new LabelNode();

                // if (!"jrmcTpint".equals(key)) goto other
                m.instructions.add(new LdcInsnNode("jrmcTpint"));
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
                m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
                m.instructions.add(new JumpInsnNode(Opcodes.IFEQ, lOtherKey));

                // TP path: setTPFromMirror(nbt(player,"pres"), value)
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                m.instructions.add(new LdcInsnNode("pres"));
                m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "nbt", "(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Lnet/minecraft/nbt/NBTTagCompound;", false));
                m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 0));
                m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "setTPFromMirror", "(Lnet/minecraft/nbt/NBTTagCompound;I)V", false));
                m.instructions.add(new InsnNode(Opcodes.RETURN));

                // Other keys: nbt(player,"pres").setInteger(key, value)
                m.instructions.add(lOtherKey);
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                m.instructions.add(new LdcInsnNode("pres"));
                m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "nbt", "(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Lnet/minecraft/nbt/NBTTagCompound;", false));
                m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
                m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 0));
                m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V", false));
                m.instructions.add(new InsnNode(Opcodes.RETURN));
                System.out.println("  -> setInt(int,...) rewritten: jrmcTpint -> setTPFromMirror (long-safe), other keys unchanged");
            }
        }

        // ============================================================
        // Long-TP authority helpers
        //   jrmcTplong = 64-bit authority
        //   jrmcTpint  = saturated int mirror (min(authority, Integer.MAX_VALUE), floor 0)
        // ============================================================

        // getTPLong(NBTTagCompound)J: reads jrmcTplong, falls back to jrmcTpint for legacy data
        if (!hasMethodDecl(cn, "getTPLong", "(Lnet/minecraft/nbt/NBTTagCompound;)J")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "getTPLong", "(Lnet/minecraft/nbt/NBTTagCompound;)J", null, null);
            LabelNode lFallback = new LabelNode();
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new LdcInsnNode("jrmcTplong"));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74763_f", "(Ljava/lang/String;)J", false));
            m.instructions.add(new VarInsnNode(Opcodes.LSTORE, 1));
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 1));
            m.instructions.add(new InsnNode(Opcodes.LCONST_0));
            m.instructions.add(new InsnNode(Opcodes.LCMP));
            m.instructions.add(new JumpInsnNode(Opcodes.IFEQ, lFallback));
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 1));
            m.instructions.add(new InsnNode(Opcodes.LRETURN));
            m.instructions.add(lFallback);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new LdcInsnNode("jrmcTpint"));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74762_e", "(Ljava/lang/String;)I", false));
            m.instructions.add(new InsnNode(Opcodes.I2L));
            m.instructions.add(new InsnNode(Opcodes.LRETURN));
            cn.methods.add(m);
            System.out.println("  -> Added getTPLong helper (long authority with int fallback)");
        }

        // saturateTP(J)I: <=0 -> 0, 0<v<=MAX -> v, >MAX -> MAX
        if (!hasMethodDecl(cn, "saturateTP", "(J)I")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "saturateTP", "(J)I", null, null);
            LabelNode lPositive = new LabelNode();
            LabelNode lExact = new LabelNode();
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 0));
            m.instructions.add(new InsnNode(Opcodes.LCONST_0));
            m.instructions.add(new InsnNode(Opcodes.LCMP));
            m.instructions.add(new JumpInsnNode(Opcodes.IFGT, lPositive));
            m.instructions.add(new InsnNode(Opcodes.ICONST_0));
            m.instructions.add(new InsnNode(Opcodes.IRETURN));
            m.instructions.add(lPositive);
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 0));
            m.instructions.add(new LdcInsnNode((long) Integer.MAX_VALUE));
            m.instructions.add(new InsnNode(Opcodes.LCMP));
            m.instructions.add(new JumpInsnNode(Opcodes.IFLT, lExact));
            m.instructions.add(new LdcInsnNode(Integer.MAX_VALUE));
            m.instructions.add(new InsnNode(Opcodes.IRETURN));
            m.instructions.add(lExact);
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 0));
            m.instructions.add(new InsnNode(Opcodes.L2I));
            m.instructions.add(new InsnNode(Opcodes.IRETURN));
            cn.methods.add(m);
            System.out.println("  -> Added saturateTP helper");
        }

        // writeTP(NBTTagCompound,J)V: writes long authority + saturated int mirror
        if (!hasMethodDecl(cn, "writeTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)V")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "writeTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)V", null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new LdcInsnNode("jrmcTplong"));
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 1));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74772_a", "(Ljava/lang/String;J)V", false));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new LdcInsnNode("jrmcTpint"));
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 1));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "saturateTP", "(J)I", false));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V", false));
            m.instructions.add(new InsnNode(Opcodes.RETURN));
            cn.methods.add(m);
            System.out.println("  -> Added writeTP helper (long authority + saturated mirror)");
        }

        // setTPFromMirror(NBTTagCompound,I)V: safe absolute int write
        //   cur <= MAX: exact value (legacy int semantics)
        //   cur >  MAX: value is saturated, apply (value - MAX) as a delta to the long authority
        if (!hasMethodDecl(cn, "setTPFromMirror", "(Lnet/minecraft/nbt/NBTTagCompound;I)V")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "setTPFromMirror", "(Lnet/minecraft/nbt/NBTTagCompound;I)V", null, null);
            LabelNode lSat = new LabelNode();
            LabelNode lStore = new LabelNode();
            LabelNode lWrite = new LabelNode();
            // cur = getTPLong(nbt)
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "getTPLong", "(Lnet/minecraft/nbt/NBTTagCompound;)J", false));
            m.instructions.add(new VarInsnNode(Opcodes.LSTORE, 2));
            // if (cur > MAX) goto saturated
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 2));
            m.instructions.add(new LdcInsnNode((long) Integer.MAX_VALUE));
            m.instructions.add(new InsnNode(Opcodes.LCMP));
            m.instructions.add(new JumpInsnNode(Opcodes.IFGT, lSat));
            // exact: new = value
            m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
            m.instructions.add(new InsnNode(Opcodes.I2L));
            m.instructions.add(new VarInsnNode(Opcodes.LSTORE, 4));
            m.instructions.add(new JumpInsnNode(Opcodes.GOTO, lStore));
            // saturated: new = cur + (value - MAX)
            m.instructions.add(lSat);
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 2));
            m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
            m.instructions.add(new InsnNode(Opcodes.I2L));
            m.instructions.add(new LdcInsnNode((long) Integer.MAX_VALUE));
            m.instructions.add(new InsnNode(Opcodes.LSUB));
            m.instructions.add(new InsnNode(Opcodes.LADD));
            m.instructions.add(new VarInsnNode(Opcodes.LSTORE, 4));
            // clamp floor at 0
            m.instructions.add(lStore);
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 4));
            m.instructions.add(new InsnNode(Opcodes.LCONST_0));
            m.instructions.add(new InsnNode(Opcodes.LCMP));
            m.instructions.add(new JumpInsnNode(Opcodes.IFGE, lWrite));
            m.instructions.add(new InsnNode(Opcodes.LCONST_0));
            m.instructions.add(new VarInsnNode(Opcodes.LSTORE, 4));
            m.instructions.add(lWrite);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 4));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "writeTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)V", false));
            m.instructions.add(new InsnNode(Opcodes.RETURN));
            cn.methods.add(m);
            System.out.println("  -> Added setTPFromMirror helper (never truncates long authority)");
        }

        // adjustTP(NBTTagCompound,J)J: delta operation on the long authority
        if (!hasMethodDecl(cn, "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", null, null);
            LabelNode lWrite = new LabelNode();
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "getTPLong", "(Lnet/minecraft/nbt/NBTTagCompound;)J", false));
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 1));
            m.instructions.add(new InsnNode(Opcodes.LADD));
            m.instructions.add(new VarInsnNode(Opcodes.LSTORE, 3));
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 3));
            m.instructions.add(new InsnNode(Opcodes.LCONST_0));
            m.instructions.add(new InsnNode(Opcodes.LCMP));
            m.instructions.add(new JumpInsnNode(Opcodes.IFGE, lWrite));
            m.instructions.add(new InsnNode(Opcodes.LCONST_0));
            m.instructions.add(new VarInsnNode(Opcodes.LSTORE, 3));
            m.instructions.add(lWrite);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 3));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "writeTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)V", false));
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 3));
            m.instructions.add(new InsnNode(Opcodes.LRETURN));
            cn.methods.add(m);
            System.out.println("  -> Added adjustTP(NBTTagCompound,J)J helper");
        }

        // adjustTP(EntityPlayer,J)J
        if (!hasMethodDecl(cn, "adjustTP", "(Lnet/minecraft/entity/player/EntityPlayer;J)J")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "adjustTP", "(Lnet/minecraft/entity/player/EntityPlayer;J)J", null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new LdcInsnNode("pres"));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "nbt", "(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Lnet/minecraft/nbt/NBTTagCompound;", false));
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 1));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", false));
            m.instructions.add(new InsnNode(Opcodes.LRETURN));
            cn.methods.add(m);
            System.out.println("  -> Added adjustTP(EntityPlayer,J)J helper");
        }

        // Helper to check if a method already exists
        java.util.function.BiPredicate<String, String> hasMethod = (name, desc) -> {
            for (MethodNode m : cn.methods) {
                if (m.name.equals(name) && m.desc.equals(desc)) {
                    return true;
                }
            }
            return false;
        };

        // 5. Add getLong method for direct long access (if not exists)
        if (!hasMethod.test("getLong", "(Lnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)J")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "getLong", "(Lnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)J", null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new LdcInsnNode("pres"));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "nbt", "(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Lnet/minecraft/nbt/NBTTagCompound;", false));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74763_f", "(Ljava/lang/String;)J", false));
            m.instructions.add(new InsnNode(Opcodes.LRETURN));
            cn.methods.add(m);
            System.out.println("  -> Added getLong method");
        } else {
            System.out.println("  -> getLong method already exists, skipping");
        }

        // 6. Add setLong method for direct long writes (if not exists)
        // Signature: (JLnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)V
        // Locals: 0-1=long, 2=EntityPlayer, 3=String
        if (!hasMethod.test("setLong", "(JLnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)V")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "setLong", "(JLnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)V", null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // EntityPlayer
            m.instructions.add(new LdcInsnNode("pres"));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "nbt", "(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Lnet/minecraft/nbt/NBTTagCompound;", false));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3)); // String
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 0)); // long value (locals 0-1)
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74772_a", "(Ljava/lang/String;J)V", false));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // EntityPlayer
            m.instructions.add(new LdcInsnNode("pres"));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "nbt", "(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Lnet/minecraft/nbt/NBTTagCompound;", false));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3)); // String
            // Saturate long to int for mirror: min(value, Integer.MAX_VALUE)
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 0)); // long value
            m.instructions.add(new LdcInsnNode((long) Integer.MAX_VALUE)); // Integer.MAX_VALUE as long
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(JJ)J", false));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Math", "toIntExact", "(J)I", false));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V", false));
            m.instructions.add(new InsnNode(Opcodes.RETURN));
            cn.methods.add(m);
            System.out.println("  -> Added setLong method");
        } else {
            System.out.println("  -> setLong method already exists, skipping");
        }

        // 7. Add/Replace addLong method for delta addition
        // Signature: (JLnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)J
        // Locals:
        //   0-1 = delta (long parameter)
        //   2 = EntityPlayer
        //   3 = String key
        //   4 = NBTTagCompound (player "pres")
        //   5-6 = newValue (long) for return
        //   7 = saturated int (for mirror)
        // Remove existing addLong method if present
        for (int i = 0; i < cn.methods.size(); i++) {
            MethodNode existing = cn.methods.get(i);
            if (existing.name.equals("addLong") && existing.desc.equals("(JLnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)J")) {
                cn.methods.remove(i);
                System.out.println("  -> Removed existing addLong method");
                break;
            }
        }
        { // Block scope for addLong method variable
        MethodNode mAddLong = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "addLong", "(JLnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)J", null, null);
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        mAddLong.instructions.add(start);

        // 1. Get NBTTagCompound for player "pres" -> store in local 4
        mAddLong.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // EntityPlayer
        mAddLong.instructions.add(new LdcInsnNode("pres"));
        mAddLong.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "nbt", "(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Lnet/minecraft/nbt/NBTTagCompound;", false));
        mAddLong.instructions.add(new VarInsnNode(Opcodes.ASTORE, 4)); // local 4 = NBTTagCompound

        // 2. Get current jrmcTplong from NBT: nbt.getLong(key) -> stack: [long]
        mAddLong.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4)); // NBTTagCompound
        mAddLong.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3)); // String key
        mAddLong.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74763_f", "(Ljava/lang/String;)J", false));

        // 3. Add delta (local 0-1): stack = [current_long] + [delta_long] -> LADD -> [new_long]
        mAddLong.instructions.add(new VarInsnNode(Opcodes.LLOAD, 0)); // delta
        mAddLong.instructions.add(new InsnNode(Opcodes.LADD)); // new value on stack [long, long_2nd]

        // 4. DUP2 to keep copy for return, store one copy in local 5-6
        mAddLong.instructions.add(new InsnNode(Opcodes.DUP2)); // [new, new]
        mAddLong.instructions.add(new VarInsnNode(Opcodes.LSTORE, 5)); // local 5-6 = new value, stack = [new]

        // 5. Write new value to NBT (jrmcTplong): nbt.setLong(key, value)
        // Need stack: [NBT, String, long, long_2nd]
        mAddLong.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4)); // NBTTagCompound
        mAddLong.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3)); // String key
        mAddLong.instructions.add(new VarInsnNode(Opcodes.LLOAD, 5)); // long value from local 5-6
        mAddLong.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74772_a", "(Ljava/lang/String;J)V", false));
        // stack: empty

        // 6. Sync int mirror (jrmcTpint): FIRST compute saturated int, THEN get NBT and write
        // Compute saturated int: min(value, Integer.MAX_VALUE) -> toIntExact -> store in local 7
        mAddLong.instructions.add(new VarInsnNode(Opcodes.LLOAD, 5)); // long value
        mAddLong.instructions.add(new LdcInsnNode((long) Integer.MAX_VALUE)); // Integer.MAX_VALUE as long
        mAddLong.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(JJ)J", false));
        mAddLong.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Math", "toIntExact", "(J)I", false));
        mAddLong.instructions.add(new VarInsnNode(Opcodes.ISTORE, 7)); // saturated int in local 7

        // Now get NBT and write the int
        mAddLong.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // EntityPlayer
        mAddLong.instructions.add(new LdcInsnNode("pres"));
        mAddLong.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "nbt", "(Lnet/minecraft/entity/Entity;Ljava/lang/String;)Lnet/minecraft/nbt/NBTTagCompound;", false));
        mAddLong.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3)); // String key
        mAddLong.instructions.add(new VarInsnNode(Opcodes.ILOAD, 7)); // saturated int
        mAddLong.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V", false));
        // stack: empty

        // 7. Return the long value from local 5-6
        mAddLong.instructions.add(new VarInsnNode(Opcodes.LLOAD, 5));
        mAddLong.instructions.add(new InsnNode(Opcodes.LRETURN));

        mAddLong.instructions.add(end);
        mAddLong.localVariables.add(new LocalVariableNode("delta", "J", null, start, end, 0));
        mAddLong.localVariables.add(new LocalVariableNode("player", "Lnet/minecraft/entity/player/EntityPlayer;", null, start, end, 2));
        mAddLong.localVariables.add(new LocalVariableNode("key", "Ljava/lang/String;", null, start, end, 3));
        mAddLong.localVariables.add(new LocalVariableNode("nbt", "Lnet/minecraft/nbt/NBTTagCompound;", null, start, end, 4));
        mAddLong.localVariables.add(new LocalVariableNode("newValue", "J", null, start, end, 5));
        mAddLong.localVariables.add(new LocalVariableNode("saturatedInt", "I", null, start, end, 7));

        cn.methods.add(mAddLong);
        System.out.println("  -> Added addLong method (fixed stack management)");
        }

        // 8. Add helper methods for packet NBT TP handling (to avoid bytecode issues in JRMCorePacHanS)
        // getLongFromNBT: reads a long from any NBTTagCompound
        if (!hasMethod.test("getLongFromNBT", "(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;)J")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "getLongFromNBT", "(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;)J", null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74763_f", "(Ljava/lang/String;)J", false));
            m.instructions.add(new InsnNode(Opcodes.LRETURN));
            cn.methods.add(m);
            System.out.println("  -> Added getLongFromNBT helper");
        } else {
            System.out.println("  -> getLongFromNBT helper already exists, skipping");
        }

        // setLongInNBT: writes a long to any NBTTagCompound
        if (!hasMethod.test("setLongInNBT", "(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;J)V")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "setLongInNBT", "(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;J)V", null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 2));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74772_a", "(Ljava/lang/String;J)V", false));
            m.instructions.add(new InsnNode(Opcodes.RETURN));
            cn.methods.add(m);
            System.out.println("  -> Added setLongInNBT helper");
        } else {
            System.out.println("  -> setLongInNBT helper already exists, skipping");
        }

        // getIntFromNBT: reads an int from any NBTTagCompound
        if (!hasMethod.test("getIntFromNBT", "(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;)I")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "getIntFromNBT", "(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;)I", null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74762_e", "(Ljava/lang/String;)I", false));
            m.instructions.add(new InsnNode(Opcodes.IRETURN));
            cn.methods.add(m);
            System.out.println("  -> Added getIntFromNBT helper");
        } else {
            System.out.println("  -> getIntFromNBT helper already exists, skipping");
        }

        // setIntInNBT: writes an int to any NBTTagCompound
        if (!hasMethod.test("setIntInNBT", "(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;I)V")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "setIntInNBT", "(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;I)V", null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V", false));
            m.instructions.add(new InsnNode(Opcodes.RETURN));
            cn.methods.add(m);
            System.out.println("  -> Added setIntInNBT helper");
        } else {
            System.out.println("  -> setIntInNBT helper already exists, skipping");
        }

        // applyPacketTPChange: handles the complete TP packet logic (reads, computes delta, writes, syncs)
        if (!hasMethod.test("applyPacketTPChange", "(Lnet/minecraft/nbt/NBTTagCompound;Lnet/minecraft/entity/player/EntityPlayer;)V")) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "applyPacketTPChange", "(Lnet/minecraft/nbt/NBTTagCompound;Lnet/minecraft/entity/player/EntityPlayer;)V", null, null);
            // nbt: ALOAD 0, player: ALOAD 1
            // Read jrmcTplong from packet NBT
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new LdcInsnNode("jrmcTplong"));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "getLongFromNBT", "(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;)J", false));
            m.instructions.add(new VarInsnNode(Opcodes.LSTORE, 2)); // store in local 2-3 (long)
            
            // Read jrmcTpint from packet NBT
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new LdcInsnNode("jrmcTpint"));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "getIntFromNBT", "(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;)I", false));
            m.instructions.add(new VarInsnNode(Opcodes.ISTORE, 4)); // store in local 4 (int)
            
            // Compute delta = jrmcTplong - jrmcTpint
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 2));
            m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
            m.instructions.add(new InsnNode(Opcodes.I2L));
            m.instructions.add(new InsnNode(Opcodes.LSUB));
            m.instructions.add(new VarInsnNode(Opcodes.LSTORE, 5)); // store delta in local 5-6 (long)
            
            // Saturate delta to int range
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 5));
            m.instructions.add(new LdcInsnNode((long) Integer.MAX_VALUE));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(JJ)J", false));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Math", "toIntExact", "(J)I", false));
            m.instructions.add(new VarInsnNode(Opcodes.ISTORE, 7)); // saturated delta in local 7 (int)
            
            // Write saturated delta to packet NBT jrmcTpint
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new LdcInsnNode("jrmcTpint"));
            m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 7));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "setIntInNBT", "(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;I)V", false));
            
            // Sync to player: add delta to player's jrmcTplong
            m.instructions.add(new VarInsnNode(Opcodes.LLOAD, 5)); // original long delta
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            m.instructions.add(new LdcInsnNode("jrmcTplong"));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "addLong", "(JLnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)J", false));
            m.instructions.add(new InsnNode(Opcodes.POP2)); // discard return value
            
            m.instructions.add(new InsnNode(Opcodes.RETURN));
            cn.methods.add(m);
            System.out.println("  -> Added applyPacketTPChange helper");
        }

        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    static boolean hasMethodDecl(ClassNode cn, String name, String desc) {
        for (MethodNode m : cn.methods) {
            if (m.name.equals(name) && m.desc.equals(desc)) return true;
        }
        return false;
    }

    // ============================================================
    // ComJrmctp - /jrmctp writes go to the long authority.
    // Only the two final mirror writes are replaced; messages and locals stay intact.
    // ============================================================
    static byte[] patchComJrmctp(byte[] data) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(data).accept(cn, 0);
        int changes = 0;

        for (MethodNode m : cn.methods) {
            if (!m.name.equals("func_71515_b")) continue;
            AbstractInsnNode i = m.instructions.getFirst();
            while (i != null) {
                AbstractInsnNode next = i.getNext();
                if (!isInvoke(i, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V")) { i = next; continue; }
                AbstractInsnNode n2 = prev(i);
                AbstractInsnNode n1 = prev(n2);
                AbstractInsnNode n0 = prev(n1);
                if (isLdc(n1, "jrmcTpint") && isVar(n0, Opcodes.ALOAD, 12) && n2 != null && n2.getOpcode() == Opcodes.ILOAD) {
                    int local = ((VarInsnNode) n2).var;
                    if (local == 14) {
                        // sub path: adjustTP(nbt, -amount)
                        replace(m, n0, i,
                                new VarInsnNode(Opcodes.ALOAD, 12),
                                new VarInsnNode(Opcodes.LLOAD, 6),
                                new InsnNode(Opcodes.LNEG),
                                new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", false),
                                new InsnNode(Opcodes.POP2));
                        System.out.println("     ComJrmctp: subtract routed through long authority");
                        changes++;
                    } else if (local == 16) {
                        // add path: adjustTP(nbt, +amount)
                        replace(m, n0, i,
                                new VarInsnNode(Opcodes.ALOAD, 12),
                                new VarInsnNode(Opcodes.LLOAD, 6),
                                new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", false),
                                new InsnNode(Opcodes.POP2));
                        System.out.println("     ComJrmctp: add routed through long authority");
                        changes++;
                    }
                }
                i = next;
            }
        }

        System.out.println("  -> ComJrmctp: " + changes + " TP write site(s) routed through long authority");
        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    // ============================================================
    // ComJrmcRacialSkill - racial skill cost write on long authority
    // ============================================================
    static byte[] patchComJrmcRacialSkill(byte[] data) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(data).accept(cn, 0);
        int changes = 0;

        for (MethodNode m : cn.methods) {
            if (!m.name.equals("func_71515_b")) continue;
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (!isInvoke(i, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V")) continue;
                AbstractInsnNode p6 = prev(i);   // ISUB
                AbstractInsnNode p5 = prev(p6);  // GETFIELD tp_cost
                AbstractInsnNode p4 = prev(p5);  // ALOAD 0
                AbstractInsnNode p3 = prev(p4);  // GETFIELD tp_current
                AbstractInsnNode p2 = prev(p3);  // ALOAD 0
                AbstractInsnNode p1 = prev(p2);  // LDC jrmcTpint
                AbstractInsnNode p0 = prev(p1);  // ALOAD 8
                if (isLdc(p1, "jrmcTpint") && isVar(p0, Opcodes.ALOAD, 8)
                        && p6 != null && p6.getOpcode() == Opcodes.ISUB
                        && p5 instanceof FieldInsnNode && ((FieldInsnNode) p5).name.equals("tp_cost")
                        && p3 instanceof FieldInsnNode && ((FieldInsnNode) p3).name.equals("tp_current")) {
                    replace(m, p0, i,
                            new VarInsnNode(Opcodes.ALOAD, 8),
                            new VarInsnNode(Opcodes.ALOAD, 0),
                            new FieldInsnNode(Opcodes.GETFIELD, "JinRyuu/JRMCore/ComJrmcRacialSkill", "tp_cost", "I"),
                            new InsnNode(Opcodes.I2L),
                            new InsnNode(Opcodes.LNEG),
                            new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", false),
                            new InsnNode(Opcodes.POP2));
                    System.out.println("     ComJrmcRacialSkill: TP cost routed through long authority");
                    changes++;
                }
            }
        }

        System.out.println("  -> ComJrmcRacialSkill: " + changes + " TP write site(s) routed through long authority");
        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    // ============================================================
    // JRMCoreComTickH - SKIP patching to avoid Mixin frame computation issues
    // Only the updatePlayersData TP gain write is replaced (long authority + mirror).
    // Locals and StackMapTable shapes are untouched.
    // ============================================================
    static byte[] patchJRMCoreComTickH(byte[] data) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(data).accept(cn, 0);
        int changes = 0;

        for (MethodNode m : cn.methods) {
            if (!m.name.equals("updatePlayersData")) continue;
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (!isInvoke(i, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V")) continue;
                AbstractInsnNode n2 = prev(i);
                AbstractInsnNode n1 = prev(n2);
                AbstractInsnNode n0 = prev(n1);
                if (isLdc(n1, "jrmcTpint") && isVar(n0, Opcodes.ALOAD, 5)
                        && isVar(n2, Opcodes.ILOAD, 44)) {
                    replace(m, n0, i,
                            new VarInsnNode(Opcodes.ALOAD, 5),
                            new VarInsnNode(Opcodes.ILOAD, 42),
                            new InsnNode(Opcodes.I2L),
                            new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", false),
                            new InsnNode(Opcodes.POP2));
                    System.out.println("     updatePlayersData: TP gain routed through long authority");
                    changes++;
                }
            }
        }

        System.out.println("  -> JRMCoreComTickH: " + changes + " TP write site(s) routed through long authority");
        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    static AbstractInsnNode prev(AbstractInsnNode i) {
        if (i == null) return null;
        i = i.getPrevious();
        while (i != null && (i instanceof LabelNode || i instanceof LineNumberNode || i instanceof FrameNode)) i = i.getPrevious();
        return i;
    }

    static boolean isVar(AbstractInsnNode i, int op, int var) {
        return i != null && i.getOpcode() == op && i instanceof VarInsnNode && ((VarInsnNode) i).var == var;
    }

    static boolean isLdc(AbstractInsnNode i, Object cst) {
        return i != null && i instanceof LdcInsnNode && java.util.Objects.equals(((LdcInsnNode) i).cst, cst);
    }

    static boolean isInsn(AbstractInsnNode i, int op) {
        return i != null && i.getOpcode() == op;
    }

    static boolean isInvoke(AbstractInsnNode i, String owner, String name, String desc) {
        return i != null && i instanceof MethodInsnNode && ((MethodInsnNode) i).owner.equals(owner)
                && ((MethodInsnNode) i).name.equals(name) && ((MethodInsnNode) i).desc.equals(desc);
    }

    static void replace(MethodNode m, AbstractInsnNode from, AbstractInsnNode to, AbstractInsnNode... insns) {
        if (from == null || to == null) return;
        for (AbstractInsnNode insn : insns) {
            m.instructions.insertBefore(from, insn);
        }
        AbstractInsnNode n = from;
        while (n != null) {
            AbstractInsnNode next = n.getNext();
            m.instructions.remove(n);
            if (n == to) break;
            n = next;
        }
    }

    // ============================================================
    // JRMCorePacHanS - targeted TP write sites only.
    // Mixin-safe rule: no locals added/removed, stack is empty before and after
    // each replaced write, COMPUTE_MAXS only (StackMapTable labels untouched).
    // ============================================================
    static byte[] patchJRMCorePacHanS(byte[] data) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(data).accept(cn, 0);
        int changes = 0;

        for (MethodNode m : cn.methods) {
            if (m.name.equals("handleUpgrade")) {
                changes += patchHandleUpgrade(m);
            } else if (m.name.equals("handleChar")) {
                changes += patchHandleChar(m);
            } else if (m.name.equals("handleData")) {
                changes += patchHandleData(m);
            } else if (m.name.equals("handleTech")) {
                changes += patchHandleTechTP(m);
            } else if (m.name.equals("handleQuad")) {
                changes += patchHandleQuad(m);
            } else if (m.name.equals("handleStats3")) {
                changes += patchHandleStats3(m);
            }
        }

        System.out.println("  -> JRMCorePacHanS: " + changes + " TP write site(s) routed through long authority");
        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    // handleUpgrade: nbt.setInteger("jrmcTpint", nbt.getInteger("jrmcTpint") - cost)
    //            -> adjustTP(nbt, -cost)
    static int patchHandleUpgrade(MethodNode m) {
        int changes = 0;

        // Affordability check must read the long authority (saturated), not the raw mirror.
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (!isInvoke(i, "net/minecraft/nbt/NBTTagCompound", "func_74762_e", "(Ljava/lang/String;)I")) continue;
            AbstractInsnNode n0 = prev(i);
            AbstractInsnNode n1 = prev(n0);
            AbstractInsnNode n2 = i.getNext();
            AbstractInsnNode n3 = n2 == null ? null : n2.getNext();
            if (isLdc(n0, "jrmcTpint") && isVar(n1, Opcodes.ALOAD, 3)
                    && isVar(n2, Opcodes.ILOAD, 17) && n3 != null && n3.getOpcode() == Opcodes.IF_ICMPLT) {
                replace(m, n1, i,
                        new VarInsnNode(Opcodes.ALOAD, 3),
                        new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "getTPLong", "(Lnet/minecraft/nbt/NBTTagCompound;)J", false),
                        new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "saturateTP", "(J)I", false));
                System.out.println("     handleUpgrade: affordability check reads long authority");
                changes++;
                break;
            }
        }

        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (!isInvoke(i, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V")) continue;
            AbstractInsnNode n6 = prev(i);
            AbstractInsnNode n5 = prev(n6);
            AbstractInsnNode n4 = prev(n5);
            AbstractInsnNode n3 = prev(n4);
            AbstractInsnNode n2 = prev(n3);
            AbstractInsnNode n1 = prev(n2);
            AbstractInsnNode n0 = prev(n1);
            if (isLdc(n1, "jrmcTpint") && isLdc(n3, "jrmcTpint")
                    && isInvoke(n4, "net/minecraft/nbt/NBTTagCompound", "func_74762_e", "(Ljava/lang/String;)I")
                    && isVar(n5, Opcodes.ILOAD, 17) && n6 != null && n6.getOpcode() == Opcodes.ISUB
                    && isVar(n2, Opcodes.ALOAD, 3) && isVar(n0, Opcodes.ALOAD, 3)) {
                replace(m, n0, i,
                        new VarInsnNode(Opcodes.ALOAD, 3),
                        new VarInsnNode(Opcodes.ILOAD, 17),
                        new InsnNode(Opcodes.I2L),
                        new InsnNode(Opcodes.LNEG),
                        new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", false),
                        new InsnNode(Opcodes.POP2));
                System.out.println("     handleUpgrade: TP discount now on long authority (-cost)");
                changes++;
                break;
            }
        }
        if (changes < 2) System.out.println("     WARN handleUpgrade: expected 2 TP sites, patched " + changes);
        return changes;
    }

    // handleChar: nbt.setInteger("jrmcTpint", localInt - 20) -> adjustTP(nbt, -20)
    static int patchHandleChar(MethodNode m) {
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (!isInvoke(i, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V")) continue;
            AbstractInsnNode n4 = prev(i);
            AbstractInsnNode n3 = prev(n4);
            AbstractInsnNode n2 = prev(n3);
            AbstractInsnNode n1 = prev(n2);
            AbstractInsnNode n0 = prev(n1);
            if (isLdc(n1, "jrmcTpint") && isVar(n2, Opcodes.ILOAD, 11)
                    && isInsn(n3, Opcodes.BIPUSH) && n4 != null && n4.getOpcode() == Opcodes.ISUB
                    && isVar(n0, Opcodes.ALOAD, 4)) {
                replace(m, n0, i,
                        new VarInsnNode(Opcodes.ALOAD, 4),
                        new LdcInsnNode(-20L),
                        new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", false),
                        new InsnNode(Opcodes.POP2));
                System.out.println("     handleChar: TP cost now on long authority (-20)");
                return 1;
            }
        }
        System.out.println("     WARN handleChar: TP cost pattern not found");
        return 0;
    }

    // handleData: nbt.setInteger("jrmcTpint", clampedValue) -> setTPFromMirror(nbt, clampedValue)
    static int patchHandleData(MethodNode m) {
        int changes = 0;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (!isInvoke(i, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V")) continue;
            AbstractInsnNode n2 = prev(i);
            AbstractInsnNode n1 = prev(n2);
            AbstractInsnNode n0 = prev(n1);
            if (isLdc(n1, "jrmcTpint") && isVar(n0, Opcodes.ALOAD, 6)
                    && isVar(n2, Opcodes.ILOAD, 22)) {
                replace(m, n0, i,
                        new VarInsnNode(Opcodes.ALOAD, 6),
                        new VarInsnNode(Opcodes.ILOAD, 22),
                        new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "setTPFromMirror", "(Lnet/minecraft/nbt/NBTTagCompound;I)V", false));
                System.out.println("     handleData: TP grant routed through long authority");
                changes++;
            }
        }
        return changes;
    }

    // handleTech: setInt(cur +/- delta) -> adjustTP(nbt, +/- delta)
    static int patchHandleTechTP(MethodNode m) {
        int changes = 0;
        AbstractInsnNode i = m.instructions.getFirst();
        while (i != null) {
            AbstractInsnNode next = i.getNext();
            if (isInvoke(i, "JinRyuu/JRMCore/JRMCoreH", "setInt", "(ILnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)V")) {
                AbstractInsnNode p1 = prev(i);   // LDC jrmcTpint
                AbstractInsnNode p2 = prev(p1); // ALOAD nbt
                AbstractInsnNode p3 = prev(p2); // IADD / ISUB
                AbstractInsnNode p4 = prev(p3); // ILOAD value
                AbstractInsnNode p5 = prev(p4); // ILOAD current
                if (isLdc(p1, "jrmcTpint") && p2 instanceof VarInsnNode && p3 != null
                        && (p3.getOpcode() == Opcodes.IADD || p3.getOpcode() == Opcodes.ISUB)
                        && p4 instanceof VarInsnNode && p4.getOpcode() == Opcodes.ILOAD
                        && p5 instanceof VarInsnNode && p5.getOpcode() == Opcodes.ILOAD) {
                    int nbtVar = ((VarInsnNode) p2).var;
                    int valueVar = ((VarInsnNode) p4).var;
                    boolean negate = p3.getOpcode() == Opcodes.ISUB;
                    java.util.List<AbstractInsnNode> repl = new java.util.ArrayList<>();
                    repl.add(new VarInsnNode(Opcodes.ALOAD, nbtVar));
                    repl.add(new VarInsnNode(Opcodes.ILOAD, valueVar));
                    repl.add(new InsnNode(Opcodes.I2L));
                    if (negate) repl.add(new InsnNode(Opcodes.LNEG));
                    repl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", false));
                    repl.add(new InsnNode(Opcodes.POP2));
                    replace(m, p5, i, repl.toArray(new AbstractInsnNode[0]));
                    System.out.println("     handleTech: TP " + (negate ? "-" : "+") + " delta routed through long authority");
                    changes++;
                }
            }
            i = next;
        }
        return changes;
    }

    // handleQuad:
    //   cost sites: nbt.setInteger("jrmcTpint", local8 - 1) -> adjustTP(nbt, -1)
    //   grant sites: nbt.setInteger("jrmcTpint", min(local8 + local14, MAX)) -> adjustTP(nbt, +local14)
    static int patchHandleQuad(MethodNode m) {
        int changes = 0;
        AbstractInsnNode i = m.instructions.getFirst();
        while (i != null) {
            AbstractInsnNode next = i.getNext();
            if (isInvoke(i, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V")) {
                AbstractInsnNode n2 = prev(i);
                AbstractInsnNode n1 = prev(n2);
                AbstractInsnNode n0 = prev(n1);
                if (isVar(n2, Opcodes.ILOAD, 15)) {
                    // grant: ALOAD 7; LDC jrmcTpint; ILOAD 15; setInt
                    if (isLdc(n1, "jrmcTpint") && isVar(n0, Opcodes.ALOAD, 7)) {
                        replace(m, n0, i,
                                new VarInsnNode(Opcodes.ALOAD, 7),
                                new VarInsnNode(Opcodes.ILOAD, 14),
                                new InsnNode(Opcodes.I2L),
                                new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", false),
                                new InsnNode(Opcodes.POP2));
                        System.out.println("     handleQuad: TP reward routed through long authority (+local14)");
                        changes++;
                    }
                } else if (n2 != null && n2.getOpcode() == Opcodes.ISUB) {
                    // cost: ALOAD 7; LDC jrmcTpint; ILOAD 8; ICONST_1; ISUB; setInt
                    AbstractInsnNode c1 = prev(n1); // ILOAD 8
                    AbstractInsnNode c2 = prev(c1); // LDC jrmcTpint
                    AbstractInsnNode c3 = prev(c2); // ALOAD 7
                    if (isInsn(n1, Opcodes.ICONST_1) && isVar(c1, Opcodes.ILOAD, 8)
                            && isLdc(c2, "jrmcTpint") && isVar(c3, Opcodes.ALOAD, 7)) {
                        replace(m, c3, i,
                                new VarInsnNode(Opcodes.ALOAD, 7),
                                new LdcInsnNode(-1L),
                                new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", false),
                                new InsnNode(Opcodes.POP2));
                        System.out.println("     handleQuad: TP cost routed through long authority (-1)");
                        changes++;
                    }
                }
            }
            i = next;
        }
        if (changes < 5) System.out.println("     WARN handleQuad: expected 5 TP sites, patched " + changes);
        return changes;
    }

    // handleStats3:
    //   local42 = local31 - local32  (local32 = skill TP cost)
    //   local38 - local37            (local37 = skill TP cost)
    // Both -> adjustTP(nbt, -cost) on the long authority.
    static int patchHandleStats3(MethodNode m) {
        int changes = 0;
        AbstractInsnNode i = m.instructions.getFirst();
        while (i != null) {
            AbstractInsnNode next = i.getNext();
            if (isInvoke(i, "net/minecraft/nbt/NBTTagCompound", "func_74768_a", "(Ljava/lang/String;I)V")) {
                AbstractInsnNode n2 = prev(i);
                AbstractInsnNode n1 = prev(n2);
                AbstractInsnNode n0 = prev(n1);
                if (isVar(n2, Opcodes.ILOAD, 42)) {
                    // ALOAD 5; LDC jrmcTpint; ILOAD 42; setInt
                    if (isLdc(n1, "jrmcTpint") && isVar(n0, Opcodes.ALOAD, 5)) {
                        replace(m, n0, i,
                                new VarInsnNode(Opcodes.ALOAD, 5),
                                new VarInsnNode(Opcodes.ILOAD, 32),
                                new InsnNode(Opcodes.I2L),
                                new InsnNode(Opcodes.LNEG),
                                new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", false),
                                new InsnNode(Opcodes.POP2));
                        System.out.println("     handleStats3: TP cost routed through long authority (-local32)");
                        changes++;
                    }
                } else if (n2 != null && n2.getOpcode() == Opcodes.ISUB) {
                    // ALOAD 5; LDC jrmcTpint; ILOAD 38; ILOAD 37; ISUB; setInt
                    AbstractInsnNode c1 = prev(n1); // ILOAD 38
                    AbstractInsnNode c2 = prev(c1); // LDC jrmcTpint
                    AbstractInsnNode c3 = prev(c2); // ALOAD 5
                    if (isVar(n1, Opcodes.ILOAD, 37) && isVar(c1, Opcodes.ILOAD, 38)
                            && isLdc(c2, "jrmcTpint") && isVar(c3, Opcodes.ALOAD, 5)) {
                        replace(m, c3, i,
                                new VarInsnNode(Opcodes.ALOAD, 5),
                                new VarInsnNode(Opcodes.ILOAD, 37),
                                new InsnNode(Opcodes.I2L),
                                new InsnNode(Opcodes.LNEG),
                                new MethodInsnNode(Opcodes.INVOKESTATIC, "JinRyuu/JRMCore/JRMCoreH", "adjustTP", "(Lnet/minecraft/nbt/NBTTagCompound;J)J", false),
                                new InsnNode(Opcodes.POP2));
                        System.out.println("     handleStats3: TP cost routed through long authority (-local37)");
                        changes++;
                    }
                }
            }
            i = next;
        }
        if (changes < 2) System.out.println("     WARN handleStats3: expected 2 TP sites, patched " + changes);
        return changes;
    }
    // curTP in this packet mirrors the int display value (JRMCoreH.curTP).
    // The 64-bit authority lives in player NBT (jrmcTplong, handled in JRMCoreH).
    // Long-izing this DTO would require widening Handler, PacHanC and the
    // JRMCoreH.curTP static field, which are shared with Mixin/UI code.
    // Keeping it int preserves the original wire format and avoids VerifyError.
    // ============================================================
    static byte[] patchJRMCorePStats2(byte[] data) throws Exception {
        System.out.println("  -> JRMCorePStats2: SKIPPED (int-native DTO, long authority is jrmcTplong)");
        return data;
    }


    // ============================================================
    // JRMCorePCost - Change cost from short to int
    // ============================================================
    static byte[] patchJRMCorePCost(byte[] data) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(data).accept(cn, 0);

        int changes = 0;

        // Change fields from short to int
        for (FieldNode f : cn.fields) {
            if (f.desc.equals("S") && (f.name.contains("cost") || f.name.contains("Cost") || f.name.contains("tp"))) {
                f.desc = "I";
                changes++;
                System.out.println("  -> JRMCorePCost field " + f.name + ": short -> int");
            }
        }

        // Change readShort/writeShort to readInt/writeInt in toBytes/fromBytes
        for (MethodNode m : cn.methods) {
            if (m.name.equals("toBytes") || m.name.equals("fromBytes")) {
                AbstractInsnNode insn = m.instructions.getFirst();
                while (insn != null) {
                    if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode min = (MethodInsnNode) insn;
                        if (min.owner.equals("io/netty/buffer/ByteBuf")) {
                            if (min.name.equals("writeShort") && min.desc.equals("(S)Lio/netty/buffer/ByteBuf;")) {
                                min.name = "writeInt";
                                min.desc = "(I)Lio/netty/buffer/ByteBuf;";
                                changes++;
                                System.out.println("  -> JRMCorePCost." + m.name + ": writeShort -> writeInt");
                            } else if (min.name.equals("readShort") && min.desc.equals("()S")) {
                                min.name = "readInt";
                                min.desc = "()I";
                                changes++;
                                System.out.println("  -> JRMCorePCost." + m.name + ": readShort -> readInt");
                            }
                        }
                    }
                    insn = insn.getNext();
                }
            }
        }

        if (changes == 0) {
            System.out.println("  -> JRMCorePCost: No short fields found to patch (may already be int)");
        } else {
            System.out.println("  -> JRMCorePCost total: " + changes + " changes");
        }

        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    // ============================================================
    // JRMCorePacHanC - Change handleStats2 to read long curTP
    // ============================================================
    static byte[] patchJRMCorePacHanC(byte[] data) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(data).accept(cn, 0);

        for (MethodNode m : cn.methods) {
            if (m.name.equals("handleStats2") && m.desc.equals("(III[ILnet/minecraft/entity/player/EntityPlayer;)V")) {
                m.desc = "(JII[ILnet/minecraft/entity/player/EntityPlayer;)V";

                AbstractInsnNode insn = m.instructions.getFirst();
                while (insn != null) {
                    if (insn.getOpcode() == Opcodes.PUTSTATIC) {
                        FieldInsnNode fin = (FieldInsnNode) insn;
                        if (fin.name.equals("curTP") && fin.owner.equals("JinRyuu/JRMCore/JRMCoreH")) {
                            fin.desc = "J";
                            AbstractInsnNode prev = insn.getPrevious();
                            if (prev != null && prev.getOpcode() == Opcodes.ILOAD) {
                                ((VarInsnNode) prev).setOpcode(Opcodes.LLOAD);
                            }
                            System.out.println("  -> JRMCorePacHanC.handleStats2: curTP int -> long");
                        }
                    }
                    insn = insn.getNext();
                }
            }
        }

        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }
}

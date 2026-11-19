import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Auditoria completa de referencias a Training Points (TP).
 * 
 * Extende TpSiteScan com cobertura total de todos os padroes de gameplay,
 * persistencia, compatibilidade e interface.
 * 
 * Uso: java FullTpAudit <jar1> <jar2> <jar3> <saida.csv>
 * 
 * Saida (CSV, uma linha por site, ordenada):
 *   jar,classe,metodo,descritor,offset,chave,operacao,descritor_operacao,contexto,categoria,origem,destino
 * 
 * Categorias:
 *   GAMEPLAY      - logica de ganho/gasto/verificacao de saldo que afeta o jogo
 *   PERSISTENCE   - leitura/escrita de NBT (save/load)
 *   NETWORK       - envio/recebimento de pacotes
 *   COMPATIBILITY - APIs INT legadas usadas por outros mods
 *   DISPLAY       - interface/exibicao (saturado)
 *   UNKNOWN       - nao classificado
 * 
 * Origem/Destino:
 *   LONG_AUTHORITATIVE - valor vem/vai para jrmcTplong/curTPLong/getTP/gain/spend
 *   INT_MIRROR         - valor vem/vai para jrmcTpint/curTP/getTPInt/getPlayerInt/setPlayerInt
 *   MIXED              - ambos envolvidos
 *   CONSTANT           - valor constante/literal
 *   COMPUTED           - calculado a partir de outras variaveis
 */
public final class FullTpAudit {

    // Nomes internos das classes principais
    private static final String NBT = "net/minecraft/nbt/NBTTagCompound";
    private static final String NBT_BASE = "net/minecraft/nbt/NBTBase";
    private static final String JRMCOREH = "JinRyuu/JRMCore/JRMCoreH";
    private static final String TPUTIL = "JinRyuu/JRMCore/TpUtil";
    private static final String TPLOGIC = "JinRyuu/JRMCore/TpLogic";
    private static final String TPNBTCORE = "JinRyuu/JRMCore/TpNbtCore";
    private static final String TPCLIENTSTATE = "JinRyuu/JRMCore/TpClientState";
    private static final String TPNETWORK = "JinRyuu/JRMCore/TpNetwork";
    private static final String ENTITY_PLAYER = "net/minecraft/entity/player/EntityPlayer";
    private static final String ENTITY_PLAYER_MP = "net/minecraft/entity/player/EntityPlayerMP";

    // Chaves de TP conhecidas
    private static final String KEY_LONG = "jrmcTplong";
    private static final String KEY_INT = "jrmcTpint";

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Uso: FullTpAudit <jar1> <jar2> <jar3> <saida.csv>");
            System.exit(2);
        }
        
        List<Row> allRows = new ArrayList<>();
        
        for (int jarIdx = 0; jarIdx < 3; jarIdx++) {
            String jarPath = args[jarIdx];
            String jarName = new File(jarPath).getName();
            
            List<Row> rows = new ArrayList<>();
            ZipFile zip = new ZipFile(jarPath);
            try {
                List<String> names = new ArrayList<>();
                for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                    ZipEntry en = e.nextElement();
                    if (!en.isDirectory() && en.getName().endsWith(".class")) {
                        names.add(en.getName());
                    }
                }
                Collections.sort(names);
                for (String name : names) {
                    ClassNode cn = new ClassNode();
                    new ClassReader(zip.getInputStream(zip.getEntry(name))).accept(cn, 0);
                    for (MethodNode mn : cn.methods) {
                        scanMethod(rows, cn.name, mn, jarName);
                    }
                }
            } finally {
                zip.close();
            }
            allRows.addAll(rows);
            System.out.println(jarName + ": " + rows.size() + " sites");
        }

        // Ordenacao estavel: jar, classe, metodo, descritor, offset
        Collections.sort(allRows, new Comparator<Row>() {
            @Override
            public int compare(Row a, Row b) {
                int c = a.jar.compareTo(b.jar);
                if (c != 0) return c;
                c = a.clazz.compareTo(b.clazz);
                if (c != 0) return c;
                c = a.method.compareTo(b.method);
                if (c != 0) return c;
                c = a.desc.compareTo(b.desc);
                if (c != 0) return c;
                return Integer.compare(a.offset, b.offset);
            }
        });

        Writer w = new OutputStreamWriter(new FileOutputStream(args[3]), "UTF-8");
        try {
            w.write("jar,classe,metodo,descritor,offset,chave,operacao,descritor_operacao,contexto,categoria,origem,destino\n");
            for (Row r : allRows) {
                w.write(csv(r.jar) + "," + csv(r.clazz) + "," + csv(r.method) + "," + csv(r.desc) + ","
                        + r.offset + "," + csv(r.key) + "," + csv(r.op) + ","
                        + csv(r.opDesc) + "," + csv(r.context) + "," + csv(r.category) + ","
                        + csv(r.origin) + "," + csv(r.destination) + "\n");
            }
        } finally {
            w.close();
        }
        System.out.println("Total: " + allRows.size() + " sites -> " + args[3]);
    }

    private static void scanMethod(List<Row> rows, String clazz, MethodNode mn, String jarName) {
        AbstractInsnNode[] insns = mn.instructions.toArray();
        for (int i = 0; i < insns.length; i++) {
            AbstractInsnNode insn = insns[i];
            
            // 1. Field access: curTP, curTPLong
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode fi = (FieldInsnNode) insn;
                if (fi.owner.equals(JRMCOREH)) {
                    if ("curTP".equals(fi.name) || "curTPLong".equals(fi.name)) {
                        String op = (fi.getOpcode() == Opcodes.GETFIELD || fi.getOpcode() == Opcodes.GETSTATIC)
                                ? "field-read" : "field-write";
                        String category = classifyFieldAccess(fi.name, fi.getOpcode());
                        String origin = fi.name.equals("curTPLong") ? "LONG_AUTHORITATIVE" : "INT_MIRROR";
                        String dest = fi.name.equals("curTPLong") ? "LONG_AUTHORITATIVE" : "INT_MIRROR";
                        rows.add(new Row(jarName, clazz, mn.name, mn.desc, i, fi.name,
                                op + "(" + fi.name + ":" + fi.desc + ")", fi.desc,
                                context(insns, i), category, origin, dest));
                    }
                }
                continue;
            }
            
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode mi = (MethodInsnNode) insn;
            
            // 2. NBT operations with TP keys
            String nbtOp = nbtOperation(mi);
            if (nbtOp != null) {
                String key = keyBefore(insns, i);
                if (key != null && (key.equals(KEY_LONG) || key.equals(KEY_INT))) {
                    String category = classifyNbtOperation(nbtOp, key);
                    String origin = key.equals(KEY_LONG) ? "LONG_AUTHORITATIVE" : "INT_MIRROR";
                    String dest = key.equals(KEY_LONG) ? "LONG_AUTHORITATIVE" : "INT_MIRROR";
                    rows.add(new Row(jarName, clazz, mn.name, mn.desc, i, key,
                            nbtOp + "(" + mi.name + ")", mi.desc,
                            context(insns, i), category, origin, dest));
                }
                continue;
            }
            
            // 3. JRMCoreH helper methods
            String jrmcOp = jrmcHelperOperation(mi);
            if (jrmcOp != null) {
                String key = keyBefore(insns, i);
                // For helper methods, key might be on stack or implicit
                String category = classifyJrmcHelper(jrmcOp, mi.name);
                String origin = inferOriginFromJrmcHelper(mi.name, insns, i);
                String dest = inferDestFromJrmcHelper(mi.name, insns, i);
                rows.add(new Row(jarName, clazz, mn.name, mn.desc, i, key != null ? key : "",
                        jrmcOp + "(" + mi.name + ")", mi.desc,
                        context(insns, i), category, origin, dest));
                continue;
            }
            
            // 4. TpUtil methods (authoritative LONG API)
            String tpUtilOp = tpUtilOperation(mi);
            if (tpUtilOp != null) {
                String category = classifyTpUtil(tpUtilOp, mi.name);
                String origin = "LONG_AUTHORITATIVE";
                String dest = "LONG_AUTHORITATIVE";
                if (mi.name.contains("Int") || mi.name.contains("mirror") || mi.name.contains("clamp")) {
                    origin = "INT_MIRROR";
                    dest = "INT_MIRROR";
                }
                rows.add(new Row(jarName, clazz, mn.name, mn.desc, i, "",
                        tpUtilOp + "(" + mi.name + ")", mi.desc,
                        context(insns, i), category, origin, dest));
                continue;
            }
            
            // 5. TpLogic methods (pure logic)
            String tpLogicOp = tpLogicOperation(mi);
            if (tpLogicOp != null) {
                String category = classifyTpLogic(tpLogicOp, mi.name);
                rows.add(new Row(jarName, clazz, mn.name, mn.desc, i, "",
                        tpLogicOp + "(" + mi.name + ")", mi.desc,
                        context(insns, i), category, "LONG_AUTHORITATIVE", "LONG_AUTHORITATIVE"));
                continue;
            }
            
            // 6. TP-related string constants (LDC of jrmcTp*)
            if (insn instanceof LdcInsnNode) {
                Object cst = ((LdcInsnNode) insn).cst;
                if (cst instanceof String && ((String) cst).startsWith("jrmcTp")) {
                    // This is captured by keyBefore for nearby calls, but record standalone too
                    String category = "CONSTANT";
                    rows.add(new Row(jarName, clazz, mn.name, mn.desc, i, (String) cst,
                            "ldc-string", "Ljava/lang/String;",
                            context(insns, i), category, "CONSTANT", "CONSTANT"));
                }
            }
        }
    }

    // =========================================================================
    // Classification helpers
    // =========================================================================

    private static String classifyFieldAccess(String fieldName, int opcode) {
        boolean isRead = (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC);
        if (fieldName.equals("curTPLong")) {
            return isRead ? "GAMEPLAY" : "PERSISTENCE"; // curTPLong write usually from network/apply
        }
        if (fieldName.equals("curTP")) {
            return isRead ? "DISPLAY" : "COMPATIBILITY"; // curTP is INT mirror for display/compat
        }
        return "UNKNOWN";
    }

    private static String classifyNbtOperation(String op, String key) {
        boolean isLong = key.equals(KEY_LONG);
        if (op.startsWith("nbt-read")) {
            return isLong ? "PERSISTENCE" : "COMPATIBILITY";
        }
        if (op.startsWith("nbt-write")) {
            return isLong ? "PERSISTENCE" : "COMPATIBILITY";
        }
        if (op.equals("nbt-get-tag") || op.equals("nbt-has-key")) {
            return isLong ? "PERSISTENCE" : "COMPATIBILITY";
        }
        return "PERSISTENCE";
    }

    private static String classifyJrmcHelper(String op, String methodName) {
        if (methodName.equals("nbt")) return "PERSISTENCE";
        if (methodName.equals("tpShort") || methodName.equals("clampTP")) return "DISPLAY";
        if (methodName.equals("getMaxTP")) return "GAMEPLAY"; // This is a gameplay limit
        if (methodName.equals("getInt") || methodName.equals("setInt")
                || methodName.equals("getLong") || methodName.equals("setLong")
                || methodName.equals("getByte") || methodName.equals("setByte")) {
            return "COMPATIBILITY";
        }
        return "UNKNOWN";
    }

    private static String inferOriginFromJrmcHelper(String methodName, AbstractInsnNode[] insns, int i) {
        // Look at what's on stack before the call
        String ctx = context(insns, i).toLowerCase();
        if (methodName.equals("getLong") || methodName.equals("getInt")) {
            // These read from NBT, origin depends on key
            for (int j = i - 1; j >= 0 && j >= i - 8; j--) {
                if (insns[j] instanceof LdcInsnNode) {
                    Object cst = ((LdcInsnNode) insns[j]).cst;
                    if (cst instanceof String) {
                        String s = (String) cst;
                        if (s.equals(KEY_LONG)) return "LONG_AUTHORITATIVE";
                        if (s.equals(KEY_INT)) return "INT_MIRROR";
                    }
                }
            }
        }
        if (methodName.equals("setLong") || methodName.equals("setInt")) {
            // These write to NBT, look at value being pushed
            if (ctx.contains("lconst") || ctx.contains("lpush") || ctx.contains("l2i") || ctx.contains("i2l")) {
                return ctx.contains("lconst") || ctx.contains("lpush") || ctx.contains("i2l") ? "LONG_AUTHORITATIVE" : "MIXED";
            }
        }
        if (methodName.equals("clampTP") || methodName.equals("tpShort")) {
            return "LONG_AUTHORITATIVE"; // These take long and saturate
        }
        return "MIXED";
    }

    private static String inferDestFromJrmcHelper(String methodName, AbstractInsnNode[] insns, int i) {
        if (methodName.equals("getLong") || methodName.equals("clampTP") || methodName.equals("tpShort")
                || methodName.equals("getMaxTP")) {
            return "LONG_AUTHORITATIVE"; // Result is long (or int from long)
        }
        if (methodName.equals("getInt")) {
            return "INT_MIRROR";
        }
        if (methodName.equals("setLong") || methodName.equals("setInt")) {
            return methodName.equals("setLong") ? "LONG_AUTHORITATIVE" : "INT_MIRROR";
        }
        return "MIXED";
    }

    private static String classifyTpUtil(String op, String methodName) {
        if (methodName.startsWith("gain")) return "GAMEPLAY";
        if (methodName.startsWith("spend")) return "GAMEPLAY";
        if (methodName.startsWith("write")) return "PERSISTENCE";
        if (methodName.startsWith("read") || methodName.equals("getTP") || methodName.equals("readUsable")) return "PERSISTENCE";
        if (methodName.equals("syncToClient") || methodName.equals("acceptNetwork") || methodName.equals("applyLocal")) return "NETWORK";
        if (methodName.equals("getTPInt") || methodName.equals("mirror")) return "DISPLAY";
        if (methodName.equals("getPlayerInt") || methodName.equals("setPlayerInt")
                || methodName.equals("getTagInt") || methodName.equals("setTagInt")
                || methodName.equals("writeLegacyInt")) return "COMPATIBILITY";
        if (methodName.startsWith("command")) return "GAMEPLAY"; // Commands are gameplay
        return "UNKNOWN";
    }

    private static String classifyTpLogic(String op, String methodName) {
        if (methodName.equals("gain") || methodName.equals("spend")
                || methodName.equals("applyDelta") || methodName.equals("absolute")) {
            return "GAMEPLAY";
        }
        if (methodName.equals("mirror") || methodName.equals("normalize") || methodName.equals("fromLegacyInt")) {
            return "COMPATIBILITY";
        }
        return "GAMEPLAY";
    }

    // =========================================================================
    // Operation detection
    // =========================================================================

    private static String nbtOperation(MethodInsnNode mi) {
        if (!mi.owner.equals(NBT) && !mi.owner.equals(NBT_BASE)) return null;
        
        // Mapped names (SRG) and deobfuscated
        String name = mi.name;
        if ("func_74762_e".equals(name) || "getInteger".equals(name)) return "nbt-read-int";
        if ("func_74763_f".equals(name) || "getLong".equals(name)) return "nbt-read-long";
        if ("func_74765_d".equals(name) || "getShort".equals(name)) return "nbt-read-short";
        if ("func_74771_c".equals(name) || "getByte".equals(name)) return "nbt-read-byte";
        if ("func_74768_a".equals(name) || "setInteger".equals(name)) return "nbt-write-int";
        if ("func_74772_a".equals(name) || "setLong".equals(name)) return "nbt-write-long";
        if ("func_74777_a".equals(name) || "setShort".equals(name)) return "nbt-write-short";
        if ("func_74774_a".equals(name) || "setByte".equals(name)) return "nbt-write-byte";
        if ("func_74781_a".equals(name) || "getTag".equals(name)) return "nbt-get-tag";
        if ("func_74764_b".equals(name) || "hasKey".equals(name)) return "nbt-has-key";
        return null;
    }

    private static String jrmcHelperOperation(MethodInsnNode mi) {
        if (!mi.owner.equals(JRMCOREH)) return null;
        
        String name = mi.name;
        String desc = mi.desc;
        
        if ("nbt".equals(name)) return "jrmc-nbt";
        if ("tpShort".equals(name) && "(J)S".equals(desc)) return "jrmc-tpShort";
        if ("clampTP".equals(name) && "(J)I".equals(desc)) return "jrmc-clampTP";
        if ("getMaxTP".equals(name) && "()I".equals(desc)) return "jrmc-getMaxTP";
        if ("getInt".equals(name) && desc.equals("(Lnet/minecraft/entity/Entity;Ljava/lang/String;)I")) return "jrmc-getInt";
        if ("setInt".equals(name) && desc.equals("(Lnet/minecraft/entity/Entity;Ljava/lang/String;I)V")) return "jrmc-setInt";
        if ("getLong".equals(name) && desc.equals("(Lnet/minecraft/entity/Entity;Ljava/lang/String;)J")) return "jrmc-getLong";
        if ("setLong".equals(name) && desc.equals("(Lnet/minecraft/entity/Entity;Ljava/lang/String;J)V")) return "jrmc-setLong";
        if ("getByte".equals(name) && desc.equals("(Lnet/minecraft/entity/Entity;Ljava/lang/String;)B")) return "jrmc-getByte";
        if ("setByte".equals(name) && desc.equals("(Lnet/minecraft/entity/Entity;Ljava/lang/String;B)V")) return "jrmc-setByte";
        return null;
    }

    private static String tpUtilOperation(MethodInsnNode mi) {
        if (!mi.owner.equals(TPUTIL)) return null;
        
        String name = mi.name;
        String desc = mi.desc;
        
        // Read operations
        if ("getTP".equals(name)) {
            if (desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;)J") 
                    || desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;)J")) {
                return "tputil-getTP";
            }
        }
        if ("readUsable".equals(name)) {
            if (desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;)J")
                    || desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;)J")) {
                return "tputil-readUsable";
            }
        }
        if ("getTPInt".equals(name)) {
            if (desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;)I")
                    || desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;)I")) {
                return "tputil-getTPInt";
            }
        }
        
        // Write operations
        if ("write".equals(name)) {
            if (desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)V")
                    || desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;J)V")) {
                return "tputil-write";
            }
        }
        if ("writeChecked".equals(name) || "writeCheckedVoid".equals(name)) {
            if (desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)V")
                    || desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;J)V")
                    || desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)LJinRyuu/JRMCore/TpLogic$Result;")
                    || desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;J)LJinRyuu/JRMCore/TpLogic$Result;")) {
                return "tputil-writeChecked";
            }
        }
        
        // Gain operations
        if ("gain".equals(name)) {
            if (desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)LJinRyuu/JRMCore/TpLogic$Result;")
                    || desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;J)LJinRyuu/JRMCore/TpLogic$Result;")) {
                return "tputil-gain";
            }
        }
        if ("gainVoid".equals(name)) {
            if (desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)V")
                    || desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;J)V")) {
                return "tputil-gainVoid";
            }
        }
        
        // Spend operations
        if ("spend".equals(name)) {
            if (desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)LJinRyuu/JRMCore/TpLogic$Result;")
                    || desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;J)LJinRyuu/JRMCore/TpLogic$Result;")) {
                return "tputil-spend";
            }
        }
        if ("spendVoid".equals(name)) {
            if (desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)V")
                    || desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;J)V")) {
                return "tputil-spendVoid";
            }
        }
        
        // Network/state
        if ("syncToClient".equals(name) && desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;)V")) return "tputil-syncToClient";
        if ("applyLocal".equals(name) && desc.equals("(J)V")) return "tputil-applyLocal";
        if ("acceptNetwork".equals(name) && desc.equals("(J)V")) return "tputil-acceptNetwork";
        
        // Legacy mirror
        if ("mirror".equals(name) && desc.equals("(J)I")) return "tputil-mirror";
        
        // Legacy INT bridge
        if ("getPlayerInt".equals(name) && desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)I")) return "tputil-getPlayerInt";
        if ("setPlayerInt".equals(name) && desc.equals("(ILnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)V")) return "tputil-setPlayerInt";
        if ("getTagInt".equals(name) && desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;)I")) return "tputil-getTagInt";
        if ("setTagInt".equals(name) && desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;I)V")) return "tputil-setTagInt";
        if ("writeLegacyInt".equals(name) && desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;I)V")) return "tputil-writeLegacyInt";
        
        // Commands
        if ("commandAdd".equals(name) || "commandAddLong".equals(name)) {
            if (desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)J")
                    || desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)V")) return "tputil-commandAdd";
        }
        if ("commandTake".equals(name) || "commandTakeLong".equals(name)) {
            if (desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)J")
                    || desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)V")) return "tputil-commandTake";
        }
        if ("commandWrite".equals(name) || "commandWriteLong".equals(name)) {
            if (desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)J")
                    || desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;J)V")) return "tputil-commandWrite";
        }
        
        // Parse/other
        if ("parseLong".equals(name) && desc.equals("(Ljava/lang/String;)J")) return "tputil-parseLong";
        if ("isTpKey".equals(name) && desc.equals("(Ljava/lang/String;)Z")) return "tputil-isTpKey";
        
        return null;
    }

    private static String tpLogicOperation(MethodInsnNode mi) {
        if (!mi.owner.equals(TPLOGIC)) return null;
        
        String name = mi.name;
        String desc = mi.desc;
        
        if ("gain".equals(name) && desc.equals("(JJ)LJinRyuu/JRMCore/TpLogic$Result;")) return "tplogic-gain";
        if ("spend".equals(name) && desc.equals("(JJ)LJinRyuu/JRMCore/TpLogic$Result;")) return "tplogic-spend";
        if ("applyDelta".equals(name) && desc.equals("(JJ)LJinRyuu/JRMCore/TpLogic$Result;")) return "tplogic-applyDelta";
        if ("absolute".equals(name) && desc.equals("(J)LJinRyuu/JRMCore/TpLogic$Result;")) return "tplogic-absolute";
        if ("mirror".equals(name) && desc.equals("(J)I")) return "tplogic-mirror";
        if ("normalize".equals(name) && desc.equals("(J)J")) return "tplogic-normalize";
        if ("fromLegacyInt".equals(name) && desc.equals("(I)J")) return "tplogic-fromLegacyInt";
        if ("rejected".equals(name) && desc.equals("(LJinRyuu/JRMCore/TpLogic$Status;J)LJinRyuu/JRMCore/TpLogic$Result;")) return "tplogic-rejected";
        
        return null;
    }

    // =========================================================================
    // Context/Key extraction (from TpSiteScan)
    // =========================================================================

    private static String keyBefore(AbstractInsnNode[] a, int i) {
        for (int j = i - 1; j >= 0 && j >= i - 12; j--) { // Extended to 12 for more context
            AbstractInsnNode p = a[j];
            if (p instanceof LdcInsnNode) {
                Object c = ((LdcInsnNode) p).cst;
                if (c instanceof String && ((String) c).startsWith("jrmcTp")) {
                    return (String) c;
                }
            }
            if (p instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) p;
                if (!(mi.owner.equals(JRMCOREH) && mi.name.equals("nbt"))) {
                    return null;
                }
            }
            if (p instanceof FieldInsnNode) {
                return null;
            }
        }
        return null;
    }

    private static String context(AbstractInsnNode[] a, int i) {
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, i - 8);
        for (int j = from; j < i; j++) {
            String s = shortInsn(a[j]);
            if (s == null) continue;
            if (sb.length() > 0) sb.append(" | ");
            sb.append(s);
        }
        return sb.toString();
    }

    private static String shortInsn(AbstractInsnNode insn) {
        if (insn instanceof LdcInsnNode) {
            Object c = ((LdcInsnNode) insn).cst;
            if (c instanceof String) {
                String s = (String) c;
                if (s.length() > 24) s = s.substring(0, 24) + "...";
                return "ldc\"" + s + "\"";
            }
            return "ldc" + c;
        }
        if (insn instanceof IntInsnNode) {
            return "push" + ((IntInsnNode) insn).operand;
        }
        if (insn instanceof VarInsnNode) {
            return "var" + ((VarInsnNode) insn).var;
        }
        if (insn instanceof FieldInsnNode) {
            FieldInsnNode f = (FieldInsnNode) insn;
            return "getfield:" + f.owner.substring(f.owner.lastIndexOf('/') + 1) + "." + f.name;
        }
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode m = (MethodInsnNode) insn;
            return "call:" + m.owner.substring(m.owner.lastIndexOf('/') + 1) + "." + m.name;
        }
        if (insn instanceof IincInsnNode) {
            return "iinc" + ((IincInsnNode) insn).var + ":" + ((IincInsnNode) insn).incr;
        }
        int op = insn.getOpcode();
        switch (op) {
            case Opcodes.IADD: return "iadd";
            case Opcodes.ISUB: return "isub";
            case Opcodes.IMUL: return "imul";
            case Opcodes.IDIV: return "idiv";
            case Opcodes.I2L: return "i2l";
            case Opcodes.L2I: return "l2i";
            case Opcodes.LADD: return "ladd";
            case Opcodes.LSUB: return "lsub";
            case Opcodes.LMUL: return "lmul";
            case Opcodes.LDIV: return "ldiv";
            case Opcodes.LCMP: return "lcmp";
            case Opcodes.IFGE: return "ifge";
            case Opcodes.IFGT: return "ifgt";
            case Opcodes.IFLE: return "ifle";
            case Opcodes.IFLT: return "iflt";
            case Opcodes.IFEQ: return "ifeq";
            case Opcodes.IFNE: return "ifne";
            case Opcodes.IF_ICMPGE: return "if_icmpge";
            case Opcodes.IF_ICMPGT: return "if_icmpgt";
            case Opcodes.IF_ICMPLE: return "if_icmple";
            case Opcodes.IF_ICMPLT: return "if_icmplt";
            case Opcodes.IF_ICMPEQ: return "if_icmpeq";
            case Opcodes.IF_ICMPNE: return "if_icmpne";
            case Opcodes.DUP: return "dup";
            case Opcodes.POP: return "pop";
            case Opcodes.ICONST_0: return "0";
            case Opcodes.ICONST_1: return "1";
            case Opcodes.ICONST_M1: return "-1";
            case Opcodes.LCONST_0: return "0L";
            case Opcodes.LCONST_1: return "1L";
            case Opcodes.INEG: return "ineg";
            case Opcodes.LNEG: return "lneg";
            case Opcodes.I2B: return "i2b";
            default: return null;
        }
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static final class Row {
        final String jar;
        final String clazz;
        final String method;
        final String desc;
        final int offset;
        final String key;
        final String op;
        final String opDesc;
        final String context;
        final String category;
        final String origin;
        final String destination;

        Row(String jar, String clazz, String method, String desc, int offset, String key,
            String op, String opDesc, String context, String category, String origin, String destination) {
            this.jar = jar;
            this.clazz = clazz;
            this.method = method;
            this.desc = desc;
            this.offset = offset;
            this.key = key;
            this.op = op;
            this.opDesc = opDesc;
            this.context = context;
            this.category = category;
            this.origin = origin;
            this.destination = destination;
        }
    }
}
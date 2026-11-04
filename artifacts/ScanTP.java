import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Audita todos os acessos a TP em todos os jars de mods.
 * Saida: linhas TSV em stdout.
 */
public class ScanTP {
    static final String H = "JinRyuu/JRMCore/JRMCoreH";

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0]
            : "C:\\Users\\Fernando\\Desktop\\NanatsuCore\\nntdarkmp_copy\\mods");
        List<File> jars = new ArrayList<>();
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".jar")) jars.add(f);
        }
        Collections.sort(jars);

        for (File jar : jars) {
            if (jar.getName().contains("bignum")) continue; // evita duplicar JRMCore patchado
            try (ZipFile zip = new ZipFile(jar)) {
                for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                    ZipEntry entry = e.nextElement();
                    if (!entry.getName().endsWith(".class")) continue;
                    byte[] data;
                    try { data = zip.getInputStream(entry).readAllBytes(); } catch (Exception ex) { continue; }
                    ClassNode cn = new ClassNode();
                    try { new ClassReader(data).accept(cn, 0); } catch (Throwable t) { continue; }

                    for (MethodNode m : cn.methods) {
                        List<AbstractInsnNode> list = new ArrayList<>();
                        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                            if (i instanceof LabelNode || i instanceof LineNumberNode || i instanceof FrameNode) continue;
                            list.add(i);
                        }
                        for (int idx = 0; idx < list.size(); idx++) {
                            AbstractInsnNode i = list.get(idx);

                            if (i instanceof MethodInsnNode) {
                                MethodInsnNode mi = (MethodInsnNode) i;
                                if (mi.owner.equals(H) && (mi.name.equals("getInt") || mi.name.equals("setInt")
                                        || mi.name.equals("getLong") || mi.name.equals("setLong") || mi.name.equals("addLong"))) {
                                    String key = nearestString(list, idx);
                                    System.out.println("CALL\t" + jar.getName() + "\t" + cn.name + "\t" + m.name + m.desc
                                        + "\t" + mi.name + mi.desc + "\tkey=" + key);
                                }
                                if (mi.owner.equals("net/minecraft/nbt/NBTTagCompound")) {
                                    String key = nearestString(list, idx);
                                    if (key != null && key.startsWith("jrmcTp")) {
                                        System.out.println("NBT\t" + jar.getName() + "\t" + cn.name + "\t" + m.name + m.desc
                                            + "\t" + mi.name + mi.desc + "\tkey=" + key
                                            + "\tctx=" + ctx(list, idx));
                                    }
                                }
                            } else if (i instanceof FieldInsnNode) {
                                FieldInsnNode fi = (FieldInsnNode) i;
                                if (fi.owner.equals(H) && fi.name.toLowerCase().contains("tp")) {
                                    System.out.println("FIELD\t" + jar.getName() + "\t" + cn.name + "\t" + m.name + m.desc
                                        + "\t" + op(fi.getOpcode()) + " " + fi.name + " " + fi.desc);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("SKIP " + jar.getName() + ": " + ex);
            }
        }
    }

    static String nearestString(List<AbstractInsnNode> list, int idx) {
        for (int j = idx - 1; j >= Math.max(0, idx - 6); j--) {
            AbstractInsnNode p = list.get(j);
            if (p instanceof LdcInsnNode && ((LdcInsnNode) p).cst instanceof String) {
                return (String) ((LdcInsnNode) p).cst;
            }
            if (p instanceof MethodInsnNode || p instanceof FieldInsnNode) {
                // continua procurando: a key costuma vir antes do getInteger
            }
        }
        return null;
    }

    static String ctx(List<AbstractInsnNode> list, int idx) {
        int from = Math.max(0, idx - 4);
        int to = Math.min(list.size() - 1, idx + 2);
        StringBuilder sb = new StringBuilder();
        for (int j = from; j <= to; j++) {
            if (j == idx) sb.append(" <<").append(op(list.get(j).getOpcode())).append(">> ");
            else sb.append(op(list.get(j).getOpcode())).append(' ');
        }
        return sb.toString().trim();
    }

    static String op(int op) {
        return op < 0 ? "PSEUDO" : org.objectweb.asm.util.Printer.OPCODES[op];
    }
}

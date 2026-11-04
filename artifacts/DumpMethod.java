import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Dump de metodo com indice, labels resolvidos e alvos de salto. */
public class DumpMethod {
    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        String cls = args[1];
        String want = args[2];

        try (ZipFile zip = new ZipFile(jarPath)) {
            ZipEntry entry = zip.getEntry(cls);
            if (entry == null) { System.out.println("not found: " + cls); return; }
            byte[] data = zip.getInputStream(entry).readAllBytes();
            ClassNode cn = new ClassNode();
            new ClassReader(data).accept(cn, 0);

            for (MethodNode m : cn.methods) {
                if (!m.name.equals(want)) continue;
                System.out.println("=== " + cn.name + "." + m.name + m.desc + " maxStack=" + m.maxStack + " maxLocals=" + m.maxLocals + " ===");

                Map<LabelNode, Integer> idx = new HashMap<>();
                int n = 0;
                for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                    if (i instanceof LabelNode) idx.put((LabelNode) i, n);
                    n++;
                }

                n = 0;
                for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext(), n++) {
                    String s;
                    if (i instanceof LabelNode) s = "L" + idx.get(i) + ":";
                    else if (i instanceof LineNumberNode) s = "// line " + ((LineNumberNode) i).line;
                    else if (i instanceof FrameNode) s = "// frame";
                    else if (i instanceof MethodInsnNode) {
                        MethodInsnNode mi = (MethodInsnNode) i;
                        s = name(mi.getOpcode()) + " " + mi.owner + "." + mi.name + mi.desc;
                    } else if (i instanceof FieldInsnNode) {
                        FieldInsnNode fi = (FieldInsnNode) i;
                        s = name(fi.getOpcode()) + " " + fi.owner + "." + fi.name + " " + fi.desc;
                    } else if (i instanceof VarInsnNode) {
                        s = name(i.getOpcode()) + " " + ((VarInsnNode) i).var;
                    } else if (i instanceof LdcInsnNode) {
                        s = "LDC " + ((LdcInsnNode) i).cst;
                    } else if (i instanceof IntInsnNode) {
                        s = name(i.getOpcode()) + " " + ((IntInsnNode) i).operand;
                    } else if (i instanceof IincInsnNode) {
                        IincInsnNode ii = (IincInsnNode) i;
                        s = "IINC " + ii.var + " " + ii.incr;
                    } else if (i instanceof JumpInsnNode) {
                        s = name(i.getOpcode()) + " -> L" + idx.get(((JumpInsnNode) i).label);
                    } else if (i instanceof TypeInsnNode) {
                        s = name(i.getOpcode()) + " " + ((TypeInsnNode) i).desc;
                    } else if (i instanceof InsnNode) {
                        s = name(i.getOpcode());
                    } else if (i instanceof TableSwitchInsnNode) {
                        s = "TABLESWITCH";
                    } else if (i instanceof LookupSwitchInsnNode) {
                        s = "LOOKUPSWITCH";
                    } else {
                        s = i.getClass().getSimpleName();
                    }
                    System.out.println(String.format("%4d %s", n, s));
                }
                System.out.println();
            }
        }
    }

    static String name(int op) {
        return op < 0 ? "PSEUDO" : org.objectweb.asm.util.Printer.OPCODES[op];
    }
}

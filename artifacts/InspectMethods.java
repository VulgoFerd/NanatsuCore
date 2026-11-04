import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.zip.*;

public class InspectMethods {
    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        String cls = args[1];
        String want = args.length > 2 ? args[2] : null;

        try (ZipFile zip = new ZipFile(jarPath)) {
            ZipEntry entry = zip.getEntry(cls);
            if (entry == null) { System.out.println("not found: " + cls); return; }
            byte[] data = zip.getInputStream(entry).readAllBytes();
            ClassNode cn = new ClassNode();
            new ClassReader(data).accept(cn, 0);

            for (MethodNode m : cn.methods) {
                if (want != null && !m.name.equals(want)) continue;
                System.out.println("=== " + m.name + m.desc + " ===");
                for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    System.out.println("  " + insn.getOpcode() + " " + describe(insn));
                }
                System.out.println();
            }
        }
    }

    static String describe(AbstractInsnNode insn) {
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode m = (MethodInsnNode) insn;
            return m.owner + "." + m.name + m.desc;
        } else if (insn instanceof FieldInsnNode) {
            FieldInsnNode f = (FieldInsnNode) insn;
            return f.owner + "." + f.name + " " + f.desc;
        } else if (insn instanceof VarInsnNode) {
            return "var=" + ((VarInsnNode) insn).var;
        } else if (insn instanceof LdcInsnNode) {
            return "cst=" + ((LdcInsnNode) insn).cst;
        } else if (insn instanceof IntInsnNode) {
            return "op=" + ((IntInsnNode) insn).operand;
        } else if (insn instanceof IincInsnNode) {
            IincInsnNode i = (IincInsnNode) insn;
            return "var=" + i.var + " incr=" + i.incr;
        } else if (insn instanceof JumpInsnNode) {
            return "->label";
        } else if (insn instanceof TypeInsnNode) {
            return ((TypeInsnNode) insn).desc;
        }
        return "";
    }
}

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.util.CheckClassAdapter;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Verificacao real de dataflow com BasicVerifier (nao precisa de hierarquia
 * de classes do Minecraft) + inspecao de descritores/operandos dos sites de TP.
 */
public class VerifyASM {
    static final String JAR = "C:\\Users\\Fernando\\Desktop\\NanatsuCore\\artifacts\\JRMCore-v1.3.51-bignum.jar";
    static int failures = 0;
    static int checked = 0;

    public static void main(String[] args) throws Exception {
        String[] targets = {
            "JinRyuu/JRMCore/JRMCoreH.class",
            "JinRyuu/JRMCore/JRMCorePacHanS.class",
            "JinRyuu/JRMCore/JRMCoreComTickH.class",
            "JinRyuu/JRMCore/ComJrmctp.class",
            "JinRyuu/JRMCore/ComJrmcRacialSkill.class",
            "JinRyuu/JRMCore/p/JRMCorePStats2.class",
            "JinRyuu/JRMCore/p/JRMCorePCost.class",
            "JinRyuu/JRMCore/JRMCorePacHanC.class"
        };
        try (ZipFile zip = new ZipFile(JAR)) {
            for (String t : targets) {
                ZipEntry e = zip.getEntry(t);
                if (e == null) { System.out.println("MISSING " + t); failures++; continue; }
                byte[] data = zip.getInputStream(e).readAllBytes();
                verify(t, data);
            }
        }
        System.out.println("\n=== RESULT: checked=" + checked + " failures=" + failures + " ===");
        if (failures > 0) System.exit(1);
    }

    static void verify(String name, byte[] data) {
        // CheckClassAdapter: valida contrato de visitacao/descritores/constantes
        try {
            new ClassReader(data).accept(new CheckClassAdapter(new ClassWriter(0)), 0);
            System.out.println("CheckClassAdapter OK: " + name);
        } catch (Throwable ex) {
            failures++;
            System.out.println("CheckClassAdapter FAIL: " + name + " :: " + ex);
        }

        ClassNode cn = new ClassNode();
        new ClassReader(data).accept(cn, 0);
        System.out.println("\n--- " + name + " (version " + cn.version + ") ---");
        for (MethodNode m : cn.methods) {
            checked++;
            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicVerifier());
            try {
                analyzer.analyze(cn.name, m);
            } catch (AnalyzerException ex) {
                failures++;
                System.out.println("  [FAIL] " + m.name + m.desc + " :: " + ex.getMessage());
            } catch (Throwable ex) {
                failures++;
                System.out.println("  [FAIL] " + m.name + m.desc + " :: " + ex);
            }
        }
        // sites de TP
        for (MethodNode m : cn.methods) {
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof MethodInsnNode) {
                    MethodInsnNode mi = (MethodInsnNode) i;
                    if (mi.owner.equals("JinRyuu/JRMCore/JRMCoreH")
                            && (mi.name.equals("adjustTP") || mi.name.equals("setTPFromMirror")
                                || mi.name.equals("writeTP") || mi.name.equals("getTPLong")
                                || mi.name.equals("saturateTP"))) {
                        System.out.println("  TP-SITE " + m.name + m.desc + " -> " + mi.name + mi.desc);
                    }
                }
            }
        }
    }
}

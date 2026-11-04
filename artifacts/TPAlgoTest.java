import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.zip.*;

/**
 * Testes obrigatorios de aritmetica/saturacao do TP longo.
 * 1) Simula exatamente a semantica dos helpers gravados no bytecode.
 * 2) Inspeciona o bytecode patchado para garantir ausencia de Math.toIntExact
 *    em getInt e presenca da saturacao por LCMP contra 2147483647L.
 */
public class TPAlgoTest {
    static int pass = 0, fail = 0;
    static final long MAXL = Integer.MAX_VALUE;

    public static void main(String[] args) throws Exception {
        // ---- 1. semantica ----
        expect("getInt(2147483647)", getInt(2147483647L, 0), Integer.MAX_VALUE);
        expect("getInt(2147483648)", getInt(2147483648L, 0), Integer.MAX_VALUE);
        expect("getInt(3000000000)", getInt(3000000000L, 0), Integer.MAX_VALUE);
        expect("getInt(1000000000000)", getInt(1000000000000L, 0), Integer.MAX_VALUE);
        expect("getInt(0 fallback 123)", getInt(0L, 123), 123);
        expect("getInt(-5)", getInt(-5L, 0), 0);
        expect("getInt(123)", getInt(123L, 0), 123);

        // cobranca acima de MAX: nao pode truncar
        long bal = 3000000000L;
        bal = adjust(bal, -12345L);
        expect("3e9 - 12345", (int) (bal & 0xFFFFFFFFL), (int) (2999987655L & 0xFFFFFFFFL));
        expect("3e9 - 12345 sat", (int) sat(bal), Integer.MAX_VALUE);

        // setInt absoluto no mundo int
        expect("setTPFromMirror cur=1000 value=900", setFromMirror(1000L, 900), 900L);
        // setInt com espelho saturado: valor = MAX - 500
        long afterCost = setFromMirror(3000000000L, Integer.MAX_VALUE - 500);
        expect("setTPFromMirror cur=3e9 value=MAX-500 -> delta", (int) sat(afterCost), Integer.MAX_VALUE);
        if (afterCost != 2999999500L) { fail++; System.out.println("[FAIL] setFromMirror saturated delta: got " + afterCost + " want 2999999500"); } else pass++;

        // add permitido acima de MAX (comando)
        long big = adjust(3000000000L, 1000000000L);
        expect("3e9 + 1e9", (int) (big & 0xFFFFFFFFL), (int) (4000000000L & 0xFFFFFFFFL));
        expect("3e9 + 1e9 mirror", (int) sat(big), Integer.MAX_VALUE);

        // 1e12
        long t = adjust(0L, 1000000000000L);
        expect("0 + 1e12", (int) (t & 0xFFFFFFFFL), (int) (1000000000000L & 0xFFFFFFFFL));
        long t2 = adjust(t, -1000000000L);
        expect("1e12 - 1e9", (int) (t2 & 0xFFFFFFFFL), (int) (999000000000L & 0xFFFFFFFFL));
        expect("floor at zero", adjust(5L, -100L), 0L);

        // persistencia (round-trip de long em 8 bytes)
        expect("persist 1000000000000", readLong(writeLong(1000000000000L)), 1000000000000L);
        expect("persist 3000000000", readLong(writeLong(3000000000L)), 3000000000L);

        // ---- 2. bytecode ----
        String jar = "C:\\Users\\Fernando\\Desktop\\NanatsuCore\\artifacts\\JRMCore-v1.3.51-bignum.jar";
        try (ZipFile zip = new ZipFile(jar)) {
            ClassNode cn = new ClassNode();
            new ClassReader(zip.getInputStream(zip.getEntry("JinRyuu/JRMCore/JRMCoreH.class")).readAllBytes()).accept(cn, 0);
            MethodNode getInt = find(cn, "getInt", "(Lnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)I");
            MethodNode setInt = find(cn, "setInt", "(ILnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;)V");
            if (getInt == null || setInt == null) { fail++; System.out.println("[FAIL] getInt/setInt missing"); }
            else {
                check("getInt sem Math.toIntExact", !hasInvoke(getInt, "java/lang/Math", "toIntExact"));
                check("getInt compara com 2147483647L", hasLongConst(getInt, 2147483647L));
                check("getInt usa jrmcTplong", hasString(getInt, "jrmcTplong"));
                check("setInt chama setTPFromMirror", hasInvoke(setInt, "JinRyuu/JRMCore/JRMCoreH", "setTPFromMirror"));
                check("setInt compara key jrmcTpint", hasString(setInt, "jrmcTpint"));
            }
        }

        System.out.println("\n=== TP ALGO: pass=" + pass + " fail=" + fail + " ===");
        if (fail > 0) System.exit(1);
    }

    // ---- semantica espelhada no bytecode ----
    static int getInt(long authority, int mirror) {
        if (authority == 0L) return mirror;
        return sat(authority);
    }
    static int sat(long v) {
        if (v <= 0) return 0;
        if (v >= MAXL) return Integer.MAX_VALUE;
        return (int) v;
    }
    static long adjust(long cur, long delta) {
        long n = cur + delta;
        return n < 0 ? 0 : n;
    }
    static long setFromMirror(long cur, int value) {
        long n = cur <= MAXL ? value : cur + ((long) value - MAXL);
        return n < 0 ? 0 : n;
    }
    static byte[] writeLong(long v) {
        return new byte[]{(byte)(v>>>56),(byte)(v>>>48),(byte)(v>>>40),(byte)(v>>>32),(byte)(v>>>24),(byte)(v>>>16),(byte)(v>>>8),(byte)v};
    }
    static long readLong(byte[] b) {
        long v = 0;
        for (byte x : b) v = (v << 8) | (x & 0xFFL);
        return v;
    }

    static void expect(String name, long got, long want) {
        if (got == want) { pass++; }
        else { fail++; System.out.println("[FAIL] " + name + ": got " + got + " want " + want); }
    }
    static void check(String name, boolean ok) {
        if (ok) pass++; else { fail++; System.out.println("[FAIL] " + name); }
    }

    static MethodNode find(ClassNode cn, String n, String d) {
        for (MethodNode m : cn.methods) if (m.name.equals(n) && m.desc.equals(d)) return m;
        return null;
    }
    static boolean hasInvoke(MethodNode m, String owner, String name) {
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext())
            if (i instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) i;
                if (mi.owner.equals(owner) && mi.name.equals(name)) return true;
            }
        return false;
    }
    static boolean hasLongConst(MethodNode m, long v) {
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext())
            if (i instanceof LdcInsnNode && Long.valueOf(v).equals(((LdcInsnNode) i).cst)) return true;
        return false;
    }
    static boolean hasString(MethodNode m, String s) {
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext())
            if (i instanceof LdcInsnNode && s.equals(((LdcInsnNode) i).cst)) return true;
        return false;
    }
}

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Compara descritores de metodos/campos entre o jar original e o patchado. */
public class DiffDescriptors {
    static final String ORIG = "C:\\Users\\Fernando\\nntdarkmp\\mods\\JRMCore-v1.3.51.jar";
    static final String PATCHED = "C:\\Users\\Fernando\\Desktop\\NanatsuCore\\artifacts\\JRMCore-v1.3.51-bignum.jar";

    public static void main(String[] args) throws Exception {
        Map<String, Set<String>> a = load(ORIG);
        Map<String, Set<String>> b = load(PATCHED);
        int changed = 0;
        for (String cls : new TreeSet<>(a.keySet())) {
            Set<String> ma = a.get(cls);
            Set<String> mb = b.getOrDefault(cls, Collections.emptySet());
            Set<String> removed = new TreeSet<>(ma); removed.removeAll(mb);
            Set<String> added = new TreeSet<>(mb); added.removeAll(ma);
            if (!removed.isEmpty() || !added.isEmpty()) {
                System.out.println("CLASS " + cls);
                for (String s : removed) System.out.println("  - " + s);
                for (String s : added) System.out.println("  + " + s);
                changed++;
            }
        }
        for (String cls : b.keySet()) if (!a.containsKey(cls)) System.out.println("NEW CLASS " + cls);
        System.out.println("\nClasses with method/field signature changes: " + changed);
        System.out.println("(expected: only + helpers in JRMCoreH and + curTPLong field)");
    }

    static Map<String, Set<String>> load(String jar) throws Exception {
        Map<String, Set<String>> out = new TreeMap<>();
        try (ZipFile zip = new ZipFile(jar)) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry en = e.nextElement();
                if (!en.getName().endsWith(".class")) continue;
                ClassNode cn = new ClassNode();
                new ClassReader(zip.getInputStream(en).readAllBytes()).accept(cn, 0);
                Set<String> sig = new TreeSet<>();
                for (MethodNode m : cn.methods) sig.add("M " + m.name + m.desc);
                for (FieldNode f : cn.fields) sig.add("F " + f.name + " " + f.desc);
                out.put(cn.name, sig);
            }
        }
        return out;
    }
}

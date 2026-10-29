import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.commons.ClassRemapper;
import jdk.internal.org.objectweb.asm.commons.Remapper;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.FieldNode;
import jdk.internal.org.objectweb.asm.tree.InvokeDynamicInsnNode;
import jdk.internal.org.objectweb.asm.tree.LdcInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.TypeInsnNode;
import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicVerifier;

/**
 * Turns the awakening classes, compiled against Mojang-named API stubs, into classes that link
 * against the SRG-named Forge 1.20.1 runtime, and refuses to produce anything it cannot prove.
 *
 * <p>Usage: {@code KaguneRemap <compiled classes> <stub classes> <mappings.srg> <input.jar> <output dir>}
 */
public final class KaguneRemap implements Opcodes {
    private static final String[] OBFUSCATED = {"net/minecraft/", "com/mojang/blaze3d/", "com/mojang/math/"};
    private static final String[] CLIENT_ONLY = {"net/minecraft/client/", "com/mojang/blaze3d/", "net/minecraftforge/client/",
            "com/hex/kagune/client/", "com/hex/kagune/control/client/", "com/hex/kagune/control/awakening/client/"};
    private static final Set<String> OBJECT_METHODS = Set.of("equals", "hashCode", "toString", "getClass", "ordinal", "name", "compareTo",
            "values", "valueOf", "getDeclaringClass");

    private record Stub(String superName, List<String> interfaces, Set<String> methods, Set<String> fields, Set<String> enumConstants) {
    }

    private static boolean obfuscated(String owner) {
        for (String prefix : OBFUSCATED) if (owner.startsWith(prefix)) return true;
        return false;
    }

    private static boolean clientOnly(String owner) {
        for (String prefix : CLIENT_ONLY) if (owner.startsWith(prefix)) return true;
        return false;
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static List<Path> classes(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(path -> path.toString().endsWith(".class")).sorted().toList();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: KaguneRemap <classes> <stubs> <mappings.srg> <input.jar> <output>");
        Path compiled = Path.of(args[0]);
        Map<String, Stub> stubs = new HashMap<>();
        for (Path file : classes(Path.of(args[1]))) {
            ClassNode node = read(Files.readAllBytes(file));
            Set<String> methods = new HashSet<>();
            for (MethodNode method : node.methods) methods.add(method.name + method.desc);
            Set<String> fields = new HashSet<>();
            Set<String> enums = new HashSet<>();
            for (FieldNode field : node.fields) {
                fields.add(field.name + ":" + field.desc);
                if ((field.access & ACC_ENUM) != 0) enums.add(field.name);
            }
            stubs.put(node.name, new Stub(node.superName, node.interfaces, methods, fields, enums));
        }

        Map<String, String> table = new HashMap<>();
        for (String raw : Files.readAllLines(Path.of(args[2]), StandardCharsets.UTF_8)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("C ")) continue;
            String[] p = line.split("\\s+");
            String key = p[0] + " " + p[1] + " " + p[2] + " " + p[3];
            if (table.put(key, p[4]) != null) throw new IllegalStateException("Duplicate mapping " + key);
        }

        // Evidence from the original JAR: every SRG member it links against at runtime.
        Set<String> proven = new HashSet<>();
        try (ZipFile jar = new ZipFile(args[3])) {
            for (var entry : Collections.list(jar.entries())) {
                if (!entry.getName().endsWith(".class")) continue;
                ClassNode node = read(jar.getInputStream(entry).readAllBytes());
                for (MethodNode method : node.methods) {
                    for (AbstractInsnNode insn : method.instructions) {
                        if (insn instanceof MethodInsnNode call) proven.add(call.name + call.desc);
                        if (insn instanceof FieldInsnNode access) proven.add(access.name + ":" + access.desc);
                    }
                }
            }
        }
        Set<String> tabled = new HashSet<>();
        for (Map.Entry<String, String> entry : table.entrySet()) {
            String[] key = entry.getKey().split(" ");
            tabled.add(key[0].equals("M") ? entry.getValue() + key[3] : entry.getValue() + ":" + key[3]);
        }

        // The awakening classes themselves: a class may extend a Minecraft class (a screen, a render
        // layer), so its overriding declarations and the members it inherits need SRG names too.
        Map<String, Stub> ours = new HashMap<>();
        for (Path file : classes(compiled)) {
            ClassNode node = read(Files.readAllBytes(file));
            Set<String> methods = new HashSet<>();
            for (MethodNode method : node.methods) methods.add(method.name + method.desc);
            Set<String> fields = new HashSet<>();
            for (FieldNode field : node.fields) fields.add(field.name + ":" + field.desc);
            ours.put(node.name, new Stub(node.superName, node.interfaces, methods, fields, Set.of()));
        }

        List<String> errors = new ArrayList<>();
        Set<String> used = new TreeSet<>();
        Remapper remapper = new Remapper() {
            @Override
            public String mapMethodName(String owner, String name, String descriptor) {
                if (name.startsWith("<")) return name;
                if (ours.containsKey(owner)) return inherited("M", owner, name, descriptor, false);
                if (!obfuscated(owner)) return name;
                String mapped = resolve("M", owner, name, descriptor, false);
                if (mapped == null) errors.add("Unmapped method " + owner + "." + name + descriptor);
                return mapped == null ? name : mapped;
            }

            @Override
            public String mapFieldName(String owner, String name, String descriptor) {
                if (ours.containsKey(owner)) {
                    // A field an awakening class declares is its own; only inherited ones are Minecraft's.
                    if (ours.get(owner).fields.contains(name + ":" + descriptor)) return name;
                    return inherited("F", owner, name, descriptor, true);
                }
                if (!obfuscated(owner)) return name;
                String mapped = resolve("F", owner, name, descriptor, true);
                if (mapped == null) errors.add("Unmapped field " + owner + "." + name + ":" + descriptor);
                return mapped == null ? name : mapped;
            }

            /**
             * A lambda implementing a Minecraft functional interface: the interface method's name in
             * the invokedynamic must be its SRG name, or the lambda object would not implement it.
             */
            @Override
            public String mapInvokeDynamicMethodName(String name, String descriptor) {
                String face = jdk.internal.org.objectweb.asm.Type.getReturnType(descriptor).getInternalName();
                if (!obfuscated(face)) return name;
                Stub stub = stubs.get(face);
                String found = null;
                if (stub != null) {
                    for (String method : stub.methods) {
                        if (method.startsWith(name + "(")) {
                            if (found != null) errors.add("Ambiguous lambda method " + face + "." + name);
                            found = method.substring(name.length());
                        }
                    }
                }
                String mapped = found == null ? null : resolve("M", face, name, found, false);
                if (mapped == null) errors.add("Unmapped lambda method " + face + "." + name);
                return mapped == null ? name : mapped;
            }

            /**
             * A member referenced through, or declared by, an awakening class: if a Minecraft
             * supertype declares it, the awakening class overrides or inherits it and it takes that
             * member's SRG name; otherwise it is the awakening class's own and keeps its name.
             */
            private String inherited(String kind, String owner, String name, String descriptor, boolean field) {
                Deque<String> queue = new ArrayDeque<>();
                Stub self = ours.get(owner);
                if (self.superName != null) queue.add(self.superName);
                queue.addAll(self.interfaces);
                Set<String> seen = new HashSet<>();
                while (!queue.isEmpty()) {
                    String type = queue.poll();
                    if (!seen.add(type)) continue;
                    Stub stub = ours.containsKey(type) ? ours.get(type) : stubs.get(type);
                    if (stub == null) continue;
                    boolean declares = field ? stub.fields.contains(name + ":" + descriptor) : stub.methods.contains(name + descriptor);
                    if (declares && obfuscated(type)) {
                        String mapped = resolve(kind, type, name, descriptor, field);
                        if (mapped == null) errors.add("Unmapped inherited " + (field ? "field " : "method ") + type + "." + name + descriptor + " via " + owner);
                        return mapped == null ? name : mapped;
                    }
                    if (declares && ours.containsKey(type)) return inherited(kind, type, name, descriptor, field);
                    if (stub.superName != null) queue.add(stub.superName);
                    queue.addAll(stub.interfaces);
                }
                return name;
            }

            private String resolve(String kind, String owner, String name, String descriptor, boolean field) {
                Deque<String> queue = new ArrayDeque<>(List.of(owner));
                Set<String> seen = new HashSet<>();
                while (!queue.isEmpty()) {
                    String type = queue.poll();
                    if (!seen.add(type)) continue;
                    String srg = table.get(kind + " " + type + " " + name + " " + descriptor);
                    if (srg != null) {
                        used.add(kind + " " + type + " " + name + " " + descriptor + " -> " + srg);
                        return srg;
                    }
                    Stub stub = stubs.get(type);
                    if (stub == null) {
                        // Left the stubbed API: only java.lang members are unobfuscated out there.
                        if (!field && type.startsWith("java/") && OBJECT_METHODS.contains(name)) return name;
                        continue;
                    }
                    if (field && stub.enumConstants.contains(name)) return name;
                    boolean declares = field ? stub.fields.contains(name + ":" + descriptor) : stub.methods.contains(name + descriptor);
                    if (declares && !obfuscated(type)) return name;
                    if (stub.superName != null) queue.add(stub.superName);
                    queue.addAll(stub.interfaces);
                }
                return null;
            }
        };

        Path output = Path.of(args[4]);
        int written = 0;
        for (Path file : classes(compiled)) {
            ClassNode original = read(Files.readAllBytes(file));
            if (!original.name.startsWith("com/hex/kagune/control/awakening/")) throw new IllegalStateException("Unexpected class " + original.name);
            ClassWriter writer = new ClassWriter(0);
            new ClassReader(Files.readAllBytes(file)).accept(new ClassRemapper(writer, remapper), 0);
            byte[] bytes = writer.toByteArray();
            ClassNode remapped = read(bytes);
            verify(remapped, proven, tabled, errors);
            Path target = output.resolve(remapped.name + ".class");
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            written++;
        }
        if (!errors.isEmpty()) {
            new TreeSet<>(errors).forEach(System.err::println);
            throw new IllegalStateException(errors.size() + " remapping problems");
        }
        System.out.println("Remapped " + written + " awakening classes through " + used.size() + " verified SRG members.");
    }

    private static void verify(ClassNode node, Set<String> proven, Set<String> tabled, List<String> errors) {
        boolean client = node.name.startsWith("com/hex/kagune/control/awakening/client/");
        for (MethodNode method : node.methods) {
            try {
                new Analyzer<>(new BasicVerifier()).analyze(node.name, method);
            } catch (Exception failure) {
                errors.add("Bytecode verification failed in " + node.name + "." + method.name + ": " + failure.getMessage());
            }
            boolean deferred = method.name.startsWith("lambda$");
            for (AbstractInsnNode insn : method.instructions) {
                String owner = null;
                String member = null;
                if (insn instanceof MethodInsnNode call) {
                    owner = call.owner;
                    if (!call.name.startsWith("<")) member = call.name + call.desc;
                } else if (insn instanceof FieldInsnNode access) {
                    owner = access.owner;
                    member = access.name + ":" + access.desc;
                } else if (insn instanceof TypeInsnNode type) {
                    owner = type.desc;
                } else if (insn instanceof LdcInsnNode constant && constant.cst instanceof jdk.internal.org.objectweb.asm.Type type) {
                    owner = type.getInternalName();
                } else if (insn instanceof InvokeDynamicInsnNode indy) {
                    for (Object argument : indy.bsmArgs) {
                        if (argument instanceof Handle handle && obfuscated(handle.getOwner()) && !handle.getName().startsWith("<")) {
                            checkSrg(node, handle.getName() + handle.getDesc(), proven, tabled, errors);
                        }
                        if (argument instanceof Handle handle && !client && !deferred && clientOnly(handle.getOwner())) {
                            errors.add(node.name + "." + method.name + " references client-only " + handle.getOwner() + " outside a deferred lambda");
                        }
                    }
                }
                if (owner == null) continue;
                if (owner.startsWith("[")) owner = owner.replaceAll("^\\[+L?|;$", "");
                if (!client && !deferred && clientOnly(owner)) {
                    errors.add(node.name + "." + method.name + " references client-only " + owner + " outside a deferred lambda");
                }
                if (member != null && obfuscated(owner) && (member.startsWith("m_") || member.startsWith("f_"))) {
                    checkSrg(node, member, proven, tabled, errors);
                }
            }
        }
    }

    private static void checkSrg(ClassNode node, String member, Set<String> proven, Set<String> tabled, List<String> errors) {
        if (!proven.contains(member) && !tabled.contains(member)) {
            errors.add(node.name + " links " + member + ", which neither the original JAR nor the verified table provides");
        }
    }
}

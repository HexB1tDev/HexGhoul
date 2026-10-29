import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.AnnotationNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FrameNode;
import jdk.internal.org.objectweb.asm.tree.InsnNode;
import jdk.internal.org.objectweb.asm.tree.LabelNode;
import jdk.internal.org.objectweb.asm.tree.LdcInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;
import jdk.internal.org.objectweb.asm.tree.VarInsnNode;
import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicValue;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicVerifier;

/**
 * 6.0.10: nothing in this mod may take a player off the server or the server down. The last step of the build, after the JAR is
 * patched, assembled and verified: every way Minecraft calls into the mod while the game runs is wrapped, whole, in a catch
 * that hands the error to {@code com.hex.kagune.control.Safety} (logged, and the game carries on) and returns:
 * <ul>
 *   <li>every event listener ({@code @SubscribeEvent}), the original's and the expansion's, server and client: a listener that
 *       throws used to fail the whole event, and a player tick that threw kicked the player ("Ticking player"), a world tick
 *       crashed the server;</li>
 *   <li>the kagune spike's tick and hits (an entity: run in the world's tick, where a throw crashes the server), the spike being
 *       removed after (so it can't fail again every tick), and its saves;</li>
 *   <li>the bleeding effect's tick (run in the entity's tick) and the spike's renderer;</li>
 *   <li>7.0.2: the ghoul NPC's AI step (removed after a failure, as the spike), its goals and its drops.</li>
 * </ul>
 * Each wrapped method keeps its own code untouched: a label before its first instruction, and after its last a handler (a full
 * frame: only {@code this}, the throwable on the stack) that calls {@code Safety.caught(throwable, "Class.method")} and returns;
 * its catch is the last in the method's table, so its own catches still come first. Every wrapped method is type-checked after,
 * every listener in the JAR is checked to be wrapped, and the changed classes are written to the report for the build's scope
 * check.
 */
public final class KaguneGuard {
    static final String SAFETY = "com/hex/kagune/control/Safety";
    static final String SUBSCRIBE = "Lnet/minecraftforge/eventbus/api/SubscribeEvent;";
    static final String SPIKE = "com/hex/kagune/entity/KaguneSpikeEntity";
    /** The original's callbacks Minecraft runs outside the event bus: after a failure the entity is removed (tick and hits). */
    static final String GHOUL = "com/hex/kagune/control/npc/GhoulNpc";
    static final Map<String, Set<String>> DISCARD = Map.of(SPIKE, Set.of("m_8119_", "m_5790_", "m_8060_"),
        // 7.0.2: the ghoul NPC's AI step (run in its tick): one that fails is removed rather than failing every tick.
        GHOUL, Set.of("m_8024_"));
    /** And the ones where logging is enough: the spike's saves, the bleeding effect's tick, the spike's renderer. */
    static final Map<String, Set<String>> CALLBACKS = Map.of(
        SPIKE, Set.of("m_7380_", "m_7378_"),
        "com/hex/kagune/effect/BleedingEffect", Set.of("m_6742_"),
        "com/hex/kagune/client/render/KaguneSpikeRenderer", Set.of("m_7392_"),
        // 7.0.2: the ghoul NPC's goals (set up as it is made) and its drops.
        GHOUL, Set.of("m_8099_", "m_7472_"));
    /** At least this many listeners exist (a sanity floor: the annotation not being found would guard nothing). */
    static final int LISTENER_FLOOR = 100;

    private KaguneGuard() {
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]);
        Path report = Path.of(args[1]);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (ZipEntry entry : java.util.Collections.list(zip.entries())) {
                try (InputStream in = zip.getInputStream(entry)) {
                    entries.put(entry.getName(), in.readAllBytes());
                }
            }
        }
        List<String> changed = new ArrayList<>();
        int listeners = 0;
        int callbacks = 0;
        Map<String, Set<String>> expected = new LinkedHashMap<>();
        DISCARD.forEach((owner, names) -> expected.computeIfAbsent(owner, k -> new java.util.HashSet<>()).addAll(names));
        CALLBACKS.forEach((owner, names) -> expected.computeIfAbsent(owner, k -> new java.util.HashSet<>()).addAll(names));
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith("com/hex/kagune/") || !name.endsWith(".class")) {
                continue;
            }
            ClassNode node = read(entry.getValue());
            List<String> wrapped = new ArrayList<>();
            for (MethodNode method : node.methods) {
                boolean listener = subscribes(method);
                boolean discard = DISCARD.getOrDefault(node.name, Set.of()).contains(method.name);
                boolean callback = CALLBACKS.getOrDefault(node.name, Set.of()).contains(method.name);
                if (!listener && !discard && !callback) {
                    continue;
                }
                if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0 || method.instructions.size() == 0) {
                    throw new IllegalStateException(node.name + "." + method.name + " has no body to guard");
                }
                if (Type.getReturnType(method.desc) != Type.VOID_TYPE || method.name.startsWith("<")) {
                    throw new IllegalStateException(node.name + "." + method.name + method.desc + " is not a void method");
                }
                if (guarded(method)) {
                    throw new IllegalStateException(node.name + "." + method.name + " is already guarded");
                }
                guard(node, method, discard, site(node.name, method.name));
                wrapped.add(method.name + method.desc);
                if (listener) {
                    listeners++;
                } else {
                    callbacks++;
                    Set<String> left = expected.get(node.name);
                    if (left != null) {
                        left.remove(method.name);
                    }
                }
            }
            if (wrapped.isEmpty()) {
                continue;
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            byte[] bytes = writer.toByteArray();
            // Type-check every wrapped method as written.
            ClassNode check = read(bytes);
            for (MethodNode method : check.methods) {
                if (wrapped.contains(method.name + method.desc)) {
                    if (!guarded(method)) {
                        throw new IllegalStateException(check.name + "." + method.name + " lost its guard");
                    }
                    new Analyzer<BasicValue>(new BasicVerifier()).analyze(check.name, method);
                }
            }
            entry.setValue(bytes);
            changed.add(name);
        }
        for (Map.Entry<String, Set<String>> left : expected.entrySet()) {
            if (!left.getValue().isEmpty()) {
                throw new IllegalStateException("Callbacks not found to guard in " + left.getKey() + ": " + left.getValue());
            }
        }
        if (listeners < LISTENER_FLOOR) {
            throw new IllegalStateException("Only " + listeners + " event listeners found to guard (expected at least " + LISTENER_FLOOR + ")");
        }
        // Every listener in the JAR is guarded, every one exactly once.
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            if (entry.getKey().startsWith("com/hex/kagune/") && entry.getKey().endsWith(".class")) {
                ClassNode node = read(entry.getValue());
                for (MethodNode method : node.methods) {
                    if (subscribes(method) && !guarded(method)) {
                        throw new IllegalStateException(node.name + "." + method.name + " is an unguarded listener");
                    }
                }
            }
        }
        Path written = jar.resolveSibling(jar.getFileName() + ".guarded");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(written))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        Files.move(written, jar, StandardCopyOption.REPLACE_EXISTING);
        java.util.Collections.sort(changed);
        Files.writeString(report, String.join("\n", changed) + "\n", StandardCharsets.UTF_8);
        System.out.println("Guarded " + listeners + " event listeners and " + callbacks + " callbacks in " + changed.size()
            + " classes: an error inside any of them is logged and the game carries on.");
    }

    static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    static boolean subscribes(MethodNode method) {
        if (method.visibleAnnotations == null) {
            return false;
        }
        for (AnnotationNode annotation : method.visibleAnnotations) {
            if (SUBSCRIBE.equals(annotation.desc)) {
                return true;
            }
        }
        return false;
    }

    /** "Class.method", the class's simple name (nested classes with their outer one). */
    static String site(String owner, String method) {
        return owner.substring(owner.lastIndexOf('/') + 1) + "." + method;
    }

    /** True if the method's last catch is a guard: a Throwable handler whose code calls {@code Safety.caught}. */
    static boolean guarded(MethodNode method) {
        if (method.tryCatchBlocks == null || method.tryCatchBlocks.isEmpty()) {
            return false;
        }
        TryCatchBlockNode last = method.tryCatchBlocks.get(method.tryCatchBlocks.size() - 1);
        if (!"java/lang/Throwable".equals(last.type)) {
            return false;
        }
        for (AbstractInsnNode at = last.handler; at != null; at = at.getNext()) {
            if (at instanceof MethodInsnNode call && call.owner.equals(SAFETY) && call.name.equals("caught")) {
                return last.start == firstLabel(method);
            }
            if (at.getOpcode() == Opcodes.RETURN) {
                return false;
            }
        }
        return false;
    }

    private static LabelNode firstLabel(MethodNode method) {
        AbstractInsnNode first = method.instructions.getFirst();
        return first instanceof LabelNode label ? label : null;
    }

    /** Wraps {@code method} whole: on any throwable, {@code Safety.caught(it, site)} (and {@code Safety.discard(this)}), then return. */
    static void guard(ClassNode owner, MethodNode method, boolean discard, String site) {
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        method.instructions.insert(start);
        method.instructions.add(end);
        method.instructions.add(handler);
        boolean instance = (method.access & Opcodes.ACC_STATIC) == 0;
        Object[] locals = instance ? new Object[] {owner.name} : new Object[0];
        method.instructions.add(new FrameNode(Opcodes.F_FULL, locals.length, locals, 1, new Object[] {"java/lang/Throwable"}));
        method.instructions.add(new LdcInsnNode(site));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SAFETY, "caught", "(Ljava/lang/Throwable;Ljava/lang/String;)V", false));
        if (discard) {
            if (!instance) {
                throw new IllegalStateException(owner.name + "." + method.name + " is static: no entity to remove");
            }
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SAFETY, "discard", "(Lnet/minecraft/world/entity/Entity;)V", false));
        }
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, "java/lang/Throwable"));
    }
}

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
import jdk.internal.org.objectweb.asm.tree.InsnList;
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
        GHOUL, Set.of("m_8024_"),
        // 4.1.103: a flesh creature's too (as its own catch did, for RuntimeExceptions only).
        "com/hex/kagune/control/npc/FleshThrall", Set.of("m_8024_"));
    /** And the ones where logging is enough: the spike's saves, the bleeding effect's tick, the spike's renderer. */
    static final Map<String, Set<String>> CALLBACKS = Map.of(
        SPIKE, Set.of("m_7380_", "m_7378_"),
        "com/hex/kagune/effect/BleedingEffect", Set.of("m_6742_"),
        "com/hex/kagune/client/render/KaguneSpikeRenderer", Set.of("m_7392_"),
        // 7.0.2: the ghoul NPC's goals (set up as it is made) and its drops.
        GHOUL, Set.of("m_8099_", "m_7472_"),
        // 4.1.103 (the owner: "making sure absolutely nothing in the mod fails"): the investigators' and the townspeople's brains (every
        // tick: a throw was a "Ticking entity" crash of the server) and drops; the companions' and the children's brains (their own catches
        // took RuntimeExceptions only) and a child's push. Logged and carried on: an official or the Reaper is never removed for a bug.
        "com/hex/kagune/control/npc/CcgAgent", Set.of("m_8024_", "m_7472_"),
        "com/hex/kagune/control/npc/HumanNpc", Set.of("m_8024_", "m_7472_"),
        "com/hex/kagune/control/npc/EtoNpc", Set.of("m_8024_"),
        "com/hex/kagune/control/npc/ChildNpc", Set.of("m_8024_", "m_6138_"));
    /** 4.1.103: and by name in every class of the mod: each screen's drawing (Screen.render; a throw there was a crash of the game). */
    static final Set<String> GUARDED_NAMES = Set.of("m_88315_");
    /** At least this many listeners exist (a sanity floor: the annotation not being found would guard nothing). */
    static final int LISTENER_FLOOR = 100;
    /**
     * 4.1.99: the mod's profiler ({@code com.hex.kagune.control.LagMeter}, read by {@code /kagune lag}). Every guarded method is timed too:
     * {@code LagMeter.enter(site)} right after the guard's first label, {@code LagMeter.exit(site)} before each of its returns and first in
     * its catch. And these, timed only (a catch of their own that tells the meter and throws on): our NPCs' brains (GhoulNpc's is guarded,
     * so timed with its guard) and the structures' searches and placing (on the world generation workers, and the server's thread for
     * /locate).
     */
    static final String METER = "com/hex/kagune/control/LagMeter";
    static final String STRUCTURES = "com/hex/kagune/control/ControlStructures$";
    static final Map<String, Set<String>> TIMED = Map.of(
        // 4.1.103: the NPCs' brains are guarded now (CALLBACKS, DISCARD), so timed with their guard.
        STRUCTURES + "DryJigsaw", Set.of("m_214086_", "m_262864_", "m_214110_"),
        STRUCTURES + "Site", Set.of("m_214086_", "m_262864_", "m_214110_"),
        STRUCTURES + "DryPlace", Set.of("m_214086_", "m_262864_", "m_214110_"));
    /** And by name in every class of the mod: renderers (EntityRenderer.render) and their layers (RenderLayer.render). */
    static final Set<String> TIMED_NAMES = Set.of("m_7392_", "m_6494_");
    /** And in the network package: each packet's encoder (one call a packet sent) and the work its handler gives the game's thread. */
    static final String NETWORK = "com/hex/kagune/control/network/";
    /** At least this many timed-only methods (a floor: a rule matching nothing would time nothing). */
    static final int TIMED_FLOOR = 100;
    /** The meter's names for the game's methods (SRG names read as Mojang's). */
    static final Map<String, String> NAMES = Map.ofEntries(Map.entry("m_8024_", "serverAiStep"), Map.entry("m_7392_", "render"),
        Map.entry("m_6494_", "renderLayer"), Map.entry("m_214086_", "findGenerationPoint"), Map.entry("m_262864_", "findValidGenerationPoint"),
        Map.entry("m_214110_", "afterPlace"), Map.entry("m_8119_", "tick"), Map.entry("m_5790_", "onHitEntity"), Map.entry("m_8060_", "onHitBlock"),
        Map.entry("m_7380_", "save"), Map.entry("m_7378_", "load"), Map.entry("m_6742_", "effectTick"), Map.entry("m_8099_", "registerGoals"),
        Map.entry("m_7472_", "dropLoot"));

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
        if (!entries.containsKey(METER + ".class")) {
            throw new IllegalStateException(METER + " is not in the JAR: nothing to time with");
        }
        List<String> changed = new ArrayList<>();
        int listeners = 0;
        int callbacks = 0;
        int timedOnly = 0;
        int screens = 0;
        Map<String, Set<String>> expected = new LinkedHashMap<>();
        DISCARD.forEach((owner, names) -> expected.computeIfAbsent(owner, k -> new java.util.HashSet<>()).addAll(names));
        CALLBACKS.forEach((owner, names) -> expected.computeIfAbsent(owner, k -> new java.util.HashSet<>()).addAll(names));
        Map<String, Set<String>> timedLeft = new LinkedHashMap<>();
        TIMED.forEach((owner, names) -> timedLeft.put(owner, new java.util.HashSet<>(names)));
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
                boolean callback = CALLBACKS.getOrDefault(node.name, Set.of()).contains(method.name) || guardedByName(node, method);
                if (!listener && !discard && !callback) {
                    if (timesOnly(node, method)) {
                        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0 || method.instructions.size() == 0) {
                            throw new IllegalStateException(node.name + "." + method.name + " has no body to time");
                        }
                        if (method.name.startsWith("<")) {
                            throw new IllegalStateException(node.name + "." + method.name + " is a constructor");
                        }
                        time(node, method, meterSite(node.name, method.name));
                        wrapped.add(method.name + method.desc);
                        timedOnly++;
                        Set<String> left = timedLeft.get(node.name);
                        if (left != null) {
                            left.remove(method.name);
                        }
                    }
                    continue;
                }
                if (TIMED.getOrDefault(node.name, Set.of()).contains(method.name)) {
                    throw new IllegalStateException(node.name + "." + method.name + " is guarded: timed with its guard, not in TIMED");
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
                guard(node, method, discard, site(node.name, method.name), meterSite(node.name, method.name));
                wrapped.add(method.name + method.desc);
                if (listener) {
                    listeners++;
                } else if (guardedByName(node, method)) {
                    screens++;
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
                    boolean guardable = subscribes(method) || DISCARD.getOrDefault(check.name, Set.of()).contains(method.name)
                        || CALLBACKS.getOrDefault(check.name, Set.of()).contains(method.name) || guardedByName(check, method);
                    if (guardable && !guarded(method)) {
                        throw new IllegalStateException(check.name + "." + method.name + " lost its guard");
                    }
                    if (!timed(method, meterSite(check.name, method.name))) {
                        throw new IllegalStateException(check.name + "." + method.name + " is not timed at its start and every way out");
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
        for (Map.Entry<String, Set<String>> left : timedLeft.entrySet()) {
            if (!left.getValue().isEmpty()) {
                throw new IllegalStateException("Methods not found to time in " + left.getKey() + ": " + left.getValue());
            }
        }
        if (screens < SCREEN_FLOOR) {
            throw new IllegalStateException("Only " + screens + " screens' drawing found to guard (expected at least " + SCREEN_FLOOR + ")");
        }
        if (timedOnly < TIMED_FLOOR) {
            throw new IllegalStateException("Only " + timedOnly + " methods found to time (expected at least " + TIMED_FLOOR + ")");
        }
        if (listeners < LISTENER_FLOOR) {
            throw new IllegalStateException("Only " + listeners + " event listeners found to guard (expected at least " + LISTENER_FLOOR + ")");
        }
        // Every listener in the JAR is guarded, every one exactly once.
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            if (entry.getKey().startsWith("com/hex/kagune/") && entry.getKey().endsWith(".class")) {
                ClassNode node = read(entry.getValue());
                for (MethodNode method : node.methods) {
                    if ((subscribes(method) || guardedByName(node, method)) && !guarded(method)) {
                        throw new IllegalStateException(node.name + "." + method.name + " is an unguarded listener or screen");
                    }
                    if ((subscribes(method) || timesOnly(node, method)) && !timed(method, meterSite(node.name, method.name))) {
                        throw new IllegalStateException(node.name + "." + method.name + " is an untimed listener or timed place");
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
            + " classes, and " + screens + " screens' drawing: an error inside any of them is logged and the game carries on. Timed them and " + timedOnly
            + " more (NPC brains, renderers, structures, packets) for /kagune lag.");
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

    /** The meter's name for a place: "Class.method", the game's methods by their Mojang names, a packet's handler work as "handle". */
    static String meterSite(String owner, String method) {
        String name = method.startsWith("lambda$handle$") ? "handle" : NAMES.getOrDefault(method, method);
        return site(owner, name);
    }

    /** 4.1.103: a screen's drawing (by name, {@link #GUARDED_NAMES}): every class of the mod's, not a bridge, with a body, void. */
    static boolean guardedByName(ClassNode owner, MethodNode method) {
        return owner.name.startsWith("com/hex/kagune/") && GUARDED_NAMES.contains(method.name)
            && (method.access & (Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_ABSTRACT)) == 0
            && Type.getReturnType(method.desc) == Type.VOID_TYPE;
    }

    /** At least this many screens' drawing is guarded (a floor: the name not found would guard nothing). */
    static final int SCREEN_FLOOR = 10;

    /** 4.1.99: a method timed but not guarded (the {@link #TIMED} ones, renderers and layers, packets' encoders and handlers' work). */
    static boolean timesOnly(ClassNode owner, MethodNode method) {
        if (!owner.name.startsWith("com/hex/kagune/") || (method.access & (Opcodes.ACC_BRIDGE | Opcodes.ACC_ABSTRACT)) != 0) {
            return false;
        }
        if (TIMED.getOrDefault(owner.name, Set.of()).contains(method.name)) {
            return true;
        }
        if (TIMED_NAMES.contains(method.name) && (method.access & Opcodes.ACC_SYNTHETIC) == 0) {
            return true;
        }
        if (!owner.name.startsWith(NETWORK) || Type.getReturnType(method.desc) != Type.VOID_TYPE) {
            return false;
        }
        if (method.name.equals("encode") && (method.access & Opcodes.ACC_STATIC) != 0) {
            return true;
        }
        // The innermost work a handler gives the game's thread: a void lambda that makes no lambda of its own (the one inside the
        // DistExecutor's supplier, or the enqueued work itself), so no packet is counted twice.
        if (method.name.startsWith("lambda$handle$")) {
            for (AbstractInsnNode at = method.instructions.getFirst(); at != null; at = at.getNext()) {
                if (at.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /** True if the method tells the meter at its start (after its first label) and before every return. */
    static boolean timed(MethodNode method, String site) {
        LabelNode first = firstLabel(method);
        if (first == null || !meterCall(first.getNext(), "enter", site)) {
            return false;
        }
        for (AbstractInsnNode at = method.instructions.getFirst(); at != null; at = at.getNext()) {
            if (at.getOpcode() >= Opcodes.IRETURN && at.getOpcode() <= Opcodes.RETURN) {
                AbstractInsnNode before = at.getPrevious();
                // A guard's own return comes after Safety.caught (and Safety.discard): its exit is first in its catch.
                boolean guardReturn = before instanceof MethodInsnNode call && call.owner.equals(SAFETY);
                if (!guardReturn && !(before != null && meterCall(before.getPrevious(), "exit", site))) {
                    return false;
                }
            }
        }
        if (method.tryCatchBlocks == null || method.tryCatchBlocks.isEmpty()) {
            return false;
        }
        TryCatchBlockNode last = method.tryCatchBlocks.get(method.tryCatchBlocks.size() - 1);
        AbstractInsnNode handler = last.handler.getNext();
        while (handler instanceof FrameNode) {
            handler = handler.getNext();
        }
        return last.start == first && meterCall(handler, "exit", site);
    }

    /** True if {@code at} is {@code ldc site} followed by {@code LagMeter.<name>(String)}. */
    private static boolean meterCall(AbstractInsnNode at, String name, String site) {
        return at instanceof LdcInsnNode ldc && site.equals(ldc.cst) && at.getNext() instanceof MethodInsnNode call
            && call.owner.equals(METER) && call.name.equals(name) && call.desc.equals("(Ljava/lang/String;)V");
    }

    private static InsnList meter(String name, String site) {
        InsnList out = new InsnList();
        out.add(new LdcInsnNode(site));
        out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, METER, name, "(Ljava/lang/String;)V", false));
        return out;
    }

    /** {@code LagMeter.exit(site)} before every return of the method's own code. */
    private static void exits(MethodNode method, String site) {
        List<AbstractInsnNode> returns = new ArrayList<>();
        for (AbstractInsnNode at = method.instructions.getFirst(); at != null; at = at.getNext()) {
            if (at.getOpcode() >= Opcodes.IRETURN && at.getOpcode() <= Opcodes.RETURN) {
                returns.add(at);
            }
        }
        for (AbstractInsnNode at : returns) {
            method.instructions.insertBefore(at, meter("exit", site));
        }
    }

    /** Times {@code method} without guarding it: enter at its start, exit at each return, and a catch that exits and throws on. */
    static void time(ClassNode owner, MethodNode method, String site) {
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        exits(method, site);
        method.instructions.insert(start);
        method.instructions.insert(start, meter("enter", site));
        method.instructions.add(end);
        method.instructions.add(handler);
        boolean instance = (method.access & Opcodes.ACC_STATIC) == 0;
        Object[] locals = instance ? new Object[] {owner.name} : new Object[0];
        method.instructions.add(new FrameNode(Opcodes.F_FULL, locals.length, locals, 1, new Object[] {"java/lang/Throwable"}));
        method.instructions.add(meter("exit", site));
        method.instructions.add(new InsnNode(Opcodes.ATHROW));
        method.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, "java/lang/Throwable"));
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

    /**
     * Wraps {@code method} whole: on any throwable, {@code Safety.caught(it, site)} (and {@code Safety.discard(this)}), then return. 4.1.99:
     * and times it ({@code meterSite}): enter after the first label, exit before each of its own returns and first in the catch.
     */
    static void guard(ClassNode owner, MethodNode method, boolean discard, String site, String meterSite) {
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        exits(method, meterSite);
        method.instructions.insert(start);
        method.instructions.insert(start, meter("enter", meterSite));
        method.instructions.add(end);
        method.instructions.add(handler);
        boolean instance = (method.access & Opcodes.ACC_STATIC) == 0;
        Object[] locals = instance ? new Object[] {owner.name} : new Object[0];
        method.instructions.add(new FrameNode(Opcodes.F_FULL, locals.length, locals, 1, new Object[] {"java/lang/Throwable"}));
        method.instructions.add(meter("exit", meterSite));
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

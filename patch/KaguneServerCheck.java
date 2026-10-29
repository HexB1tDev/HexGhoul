import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.AnnotationNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.InvokeDynamicInsnNode;
import jdk.internal.org.objectweb.asm.tree.LdcInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.MultiANewArrayInsnNode;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;
import jdk.internal.org.objectweb.asm.tree.TypeInsnNode;
import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import jdk.internal.org.objectweb.asm.tree.analysis.AnalyzerException;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicValue;
import jdk.internal.org.objectweb.asm.tree.analysis.SimpleVerifier;

/**
 * 5.0.13: proves the built JAR loads and runs on a dedicated server, where no client class exists (Minecraft's
 * {@code net.minecraft.client}, Blaze3D, Forge's client package, and the client-only mods PlayerAnimator, bendy-lib and Photon,
 * which the server never has). It walks what a server actually runs, from what Forge starts on it:
 * <ul>
 *   <li>the {@code @Mod} class's constructor (and the control bootstrap and network it calls);</li>
 *   <li>every {@code @Mod.EventBusSubscriber} Forge registers on a server (no {@code value}, or one naming
 *       {@code DEDICATED_SERVER}): the class is initialised and its static {@code @SubscribeEvent} methods registered;</li>
 *   <li>every method those reach: calls (with overrides), field accesses, {@code new}, casts, class literals, and the bodies of
 *       lambdas and method references, except the client half of {@code DistExecutor} (the only way the code runs client code
 *       from a class both sides load).</li>
 * </ul>
 * It fails the build if, on that path,
 * <ol>
 *   <li>any instruction touches a client-only class or member (a {@code NoClassDefFoundError} on the server);</li>
 *   <li>a class the server loads extends, implements or is annotated {@code @OnlyIn(Dist.CLIENT)};</li>
 *   <li>a public method of a server-registered subscriber names a client-only type (Forge's {@code EventBus.register} reflects over
 *       every public method, which resolves those types);</li>
 *   <li>the JVM's verifier would have to load a client-only type to check any method (run or not) of a class the server loads:
 *       a value of one class type passed, stored or returned where another class type is expected (checked with ASM's
 *       verifier over the real class hierarchy, recording every type it must resolve).</li>
 * </ol>
 * Usage: {@code KaguneServerCheck <mod.jar> <stubs-dir>}.
 */
public final class KaguneServerCheck implements Opcodes {
    private static final String[] CLIENT_ONLY = {
        "net/minecraft/client/", "com/mojang/blaze3d/", "net/minecraftforge/client/", "dev/kosmx/", "io/github/kosmx/", "com/lowdragmc/",
    };
    private static final String SUBSCRIBER = "Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber;";
    private static final String MOD = "Lnet/minecraftforge/fml/common/Mod;";
    private static final String SUBSCRIBE = "Lnet/minecraftforge/eventbus/api/SubscribeEvent;";
    private static final String ONLY_IN = "Lnet/minecraftforge/api/distmarker/OnlyIn;";
    private static final String DIST = "net/minecraftforge/api/distmarker/Dist";
    private static final String DIST_EXECUTOR = "net/minecraftforge/fml/DistExecutor";

    private final Map<String, ClassNode> jar = new HashMap<>();
    private final Map<String, ClassNode> api = new HashMap<>();
    private final Set<String> loaded = new TreeSet<>();
    private final Set<String> reachable = new HashSet<>();
    private final Deque<String[]> work = new ArrayDeque<>();
    private final Set<String> problems = new TreeSet<>();
    private final Set<String> subscribers = new TreeSet<>();
    private int skippedClientLambdas;

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_FRAMES);
        return node;
    }

    /** The class (or array element class) a descriptor or internal name stands for, or null for a primitive. */
    private static String element(String type) {
        String t = type;
        while (t.startsWith("[")) {
            t = t.substring(1);
        }
        if (t.startsWith("L") && t.endsWith(";")) {
            return t.substring(1, t.length() - 1);
        }
        return t.length() == 1 ? null : t;
    }

    private static boolean annotated(List<AnnotationNode> annotations, String desc) {
        return annotations != null && annotations.stream().anyMatch(a -> a.desc.equals(desc));
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values == null) {
            return null;
        }
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) {
                return annotation.values.get(i + 1);
            }
        }
        return null;
    }

    private static boolean onlyInClient(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(ONLY_IN) && value(annotation, "value") instanceof String[] dist && "CLIENT".equals(dist[1])) {
                return true;
            }
        }
        return false;
    }

    private boolean clientOnly(String type) {
        if (type == null) {
            return false;
        }
        for (String prefix : CLIENT_ONLY) {
            if (type.startsWith(prefix)) {
                return true;
            }
        }
        ClassNode node = this.jar.get(type);
        return node != null && (onlyInClient(node.visibleAnnotations) || onlyInClient(node.invisibleAnnotations));
    }

    /** A class (from the JAR, the API stubs or the JDK) for the hierarchy, or null if unknown. */
    private ClassNode hierarchy(String type) {
        ClassNode node = this.jar.get(type);
        if (node != null) {
            return node;
        }
        if (this.api.containsKey(type)) {
            return this.api.get(type);
        }
        ClassNode found = null;
        try (InputStream in = ClassLoader.getSystemResourceAsStream(type + ".class")) {
            if (in != null) {
                found = read(in.readAllBytes());
            }
        } catch (Exception unreadable) {
            found = null;
        }
        this.api.put(type, found);
        return found;
    }

    private void problem(String text) {
        this.problems.add(text);
    }

    /** The server loads (and initialises) {@code type}. */
    private void load(String type, String why) {
        String t = element(type);
        if (t == null) {
            return;
        }
        if (clientOnly(t)) {
            problem(why + ": loads client-only " + t);
            return;
        }
        ClassNode node = this.jar.get(t);
        if (node == null || !this.loaded.add(t)) {
            return;
        }
        if (node.superName != null) {
            load(node.superName, t + " extends");
        }
        for (String face : node.interfaces) {
            load(face, t + " implements");
        }
        reach(t, "<clinit>", "()V", t + " initialised");
    }

    private MethodNode declared(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }
        return null;
    }

    private boolean subtype(String type, String of) {
        for (String t = type; t != null; ) {
            if (t.equals(of)) {
                return true;
            }
            ClassNode node = hierarchy(t);
            if (node == null) {
                return false;
            }
            for (String face : node.interfaces) {
                if (subtype(face, of)) {
                    return true;
                }
            }
            t = node.superName;
        }
        return false;
    }

    /** True or false when the hierarchy proves it, null when a class on the way is unknown. */
    private Boolean knownSubtype(String type, String of) {
        for (String t = type; t != null; ) {
            if (t.equals(of)) {
                return true;
            }
            ClassNode node = hierarchy(t);
            if (node == null) {
                return null;
            }
            t = node.superName;
        }
        return false;
    }

    /** {@code owner.name desc} runs on the server (and, for an instance method, every override of it in the JAR). */
    private void reach(String owner, String name, String desc, String why) {
        String o = element(owner);
        if (o == null) {
            return;
        }
        if (clientOnly(o)) {
            problem(why + ": calls client-only " + o + "." + name + desc);
            return;
        }
        for (String t = o; t != null; ) {
            ClassNode node = this.jar.get(t);
            if (node == null) {
                break;
            }
            MethodNode method = declared(node, name, desc);
            if (method != null) {
                enqueue(node, method, why);
                break;
            }
            t = node.superName;
        }
        if (!name.startsWith("<")) {
            for (ClassNode node : this.jar.values()) {
                MethodNode method = declared(node, name, desc);
                if (method != null && (method.access & ACC_STATIC) == 0 && !node.name.equals(o) && subtype(node.name, o)) {
                    enqueue(node, method, why + " (override in " + node.name + ")");
                }
            }
        }
    }

    private void enqueue(ClassNode node, MethodNode method, String why) {
        if (this.reachable.add(node.name + "." + method.name + method.desc)) {
            load(node.name, why);
            this.work.add(new String[] {node.name, method.name, method.desc});
        }
    }

    private static AbstractInsnNode next(AbstractInsnNode insn) {
        AbstractInsnNode n = insn.getNext();
        while (n != null && n.getOpcode() < 0) {
            n = n.getNext();
        }
        return n;
    }

    /** True for a lambda handed straight to {@code DistExecutor} as its client-side code. */
    private static boolean clientLambda(InvokeDynamicInsnNode indy) {
        AbstractInsnNode after = next(indy);
        if (after instanceof InvokeDynamicInsnNode second && next(second) instanceof MethodInsnNode call && call.owner.equals(DIST_EXECUTOR)
            && call.name.endsWith("ForDist")) {
            return true; // runForDist(client, server): the first supplier is the client's
        }
        if (!(after instanceof MethodInsnNode call) || !call.owner.equals(DIST_EXECUTOR) || call.name.endsWith("ForDist")) {
            return false;
        }
        int budget = 8;
        for (AbstractInsnNode p = indy.getPrevious(); p != null && budget-- > 0; p = p.getPrevious()) {
            if (p instanceof FieldInsnNode field && field.owner.equals(DIST)) {
                return field.name.equals("CLIENT");
            }
        }
        return false;
    }

    private void walk(String owner, String name, String desc) {
        ClassNode node = this.jar.get(owner);
        MethodNode method = declared(node, name, desc);
        String where = owner + "." + name + desc;
        for (TryCatchBlockNode block : method.tryCatchBlocks) {
            if (block.type != null) {
                load(block.type, where + " catches");
            }
        }
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof MethodInsnNode call) {
                load(call.owner, where);
                reach(call.owner, call.name, call.desc, where);
                if (call.getOpcode() == INVOKESPECIAL && call.name.equals("<init>")) {
                    instantiated(call.owner, where);
                }
            } else if (insn instanceof FieldInsnNode field) {
                load(field.owner, where);
            } else if (insn instanceof TypeInsnNode type) {
                load(type.desc, where);
            } else if (insn instanceof MultiANewArrayInsnNode array) {
                load(array.desc, where);
            } else if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type type && type.getSort() == Type.OBJECT) {
                load(type.getInternalName(), where);
                registered(type.getInternalName(), where + " (class literal)");
            } else if (insn instanceof InvokeDynamicInsnNode indy && indy.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")
                && indy.bsmArgs.length > 1 && indy.bsmArgs[1] instanceof Handle target) {
                if (clientLambda(indy)) {
                    this.skippedClientLambdas++;
                    continue;
                }
                load(target.getOwner(), where);
                reach(target.getOwner(), target.getName(), target.getDesc(), where + " (lambda)");
            }
        }
    }

    /** {@code new type()}: anything outside the JAR may call any of its instance methods (listeners, layers, records). */
    private void instantiated(String type, String why) {
        ClassNode node = this.jar.get(type);
        for (; node != null; node = this.jar.get(node.superName)) {
            for (MethodNode method : node.methods) {
                if ((method.access & (ACC_STATIC | ACC_PRIVATE)) == 0 && !method.name.startsWith("<")) {
                    enqueue(node, method, why + " (instance of " + type + ")");
                }
            }
        }
    }

    /** A class handed to an event bus: Forge reflects over its public methods and registers its static listeners. */
    private void registered(String type, String why) {
        ClassNode node = this.jar.get(type);
        if (node == null || !this.subscribers.add(type)) {
            return;
        }
        load(type, why);
        for (ClassNode c = node; c != null; c = this.jar.get(c.superName)) {
            for (MethodNode method : c.methods) {
                if ((method.access & ACC_PUBLIC) == 0 || method.name.startsWith("<")) {
                    continue;
                }
                Type signature = Type.getMethodType(method.desc);
                List<Type> types = new ArrayList<>(List.of(signature.getArgumentTypes()));
                types.add(signature.getReturnType());
                for (Type t : types) {
                    String e = t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY ? element(t.getDescriptor()) : null;
                    if (clientOnly(e)) {
                        problem(type + ": public " + method.name + method.desc + " names client-only " + e
                            + " (Forge's EventBus.register resolves every public method's types)");
                    }
                }
                if ((method.access & ACC_STATIC) != 0 && annotated(method.visibleAnnotations, SUBSCRIBE)) {
                    enqueue(c, method, type + " listener");
                }
            }
        }
    }

    /** ASM's verifier on the JAR's own hierarchy, recording every class type it has to resolve, as HotSpot's verifier would. */
    private final class Resolving extends SimpleVerifier {
        final Set<String> resolved = new TreeSet<>();

        Resolving() {
            super(ASM8, null, null, null, false);
        }

        private void resolve(Type type) {
            if (type.getSort() == Type.OBJECT) {
                this.resolved.add(type.getInternalName());
            }
        }

        @Override
        protected boolean isInterface(Type type) {
            ClassNode node = hierarchy(type.getInternalName());
            return node != null && (node.access & ACC_INTERFACE) != 0;
        }

        @Override
        protected Type getSuperClass(Type type) {
            resolve(type);
            ClassNode node = hierarchy(type.getInternalName());
            return node == null || node.superName == null ? Type.getObjectType("java/lang/Object") : Type.getObjectType(node.superName);
        }

        @Override
        protected boolean isSubTypeOf(BasicValue value, BasicValue expected) {
            Type to = expected.getType();
            Type from = value.getType();
            if (to != null && from != null && (to.getSort() == Type.OBJECT || to.getSort() == Type.ARRAY)
                && (from.getSort() == Type.OBJECT || from.getSort() == Type.ARRAY)) {
                if (!from.getInternalName().equals("null")) {
                    isAssignableFrom(to, from);
                }
                return true; // KaguneVerify type-checks; this only records what must be resolved
            }
            return super.isSubTypeOf(value, expected);
        }

        @Override
        protected boolean isAssignableFrom(Type to, Type from) {
            if (to.equals(from) || to.getDescriptor().equals("Ljava/lang/Object;") || from.getInternalName().equals("null")) {
                return true;
            }
            if (to.getSort() == Type.ARRAY || from.getSort() == Type.ARRAY) {
                if (to.getSort() == Type.ARRAY && from.getSort() == Type.ARRAY) {
                    Type a = Type.getType(to.getDescriptor().substring(1));
                    Type b = Type.getType(from.getDescriptor().substring(1));
                    return a.getSort() == Type.OBJECT || a.getSort() == Type.ARRAY ? isAssignableFrom(a, b) : a.equals(b);
                }
                return to.getSort() == Type.OBJECT; // an array goes into Object, Cloneable or Serializable only
            }
            resolve(to);
            if (isInterface(to)) {
                return true; // the verifier treats interfaces as Object
            }
            resolve(from);
            Boolean known = knownSubtype(from.getInternalName(), to.getInternalName());
            return known == null || known;
        }

        @Override
        protected Class<?> getClass(Type type) {
            throw new IllegalStateException("never load: " + type);
        }
    }

    private void verify(String type) {
        ClassNode node = this.jar.get(type);
        for (MethodNode method : node.methods) {
            if (method.instructions.size() == 0) {
                continue;
            }
            Resolving verifier = new Resolving();
            try {
                new Analyzer<BasicValue>(verifier).analyze(node.name, method);
            } catch (AnalyzerException | RuntimeException unanalysable) {
                // KaguneVerify already type-checks every method; an analysis gap here must not hide a client type either.
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn instanceof FieldInsnNode field && clientOnly(element(Type.getType(field.desc).getDescriptor()))) {
                        problem(type + "." + method.name + method.desc + ": not analysable (" + unanalysable.getMessage() + ") and touches "
                            + field.desc);
                    }
                }
                continue;
            }
            for (String t : verifier.resolved) {
                if (clientOnly(t)) {
                    problem(type + "." + method.name + method.desc + ": the verifier must load client-only " + t
                        + " (a client type is assigned to another class type)");
                }
            }
        }
    }

    private static Map<String, Object> dists(AnnotationNode annotation) {
        Map<String, Object> out = new HashMap<>();
        if (annotation.values != null) {
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                out.put((String)annotation.values.get(i), annotation.values.get(i + 1));
            }
        }
        return out;
    }

    /** True when Forge registers this subscriber on a dedicated server ({@code value} absent, or naming DEDICATED_SERVER). */
    private static boolean onServer(AnnotationNode subscriber) {
        Object value = dists(subscriber).get("value");
        if (!(value instanceof List<?> sides)) {
            return true;
        }
        for (Object side : sides) {
            if (side instanceof String[] e && "DEDICATED_SERVER".equals(e[1])) {
                return true;
            }
        }
        return false;
    }

    private void run(Path modJar, Path stubs) throws Exception {
        try (ZipFile zip = new ZipFile(modJar.toFile())) {
            for (ZipEntry entry : java.util.Collections.list(zip.entries())) {
                if (entry.getName().endsWith(".class")) {
                    ClassNode node = read(zip.getInputStream(entry).readAllBytes());
                    this.jar.put(node.name, node);
                }
            }
        }
        try (Stream<Path> files = Files.walk(stubs)) {
            for (Path file : (Iterable<Path>)files.filter(f -> f.toString().endsWith(".class"))::iterator) {
                ClassNode node = read(Files.readAllBytes(file));
                this.api.put(node.name, node);
            }
        }
        int mods = 0;
        int serverSubscribers = 0;
        int clientSubscribers = 0;
        for (ClassNode node : new ArrayList<>(this.jar.values())) {
            if (annotated(node.visibleAnnotations, MOD)) {
                mods++;
                load(node.name, "@Mod " + node.name);
                for (MethodNode method : node.methods) {
                    if (method.name.equals("<init>")) {
                        enqueue(node, method, "@Mod constructor");
                    }
                }
            }
            if (node.visibleAnnotations != null) {
                for (AnnotationNode annotation : node.visibleAnnotations) {
                    if (annotation.desc.equals(SUBSCRIBER)) {
                        if (onServer(annotation)) {
                            serverSubscribers++;
                            registered(node.name, "@EventBusSubscriber " + node.name);
                        } else {
                            clientSubscribers++;
                        }
                    }
                }
            }
        }
        if (mods != 1) {
            throw new AssertionError("expected one @Mod class, found " + mods);
        }
        while (!this.work.isEmpty()) {
            String[] next = this.work.poll();
            walk(next[0], next[1], next[2]);
        }
        for (String type : this.loaded) {
            verify(type);
        }
        if (!this.problems.isEmpty()) {
            this.problems.stream().limit(60).forEach(p -> System.err.println("SERVER: " + p));
            throw new AssertionError(this.problems.size() + " dedicated-server problems: the JAR would not load or run on a server");
        }
        long expansion = this.loaded.stream().filter(t -> t.startsWith("com/hex/kagune/control/")).count();
        System.out.println("Verified dedicated server: " + this.loaded.size() + " classes load on a server (" + expansion + " of the expansion), "
            + this.reachable.size() + " methods reachable from the @Mod constructor and " + serverSubscribers + " server event subscribers ("
            + clientSubscribers + " client-only ones are never registered there, " + this.skippedClientLambdas + " DistExecutor client lambdas "
            + "never run there); no client class is touched, named in a listener's signature, or needed by the verifier.");
    }

    public static void main(String[] args) throws Exception {
        new KaguneServerCheck().run(Path.of(args[0]), Path.of(args[1]));
    }
}

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import jdk.internal.org.objectweb.asm.tree.FieldNode;
import jdk.internal.org.objectweb.asm.tree.FrameNode;
import jdk.internal.org.objectweb.asm.tree.InvokeDynamicInsnNode;
import jdk.internal.org.objectweb.asm.tree.LabelNode;
import jdk.internal.org.objectweb.asm.tree.LdcInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.MultiANewArrayInsnNode;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;
import jdk.internal.org.objectweb.asm.tree.TypeInsnNode;
import jdk.internal.org.objectweb.asm.tree.VarInsnNode;
import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import jdk.internal.org.objectweb.asm.tree.analysis.AnalyzerException;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicVerifier;
import jdk.internal.org.objectweb.asm.tree.analysis.SimpleVerifier;

/**
 * Assert shipped bytecode contracts without downloading Minecraft dependencies.
 *
 * <ol>
 *   <li>The 1.0.2 grapple/particle contracts (unchanged).</li>
 *   <li>The 1.1.0 hooks: every trampoline/redirect exists exactly once and points at the expansion.</li>
 *   <li>Type-checking analysis of every new or changed method against a class hierarchy built from the
 *       output JAR plus the compile-only stubs.</li>
 *   <li>A reference audit: every Minecraft/Forge/library member or class touched by new or changed code
 *       must already be used by the original 1.0.0 JAR (proven at runtime) or be listed in
 *       {@code verify/api-allowlist.txt} with its verification source. Internal references must resolve.</li>
 * </ol>
 * Usage: KaguneVerify output.jar input.jar api-allowlist.txt stub-classes-dir
 */
public final class KaguneVerify implements Opcodes {
    private static final String CONTROLLER = "com/hex/kagune/gameplay/KaguneServerController";
    private static final String TENDRIL = "com/hex/kagune/client/render/TendrilChain";
    private static final String MANIFEST = "com/hex/kagune/client/KaguneManifestationEffects";
    private static final String MOD = "com/hex/kagune/KaguneMod";
    private static final String PROFILE = "com/hex/kagune/data/KaguneProfile";
    private static final String WORLD_RENDERER = "com/hex/kagune/client/render/KaguneWorldRenderer";
    private static final String AMBIENT = "com/hex/kagune/client/KaguneAmbientSounds";
    private static final String SPIKE_RENDERER = "com/hex/kagune/client/render/KaguneSpikeRenderer";
    private static final String EXPANSION = "com/hex/kagune/control/";
    private static final Set<String> CHANGED_CLASSES = Set.of(
        CONTROLLER, "com/hex/kagune/gameplay/KaguneRuntime$State", "com/hex/kagune/client/KaguneAuraEffects", TENDRIL, MANIFEST, MOD,
        PROFILE, WORLD_RENDERER, AMBIENT, SPIKE_RENDERER, "com/hex/kagune/gameplay/KaguneForm", "com/hex/kagune/gameplay/KaguneForm$1",
        "com/hex/kagune/gameplay/KaguneEvents$1", "com/hex/kagune/client/screen/KaguneScreen", "com/hex/kagune/gameplay/MutationNode",
        "com/hex/kagune/client/KaguneEnergyHud");

    private static ClassNode read(ZipFile jar, String name) throws IOException {
        ZipEntry entry = jar.getEntry(name + ".class");
        if (entry == null) throw new AssertionError("missing class " + name);
        ClassNode node = new ClassNode();
        new ClassReader(jar.getInputStream(entry).readAllBytes()).accept(node, 0);
        return node;
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(m -> m.name.equals(name)).findFirst()
            .orElseThrow(() -> new AssertionError("missing method " + node.name + "." + name));
    }

    private static MethodNode method(ClassNode node, String name, String desc) {
        return node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).findFirst()
            .orElseThrow(() -> new AssertionError("missing method " + node.name + "." + name + desc));
    }

    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }

    private static int calls(MethodNode method, String target) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray())
            if (instruction instanceof MethodInsnNode call && call.name.equals(target)) count++;
        return count;
    }

    private static int calls(MethodNode method, String owner, String target) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray())
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(target)) count++;
        return count;
    }

    private static boolean field(MethodNode method, String target) {
        for (AbstractInsnNode instruction : method.instructions.toArray())
            if (instruction instanceof FieldInsnNode access && access.name.equals(target)) return true;
        return false;
    }

    private static boolean literal(MethodNode method, Object target) {
        for (AbstractInsnNode instruction : method.instructions.toArray())
            if (instruction instanceof LdcInsnNode constant && target.equals(constant.cst)) return true;
        return false;
    }

    private static boolean isPublic(MethodNode method) {
        return (method.access & ACC_PUBLIC) != 0;
    }

    // ------------------------------------------------------------------ 1.0.2 contracts

    private static void legacyChecks(ZipFile jar) throws Exception {
        ClassNode controller = read(jar, CONTROLLER);
        ClassNode state = read(jar, "com/hex/kagune/gameplay/KaguneRuntime$State");
        ClassNode aura = read(jar, "com/hex/kagune/client/KaguneAuraEffects");
        // 1.2.0: the original grapple body lives in slingshotLegacy behind the Ukaku gate (checked in hookChecks).
        MethodNode grapple = method(controller, "slingshotLegacy");
        MethodNode launch = method(controller, "launchSlingshot");
        MethodNode free = method(controller, "freeGrapple");
        String flight = "com/hex/kagune/control/server/RinkakuSlingshot";
        check(state.fields.stream().anyMatch(f -> f.name.equals("slingshotCooldownUntil") && f.desc.equals("J")), "missing dedicated cooldown");
        check(field(grapple, "slingshotCooldownUntil") && !field(grapple, "inputCooldownUntil"), "grapple uses global cooldown");
        check(field(launch, "slingshotCooldownUntil") && !field(launch, "inputCooldownUntil"), "launch uses global cooldown");
        check(literal(launch, 10L) && !literal(launch, 12L), "launch cooldown should be 10 ticks");
        check(literal(launch, 8.0) && literal(launch, 0.23) && literal(launch, 4.4), "launch trajectory unchanged");
        check(calls(grapple, "freeGrapple") == 1 && calls(launch, "freeGrapple") == 1, "grapple cost unchanged");
        // 3.0.0: the launch hands the flight onto the grabbed block to the expansion; a new grapple ends one under way.
        check(calls(launch, flight, "launch") == 1 && calls(launch, "m_20256_") == 0, "the launch must fly him onto the grabbed block");
        check(calls(grapple, flight, "cancel") == 1, "a new grapple must end the flight under way");
        check(controller.methods.stream().noneMatch(m -> m.name.equals("tickTreeVault") || m.name.equals("startTreeVault")),
                "the old tree vault is gone");
        check(calls(grapple, "useEnergy") == 0 && calls(launch, "useEnergy") == 0, "grapple spends energy");
        check(free.instructions.getFirst().getOpcode() == ICONST_1 && free.instructions.getLast().getOpcode() == IRETURN, "free helper should always succeed");
        MethodNode spawn = method(aura, "spawnFor");
        check(spawn.instructions.size() == 1 && spawn.instructions.getFirst().getOpcode() == RETURN, "limb particles still active");
        check(jar.getEntry("com/hex/kagune/client/KaguneManifestationEffects.class") != null,
                "back-origin effects class missing");
        for (ClassNode type : new ClassNode[] {controller, state, aura}) {
            for (MethodNode body : type.methods) {
                new Analyzer<>(new BasicVerifier()).analyze(type.name, body);
            }
        }
    }

    // ------------------------------------------------------------------ 1.1.0 hooks

    /** A trampoline is one straight-line forward of every argument to {@code owner.target}. */
    private static void checkTrampoline(ClassNode node, String name, String owner, String target) {
        List<MethodNode> named = node.methods.stream().filter(m -> m.name.equals(name)).toList();
        check(named.size() == 1, node.name + "." + name + " must be a single trampoline");
        MethodNode bridge = named.get(0);
        MethodNode legacy = method(node, name + "Legacy", bridge.desc);
        check(isPublic(legacy), name + "Legacy must be public");
        check(calls(bridge, owner, target) == 1 && bridge.instructions.size() == Type.getArgumentTypes(bridge.desc).length + 2,
            node.name + "." + name + " must only forward to " + owner + "." + target);
        for (AbstractInsnNode instruction : bridge.instructions.toArray())
            check(instruction.getType() != AbstractInsnNode.JUMP_INSN && !(instruction instanceof FrameNode), name + " trampoline must be straight-line");
    }

    /** Inserted argument loads must sit directly in front of their redirected call, with no frame/label in between. */
    private static void checkContiguous(MethodNode method, String owner, String name, int insertedLoads) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) {
                AbstractInsnNode cursor = call.getPrevious();
                for (int i = 0; i < insertedLoads; i++) {
                    check(cursor instanceof VarInsnNode || cursor instanceof FieldInsnNode,
                        method.name + ": redirect to " + name + " is not preceded by its inserted loads");
                    cursor = cursor.getPrevious();
                }
            }
        }
    }

    /** Path of the original input JAR (set from the command line before the hook checks run). */
    private static String ORIGINAL;

    private static void hookChecks(ZipFile jar) throws Exception {
        ClassNode controller = read(jar, CONTROLLER);
        checkTrampoline(controller, "startSwipe", EXPANSION + "server/KaguneCombat", "startSwipe");
        checkTrampoline(controller, "toggle", EXPANSION + "server/PathServer", "toggle");
        MethodNode onAction = method(controller, "onAction", "(Lnet/minecraft/server/level/ServerPlayer;Lcom/hex/kagune/gameplay/KaguneAction;I)V");
        check(calls(onAction, EXPANSION + "DragonLock", "locked") == 1, "the kagune actions must ask the Dragon's lock first (1.0.4)");
        check(calls(onAction, CONTROLLER, "toggle") == 1 && calls(onAction, CONTROLLER, "toggleLegacy") == 0,
            "TOGGLE must reach the original only through the path gate");
        // 1.2.0: Rapid Strike goes through the Ukaku filter (the Wing Vortex on the Ukaku path since 1.3.0); the original runs only from there.
        checkTrampoline(controller, "startRapidStrike", EXPANSION + "server/UkakuVortex", "startRapidStrike");
        check(calls(onAction, CONTROLLER, "startRapidStrike") == 1, "RAPID_STRIKE must still call startRapidStrike");
        for (MethodNode body : controller.methods)
            check(calls(body, CONTROLLER, "startRapidStrikeLegacy") == 0, body.name + " bypasses the Rapid Strike filter");
        // 1.2.0: tendril-only moves go through the Ukaku gates, each from exactly its original call site.
        String gates = EXPANSION + "server/UkakuGates";
        for (String gated : new String[] {"slingshot", "grabOrThrow", "throwHeld", "crush", "lift", "slam", "toggleThreatAutoAttack"}) {
            checkTrampoline(controller, gated, gates, gated);
            check(calls(onAction, CONTROLLER, gated) == 1, "onAction must still call " + gated);
        }
        checkTrampoline(controller, "tryStartFallArrest", gates, "tryStartFallArrest");
        checkTrampoline(controller, "toggleGuard", gates, "toggleGuard");
        // 5.0.14: Auto Projectile's deflection is the tentacle guard.
        checkTrampoline(controller, "tickAutoProjectile", EXPANSION + "server/TendrilShieldServer", "tickAutoProjectile");
        checkTrampoline(controller, "deflectProjectileImpact", EXPANSION + "server/TendrilShieldServer", "deflectProjectileImpact");
        check(calls(method(controller, "tick"), CONTROLLER, "tickAutoProjectile") == 1, "the tick must still run the auto projectile guard");
        check(calls(onAction, CONTROLLER, "toggleGuard") == 1, "TOGGLE_GUARD must still call toggleGuard");
        checkTrampoline(controller, "cycleTendrils", EXPANSION + "server/UkakuForms", "cycleTendrils");
        check(calls(onAction, CONTROLLER, "cycleTendrils") == 1, "CYCLE_TENDRILS must still call cycleTendrils");
        // 3.0.3: the crush ends by letting go of what it crushed.
        MethodNode crush = method(controller, "tickCrush");
        check(calls(crush, EXPANSION + "server/RinkakuGrip", "crushDone") == 1 && calls(crush, "com/hex/kagune/gameplay/KaguneRuntime$State", "begin") == 0,
            "the crush must end through RinkakuGrip");
        checkContiguous(crush, EXPANSION + "server/RinkakuGrip", "crushDone", 1);
        checkTrampoline(controller, "refreshFormEffect", EXPANSION + "server/RinkakuFormBuffs", "refreshFormEffect");
        check(calls(method(controller, "tickCentipedeFormBuffs"), CONTROLLER, "refreshFormEffect") == 3,
            "the centipede form buffs must go through RinkakuFormBuffs");
        for (MethodNode body : controller.methods)
            check(calls(body, CONTROLLER, "refreshFormEffectLegacy") == 0, body.name + " bypasses RinkakuFormBuffs");
        check(calls(method(controller, "tick"), CONTROLLER, "tryStartFallArrest") == 1, "the tick must still try the fall arrest");
        for (MethodNode body : controller.methods)
            for (String gated : new String[] {"slingshot", "grabOrThrow", "throwHeld", "crush", "lift", "slam", "toggleThreatAutoAttack",
                    "tryStartFallArrest", "toggleGuard", "cycleTendrils", "tickAutoProjectile", "deflectProjectileImpact"})
                check(calls(body, CONTROLLER, gated + "Legacy") == 0, body.name + " bypasses the Ukaku gate on " + gated);
        int activations = 0;
        for (MethodNode body : controller.methods) {
            for (AbstractInsnNode instruction : body.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode call && call.name.equals("setActive") && call.getPrevious().getOpcode() == ICONST_1) {
                    check(body.name.equals("toggleLegacy"), "the kagune may only turn active inside the gated toggle, not " + body.name);
                    activations++;
                }
            }
        }
        check(activations == 1, "expected exactly one activation site, found " + activations);
        MethodNode tick = method(controller, "tick");
        check(calls(tick, EXPANSION + "server/KaguneCombat", "tickSwipe") == 1 && calls(tick, CONTROLLER, "tickSwipe") == 0,
            "SWIPE ticks must go through the combo system");
        for (String reused : new String[] {"tickSwipe", "validSwipeTarget", "clearLineTo", "applyCentipedeBleed", "multiTendrilFollowUp",
                "playAt", "playSelf", "vary", "formPitch", "center", "traceLiving", "validHintedLiving", "traceBlock", "useEnergy",
                "takeNextAttackLimb", "interruptCentipedeIfUsingLimb", "committed", "beginRetract", "isHostileTo", "refreshFormEffect",
                "releaseHeldSlot", "returnToHoldingOrRetract", "centipedeHit", "centipedeCrushHit", "captureAutonomousCentipedeTarget",
                "syncKaguneHeldPlayer"}) {
            controller.methods.stream().filter(m -> m.name.equals(reused))
                .forEach(m -> check(isPublic(m), "controller helper not widened: " + reused));
        }
        // 3.1.0: the centipede hunt, its guard and its hold are CentipedeHunt's.
        String hunt = EXPANSION + "server/CentipedeHunt";
        checkTrampoline(controller, "tickCentipedeAutoAttack", hunt, "tickCentipedeAutoAttack");
        checkTrampoline(controller, "tryCentipedeBlock", hunt, "tryCentipedeBlock");
        checkTrampoline(controller, "applyCentipedeBleed", EXPANSION + "server/RinkakuBleed", "applyCentipedeBleed");
        check(calls(method(controller, "centipedeHit"), CONTROLLER, "applyCentipedeBleed") == 1,
            "a centipede hit must bleed through the trampoline (Ukaku's bleeding)");
        check(calls(tick, CONTROLLER, "tickCentipedeAutoAttack") == 1, "the server tick must still run the centipede hunt");
        MethodNode heldTick = method(controller, "tickHeldEntities");
        check(calls(heldTick, hunt, "hold") == 1 && calls(heldTick, CONTROLLER, "restrainCentipedeTarget") == 0,
            "a body held by a centipede leg must be placed by CentipedeHunt.hold");
        check(calls(method(controller, "onAction", "(Lnet/minecraft/server/level/ServerPlayer;Lcom/hex/kagune/gameplay/KaguneAction;I)V"),
            CONTROLLER, "startSwipe") == 1, "SWIPE action must still call startSwipe");
        check(calls(method(controller, "startSwipeFromVanillaAttack"), CONTROLLER, "startSwipe") == 1, "vanilla attack must still call startSwipe");
        check(calls(method(controller, "tickThreatAutoAttack"), CONTROLLER, "startSwipe") == 0, "Threat Auto Attack must remain independent");

        ClassNode tendril = read(jar, TENDRIL);
        String strikes = EXPANSION + "client/render/StrikePaths";
        int limbRedirects = 0;
        for (String host : new String[] {"updateSegmentedCurve", "simulate", "renderPath"}) {
            MethodNode body = method(tendril, host);
            limbRedirects += calls(body, strikes, "isActionLimb");
            check(calls(body, TENDRIL, "isActionLimb") == 0, host + " still calls isActionLimb directly");
            checkContiguous(body, strikes, "isActionLimb", 1);
        }
        check(limbRedirects == 5, "expected five isActionLimb redirects, found " + limbRedirects);
        MethodNode action = method(tendril, "actionTarget");
        check(calls(action, strikes, "strikeTarget") == 2 && calls(action, TENDRIL, "thrustTarget") == 0, "strike path redirects");
        checkContiguous(action, strikes, "strikeTarget", 3);
        check(isPublic(method(tendril, "thrustTarget")) && isPublic(method(tendril, "isActionLimb")), "TendrilChain helpers not widened");
        // 3.0.0: every drawn path goes through the owner's body clearance right after it is resampled.
        MethodNode render = method(tendril, "renderPath");
        check(calls(render, strikes, "clearOwner") == 1 && calls(render, TENDRIL, "resample") == 1, "render path body clearance");
        checkContiguous(render, strikes, "clearOwner", 4);
        MethodNode getter = method(tendril, "kaguneLimbIndex", "()I");
        check(isPublic(getter) && getter.instructions.size() == 3, "limb index getter");

        ClassNode manifest = read(jar, MANIFEST);
        String body = EXPANSION + "client/animation/BodyAnimations";
        checkTrampoline(manifest, "playPain", body, "playManifestPain");
        checkTrampoline(manifest, "stopPain", body, "stopManifestPain");
        checkTrampoline(manifest, "spawnOriginMist", EXPANSION + "client/UkakuClient", "spawnOriginMist");
        checkTrampoline(manifest, "spawnBlood", EXPANSION + "client/UkakuClient", "spawnManifestBlood");
        check(isPublic(method(manifest, "bodyEffectLayer")), "bodyEffectLayer not widened");

        ClassNode mod = read(jar, MOD);
        MethodNode constructor = method(mod, "<init>");
        check(calls(constructor, EXPANSION + "KaguneControlBootstrap", "init") == 1, "control bootstrap hook missing");
        AbstractInsnNode network = null;
        for (AbstractInsnNode instruction : constructor.instructions.toArray())
            if (instruction instanceof MethodInsnNode call && call.name.equals("init") && call.owner.endsWith("KaguneNetwork")) network = instruction;
        check(network != null && network.getNext() instanceof VarInsnNode && network.getNext().getNext() instanceof MethodInsnNode next
            && next.owner.equals(EXPANSION + "KaguneControlBootstrap"), "bootstrap must run right after the original network init");

        // 1.2.0: the tendril renderer and the writhing sounds look players up through the Ukaku filter (wings are drawn separately).
        for (String[] host : new String[][] {{WORLD_RENDERER, "renderWorld"}, {AMBIENT, "clientTick"}}) {
            MethodNode lookup = method(read(jar, host[0]), host[1]);
            check(calls(lookup, EXPANSION + "client/UkakuClient", "tendrilView") == 1
                && calls(lookup, "com/hex/kagune/client/KaguneClientState", "get") == 0, host[0] + "." + host[1] + " must use the Ukaku view filter");
        }

        formOrderChecks(jar);

        // 3.0.0: Rinkaku's kakuja mask rides the head model part; the world-space one is a trampoline to the layer's no-op.
        checkTrampoline(read(jar, WORLD_RENDERER), "emitRinkakuMask", EXPANSION + "client/render/RinkakuMaskLayer", "emitRinkakuMask");

        // 3.1.0: the centipede legs are CentipedeRenderer's; every other limb still goes through the original tube.
        ClassNode worldRenderer = read(jar, WORLD_RENDERER);
        checkTrampoline(worldRenderer, "emitTube", EXPANSION + "client/render/CentipedeRenderer", "emitTube");
        check(calls(method(worldRenderer, "renderWorld"), WORLD_RENDERER, "emitTube") == 1, "renderWorld must draw limbs through the emitTube trampoline");
        check(isPublic(method(worldRenderer, "limbDeployment")), "limbDeployment not widened");
        checkTrampoline(worldRenderer, "limbDeployment", EXPANSION + "client/fx/KakujaFx", "limbDeployment");

        // 1.2.0: the unused original spike becomes Ukaku's RC needle: only its three model-scale constants change (1.35 -> 0.85, 0.32, 0.32).
        try (ZipFile original = new ZipFile(Path.of(ORIGINAL).toFile())) {
            MethodNode before = method(read(original, SPIKE_RENDERER), "render");
            MethodNode after = method(read(jar, SPIKE_RENDERER), "render");
            AbstractInsnNode[] old = before.instructions.toArray();
            AbstractInsnNode[] now = after.instructions.toArray();
            check(old.length == now.length && before.desc.equals(after.desc), "spike renderer body changed shape");
            int scaled = 0;
            float[] expected = {0.85F, 0.32F, 0.32F};
            for (int i = 0; i < old.length; i++) {
                check(old[i].getOpcode() == now[i].getOpcode(), "spike renderer instruction " + i + " changed");
                if (old[i] instanceof LdcInsnNode was && now[i] instanceof LdcInsnNode is && !was.cst.equals(is.cst)) {
                    check(scaled < 3 && Float.valueOf(1.35F).equals(was.cst) && Float.valueOf(expected[scaled]).equals(is.cst),
                        "spike constant " + was.cst + " changed to " + is.cst);
                    scaled++;
                }
            }
            check(scaled == 3, "expected three spike scale constants, found " + scaled);
        }

        // 1.2.0: only the profile root accessor is widened (for the stored kagune path). 3.0.3: hasMutation is a trampoline to
        // RinkakuMutationsServer (the cut mutations). Nothing else in the profile changes.
        ClassNode profile = read(jar, PROFILE);
        MethodNode root = method(profile, "root", "(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/nbt/CompoundTag;");
        check(isPublic(root) && (root.access & ACC_STATIC) != 0, "profile root not widened");
        checkTrampoline(profile, "hasMutation", EXPANSION + "server/RinkakuMutationsServer", "hasMutation");
        // 3.0.3: swipe, crush and slam damage are trampolines to KaguneBalance (DamageBalance).
        Set<String> trampolined = Set.of("hasMutation", "swipeDamage", "crushDamage", "slamDamage", "actionEnergyCost", "energy", "setEnergy",
            "consumeEnergy", "restoreEnergy");
        // 7.0.0: RC, healing and food are one bar: the profile's RC goes through SharedBar.
        for (String energy : new String[] {"energy", "setEnergy", "consumeEnergy", "restoreEnergy"})
            checkTrampoline(profile, energy, EXPANSION + "server/SharedBar", energy);
        for (String damage : new String[] {"swipeDamage", "crushDamage", "slamDamage"})
            checkTrampoline(profile, damage, EXPANSION + "server/KaguneBalance", damage);
        // 6.1.0: what a move costs goes through KaguneBalance (EnergyBalance: mastery takes more off).
        checkTrampoline(profile, "actionEnergyCost", EXPANSION + "server/KaguneBalance", "actionEnergyCost");
        // 6.0.4: what gameplay earns (addMastery with an action) goes up the curve; nothing else adds to it unbent.
        MethodNode earned = method(profile, "addMastery", "(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;I)V");
        check(calls(earned, EXPANSION + "server/MasteryGain", "earn") == 1 && calls(earned, PROFILE, "addMastery") == 0,
            "action mastery must go through MasteryGain.earn exactly once");
        try (ZipFile original = new ZipFile(Path.of(ORIGINAL).toFile())) {
            ClassNode before = read(original, PROFILE);
            check(before.methods.size() + trampolined.size() == profile.methods.size(), "profile methods added or removed");
            for (MethodNode old : before.methods) {
                MethodNode now = method(profile, trampolined.contains(old.name) ? old.name + "Legacy" : old.name, old.desc);
                check(now.instructions.size() == old.instructions.size(), "profile method body changed: " + old.name);
                check(old.name.equals("root") || trampolined.contains(old.name) || now.access == old.access, "profile access changed: " + old.name);
            }
        }
    }

    /**
     * 7.0.0: the original RC bar's listener is a trampoline to ReserveHud.energyHud carrying the {@code @SubscribeEvent} (its body,
     * renamed, is unsubscribed and unchanged); nothing else in the class changes.
     */
    private static void checkEnergyHud(ZipFile jar) throws IOException {
        String name = "com/hex/kagune/client/KaguneEnergyHud";
        ClassNode hud = read(jar, name);
        checkTrampoline(hud, "render", EXPANSION + "client/ReserveHud", "energyHud");
        MethodNode bridge = method(hud, "render");
        MethodNode legacy = method(hud, "renderLegacy", bridge.desc);
        check(bridge.visibleAnnotations != null && bridge.visibleAnnotations.stream()
            .anyMatch(a -> a.desc.equals("Lnet/minecraftforge/eventbus/api/SubscribeEvent;")), "the energy HUD bridge must be the listener");
        check(legacy.visibleAnnotations == null || legacy.visibleAnnotations.isEmpty(), "the energy HUD's own body must not stay subscribed");
        try (ZipFile original = new ZipFile(Path.of(ORIGINAL).toFile())) {
            ClassNode before = read(original, name);
            check(before.methods.size() + 1 == hud.methods.size(), "energy HUD methods added or removed");
            for (MethodNode old : before.methods) {
                MethodNode now = method(hud, old.name.equals("render") ? "renderLegacy" : old.name, old.desc);
                check(described(now).equals(described(old)), "energy HUD method body changed: " + old.name);
            }
        }
    }

    /** One real instruction as text (opcode and operand), for comparing a patched method with the original. */
    private static String describe(AbstractInsnNode instruction) {
        String operand = "";
        if (instruction instanceof FieldInsnNode field) operand = field.owner + "." + field.name;
        else if (instruction instanceof MethodInsnNode call) operand = call.owner + "." + call.name + call.desc;
        else if (instruction instanceof LdcInsnNode ldc) operand = String.valueOf(ldc.cst);
        else if (instruction instanceof VarInsnNode variable) operand = String.valueOf(variable.var);
        else if (instruction instanceof jdk.internal.org.objectweb.asm.tree.IntInsnNode push) operand = String.valueOf(push.operand);
        else if (instruction instanceof TypeInsnNode type) operand = type.desc;
        return instruction.getOpcode() + " " + operand;
    }

    private static List<String> described(MethodNode method) {
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions.toArray()) if (instruction.getOpcode() >= 0) out.add(describe(instruction));
        return out;
    }

    /** Every instruction that differs between the original and the patched class, as "method i: was -> now" (same-length methods). */
    private static Set<String> differences(ClassNode before, ClassNode after, Set<String> skip) {
        KagunePatch.renameOriginal(before); // 1.2.5: the original's names for the show's characters, renamed as the patch does
        check(before.methods.size() == after.methods.size() && before.fields.size() == after.fields.size(), after.name + " gained or lost members");
        Set<String> out = new TreeSet<>();
        for (MethodNode old : before.methods) {
            if (skip.contains(old.name)) continue;
            MethodNode now = method(after, old.name, old.desc);
            check(now.access == old.access, after.name + "." + old.name + " access changed");
            List<String> was = described(old);
            List<String> is = described(now);
            check(was.size() == is.size(), after.name + "." + old.name + " changed shape");
            for (int i = 0; i < was.size(); i++)
                if (!was.get(i).equals(is.get(i))) out.add(old.name + " " + i + ": " + was.get(i) + " -> " + is.get(i));
        }
        return out;
    }

    /**
     * 3.0.1: Centipede Form and Partial Kakuja trade places (slot, mastery, armour; ids, names and limbs stay), the damage and
     * hurt-reduction slots follow, and the menu gains the two colour filters and the 750 stage name. 3.0.2: the Partial Kakuja
     * never unlocks and the form lists players see skip it ({@code RinkakuForms.forms}), Twin and Four-Tail move to 250 and 500,
     * and the 450 milestone (menu stage, next milestone, third mutation tier) moves to 500. Nothing else changes.
     */
    private static void formOrderChecks(ZipFile jar) throws Exception {
        String form = "com/hex/kagune/gameplay/KaguneForm";
        String forms = EXPANSION + "RinkakuForms";
        String valuesCall = "184 " + form + ".values()[L" + form + ";";
        String formsCall = "184 " + forms + ".forms()[L" + form + ";";
        try (ZipFile original = new ZipFile(Path.of(ORIGINAL).toFile())) {
            Set<String> enumDiffs = differences(read(original, form), read(jar, form), Set.of());
            check(enumDiffs.size() == 12, "KaguneForm: expected 12 changed instructions, found " + enumDiffs);
            String joined = String.join("\n", enumDiffs);
            check(joined.contains(": 6  -> 7 ") && joined.contains(": 7  -> 6 ") && joined.contains(": 17 450 -> 17 750")
                && joined.contains(": 17 750 -> 18 " + Integer.MAX_VALUE) && joined.contains(": 4  -> 5 ") && joined.contains(": 5  -> 4 ")
                && joined.contains(": 16 100 -> 17 250") && joined.contains(": 17 250 -> 17 500")
                && joined.contains(": 178 " + form + ".CENTIPEDE -> 178 " + form + ".PARTIAL_KAKUJA")
                && joined.contains(": 178 " + form + ".PARTIAL_KAKUJA -> 178 " + form + ".CENTIPEDE")
                && enumDiffs.stream().filter(d -> (d.startsWith("find ") || d.startsWith("ids ")) && d.endsWith(valuesCall + " -> " + formsCall)).count() == 2,
                "KaguneForm reslot: " + joined);
            ClassNode patched = read(jar, form);
            List<String> order = new ArrayList<>();
            for (AbstractInsnNode instruction : method(patched, "$values").instructions.toArray())
                if (instruction instanceof FieldInsnNode field && field.getOpcode() == GETSTATIC) order.add(field.name);
            check(order.equals(List.of("RINKAKU_SEED", "RINKAKU_DUAL", "RINKAKU_QUAD", "PARTIAL_KAKUJA", "CENTIPEDE", "FULL_KAKUJA")),
                "form order " + order);
            Set<String> damage = differences(read(original, form + "$1"), read(jar, form + "$1"), Set.of());
            check(damage.size() == 2 && String.join(" ", damage).contains("CENTIPEDE -> 178 " + form + ".PARTIAL_KAKUJA")
                && String.join(" ", damage).contains("PARTIAL_KAKUJA -> 178 " + form + ".CENTIPEDE"), "damage multiplier slots: " + damage);
            Set<String> defence = differences(read(original, "com/hex/kagune/gameplay/KaguneEvents$1"),
                read(jar, "com/hex/kagune/gameplay/KaguneEvents$1"), Set.of());
            check(defence.size() == 1 && defence.iterator().next().endsWith("PARTIAL_KAKUJA -> 178 " + form + ".CENTIPEDE"),
                "hurt reduction slot: " + defence);
            // 3.0.3: the ten cut mutations never unlock, Centipede Body moves to 500, and nothing locks out or needs another.
            String mutation = "com/hex/kagune/gameplay/MutationNode";
            ClassNode mutations = read(jar, mutation);
            Set<String> tiers = differences(read(original, mutation), mutations, Set.of("conflicts", "prerequisitesMet"));
            // 5.0.14: and Auto Projectile's and the Reflex Organ's descriptions say they guard (two string constants).
            check(tiers.size() == 13 && tiers.stream().allMatch(d -> d.startsWith("<clinit> "))
                && tiers.stream().filter(d -> d.endsWith(": 17 450 -> 17 500")).count() == 1
                && tiers.stream().filter(d -> d.endsWith(" -> 18 " + Integer.MAX_VALUE)).count() == 10
                && tiers.stream().filter(d -> d.contains("slaps away incoming projectiles") && d.contains("swing across incoming projectiles")).count() == 1
                && tiers.stream().filter(d -> d.contains("harder deflection") && d.contains("sturdier guards")).count() == 1, "mutation tiers: " + tiers);
            check(described(method(mutations, "conflicts")).equals(List.of("3 ", "172 "))
                && described(method(mutations, "prerequisitesMet")).equals(List.of("4 ", "172 ")), "mutations neither conflict nor need another");
            String screen = "com/hex/kagune/client/screen/KaguneScreen";
            ClassNode before = read(original, screen);
            ClassNode after = read(jar, screen);
            Set<String> menu = differences(before, after, Set.of("drawPreview"));
            String nodesCall = "184 " + EXPANSION + "RinkakuMutations.nodes()[L" + mutation + ";";
            check(menu.size() == 9 && menu.stream().filter(d -> d.startsWith("stageName ") && d.endsWith("-> 18 Centipede Kakuja")).count() == 1
                && menu.stream().filter(d -> d.endsWith(": 17 450 -> 17 500")).count() == 4
                && menu.stream().filter(d -> d.startsWith("nextMilestone ")).count() == 2
                && menu.stream().filter(d -> d.startsWith("drawMutationTree ")).count() == 4
                && menu.stream().filter(d -> d.endsWith(" -> " + nodesCall)).count() == 3
                && menu.stream().filter(d -> d.endsWith("-> 18 Every mutation can be taken. /kagune reset starts over.")).count() == 1,
                "menu changes outside the form buttons: " + menu);
            MethodNode draw = method(after, "drawPreview");
            check(calls(draw, forms, "buttonColor") == 1 && calls(draw, forms, "labelColor") == 1, "menu form colour filters missing");
            check(calls(draw, forms, "forms") == 1 && calls(draw, form, "values") == 0, "the menu's form buttons must skip the removed form");
            List<String> stripped = new ArrayList<>(described(draw));
            for (String filter : new String[] {"buttonColor", "labelColor"}) {
                int at = -1;
                for (int i = 0; i < stripped.size(); i++) if (stripped.get(i).contains(forms + "." + filter)) at = i;
                int loads = filter.equals("buttonColor") ? 3 : 2;
                for (int i = at; i >= at - loads; i--) stripped.remove(i);
            }
            stripped.replaceAll(instruction -> instruction.equals(formsCall) ? valuesCall : instruction);
            check(stripped.equals(described(method(before, "drawPreview"))), "drawPreview changed beyond the colour filters and the form list");
        }
    }

    // ------------------------------------------------------------------ hierarchy + type analysis

    private record Info(String superName, List<String> interfaces, boolean isInterface) {
    }

    private static final Map<String, Info> HIERARCHY = new HashMap<>();

    private static void addHierarchy(ClassNode node) {
        HIERARCHY.putIfAbsent(node.name, new Info(node.superName, node.interfaces, (node.access & ACC_INTERFACE) != 0));
    }

    private static Info info(String name) {
        Info known = HIERARCHY.get(name);
        if (known != null) return known;
        try {
            Class<?> type = Class.forName(name.replace('/', '.'), false, ClassLoader.getPlatformClassLoader());
            List<String> interfaces = new ArrayList<>();
            for (Class<?> each : type.getInterfaces()) interfaces.add(Type.getInternalName(each));
            Info found = new Info(type.getSuperclass() == null ? null : Type.getInternalName(type.getSuperclass()), interfaces, type.isInterface());
            HIERARCHY.put(name, found);
            return found;
        } catch (ClassNotFoundException | LinkageError unknown) {
            return null;
        }
    }

    /** True if {@code ancestor} is {@code type} or one of its superclasses/superinterfaces. Unknown chains return false. */
    private static boolean isAncestor(String ancestor, String type) {
        if (ancestor.equals(type)) return true;
        Info node = info(type);
        if (node == null) return false;
        if (node.superName() != null && isAncestor(ancestor, node.superName())) return true;
        for (String each : node.interfaces()) if (isAncestor(ancestor, each)) return true;
        return false;
    }

    /** SimpleVerifier backed by the JAR + stub hierarchy; lenient only for types neither source knows (untouched original code). */
    private static final class HierarchyVerifier extends SimpleVerifier {
        HierarchyVerifier(ClassNode owner) {
            super(ASM8, Type.getObjectType(owner.name), owner.superName == null ? null : Type.getObjectType(owner.superName),
                owner.interfaces.stream().map(Type::getObjectType).toList(), (owner.access & ACC_INTERFACE) != 0);
        }

        @Override
        protected boolean isInterface(Type type) {
            Info node = info(type.getInternalName());
            return node != null && node.isInterface();
        }

        @Override
        protected Type getSuperClass(Type type) {
            Info node = info(type.getInternalName());
            return node == null || node.superName() == null ? (type.getInternalName().equals("java/lang/Object") ? null : Type.getObjectType("java/lang/Object"))
                : Type.getObjectType(node.superName());
        }

        @Override
        protected boolean isAssignableFrom(Type target, Type value) {
            if (target.equals(value)) return true;
            if (target.getSort() == Type.ARRAY && value.getSort() == Type.ARRAY) {
                // JVMS 4.10.1.2: arrays are assignable component-wise; primitive components must match exactly,
                // so float[][][] fits Object[] (a float[][] is an Object) but int[] never fits float[].
                Type to = Type.getType(target.getDescriptor().substring(1));
                Type from = Type.getType(value.getDescriptor().substring(1));
                if (to.getSort() < Type.ARRAY || from.getSort() < Type.ARRAY) return to.equals(from);
                return isAssignableFrom(to, from);
            }
            if (value.getSort() == Type.ARRAY) {
                String to = target.getInternalName();
                return to.equals("java/lang/Object") || to.equals("java/lang/Cloneable") || to.equals("java/io/Serializable");
            }
            if (target.getSort() == Type.ARRAY) return false; // a plain object is never an array
            String to = target.getInternalName();
            String from = value.getInternalName();
            if (to.equals("java/lang/Object") || info(to) == null || info(from) == null) return true;
            if (info(to).isInterface()) return true; // the JVM verifier treats interface targets like Object too
            return isAncestor(to, from);
        }
    }

    private static void analyze(ClassNode node, MethodNode body) {
        try {
            new Analyzer<>(new HierarchyVerifier(node)).analyze(node.name, body);
        } catch (AnalyzerException failure) {
            throw new AssertionError("type check failed in " + node.name + "." + body.name + body.desc + ": " + failure.getMessage(), failure);
        }
    }

    // ------------------------------------------------------------------ reference audit

    private static final Set<String> PROVEN = new HashSet<>();        // "M owner name desc" / "F owner name desc" from the 1.0.0 JAR
    private static final Set<String> PROVEN_CLASSES = new HashSet<>();
    private static final Set<String> ALLOWED = new HashSet<>();
    private static final Set<String> ALLOWED_CLASSES = new HashSet<>();
    private static final Set<String> USED_ALLOWLIST = new HashSet<>();

    private static boolean external(String owner) {
        return !(owner.startsWith("java/") || owner.startsWith("javax/") || owner.startsWith("jdk/") || owner.startsWith("com/hex/kagune/")
            || owner.startsWith("["));
    }

    private static void collectProven(ZipFile input) throws IOException {
        for (ZipEntry entry : Collections.list(input.entries())) {
            if (!entry.getName().endsWith(".class")) continue;
            ClassNode node = new ClassNode();
            new ClassReader(input.getInputStream(entry).readAllBytes()).accept(node, 0);
            Set<String> classes = new HashSet<>();
            Set<String> members = new HashSet<>();
            references(node, classes, members);
            PROVEN_CLASSES.addAll(classes);
            PROVEN.addAll(members);
        }
    }

    private static void addTypes(Set<String> classes, Type type) {
        switch (type.getSort()) {
            case Type.ARRAY -> addTypes(classes, type.getElementType());
            case Type.OBJECT -> classes.add(type.getInternalName());
            case Type.METHOD -> {
                addTypes(classes, type.getReturnType());
                for (Type argument : type.getArgumentTypes()) addTypes(classes, argument);
            }
            default -> {
            }
        }
    }

    private static void addAnnotations(Set<String> classes, List<AnnotationNode> annotations) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) {
            addTypes(classes, Type.getType(annotation.desc));
            if (annotation.values != null)
                for (Object value : annotation.values) {
                    if (value instanceof String[] enumValue && enumValue.length == 2) addTypes(classes, Type.getType(enumValue[0]));
                    if (value instanceof List<?> list)
                        for (Object element : list)
                            if (element instanceof String[] enumValue && enumValue.length == 2) addTypes(classes, Type.getType(enumValue[0]));
                }
        }
    }

    /** Every class and member a class needs at link/run time. */
    private static void references(ClassNode node, Set<String> classes, Set<String> members) {
        if (node.superName != null) classes.add(node.superName);
        classes.addAll(node.interfaces);
        addAnnotations(classes, node.visibleAnnotations);
        addAnnotations(classes, node.invisibleAnnotations);
        for (FieldNode field : node.fields) {
            addTypes(classes, Type.getType(field.desc));
            addAnnotations(classes, field.visibleAnnotations);
        }
        for (MethodNode method : node.methods) {
            addTypes(classes, Type.getMethodType(method.desc));
            addAnnotations(classes, method.visibleAnnotations);
            if (method.exceptions != null) classes.addAll(method.exceptions);
            for (TryCatchBlockNode handler : method.tryCatchBlocks) if (handler.type != null) classes.add(handler.type);
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call) {
                    if (!call.owner.startsWith("[")) classes.add(call.owner);
                    addTypes(classes, Type.getMethodType(call.desc));
                    members.add("M " + call.owner + " " + call.name + " " + call.desc);
                } else if (instruction instanceof FieldInsnNode access) {
                    classes.add(access.owner);
                    addTypes(classes, Type.getType(access.desc));
                    members.add("F " + access.owner + " " + access.name + " " + access.desc);
                } else if (instruction instanceof TypeInsnNode typed) {
                    addTypes(classes, Type.getObjectType(typed.desc));
                } else if (instruction instanceof MultiANewArrayInsnNode array) {
                    addTypes(classes, Type.getType(array.desc));
                } else if (instruction instanceof LdcInsnNode constant && constant.cst instanceof Type type) {
                    addTypes(classes, type);
                } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                    addTypes(classes, Type.getMethodType(dynamic.desc));
                    if (dynamic.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory") && dynamic.bsmArgs.length >= 3
                        && dynamic.bsmArgs[0] instanceof Type sam && dynamic.bsmArgs[1] instanceof Handle implementation) {
                        String functional = Type.getReturnType(dynamic.desc).getInternalName();
                        members.add("M " + functional + " " + dynamic.name + " " + sam.getDescriptor());
                        addTypes(classes, sam);
                        members.add((implementation.getTag() <= H_PUTSTATIC ? "F " : "M ") + implementation.getOwner() + " "
                            + implementation.getName() + " " + implementation.getDesc());
                        if (!implementation.getOwner().startsWith("[")) classes.add(implementation.getOwner());
                    }
                }
            }
        }
        // Implementations of external API methods (IAnimation, IGuiOverlay, ...) must match a verified declaration.
        for (MethodNode method : node.methods) {
            if ((method.access & (ACC_STATIC | ACC_PRIVATE)) != 0 || method.name.startsWith("<")) continue;
            for (String supertype : externalSupertypes(node.name)) {
                if (STUB_METHODS.getOrDefault(supertype, Set.of()).contains(method.name + " " + method.desc))
                    members.add("O " + supertype + " " + method.name + " " + method.desc);
            }
        }
    }

    /** Stub-declared methods per external class: the compile-time view of what an override overrides. */
    private static final Map<String, Set<String>> STUB_METHODS = new HashMap<>();

    private static Set<String> externalSupertypes(String name) {
        Set<String> all = new LinkedHashSet<>();
        Info node = info(name);
        if (node == null) return all;
        List<String> direct = new ArrayList<>(node.interfaces());
        if (node.superName() != null) direct.add(node.superName());
        for (String each : direct) {
            if (external(each)) all.add(each);
            all.addAll(externalSupertypes(each));
        }
        return all;
    }

    private static void loadAllowlist(Path file) throws IOException {
        for (String raw : Files.readAllLines(file)) {
            String line = raw.contains("#") ? raw.substring(0, raw.indexOf('#')).trim() : raw.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            if (parts[0].equals("C") && parts.length == 2) ALLOWED_CLASSES.add(parts[1]);
            else if ((parts[0].equals("M") || parts[0].equals("F")) && parts.length == 4) ALLOWED.add(String.join(" ", parts));
            else throw new AssertionError("bad allowlist line: " + raw);
        }
    }

    /** A member reference through {@code owner} is safe if a verified entry declares it on {@code owner} or one of its supertypes. */
    private static boolean memberKnown(String kind, String owner, String name, String desc) {
        for (Set<String> source : List.of(PROVEN, ALLOWED)) {
            for (String candidate : source) {
                String[] parts = candidate.split(" ");
                if (!parts[0].equals(kind) || !parts[2].equals(name) || !parts[3].equals(desc)) continue;
                if (isAncestor(parts[1], owner)) {
                    if (source == ALLOWED) USED_ALLOWLIST.add(candidate);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean classKnown(String name) {
        if (!external(name)) return true;
        if (ALLOWED_CLASSES.contains(name)) {
            USED_ALLOWLIST.add("C " + name);
            return true;
        }
        return PROVEN_CLASSES.contains(name);
    }

    private static boolean resolvesInternally(Map<String, ClassNode> classes, String kind, String owner, String name, String desc) {
        return resolvesInternally(classes, kind, owner, name, desc, false);
    }

    /**
     * {@code viaInterface}: looked for in an interface the class implements (1.3.3): found only if declared there (or in what it
     * extends); an interface's superclass is Object, and reaching Object through one must not count as resolving everything.
     */
    private static boolean resolvesInternally(Map<String, ClassNode> classes, String kind, String owner, String name, String desc, boolean viaInterface) {
        if (PROVEN.contains(kind + " " + owner + " " + name + " " + desc)) return true; // the original JAR links it already
        String cursor = owner;
        while (cursor != null && cursor.startsWith("com/hex/kagune/")) {
            ClassNode node = classes.get(cursor);
            if (node == null) return false;
            if (kind.equals("F") && node.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals(desc))) return true;
            if (kind.equals("M") && node.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(desc))) return true;
            for (String each : node.interfaces) if (resolvesInternally(classes, kind, each, name, desc, true)) return true;
            cursor = node.superName;
        }
        return cursor != null && memberKnown(kind, cursor, name, desc)
            || !viaInterface && cursor != null && (cursor.equals("java/lang/Object") || cursor.equals("java/lang/Enum") || cursor.equals("java/lang/Record"));
    }

    private static void audit(Map<String, ClassNode> classes, Set<String> audited, Path allowlist) {
        Set<String> problems = new TreeSet<>();
        for (String name : audited) {
            ClassNode node = classes.get(name);
            Set<String> types = new LinkedHashSet<>();
            Set<String> members = new LinkedHashSet<>();
            references(node, types, members);
            for (String type : types) if (!classKnown(type)) problems.add("unverified class " + type + " (used by " + name + ")");
            for (String member : members) {
                String[] parts = member.split(" ");
                String kind = parts[0];
                String owner = parts[1];
                if (kind.equals("O")) {
                    // Implementing an external API method: the original JAR implemented it too, or it is allowlisted.
                    if (!PROVEN.contains(member) && !memberKnown("M", owner, parts[2], parts[3]))
                        problems.add("implementation of unverified " + owner + "." + parts[2] + parts[3] + " (in " + name + ")");
                    continue;
                }
                if (owner.startsWith("[")) continue;
                if (!external(owner)) {
                    if (owner.startsWith("com/hex/kagune/") && !resolvesInternally(classes, kind, owner, parts[2], parts[3]))
                        problems.add("unresolved internal " + member + " (used by " + name + ")");
                    continue;
                }
                if (!memberKnown(kind, owner, parts[2], parts[3])) problems.add("unverified " + member + " (used by " + name + ")");
            }
        }
        if (!problems.isEmpty()) {
            problems.forEach(problem -> System.err.println("  " + problem));
            throw new AssertionError(problems.size() + " external references are not proven by the 1.0.0 JAR nor in " + allowlist);
        }
        Set<String> stale = new TreeSet<>();
        for (String entry : ALLOWED) if (!USED_ALLOWLIST.contains(entry)) stale.add(entry);
        for (String entry : ALLOWED_CLASSES) if (!USED_ALLOWLIST.contains("C " + entry)) stale.add("C " + entry);
        if (!stale.isEmpty()) {
            stale.forEach(entry -> System.err.println("  stale allowlist entry: " + entry));
            throw new AssertionError("allowlist entries are no longer used; remove them so the list stays an exact inventory");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: KaguneVerify output.jar input.jar api-allowlist.txt stub-classes-dir");
        Map<String, ClassNode> classes = new HashMap<>();
        try (ZipFile jar = new ZipFile(Path.of(args[0]).toFile()); ZipFile input = new ZipFile(Path.of(args[1]).toFile())) {
            legacyChecks(jar);
            ORIGINAL = args[1];
            hookChecks(jar);
            checkEnergyHud(jar);
            for (ZipEntry entry : Collections.list(jar.entries())) {
                if (!entry.getName().endsWith(".class")) continue;
                ClassNode node = new ClassNode();
                new ClassReader(jar.getInputStream(entry).readAllBytes()).accept(node, 0);
                classes.put(node.name, node);
                addHierarchy(node);
            }
            try (Stream<Path> stubs = Files.walk(Path.of(args[3]))) {
                for (Path stub : stubs.filter(p -> p.toString().endsWith(".class")).toList()) {
                    ClassNode node = new ClassNode();
                    new ClassReader(Files.readAllBytes(stub)).accept(node, 0);
                    check(!node.name.startsWith("com/hex/"), "stub shadows a mod class: " + node.name);
                    check(!classes.containsKey(node.name), "stub class was packaged: " + node.name);
                    addHierarchy(node);
                    Set<String> declared = new HashSet<>();
                    for (MethodNode method : node.methods) declared.add(method.name + " " + method.desc);
                    STUB_METHODS.put(node.name, declared);
                }
            }
            collectProven(input);
        }
        Set<String> audited = new TreeSet<>();
        for (String name : classes.keySet()) if (name.startsWith(EXPANSION) || CHANGED_CLASSES.contains(name)) audited.add(name);
        check(audited.stream().filter(n -> n.startsWith(EXPANSION)).count() > 10, "expansion classes missing from the JAR");
        int analyzed = 0;
        for (String name : audited) {
            ClassNode node = classes.get(name);
            for (MethodNode body : node.methods) {
                if ((body.access & (ACC_ABSTRACT | ACC_NATIVE)) == 0) {
                    analyze(node, body);
                    analyzed++;
                }
            }
        }
        loadAllowlist(Path.of(args[2]));
        audit(classes, audited, Path.of(args[2]));
        System.out.println("Verified: 1.0.2 grapple/particle contracts, 1.1.0 hooks, type-checked " + analyzed + " methods in "
            + audited.size() + " classes, and every external reference is proven or allowlisted.");
    }
}

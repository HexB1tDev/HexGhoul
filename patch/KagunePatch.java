import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.FieldNode;
import jdk.internal.org.objectweb.asm.tree.InsnNode;
import jdk.internal.org.objectweb.asm.tree.IntInsnNode;
import jdk.internal.org.objectweb.asm.tree.InvokeDynamicInsnNode;
import jdk.internal.org.objectweb.asm.tree.LdcInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.FrameNode;
import jdk.internal.org.objectweb.asm.tree.JumpInsnNode;
import jdk.internal.org.objectweb.asm.tree.LabelNode;
import jdk.internal.org.objectweb.asm.tree.VarInsnNode;
import jdk.internal.org.objectweb.asm.tree.InsnList;

/**
 * Narrow, reproducible patch of the supplied Forge 1.20.1 runtime JAR.
 * 1.0.2: grapple/leap and limb-particle changes. 1.1.0: access widening and straight-line trampolines
 * that hand M1 swipes, tendril strike paths, the manifestation pose and mod start-up to the
 * {@code com.hex.kagune.control} expansion. 1.2.0: the profile NBT root is widened so the expansion can
 * store the chosen kagune path next to the original keys. No Minecraft class is modified.
 */
public final class KagunePatch implements Opcodes {
    private static final String CONTROLLER = "com/hex/kagune/gameplay/KaguneServerController";
    private static final String STATE = "com/hex/kagune/gameplay/KaguneRuntime$State";
    private static final String AURA = "com/hex/kagune/client/KaguneAuraEffects";
    private static final String COOLDOWN = "slingshotCooldownUntil";
    /** 3.0.0: the grapple's flight onto the grabbed block. */
    static final String SLINGSHOT = "com/hex/kagune/control/server/RinkakuSlingshot";
    static final String VERSION = "1.3.17";
    private static final String PLAYER = "net/minecraft/server/level/ServerPlayer";
    private static final String VECTOR = "net/minecraft/world/phys/Vec3";
    // 1.1.0 kagune control expansion hooks
    private static final String MOD = "com/hex/kagune/KaguneMod";
    private static final String PROFILE = "com/hex/kagune/data/KaguneProfile";
    private static final String WORLD_RENDERER = "com/hex/kagune/client/render/KaguneWorldRenderer";
    private static final String HUNT = "com/hex/kagune/control/server/CentipedeHunt";
    private static final String AMBIENT = "com/hex/kagune/client/KaguneAmbientSounds";
    private static final String CLIENT_STATE = "com/hex/kagune/client/KaguneClientState";
    private static final String UKAKU_CLIENT = "com/hex/kagune/control/client/UkakuClient";
    static final String VIEW_LOOKUP = "(Lnet/minecraft/world/entity/player/Player;)Lcom/hex/kagune/client/KaguneClientState$View;";
    private static final String NETWORK = "com/hex/kagune/network/KaguneNetwork";
    private static final String TENDRIL = "com/hex/kagune/client/render/TendrilChain";
    private static final String VIEW = "com/hex/kagune/client/KaguneClientState$View";
    private static final String MANIFEST = "com/hex/kagune/client/KaguneManifestationEffects";
    private static final String CLIENT_PLAYER = "net/minecraft/client/player/AbstractClientPlayer";
    private static final String SERVER_ENTITY = "net/minecraft/world/entity/Entity";
    private static final String BOOTSTRAP = "com/hex/kagune/control/KaguneControlBootstrap";
    private static final String COMBAT = "com/hex/kagune/control/server/KaguneCombat";
    private static final String PATHS = "com/hex/kagune/control/server/PathServer";
    private static final String VORTEX = "com/hex/kagune/control/server/UkakuVortex";
    private static final String GATES = "com/hex/kagune/control/server/UkakuGates";
    private static final String SHIELD = "com/hex/kagune/control/server/TendrilShieldServer";
    private static final String FORMS = "com/hex/kagune/control/server/UkakuForms";
    static final String FORM = "com/hex/kagune/gameplay/KaguneForm";
    static final String EVENTS_SWITCH = "com/hex/kagune/gameplay/KaguneEvents$1";
    static final String MUTATION = "com/hex/kagune/gameplay/MutationNode";
    static final String SCREEN = "com/hex/kagune/client/screen/KaguneScreen";
    static final String RINKAKU_FORMS = "com/hex/kagune/control/RinkakuForms";
    static final String SPIKE_RENDERER = "com/hex/kagune/client/render/KaguneSpikeRenderer";
    static final String ENERGY_HUD = "com/hex/kagune/client/KaguneEnergyHud";
    /** The original spike's model scale, and the long thin RC needle it becomes for Ukaku's Shard Volley (length, then girth x2). */
    static final float SPIKE_SCALE = 1.35F;
    static final float[] SHARD_SCALE = {0.85F, 0.32F, 0.32F};
    private static final String STRIKES = "com/hex/kagune/control/client/render/StrikePaths";
    private static final String BODY = "com/hex/kagune/control/client/animation/BodyAnimations";
    /** Private controller helpers the expansion reuses instead of duplicating (access widened only). */
    static final String[] CONTROLLER_REUSED = {
        "tickSwipe", "validSwipeTarget", "clearLineTo", "applyCentipedeBleed", "multiTendrilFollowUp", "playAt", "playSelf",
        "vary", "formPitch", "center", "traceLiving", "validHintedLiving", "traceBlock", "useEnergy", "takeNextAttackLimb",
        "interruptCentipedeIfUsingLimb", "committed", "beginRetract", "isHostileTo", "refreshFormEffect", "releaseHeldSlot",
        "returnToHoldingOrRetract", "centipedeHit", "centipedeCrushHit", "captureAutonomousCentipedeTarget", "syncKaguneHeldPlayer"
    };
    /** 1.2.0: tendril-only moves the Ukaku path refuses (UkakuGates); everyone else still runs the original. */
    static final String[] UKAKU_GATED = {
        "slingshot", "grabOrThrow", "throwHeld", "crush", "lift", "slam", "toggleThreatAutoAttack", "tryStartFallArrest"
    };

    /**
     * 1.2.5: what the original 1.0.0 jar calls its own members and text, and what the mod is built with instead: no character of the
     * show is named anywhere in the mod (the kakuja mask's method on the form and on the world renderer, and the menu's line on it).
     * The old names are written here only to be found in the original jar; the build checks none reaches the mod's jar.
     */
    static final String[][] ORIGINAL_NAMES = {{"emitKanekiMask", "emitRinkakuMask"}, {"hasKanekiMask", "hasRinkakuMask"}};
    static final String[][] ORIGINAL_TEXT = {{"Kaneki mask / ordered limbs", "Rinkaku mask / ordered limbs"}};

    private static String renamedMember(String name) {
        for (String[] pair : ORIGINAL_NAMES) {
            if (pair[0].equals(name)) {
                return pair[1];
            }
        }
        return null;
    }

    private static String renamedText(String text) {
        for (String[] pair : ORIGINAL_TEXT) {
            text = text.replace(pair[0], pair[1]);
        }
        return text;
    }

    /** 1.2.5: {@link #ORIGINAL_NAMES} and {@link #ORIGINAL_TEXT} renamed through {@code node}; true if anything was. */
    static boolean renameOriginal(ClassNode node) {
        boolean changed = false;
        for (MethodNode method : node.methods) {
            String to = renamedMember(method.name);
            if (to != null) {
                method.name = to;
                changed = true;
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode call && (to = renamedMember(call.name)) != null) {
                    call.name = to;
                    changed = true;
                } else if (instruction instanceof LdcInsnNode constant && constant.cst instanceof String text && !renamedText(text).equals(text)) {
                    constant.cst = renamedText(text);
                    changed = true;
                } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                    for (int k = 0; k < dynamic.bsmArgs.length; k++) {
                        Object arg = dynamic.bsmArgs[k];
                        if (arg instanceof String text && !renamedText(text).equals(text)) {
                            dynamic.bsmArgs[k] = renamedText(text);
                            changed = true;
                        } else if (arg instanceof Handle handle && (to = renamedMember(handle.getName())) != null) {
                            dynamic.bsmArgs[k] = new Handle(handle.getTag(), handle.getOwner(), to, handle.getDesc(), handle.isInterface());
                            changed = true;
                        }
                    }
                }
            }
        }
        return changed;
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(m -> m.name.equals(name)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing " + node.name + "." + name));
    }

    private static void expect(int actual, int expected, String description) {
        if (actual != expected) throw new IllegalStateException(description + ": expected " + expected + ", got " + actual);
    }

    private static void patchCooldown(MethodNode method, String operation, long oldTicks, long newTicks) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.owner.equals(STATE)
                    && field.name.equals("inputCooldownUntil")
                    && field.getOpcode() == (operation.equals("read") ? GETFIELD : PUTFIELD)) {
                if (operation.equals("write")) {
                    AbstractInsnNode add = field.getPrevious();
                    AbstractInsnNode amount = add.getPrevious();
                    if (add.getOpcode() != LADD || !(amount instanceof LdcInsnNode constant)
                            || !Long.valueOf(oldTicks).equals(constant.cst)) {
                        throw new IllegalStateException("Unexpected grapple cooldown expression");
                    }
                    constant.cst = newTicks;
                }
                field.name = COOLDOWN;
                count++;
            }
        }
        expect(count, 1, method.name + " grapple cooldown " + operation);
    }

    private static void patchEnergy(MethodNode method) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(CONTROLLER)
                    && call.name.equals("useEnergy")) {
                call.name = "freeGrapple";
                count++;
            }
        }
        expect(count, 1, method.name + " energy bypass");
    }

    private static void changeNumber(MethodNode method, double from, double to) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof LdcInsnNode constant && Double.valueOf(from).equals(constant.cst)) {
                constant.cst = to;
                count++;
            }
        }
        expect(count, 1, method.name + " " + from);
    }

    private static void patchController(ClassNode node) {
        MethodNode grapple = method(node, "slingshot");
        patchCooldown(grapple, "read", 0, 0);
        patchCooldown(grapple, "write", 3, 3);
        patchEnergy(grapple);
        int clearPriorBoost = 0;
        for (AbstractInsnNode instruction : grapple.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(STATE) && call.name.equals("begin")) {
                // 3.0.0: a new grapple ends a flight still under way (RinkakuSlingshot).
                InsnList clear = new InsnList();
                clear.add(new VarInsnNode(ALOAD, 0)); // player; local 0 in slingshot
                clear.add(new MethodInsnNode(INVOKESTATIC, SLINGSHOT, "cancel", "(L" + PLAYER + ";)V", false));
                grapple.instructions.insert(call, clear);
                clearPriorBoost++;
            }
        }
        expect(clearPriorBoost, 1, "cancel prior flight on a new grapple");
        MethodNode launch = method(node, "launchSlingshot");
        patchCooldown(launch, "write", 12, 10); // 0.5 seconds at 20 TPS
        patchEnergy(launch);
        changeNumber(launch, 4.0, 8.0); // block center + 8 gives launch clearance while passing the anchor
        changeNumber(launch, 0.19, 0.23);
        changeNumber(launch, 3.4, 4.4);
        changeNumber(launch, 4.6, 6.0);

        int launchHook = 0;
        for (AbstractInsnNode instruction : launch.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(PLAYER)
                    && call.name.equals("m_20256_") && call.desc.equals("(L" + VECTOR + ";)V")) {
                // 3.0.0: the tendril hauls him onto the top of the grabbed block (RinkakuSlingshot flies him there).
                InsnList extra = new InsnList();
                extra.add(new VarInsnNode(ALOAD, 1)); // State
                extra.add(new VarInsnNode(ALOAD, 5)); // launchTarget (the block's centre, 8 up)
                launch.instructions.insertBefore(call, extra);
                call.setOpcode(INVOKESTATIC);
                call.owner = SLINGSHOT;
                call.name = "launch";
                call.desc = "(L" + PLAYER + ";L" + VECTOR + ";L" + STATE + ";L" + VECTOR + ";)V";
                launchHook++;
            }
        }
        expect(launchHook, 1, "launch flight hook");

        MethodNode free = new MethodNode(ACC_PRIVATE | ACC_STATIC, "freeGrapple",
                "(Lnet/minecraft/server/level/ServerPlayer;FZ)Z", null, null);
        free.instructions.add(new InsnNode(ICONST_1));
        free.instructions.add(new InsnNode(IRETURN));
        node.methods.add(free);
    }

    private static void patchAura(ClassNode node) {
        // This function exclusively emits dust and Photon wisps along the limb paths.
        // Manifestation origin effects (at the player's back) live in a separate class.
        MethodNode spawn = method(node, "spawnFor");
        boolean hadParticle = false;
        for (AbstractInsnNode instruction : spawn.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("m_7106_")) hadParticle = true;
        }
        if (!hadParticle) throw new IllegalStateException("Expected limb particle emitter was not found");
        spawn.instructions.clear();
        spawn.tryCatchBlocks.clear();
        if (spawn.localVariables != null) spawn.localVariables.clear();
        spawn.instructions.add(new InsnNode(RETURN));
    }

    private static int widen(ClassNode node, String name) {
        int count = 0;
        for (MethodNode method : node.methods) {
            if (method.name.equals(name)) {
                method.access = method.access & ~(ACC_PRIVATE | ACC_PROTECTED) | ACC_PUBLIC;
                count++;
            }
        }
        if (count == 0) throw new IllegalStateException("Missing " + node.name + "." + name);
        return count;
    }

    /**
     * 6.0.4: inside {@code addMastery(player, action, amount)} the one call that added the amount straight to the mastery goes to
     * {@code MasteryGain.earn} instead (the action's own tally is left as it was).
     */
    private static void patchMasteryGain(ClassNode profile) {
        String earnDesc = "(L" + PLAYER + ";Ljava/lang/String;I)V";
        MethodNode earn = profile.methods.stream().filter(m -> m.name.equals("addMastery") && m.desc.equals(earnDesc)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing KaguneProfile.addMastery" + earnDesc));
        int count = 0;
        for (AbstractInsnNode instruction : earn.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == INVOKESTATIC && call.owner.equals(PROFILE)
                    && call.name.equals("addMastery") && call.desc.equals("(L" + PLAYER + ";I)V")) {
                call.owner = "com/hex/kagune/control/server/MasteryGain";
                call.name = "earn";
                count++;
            }
        }
        expect(count, 1, "action mastery curve");
    }

    /** Renames {@code name} to {@code name + "Legacy"} (public) and adds a same-signature trampoline to {@code owner.target}. */
    private static void trampoline(ClassNode node, String name, String owner, String target) {
        MethodNode original = method(node, name);
        if (node.methods.stream().filter(m -> m.name.equals(name)).count() != 1)
            throw new IllegalStateException("Ambiguous trampoline source " + node.name + "." + name);
        if ((original.access & ACC_STATIC) == 0) throw new IllegalStateException("Trampolines are for static methods: " + name);
        int access = original.access;
        original.name = name + "Legacy";
        original.access = original.access & ~(ACC_PRIVATE | ACC_PROTECTED) | ACC_PUBLIC;
        MethodNode bridge = new MethodNode(access, name, original.desc, null, null);
        jdk.internal.org.objectweb.asm.Type[] arguments = jdk.internal.org.objectweb.asm.Type.getArgumentTypes(original.desc);
        int slot = 0;
        for (jdk.internal.org.objectweb.asm.Type argument : arguments) {
            bridge.instructions.add(new VarInsnNode(argument.getOpcode(ILOAD), slot));
            slot += argument.getSize();
        }
        bridge.instructions.add(new MethodInsnNode(INVOKESTATIC, owner, target, original.desc, false));
        bridge.instructions.add(new InsnNode(jdk.internal.org.objectweb.asm.Type.getReturnType(original.desc).getOpcode(IRETURN)));
        node.methods.add(bridge);
    }

    /**
     * A trampoline for an event listener: as {@link #trampoline}, and its {@code @SubscribeEvent} moves to the bridge, so the
     * event bus calls the bridge (the renamed original keeps its body, no longer subscribed).
     */
    private static void trampolineListener(ClassNode node, String name, String owner, String target) {
        MethodNode original = method(node, name);
        java.util.List<jdk.internal.org.objectweb.asm.tree.AnnotationNode> annotations = original.visibleAnnotations;
        if (annotations == null || annotations.stream().noneMatch(a -> a.desc.equals("Lnet/minecraftforge/eventbus/api/SubscribeEvent;")))
            throw new IllegalStateException("Not an event listener: " + node.name + "." + name);
        trampoline(node, name, owner, target);
        original.visibleAnnotations = null;
        method(node, name).visibleAnnotations = annotations;
    }

    private static void patchControllerForControl(ClassNode node) {
        for (String name : CONTROLLER_REUSED) widen(node, name);
        // Every M1 path (C2S SWIPE action, vanilla attack, instinct) funnels through startSwipe.
        trampoline(node, "startSwipe", COMBAT, "startSwipe");
        // 1.3.0: the single manifest rip is replaced by per-limb sounds every client plays (ManifestSounds).
        MethodNode toggle = method(node, "toggle");
        int rips = 0;
        boolean afterRip = false;
        for (AbstractInsnNode instruction : toggle.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == GETSTATIC && field.name.equals("MANIFEST_RIP")) {
                afterRip = true;
            } else if (afterRip && instruction instanceof MethodInsnNode call && call.owner.equals(CONTROLLER) && call.name.equals("playSelf")) {
                call.owner = PATHS;
                call.name = "manifestRip";
                afterRip = false;
                rips++;
            }
        }
        expect(rips, 1, "manifest rip redirect");
        // 1.2.0: manifesting (the only place the kagune turns active) waits until a path is chosen.
        trampoline(node, "toggle", PATHS, "toggle");
        // 1.2.0: on the Ukaku path the Rapid Strike key is his own move (the Wing Vortex since 1.3.0).
        trampoline(node, "startRapidStrike", VORTEX, "startRapidStrike");
        for (String gated : UKAKU_GATED) trampoline(node, gated, GATES, gated);
        // 1.2.0: the Guard key goes through the Ukaku gate (the Wing Block until 5.0.13; since then nothing on the Ukaku path).
        trampoline(node, "toggleGuard", GATES, "toggleGuard");
        // 1.2.0: on the Ukaku path the tail-cycle key switches wing form.
        trampoline(node, "cycleTendrils", FORMS, "cycleTendrils");
        // 3.0.3: a crush is once: when it ends, what it crushed is let go (RinkakuGrip) instead of held for the next one.
        MethodNode crush = method(node, "tickCrush");
        int ends = 0;
        for (AbstractInsnNode instruction : crush.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == INVOKEVIRTUAL && call.owner.equals(STATE) && call.name.equals("begin")) {
                crush.instructions.insertBefore(call, new VarInsnNode(ALOAD, 0));
                crush.instructions.set(call, new MethodInsnNode(INVOKESTATIC, "com/hex/kagune/control/server/RinkakuGrip", "crushDone",
                    "(L" + STATE + ";Lcom/hex/kagune/gameplay/TendrilMode;JIILnet/minecraft/world/phys/Vec3;Lnet/minecraft/server/level/ServerPlayer;)V",
                    false));
                ends++;
            }
        }
        expect(ends, 1, "crush end");
        // 1.0.42: Rinkaku's grab is slower and can be broken (RinkakuGrab): first thing every tick of its reach, a swing back at him by
        // the one it reaches for breaks it; and it reels its catch in only over the reach's last ticks, not from its 3rd.
        MethodNode reach = method(node, "tickReachEntity");
        String grab = "com/hex/kagune/control/server/RinkakuGrab";
        LabelNode reaching = new LabelNode();
        InsnList broken = new InsnList();
        broken.add(new VarInsnNode(ALOAD, 0));
        broken.add(new VarInsnNode(ALOAD, 1));
        broken.add(new VarInsnNode(LLOAD, 2));
        broken.add(new MethodInsnNode(INVOKESTATIC, grab, "broken", "(Lnet/minecraft/server/level/ServerPlayer;L" + STATE + ";J)Z", false));
        broken.add(new JumpInsnNode(IFEQ, reaching));
        broken.add(new InsnNode(RETURN));
        broken.add(reaching);
        broken.add(new FrameNode(F_SAME, 0, null, 0, null));
        reach.instructions.insert(broken);
        int reels = 0;
        for (AbstractInsnNode instruction : reach.instructions.toArray()) {
            if (instruction instanceof LdcInsnNode constant && Long.valueOf(3L).equals(constant.cst)) {
                reach.instructions.insertBefore(constant, new VarInsnNode(ALOAD, 1));
                reach.instructions.set(constant, new MethodInsnNode(INVOKESTATIC, grab, "reelFrom", "(L" + STATE + ";)J", false));
                reels++;
            }
        }
        expect(reels, 1, "grab reel-in tick");
        // 3.1.0: the centipede legs hunt by CentipedeHunt (nearest threat in a leg's reach, the grab as a sequence); a caught body
        // is placed by it every tick instead of dragged to the original's spot behind him; a leg busy holding can't guard.
        trampoline(node, "tickCentipedeAutoAttack", HUNT, "tickCentipedeAutoAttack");
        trampoline(node, "tryCentipedeBlock", HUNT, "tryCentipedeBlock");
        // 5.0.14: Auto Projectile is a guard: tentacles held across incoming shots (TendrilShieldServer), not one swing at them.
        trampoline(node, "tickAutoProjectile", SHIELD, "tickAutoProjectile");
        trampoline(node, "deflectProjectileImpact", SHIELD, "deflectProjectileImpact");
        // 3.1.0: the centipede legs bleed a body exactly as Ukaku's spikes do (RinkakuBleed -> UkakuCrystalCombat's bleeding and blood).
        trampoline(node, "applyCentipedeBleed", "com/hex/kagune/control/server/RinkakuBleed", "applyCentipedeBleed");
        MethodNode held = method(node, "tickHeldEntities");
        String restrain = "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/LivingEntity;IJ)V";
        int restraints = 0;
        for (AbstractInsnNode instruction : held.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(CONTROLLER) && call.name.equals("restrainCentipedeTarget")
                    && call.desc.equals(restrain)) {
                call.owner = HUNT;
                call.name = "hold";
                restraints++;
            }
        }
        expect(restraints, 1, "centipede hold redirect");
        // 3.0.2: re-adding the centipede forms' Health Boost no longer cuts the health down every few ticks (RinkakuFormBuffs).
        trampoline(node, "refreshFormEffect", "com/hex/kagune/control/server/RinkakuFormBuffs", "refreshFormEffect");
        MethodNode tick = method(node, "tick");
        int redirected = 0;
        for (AbstractInsnNode instruction : tick.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(CONTROLLER) && call.name.equals("tickSwipe")) {
                call.owner = COMBAT;
                redirected++;
            }
        }
        expect(redirected, 1, "SWIPE tick redirect");
    }

    /**
     * 1.0.4: the Dragon (Kakuja V2) has the player's body for its rampage: the original kagune actions return at once while
     * {@code DragonLock.locked} says so (the expansion's own action packets ask it in their handlers).
     */
    private static void patchDragonLock(ClassNode node) {
        MethodNode onAction = node.methods.stream()
            .filter(m -> m.name.equals("onAction") && m.desc.equals("(Lnet/minecraft/server/level/ServerPlayer;Lcom/hex/kagune/gameplay/KaguneAction;I)V"))
            .findFirst().orElseThrow(() -> new IllegalStateException("Missing onAction(ServerPlayer, KaguneAction, int)"));
        LabelNode proceed = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(ALOAD, 0));
        guard.add(new MethodInsnNode(INVOKESTATIC, "com/hex/kagune/control/DragonLock", "locked", "(Lnet/minecraft/server/level/ServerPlayer;)Z", false));
        guard.add(new JumpInsnNode(IFEQ, proceed));
        guard.add(new InsnNode(RETURN));
        guard.add(proceed);
        guard.add(new FrameNode(F_SAME, 0, null, 0, null));
        onAction.instructions.insert(guard);
    }

    /**
     * 1.3.14: a companion holding the player's hand (or clinging to them, hugging them, kissing them) is never one of their targets: the
     * original's target checks ({@code isHostileTo}, {@code validSwipeTarget}) say no at once while {@code EtoBrain.spared} says so.
     */
    private static void patchSpared(ClassNode node, String name) {
        MethodNode check = node.methods.stream()
            .filter(m -> m.name.equals(name) && m.desc.equals("(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/LivingEntity;)Z"))
            .findFirst().orElseThrow(() -> new IllegalStateException("Missing " + name + "(ServerPlayer, LivingEntity)"));
        LabelNode proceed = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(ALOAD, 0));
        guard.add(new VarInsnNode(ALOAD, 1));
        guard.add(new MethodInsnNode(INVOKESTATIC, "com/hex/kagune/control/server/EtoBrain", "spared",
            "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/LivingEntity;)Z", false));
        guard.add(new JumpInsnNode(IFEQ, proceed));
        guard.add(new InsnNode(ICONST_0));
        guard.add(new InsnNode(IRETURN));
        guard.add(proceed);
        guard.add(new FrameNode(F_SAME, 0, null, 0, null));
        check.instructions.insert(guard);
    }

    private static void patchTendrilChain(ClassNode node) {
        widen(node, "thrustTarget");
        widen(node, "isActionLimb");
        MethodNode getter = new MethodNode(ACC_PUBLIC, "kaguneLimbIndex", "()I", null, null);
        getter.instructions.add(new VarInsnNode(ALOAD, 0));
        getter.instructions.add(new FieldInsnNode(GETFIELD, TENDRIL, "limbIndex", "I"));
        getter.instructions.add(new InsnNode(IRETURN));
        node.methods.add(getter);
        String playerFirst = "(L" + CLIENT_PLAYER + ";L" + VIEW + ";";
        int limbCalls = 0;
        for (String host : new String[] {"updateSegmentedCurve", "simulate", "renderPath"}) {
            MethodNode method = method(node, host);
            if ((method.access & ACC_STATIC) != 0 || !method.desc.startsWith(playerFirst))
                throw new IllegalStateException("Unexpected signature for " + host + method.desc);
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(TENDRIL) && call.name.equals("isActionLimb")) {
                    method.instructions.insertBefore(call, new VarInsnNode(ALOAD, 1)); // the rendered player
                    call.setOpcode(INVOKESTATIC);
                    call.owner = STRIKES;
                    call.desc = "(L" + TENDRIL + ";L" + VIEW + ";L" + CLIENT_PLAYER + ";)Z";
                    call.itf = false;
                    limbCalls++;
                }
            }
        }
        expect(limbCalls, 5, "isActionLimb redirects");
        MethodNode action = method(node, "actionTarget");
        if ((action.access & ACC_STATIC) != 0 || !action.desc.startsWith(playerFirst))
            throw new IllegalStateException("Unexpected actionTarget signature " + action.desc);
        String thrust = "(L" + VECTOR + ";L" + VECTOR + ";L" + VECTOR + ";L" + VECTOR + ";L" + VECTOR + ";FD)L" + VECTOR + ";";
        int strikeCalls = 0;
        for (AbstractInsnNode instruction : action.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(TENDRIL) && call.name.equals("thrustTarget")) {
                if (!call.desc.equals(thrust)) throw new IllegalStateException("Unexpected thrustTarget descriptor");
                InsnList extra = new InsnList();
                extra.add(new VarInsnNode(ALOAD, 1)); // player
                extra.add(new VarInsnNode(ALOAD, 2)); // view
                extra.add(new VarInsnNode(ALOAD, 0)); // chain
                action.instructions.insertBefore(call, extra);
                call.owner = STRIKES;
                call.name = "strikeTarget";
                call.desc = "(L" + VECTOR + ";L" + VECTOR + ";L" + VECTOR + ";L" + VECTOR + ";L" + VECTOR + ";FDL"
                        + CLIENT_PLAYER + ";L" + VIEW + ";L" + TENDRIL + ";)L" + VECTOR + ";";
                strikeCalls++;
            }
        }
        expect(strikeCalls, 2, "SWIPE/RAPID_STRIKE path redirects");
        // 3.0.0: every drawn path is kept out of its owner's body right after it is resampled (StrikePaths.clearOwner).
        MethodNode render = method(node, "renderPath");
        String points = "[L" + VECTOR + ";";
        int clearCalls = 0;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(TENDRIL) && call.name.equals("resample")
                    && call.desc.equals("(" + points + ")" + points)) {
                InsnList extra = new InsnList();
                extra.add(new VarInsnNode(ALOAD, 0)); // chain
                extra.add(new VarInsnNode(ALOAD, 1)); // player
                extra.add(new VarInsnNode(ALOAD, 2)); // view
                extra.add(new VarInsnNode(FLOAD, 3)); // partialTick
                extra.add(new MethodInsnNode(INVOKESTATIC, STRIKES, "clearOwner",
                        "(" + points + "L" + TENDRIL + ";L" + CLIENT_PLAYER + ";L" + VIEW + ";F)" + points, false));
                render.instructions.insert(call, extra);
                clearCalls++;
            }
        }
        expect(clearCalls, 1, "render path body clearance");
    }

    private static void patchManifestation(ClassNode node) {
        widen(node, "bodyEffectLayer");
        trampoline(node, "playPain", BODY, "playManifestPain");
        trampoline(node, "stopPain", BODY, "stopManifestPain");
        // 1.2.0: Ukaku's wings bring their own eruption; Rinkaku's origin mist and blood (vanilla poof clouds without
        // Photon) stay on the Rinkaku path.
        trampoline(node, "spawnOriginMist", UKAKU_CLIENT, "spawnOriginMist");
        trampoline(node, "spawnBlood", UKAKU_CLIENT, "spawnManifestBlood");
    }

    private static void patchModConstructor(ClassNode node) {
        MethodNode constructor = method(node, "<init>");
        int hooks = 0;
        for (AbstractInsnNode instruction : constructor.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(NETWORK) && call.name.equals("init")) {
                InsnList extra = new InsnList();
                extra.add(new VarInsnNode(ALOAD, 1)); // FMLJavaModLoadingContext
                extra.add(new MethodInsnNode(INVOKESTATIC, BOOTSTRAP, "init",
                        "(Lnet/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext;)V", false));
                constructor.instructions.insert(call, extra);
                hooks++;
            }
        }
        expect(hooks, 1, "control bootstrap hook");
    }

    /** 1.2.0: Ukaku players' kagune is wings, so the tendril renderer and writhing sounds read them as having no tendrils out. */
    private static void redirectTendrilView(ClassNode node, String methodName) {
        MethodNode method = method(node, methodName);
        int redirected = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == INVOKESTATIC && call.owner.equals(CLIENT_STATE)
                    && call.name.equals("get") && call.desc.equals(VIEW_LOOKUP)) {
                call.owner = UKAKU_CLIENT;
                call.name = "tendrilView";
                redirected++;
            }
        }
        expect(redirected, 1, node.name + "." + methodName + " tendril view redirect");
    }

    /**
     * 1.2.0: the original spike projectile (registered but never spawned by the 1.0.0 code) becomes Ukaku's RC shard; only
     * its three model-scale constants (x = length, y and z = girth) change, from a stubby 1.6-block javelin to a thin
     * 1-block crystal needle like the shards in the anime.
     */
    private static void patchSpikeRenderer(ClassNode node) {
        MethodNode render = method(node, "render");
        int changed = 0;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (instruction instanceof LdcInsnNode constant && Float.valueOf(SPIKE_SCALE).equals(constant.cst)) {
                if (changed < SHARD_SCALE.length) constant.cst = SHARD_SCALE[changed];
                changed++;
            }
        }
        expect(changed, 3, "spike model scale");
    }

    /** The next real instruction after {@code from} (labels, line numbers and frames skipped). */
    private static AbstractInsnNode next(AbstractInsnNode from) {
        AbstractInsnNode at = from.getNext();
        while (at != null && at.getOpcode() < 0) at = at.getNext();
        return at;
    }

    /** The previous real instruction before {@code from}. */
    private static AbstractInsnNode previous(AbstractInsnNode from) {
        AbstractInsnNode at = from.getPrevious();
        while (at != null && at.getOpcode() < 0) at = at.getPrevious();
        return at;
    }

    private static AbstractInsnNode onlyLdc(MethodNode method, Object constant) {
        AbstractInsnNode found = null;
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof LdcInsnNode ldc && constant.equals(ldc.cst)) {
                found = instruction;
                count++;
            }
        }
        expect(count, 1, method.name + " constant " + constant);
        return found;
    }

    /** Rewrites one form constructor call in {@code KaguneForm.<clinit>}: its ordinal, required mastery and armour level. */
    private static void reslotForm(MethodNode init, String name, int ordinal, int newOrdinal, int mastery, int newMastery, int armor, int newArmor) {
        AbstractInsnNode enumName = onlyLdc(init, name);
        AbstractInsnNode slot = next(enumName);
        expect(slot.getOpcode(), ICONST_0 + ordinal, name + " ordinal");
        init.instructions.set(slot, new InsnNode(ICONST_0 + newOrdinal));
        AbstractInsnNode required = next(next(next(next(enumName))));
        if (!(required instanceof IntInsnNode push) || push.getOpcode() != SIPUSH || push.operand != mastery)
            throw new IllegalStateException(name + " required mastery is not " + mastery);
        push.operand = newMastery;
        AbstractInsnNode armour = next(required);
        expect(armour.getOpcode(), ICONST_0 + armor, name + " armour level");
        init.instructions.set(armour, new InsnNode(ICONST_0 + newArmor));
    }

    /** Rewrites the required mastery ({@code expected}) of one enum constant in {@code init} (KaguneForm or MutationNode). */
    private static void setMastery(MethodNode init, String name, int expected, int value) {
        AbstractInsnNode required = next(next(next(next(onlyLdc(init, name)))));
        if (!(required instanceof IntInsnNode push) || push.operand != expected)
            throw new IllegalStateException(name + " required mastery is not " + expected);
        init.instructions.set(required, value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE ? new IntInsnNode(BIPUSH, value)
            : value >= Short.MIN_VALUE && value <= Short.MAX_VALUE ? new IntInsnNode(SIPUSH, value) : new LdcInsnNode(value));
    }

    /** Replaces every {@code SIPUSH expected} in {@code method} with {@code SIPUSH value}; returns how many changed. */
    private static int replacePush(MethodNode method, int expected, int value) {
        int replaced = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof IntInsnNode push && push.getOpcode() == SIPUSH && push.operand == expected) {
                push.operand = value;
                replaced++;
            }
        }
        return replaced;
    }

    /** Points every {@code KaguneForm.values()} in {@code method} at {@code RinkakuForms.forms()} (the forms a player can have). */
    private static int redirectValues(MethodNode method) {
        int redirected = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == INVOKESTATIC && call.owner.equals(FORM)
                    && call.name.equals("values")) {
                call.owner = RINKAKU_FORMS;
                call.name = "forms";
                redirected++;
            }
        }
        return redirected;
    }

    /** Points every {@code MutationNode.values()} in {@code method} at {@code RinkakuMutations.nodes()}; returns how many changed. */
    private static int redirectMutations(MethodNode method) {
        int redirected = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == INVOKESTATIC && call.owner.equals(MUTATION)
                    && call.name.equals("values")) {
                call.owner = "com/hex/kagune/control/RinkakuMutations";
                call.name = "nodes";
                redirected++;
            }
        }
        return redirected;
    }

    /** 3.0.3: the mutations the user cut (RinkakuMutations), with the mastery each one had. */
    static final String[] CUT_MUTATIONS = {"MASS", "AGILITY", "ARM_WRAP", "GUARD_MEMBRANE", "MULTI_CONTROL", "REGEN_RESERVOIR", "PARTIAL_ARMOR",
        "GRAPPLER_FRAME", "FULL_MASS", "FULL_GRAPPLER"};
    static final int[] CUT_MASTERY = {100, 100, 250, 250, 450, 450, 750, 750, 1000, 1000};

    /**
     * 3.0.2: the third mutation tier moves from 450 to 500 with the Four-Tail Rinkaku. 3.0.3: Rinkaku keeps five mutations, one a
     * tier, and can take all of them: the ten cut ones never unlock, no pick locks out another and none needs one before it.
     */
    private static void patchMutationTiers(ClassNode mutation) {
        MethodNode init = method(mutation, "<clinit>");
        setMastery(init, "CENTIPEDE_BODY", 450, 500);
        for (int i = 0; i < CUT_MUTATIONS.length; i++) setMastery(init, CUT_MUTATIONS[i], CUT_MASTERY[i], Integer.MAX_VALUE);
        // 5.0.14: Auto Projectile guards with tentacles held across the shots instead of slapping them away.
        setDescription(init, "WARNING: Always autonomous while manifested. One random non-Centipede tendril slaps away incoming projectiles, consuming RC.",
            "WARNING: Always autonomous while manifested. Tendrils swing across incoming projectiles to guard you, one per direction, consuming RC.");
        setDescription(init, "WARNING: Always autonomous while manifested. Unlocks Auto Projectile with a lower RC cost and harder deflection.",
            "WARNING: Always autonomous while manifested. Unlocks Auto Projectile: cheaper, sturdier guards that send arrows back.");
        replaceBody(method(mutation, "conflicts"), ICONST_0);
        replaceBody(method(mutation, "prerequisitesMet"), ICONST_1);
    }

    /** Replaces the one string constant {@code expected} in {@code init} with {@code value}. */
    private static void setDescription(MethodNode init, String expected, String value) {
        int found = 0;
        for (AbstractInsnNode instruction : init.instructions.toArray()) {
            if (instruction instanceof LdcInsnNode constant && expected.equals(constant.cst)) {
                constant.cst = value;
                found++;
            }
        }
        expect(found, 1, "mutation description");
    }

    /** Makes {@code method} return the int/boolean constant {@code constant} (ICONST_0 or ICONST_1) and nothing else. */
    private static void replaceBody(MethodNode method, int constant) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        if (method.localVariables != null) method.localVariables.clear();
        method.instructions.add(new InsnNode(constant));
        method.instructions.add(new InsnNode(IRETURN));
        method.maxStack = 1;
    }

    /** Swaps every {@code GETSTATIC KaguneForm.a} with {@code KaguneForm.b} in {@code method}; returns how many changed. */
    private static int swapForms(MethodNode method, String a, String b) {
        int swapped = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == GETSTATIC && field.owner.equals(FORM)) {
                if (field.name.equals(a)) {
                    field.name = b;
                    swapped++;
                } else if (field.name.equals(b)) {
                    field.name = a;
                    swapped++;
                }
            }
        }
        return swapped;
    }

    /**
     * 3.0.1: the Centipede Form and the Partial Kakuja trade places, so Rinkaku's last two forms are always his kakuja: Rinkaku
     * Seed 0, Twin 100, Four-Tail 250, Partial Kakuja 450, Centipede Form 750 (first kakuja), Full Kakuja 1000 (final kakuja).
     * Slot (ordinal and {@code values()} order, which the menu draws in), required mastery and armour level ({@code isKakuja} is
     * armour 2+) move; ids, names, limbs and combos stay with each form, so stored forms load as before. The damage multiplier
     * and the hurt reduction belong to the slot, so power keeps climbing with mastery.
     */
    private static void patchFormOrder(ClassNode form, ClassNode formSwitch, ClassNode eventsSwitch) {
        MethodNode init = method(form, "<clinit>");
        reslotForm(init, "CENTIPEDE", 3, 4, 450, 750, 1, 2);
        reslotForm(init, "PARTIAL_KAKUJA", 4, 3, 750, 450, 2, 1);
        // 3.0.2: the Partial Kakuja is gone. It stays an enum constant (switch maps, saves and packets index forms by ordinal) but
        // never unlocks, and the lists players see (the menu, /kagune form, stored ids) skip it (RinkakuForms.forms). The forms
        // left come every 250 mastery: Seed 0, Twin 250, Four-Tail 500, the first kakuja 750, the final kakuja 1000.
        setMastery(init, "PARTIAL_KAKUJA", 450, Integer.MAX_VALUE);
        setMastery(init, "RINKAKU_DUAL", 100, 250);
        setMastery(init, "RINKAKU_QUAD", 250, 500);
        expect(redirectValues(method(form, "find")), 1, "form lookup list");
        expect(redirectValues(method(form, "ids")), 1, "form id list");
        expect(swapForms(method(form, "$values"), "CENTIPEDE", "PARTIAL_KAKUJA"), 2, "form values order");
        // damageMultiplier: the 450 form hits as the Centipede did (1.3), the first kakuja as the Partial did (1.42).
        expect(swapForms(method(formSwitch, "<clinit>"), "CENTIPEDE", "PARTIAL_KAKUJA"), 2, "damage multiplier slots");
        // defensiveFormsAndRegenerationDelay: the 0.9 hurt reduction of the 750 kakuja goes with the first kakuja.
        int defence = 0;
        for (AbstractInsnNode instruction : method(eventsSwitch, "<clinit>").instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == GETSTATIC && field.owner.equals(FORM)
                    && field.name.equals("PARTIAL_KAKUJA")) {
                field.name = "CENTIPEDE";
                defence++;
            }
        }
        expect(defence, 1, "kakuja hurt reduction slot");
    }

    /** 3.0.1: the menu's form buttons turn purple for the two kakuja, and the 750 stage reads as the Centipede Kakuja. */
    private static void patchFormMenu(ClassNode screen) {
        LdcInsnNode stage = (LdcInsnNode)onlyLdc(method(screen, "stageName"), "Partial Kakuja");
        stage.cst = "Centipede Kakuja";
        MethodNode draw = method(screen, "drawPreview");
        // 3.0.3: the board and the identity panel list only the five mutations Rinkaku keeps, all of which can be taken.
        expect(redirectMutations(method(screen, "drawMutationTree")), 2, "board mutation lists");
        expect(redirectMutations(method(screen, "drawIdentity")), 1, "identity mutation list");
        ((LdcInsnNode)onlyLdc(method(screen, "drawMutationTree"), "Tier choice locks its siblings. /kagune reset starts over.")).cst =
            "Every mutation can be taken. /kagune reset starts over.";
        // 3.0.2: the Partial Kakuja has no button; the 450 milestone (stage name, next milestone, third mutation tier) is now 500.
        expect(redirectValues(draw), 1, "menu form list");
        expect(replacePush(method(screen, "stageName"), 450, 500), 1, "menu stage at 450");
        expect(replacePush(method(screen, "nextMilestone"), 450, 500), 2, "menu milestone at 450");
        expect(replacePush(method(screen, "drawMutationTree"), 450, 500), 1, "menu mutation tier at 450");
        // for (KaguneForm form : values()) { unlocked = form.isUnlocked(view.mastery()); selected = form == view.form(); ... }
        MethodInsnNode isUnlocked = null;
        int unlockedCalls = 0;
        for (AbstractInsnNode instruction : draw.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(FORM) && call.name.equals("isUnlocked")) {
                isUnlocked = call;
                unlockedCalls++;
            }
        }
        expect(unlockedCalls, 1, "menu form unlock check");
        if (!(previous(previous(previous(isUnlocked))) instanceof VarInsnNode formLoad) || formLoad.getOpcode() != ALOAD)
            throw new IllegalStateException("Unexpected menu form loop");
        int formVar = formLoad.var;
        // colour = selected ? 0xFFB51E46 : unlocked ? 0xFF5D1830 : 0xFF2E2228;
        AbstractInsnNode selectedFill = onlyLdc(draw, 0xFFB51E46);
        if (!(previous(previous(selectedFill)) instanceof VarInsnNode selectedLoad) || selectedLoad.getOpcode() != ILOAD)
            throw new IllegalStateException("Unexpected selected form check");
        if (!(next(next(selectedFill)) instanceof VarInsnNode unlockedLoad) || unlockedLoad.getOpcode() != ILOAD)
            throw new IllegalStateException("Unexpected unlocked form check");
        AbstractInsnNode store = next(onlyLdc(draw, 0xFF2E2228));
        expect(store.getOpcode(), ISTORE, "menu form button colour store");
        InsnList fill = new InsnList();
        fill.add(new VarInsnNode(ALOAD, formVar));
        fill.add(new VarInsnNode(ILOAD, selectedLoad.var));
        fill.add(new VarInsnNode(ILOAD, unlockedLoad.var));
        fill.add(new MethodInsnNode(INVOKESTATIC, RINKAKU_FORMS, "buttonColor", "(IL" + FORM + ";ZZ)I", false));
        draw.instructions.insertBefore(store, fill);
        // label colour = unlocked ? 0xFFF6DDE4 : 0xFF705860, handed straight to drawCenteredString
        AbstractInsnNode labelDraw = next(onlyLdc(draw, 0xFF705860));
        if (!(labelDraw instanceof MethodInsnNode centered) || !centered.name.equals("m_280137_"))
            throw new IllegalStateException("Unexpected menu form label draw");
        InsnList label = new InsnList();
        label.add(new VarInsnNode(ALOAD, formVar));
        label.add(new VarInsnNode(ILOAD, unlockedLoad.var));
        label.add(new MethodInsnNode(INVOKESTATIC, RINKAKU_FORMS, "labelColor", "(IL" + FORM + ";Z)I", false));
        draw.instructions.insertBefore(labelDraw, label);
    }

    private static void replaceText(Map<String, byte[]> entries, String path, String original, String replacement) {
        String text = new String(entries.get(path), StandardCharsets.UTF_8);
        if (text.indexOf(original) < 0 || text.indexOf(original) != text.lastIndexOf(original))
            throw new IllegalStateException("Expected one version marker in " + path);
        entries.put(path, text.replace(original, replacement).getBytes(StandardCharsets.UTF_8));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: KagunePatch input.jar output.jar");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipFile input = new ZipFile(args[0])) {
            var cursor = input.entries();
            while (cursor.hasMoreElements()) {
                ZipEntry entry = cursor.nextElement();
                entries.put(entry.getName(), input.getInputStream(entry).readAllBytes());
            }
        }
        // 1.2.5: first, the original's own names for the show's characters (ORIGINAL_NAMES), in every class that has them.
        int renamedClasses = 0;
        for (var entry : entries.entrySet()) {
            if (entry.getKey().endsWith(".class")) {
                ClassNode node = read(entry.getValue());
                if (renameOriginal(node)) {
                    entry.setValue(write(node));
                    renamedClasses++;
                }
            }
        }
        expect(renamedClasses, 3, "original classes renamed (the form, the world renderer, the menu)");
        ClassNode controller = read(entries.get(CONTROLLER + ".class"));
        ClassNode state = read(entries.get(STATE + ".class"));
        ClassNode aura = read(entries.get(AURA + ".class"));
        if (state.fields.stream().anyMatch(f -> f.name.equals(COOLDOWN)))
            throw new IllegalStateException("Input JAR has already been patched");
        state.fields.add(new FieldNode(ACC_PUBLIC, COOLDOWN, "J", null, null));
        patchController(controller);
        patchControllerForControl(controller);
        patchDragonLock(controller);
        patchSpared(controller, "isHostileTo");
        patchSpared(controller, "validSwipeTarget");
        patchAura(aura);
        ClassNode tendril = read(entries.get(TENDRIL + ".class"));
        ClassNode manifest = read(entries.get(MANIFEST + ".class"));
        ClassNode mod = read(entries.get(MOD + ".class"));
        patchTendrilChain(tendril);
        patchManifestation(manifest);
        patchModConstructor(mod);
        ClassNode profile = read(entries.get(PROFILE + ".class"));
        // 1.2.0: the chosen path lives inside the profile root, so death-cloning carries it and /kagune reset clears it.
        expect(widen(profile, "root"), 1, "profile root widening");
        // 3.0.3: a cut mutation never counts and the guard is everyone's (RinkakuMutationsServer).
        trampoline(profile, "hasMutation", "com/hex/kagune/control/server/RinkakuMutationsServer", "hasMutation");
        // 3.0.3: every Rinkaku hit is brought down with Ukaku's (DamageBalance).
        for (String damage : new String[] {"swipeDamage", "crushDamage", "slamDamage"})
            trampoline(profile, damage, "com/hex/kagune/control/server/KaguneBalance", damage);
        // 6.0.4: what gameplay earns goes up the mastery curve (MasteryGain); the command's own adds stay exact.
        patchMasteryGain(profile);
        // 6.1.0: mastery takes more off what every move costs (EnergyBalance, through KaguneBalance).
        trampoline(profile, "actionEnergyCost", "com/hex/kagune/control/server/KaguneBalance", "actionEnergyCost");
        // 7.0.0: RC, healing and food are one bar (SharedBar): RC reads and writes the flesh reserve while a ghoul lives on it.
        for (String energy : new String[] {"energy", "setEnergy", "consumeEnergy", "restoreEnergy"})
            trampoline(profile, energy, "com/hex/kagune/control/server/SharedBar", energy);
        entries.put(CONTROLLER + ".class", write(controller));
        entries.put(STATE + ".class", write(state));
        entries.put(AURA + ".class", write(aura));
        entries.put(TENDRIL + ".class", write(tendril));
        entries.put(MANIFEST + ".class", write(manifest));
        entries.put(MOD + ".class", write(mod));
        entries.put(PROFILE + ".class", write(profile));
        ClassNode worldRenderer = read(entries.get(WORLD_RENDERER + ".class"));
        ClassNode ambient = read(entries.get(AMBIENT + ".class"));
        redirectTendrilView(worldRenderer, "renderWorld");
        // 3.0.0: Rinkaku's kakuja mask is drawn on the head model part (RinkakuMaskLayer), not in the world beside it.
        trampoline(worldRenderer, "emitRinkakuMask", "com/hex/kagune/control/client/render/RinkakuMaskLayer", "emitRinkakuMask");
        // 3.1.0: Rinkaku's centipede legs are drawn by CentipedeRenderer (plated legs on their own springs); the original's tube
        // still draws every other limb. Its limb deployment (manifest/dismiss/reshape timing) is shared.
        trampoline(worldRenderer, "emitTube", "com/hex/kagune/control/client/render/CentipedeRenderer", "emitTube");
        expect(widen(worldRenderer, "limbDeployment"), 1, "limb deployment widening");
        // 5.0.0: Rinkaku's kakuja burst out of his body and shatter (KakujaFx): the limb deployment goes through it.
        trampoline(worldRenderer, "limbDeployment", "com/hex/kagune/control/client/fx/KakujaFx", "limbDeployment");
        redirectTendrilView(ambient, "clientTick");
        entries.put(WORLD_RENDERER + ".class", write(worldRenderer));
        entries.put(AMBIENT + ".class", write(ambient));
        // 7.0.0: the original RC bar is not drawn while the shared bar (ReserveHud) shows: it is the same bar.
        ClassNode energyHud = read(entries.get(ENERGY_HUD + ".class"));
        trampolineListener(energyHud, "render", "com/hex/kagune/control/client/ReserveHud", "energyHud");
        entries.put(ENERGY_HUD + ".class", write(energyHud));
        ClassNode spike = read(entries.get(SPIKE_RENDERER + ".class"));
        patchSpikeRenderer(spike);
        entries.put(SPIKE_RENDERER + ".class", write(spike));
        ClassNode form = read(entries.get(FORM + ".class"));
        ClassNode formSwitch = read(entries.get(FORM + "$1.class"));
        ClassNode eventsSwitch = read(entries.get(EVENTS_SWITCH + ".class"));
        patchFormOrder(form, formSwitch, eventsSwitch);
        entries.put(FORM + ".class", write(form));
        entries.put(FORM + "$1.class", write(formSwitch));
        entries.put(EVENTS_SWITCH + ".class", write(eventsSwitch));
        ClassNode mutation = read(entries.get(MUTATION + ".class"));
        patchMutationTiers(mutation);
        entries.put(MUTATION + ".class", write(mutation));
        ClassNode screen = read(entries.get(SCREEN + ".class"));
        patchFormMenu(screen);
        entries.put(SCREEN + ".class", write(screen));
        replaceText(entries, "META-INF/mods.toml", "version=\"1.0.0\"", "version=\"" + VERSION + "\"");
        replaceText(entries, "META-INF/MANIFEST.MF", "Implementation-Version: 1.0.0", "Implementation-Version: " + VERSION);
        Path output = Path.of(args[1]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (ZipOutputStream archive = new ZipOutputStream(Files.newOutputStream(output))) {
            for (var entry : entries.entrySet()) {
                archive.putNextEntry(new ZipEntry(entry.getKey()));
                archive.write(entry.getValue());
                archive.closeEntry();
            }
        }
        System.out.println("Patched grapple travel, cost/cooldown, limb particles and kagune-control hooks: " + output);
    }
}

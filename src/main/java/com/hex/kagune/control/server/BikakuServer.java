package com.hex.kagune.control.server;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.bikaku.BikakuMove;
import com.hex.kagune.control.bikaku.BikakuRules;
import com.hex.kagune.control.bikaku.BikakuRules.Bite;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CBikakuState;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.4.9: server owner of the Bikaku path (the worm kagune with the mouth): the bite of the M1 string the worm is making, where it is
 * thrown and at whom, the string's chain, and what every client tracking him needs to draw it ({@link S2CBikakuState}).
 *
 * <p>His kagune is the original's manifested kagune (Alt + R brings it out, R puts it away, the RC drains as always), drawn as the worm
 * ({@code BikakuRenderer}) instead of the original tendrils, which never come out for him; the original's other kagune keys do nothing on
 * this path yet (its own moves come in the next versions). M1 is a string of three bites ({@link Bite}): each thrown out at what he looks
 * at, shutting on it at its landing tick: its damage by mastery (a player takes a share), bleeding, the third a stun; and Predatory
 * Feeding: a bite that lands gives back a little health, a kill with it more.
 *
 * <p>Runs after the original controller each tick (LOW priority), like the other paths.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class BikakuServer {
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final long NOTICE_TICKS = 60L;
    private static long nextErrorLogAt;

    /** Transient per-player state on the logical server. */
    static final class State {
        Bite bite = Bite.NONE;
        long startedAt = Long.MIN_VALUE;
        /** 3.6.3: the worm making the bite (its turn's, or the next one while the turn's is feeding). */
        int biteWorm;
        /**
         * 3.6.3, Grand Devouring: the body being fed on (0 none; kept until the worm is itself again), the worm feeding, when it began,
         * where the body was caught (its feet) and its size, whether it has been swallowed (killed) and he has been fed, and the next
         * wet sound's tick.
         */
        int devourVictim;
        int devourWorm = -1;
        long devourAt = Long.MIN_VALUE;
        Vec3 devourSpot;
        float devourWidth;
        float devourHeight;
        /** What it swallowed was worth (its full health): the bar it refills. */
        float devourWorth;
        boolean devourKilled;
        boolean devourFed;
        /** 3.6.3: when Grand Devouring can feed again (its cooldown). */
        long devourReadyAt = Long.MIN_VALUE;
        /** 3.6.3: the worm a form's move was made without (feeding when it began; -1 none). */
        int moveSkip = -1;
        Vec3 aim = Vec3.f_82478_;
        int targetId;
        boolean landed;
        /** The M1 string: the last bite, when it ends, a click waiting for the bite still out. */
        Bite lastBite;
        long chainEndsAt = Long.MIN_VALUE;
        long queuedAt = Long.MIN_VALUE;
        int queuedHint;
        long nextNoticeAt = Long.MIN_VALUE;
        boolean dirty = true;
        /** 3.5.0: the move it is making, when it began, its point (the world), the bodies it goes after, what it holds. */
        BikakuMove move = BikakuMove.NONE;
        long moveAt = Long.MIN_VALUE;
        Vec3 movePoint = Vec3.f_82478_;
        final int[] targets = new int[S2CBikakuState.TARGETS];
        int heldId;
        /** Whether its bite caught anything, and where (the body's frame) the catch was when it was bitten. */
        boolean caught;
        double[] heldAt;
        /** The beats of the move already done (each once). */
        int done;
        final java.util.EnumMap<BikakuMove, Long> readyAt = new java.util.EnumMap<>(BikakuMove.class);
        /** A move key pressed while the worm was busy: it waits this long for it. */
        /** 3.5.4: in the kakuja; when it last went in or out (long ago to start with); when it can be gone into again. */
        boolean kakuja;
        long kakujaAt = -1_000_000L;
        long kakujaReadyAt = Long.MIN_VALUE;
        /** 3.7.1: carried in The Walker (K tapped); when he last went in and came out (long ago to start with). */
        boolean walker;
        long walkerAt = -1_000_000L;
        long walkerLeftAt = -1_000_000L;
        /** 3.5.3: when Monstrous Regeneration last began (long ago to start with). */
        long regenAt = -1_000_000L;
        /** 3.5.7: when Undying Abomination last began (long ago to start with); until when a heavy blow slows its healing. */
        long undyingAt = -1_000_000L;
        long undyingSlowUntil = Long.MIN_VALUE;
        /** 3.5.7, Autonomous Predation: for each tentacle, when its last bite of its own began, at whom, whether it has shut, when it may bite again. */
        final long[] autoAt = filledLongs(Long.MIN_VALUE);
        final int[] autoTarget = new int[S2CBikakuState.AUTOS];
        final boolean[] autoShut = new boolean[S2CBikakuState.AUTOS];
        final long[] autoReadyAt = filledLongs(Long.MIN_VALUE);
        long nextAutoAt = Long.MIN_VALUE;
        /**
         * 3.5.8: how far the bite out was thrown and how many times slower it is for it ({@link BikakuRules#slower}); the move's likewise;
         * a key held to wind up a throw ({@link #CHARGE_ATTACK} M1, else its {@code BikakuMove.Slot} ordinal; -1 none), since when, at
         * whom; how far the waiting click and move key go.
         */
        double biteReach = BikakuRules.REACH;
        double biteSlower = 1.0;
        double moveReach;
        double moveSlower = 1.0;
        int charge = -1;
        long chargeAt = Long.MIN_VALUE;
        int chargeHint;
        double queuedReach = BikakuRules.REACH;
        double queuedSlotReach;
        /**
         * 3.6.1: a worm move made by several worms (a form of several): how many; each worm's part of it (its body is {@code targets[i]},
         * its way {@code moveRoutes[i]}): whom it holds, where (the body's frame) it caught it, whether it caught, its beats done, its mark
         * (the world), what Carrion Eruption came up under. Worm 0's part is copied into the single fields above for the clients.
         */
        int moveWorms = 1;
        final int[] held = new int[3];
        final double[][] heldAts = new double[3][];
        final boolean[] caughts = new boolean[3];
        final int[] dones = new int[3];
        final Vec3[] points = new Vec3[3];
        final int[] victims = new int[3];

        /** 3.6.1: no worm's part of a move left. */
        void calmWorms() {
            this.moveWorms = 1;
            java.util.Arrays.fill(this.held, 0);
            java.util.Arrays.fill(this.heldAts, null);
            java.util.Arrays.fill(this.caughts, false);
            java.util.Arrays.fill(this.dones, 0);
            java.util.Arrays.fill(this.points, null);
            java.util.Arrays.fill(this.victims, 0);
        }

        /** 3.6.0: the form (read from his profile on first use; null until then). */
        com.hex.kagune.control.bikaku.BikakuForm form;
        /** 3.5.9: the way round blocks the bite out takes, and the move's (one for each of Ravenous Pursuit's worms), in the world; null straight. */
        double[][] biteRoute;
        final double[][][] moveRoutes = new double[3][][];
        /** 3.5.7, Flesh Cataclysm: the bodies it has already struck (each once). */
        final java.util.Set<Integer> struck = new java.util.HashSet<>();

        private static long[] filledLongs(long value) {
            long[] out = new long[S2CBikakuState.AUTOS];
            java.util.Arrays.fill(out, value);
            return out;
        }

        /** 3.5.7: no bite of its own running or waiting. */
        void calmAutos() {
            java.util.Arrays.fill(this.autoAt, Long.MIN_VALUE);
            java.util.Arrays.fill(this.autoTarget, 0);
            java.util.Arrays.fill(this.autoShut, false);
            java.util.Arrays.fill(this.autoReadyAt, Long.MIN_VALUE);
        }
        /** 3.5.1: what Carrion Eruption came up under (server only: the clients are told where, not whom). */
        int eruptVictim;
        /** True for the moment of Predatory Drag's slam (its wound is a slam's). */
        boolean slamming;
        BikakuMove.Slot queuedSlot;
        long queuedSlotAt = Long.MIN_VALUE;
        int queuedSlotHint;

        void calm() {
            this.move = BikakuMove.NONE;
            this.moveAt = Long.MIN_VALUE;
            java.util.Arrays.fill(this.targets, 0);
            this.heldId = 0;
            this.caught = false;
            this.heldAt = null;
            this.done = 0;
            this.queuedSlot = null;
            this.bite = Bite.NONE;
            this.startedAt = Long.MIN_VALUE;
            this.targetId = 0;
            this.landed = false;
            this.lastBite = null;
            this.queuedAt = Long.MIN_VALUE;
            this.calmWorms(); // 3.6.1
            this.biteRoute = null; // 3.5.9
            java.util.Arrays.fill(this.moveRoutes, null);
            this.charge = -1; // 3.5.8
            this.chargeAt = Long.MIN_VALUE;
            this.undyingAt = -1_000_000L; // 3.5.7
            this.undyingSlowUntil = Long.MIN_VALUE;
            this.calmAutos();
            this.struck.clear();
            this.dirty = true;
        }
    }

    private BikakuServer() {
    }

    public static boolean isBikaku(ServerPlayer player) {
        return KaguneProfile.isUnlocked(player) && PathServer.path(player) == KagunePath.BIKAKU;
    }

    /** True while his kagune is out and he is alive: the only time the worm moves for him. */
    static boolean ready(ServerPlayer player) {
        return isBikaku(player) && KaguneProfile.isActive(player) && player.m_6084_();
    }

    static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.m_20148_(), ignored -> new State());
    }

    /** True this tick while one of his bites is shutting on something (its wounds are the teeth's punctures; 3.5.0: a move's bite too). */
    public static boolean biting(ServerPlayer player) {
        State state = STATES.get(player.m_20148_());
        if (state == null) {
            return false;
        }
        long now = player.m_9236_().m_46467_();
        return state.bite != Bite.NONE && now - state.startedAt == (long)Math.ceil(state.bite.land() * state.biteSlower) || BikakuMoves.biting(state, now);
    }

    /** 3.5.3: true while Monstrous Regeneration lasts. */
    static boolean regenerating(State state, long now) {
        return now >= state.regenAt && now - state.regenAt < BikakuMove.REGENERATION.ticks();
    }

    /** 3.5.3, Abnormal Regeneration: true while {@code player}'s worm is out (their ghoul's regeneration is far faster). */
    public static boolean abnormal(ServerPlayer player) {
        return ready(player);
    }

    /** 3.5.3, Pain Suppression: the share of a stun {@code body} still takes (a Bikaku player with the worm out shrugs half of it off). */
    public static double stunShare(net.minecraft.world.entity.LivingEntity body) {
        return body instanceof ServerPlayer player && ready(player) ? BikakuRules.STUN_SHARE : 1.0;
    }

    private static final UUID STEADY = UUID.fromString("5e2b8b0c-7d55-4f1e-9a0b-1c0de5a10b1c");

    /** 3.5.3, Pain Suppression: knockback resistance while the worm is out ({@code on}), taken off when it isn't. */
    static void steady(ServerPlayer player, boolean on) {
        steady(player, on, BikakuRules.KNOCKBACK_RESIST);
    }

    /** 3.5.4: as above, {@code amount} of it (the kakuja's Abnormal Flesh: all of it). */
    static void steady(ServerPlayer player, boolean on, double amount) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance resist = player.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_);
        if (resist == null) {
            return;
        }
        net.minecraft.world.entity.ai.attributes.AttributeModifier had = resist.m_22111_(STEADY);
        Double applied = STEADY_AMOUNT.get(player.m_20148_());
        if (had != null && (!on || applied == null || applied != amount)) {
            resist.m_22120_(STEADY);
            had = null;
        }
        if (on && had == null) {
            resist.m_22118_(new net.minecraft.world.entity.ai.attributes.AttributeModifier(STEADY, "Bikaku pain suppression", amount,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
            STEADY_AMOUNT.put(player.m_20148_(), amount);
        } else if (!on) {
            STEADY_AMOUNT.remove(player.m_20148_());
        }
    }

    /** The knockback resistance each player's modifier was made with. */
    private static final Map<UUID, Double> STEADY_AMOUNT = new HashMap<>();

    // ---------------------------------------------------------------- 3.5.4: the kakuja

    /** True while {@code player} is in the kakuja (growing into it, or grown). */
    public static boolean kakuja(ServerPlayer player) {
        State state = STATES.get(player.m_20148_());
        return state != null && state.kakuja;
    }

    /**
     * K (through {@code UkakuServer}): into the kakuja (its mastery, its RC, its cooldown) or out of it. 3.7.1 (the owner: "the walker is
     * the first tap K half kakuja, the full kakuja is the hold K"): tapped, into The Walker or out of it; held, into the kakuja (out of The
     * Walker into it); either, out of the kakuja.
     */
    public static void kakujaKey(ServerPlayer player, boolean hold) {
        if (!ready(player) || PaleStun.stunned(player) || PaleRevive.down(player)) {
            return;
        }
        State state = state(player);
        long now = player.m_9236_().m_46467_();
        long since = now - state.kakujaAt;
        if (since >= 0 && since < (state.kakuja ? com.hex.kagune.control.bikaku.MawKakuja.IN : com.hex.kagune.control.bikaku.MawKakuja.OUT)) {
            return; // mid-transformation
        }
        if (state.kakuja) {
            leaveKakuja(player, state, now);
            return;
        }
        if (!hold) {
            if (state.walker) {
                leaveWalker(player, state, now);
            } else {
                enterWalker(player, state, now);
            }
            return;
        }
        if (KaguneProfile.mastery(player) < BikakuRules.KAKUJA) {
            notice(player, state, Component.m_237110_("message.hexkagune.bikaku.locked", "The kakuja", BikakuRules.KAKUJA));
            return;
        }
        if (!QuestServer.pass(player, com.hex.kagune.control.quest.Gate.BIKAKU_KAKUJA)) {
            return; // 4.0.0: Noro teaches it
        }
        if (state.devourVictim != 0) {
            notice(player, state, Component.m_237115_("message.hexkagune.bikaku.feeding")); // 3.6.3: its worm is feeding
            return;
        }
        if (now < state.kakujaReadyAt) {
            notice(player, state, Component.m_237110_("message.hexkagune.bikaku.cooldown", "The kakuja", (state.kakujaReadyAt - now + 19) / 20));
            return;
        }
        if (!KaguneServerController.useEnergy(player, BikakuRules.KAKUJA_COST, true)) {
            return;
        }
        BikakuMoves.release(player, state, false);
        state.move = BikakuMove.NONE;
        state.moveAt = Long.MIN_VALUE;
        state.bite = Bite.NONE;
        state.startedAt = Long.MIN_VALUE;
        if (state.walker) {
            state.walker = false; // 3.7.1: out of The Walker into it (its walking tentacles go into it)
            state.walkerLeftAt = now;
        }
        state.kakuja = true;
        state.kakujaAt = now;
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "hexkagune:sfx.transform.heavy_morph", 1.0F, 0.85F);
        KaguneNetwork.sendTo(player, player);
        broadcast(player, state);
    }

    /** 3.7.1: K tapped: into The Walker (its mastery, half the kakuja's energy; not while a worm feeds). */
    private static void enterWalker(ServerPlayer player, State state, long now) {
        com.hex.kagune.control.bikaku.BikakuForm walker = com.hex.kagune.control.bikaku.BikakuForm.WALKER;
        if (walker.unlocked(KaguneProfile.mastery(player)) && !QuestServer.pass(player, com.hex.kagune.control.quest.Gate.BIKAKU_WALKER)) {
            return; // 4.0.0: Noro teaches it (the half kakuja)
        }
        if (!walker.unlocked(KaguneProfile.mastery(player))) {
            notice(player, state, Component.m_237110_("message.hexkagune.bikaku.locked", walker.displayName(), walker.mastery()));
            return;
        }
        if (state.devourVictim != 0) {
            notice(player, state, Component.m_237115_("message.hexkagune.bikaku.feeding")); // as a form change: not while a worm feeds
            return;
        }
        if (!KaguneServerController.useEnergy(player, BikakuRules.WALKER_COST, true)) {
            return;
        }
        BikakuMoves.release(player, state, false);
        state.move = BikakuMove.NONE;
        state.moveAt = Long.MIN_VALUE;
        state.bite = Bite.NONE;
        state.startedAt = Long.MIN_VALUE;
        state.walker = true;
        state.walkerAt = now;
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "hexkagune:sfx.transform.heavy_morph", 0.8F, 1.15F);
        broadcast(player, state);
    }

    /** 3.7.1: out of The Walker (K tapped again, its energy run dry): let down from its height, no fall damage for it. */
    static void leaveWalker(ServerPlayer player, State state, long now) {
        if (!state.walker) {
            return;
        }
        BikakuMoves.release(player, state, false);
        state.move = BikakuMove.NONE;
        state.moveAt = Long.MIN_VALUE;
        state.bite = Bite.NONE;
        state.startedAt = Long.MIN_VALUE;
        state.walker = false;
        state.walkerLeftAt = now;
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 0.9F, KaguneServerController.vary(player, 0.7F, 0.05F));
        broadcast(player, state);
    }

    /** 3.7.1: true while he is carried in The Walker or just let down from it (no fall damage; not thrown out for floating). */
    static boolean carried(State state, long now) {
        return state.walker || now - state.walkerLeftAt < BikakuRules.WALKER_FALL_GRACE;
    }

    private static boolean floatingFailed;

    /**
     * 3.7.1: carried in The Walker he is off the ground for as long as it lasts: a server without flight allowed would throw him out for
     * floating after 4 seconds. Its count is set back each tick (through Forge's {@code ObfuscationReflectionHelper}; if that ever fails,
     * logged once, and only such a server's own rule applies).
     */
    private static void notFloating(ServerPlayer player) {
        if (floatingFailed || player.f_8906_ == null) {
            return;
        }
        try {
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(net.minecraft.server.network.ServerGamePacketListenerImpl.class,
                player.f_8906_, 0, "f_9737_");
        } catch (RuntimeException | LinkageError failure) {
            floatingFailed = true;
            com.hex.kagune.control.Safety.caught(failure, "keeping a player carried by The Walker from being thrown out for floating");
        }
    }

    static void leaveKakuja(ServerPlayer player, State state, long now) {
        if (!state.kakuja) {
            return;
        }
        state.kakuja = false;
        state.kakujaAt = now;
        state.bite = Bite.NONE;
        state.startedAt = Long.MIN_VALUE;
        state.kakujaReadyAt = now + Cooldowns.of(player, (long)BikakuRules.KAKUJA_COOLDOWN);
        endKakujaMoves(player, state);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        broadcast(player, state);
    }

    /** Each tick in the kakuja: its RC drain (out when it runs dry), rooted (Immovable Horror), regenerating (Monstrous Regeneration). */
    private static void kakujaTick(ServerPlayer player, State state, long now) {
        if (!KaguneProfile.consumeEnergy(player, BikakuRules.KAKUJA_DRAIN) || KaguneProfile.mastery(player) < BikakuRules.KAKUJA) {
            KaguneProfile.setEnergy(player, 0.0F);
            leaveKakuja(player, state, now);
            player.m_5661_(Component.m_237115_("message.hexkagune.bikaku.kakuja_spent"), true);
            return;
        }
        if (now % 10L == 0L) {
            KaguneServerController.refreshFormEffect(player, net.minecraft.world.effect.MobEffects.f_19597_, BikakuRules.KAKUJA_SLOW);
            KaguneNetwork.sendTo(player, player);
        }
        if ((now - state.kakujaAt) % BikakuRules.KAKUJA_REGEN_EVERY == 0L && player.m_21223_() < player.m_21233_()) {
            GhoulRegen.healOwn(player, 1.0F);
        }
        // 3.5.7, Undying Abomination: far more on top (slowed a while by a heavy blow, never stopped).
        if (undying(state, now)) {
            int every = now < state.undyingSlowUntil ? BikakuRules.UNDYING_SLOW_EVERY : BikakuRules.UNDYING_EVERY;
            if ((now - state.undyingAt) % every == 0L && player.m_21223_() < player.m_21233_()) {
                GhoulRegen.healOwn(player, BikakuRules.UNDYING_HEAL);
            }
        }
        // 3.5.7, Autonomous Predation (grown into it).
        if (now - state.kakujaAt >= com.hex.kagune.control.bikaku.MawKakuja.IN) {
            BikakuMoves.autoTick(player, state, now);
        }
    }

    /** 3.5.7: true while Undying Abomination lasts. */
    static boolean undying(State state, long now) {
        return state.kakuja && now >= state.undyingAt && now - state.undyingAt < BikakuMove.UNDYING.ticks();
    }

    /** 3.5.7: out of the kakuja, its own moves end: what Endless Hunger holds is let go, Undying Abomination and its own bites stop. */
    static void endKakujaMoves(ServerPlayer player, State state) {
        if (state.move.kakuja()) {
            BikakuMoves.release(player, state, false);
            state.move = BikakuMove.NONE;
            state.moveAt = Long.MIN_VALUE;
            java.util.Arrays.fill(state.targets, 0);
            state.caught = false;
            state.heldAt = null;
            state.done = 0;
        }
        state.undyingAt = -1_000_000L;
        state.undyingSlowUntil = Long.MIN_VALUE;
        state.calmAutos();
        state.struck.clear();
    }

    /** 3.5.4, Abnormal Flesh: in the kakuja he takes a share of every blow. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || STATES.isEmpty() || event.getAmount() <= 0.0F || event.getAmount() > 1000.0F) {
            return;
        }
        State state = STATES.get(player.m_20148_());
        if (state != null && state.kakuja && isBikaku(player)) {
            float blow = event.getAmount();
            float taken = BikakuRules.KAKUJA_TAKEN;
            long now = player.m_9236_().m_46467_();
            if (undying(state, now)) {
                // 3.5.7, Undying Abomination: it takes still less, and a heavy blow slows its mending a while.
                taken *= BikakuRules.UNDYING_TAKEN;
                if (blow >= BikakuRules.UNDYING_HEAVY) {
                    state.undyingSlowUntil = now + BikakuRules.UNDYING_SLOWED;
                }
            }
            event.setAmount(blow * taken);
        }
    }

    /** 3.5.0: true for the moment of Predatory Drag's slam. */
    public static boolean slamming(ServerPlayer player) {
        State state = STATES.get(player.m_20148_());
        return state != null && state.slamming;
    }

    /** 3.5.0: {@code rig} (his body's frame: blocks from his feet, x to his right, y up, z ahead) in the world. */
    static Vec3 world(ServerPlayer player, double[] rig) {
        double yaw = Math.toRadians(player.f_20883_);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        return player.m_20182_().m_82520_(rx * rig[0] + fx * rig[2], rig[1], rz * rig[0] + fz * rig[2]);
    }

    /** 3.5.0: a point of the world in his body's frame. */
    static double[] rig(ServerPlayer player, Vec3 world) {
        double yaw = Math.toRadians(player.f_20883_);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        Vec3 d = world.m_82546_(player.m_20182_());
        return new double[] {d.f_82479_ * rx + d.f_82481_ * rz, d.f_82480_, d.f_82479_ * fx + d.f_82481_ * fz};
    }

    static S2CBikakuState snapshot(ServerPlayer player, State state) {
        return new S2CBikakuState(player.m_20148_(), state.bite.ordinal(), state.startedAt, state.aim.f_82479_, state.aim.f_82480_, state.aim.f_82481_,
            state.targetId, state.move.ordinal(), state.moveAt, state.movePoint.f_82479_, state.movePoint.f_82480_, state.movePoint.f_82481_,
            state.targets.clone(), state.heldId, state.caught, state.regenAt, state.kakuja, state.kakujaAt, state.undyingAt, state.autoAt.clone(),
            state.autoTarget.clone(), (float)state.biteReach, (float)state.moveReach, state.charge, state.chargeAt,
            new double[][][] {state.biteRoute, state.moveRoutes[0], state.moveRoutes[1], state.moveRoutes[2]}, form(player, state).ordinal(),
            state.moveWorms, state.held.clone(), caughtBits(state), pointsOf(state), BikakuDevour.mutations(player), state.devourVictim,
            state.devourWorm, state.devourAt, state.devourSpot == null ? null : new double[] {state.devourSpot.f_82479_, state.devourSpot.f_82480_,
                state.devourSpot.f_82481_}, state.devourWidth, state.devourHeight, state.devourReadyAt);
    }

    /** 3.6.1: which worms caught (a bit each). */
    private static int caughtBits(State state) {
        int bits = 0;
        for (int i = 0; i < state.caughts.length; i++) {
            bits |= state.caughts[i] ? 1 << i : 0;
        }
        return bits;
    }

    /** 3.6.1: each worm's mark (the world; null none). */
    private static double[][] pointsOf(State state) {
        double[][] out = new double[state.points.length][];
        for (int i = 0; i < out.length; i++) {
            Vec3 p = state.points[i];
            out[i] = p == null ? null : new double[] {p.f_82479_, p.f_82480_, p.f_82481_};
        }
        return out;
    }

    static void broadcast(ServerPlayer player, State state) {
        state.dirty = false;
        ControlNetwork.broadcast(player, snapshot(player, state));
    }

    /** "Not yet" on the action bar, at most every few seconds: the original's kagune keys have no move on this path yet. */
    static void notYet(ServerPlayer player) {
        State state = state(player);
        long now = player.m_9236_().m_46467_();
        if (now >= state.nextNoticeAt || now < state.nextNoticeAt - NOTICE_TICKS) {
            state.nextNoticeAt = now + NOTICE_TICKS;
            player.m_5661_(Component.m_237115_("message.hexkagune.bikaku.not_yet"), true);
        }
    }

    /** Called through {@code UkakuServer.onPathChanged}: whatever was in flight stops and every client learns his worm anew. */
    static void onPathChanged(ServerPlayer player) {
        steady(player, false); // 3.5.3: off the path, nothing of Pain Suppression stays
        State state = STATES.remove(player.m_20148_());
        if (state != null) {
            BikakuMoves.release(player, state, false);
            state.calm();
            ControlNetwork.broadcast(player, snapshot(player, state));
        }
        if (isBikaku(player)) {
            broadcast(player, state(player));
        }
    }

    // ---------------------------------------------------------------- 3.5.0: the move keys

    /** A Bikaku input from {@code C2SBikakuAction}. */
    public static void onAction(ServerPlayer player, int action, int arg, int targetId) {
        if (action == com.hex.kagune.control.network.C2SBikakuAction.FORM && isBikaku(player)) {
            pickForm(player, state(player), arg); // 3.6.0: from the menu, out or not
            return;
        }
        if (!isBikaku(player) || PaleWitness.frozen(player) || PaleRevive.down(player) || PaleStun.stunned(player) || !ready(player)) {
            return;
        }
        State state = state(player);
        long now = player.m_9236_().m_46467_();
        Entity hint = targetId > 0 ? player.m_284548_().m_6815_(targetId) : null;
        // 3.5.8: M1 and the worm's thrown moves are held: the press winds it up, letting go throws it as far as it was held for.
        if (action == com.hex.kagune.control.network.C2SBikakuAction.ATTACK) {
            if (state.kakuja) {
                strike(player, hint);
            } else {
                windUp(player, state, CHARGE_ATTACK, targetId, now);
            }
            return;
        }
        if (action == com.hex.kagune.control.network.C2SBikakuAction.ATTACK_UP || action == com.hex.kagune.control.network.C2SBikakuAction.KEY_UP) {
            int which = action == com.hex.kagune.control.network.C2SBikakuAction.ATTACK_UP ? CHARGE_ATTACK : arg;
            if (state.charge == which) {
                letGo(player, state, targetId, now);
            }
            return;
        }
        if (action != com.hex.kagune.control.network.C2SBikakuAction.KEY) {
            return;
        }
        BikakuMove.Slot[] slots = BikakuMove.Slot.values();
        if (arg <= 0 || arg >= slots.length) {
            return;
        }
        KakujaPassive.attacked(player);
        BikakuMove pressed = BikakuMove.of(slots[arg], state.kakuja);
        if (!state.kakuja && pressed.held()) {
            windUp(player, state, arg, targetId, now);
            return;
        }
        if (state.kakuja && now - state.kakujaAt < com.hex.kagune.control.bikaku.MawKakuja.IN) {
            return; // 3.5.7: still growing into it
        }
        if ((out(state, now) || BikakuMoves.busy(state, now)) && BikakuMove.of(slots[arg], state.kakuja).occupies()) {
            state.queuedSlot = slots[arg];
            state.queuedSlotAt = now;
            state.queuedSlotHint = targetId;
            state.queuedSlotReach = pressed.baseReach();
            return;
        }
        BikakuMoves.key(player, state, slots[arg], hint, now, pressed.baseReach());
    }

    // ---------------------------------------------------------------- 3.6.0: the forms

    /** Where his form is kept in his profile (death carries it; a reset clears it). */
    static final String FORM_KEY = "BikakuForm";

    /**
     * 4.0.0: true if {@code move} is his to make: the Maw and the Drag always (the worm's own); Ravenous Pursuit, Carrion Eruption and Monstrous
     * Regeneration once Noro has taught the Worms; the kakuja's own once he has taught the kakuja. False (after saying who teaches it) if not.
     */
    static boolean taught(ServerPlayer player, com.hex.kagune.control.bikaku.BikakuMove move) {
        com.hex.kagune.control.quest.Gate gate = switch (move) {
            case PURSUIT, ERUPTION, REGENERATION -> com.hex.kagune.control.quest.Gate.BIKAKU_WORMS;
            case JAWS, HUNGER, CATACLYSM, UNDYING -> com.hex.kagune.control.quest.Gate.BIKAKU_KAKUJA;
            default -> null;
        };
        return gate == null || QuestServer.pass(player, gate);
    }

    /** His form: as he picked it (kept in his profile), or the strongest his mastery has opened below it. */
    static com.hex.kagune.control.bikaku.BikakuForm form(ServerPlayer player, State state) {
        if (state.form == null) {
            net.minecraft.nbt.CompoundTag root = KaguneProfile.root(player);
            state.form = com.hex.kagune.control.bikaku.BikakuForm.byOrdinal(root.m_128441_(FORM_KEY) ? root.m_128451_(FORM_KEY) : 0);
        }
        com.hex.kagune.control.bikaku.BikakuForm allowed = com.hex.kagune.control.bikaku.BikakuForm.allowed(state.form, KaguneProfile.mastery(player));
        if (allowed.worms() > 1 && !QuestServer.open(player, com.hex.kagune.control.quest.Gate.BIKAKU_WORMS)) {
            allowed = com.hex.kagune.control.bikaku.BikakuForm.ONE; // 4.0.0: more worms than one are Noro's to teach
        }
        state.form = allowed;
        // 3.7.1: The Walker is not picked: K tapped carries him in it (its moves are Three Worms').
        return state.walker ? com.hex.kagune.control.bikaku.BikakuForm.WALKER : allowed;
    }

    /** How many worms his form has (one in the kakuja: the worm it grows from). */
    static int worms(ServerPlayer player, State state) {
        return state.kakuja ? 1 : form(player, state).worms();
    }

    /** A form picked in the menu: taken if his mastery has opened it, kept in his profile, every client told. */
    private static void pickForm(ServerPlayer player, State state, int ordinal) {
        com.hex.kagune.control.bikaku.BikakuForm wanted = com.hex.kagune.control.bikaku.BikakuForm.byOrdinal(ordinal);
        if (state.devourVictim != 0) {
            notice(player, state, Component.m_237115_("message.hexkagune.bikaku.feeding")); // 3.6.3: not while a worm feeds
            return;
        }
        if (!wanted.picked()) {
            notice(player, state, Component.m_237115_("message.hexkagune.bikaku.walker_key")); // 3.7.1: The Walker is K tapped
            return;
        }
        if (!wanted.unlocked(KaguneProfile.mastery(player))) {
            notice(player, state, Component.m_237110_("message.hexkagune.bikaku.locked", wanted.displayName(), wanted.mastery()));
            return;
        }
        if (wanted.worms() > 1 && !QuestServer.pass(player, com.hex.kagune.control.quest.Gate.BIKAKU_WORMS)) {
            return; // 4.0.0: Noro teaches the forms
        }
        state.form = wanted;
        KaguneProfile.root(player).m_128405_(FORM_KEY, wanted.ordinal());
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 0.9F, KaguneServerController.vary(player, 0.8F, 0.05F));
        broadcast(player, state);
    }

    /** 3.6.0: the shoulder (the world) worm {@code worm} of his form throws from. */
    static Vec3 shoulder(ServerPlayer player, State state, int worm) {
        return world(player, com.hex.kagune.control.bikaku.MawMotion.wormShoulder(worms(player, state), worm));
    }

    /** "{@code key}" on the action bar with its arguments, at most every few seconds. */
    static void notice(ServerPlayer player, State state, Component message) {
        long now = player.m_9236_().m_46467_();
        if (now >= state.nextNoticeAt || now < state.nextNoticeAt - NOTICE_TICKS) {
            state.nextNoticeAt = now + NOTICE_TICKS;
            player.m_5661_(message, true);
        }
    }

    /**
     * Every running cooldown of his, over now ({@link Cooldowns}): his moves', the kakuja's and Grand Devouring's. 3.6.4 (the owner, on
     * {@code /kagune cooldowns off} not ending them: "it should."): called by {@link Cooldowns#clear} with every other path's, and his
     * clients told (the menu's wait).
     */
    static void clearCooldowns(ServerPlayer player) {
        State state = STATES.get(player.m_20148_());
        if (state != null) {
            state.readyAt.clear();
            state.kakujaReadyAt = Long.MIN_VALUE;
            state.devourReadyAt = Long.MIN_VALUE;
            if (isBikaku(player)) {
                broadcast(player, state);
            }
        }
    }

    // ---------------------------------------------------------------- M1

    /** Trampoline target of the original M1 (through {@code KaguneCombat.startSwipe}): the next bite of the string. */
    public static void strike(ServerPlayer player, Entity hint) {
        strike(player, hint, BikakuRules.REACH);
    }

    /** 3.5.8: the next bite, thrown as far as {@code reach} (a click waiting for the worm to be back waits with it). */
    static void strike(ServerPlayer player, Entity hint, double reach) {
        if (!ready(player) || PaleWitness.frozen(player) || PaleRevive.down(player) || PaleStun.stunned(player)) {
            return;
        }
        State state = state(player);
        long now = player.m_9236_().m_46467_();
        if (out(state, now) || BikakuMoves.busy(state, now)) {
            state.queuedAt = now;
            state.queuedHint = hint == null ? 0 : hint.m_19879_();
            state.queuedReach = reach;
            return;
        }
        begin(player, state, hint, now, reach);
    }

    // ---------------------------------------------------------------- 3.5.8: held to throw it further

    /** A wind-up of M1 (as opposed to a move key's slot ordinal). */
    static final int CHARGE_ATTACK = 9;

    /** M1 or a thrown move's key goes down: the worm winds up (every client sees it), until it is let go. */
    private static void windUp(ServerPlayer player, State state, int which, int hint, long now) {
        state.charge = which;
        state.chargeAt = now;
        state.chargeHint = hint;
        broadcast(player, state);
    }

    /** Let go: thrown as far as it was held for ({@link BikakuRules#heldReach}); if the worm is still out, when it is back. */
    private static void letGo(ServerPlayer player, State state, int hintId, long now) {
        int which = state.charge;
        long held = Math.max(0L, now - state.chargeAt);
        state.charge = -1;
        state.chargeAt = Long.MIN_VALUE;
        int aimed = hintId > 0 ? hintId : state.chargeHint;
        Entity hint = aimed > 0 ? player.m_284548_().m_6815_(aimed) : null;
        broadcast(player, state);
        if (which == CHARGE_ATTACK) {
            strike(player, hint, BikakuRules.heldReach(BikakuRules.REACH, held));
            return;
        }
        BikakuMove.Slot[] slots = BikakuMove.Slot.values();
        if (which <= 0 || which >= slots.length) {
            return;
        }
        BikakuMove move = BikakuMove.of(slots[which], state.kakuja);
        double reach = move.held() ? BikakuRules.heldReach(move.baseReach(), held) : move.baseReach();
        if ((out(state, now) || BikakuMoves.busy(state, now)) && move.occupies()) {
            state.queuedSlot = slots[which];
            state.queuedSlotAt = now;
            state.queuedSlotHint = aimed;
            state.queuedSlotReach = reach;
            return;
        }
        BikakuMoves.key(player, state, slots[which], hint, now, reach);
    }

    /** True while a bite is still out (until it is back at rest). */
    static boolean out(State state, long now) {
        return state.bite != Bite.NONE && now - state.startedAt < state.bite.ticks() * state.biteSlower && now >= state.startedAt;
    }

    private static void begin(ServerPlayer player, State state, Entity hint, long now) {
        begin(player, state, hint, now, BikakuRules.REACH);
    }

    /** 3.5.8: a bite thrown as far as {@code reach} (the kakuja's tentacle as far as it always does). */
    private static void begin(ServerPlayer player, State state, Entity hint, long now, double reach) {
        boolean big = state.kakuja;
        state.biteReach = big ? BikakuRules.KAKUJA_REACH : Math.max(BikakuRules.REACH, reach);
        state.biteSlower = big ? 1.0 : BikakuRules.slower(state.biteReach, BikakuRules.REACH);
        if (big && now - state.kakujaAt < com.hex.kagune.control.bikaku.MawKakuja.IN) {
            return; // 3.5.4: still growing into it
        }
        boolean chained = state.lastBite != null && now <= state.chainEndsAt + BikakuRules.CHAIN_WINDOW;
        Bite bite = big ? Bite.TENTACLE : chained && state.lastBite != Bite.TENTACLE ? state.lastBite.next() : Bite.SNAP;
        // 3.6.3: the turn's worm, or the next while it feeds (none left: nothing to bite with).
        int worm = big ? 0 : com.hex.kagune.control.bikaku.BikakuForm.biter(bite, worms(player, state), BikakuDevour.busyWorm(state));
        if (worm < 0) {
            notice(player, state, Component.m_237115_("message.hexkagune.bikaku.feeding"));
            return;
        }
        if (!KaguneServerController.useEnergy(player, bite.cost(), true)) {
            return;
        }
        state.biteWorm = worm;
        // 3.5.9: the worm's bite goes round blocks to what it goes after (the kakuja's tentacles as they always did).
        Vec3 throwFrom = big ? kakujaCentre(player) : shoulder(player, state, worm);
        Aim routed = big ? new Aim(aimFrom(player, hint, kakujaCentre(player), BikakuRules.KAKUJA_REACH), null) : aimRouted(player, hint, throwFrom,
            state.biteReach);
        LivingEntity target = routed.body();
        state.biteRoute = big || !BikakuRoutes.bends(routed.route()) ? null : routed.route();
        state.bite = bite;
        state.startedAt = now;
        state.targetId = target == null ? 0 : target.m_19879_();
        Vec3 air = player.m_146892_().m_82549_(player.m_20154_().m_82490_((big ? BikakuRules.KAKUJA_REACH : state.biteReach) * (big ? 0.8 : 0.85)));
        state.aim = target != null ? KaguneServerController.center(target) : big ? air : BikakuRoutes.clipped(player, throwFrom, air);
        state.landed = false;
        state.lastBite = bite;
        state.chainEndsAt = now + bite.ticks();
        state.queuedAt = Long.MIN_VALUE;
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, bite.heavy() ? 0.9F : 0.7F,
            KaguneServerController.vary(player, bite.heavy() ? 0.7F : 0.85F, 0.06F));
        // 1.0.40 (as every path's M1): another's M1 landing on him before this bite shuts meets it, and both stop.
        long began = state.startedAt;
        double hit = KaguneClash.strength(player, bite.power(), true);
        KaguneClash.swing(player, now, (int)Math.ceil(bite.land() * state.biteSlower) + 1, state.biteReach, hit, () -> state.bite == bite && state.startedAt == began,
            () -> clashed(player, state));
        broadcast(player, state);
    }

    /** His bite met another's blow coming back (KaguneClash): drawn back, nothing queued after it. */
    private static void clashed(ServerPlayer player, State state) {
        state.queuedAt = Long.MIN_VALUE;
        state.bite = Bite.NONE;
        state.startedAt = Long.MIN_VALUE;
        state.targetId = 0;
        broadcast(player, state);
    }

    /** Over his right shoulder, where the worm passes him when it is thrown out (the world). */
    static Vec3 shoulder(ServerPlayer player) {
        return world(player, com.hex.kagune.control.bikaku.MawMotion.shoulder());
    }

    /** A bite at nothing: ahead along his look, as far as the worm reaches from over his shoulder. */
    static Vec3 ahead(ServerPlayer player) {
        return player.m_146892_().m_82549_(player.m_20154_().m_82490_(BikakuRules.REACH * 0.85));
    }

    /** What a bite goes after: the hinted body if fair game, in reach and in sight, else the nearest in front of him. */
    static LivingEntity aim(ServerPlayer player, Entity hint) {
        return aimFrom(player, hint, shoulder(player), BikakuRules.REACH);
    }

    /** 3.5.4: the kakuja's middle (the world): its tentacles' reach is measured from here. */
    static Vec3 kakujaCentre(ServerPlayer player) {
        return world(player, BikakuRules.KAKUJA_CENTRE);
    }

    /** 3.5.9: a body to go after and the way round blocks to it (null: a straight line; both null: nothing). */
    record Aim(LivingEntity body, double[][] route) {
    }

    /**
     * 3.5.9: what a bite or move from {@code from} reaching {@code reach} goes after, and its way there: the hinted body if there is a way
     * round blocks to it in reach, else (as before) the hinted or nearest one in front in a clear line.
     */
    static Aim aimRouted(ServerPlayer player, Entity hint, Vec3 from, double reach) {
        if (hint instanceof LivingEntity living && hint != player && living.m_6084_() && KaguneServerController.validSwipeTarget(player, living)
            && KaguneServerController.center(living).m_82554_(from) <= reach + living.m_20205_()) {
            double[][] route = BikakuRoutes.route(player, from, KaguneServerController.center(living), reach + living.m_20205_());
            if (route != null) {
                return new Aim(living, route);
            }
        }
        LivingEntity body = aimFrom(player, hint, from, reach);
        return new Aim(body, body == null ? null : BikakuRoutes.route(player, from, KaguneServerController.center(body), reach + body.m_20205_()));
    }

    /** What a bite from {@code from} reaching {@code reach} goes after: the hinted body, else the nearest in front. */
    static LivingEntity aimFrom(ServerPlayer player, Entity hint, Vec3 from, double reach) {
        if (hint instanceof LivingEntity living && hint != player && KaguneServerController.validSwipeTarget(player, living)
            && KaguneServerController.center(living).m_82554_(from) <= reach + living.m_20205_() && KaguneServerController.clearLineTo(player, living)) {
            return living;
        }
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        AABB area = player.m_20191_().m_82400_(reach + 1.0);
        for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, area,
            living -> living != player && KaguneServerController.validSwipeTarget(player, living))) {
            Vec3 centre = KaguneServerController.center(body);
            if (centre.m_82554_(from) > reach + body.m_20205_() * 0.5) {
                continue;
            }
            Vec3 to = centre.m_82546_(eye);
            double distance = to.m_82553_();
            if (distance < 1.0E-3) {
                continue;
            }
            double facing = to.m_82490_(1.0 / distance).m_82526_(look);
            if (facing < 0.8 || !KaguneServerController.clearLineTo(player, body)) {
                continue;
            }
            double score = distance * (2.0 - facing);
            if (score < bestScore) {
                bestScore = score;
                best = body;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- the tick

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        try {
            tick(player);
        } catch (RuntimeException failure) {
            long now = System.currentTimeMillis();
            if (now >= nextErrorLogAt) {
                nextErrorLogAt = now + 30000L;
                KaguneMod.LOGGER.error("HexKagune Bikaku tick failed", failure);
            }
        }
    }

    private static void tick(ServerPlayer player) {
        if (!isBikaku(player)) {
            return;
        }
        State state = state(player);
        long now = player.m_9236_().m_46467_();
        BikakuDevour.tick(player, state, now); // 3.6.3: Grand Devouring (and its end, if the kagune went away)
        if (now % 20L == 0L) {
            // His profile keeps the neutral seed: whatever Rinkaku form or mutation found its way in is parked within a second.
            RinkakuStash.stash(player);
        }
        // 3.5.4: the kakuja: it ends with the kagune or his life; it drains RC, roots him, regenerates him.
        if (state.kakuja) {
            if (!KaguneProfile.isActive(player) || !player.m_6084_()) {
                state.kakuja = false;
                state.kakujaAt = now;
                endKakujaMoves(player, state);
                state.dirty = true;
            } else {
                kakujaTick(player, state, now);
            }
        }
        // 3.7.1: The Walker (K tapped): it ends with the kagune or his life; it drains half the kakuja's energy (out when it runs dry).
        if (state.walker) {
            if (!KaguneProfile.isActive(player) || !player.m_6084_() || state.kakuja) {
                state.walker = false;
                state.walkerLeftAt = now;
                state.dirty = true;
            } else if (!KaguneProfile.consumeEnergy(player, BikakuRules.WALKER_DRAIN)
                || KaguneProfile.mastery(player) < com.hex.kagune.control.bikaku.BikakuForm.WALKER.mastery()) {
                leaveWalker(player, state, now);
                player.m_5661_(Component.m_237115_("message.hexkagune.bikaku.walker_spent"), true);
            }
        }
        // 3.6.2: The Walker: his walking tentacles take a fall for him (no fall damage while they are out); his client climbs walls on them.
        // 3.7.1: they carry him well off the ground: nor for a moment after, as he is let down; and he is never thrown out for floating.
        if (carried(state, now) && player.m_6084_()) {
            player.f_19789_ = 0.0F;
            notFloating(player);
        }
        // 3.5.3: Pain Suppression: hard to throw while the worm is out (and Monstrous Regeneration's healing while it lasts).
        if (now % 10L == 0L) {
            steady(player, KaguneProfile.isActive(player) && player.m_6084_(), state.kakuja ? BikakuRules.KAKUJA_STEADY : BikakuRules.KNOCKBACK_RESIST);
        }
        if (regenerating(state, now) && player.m_6084_() && (now - state.regenAt) % BikakuRules.REGEN_EVERY == 0L
            && player.m_21223_() < player.m_21233_()) {
            GhoulRegen.healOwn(player, BikakuRules.REGEN_HEAL);
        }
        if (!KaguneProfile.isActive(player) || !player.m_6084_()) {
            if (state.bite != Bite.NONE || state.move != BikakuMove.NONE) {
                BikakuMoves.release(player, state, false);
                state.calm();
                broadcast(player, state);
            }
            return;
        }
        // 3.5.8: a wind-up held too long (its letting go lost, or forgotten) goes by itself; while held, how far it will go, on the action bar.
        if (state.charge >= 0 && now - state.chargeAt > BikakuRules.CHARGE_GIVE_UP) {
            letGo(player, state, 0, now);
        } else if (state.charge >= 0 && (now - state.chargeAt) % 3L == 0L) {
            double base = state.charge == CHARGE_ATTACK ? BikakuRules.REACH : state.charge > 0 && state.charge < BikakuMove.Slot.values().length
                ? BikakuMove.of(BikakuMove.Slot.values()[state.charge], false).baseReach() : 0.0;
            if (base > 0.0) {
                player.m_5661_(Component.m_237110_("message.hexkagune.bikaku.reach", Math.round(BikakuRules.heldReach(base, now - state.chargeAt))), true);
            }
        }
        // 3.5.0: the move it is making; when it ends, a key or a click waiting for it.
        if (state.move != BikakuMove.NONE) {
            BikakuMoves.tick(player, state, now);
            if (state.move == BikakuMove.NONE) {
                if (state.queuedSlot != null && now - state.queuedSlotAt <= BikakuRules.BUFFER + 40) {
                    BikakuMove.Slot slot = state.queuedSlot;
                    state.queuedSlot = null;
                    Entity hint = state.queuedSlotHint > 0 ? player.m_284548_().m_6815_(state.queuedSlotHint) : null;
                    BikakuMoves.key(player, state, slot, hint, now, state.queuedSlotReach);
                } else if (state.queuedAt != Long.MIN_VALUE && now - state.queuedAt <= BikakuRules.BUFFER + 40) {
                    Entity hint = state.queuedHint > 0 ? player.m_284548_().m_6815_(state.queuedHint) : null;
                    state.queuedAt = Long.MIN_VALUE;
                    begin(player, state, hint, now, state.queuedReach);
                }
                state.queuedSlot = null;
                state.queuedAt = Long.MIN_VALUE;
            }
        }
        if (state.bite != Bite.NONE) {
            long age = now - state.startedAt;
            if (!state.landed && age >= state.bite.land() * state.biteSlower) {
                state.landed = true;
                land(player, state, now);
            }
            if (age >= state.bite.ticks() * state.biteSlower || age < 0) {
                state.bite = Bite.NONE;
                state.startedAt = Long.MIN_VALUE;
                state.targetId = 0;
                state.dirty = true;
                if (state.queuedSlot != null && now - state.queuedSlotAt <= BikakuRules.BUFFER + state.lastBite.ticks() * state.biteSlower) {
                    // 3.5.0: a move key pressed during the bite goes first.
                    BikakuMove.Slot slot = state.queuedSlot;
                    state.queuedSlot = null;
                    Entity hint = state.queuedSlotHint > 0 ? player.m_284548_().m_6815_(state.queuedSlotHint) : null;
                    BikakuMoves.key(player, state, slot, hint, now, state.queuedSlotReach);
                } else if (state.queuedAt != Long.MIN_VALUE && now - state.queuedAt <= BikakuRules.BUFFER + state.lastBite.ticks() * state.biteSlower) {
                    Entity hint = state.queuedHint > 0 ? player.m_284548_().m_6815_(state.queuedHint) : null;
                    state.queuedAt = Long.MIN_VALUE;
                    begin(player, state, hint, now, state.queuedReach);
                }
                state.queuedAt = Long.MIN_VALUE;
                state.queuedSlot = null;
            }
        }
        if (state.dirty) {
            broadcast(player, state);
        }
    }

    /** The mouth shuts: on its target if it is still in reach and in sight, else on whatever is at the mouth. */
    private static void land(ServerPlayer player, State state, long now) {
        boolean big = state.bite == Bite.TENTACLE;
        Vec3 from = big ? kakujaCentre(player) : shoulder(player, state, state.biteWorm);
        double reach = big ? BikakuRules.KAKUJA_REACH : state.biteReach;
        LivingEntity body = null;
        if (state.biteRoute != null && !big) {
            // 3.5.9: round blocks: what it went after, if it is still where its way ends.
            if (state.targetId != 0 && player.m_284548_().m_6815_(state.targetId) instanceof LivingEntity target && target.m_6084_()
                && KaguneServerController.validSwipeTarget(player, target)
                && BikakuRoutes.atEnd(player, state.biteRoute, KaguneServerController.center(target), BikakuRules.MOUTH_RADIUS + 1.0 + target.m_20205_())) {
                bite(player, state, target, now);
            } else {
                double[] end = state.biteRoute[state.biteRoute.length - 1];
                KaguneServerController.playAt(player, new Vec3(end[0], end[1], end[2]), HexKaguneSounds.KAGUNE_WHIP, 0.6F,
                    KaguneServerController.vary(player, 0.6F, 0.05F));
            }
            return;
        }
        if (state.targetId != 0 && player.m_284548_().m_6815_(state.targetId) instanceof LivingEntity target && target.m_6084_()
            && KaguneServerController.validSwipeTarget(player, target)
            && KaguneServerController.center(target).m_82554_(from) <= reach + target.m_20205_()
            && KaguneServerController.clearLineTo(player, target)) {
            body = target;
        }
        Vec3 mouth = body != null ? KaguneServerController.center(body) : mouth(from, state.aim, reach);
        if (body == null) {
            double best = BikakuRules.MOUTH_RADIUS * BikakuRules.MOUTH_RADIUS;
            double r = BikakuRules.MOUTH_RADIUS + 1.0;
            AABB area = new AABB(mouth.f_82479_ - r, mouth.f_82480_ - r, mouth.f_82481_ - r, mouth.f_82479_ + r, mouth.f_82480_ + r, mouth.f_82481_ + r);
            for (LivingEntity near : player.m_284548_().m_6443_(LivingEntity.class, area,
                living -> living != player && KaguneServerController.validSwipeTarget(player, living))) {
                double d = KaguneServerController.center(near).m_82557_(mouth);
                if (d <= best && KaguneServerController.clearLineTo(player, near)) {
                    best = d;
                    body = near;
                }
            }
        }
        if (body == null) {
            KaguneServerController.playAt(player, mouth, HexKaguneSounds.KAGUNE_WHIP, 0.6F, KaguneServerController.vary(player, 0.6F, 0.05F));
            return;
        }
        bite(player, state, body, now);
    }

    /** Where a bite at {@code aim} shuts: the aim, or as far toward it as the worm reaches from over the shoulder. */
    static Vec3 mouth(Vec3 from, Vec3 aim) {
        return mouth(from, aim, BikakuRules.REACH);
    }

    /** Where a bite from {@code from} at {@code aim} shuts: the aim, or as far toward it as {@code reach}. */
    static Vec3 mouth(Vec3 from, Vec3 aim, double reach) {
        Vec3 to = aim.m_82546_(from);
        double distance = to.m_82553_();
        return distance <= reach || distance < 1.0E-6 ? aim : from.m_82549_(to.m_82490_(reach / distance));
    }

    /** The teeth close on {@code body}: its damage, its wound (punctures), bleeding, the heavy bite's stun and shake, and his feeding. */
    private static void bite(ServerPlayer player, State state, LivingEntity body, long now) {
        if (KaguneClash.meets(player, body, now)) {
            return; // 1.0.40: an M1 on a player whose own M1 is on its way back at him: the two meet, and neither lands
        }
        Bite bite = state.bite;
        float damage = BikakuRules.damage(KaguneProfile.mastery(player)) * bite.power();
        if (body instanceof net.minecraft.world.entity.player.Player) {
            damage *= BikakuRules.PVP_SHARE;
        }
        Vec3 centre = KaguneServerController.center(body);
        Vec3 into = centre.m_82546_(shoulder(player));
        into = into.m_82556_() < 1.0E-6 ? player.m_20154_() : into.m_82541_();
        // Its wound is the teeth's punctures (RinkakuWounds reads biting(): a stab where it shut).
        body.f_19802_ = 0; // every bite is its own blow, as on every path
        if (!body.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            return;
        }
        KaguneClash.landed(player);
        UkakuCrystalCombat.graftBleed(body, player, BikakuRules.bleedStacks(bite), now);
        if (bite.heavy()) {
            PaleStun.stun(body, now, BikakuRules.STUN);
            // Shaken and let go: thrown a little to his left, off the way it came.
            Vec3 side = new Vec3(-into.f_82481_, 0.0, into.f_82479_);
            side = side.m_82556_() < 1.0E-6 ? Vec3.f_82478_ : side.m_82541_();
            body.m_20256_(body.m_20184_().m_82490_(0.3).m_82549_(side.m_82490_(0.45)).m_82520_(0.0, 0.22, 0.0));
            body.f_19864_ = true;
        }
        KaguneServerController.playAt(player, centre, bite.heavy() ? HexKaguneSounds.BONE_CRUSH : HexKaguneSounds.FLESH_IMPACT, 0.9F,
            KaguneServerController.vary(player, bite.heavy() ? 0.85F : 1.0F, 0.08F));
        // Predatory Feeding: a bite gives back a little, a kill more.
        float heal = BikakuRules.BITE_HEAL + (body.m_6084_() ? 0.0F : BikakuRules.KILL_HEAL);
        if (player.m_21223_() < player.m_21233_()) {
            GhoulRegen.healOwn(player, heal); // 3.5.2: a ghoul's own heal (half a heart was taken for food's and cancelled)
            KaguneNetwork.sendTo(player, player);
        }
        KaguneProfile.addMastery(player, "Bikaku", 1);
        ControlServer.onKaguneHit(player, 1);
    }

    // ---------------------------------------------------------------- tracking

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject && isBikaku(subject)) {
            ControlNetwork.sendTo(viewer, snapshot(subject, state(subject)));
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player.m_20148_());
            if (isBikaku(player)) {
                broadcast(player, state(player));
            }
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State state = STATES.get(player.m_20148_());
            if (state != null) {
                BikakuMoves.release(player, state, false);
                state.calm();
                state.kakuja = false;
                if (isBikaku(player)) {
                    broadcast(player, state);
                }
            }
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State state = STATES.get(player.m_20148_());
            if (state != null) {
                BikakuMoves.release(player, state, false);
                state.calm();
                state.kakuja = false;
                if (isBikaku(player)) {
                    broadcast(player, state);
                }
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        State state = STATES.remove(event.getEntity().m_20148_());
        if (state != null && event.getEntity() instanceof ServerPlayer player) {
            BikakuMoves.release(player, state, false);
        }
    }
}

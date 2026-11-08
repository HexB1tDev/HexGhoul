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
        return state.bite != Bite.NONE && now - state.startedAt == state.bite.land() || BikakuMoves.biting(state, now);
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
            state.targets.clone(), state.heldId, state.caught);
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
        if (!isBikaku(player) || PaleWitness.frozen(player) || PaleRevive.down(player) || PaleStun.stunned(player) || !ready(player)) {
            return;
        }
        if (action != com.hex.kagune.control.network.C2SBikakuAction.KEY) {
            return;
        }
        BikakuMove.Slot[] slots = BikakuMove.Slot.values();
        if (arg <= 0 || arg >= slots.length) {
            return;
        }
        State state = state(player);
        long now = player.m_9236_().m_46467_();
        Entity hint = targetId > 0 ? player.m_284548_().m_6815_(targetId) : null;
        KakujaPassive.attacked(player);
        if (out(state, now) || BikakuMoves.busy(state, now)) {
            state.queuedSlot = slots[arg];
            state.queuedSlotAt = now;
            state.queuedSlotHint = targetId;
            return;
        }
        BikakuMoves.key(player, state, slots[arg], hint, now);
    }

    /** "{@code key}" on the action bar with its arguments, at most every few seconds. */
    static void notice(ServerPlayer player, State state, Component message) {
        long now = player.m_9236_().m_46467_();
        if (now >= state.nextNoticeAt || now < state.nextNoticeAt - NOTICE_TICKS) {
            state.nextNoticeAt = now + NOTICE_TICKS;
            player.m_5661_(message, true);
        }
    }

    /** Every running cooldown of his, over now ({@link Cooldowns}). */
    static void clearCooldowns(ServerPlayer player) {
        State state = STATES.get(player.m_20148_());
        if (state != null) {
            state.readyAt.clear();
        }
    }

    // ---------------------------------------------------------------- M1

    /** Trampoline target of the original M1 (through {@code KaguneCombat.startSwipe}): the next bite of the string. */
    public static void strike(ServerPlayer player, Entity hint) {
        if (!ready(player) || PaleWitness.frozen(player) || PaleRevive.down(player) || PaleStun.stunned(player)) {
            return;
        }
        State state = state(player);
        long now = player.m_9236_().m_46467_();
        if (out(state, now) || BikakuMoves.busy(state, now)) {
            state.queuedAt = now;
            state.queuedHint = hint == null ? 0 : hint.m_19879_();
            return;
        }
        begin(player, state, hint, now);
    }

    /** True while a bite is still out (until it is back at rest). */
    static boolean out(State state, long now) {
        return state.bite != Bite.NONE && now - state.startedAt < state.bite.ticks() && now >= state.startedAt;
    }

    private static void begin(ServerPlayer player, State state, Entity hint, long now) {
        boolean chained = state.lastBite != null && now <= state.chainEndsAt + BikakuRules.CHAIN_WINDOW;
        Bite bite = chained ? state.lastBite.next() : Bite.SNAP;
        if (!KaguneServerController.useEnergy(player, bite.cost(), true)) {
            return;
        }
        LivingEntity target = aim(player, hint);
        state.bite = bite;
        state.startedAt = now;
        state.targetId = target == null ? 0 : target.m_19879_();
        state.aim = target == null ? ahead(player) : KaguneServerController.center(target);
        state.landed = false;
        state.lastBite = bite;
        state.chainEndsAt = now + bite.ticks();
        state.queuedAt = Long.MIN_VALUE;
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, bite.heavy() ? 0.9F : 0.7F,
            KaguneServerController.vary(player, bite.heavy() ? 0.7F : 0.85F, 0.06F));
        // 1.0.40 (as every path's M1): another's M1 landing on him before this bite shuts meets it, and both stop.
        long began = state.startedAt;
        double hit = KaguneClash.strength(player, bite.power(), true);
        KaguneClash.swing(player, now, bite.land() + 1, BikakuRules.REACH, hit, () -> state.bite == bite && state.startedAt == began,
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
        Vec3 from = shoulder(player);
        double reach = BikakuRules.REACH;
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
        if (now % 20L == 0L) {
            // His profile keeps the neutral seed: whatever Rinkaku form or mutation found its way in is parked within a second.
            RinkakuStash.stash(player);
        }
        if (!KaguneProfile.isActive(player) || !player.m_6084_()) {
            if (state.bite != Bite.NONE || state.move != BikakuMove.NONE) {
                BikakuMoves.release(player, state, false);
                state.calm();
                broadcast(player, state);
            }
            return;
        }
        // 3.5.0: the move it is making; when it ends, a key or a click waiting for it.
        if (state.move != BikakuMove.NONE) {
            BikakuMoves.tick(player, state, now);
            if (state.move == BikakuMove.NONE) {
                if (state.queuedSlot != null && now - state.queuedSlotAt <= BikakuRules.BUFFER + 40) {
                    BikakuMove.Slot slot = state.queuedSlot;
                    state.queuedSlot = null;
                    Entity hint = state.queuedSlotHint > 0 ? player.m_284548_().m_6815_(state.queuedSlotHint) : null;
                    BikakuMoves.key(player, state, slot, hint, now);
                } else if (state.queuedAt != Long.MIN_VALUE && now - state.queuedAt <= BikakuRules.BUFFER + 40) {
                    Entity hint = state.queuedHint > 0 ? player.m_284548_().m_6815_(state.queuedHint) : null;
                    state.queuedAt = Long.MIN_VALUE;
                    begin(player, state, hint, now);
                }
                state.queuedSlot = null;
                state.queuedAt = Long.MIN_VALUE;
            }
        }
        if (state.bite != Bite.NONE) {
            long age = now - state.startedAt;
            if (!state.landed && age >= state.bite.land()) {
                state.landed = true;
                land(player, state, now);
            }
            if (age >= state.bite.ticks() || age < 0) {
                state.bite = Bite.NONE;
                state.startedAt = Long.MIN_VALUE;
                state.targetId = 0;
                state.dirty = true;
                if (state.queuedSlot != null && now - state.queuedSlotAt <= BikakuRules.BUFFER + state.lastBite.ticks()) {
                    // 3.5.0: a move key pressed during the bite goes first.
                    BikakuMove.Slot slot = state.queuedSlot;
                    state.queuedSlot = null;
                    Entity hint = state.queuedSlotHint > 0 ? player.m_284548_().m_6815_(state.queuedSlotHint) : null;
                    BikakuMoves.key(player, state, slot, hint, now);
                } else if (state.queuedAt != Long.MIN_VALUE && now - state.queuedAt <= BikakuRules.BUFFER + state.lastBite.ticks()) {
                    Entity hint = state.queuedHint > 0 ? player.m_284548_().m_6815_(state.queuedHint) : null;
                    state.queuedAt = Long.MIN_VALUE;
                    begin(player, state, hint, now);
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
        Vec3 from = shoulder(player);
        LivingEntity body = null;
        if (state.targetId != 0 && player.m_284548_().m_6815_(state.targetId) instanceof LivingEntity target && target.m_6084_()
            && KaguneServerController.validSwipeTarget(player, target)
            && KaguneServerController.center(target).m_82554_(from) <= BikakuRules.REACH + target.m_20205_()
            && KaguneServerController.clearLineTo(player, target)) {
            body = target;
        }
        Vec3 mouth = body != null ? KaguneServerController.center(body) : mouth(from, state.aim);
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
        Vec3 to = aim.m_82546_(from);
        double distance = to.m_82553_();
        return distance <= BikakuRules.REACH || distance < 1.0E-6 ? aim : from.m_82549_(to.m_82490_(BikakuRules.REACH / distance));
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
            player.m_5634_(heal);
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

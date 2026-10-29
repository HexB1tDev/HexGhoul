package com.hex.kagune.control.server;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.PaleStrike;
import com.hex.kagune.control.pale.EyeRules;
import com.hex.kagune.control.network.C2SPaleAction;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CPaleState;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.network.KaguneNetwork;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server owner of the Pale path (6.1.0): her base form is the original's manifested kagune (Alt + R brings it out, R puts it
 * away, the RC drains as it always has) drawn as nothing but her: one kakugan, steam off the skin, and the pale flesh only when a
 * move calls it out. This class holds what her body is doing ({@link PaleMove}, one move at a time), times her held keys, keeps
 * her cooldowns, and tells every client tracking her the moment a move begins so they all animate it in step. The moves
 * themselves are {@link PaleBlitz} (M1), {@link PaleMoves} (flips, Big Punch, the speed kick, the flesh hammer) and
 * {@link PaleFlesh} (the swipe, the grab and throw); launched bodies smash through weak blocks by {@link BlockSmash}.
 *
 * <p>Runs after the original controller each tick (LOW priority), like the Ukaku kit.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class PaleServer {
    private static final Map<UUID, PaleState> STATES = new HashMap<>();
    /** Ticks between two "not yet" reminders on the action bar. */
    private static final long NOTICE_TICKS = 40L;
    private static long nextErrorLogAt;

    /** Transient per-player Pale state on the logical server. */
    static final class PaleState {
        PaleMove move = PaleMove.NONE;
        int detail;
        long startedAt = Long.MIN_VALUE;
        int targetId;
        float charge;
        float yaw;
        Vec3 point = Vec3.f_82478_;
        /** Everything this move has already hit (each body at most once a move). */
        final Set<Integer> hit = new HashSet<>();
        /** Game time each move may next start. */
        long flipReadyAt = Long.MIN_VALUE;
        long punchReadyAt = Long.MIN_VALUE;
        long swipeReadyAt = Long.MIN_VALUE;
        long grabReadyAt = Long.MIN_VALUE;
        long kickReadyAt = Long.MIN_VALUE;
        long hammerReadyAt = Long.MIN_VALUE;
        /** 6.2.0: Watch This may next be called (after its tentacles are back in and its cooldown). */
        long watchReadyAt = Long.MIN_VALUE;
        /** 6.2.0: the Mutated Arm may next shoot out; whether it is out now (nothing else starts while it is). */
        long armReadyAt = Long.MIN_VALUE;
        boolean armOut;
        /**
         * 6.2.0: the Owl (her first kakuja): whether she is in it, when it began growing or going (long ago to start with: never
         * Long.MIN_VALUE, the time since would overflow), and when K is next heard.
         */
        boolean owl;
        long owlAt = -1_000_000L;
        long owlReadyAt = Long.MIN_VALUE;
        /** 6.2.0: the Owl's Rend (its M1): the last cut thrown (-1: none) and when it ends (a click within the chain window goes on). */
        int owlRend = -1;
        long owlRendEndsAt = Long.MIN_VALUE;
        /** The held keys (Z, J, V): the last tick the client said each was down. */
        long punchHeardAt = Long.MIN_VALUE;
        /** 6.2.2: an instant Big Punch went off on this press of Z: nothing more until Z is let go. */
        boolean punchSpent;
        long grabHeardAt = Long.MIN_VALUE;
        long hammerHeardAt = Long.MIN_VALUE;
        /** The Speed Blitz: the last strike thrown, when it ends, a click waiting for it, the recovery after the finisher. */
        PaleStrike lastStrike;
        long strikeEndsAt = Long.MIN_VALUE;
        long queuedAt = Long.MIN_VALUE;
        int queuedHint;
        long recoverUntil = Long.MIN_VALUE;
        /** A push renewed for a few ticks (a flip's takeoff, a step-in), and how many ticks are left. */
        Vec3 push;
        int pushTicks;
        /** 6.2.0: a move was cut off in the air: no fall hurts her until she is down again. */
        boolean fallSafe;
        /** A flip's touchdown (game time; MIN_VALUE while still in the air). */
        long landedAt = Long.MIN_VALUE;
        /** 6.2.0, Up We Go: this flip soars; which flip, its way (radians) and the next tick of its launch's ramp. */
        boolean soaring;
        int soarFlip;
        double soarYaw;
        int soarTick = PaleRules.SOAR_RAMP;
        /** The body the flesh holds (entity id; 0: none). */
        int heldId;
        /** J was let go while the flesh was still reaching: fling as soon as it holds. */
        boolean grabLetGo;
        /** V was let go before her leg was up: slam as soon as it is. */
        boolean hammerLetGo;
        /** Where the thrown body was when the throw began (blocks: to her right, up from her feet, ahead; see PaleRules.throwAt). */
        double[] thrownFrom = {0.0, PaleRules.GRAB_HOLD_UP, PaleRules.GRAB_HOLD_AHEAD};
        long nextNoticeAt = Long.MIN_VALUE;

        /** Nothing in flight (death, respawn, the kagune put away). */
        void calm() {
            this.move = PaleMove.NONE;
            this.detail = 0;
            this.startedAt = Long.MIN_VALUE;
            this.targetId = 0;
            this.charge = 0.0F;
            this.hit.clear();
            this.lastStrike = null;
            this.strikeEndsAt = Long.MIN_VALUE;
            this.queuedAt = Long.MIN_VALUE;
            this.push = null;
            this.pushTicks = 0;
            this.punchHeardAt = Long.MIN_VALUE;
            this.punchSpent = false;
            this.grabHeardAt = Long.MIN_VALUE;
            this.hammerHeardAt = Long.MIN_VALUE;
            this.heldId = 0;
            this.grabLetGo = false;
            this.hammerLetGo = false;
            this.soaring = false;
            this.soarTick = PaleRules.SOAR_RAMP;
        }
    }

    private PaleServer() {
    }

    public static boolean isPale(ServerPlayer player) {
        return KaguneProfile.isUnlocked(player) && PathServer.path(player) == KagunePath.PALE;
    }

    /**
     * 7.0.1: the shared eye-mode fight ({@link EyeRules}): any other ghoul with only the kakugan open (the kagune put away) fights
     * with her body: the Speed Blitz, the flips, the Big Punch and the Speed Kick, never the flesh.
     */
    public static boolean eyes(ServerPlayer player) {
        return !isPale(player) && KaguneProfile.isUnlocked(player) && GhoulServer.ghoul(player) && !KaguneProfile.isActive(player);
    }

    /** 7.0.1: how hard her body hits: all of it in her base form; the eyes alone, more the more mastered ({@link EyeRules#power}). */
    static float power(ServerPlayer player) {
        return isPale(player) ? 1.0F : EyeRules.power(KaguneProfile.mastery(player));
    }

    /** 7.0.1: a blow of hers landed on {@code targets} bodies: the kagune's frenzy hears of it only with the kagune out. */
    static void landed(ServerPlayer player, int targets) {
        if (KaguneProfile.isActive(player)) {
            ControlServer.onKaguneHit(player, targets);
        }
    }

    static PaleState peek(ServerPlayer player) {
        return STATES.get(player.m_20148_());
    }

    static PaleState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.m_20148_(), ignored -> new PaleState());
    }

    /** The move her body is making now (NONE when she has no state). */
    public static PaleMove move(ServerPlayer player) {
        PaleState state = STATES.get(player.m_20148_());
        return state == null ? PaleMove.NONE : state.move;
    }

    /** 6.2.0: true while nothing can hurt her: the Speed Kick's lunge and its dash, until she is there ({@link PaleRules#kickUntouchable}). */
    static boolean untouchable(ServerPlayer player) {
        PaleState state = STATES.get(player.m_20148_());
        if (state == null || state.move == null) {
            return false;
        }
        long age = player.m_9236_().m_46467_() - state.startedAt;
        // 6.2.2: every dash is unseen until its blow lands: the Speed Kick's (and the Owl's), the Big Punch's blink, Witness This.
        return switch (state.move) {
            case KICK -> PaleRules.kickUntouchable(state.detail, age) || age <= PaleRules.kickArrive(state.detail) + PaleRules.KICK_CONTACT;
            case OWL_KICK -> age <= Math.max(1, state.detail) + com.hex.kagune.control.pale.OwlRules.KICK_SKEWER;
            case PUNCH, OWL_PUNCH -> age <= PaleRules.PUNCH_IMPACT;
            case WITNESS -> age < com.hex.kagune.control.pale.WitnessRules.FREE;
            default -> false;
        };
    }

    /** 6.2.0: whatever would hurt her while she is gone in a dash (6.2.2: any of them, until its blow) misses: no damage, no knockback, no flash. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attacked(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !STATES.isEmpty() && untouchable(player)) {
            event.setCanceled(true);
        }
    }

    /** 6.2.1: in the Owl (grown or growing) she tanks it: whatever hurts her is cut to {@link com.hex.kagune.control.pale.OwlRules#TAKEN}. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void owlHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !STATES.isEmpty() && event.getAmount() > 0.0F) {
            PaleState state = peek(player);
            if (state != null && state.owl) {
                event.setAmount(com.hex.kagune.control.pale.OwlRules.taken(event.getAmount()));
            }
        }
    }

    /** True while her base form is out (7.0.1: or another ghoul's eyes alone are open) and she is alive: the only time her moves are heard. */
    static boolean ready(ServerPlayer player) {
        return (isPale(player) && KaguneProfile.isActive(player) || eyes(player)) && player.m_6084_();
    }

    /** Starts {@code move} now and tells every client. */
    static void begin(ServerPlayer player, PaleState state, PaleMove move, int detail, int targetId, float charge, Vec3 point, long now) {
        begin(player, state, move, detail, targetId, charge, point, now, player.m_146908_());
    }

    /** As above, thrown the way {@code yaw} faces (degrees), whatever way she looks. */
    static void begin(ServerPlayer player, PaleState state, PaleMove move, int detail, int targetId, float charge, Vec3 point, long now, float yaw) {
        state.move = move;
        state.detail = detail;
        state.startedAt = now;
        state.targetId = targetId;
        state.charge = charge;
        state.yaw = yaw;
        state.point = point == null ? Vec3.f_82478_ : point;
        state.hit.clear();
        broadcast(player, state);
    }

    /** Back to nothing (her stance), and every client told. */
    static void end(ServerPlayer player, PaleState state) {
        if (state.move == PaleMove.NONE) {
            return;
        }
        state.move = PaleMove.NONE;
        state.detail = 0;
        state.targetId = 0;
        state.charge = 0.0F;
        state.hit.clear();
        broadcast(player, state);
    }

    static S2CPaleState snapshot(ServerPlayer player, PaleState state) {
        return new S2CPaleState(player.m_20148_(), state.move.ordinal(), state.detail, state.startedAt, state.targetId, state.charge, state.yaw,
            state.point.f_82479_, state.point.f_82480_, state.point.f_82481_);
    }

    static void broadcast(ServerPlayer player, PaleState state) {
        ControlNetwork.broadcast(player, snapshot(player, state));
    }

    /** "{@code name} unlocks at mastery {@code mastery}" on the action bar, at most every two seconds. */
    static boolean locked(ServerPlayer player, PaleState state, String name, int mastery) {
        if (KaguneProfile.mastery(player) >= mastery) {
            return false;
        }
        notice(player, state, Component.m_237110_("message.hexkagune.pale.locked", name, mastery));
        return true;
    }

    static void notice(ServerPlayer player, PaleState state, Component message) {
        long now = player.m_9236_().m_46467_();
        if (now >= state.nextNoticeAt || now < state.nextNoticeAt - NOTICE_TICKS) {
            state.nextNoticeAt = now + NOTICE_TICKS;
            player.m_5661_(message, true);
        }
    }

    /** 6.2.0: every running cooldown of hers ended now ({@link Cooldowns}). */
    static void clearCooldowns(ServerPlayer player) {
        PaleState state = STATES.get(player.m_20148_());
        if (state == null) {
            return;
        }
        state.flipReadyAt = Long.MIN_VALUE;
        state.punchReadyAt = Long.MIN_VALUE;
        state.swipeReadyAt = Long.MIN_VALUE;
        state.grabReadyAt = Long.MIN_VALUE;
        state.kickReadyAt = Long.MIN_VALUE;
        state.hammerReadyAt = Long.MIN_VALUE;
        state.watchReadyAt = Long.MIN_VALUE;
        state.armReadyAt = Long.MIN_VALUE;
        state.owlReadyAt = Long.MIN_VALUE;
        state.recoverUntil = Long.MIN_VALUE;
    }

    /**
     * Called by {@link PathServer#setPath} through {@code UkakuServer.onPathChanged}: whatever was in flight stops (a held body is
     * let go) and every client learns she stands still.
     */
    static void onPathChanged(ServerPlayer player) {
        PaleRevive.end(player);
        if (isPale(player)) {
            PaleRevive.sync(player);
        }
        PaleState state = STATES.remove(player.m_20148_());
        PaleWatch.forget(player);
        OwlVolley.forget(player);
        OwlWatch.forget(player);
        PaleArm.forget(player, state);
        PaleOwl.forget(player, state);
        if (state != null) {
            PaleFlesh.release(player, state);
            state.calm();
            broadcast(player, state);
        }
    }

    /**
     * 6.2.0: her body has just been blown apart (You Thought You'd Get Rid of Me That Easily?, {@link PaleRevive}): whatever was in
     * flight stops at once (the flesh lets go, the arm, the tentacles and the Owl are gone) and every client learns she stands
     * still.
     */
    static void apart(ServerPlayer player) {
        PaleState state = STATES.get(player.m_20148_());
        PaleWatch.forget(player);
        OwlVolley.forget(player);
        OwlWatch.forget(player);
        PaleArm.forget(player, state);
        PaleOwl.forget(player, state);
        if (state != null) {
            PaleFlesh.release(player, state);
            state.calm();
            broadcast(player, state);
        }
    }

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
                KaguneMod.LOGGER.error("HexKagune Owl DNA tick failed", failure);
            }
        }
    }

    private static void tick(ServerPlayer player) {
        if (!isPale(player)) {
            eyesTick(player);
            return;
        }
        PaleState state = state(player);
        long now = player.m_9236_().m_46467_();
        if (now % 20L == 0L) {
            // Her profile keeps the neutral seed: whatever Rinkaku form or mutation found its way in is parked within a second.
            RinkakuStash.stash(player);
        }
        if (!KaguneProfile.isActive(player) || !player.m_6084_()) {
            PaleWatch.forget(player);
            OwlVolley.forget(player);
            OwlWatch.forget(player);
            if (state.armOut) {
                PaleArm.forget(player, state);
            }
            PaleOwl.forget(player, state);
            if (state.move != PaleMove.NONE || state.heldId != 0) {
                PaleFlesh.release(player, state);
                state.calm();
                broadcast(player, state);
            }
            return;
        }
        regenerate(player, now);
        body(player, state, now);
        PaleFlesh.tick(player, state, now);
        PaleWatch.tick(player, now);
        OwlWatch.tick(player, now);
        OwlVolley.tick(player, state, now);
        PaleArm.tick(player, state, now);
        PaleOwl.tick(player, state, now);
    }

    /** One tick of her body's moves (the strikes, flips, Big Punch and Speed Kick), and the pushes and the safe falls they leave. */
    private static void body(ServerPlayer player, PaleState state, long now) {
        if (state.pushTicks > 0 && state.push != null) {
            state.pushTicks--;
            UkakuFire.push(player, state.push);
        }
        if (state.soarTick > 0 && state.soarTick < PaleRules.SOAR_RAMP) {
            PaleMoves.soarTick(player, state);
        }
        safeFall(player, state);
        PaleBlitz.tick(player, state, now);
        PaleMoves.tick(player, state, now);
    }

    /** A move cut off in the air: no fall hurts until the ground (or water) is reached. */
    private static void safeFall(ServerPlayer player, PaleState state) {
        if (state.fallSafe) {
            if (player.m_20096_() || player.m_20069_()) {
                state.fallSafe = false;
            } else {
                player.f_19789_ = 0.0F;
            }
        }
    }

    /**
     * 7.0.1: the shared eye-mode fight's tick for every other ghoul: her body's moves while only the eyes are open. The eyes
     * closed, the kagune out or death end whatever was in flight at once (every client sees it stop; a body caught mid-air still
     * lands without hurt) and the path's own kit is all there is.
     */
    private static void eyesTick(ServerPlayer player) {
        PaleState state = peek(player);
        if (!eyes(player) || !player.m_6084_()) {
            if (state != null) {
                if (state.move != PaleMove.NONE) {
                    state.fallSafe |= !player.m_20096_();
                    state.calm();
                    broadcast(player, state);
                }
                safeFall(player, state);
            }
            return;
        }
        body(player, state == null ? state(player) : state, player.m_9236_().m_46467_());
    }

    /**
     * Her RC comes back while her form is out (she keeps no kagune out to feed; the original only refills it with the kagune put
     * away), faster with mastery ({@link PaleRules#formRegen}); the client hears of it every half second.
     */
    private static void regenerate(ServerPlayer player, long now) {
        if (KaguneProfile.energy(player) >= KaguneProfile.MAX_ENERGY) {
            return;
        }
        KaguneProfile.restoreEnergy(player, PaleRules.formRegen(KaguneProfile.mastery(player)));
        if (now % 10L == 0L) {
            KaguneNetwork.sendTo(player, player);
        }
    }

    /** An Pale input from {@code C2SPaleAction}. */
    public static void onAction(ServerPlayer player, int action, int arg, int targetId) {
        if (PaleStun.stunned(player) && action != C2SPaleAction.MUTATION && action != C2SPaleAction.INSTANT_PUNCH) {
            return; // 7.0.2: stunned, nothing he does counts (his menu aside)
        }
        if (!isPale(player)) {
            eyesAction(player, action, arg, targetId);
            return;
        }
        if (action == C2SPaleAction.STRIKE) {
            return; // (her M1 comes through the original's swipe)
        }
        if (action == C2SPaleAction.MUTATION) {
            PaleRevive.unlock(player, arg); // from her menu, whatever she is doing
            return;
        }
        if (action == C2SPaleAction.INSTANT_PUNCH) {
            PaleRevive.instantPunch(player, arg != 0);
            return;
        }
        PaleState state = state(player);
        if (!ready(player) || PaleRevive.down(player) || PaleWitness.frozen(player)) {
            return; // (in pieces, or 6.2.0 frozen in someone's Witness This: nothing she does counts)
        }
        long now = player.m_9236_().m_46467_();
        if (action == C2SPaleAction.KAKUJA) {
            PaleOwl.key(player, state, arg != 0, now);
            return;
        }
        if (PaleOwl.busy(state, now)) {
            // 6.2.0: in the Owl her keys are its own moves (nothing while it grows or goes).
            if (!PaleOwl.ready(state, now)) {
                return;
            }
            switch (action) {
                case C2SPaleAction.PUNCH -> PaleMoves.punchKey(player, state, arg != 0, targetId, now);
                case C2SPaleAction.KICK -> PaleOwlMoves.kick(player, state, now);
                case C2SPaleAction.GRAB -> OwlVolley.key(player, state, arg == 0 ? 0 : state.move == PaleMove.OWL_VOLLEY ? 1 : 2, now);
                case C2SPaleAction.SWIPE -> PaleOwlMoves.whirl(player, state, now);
                case C2SPaleAction.FLIP -> PaleOwlMoves.pounce(player, state, arg, now);
                case C2SPaleAction.HAMMER -> PaleOwlMoves.fissureKey(player, state, arg != 0, now);
                case C2SPaleAction.WATCH -> OwlWatch.start(player, state, now);
                case C2SPaleAction.ARM_TAP -> OwlGrab.start(player, state, false, now);
                case C2SPaleAction.ARM -> OwlGrab.key(player, state, arg, now);
                default -> {
                }
            }
            return;
        }
        switch (action) {
            case C2SPaleAction.FLIP -> PaleMoves.flip(player, state, arg, now);
            case C2SPaleAction.PUNCH -> PaleMoves.punchKey(player, state, arg != 0, targetId, now);
            case C2SPaleAction.SWIPE -> PaleFlesh.swipe(player, state, now);
            case C2SPaleAction.GRAB -> PaleFlesh.grabKey(player, state, arg != 0, targetId, now);
            case C2SPaleAction.KICK -> PaleMoves.kick(player, state, now);
            case C2SPaleAction.HAMMER -> PaleMoves.hammerKey(player, state, arg != 0, now);
            case C2SPaleAction.WATCH -> PaleWatch.start(player, state, now);
            case C2SPaleAction.ARM_TAP -> PaleArm.start(player, state, false, now);
            case C2SPaleAction.ARM -> PaleArm.key(player, state, arg, now);
            default -> {
            }
        }
    }

    /**
     * 7.0.1: the eyes alone hear only her body's moves: M1 (sent by the client, the original only sends one with the kagune out),
     * the flips, the Big Punch and the Speed Kick.
     */
    private static void eyesAction(ServerPlayer player, int action, int arg, int targetId) {
        if (!eyes(player) || !ready(player) || PaleWitness.frozen(player)) {
            return;
        }
        PaleState state = state(player);
        long now = player.m_9236_().m_46467_();
        switch (action) {
            case C2SPaleAction.STRIKE -> PaleBlitz.click(player, targetId == 0 ? null : player.m_9236_().m_6815_(targetId));
            case C2SPaleAction.FLIP -> PaleMoves.flip(player, state, arg, now);
            case C2SPaleAction.PUNCH -> PaleMoves.punchKey(player, state, arg != 0, targetId, now);
            case C2SPaleAction.KICK -> PaleMoves.kick(player, state, now);
            default -> {
            }
        }
    }

    /** 7.0.1: with only the eyes open a vanilla punch is the Speed Blitz instead (the client sends it so; this catches the rest). */
    @SubscribeEvent
    public static void punched(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && eyes(player)) {
            event.setCanceled(true);
            PaleBlitz.click(player, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject && eyes(subject)) {
            PaleState state = STATES.get(subject.m_20148_());
            if (state != null && state.move != PaleMove.NONE) {
                ControlNetwork.sendTo(viewer, snapshot(subject, state));
            }
            return;
        }
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject && isPale(subject)) {
            PaleState state = STATES.get(subject.m_20148_());
            if (state != null && state.move != PaleMove.NONE) {
                ControlNetwork.sendTo(viewer, snapshot(subject, state));
            }
            PaleArm.tracked(viewer, subject);
            PaleOwl.tracked(viewer, subject, state);
            OwlWatch.tracked(viewer, subject);
            OwlVolley.tracked(viewer, subject);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PaleState state = STATES.get(player.m_20148_());
            PaleWatch.forget(player);
            OwlVolley.forget(player);
            OwlWatch.forget(player);
            PaleArm.forget(player, state);
            PaleOwl.forget(player, state);
            if (state != null) {
                PaleFlesh.release(player, state);
                state.calm();
            }
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PaleState state = STATES.get(player.m_20148_());
            PaleWatch.forget(player);
            OwlVolley.forget(player);
            OwlWatch.forget(player);
            PaleArm.forget(player, state);
            PaleOwl.forget(player, state);
            if (state != null) {
                PaleFlesh.release(player, state);
                state.calm();
                broadcast(player, state);
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PaleState state = STATES.remove(player.m_20148_());
            PaleWatch.forget(player);
            OwlVolley.forget(player);
            OwlWatch.forget(player);
            PaleArm.forget(player, state);
            PaleOwl.forget(player, state);
            if (state != null) {
                PaleFlesh.release(player, state);
            }
        }
    }
}

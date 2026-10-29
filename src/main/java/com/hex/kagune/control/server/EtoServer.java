package com.hex.kagune.control.server;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.eto.EtoMove;
import com.hex.kagune.control.eto.EtoRules;
import com.hex.kagune.control.eto.EtoStrike;
import com.hex.kagune.control.eto.EyeRules;
import com.hex.kagune.control.network.C2SEtoAction;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CEtoState;
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
 * Server owner of the Eto path (6.1.0): her base form is the original's manifested kagune (Alt + R brings it out, R puts it
 * away, the RC drains as it always has) drawn as nothing but her: one kakugan, steam off the skin, and the pale flesh only when a
 * move calls it out. This class holds what her body is doing ({@link EtoMove}, one move at a time), times her held keys, keeps
 * her cooldowns, and tells every client tracking her the moment a move begins so they all animate it in step. The moves
 * themselves are {@link EtoBlitz} (M1), {@link EtoMoves} (flips, Big Punch, the speed kick, the flesh hammer) and
 * {@link EtoFlesh} (the swipe, the grab and throw); launched bodies smash through weak blocks by {@link BlockSmash}.
 *
 * <p>Runs after the original controller each tick (LOW priority), like the Ayato kit.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class EtoServer {
    private static final Map<UUID, EtoState> STATES = new HashMap<>();
    /** Ticks between two "not yet" reminders on the action bar. */
    private static final long NOTICE_TICKS = 40L;
    private static long nextErrorLogAt;

    /** Transient per-player Eto state on the logical server. */
    static final class EtoState {
        EtoMove move = EtoMove.NONE;
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
        EtoStrike lastStrike;
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
        int soarTick = EtoRules.SOAR_RAMP;
        /** The body the flesh holds (entity id; 0: none). */
        int heldId;
        /** J was let go while the flesh was still reaching: fling as soon as it holds. */
        boolean grabLetGo;
        /** V was let go before her leg was up: slam as soon as it is. */
        boolean hammerLetGo;
        /** Where the thrown body was when the throw began (blocks: to her right, up from her feet, ahead; see EtoRules.throwAt). */
        double[] thrownFrom = {0.0, EtoRules.GRAB_HOLD_UP, EtoRules.GRAB_HOLD_AHEAD};
        long nextNoticeAt = Long.MIN_VALUE;

        /** Nothing in flight (death, respawn, the kagune put away). */
        void calm() {
            this.move = EtoMove.NONE;
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
            this.soarTick = EtoRules.SOAR_RAMP;
        }
    }

    private EtoServer() {
    }

    public static boolean isEto(ServerPlayer player) {
        return KaguneProfile.isUnlocked(player) && PathServer.path(player) == KagunePath.ETO;
    }

    /**
     * 7.0.1: the shared eye-mode fight ({@link EyeRules}): any other ghoul with only the kakugan open (the kagune put away) fights
     * with her body: the Speed Blitz, the flips, the Big Punch and the Speed Kick, never the flesh.
     */
    public static boolean eyes(ServerPlayer player) {
        return !isEto(player) && KaguneProfile.isUnlocked(player) && GhoulServer.ghoul(player) && !KaguneProfile.isActive(player);
    }

    /** 7.0.1: how hard her body hits: all of it in her base form; the eyes alone, more the more mastered ({@link EyeRules#power}). */
    static float power(ServerPlayer player) {
        return isEto(player) ? 1.0F : EyeRules.power(KaguneProfile.mastery(player));
    }

    /** 7.0.1: a blow of hers landed on {@code targets} bodies: the kagune's frenzy hears of it only with the kagune out. */
    static void landed(ServerPlayer player, int targets) {
        if (KaguneProfile.isActive(player)) {
            ControlServer.onKaguneHit(player, targets);
        }
    }

    static EtoState peek(ServerPlayer player) {
        return STATES.get(player.m_20148_());
    }

    static EtoState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.m_20148_(), ignored -> new EtoState());
    }

    /** The move her body is making now (NONE when she has no state). */
    public static EtoMove move(ServerPlayer player) {
        EtoState state = STATES.get(player.m_20148_());
        return state == null ? EtoMove.NONE : state.move;
    }

    /** 6.2.0: true while nothing can hurt her: the Speed Kick's lunge and its dash, until she is there ({@link EtoRules#kickUntouchable}). */
    static boolean untouchable(ServerPlayer player) {
        EtoState state = STATES.get(player.m_20148_());
        if (state == null || state.move == null) {
            return false;
        }
        long age = player.m_9236_().m_46467_() - state.startedAt;
        // 6.2.2: every dash is unseen until its blow lands: the Speed Kick's (and the Owl's), the Big Punch's blink, Witness This.
        return switch (state.move) {
            case KICK -> EtoRules.kickUntouchable(state.detail, age) || age <= EtoRules.kickArrive(state.detail) + EtoRules.KICK_CONTACT;
            case OWL_KICK -> age <= Math.max(1, state.detail) + com.hex.kagune.control.eto.OwlRules.KICK_SKEWER;
            case PUNCH, OWL_PUNCH -> age <= EtoRules.PUNCH_IMPACT;
            case WITNESS -> age < com.hex.kagune.control.eto.WitnessRules.FREE;
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

    /** 6.2.1: in the Owl (grown or growing) she tanks it: whatever hurts her is cut to {@link com.hex.kagune.control.eto.OwlRules#TAKEN}. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void owlHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !STATES.isEmpty() && event.getAmount() > 0.0F) {
            EtoState state = peek(player);
            if (state != null && state.owl) {
                event.setAmount(com.hex.kagune.control.eto.OwlRules.taken(event.getAmount()));
            }
        }
    }

    /** True while her base form is out (7.0.1: or another ghoul's eyes alone are open) and she is alive: the only time her moves are heard. */
    static boolean ready(ServerPlayer player) {
        return (isEto(player) && KaguneProfile.isActive(player) || eyes(player)) && player.m_6084_();
    }

    /** Starts {@code move} now and tells every client. */
    static void begin(ServerPlayer player, EtoState state, EtoMove move, int detail, int targetId, float charge, Vec3 point, long now) {
        begin(player, state, move, detail, targetId, charge, point, now, player.m_146908_());
    }

    /** As above, thrown the way {@code yaw} faces (degrees), whatever way she looks. */
    static void begin(ServerPlayer player, EtoState state, EtoMove move, int detail, int targetId, float charge, Vec3 point, long now, float yaw) {
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
    static void end(ServerPlayer player, EtoState state) {
        if (state.move == EtoMove.NONE) {
            return;
        }
        state.move = EtoMove.NONE;
        state.detail = 0;
        state.targetId = 0;
        state.charge = 0.0F;
        state.hit.clear();
        broadcast(player, state);
    }

    static S2CEtoState snapshot(ServerPlayer player, EtoState state) {
        return new S2CEtoState(player.m_20148_(), state.move.ordinal(), state.detail, state.startedAt, state.targetId, state.charge, state.yaw,
            state.point.f_82479_, state.point.f_82480_, state.point.f_82481_);
    }

    static void broadcast(ServerPlayer player, EtoState state) {
        ControlNetwork.broadcast(player, snapshot(player, state));
    }

    /** "{@code name} unlocks at mastery {@code mastery}" on the action bar, at most every two seconds. */
    static boolean locked(ServerPlayer player, EtoState state, String name, int mastery) {
        if (KaguneProfile.mastery(player) >= mastery) {
            return false;
        }
        notice(player, state, Component.m_237110_("message.hexkagune.eto.locked", name, mastery));
        return true;
    }

    static void notice(ServerPlayer player, EtoState state, Component message) {
        long now = player.m_9236_().m_46467_();
        if (now >= state.nextNoticeAt || now < state.nextNoticeAt - NOTICE_TICKS) {
            state.nextNoticeAt = now + NOTICE_TICKS;
            player.m_5661_(message, true);
        }
    }

    /** 6.2.0: every running cooldown of hers ended now ({@link Cooldowns}). */
    static void clearCooldowns(ServerPlayer player) {
        EtoState state = STATES.get(player.m_20148_());
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
     * Called by {@link PathServer#setPath} through {@code AyatoServer.onPathChanged}: whatever was in flight stops (a held body is
     * let go) and every client learns she stands still.
     */
    static void onPathChanged(ServerPlayer player) {
        EtoRevive.end(player);
        if (isEto(player)) {
            EtoRevive.sync(player);
        }
        EtoState state = STATES.remove(player.m_20148_());
        EtoWatch.forget(player);
        OwlVolley.forget(player);
        OwlWatch.forget(player);
        EtoArm.forget(player, state);
        EtoOwl.forget(player, state);
        if (state != null) {
            EtoFlesh.release(player, state);
            state.calm();
            broadcast(player, state);
        }
    }

    /**
     * 6.2.0: her body has just been blown apart (You Thought You'd Get Rid of Me That Easily?, {@link EtoRevive}): whatever was in
     * flight stops at once (the flesh lets go, the arm, the tentacles and the Owl are gone) and every client learns she stands
     * still.
     */
    static void apart(ServerPlayer player) {
        EtoState state = STATES.get(player.m_20148_());
        EtoWatch.forget(player);
        OwlVolley.forget(player);
        OwlWatch.forget(player);
        EtoArm.forget(player, state);
        EtoOwl.forget(player, state);
        if (state != null) {
            EtoFlesh.release(player, state);
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
        if (!isEto(player)) {
            eyesTick(player);
            return;
        }
        EtoState state = state(player);
        long now = player.m_9236_().m_46467_();
        if (now % 20L == 0L) {
            // Her profile keeps the neutral seed: whatever Kaneki form or mutation found its way in is parked within a second.
            KanekiStash.stash(player);
        }
        if (!KaguneProfile.isActive(player) || !player.m_6084_()) {
            EtoWatch.forget(player);
            OwlVolley.forget(player);
            OwlWatch.forget(player);
            if (state.armOut) {
                EtoArm.forget(player, state);
            }
            EtoOwl.forget(player, state);
            if (state.move != EtoMove.NONE || state.heldId != 0) {
                EtoFlesh.release(player, state);
                state.calm();
                broadcast(player, state);
            }
            return;
        }
        regenerate(player, now);
        body(player, state, now);
        EtoFlesh.tick(player, state, now);
        EtoWatch.tick(player, now);
        OwlWatch.tick(player, now);
        OwlVolley.tick(player, state, now);
        EtoArm.tick(player, state, now);
        EtoOwl.tick(player, state, now);
    }

    /** One tick of her body's moves (the strikes, flips, Big Punch and Speed Kick), and the pushes and the safe falls they leave. */
    private static void body(ServerPlayer player, EtoState state, long now) {
        if (state.pushTicks > 0 && state.push != null) {
            state.pushTicks--;
            AyatoFire.push(player, state.push);
        }
        if (state.soarTick > 0 && state.soarTick < EtoRules.SOAR_RAMP) {
            EtoMoves.soarTick(player, state);
        }
        safeFall(player, state);
        EtoBlitz.tick(player, state, now);
        EtoMoves.tick(player, state, now);
    }

    /** A move cut off in the air: no fall hurts until the ground (or water) is reached. */
    private static void safeFall(ServerPlayer player, EtoState state) {
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
        EtoState state = peek(player);
        if (!eyes(player) || !player.m_6084_()) {
            if (state != null) {
                if (state.move != EtoMove.NONE) {
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
     * away), faster with mastery ({@link EtoRules#formRegen}); the client hears of it every half second.
     */
    private static void regenerate(ServerPlayer player, long now) {
        if (KaguneProfile.energy(player) >= KaguneProfile.MAX_ENERGY) {
            return;
        }
        KaguneProfile.restoreEnergy(player, EtoRules.formRegen(KaguneProfile.mastery(player)));
        if (now % 10L == 0L) {
            KaguneNetwork.sendTo(player, player);
        }
    }

    /** An Eto input from {@code C2SEtoAction}. */
    public static void onAction(ServerPlayer player, int action, int arg, int targetId) {
        if (EtoStun.stunned(player) && action != C2SEtoAction.MUTATION && action != C2SEtoAction.INSTANT_PUNCH) {
            return; // 7.0.2: stunned, nothing he does counts (his menu aside)
        }
        if (!isEto(player)) {
            eyesAction(player, action, arg, targetId);
            return;
        }
        if (action == C2SEtoAction.STRIKE) {
            return; // (her M1 comes through the original's swipe)
        }
        if (action == C2SEtoAction.MUTATION) {
            EtoRevive.unlock(player, arg); // from her menu, whatever she is doing
            return;
        }
        if (action == C2SEtoAction.INSTANT_PUNCH) {
            EtoRevive.instantPunch(player, arg != 0);
            return;
        }
        EtoState state = state(player);
        if (!ready(player) || EtoRevive.down(player) || EtoWitness.frozen(player)) {
            return; // (in pieces, or 6.2.0 frozen in someone's Witness This: nothing she does counts)
        }
        long now = player.m_9236_().m_46467_();
        if (action == C2SEtoAction.KAKUJA) {
            EtoOwl.key(player, state, arg != 0, now);
            return;
        }
        if (EtoOwl.busy(state, now)) {
            // 6.2.0: in the Owl her keys are its own moves (nothing while it grows or goes).
            if (!EtoOwl.ready(state, now)) {
                return;
            }
            switch (action) {
                case C2SEtoAction.PUNCH -> EtoMoves.punchKey(player, state, arg != 0, targetId, now);
                case C2SEtoAction.KICK -> EtoOwlMoves.kick(player, state, now);
                case C2SEtoAction.GRAB -> OwlVolley.key(player, state, arg == 0 ? 0 : state.move == EtoMove.OWL_VOLLEY ? 1 : 2, now);
                case C2SEtoAction.SWIPE -> EtoOwlMoves.whirl(player, state, now);
                case C2SEtoAction.FLIP -> EtoOwlMoves.pounce(player, state, arg, now);
                case C2SEtoAction.HAMMER -> EtoOwlMoves.fissureKey(player, state, arg != 0, now);
                case C2SEtoAction.WATCH -> OwlWatch.start(player, state, now);
                case C2SEtoAction.ARM_TAP -> OwlGrab.start(player, state, false, now);
                case C2SEtoAction.ARM -> OwlGrab.key(player, state, arg, now);
                default -> {
                }
            }
            return;
        }
        switch (action) {
            case C2SEtoAction.FLIP -> EtoMoves.flip(player, state, arg, now);
            case C2SEtoAction.PUNCH -> EtoMoves.punchKey(player, state, arg != 0, targetId, now);
            case C2SEtoAction.SWIPE -> EtoFlesh.swipe(player, state, now);
            case C2SEtoAction.GRAB -> EtoFlesh.grabKey(player, state, arg != 0, targetId, now);
            case C2SEtoAction.KICK -> EtoMoves.kick(player, state, now);
            case C2SEtoAction.HAMMER -> EtoMoves.hammerKey(player, state, arg != 0, now);
            case C2SEtoAction.WATCH -> EtoWatch.start(player, state, now);
            case C2SEtoAction.ARM_TAP -> EtoArm.start(player, state, false, now);
            case C2SEtoAction.ARM -> EtoArm.key(player, state, arg, now);
            default -> {
            }
        }
    }

    /**
     * 7.0.1: the eyes alone hear only her body's moves: M1 (sent by the client, the original only sends one with the kagune out),
     * the flips, the Big Punch and the Speed Kick.
     */
    private static void eyesAction(ServerPlayer player, int action, int arg, int targetId) {
        if (!eyes(player) || !ready(player) || EtoWitness.frozen(player)) {
            return;
        }
        EtoState state = state(player);
        long now = player.m_9236_().m_46467_();
        switch (action) {
            case C2SEtoAction.STRIKE -> EtoBlitz.click(player, targetId == 0 ? null : player.m_9236_().m_6815_(targetId));
            case C2SEtoAction.FLIP -> EtoMoves.flip(player, state, arg, now);
            case C2SEtoAction.PUNCH -> EtoMoves.punchKey(player, state, arg != 0, targetId, now);
            case C2SEtoAction.KICK -> EtoMoves.kick(player, state, now);
            default -> {
            }
        }
    }

    /** 7.0.1: with only the eyes open a vanilla punch is the Speed Blitz instead (the client sends it so; this catches the rest). */
    @SubscribeEvent
    public static void punched(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && eyes(player)) {
            event.setCanceled(true);
            EtoBlitz.click(player, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject && eyes(subject)) {
            EtoState state = STATES.get(subject.m_20148_());
            if (state != null && state.move != EtoMove.NONE) {
                ControlNetwork.sendTo(viewer, snapshot(subject, state));
            }
            return;
        }
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject && isEto(subject)) {
            EtoState state = STATES.get(subject.m_20148_());
            if (state != null && state.move != EtoMove.NONE) {
                ControlNetwork.sendTo(viewer, snapshot(subject, state));
            }
            EtoArm.tracked(viewer, subject);
            EtoOwl.tracked(viewer, subject, state);
            OwlWatch.tracked(viewer, subject);
            OwlVolley.tracked(viewer, subject);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EtoState state = STATES.get(player.m_20148_());
            EtoWatch.forget(player);
            OwlVolley.forget(player);
            OwlWatch.forget(player);
            EtoArm.forget(player, state);
            EtoOwl.forget(player, state);
            if (state != null) {
                EtoFlesh.release(player, state);
                state.calm();
            }
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EtoState state = STATES.get(player.m_20148_());
            EtoWatch.forget(player);
            OwlVolley.forget(player);
            OwlWatch.forget(player);
            EtoArm.forget(player, state);
            EtoOwl.forget(player, state);
            if (state != null) {
                EtoFlesh.release(player, state);
                state.calm();
                broadcast(player, state);
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EtoState state = STATES.remove(player.m_20148_());
            EtoWatch.forget(player);
            OwlVolley.forget(player);
            OwlWatch.forget(player);
            EtoArm.forget(player, state);
            EtoOwl.forget(player, state);
            if (state != null) {
                EtoFlesh.release(player, state);
            }
        }
    }
}

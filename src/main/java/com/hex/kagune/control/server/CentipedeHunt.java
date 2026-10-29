package com.hex.kagune.control.server;

import com.hex.kagune.control.ayato.CentipedeMotion;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.CentipedeTechnique;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.gameplay.TendrilMode;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Kaneki's centipede legs hunt (3.1.0), replacing the original's autonomous attack ({@code tickCentipedeAutoAttack}, a
 * trampoline). The legs go after the nearest body that is hostile to him (it attacked him or is after him), only within what a
 * leg really reaches ({@link CentipedeMotion#reach}), with the leg on its side; one leg at a time, and it keeps its target.
 * Everything rides the original's synced centipede fields (technique, start, duration, limb, target), so every client animates
 * it ({@code CentipedeRenderer}):
 * <ul>
 *   <li>STRIKE: rear back (ticks 0-3), one arc, the hit lands on tick {@link #STRIKE_HIT}, settle back.</li>
 *   <li>GRAB: rear back with the jaw open (0-4), strike (4-7), the bite on tick {@link #BITE} catches it in the leg's held slot
 *   (the original's, so every release path the original has still lets it go), then it is hoisted over to beside him (to tick
 *   {@link #GRAB_TICKS}); CRUSH one to three times ({@link #CRUSH_TICKS} each, the squeeze on tick {@link #CRUSH_HIT}); then a
 *   SLAM (lifted, driven down into the ground) or a THROW (wound back, flung out), let go on tick {@link #FINISH_HIT}.</li>
 * </ul>
 * A caught body is placed every tick ({@link #hold}, the redirect of the original's restraint): straight to where the coil holds
 * it, never dragged through terrain, never left lagging.
 */
public final class CentipedeHunt {
    static final int STRIKE_TICKS = 11;
    static final int STRIKE_HIT = 5;
    static final int BITE = 7;
    static final int GRAB_TICKS = 13;
    static final int CRUSH_TICKS = 9;
    static final int CRUSH_HIT = 4;
    static final int FINISH_TICKS = 10;
    static final int FINISH_HIT = 4;
    static final float GRAB_CHANCE = 0.42F;
    /** The largest body a leg can wrap (blocks wide, tall). */
    static final double GRAB_WIDTH = 1.5;
    static final double GRAB_HEIGHT = 3.0;
    /** Where each caught body started its hoist (by the hunter). */
    private static final Map<UUID, Vec3> HOISTED_FROM = new ConcurrentHashMap<>();

    private CentipedeHunt() {
    }

    /** Trampoline target of {@code KaguneServerController.tickCentipedeAutoAttack}. */
    public static void tickCentipedeAutoAttack(ServerPlayer player, KaguneRuntime.State state, long now) {
        KaguneForm form = KaguneProfile.form(player);
        if (!form.hasCentipedeLegs()) {
            if (state.centipedeAttackActive()) {
                letGo(player, state);
                state.clearCentipedeAttack();
                KaguneNetwork.broadcast(player);
            }
            return;
        }
        if (KakujaPassive.vanished(player)) {
            // 6.0.0: vanished in the wind, the legs keep still (a hunt would give him away).
            if (state.centipedeAttackActive()) {
                letGo(player, state);
                state.clearCentipedeAttack();
                KaguneNetwork.broadcast(player);
            }
            return;
        }
        if (state.centipedeAttackActive()) {
            continueHunt(player, state, now);
        } else if (now >= state.nextCentipedeAttackAt && free(state.mode)) {
            startHunt(player, state, now);
        }
    }

    /** Not while the kagune is coming out, going in, changing shape, breaking a fall or slinging him. */
    static boolean free(TendrilMode mode) {
        return mode != TendrilMode.MANIFEST && mode != TendrilMode.DISMISS && mode != TendrilMode.RESHAPE && mode != TendrilMode.FALL_ARREST
            && mode != TendrilMode.SLINGSHOT_REACH && mode != TendrilMode.SLINGSHOT_HOLD;
    }

    static double length(ServerPlayer player) {
        return CentipedeMotion.length(KaguneProfile.mastery(player));
    }

    /** The nearest hostile body within a leg's reach, taken by the leg on its side. */
    static void startHunt(ServerPlayer player, KaguneRuntime.State state, long now) {
        double reach = CentipedeMotion.reach(length(player));
        AABB area = player.m_20191_().m_82400_(reach + 2.0);
        List<LivingEntity> threats = player.m_9236_().m_6443_(LivingEntity.class, area, entity -> KaguneServerController.isHostileTo(player, entity)
            && KaguneServerController.validSwipeTarget(player, entity) && !KaguneRuntime.isEntityHeld(entity.m_20148_())
            && inReach(player, entity, reach) && KaguneServerController.clearLineTo(player, entity));
        LivingEntity nearest = null;
        for (LivingEntity threat : threats) {
            if (nearest == null || threat.m_20280_(player) < nearest.m_20280_(player)) {
                nearest = threat;
            }
        }
        if (nearest == null) {
            state.nextCentipedeAttackAt = now + 7L;
            return;
        }
        int limb = legFor(player, state, nearest);
        if (limb < 0) {
            state.nextCentipedeAttackAt = now + 7L;
            return;
        }
        boolean grab = nearest.m_20205_() <= GRAB_WIDTH && nearest.m_20206_() <= GRAB_HEIGHT && player.m_217043_().m_188501_() < GRAB_CHANCE;
        if (grab) {
            state.beginCentipedeAttack(now, GRAB_TICKS, limb, nearest.m_19879_(), KaguneServerController.center(nearest), CentipedeTechnique.GRAB);
            state.centipedeCrushesRemaining = 1 + player.m_217043_().m_188503_(3);
        } else {
            state.beginCentipedeAttack(now, STRIKE_TICKS, limb, nearest.m_19879_(), KaguneServerController.center(nearest), CentipedeTechnique.STRIKE);
        }
        KaguneServerController.playSelf(player, HexKaguneSounds.CENTIPEDE_CLICK, 0.7F, KaguneServerController.vary(player, 0.85F, 0.08F));
        KaguneNetwork.broadcast(player);
    }

    /** A body's middle within {@code reach} of him (horizontally, counting its own half-width), and not far above or below. */
    static boolean inReach(ServerPlayer player, Entity entity, double reach) {
        Vec3 c = KaguneServerController.center(entity);
        double dx = c.f_82479_ - player.m_20185_();
        double dz = c.f_82481_ - player.m_20189_();
        double dy = c.f_82480_ - player.m_20186_();
        return Math.sqrt(dx * dx + dz * dz) <= reach + entity.m_20205_() * 0.5 && dy > -1.5 && dy < 4.0;
    }

    /** The free leg on the body's side (his right or left), or the other one; -1 if neither is free. */
    static int legFor(ServerPlayer player, KaguneRuntime.State state, Entity target) {
        KaguneForm form = KaguneProfile.form(player);
        int manifested = Math.min(KaguneProfile.manifestedLimbs(player), form.limbCount());
        double yaw = Math.toRadians(player.m_146908_());
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double side = (target.m_20185_() - player.m_20185_()) * rightX + (target.m_20189_() - player.m_20189_()) * rightZ;
        int manual = Math.floorMod(state.actionLimbIndex, Math.max(1, manifested));
        int same = -1;
        int other = -1;
        for (int limb = 0; limb < manifested; limb++) {
            if (!form.limb(limb).segmented() || state.heldEntityId(limb) != 0 || KaguneServerController.committed(state.mode) && limb == manual) {
                continue;
            }
            boolean right = form.limb(limb).fanSide() >= 0.0;
            if (right == side >= 0.0) {
                same = same < 0 ? limb : same;
            } else {
                other = other < 0 ? limb : other;
            }
        }
        return same >= 0 ? same : other;
    }

    /** One tick of the hunt under way. */
    static void continueHunt(ServerPlayer player, KaguneRuntime.State state, long now) {
        long elapsed = Math.max(0L, now - state.centipedeAttackStarted);
        int limb = state.centipedeAttackLimbIndex;
        Entity found = player.m_284548_().m_6815_(state.centipedeAttackTargetId);
        LivingEntity target = found instanceof LivingEntity living && living.m_6084_() ? living : null;
        CentipedeTechnique technique = state.centipedeTechnique;
        boolean caught = target != null && state.heldEntityId(limb) == target.m_19879_();
        boolean mustHold = technique == CentipedeTechnique.CRUSH || technique == CentipedeTechnique.SLAM && elapsed < FINISH_HIT
            || technique == CentipedeTechnique.THROW && elapsed < FINISH_HIT || technique == CentipedeTechnique.GRAB && elapsed > BITE;
        if (target == null || mustHold && !caught || target.m_20280_(player) > 24.0 * 24.0) {
            finish(player, state, now);
            return;
        }
        state.centipedeAttackTargetPosition = KaguneServerController.center(target);
        switch (technique) {
            case STRIKE -> strike(player, state, target, elapsed, now);
            case GRAB -> grab(player, state, target, elapsed, now);
            case CRUSH -> crush(player, state, target, elapsed, now);
            case SLAM, THROW -> finisher(player, state, target, elapsed, now, technique == CentipedeTechnique.SLAM);
            case BLOCK -> {
                if (elapsed >= state.centipedeAttackDuration) {
                    finish(player, state, now);
                }
            }
        }
    }

    static void strike(ServerPlayer player, KaguneRuntime.State state, LivingEntity target, long elapsed, long now) {
        if (!state.centipedeAttackDamageApplied && elapsed >= STRIKE_HIT) {
            state.centipedeAttackDamageApplied = true;
            // It dodged out of reach while the leg came over: the leg lands short, no hit.
            if (inReach(player, target, CentipedeMotion.reach(length(player)) + 0.6) && KaguneServerController.centipedeHit(player, target, 0.58F, true)) {
                Vec3 away = target.m_20182_().m_82546_(player.m_20182_());
                if (away.m_165925_() > 1.0E-4) {
                    target.m_147240_(0.38F, -away.f_82479_, -away.f_82481_);
                }
                KaguneServerController.playAt(player, target, HexKaguneSounds.FLESH_IMPACT, 0.8F, KaguneServerController.vary(player, 0.85F, 0.06F));
            }
        }
        if (elapsed >= state.centipedeAttackDuration) {
            finish(player, state, now);
        }
    }

    static void grab(ServerPlayer player, KaguneRuntime.State state, LivingEntity target, long elapsed, long now) {
        int limb = state.centipedeAttackLimbIndex;
        if (!state.centipedeAttackDamageApplied && elapsed >= BITE) {
            state.centipedeAttackDamageApplied = true;
            if (!inReach(player, target, CentipedeMotion.reach(length(player)) + 0.6) || KaguneRuntime.isEntityHeld(target.m_20148_())) {
                finish(player, state, now);
                return;
            }
            KaguneServerController.centipedeHit(player, target, 0.3F, false);
            KaguneServerController.playAt(player, target, HexKaguneSounds.KAGUNE_GRAB, 0.85F, KaguneServerController.vary(player, 0.88F, 0.05F));
            if (!target.m_6084_() || !KaguneServerController.captureAutonomousCentipedeTarget(player, state, target, limb, now)) {
                finish(player, state, now);
                return;
            }
            // Held until the hunt lets it go (the original's timed release would fling it mid-crush).
            state.autonomousCentipedeReleaseAt[limb] = Long.MAX_VALUE;
            HOISTED_FROM.put(player.m_20148_(), target.m_20182_());
            KaguneNetwork.broadcast(player);
        }
        if (elapsed >= GRAB_TICKS) {
            phase(player, state, target, now, CentipedeTechnique.CRUSH, CRUSH_TICKS);
        }
    }

    static void crush(ServerPlayer player, KaguneRuntime.State state, LivingEntity target, long elapsed, long now) {
        if (!state.centipedeAttackDamageApplied && elapsed >= CRUSH_HIT) {
            state.centipedeAttackDamageApplied = true;
            KaguneServerController.centipedeCrushHit(player, target, 0.42F, false);
            boolean last = state.centipedeCrushesRemaining <= 1;
            KaguneServerController.playAt(player, target, last ? HexKaguneSounds.BONE_CRUSH : HexKaguneSounds.BONE_CRACK, 0.9F,
                KaguneServerController.vary(player, last ? 0.95F : 0.9F, last ? 0.05F : 0.08F));
        }
        if (elapsed >= state.centipedeAttackDuration) {
            state.centipedeCrushesRemaining--;
            if (state.centipedeCrushesRemaining > 0) {
                phase(player, state, target, now, CentipedeTechnique.CRUSH, CRUSH_TICKS);
            } else {
                phase(player, state, target, now, player.m_217043_().m_188499_() ? CentipedeTechnique.SLAM : CentipedeTechnique.THROW, FINISH_TICKS);
            }
        }
    }

    static void finisher(ServerPlayer player, KaguneRuntime.State state, LivingEntity target, long elapsed, long now, boolean slam) {
        if (!state.centipedeAttackDamageApplied && elapsed >= FINISH_HIT) {
            state.centipedeAttackDamageApplied = true;
            state.centipedeHoldingTarget = false;
            letGo(player, state);
            if (slam) {
                KaguneServerController.centipedeHit(player, target, 0.62F, false);
                target.m_20334_(0.0, -2.15, 0.0);
                KaguneServerController.playAt(player, target, HexKaguneSounds.KAGUNE_SLAM, 0.95F, KaguneServerController.vary(player, 0.92F, 0.04F));
            } else {
                KaguneServerController.centipedeHit(player, target, 0.34F, false);
                Vec3 outward = target.m_20182_().m_82546_(player.m_20182_());
                Vec3 flat = new Vec3(outward.f_82479_, 0.0, outward.f_82481_);
                if (flat.m_82556_() < 1.0E-4) {
                    double yaw = Math.toRadians(player.m_146908_());
                    flat = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
                }
                target.m_20256_(flat.m_82541_().m_82490_(2.75).m_82520_(0.0, 0.62, 0.0));
                KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.85F, KaguneServerController.vary(player, 0.75F, 0.05F));
            }
            target.f_19812_ = true;
            target.f_19789_ = 0.0F;
            KaguneNetwork.broadcast(player);
        }
        if (elapsed >= state.centipedeAttackDuration) {
            finish(player, state, now);
        }
    }

    /** The next phase of the grab on the same leg and body. */
    static void phase(ServerPlayer player, KaguneRuntime.State state, LivingEntity target, long now, CentipedeTechnique technique, int duration) {
        state.beginCentipedeAttack(now, duration, state.centipedeAttackLimbIndex, target.m_19879_(), KaguneServerController.center(target), technique);
        state.centipedeHoldingTarget = true;
        KaguneNetwork.broadcast(player);
    }

    /** The hunt is over: anything still held is let go, and the legs rest a moment. */
    static void finish(ServerPlayer player, KaguneRuntime.State state, long now) {
        letGo(player, state);
        state.clearCentipedeAttack();
        state.nextCentipedeAttackAt = now + 13L + player.m_217043_().m_188503_(9);
        KaguneNetwork.broadcast(player);
    }

    /** Let go of whatever this hunt's leg holds (the original's release: AI, gravity and motion as they were). */
    static void letGo(ServerPlayer player, KaguneRuntime.State state) {
        int limb = state.centipedeAttackLimbIndex;
        if (limb >= 0 && limb < state.heldEntityIds.length && state.heldEntityIds[limb] != 0 && state.autonomousCentipedeHeld[limb]) {
            KaguneServerController.releaseHeldSlot(player.m_284548_(), state, limb);
        }
        HOISTED_FROM.remove(player.m_20148_());
    }

    /**
     * Redirect target of the original's restraint of a body held by a centipede leg ({@code restrainCentipedeTarget} in
     * {@code tickHeldEntities}): put it where the hunt holds it this tick, straight there (no dragging through terrain, no lag):
     * hoisted from where it was bitten to beside him, held there through the crushes, lifted over before a slam, wound back
     * before a throw. If that spot is inside blocks it tries just above, else stays where it is.
     */
    public static void hold(ServerPlayer player, LivingEntity target, int slot, long now) {
        KaguneRuntime.State state = KaguneRuntime.get(player.m_20148_());
        if (!state.centipedeAttackActive() || state.centipedeAttackLimbIndex != slot || state.centipedeAttackTargetId != target.m_19879_()) {
            // Held by a hunt that was cut short (a manual move took the leg): let it go rather than leave it hanging.
            KaguneServerController.releaseHeldSlot(player.m_284548_(), state, slot);
            KaguneNetwork.broadcast(player);
            return;
        }
        place(player, target, where(player, state, target, slot, now));
    }

    /** Where the held body's feet go this tick. */
    static Vec3 where(ServerPlayer player, KaguneRuntime.State state, LivingEntity target, int slot, long now) {
        KaguneForm form = KaguneProfile.form(player);
        int side = form.limb(slot).fanSide() >= 0.0 ? 1 : -1;
        double length = length(player);
        double[] rig = CentipedeMotion.holdPoint(side, length);
        long elapsed = Math.max(0L, now - state.centipedeAttackStarted);
        CentipedeTechnique technique = state.centipedeTechnique;
        if (technique == CentipedeTechnique.SLAM) {
            double up = smooth(elapsed / (double)FINISH_HIT);
            rig = new double[] {rig[0] * (1.0 - 0.4 * up), rig[1] + 1.6 * up, rig[2] + 0.6 * up};
        } else if (technique == CentipedeTechnique.THROW) {
            double back = smooth(elapsed / (double)FINISH_HIT);
            rig = new double[] {rig[0] + side * 0.6 * back, rig[1] + 0.5 * back, rig[2] - 0.5 * back};
        }
        double yaw = Math.toRadians(player.m_146908_());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        Vec3 middle = new Vec3(player.m_20185_() + rightX * rig[0] + forwardX * rig[2], player.m_20186_() + rig[1],
            player.m_20189_() + rightZ * rig[0] + forwardZ * rig[2]);
        Vec3 feet = middle.m_82520_(0.0, -target.m_20206_() * 0.52, 0.0);
        if (technique == CentipedeTechnique.GRAB) {
            Vec3 from = HOISTED_FROM.get(player.m_20148_());
            if (from != null) {
                double hoist = smooth((elapsed - BITE) / (double)(GRAB_TICKS - BITE));
                feet = from.m_82549_(feet.m_82546_(from).m_82490_(hoist));
            }
        }
        return feet;
    }

    /** Set the body at {@code feet} if it fits there (or just above), its fall and swell stopped; a held player is told. */
    static void place(ServerPlayer player, LivingEntity target, Vec3 feet) {
        ServerLevel level = player.m_284548_();
        Vec3 at = target.m_20182_();
        Vec3 chosen = null;
        for (double lift : new double[] {0.0, 0.6, 1.2}) {
            Vec3 candidate = feet.m_82520_(0.0, lift, 0.0);
            Vec3 offset = candidate.m_82546_(at);
            if (level.m_45756_(target, target.m_20191_().m_82386_(offset.f_82479_, offset.f_82480_, offset.f_82481_))) {
                chosen = candidate;
                break;
            }
        }
        if (chosen != null && chosen.m_82557_(at) > 1.0E-6) {
            target.m_6021_(chosen.f_82479_, chosen.f_82480_, chosen.f_82481_);
        }
        target.m_20256_(Vec3.f_82478_);
        target.f_19789_ = 0.0F;
        if (target instanceof Creeper creeper) {
            creeper.m_32283_(-1);
        }
        KaguneServerController.syncKaguneHeldPlayer(target);
    }

    /**
     * Trampoline target of {@code KaguneServerController.tryCentipedeBlock}: a leg busy holding a body can't guard (the blow
     * lands); otherwise the original's guard.
     */
    public static boolean tryCentipedeBlock(ServerPlayer player, Entity attacker) {
        KaguneRuntime.State state = KaguneRuntime.peek(player.m_20148_());
        if (state != null && state.centipedeAttackActive() && state.centipedeTechnique != CentipedeTechnique.STRIKE
            && state.centipedeTechnique != CentipedeTechnique.BLOCK) {
            return false;
        }
        return KaguneServerController.tryCentipedeBlockLegacy(player, attacker);
    }

    static double smooth(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * (3.0 - 2.0 * t);
    }
}

package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.ControlMath;
import com.hex.kagune.control.UrgeRules;
import com.hex.kagune.control.UrgeType;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.gameplay.TendrilMode;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Involuntary kagune instincts. The server alone decides when an urge happens, what it is and at whom;
 * pushes use the same velocity packets as the original slingshot, and forced strikes go through the
 * normal authoritative combo path. Urges are short, rate-limited, never target players or villagers
 * with attacks, and disappear as control approaches mastery.
 */
final class KaguneInstinct {
    private static final double SCAN_RADIUS = 10.0;

    private KaguneInstinct() {
    }

    private record Prey(LivingEntity entity, UrgeRules.PreyKind kind, double distance) {
    }

    static void tick(ServerPlayer player, KaguneRuntime.State runtime, ServerControlState state, long now) {
        if (state.urge != UrgeType.NONE) {
            continueUrge(player, runtime, state, now);
            return;
        }
        if (!ControlConfig.involuntaryActions() || now < state.nextUrgeCheckAt) {
            return;
        }
        state.nextUrgeCheckAt = now + 10L;
        if (now < state.urgeCooldownUntil || !eligible(player, runtime)) {
            return;
        }
        float instinct = ControlMath.instinct(state.control, state.hunger, state.frenzy);
        if (instinct <= ControlMath.URGE_THRESHOLD) {
            return;
        }
        Prey prey = findPrey(player, state);
        if (prey == null) {
            return;
        }
        float chance = ControlMath.urgeChance(instinct, UrgeRules.contextWeight(prey.kind(), state.hunger, state.frenzy))
            * ControlConfig.instinctStrength();
        if (player.m_217043_().m_188501_() >= chance) {
            return;
        }
        boolean canStrike = ControlConfig.involuntaryAttacks() && prey.kind() != UrgeRules.PreyKind.SCENT
            && canStrikeNow(player, runtime, prey, now);
        UrgeType type = UrgeRules.choose(instinct, prey.kind(), player.m_20096_(), prey.distance(), canStrike, player.m_217043_().m_188501_());
        begin(player, state, type, prey.entity(), instinct, now);
    }

    /** Operator test hook: start {@code type} at the nearest prey regardless of control. Returns the prey or null. */
    static LivingEntity forceUrge(ServerPlayer player, UrgeType type) {
        KaguneRuntime.State runtime = KaguneRuntime.peek(player.m_20148_());
        if (runtime == null || !KaguneProfile.isActive(player) || !basicEligible(player)) {
            return null;
        }
        ServerControlState state = ControlServer.state(player);
        long now = player.m_9236_().m_46467_();
        float savedHunger = state.hunger;
        float savedFrenzy = state.frenzy;
        state.hunger = 1.0F;
        state.frenzy = Math.max(state.frenzy, 1.0F);
        Prey prey = findPrey(player, state);
        state.hunger = savedHunger;
        state.frenzy = savedFrenzy;
        if (prey == null || type == UrgeType.STRIKE && (prey.kind() == UrgeRules.PreyKind.SCENT || !canStrikeNow(player, runtime, prey, now))) {
            return null;
        }
        if (state.urge != UrgeType.NONE) {
            end(player, state, now, false);
        }
        begin(player, state, type, prey.entity(), Math.max(0.8F, ControlMath.instinct(state.control, state.hunger, state.frenzy)), now);
        return prey.entity();
    }

    private static boolean eligible(ServerPlayer player, KaguneRuntime.State runtime) {
        if (!basicEligible(player) || runtime.guarding || runtime.hasManualHeldEntities()) {
            return false;
        }
        return runtime.mode == TendrilMode.IDLE || runtime.mode == TendrilMode.RETRACT || runtime.mode == TendrilMode.SWIPE;
    }

    private static boolean basicEligible(ServerPlayer player) {
        return player.m_6084_() && !player.m_7500_() && !player.m_5833_() && !player.m_20159_() && !player.m_21255_() && !player.m_5803_();
    }

    private static boolean canStrikeNow(ServerPlayer player, KaguneRuntime.State runtime, Prey prey, long now) {
        return !KaguneServerController.committed(runtime.mode)
            && now >= runtime.inputCooldownUntil
            && prey.distance() <= KaguneProfile.attackRange(player)
            && KaguneProfile.energy(player) >= 3.0F;
    }

    private static Prey findPrey(ServerPlayer player, ServerControlState state) {
        AABB area = player.m_20191_().m_82400_(SCAN_RADIUS);
        List<LivingEntity> nearby = player.m_284548_().m_6443_(LivingEntity.class, area,
            entity -> entity != player && entity.m_6084_() && !entity.m_5833_());
        Prey best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity entity : nearby) {
            double distanceSqr = entity.m_20280_(player);
            if (distanceSqr > SCAN_RADIUS * SCAN_RADIUS) {
                continue;
            }
            UrgeRules.PreyKind kind = classify(player, entity, distanceSqr);
            if (kind == null || UrgeRules.contextWeight(kind, state.hunger, state.frenzy) <= 0.0F) {
                continue;
            }
            double score = kind.ordinal() * 1000.0 + distanceSqr;
            if (score < bestScore && KaguneServerController.clearLineTo(player, entity)) {
                bestScore = score;
                best = new Prey(entity, kind, Math.sqrt(distanceSqr));
            }
        }
        return best;
    }

    private static UrgeRules.PreyKind classify(ServerPlayer player, LivingEntity entity, double distanceSqr) {
        if (entity instanceof Player other) {
            // Other players are only ever a scent: a glance or a step, never a forced attack.
            return other.m_7500_() || other.m_5833_() || player.m_7307_(other) ? null : UrgeRules.PreyKind.SCENT;
        }
        if (entity instanceof AbstractVillager) {
            return UrgeRules.PreyKind.SCENT;
        }
        if (!KaguneServerController.validSwipeTarget(player, entity)) {
            return null;
        }
        if (KaguneServerController.isHostileTo(player, entity) || entity instanceof Enemy && distanceSqr <= 64.0) {
            return UrgeRules.PreyKind.THREAT;
        }
        if (entity instanceof Animal) {
            boolean someonesPet = entity instanceof TamableAnimal tamable && tamable.m_21824_();
            return someonesPet || entity.m_8077_() ? null : UrgeRules.PreyKind.HUNT;
        }
        return null;
    }

    private static void begin(ServerPlayer player, ServerControlState state, UrgeType type, LivingEntity target, float instinct, long now) {
        state.clearUrge();
        state.urge = type;
        state.urgeStarted = now;
        state.urgeDuration = UrgeRules.durationTicks(type, instinct);
        state.urgeTargetId = target.m_19879_();
        state.urgeStrength = UrgeRules.strength(instinct);
        state.urgeInstinct = instinct;
        if (type == UrgeType.STRIKE) {
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.55F,
                KaguneServerController.vary(player, KaguneServerController.formPitch(player) * 1.05F, 0.05F));
        } else if (type != UrgeType.GLANCE) {
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 0.4F,
                KaguneServerController.vary(player, KaguneServerController.formPitch(player) * 1.15F, 0.06F));
        }
        if (type.restrainable() && state.urgeHintsShown < 3) {
            state.urgeHintsShown++;
            ControlServer.tell(player, "message.hexkagune.urge_hint");
        }
        ControlServer.broadcast(player, state);
    }

    private static void continueUrge(ServerPlayer player, KaguneRuntime.State runtime, ServerControlState state, long now) {
        long elapsed = now - state.urgeStarted;
        LivingEntity target = player.m_284548_().m_6815_(state.urgeTargetId) instanceof LivingEntity living && living.m_6084_()
            && living.m_20280_(player) <= 256.0 ? living : null;
        if (target == null || !basicEligible(player) || elapsed < 0L) {
            end(player, state, now, true);
            return;
        }
        if (state.urge.restrainable() && !state.urgeRestrained && !state.urgeRestrainAttempted && !state.urgeActed && player.m_6144_()) {
            state.urgeRestrainAttempted = true;
            if (player.m_217043_().m_188501_() < ControlMath.restrainChance(state.control, state.hunger, state.frenzy)) {
                state.urgeRestrained = true;
                state.urgeDuration = (int)Math.min(Integer.MAX_VALUE, elapsed + 6L);
                ControlServer.tell(player, "message.hexkagune.urge_restrained");
                QuestServer.signal(player, com.hex.kagune.control.quest.Signal.RESTRAIN, 1); // 7.0.1
                if (now >= state.nextRestraintAwardAt) {
                    state.nextRestraintAwardAt = now + 200L;
                    ControlServer.awardRestraint(player, state, state.urge.severity() >= 3 ? 2 : 1);
                }
                state.frenzy = Math.max(0.0F, state.frenzy - 0.08F);
            } else {
                ControlServer.tell(player, "message.hexkagune.urge_overpowers");
            }
            state.dirty = true;
        }
        if (!state.urgeRestrained && !state.urgeActed) {
            if ((state.urge == UrgeType.STEP || state.urge == UrgeType.LUNGE) && elapsed >= 2L) {
                state.urgeActed = true;
                push(player, target, state);
            } else if (state.urge == UrgeType.STRIKE && elapsed >= state.urgeDuration) {
                state.urgeActed = true;
                if (KaguneServerController.validSwipeTarget(player, target) && !(target instanceof Player) && !(target instanceof AbstractVillager)
                    && KaguneCombat.startInvoluntary(player, target)) {
                    ControlServer.tell(player, "message.hexkagune.kagune_lashes_out");
                }
            }
        }
        long hold = state.urge == UrgeType.STRIKE && !state.urgeRestrained ? 4L : 0L;
        if (elapsed >= state.urgeDuration + hold) {
            end(player, state, now, true);
        }
    }

    private static void push(ServerPlayer player, LivingEntity target, ServerControlState state) {
        if (!player.m_20096_() || player.m_20072_() || player.m_20159_() || player.m_21255_() || player.m_6147_() || player.m_6144_()) {
            return;
        }
        Vec3 toward = target.m_20182_().m_82546_(player.m_20182_());
        double horizontal = Math.sqrt(toward.f_82479_ * toward.f_82479_ + toward.f_82481_ * toward.f_82481_);
        if (horizontal < 1.8) {
            return;
        }
        double speed = UrgeRules.pushSpeed(state.urge, state.urgeInstinct);
        Vec3 velocity = player.m_20184_();
        double lift = state.urge == UrgeType.LUNGE ? Math.max(velocity.f_82480_, 0.2) : velocity.f_82480_;
        player.m_20256_(new Vec3(toward.f_82479_ / horizontal * speed + velocity.f_82479_ * 0.25, lift,
            toward.f_82481_ / horizontal * speed + velocity.f_82481_ * 0.25));
        player.f_19864_ = true;
        player.f_8906_.m_9829_(new ClientboundSetEntityMotionPacket(player));
    }

    static void end(ServerPlayer player, ServerControlState state, long now, boolean cooldown) {
        if (cooldown) {
            state.urgeCooldownUntil = now + ControlMath.urgeCooldownTicks(state.control, player.m_217043_().m_188501_());
        }
        state.clearUrge();
        state.dirty = true;
    }
}

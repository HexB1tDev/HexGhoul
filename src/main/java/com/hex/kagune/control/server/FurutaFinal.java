package com.hex.kagune.control.server;

import com.hex.kagune.control.furuta.FurutaMove;
import com.hex.kagune.control.furuta.FurutaRules;
import com.hex.kagune.control.furuta.FurutaStage;
import com.hex.kagune.control.network.S2CEtoFx;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.effect.KaguneEffects;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 7.0.2, the user's rework: the final kakuja's four on the server, beyond the beats {@link FurutaCombat} runs. King's Checkmate:
 * once the second colossus has hooked it from behind, he charges in on it for his combination. Centipede Hunt: hurled after it,
 * steered at it every tick, until it is near enough to catch; caught, it is dragged along the ground at the colossus's side as he
 * runs on, bleeding, then thrown. Monstrous Recovery (a job): his body knits fast, paid in RC as it comes, the coils taking half of
 * every blow; he can move, but strike nothing until it is over (the key again stops it). The Final Performance (a job): the colossi
 * go at whatever he looks at (looked for again every few ticks), his claws as fast as he clicks; the key again, or its time running
 * out, ends it: one colossus catches the prey and hauls it to him, the other runs it through, his last blow throws it up.
 */
final class FurutaFinal {
    private FurutaFinal() {
    }

    private static LivingEntity alive(ServerPlayer player, int id) {
        return id != 0 && player.m_284548_().m_6815_(id) instanceof LivingEntity living && living.m_6084_() ? living : null;
    }

    private static float yawTo(ServerPlayer player, Vec3 at) {
        Vec3 to = at.m_82546_(player.m_20182_());
        return (float)Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_));
    }

    static void tick(ServerPlayer player, FurutaServer.FurutaState state, FurutaMove move, long age, long now) {
        switch (move) {
            case CHECKMATE -> checkmate(player, state, age);
            case HUNT -> hunt(player, state, age, now);
            case HUNT_DRAG -> drag(player, state, age, now);
            case PERF_FINISH -> {
                LivingEntity held = alive(player, state.heldId);
                if (held != null) {
                    state.yaw = yawTo(player, held.m_20182_());
                }
                if (age == 19 && state.heldId != 0) {
                    FurutaCombat.release(player, state, true); // the last blow has thrown it up: let it fly
                    FurutaServer.broadcast(player, state);
                }
            }
            case PERF_CLAW -> {
                LivingEntity prey = alive(player, state.targets[0]);
                if (age == 0 && prey != null && prey.m_20280_(player) > 2.5 * 2.5) {
                    Vec3 way = AyatoFire.horizontal(prey.m_20182_().m_82546_(player.m_20182_()));
                    AyatoFire.push(player, way.m_82490_(0.6).m_82520_(0.0, 0.05, 0.0));
                }
                if (prey != null) {
                    state.yaw = yawTo(player, prey.m_20182_());
                }
            }
            default -> {
            }
        }
    }

    // ---------------------------------------------------------------- King's Checkmate

    /** Once it is hooked from behind, he charges in on it; through the combination he faces it (his claws and jaws are aimed so). */
    private static void checkmate(ServerPlayer player, FurutaServer.FurutaState state, long age) {
        LivingEntity prey = alive(player, state.heldId != 0 ? state.heldId : state.targets[0]);
        if (prey == null || age < FurutaRules.CHECKMATE_CHARGE_FROM) {
            return;
        }
        state.yaw = yawTo(player, prey.m_20182_());
        if (age > FurutaRules.CHECKMATE_CHARGE_TO) {
            return;
        }
        Vec3 gap = new Vec3(prey.m_20185_() - player.m_20185_(), 0.0, prey.m_20189_() - player.m_20189_());
        double distance = gap.m_82553_();
        double close = FurutaRules.CHECKMATE_CLOSE + prey.m_20205_() * 0.5;
        if (distance > close) {
            double speed = Math.min(1.6, (distance - close) * 0.7);
            AyatoFire.push(player, gap.m_82490_(speed / distance).m_82520_(0.0, age == FurutaRules.CHECKMATE_CHARGE_FROM ? 0.12
                : player.m_20184_().f_82480_, 0.0));
            state.fallSafe = true;
        }
    }

    // ---------------------------------------------------------------- Centipede Hunt

    /** The pursuit: hurled at it, steered at it every tick; near enough, it is caught and the drag begins. */
    private static void hunt(ServerPlayer player, FurutaServer.FurutaState state, long age, long now) {
        state.fallSafe = true;
        LivingEntity prey = alive(player, state.targets[0]);
        if (prey == null) {
            if (age >= 1 && age <= 20) {
                AyatoFire.push(player, AyatoFire.horizontal(player.m_20154_()).m_82490_(FurutaRules.HUNT_SPEED * 0.8)
                    .m_82520_(0.0, player.m_20184_().f_82480_, 0.0));
            }
            return;
        }
        Vec3 gap = new Vec3(prey.m_20185_() - player.m_20185_(), 0.0, prey.m_20189_() - player.m_20189_());
        double distance = gap.m_82553_();
        state.yaw = yawTo(player, prey.m_20182_());
        if (age >= 3 && distance <= FurutaRules.HUNT_CATCH + prey.m_20205_() * 0.5) {
            FurutaCombat.strikeBody(player, state, prey, 0.5F, Vec3.f_82478_, FurutaMove.BLEED | FurutaMove.STUN, now, WoundBook.Blow.CRUSH);
            EtoBlitz.fx(player, S2CEtoFx.CRUSHED, prey.m_19879_(), KaguneServerController.center(prey), Vec3.f_82478_, prey.m_20206_(), 0);
            KaguneServerController.playAt(player, prey, HexKaguneSounds.BONE_CRUSH, 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
            FurutaCombat.release(player, state, false);
            state.heldId = prey.m_19879_();
            FurutaCombat.begin(player, state, FurutaMove.HUNT_DRAG, now, state.yaw, KaguneServerController.center(prey));
            return;
        }
        if (age >= 1) {
            double speed = Math.min(FurutaRules.HUNT_SPEED, 0.4 + Math.max(0.0, distance - 2.0) * 0.7);
            double lift = player.m_20096_() ? 0.12 : player.m_20184_().f_82480_;
            AyatoFire.push(player, gap.m_82490_(speed / Math.max(1.0E-6, distance)).m_82520_(0.0, lift, 0.0));
        }
    }

    /** The drag: he runs on along his look, it dragged low at the biting colossus's side, chewed as it goes; then thrown. */
    private static void drag(ServerPlayer player, FurutaServer.FurutaState state, long age, long now) {
        state.fallSafe = true;
        LivingEntity held = alive(player, state.heldId);
        if (held == null) {
            return;
        }
        Vec3 look = AyatoFire.horizontal(player.m_20154_());
        if (age < FurutaRules.HUNT_THROW) {
            AyatoFire.push(player, look.m_82490_(FurutaRules.HUNT_DRAG_SPEED).m_82520_(0.0, player.m_20096_() ? 0.0 : player.m_20184_().f_82480_, 0.0));
            if (!EtoStun.stun(held, now, 6)) {
                FurutaCombat.release(player, state, false); // 7.0.2: a player held too long breaks free
                FurutaServer.broadcast(player, state);
                return;
            }
            Vec3 where = FurutaCombat.around(player, new double[] {2.4, 0.45, 0.8});
            FurutaCombat.carry(held, where.m_82520_(0.0, -held.m_20206_() * 0.5, 0.0));
            if (age > 0 && age % 4 == 0) {
                FurutaCombat.strikeBody(player, state, held, 0.25F, Vec3.f_82478_, FurutaMove.BLEED | FurutaMove.STUN, now, WoundBook.Blow.CRUSH);
                FurutaCombat.ground(player, held.m_20182_(), 1.0F);
            }
            return;
        }
        if (age == FurutaRules.HUNT_THROW) {
            FurutaCombat.strikeBody(player, state, held, 0.8F, Vec3.f_82478_, FurutaMove.STUN | FurutaMove.HEAVY | FurutaMove.BLEED, now, WoundBook.Blow.SLAM);
            FurutaCombat.release(player, state, true);
            BlockSmash.launch(player, held, look.m_82490_(FurutaRules.HUNT_THROW_SPEED).m_82520_(0.0, 0.5, 0.0), FurutaCombat.THROWN);
            FurutaServer.broadcast(player, state);
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
        }
    }

    // ---------------------------------------------------------------- the jobs

    /** The kakuja's jobs this tick: the recovery's healing, the performance's colossi. */
    static void job(ServerPlayer player, FurutaServer.FurutaState state, long age, long now) {
        if (state.job == FurutaMove.RECOVERY) {
            recover(player, state, age);
        } else if (state.job == FurutaMove.PERFORMANCE) {
            perform(player, state, age, now);
        }
    }

    /** His body knitting: a share of his health over the job, each bit paid in RC as it comes (out of RC, it stops). */
    private static void recover(ServerPlayer player, FurutaServer.FurutaState state, long age) {
        if (age == 0) {
            player.m_21195_(KaguneEffects.BLEEDING.get());
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        }
        float amount = player.m_21233_() * FurutaRules.RECOVERY_SHARE / FurutaMove.RECOVERY.ticks();
        if (player.m_21223_() < player.m_21233_()) {
            if (!KaguneProfile.consumeEnergy(player, amount * FurutaRules.RECOVERY_COST)) {
                FurutaCentipedes.endJob(player, state, false, true);
                FurutaServer.notice(player, state, Component.m_237115_("message.hexkagune.furuta.recovery_spent"));
                return;
            }
            player.m_5634_(amount);
        }
        if (age % 10 == 0) {
            KaguneNetwork.sendTo(player, player);
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.6F, KaguneServerController.vary(player, 0.8F, 0.1F));
        }
    }

    /** The share of a blow he takes while the recovery's coils are round him (1 otherwise). */
    static float taken(FurutaServer.FurutaState state) {
        return state.job == FurutaMove.RECOVERY ? FurutaRules.RECOVERY_TAKEN : 1.0F;
    }

    /** The performance: the colossi's prey looked for again where he looks every few ticks; each colossus blow on its tick; at its end, the finish. */
    private static void perform(ServerPlayer player, FurutaServer.FurutaState state, long age, long now) {
        if (age % FurutaRules.PERFORMANCE_RETARGET == 0) {
            LivingEntity seen = FurutaCombat.aim(player, null, FurutaRules.PERFORMANCE_RANGE);
            if (seen != null && seen.m_19879_() != state.jobHeld) {
                state.jobHeld = seen.m_19879_(); // redirected: the colossi turn on what he looks at
                FurutaServer.broadcast(player, state);
            }
        }
        LivingEntity prey = alive(player, state.jobHeld);
        if (prey != null && prey.m_20280_(player) <= (FurutaRules.PERFORMANCE_RANGE + 2.0) * (FurutaRules.PERFORMANCE_RANGE + 2.0)
            && KaguneServerController.clearLineTo(player, prey)) {
            for (FurutaMove.Beat beat : FurutaMove.PERFORMANCE.beats(2)) {
                if (age != beat.land()) {
                    continue;
                }
                Vec3 into = AyatoFire.horizontal(KaguneServerController.center(prey).m_82546_(player.m_20182_()));
                FurutaCombat.strikeBody(player, state, prey, beat.power(), into.m_82490_(0.2), beat.flags(), now, WoundBook.Blow.STAB);
                EtoBlitz.fx(player, S2CEtoFx.IMPALED, prey.m_19879_(), KaguneServerController.center(prey), into, prey.m_20206_(), 0);
            }
        }
        if (age >= FurutaMove.PERFORMANCE.ticks()) {
            finish(player, state, now);
        }
    }

    /** The performance's end: one colossus catches the prey and hauls it to him, the other through it, his last blow. */
    private static void finish(ServerPlayer player, FurutaServer.FurutaState state, long now) {
        LivingEntity prey = alive(player, state.jobHeld);
        if (prey == null) {
            prey = FurutaCombat.aim(player, null, FurutaRules.PERFORMANCE_RANGE);
        }
        FurutaCentipedes.endJob(player, state, false, false);
        FurutaCombat.release(player, state, false);
        java.util.Arrays.fill(state.targets, 0);
        Vec3 point = prey != null ? KaguneServerController.center(prey) : FurutaCombat.lookPoint(player, 10.0);
        if (prey != null) {
            state.targets[0] = prey.m_19879_();
        }
        FurutaCombat.begin(player, state, FurutaMove.PERF_FINISH, now, yawTo(player, point), point);
        KaguneServerController.playSelf(player, HexKaguneSounds.MANIFEST_RIP, 0.9F, KaguneServerController.vary(player, 0.7F, 0.05F));
    }

    // ---------------------------------------------------------------- the keys

    /** A key in the kakuja's jobs: in the recovery, only its own (it stops it); in the performance, its own ends it. True if handled. */
    static boolean key(ServerPlayer player, FurutaServer.FurutaState state, FurutaMove.Slot slot, long now) {
        if (FurutaServer.stage(player) != FurutaStage.KAKUJA) {
            return false;
        }
        if (state.job == FurutaMove.RECOVERY) {
            if (slot == FurutaMove.Slot.V && now - state.jobAt >= 4) {
                FurutaCentipedes.endJob(player, state, false, true);
            } else if (slot != FurutaMove.Slot.V) {
                FurutaServer.notice(player, state, Component.m_237115_("message.hexkagune.furuta.recovering"));
            }
            return true;
        }
        if (state.job == FurutaMove.PERFORMANCE && slot == FurutaMove.Slot.B && now - state.jobAt >= 6) {
            finish(player, state, now);
            return true;
        }
        return false;
    }

    /** M1 in the kakuja's jobs: refused in the recovery; in the performance, his claws as fast as he clicks. True if handled. */
    static boolean strike(ServerPlayer player, FurutaServer.FurutaState state, long now) {
        if (!state.kakuja) {
            return false;
        }
        if (state.job == FurutaMove.RECOVERY) {
            FurutaServer.notice(player, state, Component.m_237115_("message.hexkagune.furuta.recovering"));
            return true;
        }
        if (state.job != FurutaMove.PERFORMANCE) {
            return false;
        }
        if (state.move == FurutaMove.PERF_CLAW && now - state.startedAt < FurutaRules.PERF_CLAW_EVERY || state.move == FurutaMove.PERF_FINISH) {
            return true; // the claw before it still raking
        }
        LivingEntity prey = alive(player, state.jobHeld);
        FurutaCombat.release(player, state, false);
        java.util.Arrays.fill(state.targets, 0);
        Vec3 point = prey != null ? KaguneServerController.center(prey) : FurutaCombat.lookPoint(player, 3.4);
        if (prey != null) {
            state.targets[0] = prey.m_19879_();
        }
        FurutaCombat.begin(player, state, FurutaMove.PERF_CLAW, now, yawTo(player, point), point);
        return true;
    }
}

package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.PaleStrike;
import com.hex.kagune.control.pale.WitnessRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.4: Eto's fight, her Owl DNA kit: the very moves a Pale player throws, on the same ticks, out of the same rules
 * ({@link PaleRules}, {@link PaleStrike}, {@link WitnessRules}), drawn by every client with the same animator as a player's
 * ({@code PaleAnimator}, from {@code S2CEto}'s move) and the same impacts ({@code S2CPaleFx}). No Ukaku of any kind.
 * <ul>
 *   <li>The Speed Blitz: the eight strikes chained one into the next (the jab, the cross, the hook, the liver shot, the uppercut, the
 *       palm from its side after a blink, the spinning backfist, the twin palm finisher after a blink to its front), stepping in from
 *       just out of reach.</li>
 *   <li>The Big Punch: she sinks under the wind-up, lets it go, blinks in front of it and drives it through; at the full charge,
 *       landed, Witness This: behind it, the uppercut, the storm of blows all over the sky, the stomp down into a crater.</li>
 *   <li>The Speed Kick: the lunge, gone, there, the leg driven through its middle and it launched far away, tumbling; the rebound
 *       over backwards.</li>
 *   <li>Her flips: in over a gap, back clear.</li>
 * </ul>
 * What she hits is what she fights and anything hostile caught round it; never a player she isn't fighting, never her partner.
 */
final class EtoOwl {
    /** Her move: what, its detail, when it began, at what, its charge, the way she faced, its point; her cooldowns and the blitz's chain. */
    static final class Fight {
        PaleMove move = PaleMove.NONE;
        int detail;
        long at = Long.MIN_VALUE;
        int target;
        float charge;
        float yaw;
        Vec3 point = Vec3.f_82478_;
        /** Where she was when the kick began (it is laid out from there). */
        Vec3 from = Vec3.f_82478_;
        int hold;
        int strike;
        long strikeEnded = Long.MIN_VALUE;
        long punchReady = Long.MIN_VALUE;
        long kickReady = Long.MIN_VALUE;
        long flipReady = Long.MIN_VALUE;
        /** 1.3.6: her flesh moves and Watch This, when each is back. */
        long swipeReady = Long.MIN_VALUE;
        long grabReady = Long.MIN_VALUE;
        long hammerReady = Long.MIN_VALUE;
        long watchReady = Long.MIN_VALUE;
        /** 1.3.7: her Mutated Arm, when it is back. */
        long armReady = Long.MIN_VALUE;
        /** 1.3.8: the Owl: whether she is in it, since when, when it comes back; its Rend's last cut and end; its moves' cooldowns. */
        boolean owl;
        long owlAt = Long.MIN_VALUE;
        long owlReady = Long.MIN_VALUE;
        int rend = -1;
        long rendEndsAt = Long.MIN_VALUE;
        long whirlReady = Long.MIN_VALUE;
        long leapReady = Long.MIN_VALUE;
        long landedAt = Long.MIN_VALUE;
        /** 1.3.6: what the swipe has cut this time (entity ids), what the flesh holds (0 none), where the throw began (her frame). */
        final java.util.Set<Integer> hit = new java.util.HashSet<>();
        int heldId;
        double[] thrownFrom = new double[3];
        Vec3 flipPush;
        boolean dirty = true;

        boolean busy() {
            return this.move != PaleMove.NONE;
        }
    }

    private EtoOwl() {
    }

    // ---------------------------------------------------------------- each tick

    /** A tick of her fight against {@code target} (null: none left; a move under way plays out). */
    static void tick(EtoNpc npc, EtoBrain.Mind mind, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        Fight f = mind.owl;
        if (f.busy()) {
            carry(npc, mind, f, level, now, phase);
            return;
        }
        if (target == null) {
            return;
        }
        // 1.3.8: in the Owl, its own moves; hurt far enough, she grows it (her last resort).
        if (f.owl) {
            EtoOwlForm.fight(npc, mind, f, target, level, now, phase);
            return;
        }
        if (EtoRules.owlNow(share(npc), f.owl, now, f.owlReady)) {
            face(npc, target);
            EtoOwlForm.grow(npc, f, level, now);
            return;
        }
        double distance = gap(npc, target);
        boolean sees = npc.m_142582_(target);
        if (EtoArm.out(npc)) {
            // 1.3.7: her Mutated Arm out: she walks on at it (as a player walks with hers out), nothing else thrown meanwhile.
            face(npc, target);
            npc.m_21573_().m_5624_(target, phase.chase * 0.8);
            return;
        }
        EtoRules.Act act = EtoRules.choose(distance, sees, npc.m_20096_(), share(npc), now, new EtoRules.Ready(f.punchReady, f.kickReady, f.flipReady,
            f.swipeReady, f.grabReady, f.hammerReady, EtoWatch.out(npc) ? Long.MAX_VALUE : f.watchReady, f.armReady), npc.m_217043_().m_188500_());
        face(npc, target);
        switch (act) {
            case SWIPE -> EtoFlesh.swipe(npc, f, target, level, now, phase);
            case GRAB -> EtoFlesh.grab(npc, f, target, level, now, phase);
            case HAMMER -> EtoFlesh.hammer(npc, f, target, level, now, phase);
            case WATCH -> EtoWatch.start(npc, f, level, now, phase);
            case ARM -> EtoArm.start(npc, f, target, level, now, phase);
            case KICK -> {
                if (!kick(npc, f, target, level, now, phase)) {
                    npc.m_21573_().m_5624_(target, phase.chase);
                }
            }
            case PUNCH -> {
                begin(npc, f, PaleMove.PUNCH_CHARGE, 0, target.m_19879_(), 0.0F, Vec3.f_82478_, now);
                f.hold = EtoRules.punchHold(phase, npc.m_217043_().m_188500_());
                npc.m_21573_().m_26573_();
                UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.nodamage", 0.8F, 0.6F);
            }
            case FLIP_IN, FLIP_BACK -> flip(npc, f, act == EtoRules.Act.FLIP_IN, target, level, now, phase);
            case STRIKE -> strike(npc, f, target, level, now);
            case STEP_IN -> {
                // A lunge in, then the blitz.
                Vec3 way = UkakuFire.horizontal(target.m_20182_().m_82546_(npc.m_20182_()));
                npc.m_20256_(new Vec3(way.f_82479_ * PaleRules.STEP_IN_SPEED * 0.6, npc.m_20184_().f_82480_, way.f_82481_ * PaleRules.STEP_IN_SPEED * 0.6));
                npc.f_19864_ = true;
            }
            default -> npc.m_21573_().m_5624_(target, phase.chase);
        }
    }

    /** True while nothing can hurt her: the Speed Kick's lunge and dash. */
    static boolean untouchable(Fight f, long now) {
        return f.move == PaleMove.KICK && PaleRules.kickUntouchable(f.detail, now - f.at);
    }

    /** True while a move carries her about (the kick, Witness This): nothing else moves her. */
    static boolean carried(Fight f) {
        return f.move == PaleMove.KICK || f.move == PaleMove.WITNESS;
    }

    private static void carry(EtoNpc npc, EtoBrain.Mind mind, Fight f, ServerLevel level, long now, EtoRules.Phase phase) {
        long age = now - f.at;
        LivingEntity target = f.target == 0 ? null : level.m_6815_(f.target) instanceof LivingEntity living && living.m_6084_() ? living : null;
        npc.f_19789_ = 0.0F;
        switch (f.move) {
            case STRIKE -> tickStrike(npc, mind, f, target, level, now, age, phase);
            case PUNCH_CHARGE -> {
                npc.m_21573_().m_26573_();
                if (target != null) {
                    face(npc, target);
                }
                if (age >= f.hold || target == null) {
                    release(npc, f, target, level, now, phase);
                }
            }
            case PUNCH -> tickPunch(npc, mind, f, target, level, now, age, phase);
            case KICK -> tickKick(npc, f, target, level, now, age, phase);
            case WITNESS -> tickWitness(npc, mind, f, target, level, now, age, phase);
            case SWIPE, GRAB_REACH, GRAB_HOLD, GRAB_THROW, GRAB_MISS, HAMMER_CHARGE, HAMMER -> EtoFlesh.tick(npc, mind, f, target, level, now, age, phase);
            case OWL_STRIKE, OWL_WHIRL, OWL_LEAP -> EtoOwlForm.tick(npc, mind, f, target, level, now, age, phase);
            case WATCH -> {
                npc.m_21573_().m_26573_();
                if (age >= com.hex.kagune.control.pale.WatchRules.POSE_TICKS) {
                    end(f);
                }
            }
            case FLIP -> {
                if (age == 1L && f.flipPush != null) {
                    npc.m_20256_(f.flipPush);
                    npc.f_19864_ = true;
                }
                if (age >= NpcRules.FLIP_BUSY && (npc.m_20096_() || age >= PaleRules.FLIP_MAX_TICKS)) {
                    end(f);
                } else {
                    npc.m_21573_().m_26573_();
                }
            }
            default -> end(f);
        }
    }

    // ---------------------------------------------------------------- the Speed Blitz

    private static void strike(EtoNpc npc, Fight f, LivingEntity target, ServerLevel level, long now) {
        if (f.strikeEnded != Long.MIN_VALUE && now - f.strikeEnded > PaleRules.CHAIN_WINDOW) {
            f.strike = 0;
        }
        if (f.strike == 0 && f.strikeEnded != Long.MIN_VALUE && now - f.strikeEnded < PaleRules.FINISH_RECOVERY && f.detail == PaleStrike.TWIN_PALM.ordinal()) {
            return;
        }
        PaleStrike strike = PaleStrike.values()[Math.floorMod(f.strike, PaleStrike.values().length)];
        npc.m_21573_().m_26573_();
        begin(npc, f, PaleMove.STRIKE, strike.ordinal(), target.m_19879_(), 0.0F, Vec3.f_82478_, now);
    }

    private static void tickStrike(EtoNpc npc, EtoBrain.Mind mind, Fight f, LivingEntity target, ServerLevel level, long now, long age, EtoRules.Phase phase) {
        PaleStrike strike = PaleStrike.values()[f.detail];
        if (target != null && age < strike.impact) {
            face(npc, target);
            f.yaw = npc.f_20883_;
        }
        if (age == PaleStrike.BLINK_TICK && strike.blink != PaleStrike.Blink.NONE && target != null) {
            blink(npc, f, target, level, strike.blink);
        }
        if (age == strike.impact && target != null) {
            double reach = strike.reach * EtoRules.SIZE + 0.4;
            if (gap(npc, target) <= reach) {
                Vec3 facing = UkakuFire.horizontal(target.m_20182_().m_82546_(npc.m_20182_()));
                Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
                Vec3 knock = facing.m_82490_(strike.back).m_82549_(right.m_82490_(strike.sideways)).m_82520_(0.0, strike.up, 0.0);
                float damage = PaleRules.fist(EtoRules.MASTERY) * strike.damage * phase.power;
                Vec3 at = GhoulNpcBrain.centre(target).m_82546_(facing.m_82490_(target.m_20205_() * 0.5));
                if (hurt(npc, target, damage, at, facing)) {
                    target.m_20256_(target.m_20184_().m_82490_(0.3).m_82549_(knock));
                    target.f_19864_ = true;
                }
                fx(npc, S2CPaleFx.IMPACT, target.m_19879_(), at, knock, strike.finisher() ? 1.6F : 1.0F, strike.ordinal());
                UkakuFire.sound(level, at, strike.finisher() ? "entity.player.attack.strong" : "entity.player.attack.knockback", 0.9F, 1.0F);
            } else {
                UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.sweep", 0.5F, 1.3F);
            }
        }
        boolean chain = age >= strike.chain && target != null && gap(npc, target) <= EtoRules.STRIKE_REACH + 0.6 && !strike.finisher();
        if (chain || age >= strike.duration) {
            f.strikeEnded = now;
            f.strike = strike.finisher() ? 0 : f.strike + 1;
            end(f);
            if (chain) {
                strike(npc, f, target, level, now);
            }
        }
    }

    /** She vanishes and is at its side ({@code SIDE}) or square in front of it ({@code FRONT}), already striking. */
    private static void blink(EtoNpc npc, Fight f, LivingEntity target, ServerLevel level, PaleStrike.Blink kind) {
        Vec3 from = npc.m_20182_();
        Vec3 facing = UkakuFire.horizontal(target.m_20182_().m_82546_(from));
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        double stand = PaleRules.strikeStand(target.m_20205_()) * EtoRules.SIZE;
        Vec3 to = kind == PaleStrike.Blink.SIDE ? target.m_20182_().m_82549_(right.m_82490_(stand)) : target.m_20182_().m_82546_(facing.m_82490_(stand));
        if (!level.m_45756_(npc, npc.m_20191_().m_82386_(to.f_82479_ - from.f_82479_, to.f_82480_ - from.f_82480_, to.f_82481_ - from.f_82481_))) {
            return;
        }
        fx(npc, S2CPaleFx.BLINK, target.m_19879_(), from, to.m_82546_(from), 1.0F, 0);
        npc.m_6021_(to.f_82479_, to.f_82480_, to.f_82481_);
        face(npc, target);
        f.yaw = npc.f_20883_;
        UkakuFire.sound(level, to, "entity.illusioner.mirror_move", 0.6F, 1.5F);
    }

    // ---------------------------------------------------------------- the Big Punch, and Witness This

    private static void release(EtoNpc npc, Fight f, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        float charge = PaleRules.punchCharge(now - f.at);
        LivingEntity aim = target != null && gap(npc, target) <= PaleRules.punchRange(charge) && npc.m_142582_(target) ? target : null;
        begin(npc, f, PaleMove.PUNCH, 0, aim == null ? 0 : aim.m_19879_(), charge, Vec3.f_82478_, now);
        f.punchReady = now + EtoRules.cooldown(EtoRules.PUNCH_EVERY, phase);
        UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.sweep", 0.8F, 0.55F);
    }

    private static void tickPunch(EtoNpc npc, EtoBrain.Mind mind, Fight f, LivingEntity target, ServerLevel level, long now, long age, EtoRules.Phase phase) {
        npc.m_21573_().m_26573_();
        if (age <= PaleRules.PUNCH_IMPACT && target != null) {
            // It never sees her coming: nothing it does lands on her before the blow.
            PaleStun.stun(target, now, 2);
        }
        if (age == PaleRules.PUNCH_BLINK && target != null) {
            Vec3 from = npc.m_20182_();
            Vec3 facing = UkakuFire.horizontal(target.m_20182_().m_82546_(from));
            Vec3 to = target.m_20182_().m_82546_(facing.m_82490_((PaleRules.strikeStand(target.m_20205_()) - 0.15) * EtoRules.SIZE));
            if (level.m_45756_(npc, npc.m_20191_().m_82386_(to.f_82479_ - from.f_82479_, to.f_82480_ - from.f_82480_, to.f_82481_ - from.f_82481_))) {
                fx(npc, S2CPaleFx.BLINK, target.m_19879_(), from, to.m_82546_(from), 1.2F, 0);
                npc.m_6021_(to.f_82479_, to.f_82480_, to.f_82481_);
            }
            face(npc, target);
            f.yaw = npc.f_20883_;
        }
        if (age == PaleRules.PUNCH_IMPACT) {
            punch(npc, mind, f, target, level, now, phase);
            if (f.move == PaleMove.WITNESS) {
                return;
            }
        }
        if (age >= PaleRules.PUNCH_TICKS) {
            end(f);
        }
    }

    /** The blow: the target, if she reached it, launched; round it, the air thrown. At the full charge, landed: Witness This. */
    private static void punch(EtoNpc npc, EtoBrain.Mind mind, Fight f, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        float charge = f.charge;
        Vec3 facing = target != null ? UkakuFire.horizontal(target.m_20182_().m_82546_(npc.m_20182_())) : UkakuFire.horizontal(npc.m_20154_());
        Vec3 fist = npc.m_20182_().m_82520_(facing.f_82479_ * 1.1, 0.9, facing.f_82481_ * 1.1);
        boolean reached = target != null && gap(npc, target) <= 2.6;
        float damage = PaleRules.punchDamage(EtoRules.MASTERY, charge) * phase.power;
        if (reached) {
            Vec3 centre = GhoulNpcBrain.centre(target);
            fist = new Vec3(centre.f_82479_ - facing.f_82479_ * target.m_20205_() * 0.5, target.m_20186_() + target.m_20206_() * 0.45,
                centre.f_82481_ - facing.f_82481_ * target.m_20205_() * 0.5);
            if (hurt(npc, target, damage, fist, facing)) {
                if (charge >= 0.999F && target.m_6084_()) {
                    witness(npc, f, target, facing, level, now);
                } else {
                    double resist = resist(target);
                    Vec3 launch = facing.m_82490_(PaleRules.punchLaunch(charge) * 0.6 * (1.0 - 0.6 * resist)).m_82520_(0.0, PaleRules.punchLift(charge), 0.0);
                    target.m_20256_(launch);
                    target.f_19864_ = true;
                    PaleKnockdown.launched(target, now);
                }
            }
        }
        // The air itself is thrown: anything hostile close round the blow is knocked back.
        AABB around = new AABB(fist.f_82479_ - 3.0, fist.f_82480_ - 2.0, fist.f_82481_ - 3.0, fist.f_82479_ + 3.0, fist.f_82480_ + 2.0, fist.f_82481_ + 3.0);
        for (LivingEntity other : level.m_6443_(LivingEntity.class, around, body -> body != target && fair(npc, mind, body))) {
            Vec3 away = UkakuFire.horizontal(other.m_20182_().m_82546_(npc.m_20182_()));
            if (away.m_82526_(facing) < 0.2) {
                continue;
            }
            if (hurt(npc, other, damage * 0.3F, GhoulNpcBrain.centre(other), away)) {
                other.m_20256_(other.m_20184_().m_82490_(0.3).m_82549_(away.m_82490_(0.8 + charge)).m_82520_(0.0, 0.3, 0.0));
                other.f_19864_ = true;
            }
        }
        fx(npc, S2CPaleFx.IMPACT, reached ? target.m_19879_() : 0, fist, facing.m_82490_(PaleRules.punchLaunch(charge)), reached ? 2.0F + charge : 1.2F + charge,
            S2CPaleFx.BIG_PUNCH);
        UkakuFire.sound(level, fist, "entity.generic.explode", 0.7F + 0.3F * charge, 1.25F);
        if (reached) {
            UkakuFire.sound(level, fist, "entity.player.attack.knockback", 1.2F, 0.6F);
        }
    }

    /** The full charge landed: Witness This, from now. */
    private static void witness(EtoNpc npc, Fight f, LivingEntity target, Vec3 facing, ServerLevel level, long now) {
        Vec3 at = target.m_20182_();
        float yaw = (float)Math.toDegrees(Math.atan2(-facing.f_82479_, facing.f_82481_));
        Vec3 over = at.m_82549_(facing.m_82490_(WitnessRules.KNOCK_BACK));
        BlockHitResult ground = level.m_45547_(new ClipContext(over.m_82520_(0.0, 0.5, 0.0), over.m_82520_(0.0, -6.0, 0.0), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, target));
        double groundY = ground.m_6662_() == HitResult.Type.BLOCK ? ground.m_82450_().f_82480_ - at.f_82480_ : 0.0;
        begin(npc, f, PaleMove.WITNESS, 0, target.m_19879_(), (float)groundY, at, now);
        f.yaw = yaw;
        hold(npc, f, target, 0L, now);
        UkakuFire.sound(level, at, "entity.illusioner.mirror_move", 0.8F, 1.1F);
    }

    private static void tickWitness(EtoNpc npc, EtoBrain.Mind mind, Fight f, LivingEntity target, ServerLevel level, long now, long age, EtoRules.Phase phase) {
        npc.m_21573_().m_26573_();
        if (target == null && age < WitnessRules.IMPACT) {
            end(f);
            return;
        }
        if (target != null && WitnessRules.frozen(age)) {
            hold(npc, f, target, age, now);
        }
        // She is where the combo has her (behind it, after it, round it, above it), until she kicks off it and falls.
        double[] her = WitnessRules.her(age + 1.0, f.charge);
        if (her != null) {
            Vec3 to = f.point.m_82549_(local(f, her[0], her[1], her[2]));
            npc.m_6021_(to.f_82479_, to.f_82480_, to.f_82481_);
            npc.m_20256_(Vec3.f_82478_);
        }
        if (WitnessRules.blinks((int)age)) {
            UkakuFire.sound(level, npc.m_20182_(), "entity.illusioner.mirror_move", 0.5F, 1.5F);
        }
        float fist = PaleRules.fist(EtoRules.MASTERY) * phase.power;
        if (age == WitnessRules.UPPER && target != null) {
            blow(npc, f, target, level, fist * WitnessRules.UPPER_DAMAGE, new Vec3(0.0, 1.0, 0.0), 2.4F, true);
            UkakuFire.sound(level, target.m_20182_(), "entity.player.attack.crit", 1.2F, 0.7F);
            PaleKnockdown.launched(target, now, WitnessRules.DOWN_TICKS, true);
            fx(npc, S2CPaleFx.KICKED, target.m_19879_(), target.m_20182_(), local(f, 0.0, 1.2, 0.8), 1.0F, 0);
        }
        for (int hit = 0; hit < WitnessRules.HITS; hit++) {
            if (age == WitnessRules.hitAt(hit) && target != null) {
                double[] k = WitnessRules.knock(hit);
                boolean heavy = WitnessRules.heavy(hit);
                boolean hard = heavy || WitnessRules.blow(hit).kick();
                float power = (float)WitnessRules.power(hit);
                float size = (heavy ? 2.1F : hard ? 1.7F : 1.35F) * (0.75F + 0.25F * power);
                blow(npc, f, target, level, fist * WitnessRules.HIT_DAMAGE * (0.6F + 0.4F * power), local(f, k[0], k[1], k[2]), size, hard);
                if (heavy) {
                    UkakuFire.sound(level, target.m_20182_(), "entity.generic.explode", 0.45F, 1.5F);
                }
            }
        }
        if (age == WitnessRules.DROP && target != null) {
            blow(npc, f, target, level, 0.0F, new Vec3(0.0, -1.0, 0.0), 3.4F, true);
            UkakuFire.sound(level, target.m_20182_(), "entity.generic.explode", 0.7F, 1.3F);
        }
        if (age == WitnessRules.IMPACT) {
            crater(npc, mind, f, target, level, now, fist);
        }
        if (age >= WitnessRules.LONGEST || age >= WitnessRules.TICKS && (npc.m_20096_() || npc.m_20069_())) {
            end(f);
        }
    }

    /** What she hit is where the combo has it this tick, can do nothing, and takes no fall. */
    private static void hold(EtoNpc npc, Fight f, LivingEntity target, long age, long now) {
        double[] at = WitnessRules.target(age + 1.0, f.charge);
        Vec3 feet = f.point.m_82549_(local(f, at[0], at[1], at[2]));
        PaleStun.stun(target, now, 3);
        target.f_19789_ = 0.0F;
        if (target instanceof ServerPlayer player) {
            if (player.m_20182_().m_82554_(feet) > 2.5) {
                player.m_6021_(feet.f_82479_, feet.f_82480_, feet.f_82481_);
            }
            player.m_20256_(Vec3.f_82478_);
            return;
        }
        target.m_6021_(feet.f_82479_, feet.f_82480_, feet.f_82481_);
        target.m_20256_(Vec3.f_82478_);
        target.f_19864_ = true;
    }

    private static void blow(EtoNpc npc, Fight f, LivingEntity target, ServerLevel level, float damage, Vec3 push, float size, boolean hard) {
        Vec3 middle = target.m_20182_().m_82520_(0.0, target.m_20206_() * 0.5, 0.0);
        if (damage > 0.0F) {
            hurt(npc, target, damage, middle, push);
        }
        fx(npc, S2CPaleFx.IMPACT, target.m_19879_(), middle.m_82546_(push.m_82490_(target.m_20205_() * 0.5)), push, size, hard ? S2CPaleFx.BIG_PUNCH : 0);
        UkakuFire.sound(level, middle, hard ? "entity.player.attack.strong" : "entity.player.attack.knockback", 0.9F, hard ? 0.6F : 0.9F);
    }

    /** Down: the shock round where it hits, hurting and throwing everything hostile near; the flash and the rings. */
    private static void crater(EtoNpc npc, EtoBrain.Mind mind, Fight f, LivingEntity target, ServerLevel level, long now, float fist) {
        double[] at = WitnessRules.target(WitnessRules.IMPACT, f.charge);
        Vec3 down = f.point.m_82549_(local(f, at[0], at[1], at[2]));
        if (target != null) {
            hurt(npc, target, fist * WitnessRules.DROP_DAMAGE, target.m_20182_(), new Vec3(0.0, -1.0, 0.0));
            target.m_20256_(Vec3.f_82478_);
            target.f_19864_ = true;
            PaleStun.stun(target, now, 20);
        }
        AABB area = new AABB(down.f_82479_ - WitnessRules.SHOCK, down.f_82480_ - 2.0, down.f_82481_ - WitnessRules.SHOCK, down.f_82479_ + WitnessRules.SHOCK,
            down.f_82480_ + 3.0, down.f_82481_ + WitnessRules.SHOCK);
        for (LivingEntity body : level.m_6443_(LivingEntity.class, area, entity -> entity != target && fair(npc, mind, entity))) {
            Vec3 off = body.m_20182_().m_82546_(down);
            double flat = Math.sqrt(off.f_82479_ * off.f_82479_ + off.f_82481_ * off.f_82481_);
            if (flat > WitnessRules.SHOCK) {
                continue;
            }
            double near = 1.0 - flat / WitnessRules.SHOCK;
            Vec3 away = flat < 0.2 ? local(f, 0.0, 0.0, 1.0) : new Vec3(off.f_82479_ / flat, 0.0, off.f_82481_ / flat);
            if (hurt(npc, body, fist * WitnessRules.SHOCK_DAMAGE * (float)(0.4 + 0.6 * near), body.m_20182_(), away)) {
                body.m_20256_(away.m_82490_(0.6 + 0.9 * near).m_82520_(0.0, 0.5 + 0.6 * near, 0.0));
                body.f_19864_ = true;
            }
        }
        int ground = net.minecraft.world.level.block.Block.m_49956_(level.m_8055_(net.minecraft.core.BlockPos.m_274561_(down.f_82479_, down.f_82480_ - 0.01,
            down.f_82481_)));
        fx(npc, S2CPaleFx.SHOCKWAVE, 0, down, local(f, 0.0, 0.0, 1.0), (float)WitnessRules.SHOCK, ground);
        fx(npc, S2CPaleFx.IMPACT, target == null ? 0 : target.m_19879_(), down.m_82520_(0.0, 0.4, 0.0), new Vec3(0.0, -1.0, 0.0), 4.0F, S2CPaleFx.BIG_PUNCH);
        fx(npc, S2CPaleFx.SHOCKWAVE, 0, down, local(f, 1.0, 0.0, 0.0), (float)(WitnessRules.SHOCK * 0.6), ground);
        UkakuFire.sound(level, down, "entity.lightning_bolt.thunder", 0.6F, 1.2F);
        UkakuFire.sound(level, down, "entity.generic.explode", 1.5F, 0.5F);
        UkakuFire.sound(level, down, "block.anvil.land", 0.8F, 0.45F);
    }

    // ---------------------------------------------------------------- the Speed Kick

    /** The kick at {@code target} if there is a way to it and room to kick from; false (nothing begun) if not. */
    private static boolean kick(EtoNpc npc, Fight f, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        Vec3 from = npc.m_20182_();
        Vec3 at = target.m_20182_();
        Vec3 facing = UkakuFire.horizontal(at.m_82546_(from));
        if (facing.m_82556_() < 1.0E-6) {
            return false;
        }
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        double[] stand = PaleRules.kickStand(target.m_20205_() * 0.5);
        Vec3 spot = at.m_82549_(right.m_82490_(stand[0] * EtoRules.SIZE)).m_82549_(facing.m_82490_(stand[1] * EtoRules.SIZE));
        double lift = PaleRules.kickLift(target.m_20206_() * PaleRules.KICK_AIM / EtoRules.SIZE) * EtoRules.SIZE;
        Vec3 offset = spot.m_82546_(from);
        AABB there = npc.m_20191_().m_82386_(offset.f_82479_, offset.f_82480_, offset.f_82481_);
        if (!level.m_45756_(npc, there) || !level.m_45756_(npc, there.m_82386_(0.0, lift, 0.0))) {
            return false;
        }
        for (double up : new double[] {0.6, 1.4}) {
            if (level.m_45547_(new ClipContext(from.m_82520_(0.0, up, 0.0), spot.m_82520_(0.0, up, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc))
                .m_6662_() != HitResult.Type.MISS) {
                return false;
            }
        }
        int dash = PaleRules.kickDash(offset.m_82553_());
        npc.m_21573_().m_26573_();
        begin(npc, f, PaleMove.KICK, dash, target.m_19879_(), (float)lift, spot, now);
        f.from = from;
        f.yaw = (float)Math.toDegrees(Math.atan2(-facing.f_82479_, facing.f_82481_));
        f.kickReady = now + EtoRules.cooldown(EtoRules.KICK_EVERY, phase);
        pin(target, now);
        UkakuFire.sound(level, from, "block.gravel.break", 1.0F, 0.7F);
        return true;
    }

    private static void tickKick(EtoNpc npc, Fight f, LivingEntity target, ServerLevel level, long now, long age, EtoRules.Phase phase) {
        int dash = f.detail;
        int arrive = PaleRules.kickArrive(dash);
        long contact = arrive + PaleRules.KICK_CONTACT;
        if (age < contact && target != null) {
            pin(target, now);
        }
        // Where the kick has her (the lunge, the dash, the blow held up there, the rebound's hop), relative to where she began.
        Vec3 to = f.point.m_82546_(f.from);
        double[] place = PaleRules.kickPlace(new double[] {to.f_82479_, to.f_82480_, to.f_82481_}, dash, f.charge, age + 1.0);
        if (place != null) {
            npc.m_6021_(f.from.f_82479_ + place[0], f.from.f_82480_ + place[1], f.from.f_82481_ + place[2]);
            npc.m_20256_(Vec3.f_82478_);
        }
        npc.m_146922_(f.yaw);
        npc.f_20883_ = f.yaw;
        npc.f_20885_ = f.yaw;
        if (age == PaleRules.KICK_BURST) {
            // Gone.
            npc.m_7292_(new MobEffectInstance(MobEffects.f_19609_, Math.max(1, dash), 0, false, false, false));
            UkakuFire.sound(level, npc.m_20182_(), "entity.illusioner.mirror_move", 1.0F, 1.3F);
        }
        if (age == arrive) {
            UkakuFire.sound(level, f.point, "entity.ender_dragon.flap", 0.8F, 1.6F);
            UkakuFire.sound(level, f.point, "entity.player.attack.sweep", 1.0F, 0.55F);
        }
        if (age == contact) {
            kickLands(npc, f, target, level, now, phase);
        }
        if (age == arrive + (long)Math.ceil(PaleRules.KICK_LAND)) {
            UkakuFire.sound(level, npc.m_20182_(), "entity.player.small_fall", 0.9F, 0.8F);
        }
        if (age >= arrive + PaleRules.KICK_TICKS) {
            end(f);
        }
    }

    /** The blow, the sole driven through its middle: hurt, and launched far away from her, tumbling; it lies there a moment. */
    private static void kickLands(EtoNpc npc, Fight f, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        if (target == null || gap(npc, target) > 2.4) {
            UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.sweep", 0.8F, 0.8F);
            return;
        }
        double yaw = Math.toRadians(f.yaw);
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 struck = target.m_20182_().m_82520_(0.0, target.m_20206_() * PaleRules.KICK_AIM, 0.0).m_82546_(facing.m_82490_(target.m_20205_() * 0.5));
        float damage = PaleRules.fist(EtoRules.MASTERY) * PaleRules.KICK_DAMAGE * phase.power;
        if (!hurt(npc, target, damage, struck, facing)) {
            return;
        }
        double speed = PaleRules.KICK_SPEED * 0.55 * (1.0 - PaleRules.KICK_RESIST * resist(target));
        Vec3 launch = facing.m_82490_(speed).m_82520_(0.0, PaleRules.KICK_UP, 0.0);
        target.m_20256_(launch);
        target.f_19864_ = true;
        PaleKnockdown.launched(target, now);
        fx(npc, S2CPaleFx.KICKED, target.m_19879_(), struck, launch, 1.0F, 0);
        UkakuFire.sound(level, struck, "entity.player.attack.strong", 1.4F, 0.5F);
        UkakuFire.sound(level, struck, "entity.warden.sonic_boom", 0.7F, 1.35F);
        UkakuFire.sound(level, struck, "entity.generic.explode", 0.6F, 1.5F);
    }

    /** Held where it is (no going anywhere but down), too slow to do anything about her. */
    private static void pin(LivingEntity body, long now) {
        body.m_20256_(new Vec3(0.0, Math.min(0.0, body.m_20184_().f_82480_), 0.0));
        body.f_19864_ = true;
        PaleStun.stun(body, now, 2);
    }

    // ---------------------------------------------------------------- flips

    private static void flip(EtoNpc npc, Fight f, boolean forward, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        Vec3 to = target.m_20182_().m_82546_(npc.m_20182_());
        Vec3 way = UkakuFire.horizontal(to);
        if (way.m_82556_() < 1.0E-6) {
            way = UkakuFire.horizontal(npc.m_20154_());
        }
        if (!forward) {
            way = way.m_82490_(-1.0);
        }
        double speed = forward ? NpcRules.flipSpeed(gap(npc, target) - EtoRules.STRIKE_REACH) : NpcRules.FLIP_BACK_SPEED;
        f.flipPush = new Vec3(way.f_82479_ * speed, forward ? NpcRules.FLIP_FORWARD_UP : NpcRules.FLIP_BACK_UP, way.f_82481_ * speed);
        npc.m_21573_().m_26573_();
        npc.m_20256_(f.flipPush);
        npc.f_19864_ = true;
        begin(npc, f, PaleMove.FLIP, (forward ? PaleRules.Flip.FORWARD : PaleRules.Flip.BACK).ordinal(), target.m_19879_(), 0.0F, Vec3.f_82478_, now);
        f.flipReady = now + EtoRules.cooldown(EtoRules.FLIP_EVERY, phase);
        UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.sweep", 0.35F, 0.7F);
    }

    // ---------------------------------------------------------------- helpers

    static void begin(EtoNpc npc, Fight f, PaleMove move, int detail, int target, float charge, Vec3 point, long now) {
        f.move = move;
        f.detail = detail;
        f.at = now;
        f.target = target;
        f.charge = charge;
        f.point = point;
        f.yaw = npc.f_20883_;
        f.dirty = true;
    }

    static void end(Fight f) {
        if (f.move != PaleMove.NONE) {
            f.move = PaleMove.NONE;
            f.at = Long.MIN_VALUE;
            f.dirty = true;
        }
    }

    /** Her blow on {@code body}: the hurt, a bruise where it landed; true if it took it. */
    static boolean hurt(EtoNpc npc, LivingEntity body, float damage, Vec3 at, Vec3 way) {
        body.f_19802_ = 0;
        WoundHits.expect(body, com.hex.kagune.control.wound.WoundBook.Blow.BLUNT, at, way, 0.0);
        return body.m_6469_(npc.m_269291_().m_269333_(npc), damage);
    }

    /** What her blows may catch besides what she fights: anything hostile; never her partner, never a player she isn't fighting. */
    static boolean fair(EtoNpc npc, EtoBrain.Mind mind, LivingEntity body) {
        if (body == npc || !body.m_6084_() || EtoBrain.partnerOf(mind, body)) {
            return false;
        }
        return body.m_19879_() == mind.target || body instanceof Enemy;
    }

    /** How far she is from {@code body}'s near side (blocks, level). */
    static double gap(EtoNpc npc, LivingEntity body) {
        double dx = body.m_20185_() - npc.m_20185_();
        double dz = body.m_20189_() - npc.m_20189_();
        return Math.max(0.0, Math.sqrt(dx * dx + dz * dz) - body.m_20205_() * 0.5);
    }

    private static float share(EtoNpc npc) {
        return npc.m_21223_() / Math.max(1.0F, npc.m_21233_());
    }

    static double resist(LivingEntity body) {
        return Math.max(0.0, Math.min(1.0, body.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
    }

    static void face(EtoNpc npc, LivingEntity other) {
        Vec3 to = other.m_20182_().m_82546_(npc.m_20182_());
        if (to.f_82479_ * to.f_82479_ + to.f_82481_ * to.f_82481_ < 1.0E-4) {
            return;
        }
        float yaw = (float)Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_));
        npc.m_146922_(yaw);
        npc.f_20883_ = yaw;
        npc.f_20885_ = yaw;
    }

    /** A point in the move's frame (x to her right as she faced, y up, z the way she faced) as a world offset. */
    static Vec3 local(Fight f, double x, double y, double z) {
        double yaw = Math.toRadians(f.yaw);
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        return right.m_82490_(x).m_82549_(facing.m_82490_(z)).m_82520_(0.0, y, 0.0);
    }

    static void fx(EtoNpc npc, int kind, int target, Vec3 at, Vec3 velocity, float size, int extra) {
        ControlNetwork.broadcastAround(npc, PaleBlitz.packet(kind, npc.m_19879_(), target, at, velocity, size, extra));
    }
}

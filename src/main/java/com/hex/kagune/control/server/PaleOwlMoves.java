package com.hex.kagune.control.server;

import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.OwlRules;
import com.hex.kagune.control.pale.OwlSpikes;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 6.2.0: the Owl's own moves (Pale's first kakuja, {@link OwlRules}), what lands of them on the server; their keys, timing and
 * cancelling go through the same paths as her base moves ({@link PaleMoves}, {@link PaleCancel}), each base move turned into the
 * Owl's own while she is in it.
 */
final class PaleOwlMoves {
    private PaleOwlMoves() {
    }

    // ------------------------------------------------------------------ its Rend (M1)

    /** An M1 in the Owl: its next cut, now or at the current one's chain tick (a click during a cut is kept for it). */
    static void click(ServerPlayer player, PaleServer.PaleState state, net.minecraft.world.entity.Entity hint, long now) {
        if (!PaleOwl.ready(state, now)) {
            return;
        }
        if (state.move == com.hex.kagune.control.pale.PaleMove.OWL_STRIKE) {
            state.queuedAt = now;
            state.queuedHint = hint == null ? 0 : hint.m_19879_();
            return;
        }
        if (now < state.recoverUntil) {
            return; // catching her breath after the finisher
        }
        rend(player, state, hint, now);
    }

    /** The next cut (on through the chain within its window, else the first), at what she clicked or what is in front of her. */
    private static void rend(ServerPlayer player, PaleServer.PaleState state, net.minecraft.world.entity.Entity hint, long now) {
        int stage = state.owlRend >= 0 && now - state.owlRendEndsAt <= PaleRules.CHAIN_WINDOW ? OwlRules.nextRend(state.owlRend) : 0;
        if (!KaguneServerController.useEnergy(player, OwlRules.REND_COST[stage], true)) {
            return;
        }
        LivingEntity target = PaleBlitz.lockOn(player, hint);
        PaleCancel.cut(player, state, now);
        float yaw = player.m_146908_();
        if (target != null) {
            Vec3 to = UkakuFire.horizontal(target.m_20182_().m_82546_(player.m_20182_()));
            yaw = (float)Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_));
        }
        PaleServer.begin(player, state, com.hex.kagune.control.pale.PaleMove.OWL_STRIKE, stage, target == null ? 0 : target.m_19879_(), 0.0F, null, now, yaw);
        state.owlRend = stage;
        state.owlRendEndsAt = now + OwlRules.REND_TICKS[stage];
        state.queuedAt = Long.MIN_VALUE;
    }

    /** A cut in flight: the step in, the blade landing, the next cut out of it if a click was kept, the end. */
    static void tickRend(ServerPlayer player, PaleServer.PaleState state, long now) {
        int stage = Math.max(0, Math.min(OwlRules.REND_STAGES - 1, state.detail));
        long age = now - state.startedAt;
        LivingEntity target = state.targetId == 0 ? null
            : player.m_9236_().m_6815_(state.targetId) instanceof LivingEntity living && PaleBlitz.valid(player, living)
                && living.m_20280_(player) <= PaleRules.STEP_IN_RANGE * PaleRules.STEP_IN_RANGE * 2.0 ? living : null;
        if (target != null && age < OwlRules.REND_IMPACT[stage]) {
            // Stepping in through the wind-up, so the blade meets it on the impact tick.
            Vec3 to = target.m_20182_().m_82546_(player.m_20182_());
            double distance = Math.sqrt(to.f_82479_ * to.f_82479_ + to.f_82481_ * to.f_82481_);
            double stop = OwlRules.REND_STAND + target.m_20205_() * 0.5;
            if (distance > stop + 0.05) {
                double left = Math.max(1.0, OwlRules.REND_IMPACT[stage] - age);
                double speed = Math.min(PaleRules.STEP_IN_SPEED, (distance - stop) / left);
                Vec3 heading = UkakuFire.horizontal(to);
                double up = player.m_20096_() ? Math.min(player.m_20184_().f_82480_, 0.0) : player.m_20184_().f_82480_;
                UkakuFire.push(player, new Vec3(heading.f_82479_ * speed, up, heading.f_82481_ * speed));
            }
        }
        if (age == OwlRules.REND_IMPACT[stage] - 1) {
            UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.attack.sweep", 1.0F, KaguneServerController.vary(player, stage == 3 ? 0.45F : 0.6F, 0.05F));
        }
        if (age == OwlRules.REND_IMPACT[stage]) {
            landRend(player, state, stage, now);
        }
        if (age >= OwlRules.REND_CHAIN[stage] && state.queuedAt >= state.startedAt) {
            net.minecraft.world.entity.Entity hint = state.queuedHint == 0 ? null : player.m_9236_().m_6815_(state.queuedHint);
            state.owlRendEndsAt = now;
            state.move = com.hex.kagune.control.pale.PaleMove.NONE;
            rend(player, state, hint, now);
            if (state.move != com.hex.kagune.control.pale.PaleMove.OWL_STRIKE) {
                PaleServer.broadcast(player, state); // no RC left for it: she stops
            }
            return;
        }
        if (age >= OwlRules.REND_TICKS[stage]) {
            if (stage == OwlRules.REND_STAGES - 1) {
                state.recoverUntil = now + Cooldowns.of(player, PaleRules.FINISH_RECOVERY);
            }
            state.push = null;
            state.pushTicks = 0;
            PaleServer.end(player, state);
        }
    }

    /**
     * A cut lands: everything in front of her within the blade's reach and arc is cut once: the hurt, a bleed, thrown the way the blade
     * went, stunned (it can't fight back mid-combo; the finisher longer, and it is knocked down); the finisher is its bite.
     */
    private static void landRend(ServerPlayer player, PaleServer.PaleState state, int stage, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 facing = facing(state);
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        double[] k = OwlRules.REND_KNOCK[stage];
        Vec3 knock = facing.m_82490_(k[0]).m_82549_(right.m_82490_(k[1])).m_82520_(0.0, k[2], 0.0);
        float damage = PaleRules.fist(KaguneProfile.mastery(player)) * OwlRules.REND_DAMAGE[stage];
        double arc = Math.cos(Math.toRadians(OwlRules.REND_ARC[stage]));
        Vec3 eye = player.m_146892_();
        int landed = 0;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, player.m_20191_().m_82400_(OwlRules.REND_REACH[stage] + 1.0), entity -> PaleBlitz.valid(player, entity))) {
            if (state.hit.contains(body.m_19879_())) {
                continue;
            }
            Vec3 middle = KaguneServerController.center(body);
            Vec3 to = middle.m_82546_(eye);
            if (to.m_82553_() - body.m_20205_() * 0.5 > OwlRules.REND_REACH[stage] || UkakuFire.horizontal(to).m_82526_(facing) < arc
                && body.m_19879_() != state.targetId) {
                continue;
            }
            state.hit.add(body.m_19879_());
            Vec3 cut = middle.m_82546_(facing.m_82490_(body.m_20205_() * 0.5));
            Vec3 along = knock.m_82556_() < 1.0E-6 ? facing : knock.m_82541_();
            body.f_19802_ = 0;
            WoundHits.expect(body, com.hex.kagune.control.wound.WoundBook.Blow.SLASH, cut, along, 0.0);
            if (!body.m_6469_(player.m_269291_().m_269075_(player), damage)) {
                continue;
            }
            landed++;
            UkakuCrystalCombat.rinkakuBleed(body, player, now);
            double resist = Math.max(0.0, Math.min(1.0, body.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
            body.m_20256_(body.m_20184_().m_82490_(0.15).m_82549_(knock.m_82490_(1.0 - 0.75 * resist)));
            body.f_19864_ = true;
            boolean finisher = stage == OwlRules.REND_STAGES - 1;
            PaleStun.stun(body, now, finisher ? com.hex.kagune.control.pale.StunRules.HEAVY_TICKS : com.hex.kagune.control.pale.StunRules.TICKS);
            if (finisher) {
                PaleKnockdown.launched(body, now);
            }
            PaleBlitz.fx(player, S2CPaleFx.IMPACT, body.m_19879_(), cut, along.m_82490_(1.2), finisher ? 2.0F : 1.3F, S2CPaleFx.BONE);
            KaguneServerController.playAt(player, body, HexKaguneSounds.FLESH_IMPACT, 0.9F, KaguneServerController.vary(player, finisher ? 0.6F : 0.85F, 0.06F));
            UkakuFire.sound(level, cut, "entity.player.attack.strong", 0.9F, KaguneServerController.vary(player, finisher ? 0.55F : 0.8F, 0.06F));
        }
        if (stage == OwlRules.REND_STAGES - 1) {
            // 6.2.4: the finisher is its jaws snapping shut on what is before it.
            Vec3 jaws = player.m_20182_().m_82549_(facing.m_82490_(OwlRules.REND_BITE_AHEAD)).m_82520_(0.0, 1.6, 0.0);
            UkakuFire.sound(level, jaws, "entity.generic.eat", 1.2F, KaguneServerController.vary(player, 0.45F, 0.05F));
            UkakuFire.sound(level, jaws, "entity.evoker_fangs.attack", 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        }
        if (landed > 0) {
            KaguneProfile.addMastery(player, "Swipe", 1);
            KaguneNetwork.broadcast(player);
            PaleServer.landed(player, landed);
        }
    }

    // ------------------------------------------------------------------ its Fissure (V held)

    /** V held in the Owl: the Fissure's charge (and kept alive while the client says so); V let go: brought down. */
    static void fissureKey(ServerPlayer player, PaleServer.PaleState state, boolean held, long now) {
        if (!held) {
            if (state.move == com.hex.kagune.control.pale.PaleMove.OWL_FISSURE_CHARGE) {
                bringDown(player, state, now);
            }
            return;
        }
        state.hammerHeardAt = now;
        if (state.move == com.hex.kagune.control.pale.PaleMove.OWL_FISSURE_CHARGE || now < state.hammerReadyAt || !PaleCancel.allowedOwl(state, now)
            || PaleServer.locked(player, state, "Flesh Hammer", PaleRules.HAMMER_MASTERY)) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, PaleRules.HAMMER_START_COST, true)) {
            return;
        }
        PaleCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        PaleServer.begin(player, state, com.hex.kagune.control.pale.PaleMove.OWL_FISSURE_CHARGE, 0, 0, 0.0F, null, now);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.8F, KaguneServerController.vary(player, 0.65F, 0.05F));
    }

    /** Charging: rooted to the spot; let go (or unheard of a while, or held as long as it goes), brought down. */
    static void tickFissureCharge(ServerPlayer player, PaleServer.PaleState state, long now) {
        long held = now - state.startedAt;
        player.m_147207_(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.f_19597_, 4, 4, false, false, false), player);
        if (held == PaleRules.HAMMER_GROW_TICKS) {
            KaguneServerController.playSelf(player, HexKaguneSounds.BONE_CRACK, 0.8F, KaguneServerController.vary(player, 0.5F, 0.04F));
        }
        if (now - state.hammerHeardAt > PaleRules.HOLD_TIMEOUT || held >= PaleRules.HAMMER_MAX_HOLD) {
            bringDown(player, state, now);
        }
    }

    /** Let go: as charged as it got (the least, if she cannot pay for more). */
    private static void bringDown(ServerPlayer player, PaleServer.PaleState state, long now) {
        float charge = PaleRules.hammerCharge(now - state.startedAt);
        if (!KaguneServerController.useEnergy(player, Math.max(0.0F, PaleRules.hammerCost(charge) - PaleRules.HAMMER_START_COST), true)) {
            charge = PaleRules.hammerCharge(0.0);
        }
        PaleServer.begin(player, state, com.hex.kagune.control.pale.PaleMove.OWL_FISSURE, 0, 0, charge, null, now);
        state.hammerReadyAt = now + Cooldowns.of(player, PaleRules.HAMMER_COOLDOWN);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 0.45F, 0.04F));
    }

    /** Brought down: the blow where both blades land, then the split running on along the ground, a little more each tick. */
    static void tickFissure(ServerPlayer player, PaleServer.PaleState state, long now) {
        long age = now - state.startedAt;
        ServerLevel level = player.m_284548_();
        Vec3 facing = facing(state);
        Vec3 start = player.m_20182_().m_82549_(facing.m_82490_(OwlRules.FISSURE_AHEAD));
        if (age == OwlRules.FISSURE_IMPACT) {
            // A heavier crater where both blades come down.
            BlockSmash.crater(player, start, OwlRules.FISSURE_CRATER * 1.5);
            UkakuFire.sound(level, start, "entity.generic.explode", 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
            UkakuFire.sound(level, start, "block.anvil.land", 0.8F, KaguneServerController.vary(player, 0.45F, 0.05F));
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.5F, 0.05F));
        }
        double was = OwlRules.fissureRun(age - 1.0, state.charge);
        double now2 = OwlRules.fissureRun(age, state.charge);
        if (now2 >= 0.0 && now2 > was) {
            for (double along = Math.max(0.0, Math.ceil(Math.max(0.0, was) / OwlRules.FISSURE_STEP) * OwlRules.FISSURE_STEP); along <= now2 + 1.0E-9;
                 along += OwlRules.FISSURE_STEP) {
                if (along <= was && was >= 0.0) {
                    continue;
                }
                Vec3 at = ground(level, player, start.m_82549_(facing.m_82490_(along)));
                BlockSmash.crater(player, at, OwlRules.FISSURE_CRATER);
                net.minecraft.core.BlockPos below = net.minecraft.core.BlockPos.m_274561_(at.f_82479_, at.f_82480_ - 0.01, at.f_82481_);
                PaleBlitz.fx(player, S2CPaleFx.SHOCKWAVE, 0, at, facing, (float)(along < 0.5 ? 2.2 : 1.1),
                    net.minecraft.world.level.block.Block.m_49956_(level.m_8055_(below)));
                if (((int)along & 1) == 0) {
                    UkakuFire.sound(level, at, "block.stone.break", 1.0F, KaguneServerController.vary(player, 0.6F, 0.08F));
                }
            }
            split(player, state, start, facing, Math.max(0.0, was), now2, now);
        }
        if (age >= OwlRules.FISSURE_TICKS) {
            PaleServer.end(player, state);
        }
    }

    /** The surface under {@code at} (a little up or down from it), or {@code at} where there is none near. */
    private static Vec3 ground(ServerLevel level, ServerPlayer player, Vec3 at) {
        net.minecraft.world.phys.BlockHitResult hit = level.m_45547_(new net.minecraft.world.level.ClipContext(at.m_82520_(0.0, 1.5, 0.0), at.m_82520_(0.0, -2.5, 0.0),
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        return hit.m_6662_() == net.minecraft.world.phys.HitResult.Type.BLOCK ? hit.m_82450_() : at;
    }

    /** What stands along the split from {@code from} to {@code to} (blocks on from where the blades came down): thrown up, hurt, stunned (once). */
    private static void split(ServerPlayer player, PaleServer.PaleState state, Vec3 start, Vec3 facing, double from, double to, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 a = start.m_82549_(facing.m_82490_(from - OwlRules.FISSURE_WIDTH * 0.5));
        Vec3 b = start.m_82549_(facing.m_82490_(to + OwlRules.FISSURE_WIDTH * 0.5));
        AABB area = new AABB(Math.min(a.f_82479_, b.f_82479_), Math.min(a.f_82480_, b.f_82480_) - 2.0, Math.min(a.f_82481_, b.f_82481_),
            Math.max(a.f_82479_, b.f_82479_), Math.max(a.f_82480_, b.f_82480_) + 3.0, Math.max(a.f_82481_, b.f_82481_)).m_82400_(OwlRules.FISSURE_WIDTH + 0.5);
        float damage = PaleRules.fist(KaguneProfile.mastery(player)) * OwlRules.FISSURE_DAMAGE * (0.7F + 0.3F * state.charge);
        int landed = 0;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, area, entity -> PaleBlitz.valid(player, entity))) {
            if (state.hit.contains(body.m_19879_())) {
                continue;
            }
            Vec3 feet = body.m_20182_();
            if (distanceToSegment(new Vec3(a.f_82479_, feet.f_82480_, a.f_82481_), new Vec3(b.f_82479_, feet.f_82480_, b.f_82481_), feet)
                > OwlRules.FISSURE_WIDTH + body.m_20205_() * 0.5 || Math.abs(feet.f_82480_ - start.f_82480_) > 2.5) {
                continue;
            }
            state.hit.add(body.m_19879_());
            body.f_19802_ = 0;
            WoundHits.expect(body, com.hex.kagune.control.wound.WoundBook.Blow.SLAM, feet, new Vec3(0.0, 1.0, 0.0), 0.0);
            if (!body.m_6469_(player.m_269291_().m_269075_(player), damage)) {
                continue;
            }
            landed++;
            double resist = Math.max(0.0, Math.min(1.0, body.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
            Vec3 thrown = facing.m_82490_(0.3).m_82520_(0.0, OwlRules.FISSURE_UP * (0.75 + 0.25 * state.charge) * (1.0 - 0.4 * resist), 0.0);
            BlockSmash.launch(player, body, thrown, 30);
            PaleStun.stun(body, now, com.hex.kagune.control.pale.StunRules.HEAVY_TICKS);
            PaleKnockdown.launched(body, now);
            PaleBlitz.fx(player, S2CPaleFx.IMPACT, body.m_19879_(), feet.m_82520_(0.0, 0.3, 0.0), thrown, 1.5F, S2CPaleFx.BIG_PUNCH);
        }
        if (landed > 0) {
            KaguneProfile.addMastery(player, "Swipe", 2);
            KaguneNetwork.broadcast(player);
            PaleServer.landed(player, landed);
        }
    }

    // ------------------------------------------------------------------ its Pounce (M2)

    /** Right click in the Owl (with W, S, A or D, or none: ahead): the pounce, only off the ground. */
    static void pounce(ServerPlayer player, PaleServer.PaleState state, int direction, long now) {
        if (!PaleCancel.allowedOwl(state, now) || now < state.flipReadyAt || !player.m_20096_() || player.m_20069_()) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, OwlRules.LEAP_COST, true)) {
            return;
        }
        PaleCancel.cut(player, state, now);
        PaleRules.Flip flip = PaleRules.Flip.byOrdinal(direction);
        boolean soaring = PaleRevive.hasMutation(player, com.hex.kagune.control.pale.PaleMutation.UP_WE_GO);
        state.flipReadyAt = now + Cooldowns.of(player, OwlRules.LEAP_COOLDOWN);
        state.landedAt = Long.MIN_VALUE;
        state.queuedAt = Long.MIN_VALUE;
        PaleServer.begin(player, state, com.hex.kagune.control.pale.PaleMove.OWL_LEAP, OwlRules.leapDetail(flip, soaring), 0, 0.0F, null, now);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.8F, KaguneServerController.vary(player, 0.6F, 0.05F));
    }

    /** The pounce: the crouch, the bound, the air (no fall hurts her), the slam as she lands, her recovery. */
    static void tickPounce(ServerPlayer player, PaleServer.PaleState state, long now) {
        long age = now - state.startedAt;
        player.f_19789_ = 0.0F;
        ServerLevel level = player.m_284548_();
        PaleRules.Flip flip = PaleRules.Flip.byOrdinal(state.detail & 15);
        if (age == OwlRules.LEAP_CROUCH) {
            double[] launch = OwlRules.leap(flip, (state.detail & 16) != 0);
            double yaw = Math.toRadians(state.yaw + flip.bearing);
            Vec3 motion = new Vec3(-Math.sin(yaw) * launch[0], launch[1], Math.cos(yaw) * launch[0]);
            state.push = motion;
            state.pushTicks = 1;
            UkakuFire.push(player, motion);
            UkakuFire.sound(level, player.m_20182_(), "entity.ender_dragon.flap", 0.8F, KaguneServerController.vary(player, 0.75F, 0.05F));
            UkakuFire.sound(level, player.m_20182_(), "block.gravel.break", 1.0F, KaguneServerController.vary(player, 0.6F, 0.08F));
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.9F, KaguneServerController.vary(player, 0.5F, 0.05F));
        }
        if (age >= OwlRules.LEAP_CROUCH + 3 && state.landedAt == Long.MIN_VALUE && (player.m_20096_() || player.m_20069_())) {
            state.landedAt = now;
            state.push = null;
            state.pushTicks = 0;
            state.charge = (float)age;
            PaleServer.broadcast(player, state);
        }
        if (state.landedAt != Long.MIN_VALUE && now - state.landedAt == OwlRules.LEAP_SLAM_AFTER) {
            slam(player, state, now);
        }
        boolean done = state.landedAt != Long.MIN_VALUE && now - state.landedAt >= OwlRules.LEAP_RECOVER;
        if (done || age > OwlRules.LEAP_MOST) {
            state.push = null;
            state.pushTicks = 0;
            PaleServer.end(player, state);
        }
    }

    /** As she lands: both blades into the ground ahead of her: what is round the blow hurt, cut, thrown up and stunned; craters where they bite. */
    private static void slam(ServerPlayer player, PaleServer.PaleState state, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 facing = facing(state);
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        Vec3 feet = player.m_20182_();
        Vec3 at = feet.m_82549_(facing.m_82490_(OwlRules.LEAP_SLAM_AHEAD));
        float damage = PaleRules.fist(KaguneProfile.mastery(player)) * OwlRules.LEAP_DAMAGE;
        int landed = 0;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, new AABB(at.f_82479_ - OwlRules.LEAP_SLAM_REACH, at.f_82480_ - 1.5,
            at.f_82481_ - OwlRules.LEAP_SLAM_REACH, at.f_82479_ + OwlRules.LEAP_SLAM_REACH, at.f_82480_ + 2.5, at.f_82481_ + OwlRules.LEAP_SLAM_REACH),
            entity -> PaleBlitz.valid(player, entity))) {
            Vec3 off = body.m_20182_().m_82546_(at);
            double flat = Math.sqrt(off.f_82479_ * off.f_82479_ + off.f_82481_ * off.f_82481_) - body.m_20205_() * 0.5;
            if (flat > OwlRules.LEAP_SLAM_REACH) {
                continue;
            }
            Vec3 away = flat < 0.2 ? facing : UkakuFire.horizontal(off);
            body.f_19802_ = 0;
            WoundHits.expect(body, com.hex.kagune.control.wound.WoundBook.Blow.SLASH, KaguneServerController.center(body), away.m_82520_(0.0, -0.5, 0.0), 0.0);
            if (!body.m_6469_(player.m_269291_().m_269075_(player), damage)) {
                continue;
            }
            landed++;
            UkakuCrystalCombat.rinkakuBleed(body, player, now);
            double resist = Math.max(0.0, Math.min(1.0, body.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
            BlockSmash.launch(player, body, away.m_82490_(0.55 * (1.0 - 0.6 * resist)).m_82520_(0.0, 0.5 * (1.0 - 0.4 * resist), 0.0), 24);
            PaleStun.stun(body, now, com.hex.kagune.control.pale.StunRules.HEAVY_TICKS);
            PaleBlitz.fx(player, S2CPaleFx.IMPACT, body.m_19879_(), KaguneServerController.center(body), away.m_82520_(0.0, 0.6, 0.0), 1.6F, S2CPaleFx.BONE);
        }
        for (double side : new double[] {-1.0, 1.0}) {
            Vec3 bite = at.m_82549_(right.m_82490_(side * OwlRules.REND_BITE_SIDE));
            BlockSmash.crater(player, bite, OwlRules.LEAP_CRATER);
        }
        net.minecraft.core.BlockPos below = net.minecraft.core.BlockPos.m_274561_(at.f_82479_, at.f_82480_ - 0.01, at.f_82481_);
        PaleBlitz.fx(player, S2CPaleFx.SHOCKWAVE, 0, at, facing, (float)OwlRules.LEAP_SLAM_REACH, net.minecraft.world.level.block.Block.m_49956_(level.m_8055_(below)));
        UkakuFire.sound(level, at, "entity.generic.explode", 0.8F, KaguneServerController.vary(player, 0.75F, 0.05F));
        UkakuFire.sound(level, at, "block.anvil.land", 0.7F, KaguneServerController.vary(player, 0.55F, 0.05F));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        if (landed > 0) {
            KaguneProfile.addMastery(player, "Swipe", 2);
            KaguneNetwork.broadcast(player);
            PaleServer.landed(player, landed);
        }
    }

    // ------------------------------------------------------------------ its Reaping Whirl (J tapped)

    /** J tapped in the Owl: the whirl (on the Flesh Swipe's cooldown). */
    static void whirl(ServerPlayer player, PaleServer.PaleState state, long now) {
        if (!PaleCancel.allowedOwl(state, now) || now < state.swipeReadyAt || PaleServer.locked(player, state, "Flesh Swipe", PaleRules.SWIPE_MASTERY)) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, OwlRules.WHIRL_COST, true)) {
            return;
        }
        PaleCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        PaleServer.begin(player, state, com.hex.kagune.control.pale.PaleMove.OWL_WHIRL, 0, 0, 0.0F, null, now);
        state.swipeReadyAt = now + Cooldowns.of(player, PaleRules.SWIPE_COOLDOWN);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.7F, KaguneServerController.vary(player, 0.75F, 0.05F));
    }

    /**
     * The whirl in flight: each tick of the turn, both blades (out to her right and her left) sweep round the way she turns; what
     * they pass over within their reach this tick is reaped (once a whirl): the hurt, a bleed, thrown out from her and on round.
     */
    static void tickWhirl(ServerPlayer player, PaleServer.PaleState state, long now) {
        long age = now - state.startedAt;
        ServerLevel level = player.m_284548_();
        if (age == OwlRules.WHIRL_START) {
            UkakuFire.sound(level, player.m_20182_(), "entity.player.attack.sweep", 1.2F, KaguneServerController.vary(player, 0.45F, 0.05F));
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 0.5F, 0.05F));
            PaleBlitz.fx(player, S2CPaleFx.IMPACT, 0, player.m_20182_().m_82520_(0.0, 1.0, 0.0), new Vec3(0.0, 1.0, 0.0), 1.8F, S2CPaleFx.OWL_WHIRL);
        }
        if (age > OwlRules.WHIRL_START && age <= OwlRules.WHIRL_END + 1) {
            double from = OwlRules.whirlTurn(age - 1.0);
            double to = OwlRules.whirlTurn(age);
            if (to > from) {
                reap(player, state, from, to, now);
            }
        }
        if (age >= OwlRules.WHIRL_TICKS) {
            PaleServer.end(player, state);
        }
    }

    /** What the blades passed over as she turned from {@code from} to {@code to} (radians round, + to her left). */
    private static void reap(ServerPlayer player, PaleServer.PaleState state, double from, double to, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 facing = facing(state);
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        Vec3 middle = player.m_20182_().m_82520_(0.0, 1.0, 0.0);
        float damage = PaleRules.fist(KaguneProfile.mastery(player)) * OwlRules.WHIRL_DAMAGE;
        int landed = 0;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, player.m_20191_().m_82400_(OwlRules.WHIRL_REACH + 1.0), entity -> PaleBlitz.valid(player, entity))) {
            if (state.hit.contains(body.m_19879_())) {
                continue;
            }
            Vec3 off = KaguneServerController.center(body).m_82546_(middle);
            double flat = Math.sqrt(off.f_82479_ * off.f_82479_ + off.f_82481_ * off.f_82481_) - body.m_20205_() * 0.5;
            if (flat > OwlRules.WHIRL_REACH || flat + body.m_20205_() < OwlRules.WHIRL_NEAR || off.f_82480_ < -1.6 || off.f_82480_ > 2.4) {
                continue;
            }
            // Its angle round her, as her turn counts it (0 out to her right, + round to her left): either blade over it this tick.
            double angle = Math.atan2(off.m_82526_(facing), off.m_82526_(right));
            if (!swept(angle, from, to) && !swept(angle + Math.PI, from, to)) {
                continue;
            }
            state.hit.add(body.m_19879_());
            Vec3 out = UkakuFire.horizontal(off);
            Vec3 along = new Vec3(-out.f_82481_, 0.0, out.f_82479_).m_82490_(-1.0);
            Vec3 cut = KaguneServerController.center(body).m_82546_(out.m_82490_(body.m_20205_() * 0.5));
            body.f_19802_ = 0;
            WoundHits.expect(body, com.hex.kagune.control.wound.WoundBook.Blow.SLASH, cut, along, 0.0);
            if (!body.m_6469_(player.m_269291_().m_269075_(player), damage)) {
                continue;
            }
            landed++;
            UkakuCrystalCombat.rinkakuBleed(body, player, now);
            double resist = Math.max(0.0, Math.min(1.0, body.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
            Vec3 thrown = out.m_82490_(OwlRules.WHIRL_OUT).m_82549_(along.m_82490_(OwlRules.WHIRL_ALONG)).m_82520_(0.0, OwlRules.WHIRL_UP, 0.0)
                .m_82490_(1.0 - 0.6 * resist);
            BlockSmash.launch(player, body, thrown, 24);
            PaleStun.stun(body, now, com.hex.kagune.control.pale.StunRules.TICKS);
            PaleBlitz.fx(player, S2CPaleFx.IMPACT, body.m_19879_(), cut, along.m_82490_(1.2), 1.5F, S2CPaleFx.BONE);
            KaguneServerController.playAt(player, body, HexKaguneSounds.FLESH_IMPACT, 0.9F, KaguneServerController.vary(player, 0.8F, 0.06F));
        }
        if (landed > 0) {
            KaguneProfile.addMastery(player, "Swipe", 2);
            KaguneNetwork.broadcast(player);
            PaleServer.landed(player, landed);
        }
    }

    /** Whether {@code angle} lies in ({@code from}, {@code to}], turns round counted alike. */
    static boolean swept(double angle, double from, double to) {
        double turn = Math.PI * 2.0;
        double a = angle - turn * Math.floor((angle - from) / turn);
        return a > from && a <= to || a + turn <= to;
    }

    // ------------------------------------------------------------------ its bone spikes (J held)

    /**
     * J held in the Owl: she hunches and the spikes burst out of her back all round her ({@link OwlSpikes}); held out while J is (at
     * most {@link OwlRules#SPIKES_HOLD} once all out); J let go (or unheard of a while), drawn back in.
     */
    static void spikesKey(ServerPlayer player, PaleServer.PaleState state, boolean held, long now) {
        if (!held) {
            letGoSpikes(player, state, now);
            return;
        }
        state.grabHeardAt = now;
        if (state.move == com.hex.kagune.control.pale.PaleMove.OWL_SPIKES || !PaleCancel.allowedOwl(state, now) || now < state.grabReadyAt
            || PaleServer.locked(player, state, "Flesh Grab", PaleRules.GRAB_MASTERY)) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, OwlRules.SPIKES_COST, true)) {
            return;
        }
        PaleCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        int seed = (int)(now * 31L + player.m_19879_());
        PaleServer.begin(player, state, com.hex.kagune.control.pale.PaleMove.OWL_SPIKES, seed, 0, 0.0F, null, now);
        state.grabReadyAt = now + Cooldowns.of(player, PaleRules.GRAB_COOLDOWN);
        // Hunched over: the flesh of her back straining.
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.9F, KaguneServerController.vary(player, 0.55F, 0.05F));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 0.8F, KaguneServerController.vary(player, 0.5F, 0.05F));
    }

    /** Let go: drawn back in from now (or from when they are all out), every client told. */
    private static void letGoSpikes(ServerPlayer player, PaleServer.PaleState state, long now) {
        if (state.move != com.hex.kagune.control.pale.PaleMove.OWL_SPIKES || state.charge > 0.0F) {
            return;
        }
        state.charge = Math.max(1.0F, (float)(now - state.startedAt));
        PaleServer.broadcast(player, state);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_DISMISS, 0.9F, KaguneServerController.vary(player, 0.6F, 0.05F));
    }

    /** The spikes out: each one, as it bursts out, hurting what it reaches (once each burst); what is run through held on it. */
    static void tickSpikes(ServerPlayer player, PaleServer.PaleState state, long now) {
        long age = now - state.startedAt;
        OwlSpikes.Spike[] spikes = OwlSpikes.of(state.detail);
        double allOut = OwlSpikes.allOut(spikes);
        if (state.charge <= 0.0F && (now - state.grabHeardAt > PaleRules.HOLD_TIMEOUT && age > allOut || age > allOut + OwlRules.SPIKES_HOLD)) {
            letGoSpikes(player, state, now);
        }
        player.m_147207_(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.f_19597_, 4, 4, false, false, false), player);
        ServerLevel level = player.m_284548_();
        Vec3 feet = player.m_20182_();
        double yaw = Math.toRadians(state.yaw);
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        float damage = PaleRules.fist(KaguneProfile.mastery(player)) * OwlRules.SPIKES_DAMAGE;
        for (OwlSpikes.Spike spike : spikes) {
            if (age >= spike.erupt() - 1.0 && age < spike.erupt()) {
                // The crack of it bursting out of her.
                if ((spike.seed() & 1) == 0) {
                    KaguneServerController.playSelf(player, HexKaguneSounds.BONE_CRACK, 0.8F, KaguneServerController.vary(player, 0.7F + 0.1F * (spike.seed() & 3), 0.05F));
                }
            }
            double out = OwlSpikes.out(spike, age, state.charge);
            if (out < 0.25) {
                continue;
            }
            double[] r = OwlSpikes.root(spike);
            double[] t = OwlSpikes.tip(spike, r, out);
            Vec3 root = world(feet, right, facing, r);
            Vec3 tip = world(feet, right, facing, t);
            Vec3 along = tip.m_82546_(root);
            if (along.m_82556_() < 1.0E-6) {
                continue;
            }
            AABB reach = new AABB(Math.min(root.f_82479_, tip.f_82479_), Math.min(root.f_82480_, tip.f_82480_), Math.min(root.f_82481_, tip.f_82481_),
                Math.max(root.f_82479_, tip.f_82479_), Math.max(root.f_82480_, tip.f_82480_), Math.max(root.f_82481_, tip.f_82481_)).m_82400_(1.5);
            for (LivingEntity body : level.m_6443_(LivingEntity.class, reach, entity -> PaleBlitz.valid(player, entity))) {
                Vec3 middle = chest(body);
                double off = distanceToSegment(root, tip, middle);
                double half = body.m_20205_() * 0.5;
                boolean held = state.hit.contains(body.m_19879_());
                if (held) {
                    if (off <= half * OwlRules.SPIKES_THROUGH + spike.radius() && state.charge <= 0.0F) {
                        pin(player, body, now); // run through: held on it while it stays out
                    }
                    continue;
                }
                if (off > half + spike.radius() + OwlRules.SPIKES_REACH || !crosses(body.m_20191_().m_82400_(spike.radius()), root, tip)
                    && off > half * OwlRules.SPIKES_THROUGH) {
                    continue;
                }
                spiked(player, state, body, middle, along.m_82541_(), off <= half * OwlRules.SPIKES_THROUGH + spike.radius(), damage, now);
            }
        }
        if (state.charge > 0.0F && age >= OwlSpikes.allIn(spikes, state.charge)) {
            PaleServer.end(player, state);
        }
    }

    /** One spike in it: the hurt, a bleed; run through (held on it, blood out of its back) or knocked aside and staggered. */
    private static void spiked(ServerPlayer player, PaleServer.PaleState state, LivingEntity body, Vec3 middle, Vec3 along, boolean through, float damage,
                               long now) {
        state.hit.add(body.m_19879_());
        body.f_19802_ = 0;
        WoundHits.expect(body, com.hex.kagune.control.wound.WoundBook.Blow.STAB, middle.m_82546_(along.m_82490_(body.m_20205_() * 0.5)), along, 0.0);
        if (!body.m_6469_(player.m_269291_().m_269075_(player), through ? damage : damage * 0.7F)) {
            return;
        }
        UkakuCrystalCombat.rinkakuBleed(body, player, now);
        ServerLevel level = player.m_284548_();
        if (through) {
            pin(player, body, now);
            PaleStun.stun(body, now, 12);
            PaleBlitz.fx(player, S2CPaleFx.IMPALED, body.m_19879_(), middle, along, body.m_20206_(), 0);
            UkakuFire.sound(level, middle, "entity.slime.squish", 1.0F, KaguneServerController.vary(player, 0.65F, 0.05F));
        } else {
            Vec3 knock = new Vec3(along.f_82479_, 0.0, along.f_82481_);
            knock = knock.m_82556_() < 1.0E-6 ? Vec3.f_82478_ : knock.m_82541_().m_82490_(0.7).m_82520_(0.0, 0.25, 0.0);
            body.m_20256_(body.m_20184_().m_82490_(0.3).m_82549_(knock));
            body.f_19864_ = true;
            PaleStun.stun(body, now, 10);
            PaleBlitz.fx(player, S2CPaleFx.IMPACT, body.m_19879_(), middle, along, 1.2F, S2CPaleFx.BONE);
        }
        UkakuFire.sound(level, middle, "entity.player.attack.strong", 0.9F, KaguneServerController.vary(player, 0.7F, 0.06F));
        KaguneServerController.playAt(player, body, HexKaguneSounds.FLESH_IMPACT, 0.9F, KaguneServerController.vary(player, 0.8F, 0.06F));
        hit(player);
    }

    /** A point given in her frame (blocks: x to her right, y up, z ahead) as it is in the world. */
    static Vec3 world(Vec3 feet, Vec3 right, Vec3 facing, double[] p) {
        return feet.m_82549_(right.m_82490_(p[0])).m_82549_(facing.m_82490_(p[2])).m_82520_(0.0, p[1], 0.0);
    }

    /** Whether the segment {@code a}-{@code b} passes through {@code box} (anywhere along it). */
    static boolean crosses(AABB box, Vec3 a, Vec3 b) {
        double[] from = {a.f_82479_, a.f_82480_, a.f_82481_};
        double[] way = {b.f_82479_ - a.f_82479_, b.f_82480_ - a.f_82480_, b.f_82481_ - a.f_82481_};
        double[] min = {box.f_82288_, box.f_82289_, box.f_82290_};
        double[] max = {box.f_82291_, box.f_82292_, box.f_82293_};
        double enter = 0.0;
        double leave = 1.0;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(way[i]) < 1.0E-12) {
                if (from[i] < min[i] || from[i] > max[i]) {
                    return false;
                }
                continue;
            }
            double t0 = (min[i] - from[i]) / way[i];
            double t1 = (max[i] - from[i]) / way[i];
            enter = Math.max(enter, Math.min(t0, t1));
            leave = Math.min(leave, Math.max(t0, t1));
            if (enter > leave) {
                return false;
            }
        }
        return true;
    }

    /** How far {@code p} is from the segment {@code a}-{@code b}. */
    static double distanceToSegment(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.m_82546_(a);
        double t = Math.max(0.0, Math.min(1.0, p.m_82546_(a).m_82526_(ab) / Math.max(1.0E-9, ab.m_82556_())));
        return a.m_82549_(ab.m_82490_(t)).m_82554_(p);
    }

    // ------------------------------------------------------------------ its Speed Kick (V tapped)

    /**
     * V tapped in the Owl: the body closest to her crosshair, in front of her and in her sight, within the Speed Kick's range, with
     * room to plant in front of it ({@link OwlRules#KICK_STAND}) and a clear way there. With none, only "No target found" (no move, no
     * RC, no cooldown). Otherwise she dashes there, all but unseen (her own client moves her, {@code PaleClient}), and it is held where
     * it is until her heel lands ({@link #tickKick}).
     */
    static void kick(ServerPlayer player, PaleServer.PaleState state, long now) {
        if (!PaleCancel.allowedOwl(state, now) || now < state.kickReadyAt || PaleServer.locked(player, state, "Speed Kick", PaleRules.KICK_MASTERY)) {
            return;
        }
        PaleMoves.KickPlan plan = kickPlan(player);
        if (plan == null) {
            PaleServer.notice(player, state, net.minecraft.network.chat.Component.m_237115_("message.hexkagune.pale.kick_no_target"));
            return;
        }
        if (!KaguneServerController.useEnergy(player, PaleRules.KICK_COST, true)) {
            return;
        }
        PaleCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        PaleServer.begin(player, state, com.hex.kagune.control.pale.PaleMove.OWL_KICK, plan.dash(), plan.target().m_19879_(), 0.0F, plan.spot(), now, plan.yaw());
        state.kickReadyAt = now + Cooldowns.of(player, PaleRules.KICK_COOLDOWN);
        pin(player, plan.target(), now);
        // Off the mark: the ground torn up, the air split.
        ServerLevel level = player.m_284548_();
        UkakuFire.sound(level, player.m_20182_(), "item.trident.riptide_2", 1.0F, KaguneServerController.vary(player, 1.1F, 0.06F));
        UkakuFire.sound(level, player.m_20182_(), "block.gravel.break", 1.0F, KaguneServerController.vary(player, 0.6F, 0.08F));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.8F, KaguneServerController.vary(player, 0.5F, 0.05F));
    }

    /** The body the Owl's kick goes for (closest to her crosshair), and how; null when there is none she can reach. */
    static PaleMoves.KickPlan kickPlan(ServerPlayer player) {
        ServerLevel level = player.m_284548_();
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        double cone = Math.cos(Math.toRadians(PaleRules.KICK_CONE));
        AABB area = player.m_20191_().m_82400_(OwlRules.KICK_RANGE);
        PaleMoves.KickPlan best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : level.m_6443_(LivingEntity.class, area, entity -> PaleBlitz.valid(player, entity))) {
            Vec3 to = candidate.m_146892_().m_82546_(eye);
            double distance = to.m_82553_();
            if (distance > OwlRules.KICK_RANGE || distance < 1.0E-3) {
                continue;
            }
            double facing = to.m_82526_(look) / distance;
            if (facing < cone || to.f_82479_ * look.f_82479_ + to.f_82481_ * look.f_82481_ <= 0.0) {
                continue;
            }
            double score = Math.acos(Math.min(1.0, facing)) + distance * 0.004;
            if (score >= bestScore || !PaleMoves.seen(level, player, eye, candidate)) {
                continue;
            }
            PaleMoves.KickPlan plan = plan(player, candidate);
            if (plan != null) {
                best = plan;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * Where she plants to kick {@code target}: {@link OwlRules#KICK_STAND} in front of its middle, toward where she is, on the ground
     * it stands on (or where she is, if she is closer than that already); null when she doesn't fit there or the way is blocked.
     */
    static PaleMoves.KickPlan plan(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.m_284548_();
        Vec3 from = player.m_20182_();
        Vec3 at = target.m_20182_();
        Vec3 facing = UkakuFire.horizontal(at.m_82546_(from));
        if (facing.m_82556_() < 1.0E-6) {
            facing = UkakuFire.horizontal(player.m_20154_());
        }
        double stand = OwlRules.KICK_STAND + target.m_20205_() * 0.5 - 0.3;
        Vec3 spot = new Vec3(at.f_82479_ - facing.f_82479_ * stand, at.f_82480_, at.f_82481_ - facing.f_82481_ * stand);
        Vec3 offset = spot.m_82546_(from);
        AABB there = player.m_20191_().m_82386_(offset.f_82479_, offset.f_82480_, offset.f_82481_);
        if (!level.m_45756_(player, there)) {
            return null;
        }
        for (double up : new double[] {0.6, 1.5}) {
            if (!PaleMoves.clear(level, player, from.m_82520_(0.0, up, 0.0), spot.m_82520_(0.0, up, 0.0))) {
                return null;
            }
        }
        int dash = OwlRules.kickDash(offset.m_82553_());
        float yaw = (float)Math.toDegrees(Math.atan2(-facing.f_82479_, facing.f_82481_));
        return new PaleMoves.KickPlan(target, spot, dash, 0.0, yaw);
    }

    /**
     * The Owl's kick in flight: what she kicks is held where it is until her heel lands; the skewer, the slash, the blade drawn out,
     * the heel, each on its own tick after the dash; done once she is herself again.
     */
    static void tickKick(ServerPlayer player, PaleServer.PaleState state, long now) {
        long age = now - state.startedAt;
        int dash = Math.max(1, state.detail);
        player.f_19789_ = 0.0F;
        LivingEntity target = state.targetId == 0 ? null
            : player.m_9236_().m_6815_(state.targetId) instanceof LivingEntity living && PaleBlitz.valid(player, living) ? living : null;
        long since = age - dash;
        boolean there = Math.hypot(player.m_20185_() - state.point.f_82479_, player.m_20189_() - state.point.f_82481_) <= OwlRules.KICK_THERE;
        if (target != null && since < OwlRules.KICK_BLOW) {
            pin(player, target, now);
        }
        ServerLevel level = player.m_284548_();
        if (since == 0) {
            // She plants: the ground under her takes it.
            UkakuFire.sound(level, player.m_20182_(), "entity.generic.big_fall", 0.7F, KaguneServerController.vary(player, 1.1F, 0.06F));
        }
        if (since == OwlRules.KICK_SKEWER - 1 || since == OwlRules.KICK_SLASH - 1) {
            UkakuFire.sound(level, player.m_20182_(), "entity.player.attack.sweep", 0.9F, KaguneServerController.vary(player, 0.55F, 0.05F));
        }
        if (target != null && there) {
            if (since == OwlRules.KICK_SKEWER) {
                skewer(player, state, target, now);
            } else if (since == OwlRules.KICK_SLASH) {
                slash(player, state, target, now);
            } else if (since == OwlRules.KICK_PULL && state.hit.contains(target.m_19879_())) {
                // Drawn back out of it: the blood follows the blade.
                Vec3 facing = facing(state);
                PaleBlitz.fx(player, S2CPaleFx.IMPACT, target.m_19879_(), chest(target), facing.m_82490_(-1.0), 0.8F, S2CPaleFx.BONE);
                UkakuFire.sound(level, chest(target), "entity.slime.squish", 0.9F, KaguneServerController.vary(player, 0.6F, 0.05F));
            } else if (since == OwlRules.KICK_BLOW) {
                heel(player, state, target, now);
            }
        }
        if (since == OwlRules.KICK_BLOW - 2) {
            UkakuFire.sound(level, player.m_20182_(), "entity.player.attack.sweep", 1.0F, KaguneServerController.vary(player, 0.45F, 0.05F));
        }
        if (since >= OwlRules.KICK_TICKS) {
            PaleServer.end(player, state);
        }
    }

    /** The right giant arm's blade straight through its middle: the hurt, a bleed, blood out of its back, a hole through it. */
    private static void skewer(ServerPlayer player, PaleServer.PaleState state, LivingEntity target, long now) {
        Vec3 facing = facing(state);
        Vec3 middle = chest(target);
        target.f_19802_ = 0;
        WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.STAB, middle.m_82546_(facing.m_82490_(target.m_20205_() * 0.5)), facing, 0.0);
        float damage = PaleRules.fist(KaguneProfile.mastery(player)) * OwlRules.KICK_SKEWER_DAMAGE;
        if (!target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            return;
        }
        state.hit.add(target.m_19879_());
        UkakuCrystalCombat.rinkakuBleed(target, player, now);
        PaleStun.stun(target, now, OwlRules.KICK_BLOW - OwlRules.KICK_SKEWER + 4);
        PaleBlitz.fx(player, S2CPaleFx.IMPALED, target.m_19879_(), middle, facing, target.m_20206_(), 1);
        ServerLevel level = player.m_284548_();
        UkakuFire.sound(level, middle, "entity.player.attack.strong", 1.1F, KaguneServerController.vary(player, 0.55F, 0.05F));
        UkakuFire.sound(level, middle, "entity.slime.squish", 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        KaguneServerController.playAt(player, target, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
        hit(player);
    }

    /** The left giant arm's blade across its neck, from her left to her right: the hurt, a bleed, blood thrown the way it cut. */
    private static void slash(ServerPlayer player, PaleServer.PaleState state, LivingEntity target, long now) {
        Vec3 facing = facing(state);
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        Vec3 neck = target.m_146892_().m_82520_(0.0, -0.2, 0.0);
        target.f_19802_ = 0;
        WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.SLASH, neck.m_82546_(right.m_82490_(target.m_20205_() * 0.5)), right, 0.0);
        float damage = PaleRules.fist(KaguneProfile.mastery(player)) * OwlRules.KICK_SLASH_DAMAGE;
        if (!target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            return;
        }
        UkakuCrystalCombat.rinkakuBleed(target, player, now);
        PaleStun.stun(target, now, OwlRules.KICK_BLOW - OwlRules.KICK_SLASH + 4);
        PaleBlitz.fx(player, S2CPaleFx.IMPACT, target.m_19879_(), neck, right.m_82490_(1.2), 1.3F, S2CPaleFx.BONE);
        KaguneServerController.playAt(player, target, HexKaguneSounds.FLESH_IMPACT, 1.0F, KaguneServerController.vary(player, 0.8F, 0.06F));
        hit(player);
    }

    /** The spinning heel: the hurt, and it is sent flying straight away from her and up, through weak blocks; it lies there a moment. */
    private static void heel(ServerPlayer player, PaleServer.PaleState state, LivingEntity target, long now) {
        Vec3 facing = facing(state);
        Vec3 struck = chest(target).m_82546_(facing.m_82490_(target.m_20205_() * 0.5));
        target.f_19802_ = 0;
        WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.BLUNT, struck, facing, 0.0);
        float damage = PaleRules.fist(KaguneProfile.mastery(player)) * OwlRules.KICK_BLOW_DAMAGE;
        if (!target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            return;
        }
        double resist = Math.max(0.0, Math.min(1.0, target.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
        Vec3 launch = facing.m_82490_(OwlRules.KICK_FLIGHT * (1.0 - OwlRules.KICK_RESIST * resist)).m_82520_(0.0, OwlRules.KICK_UP, 0.0);
        BlockSmash.launch(player, target, launch, 40);
        PaleKnockdown.launched(target, now);
        PaleBlitz.fx(player, S2CPaleFx.KICKED, target.m_19879_(), struck, launch, 1.0F, 0);
        PaleBlitz.fx(player, S2CPaleFx.IMPACT, target.m_19879_(), struck, launch, 2.2F, S2CPaleFx.BIG_PUNCH);
        ServerLevel level = player.m_284548_();
        UkakuFire.sound(level, struck, "entity.player.attack.strong", 1.3F, KaguneServerController.vary(player, 0.5F, 0.04F));
        UkakuFire.sound(level, struck, "entity.player.attack.knockback", 1.1F, KaguneServerController.vary(player, 0.55F, 0.05F));
        KaguneServerController.playAt(player, target, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.65F, 0.05F));
        hit(player);
    }

    /** Held where it is, slowed to nothing, unable to act: it never sees her coming. */
    private static void pin(ServerPlayer player, LivingEntity body, long now) {
        body.m_20256_(new Vec3(0.0, Math.min(0.0, body.m_20184_().f_82480_), 0.0));
        body.f_19864_ = true;
        body.m_147207_(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.f_19597_, 3, 6, false, false, false), player);
        PaleStun.stun(body, now, 2);
    }

    /** The way the move faces (level). */
    static Vec3 facing(PaleServer.PaleState state) {
        double yaw = Math.toRadians(state.yaw);
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    /** Its middle, a little over half its height up. */
    static Vec3 chest(LivingEntity body) {
        return body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.55, 0.0);
    }

    /** A blow of the Owl's landed: mastery, her RC told, bloodlust fed. */
    private static void hit(ServerPlayer player) {
        KaguneProfile.addMastery(player, "Swipe", 2);
        KaguneNetwork.broadcast(player);
        PaleServer.landed(player, 1);
    }

    /**
     * The Owl's Big Punch lands (on {@link PaleRules#PUNCH_IMPACT}, out of the blink in front of it as the Big Punch's): her fist and
     * the right giant arm's elbow driven into it together. It flies off dead level along the blow ({@link BlockSmash#drive}: the
     * way she faced as she struck, never up or down), as far as the charge sends it, through whatever is in its way near her; round
     * the blow the air itself is thrown, level, along it.
     */
    static void punch(ServerPlayer player, PaleServer.PaleState state, LivingEntity target, long now) {
        ServerLevel level = player.m_284548_();
        float charge = state.charge;
        double yaw = Math.toRadians(state.yaw);
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 fist = player.m_20182_().m_82520_(facing.f_82479_ * 1.3, 1.0, facing.f_82481_ * 1.3);
        boolean reached = target != null && KaguneServerController.center(target).m_82554_(player.m_146892_()) - target.m_20205_() * 0.5 <= 4.0;
        int mastery = KaguneProfile.mastery(player);
        float damage = PaleRules.punchDamage(mastery, charge) * OwlRules.PUNCH_DAMAGE;
        if (reached) {
            Vec3 centre = KaguneServerController.center(target);
            fist = new Vec3(centre.f_82479_ - facing.f_82479_ * target.m_20205_() * 0.5, target.m_20186_() + target.m_20206_() * 0.45,
                centre.f_82481_ - facing.f_82481_ * target.m_20205_() * 0.5);
            target.f_19802_ = 0;
            WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.BLUNT, fist, facing, 0.0);
            if (target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
                double resist = Math.max(0.0, Math.min(1.0, target.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
                double distance = OwlRules.punchDistance(charge) * (1.0 - OwlRules.PUNCH_RESIST * resist);
                BlockSmash.drive(player, target, facing, distance, OwlRules.punchSpeed(charge), damage);
                PaleStun.stun(target, now, 3);
                KaguneProfile.addMastery(player, "Swipe", 2);
                KaguneNetwork.broadcast(player);
                PaleServer.landed(player, 1);
            }
        }
        // The air thrown: everything else close round the blow and ahead of it is knocked back, level, along the blow.
        AABB around = new AABB(fist.f_82479_ - 4.0, fist.f_82480_ - 2.0, fist.f_82481_ - 4.0, fist.f_82479_ + 4.0, fist.f_82480_ + 2.5, fist.f_82481_ + 4.0);
        for (LivingEntity other : level.m_6443_(LivingEntity.class, around, entity -> PaleBlitz.valid(player, entity) && entity != target)) {
            Vec3 away = UkakuFire.horizontal(other.m_20182_().m_82546_(player.m_20182_()));
            double ahead = away.m_82526_(facing);
            if (ahead < 0.3 || KaguneServerController.center(other).m_82554_(fist) > 4.2) {
                continue;
            }
            other.f_19802_ = 0;
            if (other.m_6469_(player.m_269291_().m_269075_(player), damage * 0.3F)) {
                Vec3 blown = facing.m_82490_(1.0 + charge).m_82549_(away.m_82490_(0.4));
                BlockSmash.launch(player, other, new Vec3(blown.f_82479_, 0.12, blown.f_82481_), 20);
            }
        }
        PaleBlitz.fx(player, S2CPaleFx.IMPACT, reached ? target.m_19879_() : 0, fist, facing.m_82490_(OwlRules.punchSpeed(charge)),
            reached ? 2.4F + 1.2F * charge : 1.4F + charge, S2CPaleFx.OWL_PUNCH);
        UkakuFire.sound(level, fist, "entity.generic.explode", 0.9F + 0.4F * charge, KaguneServerController.vary(player, 0.9F, 0.05F));
        UkakuFire.sound(level, fist, "entity.iron_golem.damage", 1.0F, KaguneServerController.vary(player, 0.55F, 0.05F));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        if (reached) {
            KaguneServerController.playAt(player, target, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        }
    }
}

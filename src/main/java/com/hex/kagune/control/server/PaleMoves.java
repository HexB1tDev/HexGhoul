package com.hex.kagune.control.server;

import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleMutation;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.PaleStrike;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Pale's flips (M2), Big Punch, Speed Kick and Flesh Hammer (6.1.0).
 *
 * <p>A flip ({@link PaleRules.Flip}): right click with W, S, A or D (or nothing: forward) throws her off the ground in a huge
 * front, back or side flip; her whole body turns over once in the air ({@code PaleMotions.flip}) and she lands on her feet, no fall
 * damage taken. Only from the ground, and out of a strike once its blow has landed (a flip away is how a blitz ends safely).
 */
final class PaleMoves {
    private PaleMoves() {
    }

    static void flip(ServerPlayer player, PaleServer.PaleState state, int direction, long now) {
        // 6.2.0: out of anything (PaleCancel), but only off the ground.
        if (!PaleCancel.allowed(state, now) || now < state.flipReadyAt || !player.m_20096_() || player.m_20069_()) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, PaleRules.FLIP_COST, true)) {
            return;
        }
        PaleCancel.cut(player, state, now);
        PaleRules.Flip flip = PaleRules.Flip.byOrdinal(direction);
        double yaw = Math.toRadians(player.m_146908_() + flip.bearing);
        // 7.0.1: Up We Go is her mutation, and the eyes alone's once its quest is done.
        boolean soaring = PaleRevive.hasMutation(player, PaleMutation.UP_WE_GO)
            || PaleServer.eyes(player) && QuestServer.open(player, com.hex.kagune.control.quest.Gate.EYES_SOAR);
        state.soaring = soaring;
        if (soaring) {
            // 6.2.0, Up We Go: the launch builds up over a few ticks, each setting her speed higher (soarTick).
            state.push = null;
            state.pushTicks = 0;
            state.soarFlip = flip.ordinal();
            state.soarYaw = yaw;
            state.soarTick = 0;
            soarTick(player, state);
        } else {
            Vec3 motion = new Vec3(-Math.sin(yaw) * flip.speed, flip.up, Math.cos(yaw) * flip.speed);
            state.push = motion;
            state.pushTicks = PaleRules.FLIP_PUSH_TICKS - 1;
            UkakuFire.push(player, motion);
        }
        state.flipReadyAt = now + Cooldowns.of(player, PaleRules.FLIP_COOLDOWN);
        state.landedAt = Long.MIN_VALUE;
        state.lastStrike = null;
        state.queuedAt = Long.MIN_VALUE;
        PaleServer.begin(player, state, PaleMove.FLIP, flip.ordinal(), 0, soaring ? 1.0F : 0.0F, null, now);
        QuestServer.signal(player, com.hex.kagune.control.quest.Signal.FLIP, 1);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.55F, KaguneServerController.vary(player, 0.85F, 0.06F));
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.attack.sweep", 0.35F, KaguneServerController.vary(player, 0.7F, 0.05F));
        if (soaring) {
            UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.ender_dragon.flap", 0.5F, KaguneServerController.vary(player, 1.3F, 0.05F));
        }
    }

    /** 6.2.0, Up We Go: one tick of the launch's ramp, her speed set a little higher along the flip's way and up. */
    static void soarTick(ServerPlayer player, PaleServer.PaleState state) {
        if (state.move != PaleMove.FLIP && state.soarTick > 0 || state.soarTick >= PaleRules.SOAR_RAMP) {
            state.soarTick = PaleRules.SOAR_RAMP;
            return;
        }
        double[] launch = PaleRules.soar(PaleRules.Flip.byOrdinal(state.soarFlip), state.soarTick);
        UkakuFire.push(player, new Vec3(-Math.sin(state.soarYaw) * launch[0], launch[1], Math.cos(state.soarYaw) * launch[0]));
        state.soarTick++;
    }

    // ------------------------------------------------------------------ Big Punch (Z held, let go)

    /**
     * Z down starts the charge (and keeps it alive while the client keeps saying so); Z up lets it go. She can start it standing,
     * walking, or out of a strike once its blow has landed.
     */
    static void punchKey(ServerPlayer player, PaleServer.PaleState state, boolean held, int targetId, long now) {
        if (!held) {
            state.punchSpent = false;
            if (state.move == PaleMove.PUNCH_CHARGE || state.move == PaleMove.OWL_PUNCH_CHARGE) {
                release(player, state, now);
            }
            return;
        }
        state.punchHeardAt = now;
        // 6.2.0: in the Owl, its own Big Punch (PaleOwlMoves): the same charge, the same keys.
        boolean owl = state.owl;
        PaleMove charging = owl ? PaleMove.OWL_PUNCH_CHARGE : PaleMove.PUNCH_CHARGE;
        if (state.move == charging || state.punchSpent || now < state.punchReadyAt
            || !(owl ? PaleCancel.allowedOwl(state, now) : PaleCancel.allowed(state, now))) {
            return;
        }
        PaleCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        PaleServer.begin(player, state, charging, 0, 0, 0.0F, null, now);
        if (!owl && PaleRevive.instantPunch(player)) {
            // 6.2.2, Instant Big Punch (Witness This): the tap is the full charge at once, and so the combo. Once a press.
            state.punchSpent = true;
            release(player, state, now, 1.0F);
            return;
        }
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.7F, KaguneServerController.vary(player, owl ? 0.6F : 0.8F, 0.05F));
        if (owl) {
            // The giant arm rearing up behind her: the flesh of it straining.
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 0.7F, KaguneServerController.vary(player, 0.7F, 0.05F));
        }
    }

    /** Let go: as charged as it got, at whatever she is looking at in range (or the air). */
    private static void release(ServerPlayer player, PaleServer.PaleState state, long now) {
        release(player, state, now, PaleRules.punchCharge(now - state.startedAt));
    }

    private static void release(ServerPlayer player, PaleServer.PaleState state, long now, float charge) {
        if (!KaguneServerController.useEnergy(player, PaleRules.punchCost(charge), true)) {
            PaleServer.end(player, state);
            return;
        }
        LivingEntity target = gaze(player, PaleRules.punchRange(charge));
        PaleMove punch = state.move == PaleMove.OWL_PUNCH_CHARGE ? PaleMove.OWL_PUNCH : PaleMove.PUNCH;
        PaleServer.begin(player, state, punch, 0, target == null ? 0 : target.m_19879_(), charge, null, now);
        state.punchReadyAt = now + Cooldowns.of(player, PaleRules.PUNCH_COOLDOWN);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.8F, KaguneServerController.vary(player, 0.55F, 0.04F));
    }

    /**
     * What she is looking at, within {@code range} and in sight: the nearest body whose middle lies within its own half-width (and
     * a little more, more the further it is) of the line from her eye, short of the first block in the way.
     */
    static LivingEntity gaze(ServerPlayer player, double range) {
        ServerLevel level = player.m_284548_();
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        Vec3 end = eye.m_82549_(look.m_82490_(range));
        BlockHitResult wall = level.m_45547_(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double limit = wall.m_6662_() == HitResult.Type.BLOCK ? wall.m_82450_().m_82554_(eye) : range;
        AABB area = new AABB(eye.f_82479_, eye.f_82480_, eye.f_82481_, end.f_82479_, end.f_82480_, end.f_82481_).m_82400_(2.0);
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity candidate : level.m_6443_(LivingEntity.class, area, entity -> PaleBlitz.valid(player, entity))) {
            Vec3 to = KaguneServerController.center(candidate).m_82546_(eye);
            double along = to.m_82526_(look);
            if (along <= 0.0 || along > limit + candidate.m_20205_()) {
                continue;
            }
            double off = to.m_82546_(look.m_82490_(along)).m_82553_();
            if (off <= candidate.m_20205_() * 0.5 + 0.5 + along * 0.03 && along < nearest) {
                nearest = along;
                best = candidate;
            }
        }
        return best;
    }

    /** The punch in flight: the blink in front of the target, the blow, the hold. */
    private static void tickPunch(ServerPlayer player, PaleServer.PaleState state, long now) {
        long age = now - state.startedAt;
        LivingEntity target = state.targetId == 0 ? null
            : player.m_9236_().m_6815_(state.targetId) instanceof LivingEntity living && PaleBlitz.valid(player, living) ? living : null;
        if (age <= PaleRules.PUNCH_IMPACT && target != null) {
            // 6.2.2: it never sees her coming: nothing it does lands on her before the blow.
            PaleStun.stun(target, now, 2);
        }
        if (age == PaleRules.PUNCH_BLINK && target != null) {
            PaleBlitz.blinkTo(player, state, target, PaleRules.strikeStand(target.m_20205_()) - 0.15, PaleStrike.Blink.FRONT, -1);
        }
        if (age == PaleRules.PUNCH_IMPACT) {
            if (state.move == PaleMove.OWL_PUNCH) {
                PaleOwlMoves.punch(player, state, target, now);
            } else {
                punch(player, state, target);
            }
        }
        if (age >= PaleRules.PUNCH_TICKS) {
            PaleServer.end(player, state);
        }
    }

    /** The blow: the target, if she reached it, launched clean through whatever weak blocks lie behind it; round it, a shockwave. */
    private static void punch(ServerPlayer player, PaleServer.PaleState state, LivingEntity target) {
        ServerLevel level = player.m_284548_();
        float charge = state.charge;
        Vec3 facing = target != null ? UkakuFire.horizontal(target.m_20182_().m_82546_(player.m_20182_())) : UkakuFire.horizontal(player.m_20154_());
        Vec3 fist = player.m_20182_().m_82520_(facing.f_82479_ * 1.2, 1.0, facing.f_82481_ * 1.2);
        boolean reached = target != null && KaguneServerController.center(target).m_82554_(player.m_146892_()) - target.m_20205_() * 0.5 <= 3.6;
        int mastery = KaguneProfile.mastery(player);
        if (reached) {
            Vec3 centre = KaguneServerController.center(target);
            fist = new Vec3(centre.f_82479_ - facing.f_82479_ * target.m_20205_() * 0.5, target.m_20186_() + target.m_20206_() * 0.45,
                centre.f_82481_ - facing.f_82481_ * target.m_20205_() * 0.5);
            target.f_19802_ = 0;
            WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.BLUNT, fist, facing, 0.0);
            if (target.m_6469_(player.m_269291_().m_269075_(player), PaleRules.punchDamage(mastery, charge) * PaleServer.power(player))) {
                if (charge >= 0.999F && PaleWitness.has(player) && target.m_6084_()) {
                    // 6.2.0, Witness This: at the full charge the punch goes on into the combo.
                    PaleWitness.start(player, state, target, facing, level.m_46467_());
                } else {
                    double resist = Math.max(0.0, Math.min(1.0, target.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
                    Vec3 launch = facing.m_82490_(PaleRules.punchLaunch(charge) * (1.0 - 0.6 * resist)).m_82520_(0.0, PaleRules.punchLift(charge), 0.0);
                    BlockSmash.launch(player, target, launch, PaleRules.PUNCH_FLIGHT);
                }
                KaguneProfile.addMastery(player, "Swipe", 2);
                KaguneNetwork.broadcast(player);
                PaleServer.landed(player, 1);
                QuestServer.signal(player, com.hex.kagune.control.quest.Signal.PUNCH, 1, charge >= 0.999F ? com.hex.kagune.control.quest.Tags.FULL : "partial");
            }
        }
        // The air itself is thrown: everything else close round the blow is knocked back.
        AABB around = new AABB(fist.f_82479_ - 3.0, fist.f_82480_ - 2.0, fist.f_82481_ - 3.0, fist.f_82479_ + 3.0, fist.f_82480_ + 2.0, fist.f_82481_ + 3.0);
        for (LivingEntity other : level.m_6443_(LivingEntity.class, around, entity -> PaleBlitz.valid(player, entity) && entity != target)) {
            Vec3 away = UkakuFire.horizontal(other.m_20182_().m_82546_(player.m_20182_()));
            double ahead = away.m_82526_(facing);
            if (ahead < 0.2 || KaguneServerController.center(other).m_82554_(fist) > 3.5) {
                continue;
            }
            other.f_19802_ = 0;
            if (other.m_6469_(player.m_269291_().m_269075_(player), PaleRules.punchDamage(mastery, charge) * PaleServer.power(player) * 0.3F)) {
                other.m_20256_(other.m_20184_().m_82490_(0.3).m_82549_(away.m_82490_(0.8 + charge)).m_82520_(0.0, 0.3, 0.0));
                other.f_19864_ = true;
            }
        }
        PaleBlitz.fx(player, S2CPaleFx.IMPACT, reached ? target.m_19879_() : 0, fist, facing.m_82490_(PaleRules.punchLaunch(charge)),
            reached ? 2.0F + charge : 1.2F + charge, S2CPaleFx.BIG_PUNCH);
        UkakuFire.sound(level, fist, "entity.generic.explode", 0.7F + 0.3F * charge, KaguneServerController.vary(player, 1.25F, 0.05F));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
        if (reached) {
            KaguneServerController.playAt(player, target, HexKaguneSounds.BONE_CRACK, 0.9F, KaguneServerController.vary(player, 0.75F, 0.05F));
        }
    }

    // ------------------------------------------------------------------ Speed Kick (V tapped)

    /** What the Speed Kick goes for and how: where she plants, the dash's ticks, how high she springs, the way she faces (yaw). */
    record KickPlan(LivingEntity target, Vec3 spot, int dash, double lift, float yaw) {
    }

    /**
     * V tapped (6.2.0, Kizaru's kick): the body closest to her crosshair, in front of her and in her sight, within
     * {@link PaleRules#KICK_RANGE}, with room to kick it and a clear way to it. With none, only "No target found" (no move, no RC, no
     * cooldown). Otherwise she throws herself into the lunge and is gone along the ground to where she kicks from (her own client
     * moves her there, {@code PaleClient}; nothing can hurt her meanwhile, {@link PaleServer#untouchable}; it is held where it is until
     * her foot lands), there she drives the kick into its middle ({@link #tickKick}).
     */
    static void kick(ServerPlayer player, PaleServer.PaleState state, long now) {
        if (!PaleCancel.allowed(state, now) || now < state.kickReadyAt || PaleServer.locked(player, state, "Speed Kick", PaleRules.KICK_MASTERY)
            || PaleServer.eyes(player) && !QuestServer.pass(player, com.hex.kagune.control.quest.Gate.EYES_KICK)) {
            return; // (7.0.1: with the eyes alone it also needs its quest)
        }
        KickPlan plan = kickPlan(player);
        if (plan == null) {
            PaleServer.notice(player, state, Component.m_237115_("message.hexkagune.pale.kick_no_target"));
            return;
        }
        if (!KaguneServerController.useEnergy(player, PaleRules.KICK_COST, true)) {
            return;
        }
        PaleCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        PaleServer.begin(player, state, PaleMove.KICK, plan.dash(), plan.target().m_19879_(), (float)plan.lift(), plan.spot(), now, plan.yaw());
        state.kickReadyAt = now + Cooldowns.of(player, PaleRules.KICK_COOLDOWN);
        pin(player, plan.target());
        // Off the mark: the ground torn up under her.
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "block.gravel.break", 1.0F, KaguneServerController.vary(player, 0.7F, 0.08F));
    }

    /** The body the Speed Kick goes for (closest to her crosshair), and how; null when there is none she can kick. */
    static KickPlan kickPlan(ServerPlayer player) {
        ServerLevel level = player.m_284548_();
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        double cone = Math.cos(Math.toRadians(PaleRules.KICK_CONE));
        AABB area = player.m_20191_().m_82400_(PaleRules.KICK_RANGE);
        KickPlan best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : level.m_6443_(LivingEntity.class, area, entity -> PaleBlitz.valid(player, entity))) {
            Vec3 to = candidate.m_146892_().m_82546_(eye);
            double distance = to.m_82553_();
            if (distance > PaleRules.KICK_RANGE || distance < 1.0E-3) {
                continue;
            }
            double facing = to.m_82526_(look) / distance;
            // In front of her, near her crosshair: never behind her.
            if (facing < cone || to.f_82479_ * look.f_82479_ + to.f_82481_ * look.f_82481_ <= 0.0) {
                continue;
            }
            double score = Math.acos(Math.min(1.0, facing)) + distance * 0.004;
            if (score >= bestScore || !seen(level, player, eye, candidate)) {
                continue;
            }
            KickPlan plan = plan(player, candidate);
            if (plan != null) {
                best = plan;
                bestScore = score;
            }
        }
        return best;
    }

    /** She sees it: nothing solid between her eyes and its eyes, or its middle. */
    static boolean seen(ServerLevel level, ServerPlayer player, Vec3 eye, LivingEntity body) {
        return clear(level, player, eye, body.m_146892_()) || clear(level, player, eye, KaguneServerController.center(body));
    }

    static boolean clear(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to) {
        return level.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).m_6662_() == HitResult.Type.MISS;
    }

    /**
     * How she kicks {@code target}: from straight in front of it, where her leg's reach puts the sole on its front
     * ({@link PaleRules#kickStand}), on the ground it stands on, risen as high as puts the foot at its middle
     * ({@link PaleRules#kickLift}); null when she doesn't fit there (or up where she rises to) or the way there is blocked.
     */
    static KickPlan plan(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.m_284548_();
        Vec3 from = player.m_20182_();
        Vec3 at = target.m_20182_();
        Vec3 facing = UkakuFire.horizontal(at.m_82546_(from));
        if (facing.m_82556_() < 1.0E-6) {
            facing = UkakuFire.horizontal(player.m_20154_());
        }
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        double[] stand = PaleRules.kickStand(target.m_20205_() * 0.5);
        Vec3 spot = at.m_82549_(right.m_82490_(stand[0])).m_82549_(facing.m_82490_(stand[1]));
        double lift = PaleRules.kickLift(target.m_20206_() * PaleRules.KICK_AIM);
        Vec3 offset = spot.m_82546_(from);
        AABB there = player.m_20191_().m_82386_(offset.f_82479_, offset.f_82480_, offset.f_82481_);
        if (!level.m_45756_(player, there) || !level.m_45756_(player, there.m_82386_(0.0, lift, 0.0))) {
            return null;
        }
        // A clear way there (low and high): she dashes, she doesn't pass through anything.
        for (double up : new double[] {0.6, 1.5}) {
            if (!clear(level, player, from.m_82520_(0.0, up, 0.0), spot.m_82520_(0.0, up, 0.0))) {
                return null;
            }
        }
        int dash = PaleRules.kickDash(offset.m_82553_());
        float yaw = (float)Math.toDegrees(Math.atan2(-facing.f_82479_, facing.f_82481_));
        return new KickPlan(target, spot, dash, lift, yaw);
    }

    /**
     * The kick in flight: what she kicks is held where it is (it never sees her coming) until her foot lands. The lunge; gone (unseen:
     * invisible, so not even her shadow shows, for just the dash; nothing hurts her, {@link PaleServer#untouchable}); there, with a
     * crack of air; the blow on its very frame ({@link PaleRules#KICK_CONTACT} after she is there); the rebound's whoosh; a thud as
     * she comes down; done once she is up in her stance.
     */
    private static void tickKick(ServerPlayer player, PaleServer.PaleState state, long now) {
        long age = now - state.startedAt;
        int dash = state.detail;
        int arrive = PaleRules.kickArrive(dash);
        player.f_19789_ = 0.0F;
        LivingEntity target = state.targetId == 0 ? null
            : player.m_9236_().m_6815_(state.targetId) instanceof LivingEntity living && PaleBlitz.valid(player, living) ? living : null;
        long contact = arrive + PaleRules.KICK_CONTACT;
        if (age < contact && target != null) {
            pin(player, target);
        }
        if (age == PaleRules.KICK_BURST) {
            // Gone.
            if (!player.m_21023_(MobEffects.f_19609_)) {
                player.m_7292_(new MobEffectInstance(MobEffects.f_19609_, Math.max(1, dash), 0, false, false, false));
            }
            UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.illusioner.mirror_move", 1.0F, KaguneServerController.vary(player, 1.3F, 0.06F));
            UkakuFire.sound(player.m_284548_(), player.m_20182_(), "item.trident.riptide_1", 0.9F, KaguneServerController.vary(player, 1.4F, 0.06F));
        }
        if (age == arrive) {
            // There, out of nowhere: the air cracks where she is.
            UkakuFire.sound(player.m_284548_(), state.point, "entity.ender_dragon.flap", 0.8F, KaguneServerController.vary(player, 1.6F, 0.06F));
            UkakuFire.sound(player.m_284548_(), state.point, "entity.player.attack.sweep", 1.0F, KaguneServerController.vary(player, 0.55F, 0.05F));
        }
        if (age == contact) {
            kickLands(player, state, target, now);
        }
        if (age == arrive + (long)Math.ceil(PaleRules.KICK_HOLD)) {
            // The rebound off it: over backwards.
            UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.attack.sweep", 0.8F, KaguneServerController.vary(player, 0.85F, 0.06F));
        }
        if (age == arrive + (long)Math.ceil(PaleRules.KICK_LAND)) {
            UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.small_fall", 0.9F, KaguneServerController.vary(player, 0.8F, 0.06F));
        }
        if (age >= arrive + PaleRules.KICK_TICKS) {
            PaleServer.end(player, state);
        }
    }

    /** Held where it is (no going anywhere but down), slowed to nothing. */
    private static void pin(ServerPlayer player, LivingEntity body) {
        body.m_20256_(new Vec3(0.0, Math.min(0.0, body.m_20184_().f_82480_), 0.0));
        body.f_19864_ = true;
        body.m_147207_(new MobEffectInstance(MobEffects.f_19597_, 3, 6, false, false, false), player);
        // 6.2.2: it never sees her coming: nothing it does lands on her before the kick.
        PaleStun.stun(body, body.m_9236_().m_46467_(), 2);
    }

    /**
     * The blow, the sole driven into its middle (if she made it there and it is still where she aimed): the hurt, a bruise there, and
     * it is launched straight away from her ({@link PaleRules#kickFlight}), carried low and far through whatever it can break
     * ({@link BlockSmash#kick}), tumbling; it lies there a moment where it comes down ({@link PaleKnockdown}).
     */
    private static void kickLands(ServerPlayer player, PaleServer.PaleState state, LivingEntity target, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 facing = facing(state);
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        Vec3 spot = state.point;
        Vec3 foot = spot.m_82549_(right.m_82490_(PaleRules.KICK_FOOT_X)).m_82549_(facing.m_82490_(PaleRules.KICK_FOOT_Z))
            .m_82520_(0.0, PaleRules.KICK_FOOT_Y + state.charge, 0.0);
        Vec3 here = player.m_20182_();
        double away = Math.hypot(here.f_82479_ - spot.f_82479_, here.f_82481_ - spot.f_82481_);
        if (target == null || away > PaleRules.KICK_THERE) {
            UkakuFire.sound(level, foot, "entity.player.attack.sweep", 0.8F, KaguneServerController.vary(player, 0.8F, 0.06F));
            return;
        }
        Vec3 middle = target.m_20182_().m_82520_(0.0, target.m_20206_() * PaleRules.KICK_AIM, 0.0);
        double off = Math.hypot(middle.f_82479_ - foot.f_82479_, middle.f_82481_ - foot.f_82481_) - target.m_20205_() * 0.5;
        AABB box = target.m_20191_();
        if (off > PaleRules.KICK_SLACK || foot.f_82480_ < box.f_82289_ - 0.5 || foot.f_82480_ > box.f_82292_ + 0.5) {
            UkakuFire.sound(level, foot, "entity.player.attack.sweep", 0.8F, KaguneServerController.vary(player, 0.8F, 0.06F));
            return;
        }
        double[] flight = PaleRules.kickFlight();
        Vec3 along = right.m_82490_(flight[0]).m_82549_(facing.m_82490_(flight[1]));
        Vec3 struck = new Vec3(foot.f_82479_, Math.max(box.f_82289_ + 0.1, Math.min(box.f_82292_ - 0.1, foot.f_82480_)), foot.f_82481_)
            .m_82549_(facing.m_82490_(0.05));
        target.f_19802_ = 0;
        WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.BLUNT, struck, along, 0.0);
        float damage = PaleRules.fist(KaguneProfile.mastery(player)) * PaleRules.KICK_DAMAGE * PaleServer.power(player);
        if (!target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            return;
        }
        double resist = Math.max(0.0, Math.min(1.0, target.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
        double speed = PaleRules.KICK_SPEED * (1.0 - PaleRules.KICK_RESIST * resist);
        BlockSmash.kick(player, target, along, speed, PaleRules.KICK_UP, damage);
        PaleKnockdown.launched(target, now);
        Vec3 launch = along.m_82490_(speed).m_82520_(0.0, PaleRules.KICK_UP, 0.0);
        PaleBlitz.fx(player, S2CPaleFx.KICKED, target.m_19879_(), struck, launch, 1.0F, 0);
        UkakuFire.sound(level, struck, "entity.player.attack.strong", 1.4F, KaguneServerController.vary(player, 0.5F, 0.04F));
        UkakuFire.sound(level, struck, "entity.player.attack.knockback", 1.2F, KaguneServerController.vary(player, 0.55F, 0.05F));
        UkakuFire.sound(level, struck, "entity.warden.sonic_boom", 0.7F, KaguneServerController.vary(player, 1.35F, 0.05F));
        UkakuFire.sound(level, struck, "entity.generic.explode", 0.6F, KaguneServerController.vary(player, 1.5F, 0.06F));
        KaguneServerController.playAt(player, target, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
        KaguneProfile.addMastery(player, "Swipe", 2);
        KaguneNetwork.broadcast(player);
        PaleServer.landed(player, 1);
        QuestServer.signal(player, com.hex.kagune.control.quest.Signal.KICK, 1);
    }

    /** The way the kick goes (level, as she faces). */
    private static Vec3 facing(PaleServer.PaleState state) {
        double yaw = Math.toRadians(state.yaw);
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    // ------------------------------------------------------------------ Flesh Hammer (V held, let go)

    /**
     * V held: up on her left leg, the right one raised out in front and her body tipped back under it, a club of flesh grows round
     * it, bigger the longer she holds (to {@link PaleRules#HAMMER_GROW_TICKS}). Let go (or held as long as it goes) and the axe kick
     * slams it into the ground ahead: a crater knocked out of the weak ground there, and a shockwave throwing everything round it up
     * and away. Let go before the leg is up, she raises it first.
     */
    static void hammerKey(ServerPlayer player, PaleServer.PaleState state, boolean held, long now) {
        if (!held) {
            if (state.move == PaleMove.HAMMER_CHARGE) {
                if (now - state.startedAt >= PaleRules.HAMMER_RAISE_TICKS) {
                    slam(player, state, now);
                } else {
                    state.hammerLetGo = true;
                }
            }
            return;
        }
        state.hammerHeardAt = now;
        if (state.move == PaleMove.HAMMER_CHARGE || now < state.hammerReadyAt || !PaleCancel.allowed(state, now)
            || PaleServer.locked(player, state, "Flesh Hammer", PaleRules.HAMMER_MASTERY)) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, PaleRules.HAMMER_START_COST, true)) {
            return;
        }
        PaleCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        state.hammerLetGo = false;
        // 6.2.0: detail 1 with True Anonymity (every client grows her the huge spiked club).
        PaleServer.begin(player, state, PaleMove.HAMMER_CHARGE, huge(player) ? 1 : 0, 0, 0.0F, null, now);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.6F, KaguneServerController.vary(player, 0.9F, 0.05F));
    }

    /** Held: she is rooted on her left leg while the club grows; once the leg is up, let go (or unheard, or held as long as it goes) it slams. */
    private static void tickHammerCharge(ServerPlayer player, PaleServer.PaleState state, long now) {
        long held = now - state.startedAt;
        player.m_147207_(new MobEffectInstance(MobEffects.f_19597_, 4, 3, false, false, false), player);
        if (held == PaleRules.HAMMER_SPROUT) {
            KaguneServerController.playSelf(player, HexKaguneSounds.MANIFEST_RIP, 0.8F, KaguneServerController.vary(player, 0.75F, 0.05F));
        }
        if (held == PaleRules.HAMMER_GROW_TICKS) {
            KaguneServerController.playSelf(player, HexKaguneSounds.BONE_CRACK, 0.6F, KaguneServerController.vary(player, 0.55F, 0.04F));
        }
        boolean up = held >= PaleRules.HAMMER_RAISE_TICKS;
        if (up && (state.hammerLetGo || now - state.hammerHeardAt > PaleRules.HOLD_TIMEOUT || held >= PaleRules.HAMMER_MAX_HOLD)) {
            slam(player, state, now);
        }
    }

    /** 6.2.0: whether her Flesh Hammer is True Anonymity's (the mutation): the huge spiked club. */
    static boolean huge(ServerPlayer player) {
        return PaleRevive.hasMutation(player, PaleMutation.TRUE_ANONYMITY);
    }

    /** Let go: the axe kick, the club as big as it grew (the least, if she cannot pay for more). */
    private static void slam(ServerPlayer player, PaleServer.PaleState state, long now) {
        boolean huge = state.detail == 1;
        float charge = PaleRules.hammerCharge(now - state.startedAt, huge);
        if (!KaguneServerController.useEnergy(player, Math.max(0.0F, PaleRules.hammerCost(charge) - PaleRules.HAMMER_START_COST), true)) {
            charge = PaleRules.hammerCharge(0.0, huge);
        }
        state.hammerLetGo = false;
        PaleServer.begin(player, state, PaleMove.HAMMER, huge ? 1 : 0, 0, charge, null, now);
        state.hammerReadyAt = now + Cooldowns.of(player, PaleRules.HAMMER_COOLDOWN);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 0.5F, 0.04F));
    }

    private static void tickHammer(ServerPlayer player, PaleServer.PaleState state, long now) {
        long age = now - state.startedAt;
        if (age == PaleRules.HAMMER_IMPACT) {
            hammerLands(player, state);
        }
        if (age >= PaleRules.HAMMER_TICKS) {
            PaleServer.end(player, state);
        }
    }

    /**
     * The club comes down on the ground ahead of her (where its head lies at the blow, {@link PaleRules#hammerReach}): the weak ground
     * there is knocked out in a crater (put back later, as anything a body smashes), and the shockwave hurts everything round it
     * (most at the middle) and throws it up and away, through weak blocks.
     */
    private static void hammerLands(ServerPlayer player, PaleServer.PaleState state) {
        ServerLevel level = player.m_284548_();
        float charge = state.charge;
        double yaw = Math.toRadians(state.yaw);
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3 head = player.m_20182_().m_82549_(facing.m_82490_(PaleRules.hammerReach(charge))).m_82549_(right.m_82490_(0.1));
        BlockHitResult ground = level.m_45547_(new ClipContext(head.m_82520_(0.0, 1.5, 0.0), head.m_82520_(0.0, -3.5, 0.0), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player));
        Vec3 at = ground.m_6662_() == HitResult.Type.BLOCK ? ground.m_82450_() : new Vec3(head.f_82479_, player.m_20186_(), head.f_82481_);
        BlockPos below = BlockPos.m_274561_(at.f_82479_, at.f_82480_ - 0.01, at.f_82481_);
        int groundState = Block.m_49956_(level.m_8055_(below));
        BlockSmash.crater(player, at, PaleRules.hammerCrater(charge));
        double radius = PaleRules.hammerRadius(charge);
        int mastery = KaguneProfile.mastery(player);
        AABB area = new AABB(at.f_82479_ - radius - 1.0, at.f_82480_ - 2.0, at.f_82481_ - radius - 1.0, at.f_82479_ + radius + 1.0, at.f_82480_ + 3.0,
            at.f_82481_ + radius + 1.0);
        int landed = 0;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, area, entity -> PaleBlitz.valid(player, entity))) {
            Vec3 off = body.m_20182_().m_82546_(at);
            double flat = Math.sqrt(off.f_82479_ * off.f_82479_ + off.f_82481_ * off.f_82481_);
            if (flat - body.m_20205_() * 0.5 > radius) {
                continue;
            }
            double near = 1.0 - Math.max(0.0, Math.min(1.0, flat / radius));
            body.f_19802_ = 0;
            Vec3 away = flat < 0.2 ? facing : new Vec3(off.f_82479_ / flat, 0.0, off.f_82481_ / flat);
            WoundHits.expect(body, com.hex.kagune.control.wound.WoundBook.Blow.SLAM, body.m_20182_(), away.m_82520_(0.0, 1.0, 0.0), 0.0);
            if (!body.m_6469_(player.m_269291_().m_269075_(player), PaleRules.hammerDamage(mastery, charge) * (float)(0.4 + 0.6 * near))) {
                continue;
            }
            landed++;
            if (state.detail == 1) {
                // True Anonymity: the bone spikes all over the club tear what it catches.
                UkakuCrystalCombat.rinkakuBleed(body, player, level.m_46467_());
            }
            double resist = Math.max(0.0, Math.min(1.0, body.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
            double launch = PaleRules.hammerLaunch(charge) * (0.35 + 0.65 * near) * (1.0 - 0.6 * resist);
            BlockSmash.launch(player, body, away.m_82490_(launch * 0.55).m_82520_(0.0, launch * 0.7 + 0.2, 0.0), 30);
        }
        PaleBlitz.fx(player, S2CPaleFx.SHOCKWAVE, 0, at, facing, (float)radius, groundState);
        UkakuFire.sound(level, at, "entity.generic.explode", 0.8F + 0.4F * charge, KaguneServerController.vary(player, 0.7F, 0.05F));
        UkakuFire.sound(level, at, "block.anvil.land", 0.6F, KaguneServerController.vary(player, 0.5F, 0.05F));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        if (landed > 0) {
            KaguneProfile.addMastery(player, "Swipe", 2);
            KaguneNetwork.broadcast(player);
            PaleServer.landed(player, landed);
        }
    }

    static void tick(ServerPlayer player, PaleServer.PaleState state, long now) {
        switch (state.move) {
            case FLIP -> tickFlip(player, state, now);
            case PUNCH_CHARGE, OWL_PUNCH_CHARGE -> tickCharge(player, state, now);
            case PUNCH, OWL_PUNCH -> tickPunch(player, state, now);
            case WITNESS -> PaleWitness.tick(player, state, now);
            case KICK -> tickKick(player, state, now);
            case OWL_KICK -> PaleOwlMoves.tickKick(player, state, now);
            case OWL_SPIKES -> PaleOwlMoves.tickSpikes(player, state, now);
            case OWL_STRIKE -> PaleOwlMoves.tickRend(player, state, now);
            case OWL_WHIRL -> PaleOwlMoves.tickWhirl(player, state, now);
            case OWL_LEAP -> PaleOwlMoves.tickPounce(player, state, now);
            case OWL_FISSURE_CHARGE -> PaleOwlMoves.tickFissureCharge(player, state, now);
            case OWL_FISSURE -> PaleOwlMoves.tickFissure(player, state, now);
            case OWL_VOLLEY -> OwlVolley.tickMove(player, state, now);
            case OWL_GRAB_REACH, OWL_GRAB_HOLD, OWL_GRAB_SLAM, OWL_GRAB_MISS -> OwlGrab.tick(player, state, now);
            case OWL_WATCH -> {
                // Reared up as its lances burst out; then she is her own again (they hunt on, OwlWatch).
                if (now - state.startedAt >= com.hex.kagune.control.pale.OwlRules.LANCES_POSE_TICKS) {
                    PaleServer.end(player, state);
                }
            }
            case HAMMER_CHARGE -> tickHammerCharge(player, state, now);
            case HAMMER -> tickHammer(player, state, now);
            case WATCH -> {
                // Her cross held while the tentacles burst out; then she is her own again (they hunt on, PaleWatch).
                if (now - state.startedAt >= com.hex.kagune.control.pale.WatchRules.POSE_TICKS) {
                    PaleServer.end(player, state);
                }
            }
            default -> {
            }
        }
    }

    /** Charging: she is rooted to the spot; Z let go (or unheard of a while, or held as long as it goes), it is let go. */
    private static void tickCharge(ServerPlayer player, PaleServer.PaleState state, long now) {
        long held = now - state.startedAt;
        player.m_147207_(new MobEffectInstance(MobEffects.f_19597_, 4, 3, false, false, false), player);
        if (held == PaleRules.PUNCH_CHARGE_TICKS) {
            // Full: a crack through her as the power sets.
            KaguneServerController.playSelf(player, HexKaguneSounds.BONE_CRACK, 0.6F, KaguneServerController.vary(player, 0.6F, 0.04F));
        }
        if (now - state.punchHeardAt > PaleRules.HOLD_TIMEOUT || held >= PaleRules.PUNCH_MAX_HOLD) {
            release(player, state, now);
        }
    }

    /** In the air no fall hurts her; a few ticks after touchdown (or in water, or far too long) the flip is over. */
    private static void tickFlip(ServerPlayer player, PaleServer.PaleState state, long now) {
        long age = now - state.startedAt;
        player.f_19789_ = 0.0F;
        if (age >= 3 && player.m_20096_() && state.landedAt == Long.MIN_VALUE) {
            state.landedAt = now;
            UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.small_fall", 0.6F, KaguneServerController.vary(player, 0.9F, 0.05F));
        }
        boolean landed = state.landedAt != Long.MIN_VALUE && now - state.landedAt >= PaleRules.FLIP_LAND_TICKS;
        if (landed || age > (state.soaring ? PaleRules.SOAR_MAX_TICKS : PaleRules.FLIP_MAX_TICKS) || player.m_20069_()) {
            state.push = null;
            state.pushTicks = 0;
            PaleServer.end(player, state);
        }
    }
}

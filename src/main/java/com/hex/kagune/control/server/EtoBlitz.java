package com.hex.kagune.control.server;

import com.hex.kagune.control.ClashRules;
import com.hex.kagune.control.eto.EtoMove;
import com.hex.kagune.control.eto.EtoRules;
import com.hex.kagune.control.eto.EtoStrike;
import com.hex.kagune.control.eto.StunRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CEtoFx;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Eto's Speed Blitz (M1; 6.1.0): click by click a chain of strikes ({@link EtoStrike}), each thrown with the whole body. A click
 * during a strike is kept and thrown the moment that strike reaches its chain tick (the last strike's extension is the next one's
 * coil); a click after one, within {@link EtoRules#CHAIN_WINDOW}, carries on the chain; later, it starts over with the jab.
 *
 * <p>She locks onto what she clicked (or the nearest body in front of her within {@link EtoRules#STEP_IN_RANGE}) and closes the
 * gap in the coil, so the fist lands on the impact tick; the blinking strikes vanish her to the target's side or square in front of
 * it. On the impact tick everything in front of her within the strike's reach is hit once: damage, a bruise where the fist met it,
 * a knockback that follows the blow (back, to her right or left, up), a stagger, a stun ({@link EtoStun}: it can't hurt anything
 * or walk for a moment, renewed by every hit), and every client sees the body reel.
 */
final class EtoBlitz {
    /** How far a launched body's stagger slows it (ticks, amplifier), and the finisher's. */
    private static final int STAGGER_TICKS = 10;
    private static final int FINISH_STAGGER_TICKS = 24;
    /** Strikes hit what is in front of her: within this angle of where she faces (the backfist: all round). */
    private static final double FRONT_COS = Math.cos(Math.toRadians(58.0));

    private EtoBlitz() {
    }

    /** An M1 (the original's swipe trampoline): the next strike, now or at the current one's chain tick. */
    static void click(ServerPlayer player, Entity hint) {
        if (!EtoServer.ready(player)) {
            return;
        }
        EtoServer.EtoState state = EtoServer.state(player);
        long now = player.m_9236_().m_46467_();
        if (EtoOwl.busy(state, now)) {
            // 6.2.0: in the Owl, its own M1 (its Rend); nothing while it grows or goes.
            EtoOwlMoves.click(player, state, hint, now);
            return;
        }
        if (state.move == EtoMove.STRIKE) {
            state.queuedAt = now;
            state.queuedHint = hint == null ? 0 : hint.m_19879_();
            return;
        }
        if (now < state.recoverUntil) {
            return; // catching her breath after the finisher
        }
        start(player, state, hint, now); // 6.2.0: out of anything else she is doing (EtoCancel)
    }

    private static void start(ServerPlayer player, EtoServer.EtoState state, Entity hint, long now) {
        int mastery = KaguneProfile.mastery(player);
        EtoStrike previous = state.lastStrike != null && now - state.strikeEndsAt <= EtoRules.CHAIN_WINDOW ? state.lastStrike : null;
        EtoStrike strike = EtoStrike.next(previous, mastery);
        if (!KaguneServerController.useEnergy(player, strike.cost(), true)) {
            return;
        }
        LivingEntity target = lockOn(player, hint);
        EtoCancel.cut(player, state, now);
        EtoServer.begin(player, state, EtoMove.STRIKE, strike.ordinal(), target == null ? 0 : target.m_19879_(), 0.0F, null, now);
        state.lastStrike = strike;
        state.strikeEndsAt = now + strike.duration;
        state.queuedAt = Long.MIN_VALUE;
        // 1.0.40: on its way: another's M1 landing on her before it lands meets it (KaguneClash), and both stop.
        KaguneClash.swing(player, now, strike.impact, strike.reach, () -> state.move == EtoMove.STRIKE && state.startedAt == now, () -> clashed(player, state));
    }

    /** 1.0.40: her blow met another's coming back (KaguneClash): cut off, nothing queued after it, a moment before she can strike again. */
    private static void clashed(ServerPlayer player, EtoServer.EtoState state) {
        long now = player.m_9236_().m_46467_();
        state.queuedAt = Long.MIN_VALUE;
        state.strikeEndsAt = now;
        state.recoverUntil = now + ClashRules.STAGGER;
        state.push = null;
        state.pushTicks = 0;
        EtoServer.end(player, state);
    }

    /** What she strikes at: what she clicked, else the nearest body in front of her she could step in to. */
    static LivingEntity lockOn(ServerPlayer player, Entity hint) {
        if (hint instanceof LivingEntity living && valid(player, living) && living.m_20280_(player) <= EtoRules.STEP_IN_RANGE * EtoRules.STEP_IN_RANGE) {
            return living;
        }
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        AABB area = player.m_20191_().m_82400_(EtoRules.STEP_IN_RANGE);
        for (LivingEntity candidate : player.m_284548_().m_6443_(LivingEntity.class, area, entity -> valid(player, entity))) {
            Vec3 to = KaguneServerController.center(candidate).m_82546_(eye);
            double distance = to.m_82553_();
            if (distance > EtoRules.STEP_IN_RANGE || distance < 1.0E-4) {
                continue;
            }
            double facing = to.m_82490_(1.0 / distance).m_82526_(look);
            if (facing < 0.9) {
                continue; // within about 25 degrees of the crosshair
            }
            double score = distance * (2.0 - facing);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    static boolean valid(ServerPlayer player, LivingEntity entity) {
        return entity != player && entity.m_6084_() && KaguneServerController.validSwipeTarget(player, entity);
    }

    static void tick(ServerPlayer player, EtoServer.EtoState state, long now) {
        if (state.move != EtoMove.STRIKE) {
            return;
        }
        EtoStrike strike = EtoStrike.byOrdinal(state.detail);
        long age = now - state.startedAt;
        LivingEntity target = target(player, state);
        if (strike.blink != EtoStrike.Blink.NONE && age == EtoStrike.BLINK_TICK && target != null) {
            blink(player, state, strike, target);
        }
        if (target != null && age < strike.impact) {
            stepIn(player, state, strike, target, age);
        }
        if (age == strike.impact - 1) {
            // The drive: the air cut.
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.35F, KaguneServerController.vary(player, 1.75F, 0.1F));
        }
        if (age == strike.impact) {
            land(player, state, strike, target);
            if (state.move != EtoMove.STRIKE) {
                return; // 1.0.40: it met another's (KaguneClash): cut off
            }
        }
        if (age >= strike.chain && state.queuedAt >= state.startedAt && !strike.finisher()) {
            // The next strike out of this one's hold.
            Entity hint = state.queuedHint == 0 ? null : player.m_9236_().m_6815_(state.queuedHint);
            state.strikeEndsAt = now;
            state.move = EtoMove.NONE;
            start(player, state, hint, now);
            if (state.move == EtoMove.STRIKE) {
                return;
            }
            EtoServer.broadcast(player, state); // no RC left for it: she stops
            return;
        }
        if (age >= strike.duration) {
            if (strike.finisher()) {
                state.recoverUntil = now + Cooldowns.of(player, EtoRules.FINISH_RECOVERY);
            }
            state.push = null;
            state.pushTicks = 0;
            EtoServer.end(player, state);
        }
    }

    /** The locked-on body, while it is still there to hit. */
    private static LivingEntity target(ServerPlayer player, EtoServer.EtoState state) {
        if (state.targetId == 0) {
            return null;
        }
        Entity entity = player.m_9236_().m_6815_(state.targetId);
        double range = EtoRules.STEP_IN_RANGE + EtoRules.BLINK_RANGE;
        return entity instanceof LivingEntity living && valid(player, living) && living.m_20280_(player) <= range * range ? living : null;
    }

    /**
     * Closes the gap to the target over the coil, so the fist meets it on the impact tick: a push renewed each tick, never faster
     * than {@link EtoRules#STEP_IN_SPEED}, stopping where her reach starts.
     */
    private static void stepIn(ServerPlayer player, EtoServer.EtoState state, EtoStrike strike, LivingEntity target, long age) {
        Vec3 to = target.m_20182_().m_82546_(player.m_20182_());
        double distance = Math.sqrt(to.f_82479_ * to.f_82479_ + to.f_82481_ * to.f_82481_);
        double stop = EtoRules.strikeStand(target.m_20205_());
        if (distance <= stop + 0.05) {
            return;
        }
        double left = Math.max(1.0, strike.impact - age);
        double speed = Math.min(EtoRules.STEP_IN_SPEED, (distance - stop) / left);
        Vec3 heading = AyatoFire.horizontal(to);
        double up = player.m_20096_() ? Math.min(player.m_20184_().f_82480_, 0.0) : player.m_20184_().f_82480_;
        Vec3 motion = new Vec3(heading.f_82479_ * speed, up, heading.f_82481_ * speed);
        AyatoFire.push(player, motion);
    }

    /** She vanishes and is there: at the target's side (whichever side is free) or square in front of it, facing it. */
    private static void blink(ServerPlayer player, EtoServer.EtoState state, EtoStrike strike, LivingEntity target) {
        blinkTo(player, state, target, EtoRules.strikeStand(target.m_20205_()), strike.blink, strike.ordinal());
    }

    /**
     * She vanishes (an afterimage left where she stood) and is there, {@code stand} blocks from the target's centre and facing it:
     * square in front of it, toward where she came from ({@link EtoStrike.Blink#FRONT}), or round at its side, whichever side is
     * free ({@link EtoStrike.Blink#SIDE}); only where she fits. False when there was nowhere to be (she stays). {@code extra} rides
     * on the effect (what she blinked for).
     */
    static boolean blinkTo(ServerPlayer player, EtoServer.EtoState state, LivingEntity target, double stand, EtoStrike.Blink kind, int extra) {
        return blinkTo(player, state, target, stand, kind, extra, 0.0);
    }

    /** As above, lifted {@code rise} blocks off the ground there (less, if there is no room for that much). */
    static boolean blinkTo(ServerPlayer player, EtoServer.EtoState state, LivingEntity target, double stand, EtoStrike.Blink kind, int extra,
                           double rise) {
        ServerLevel level = player.m_284548_();
        Vec3 from = player.m_20182_();
        Vec3 at = target.m_20182_();
        Vec3 toward = AyatoFire.horizontal(at.m_82546_(from));
        Vec3[] spots;
        if (kind == EtoStrike.Blink.SIDE) {
            // Round to the side, a little in front of it: the target's right first (from where she was), then its left.
            spots = new Vec3[] {at.m_82549_(turn(toward.m_82490_(-1.0), 70.0).m_82490_(stand)), at.m_82549_(turn(toward.m_82490_(-1.0), -70.0).m_82490_(stand))};
        } else {
            spots = new Vec3[] {at.m_82546_(toward.m_82490_(stand))};
        }
        for (Vec3 spot : spots) {
            double[] lifts = rise > 0.0 ? new double[] {rise, rise * 0.66, rise * 0.33, 0.0, 0.5, 1.0} : new double[] {0.0, 0.5, 1.0};
            for (double lift : lifts) {
                Vec3 candidate = spot.m_82520_(0.0, lift, 0.0);
                Vec3 offset = candidate.m_82546_(from);
                if (!level.m_45756_(player, player.m_20191_().m_82386_(offset.f_82479_, offset.f_82480_, offset.f_82481_))) {
                    continue;
                }
                Vec3 facing = AyatoFire.horizontal(at.m_82546_(candidate));
                float yaw = (float)Math.toDegrees(Math.atan2(-facing.f_82479_, facing.f_82481_));
                fx(player, S2CEtoFx.BLINK, target.m_19879_(), from, candidate, yaw, extra);
                player.m_6021_(candidate.f_82479_, candidate.f_82480_, candidate.f_82481_);
                player.f_19789_ = 0.0F;
                // 6.2.2: it never saw her coming: it can't swing at her before her blow lands.
                EtoStun.stun(target, level.m_46467_(), 6);
                AyatoFire.push(player, Vec3.f_82478_);
                state.yaw = yaw;
                EtoServer.broadcast(player, state);
                KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.6F, KaguneServerController.vary(player, 0.6F, 0.05F));
                return true;
            }
        }
        return false;
    }

    /** {@code v} turned {@code degrees} about the vertical (+ to the left of its own heading, as seen from above). */
    static Vec3 turn(Vec3 v, double degrees) {
        double r = Math.toRadians(degrees);
        double c = Math.cos(r);
        double s = Math.sin(r);
        return new Vec3(v.f_82479_ * c + v.f_82481_ * s, v.f_82480_, -v.f_82479_ * s + v.f_82481_ * c);
    }

    /** The impact tick: everything in front of her within reach is hit once. */
    private static void land(ServerPlayer player, EtoServer.EtoState state, EtoStrike strike, LivingEntity target) {
        ServerLevel level = player.m_284548_();
        Vec3 eye = player.m_146892_();
        Vec3 facing = target != null ? AyatoFire.horizontal(target.m_20182_().m_82546_(player.m_20182_())) : AyatoFire.horizontal(player.m_20154_());
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        Vec3 knock = facing.m_82490_(strike.back).m_82549_(right.m_82490_(strike.sideways)).m_82520_(0.0, strike.up, 0.0);
        int mastery = KaguneProfile.mastery(player);
        float damage = EtoRules.fist(mastery) * strike.damage * EtoServer.power(player);
        boolean all = strike == EtoStrike.BACKFIST;
        AABB area = player.m_20191_().m_82400_(strike.reach + 1.0);
        List<LivingEntity> candidates = level.m_6443_(LivingEntity.class, area, entity -> valid(player, entity));
        int landed = 0;
        for (LivingEntity candidate : candidates) {
            if (state.hit.contains(candidate.m_19879_())) {
                continue;
            }
            Vec3 centre = KaguneServerController.center(candidate);
            Vec3 to = centre.m_82546_(eye);
            double reach = to.m_82553_() - candidate.m_20205_() * 0.5;
            if (reach > strike.reach) {
                continue;
            }
            Vec3 flat = AyatoFire.horizontal(to);
            if (!all && candidate != target && flat.m_82526_(facing) < FRONT_COS) {
                continue;
            }
            // 1.0.40: a player whose own M1 is on its way back at her: the two meet, and neither lands.
            if (KaguneClash.meets(player, candidate, level.m_46467_())) {
                return;
            }
            state.hit.add(candidate.m_19879_());
            if (strike(player, candidate, strike, damage, knock, facing)) {
                landed++;
            }
        }
        KaguneClash.landed(player);
        if (landed > 0) {
            // 7.0.1: the quests count the blows (the finisher's own).
            QuestServer.signal(player, com.hex.kagune.control.quest.Signal.STRIKE, landed, strike.finisher() ? com.hex.kagune.control.quest.Tags.FINISHER : "strike");
            KaguneProfile.addMastery(player, "Swipe", 1);
            KaguneNetwork.broadcast(player);
            EtoServer.landed(player, landed);
        } else {
            AyatoFire.sound(level, eye, "entity.player.attack.nodamage", 0.7F, KaguneServerController.vary(player, 1.1F, 0.08F));
        }
    }

    /** One body hit: damage, the bruise, the knockback along the blow, the stagger, and every client sees it reel. */
    private static boolean strike(ServerPlayer player, LivingEntity target, EtoStrike strike, float damage, Vec3 knock, Vec3 facing) {
        Vec3 centre = KaguneServerController.center(target);
        // Where the fist meets the body: the side facing her, at the height the strike goes in at.
        double height = switch (strike) {
            case UPPERCUT -> 0.82;
            case LIVER, BLINK_PALM -> 0.42;
            case TWIN_PALM -> 0.55;
            default -> 0.74;
        };
        Vec3 contact = new Vec3(centre.f_82479_ - facing.f_82479_ * target.m_20205_() * 0.5, target.m_20186_() + target.m_20206_() * height,
            centre.f_82481_ - facing.f_82481_ * target.m_20205_() * 0.5);
        Vec3 into = knock.m_82556_() > 1.0E-6 ? knock.m_82541_() : facing;
        target.f_19802_ = 0;
        WoundHits.expect(target, WoundBook.Blow.BLUNT, contact, into, 0.0);
        if (!target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            return false;
        }
        // The blow decides where the body goes: most of what it was doing is lost.
        double resist = Math.max(0.0, Math.min(1.0, target.m_21133_(Attributes.f_22278_)));
        Vec3 push = knock.m_82490_(1.0 - 0.75 * resist);
        target.m_20256_(target.m_20184_().m_82490_(0.15).m_82549_(push));
        target.f_19864_ = true;
        target.f_19812_ = true;
        boolean heavy = strike.finisher() || strike == EtoStrike.UPPERCUT || strike == EtoStrike.BLINK_PALM;
        target.m_147207_(new MobEffectInstance(MobEffects.f_19597_, heavy ? FINISH_STAGGER_TICKS : STAGGER_TICKS, heavy ? 3 : 1, false, false, false), player);
        // 6.2.0: it can't fight back mid-combo.
        EtoStun.stun(target, player.m_9236_().m_46467_(), heavy ? StunRules.HEAVY_TICKS : StunRules.TICKS);
        fx(player, S2CEtoFx.IMPACT, target.m_19879_(), contact, push, strike.damage, strike.ordinal());
        sounds(player, target, strike);
        return true;
    }

    /** Each strike sounds its own: a snap for the jab, the meat of a cross, ribs for the liver shot, a crack for the finisher. */
    private static void sounds(ServerPlayer player, LivingEntity target, EtoStrike strike) {
        ServerLevel level = player.m_284548_();
        Vec3 at = KaguneServerController.center(target);
        float pitch = KaguneServerController.vary(player, 1.0F, 0.06F);
        switch (strike) {
            case JAB -> {
                AyatoFire.sound(level, at, "entity.player.attack.strong", 0.8F, 1.35F * pitch);
                KaguneServerController.playAt(player, target, HexKaguneSounds.FLESH_IMPACT, 0.45F, 1.5F * pitch);
            }
            case CROSS, HOOK -> {
                AyatoFire.sound(level, at, "entity.player.attack.strong", 1.0F, 1.05F * pitch);
                KaguneServerController.playAt(player, target, HexKaguneSounds.FLESH_IMPACT, 0.7F, 1.15F * pitch);
            }
            case LIVER -> {
                AyatoFire.sound(level, at, "entity.player.attack.knockback", 1.0F, 0.8F * pitch);
                KaguneServerController.playAt(player, target, HexKaguneSounds.BONE_CRACK, 0.5F, 1.3F * pitch);
            }
            case UPPERCUT, BACKFIST -> {
                AyatoFire.sound(level, at, "entity.player.attack.crit", 1.0F, 0.9F * pitch);
                KaguneServerController.playAt(player, target, HexKaguneSounds.FLESH_IMPACT, 0.85F, 0.95F * pitch);
            }
            case BLINK_PALM -> {
                AyatoFire.sound(level, at, "entity.player.attack.knockback", 1.0F, 0.7F * pitch);
                KaguneServerController.playAt(player, target, HexKaguneSounds.FLESH_IMPACT, 0.9F, 0.8F * pitch);
            }
            case TWIN_PALM -> {
                AyatoFire.sound(level, at, "entity.generic.explode", 0.55F, 1.55F * pitch);
                KaguneServerController.playAt(player, target, HexKaguneSounds.KAGUNE_SLAM, 0.9F, 1.2F * pitch);
                KaguneServerController.playAt(player, target, HexKaguneSounds.BONE_CRACK, 0.6F, 0.9F * pitch);
            }
        }
    }

    static void fx(ServerPlayer player, int kind, int target, Vec3 at, Vec3 velocity, float size, int extra) {
        ControlNetwork.broadcast(player, packet(kind, player.m_19879_(), target, at, velocity, size, extra));
    }

    /** An effect every client tracking {@code subject} sees (a launched body, wherever it has flown). */
    static void fxAround(Entity subject, int owner, int kind, Vec3 at, Vec3 velocity, float size, int extra) {
        ControlNetwork.broadcastAround(subject, packet(kind, owner, subject.m_19879_(), at, velocity, size, extra));
    }

    /** An effect that belongs to no one (a block grown back), for every player near it. */
    static void fxNear(ServerLevel level, int kind, Vec3 at, float size, int extra) {
        ControlNetwork.sendNear(level, at, 64.0, packet(kind, 0, 0, at, Vec3.f_82478_, size, extra));
    }

    static S2CEtoFx packet(int kind, int owner, int target, Vec3 at, Vec3 velocity, float size, int extra) {
        return new S2CEtoFx(kind, owner, target, at.f_82479_, at.f_82480_, at.f_82481_, (float)velocity.f_82479_, (float)velocity.f_82480_,
            (float)velocity.f_82481_, size, extra);
    }
}

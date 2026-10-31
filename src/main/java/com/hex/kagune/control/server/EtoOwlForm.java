package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.network.S2CPaleOwl;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.pale.OwlRules;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.StunRules;
import com.hex.kagune.control.wound.WoundBook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.8: the Owl, Eto's kakuja and her last resort, a Pale player's ({@link PaleOwl}, {@link OwlRules}) on her: hurt far enough
 * ({@link EtoRules#OWL_AT}) she grows it, the flesh bursting out of her into the whole creature, drawn in her place by every client
 * from the same {@link S2CPaleOwl} a player's is (sent by her UUID). In it she takes only part of every blow and fights with its own
 * moves, out of the same rules and on the same ticks as a player's: its Rend (the four cuts of the giant blades, the last its jaws),
 * its Reaping Whirl, its Pounce. Out of a fight a while, she sheds it. Nothing she does in it breaks the ground (her Library stands).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class EtoOwlForm {
    private EtoOwlForm() {
    }

    /** Whether she is in it (or growing it). */
    static boolean in(EtoOwl.Fight f) {
        return f.owl;
    }

    /** Whether it is still growing out of her or going back in: nothing else starts. */
    static boolean changing(EtoOwl.Fight f, long now) {
        return f.owlAt != Long.MIN_VALUE && OwlRules.changing(f.owl, now - f.owlAt);
    }

    /** She grows it: whatever she was doing cut off, her arm and tentacles back in her. */
    static void grow(EtoNpc npc, EtoOwl.Fight f, ServerLevel level, long now) {
        EtoFlesh.release(level, f);
        EtoOwl.end(f);
        EtoArm.forget(npc);
        EtoWatch.forget(npc);
        npc.m_21573_().m_26573_();
        set(npc, f, true, now);
        EtoBrain.bark(npc, level, com.hex.kagune.control.npc.EtoLines.owl(npc.m_217043_().m_188503_(1000)));
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_strain", 1.0F, 0.55F);
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:manifest_rip", 1.0F, 0.6F);
    }

    /** She sheds it (drawn back into her); it comes back a while later. */
    static void shed(EtoNpc npc, EtoOwl.Fight f, long now) {
        if (!f.owl) {
            return;
        }
        EtoOwl.end(f);
        set(npc, f, false, now);
        f.owlReady = now + EtoRules.OWL_EVERY;
        UkakuFire.sound((ServerLevel)npc.m_9236_(), npc.m_20182_(), "hexkagune:kagune_dismiss", 1.0F, 0.7F);
    }

    private static void set(EtoNpc npc, EtoOwl.Fight f, boolean on, long now) {
        f.owl = on;
        f.owlAt = now;
        f.dirty = true;
        ControlNetwork.broadcastAround(npc, new S2CPaleOwl(npc.m_20148_(), on, now));
    }

    /** Each tick of hers: the burst as it tears out of her; out of a fight a while, she sheds it. */
    static void tick(EtoNpc npc, EtoBrain.Mind mind, ServerLevel level, long now) {
        EtoOwl.Fight f = mind.owl;
        if (!f.owl) {
            return;
        }
        if (now - f.owlAt == OwlRules.BURST) {
            UkakuFire.sound(level, npc.m_20182_(), "hexkagune:bone_crack", 1.0F, 0.55F);
            UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_reshape", 1.0F, 0.6F);
        }
        if (mind.target == 0 && !f.busy() && !changing(f, now) && now - mind.foughtAt > EtoRules.OWL_SHED_AFTER) {
            shed(npc, f, now);
        }
    }

    /** A tick of her fight in it, nothing under way: its pick ({@link EtoRules#chooseOwl}). */
    static void fight(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        EtoOwl.face(npc, target);
        if (changing(f, now)) {
            npc.m_21573_().m_26573_();
            return;
        }
        double distance = EtoOwl.gap(npc, target);
        int around = 0;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(OwlRules.WHIRL_REACH),
            entity -> EtoOwl.fair(npc, mind, entity) && (entity.m_19879_() == mind.target || entity instanceof Mob mob && mob.m_5448_() == npc))) {
            around++;
        }
        EtoRules.Act act = EtoRules.chooseOwl(distance, npc.m_142582_(target), npc.m_20096_(), now, f.whirlReady, f.leapReady, around,
            npc.m_217043_().m_188500_());
        switch (act) {
            case OWL_LEAP -> pounce(npc, f, target, level, now);
            case OWL_WHIRL -> whirl(npc, f, level, now);
            case OWL_REND -> rend(npc, f, target, level, now);
            default -> npc.m_21573_().m_5624_(target, phase.chase);
        }
    }

    /** A tick of one of its moves under way. */
    static void tick(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, LivingEntity target, ServerLevel level, long now, long age, EtoRules.Phase phase) {
        switch (f.move) {
            case OWL_STRIKE -> tickRend(npc, mind, f, target, level, now, age, phase);
            case OWL_WHIRL -> tickWhirl(npc, mind, f, level, now, age, phase);
            case OWL_LEAP -> tickPounce(npc, mind, f, level, now, age, phase);
            default -> EtoOwl.end(f);
        }
    }

    // ---------------------------------------------------------------- its Rend

    private static void rend(EtoNpc npc, EtoOwl.Fight f, LivingEntity target, ServerLevel level, long now) {
        int stage = f.rend >= 0 && now - f.rendEndsAt <= PaleRules.CHAIN_WINDOW ? OwlRules.nextRend(f.rend) : 0;
        npc.m_21573_().m_26573_();
        EtoOwl.face(npc, target);
        EtoOwl.begin(npc, f, PaleMove.OWL_STRIKE, stage, target.m_19879_(), 0.0F, Vec3.f_82478_, now);
        f.hit.clear();
        f.rend = stage;
        f.rendEndsAt = now + OwlRules.REND_TICKS[stage];
    }

    private static void tickRend(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, LivingEntity target, ServerLevel level, long now, long age,
                                 EtoRules.Phase phase) {
        int stage = Math.max(0, Math.min(OwlRules.REND_STAGES - 1, f.detail));
        if (target != null && age < OwlRules.REND_IMPACT[stage]) {
            // Stepping in through the wind-up, so the blade meets it on the impact tick.
            Vec3 to = target.m_20182_().m_82546_(npc.m_20182_());
            double distance = Math.sqrt(to.f_82479_ * to.f_82479_ + to.f_82481_ * to.f_82481_);
            double stop = OwlRules.REND_STAND + target.m_20205_() * 0.5;
            if (distance > stop + 0.05) {
                double left = Math.max(1.0, OwlRules.REND_IMPACT[stage] - age);
                double speed = Math.min(PaleRules.STEP_IN_SPEED, (distance - stop) / left);
                Vec3 heading = UkakuFire.horizontal(to);
                npc.m_20256_(new Vec3(heading.f_82479_ * speed, Math.min(npc.m_20184_().f_82480_, 0.0), heading.f_82481_ * speed));
                npc.f_19864_ = true;
            }
        }
        if (age == OwlRules.REND_IMPACT[stage] - 1) {
            UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.sweep", 1.0F, stage == 3 ? 0.45F : 0.6F);
        }
        if (age == OwlRules.REND_IMPACT[stage]) {
            landRend(npc, mind, f, stage, level, now, phase);
        }
        boolean finisher = stage == OwlRules.REND_STAGES - 1;
        if (!finisher && age >= OwlRules.REND_CHAIN[stage] && target != null && EtoOwl.gap(npc, target) <= OwlRules.REND_REACH[stage] + 0.5) {
            f.rendEndsAt = now;
            EtoOwl.end(f);
            rend(npc, f, target, level, now);
            return;
        }
        if (age >= OwlRules.REND_TICKS[stage]) {
            f.rendEndsAt = now;
            if (finisher) {
                f.rend = -1;
            }
            EtoOwl.end(f);
        }
    }

    /** A cut lands: everything in front of her within the blade's reach and arc is cut once, thrown the way the blade went, stunned. */
    private static void landRend(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, int stage, ServerLevel level, long now, EtoRules.Phase phase) {
        Vec3 facing = EtoOwl.local(f, 0.0, 0.0, 1.0);
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        double[] k = OwlRules.REND_KNOCK[stage];
        Vec3 knock = facing.m_82490_(k[0]).m_82549_(right.m_82490_(k[1])).m_82520_(0.0, k[2], 0.0);
        float damage = PaleRules.fist(EtoRules.MASTERY) * OwlRules.REND_DAMAGE[stage] * phase.power;
        double arc = Math.cos(Math.toRadians(OwlRules.REND_ARC[stage]));
        Vec3 eye = npc.m_146892_();
        boolean finisher = stage == OwlRules.REND_STAGES - 1;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(OwlRules.REND_REACH[stage] + 1.0), entity -> EtoOwl.fair(npc, mind, entity))) {
            if (f.hit.contains(body.m_19879_())) {
                continue;
            }
            Vec3 middle = GhoulNpcBrain.centre(body);
            Vec3 to = middle.m_82546_(eye);
            if (to.m_82553_() - body.m_20205_() * 0.5 > OwlRules.REND_REACH[stage] || UkakuFire.horizontal(to).m_82526_(facing) < arc && body.m_19879_() != f.target) {
                continue;
            }
            f.hit.add(body.m_19879_());
            Vec3 cut = middle.m_82546_(facing.m_82490_(body.m_20205_() * 0.5));
            Vec3 along = knock.m_82556_() < 1.0E-6 ? facing : knock.m_82541_();
            if (!hurt(npc, body, damage, cut, along, WoundBook.Blow.SLASH)) {
                continue;
            }
            body.m_20256_(body.m_20184_().m_82490_(0.15).m_82549_(knock.m_82490_(1.0 - 0.75 * EtoOwl.resist(body))));
            body.f_19864_ = true;
            PaleStun.stun(body, now, finisher ? StunRules.HEAVY_TICKS : StunRules.TICKS);
            if (finisher) {
                PaleKnockdown.launched(body, now);
            }
            EtoOwl.fx(npc, S2CPaleFx.IMPACT, body.m_19879_(), cut, along.m_82490_(1.2), finisher ? 2.0F : 1.3F, S2CPaleFx.BONE);
            UkakuFire.sound(level, cut, "hexkagune:flesh_impact", 0.9F, finisher ? 0.6F : 0.85F);
            UkakuFire.sound(level, cut, "entity.player.attack.strong", 0.9F, finisher ? 0.55F : 0.8F);
        }
        if (finisher) {
            // Its jaws snapping shut on what is before it.
            Vec3 jaws = npc.m_20182_().m_82549_(facing.m_82490_(OwlRules.REND_BITE_AHEAD)).m_82520_(0.0, 1.6, 0.0);
            UkakuFire.sound(level, jaws, "entity.generic.eat", 1.2F, 0.45F);
            UkakuFire.sound(level, jaws, "entity.evoker_fangs.attack", 1.0F, 0.6F);
        }
    }

    // ---------------------------------------------------------------- its Reaping Whirl

    private static void whirl(EtoNpc npc, EtoOwl.Fight f, ServerLevel level, long now) {
        npc.m_21573_().m_26573_();
        EtoOwl.begin(npc, f, PaleMove.OWL_WHIRL, 0, 0, 0.0F, Vec3.f_82478_, now);
        f.hit.clear();
        f.whirlReady = now + EtoRules.WHIRL_EVERY;
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_strain", 0.7F, 0.75F);
    }

    private static void tickWhirl(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, ServerLevel level, long now, long age, EtoRules.Phase phase) {
        npc.m_21573_().m_26573_();
        if (age == OwlRules.WHIRL_START) {
            UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.sweep", 1.2F, 0.45F);
            UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_whip", 1.0F, 0.5F);
            EtoOwl.fx(npc, S2CPaleFx.IMPACT, 0, npc.m_20182_().m_82520_(0.0, 1.0, 0.0), new Vec3(0.0, 1.0, 0.0), 1.8F, S2CPaleFx.OWL_WHIRL);
        }
        if (age > OwlRules.WHIRL_START && age <= OwlRules.WHIRL_END + 1) {
            double from = OwlRules.whirlTurn(age - 1.0);
            double to = OwlRules.whirlTurn(age);
            if (to > from) {
                reap(npc, mind, f, level, from, to, now, phase);
            }
        }
        if (age >= OwlRules.WHIRL_TICKS) {
            EtoOwl.end(f);
        }
    }

    /** What the blades passed over as she turned from {@code from} to {@code to} (radians round, + to her left). */
    private static void reap(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, ServerLevel level, double from, double to, long now, EtoRules.Phase phase) {
        Vec3 facing = EtoOwl.local(f, 0.0, 0.0, 1.0);
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        Vec3 middle = npc.m_20182_().m_82520_(0.0, 1.0, 0.0);
        float damage = PaleRules.fist(EtoRules.MASTERY) * OwlRules.WHIRL_DAMAGE * phase.power;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(OwlRules.WHIRL_REACH + 1.0), entity -> EtoOwl.fair(npc, mind, entity))) {
            if (f.hit.contains(body.m_19879_())) {
                continue;
            }
            Vec3 off = GhoulNpcBrain.centre(body).m_82546_(middle);
            double flat = Math.sqrt(off.f_82479_ * off.f_82479_ + off.f_82481_ * off.f_82481_) - body.m_20205_() * 0.5;
            if (flat > OwlRules.WHIRL_REACH || flat + body.m_20205_() < OwlRules.WHIRL_NEAR || off.f_82480_ < -1.6 || off.f_82480_ > 2.4) {
                continue;
            }
            double angle = Math.atan2(off.m_82526_(facing), off.m_82526_(right));
            if (!PaleOwlMoves.swept(angle, from, to) && !PaleOwlMoves.swept(angle + Math.PI, from, to)) {
                continue;
            }
            f.hit.add(body.m_19879_());
            Vec3 out = UkakuFire.horizontal(off);
            Vec3 along = new Vec3(-out.f_82481_, 0.0, out.f_82479_).m_82490_(-1.0);
            Vec3 cut = GhoulNpcBrain.centre(body).m_82546_(out.m_82490_(body.m_20205_() * 0.5));
            if (!hurt(npc, body, damage, cut, along, WoundBook.Blow.SLASH)) {
                continue;
            }
            Vec3 thrown = out.m_82490_(OwlRules.WHIRL_OUT).m_82549_(along.m_82490_(OwlRules.WHIRL_ALONG)).m_82520_(0.0, OwlRules.WHIRL_UP, 0.0)
                .m_82490_(1.0 - 0.6 * EtoOwl.resist(body));
            body.m_20256_(thrown);
            body.f_19864_ = true;
            PaleStun.stun(body, now, StunRules.TICKS);
            EtoOwl.fx(npc, S2CPaleFx.IMPACT, body.m_19879_(), cut, along.m_82490_(1.2), 1.5F, S2CPaleFx.BONE);
            UkakuFire.sound(level, cut, "hexkagune:flesh_impact", 0.9F, 0.8F);
        }
    }

    // ---------------------------------------------------------------- its Pounce

    private static void pounce(EtoNpc npc, EtoOwl.Fight f, LivingEntity target, ServerLevel level, long now) {
        npc.m_21573_().m_26573_();
        EtoOwl.face(npc, target);
        EtoOwl.begin(npc, f, PaleMove.OWL_LEAP, OwlRules.leapDetail(PaleRules.Flip.FORWARD, false), target.m_19879_(), 0.0F, Vec3.f_82478_, now);
        f.landedAt = Long.MIN_VALUE;
        f.leapReady = now + EtoRules.LEAP_EVERY;
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_strain", 0.8F, 0.6F);
    }

    /** The crouch, the bound, the air (no fall hurts her), the slam as she lands, her recovery. */
    private static void tickPounce(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, ServerLevel level, long now, long age, EtoRules.Phase phase) {
        npc.f_19789_ = 0.0F;
        npc.m_21573_().m_26573_();
        if (age == OwlRules.LEAP_CROUCH) {
            double[] launch = OwlRules.leap(PaleRules.Flip.FORWARD, false);
            Vec3 ahead = EtoOwl.local(f, 0.0, 0.0, 1.0);
            npc.m_20256_(new Vec3(ahead.f_82479_ * launch[0], launch[1], ahead.f_82481_ * launch[0]));
            npc.f_19864_ = true;
            UkakuFire.sound(level, npc.m_20182_(), "entity.ender_dragon.flap", 0.8F, 0.75F);
            UkakuFire.sound(level, npc.m_20182_(), "block.gravel.break", 1.0F, 0.6F);
            UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_whip", 0.9F, 0.5F);
        }
        if (age >= OwlRules.LEAP_CROUCH + 3 && f.landedAt == Long.MIN_VALUE && (npc.m_20096_() || npc.m_20069_())) {
            f.landedAt = now;
            f.charge = (float)age;
            f.dirty = true;
        }
        if (f.landedAt != Long.MIN_VALUE && now - f.landedAt == OwlRules.LEAP_SLAM_AFTER) {
            slam(npc, mind, f, level, now, phase);
        }
        boolean done = f.landedAt != Long.MIN_VALUE && now - f.landedAt >= OwlRules.LEAP_RECOVER;
        if (done || age > OwlRules.LEAP_MOST) {
            EtoOwl.end(f);
        }
    }

    /** As she lands: both blades into the ground ahead of her: what is round the blow hurt, cut, thrown up and stunned. */
    private static void slam(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, ServerLevel level, long now, EtoRules.Phase phase) {
        Vec3 facing = EtoOwl.local(f, 0.0, 0.0, 1.0);
        Vec3 at = npc.m_20182_().m_82549_(facing.m_82490_(OwlRules.LEAP_SLAM_AHEAD));
        float damage = PaleRules.fist(EtoRules.MASTERY) * OwlRules.LEAP_DAMAGE * phase.power;
        double reach = OwlRules.LEAP_SLAM_REACH;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, new AABB(at.f_82479_ - reach, at.f_82480_ - 1.5, at.f_82481_ - reach, at.f_82479_ + reach,
            at.f_82480_ + 2.5, at.f_82481_ + reach), entity -> EtoOwl.fair(npc, mind, entity))) {
            Vec3 off = body.m_20182_().m_82546_(at);
            double flat = Math.sqrt(off.f_82479_ * off.f_82479_ + off.f_82481_ * off.f_82481_) - body.m_20205_() * 0.5;
            if (flat > reach) {
                continue;
            }
            Vec3 away = flat < 0.2 ? facing : UkakuFire.horizontal(off);
            if (!hurt(npc, body, damage, GhoulNpcBrain.centre(body), away.m_82520_(0.0, -0.5, 0.0), WoundBook.Blow.SLASH)) {
                continue;
            }
            double resist = EtoOwl.resist(body);
            body.m_20256_(away.m_82490_(0.55 * (1.0 - 0.6 * resist)).m_82520_(0.0, 0.5 * (1.0 - 0.4 * resist), 0.0));
            body.f_19864_ = true;
            PaleStun.stun(body, now, StunRules.HEAVY_TICKS);
            EtoOwl.fx(npc, S2CPaleFx.IMPACT, body.m_19879_(), GhoulNpcBrain.centre(body), away.m_82520_(0.0, 0.6, 0.0), 1.6F, S2CPaleFx.BONE);
        }
        int ground = net.minecraft.world.level.block.Block.m_49956_(level.m_8055_(net.minecraft.core.BlockPos.m_274561_(at.f_82479_, at.f_82480_ - 0.01,
            at.f_82481_)));
        EtoOwl.fx(npc, S2CPaleFx.SHOCKWAVE, 0, at, facing, (float)reach, ground);
        UkakuFire.sound(level, at, "entity.generic.explode", 0.8F, 0.75F);
        UkakuFire.sound(level, at, "block.anvil.land", 0.7F, 0.55F);
        UkakuFire.sound(level, at, "hexkagune:kagune_slam", 1.0F, 0.6F);
    }

    // ---------------------------------------------------------------- the rest

    private static boolean hurt(EtoNpc npc, LivingEntity body, float damage, Vec3 at, Vec3 way, WoundBook.Blow blow) {
        body.f_19802_ = 0;
        WoundHits.expect(body, blow, at, way, 0.0);
        return body.m_6469_(npc.m_269291_().m_269333_(npc), damage);
    }

    /** {@code viewer} has just come to see her: it learns she is in it, if she is. */
    static void tracked(ServerPlayer viewer, EtoNpc npc) {
        if (npc.mind != null && npc.mind.owl.owl) {
            ControlNetwork.sendTo(viewer, new S2CPaleOwl(npc.m_20148_(), true, npc.mind.owl.owlAt));
        }
    }

    /** She died in it: every client told it is gone. */
    static void forget(EtoNpc npc) {
        if (npc.mind != null && npc.mind.owl.owl) {
            set(npc, npc.mind.owl, false, npc.m_9236_().m_46467_());
        }
    }

    /** In it, she takes only part of every blow. */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof EtoNpc npc && npc.mind != null && npc.mind.owl.owl) {
            event.setAmount(event.getAmount() * EtoRules.OWL_TAKEN);
        }
    }
}

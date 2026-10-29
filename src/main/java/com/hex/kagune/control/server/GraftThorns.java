package com.hex.kagune.control.server;

import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftRules;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 7.0.2, the user's rework: the four thorns' four on the server, beyond the beats {@link GraftCombat} runs. Crucifixion: held up
 * between the upper pair while the lower pair's thrusts run it through, then hurled. Unstoppable Breach: thrown on along his look,
 * the ground and walls in his way broken (put back later, as every crater is), what he runs into struck and thrown aside, the four
 * thorns' thrusts smashing what stands where they land or running through what lives. Dragon's Dragnet: what the lower pair's
 * sweeps pass is dragged in to the middle and held there for the upper pair's slam. Living Catapult: flung high off the four,
 * steered where he looks through the flight (a job), and where he comes down the four go into the ground round him in turn.
 */
final class GraftThorns {
    /** The bodies the dragnet has caught (a hit set of the move's). */
    static final int GATHERED = -5;

    private GraftThorns() {
    }

    static void tick(ServerPlayer player, GraftServer.GraftState state, GraftMove move, long age, long now) {
        switch (move) {
            case CRUCIFIXION -> crucify(player, state, age, now);
            case BREACH -> breach(player, state, age, now);
            case DRAGNET -> dragnet(player, state, age);
            case CATAPULT -> {
                if (age == GraftRules.CATAPULT_LAUNCH) {
                    launch(player, state, now);
                }
            }
            default -> {
            }
        }
    }

    private static LivingEntity alive(ServerPlayer player, int id) {
        return id != 0 && player.m_284548_().m_6815_(id) instanceof LivingEntity living && living.m_6084_() ? living : null;
    }

    // ---------------------------------------------------------------- Crucifixion

    /** Held up through the thrusts (the hold carries it to then); raised for the throw; hurled along his look. */
    private static void crucify(ServerPlayer player, GraftServer.GraftState state, long age, long now) {
        LivingEntity held = alive(player, state.heldId);
        if (held == null || age < 35) {
            return;
        }
        if (age < GraftRules.CRUCIFY_THROW) {
            if (!PaleStun.stun(held, now, 6)) {
                GraftCombat.release(player, state, false); // 7.0.2: a player held too long breaks free
                GraftServer.broadcast(player, state);
                return;
            }
            Vec3 where = GraftCombat.around(player, new double[] {0.0, 3.4, 3.0});
            GraftCombat.carry(held, where.m_82520_(0.0, -held.m_20206_() * 0.5, 0.0));
            return;
        }
        if (age == GraftRules.CRUCIFY_THROW) {
            Vec3 look = UkakuFire.horizontal(player.m_20154_());
            GraftCombat.strikeBody(player, state, held, 1.0F, Vec3.f_82478_, GraftMove.STUN | GraftMove.HEAVY | GraftMove.BLEED, now, WoundBook.Blow.SLAM);
            GraftCombat.release(player, state, true);
            BlockSmash.launch(player, held, look.m_82490_(GraftRules.CRUCIFY_SPEED).m_82520_(0.0, GraftRules.CRUCIFY_LIFT, 0.0), GraftCombat.THROWN);
            GraftServer.broadcast(player, state);
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.9F, 0.05F));
        }
    }

    // ---------------------------------------------------------------- Unstoppable Breach

    /**
     * The breach: thrown on along his look, what stands in his way at his height broken ahead of him, what he runs into struck
     * (a heavy stun) and thrown aside out of his path.
     */
    private static void breach(ServerPlayer player, GraftServer.GraftState state, long age, long now) {
        if (age < 1 || age > GraftRules.BREACH_TICKS) {
            return;
        }
        Vec3 look = UkakuFire.horizontal(player.m_20154_());
        state.fallSafe = true;
        if (age % 2 == 1) {
            BlockSmash.crater(player, player.m_20182_().m_82549_(look.m_82490_(1.6)).m_82520_(0.0, 1.2, 0.0), 1.1);
        }
        Set<Integer> hit = GraftCombat.hitSet(state, -1);
        Vec3 right = new Vec3(-look.f_82481_, 0.0, look.f_82479_);
        for (LivingEntity body : GraftCombat.around(player, GraftRules.BREACH_CONTACT + 0.5)) {
            if (!hit.add(body.m_19879_())) {
                continue;
            }
            Vec3 off = body.m_20182_().m_82546_(player.m_20182_());
            double which = off.f_82479_ * right.f_82479_ + off.f_82481_ * right.f_82481_ >= 0.0 ? 1.0 : -1.0;
            Vec3 aside = right.m_82490_(which).m_82549_(look.m_82490_(0.6));
            GraftCombat.strikeBody(player, state, body, 0.6F, aside, GraftMove.STUN | GraftMove.HEAVY, now, WoundBook.Blow.SLAM);
            PaleBlitz.fx(player, S2CPaleFx.IMPACT, body.m_19879_(), KaguneServerController.center(body), aside, 1.2F, S2CPaleFx.BONE);
        }
        double vy = player.m_20184_().f_82480_;
        UkakuFire.push(player, look.m_82490_(GraftRules.BREACH_SPEED).m_82520_(0.0, player.m_20096_() ? Math.max(0.0, vy) : vy, 0.0));
        if (age == 1) {
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 0.9F, KaguneServerController.vary(player, 0.8F, 0.05F));
        }
    }

    // ---------------------------------------------------------------- Dragon's Dragnet

    /** A dragnet sweep passing {@code body}: caught (a light blow, stunned) and dragged in to the middle from now on. */
    static void gather(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat, LivingEntity body, long now) {
        if (!GraftCombat.hitSet(state, GATHERED).add(body.m_19879_())) {
            return;
        }
        GraftCombat.strikeBody(player, state, body, beat.power(), Vec3.f_82478_, beat.flags(), now, WoundBook.Blow.SLASH);
        PaleBlitz.fx(player, S2CPaleFx.IMPACT, body.m_19879_(), KaguneServerController.center(body), Vec3.f_82478_, 0.8F, S2CPaleFx.BONE);
    }

    /** The dragnet's middle set before him as it begins; what it caught dragged there and held until the slam. */
    private static void dragnet(ServerPlayer player, GraftServer.GraftState state, long age) {
        if (age == 0) {
            Vec3 look = UkakuFire.horizontal(player.m_20154_());
            state.point = player.m_20182_().m_82549_(look.m_82490_(GraftRules.DRAGNET_POINT)).m_82520_(0.0, 1.0, 0.0);
            GraftServer.broadcast(player, state);
            return;
        }
        if (age > GraftRules.DRAGNET_HOLD) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        for (int id : GraftCombat.hitSet(state, GATHERED)) {
            LivingEntity body = alive(player, id);
            if (body == null) {
                continue;
            }
            if (!PaleStun.stun(body, now, 6)) {
                continue; // 7.0.2: a player held too long breaks free
            }
            Vec3 middle = new Vec3(state.point.f_82479_, body.m_20186_(), state.point.f_82481_);
            GraftCombat.carry(body, middle);
        }
    }

    // ---------------------------------------------------------------- Living Catapult

    /** The four into the ground round him (the ground broken where they go in) and he is flung high: the flight begins. */
    private static void launch(ServerPlayer player, GraftServer.GraftState state, long now) {
        Vec3 look = UkakuFire.horizontal(player.m_20154_());
        Vec3 right = new Vec3(-look.f_82481_, 0.0, look.f_82479_);
        for (double angle : new double[] {40.0, -40.0, 135.0, -135.0}) {
            double a = Math.toRadians(angle);
            Vec3 at = player.m_20182_().m_82549_(look.m_82490_(Math.cos(a) * 1.9)).m_82549_(right.m_82490_(Math.sin(a) * 1.9));
            BlockHitResult ground = player.m_284548_().m_45547_(new ClipContext(at.m_82520_(0.0, 1.0, 0.0), at.m_82520_(0.0, -2.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (ground.m_6662_() == HitResult.Type.BLOCK) {
                BlockSmash.crater(player, ground.m_82450_(), 0.6);
            }
        }
        GraftCombat.ground(player, player.m_20182_(), 2.0F);
        UkakuFire.push(player, look.m_82490_(GraftRules.CATAPULT_AHEAD).m_82520_(0.0, GraftRules.CATAPULT_UP, 0.0));
        state.fallSafe = true;
        GraftCentipedes.beginJob(player, state, GraftMove.CATAPULT_FLIGHT, null, now);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.generic.explode", 0.4F, KaguneServerController.vary(player, 1.3F, 0.05F));
    }

    /**
     * The flight (a job): steered toward where he looks (looking steeply down brings him down sooner); down on the ground (or in
     * water), the landing: the four into the ground round him in turn.
     */
    static void flight(ServerPlayer player, GraftServer.GraftState state, long age, long now) {
        state.fallSafe = true;
        if (age > 4 && (player.m_20096_() || player.m_20069_())) {
            GraftCentipedes.endJob(player, state, false, false);
            GraftCombat.release(player, state, false);
            java.util.Arrays.fill(state.targets, 0);
            GraftCombat.begin(player, state, GraftMove.CATAPULT_LAND, now, player.m_146908_(), player.m_20182_());
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.75F, 0.05F));
            return;
        }
        Vec3 look = player.m_20154_();
        Vec3 level = UkakuFire.horizontal(look);
        Vec3 v = player.m_20184_();
        double down = look.f_82480_ < -0.6 ? -0.06 : 0.0;
        UkakuFire.push(player, new Vec3(v.f_82479_ * 0.9 + level.f_82479_ * GraftRules.CATAPULT_STEER, v.f_82480_ + down,
            v.f_82481_ * 0.9 + level.f_82481_ * GraftRules.CATAPULT_STEER));
    }
}

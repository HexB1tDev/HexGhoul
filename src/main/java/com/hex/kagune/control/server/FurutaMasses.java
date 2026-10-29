package com.hex.kagune.control.server;

import com.hex.kagune.control.furuta.FurutaMove;
import com.hex.kagune.control.furuta.FurutaRules;
import com.hex.kagune.control.network.S2CEtoFx;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 7.0.2, the user's rework: the mass of eyes' four on the server, beyond the beats {@link FurutaCombat} runs. Eye of the Storm: each
 * crescent wheels round him on its own path ({@link FurutaRules#stormAngle}, the rig's own), cutting what it passes (once each half
 * turn). Flesh Bloom: flung out all round, it cuts what is within reach on each wave of spikes. Black Curtain: what stands under the
 * wall is kept from slipping out round its ends while it stands, struck down when it falls and held there. The King's Game (a job):
 * the crescents at their prey in turn while he goes where he likes; a sword's M1 rushes him in on it for his own blow, which bleeds,
 * a crescent driven in with it.
 */
final class FurutaMasses {
    /** The bodies the curtain came down on (a hit set of the move's). */
    static final int UNDER = -7;

    private FurutaMasses() {
    }

    static void tick(ServerPlayer player, FurutaServer.FurutaState state, FurutaMove move, long age, long now) {
        switch (move) {
            case STORM -> storm(player, state, age, now);
            case BLOOM -> bloom(player, state, age, now);
            case CURTAIN -> curtain(player, state, age, now);
            default -> {
            }
        }
    }

    private static LivingEntity alive(ServerPlayer player, int id) {
        return id != 0 && player.m_284548_().m_6815_(id) instanceof LivingEntity living && living.m_6084_() ? living : null;
    }

    private static double wrap(double degrees) {
        return ((degrees % 360.0) + 540.0) % 360.0 - 180.0;
    }

    // ---------------------------------------------------------------- Eye of the Storm

    /** Each crescent this tick: what lies in the arc it swept, out to its reach and about its height, is cut (each once a half turn). */
    private static void storm(ServerPlayer player, FurutaServer.FurutaState state, long age, long now) {
        FurutaMove.Beat beat = FurutaMove.STORM.beats(5).get(0);
        if (age < beat.land() || age > beat.end()) {
            return;
        }
        int limbs = FurutaCombat.limbs(player);
        double length = FurutaServer.stage(player).length(KaguneProfile.mastery(player));
        for (int limb = 0; limb < limbs; limb++) {
            double before = Math.toDegrees(FurutaRules.stormAngle(limb, age - 1));
            double after = Math.toDegrees(FurutaRules.stormAngle(limb, age));
            double middle = 0.5 * (before + after);
            double half = 0.5 * Math.abs(after - before) + 12.0;
            double reach = Math.max(1.2, FurutaRules.stormRadius(limb, age, length)) + 1.0;
            double height = FurutaRules.stormHeight(limb, age);
            Set<Integer> hit = FurutaCombat.hitSet(state, 1000 + limb * 100 + (int)Math.floor(Math.abs(Math.toRadians(after)) / Math.PI));
            for (LivingEntity body : FurutaCombat.around(player, reach + 0.5)) {
                if (hit.contains(body.m_19879_())) {
                    continue;
                }
                Vec3 centre = KaguneServerController.center(body);
                double flat = Math.hypot(centre.f_82479_ - player.m_20185_(), centre.f_82481_ - player.m_20189_());
                double dy = centre.f_82480_ - (player.m_20186_() + height);
                if (flat > reach + body.m_20205_() * 0.5 || Math.abs(dy) > 2.2 + body.m_20206_() * 0.5
                    || Math.abs(wrap(FurutaCombat.bearing(player, state, body) - middle)) > half || !KaguneServerController.clearLineTo(player, body)) {
                    continue;
                }
                hit.add(body.m_19879_());
                Vec3 out = AyatoFire.horizontal(centre.m_82546_(player.m_20182_()));
                double way = limb % 2 == 0 ? 1.0 : -1.0;
                Vec3 along = new Vec3(-out.f_82481_ * way, 0.0, out.f_82479_ * way);
                FurutaCombat.strikeBody(player, state, body, beat.power(), along.m_82490_(0.4), beat.flags(), now, WoundBook.Blow.SLASH);
                EtoBlitz.fx(player, S2CEtoFx.IMPACT, body.m_19879_(), centre, along, 1.0F, S2CEtoFx.BONE);
            }
        }
        if (age % 6 == 0) {
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.7F, KaguneServerController.vary(player, 0.8F, 0.1F));
        }
    }

    // ---------------------------------------------------------------- Flesh Bloom

    /** Each wave of spikes along the flung-out mass: everything within its reach is cut (once a wave). */
    private static void bloom(ServerPlayer player, FurutaServer.FurutaState state, long age, long now) {
        int[] waves = FurutaRules.BLOOM_WAVES;
        for (int i = 0; i < waves.length; i++) {
            if (age != waves[i]) {
                continue;
            }
            double reach = Math.min(7.0, FurutaServer.stage(player).length(KaguneProfile.mastery(player)) * 1.4) + 0.6;
            Set<Integer> hit = FurutaCombat.hitSet(state, 2000 + i);
            for (LivingEntity body : FurutaCombat.around(player, reach)) {
                if (!hit.add(body.m_19879_()) || !KaguneServerController.clearLineTo(player, body)) {
                    continue;
                }
                Vec3 out = AyatoFire.horizontal(KaguneServerController.center(body).m_82546_(player.m_20182_()));
                FurutaCombat.strikeBody(player, state, body, 0.45F, out.m_82490_(0.3), FurutaMove.BLEED | FurutaMove.SPIKED | FurutaMove.STUN, now,
                    WoundBook.Blow.STAB);
                EtoBlitz.fx(player, S2CEtoFx.IMPALED, body.m_19879_(), KaguneServerController.center(body), out, body.m_20206_(), 0);
            }
            KaguneServerController.playSelf(player, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.7F + 0.15F * i, 0.05F));
        }
    }

    // ---------------------------------------------------------------- Black Curtain

    /** {x across to his right, z ahead} of a body from him, by the way the move faces. */
    private static double[] local(ServerPlayer player, FurutaServer.FurutaState state, LivingEntity body) {
        double yaw = Math.toRadians(state.yaw);
        double dx = body.m_20185_() - player.m_20185_();
        double dz = body.m_20189_() - player.m_20189_();
        return new double[] {dx * -Math.cos(yaw) + dz * -Math.sin(yaw), dx * -Math.sin(yaw) + dz * Math.cos(yaw)};
    }

    private static boolean under(double[] at) {
        return Math.abs(at[0]) <= FurutaRules.CURTAIN_SPAN + 0.8 && at[1] >= FurutaRules.CURTAIN_FOOT - 0.6
            && at[1] <= FurutaRules.CURTAIN_FOOT + FurutaRules.CURTAIN_TOP + 0.8;
    }

    /**
     * The curtain: while it stands, what is in front of it is kept from slipping out round its ends; as it lands, what is under it
     * is struck down (stunned hard) and held there until it lifts.
     */
    private static void curtain(ServerPlayer player, FurutaServer.FurutaState state, long age, long now) {
        FurutaMove.Beat beat = FurutaMove.CURTAIN.beats(5).get(0);
        double yaw = Math.toRadians(state.yaw);
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        if (age >= 4 && age < beat.land()) {
            for (LivingEntity body : FurutaCombat.around(player, FurutaRules.CURTAIN_FOOT + FurutaRules.CURTAIN_TOP + 2.0)) {
                double[] at = local(player, state, body);
                if (under(at) && Math.abs(at[0]) > FurutaRules.CURTAIN_SPAN - 1.0) {
                    Vec3 v = body.m_20184_();
                    Vec3 in = right.m_82490_(-Math.signum(at[0]) * 0.35);
                    body.m_20256_(new Vec3(in.f_82479_, v.f_82480_, in.f_82481_));
                    body.f_19864_ = true;
                }
            }
            return;
        }
        Set<Integer> down = FurutaCombat.hitSet(state, UNDER);
        if (age == beat.land()) {
            for (LivingEntity body : FurutaCombat.around(player, FurutaRules.CURTAIN_FOOT + FurutaRules.CURTAIN_TOP + 2.0)) {
                if (!under(local(player, state, body)) || !down.add(body.m_19879_())) {
                    continue;
                }
                FurutaCombat.strikeBody(player, state, body, beat.power(), new Vec3(0.0, -0.4, 0.0), beat.flags(), now, WoundBook.Blow.SLAM);
                EtoBlitz.fx(player, S2CEtoFx.CRUSHED, body.m_19879_(), KaguneServerController.center(body), new Vec3(0.0, -1.0, 0.0), body.m_20206_(), 0);
            }
            Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
            FurutaCombat.ground(player, player.m_20182_().m_82549_(ahead.m_82490_(FurutaRules.CURTAIN_FOOT + FurutaRules.CURTAIN_TOP * 0.5)), 5.0F);
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
            return;
        }
        if (age > beat.land() && age <= beat.end() - 4) {
            for (int id : down) {
                LivingEntity body = alive(player, id);
                if (body != null && EtoStun.stun(body, now, 6)) {
                    FurutaTails.pin(body); // (a player held too long breaks free)
                }
            }
        }
    }

    // ---------------------------------------------------------------- The King's Game

    /** The game (a job): the crescents at their prey in turn, each on its landing tick; his sword's rush landing; out of reach, over. */
    static void game(ServerPlayer player, FurutaServer.FurutaState state, long age, long now) {
        LivingEntity prey = alive(player, state.jobHeld);
        if (prey == null || prey.m_20280_(player) > FurutaRules.GAME_RANGE * FurutaRules.GAME_RANGE) {
            FurutaCentipedes.endJob(player, state, false, true);
            return;
        }
        for (FurutaMove.Beat beat : FurutaMove.KINGS_GAME.beats(FurutaCombat.limbs(player))) {
            if (age != beat.land() || !KaguneServerController.clearLineTo(player, prey)) {
                continue;
            }
            Vec3 into = AyatoFire.horizontal(KaguneServerController.center(prey).m_82546_(player.m_20182_()));
            FurutaCombat.strikeBody(player, state, prey, beat.power(), into.m_82490_(0.2), beat.flags(), now, WoundBook.Blow.STAB);
            EtoBlitz.fx(player, S2CEtoFx.IMPALED, prey.m_19879_(), KaguneServerController.center(prey), into, prey.m_20206_(), 0);
            KaguneServerController.playAt(player, prey, HexKaguneSounds.FLESH_IMPACT, 0.7F, KaguneServerController.vary(player, 1.1F, 0.1F));
        }
        if (state.kingStrikeAt != Long.MIN_VALUE && now >= state.kingStrikeAt) {
            state.kingStrikeAt = Long.MIN_VALUE;
            blow(player, state, prey, now);
        }
    }

    /**
     * M1 with a sword or an axe while the game is on: too far, he rushes in on the prey (his blow lands as he gets there); near, it
     * lands now. True if it was that.
     */
    static boolean sword(ServerPlayer player, FurutaServer.FurutaState state, long now) {
        if (state.job != FurutaMove.KINGS_GAME) {
            return false;
        }
        LivingEntity prey = alive(player, state.jobHeld);
        if (prey == null) {
            return false;
        }
        player.m_21011_(InteractionHand.MAIN_HAND, true);
        if (Math.sqrt(prey.m_20280_(player)) > FurutaRules.GAME_REACH) {
            Vec3 way = AyatoFire.horizontal(prey.m_20182_().m_82546_(player.m_20182_()));
            AyatoFire.push(player, way.m_82490_(FurutaRules.GAME_RUSH).m_82520_(0.0, 0.1, 0.0));
            state.fallSafe = true;
            state.kingStrikeAt = now + 3;
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.8F, KaguneServerController.vary(player, 1.3F, 0.05F));
            return true;
        }
        blow(player, state, prey, now);
        return true;
    }

    /** His own blade on the prey (as Minecraft swings it), the cut bleeding, a crescent driven in with it. */
    private static void blow(ServerPlayer player, FurutaServer.FurutaState state, LivingEntity prey, long now) {
        if (Math.sqrt(prey.m_20280_(player)) > FurutaRules.GAME_REACH + 1.5) {
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.6F, KaguneServerController.vary(player, 1.5F, 0.05F));
            return;
        }
        player.m_21011_(InteractionHand.MAIN_HAND, true);
        prey.f_19802_ = 0;
        player.m_5706_(prey);
        AyatoCrystal.furutaBleed(prey, player, 4, now);
        FurutaCombat.release(player, state, false);
        java.util.Arrays.fill(state.targets, 0);
        state.targets[0] = prey.m_19879_();
        Vec3 to = prey.m_20182_().m_82546_(player.m_20182_());
        FurutaCombat.begin(player, state, FurutaMove.KINGS_STRIKE, now, (float)Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_)),
            KaguneServerController.center(prey));
    }
}

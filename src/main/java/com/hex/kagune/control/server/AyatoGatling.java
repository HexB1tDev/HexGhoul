package com.hex.kagune.control.server;

import com.hex.kagune.control.ayato.AyatoRules;
import com.hex.kagune.control.ayato.WingForm;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Gatling (J, held; the original's Slingshot key, which Ukaku wings have no use for). In the first kakuja it is the
 * Needle Gatling ({@link #tickNeedles}); in the final kakuja the Pyre Gatling, needle salvos from the right tentacles and
 * fireballs from the fire tendrils taking turns; with the wings, only in the crystal mode: both
 * crystallized wings curl forward and spin up, then fire a stream of crystal spikes from their tips, the wings alternating,
 * faster and faster ({@link AyatoRules#gatlingRate}), at wherever he is looking (still on the target if it is roughly
 * there). Each spike is a light barrage spike that sticks and bleeds like any other ({@link AyatoCrystal#gatlingSpike}). It
 * drains RC every tick, more the longer it runs ({@link AyatoRules#gatlingDrain}), and stops when the trigger is let go, the
 * RC runs out, the wings go in or out of the crystal mode or a wing is thrown. The client tells the server the trigger is
 * held every few ticks ({@code C2SAyatoAction.GATLING}); without word for a moment it stops.
 */
final class AyatoGatling {
    /** Ticks between two "crystallize first" reminders. */
    private static final long NOTICE_TICKS = 60L;

    private AyatoGatling() {
    }

    /** True while his Gatling is spinning or firing (his wings are busy: no M1, no enders, no vortex). */
    static boolean firing(ServerPlayer player) {
        AyatoServer.AyatoState state = AyatoServer.peek(player);
        return state != null && state.gatling;
    }

    /** The trigger, from the client: {@code held} while J is down (repeated every few ticks), false when it is let go. */
    static void trigger(ServerPlayer player, boolean held) {
        AyatoServer.AyatoState state = AyatoServer.state(player);
        long now = player.m_9236_().m_46467_();
        if (!held) {
            stop(state, now);
            return;
        }
        if (!state.gatling) {
            boolean kakuja = AyatoServer.form(player).kakuja();
            // The final kakuja's Pyre Gatling comes with the form (mastery 1000).
            int needs = kakuja ? AyatoRules.NEEDLE_GATLING_MASTERY : AyatoRules.GATLING_MASTERY;
            if (KaguneProfile.mastery(player) < needs) {
                notice(player, state, now, AyatoServer.locked(kakuja ? "The Needle Gatling" : "The Gatling", needs));
                return;
            }
            if (!kakuja && !QuestServer.pass(player, com.hex.kagune.control.quest.Gate.AYATO_GATLING)) {
                return; // 7.0.1: the Gatling also needs its quest
            }
            if (!kakuja && !AyatoServer.crystalMode(player)) {
                notice(player, state, now, "message.hexkagune.ayato.gatling_crystal");
                return;
            }
            if (!kakuja && (!AyatoServer.form(player).wings() || AyatoServer.wingGone(player, 1, now) || AyatoServer.wingGone(player, -1, now)
                || AyatoVortex.throwing(player))) {
                return; // a wing is away as a vortex: both are needed
            }
            state.gatling = true;
            state.gatlingChangedAt = now;
            state.gatlingAccum = 0.0;
            state.gatlingShot = 0;
            state.dirty = true;
            AyatoFire.sound(player.m_284548_(), player.m_146892_(), "block.amethyst_cluster.place", 0.9F, KaguneServerController.vary(player, 1.4F, 0.05F));
        }
        state.gatlingHeldUntil = now + AyatoRules.GATLING_KEEPALIVE;
    }

    /** Spins, drains and fires. Called every server tick for Ayato players. */
    static void tick(ServerPlayer player, AyatoServer.AyatoState state, long now) {
        if (!state.gatling) {
            return;
        }
        WingForm form = AyatoServer.form(player);
        if (form.kakuja()) {
            tickNeedles(player, state, now, form.finalKakuja());
            return;
        }
        boolean wingsFree = !AyatoServer.wingGone(player, 1, now) && !AyatoServer.wingGone(player, -1, now);
        if (now > state.gatlingHeldUntil || !KaguneProfile.isActive(player) || !player.m_6084_() || !AyatoServer.crystalMode(player)
            || !wingsFree || !form.wings()) {
            stop(state, now);
            return;
        }
        int mastery = KaguneProfile.mastery(player);
        long held = now - state.gatlingChangedAt;
        if (!KaguneServerController.useEnergy(player, AyatoRules.gatlingDrain(held, mastery), true)) {
            stop(state, now);
            return;
        }
        state.gatlingAccum += AyatoRules.gatlingRate(held);
        ServerLevel level = player.m_284548_();
        while (state.gatlingAccum >= 1.0) {
            state.gatlingAccum -= 1.0;
            int side = form.rightWing() && state.gatlingShot % 2 == 0 ? 1 : -1;
            Vec3 aim = aim(player, mastery);
            Vec3 tip = AyatoFire.tip(player, side, aim);
            double[] spread = AyatoRules.spreadDirection(aim.f_82479_ - tip.f_82479_, aim.f_82480_ - tip.f_82480_, aim.f_82481_ - tip.f_82481_,
                AyatoRules.GATLING_SPREAD, player.m_217043_().m_188500_(), player.m_217043_().m_188500_());
            if (AyatoCrystal.gatlingSpike(player, tip, new Vec3(spread[0], spread[1], spread[2]), AyatoRules.gatlingDamage(mastery),
                AyatoRules.GATLING_MOST_IN_FLIGHT)) {
                state.gatlingShot++;
                if (state.gatlingShot % 2 == 1) {
                    AyatoFire.sound(level, tip, "block.amethyst_block.hit", 0.5F,
                        KaguneServerController.vary(player, 1.5F + 0.4F * (float)Math.min(1.0, held / 40.0), 0.1F));
                }
            }
        }
        // The stream trains the kagune once a second while it hits something (not once per spike).
        if (held % 20L == 19L && AyatoCrystal.gatlingLanded(player)) {
            KaguneProfile.addMastery(player, "Swipe", 1);
            KaguneNetwork.broadcast(player);
        }
    }

    /** How big the Pyre Gatling's fireballs are drawn, against a fire shot. */
    private static final float PYRE_FIREBALL_SCALE = 1.5F;

    /**
     * The kakuja's Needle Gatling: all six turned on the target, salvos of two needles from one tentacle at a time, round the
     * six (the sides taking turns, never all at once), faster and faster, draining RC more the longer it runs. The final
     * kakuja's ({@code pyre}): a needle salvo from a right tentacle, then a fireball from a fire tendril, and so on round them.
     */
    private static void tickNeedles(ServerPlayer player, AyatoServer.AyatoState state, long now, boolean pyre) {
        if (now > state.gatlingHeldUntil || !KaguneProfile.isActive(player) || !player.m_6084_()) {
            stop(state, now);
            return;
        }
        int mastery = KaguneProfile.mastery(player);
        long held = now - state.gatlingChangedAt;
        if (!KaguneServerController.useEnergy(player, AyatoRules.needleGatlingDrain(held, mastery), true)) {
            stop(state, now);
            return;
        }
        state.gatlingAccum += AyatoRules.needleGatlingRate(held);
        ServerLevel level = player.m_284548_();
        while (state.gatlingAccum >= 1.0) {
            state.gatlingAccum -= 1.0;
            Vec3 aim = aim(player, mastery, AyatoRules.NEEDLE_RANGE);
            float pitch = 0.4F * (float)Math.min(1.0, held / 40.0);
            // 5.0.18: with the Steel Blade out the steel tentacles are his blade: only the fire tendrils fire, fireball after fireball.
            boolean fireOnly = pyre && state.blade;
            if (pyre && (fireOnly || AyatoRules.pyreGatlingFire(state.gatlingShot))) {
                int index = fireOnly ? 3 + Math.floorMod(state.gatlingShot, 3) : AyatoRules.pyreGatlingTentacle(state.gatlingShot);
                Vec3 mouth = AyatoPyre.mouth(player, index, aim);
                float yaw = (player.m_217043_().m_188501_() * 2.0F - 1.0F) * AyatoRules.GATLING_SPREAD;
                AyatoFire.shard(player, mouth, aim, AyatoFire.Kind.SHOT, AyatoRules.pyreGatlingFireDamage(mastery), yaw, PYRE_FIREBALL_SCALE, 0L);
                state.gatlingShot++;
                AyatoFire.sound(level, mouth, "entity.blaze.shoot", 0.4F, KaguneServerController.vary(player, 1.2F + pitch, 0.1F));
                continue;
            }
            int index = pyre ? AyatoRules.pyreGatlingTentacle(state.gatlingShot) : AyatoRules.sixTailTentacle(state.gatlingShot);
            if (AyatoCrystal.needleSalvo(player, index, aim, AyatoRules.needleGatlingDamage(mastery), AyatoRules.NEEDLE_GATLING_PER_SALVO,
                AyatoRules.GATLING_MOST_IN_FLIGHT)) {
                state.gatlingShot++;
                if (pyre || state.gatlingShot % 2 == 1) {
                    AyatoFire.sound(level, player.m_146892_(), "block.amethyst_block.hit", 0.45F, KaguneServerController.vary(player, 1.7F + pitch, 0.1F));
                }
            }
        }
        if (held % 20L == 19L && AyatoCrystal.gatlingLanded(player)) {
            KaguneProfile.addMastery(player, "Swipe", 1);
            KaguneNetwork.broadcast(player);
        }
    }

    /** Where the stream goes: where he looks, or the body he is roughly looking at. */
    private static Vec3 aim(ServerPlayer player, int mastery) {
        return aim(player, mastery, AyatoRules.crystalRange(mastery));
    }

    /** {@link #aim(ServerPlayer, int)} out to {@code range}. */
    private static Vec3 aim(ServerPlayer player, int mastery, double range) {
        ServerLevel level = player.m_284548_();
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        LivingEntity target = KaguneServerController.traceLiving(player, range, false);
        if (target != null) {
            Vec3 centre = KaguneServerController.center(target);
            Vec3 to = centre.m_82546_(eye);
            if (to.m_82556_() > 1.0E-4 && to.m_82541_().m_82526_(look) > 0.94) {
                return centre;
            }
        }
        Vec3 end = eye.m_82549_(look.m_82490_(range));
        BlockHitResult block = level.m_45547_(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return block.m_6662_() == HitResult.Type.BLOCK ? block.m_82450_() : end;
    }

    private static void stop(AyatoServer.AyatoState state, long now) {
        if (state.gatling) {
            state.gatling = false;
            state.gatlingChangedAt = now;
            state.dirty = true;
        }
    }

    private static void notice(ServerPlayer player, AyatoServer.AyatoState state, long now, String message) {
        notice(player, state, now, Component.m_237115_(message));
    }

    private static void notice(ServerPlayer player, AyatoServer.AyatoState state, long now, Component message) {
        if (now >= state.nextNoticeAt || now < state.nextNoticeAt - NOTICE_TICKS) {
            state.nextNoticeAt = now + NOTICE_TICKS;
            player.m_5661_(message, true);
        }
    }
}

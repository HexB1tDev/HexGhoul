package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.client.animation.EtoMotions;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.client.render.EtoAfterimages;
import com.hex.kagune.control.eto.EtoMove;
import com.hex.kagune.control.eto.EtoRules;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Speed Kick's dash and blow, seen. 6.2.0 (Kizaru's kick): into the lunge the ground blows out round her feet; she is gone, a
 * pale afterimage of her lunge left where she was and the air bursting off it; all along the way (nothing of her is drawn) the
 * ground is torn up in a wake behind where she went and the wind streams on; where she is there, the air cracks round her; at the
 * blow a blinding flash on its middle, rays of light bursting out every way, rings of shock, a cone of torn air thrown on the way it
 * flies and the dust blasted off the ground below; where she lands from the backflip, dust. All from what every client already
 * knows (her move and her path): no packets.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class KickDust {
    /** Beyond this (blocks from the eye) nothing is thrown up. */
    static final double RANGE = 64.0;
    /** The dust and the grit (a pale earth, darker bits of it). */
    static final int DUST = 0xC8D2C6B4;
    static final int DUST_FADE = 0x00B4A894;
    static final int GRIT = 0xFF6E6252;
    static final int GRIT_FADE = 0x004A4034;
    /** How much: off the mark, one puff every so many blocks of the dash (and at most), grit a tick, where she springs, at the blow. */
    static final float OFF = 18.0F;
    static final double WAKE_EVERY = 0.55;
    static final int WAKE_MOST = 16;
    static final float GRIT_TICK = 6.0F;
    static final float STREAKS = 4.0F;
    static final float SPRING = 10.0F;
    static final float BLOW = 14.0F;
    static final float LANDING = 8.0F;
    /** None this close to her own eyes in first person (it would cloud her view). */
    static final double CLEARANCE = 1.1;

    /** One kick being followed: which (when it began) and how far in it was last tick. */
    private static final class Seen {
        long started;
        double last = -1.0;
        long tick;
    }

    private static final Map<UUID, Seen> SEEN = new HashMap<>();
    private static final java.util.Random RANDOM = new java.util.Random();

    private KickDust() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (event.phase != TickEvent.Phase.END || level == null || minecraft.m_91104_() || minecraft.f_91063_ == null) {
            if (level == null) {
                SEEN.clear();
            }
            return;
        }
        long now = level.m_46467_();
        double clock = EtoClient.clock(0.0F);
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        boolean firstPersonView = minecraft.f_91066_.m_92176_().m_90612_();
        for (AbstractClientPlayer player : level.m_6907_()) {
            EtoClient.EtoView view = EtoClient.get(player);
            if (view.move() != EtoMove.KICK && view.move() != EtoMove.OWL_KICK && view.move() != EtoMove.WITNESS || !player.m_6084_()
                || player.m_20182_().m_82557_(camera) > RANGE * RANGE) {
                continue;
            }
            Seen seen = SEEN.computeIfAbsent(player.m_20148_(), id -> new Seen());
            if (seen.started != view.startedAt()) {
                seen.started = view.startedAt();
                seen.last = -1.0;
            }
            seen.tick = now;
            Vec3 eye = player == minecraft.f_91074_ && firstPersonView ? player.m_146892_() : null;
            if (view.move() == EtoMove.WITNESS) {
                witness(player, view, seen.last, view.age(clock), eye, clock);
            } else if (view.move() == EtoMove.OWL_KICK) {
                owlDash(player, view, seen.last, view.age(clock), eye, clock);
            } else {
                throwUp(player, view, seen.last, view.age(clock), eye, clock);
            }
            seen.last = view.age(clock);
        }
        if (!SEEN.isEmpty()) {
            SEEN.values().removeIf(seen -> seen.tick != now);
        }
    }

    /** What this tick of her kick throws up, from {@code last} ticks in to {@code age}; {@code eye} hers when it is her own view. */
    private static void throwUp(AbstractClientPlayer player, EtoClient.EtoView view, double last, double age, Vec3 eye, double clock) {
        int dash = Math.max(1, view.detail());
        int arrive = EtoRules.kickArrive(dash);
        double yaw = Math.toRadians(view.yaw());
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        Vec3 was = new Vec3(player.f_19790_, player.f_19791_, player.f_19792_);
        Vec3 here = player.m_20182_();
        // The ground under her: her feet less however far up she is.
        double lastUp = EtoRules.kickHeight(dash, view.charge(), Math.max(0.0, last));
        double up = EtoRules.kickHeight(dash, view.charge(), age);
        Vec3 groundWas = was.m_82520_(0.0, -lastUp, 0.0);
        Vec3 ground = here.m_82520_(0.0, -up, 0.0);
        if (last < 0.0 && age < 1.5) {
            blowOut(groundWas, facing, eye);
        }
        double burst = EtoRules.KICK_BURST;
        if (last < burst && age >= burst) {
            // Gone: her lunge left behind where she was, the air bursting off it.
            EtoAfterimages.add(EtoMotions.kickBody(burst - 0.01, dash, 0.0F), was, view.yaw(), clock);
            vanish(was, facing, eye);
        }
        if (EtoRules.kickGone(dash, Math.max(0.0, last)) || EtoRules.kickGone(dash, age)) {
            wake(groundWas, ground, facing, up, eye);
        }
        if (last < arrive && age >= arrive) {
            appear(here, facing, eye);
        }
        double contact = arrive + EtoRules.KICK_CONTACT;
        if (last >= 0.0 && last < contact && age >= contact) {
            double[] flight = EtoRules.kickFlight();
            Vec3 flies = right.m_82490_(flight[0]).m_82549_(facing.m_82490_(flight[1]));
            Vec3 foot = here.m_82549_(right.m_82490_(EtoRules.KICK_FOOT_X)).m_82549_(facing.m_82490_(EtoRules.KICK_FOOT_Z))
                .m_82520_(0.0, EtoRules.KICK_FOOT_Y, 0.0);
            blow(foot, flies, ground.f_82480_, eye);
        }
        double land = arrive + EtoRules.KICK_LAND;
        if (last >= 0.0 && last < land && age >= land) {
            puffs(here, Vec3.f_82478_, LANDING, 0.12, eye);
        }
    }

    /**
     * 6.2.0, Witness This: each time she blinks to a new place (behind it, every blitz blow, above it) a pale afterimage of her is
     * left where she was, in the pose she was in, and the air cracks where she is.
     */
    private static void witness(AbstractClientPlayer player, EtoClient.EtoView view, double last, double age, Vec3 eye, double clock) {
        Vec3 was = new Vec3(player.f_19790_, player.f_19791_, player.f_19792_);
        for (int tick = (int)Math.floor(Math.max(0.0, last)) + 1; tick <= age; tick++) {
            if (!com.hex.kagune.control.eto.WitnessRules.blinks(tick)) {
                continue;
            }
            RigPose pose = EtoMotions.witness(tick - 0.01, null);
            pose.settle(1.0F);
            EtoAfterimages.add(pose, was, view.yaw(), clock);
            Vec3 middle = player.m_20182_().m_82520_(0.0, 0.9, 0.0);
            if (far(middle, eye)) {
                RcFx.at(RcFx.RING, false, 5, middle).colors(0xCCFFFFFF, RcFx.clear(0xFFE8E4DE)).size(1.4F, 0.2F).bloom(0.0F).spawn();
            }
        }
    }

    /** 6.2.0: where she is gone from, the air bursting off the shape of her and torn on ahead the way she went. */
    private static void vanish(Vec3 feet, Vec3 facing, Vec3 eye) {
        Vec3 middle = feet.m_82520_(0.0, 0.8, 0.0);
        RcFx.at(RcFx.RING, false, 7, middle).colors(0xCCFFFFFF, RcFx.clear(0xFFE8E4DE)).size(0.3F, 2.2F).bloom(0.0F).spawn();
        for (int i = RcFx.count(10.0F); i > 0; i--) {
            Vec3 at = middle.m_82520_(RANDOM.nextGaussian() * 0.3, RANDOM.nextGaussian() * 0.35, RANDOM.nextGaussian() * 0.3);
            if (far(at, eye)) {
                RcFx.at(RcFx.STREAK, false, 4 + RANDOM.nextInt(3), at).velocity(facing.m_82490_(0.9 + 0.8 * RANDOM.nextDouble())).drag(0.7F)
                    .colors(0xAAFFFFFF, RcFx.clear(0xFFE8E4DE)).size(0.08F, 0.02F).bloom(0.0F).spawn();
            }
        }
        puffs(feet, facing.m_82490_(0.1), 10.0F, 0.18, eye);
    }

    /** 6.2.0: where she is there, out of nowhere: the air cracks round her and the dust is shoved off the ground. */
    private static void appear(Vec3 feet, Vec3 facing, Vec3 eye) {
        Vec3 middle = feet.m_82520_(0.0, 0.8, 0.0);
        if (far(middle, eye)) {
            RcFx.at(RcFx.RING, false, 6, middle).colors(0xDDFFFFFF, RcFx.clear(0xFFE8E4DE)).size(1.8F, 0.2F).bloom(0.0F).spawn();
        }
        for (int i = RcFx.count(8.0F); i > 0; i--) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            Vec3 out = new Vec3(Math.cos(angle), RANDOM.nextGaussian() * 0.3, Math.sin(angle));
            Vec3 at = middle.m_82549_(out.m_82490_(1.2));
            if (far(at, eye)) {
                RcFx.at(RcFx.STREAK, false, 3 + RANDOM.nextInt(2), at).velocity(out.m_82490_(-0.4)).drag(0.6F).colors(0x99FFFFFF, RcFx.clear(0xFFE8E4DE))
                    .size(0.07F, 0.02F).bloom(0.0F).spawn();
            }
        }
        puffs(feet, facing.m_82490_(0.04), 8.0F, 0.14, eye);
    }

    /**
     * 6.2.0, the Owl's kick: off the mark the ground blown out; the dash all but unseen, only the wake of it and a trail of her
     * afterimages, close together, along the way; where she plants, the ground blown out again.
     */
    private static void owlDash(AbstractClientPlayer player, EtoClient.EtoView view, double last, double age, Vec3 eye, double clock) {
        int dash = Math.max(1, view.detail());
        double yaw = Math.toRadians(view.yaw());
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 was = new Vec3(player.f_19790_, player.f_19791_, player.f_19792_);
        Vec3 here = player.m_20182_();
        if (last < 0.0 && age < 1.5) {
            blowOut(was, facing, eye);
        }
        if (last < dash && age > 0.0) {
            wake(was, here, facing, 0.0, eye);
            Vec3 way = here.m_82546_(was);
            double length = way.m_82553_();
            // Afterimages strung out along the way she went, each her shape as she was there.
            int images = Math.min(4, (int)Math.ceil(length / 1.5));
            for (int i = 0; i < images; i++) {
                double s = i / (double)Math.max(1, images);
                double at = Math.max(0.0, last) + (age - Math.max(0.0, last)) * s;
                RigPose pose = com.hex.kagune.control.client.animation.OwlMotions.anyBody(EtoMove.OWL_KICK, at, dash, 0.0F, 0.0F);
                if (pose != null) {
                    EtoAfterimages.add(pose, was.m_82549_(way.m_82490_(s)), view.yaw(), clock);
                }
            }
        }
        if (last < dash && age >= dash) {
            puffs(here, facing.m_82490_(0.08), SPRING + 6.0F, 0.22, eye);
        }
    }

    /** Off the mark: the ground blown out round her feet and thrown back behind her, grit and all. */
    private static void blowOut(Vec3 feet, Vec3 facing, Vec3 eye) {
        RcFx.at(RcFx.RING, false, 9, feet.m_82520_(0.0, 0.08, 0.0)).colors(DUST, DUST_FADE).size(0.3F, 2.4F).bloom(0.0F).spawn();
        puffs(feet, facing.m_82490_(-0.12), OFF, 0.2, eye);
        for (int i = RcFx.count(OFF); i > 0; i--) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            Vec3 kick = facing.m_82490_(-0.12 - 0.12 * RANDOM.nextDouble())
                .m_82520_(Math.cos(angle) * 0.1, 0.14 + 0.16 * RANDOM.nextDouble(), Math.sin(angle) * 0.1);
            grit(feet.m_82520_(RANDOM.nextGaussian() * 0.2, 0.05, RANDOM.nextGaussian() * 0.2), kick, eye);
        }
    }

    /**
     * One tick of the dash, from {@code from} to {@code to} (the ground under her): dust torn up from under her and rolling out
     * behind and to both sides in a wake, grit thrown up behind, the wind streaming off her body (she is {@code up} off the ground).
     */
    private static void wake(Vec3 from, Vec3 to, Vec3 facing, double up, Vec3 eye) {
        Vec3 way = to.m_82546_(from);
        double length = way.m_82553_();
        int steps = Math.min(WAKE_MOST, (int)Math.ceil(length / WAKE_EVERY * RcFx.density()));
        Vec3 side = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        for (int i = 0; i < steps; i++) {
            double s = (i + RANDOM.nextDouble()) / Math.max(1, steps);
            Vec3 at = from.m_82549_(way.m_82490_(s)).m_82520_(RANDOM.nextGaussian() * 0.12, 0.06, RANDOM.nextGaussian() * 0.12);
            double out = (RANDOM.nextBoolean() ? 1.0 : -1.0) * (0.04 + 0.06 * RANDOM.nextDouble());
            Vec3 roll = side.m_82490_(out).m_82549_(facing.m_82490_(-0.02 - 0.03 * RANDOM.nextDouble())).m_82520_(0.0, 0.015 + 0.02 * RANDOM.nextDouble(), 0.0);
            if (far(at, eye)) {
                RcFx.at(RcFx.PUFF, false, 20 + RANDOM.nextInt(14), at).velocity(roll).drag(0.9F).fall(-0.002F).colors(DUST, DUST_FADE)
                    .size(0.25F, 1.0F + 0.5F * RANDOM.nextFloat()).spawn();
            }
        }
        for (int i = RcFx.count(GRIT_TICK); i > 0; i--) {
            Vec3 at = from.m_82549_(way.m_82490_(RANDOM.nextDouble())).m_82520_(RANDOM.nextGaussian() * 0.1, 0.05, RANDOM.nextGaussian() * 0.1);
            Vec3 kick = facing.m_82490_(-0.08 - 0.1 * RANDOM.nextDouble())
                .m_82520_(RANDOM.nextGaussian() * 0.06, 0.12 + 0.14 * RANDOM.nextDouble(), RANDOM.nextGaussian() * 0.06);
            grit(at, kick, eye);
        }
        // The wind off her: thin pale streaks along her body, going her way.
        Vec3 along = length < 1.0E-6 ? facing : way.m_82490_(1.0 / length);
        for (int i = RcFx.count(STREAKS); i > 0; i--) {
            Vec3 at = from.m_82549_(way.m_82490_(RANDOM.nextDouble())).m_82520_(RANDOM.nextGaussian() * 0.25, up + 0.3 + 1.3 * RANDOM.nextDouble(),
                RANDOM.nextGaussian() * 0.25);
            if (far(at, eye)) {
                RcFx.at(RcFx.STREAK, false, 4 + RANDOM.nextInt(3), at).velocity(along.m_82490_(0.4 + 0.4 * RANDOM.nextDouble())).drag(0.7F)
                    .colors(0x88FFFFFF, RcFx.clear(0xFFE8E4DE)).size(0.07F, 0.02F).bloom(0.0F).spawn();
            }
        }
    }

    /**
     * 6.2.0, at the blow: a blinding flash on its middle, rays of light bursting out every way from it (the Kizaru frame), rings of
     * shock, a cone of torn air and streaks thrown on the way it flies, puffs blown on after it, and the dust blasted off the ground
     * below ({@code groundY}) in a ring.
     */
    private static void blow(Vec3 foot, Vec3 flies, double groundY, Vec3 eye) {
        RcFx.at(RcFx.MOTE, true, 6, foot).colors(0xFFFFFFFF, RcFx.clear(0xFFFFF4D8)).size(1.4F, 3.2F).bloom(0.0F).spawn();
        RcFx.at(RcFx.MOTE, true, 9, foot).colors(0xCCFFF2C8, RcFx.clear(0xFFFFE0A0)).size(0.6F, 2.2F).bloom(1.0F).spawn();
        RcFx.at(RcFx.RING, true, 7, foot).colors(0xFFFFFFFF, RcFx.clear(0xFFFFF4D8)).size(0.15F, 2.6F).bloom(0.0F).spawn();
        RcFx.at(RcFx.RING, false, 12, foot).colors(0xBBFFFFFF, RcFx.clear(0xFFE8E4DE)).size(0.4F, 5.0F).bloom(1.0F).spawn();
        RcFx.at(RcFx.RING, false, 10, foot.m_82549_(flies.m_82490_(1.2))).colors(0xAAFFFFFF, RcFx.clear(0xFFE8E4DE)).size(0.3F, 3.4F).bloom(1.0F)
            .spawn();
        RcFx.at(RcFx.RING, false, 12, foot.m_82549_(flies.m_82490_(2.6))).colors(0x88FFFFFF, RcFx.clear(0xFFE8E4DE)).size(0.2F, 2.4F).bloom(2.0F)
            .spawn();
        // Rays of light bursting out every way.
        for (int i = RcFx.count(24.0F); i > 0; i--) {
            Vec3 ray = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian() * 0.8, RANDOM.nextGaussian());
            if (ray.m_82556_() < 1.0E-4) {
                continue;
            }
            RcFx.at(RcFx.STREAK, true, 4 + RANDOM.nextInt(4), foot).velocity(ray.m_82541_().m_82490_(0.9 + 0.9 * RANDOM.nextDouble())).drag(0.72F)
                .colors(0xFFFFFFFF, RcFx.clear(0xFFFFF0C0)).size(0.12F, 0.03F).bloom(0.0F).spawn();
        }
        // The air torn on the way it flies: a cone of streaks and puffs.
        for (int i = RcFx.count(16.0F); i > 0; i--) {
            Vec3 ray = flies.m_82490_(1.2 + 1.2 * RANDOM.nextDouble())
                .m_82520_(RANDOM.nextGaussian() * 0.25, RANDOM.nextGaussian() * 0.25, RANDOM.nextGaussian() * 0.25);
            RcFx.at(RcFx.STREAK, true, 5 + RANDOM.nextInt(3), foot).velocity(ray).drag(0.8F).colors(0xFFFFFFFF, RcFx.clear(0xFFFFF4D8))
                .size(0.1F, 0.03F).bloom(0.0F).spawn();
        }
        for (int i = RcFx.count(BLOW); i > 0; i--) {
            Vec3 at = foot.m_82520_(RANDOM.nextGaussian() * 0.2, RANDOM.nextGaussian() * 0.2, RANDOM.nextGaussian() * 0.2);
            Vec3 push = flies.m_82490_(0.12 + 0.16 * RANDOM.nextDouble())
                .m_82520_(RANDOM.nextGaussian() * 0.04, RANDOM.nextGaussian() * 0.04 + 0.01, RANDOM.nextGaussian() * 0.04);
            if (far(at, eye)) {
                RcFx.at(RcFx.PUFF, false, 16 + RANDOM.nextInt(8), at).velocity(push).drag(0.88F).colors(0xCCF4F0EA, RcFx.clear(0xFFD8D2C8))
                    .size(0.4F, 1.5F).spawn();
            }
        }
        // The dust blasted off the ground below, in a ring.
        Vec3 below = new Vec3(foot.f_82479_, groundY, foot.f_82481_);
        puffs(below, flies.m_82490_(0.08), 18.0F, 0.32, eye);
    }

    /** A puff of dust blown off the ground round {@code feet} and a little {@code drift}, {@code out} fast. */
    private static void puffs(Vec3 feet, Vec3 drift, float wanted, double out, Vec3 eye) {
        for (int i = RcFx.count(wanted); i > 0; i--) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double speed = out * (0.5 + 0.5 * RANDOM.nextDouble());
            Vec3 at = feet.m_82520_(Math.cos(angle) * 0.2, 0.08, Math.sin(angle) * 0.2);
            Vec3 push = new Vec3(Math.cos(angle) * speed, 0.01 + 0.02 * RANDOM.nextDouble(), Math.sin(angle) * speed).m_82549_(drift);
            if (far(at, eye)) {
                RcFx.at(RcFx.PUFF, false, 18 + RANDOM.nextInt(12), at).velocity(push).drag(0.88F).fall(-0.002F).colors(DUST, DUST_FADE)
                    .size(0.3F, 1.2F).spawn();
            }
        }
    }

    /** A bit of grit flung up off the ground, falling back. */
    private static void grit(Vec3 at, Vec3 velocity, Vec3 eye) {
        if (far(at, eye)) {
            RcFx.at(RcFx.MOTE, false, 12 + RANDOM.nextInt(10), at).velocity(velocity).fall(0.045F).drag(0.95F).colors(GRIT, GRIT_FADE)
                .size(0.06F, 0.04F).bloom(0.0F).spawn();
        }
    }

    private static boolean far(Vec3 at, Vec3 eye) {
        return eye == null || at.m_82557_(eye) >= CLEARANCE * CLEARANCE;
    }
}

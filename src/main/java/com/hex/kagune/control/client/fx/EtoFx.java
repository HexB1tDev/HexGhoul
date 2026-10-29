package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.client.EtoCamera;
import com.hex.kagune.control.client.animation.KaguneBodyAnimation;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.client.render.EtoAfterimages;
import com.hex.kagune.control.network.S2CEtoFx;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** 6.1.0: Eto's effects on every client (impacts, blinks, the hammer's shockwave, bodies smashing through blocks). */
public final class EtoFx {
    private static final Random RANDOM = new Random();
    /** Impacts are drawn this close to the camera (blocks). */
    private static final double RANGE = 48.0;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int PALE = 0xFFF2EDE6;
    private static final int DUST = 0x99D9D2C8;

    private EtoFx() {
    }

    public static void accept(S2CEtoFx message) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null) {
            return;
        }
        double time = EtoClient.clock(0.0F);
        switch (message.kind()) {
            case S2CEtoFx.IMPACT -> impact(minecraft, message, time);
            case S2CEtoFx.BLINK -> blink(minecraft, message, time);
            case S2CEtoFx.CRASH -> crash(message);
            case S2CEtoFx.REGROW -> regrow(message);
            case S2CEtoFx.SHOCKWAVE -> shockwave(minecraft, message, time);
            case S2CEtoFx.KICKED -> kicked(minecraft, message, time);
            case S2CEtoFx.IMPALED -> impaled(minecraft, message, time);
            case S2CEtoFx.CRUSHED -> crushed(minecraft, message, time);
            case S2CEtoFx.SMASH -> smash(minecraft, message, time);
            default -> {
            }
        }
    }

    /**
     * 6.2.0: a Watch This beak went through a chest: blood bursts out of its back the way the beak went (and a little out of the
     * front), and a mob is left with a hole through it ({@link EtoPunctures}; a player's is carved into them as a stab wound).
     */
    private static void impaled(Minecraft minecraft, S2CEtoFx message, double time) {
        Entity body = minecraft.f_91073_.m_6815_(message.target());
        Vec3 chest = new Vec3(message.x(), message.y(), message.z());
        Vec3 through = new Vec3(message.vx(), message.vy(), message.vz());
        through = through.m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : through.m_82541_();
        double half = body == null ? 0.3 : body.m_20205_() * 0.5;
        AyatoBlood.spurt(minecraft.f_91073_, chest.m_82549_(through.m_82490_(half)), through, 1.5F, body);
        AyatoBlood.spurt(minecraft.f_91073_, chest.m_82546_(through.m_82490_(half)), through.m_82490_(-0.5), 0.7F, body);
        if (body != null && !(body instanceof net.minecraft.world.entity.player.Player)) {
            com.hex.kagune.control.client.render.EtoPunctures.add(body, chest, through, time);
        }
        LocalPlayer self = minecraft.f_91074_;
        if (self != null && message.owner() == self.m_19879_()) {
            EtoCamera.kick(0.2F, time);
        } else if (self != null && message.target() == self.m_19879_()) {
            EtoCamera.kick(1.0F, time);
        }
    }

    /**
     * 6.2.0: the Mutated Arm's hand closed on a body (blood squeezed out of it the way the arm drove), or its sphere squeezed
     * everything in it (blood forced out between the folds, all round it, the last squeeze worst).
     */
    private static void crushed(Minecraft minecraft, S2CEtoFx message, double time) {
        Vec3 at = new Vec3(message.x(), message.y(), message.z());
        LocalPlayer self = minecraft.f_91074_;
        if (message.target() != 0) {
            Entity body = minecraft.f_91073_.m_6815_(message.target());
            Vec3 into = new Vec3(message.vx(), message.vy(), message.vz());
            into = into.m_82556_() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : into.m_82541_();
            AyatoBlood.spurt(minecraft.f_91073_, at, into.m_82490_(-0.4).m_82520_(0.0, 0.5, 0.0), 0.9F, body);
            if (self != null && message.target() == self.m_19879_()) {
                EtoCamera.kick(0.8F, time);
            }
            return;
        }
        if (!RcFx.near(at, RANGE)) {
            return;
        }
        double radius = message.size();
        int spurts = 4 + message.extra() * 2;
        for (int i = 0; i < spurts; i++) {
            double turn = RANDOM.nextDouble() * Math.PI * 2.0;
            double up = RANDOM.nextDouble() * 1.4 - 0.5;
            Vec3 way = new Vec3(Math.cos(turn), up, Math.sin(turn)).m_82541_();
            AyatoBlood.spurt(minecraft.f_91073_, at.m_82549_(way.m_82490_(radius * 0.95)), way, 0.8F + 0.25F * message.extra(), null);
        }
        if (self != null && message.owner() == self.m_19879_()) {
            EtoCamera.kick(0.3F + 0.1F * message.extra(), time);
        } else if (self != null && self.m_20182_().m_82554_(at) < radius + 1.5) {
            EtoCamera.kick(1.0F, time);
        }
    }

    /**
     * 6.2.0: the Speed Kick's foot landed: the body snaps, tumbles off, comes down and gets up ({@link EtoReactions#kicked}); the
     * view jolts for whoever threw it or took it (6.2.0: hard), and anyone near the blow feels it. The flash and the shock of it are
     * {@code KickDust}'s, the trail of its flight {@link EtoReactions}'.
     */
    private static void kicked(Minecraft minecraft, S2CEtoFx message, double time) {
        EtoReactions.kicked(message.target(), new Vec3(message.vx(), message.vy(), message.vz()), time);
        LocalPlayer self = minecraft.f_91074_;
        if (self != null && message.owner() == self.m_19879_()) {
            EtoCamera.kick(1.0F, time);
        } else if (self != null && message.target() == self.m_19879_()) {
            EtoCamera.kick(1.8F, time);
        } else if (self != null) {
            double away = self.m_20182_().m_82554_(new Vec3(message.x(), message.y(), message.z()));
            if (away < 12.0) {
                EtoCamera.kick((float)(0.8 * (1.0 - away / 12.0)), time);
            }
        }
    }

    /**
     * A blow landed: the body reels, the view kicks for whoever threw it or took it, and at the point of contact a white ring
     * snaps open, sparks spray the way it drove, and a hard blow throws a ring of dust off the body.
     */
    private static void impact(Minecraft minecraft, S2CEtoFx message, double time) {
        Vec3 at = new Vec3(message.x(), message.y(), message.z());
        Vec3 push = new Vec3(message.vx(), message.vy(), message.vz());
        float size = message.size();
        EtoReactions.hit(message.target(), push, size, time);
        LocalPlayer self = minecraft.f_91074_;
        if (self != null && message.owner() == self.m_19879_()) {
            EtoCamera.kick(0.28F * size, time);
        } else if (self != null && message.target() == self.m_19879_()) {
            EtoCamera.kick(0.9F * size, time);
        }
        if (!RcFx.near(at, RANGE)) {
            return;
        }
        Vec3 drive = push.m_82556_() > 1.0E-6 ? push.m_82541_() : new Vec3(0.0, 1.0, 0.0);
        if (message.extra() == S2CEtoFx.BONE) {
            bone(minecraft, message.target(), at, drive, size);
            return;
        }
        RcFx.at(RcFx.RING, true, 6, at).colors(WHITE, RcFx.clear(PALE)).size(0.12F, 0.7F + 0.55F * size).bloom(0.0F).spawn();
        int sparks = RcFx.count(5.0F + 6.0F * size);
        for (int i = 0; i < sparks; i++) {
            Vec3 spray = drive.m_82490_(0.22 + 0.3 * RANDOM.nextDouble())
                .m_82520_(RANDOM.nextGaussian() * 0.12, RANDOM.nextGaussian() * 0.12 + 0.03, RANDOM.nextGaussian() * 0.12);
            RcFx.at(RcFx.STREAK, true, 4 + RANDOM.nextInt(4), at).velocity(spray).drag(0.78F).colors(WHITE, RcFx.clear(PALE)).size(0.07F, 0.02F)
                .bloom(0.0F).spawn();
        }
        if (size >= 1.2F) {
            // Dust thrown off the body all round the line of the blow.
            Vec3 side = Math.abs(drive.f_82480_) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(-drive.f_82481_, 0.0, drive.f_82479_).m_82541_();
            Vec3 up = drive.m_82537_(side).m_82541_();
            int puffs = RcFx.count(4.0F + 4.0F * size);
            for (int i = 0; i < puffs; i++) {
                double turn = i * Math.PI * 2.0 / puffs + RANDOM.nextDouble() * 0.5;
                Vec3 out = side.m_82490_(Math.cos(turn)).m_82549_(up.m_82490_(Math.sin(turn))).m_82490_(0.1 + 0.04 * size).m_82549_(drive.m_82490_(0.05));
                RcFx.at(RcFx.PUFF, false, 12 + RANDOM.nextInt(6), at).velocity(out).drag(0.86F).colors(DUST, RcFx.clear(0xFFBDB5AA)).size(0.22F, 0.75F)
                    .spawn();
            }
        }
        if (size >= 1.7F) {
            // The finisher: a second, wider ring.
            RcFx.at(RcFx.RING, true, 10, at).colors(0xCCFFFFFF, RcFx.clear(PALE)).size(0.3F, 2.6F).bloom(1.0F).spawn();
        }
        if (message.extra() == S2CEtoFx.BIG_PUNCH) {
            bigPunch(at, drive, size);
        } else if (message.extra() == S2CEtoFx.OWL_PUNCH) {
            owlPunch(minecraft, at, drive, size, time);
        } else if (message.extra() == S2CEtoFx.OWL_WHIRL) {
            owlWhirl(at);
        } else if (message.extra() == S2CEtoFx.FLESH) {
            flesh(at, drive, size);
        }
    }

    /**
     * The swipe's blade cut: blood thrown the way the blade swung (the body's own wound bleeding after, from the bleed), and the
     * cut itself, a pale streak drawn across the body along the swing and torn off it.
     */
    private static void bone(Minecraft minecraft, int target, Vec3 at, Vec3 drive, float size) {
        Entity body = minecraft.f_91073_.m_6815_(target);
        AyatoBlood.spurt(minecraft.f_91073_, at, drive, 1.1F + 0.3F * size, body);
        Vec3 along = new Vec3(drive.f_82479_, 0.0, drive.f_82481_);
        along = along.m_82556_() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : along.m_82541_();
        int streaks = RcFx.count(14.0F);
        for (int i = 0; i < streaks; i++) {
            double t = i / (double)(streaks - 1) - 0.5;
            // A shallow arc across the body, the way the blade went.
            Vec3 on = at.m_82549_(along.m_82490_(t * 1.6)).m_82520_(0.0, -0.25 * t * t + RANDOM.nextGaussian() * 0.04, 0.0);
            RcFx.at(RcFx.STREAK, false, 5 + RANDOM.nextInt(3), on).velocity(along.m_82490_(0.25 + 0.15 * RANDOM.nextDouble())).drag(0.7F)
                .colors(0xDDF4EEE2, RcFx.clear(0xFFD8CDB8)).size(0.09F, 0.02F).bloom(0.0F).spawn();
        }
    }


    /** The flesh landed: a wet slap, pale-pink spatter thrown along the blow and a smear of a ring across it. */
    private static void flesh(Vec3 at, Vec3 drive, float size) {
        RcFx.at(RcFx.RING, true, 8, at).colors(0xCCF3D9D2, RcFx.clear(0xFFE7B9AE)).size(0.25F, 1.4F + 0.5F * size).bloom(0.0F).spawn();
        int drops = RcFx.count(10.0F + 6.0F * size);
        for (int i = 0; i < drops; i++) {
            Vec3 spray = drive.m_82490_(0.18 + 0.28 * RANDOM.nextDouble())
                .m_82520_(RANDOM.nextGaussian() * 0.14, RANDOM.nextGaussian() * 0.1 + 0.08, RANDOM.nextGaussian() * 0.14);
            RcFx.at(RcFx.MOTE, false, 10 + RANDOM.nextInt(8), at).velocity(spray).fall(0.04F).drag(0.92F).colors(0xFFF0D4CC, RcFx.clear(0xFFC98F86))
                .size(0.07F, 0.04F).spawn();
        }
    }

    /**
     * Big Punch: the air itself thrown. Rings one inside the next, the last as wide as a house; a cone of streaks along the launch;
     * dust torn off the ground under the blow.
     */
    private static void bigPunch(Vec3 at, Vec3 drive, float size) {
        for (int ring = 0; ring < 3; ring++) {
            RcFx.at(RcFx.RING, true, 8 + ring * 4, at.m_82549_(drive.m_82490_(0.3 * ring))).colors(0xEEFFFFFF, RcFx.clear(PALE))
                .size(0.4F + ring * 0.3F, 2.2F + ring * 1.6F + size * 0.4F).bloom(ring).spawn();
        }
        int streaks = RcFx.count(18.0F + 8.0F * size);
        for (int i = 0; i < streaks; i++) {
            Vec3 spray = drive.m_82490_(0.6 + 0.6 * RANDOM.nextDouble())
                .m_82520_(RANDOM.nextGaussian() * 0.2, RANDOM.nextGaussian() * 0.16, RANDOM.nextGaussian() * 0.2);
            RcFx.at(RcFx.STREAK, true, 6 + RANDOM.nextInt(6), at).velocity(spray).drag(0.82F).colors(WHITE, RcFx.clear(PALE)).size(0.1F, 0.03F)
                .bloom(0.0F).spawn();
        }
        Vec3 ground = new Vec3(at.f_82479_, at.f_82480_ - 0.9, at.f_82481_);
        int dust = RcFx.count(14.0F);
        for (int i = 0; i < dust; i++) {
            double turn = i * Math.PI * 2.0 / dust;
            Vec3 out = new Vec3(Math.cos(turn), 0.05, Math.sin(turn)).m_82490_(0.18 + 0.08 * RANDOM.nextDouble()).m_82549_(drive.m_82490_(0.08));
            RcFx.at(RcFx.PUFF, false, 16 + RANDOM.nextInt(8), ground).velocity(out).drag(0.88F).colors(DUST, RcFx.clear(0xFFBDB5AA))
                .size(0.35F, 1.1F).spawn();
        }
    }

    /**
     * 6.2.0, the Owl's Big Punch: the air thrown dead level along the blow, rings marching out along it one after another, wider and
     * wider; a tunnel of streaks racing on ahead the way it flies; the ground torn up under the blow in a wide ring of dust and
     * clods; and the view of everyone near jolted (harder the nearer).
     */
    private static void owlPunch(Minecraft minecraft, Vec3 at, Vec3 drive, float size, double time) {
        Vec3 level = new Vec3(drive.f_82479_, 0.0, drive.f_82481_);
        level = level.m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : level.m_82541_();
        LocalPlayer self = minecraft.f_91074_;
        if (self != null) {
            double away = self.m_20182_().m_82554_(at);
            if (away > 1.5 && away < 14.0) {
                EtoCamera.kick((float)(1.1 * (1.0 - away / 14.0)), time);
            }
        }
        for (int ring = 0; ring < 5; ring++) {
            RcFx.at(RcFx.RING, true, 7 + ring * 3, at.m_82549_(level.m_82490_(0.7 * ring))).colors(0xEEFFFFFF, RcFx.clear(PALE))
                .size(0.4F + ring * 0.25F, 1.8F + ring * 1.1F + size * 0.35F).bloom(ring < 2 ? 1.0F : 0.0F).spawn();
        }
        int streaks = RcFx.count(26.0F + 8.0F * size);
        for (int i = 0; i < streaks; i++) {
            double spread = 0.06 + 0.1 * RANDOM.nextDouble();
            Vec3 spray = level.m_82490_(0.9 + 0.9 * RANDOM.nextDouble())
                .m_82520_(RANDOM.nextGaussian() * spread, RANDOM.nextGaussian() * spread * 0.7, RANDOM.nextGaussian() * spread);
            RcFx.at(RcFx.STREAK, true, 6 + RANDOM.nextInt(7), at.m_82549_(level.m_82490_(RANDOM.nextDouble() * 1.5))).velocity(spray).drag(0.86F)
                .colors(WHITE, RcFx.clear(PALE)).size(0.11F, 0.03F).bloom(0.0F).spawn();
        }
        Vec3 ground = new Vec3(at.f_82479_, at.f_82480_ - 0.9, at.f_82481_);
        int dust = RcFx.count(22.0F);
        for (int i = 0; i < dust; i++) {
            double turn = i * Math.PI * 2.0 / dust;
            Vec3 out = new Vec3(Math.cos(turn), 0.04, Math.sin(turn)).m_82490_(0.22 + 0.1 * RANDOM.nextDouble()).m_82549_(level.m_82490_(0.14));
            RcFx.at(RcFx.PUFF, false, 18 + RANDOM.nextInt(10), ground).velocity(out).drag(0.88F).colors(DUST, RcFx.clear(0xFFBDB5AA))
                .size(0.4F, 1.4F).spawn();
        }
        int clods = RcFx.count(12.0F);
        for (int i = 0; i < clods; i++) {
            Vec3 throwUp = level.m_82490_(0.15 + 0.2 * RANDOM.nextDouble()).m_82520_(RANDOM.nextGaussian() * 0.12, 0.3 + 0.25 * RANDOM.nextDouble(),
                RANDOM.nextGaussian() * 0.12);
            RcFx.at(RcFx.MOTE, false, 18 + RANDOM.nextInt(10), ground).velocity(throwUp).fall(0.06F).drag(0.97F).colors(0xFF6B5A48, 0xFF4A3E32)
                .size(0.11F, 0.08F).spawn();
        }
    }

    /** 6.2.0, the Owl's Reaping Whirl beginning: a flat ring of torn air thrown out round her at the blades' height, dust off the ground. */
    private static void owlWhirl(Vec3 at) {
        RcFx.at(RcFx.RING, false, 10, at.m_82520_(0.0, 0.3, 0.0)).colors(0xAAFFFFFF, RcFx.clear(PALE)).size(0.5F, 4.4F).bloom(0.0F).spawn();
        Vec3 ground = at.m_82520_(0.0, -0.9, 0.0);
        int puffs = RcFx.count(20.0F);
        for (int i = 0; i < puffs; i++) {
            double turn = i * Math.PI * 2.0 / puffs + RANDOM.nextDouble() * 0.3;
            Vec3 out = new Vec3(Math.cos(turn), 0.02, Math.sin(turn)).m_82490_(0.2 + 0.08 * RANDOM.nextDouble());
            RcFx.at(RcFx.PUFF, false, 16 + RANDOM.nextInt(8), ground.m_82549_(out.m_82490_(2.0))).velocity(out).drag(0.87F).colors(DUST, RcFx.clear(0xFFBDB5AA))
                .size(0.35F, 1.1F).spawn();
        }
    }

    /**
     * 6.2.0: a body the Owl's punch drives broke through blocks ({@code size} of them this tick): debris thrown on along its line and
     * out, dust rolling off, a ring of the impact, and the view of anyone close jolted.
     */
    private static void smash(Minecraft minecraft, S2CEtoFx message, double time) {
        Vec3 at = new Vec3(message.x(), message.y(), message.z());
        Vec3 along = new Vec3(message.vx(), 0.0, message.vz());
        along = along.m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : along.m_82541_();
        float size = Math.max(1.0F, message.size());
        LocalPlayer self = minecraft.f_91074_;
        if (self != null) {
            double away = self.m_20182_().m_82554_(at);
            if (away < 10.0) {
                EtoCamera.kick((float)(Math.min(0.6, 0.15 + 0.05 * size) * (1.0 - away / 10.0)), time);
            }
        }
        if (!RcFx.near(at, RANGE)) {
            return;
        }
        RcFx.at(RcFx.RING, true, 6, at).colors(0xCCFFFFFF, RcFx.clear(PALE)).size(0.3F, 1.2F + 0.12F * size).bloom(0.0F).spawn();
        int chunks = RcFx.count(Math.min(30.0F, 6.0F + 2.5F * size));
        for (int i = 0; i < chunks; i++) {
            Vec3 thrown = along.m_82490_(0.3 + 0.45 * RANDOM.nextDouble())
                .m_82520_(RANDOM.nextGaussian() * 0.16, 0.08 + RANDOM.nextDouble() * 0.2, RANDOM.nextGaussian() * 0.16);
            int tone = RANDOM.nextInt(3);
            int colour = tone == 0 ? 0xFF7A7068 : tone == 1 ? 0xFF6B5A48 : 0xFF8F8A84;
            RcFx.at(RcFx.MOTE, false, 14 + RANDOM.nextInt(10), at.m_82520_(RANDOM.nextGaussian() * 0.3, RANDOM.nextGaussian() * 0.4,
                RANDOM.nextGaussian() * 0.3)).velocity(thrown).fall(0.06F).drag(0.95F).colors(colour, 0xFF3A332E).size(0.13F, 0.07F).spawn();
        }
        int puffs = RcFx.count(Math.min(16.0F, 4.0F + size));
        for (int i = 0; i < puffs; i++) {
            Vec3 out = along.m_82490_(0.06 + 0.08 * RANDOM.nextDouble())
                .m_82520_(RANDOM.nextGaussian() * 0.07, RANDOM.nextGaussian() * 0.05 + 0.02, RANDOM.nextGaussian() * 0.07);
            RcFx.at(RcFx.PUFF, false, 16 + RANDOM.nextInt(10), at).velocity(out).drag(0.87F).colors(DUST, RcFx.clear(0xFFBDB5AA)).size(0.35F, 1.1F)
                .spawn();
        }
    }

    /** A launched body slammed into something it could not break: dust off the wall and a ring where it hit. */
    private static void crash(S2CEtoFx message) {
        Vec3 at = new Vec3(message.x(), message.y(), message.z());
        if (!RcFx.near(at, RANGE)) {
            return;
        }
        Vec3 into = new Vec3(message.vx(), message.vy(), message.vz());
        Vec3 back = into.m_82556_() > 1.0E-6 ? into.m_82541_().m_82490_(-1.0) : new Vec3(0.0, 1.0, 0.0);
        RcFx.at(RcFx.RING, true, 7, at).colors(0xCCFFFFFF, RcFx.clear(PALE)).size(0.3F, 1.6F).bloom(0.0F).spawn();
        int puffs = RcFx.count(10.0F);
        for (int i = 0; i < puffs; i++) {
            Vec3 out = back.m_82490_(0.08 + 0.06 * RANDOM.nextDouble())
                .m_82520_(RANDOM.nextGaussian() * 0.08, RANDOM.nextGaussian() * 0.08, RANDOM.nextGaussian() * 0.08);
            RcFx.at(RcFx.PUFF, false, 14 + RANDOM.nextInt(8), at).velocity(out).drag(0.86F).colors(DUST, RcFx.clear(0xFFBDB5AA)).size(0.3F, 0.9F)
                .spawn();
        }
    }

    /**
     * The Flesh Hammer hit the ground: a flash where it landed, a ring of dust racing out along the ground as wide as the shockwave,
     * clods thrown up and falling back, and the view of everyone near shaken (harder the nearer).
     */
    private static void shockwave(Minecraft minecraft, S2CEtoFx message, double time) {
        Vec3 at = new Vec3(message.x(), message.y(), message.z());
        float radius = message.size();
        LocalPlayer self = minecraft.f_91074_;
        if (self != null) {
            double away = self.m_20182_().m_82554_(at);
            if (away < radius * 3.0) {
                EtoCamera.kick((float)(1.4 * (1.0 - away / (radius * 3.0))), time);
            }
        }
        if (!RcFx.near(at, RANGE)) {
            return;
        }
        Vec3 up = at.m_82520_(0.0, 0.15, 0.0);
        RcFx.at(RcFx.RING, true, 7, up).colors(0xCCE6E1DA, RcFx.clear(0xFFBDB5AA)).size(0.6F, radius * 0.9F).bloom(0.0F).spawn();
        RcFx.at(RcFx.RING, true, 12, up).colors(0x99E6DDD0, RcFx.clear(0xFFBDB5AA)).size(1.0F, radius * 1.5F).bloom(0.0F).spawn();
        int puffs = RcFx.count(26.0F + 8.0F * radius);
        for (int i = 0; i < puffs; i++) {
            double turn = i * Math.PI * 2.0 / puffs + RANDOM.nextDouble() * 0.3;
            Vec3 out = new Vec3(Math.cos(turn), 0.0, Math.sin(turn));
            double speed = 0.3 + 0.06 * radius + 0.12 * RANDOM.nextDouble();
            RcFx.at(RcFx.PUFF, false, 14 + RANDOM.nextInt(10), up.m_82549_(out.m_82490_(0.5))).velocity(out.m_82490_(speed).m_82520_(0.0, 0.03, 0.0))
                .drag(0.84F).colors(DUST, RcFx.clear(0xFFBDB5AA)).size(0.4F, 1.3F).spawn();
        }
        int clods = RcFx.count(14.0F + 5.0F * radius);
        for (int i = 0; i < clods; i++) {
            Vec3 throwUp = new Vec3(RANDOM.nextGaussian() * 0.16, 0.35 + 0.3 * RANDOM.nextDouble(), RANDOM.nextGaussian() * 0.16);
            RcFx.at(RcFx.MOTE, false, 18 + RANDOM.nextInt(10), up).velocity(throwUp).fall(0.06F).drag(0.97F).colors(0xFF6B5A48, 0xFF4A3E32)
                .size(0.11F, 0.08F).spawn();
        }
    }

    /** A smashed block grew back: a soft breath of pale dust where it formed. */
    private static void regrow(S2CEtoFx message) {
        Vec3 at = new Vec3(message.x(), message.y(), message.z());
        if (!RcFx.near(at, 32.0)) {
            return;
        }
        int puffs = RcFx.count(4.0F);
        for (int i = 0; i < puffs; i++) {
            Vec3 drift = new Vec3(RANDOM.nextGaussian() * 0.02, 0.01 + RANDOM.nextDouble() * 0.02, RANDOM.nextGaussian() * 0.02);
            RcFx.at(RcFx.PUFF, false, 14 + RANDOM.nextInt(6), at.m_82520_(RANDOM.nextGaussian() * 0.3, RANDOM.nextGaussian() * 0.3,
                RANDOM.nextGaussian() * 0.3)).velocity(drift).drag(0.9F).colors(0x66E8E2D8, RcFx.clear(0xFFE8E2D8)).size(0.3F, 0.7F).spawn();
        }
    }

    /**
     * She blinked: where she was, her body stays a moment as a pale afterimage, a breath of pale motes scattering off it; and if it
     * is this client's player, the view swings round to face where she now faces.
     */
    private static void blink(Minecraft minecraft, S2CEtoFx message, double time) {
        Entity owner = minecraft.f_91073_.m_6815_(message.owner());
        Vec3 from = new Vec3(message.x(), message.y(), message.z());
        if (owner instanceof AbstractClientPlayer player) {
            RigPose pose = KaguneBodyAnimation.drawn(player);
            if (pose != null) {
                EtoAfterimages.add(pose, from, player.f_20883_, time);
            }
        }
        if (RcFx.near(from, RANGE)) {
            int motes = RcFx.count(10.0F);
            for (int i = 0; i < motes; i++) {
                Vec3 at = from.m_82520_(RANDOM.nextGaussian() * 0.25, 0.3 + RANDOM.nextDouble() * 1.4, RANDOM.nextGaussian() * 0.25);
                Vec3 drift = new Vec3(RANDOM.nextGaussian() * 0.03, 0.02 + RANDOM.nextDouble() * 0.03, RANDOM.nextGaussian() * 0.03);
                RcFx.at(RcFx.MOTE, false, 10 + RANDOM.nextInt(8), at).velocity(drift).drag(0.9F).colors(0xAAF2EDE6, RcFx.clear(PALE)).size(0.08F, 0.03F)
                    .spawn();
            }
        }
        LocalPlayer self = minecraft.f_91074_;
        if (self != null && message.owner() == self.m_19879_()) {
            // The yaw she now faces rides in the size field.
            double turn = message.size() - self.m_146908_();
            turn -= 360.0 * Math.floor((turn + 180.0) / 360.0);
            EtoCamera.turnBy(turn, 3.0);
        }
    }
}

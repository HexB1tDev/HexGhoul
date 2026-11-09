package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.bikaku.BikakuMove;
import com.hex.kagune.control.bikaku.GrandDevour;
import com.hex.kagune.control.bikaku.MawMotion;
import com.hex.kagune.control.client.BikakuClient;
import com.hex.kagune.control.client.PaleClient;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.5.1: Carrion Eruption seen in the ground (the owner's spec: "a distinctive underground movement animation, emerging flesh particles,
 * ground debris"): dust and chips where the worm goes into the ground before him; the ground heaving and breaking all along over its head
 * as it tunnels; where it bursts up, the ground blown out (a crater, dust, chips, the ground shaking for whoever is near) and gobbets of
 * its flesh thrown up with it; dust as it sinks back and as it comes back out. All from what every client already knows (the move, when it
 * began, its mark): no packets.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class BikakuFx {
    /** Beyond this (blocks from the eye) nothing is thrown up. */
    static final double RANGE = 64.0;
    static final int DUST = 0xC0574E44;
    static final int DUST_FADE = 0x006E655A;
    static final int GRIT = 0xFF3A322A;
    static final int FLESH = 0xFF7A1424;
    static final int FLESH_DARK = 0xFF3A0610;

    /** Each player's eruption being followed: when it began and the last tick it was seen at. */
    private static final Map<UUID, double[]> SEEN = new HashMap<>();
    private static final java.util.Random RANDOM = new java.util.Random();

    private BikakuFx() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null || minecraft.f_91074_ == null) {
            SEEN.clear();
            return;
        }
        double clock = PaleClient.clock(0.0F);
        java.util.Set<UUID> here = new java.util.HashSet<>();
        for (AbstractClientPlayer player : level.m_6907_()) {
            here.add(player.m_36316_().getId());
            try {
                eruption(minecraft, level, player, clock);
            } catch (RuntimeException failure) {
                SEEN.remove(player.m_36316_().getId());
            }
            try {
                devour(player, clock);
            } catch (RuntimeException failure) {
                WET.remove(player.m_36316_().getId());
            }
        }
        SEEN.keySet().retainAll(here);
        WET.keySet().retainAll(here);
    }

    private static void eruption(Minecraft minecraft, ClientLevel level, AbstractClientPlayer player, double clock) {
        UUID id = player.m_36316_().getId();
        if (!BikakuClient.isBikaku(player)) {
            SEEN.remove(id);
            return;
        }
        BikakuClient.View view = BikakuClient.get(player);
        if (view.move() != BikakuMove.ERUPTION || !RcFx.near(player.m_20182_(), RANGE)) {
            SEEN.remove(id);
            return;
        }
        // 3.5.8: thrown further, every beat of it slower (on its own beats here).
        // 3.6.1: in a form of several worms each erupts in turn under its own body, a beat after the one before (seen per worm).
        double reach = Math.max(BikakuMove.ERUPTION.baseReach(), view.moveReach());
        double k = com.hex.kagune.control.bikaku.BikakuRules.slower(reach, BikakuMove.ERUPTION.baseReach());
        int worms = view.kakuja() ? 1 : Math.max(1, Math.min(3, view.moveWorms()));
        double[] seen = SEEN.computeIfAbsent(id, ignored -> new double[] {view.moveAt(), -1.0, -1.0, -1.0});
        if (seen[0] != view.moveAt()) {
            seen[0] = view.moveAt();
            seen[1] = -1.0;
            seen[2] = -1.0;
            seen[3] = -1.0;
        }
        double yaw = Math.toRadians(player.f_20883_);
        for (int w = 0; w < worms; w++) {
            double age = (view.moveAge(clock) - (worms >= 2 ? w * BikakuMove.ERUPTION.stagger() : 0)) / k;
            if (age < 0.0 || age > BikakuMove.ERUPTION.ticks() + 2) {
                continue;
            }
            int last = (int)Math.floor(seen[1 + w]);
            int now = (int)Math.floor(age);
            seen[1 + w] = age;
            if (now <= last) {
                continue;
            }
            Entity aimed = w < view.targets().length && view.targets()[w] != 0 ? level.m_6815_(view.targets()[w]) : null;
            double[] own = worms >= 2 && w < view.points().length ? view.points()[w] : null;
            Vec3 mark = aimed != null ? aimed.m_20182_().m_82520_(0.0, aimed.m_20206_() * 0.52, 0.0)
                : own != null ? new Vec3(own[0], own[1], own[2]) : new Vec3(view.mx(), view.my(), view.mz());
            double[] target = MawMotion.wormUnpoint(worms, w, rig(player, yaw, mark));
            for (int tick = Math.max(0, last + 1); tick <= now; tick++) {
                beat(minecraft, level, player, yaw, target, tick, reach, worms, w);
            }
        }
    }

    /** What the ground does on tick {@code tick} of the eruption. */
    private static void beat(Minecraft minecraft, ClientLevel level, AbstractClientPlayer player, double yaw, double[] target, int tick, double reach,
                             int worms, int w) {
        // 3.6.1: worked out in worm w's own lane ({@code target} is in it), placed back in his body's frame.
        double[] spot = MawMotion.wormPoint(worms, w, MawMotion.eruptSpot(target, reach));
        Vec3 entry = world(player, yaw, MawMotion.wormPoint(worms, w, new double[] {0.45, 0.05, 1.2}));
        if (tick == 6 || tick == BikakuMove.ERUPT_BACK + 1) {
            // It goes into the ground before him (and comes back out there).
            dust(entry, 10, 0.9);
            chips(entry, 8, 0.5);
        }
        double[] bulge = MawMotion.eruptBulge(target, tick, reach);
        if (bulge != null) {
            // The ground heaving over its head as it tunnels.
            Vec3 at = world(player, yaw, MawMotion.wormPoint(worms, w, bulge)).m_82520_(0.0, 0.1, 0.0);
            dust(at, 3, 0.5);
            chips(at, 3, 0.35);
        }
        Vec3 burst = world(player, yaw, spot);
        if (tick == BikakuMove.ERUPT_RISE + 1) {
            // It bursts up: the ground blown out, its flesh thrown up with it, the ground shaking for whoever is near.
            WarPathFx.crater(level, burst, 1.5);
            WarPathFx.feel(minecraft, burst, 12.0, 0.6F, 10.0);
            flesh(burst.m_82520_(0.0, 0.4, 0.0), 18);
        }
        if (tick == BikakuMove.ERUPTION.land()) {
            flesh(burst.m_82520_(0.0, 1.6, 0.0), 8);
        }
        if (tick > BikakuMove.ERUPT_HOLD && tick <= BikakuMove.ERUPT_SUNK && tick % 2 == 0) {
            dust(burst, 4, 0.6);
        }
    }

    private static void dust(Vec3 at, int wanted, double spread) {
        int n = RcFx.count(wanted);
        for (int i = 0; i < n; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2.0;
            Vec3 out = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            Vec3 p = at.m_82549_(out.m_82490_(spread * RANDOM.nextDouble()));
            RcFx.at(RcFx.PUFF, false, 22 + RANDOM.nextInt(18), p).velocity(out.m_82490_(0.05 + 0.1 * RANDOM.nextDouble()).m_82520_(0.0, 0.05 + 0.08 * RANDOM.nextDouble(), 0.0))
                .drag(0.88F).colors(DUST, RcFx.clear(DUST_FADE)).size(0.5F, 1.6F).spawn();
        }
    }

    private static void chips(Vec3 at, int wanted, double speed) {
        int n = RcFx.count(wanted);
        for (int i = 0; i < n; i++) {
            Vec3 v = new Vec3(RANDOM.nextDouble() - 0.5, 0.6 + RANDOM.nextDouble() * 0.8, RANDOM.nextDouble() - 0.5).m_82490_(speed * (0.5 + 0.5 * RANDOM.nextDouble()));
            RcFx.at(RcFx.ASH, false, 18 + RANDOM.nextInt(12), at).velocity(v).colors(GRIT, RcFx.clear(GRIT)).size(0.16F, 0.1F).fall(0.05F).spawn();
        }
    }

    /** Gobbets of its flesh and blood thrown up out of the ground. */
    private static void flesh(Vec3 at, int wanted) {
        int n = RcFx.count(wanted);
        for (int i = 0; i < n; i++) {
            Vec3 v = new Vec3(RANDOM.nextDouble() - 0.5, 0.8 + RANDOM.nextDouble() * 0.7, RANDOM.nextDouble() - 0.5).m_82490_(0.3 + 0.25 * RANDOM.nextDouble());
            RcFx.at(RcFx.ASH, false, 22 + RANDOM.nextInt(14), at).velocity(v).colors(RANDOM.nextBoolean() ? FLESH : FLESH_DARK, RcFx.clear(FLESH_DARK))
                .size(0.22F, 0.14F).fall(0.06F).spawn();
        }
    }

    // ---------------------------------------------------------------- 3.6.3: Grand Devouring

    /** 3.6.3: each feeding seen (when it began, how far into it the last tick was). */
    private static final Map<UUID, double[]> WET = new HashMap<>();
    static final int SALIVA = 0xD8D9B9B0;
    static final int SALIVA_FADE = 0x00A07870;

    /**
     * 3.6.3, Grand Devouring's wet (the owner: "Include appropriate wet biological sounds", and the libraries "to smooth out the mod"):
     * drool off its mouth as it hangs open over its victim, a splash as it comes down over it, drips while the jaws close, a spray as they
     * shut and at the gulp; more for a bigger mouth. Where its mouth is comes from the same feeding the worm is drawn by.
     */
    private static void devour(AbstractClientPlayer player, double clock) {
        UUID id = player.m_36316_().getId();
        BikakuClient.View view = BikakuClient.isBikaku(player) ? BikakuClient.get(player) : null;
        double age = view == null ? -1.0 : view.devourAge(clock);
        if (view == null || age < 0.0 || age >= GrandDevour.KILL || view.devourSpot() == null || !RcFx.near(player.m_20182_(), RANGE)) {
            WET.remove(id);
            return;
        }
        double[] seen = WET.computeIfAbsent(id, ignored -> new double[] {view.devourAt(), age});
        if (seen[0] != view.devourAt()) {
            seen[0] = view.devourAt();
            seen[1] = age;
        }
        int last = (int)Math.floor(seen[1]);
        int now = (int)Math.floor(age);
        seen[1] = age;
        if (now <= last) {
            return;
        }
        double yaw = Math.toRadians(player.f_20883_);
        double[] spot = view.devourSpot();
        int count = view.kakuja() ? 1 : view.form().worms();
        GrandDevour.Feeding feeding = GrandDevour.at(clock, 0, count, Math.max(0, view.devourWorm()), rig(player, yaw, new Vec3(spot[0], spot[1],
            spot[2])), view.devourWidth(), view.devourHeight(), age);
        double[][] path = feeding.shape().path();
        Vec3 mouth = world(player, yaw, path[path.length - 1]);
        double hole = GrandDevour.hole(view.devourWidth(), view.devourHeight());
        for (int tick = Math.max(0, last + 1); tick <= now; tick++) {
            if (tick >= GrandDevour.RISE && tick < GrandDevour.CAUGHT && tick % 3 == 0) {
                drool(mouth, hole, 2);
            } else if (tick == GrandDevour.CAUGHT) {
                splash(mouth, hole, 16);
            } else if (tick > GrandDevour.CAUGHT && tick < GrandDevour.SHUT && tick % 6 == 0) {
                drool(mouth, hole * feeding.shape().aperture(), 2);
            } else if (tick == GrandDevour.SHUT) {
                splash(mouth, hole * 0.5, 10);
            } else if (tick == GrandDevour.SHUT + GrandDevour.SWALLOW / 2) {
                splash(mouth, hole * 0.4, 10); // the gulp
            }
        }
    }

    /** Drops of drool falling off the rim of its mouth. */
    private static void drool(Vec3 mouth, double hole, int wanted) {
        int n = RcFx.count(wanted * (float)(0.6 + hole));
        for (int i = 0; i < n; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2.0;
            double r = Math.max(0.15, hole) * (0.6 + 0.35 * RANDOM.nextDouble());
            Vec3 at = mouth.m_82520_(Math.cos(a) * r, -0.05, Math.sin(a) * r);
            RcFx.at(RcFx.MOTE, false, 26 + RANDOM.nextInt(14), at).velocity(new Vec3(0.0, -0.02, 0.0)).colors(SALIVA, RcFx.clear(SALIVA_FADE))
                .size(0.12F, 0.06F).fall(0.04F).spawn();
        }
    }

    /** Spit and blood thrown out round its mouth. */
    private static void splash(Vec3 mouth, double hole, int wanted) {
        int n = RcFx.count(wanted * (float)(0.6 + hole));
        for (int i = 0; i < n; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2.0;
            Vec3 out = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            Vec3 at = mouth.m_82549_(out.m_82490_(Math.max(0.15, hole) * 0.9));
            Vec3 v = out.m_82490_(0.08 + 0.1 * RANDOM.nextDouble()).m_82520_(0.0, 0.06 + 0.08 * RANDOM.nextDouble(), 0.0);
            RcFx.at(RcFx.ASH, false, 20 + RANDOM.nextInt(12), at).velocity(v).colors(RANDOM.nextBoolean() ? FLESH : SALIVA, RcFx.clear(FLESH_DARK))
                .size(0.18F, 0.1F).fall(0.06F).spawn();
        }
    }

    /** {@code rig} (his body's frame) in the world. */
    static Vec3 world(AbstractClientPlayer player, double yaw, double[] rig) {
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        return player.m_20182_().m_82520_(rx * rig[0] + fx * rig[2], rig[1], rz * rig[0] + fz * rig[2]);
    }

    /** A point of the world in his body's frame. */
    static double[] rig(AbstractClientPlayer player, double yaw, Vec3 world) {
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        Vec3 d = world.m_82546_(player.m_20182_());
        return new double[] {d.f_82479_ * rx + d.f_82481_ * rz, d.f_82480_, d.f_82479_ * fx + d.f_82481_ * fz};
    }
}

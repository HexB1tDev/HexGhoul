package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.ayato.UkakuModel;
import com.hex.kagune.control.client.ControlClient;
import com.hex.kagune.control.client.EtoCamera;
import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.client.KingClient;
import com.hex.kagune.control.king.KingFxModel;
import com.hex.kagune.control.king.KingMarkFx;
import com.hex.kagune.control.king.KingMove;
import com.hex.kagune.control.king.WarRules;
import com.hex.kagune.control.network.S2CKingFx;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.0.27: what the warpath's moves do out in the world, on every client ({@code WarPath} on the server sends it; {@code KingFx} hands
 * it here): the spikes breaking the ground (each grown on its own tick, standing, sinking back), the blitz's afterimages and him
 * flashing out of sight and back, a body driven into the ground (the crater, the cracks splitting out across the ground, the dust
 * and the ground shaking for everyone near), and Black Sun's ring of air (smoke and snow thrown out across everything).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class WarPathFx {
    /** Ticks a spike takes to come up, how long it stands (the cone's), how long it takes to sink back. */
    static final double GROW = 1.6;
    /** 1.0.27: a ground spike's thickness for the width the server sends (its hit's). */
    static final double NEEDLE = 0.45;
    static final double SINK = 6.0;
    /** Minecraft's own smoke, for the ring of air. */
    static final ResourceLocation SMOKE = ResourceLocation.m_214293_("minecraft", "textures/particle/generic_7.png");
    /** How far off any of it is seen (blocks). */
    static final double FAR = 96.0;

    /** Who flashes out of sight until when (world ticks). */
    private static final Map<UUID, Double> FLASHING = new HashMap<>();
    /** Each blitzing wearer's last afterimage (world ticks). */
    private static final Map<UUID, Double> GHOSTED = new HashMap<>();
    /** 1.0.35: each wearer's feet last tick and when his sprint last left an afterimage: {x, y, z, when}. */
    private static final Map<UUID, double[]> RAN = new HashMap<>();
    /** 1.0.35: how fast a final kakuja must sprint (blocks a tick) to leave the dash's afterimages, and every how many ticks one. */
    static final double SPRINT_GHOST_SPEED = 0.25;
    static final double SPRINT_GHOST_EVERY = 4.0; // 1.0.36: (was 2: too many, too heavy)

    private WarPathFx() {
    }

    /** How long effect {@code kind} lasts (ticks), or -1 if it is not one of these. */
    public static int lasts(int kind) {
        return switch (kind) {
            case S2CKingFx.SPIKES -> 70;
            case S2CKingFx.BLITZ -> 10;
            case S2CKingFx.CRATER -> 90;
            case S2CKingFx.AIR_RING -> WarRules.RING_TICKS + 40;
            default -> -1;
        };
    }

    /** An effect of {@code owner}'s arrived ({@code now}: world ticks): what it does at once. */
    public static void accept(int kind, UUID owner, int[] ids, float[] data, double now) {
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null) {
            return;
        }
        switch (kind) {
            case S2CKingFx.BLITZ -> {
                if (data.length < 6) {
                    return;
                }
                Vec3 from = new Vec3(data[0], data[1], data[2]);
                Vec3 to = new Vec3(data[3], data[4], data[5]);
                streak(from, to);
                FLASHING.put(owner, now + 3.0);
                AbstractClientPlayer who = player(level, owner);
                if (who != null) {
                    // Left behind along the way: his shape, crimson, every step of it.
                    for (int k = 0; k <= 4; k++) {
                        Vec3 at = from.m_82549_(to.m_82546_(from).m_82490_(k / 4.0)).m_82520_(0.0, -1.0, 0.0);
                        ghost(who, at, now - (4 - k) * 0.4);
                    }
                }
            }
            case S2CKingFx.CRATER -> {
                if (data.length < 6) {
                    return;
                }
                Vec3 at = new Vec3(data[0], data[1], data[2]);
                double radius = data[3];
                double shake = data[4];
                crater(level, at, radius);
                feel(minecraft, at, shake, radius > 3.0 ? 1.0F : 0.7F, radius > 3.0 ? 26.0 : 16.0);
            }
            case S2CKingFx.AIR_RING -> {
                if (data.length < 6) {
                    return;
                }
                Vec3 at = new Vec3(data[0], data[1], data[2]);
                RcFx.at(RcFx.RING, true, 12, at).colors(0xF0FFFFFF, RcFx.clear(0xFFE8ECF2)).size(1.0F, 14.0F).bloom(1.0F).spawn();
                RcFx.at(RcFx.RING, true, 18, at).colors(0xC0FFE0E0, RcFx.clear(0xFFFF3040)).size(0.6F, 24.0F).bloom(1.0F).spawn();
                KingMarks.play(level, KingMarkFx.FLARE, at, Vec3.f_82478_, 3.0F, null);
                // 1.0.33: the air split straight through it along the punch: white streaks out of the back of the blow.
                Vec3[] axes = ringAxes(data);
                int streaks = RcFx.count(26.0F);
                for (int k = 0; k < streaks; k++) {
                    double a = Math.random() * Math.PI * 2.0;
                    double off = 0.4 + Math.random() * 2.2;
                    Vec3 from = at.m_82549_(axes[0].m_82490_(Math.cos(a) * off)).m_82549_(axes[1].m_82490_(Math.sin(a) * off));
                    RcFx.at(RcFx.MOTE, true, 6 + (int)(Math.random() * 6), from).velocity(axes[2].m_82490_(1.4 + Math.random() * 1.6)).drag(0.82F)
                        .colors(0xF0FFFFFF, RcFx.clear(0xFFE8ECF2)).size(0.35F, 0.05F).bloom(1.0F).spawn();
                }
                feel(minecraft, at, data[5], 1.0F, 34.0);
            }
            default -> {
            }
        }
    }

    /** Each tick of an effect, {@code age} ticks into it: the spikes' dust as each breaks the ground, the ring's smoke and snow. */
    public static void tick(int kind, UUID owner, int[] ids, float[] data, double age, double now) {
        switch (kind) {
            case S2CKingFx.SPIKES -> {
                for (int i = 0; i + 7 < data.length; i += 8) {
                    double since = age - data[i + 7];
                    if (since >= 0.0 && since < 1.0) {
                        Vec3 base = new Vec3(data[i], data[i + 1] + 0.1, data[i + 2]);
                        if (!RcFx.near(base, FAR)) {
                            continue;
                        }
                        for (int k = 0; k < RcFx.count(3.0F); k++) {
                            Vec3 v = new Vec3(Math.random() - 0.5, 0.4 + Math.random() * 0.5, Math.random() - 0.5).m_82490_(0.12);
                            RcFx.at(RcFx.PUFF, false, 16 + (int)(Math.random() * 10), base).velocity(v).drag(0.86F).colors(0xB0302A26, RcFx.clear(0xFF4A423A))
                                .size(0.35F, 1.1F).spawn();
                        }
                        for (int k = 0; k < RcFx.count(2.0F); k++) {
                            Vec3 v = new Vec3(Math.random() - 0.5, 0.6 + Math.random() * 0.6, Math.random() - 0.5).m_82490_(0.2);
                            RcFx.at(RcFx.ASH, false, 18, base).velocity(v).colors(0xFF1A1412, RcFx.clear(0xFF1A1412)).size(0.12F, 0.08F).fall(0.03F).spawn();
                        }
                        if (Math.random() < 0.5) {
                            RcFx.at(RcFx.EMBER, true, 12, base.m_82520_(0.0, 0.3, 0.0)).velocity(new Vec3(0.0, 0.05, 0.0)).colors(RcFx.RED, RcFx.clear(RcFx.CRIMSON))
                                .size(0.16F, 0.04F).spawn();
                        }
                    }
                }
            }
            case S2CKingFx.AIR_RING -> {
                if (data.length < 6 || age > data[4] + 2.0) {
                    return;
                }
                Vec3 at = new Vec3(data[0], data[1], data[2]);
                Vec3[] axes = ringAxes(data);
                double floor = ringFloor(data);
                double radius = data[3] * ease(Math.min(1.0, age / data[4]));
                double fade = 1.0 - Math.min(1.0, age / (data[4] + 2.0));
                int smoke = RcFx.count((float)(18.0 + radius * 1.4));
                for (int k = 0; k < smoke; k++) {
                    double a = Math.random() * Math.PI * 2.0;
                    Vec3 out = axes[0].m_82490_(Math.cos(a)).m_82549_(axes[1].m_82490_(Math.sin(a)));
                    Vec3 p = at.m_82549_(out.m_82490_(radius)).m_82549_(axes[2].m_82490_((Math.random() - 0.5) * (0.8 + radius * 0.04)));
                    if (p.f_82480_ < floor || !RcFx.near(p, FAR)) {
                        continue;
                    }
                    RcFx.at(SMOKE, false, 26 + (int)(Math.random() * 16), p).velocity(out.m_82490_(0.22 + 0.2 * Math.random()).m_82549_(axes[2].m_82490_(0.03)))
                        .drag(0.9F).colors((int)(0xD0 * fade) << 24 | 0xEDEFF2, RcFx.clear(0xFFC8CCD2)).size((float)(1.0 + radius * 0.02), (float)(2.6 + radius * 0.05))
                        .spawn();
                }
                int snow = RcFx.count((float)(10.0 + radius * 0.6));
                for (int k = 0; k < snow; k++) {
                    double a = Math.random() * Math.PI * 2.0;
                    Vec3 out = axes[0].m_82490_(Math.cos(a)).m_82549_(axes[1].m_82490_(Math.sin(a)));
                    Vec3 p = at.m_82549_(out.m_82490_(radius * (0.85 + 0.2 * Math.random()))).m_82549_(axes[2].m_82490_((Math.random() - 0.3) * 1.5));
                    if (p.f_82480_ < floor || !RcFx.near(p, FAR)) {
                        continue;
                    }
                    RcFx.at(RcFx.MOTE, false, 40 + (int)(Math.random() * 30), p).velocity(out.m_82490_(0.12).m_82520_(0.0, -0.01, 0.0)).drag(0.95F)
                        .colors(0xFFFFFFFF, 0x00F4F8FF).size(0.09F, 0.06F).fall(0.004F).wander(0.03F).spawn();
                }
            }
            default -> {
            }
        }
    }

    /** Its shapes this frame, {@code age} ticks in. */
    public static void draw(UkakuModel.Sink sink, int kind, int[] ids, float[] data, double age, double now) {
        switch (kind) {
            case S2CKingFx.SPIKES -> {
                for (int i = 0; i + 7 < data.length; i += 8) {
                    double since = age - data[i + 7];
                    if (since <= 0.0) {
                        continue;
                    }
                    double out = Math.min(1.0, since / GROW) * Math.max(0.0, Math.min(1.0, (GROW + WarRules.CONE_STAND + SINK - since) / SINK));
                    if (out <= 0.01) {
                        continue;
                    }
                    int seed = i / 8 * 7 + 3 + (int)(data[i] * 13.0F) * 31 + (int)(data[i + 2] * 7.0F);
                    double[] base = {data[i], data[i + 1], data[i + 2]};
                    double[] lean = {data[i + 3], 0.0, data[i + 4]};
                    KingFxModel.needle(sink, base, lean, data[i + 5], data[i + 6] * NEEDLE, out, seed);
                }
            }
            case S2CKingFx.CRATER -> {
                if (data.length < 6) {
                    return;
                }
                double[] at = {data[0], data[1], data[2]};
                double radius = data[3];
                double grow = Math.min(1.0, age / 5.0);
                double fade = Math.max(0.0, Math.min(1.0, (lasts(kind) - age) / 20.0));
                cracks(sink, at, radius * (1.2 + 1.1 * grow), radius, fade, (long)(data[0] * 31.0 + data[2] * 17.0));
                if (age < 10.0) {
                    KingFxModel.crescent(sink, new double[] {at[0], at[1] - 0.3, at[2]}, radius * 2.4 * ease(age / 10.0), 0.8, 1.0 - age / 10.0, now);
                }
            }
            case S2CKingFx.AIR_RING -> {
                if (data.length < 6 || age > data[4] + 6.0) {
                    return;
                }
                double[] at = {data[0], data[1], data[2]};
                double r = data[3] * ease(Math.min(1.0, age / data[4]));
                double fade = 1.0 - Math.min(1.0, age / (data[4] + 6.0));
                Vec3[] axes = ringAxes(data);
                KingFxModel.airRing(sink, at, r, 0.6 + 0.04 * r, fade, xyz(axes[0]), xyz(axes[1]), xyz(axes[2]), ringFloor(data));
            }
            default -> {
            }
        }
    }

    /**
     * The cracks a body driven into the ground splits out across it: jagged, branching, dark, a red glow down their middle, out to
     * {@code reach} from the crater's rim ({@code radius}) round {@code at}.
     */
    static void cracks(UkakuModel.Sink sink, double[] at, double reach, double radius, double fade, long seed) {
        if (fade <= 0.01) {
            return;
        }
        int dark = (int)Math.round(235 * fade) << 24 | 0x0C0A0B;
        int glow = 0xFF000000 | (int)Math.round(200 * fade) << 16 | (int)Math.round(24 * fade) << 8 | (int)Math.round(30 * fade);
        int cracks = 11;
        for (int c = 0; c < cracks; c++) {
            double a = (c + 0.6 * unit(seed, c)) / cracks * Math.PI * 2.0;
            double length = reach * (0.6 + 0.4 * unit(seed, c + 50));
            int steps = 7;
            double[] was = {at[0] + Math.cos(a) * radius * 0.5, at[1] + 0.06, at[2] + Math.sin(a) * radius * 0.5};
            double heading = a;
            for (int s = 1; s <= steps; s++) {
                heading += (unit(seed, c * 31 + s) - 0.5) * 0.7;
                double r = radius * 0.5 + (length - radius * 0.5) * s / steps;
                double[] now = {at[0] + Math.cos(heading) * r, at[1] + 0.06, at[2] + Math.sin(heading) * r};
                double width = 0.22 * (1.0 - 0.75 * s / (double)steps) + 0.04;
                strip(sink, was, now, width, dark, false);
                strip(sink, was, now, width * 0.35, glow, true);
                if (s == 3 && unit(seed, c + 90) < 0.6) {
                    // A branch off it.
                    double b = heading + (unit(seed, c + 70) < 0.5 ? -0.9 : 0.9);
                    double[] end = {now[0] + Math.cos(b) * length * 0.3, now[1], now[2] + Math.sin(b) * length * 0.3};
                    strip(sink, now, end, width * 0.6, dark, false);
                    strip(sink, now, end, width * 0.2, glow, true);
                }
                was = now;
            }
        }
    }

    /** A flat strip from {@code a} to {@code b} on the ground, {@code width} wide; glowing (additive) or dark. */
    static void strip(UkakuModel.Sink sink, double[] a, double[] b, double width, int argb, boolean glow) {
        double dx = b[0] - a[0];
        double dz = b[2] - a[2];
        double l = Math.hypot(dx, dz);
        if (l < 1.0E-4) {
            return;
        }
        double sx = -dz / l * width * 0.5;
        double sz = dx / l * width * 0.5;
        double lift = glow ? 0.012 : 0.0;
        double[] n = glow ? new double[] {0.0, 1.0, 0.0} : new double[] {0.0, 0.9995, 0.0316};
        sink.quad(new double[] {a[0] - sx, a[1] + lift, a[2] - sz}, new double[] {a[0] + sx, a[1] + lift, a[2] + sz}, new double[] {b[0] + sx, b[1] + lift, b[2] + sz},
            new double[] {b[0] - sx, b[1] + lift, b[2] - sz}, n, argb, argb, argb, argb);
    }

    /** Black Sun's ring of air, seen as it goes: a pale band {@code width} wide at {@code r} round {@code at}, both faces. */
    /**
     * 1.0.33: the ring's plane, {across, up, the way it faces}: Black Sun's stands up across the sky square to the punch ({@code data}
     * 6 and 7, its way x, z); without one, lying flat.
     */
    static Vec3[] ringAxes(float[] data) {
        if (data.length >= 9) {
            double x = data[6];
            double z = data[7];
            double l = Math.sqrt(x * x + z * z);
            if (l > 1.0E-4) {
                x /= l;
                z /= l;
                return new Vec3[] {new Vec3(-z, 0.0, x), new Vec3(0.0, 1.0, 0.0), new Vec3(x, 0.0, z)};
            }
        }
        return new Vec3[] {new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 1.0, 0.0)};
    }

    private static double[] xyz(Vec3 v) {
        return new double[] {v.f_82479_, v.f_82480_, v.f_82481_};
    }

    /** 1.0.33: the ground under the ring ({@code data} 8), none of it drawn below. */
    static double ringFloor(float[] data) {
        return data.length >= 9 ? data[8] + 0.2 : Double.NEGATIVE_INFINITY;
    }

    /** A body driven into the ground: dust thrown up and out all round, chips of the ground flung, black smoke and red light. */
    static void crater(ClientLevel level, Vec3 at, double radius) {
        if (!RcFx.near(at, FAR)) {
            return;
        }
        KingMarks.strike(level, KingMarkFx.QUAKE, at, new Vec3(0.0, 1.0, 0.0), (float)Math.min(3.0, radius * 0.7), null);
        KingMarks.play(level, KingMarkFx.SOOT, at.m_82520_(0.0, 0.4, 0.0), new Vec3(0.0, 1.0, 0.0), (float)Math.min(3.0, radius * 0.6), null);
        KingMarks.play(level, KingMarkFx.FLARE, at.m_82520_(0.0, 0.5, 0.0), Vec3.f_82478_, (float)Math.min(2.5, radius * 0.5), null);
        int dust = RcFx.count((float)(28.0 + radius * 8.0));
        for (int i = 0; i < dust; i++) {
            double a = Math.random() * Math.PI * 2.0;
            Vec3 out = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            Vec3 p = at.m_82549_(out.m_82490_(radius * 0.4 * Math.random())).m_82520_(0.0, 0.2, 0.0);
            RcFx.at(RcFx.PUFF, false, 30 + (int)(Math.random() * 25), p).velocity(out.m_82490_(0.2 + 0.35 * Math.random()).m_82520_(0.0, 0.08 + 0.12 * Math.random(), 0.0))
                .drag(0.88F).colors(0xC0574E44, RcFx.clear(0xFF6E655A)).size(0.8F, 2.6F).spawn();
        }
        int chips = RcFx.count((float)(16.0 + radius * 4.0));
        for (int i = 0; i < chips; i++) {
            Vec3 v = new Vec3(Math.random() - 0.5, 0.7 + Math.random() * 0.8, Math.random() - 0.5).m_82490_(0.35 + 0.25 * Math.random());
            RcFx.at(RcFx.ASH, false, 24 + (int)(Math.random() * 12), at.m_82520_(0.0, 0.3, 0.0)).velocity(v).colors(0xFF2A2420, RcFx.clear(0xFF2A2420))
                .size(0.2F, 0.14F).fall(0.05F).spawn();
        }
        RcFx.at(RcFx.RING, true, 10, at.m_82520_(0.0, 0.2, 0.0)).colors(0xC0FF4A3A, RcFx.clear(RcFx.CRIMSON)).size(0.5F, (float)(radius * 2.2)).bloom(1.0F).spawn();
    }

    /** The ground shaking for whoever is near {@code at} (within {@code reach} blocks): a kick, and a rumble a while after. */
    static void feel(Minecraft minecraft, Vec3 at, double reach, float strength, double ticks) {
        LocalPlayer self = minecraft.f_91074_;
        if (self == null) {
            return;
        }
        double away = self.m_20182_().m_82554_(at);
        if (away > reach) {
            return;
        }
        float k = (float)(strength * (1.0 - away / reach));
        double clock = EtoClient.clock(0.0F);
        EtoCamera.kick(Math.max(0.2F, k), clock);
        EtoCamera.rumble(k, ticks, clock);
    }

    /** The blitz's dash or blink: a black-crimson streak along it. */
    static void streak(Vec3 from, Vec3 to) {
        streak(from, to, true, 28.0F);
    }

    /** 1.0.35: as above, {@code amount} of it, War's black-crimson or ({@code war} false) Peace's white and pale blue. */
    static void streak(Vec3 from, Vec3 to, boolean war, float amount) {
        if (!RcFx.near(from, FAR)) {
            return;
        }
        Vec3 d = to.m_82546_(from);
        if (d.m_82556_() < 1.0E-6) {
            return;
        }
        if (!war) {
            int n = RcFx.count(amount);
            for (int i = 0; i < n; i++) {
                Vec3 at = from.m_82549_(d.m_82490_(Math.random())).m_82520_((Math.random() - 0.5) * 0.5, (Math.random() - 0.5) * 1.4, (Math.random() - 0.5) * 0.5);
                boolean glint = i % 3 == 0;
                RcFx.at(glint ? RcFx.SPARK : RcFx.STREAK, true, 8 + (int)(Math.random() * 6), at).velocity(d.m_82541_().m_82490_(0.08))
                    .colors(glint ? 0xFFFFFFFF : 0xB0DCE8FF, glint ? 0x00C8DCFF : 0x00DCE8FF).size(glint ? 0.12F : 0.5F, 0.05F).drag(0.8F).spawn();
            }
            return;
        }
        int n = RcFx.count(amount);
        for (int i = 0; i < n; i++) {
            Vec3 at = from.m_82549_(d.m_82490_(Math.random())).m_82520_((Math.random() - 0.5) * 0.5, (Math.random() - 0.5) * 1.4, (Math.random() - 0.5) * 0.5);
            boolean red = i % 3 == 0;
            RcFx.at(red ? RcFx.EMBER : RcFx.STREAK, red, 8 + (int)(Math.random() * 6), at).velocity(d.m_82541_().m_82490_(0.08))
                .colors(red ? RcFx.RED : 0xE0100C0E, red ? RcFx.clear(RcFx.CRIMSON) : 0x00100C0E).size(red ? 0.16F : 0.55F, 0.05F).drag(0.8F).spawn();
        }
    }

    /** One of {@code who}'s afterimages at {@code feet}, in the pose his body is in now. */
    static void ghost(AbstractClientPlayer who, Vec3 feet, double time) {
        ghost(who, feet, time, true);
    }

    /** 1.0.35: as above, War's crimson or ({@code war} false) Peace's ice. */
    static void ghost(AbstractClientPlayer who, Vec3 feet, double time, boolean war) {
        com.hex.kagune.control.client.animation.RigPose pose = com.hex.kagune.control.client.animation.KaguneBodyAnimation.body(who, ControlClient.worldTime(0.0F));
        if (pose == null) {
            pose = new com.hex.kagune.control.client.animation.RigPose();
        }
        com.hex.kagune.control.client.render.KingAfterimages.add(who, pose, feet, who.f_20883_, time, war);
    }

    /** 1.0.36: the sprint's: only ever his body's shape (light), never the whole form. */
    static void sprintGhost(AbstractClientPlayer who, Vec3 feet, double time, boolean war) {
        com.hex.kagune.control.client.animation.RigPose pose = com.hex.kagune.control.client.animation.KaguneBodyAnimation.body(who, ControlClient.worldTime(0.0F));
        if (pose == null) {
            pose = new com.hex.kagune.control.client.animation.RigPose();
        }
        com.hex.kagune.control.client.render.KingAfterimages.add(who, pose, feet, who.f_20883_, time, war, false);
    }

    /**
     * 1.0.35: a final kakuja sprinting leaves the dash's own afterimages (the blitz's, the same call): his shape left where he was every
     * other tick, and a thin streak of the dash's along the way (War's black and crimson, Peace's white).
     */
    private static void sprint(AbstractClientPlayer player, KingClient.View view, double now) {
        UUID id = player.m_20148_();
        double[] was = RAN.get(id);
        Vec3 at = player.m_20182_();
        double last = was == null ? Double.NEGATIVE_INFINITY : was[3];
        if (was != null && view.on() && !view.preview() && player.m_20142_() && !player.m_20159_()) {
            double speed = Math.hypot(at.f_82479_ - was[0], at.f_82481_ - was[2]);
            if (speed >= SPRINT_GHOST_SPEED && speed < 8.0) {
                com.hex.kagune.control.client.render.KingAfterimages.blur(player, at.f_82479_ - was[0], at.f_82481_ - was[2], now);
            }
            if (speed >= SPRINT_GHOST_SPEED && speed < 8.0 && (now - last >= SPRINT_GHOST_EVERY || now < last)) {
                boolean war = view.form() == com.hex.kagune.control.king.KingForm.WAR;
                Vec3 from = new Vec3(was[0], was[1], was[2]);
                sprintGhost(player, from, now, war);
                streak(from.m_82520_(0.0, 1.0, 0.0), at.m_82520_(0.0, 1.0, 0.0), war, 4.0F);
                last = now;
            }
        }
        RAN.put(id, new double[] {at.f_82479_, at.f_82480_, at.f_82481_, last});
    }

    /** Through the blitz and Black Sun's rush, every wearer near leaves afterimages, flashing out of sight and back. */
    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null) {
            FLASHING.clear();
            GHOSTED.clear();
            RAN.clear();
            return;
        }
        double now = ControlClient.worldTime(0.0F);
        for (AbstractClientPlayer player : level.m_6907_()) {
            KingClient.View view = KingClient.get(player);
            if (view == null) {
                continue;
            }
            KingMove move = view.moveAt(now);
            double age = now - view.moveAt();
            sprint(player, view, now); // 1.0.35
            if (move == KingMove.SUNDER && age >= WarRules.SUNDER_BESIDE && age < WarRules.SUNDER_PUNCH) {
                charge(player, (age - WarRules.SUNDER_BESIDE) / (WarRules.SUNDER_PUNCH - WarRules.SUNDER_BESIDE), age);
            }
            boolean rushing = move == KingMove.BLITZ && age < WarRules.BLITZ_WIND || move == KingMove.SUNDER && age < WarRules.SUNDER_BESIDE;
            if (!rushing) {
                continue;
            }
            FLASHING.put(player.m_20148_(), Math.max(FLASHING.getOrDefault(player.m_20148_(), 0.0), now + 1.0));
            Double last = GHOSTED.get(player.m_20148_());
            if (last == null || now - last >= 2.0 || now < last) {
                GHOSTED.put(player.m_20148_(), now);
                boolean own = player == minecraft.f_91074_ && minecraft.f_91066_.m_92176_().m_90612_();
                if (!own) {
                    ghost(player, player.m_20182_(), now);
                }
            }
        }
    }

    /**
     * 1.0.33: Black Sun drawn back ({@code k} 0..1 of the draw): red sparks and black smoke pulled in out of the air round him into the
     * cocked fist, more and faster the longer it is held, and near the end a ring of red closing in on it, again and again.
     */
    static void charge(AbstractClientPlayer who, double k, double age) {
        double yaw = Math.toRadians(who.f_20883_);
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3 fist = who.m_20182_().m_82520_(0.0, 1.25, 0.0).m_82549_(right.m_82490_(0.45)).m_82549_(ahead.m_82490_(-0.55));
        if (!RcFx.near(fist, FAR)) {
            return;
        }
        int sparks = RcFx.count((float)(2.0 + 12.0 * k * k));
        for (int i = 0; i < sparks; i++) {
            Vec3 dir = new Vec3(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).m_82541_();
            double far = 1.6 + 2.6 * Math.random();
            boolean smoke = i % 3 == 2;
            RcFx.at(smoke ? SMOKE : RcFx.EMBER, !smoke, smoke ? 10 : 7, fist.m_82549_(dir.m_82490_(far))).velocity(dir.m_82490_(-far * (0.14 + 0.06 * k)))
                .colors(smoke ? 0xB0100A0C : RcFx.RED, smoke ? 0x00100A0C : RcFx.clear(RcFx.CRIMSON)).size(smoke ? 0.5F : 0.14F, smoke ? 0.1F : 0.04F)
                .drag(0.96F).spawn();
        }
        if (k > 0.55 && ((long)Math.floor(age) % 5L) == 0L) {
            RcFx.at(RcFx.RING, true, 6, fist).colors(0xD0FF2A36, RcFx.clear(RcFx.CRIMSON)).size((float)(1.2 + 1.6 * k), 0.15F).bloom(1.0F).spawn();
        }
    }

    /** Flashing out of sight and back: on every other frame or so, not drawn. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void flash(RenderPlayerEvent.Pre event) {
        Double until = FLASHING.get(event.getEntity().m_20148_());
        if (until == null) {
            return;
        }
        double t = ControlClient.worldTime(event.getPartialTick());
        if (t > until) {
            FLASHING.remove(event.getEntity().m_20148_());
            return;
        }
        if (((long)Math.floor(t * 3.0) & 1L) == 0L) {
            event.setCanceled(true);
        }
    }

    static AbstractClientPlayer player(ClientLevel level, UUID id) {
        for (AbstractClientPlayer p : level.m_6907_()) {
            if (p.m_20148_().equals(id)) {
                return p;
            }
        }
        return null;
    }

    static double ease(double x) {
        double k = 1.0 - Math.max(0.0, Math.min(1.0, x));
        return 1.0 - k * k * k;
    }

    static double unit(long seed, int k) {
        long h = seed * 0x9E3779B97F4A7C15L + k * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }
}

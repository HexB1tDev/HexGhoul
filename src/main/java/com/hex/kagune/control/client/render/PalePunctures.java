package com.hex.kagune.control.client.render;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 6.2.0: the hole a Watch This beak leaves through a mob's chest (a player's is carved into their body as a stab wound instead):
 * a dark wound where it went in and a torn one where it came out of the back, each a ragged disc on the body's surface, carried
 * round with the body as it turns, for {@link #LIFE} ticks (or until it dies).
 */
public final class PalePunctures {
    /** How long (ticks) a hole stays, how wide it is (blocks), the most kept at once. */
    static final double LIFE = 600.0;
    static final double RADIUS = 0.12;
    static final int MOST = 64;
    static final int RIM = 0xFF6E1A14;
    static final int DEEP = 0xFF1E0705;
    static final int SIDES = 9;

    /** One hole: where its middle is and the way the beak went (in the body's own frame: x its right, y up from its feet, z ahead), how deep the body is there, and when. */
    record Hole(double x, double y, double z, double dx, double dy, double dz, double half, double at) {
    }

    private static final Map<Integer, Hole> HOLES = new HashMap<>();

    private PalePunctures() {
    }

    /** The beak went through {@code body} at {@code chest} (the world), along {@code through}, at {@code time} (PaleClient's clock). */
    public static void add(Entity body, Vec3 chest, Vec3 through, double time) {
        if (!(body instanceof LivingEntity living)) {
            return;
        }
        if (HOLES.size() >= MOST) {
            HOLES.clear();
        }
        double[] p = local(living, chest.f_82479_ - living.m_20185_(), chest.f_82480_ - living.m_20186_(), chest.f_82481_ - living.m_20189_());
        double[] d = local(living, through.f_82479_, through.f_82480_, through.f_82481_);
        double l = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
        if (l < 1.0E-6) {
            return;
        }
        HOLES.put(living.m_19879_(), new Hole(p[0], p[1], p[2], d[0] / l, d[1] / l, d[2] / l, living.m_20205_() * 0.5, time));
    }

    /** A world offset in {@code body}'s own frame (its body's facing now). */
    static double[] local(LivingEntity body, double x, double y, double z) {
        double yaw = Math.toRadians(body.f_20883_);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        return new double[] {-fz * x + fx * z, y, fx * x + fz * z};
    }

    /** Each hole as quads in the world ({@code partial} of the way through the tick), into {@code sink} (corners, normal, colours). */
    static void draw(ClientLevel level, float partial, double time, Quads sink) {
        for (Iterator<Map.Entry<Integer, Hole>> it = HOLES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Hole> entry = it.next();
            Hole hole = entry.getValue();
            Entity entity = level.m_6815_(entry.getKey());
            if (!(entity instanceof LivingEntity body) || !body.m_6084_() || time - hole.at() > LIFE || time < hole.at() - 40.0) {
                it.remove();
                continue;
            }
            double feetX = body.f_19790_ + (body.m_20185_() - body.f_19790_) * partial;
            double feetY = body.f_19791_ + (body.m_20186_() - body.f_19791_) * partial;
            double feetZ = body.f_19792_ + (body.m_20189_() - body.f_19792_) * partial;
            float bodyYaw = body.f_20884_ + wrap(body.f_20883_ - body.f_20884_) * partial;
            double yaw = Math.toRadians(bodyYaw);
            double fx = -Math.sin(yaw);
            double fz = Math.cos(yaw);
            double[] centre = {feetX - fz * hole.x() + fx * hole.z(), feetY + hole.y(), feetZ + fx * hole.x() + fz * hole.z()};
            double[] way = {-fz * hole.dx() + fx * hole.dz(), hole.dy(), fx * hole.dx() + fz * hole.dz()};
            // Where it went in (facing back along the beak) and where it came out (facing on along it, torn wider).
            disc(sink, centre, way, -(hole.half() + 0.015), RADIUS, 0.0);
            disc(sink, centre, way, hole.half() + 0.015, RADIUS * 1.3, 1.0);
        }
    }

    /** A ragged disc on the body's surface {@code off} blocks along {@code way} from {@code centre}, facing out. */
    static void disc(Quads sink, double[] centre, double[] way, double off, double radius, double torn) {
        double[] n = off < 0.0 ? new double[] {-way[0], -way[1], -way[2]} : way;
        double[] mid = {centre[0] + way[0] * off, centre[1] + way[1] * off, centre[2] + way[2] * off};
        double[] a = Math.abs(n[1]) > 0.9 ? new double[] {1.0, 0.0, 0.0} : new double[] {0.0, 1.0, 0.0};
        double[] u = normalize(cross(n, a));
        double[] v = cross(n, u);
        double[][] rim = new double[SIDES][];
        for (int k = 0; k < SIDES; k++) {
            double angle = k * Math.PI * 2.0 / SIDES;
            double r = radius * (1.0 + (0.18 + 0.25 * torn) * Math.sin(angle * 3.0 + 1.1) + 0.12 * Math.sin(angle * 5.0 + 0.3));
            rim[k] = new double[] {mid[0] + (u[0] * Math.cos(angle) + v[0] * Math.sin(angle)) * r, mid[1] + (u[1] * Math.cos(angle) + v[1] * Math.sin(angle)) * r,
                mid[2] + (u[2] * Math.cos(angle) + v[2] * Math.sin(angle)) * r};
        }
        for (int k = 0; k < SIDES; k++) {
            double[] p0 = rim[k];
            double[] p1 = rim[(k + 1) % SIDES];
            sink.quad(mid, p0, p1, mid, n, DEEP, RIM, RIM, DEEP);
        }
    }

    /** Quads out, in the world. */
    interface Quads {
        void quad(double[] a, double[] b, double[] c, double[] d, double[] n, int ca, int cb, int cc, int cd);
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-9 ? new double[] {1.0, 0.0, 0.0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }

    private static float wrap(float degrees) {
        return degrees - 360.0F * (float)Math.floor((degrees + 180.0F) / 360.0F);
    }
}

package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.ukaku.UkakuModel;
import com.hex.kagune.control.client.KingClient;
import com.hex.kagune.control.king.KingMove;
import com.hex.kagune.control.king.PeaceFxModel;
import com.hex.kagune.control.king.PeaceMotion;
import com.hex.kagune.control.king.PeaceRules;
import com.hex.kagune.control.network.S2CKingFx;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.0.34: what Peace's reworked six look like on every client, from what the server says ({@link S2CKingFx}'s PEACE_* kinds):
 * <ul>
 *   <li>its blades driven into bodies and left there, torn out or drawn out again (the Waltz, the Horizon), the lance gathering
 *       before him and driven out, through a body, carrying it, pinned, called back (Piercing Salvation), the blades sunk in the ground
 *       (Rebirth Instinct): all of it as the blades' own aims ({@link #aims}, what {@link PeaceMotion.State} takes), so they and their
 *       arms are drawn going there;</li>
 *   <li>what grows out of his back besides: Aegis of :Re's three shield tendrils, swinging to meet what comes, reaching for a ledge
 *       out of lava, down under a fall; White Wing Ascent's two tendrils down to the ground holding him up;</li>
 *   <li>the red flesh torn up out of the ground by the Ascent's landing, lashing at bodies, sinking back.</li>
 * </ul>
 * Shapes are {@link PeaceFxModel}'s.
 */
public final class PeaceFx {
    /** How far off any of it is drawn (blocks). */
    static final double FAR = 96.0;

    /** A blade in a body: whose body, since when (world ticks), how (S2CKingFx PEACE_IMPALE's), torn out / let go when (NaN: not yet). */
    static final class Impale {
        final int target;
        final double at;
        final int mode;
        double tearAt = Double.NaN;
        double releaseAt = Double.NaN;
        /**
         * 1.2.8: his right and ahead, and the way it went in, as they were when it struck (world): a blade in a body stays where it went
         * in, never turned round it as he looks or walks about after.
         */
        double[] right;
        double[] ahead;
        double[] inFrom;
        /** 1.2.8: where it was last in the body (world): held there if the body goes (dies, is gone) before the move lets go. */
        double[] lastAt;

        Impale(int target, double at, int mode) {
            this.target = target;
            this.at = at;
            this.mode = mode;
        }

        double reach() {
            return this.mode == 0 ? PeaceRules.WALTZ_REACH : this.mode == 2 ? 2.0 : PeaceRules.HORIZON_REACH;
        }
    }

    /** The lance: gathering since, driven out when, from where, which way, how long, how fast; through whom; pinned, called back when. */
    static final class Lance {
        final double chargeAt;
        double firedAt = Double.NaN;
        double[] from;
        double[] dir;
        double length = PeaceRules.LANCE_SHORT;
        double speed = PeaceRules.PIERCE_SPEED;
        int target;
        double pinnedAt = Double.NaN;
        double backAt = Double.NaN;
        double[] backFrom;

        Lance(double at) {
            this.chargeAt = at;
        }
    }

    /** One of the Aegis's shields at work: since when, where to, doing what. */
    record Shield(double at, double[] to, int action) {
    }

    static final class Aegis {
        final double at;
        double offAt = Double.NaN;
        final Shield[] shields = new Shield[PeaceRules.SHIELDS];

        Aegis(double at) {
            this.at = at;
        }
    }

    /** The Ascent's two tendrils: out since, their feet, what they are doing and since when. */
    static final class Lift {
        final double at;
        final double[] left;
        final double[] right;
        int phase;
        double phaseAt;

        Lift(double at, double[] left, double[] right) {
            this.at = at;
            this.left = left;
            this.right = right;
            this.phaseAt = at;
        }
    }

    /** The flesh torn up: from its seed, when, where, how far, how many; each tendril's foot (the server's ground); its lashes. */
    static final class Erupt {
        final long seed;
        final double at;
        final double[] centre;
        final double[][] layout;
        double[][] bases;
        final Map<Integer, double[]> lashes = new HashMap<>();
        boolean[] seen;

        Erupt(long seed, double at, double[] centre, double radius, int count, float[] data) {
            this.seed = seed;
            this.at = at;
            this.centre = centre;
            this.layout = PeaceFxModel.eruption(seed, count, radius);
            this.seen = new boolean[count];
            this.bases = new double[count][];
            for (int i = 0; i < count && 5 + i < data.length; i++) {
                if (!Float.isNaN(data[5 + i])) {
                    this.bases[i] = new double[] {centre[0] + this.layout[i][0], data[5 + i], centre[2] + this.layout[i][1]};
                }
            }
        }
    }

    /** One of Rebirth Instinct's creatures as the server says: born when, for how long; its blades out, in again, its last swing (which
     *  arm), melting since (world ticks; NaN: not). */
    static final class Thrall {
        double bornAt;
        int life;
        double bladesAt = Double.NaN;
        double bladesOffAt = Double.NaN;
        double swingAt = Double.NaN;
        int side = 1;
        double meltAt = Double.NaN;
        boolean burst;

        Thrall(double bornAt, int life) {
            this.bornAt = bornAt;
            this.life = life;
        }
    }

    private static final Map<UUID, Impale[]> IMPALES = new HashMap<>();
    private static final Map<Integer, Thrall> THRALLS = new HashMap<>();
    /** 1.0.38: how far off a flesh creature is drawn (blocks). */
    static final double THRALL_FAR = 64.0;
    private static final Map<UUID, Lance> LANCES = new HashMap<>();
    private static final Map<UUID, Aegis> AEGIS = new HashMap<>();
    private static final Map<UUID, Lift> LIFTS = new HashMap<>();
    private static final Map<Long, Erupt> ERUPTS = new HashMap<>();

    private PeaceFx() {
    }

    /** How long effect {@code kind} is kept in the list of those showing (theirs is kept here instead), or -1 if it is not one of these. */
    public static int lasts(int kind) {
        return kind >= S2CKingFx.PEACE_IMPALE && kind <= S2CKingFx.PEACE_THRALL ? 1 : -1;
    }

    /** An effect arrived ({@code now}: world ticks). */
    public static void accept(int kind, UUID owner, int[] ids, float[] data, double now) {
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null) {
            return;
        }
        switch (kind) {
            case S2CKingFx.PEACE_IMPALE -> {
                if (ids.length < 3 || ids[0] < 0 || ids[0] >= PeaceMotion.BLADES) {
                    return;
                }
                IMPALES.computeIfAbsent(owner, id -> new Impale[PeaceMotion.BLADES])[ids[0]] = new Impale(ids[1], now, ids[2]);
            }
            case S2CKingFx.PEACE_TEAR -> {
                Impale[] all = IMPALES.get(owner);
                if (all == null || ids.length < 2) {
                    return;
                }
                for (Impale impale : all) {
                    if (impale == null || impale.target != ids[0] || !Double.isNaN(impale.releaseAt)) {
                        continue;
                    }
                    if (ids[1] == 0) {
                        impale.tearAt = now;
                        impale.releaseAt = now + 4.0;
                    } else {
                        impale.releaseAt = now;
                    }
                }
                Entity body = level.m_6815_(ids[0]);
                if (body != null && ids[1] == 0) {
                    tornOut(level, body);
                }
            }
            case S2CKingFx.PEACE_AEGIS -> {
                if (ids.length > 0 && ids[0] == 1) {
                    AEGIS.put(owner, new Aegis(now));
                } else {
                    Aegis aegis = AEGIS.get(owner);
                    if (aegis != null) {
                        aegis.offAt = now;
                    }
                }
            }
            case S2CKingFx.PEACE_SHIELD -> {
                Aegis aegis = AEGIS.get(owner);
                if (aegis == null || ids.length < 2 || data.length < 3 || ids[0] < 0 || ids[0] >= PeaceRules.SHIELDS) {
                    return;
                }
                aegis.shields[ids[0]] = new Shield(now, new double[] {data[0], data[1], data[2]}, ids[1]);
                if (ids[1] == PeaceRules.SHIELD_BLOCK) {
                    Vec3 at = new Vec3(data[0], data[1], data[2]);
                    for (int k = 0; k < RcFx.count(6.0F); k++) {
                        Vec3 v = new Vec3(Math.random() - 0.5, Math.random() - 0.3, Math.random() - 0.5).m_82490_(0.18);
                        RcFx.at(RcFx.SPARK, true, 8, at).velocity(v).colors(0xFFFFFFFF, 0x00C8DCFF).size(0.12F, 0.03F).drag(0.86F).spawn();
                    }
                }
            }
            case S2CKingFx.PEACE_LIFT -> {
                if (ids.length < 1) {
                    return;
                }
                if (ids[0] == 0 && data.length >= 6) {
                    LIFTS.put(owner, new Lift(now, new double[] {data[0], data[1], data[2]}, new double[] {data[3], data[4], data[5]}));
                    for (int side = 0; side < 2; side++) {
                        Vec3 foot = new Vec3(data[side * 3], data[side * 3 + 1], data[side * 3 + 2]);
                        KingMarks.play(level, com.hex.kagune.control.king.KingMarkFx.QUAKE, foot, Vec3.f_82478_, 0.6F, null);
                    }
                } else {
                    Lift lift = LIFTS.get(owner);
                    if (lift != null) {
                        lift.phase = ids[0];
                        lift.phaseAt = now;
                    }
                }
            }
            case S2CKingFx.PEACE_ERUPT -> {
                if (ids.length < 1 || data.length < 5) {
                    return;
                }
                ERUPTS.put((long)ids[0], new Erupt(ids[0], now, new double[] {data[0], data[1], data[2]}, data[3], (int)data[4], data));
                Vec3 at = new Vec3(data[0], data[1], data[2]);
                RcFx.at(RcFx.RING, true, 14, at.m_82520_(0.0, 0.2, 0.0)).colors(0xC0FF3040, RcFx.clear(RcFx.CRIMSON)).size(1.0F, 24.0F).bloom(1.0F).spawn();
                WarPathFx.feel(minecraft, at, 40.0, 1.0F, 30.0);
            }
            case S2CKingFx.PEACE_LASH -> {
                Erupt erupt = ERUPTS.get((long)(ids.length > 0 ? ids[0] : 0));
                if (erupt != null && ids.length >= 3) {
                    erupt.lashes.put(ids[1], new double[] {ids[2], now});
                }
            }
            case S2CKingFx.PEACE_LANCE -> lance(level, owner, ids, data, now);
            case S2CKingFx.PEACE_THRALL -> {
                if (ids.length < 3) {
                    return;
                }
                Thrall thrall = THRALLS.computeIfAbsent(ids[0], id -> new Thrall(now, PeaceRules.THRALL_LIFE));
                switch (ids[1]) {
                    case 0 -> {
                        thrall.bornAt = now;
                        thrall.life = ids[2];
                    }
                    case 1 -> {
                        thrall.bladesAt = now;
                        thrall.bladesOffAt = Double.NaN;
                    }
                    case 2 -> {
                        if (!Double.isNaN(thrall.bladesAt)) {
                            thrall.bladesOffAt = now;
                        }
                        thrall.bladesAt = Double.NaN;
                    }
                    case 3 -> {
                        thrall.swingAt = now;
                        thrall.side = -thrall.side;
                        if (Double.isNaN(thrall.bladesAt)) {
                            thrall.bladesAt = now - 6.0;
                        }
                    }
                    case 4 -> {
                        thrall.meltAt = now;
                        Entity body = level.m_6815_(ids[0]);
                        if (body != null) {
                            KingMarks.play(level, com.hex.kagune.control.king.KingMarkFx.BLOOD, body.m_20182_().m_82520_(0.0, 0.3, 0.0), new Vec3(0.0, -1.0, 0.0), 0.9F, null);
                        }
                    }
                    default -> {
                    }
                }
            }
            default -> {
            }
        }
    }

    private static void lance(ClientLevel level, UUID owner, int[] ids, float[] data, double now) {
        if (ids.length < 1) {
            return;
        }
        int phase = ids[0];
        if (phase == 0) {
            LANCES.put(owner, new Lance(now));
            return;
        }
        Lance lance = LANCES.get(owner);
        if (lance == null) {
            lance = new Lance(now - PeaceRules.PIERCE_CHARGE);
            LANCES.put(owner, lance);
        }
        switch (phase) {
            case 1 -> {
                if (data.length >= 8) {
                    lance.firedAt = now;
                    lance.from = new double[] {data[0], data[1], data[2]};
                    lance.dir = new double[] {data[3], data[4], data[5]};
                    lance.length = data[6];
                    lance.speed = data[7];
                }
            }
            case 2 -> lance.target = ids.length > 1 ? ids[1] : 0;
            case 3 -> {
                lance.pinnedAt = now;
                Entity body = ids.length > 1 ? level.m_6815_(ids[1]) : null;
                if (body != null) {
                    KingMarks.play(level, com.hex.kagune.control.king.KingMarkFx.QUAKE, body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.5, 0.0), Vec3.f_82478_, 1.0F, null);
                }
            }
            case 4 -> {
                double[] tip = tip(level, lance, now);
                lance.backAt = now;
                lance.backFrom = tip;
            }
            default -> {
            }
        }
    }

    /** A body the blades tore out of: blood thrown every way (the mod's blood), and a red burst. */
    private static void tornOut(ClientLevel level, Entity body) {
        Vec3 c = body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.55, 0.0);
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3.0;
            KingMarks.play(level, com.hex.kagune.control.king.KingMarkFx.BLOOD, c, new Vec3(Math.cos(a), 0.4 + 0.3 * (k % 2), Math.sin(a)), 1.4F, body);
        }
        KingMarks.play(level, com.hex.kagune.control.king.KingMarkFx.GORE, c, new Vec3(0.0, 1.0, 0.0), 1.3F, body);
    }

    // ------------------------------------------------------------------ the blades' aims

    /**
     * {@code player}'s blades as these send them at {@code now}: per blade {x, y, z body pixels, how far out 0..1, the way in x, y, z
     * (NaN: from its root), how far short its heart stops, its size}, or null; null for none at all. The lance first, then the blades in
     * bodies, then the blades sunk in the ground.
     */
    public static double[][] aims(LivingEntity player, double now) {
        return aims(player, now, 1.0F);
    }

    /**
     * 1.2.8: the frame's partial tick the aims are worked out for. They are in his body's frame, and the body is drawn where it is
     * between two ticks, turned as it is between them: taken from where he and his targets were at the last tick, every blade in a body
     * was flung about by however far he had moved or turned since (the blades jumping off what they were in as he walked or looked
     * round, in every move of Peace's that sinks them into something).
     */
    private static float partial = 1.0F;

    public static double[][] aims(LivingEntity player, double now, float frame) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level == null) {
            return null;
        }
        partial = Math.max(0.0F, Math.min(1.0F, frame));
        UUID id = player.m_20148_();
        double[][] aims = new double[PeaceMotion.BLADES][];
        boolean any = lanceAims(level, player, now, aims);
        any |= impaleAims(level, player, now, aims);
        any |= buryAims(player, now, aims);
        if (!any) {
            Impale[] all = IMPALES.get(id);
            if (all != null) {
                boolean left = false;
                for (Impale impale : all) {
                    left |= impale != null;
                }
                if (!left) {
                    IMPALES.remove(id);
                }
            }
        }
        return any ? aims : null;
    }

    /**
     * {@code world} as {@code player}'s body pixels (his feet, the way his body faces): x his left, y up, z back. 1.0.40: the way his
     * body faces, not his head: the blades are drawn off his body, and a body left up to 50 degrees behind his head (standing, turning
     * only his view) sent every blade that far off what it was aimed at.
     */
    static double[] body(LivingEntity player, double[] world) {
        double yaw = Math.toRadians(bodyYaw(player));
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double[] feet = feet(player);
        double dx = world[0] - feet[0];
        double dy = world[1] - feet[1];
        double dz = world[2] - feet[2];
        double forward = -dx * sin + dz * cos;
        double left = dx * cos + dz * sin;
        double px = PeaceMotion.BLOCK;
        return new double[] {left * px, dy * px, -forward * px};
    }

    /** A way in the world as {@code player}'s body frame (unit; 1.0.40: the way his body faces, as {@link #body}). */
    static double[] bodyWay(LivingEntity player, double[] way) {
        double yaw = Math.toRadians(bodyYaw(player));
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double forward = -way[0] * sin + way[2] * cos;
        double left = way[0] * cos + way[2] * sin;
        return new double[] {left, way[1], -forward};
    }

    /** His right and ahead in the world (level). */
    static double[] right(LivingEntity player) {
        double yaw = Math.toRadians(player.f_20885_);
        return new double[] {-Math.cos(yaw), 0.0, -Math.sin(yaw)};
    }

    static double[] ahead(LivingEntity player) {
        double yaw = Math.toRadians(player.f_20885_);
        return new double[] {-Math.sin(yaw), 0.0, Math.cos(yaw)};
    }

    private static double[] aim(LivingEntity player, double[] at, double out, double[] way, double back, double size) {
        return aim(player, at, out, way, back, size, null);
    }

    /** 1.0.36: as above, the blade's face turned to {@code face} (world, unit; null: as its way has it). */
    private static double[] aim(LivingEntity player, double[] at, double out, double[] way, double back, double size, double[] face) {
        double[] p = body(player, at);
        double[] w = way == null ? new double[] {Double.NaN, Double.NaN, Double.NaN} : bodyWay(player, way);
        double[] f = face == null ? new double[] {Double.NaN, Double.NaN, Double.NaN} : bodyWay(player, face);
        // 1.2.8: the blades are drawn on his torso as it is posed (turned to his look through a move, leaning as he runs): taken back
        // through that pose, so what a blade is in stays where it is in the world however his chest turns.
        double[] torso = torso(player);
        if (torso != null) {
            p = unposed(torso, p, true);
            if (way != null) {
                w = unposed(torso, w, false);
            }
            if (face != null) {
                f = unposed(torso, f, false);
            }
        }
        double px = PeaceMotion.BLOCK;
        return new double[] {p[0], p[1], p[2], out, w[0] * px, w[1] * px, w[2] * px, back * px, size, f[0], f[1], f[2], face == null ? 0.0 : 1.0};
    }

    static double smooth(double x) {
        double t = Math.max(0.0, Math.min(1.0, x));
        return t * t * (3.0 - 2.0 * t);
    }

    /** 1.2.8: how his torso is carried this frame ({@code BodyFrame.carry}: rig p to A p + b), or null when nothing poses it. */
    private static double[] torso(LivingEntity player) {
        com.hex.kagune.control.client.animation.RigPose pose = player instanceof net.minecraft.client.player.AbstractClientPlayer p
            ? com.hex.kagune.control.client.animation.KaguneBodyAnimation.current(p, partial)
            : com.hex.kagune.control.client.render.EtoRenderer.lastPose(player.m_19879_()); // 1.3.19: Ken, as last drawn
        return pose == null ? null : com.hex.kagune.control.client.animation.BodyFrame.carry(pose, "torso");
    }

    /**
     * 1.2.8: {@code bp} (body pixels: x his left, y up, z back; a point, or a way when {@code point} is false) as it must be given
     * for his torso as carried by {@code a} to put it there: back through the carry (rigid, so A's inverse is its transpose).
     */
    static double[] unposed(double[] a, double[] bp, boolean point) {
        double px = PeaceMotion.BLOCK;
        double k = point ? 1.0 / px : 1.0;
        double rx = -bp[0] * k;
        double ry = bp[1] * k;
        double rz = -bp[2] * k;
        if (point) {
            rx -= a[9];
            ry -= a[10];
            rz -= a[11];
        }
        double ux = a[0] * rx + a[3] * ry + a[6] * rz;
        double uy = a[1] * rx + a[4] * ry + a[7] * rz;
        double uz = a[2] * rx + a[5] * ry + a[8] * rz;
        double back = point ? px : 1.0;
        return new double[] {-ux * back, uy * back, -uz * back};
    }

    static double[] add(double[] a, double[] b, double k) {
        return new double[] {a[0] + b[0] * k, a[1] + b[1] * k, a[2] + b[2] * k};
    }

    static double[] unit(double[] v) {
        double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return l < 1.0E-9 ? new double[] {0.0, 1.0, 0.0} : new double[] {v[0] / l, v[1] / l, v[2] / l};
    }

    static double[] centre(Entity body, double share) {
        double[] feet = feet(body);
        return new double[] {feet[0], feet[1] + body.m_20206_() * share, feet[2]};
    }

    /** 1.2.8: where {@code body}'s feet are drawn this frame ({@link #partial} of the way from the last tick to this one). */
    static double[] feet(Entity body) {
        return new double[] {body.f_19790_ + (body.m_20185_() - body.f_19790_) * partial, body.f_19791_ + (body.m_20186_() - body.f_19791_) * partial,
            body.f_19792_ + (body.m_20189_() - body.f_19792_) * partial};
    }

    /** 1.2.8: the way his body is drawn turned this frame (degrees). */
    static double bodyYaw(LivingEntity player) {
        return player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * partial;
    }

    private static boolean impaleAims(ClientLevel level, LivingEntity player, double now, double[][] aims) {
        Impale[] all = IMPALES.get(player.m_20148_());
        if (all == null) {
            return false;
        }
        boolean any = false;
        for (int blade = 0; blade < all.length; blade++) {
            Impale impale = all[blade];
            if (impale == null || aims[blade] != null) {
                continue;
            }
            if (impale.right == null) {
                impale.right = right(player);
                impale.ahead = ahead(player);
            }
            double[] right = impale.right;
            double age = now - impale.at;
            if (age < -2.0 || age > 400.0 || !Double.isNaN(impale.releaseAt) && now - impale.releaseAt > 10.0) {
                all[blade] = null;
                continue;
            }
            Entity body = level.m_6815_(impale.target);
            if (body == null && impale.lastAt == null) {
                continue;
            }
            int tier = PeaceMotion.tier(blade);
            double[] at;
            if (body == null) {
                at = impale.lastAt; // 1.2.8: it stays where it struck till the move is over
            } else {
                at = centre(body, 0.62 - 0.14 * tier);
                if (impale.mode == 0) {
                    // 1.0.36: each on its own side of it, at its own height (not all into one point).
                    at = PeaceFxModel.waltzEntry(blade, feet(body), body.m_20205_(), body.m_20206_(), right);
                }
                impale.lastAt = at;
            }
            double out = smooth(age / impale.reach());
            if (!Double.isNaN(impale.releaseAt)) {
                out *= 1.0 - smooth((now - impale.releaseAt) / 8.0);
            }
            double[] way = null;
            if (impale.mode == 0) {
                // 1.0.36: in from its own side, slanting across through it: they cross inside it.
                if (impale.inFrom == null) {
                    double[] to = {at[0] - feet(player)[0], 0.0, at[2] - feet(player)[2]};
                    double flat = Math.hypot(to[0], to[2]);
                    impale.inFrom = flat < 1.0E-3 ? impale.ahead : new double[] {to[0] / flat, 0.0, to[2] / flat};
                }
                way = PeaceFxModel.waltzWay(blade, impale.inFrom, right);
            } else if (impale.mode == 1) {
                // The Horizon: each in from its own side of it, from above, level and below.
                double a = Math.toRadians(blade * 60.0 + 30.0);
                double tilt = Math.toRadians(tier == 0 ? 40.0 : tier == 1 ? 5.0 : -35.0);
                way = new double[] {-Math.cos(a) * Math.cos(tilt), -Math.sin(tilt), -Math.sin(a) * Math.cos(tilt)};
            } else if (impale.mode == 2) {
                way = unit(new double[] {impale.ahead[0] * 0.3, 1.0, impale.ahead[2] * 0.3});
            } else if (impale.mode == 3) {
                way = unit(new double[] {impale.ahead[0] * 0.2, -1.0, impale.ahead[2] * 0.2});
            }
            if (!Double.isNaN(impale.tearAt)) {
                // Torn out every way at once: each to its own side, up or down as it went in.
                double k = smooth((now - impale.tearAt) / 3.0) * 1.6;
                int r = PeaceMotion.right(blade);
                double[] outward = unit(new double[] {right[0] * r, tier == 0 ? 0.8 : tier == 2 ? -0.6 : 0.1, right[2] * r});
                at = add(at, outward, k);
            }
            aims[blade] = aim(player, at, out, way, 0.45, 1.1);
            any = true;
        }
        return any;
    }

    /** Where the lance's point is now (world). */
    static double[] tip(ClientLevel level, Lance lance, double now) {
        if (Double.isNaN(lance.firedAt) || lance.from == null) {
            return null;
        }
        if (!Double.isNaN(lance.backAt) && lance.backFrom != null) {
            return lance.backFrom;
        }
        if (lance.target != 0) {
            Entity body = level.m_6815_(lance.target);
            if (body != null) {
                return add(centre(body, 0.55), lance.dir, 0.4);
            }
        }
        double gone = Math.min(PeaceRules.PIERCE_RANGE, lance.speed * Math.max(0.0, now - lance.firedAt));
        return add(lance.from, lance.dir, gone);
    }

    private static boolean lanceAims(ClientLevel level, LivingEntity player, double now, double[][] aims) {
        Lance lance = LANCES.get(player.m_20148_());
        if (lance == null) {
            return false;
        }
        KingClient.View view = KingClient.get(player);
        boolean piercing = view != null && view.moveAt(now) == KingMove.PIERCE;
        if (!piercing && (Double.isNaN(lance.backAt) || now - lance.backAt > PeaceRules.PIERCE_BACK + 2)) {
            LANCES.remove(player.m_20148_());
            return false;
        }
        double[] way;
        double[] point;
        double length;
        double out;
        double size;
        if (Double.isNaN(lance.firedAt)) {
            // Gathering before him along his look, longer and greater the longer it is held.
            double k = PeaceRules.charge(now - lance.chargeAt);
            length = PeaceRules.lance(k);
            Vec3 eye = player.m_146892_();
            Vec3 look = player.m_20154_();
            way = unit(new double[] {look.f_82479_, look.f_82480_, look.f_82481_});
            point = add(add(new double[] {eye.f_82479_, eye.f_82480_ - 0.3, eye.f_82481_}, way, 1.1), way, length);
            out = smooth((now - lance.chargeAt) / 6.0);
            size = 1.1 + 0.8 * k;
        } else {
            way = lance.dir;
            point = tip(level, lance, now);
            if (point == null) {
                return false;
            }
            length = lance.length;
            size = 1.1 + 0.8 * Math.max(0.0, Math.min(1.0, (length - PeaceRules.LANCE_SHORT) / (PeaceRules.LANCE_LONG - PeaceRules.LANCE_SHORT)));
            out = 1.0;
            if (!Double.isNaN(lance.backAt)) {
                out = 1.0 - smooth((now - lance.backAt) / PeaceRules.PIERCE_BACK);
            }
        }
        // 1.0.36: one lance: the six closed round its axis into a cone, their points all at its tip, their flats turned out (petals
        // closed); their arms all along the one line back to him, its shaft.
        double[][] cone = PeaceFxModel.lanceCone(way, size);
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            double[] c = cone[blade];
            aims[blade] = aim(player, point, out, new double[] {c[0], c[1], c[2]}, c[6], size, new double[] {c[3], c[4], c[5]});
        }
        return true;
    }

    /** 1.2.8: each player's Rebirth as it began: {his right, ahead, feet, {when}} (world). */
    private static final Map<UUID, double[][]> BURIED = new HashMap<>();

    private static boolean buryAims(LivingEntity player, double now, double[][] aims) {
        KingClient.View view = KingClient.get(player);
        if (view == null || view.moveAt(now) != KingMove.REBIRTH) {
            return false;
        }
        double age = now - view.moveAt();
        double out = view.level() == 0 ? smooth(age / PeaceRules.REBIRTH_SINK) : 1.0 - smooth(age / PeaceRules.REBIRTH_BACK);
        if (out <= 0.0) {
            return false;
        }
        // 1.2.8: sunk where they went in as it began (his right, ahead and feet then), never swung round as he looks about.
        double[][] frame = BURIED.get(player.m_20148_());
        if (frame == null || frame[3][0] != view.moveAt()) {
            frame = new double[][] {right(player), ahead(player), feet(player), {view.moveAt()}};
            BURIED.put(player.m_20148_(), frame);
        }
        double[] right = frame[0];
        double[] ahead = frame[1];
        double[] feet = frame[2];
        boolean any = false;
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            if (aims[blade] != null) {
                continue;
            }
            int r = PeaceMotion.right(blade);
            int tier = PeaceMotion.tier(blade);
            double[] at = add(add(feet, ahead, 1.3 + 0.55 * (2 - tier)), right, r * (0.7 + 0.5 * tier));
            at[1] -= 0.75 + 0.15 * Math.sin(now * 0.3 + blade);
            double[] way = unit(new double[] {ahead[0] * 0.25, -1.0, ahead[2] * 0.25});
            aims[blade] = aim(player, at, out, way, 0.2, 1.1);
            any = true;
        }
        return any;
    }

    // ------------------------------------------------------------------ out of his back, and out of the ground

    /** What grows out of {@code player}'s back this frame (the Aegis's shields, the Ascent's tendrils), into {@code sink} (world). */
    public static void player(UkakuModel.Sink sink, LivingEntity player, float partial, double now, Vec3 camera) {
        UUID id = player.m_20148_();
        Aegis aegis = AEGIS.get(id);
        Lift lift = LIFTS.get(id);
        if (aegis == null && lift == null) {
            return;
        }
        if (player.m_20182_().m_82557_(camera) > FAR * FAR) {
            return;
        }
        double x = player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial;
        double y = player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial;
        double z = player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial;
        double yaw = Math.toRadians(player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * partial);
        double[] ahead = {-Math.sin(yaw), 0.0, Math.cos(yaw)};
        double[] right = {-Math.cos(yaw), 0.0, -Math.sin(yaw)};
        double[] up = {0.0, 1.0, 0.0};
        double[] chest = {x, y + 1.25, z};
        boolean own = player == Minecraft.m_91087_().f_91074_ && Minecraft.m_91087_().f_91066_.m_92176_().m_90612_();
        if (aegis != null) {
            double grow = smooth((now - aegis.at) / 10.0) * (Double.isNaN(aegis.offAt) ? 1.0 : 1.0 - smooth((now - aegis.offAt) / 10.0));
            if (!Double.isNaN(aegis.offAt) && now - aegis.offAt > 12.0 || now - aegis.at > PeaceRules.AEGIS_TICKS + 40) {
                AEGIS.remove(id);
            } else if (grow > 0.01) {
                for (int i = 0; i < PeaceRules.SHIELDS; i++) {
                    shield(sink, aegis, i, chest, ahead, right, up, grow, now, own);
                }
            }
        }
        if (lift != null) {
            double grow = smooth((now - lift.at) / 6.0) * (lift.phase == 3 ? 1.0 - smooth((now - lift.phaseAt) / 10.0) : 1.0);
            if (lift.phase == 3 && now - lift.phaseAt > 12.0 || now - lift.at > 600.0) {
                LIFTS.remove(id);
            } else if (grow > 0.01) {
                for (int side = -1; side <= 1; side += 2) {
                    PeaceFxModel.liftTendril(sink, side, chest, ahead, right, side < 0 ? lift.left : lift.right, grow, now, own ? 6 : 8);
                }
            }
        }
    }

    /** Shield {@code i} as it is now: at rest by him, or swung out to meet something (and back again). */
    private static void shield(UkakuModel.Sink sink, Aegis aegis, int i, double[] chest, double[] ahead, double[] right, double[] up, double grow, double now,
                               boolean own) {
        double[] rest = PeaceFxModel.shieldRest(i, chest, ahead, right, now);
        double[] anchor = rest;
        Shield work = aegis.shields[i];
        if (work != null) {
            double since = now - work.at();
            double reach = work.action() == PeaceRules.SHIELD_BLOCK ? PeaceRules.SHIELD_HOLD : 16.0;
            if (since > PeaceRules.SHIELD_MEET + reach + 8.0) {
                aegis.shields[i] = null;
                work = null;
            } else {
                double k = smooth(since / PeaceRules.SHIELD_MEET) * (1.0 - smooth((since - PeaceRules.SHIELD_MEET - reach) / 8.0));
                double[] to = work.to();
                if (work.action() == PeaceRules.SHIELD_BLOCK) {
                    to = add(chest, unit(new double[] {to[0] - chest[0], to[1] - chest[1], to[2] - chest[2]}), 1.35);
                }
                anchor = new double[] {rest[0] + (to[0] - rest[0]) * k, rest[1] + (to[1] - rest[1]) * k, rest[2] + (to[2] - rest[2]) * k};
            }
        }
        // (in first person the one over his head would sit in his eyes)
        boolean shown = !(own && i == PeaceRules.SHIELD_TOP && work == null);
        PeaceFxModel.aegisShield(sink, i, chest, ahead, right, anchor, grow, now, shown, own ? 6 : 8);
    }

    /** Rebirth Instinct's creatures near, into {@code sink} (world). 1.0.38: found here (the game's renderer for them draws nothing). */
    private static void thralls(UkakuModel.Sink sink, ClientLevel level, float partial, double now, Vec3 camera) {
        if (level.m_46467_() % 100L == 0L) {
            THRALLS.keySet().removeIf(id -> level.m_6815_(id) == null);
        }
        double r = THRALL_FAR;
        java.util.List<com.hex.kagune.control.npc.FleshThrall> near = level.m_6443_(com.hex.kagune.control.npc.FleshThrall.class,
            new net.minecraft.world.phys.AABB(camera.f_82479_ - r, camera.f_82480_ - r, camera.f_82481_ - r, camera.f_82479_ + r, camera.f_82480_ + r,
                camera.f_82481_ + r), thrall -> true);
        int drawn = 0;
        for (com.hex.kagune.control.npc.FleshThrall body : near) {
            if (++drawn > 32) {
                break;
            }
            if (body.m_213877_() || body.m_9236_() != level) {
                continue;
            }
            Thrall thrall = THRALLS.computeIfAbsent(body.m_19879_(), id -> new Thrall(now - body.f_19797_, PeaceRules.THRALL_LIFE));
            double x = body.f_19790_ + (body.m_20185_() - body.f_19790_) * partial;
            double y = body.f_19791_ + (body.m_20186_() - body.f_19791_) * partial;
            double z = body.f_19792_ + (body.m_20189_() - body.f_19792_) * partial;
            if (!thrall.burst) {
                // Up out of the ground: torn earth and its blood.
                thrall.burst = true;
                burst(level, new double[] {x, y, z}, 0.5);
            }
            double yaw = Math.toRadians(body.f_20884_ + wrap(body.f_20883_ - body.f_20884_) * partial);
            double stride = body.f_267362_.m_267590_(partial) * 0.6662;
            double walking = Math.min(1.0, body.f_267362_.m_267711_(partial) * 1.4);
            double age = now - thrall.bornAt;
            double rise = Math.min(1.0, age / PeaceRules.THRALL_RISE);
            double melt = Double.isNaN(thrall.meltAt) ? 0.0 : Math.min(1.0, (now - thrall.meltAt) / PeaceRules.THRALL_MELT);
            double blades = !Double.isNaN(thrall.bladesAt) ? smooth((now - thrall.bladesAt) / 6.0)
                : !Double.isNaN(thrall.bladesOffAt) ? 1.0 - smooth((now - thrall.bladesOffAt) / 8.0) : 0.0;
            double swing = Double.NaN;
            if (!Double.isNaN(thrall.swingAt) && now - thrall.swingAt <= 12.0) {
                swing = Math.max(0.0, (now - thrall.swingAt) / 12.0);
            }
            PeaceFxModel.thrall(sink, new double[] {x, y, z}, yaw, stride, walking, blades * (1.0 - melt), swing, thrall.side, rise, melt, now,
                body.m_19879_() * 13);
        }
    }

    /** The flesh torn up out of the ground, into {@code sink} (world). */
    public static void world(UkakuModel.Sink sink, ClientLevel level, float partial, double now, Vec3 camera) {
        thralls(sink, level, partial, now, camera);
        for (Iterator<Erupt> it = ERUPTS.values().iterator(); it.hasNext(); ) {
            Erupt e = it.next();
            double age = now - e.at;
            if (age > PeaceRules.ERUPT_TICKS + 2 || age < -20.0) {
                it.remove();
                continue;
            }
            double dx = e.centre[0] - camera.f_82479_;
            double dz = e.centre[2] - camera.f_82481_;
            if (dx * dx + dz * dz > (FAR + PeaceRules.ERUPT_RADIUS) * (FAR + PeaceRules.ERUPT_RADIUS)) {
                continue;
            }
            double sink0 = 1.0 - smooth((age - (PeaceRules.ERUPT_TICKS - PeaceRules.ERUPT_SINK)) / PeaceRules.ERUPT_SINK);
            for (int i = 0; i < e.layout.length; i++) {
                double[] base = e.bases[i];
                if (base == null) {
                    continue;
                }
                double[] t = e.layout[i];
                double local = age - t[7] / PeaceRules.ERUPT_SPREAD;
                if (local < 0.0) {
                    continue;
                }
                if (!e.seen[i]) {
                    e.seen[i] = true;
                    burst(level, base, t[3]);
                }
                double grow = smooth(local / PeaceRules.ERUPT_GROW) * sink0;
                double[] lash = null;
                double lashing = 0.0;
                double[] l = e.lashes.get(i);
                if (l != null) {
                    double la = now - l[1];
                    Entity body = level.m_6815_((int)l[0]);
                    if (la > PeaceRules.LASH_TICKS || body == null) {
                        e.lashes.remove(i);
                    } else {
                        lashing = Math.sin(Math.PI * Math.min(1.0, la / PeaceRules.LASH_TICKS));
                        lash = centre(body, 0.5);
                    }
                }
                double[][] path = PeaceFxModel.eruptionPath(base, t, 8);
                double near = (base[0] - camera.f_82479_) * (base[0] - camera.f_82479_) + (base[2] - camera.f_82481_) * (base[2] - camera.f_82481_);
                PeaceFxModel.flesh(sink, path, t[3], grow, lash, lashing, now, (int)(e.seed * 31 + i), near < 32.0 * 32.0 ? 7 : 5);
            }
        }
    }

    /** One tendril breaking up out of the ground: clods, dust and blood. */
    private static void burst(ClientLevel level, double[] base, double thick) {
        Vec3 at = new Vec3(base[0], base[1] + 0.2, base[2]);
        if (!RcFx.near(at, FAR)) {
            return;
        }
        KingMarks.play(level, com.hex.kagune.control.king.KingMarkFx.QUAKE, at, Vec3.f_82478_, (float)(0.4 + thick), null);
        for (int k = 0; k < RcFx.count((float)(4.0 + 6.0 * thick)); k++) {
            Vec3 v = new Vec3(Math.random() - 0.5, 0.3 + Math.random() * 0.4, Math.random() - 0.5).m_82490_(0.25);
            boolean red = k % 3 == 0;
            RcFx.at(red ? RcFx.EMBER : RcFx.PUFF, red, 16 + (int)(Math.random() * 10), at).velocity(v).drag(0.88F)
                .colors(red ? RcFx.RED : 0xB0403A34, red ? RcFx.clear(RcFx.CRIMSON) : RcFx.clear(0xFF5A524A)).size(red ? 0.14F : 0.7F, red ? 0.04F : 1.4F)
                .fall(red ? 0.01F : 0.0F).spawn();
        }
    }

    static float wrap(float degrees) {
        return degrees - 360.0F * (float)Math.floor((degrees + 180.0F) / 360.0F);
    }

    /** Everything of theirs forgotten (leaving the world). */
    public static void clear() {
        IMPALES.clear();
        LANCES.clear();
        AEGIS.clear();
        LIFTS.clear();
        ERUPTS.clear();
        THRALLS.clear();
    }
}

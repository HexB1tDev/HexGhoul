package com.hex.kagune.control.ayato;

import static com.hex.kagune.control.ayato.UkakuModel.*;

/**
 * The crystallized Ukaku wing: the flame frozen into a cluster of RC crystal, to strike (folding into a blade) and to
 * fire Shard Volleys (shards leave the tips).
 *
 * <p>A quartz cluster that grows out of the kagune, like Touka's crystallized wing. On the shoulder blade sits a
 * solid, faceted root mass crowned with stubby crystals. Out of it, every flame tongue ({@link UkakuFlame#layout})
 * freezes into a solid, rough crystal limb, and six-sided quartz points with pyramid tips grow along each limb,
 * packed tight and fanning outward and on toward the tip. Every crystal's base is set inside its limb (the small
 * ones inside a bigger crystal), so nothing hangs loose. A long point carries each limb out to the tongue's tip; the
 * wisps freeze into slender needles rising out of the root mass. Nothing points down at the waist: every crystal
 * leans at least a little upward, more so near the root, and the lowest tongue and the body grow none on their
 * underside there. No two crystals are alike: length, width, how long the pyramid is, uneven shoulders and an
 * off-centre point. Each is a translucent gem with flat facets (lit here, by a key light, so neighbouring faces
 * contrast like cut stone), its own slight hue and brightness, white glints that move with the camera, and a pink
 * sheen where a face turns edge-on. Colour runs from fiery orange-red at the root through red and crimson into dark
 * violet at the tips (deep navy from √A on), each crystal lighter and pinker toward its point; the limbs and the root
 * mass are deeper.
 *
 * <p>To cut, the limbs line up into one long blade along a single axis with their crystals folded close (the √A wing
 * blades), crimson with bright edges. While firing, white four-point stars with red halos flash at the tongue tips,
 * facing the camera. It forms from the root outward as the wing crystallizes, and when dismissed the crystals break
 * loose, tumbling and falling as they fade, and the limbs and root mass melt away after them.
 */
public final class UkakuCrystal {
    // Gem colours along the wing (root to tips).
    static final int EMBER = 0xFF5A1A;
    static final int RED = 0xFF1020;
    static final int CRIMSON = 0xC80A26;
    static final int VIOLET = 0x420A50;
    static final int NAVY = 0x1C1658;
    static final int POINT = 0xFFB4BC;
    /** RC crystal glows from inside: every face keeps at least this much of its own colour. */
    static final double INNER_GLOW = 0.5;
    static final int GLINT = 0xFFF2FA;
    static final int SHEEN = 0xFF2C58;
    // The folded wing blade (√A): crimson crystal with bright edges.
    static final int BLADE_BODY = 0xB00C34;
    static final int BLADE_POINT = 0xFFD6E8;
    // Volley flashes.
    static final int FLASH = 0xFFFFFF;
    static final int FLASH_HALO = 0xFF2C3A;

    /** The key light the facets are lit by (rig space: up, a little forward and to the right). */
    static final double[] LIGHT = normalize3(0.35, 1.0, 0.45);
    /** Crystal tongues reach a little further than the fire they froze from. */
    static final double CRYSTAL_REACH = 1.05;
    /** Every crystal leans at least this far upward (wing-local sine): nothing points down at the waist or hips. */
    static final double MIN_UP = 0.1;
    /** Near the root, beside the body, crystals lean further up than that. */
    static final double ROOT_UP = 0.3;
    /** Where along a main tongue its long point takes over from the limb and carries it to the tip. */
    static final double POINT_FROM = 0.45;

    private UkakuCrystal() {
    }

    /** Number of primary tongues per wing (each fires shards). */
    public static int primaries(boolean reEra) {
        int count = 0;
        for (UkakuFlame.Tongue tongue : UkakuFlame.layout(reEra)) {
            count += tongue.kind() == UkakuFlame.Kind.MAIN ? 1 : 0;
        }
        return count;
    }

    /** The primary tongues, lowest first. */
    static UkakuFlame.Tongue[] mains(boolean reEra) {
        UkakuFlame.Tongue[] all = UkakuFlame.layout(reEra);
        UkakuFlame.Tongue[] mains = new UkakuFlame.Tongue[primaries(reEra)];
        int n = 0;
        for (UkakuFlame.Tongue tongue : all) {
            if (tongue.kind() == UkakuFlame.Kind.MAIN) {
                mains[n++] = tongue;
            }
        }
        java.util.Arrays.sort(mains, java.util.Comparator.comparingDouble(UkakuFlame.Tongue::angle));
        return mains;
    }

    /** Wing-local point (u, v, w) on primary {@code primary} (0 lowest) at {@code along} (0 root .. 1 tip), at rest. */
    public static double[] launchPoint(boolean reEra, int primary, double along) {
        UkakuFlame.Tongue[] mains = mains(reEra);
        UkakuFlame.Tongue tongue = mains[Math.max(0, Math.min(mains.length - 1, primary))];
        double t = Math.max(0.0, Math.min(1.0, along));
        double[] dir = direction(tongue.angle(), tongue.tilt());
        double reach = tongue.reach() * CRYSTAL_REACH * t;
        return new double[] {dir[0] * reach, dir[1] * reach + 0.08 * tongue.reach() * t * t, dir[2] * reach};
    }

    /** The crystal front reaches a little past the tongue's own length (crystal tongues reach further than the fire). */
    static final double FRONT_REACH = 1.15;
    /** How soft the front is: a crystal forms over this much of the tongue as the front passes it. */
    static final double FRONT_SOFT = 0.12;

    /**
     * Everything about this wing's crystal for one frame. {@code weight} fades it;
     * {@code front} is how far up each tongue the crystal has climbed (0 the shoulder blade .. 1 the very tip).
     */
    record Look(double time, boolean fine, double dusk, double alpha, double weight, double deploy, boolean dissolving, double blade,
                double recoil, double[] eye, double[] eyeLocal, boolean reEra, double front) {
        Look withAlpha(double alpha) {
            return new Look(this.time, this.fine, this.dusk, alpha, this.weight, this.deploy, this.dissolving, this.blade, this.recoil, this.eye,
                this.eyeLocal, this.reEra, this.front);
        }
    }

    /** One crystallized wing, all the way to the tips, faded by {@code weight} (0..1). */
    public static void wing(Sink sink, Frame frame, Params p, double weight) {
        wing(sink, frame, p, weight, 1.0);
    }

    /**
     * One crystallized wing, faded by {@code weight}, grown up each tongue from the shoulder blade to {@code front} (0..1): the
     * crystal climbing the wing as it crystallizes, with a bright seam where it meets the fire still burning beyond it.
     */
    public static void wing(Sink sink, Frame frame, Params p, double weight, double front) {
        double deploy = clamp01((double)p.deploy());
        double alpha = clamp01((double)p.alpha()) * clamp01(weight);
        if (deploy <= 0.001 || alpha <= 0.003) {
            return;
        }
        double[] eye = p.eye() != null ? p.eye() : UkakuFlame.DEFAULT_EYE;
        Look look = new Look(p.time(), p.detail() >= 0.6F, smooth((p.mastery() - 250) / 200.0), alpha, clamp01(weight), deploy, p.dissolving(),
            smooth(clamp01((double)p.blade())), clamp01((double)p.recoil()), eye, toLocal(frame, eye), p.reEra(), clamp01(front));
        UkakuFlame.Tongue[] tongues = UkakuFlame.layout(p.reEra());
        UkakuFlame.Tongue[] mains = mains(p.reEra());
        rootMass(sink, frame, look);
        for (int i = 0; i < tongues.length; i++) {
            cluster(sink, frame, tongues[i], i, rank(mains, tongues[i]), mains.length, look);
        }
        if (look.fine() && !look.dissolving() && look.front() > 0.02 && look.front() < 0.995) {
            // The seam: where the crystal is freezing the fire, a bright band of light climbing each main tongue.
            double pulse = Math.sin(Math.PI * Math.min(1.0, look.front() / 0.15)) * 0.25 + 0.75;
            double fade = Math.min(1.0, (0.995 - look.front()) / 0.08);
            for (int i = 0; i < mains.length; i++) {
                Limb limb = limb(mains[i], pose(mains[i], i, mains.length, look), look);
                double[] at = limb.at(Math.min(1.03, look.front() * FRONT_REACH - FRONT_SOFT * 0.4));
                double size = (0.22 + 0.08 * hash(i * 11 + 5) + 0.04 * Math.sin(look.time() * 9.0 + i)) / frame.scale() * pulse;
                flash(sink, frame, at, size, look.withAlpha(look.alpha() * 0.85 * fade));
            }
        }
        if (look.fine() && look.recoil() > 0.03) {
            for (int i = 0; i < mains.length; i++) {
                double[] tip = tip(mains[i], i, mains.length, look);
                flash(sink, frame, tip, (0.5 + 0.28 * hash(i * 7 + 3)) / frame.scale() * look.recoil(), look);
            }
        }
    }

    /** A main tongue's order from the bottom (0) up, or -1. */
    static int rank(UkakuFlame.Tongue[] mains, UkakuFlame.Tongue tongue) {
        for (int i = 0; i < mains.length; i++) {
            if (mains[i] == tongue) {
                return i;
            }
        }
        return -1;
    }

    static double[] direction(double angleDeg, double tiltDeg) {
        double a = Math.toRadians(angleDeg);
        double b = Math.toRadians(tiltDeg);
        return new double[] {Math.cos(a) * Math.cos(b), Math.sin(a) * Math.cos(b), Math.sin(b)};
    }

    /** How far the wing has deployed (in from the root while dissolving is handled by the crystals breaking loose). */
    static double growth(Look look) {
        return look.dissolving() ? 1.0 : smooth(look.deploy() / 0.7);
    }

    /** A tongue's pose after folding toward the one long cutting blade: {angle, tilt, reach, width}. */
    static double[] pose(UkakuFlame.Tongue tongue, int rank, int mains, Look look) {
        double angle = tongue.angle();
        double tilt = tongue.tilt();
        double reach = tongue.reach() * CRYSTAL_REACH;
        double width = tongue.width();
        double blade = look.blade();
        if (blade > 0.0) {
            // Every tongue lines up along the wing's own axis (the swing turns that axis across the front), stacked into
            // a single blade, the middle tongue longest.
            double centre = (mains - 1) * 0.5;
            double order = rank >= 0 ? rank : centre;
            double offset = Math.abs(order - centre);
            angle = lerp(angle, 8.0 + 1.2 * (order - centre), blade);
            tilt = lerp(tilt, 2.0 * (order - centre), blade);
            reach = lerp(reach, rank >= 0 ? 1.4 - 0.1 * offset : 0.7, blade);
            width = lerp(width, rank >= 0 ? 0.06 : 0.05, blade);
        }
        return new double[] {angle, tilt, reach, width};
    }

    /** The tip of main tongue {@code rank} this frame (for the volley flashes). */
    static double[] tip(UkakuFlame.Tongue tongue, int rank, int mains, Look look) {
        double[] pose = pose(tongue, rank, mains, look);
        double reach = pose[2] * growth(look) * 0.98;
        double[] dir = direction(pose[0], pose[1]);
        return new double[] {dir[0] * reach, dir[1] * reach + 0.08 * pose[2] * 0.96, dir[2] * reach};
    }

    /** How far a crystal at {@code t} along its tongue has formed: grown as the front climbs past it. */
    static double form(Look look, double t) {
        return look.dissolving() ? 1.0 : smooth((look.front() * FRONT_REACH - t) / FRONT_SOFT);
    }

    /** How far a crystal running from {@code from} to {@code to} along its tongue has grown out behind the front (0..1). */
    static double reach(Look look, double from, double to) {
        return look.dissolving() ? 1.0 : clamp01((look.front() * FRONT_REACH - from) / Math.max(1.0E-6, to - from));
    }

    /** How solid the limbs and the root mass still are while the wing dissolves (they melt after the crystals break off). */
    static double melt(Look look, double delay) {
        return look.dissolving() ? 1.0 - smooth((1.0 - look.deploy() - delay) / 0.5) : 1.0;
    }

    /** How far up a crystal at {@code t} along its tongue must lean: the most near the root, beside the body. */
    static double minUp(double t, Look look) {
        return (MIN_UP + (ROOT_UP - MIN_UP) * (1.0 - smooth(t / 0.45))) * (1.0 - look.blade());
    }

    /** {@code axis} raised, keeping its heading, until it leans at least {@code minUp} upward (wing-local v). */
    static double[] upward(double[] axis, double minUp) {
        if (axis[1] >= minUp) {
            return axis;
        }
        double flat = Math.hypot(axis[0], axis[2]);
        if (flat < 1.0E-9) {
            return new double[] {0.0, 1.0, 0.0};
        }
        double k = Math.sqrt(1.0 - minUp * minUp) / flat;
        return new double[] {axis[0] * k, minUp, axis[2] * k};
    }

    // ------------------------------------------------------------------ the root mass

    /**
     * The solid crystal mass on the shoulder blade that every limb grows out of: a rough, faceted lump of molten-red
     * crystal crowned with stubby crystals leaning up, out and back. Sized in blocks, set a little behind the root so it
     * never reaches into the body, and flatter below than above.
     */
    private static void rootMass(Sink sink, Frame frame, Look look) {
        double unit = 1.0 / frame.scale();
        double size = growth(look) * (1.0 - 0.3 * look.blade()) * form(look, 0.0);
        double fade = melt(look, 0.3);
        if (size <= 0.02 || fade <= 0.02) {
            return;
        }
        double era = look.reEra() ? 1.2 : 1.0;
        double[] centre = {0.03 * unit, 0.03 * unit, 0.07 * unit};
        double ru = 0.16 * unit * size * era;
        double up = 0.17 * unit * size * era;
        double down = 0.1 * unit * size * era;
        double rw = 0.1 * unit * size; // thin through the back at every size, so it never reaches into the body
        int around = look.fine() ? 6 : 5;
        double[] polar = {Math.toRadians(55.0), Math.toRadians(118.0)};
        double[][][] ring = new double[polar.length][around][];
        for (int r = 0; r < polar.length; r++) {
            for (int s = 0; s < around; s++) {
                double azimuth = 2.0 * Math.PI * (s + 0.5 * r) / around + 0.4;
                double jag = 0.8 + 0.32 * hash(4001 + r * 17 + s);
                double c = Math.cos(polar[r]);
                ring[r][s] = add(centre, new double[] {Math.sin(polar[r]) * Math.cos(azimuth) * ru * jag, c * (c > 0.0 ? up : down) * jag,
                    Math.sin(polar[r]) * Math.sin(azimuth) * rw * jag});
            }
        }
        double[] top = add(centre, new double[] {0.2 * ru, up * 0.95, 0.15 * rw});
        double[] bottom = add(centre, new double[] {0.1 * ru, -down * 0.9, 0.1 * rw});
        double alpha = 0.95 * fade * look.alpha();
        for (int s = 0; s < around; s++) {
            int n = (s + 1) % around;
            int stone = mixInt(EMBER, 0xD80E26, 0.45 + 0.35 * hash(4101 + s));
            int deep = coreColour(0.12, look);
            lump(sink, frame, top, ring[0][n], ring[0][s], top, stone, stone, look, alpha);
            lump(sink, frame, ring[0][s], ring[0][n], ring[1][n], ring[1][s], stone, deep, look, alpha);
            lump(sink, frame, ring[1][s], ring[1][n], bottom, bottom, deep, deep, look, alpha);
        }
        // The crown: stubby crystals grown out of the mass, leaning up and out and back (never down, never at the head).
        int crown = look.fine() ? 5 : 2;
        for (int c = 0; c < crown; c++) {
            int h = 4201 + c * 29;
            double angle = Math.toRadians(32.0 + 58.0 * (c + 0.5 * hash(h)) / crown);
            double tilt = Math.toRadians(25.0 + 32.0 * hash(h + 1));
            double[] axis = {Math.cos(angle) * Math.cos(tilt), Math.sin(angle) * Math.cos(tilt), Math.sin(tilt)};
            double length = (0.3 + 0.18 * hash(h + 2)) * unit * size * era;
            double radius = length * (0.2 + 0.07 * hash(h + 3));
            double[] base = add(centre, scale(axis, 0.25 * Math.min(ru, up)));
            crystal(sink, frame, base, axis, length, radius, new double[] {0.0, 0.0, 1.0}, look.fine() ? 6 : 4, 0.04, h, false, look);
        }
    }

    /** One face of the root mass: lit like the crystals, its two edges coloured. */
    private static void lump(Sink sink, Frame frame, double[] a, double[] b, double[] c, double[] d, int ab, int cd, Look look, double alpha) {
        Light light = lighting(frame, a, b, c, d, look);
        int top = argb(lit(ab, light), alpha);
        int low = argb(lit(cd, light), alpha);
        glowQuad(sink, frame, a, b, c, d, top, top, low, low);
    }

    // ------------------------------------------------------------------ the limbs

    /** Where a tongue's limb runs this frame: its spine, its local axes, and its thickness. */
    record Limb(double[] dir, double[] side, double[] lift, double reach, double radius, double rise) {
        double[] at(double t) {
            return new double[] {this.dir[0] * this.reach * t, this.dir[1] * this.reach * t + this.rise * t * t, this.dir[2] * this.reach * t};
        }

        /** The way the limb runs at {@code t}: it curls up a little toward the tip, like the flame it froze from. */
        double[] tangent(double t) {
            return normalize(new double[] {this.dir[0] * this.reach, this.dir[1] * this.reach + 2.0 * this.rise * t, this.dir[2] * this.reach});
        }
    }

    static Limb limb(UkakuFlame.Tongue tongue, double[] pose, Look look) {
        double grown = growth(look);
        double[] dir = direction(pose[0], pose[1]);
        double[] side = {-Math.sin(Math.toRadians(pose[0])), Math.cos(Math.toRadians(pose[0])), 0.0};
        side = normalize(sub(side, scale(dir, dot(side, dir))));
        double share = tongue.kind() == UkakuFlame.Kind.BODY ? 0.46 : 0.4;
        return new Limb(dir, side, cross(dir, side), pose[2] * grown, pose[3] * share, 0.08 * pose[2] * grown);
    }

    /** The limb's radius at {@code t}: a narrow stem out of the root mass, swelling, then tapering toward the tip. */
    static double coreRadius(Limb limb, double t, double unit) {
        double swell = 0.6 + 0.4 * smooth(t / 0.25);
        double taper = t < 0.35 ? 1.0 : Math.pow(Math.max(0.0, (1.0 - t) / 0.65), 0.85);
        return Math.min(limb.radius() * swell * taper, 0.06 * unit + t * limb.reach() * 0.35);
    }

    /**
     * One tongue frozen: a solid limb with quartz points grown into it, packed along it and fanning out from it, a
     * few small ones sprouting off the biggest; a long point carries a main tongue to its tip. A wisp is a needle.
     */
    private static void cluster(Sink sink, Frame frame, UkakuFlame.Tongue tongue, int index, int rank, int mains, Look look) {
        double unit = 1.0 / frame.scale();
        double[] pose = pose(tongue, rank, mains, look);
        Limb limb = limb(tongue, pose, look);
        if (limb.reach() <= 0.01) {
            return;
        }
        UkakuFlame.Kind kind = tongue.kind();
        double spread = 1.0 - 0.85 * look.blade();
        if (kind == UkakuFlame.Kind.WISP) {
            needle(sink, frame, limb, index, look, spread);
            return;
        }
        boolean body = kind == UkakuFlame.Kind.BODY;
        // The limb runs out to where the long point has taken over (the body's end is covered by its own crystals), as far as
        // the front has climbed.
        double end = Math.min(body ? 0.85 : POINT_FROM + 0.22, look.dissolving() ? 1.0 : look.front() * FRONT_REACH);
        limbFaces(sink, frame, limb, index, end, body, look, unit);
        int count = look.fine() ? (body ? 9 : look.reEra() ? 9 : 11) : 3;
        int sides = look.fine() ? 6 : 4;
        double top = body ? 0.8 : 0.66;
        // The body and the lowest tongue sit beside the host: nothing grows on their underside near the root.
        boolean low = body || rank == 0;
        double[][] bases = new double[count][];
        double[][] axes = new double[count][];
        double[] lengths = new double[count];
        double[] radii = new double[count];
        double[] ts = new double[count];
        for (int j = 0; j < count; j++) {
            int h = index * 131 + j * 17 + 5;
            double t = 0.07 + (top - 0.07) * (j + 0.5 + 0.6 * (hash(h) - 0.5)) / count;
            // Around the limb: its upper edge (0), its back (90) and its lower edge (180), never its front (toward the body).
            double arcEnd = low && t < 0.45 ? 140.0 : 205.0;
            double phi = Math.toRadians(-25.0 + (arcEnd + 25.0) * fraction(j * 0.618034 + 0.2 * hash(h + 1)));
            double[] tan = limb.tangent(t);
            double[] radial = add(scale(limb.side(), Math.cos(phi)), scale(limb.lift(), Math.sin(phi)));
            radial = normalize(sub(radial, scale(tan, dot(radial, tan))));
            double r = coreRadius(limb, t, unit);
            // Fanning out from the limb and on toward its tip, widest near the root, with a little sideways jitter.
            double sweep = Math.toRadians((22.0 + 26.0 * hash(h + 2)) * (1.0 - 0.35 * t)) * spread;
            double[] axis = add(scale(tan, Math.cos(sweep)), scale(radial, Math.sin(sweep)));
            axis = normalize(add(axis, scale(cross(tan, radial), (hash(h + 3) - 0.5) * 0.25 * spread)));
            axis = upward(axis, minUp(t, look));
            double length = Math.min(limb.reach() * (1.03 - t), limb.reach() * (0.2 + 0.24 * hash(h + 4)) * (1.0 - 0.3 * t));
            // Its foot fits inside the limb: a crystal is never broader than the limb it grows from.
            double radius = Math.min(length * (0.1 + 0.08 * hash(h + 5)), 1.15 * r + 0.01 * unit);
            bases[j] = add(limb.at(t), scale(radial, r * 0.3));
            axes[j] = axis;
            lengths[j] = length;
            radii[j] = radius;
            ts[j] = t;
            // It finishes growing once the front has passed its tip: crystals never run ahead of the climbing crystal.
            double form = form(look, t + 0.35 * length / Math.max(1.0E-6, limb.reach()));
            if (form > 0.02) {
                double g = Math.min(1.0, (t * limb.reach() + length * 0.5) / Math.max(0.3, pose[2]));
                crystal(sink, frame, bases[j], axis, length * form, radius * form, limb.side(), sides, g, h, false, look);
            }
        }
        // Small crystals sprouting off the biggest ones, their feet inside the bigger crystal.
        int sprouts = look.fine() ? (body || !look.reEra() ? 2 : 1) : 0;
        for (int k = 0; k < sprouts; k++) {
            int parent = biggest(lengths, k);
            int h = index * 131 + 977 + k * 23;
            double form = form(look, ts[parent] + lengths[parent] / Math.max(1.0E-6, limb.reach()) + 0.05);
            if (form <= 0.02) {
                continue;
            }
            double[] away = normalize(cross(axes[parent], k % 2 == 0 ? limb.lift() : limb.side()));
            if (dot(away, away) < 0.5) {
                away = limb.side();
            }
            away = scale(away, hash(h) < 0.5 ? 1.0 : -1.0);
            double[] base = add(add(bases[parent], scale(axes[parent], (0.28 + 0.2 * hash(h + 1)) * lengths[parent])), scale(away, 0.25 * radii[parent]));
            double turn = Math.toRadians(26.0 + 14.0 * hash(h + 2)) * spread;
            double[] axis = upward(normalize(add(scale(axes[parent], Math.cos(turn)), scale(away, Math.sin(turn)))), minUp(ts[parent], look));
            double length = lengths[parent] * (0.38 + 0.18 * hash(h + 3));
            double radius = radii[parent] * (0.38 + 0.12 * hash(h + 4));
            double g = Math.min(1.0, (ts[parent] * limb.reach() + lengths[parent] * 0.6) / Math.max(0.3, pose[2]));
            crystal(sink, frame, base, axis, length * form, radius * form, limb.side(), 5, g, h, false, look);
        }
        if (body) {
            return;
        }
        // The long point: set into the limb where it is thick and carrying it out to the tongue's tip, growing out right behind
        // the front as it climbs.
        double[] base = limb.at(POINT_FROM);
        double[] reachOut = sub(limb.at(1.03), base);
        double length = Math.sqrt(dot(reachOut, reachOut));
        double grown = reach(look, POINT_FROM, 1.03);
        if (grown > 0.02 && length > 1.0E-4) {
            double radius = Math.min(1.05 * coreRadius(limb, POINT_FROM, unit), length * 0.13);
            crystal(sink, frame, base, scale(reachOut, 1.0 / length), length * grown, radius * (0.55 + 0.45 * smooth(grown / 0.3)), limb.side(), sides,
                0.95, index * 131 + 199, true, look);
        }
    }

    /** Index of the {@code k}-th longest crystal (0 the longest). */
    static int biggest(double[] lengths, int k) {
        int[] order = new int[lengths.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        for (int i = 0; i <= Math.min(k, order.length - 1); i++) {
            for (int j = i + 1; j < order.length; j++) {
                if (lengths[order[j]] > lengths[order[i]]) {
                    int swap = order[i];
                    order[i] = order[j];
                    order[j] = swap;
                }
            }
        }
        return order[Math.min(k, order.length - 1)];
    }

    static double fraction(double x) {
        return x - Math.floor(x);
    }

    /** A wisp frozen: a slender needle rising out of the root mass, with a smaller one sprouting off it. */
    private static void needle(Sink sink, Frame frame, Limb limb, int index, Look look, double spread) {
        int h = index * 131 + 7;
        double form = reach(look, 0.02, 0.92);
        if (form <= 0.02) {
            return;
        }
        double[] base = limb.at(0.02);
        double[] axis = upward(normalize(sub(limb.at(0.92), base)), minUp(0.5, look));
        double length = limb.reach() * 0.9;
        double radius = length * (0.035 + 0.01 * hash(h));
        crystal(sink, frame, base, axis, length * form, radius * form, limb.side(), look.fine() ? 5 : 4, 0.7, h, false, look);
        if (look.fine()) {
            double[] away = normalize(cross(axis, limb.side()));
            double[] sprout = add(base, scale(axis, (0.3 + 0.15 * hash(h + 1)) * length * form)); // on the needle as grown so far
            double turn = Math.toRadians(18.0 + 10.0 * hash(h + 2)) * spread;
            double[] tilt = upward(normalize(add(scale(axis, Math.cos(turn)), scale(away, Math.sin(turn)))), minUp(0.5, look));
            crystal(sink, frame, sprout, tilt, length * 0.4 * form, radius * 0.6 * form, limb.side(), 4, 0.8, h + 50, false, look);
        }
    }

    /** The limb: a rough, faceted crystal stem along the tongue's spine (deeper in colour than its crystals). */
    private static void limbFaces(Sink sink, Frame frame, Limb limb, int index, double end, boolean body, Look look, double unit) {
        double fade = melt(look, 0.15);
        if (end <= 0.02 || fade <= 0.02) {
            return;
        }
        int stations = look.fine() ? (body ? 5 : 4) : 3;
        int sides = look.fine() ? 5 : 4;
        double[][][] ring = new double[stations + 1][sides][];
        for (int k = 0; k <= stations; k++) {
            double t = end * k / stations;
            // The body's limb closes to a point under its crystals; a main limb ends inside its long point.
            double r = coreRadius(limb, t, unit) * (body && k == stations ? 0.2 : 1.0);
            double twist = 0.35 * k + index;
            for (int s = 0; s < sides; s++) {
                double angle = twist + 2.0 * Math.PI * s / sides;
                double jag = 0.8 + 0.35 * hash(index * 97 + k * 13 + s);
                double[] offset = add(scale(limb.side(), Math.cos(angle) * r * jag), scale(limb.lift(), Math.sin(angle) * r * jag * 0.8));
                ring[k][s] = add(limb.at(t), offset);
            }
        }
        double alpha = 0.95 * fade * look.alpha();
        for (int k = 0; k < stations; k++) {
            int c0 = coreColour(end * k / stations, look);
            int c1 = coreColour(end * (k + 1) / stations, look);
            for (int s = 0; s < sides; s++) {
                int n = (s + 1) % sides;
                double[] a = ring[k][s];
                double[] b = ring[k][n];
                double[] c = ring[k + 1][n];
                double[] d = ring[k + 1][s];
                Light light = lighting(frame, a, b, c, d, look);
                int lo = argb(lit(c0, light), alpha);
                int hi = argb(lit(c1, light), alpha);
                glowQuad(sink, frame, a, b, c, d, lo, lo, hi, hi);
            }
        }
    }

    // ------------------------------------------------------------------ one crystal

    /**
     * One quartz point: a six-sided prism (narrower at the foot, set into what it grows from) with a pyramid tip,
     * slightly flattened, each face flat-lit like cut stone. Every one its own shape: stubby or long-pointed, uneven
     * shoulders, the point pulled off-centre (except a tongue's long point, which ends exactly at the tip). {@code g}
     * is where it sits along the wing (0 root .. 1 tip), for its colour. When the wing dissolves, the crystal breaks
     * loose: it drifts out, falls, tumbles and fades.
     */
    private static void crystal(Sink sink, Frame frame, double[] base, double[] axis, double length, double radius, double[] reference, int sides,
                                double g, int h, boolean point, Look look) {
        if (length <= 1.0E-4) {
            return;
        }
        double fade = 1.0;
        if (look.dissolving() && look.deploy() < 0.999) {
            double progress = 1.0 - look.deploy();
            double loose = smooth((progress - 0.25 * hash(h + 11)) / 0.6);
            fade = 1.0 - smooth((progress - 0.35 - 0.3 * hash(h + 12)) / 0.35);
            double[] drift = {(hash(h + 13) - 0.5) * 0.5, -0.9 * loose * loose, 0.25 + 0.35 * hash(h + 14)};
            base = add(base, scale(drift, loose * 0.6));
            double spin = loose * (2.0 + 3.0 * hash(h + 15));
            axis = normalize(add(axis, scale(cross(axis, new double[] {0.3, 0.8, 0.5}), Math.sin(spin))));
            length *= 1.0 - 0.4 * loose;
            radius *= 1.0 - 0.4 * loose;
            if (fade <= 0.02) {
                return;
            }
        }
        double[] a1 = normalize(sub(reference, scale(axis, dot(reference, axis))));
        if (dot(a1, a1) < 0.5) {
            a1 = normalize(cross(axis, new double[] {0.0, 0.0, 1.0}));
        }
        double[] a2 = cross(axis, a1);
        double twist = hash(h + 9) * Math.PI;
        double body = length * (point ? 0.6 : 0.46 + 0.28 * hash(h + 8));
        double foot = 0.55 + 0.2 * hash(h + 19);
        double flat = 0.62 + 0.25 * hash(h + 20);
        double lean = point ? 0.0 : 0.8 * radius;
        double[] tip = add(add(base, scale(axis, length)), add(scale(a1, (hash(h + 21) - 0.5) * lean), scale(a2, (hash(h + 22) - 0.5) * lean)));
        double[][] low = new double[sides][];
        double[][] high = new double[sides][];
        for (int s = 0; s < sides; s++) {
            double angle = twist + 2.0 * Math.PI * s / sides;
            double[] ring = add(scale(a1, Math.cos(angle) * radius), scale(a2, Math.sin(angle) * radius * flat));
            low[s] = add(base, scale(ring, foot));
            // Uneven shoulders: every pyramid face a different size.
            high[s] = add(add(base, scale(axis, body * (1.0 + 0.26 * (hash(h * 31 + s + 7001) - 0.5)))), ring);
        }
        // Its own slight hue and brightness, so a cluster sparkles instead of reading as one surface.
        double vary = 0.85 + 0.3 * hash(h + 10);
        int stone = mixInt(gemColour(g, look), hash(h + 16) < 0.5 ? EMBER : CRIMSON, 0.15 * hash(h + 17));
        for (int s = 0; s < sides; s++) {
            int n = (s + 1) % sides;
            face(sink, frame, low[s], low[n], high[n], high[s], stone, vary, false, fade, look);
            face(sink, frame, high[s], high[n], tip, tip, stone, vary, true, fade, look);
        }
    }

    /** One flat face of a crystal: key light, a white glint from the camera, a pink sheen when edge-on. */
    private static void face(Sink sink, Frame frame, double[] a, double[] b, double[] c, double[] d, int stone, double vary, boolean point,
                             double fade, Look look) {
        Light light = lighting(frame, a, b, c, d, look).times(vary);
        int colour = point ? mixInt(stone, mixInt(POINT, BLADE_POINT, look.blade()), 0.45) : stone;
        int lit = lit(colour, light);
        double alpha = (point ? 0.9 : 0.8) * fade * look.alpha();
        int base = argb(lit, alpha);
        // The end it grows out of is a little darker and clearer, so overlapping crystals keep their depth.
        if (point) {
            glowQuad(sink, frame, a, b, c, d, base, base, base, base);
        } else {
            int deep = argb(scaleRgb(lit, 0.8), alpha * 0.85);
            glowQuad(sink, frame, a, b, c, d, deep, deep, base, base);
        }
    }

    /** The light a flat face gets: key light (with the crystal's inner glow), a white glint, a pink sheen edge-on. */
    record Light(double diffuse, double glint, double sheen) {
        Light times(double factor) {
            return new Light(this.diffuse * factor, this.glint, this.sheen);
        }
    }

    private static Light lighting(Frame frame, double[] a, double[] b, double[] c, double[] d, Look look) {
        double[] normal = normalize(cross(sub(c, a), sub(d, b)));
        if (dot(normal, normal) < 0.5) {
            normal = normalize(cross(sub(b, a), sub(c, a)));
        }
        double[] world = frame.at((a[0] + b[0] + c[0]) / 3.0, (a[1] + b[1] + c[1]) / 3.0, (a[2] + b[2] + c[2]) / 3.0);
        double[] n = rig(frame, normal);
        double[] view = normalize(sub(look.eye(), world));
        if (dot(n, view) < 0.0) {
            n = scale(n, -1.0); // unculled: light the side the camera sees
        }
        // Lit faces blaze, faces turned away keep the crystal's own inner glow: strong contrast, like cut stone.
        double diffuse = INNER_GLOW + 0.85 * Math.max(0.0, dot(n, LIGHT)) + 0.25 * Math.max(0.0, -dot(n, LIGHT));
        double[] half = normalize(add(LIGHT, view));
        double glint = Math.min(1.0, 1.6 * Math.pow(Math.max(0.0, dot(n, half)), 22.0));
        double sheen = Math.pow(1.0 - Math.abs(dot(n, view)), 2.0);
        return new Light(diffuse, glint, sheen);
    }

    private static int lit(int colour, Light light) {
        int c = scaleRgb(colour, light.diffuse());
        c = mixInt(c, SHEEN, 0.5 * light.sheen());
        return mixInt(c, GLINT, light.glint());
    }

    /** The limbs' and root mass's own colour: deeper than the crystals growing out of them. */
    static int coreColour(double t, Look look) {
        int c;
        if (t < 0.2) {
            c = mixInt(0xD80E26, 0x9A0A2C, t / 0.2);
        } else {
            c = mixInt(0x9A0A2C, mixInt(0x2A0A3E, 0x14104A, look.dusk()), smooth((t - 0.2) / 0.7));
        }
        return mixInt(c, BLADE_BODY, look.blade() * 0.7);
    }

    /** A crystal's colour by where it sits along the wing: fiery at the root, red, crimson, then dark violet or navy. */
    static int gemColour(double g, Look look) {
        int c;
        if (g < 0.18) {
            c = mixInt(EMBER, RED, g / 0.18);
        } else if (g < 0.5) {
            c = mixInt(RED, CRIMSON, (g - 0.18) / 0.32);
        } else {
            c = mixInt(CRIMSON, mixInt(VIOLET, NAVY, look.dusk()), smooth((g - 0.5) / 0.5));
        }
        return mixInt(c, BLADE_BODY, look.blade() * 0.75);
    }

    // ------------------------------------------------------------------ volley

    /** A four-point star flash with a round red halo, turned to face the camera. */
    private static void flash(Sink sink, Frame frame, double[] centre, double size, Look look) {
        double[] view = normalize(sub(look.eyeLocal(), centre));
        double[] right = normalize(cross(view, new double[] {0.0, 1.0, 0.0}));
        if (dot(right, right) < 0.5) {
            right = new double[] {1.0, 0.0, 0.0};
        }
        double[] up = cross(right, view);
        int core = argb(FLASH, Math.min(1.0, look.alpha() * 1.1));
        int clear = argb(FLASH_HALO, 0.0);
        int halo = argb(FLASH_HALO, 0.45 * look.alpha());
        double h = size * 0.8;
        for (int s = 0; s < 10; s++) {
            double a0 = 2.0 * Math.PI * s / 10.0;
            double a1 = 2.0 * Math.PI * (s + 1) / 10.0;
            double[] p0 = add(centre, add(scale(right, Math.cos(a0) * h), scale(up, Math.sin(a0) * h)));
            double[] p1 = add(centre, add(scale(right, Math.cos(a1) * h), scale(up, Math.sin(a1) * h)));
            glowQuad(sink, frame, centre, p0, p1, p1, halo, clear, clear, clear);
        }
        // Four long white points (the vertical pair longest) and four short diagonal ones.
        for (int arm = 0; arm < 8; arm++) {
            double angle = Math.PI / 4.0 * arm + 0.2;
            double length = arm % 2 == 0 ? (arm % 4 == 0 ? 1.0 : 0.75) * size : 0.32 * size;
            double[] out = add(scale(right, Math.cos(angle)), scale(up, Math.sin(angle)));
            double[] across = add(scale(right, -Math.sin(angle)), scale(up, Math.cos(angle)));
            double w = size * 0.09;
            glowQuad(sink, frame, add(centre, scale(across, w)), add(centre, scale(out, length)), add(centre, scale(across, -w)), centre, core,
                clear, core, core);
        }
    }

    // ------------------------------------------------------------------ vectors

    /** A wing-local direction turned into rig space (the frame's axes are orthonormal). */
    static double[] rig(Frame frame, double[] v) {
        return new double[] {frame.out()[0] * v[0] + frame.up()[0] * v[1] + frame.back()[0] * v[2],
            frame.out()[1] * v[0] + frame.up()[1] * v[1] + frame.back()[1] * v[2],
            frame.out()[2] * v[0] + frame.up()[2] * v[1] + frame.back()[2] * v[2]};
    }

    /** A rig-space point in the wing's local frame (inverse of {@link Frame#at}). */
    static double[] toLocal(Frame frame, double[] p) {
        double[] d = sub(p, frame.origin());
        return new double[] {dot(d, frame.out()) / frame.scale(), dot(d, frame.up()) / frame.scale(), dot(d, frame.back()) / frame.scale()};
    }

    static int scaleRgb(int rgb, double factor) {
        int r = (int)Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
        int g = (int)Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
        int b = (int)Math.min(255, Math.round((rgb & 0xFF) * factor));
        return r << 16 | g << 8 | b;
    }

    static double[] normalize3(double x, double y, double z) {
        return normalize(new double[] {x, y, z});
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] scale(double[] a, double s) {
        return new double[] {a[0] * s, a[1] * s, a[2] * s};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-12 ? new double[] {0.0, 0.0, 0.0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }
}

package com.hex.kagune.control.king;

import static com.hex.kagune.control.furuta.FurutaMesh.add;
import static com.hex.kagune.control.furuta.FurutaMesh.clamp01;
import static com.hex.kagune.control.furuta.FurutaMesh.cross;
import static com.hex.kagune.control.furuta.FurutaMesh.dot;
import static com.hex.kagune.control.furuta.FurutaMesh.lerp;
import static com.hex.kagune.control.furuta.FurutaMesh.mix;
import static com.hex.kagune.control.furuta.FurutaMesh.mul;
import static com.hex.kagune.control.furuta.FurutaMesh.normalize;
import static com.hex.kagune.control.furuta.FurutaMesh.sub;
import static com.hex.kagune.control.king.WarModel.v;

import com.hex.kagune.control.ayato.KakujaModel;
import com.hex.kagune.control.ayato.UkakuModel;
import com.hex.kagune.control.furuta.FurutaMesh;
import java.util.List;

/**
 * Peace, Kaneki :Re (pure; the other final kakuja): clean, elegant and symmetrical, white and pale silver organic
 * armour with massive wings of twisted crimson kagune and giant cross blades, after the Peace sheet
 * (tools/kakuja/reference/peace.webp). No mask: his face stays his own (1.0.18: the kakugan open in it).
 *
 * <p>Built in body pixels as {@link WarModel} is (x to the player's left, y up from the feet, z backward) and handed to the sink in
 * the pixels of the part it rides on.
 * <ul>
 *   <li>Torso: a white chest crest, a skull-like V plate with a raised ridge pointing down, swept up to the shoulders, two silver
 *       discs on the upper chest, layered plates over the ribs, pale silver plates over the belly, a white collar; on the back
 *       layered plates down a spine ridge and a crest between the shoulder blades; the white horns that rise beside his head and
 *       curl out at the tips; and the wings.</li>
 *   <li>Wings: one each side from the upper back, eleven bands of twisted crimson kagune fanning up, out and down, as far as two
 *       blocks out, a second layer between them and a lattice across, white struts along their upper edge ending in tall white
 *       spikes, ice-white shards along the lower bands. 1.0.18: alive (they breathe, writhe and follow him).</li>
 *   <li>1.0.18: the six cross blades, three each side ({@link PeaceMotion}), each held on its own blade arm of crimson kagune from
 *       his upper back, white plates down it and white claws gripping the blade's heart: four-pointed stars of white running to
 *       ice at the edges, a glowing red core down every point and a burning heart; at rest every long point turned ahead.</li>
 *   <li>Arms: a white shoulder plate and a white forearm guard on dark under-armour, the hands free. Legs: white knee plates and
 *       white armoured boots on dark under-armour.</li>
 * </ul>
 * {@code time} (ticks, fractional: built every frame) moves what lives; {@code grow} 0..1 lets it out of the body.
 */
public final class PeaceModel {
    public static final int WHITE = 0xFFF1F1F3;
    public static final int RIM = 0xFFD9DCE2;
    public static final int SILVER = 0xFFC7CCD4;
    public static final int SHADOW = 0xFF8F96A0;
    public static final int ICE = 0xFFDDF3FF;
    /** The cross blades' edges and points: ice gone steel, so they stand out against the sky. */
    public static final int STEEL_ICE = 0xFF94B8D2;
    public static final int OUTLINE = 0xFF4E6478;
    public static final int CRIMSON = 0xFF6E0A15;
    public static final int CRIMSON_L = 0xFF9A1426;
    public static final int FIBRE = 0xFF3E050C;
    public static final int CORE = 0xFFFF2234;
    public static final int BLADE_RED = 0xFFC4121F;
    public static final int UNDER = 0xFF2B2B30;
    public static final int UNDER_L = 0xFF3E3E46;

    /**
     * The wing's bands (left wing; the right mirrors x): where each roots on the upper back and where its point is, tracing the
     * wing's outline (body pixels), up over the shoulder, round the outer edge, and back in low.
     */
    static final double[][] ROOTS = {{1.6, 23.2, 3.0}, {2.2, 22.6, 3.1}, {2.8, 22.0, 3.1}, {3.2, 21.2, 3.0}, {3.5, 20.4, 3.0}, {3.6, 19.6, 2.9},
        {3.5, 18.8, 2.9}, {3.2, 18.0, 2.8}, {2.8, 17.2, 2.8}, {2.4, 16.4, 2.7}, {2.0, 15.8, 2.7}};
    static final double[][] TIPS = {{5, 47, 6}, {12, 47.5, 7}, {19, 45.5, 8}, {26, 41.5, 8.5}, {31, 35.5, 8.5}, {34, 28.5, 8}, {33.5, 21.5, 7.5},
        {30.5, 14.5, 7}, {25.5, 8.5, 6.5}, {18.5, 5, 6}, {11, 5.5, 5}};
    /** Bands across the wing, from one band's middle to another's, so the wing is a lattice, not spokes: {from, to} band indices. */
    static final int[][] CROSSES = {{0, 3}, {3, 7}, {5, 9}, {2, 6}};
    /** How far back the wing sweeps for every pixel out (the side view shows it spread deep behind him). */
    static final double SWEEP = 0.38;
    /** Where a band starts to come to its point (share of its length). */
    static final double TIP_FROM = 0.74;

    /** 1.0.18: what of Peace to build: its armour (still but for its growing over him), its kagune (alive, every frame), or both. */
    public static final int ARMOUR = 1;
    public static final int KAGUNE = 2;
    public static final int ALL = ARMOUR | KAGUNE;

    private PeaceModel() {
    }

    /** Everything of Peace that rides on {@code part}, in that part's own pixels, the blades at rest. */
    public static void build(UkakuModel.Sink sink, WarModel.Part part, double time, double grow) {
        build(sink, part, time, grow, PeaceMotion.State.REST);
    }

    /** 1.0.15: as above, the blades (and the wings) as {@code state} has them ({@link PeaceMotion}). */
    public static void build(UkakuModel.Sink sink, WarModel.Part part, double time, double grow, PeaceMotion.State state) {
        build(sink, part, time, grow, state, false);
    }

    /** 1.0.16: as above; {@code bladesOnly}: nothing but the blades on their arms (first person: what reaches out before him). */
    public static void build(UkakuModel.Sink sink, WarModel.Part part, double time, double grow, PeaceMotion.State state, boolean bladesOnly) {
        build(sink, part, time, grow, state, bladesOnly, null);
    }

    /**
     * 1.0.17: as above, coming over him ({@code state} EMERGE, {@link KingTransform}): the armour let out of his skin as far as
     * {@code growth} has reached from his heart, swelling like muscle ({@link KingGrowth}); the wings and the blades unfolding.
     */
    public static void build(UkakuModel.Sink sink, WarModel.Part part, double time, double grow, PeaceMotion.State state, boolean bladesOnly,
                             double[] growth) {
        build(sink, part, time, grow, state, bladesOnly, growth, ALL);
    }

    /** 1.0.18: as above, only {@code what} of it ({@link #ARMOUR}, {@link #KAGUNE} or {@link #ALL}). */
    public static void build(UkakuModel.Sink sink, WarModel.Part part, double time, double grow, PeaceMotion.State state, boolean bladesOnly,
                             double[] growth, int what) {
        boolean emerging = state != null && state.move() == KingMove.EMERGE;
        // 1.0.19: leaving him: the blades fold in and sink (RECEDE moves them), the wings wither, the armour recedes (growth).
        boolean receding = state != null && state.move() == KingMove.RECEDE;
        if (grow <= 0.02 && !emerging && !receding) {
            return;
        }
        WarModel.Body body = new WarModel.Body(sink, part);
        WarModel.Body armour = growth == null ? body : new WarModel.Body(KingGrowth.wrap(sink, part, growth), part);
        double g = emerging || receding ? 1.0 : FurutaMesh.smooth(clamp01(grow));
        double wings = emerging ? FurutaMesh.smooth(clamp01((state.age() - KingTransform.PEACE_WINGS_FROM) / 14.0))
            : receding ? 1.0 - FurutaMesh.smooth(clamp01((state.age() - KingTransform.PEACE_WITHER) / 18.0)) : g;
        double held = held(grow, state);
        if (bladesOnly) {
            if (part == WarModel.Part.TORSO && held > 0.02) {
                blades(body, time, held, state);
            }
            return;
        }
        boolean shell = (what & ARMOUR) != 0;
        boolean kagune = (what & KAGUNE) != 0;
        switch (part) {
            case HEAD -> {
            }
            case TORSO -> {
                if (shell) {
                    under(armour);
                    crest(armour, g);
                    back(armour, g);
                    horns(armour, time, g);
                }
                if (kagune && wings > 0.02) {
                    for (int sx = -1; sx <= 1; sx += 2) {
                        wing(body, sx, time, wings, state);
                    }
                }
                if (kagune && held > 0.02) {
                    blades(body, time, held, state);
                }
            }
            case RIGHT_ARM -> {
                if (shell) {
                    arm(armour, -1, g);
                }
            }
            case LEFT_ARM -> {
                if (shell) {
                    arm(armour, 1, g);
                }
            }
            case RIGHT_LEG -> {
                if (shell) {
                    leg(armour, -1, g);
                }
            }
            case LEFT_LEG -> {
                if (shell) {
                    leg(armour, 1, g);
                }
            }
        }
    }

    // ================================================================== the body

    /** White armour, a little silver in its hollows. */
    static int pale(double[] p, int seed) {
        return mix(WHITE, SILVER, 0.1 + 0.35 * FurutaMesh.noise(seed, p[0] * 0.5 + p[2] * 0.3, p[1] * 0.5));
    }

    /** Under-armour over the torso, dark where the white leaves it bare (the waist, the sides). */
    static void under(WarModel.Body body) {
        WarModel.shell(body, v(0.0, 18.1, 0.0), v(4.5, 6.2, 2.5), 8.0, 16, 32, p -> p[1] > 11.9 && p[1] < 24.3, p -> WarModel.mottle(p, UNDER, UNDER_L, 71));
    }

    static void crest(WarModel.Body body, double g) {
        double z = -2.75;
        // The V: two plates down from the collarbones to a point at the belly, a raised ridge down its middle.
        for (int sx = -1; sx <= 1; sx += 2) {
            double[] top = v(sx * 2.6, 23.4, z);
            double[] bottom = v(sx * 0.2, 13.4, z - 0.1);
            WarModel.shard(body, top, sub(bottom, top), v(0, 0, -1), Math.sqrt(dot(sub(bottom, top), sub(bottom, top))) * g + 0.5, 5.0, 1.2, v(1, 0, 0), WHITE,
                RIM, 0, 6, true);
            // Swept up and out to the shoulder.
            WarModel.shard(body, v(sx * 1.6, 22.6, z), v(sx * 1.0, 0.45, 0.05), v(0, 1, 0), 3.8 * g + 0.3, 2.2, 0.9, v(0, 1, 0), WHITE, SILVER, 0, 4, true);
            // Over the ribs: layered plates sweeping down and out.
            for (int k = 0; k < 3; k++) {
                double[] base = v(sx * 3.3, 21.0 - 2.4 * k, -2.3 + 0.2 * k);
                WarModel.shard(body, base, v(sx * 0.55, -1.0, 0.15), v(sx, 0, 0), (4.4 - 0.5 * k) * g + 0.3, 2.0, 0.8, v(0, 0, 1), k == 1 ? RIM : WHITE, SILVER, 0, 4,
                    true);
            }
            // The two silver discs on the upper chest.
            double[] disc = v(sx * 2.45, 21.3, z - 0.75);
            WarModel.disc(body, disc, v(0, 0, -1), v(0, 1, 0), 0.62, 1.05, 1.05, SHADOW, SILVER, 20, false);
            WarModel.disc(body, add(disc, v(0, 0, -0.06)), v(0, 0, -1), v(0, 1, 0), 0.0, 0.64, 0.64, SILVER, RIM, 16, false);
        }
        WarModel.shard(body, v(0.0, 23.2, z - 0.4), v(0, -1, 0), v(0, 0, -1), 9.6 * g + 0.4, 1.6, 1.5, v(1, 0, 0), WHITE, RIM, 0, 6);
        // The belly: pale silver plates in rows.
        for (int k = 0; k < 3; k++) {
            double y = 15.6 - 1.4 * k;
            for (int sx = -1; sx <= 1; sx += 2) {
                WarModel.shard(body, v(sx * 0.5, y, -2.6), v(sx, -0.12, 0.08), v(0, 0, -1), (3.4 - 0.4 * k) * g + 0.3, 1.2, 0.6, v(0, 1, 0), SILVER, SHADOW, 0, 3,
                    true);
            }
        }
        // The collar: white plates round the neck, rising a little, open at the throat.
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(-150.0 + 300.0 * k / 5.0);
            double[] base = v(Math.sin(a) * 2.9, 23.4, -Math.cos(a) * 2.0);
            if (Math.abs(Math.sin(a)) < 0.25 && Math.cos(a) > 0) {
                continue;
            }
            WarModel.shard(body, base, v(Math.sin(a) * 0.3, 1.0, -Math.cos(a) * 0.2), v(Math.sin(a), 0, -Math.cos(a)), 2.0 * g + 0.3, 2.2, 0.6,
                v(Math.cos(a), 0, Math.sin(a)), WHITE, RIM, 0, 3, true);
        }
    }

    static void back(WarModel.Body body, double g) {
        double z = 2.75;
        // Layered plates down the spine, and its ridge.
        for (int k = 0; k < 5; k++) {
            double y = 22.6 - 2.1 * k;
            for (int sx = -1; sx <= 1; sx += 2) {
                WarModel.shard(body, v(sx * 0.5, y, z), v(sx * 1.0, -0.35, 0.05), v(0, 0, 1), (4.0 - 0.3 * k) * g + 0.3, 1.8, 0.7, v(0, 1, 0), k % 2 == 0 ? WHITE : RIM,
                    SILVER, 0, 4, true);
            }
        }
        WarModel.shard(body, v(0.0, 23.4, z + 0.3), v(0, -1, 0), v(0, 0, 1), 10.5 * g + 0.4, 1.3, 1.4, v(1, 0, 0), WHITE, RIM, 0, 6);
        // The crest between the shoulder blades, swept up and out behind the head.
        for (int sx = -1; sx <= 1; sx += 2) {
            WarModel.shard(body, v(sx * 0.4, 21.6, z + 0.5), v(sx * 0.65, 1.0, 0.25), v(sx, 0, 0.3), 6.4 * g + 0.4, 2.4, 0.9, v(0, 0, 1), WHITE, ICE, 0, 5, true);
        }
    }

    /** A white horn along {@code control}: thick at the root to a point, silver beneath, ice at its edge. */
    static void horn(WarModel.Body body, List<double[]> control, double root, double tip) {
        KakujaModel.Spine sp = WarModel.spine(control, 22, v(0, 0, -1));
        FurutaMesh.tube(new WarModel.SinkAdapter(body), sp, 0.0, 1.0, 22, 8, new FurutaMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                return tip + (root - tip) * Math.pow(1.0 - s, 0.8);
            }

            @Override
            public int colour(double s, double a) {
                double under = Math.max(0.0, -Math.sin(a));
                double edge = Math.max(0.0, Math.cos(a));
                return mix(mix(WHITE, SHADOW, 0.55 * under), ICE, 0.5 * edge * edge * s);
            }
        }, 1.0);
    }

    static void horns(WarModel.Body body, double time, double g) {
        for (int sx = -1; sx <= 1; sx += 2) {
            double[][] big = {{2.2, 22.6, 0.2}, {4.9, 26.4, 0.6}, {6.3, 30.4, 0.6}, {6.7, 34.0, 0.2}, {8.6, 37.2, -0.2}, {11.0, 38.0, -0.4}};
            double[][] small = {{4.0, 23.0, 1.2}, {7.0, 25.4, 1.5}, {9.2, 28.4, 1.2}, {10.6, 31.6, 0.8}, {12.4, 33.2, 0.4}};
            horn(body, grown(big, sx, g), 2.2, 0.12);
            horn(body, grown(small, sx, g), 1.5, 0.1);
        }
    }

    /** Control points on side {@code sx}, grown out of the first by {@code g}. */
    static List<double[]> grown(double[][] points, int sx, double g) {
        java.util.ArrayList<double[]> out = new java.util.ArrayList<>();
        double[] root = v(sx * points[0][0], points[0][1], points[0][2]);
        for (double[] p : points) {
            out.add(lerp(root, v(sx * p[0], p[1], p[2]), g));
        }
        return out;
    }

    // ================================================================== the wings

    /**
     * One wing, side {@code sx}: bands of twisted crimson kagune fanning up, out and down from the upper back, a second layer between
     * them, a lattice across, white struts along the top ending in tall spikes, ice-white shards along the low bands. 1.0.18: alive. It
     * breathes (a slow swell open and back, running round it from its top to its foot); every band writhes on its own beat (its bow
     * swelling and slackening, an S running down it, its fibres crawling round it); the tips flutter. And it follows him: turning swings
     * it round behind him, running streams it back, rising drags its tips down, falling lifts them. Spread wide in some moves
     * ({@link #spread}).
     */
    static void wing(WarModel.Body body, int sx, double time, double g, PeaceMotion.State state) {
        double spread = spread(state);
        double lag = state == null ? 0.0 : state.lag();
        double run = state == null ? 0.0 : state.run();
        double rise = state == null ? 0.0 : Math.max(-1.0, Math.min(1.0, state.rise() / 0.45));
        int n = TIPS.length;
        double[][] roots = new double[n][];
        double[][] tips = new double[n][];
        double[][] bulges = new double[n][];
        double[][] writhes = new double[n][];
        for (int k = 0; k < n; k++) {
            roots[k] = v(sx * ROOTS[k][0], ROOTS[k][1], ROOTS[k][2]);
            double reach = TIPS[k][0] / 34.0;
            double up = (TIPS[k][1] - 20.0) / 27.0;
            double breath = Math.sin(time * 0.075 - k * 0.32);
            double x = TIPS[k][0] * (1.0 + 0.2 * spread) + 3.0 * breath * reach - 7.0 * run * reach;
            double y = TIPS[k][1] + (5.0 * spread + 2.6 * breath) * up - 8.0 * rise * reach;
            double z = TIPS[k][2] + SWEEP * TIPS[k][0] * (1.0 - 0.45 * spread) + 2.2 * (1.0 + breath) * (0.4 + 0.6 * reach) + 14.0 * run * reach;
            tips[k] = lerp(roots[k], PeaceMotion.spin(v(sx * x, y, z), -lag * 0.7 * reach), g);
            double[] out = normalize(sub(tips[k], roots[k]));
            double[] across = normalize(v(-out[1] * sx, out[0] * sx, 0.0));
            tips[k] = add(tips[k], mul(across, 1.1 * reach * g * Math.sin(time * 0.21 + k * 2.1 + sx)));
            // Each bows out a little, alternately up and down, and back, swelling and slackening; an S runs down it.
            double beat = Math.sin(time * 0.11 + k * 1.37 + (sx > 0 ? 0.0 : 2.1));
            bulges[k] = add(mul(across, (k % 2 == 0 ? 1.0 : -0.6) * (1.3 + 0.2 * (k % 3)) * (1.0 + 0.45 * beat)),
                v(0, 0, 2.2 + 0.7 * Math.sin(time * 0.09 + k)));
            writhes[k] = mul(across, 1.4 * g * Math.sin(time * 0.15 + k * 0.9 + sx));
        }
        for (int k = 0; k < n; k++) {
            KakujaModel.Spine sp = band(body, roots[k], tips[k], bulges[k], writhes[k], 2.7 - 0.06 * k, 1.2 + 0.3 * (k % 3), k * 7 + (sx > 0 ? 3 : 0),
                time * 0.05 * (k % 2 == 0 ? 1.0 : -1.0));
            // White struts along the top of the wing, running on past their bands into tall white spikes.
            if (k == 0 || k == 2) {
                strut(body, roots[k], tips[k], bulges[k], writhes[k], normalize(sub(tips[k], roots[k])), k == 0 ? 9.0 : 6.0, g);
            }
            // Ice-white shards along the low bands, pointing down and out.
            if (k >= 8) {
                for (int j = 1; j <= 3; j++) {
                    double[] at = sp.at(0.3 + 0.18 * j)[0];
                    WarModel.shard(body, at, v(sx * 0.35, -1.0, 0.1), v(sx, 0, 0), (2.8 + 0.7 * j) * g, 1.8, 0.8, v(0, 0, 1), WHITE, ICE, 0, 3);
                }
            }
        }
        // A second layer between the first, a little behind: the wing is a mass of twisted kagune, not spokes.
        for (int k = 1; k + 1 < n; k += 2) {
            double[] root = add(lerp(roots[k], roots[k + 1], 0.5), v(0, 0, 0.8));
            double[] tip = add(lerp(tips[k], tips[k + 1], 0.5), v(0, 0, 1.6));
            tip = add(root, mul(sub(tip, root), 0.9));
            double[] out = normalize(sub(tip, root));
            double[] across = normalize(v(-out[1] * sx, out[0] * sx, 0.0));
            double[] bulge = add(mul(across, 1.0 + 0.4 * Math.sin(time * 0.13 + k)), v(0, 0, 2.8));
            band(body, root, tip, bulge, mul(writhes[k], -0.8), 1.9 - 0.04 * k, 1.7, k * 13 + 5 + (sx > 0 ? 2 : 0), time * 0.04);
        }
        // The lattice: bands across, from the middle of one to the middle of another, in front of the spokes.
        for (int[] c : CROSSES) {
            double[] from = add(add(lerp(roots[c[0]], tips[c[0]], 0.45), mul(bulges[c[0]], 0.9)), v(0, 0, -0.9));
            double[] to = add(add(lerp(roots[c[1]], tips[c[1]], 0.62), mul(bulges[c[1]], 0.8)), v(0, 0, -0.9));
            double[] out = normalize(sub(to, from));
            double[] bulge = add(mul(normalize(v(-out[1], out[0], 0.0)), 1.6), v(0, 0, -0.6));
            band(body, from, to, bulge, v(0, 0, 0), 1.7, 1.6, c[0] * 11 + c[1] + (sx > 0 ? 5 : 0), -time * 0.04);
        }
    }

    /** How spread the wings are (0..1). */
    static double spread(PeaceMotion.State state) {
        if (state == null) {
            return 0.0;
        }
        double age = state.age();
        return switch (state.move()) {
            // In the air the blades are feathers of the wings: the back kagune opens a little behind them.
            case ASCENT -> 0.3 * clamp01(age / 6.0);
            case DIVE -> 0.1;
            // 1.0.34: the Horizon opens them wide as it rises under its body, and folds them as the blades let it go.
            case HORIZON -> clamp01((age - PeaceRules.HORIZON_UPPER) / 8.0)
                * (1.0 - clamp01((age - (PeaceRules.HORIZON_TICKS - PeaceRules.HORIZON_LET_GO - 20)) / 10.0));
            // 1.0.17: coming over him, the wings open wide as the blades unfurl, and fold again.
            case EMERGE -> FurutaMesh.smooth(clamp01((age - 8.0) / 10.0)) * (1.0 - FurutaMesh.smooth(clamp01((age - 32.0) / 12.0)));
            // 1.0.19: leaving him, the wings flare once as the blades fold, then close.
            case RECEDE -> 0.35 * Math.sin(Math.PI * clamp01(age / 12.0));
            default -> 0.0;
        };
    }

    /**
     * One band of the wing: twisted crimson kagune from {@code root} to {@code tip}, bowed out by {@code bulge}, an S down it by
     * {@code writhe}; 1.0.10: it comes to a point, never a cut end. 1.0.18: {@code crawl} turns its twist, so its fibres crawl round it.
     * Its centre line, for what grows along it.
     */
    static KakujaModel.Spine band(WarModel.Body body, double[] root, double[] tip, double[] bulge, double[] writhe, double width, double twist, int seed,
                                  double crawl) {
        List<double[]> control = List.of(root, add(add(lerp(root, tip, 0.33), mul(bulge, 0.85)), writhe), sub(add(lerp(root, tip, 0.66), bulge), writhe), tip);
        KakujaModel.Spine sp = WarModel.spine(new java.util.ArrayList<>(control), 30, v(0, 0, -1));
        FurutaMesh.tube(new WarModel.SinkAdapter(body), sp, 0.0, 1.0, 30, 8, new FurutaMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                double phase = twist * s * Math.PI * 2.0 + seed + crawl;
                double c = Math.cos(a - phase);
                return (width * (1.0 - 0.55 * s) + 0.12) * point(s) * (0.4 + 0.6 * c * c);
            }

            @Override
            public int colour(double s, double a) {
                return mix(flesh(s, a - crawl, seed), FIBRE, 0.3 * s);
            }
        }, 1.0);
        return sp;
    }

    /** The crimson of the kagune: fibres twisting down it, lighter on its upper side. */
    static int flesh(double s, double a, int seed) {
        double fibre = Math.sin(a * 2.0 + s * 26.0 + seed);
        int c = fibre > 0.35 ? mix(CRIMSON, FIBRE, 0.7) : CRIMSON;
        return mix(c, CRIMSON_L, 0.45 * Math.max(0.0, Math.sin(a)));
    }

    /** 1.0.10: how much of a band's width is left at {@code s} as it comes to its point: whole until {@link #TIP_FROM}, nothing at the end. */
    public static double point(double s) {
        return s <= TIP_FROM ? 1.0 : Math.max(0.0, 1.0 - Math.pow((s - TIP_FROM) / (1.0 - TIP_FROM), 1.6));
    }

    /** A white strut along a band (following its bow and its S), running {@code beyond} pixels past it into a white spike. */
    static void strut(WarModel.Body body, double[] root, double[] tip, double[] bulge, double[] writhe, double[] out, double beyond, double g) {
        double[] lift = v(0, 0, -0.8);
        List<double[]> control = new java.util.ArrayList<>(List.of(add(root, lift), add(add(add(lerp(root, tip, 0.33), mul(bulge, 0.85)), writhe), lift),
            add(sub(add(lerp(root, tip, 0.66), bulge), writhe), lift), add(tip, lift)));
        horn(body, control, 0.75, 0.32);
        WarModel.shard(body, add(tip, lift), add(out, v(0, 0.35, 0.05)), v(0, 1, 0), beyond * g + 0.5, 1.6, 1.0, v(0, 0, 1), WHITE, ICE, 0, 5);
    }

    // ================================================================== 1.0.18: the six cross blades

    /** Each cross blade's points (body pixels at its own size): the long point it strikes with, the side points, the point behind. */
    public static double longPoint(int blade) {
        return 19.0;
    }

    static double sidePoint(int blade) {
        return 10.0;
    }

    public static double backPoint(int blade) {
        return 12.5;
    }

    /** How many steps each arm's line is sampled in, and how many white plates run down it: the same always, so a blade's shapes keep
     *  their count from one moment to the next (drawn between two builds). */
    static final int ARM_POINTS = 24;
    static final int ARM_PLATES = 4;
    /** How far behind its heart (body pixels at its own size) a blade's arm takes hold of it, on the face toward his back. */
    static final double SOCKET = 2.0;
    /** His body, kept clear of the arms: anywhere this far out to either side of his middle, from his feet to over his head, from before
     *  his chest to behind his back (body pixels; the arms' own thickness is added). */
    static final double BODY_HALF = 10.0;
    static final double BODY_TOP = 34.5;
    static final double BODY_FRONT = -4.8;
    static final double BODY_BACK = 4.2;

    /**
     * 1.0.25: how far out the blades are held ({@code grow} grown, doing {@code state}): unfolding with the wings coming over him, all
     * the way while they leave him (they fold and sink on their own), else as grown. What {@link PeaceArms} drives the arms to.
     */
    public static double held(double grow, PeaceMotion.State state) {
        boolean emerging = state != null && state.move() == KingMove.EMERGE;
        boolean receding = state != null && state.move() == KingMove.RECEDE;
        if (receding) {
            return 1.0;
        }
        return emerging ? FurutaMesh.smooth(clamp01((state.age() - KingTransform.PEACE_WINGS_FROM) / 14.0)) : FurutaMesh.smooth(clamp01(grow));
    }

    /** The six blades, each on its arm from his back ({@link PeaceMotion}); 1.0.25: each arm as {@link PeaceArms} has it alive, if given. */
    static void blades(WarModel.Body body, double time, double g, PeaceMotion.State state) {
        double[][][] arms = state == null ? null : state.arms();
        for (int blade = 0; blade < PeaceMotion.BLADES; blade++) {
            PeaceMotion.Tip tip = PeaceMotion.tip(blade, time, g, state);
            if (tip.size() < 0.1 * PeaceMotion.size(blade)) {
                continue; // sunk back into him (1.0.19)
            }
            bladeArm(body, blade, tip, time, g, arms == null || blade >= arms.length ? null : arms[blade]);
            star(body, blade, tip, time);
        }
    }

    /**
     * Which face of blade {@code blade} its arm holds, and how squarely: its face toward his back, scaled down to nothing as his back
     * comes edge on to it (so the hold slides round, never jumps from face to face).
     */
    static double[] hold(int blade, PeaceMotion.Tip tip) {
        double[] n = tip.face();
        double k = Math.max(-1.0, Math.min(1.0, dot(sub(PeaceMotion.root(blade), tip.at()), n) / 6.0));
        return mul(n, k);
    }

    /**
     * 1.0.18: blade {@code blade}'s arm as drawn: its centre line from its root on his back to where it takes hold of the blade, sampled
     * (body pixels), each with its thickness ({x, y, z, radius}). A smooth curve: out of his back to its side, bending (all of the
     * bending in its middle) round to come in behind the blade's face; never inside his body; breathing a little along its length.
     */
    public static double[][] armLine(int blade, PeaceMotion.Tip tip, double time, double g) {
        double[][] pts = armCurve(blade, tip);
        double[] root = PeaceMotion.root(blade);
        double size = tip.size();
        double[] span = sub(pts[pts.length - 1], root);
        double d = Math.sqrt(dot(span, span));
        int n = ARM_POINTS;
        double side = PeaceMotion.right(blade) > 0 ? -1.0 : 1.0;
        // Alive: a slow breath down its length, the middle moving most, the ends held.
        double[] dir = dot(span, span) < 1.0E-9 ? v(0, 1, 0) : normalize(span);
        double[] across = cross(dir, v(0, 1, 0));
        across = dot(across, across) < 1.0E-6 ? v(1, 0, 0) : normalize(across);
        double[] lift = normalize(cross(across, dir));
        double[][] out = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double t = (double)i / n;
            double breath = Math.sin(Math.PI * t) * Math.min(1.3, 0.5 + d * 0.012);
            double[] p = add(pts[i], add(mul(across, breath * Math.sin(time * 0.13 + t * 3.0 + blade)), mul(lift, breath * 0.6 * Math.cos(time * 0.1 + t * 2.5 + blade))));
            if (i > 0 && i < n) {
                p = clearOf(p, side, t, radius(blade, t));
            }
            out[i] = new double[] {p[0], p[1], p[2], radius(blade, t) * Math.max(0.3, g) * Math.min(1.0, size / (0.8 * PeaceMotion.size(blade)))};
        }
        return out;
    }

    /**
     * 1.0.25: the shape blade {@code blade}'s arm would take, still ({@link #armLine} without its breath; what {@link PeaceArms} drives
     * the living arm toward): from its root on his back round to where it takes hold of the blade, kept out of his body, {@link
     * #ARM_POINTS} steps (body pixels).
     */
    public static double[][] armCurve(int blade, PeaceMotion.Tip tip) {
        double[] root = PeaceMotion.root(blade);
        double size = tip.size();
        double[] hold = hold(blade, tip);
        double[] socket = add(tip.at(), mul(hold, SOCKET * size));
        double[] in = normalize(add(mul(hold, 0.8), mul(tip.dir(), -0.6)));
        double[] span = sub(socket, root);
        double d = Math.sqrt(dot(span, span));
        double l1 = Math.max(6.0, Math.min(26.0, d * 0.38));
        double l2 = Math.max(5.0, Math.min(22.0, d * 0.34));
        double[] p1 = add(root, mul(PeaceMotion.rootOut(blade), l1));
        double[] p2 = add(socket, mul(in, l2));
        int n = ARM_POINTS;
        double[][] pts = new double[n + 1][];
        double side = PeaceMotion.right(blade) > 0 ? -1.0 : 1.0;
        for (int i = 0; i <= n; i++) {
            double t = (double)i / n;
            double u = 1.0 - t;
            pts[i] = add(add(mul(root, u * u * u), mul(p1, 3.0 * u * u * t)), add(mul(p2, 3.0 * u * t * t), mul(socket, t * t * t)));
        }
        // Kept out of his body, then smoothed so the push never leaves a kink, and kept out again.
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 1; i < n; i++) {
                pts[i] = clearOf(pts[i], side, (double)i / n, radius(blade, (double)i / n));
            }
            if (pass < 2) {
                double[][] smoothed = pts.clone();
                for (int i = 2; i < n - 1; i++) {
                    smoothed[i] = mul(add(add(pts[i - 1], pts[i + 1]), mul(pts[i], 2.0)), 0.25);
                }
                pts = smoothed;
            }
        }
        return pts;
    }

    /** 1.0.25: an arm's thickness {@code s} along it: as its living line has it there, or (still) its own shape scaled. */
    static double thickness(int blade, double[][] line, boolean alive, double scale, double s) {
        if (!alive) {
            return radius(blade, s) * scale;
        }
        double f = Math.max(0.0, Math.min(1.0, s)) * (line.length - 1);
        int i = Math.min(line.length - 2, (int)Math.floor(f));
        double k = f - i;
        return line[i][3] + (line[i + 1][3] - line[i][3]) * k;
    }

    /** 1.0.21: how much thicker (drawn in) or thinner (stretched out) blade {@code blade}'s arm is, by how far it reaches from its root. */
    static double stretch(int blade, PeaceMotion.Tip tip) {
        double[] span = sub(tip.at(), PeaceMotion.root(blade));
        double length = Math.sqrt(dot(span, span));
        double rest = PeaceMotion.by(blade, 30.0, 38.0, 32.0);
        return Math.max(0.6, Math.min(1.2, Math.sqrt(rest / Math.max(1.0, length))));
    }

    /** An arm's thickness at {@code t} along it (body pixels): a thick root, a muscular middle, narrowing to where it holds its blade. */
    static double radius(int blade, double t) {
        double root = PeaceMotion.by(blade, 2.2, 2.7, 2.4);
        double end = PeaceMotion.by(blade, 1.05, 1.35, 1.15);
        return root + (end - root) * t + 0.35 * Math.sin(Math.PI * Math.min(1.0, t * 1.6));
    }

    /**
     * {@code p} (a point of an arm {@code t} along it, {@code r} thick) kept out of his body: alongside his torso, arms and head (and a
     * little before and behind them) it is pushed out to its own side ({@code side}, +1 his left in body pixels); his back is left to
     * the arms' roots.
     */
    static double[] clearOf(double[] p, double side, double t, double r) {
        if (t < 0.08) {
            return p;
        }
        boolean alongside = p[1] > -1.0 - r && p[1] < BODY_TOP + r && p[2] > BODY_FRONT - r && p[2] < BODY_BACK + r;
        double edge = BODY_HALF + r;
        if (!alongside || Math.abs(p[0]) >= edge) {
            return p;
        }
        // Out the shorter way: past the back if it is nearly there, else out to its side.
        if (p[2] > BODY_BACK + r - 2.5 && t < 0.35) {
            return v(p[0], p[1], BODY_BACK + r);
        }
        double s = Math.abs(p[0]) > 2.0 ? Math.signum(p[0]) : side;
        return v(s * edge, p[1], p[2]);
    }

    /**
     * 1.0.10: blade {@code blade}'s arm: a thick tendril of crimson kagune from his upper back ({@link #armLine}), white plates down it,
     * plate over plate, so the arms that hold the blades read as the weapons they are; it runs on into the blade's heart, and four white
     * claws grip the blade between its points, curling round its heart.
     */
    static void bladeArm(WarModel.Body body, int blade, PeaceMotion.Tip tip, double time, double g, double[][] alive) {
        // 1.0.25: alive ({@link PeaceArms}): its own line, each point with its own thickness (drawn out thin, bunched up thick).
        double[][] line = alive != null ? alive : armLine(blade, tip, time, g);
        java.util.ArrayList<double[]> control = new java.util.ArrayList<>();
        for (double[] q : line) {
            control.add(v(q[0], q[1], q[2]));
        }
        int samples = ARM_POINTS * 2;
        KakujaModel.Spine sp = WarModel.spine(control, samples, v(0, 0, -1));
        int seed = 91 + blade * 17;
        // Thinner with its blade as that sinks back into him (1.0.19), or before it has grown; 1.0.21: thinner stretched out far, its
        // muscle drawn out with it, thicker drawn in.
        double scale = Math.max(0.3, g) * Math.min(1.0, tip.size() / (0.8 * PeaceMotion.size(blade))) * stretch(blade, tip);
        FurutaMesh.tube(new WarModel.SinkAdapter(body), sp, 0.0, 1.0, samples, 9, new FurutaMesh.Surface() {
            @Override
            public double radius(double s, double a) {
                double c = Math.cos(a - (0.8 * s * Math.PI * 2.0 + seed));
                return thickness(blade, line, alive != null, scale, s) * (0.8 + 0.2 * c * c);
            }

            @Override
            public int colour(double s, double a) {
                return mix(flesh(s, a, seed), CRIMSON_L, 0.3);
            }
        }, 1.0);
        for (int i = 0; i < ARM_PLATES; i++) {
            double at = 0.25 + 0.6 * i / (ARM_PLATES - 1.0);
            double[][] f = sp.at(at);
            double r = thickness(blade, line, alive != null, scale, at);
            WarModel.shard(body, add(f[0], mul(f[2], r * 0.6)), add(f[1], mul(f[2], 0.12)), f[2], r * 2.1, r * 1.25, r * 0.45, f[3], WHITE, SILVER, 0, 4, true);
        }
        double size = tip.size();
        if (size < 0.05) {
            return;
        }
        // It runs on into the blade's heart.
        double[] hold = hold(blade, tip);
        double[] centre = tip.at();
        double[] end = line[line.length - 1];
        double[] into = sub(centre, v(end[0], end[1], end[2]));
        double reach = Math.sqrt(dot(into, into));
        double[] d = reach > 1.0E-3 ? mul(into, 1.0 / reach) : tip.dir();
        double r = thickness(blade, line, alive != null, scale, 1.0);
        WarModel.shard(body, v(end[0], end[1], end[2]), d, PeaceMotion.level(d), reach + 0.4, r * 1.6, r * 1.6, cross(d, PeaceMotion.level(d)), CRIMSON, FIBRE,
            0, 3, true);
        // The claws: white, between the blade's points, curling round its heart from the face it is held by.
        double[] n = normalize(tip.face());
        double[] dir = normalize(sub(tip.dir(), mul(n, dot(tip.dir(), n))));
        double[] across = normalize(cross(n, dir));
        double hub = Math.max(0.9, longPoint(blade) * 0.12) * size;
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4.0 + k * Math.PI / 2.0;
            double[] out = add(mul(dir, Math.cos(a)), mul(across, Math.sin(a)));
            double[] base = add(add(centre, mul(hold, 1.4 * size)), mul(out, hub * 0.5));
            WarModel.shard(body, base, add(mul(out, 0.75), mul(hold, -0.65)), mul(out, -1.0), hub * 1.7 + 0.4, 0.7 * size + 0.1, 0.55, cross(n, out), WHITE,
                SILVER, 0, 4);
        }
    }

    /**
     * Blade {@code blade}: a cross blade, a four-pointed star at its heart, its long point along the way it goes (the point it strikes
     * with), the side points across, a shorter point behind; white running to ice at the edges, a darker steel outline just behind so it
     * reads sharp against any sky, a glowing red core down every point and a burning heart, on both faces. Alive: now and then its heart
     * beats, a brighter red running out down the cores.
     */
    static void star(WarModel.Body body, int blade, PeaceMotion.Tip tip, double time) {
        double size = tip.size();
        if (size < 0.05) {
            return;
        }
        double[] n = normalize(tip.face());
        double[] dir = normalize(sub(tip.dir(), mul(n, dot(tip.dir(), n))));
        double[] across = normalize(cross(n, dir));
        double[][] points = {dir, across, mul(dir, -1.0), mul(across, -1.0)};
        // 1.0.36: folded (the lance's cone), the side and back points drawn in flat along the long one: six long points, one spear.
        double drawn = 1.0 - 0.85 * clamp01(tip.fold());
        double[] lengths = {longPoint(blade) * size, sidePoint(blade) * size * drawn, backPoint(blade) * size * drawn, sidePoint(blade) * size * drawn};
        double[] widths = {0.35, 0.47, 0.42, 0.47};
        double[] centre = tip.at();
        double beat = Math.pow(Math.max(0.0, Math.sin(time * 0.09 + blade * 1.3)), 8.0);
        int core = mix(CORE, 0xFFFF9AA2, 0.55 * beat);
        int red = mix(BLADE_RED, CORE, 0.6 * beat);
        // 1.0.34: a real blade, not a sheet: a ridge down each point (its faces beveled off it to the edges), tapering to a keener point.
        double thick = BLADE_THICK * Math.max(0.6, size);
        for (int k = 0; k < 4; k++) {
            double length = lengths[k];
            double[] d = points[k];
            double[] side = normalize(cross(n, d));
            // 1.0.36: one piece (the dark outline copy behind it is gone: thick, it showed as a second blade); its keen edges darker.
            keen(body, centre, d, n, length, length * widths[k], thick, side, WHITE, STEEL_ICE, mix(STEEL_ICE, OUTLINE, 0.55));
            // The red core down the point's ridge, on both faces (standing on the ridge, which falls away toward the point).
            for (int f = -1; f <= 1; f += 2) {
                double[] a0 = add(centre, mul(n, f * (ridge(thick, 0.0) + 0.06)));
                double[] mid = add(add(centre, mul(d, length * 0.24)), mul(n, f * (ridge(thick, 0.24) + 0.06)));
                double[] end = add(add(centre, mul(d, length * 0.8)), mul(n, f * (ridge(thick, 0.8) + 0.06)));
                double[] w = mul(side, length * 0.035);
                body.glow(a0, add(mid, w), end, sub(mid, w), core, red, 0xFF4A0008, red);
            }
        }
        // The heart: a red glow and its white-hot middle, on both faces, on the ridges' crown.
        double[] up = PeaceMotion.level(n);
        double glow = longPoint(blade) * size;
        double crown = ridge(thick, 0.0);
        for (int f = -1; f <= 1; f += 2) {
            WarModel.disc(body, add(centre, mul(n, f * (crown + 0.1))), mul(n, f), up, 0.0, glow * 0.12, glow * 0.12, core, 0xFF5A0010, 16, true);
            WarModel.disc(body, add(centre, mul(n, f * (crown + 0.14))), mul(n, f), up, 0.0, glow * 0.06, glow * 0.06, 0xFFFFFFFF, 0xFFFFD0D4, 12, true);
        }
    }

    /** 1.0.34: how thick a cross blade's points are at their root (body pixels at full size): a ridge, beveled to the edges. */
    static final double BLADE_THICK = 2.4;
    /** 1.0.34: how sharply a point tapers (above 1: a little concave, a keener point). */
    static final double KEEN = 1.18;

    /** 1.0.34: half the ridge's height {@code u} along a point {@code thick} thick at its root. */
    static double ridge(double thick, double u) {
        return thick * 0.5 * Math.max(0.12, Math.pow(Math.max(0.0, 1.0 - u), KEEN));
    }

    /**
     * 1.0.34: one point of a cross blade: from {@code base} along {@code dir}, {@code width} across in its own plane ({@code side}) and
     * {@code thick} through it ({@code up}, its face), a diamond section (the ridge down its middle, its two edges keen), the width and
     * the ridge both drawn in to a sharp point; {@code root} to {@code tip} in colour.
     */
    static void keen(WarModel.Body body, double[] base, double[] dir, double[] up, double length, double width, double thick, double[] side, int root,
                     int tip, int edge) {
        if (length < 0.05) {
            return;
        }
        int stations = 7;
        double[] d = normalize(dir);
        double[] s = normalize(sub(side, mul(d, dot(side, d))));
        double[] nrm = normalize(cross(d, s));
        if (dot(nrm, up) < 0.0) {
            nrm = mul(nrm, -1.0);
        }
        // Curved as it always was (a fifth of its length off toward its face by the point: the crescent hook), the section carried round.
        double[] c1 = add(base, mul(d, length * 0.5));
        double[] c2 = add(add(base, mul(d, length)), mul(nrm, length * 0.22));
        double[][][] ring = new double[stations + 1][][];
        double[][] centre = new double[stations + 1][];
        int[] colour = new int[stations + 1];
        int[] keen = new int[stations + 1];
        for (int i = 0; i <= stations; i++) {
            double u = (double)i / stations;
            double[] p = add(add(mul(base, (1 - u) * (1 - u)), mul(c1, 2 * u * (1 - u))), mul(c2, u * u));
            double[] tng = normalize(add(mul(sub(c1, base), 2 * (1 - u)), mul(sub(c2, c1), 2 * u)));
            double[] ss = normalize(sub(s, mul(tng, dot(s, tng))));
            double[] nn = normalize(cross(tng, ss));
            if (dot(nn, nrm) < 0.0) {
                nn = mul(nn, -1.0);
            }
            double w = width * 0.5 * Math.pow(1.0 - u, KEEN);
            double h = ridge(thick, u);
            ring[i] = new double[][] {add(p, mul(ss, w)), add(p, mul(nn, h)), sub(p, mul(ss, w)), sub(p, mul(nn, h))};
            centre[i] = p;
            colour[i] = mix(root, tip, Math.pow(u, 0.9));
            keen[i] = mix(edge, tip, 0.25 * u);
        }
        for (int i = 0; i < stations; i++) {
            double[] mid = FurutaMesh.lerp(centre[i], centre[i + 1], 0.5);
            for (int j = 0; j < 4; j++) {
                int k = (j + 1) % 4;
                // The two edges (0, 2) darker steel, the ridge (1, 3) the blade's white.
                int a = j % 2 == 0 ? keen[i] : colour[i];
                int b = j % 2 == 0 ? keen[i + 1] : colour[i + 1];
                int c = k % 2 == 0 ? keen[i + 1] : colour[i + 1];
                int e = k % 2 == 0 ? keen[i] : colour[i];
                body.shaded(ring[i][j], ring[i + 1][j], ring[i + 1][k], ring[i][k], mid, a, b, c, e);
            }
        }
    }

    // ================================================================== arms and legs

    static void arm(WarModel.Body body, int sx, double g) {
        double cx = sx * 6.0;
        // Dark under-armour; the hand stays bare.
        WarModel.shell(body, v(cx, 18.2, 0.0), v(2.5, 6.25, 2.5), 8.0, 14, 24, p -> p[1] > 12.4, p -> WarModel.mottle(p, UNDER, UNDER_L, 73 + sx));
        // The shoulder plate: layered white plates sweeping out and down.
        for (int k = 0; k < 3; k++) {
            double[] base = v(sx * (4.6 + 0.6 * k), 24.6 - 0.7 * k, -1.8 + 1.8 * k);
            WarModel.shard(body, base, v(sx * 1.0, -0.5 - 0.15 * k, 0.0), v(0, -1, 0), (5.6 - 0.4 * k) * g + 0.4, 5.4 - 0.6 * k, 1.1, v(0, 0, 1), k == 1 ? RIM : WHITE,
                SILVER, 0, 4, true);
        }
        // The forearm guard, white with silver shading, its plates pointing back toward the elbow.
        WarModel.shell(body, v(cx, 15.0, 0.0), v(2.62, 2.5, 2.62), 8.0, 10, 24, p -> true, p -> mix(pale(p, 75 + sx), SHADOW, clamp01((14.0 - p[1]) * 0.25)));
        for (int k = 0; k < 4; k++) {
            double[] base = v(cx + sx * 2.4, 13.4 + 1.2 * k, -1.5 + 1.0 * k);
            WarModel.shard(body, base, v(sx * 0.6, 1.0, 0.1), v(sx, 0, 0), (2.4 + 0.3 * k) * g + 0.3, 1.2, 0.6, v(0, 0, 1), WHITE, ICE, 0, 3);
        }
    }

    static void leg(WarModel.Body body, int sx, double g) {
        double cx = sx * 1.95;
        WarModel.shell(body, v(cx, 8.0, 0.0), v(2.48, 4.1, 2.48), 8.0, 12, 24, p -> true, p -> WarModel.mottle(p, UNDER, UNDER_L, 77 + sx));
        // The knee plate, and its point.
        WarModel.shell(body, v(cx, 7.2, -0.1), v(2.62, 1.6, 2.62), 8.0, 8, 24, p -> p[2] < 1.0, p -> pale(p, 79 + sx));
        WarModel.shard(body, v(cx, 7.6, -2.7), v(sx * 0.1, 1.0, -0.15), v(0, 0, -1), 2.6 * g + 0.3, 1.8, 0.7, v(1, 0, 0), WHITE, RIM, 0, 3, true);
        // The boot: white, silver below, a jagged cuff.
        WarModel.shell(body, v(cx, 1.9, -0.1), v(2.64, 1.95, 2.78), 8.0, 10, 24, p -> true, p -> mix(pale(p, 81 + sx), SHADOW, clamp01((1.6 - p[1]) * 0.45)));
        for (int k = 0; k < 5; k++) {
            double a = Math.toRadians(-60.0 + 30.0 * k);
            double[] base = v(cx + Math.sin(a) * 2.4, 3.7, -Math.cos(a) * 2.4);
            WarModel.shard(body, base, v(Math.sin(a) * 0.3, 1.0, -Math.cos(a) * 0.3), v(Math.sin(a), 0, -Math.cos(a)), 1.6 * g + 0.3, 1.4, 0.5,
                v(Math.cos(a), 0, Math.sin(a)), WHITE, ICE, 0, 2, true);
        }
    }
}

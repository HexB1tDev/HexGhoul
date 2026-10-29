package com.hex.kagune.control;

import com.hex.kagune.control.ayato.AyatoBody;
import com.hex.kagune.control.ayato.CrystalSpike;
import com.hex.kagune.control.ayato.KakujaMotion;
import com.hex.kagune.control.ayato.FireTendril;
import com.hex.kagune.control.ayato.KakujaRig;
import com.hex.kagune.control.ayato.UkakuCrystal;
import com.hex.kagune.control.ayato.UkakuFlame;
import com.hex.kagune.control.ayato.AyatoRules;
import com.hex.kagune.control.ayato.UkakuModel;
import com.hex.kagune.control.ayato.UkakuWing;
import com.hex.kagune.control.ayato.WingForm;
import com.hex.kagune.control.ayato.WingRig;
import com.hex.kagune.control.ayato.WingVortex;
import java.util.ArrayList;
import java.util.List;

/** Dependency-free tests for the Ayato kit: wing geometry, the 3D rig, wing forms, combos, body poses and the balance rules. */
public final class AyatoTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    /** Total quad area of the right twin wing (quads right of the spine), aiming straight ahead by {@code aim}. */
    private static double rightWingArea(int mastery, float aim) {
        double total = 0.0;
        for (WingRig.Quad quad : WingRig.build(new WingRig.Input(WingForm.TWIN, mastery, 1.0F, 1.0, 0.0F, 0.0F, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, 1.0F, 1.0F, false, aim, 0.0F, WingRig.FORWARD, 0.0F, 0.0F))) {
            float[] v = quad.xyz();
            if (v[0] + v[3] + v[6] + v[9] <= 0.0F) {
                continue;
            }
            double[] p = {v[6] - v[0], v[7] - v[1], v[8] - v[2]};
            double[] q = {v[9] - v[3], v[10] - v[4], v[11] - v[5]};
            double x = p[1] * q[2] - p[2] * q[1];
            double y = p[2] * q[0] - p[0] * q[2];
            double z = p[0] * q[1] - p[1] * q[0];
            total += 0.5 * Math.sqrt(x * x + y * y + z * z);
        }
        return total;
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        wingGeometry();
        oneWingGeometry();
        rig();
        model();
        forms();
        rules();
        combos();
        crystalMode();
        sweep();
        spikes();
        vortex();
        gatling();
        body();
        volley();
        kakuja();
        mutations();
        unlocks();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Ayato tests: " + passed + " checks passed");
    }

    private static boolean wellFormed(UkakuWing.Poly poly) {
        if (poly.corners() < 3 || poly.corners() > 4 || poly.x().length != poly.y().length) {
            return false;
        }
        for (int i = 0; i < poly.corners(); i++) {
            if (!Float.isFinite(poly.x()[i]) || !Float.isFinite(poly.y()[i])) {
                return false;
            }
        }
        return poly.alpha() > 0.0F && poly.alpha() <= 1.0F && poly.layer() >= 0 && poly.layer() < UkakuWing.LAYERS
            && poly.rgb() >= 0 && poly.rgb() <= 0xFFFFFF;
    }

    private static void wingGeometry() {
        for (int tongues : new int[] {7, 9}) {
            float[] envelope = UkakuWing.envelope(tongues);
            check(envelope[0] > -0.6F && envelope[2] < 1.9F && envelope[1] > -0.8F && envelope[3] < 1.9F,
                "the wing stays a wing-sized shape: " + java.util.Arrays.toString(envelope));
            check(envelope[3] > 0.9F, "the wing rises well above the shoulder");
            check(envelope[2] > 0.9F, "the wing spreads well out to the side");
            for (double time = 0.0; time < 30.0; time += 0.71) {
                for (float crystal : new float[] {0.0F, 0.3F, 0.7F, 1.0F}) {
                    for (float deploy : new float[] {0.1F, 0.5F, 1.0F}) {
                        for (float detail : new float[] {0.0F, 0.5F, 1.0F}) {
                            List<UkakuWing.Poly> polys = UkakuWing.wing(time, crystal, deploy, tongues, detail);
                            check(!polys.isEmpty(), "a deployed wing draws something");
                            boolean allGood = true;
                            for (UkakuWing.Poly poly : polys) {
                                allGood &= wellFormed(poly);
                            }
                            check(allGood, "every wing polygon is well formed (t=" + time + " c=" + crystal + ")");
                            float[] b = UkakuWing.bounds(polys);
                            check(b[0] >= envelope[0] && b[2] <= envelope[2] && b[1] >= envelope[1] && b[3] <= envelope[3],
                                "every pose stays inside the fitting envelope");
                        }
                    }
                }
            }
        }
        check(UkakuWing.wing(1.0, 0.0F, 0.0F, 7, 1.0F).isEmpty(), "an undeployed wing draws nothing");
        check(sameShape(UkakuWing.wing(1.0, 1.0F, 1.0F, 7, 1.0F), UkakuWing.wing(9.0, 1.0F, 1.0F, 7, 1.0F)),
            "a crystallized wing holds perfectly still");
        check(!sameShape(UkakuWing.wing(1.0, 0.0F, 1.0F, 7, 1.0F), UkakuWing.wing(1.4, 0.0F, 1.0F, 7, 1.0F)), "gaseous flames flicker");
        check(sameShape(UkakuWing.wing(2.5, 0.0F, 1.0F, 7, 1.0F), UkakuWing.wing(2.5, 0.0F, 1.0F, 7, 1.0F)), "the wing is deterministic");
        check(UkakuWing.wing(1.0, 0.0F, 1.0F, 9, 1.0F).size() > UkakuWing.wing(1.0, 0.0F, 1.0F, 7, 1.0F).size(), ":re wings are fuller");
        check(UkakuWing.wing(1.0, 0.0F, 1.0F, 7, 0.0F).size() < UkakuWing.wing(1.0, 0.0F, 1.0F, 7, 1.0F).size(),
            "lower detail draws fewer polygons");
        float[] small = UkakuWing.bounds(UkakuWing.wing(1.0, 0.0F, 0.3F, 7, 1.0F));
        float[] full = UkakuWing.bounds(UkakuWing.wing(1.0, 0.0F, 1.0F, 7, 1.0F));
        check(small[2] < full[2] && small[3] < full[3], "deploying grows the wing out of the shoulder blade");
        // Colours from the anime: dark flames, a molten core and a pink-white glow while gaseous; a starry dark crystal with red edges.
        check(hasColour(UkakuWing.wing(1.0, 0.0F, 1.0F, 7, 1.0F), UkakuWing.FLAME_DARK)
            && hasColour(UkakuWing.wing(1.0, 0.0F, 1.0F, 7, 1.0F), UkakuWing.MOLTEN_HOT)
            && hasColour(UkakuWing.wing(1.0, 0.0F, 1.0F, 7, 1.0F), UkakuWing.AURA), "the gaseous wing has the anime's flames, core and glow");
        check(hasColour(UkakuWing.wing(1.0, 1.0F, 1.0F, 7, 1.0F), UkakuWing.CRYSTAL_DARK)
            && hasColour(UkakuWing.wing(1.0, 1.0F, 1.0F, 7, 1.0F), UkakuWing.CRYSTAL_RIM)
            && hasColour(UkakuWing.wing(1.0, 1.0F, 1.0F, 7, 1.0F), UkakuWing.STAR), "the crystallized wing is dark, starry and pink-rimmed");
        check(!hasColour(UkakuWing.wing(1.0, 1.0F, 1.0F, 7, 1.0F), UkakuWing.FLAME_DARK), "fully crystallized means no flames left");
    }

    /** True if some polygon is within a small RGB distance of {@code rgb} (gradients never hit the exact end colour). */
    private static boolean hasColour(List<UkakuWing.Poly> polys, int rgb) {
        for (UkakuWing.Poly poly : polys) {
            int dr = ((poly.rgb() >> 16) & 0xFF) - ((rgb >> 16) & 0xFF);
            int dg = ((poly.rgb() >> 8) & 0xFF) - ((rgb >> 8) & 0xFF);
            int db = (poly.rgb() & 0xFF) - (rgb & 0xFF);
            if (dr * dr + dg * dg + db * db < 24 * 24) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameShape(List<UkakuWing.Poly> a, List<UkakuWing.Poly> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!java.util.Arrays.equals(a.get(i).x(), b.get(i).x()) || !java.util.Arrays.equals(a.get(i).y(), b.get(i).y())
                || a.get(i).rgb() != b.get(i).rgb()) {
                return false;
            }
        }
        return true;
    }

    private static void oneWingGeometry() {
        for (float deploy : new float[] {0.05F, 0.4F, 1.0F}) {
            for (float detail : new float[] {0.0F, 1.0F}) {
                List<UkakuWing.Poly> polys = UkakuWing.oneWing(3.0, deploy, detail);
                check(!polys.isEmpty(), "the One-Winged Kagune draws something");
                boolean allGood = true;
                for (UkakuWing.Poly poly : polys) {
                    allGood &= wellFormed(poly);
                }
                check(allGood, "every One-Wing polygon is well formed");
                float[] b = UkakuWing.bounds(polys);
                check(b[0] > -0.6F && b[2] < 1.6F && b[1] > -0.3F && b[3] < 1.5F, "the crescent stays crescent-sized: " + java.util.Arrays.toString(b));
            }
        }
        float[] b = UkakuWing.bounds(UkakuWing.oneWing(0.0, 1.0F, 1.0F));
        check(b[3] > 0.85F && b[2] > 1.1F, "the crescent rises high and reaches far forward (the :re scythe)");
        check(UkakuWing.oneWing(0.0, 0.0F, 1.0F).isEmpty(), "an undeployed One-Wing draws nothing");
        check(sameShape(UkakuWing.oneWing(1.0, 1.0F, 1.0F), UkakuWing.oneWing(7.0, 1.0F, 1.0F)), "the crystallized crescent holds still");
        check(hasColour(UkakuWing.oneWing(1.0, 1.0F, 1.0F), UkakuWing.BLADE_INCLUSION)
            && hasColour(UkakuWing.oneWing(1.0, 1.0F, 1.0F), UkakuWing.BLADE_EDGE), "faceted red crystal with dark inclusions and bright edges");
    }

    private static WingRig.Input input(WingForm form, int mastery, float crystal, WingRig.Motion motion, float progress,
                                       float volley) {
        return new WingRig.Input(form, mastery, 1.0F, 2.0, crystal, crystal, motion, progress, 0.4F, volley, 0.0F, false, 1.0F, 1.0F,
            false);
    }

    private static float[] extent(List<WingRig.Quad> quads) {
        float[] e = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (WingRig.Quad quad : quads) {
            for (int c = 0; c < 4; c++) {
                for (int axis = 0; axis < 3; axis++) {
                    float v = quad.xyz()[c * 3 + axis];
                    e[axis] = Math.min(e[axis], v);
                    e[axis + 3] = Math.max(e[axis + 3], v);
                }
            }
        }
        return e;
    }

    private static void rig() {
        for (WingForm form : WingForm.values()) {
            for (int mastery : new int[] {0, 300, 800, 1000}) {
                for (WingRig.Motion motion : WingRig.Motion.values()) {
                    for (float progress : new float[] {0.0F, 0.3F, 0.45F, 0.8F, 1.0F}) {
                        List<WingRig.Quad> quads = WingRig.build(input(form, mastery, 0.5F, motion, progress, 0.0F));
                        boolean finite = true;
                        boolean unitNormals = true;
                        for (WingRig.Quad quad : quads) {
                            for (float v : quad.xyz()) {
                                finite &= Float.isFinite(v);
                            }
                            double n = Math.sqrt(quad.nx() * quad.nx() + quad.ny() * quad.ny() + quad.nz() * quad.nz());
                            unitNormals &= Math.abs(n - 1.0) < 1.0E-3;
                        }
                        check(!quads.isEmpty() && finite && unitNormals, "rig output is finite with unit normals: " + form + " " + motion);
                        boolean colours = true;
                        for (WingRig.Quad quad : quads) {
                            colours &= quad.argb().length == 4 && quad.xyz().length == 12;
                        }
                        check(colours, "every quad has four corners and four colours");
                        float[] e = extent(quads);
                        // A low swing may graze the ground under the feet, never sink into it.
                        check(e[1] > -0.15F && e[4] < 7.5F && Math.abs(e[0]) < 7.0F && Math.abs(e[3]) < 7.0F && Math.abs(e[2]) < 7.0F
                            && Math.abs(e[5]) < 7.0F, "the kagune stays near its host: " + form + " " + motion + " " + java.util.Arrays.toString(e));
                    }
                }
            }
        }
        check(WingRig.build(new WingRig.Input(WingForm.TWIN, 0, 0.0F, 1.0, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1, false)).isEmpty(),
            "a retracted kagune draws nothing");
        check(WingRig.build(new WingRig.Input(WingForm.TWIN, 0, 1.0F, 1.0, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 0, false)).isEmpty(),
            "a fully transparent kagune draws nothing");
        // Twin wings: one per shoulder blade, mirrored when they hold still (crystallized).
        float[] twin = extent(WingRig.build(input(WingForm.TWIN, 300, 1.0F, WingRig.Motion.NONE, 0, 0)));
        check(twin[0] < -1.0F && twin[3] > 1.0F && Math.abs(twin[0] + twin[3]) < 0.05F, "twin wings spread to both sides symmetrically");
        check(twin[4] > WingRig.ROOT_Y + 1.2F, "the wings rise well above the shoulders");
        check(twin[5] < 0.3F, "at rest the wings stay behind the body");
        // Rabbit Guise: only the left wing (Touka's side).
        List<WingRig.Quad> solid = new ArrayList<>();
        for (WingRig.Quad quad : WingRig.build(input(WingForm.RABBIT, 300, 1.0F, WingRig.Motion.NONE, 0, 0))) {
            if (quad.alpha() >= 0.5F) {
                solid.add(quad); // the soft glow may drift across the spine; the wing itself may not
            }
        }
        float[] rabbit = extent(solid);
        check(rabbit[3] < 0.45F && rabbit[0] < -1.0F, "Rabbit Guise keeps only the left wing (no right wing reaching out)");
        // A right-wing slash swings the right wing in front of the body at the impact.
        float[] slash = extent(WingRig.build(input(WingForm.TWIN, 300, 1.0F, WingRig.Motion.SLASH_RIGHT, 0.6F, 0)));
        check(slash[5] > 1.0F, "a wing slash reaches in front of the body");
        // The One-Winged Kagune rises from the left shoulder blade high above the head.
        float[] one = extent(WingRig.build(input(WingForm.ONE_WING, 800, 1.0F, WingRig.Motion.NONE, 0, 0)));
        check(one[4] > 3.5F, "the One-Winged Kagune towers over its host");
        float reach = 0.0F;
        for (float progress = 0.25F; progress <= 0.5F; progress += 0.01F) {
            reach = Math.max(reach, extent(WingRig.build(input(WingForm.ONE_WING, 800, 1.0F, WingRig.Motion.ONE_BEHEAD, progress, 0)))[5]);
        }
        check(reach > 2.0F, "during the swing, the beheading cut sweeps the blade far in front (" + reach + ")");
        // An unlocked-too-early form falls back to the twin wings.
        float[] early = extent(WingRig.build(input(WingForm.ONE_WING, 100, 1.0F, WingRig.Motion.NONE, 0, 0)));
        check(early[3] > 1.0F, "a One-Wing form below mastery 750 shows the twin wings instead");
        // The wings grow: two small wings at 100, full size at 500.
        float[] small = extent(WingRig.build(input(WingForm.TWIN, 100, 1.0F, WingRig.Motion.NONE, 0, 0)));
        float[] full = extent(WingRig.build(input(WingForm.TWIN, 500, 1.0F, WingRig.Motion.NONE, 0, 0)));
        check((full[3] - full[0]) > 1.8F * (small[3] - small[0]), "full-size wings are far larger than the first small ones");
        float[] single = extent(WingRig.build(input(WingForm.TWIN, 0, 1.0F, WingRig.Motion.NONE, 0, 0)));
        check(single[3] < 0.45F && single[0] < -0.9F, "a new Ayato has one small wing (the left)");
    }

    private static void model() {
        // Each state part has its own tests (UkakuFlameTests, UkakuCrystalTests, UkakuOneWingTests);
        // these hold for the kagune as a whole.
        check(UkakuModel.primaries(false) >= 3 && UkakuModel.primaries(true) >= UkakuModel.primaries(false), "every wing fires from several feathers");
        WingRig.Input still = input(WingForm.TWIN, 800, 1.0F, WingRig.Motion.NONE, 0, 0);
        check(sameQuads(WingRig.build(still), WingRig.build(still)), "the same frame always builds the same quads");
        List<WingRig.Quad> gasA = WingRig.build(new WingRig.Input(WingForm.TWIN, 0, 1.0F, 1.0, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false,
            1, 1, false));
        List<WingRig.Quad> gasB = WingRig.build(new WingRig.Input(WingForm.TWIN, 0, 1.0F, 1.4, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false,
            1, 1, false));
        check(!sameQuads(gasA, gasB), "gaseous wings flicker");
        int most = 0;
        for (WingForm form : WingForm.values()) {
            for (int mastery : new int[] {0, 300, 800, 1000}) {
                for (float crystal : new float[] {0.0F, 1.0F}) {
                    most = Math.max(most, WingRig.build(new WingRig.Input(form, mastery, 1.0F, 1.0, crystal, crystal, WingRig.Motion.NONE, 0, 0.4F, 1.0F, 1.0F, false, 1, 1, false)).size());
                }
            }
        }
        check(most < 3200, "a kagune stays within its quad budget (" + most + ")");
        int far = 0;
        for (float crystal : new float[] {0.0F, 1.0F}) {
            far = Math.max(far, WingRig.build(new WingRig.Input(WingForm.TWIN, 1000, 1.0F, 1.0, crystal, crystal, WingRig.Motion.NONE, 0, 0.4F, 0, 0,
                false, 0.3F, 1, false)).size());
        }
        check(far * 2 < most, "distant players are drawn with far fewer quads");
        // A half-crystallized wing (mid-transition) stays within twice the budget: both states are drawn for a moment.
        int mid = WingRig.build(new WingRig.Input(WingForm.TWIN, 1000, 1.0F, 1.0, 0.5F, 0.5F, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1,
            false)).size();
        check(mid < 2 * 3200, "crystallizing never doubles past the budget (" + mid + ")");
        // Crystallized wings grow up and out of the shoulder blades: nothing hangs down toward the waist or reaches into the body
        // (the roots touch the back; nothing goes deeper than 2.5 cm in).
        for (int mastery : new int[] {0, 300, 800, 1000}) {
            for (float volley : new float[] {0.0F, 1.0F}) {
                float lowest = Float.MAX_VALUE;
                boolean clear = true;
                for (WingRig.Quad quad : WingRig.build(new WingRig.Input(WingForm.TWIN, mastery, 1.0F, 1.0, 1.0F, 1.0F, WingRig.Motion.NONE, 0, 0.4F,
                    volley, volley, false, 1, 1, false))) {
                    float[] v = quad.xyz();
                    for (int k = 0; k < 4; k++) {
                        lowest = Math.min(lowest, v[k * 3 + 1]);
                        clear &= !(v[k * 3 + 2] > -0.1F && Math.abs(v[k * 3]) < 0.3F && v[k * 3 + 1] < 1.5F);
                    }
                }
                check(lowest > WingRig.ROOT_Y - 0.2F, "crystallized wings never hang down toward the waist (" + mastery + ": " + lowest + ")");
                check(clear, "crystallized wings never reach into the body (" + mastery + ")");
            }
        }
        List<WingRig.Quad> dissolving = WingRig.build(new WingRig.Input(WingForm.TWIN, 300, 0.5F, 1.0, 1, 1, WingRig.Motion.NONE, 0, 0.4F, 0, 0,
            false, 1, 1, true));
        List<WingRig.Quad> growing = WingRig.build(new WingRig.Input(WingForm.TWIN, 300, 0.5F, 1.0, 1, 1, WingRig.Motion.NONE, 0, 0.4F, 0, 0,
            false, 1, 1, false));
        check(!dissolving.isEmpty() && !growing.isEmpty() && !sameQuads(dissolving, growing), "dissolving looks different from emerging");
        for (WingForm form : WingForm.values()) {
            for (float deploy : new float[] {0.0F, 0.001F}) {
                check(WingRig.build(new WingRig.Input(form, 1000, deploy, 1.0, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1, false)).isEmpty(),
                    "a kagune that is not out draws nothing");
            }
        }
    }

    private static boolean sameQuads(List<WingRig.Quad> a, List<WingRig.Quad> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!java.util.Arrays.equals(a.get(i).xyz(), b.get(i).xyz()) || !java.util.Arrays.equals(a.get(i).argb(), b.get(i).argb())) {
                return false;
            }
        }
        return true;
    }

    private static void forms() {
        check(WingForm.fromId("twin") == WingForm.TWIN && WingForm.fromId("rabbit") == WingForm.RABBIT
            && WingForm.fromId("one_wing") == WingForm.ONE_WING && WingForm.fromId("kakuja") == WingForm.KAKUJA, "stored form ids never change");
        check(WingForm.fromId(null) == WingForm.TWIN && WingForm.fromId("second_kakuja") == WingForm.TWIN, "unknown forms fall back to the twin wings");
        check(WingForm.byOrdinal(-3) == WingForm.TWIN && WingForm.byOrdinal(17) == WingForm.TWIN, "bad ordinals are safe");
        check(WingForm.TWIN.usable(0) == WingForm.RABBIT && WingForm.RABBIT.next(0) == WingForm.RABBIT && WingForm.TWIN.next(0) == WingForm.RABBIT,
            "a new Ayato has only the one wing");
        check(WingForm.RABBIT.displayName(0).equals("One Wing") && WingForm.RABBIT.displayName(100).equals("Rabbit Guise"), "the one wing is named for what it is");
        check(WingForm.TWIN.next(100) == WingForm.RABBIT && WingForm.RABBIT.next(100) == WingForm.TWIN, "the twin wings come at 100 (Rabbit Guise stays)");
        // 5.0.16: a client still holding the one wing the server sent below 100 shows the twin wings as soon as his mastery reaches
        // them (set by a command, or grown), and a kakuja or the One-Winged Kagune shows as before.
        boolean second = true;
        for (int mastery = 0; mastery <= 1000; mastery++) {
            second &= WingForm.RABBIT.shown(mastery).rightWing() == (mastery >= WingForm.TWIN.requiredMastery())
                && WingForm.TWIN.shown(mastery) == WingForm.RABBIT.shown(mastery);
        }
        check(second, "the second wing shows from mastery 100, whatever wing form was sent before");
        check(WingForm.RABBIT.shown(99) == WingForm.RABBIT && WingForm.RABBIT.shown(100) == WingForm.TWIN && WingForm.RABBIT.shown(1000) == WingForm.TWIN,
            "a stale one wing is the twin wings at 100 and at 1000");
        check(WingForm.KAKUJA.shown(800) == WingForm.KAKUJA && WingForm.FINAL_KAKUJA.shown(1000) == WingForm.FINAL_KAKUJA
            && WingForm.FINAL_KAKUJA.shown(800) == WingForm.KAKUJA && WingForm.ONE_WING.shown(800) == WingForm.ONE_WING
            && WingForm.KAKUJA.shown(500) == WingForm.TWIN, "the kakuja and the One-Winged Kagune show as they are usable");
        java.util.Map<com.hex.kagune.control.AbilityGuide.Key, String> keys = new java.util.EnumMap<>(com.hex.kagune.control.AbilityGuide.Key.class);
        for (com.hex.kagune.control.AbilityGuide.Key key : com.hex.kagune.control.AbilityGuide.Key.values()) {
            keys.put(key, key.name());
        }
        for (int mastery : new int[] {0, 99, 100, 1000}) {
            com.hex.kagune.control.AbilityGuide.Panel panel = com.hex.kagune.control.AbilityGuide.panel(new com.hex.kagune.control.AbilityGuide.Input(
                com.hex.kagune.control.KagunePath.AYATO, com.hex.kagune.control.AbilityGuide.State.OUT, mastery,
                com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED, WingForm.RABBIT.shown(mastery), false, keys));
            check(panel.title().equals(mastery < 100 ? "ONE WING" : "TWIN WINGS"), "the abilities board names the wings at " + mastery + ": " + panel.title());
        }
        check(WingForm.KAKUJA.usable(749) == WingForm.TWIN && WingForm.KAKUJA.usable(750) == WingForm.KAKUJA
            && WingForm.KAKUJA.requiredMastery() == com.hex.kagune.control.AyatoStage.KAKUJA_I.requiredMastery(),
            "the kakuja awakens at 750, the stage table's Kakuja I (and the original's Partial Kakuja)");
        check(!com.hex.kagune.control.AyatoStage.KAKUJA_I.comingSoon() && !com.hex.kagune.control.AyatoStage.KAKUJA_II.comingSoon(),
            "both kakuja are built");
        check(WingForm.FINAL_KAKUJA.requiredMastery() == com.hex.kagune.control.AyatoStage.KAKUJA_II.requiredMastery()
            && WingForm.FINAL_KAKUJA.usable(999) == WingForm.KAKUJA && WingForm.FINAL_KAKUJA.usable(1000) == WingForm.FINAL_KAKUJA
            && WingForm.FINAL_KAKUJA.usable(700) == WingForm.TWIN, "the final kakuja at 1000; below it the first, below that the wings");
        check(WingForm.KAKUJA.kakuja() && WingForm.FINAL_KAKUJA.kakuja() && WingForm.FINAL_KAKUJA.finalKakuja() && !WingForm.KAKUJA.finalKakuja()
            && !WingForm.FINAL_KAKUJA.wings() && !WingForm.TWIN.kakuja() && !WingForm.ONE_WING.kakuja(), "either kakuja is a kakuja, only one is final");
        check(WingForm.byOrdinal(WingForm.FINAL_KAKUJA.ordinal()) == WingForm.FINAL_KAKUJA && WingForm.KAKUJA.ordinal() == 2 && WingForm.ONE_WING.ordinal() == 3
            && WingForm.fromId("final_kakuja") == WingForm.FINAL_KAKUJA, "the old forms keep their places in the state packet");
        check(com.hex.kagune.control.ayato.AyatoRules.style(1000, WingForm.FINAL_KAKUJA, false, false) == com.hex.kagune.control.ComboStyle.AYATO_FINAL_KAKUJA
            && com.hex.kagune.control.ayato.AyatoRules.style(1000, WingForm.FINAL_KAKUJA, false, true) == com.hex.kagune.control.ComboStyle.AYATO_FINAL_KAKUJA
            && com.hex.kagune.control.ayato.AyatoRules.style(1000, WingForm.KAKUJA, false, false) == com.hex.kagune.control.ComboStyle.AYATO_KAKUJA
            && com.hex.kagune.control.ayato.AyatoRules.style(999, WingForm.FINAL_KAKUJA, false, false) == com.hex.kagune.control.ComboStyle.AYATO_KAKUJA,
            "the final kakuja fights with its own kit (needles and fire); below 1000 it falls back to the first kakuja's");
        kakujaKey();
        check(WingForm.TWIN.wings() && WingForm.RABBIT.wings() && !WingForm.KAKUJA.wings() && !WingForm.ONE_WING.wings() && !WingForm.KAKUJA.rightWing(),
            "only the Ukaku wings are wings (the kakuja is tentacles, the One-Wing a blade)");
        check(!com.hex.kagune.control.ayato.AyatoRules.crystalModeAllowed(800, WingForm.KAKUJA)
            && com.hex.kagune.control.ayato.AyatoRules.crystalModeAllowed(800, WingForm.TWIN), "no crystal mode in the kakuja");
        check(WingForm.ONE_WING.usable(500) == WingForm.TWIN && WingForm.TWIN.usable(99) == WingForm.RABBIT && WingForm.ONE_WING.usable(50) == WingForm.RABBIT,
            "locked forms fall back (to the twin wings, or the one wing before them)");
        check(WingForm.TWIN.rightWing() && !WingForm.RABBIT.rightWing() && !WingForm.ONE_WING.rightWing(), "only the twin wings use the right side");
    }

    private static void rules() {
        float previous = 0.0F;
        for (int mastery = 0; mastery <= 1000; mastery += 50) {
            float damage = AyatoRules.meleeDamage(mastery);
            check(damage >= previous, "close-range damage never drops with mastery");
            previous = damage;
        }
        check(Math.abs(AyatoRules.meleeDamage(0) - 6.0F * com.hex.kagune.control.DamageBalance.scale(0)) < 1.0E-4F,
            "a new Ayato hits like a new Kaneki (both brought down the same way)");
        // 3.0.3 balance: the sustained M1 at mastery 1000 lands near a Sharpness V sword (about 18 a second), not three times it.
        double flame = AyatoRules.meleeDamage(1000) * ComboStyle.AYATO_FIRE.stage(0).damageMul() * 20.0 / AyatoRules.flameCadence(1000);
        double needles = AyatoRules.meleeDamage(1000) * ComboStyle.AYATO_KAKUJA.stage(0).damageMul() * 2 * AyatoRules.VOLLEY_NEEDLES * 20.0
            / AyatoRules.kakujaCadence(1000);
        double gatling = AyatoRules.gatlingDamage(1000) * AyatoRules.GATLING_FULL_RATE * 20.0;
        check(flame > 12.0 && flame < 26.0 && needles > 12.0 && needles < 26.0 && gatling > 8.0 && gatling < 26.0,
            "sustained damage at 1000: flame " + flame + ", needles " + needles + ", Gatling " + gatling);
        check(AyatoRules.rcCostMultiplier(0) == 1.6F && Math.abs(AyatoRules.rcCostMultiplier(1000) - 1.0F) < 1.0E-5F,
            "low RC supply early, parity with Kaneki at full mastery");
        check(AyatoRules.rcCostMultiplier(500) < AyatoRules.rcCostMultiplier(100), "mastery reduces RC drain");
        check(AyatoRules.wingLength(500, WingForm.TWIN) >= 2.5F * AyatoRules.wingLength(100, WingForm.TWIN), "the wings grow to full size");
        check(AyatoRules.wingLength(500, WingForm.TWIN) == AyatoRules.wingLength(1000, WingForm.TWIN), "full size is reached at 500");
        check(AyatoRules.wingLength(0, WingForm.TWIN) <= AyatoRules.wingLength(100, WingForm.TWIN), "the one wing is small");
        for (int mastery = 0; mastery < 1000; mastery += 25) {
            check(AyatoRules.wingLength(mastery + 25, WingForm.TWIN) >= AyatoRules.wingLength(mastery, WingForm.TWIN), "wings never shrink");
        }
        check(AyatoRules.speedAmplifier(0) == 0 && AyatoRules.speedAmplifier(450) == 1 && AyatoRules.jumpAmplifier(750) == 1, "Ukaku speed grows");
        check(AyatoRules.reach(1000, WingForm.ONE_WING) > AyatoRules.reach(1000, WingForm.TWIN) - 1.0, "the One-Wing reaches far");
    }

    private static final ComboStyle[] AYATO_STYLES = {ComboStyle.AYATO_KICKS, ComboStyle.AYATO_CRYSTAL_KICKS, ComboStyle.AYATO_BLADES,
        ComboStyle.AYATO_RABBIT, ComboStyle.AYATO_ONE_WING, ComboStyle.AYATO_RABBIT_BLADES, ComboStyle.AYATO_FIRE, ComboStyle.AYATO_ENDERS,
        ComboStyle.AYATO_CRYSTAL, ComboStyle.AYATO_CRYSTAL_ENDERS, ComboStyle.AYATO_KAKUJA, ComboStyle.AYATO_KAKUJA_ENDERS,
        ComboStyle.AYATO_RUSHING_DEATH, ComboStyle.AYATO_FINAL_KAKUJA, ComboStyle.AYATO_FINAL_ENDERS, ComboStyle.AYATO_STEEL_BLADE};

    private static void combos() {
        // M1 is the fire shots; the One-Winged Kagune stays a blade.
        check(AyatoRules.style(0, WingForm.TWIN, false) == ComboStyle.AYATO_FIRE && AyatoRules.style(99, WingForm.RABBIT, true)
            == ComboStyle.AYATO_FIRE && AyatoRules.style(1000, WingForm.TWIN, false) == ComboStyle.AYATO_FIRE, "the M1 is the fire shots");
        check(AyatoRules.style(750, WingForm.ONE_WING, true) == ComboStyle.AYATO_ONE_WING
            && AyatoRules.style(500, WingForm.ONE_WING, false) == ComboStyle.AYATO_FIRE, "the One-Wing is a blade, a locked one fires");
        // The chain: five shots, right wing and hand, then left, alternating.
        check(ComboStyle.AYATO_FIRE.stageCount() == AyatoRules.CHAIN_SHOTS && AyatoRules.CHAIN_SHOTS == 5, "five shots to a chain");
        for (int index = 0; index < 5; index++) {
            ComboStage shot = ComboStyle.AYATO_FIRE.stage(index);
            boolean right = index % 2 == 0;
            check(shot.pattern() == StrikePattern.FLAME_SHOT && shot.usesLimb(right ? 0 : 1) && shot.usesLimb(right ? 10 : 11)
                && !shot.usesLimb(right ? 1 : 0) && !shot.usesLimb(right ? 11 : 10), "shot " + index + " is the " + (right ? "right" : "left") + " wing and hand");
        }
        // The enders, by the shots before the right click, gated by mastery.
        StrikePattern[] enders = {StrikePattern.RABBIT_HOP, StrikePattern.FLARE, StrikePattern.RISING_FLAME, StrikePattern.FIRE_FAN,
            StrikePattern.RABBIT_DASH, StrikePattern.BLACK_RABBIT};
        check(ComboStyle.AYATO_ENDERS.stageCount() == enders.length, "one ender for each count of shots, none to five");
        for (int index = 0; index < enders.length; index++) {
            check(ComboStyle.AYATO_ENDERS.stage(index).pattern() == enders[index] && enders[index].ender() && enders[index].flame(),
                "ender " + index + " is " + enders[index]);
        }
        check(!StrikePattern.FLAME_SHOT.ender() && StrikePattern.FLAME_SHOT.flame(), "a shot is fire but not an ender");
        for (int mastery = 0; mastery <= 1000; mastery += 10) {
            check(AyatoRules.ender(0, mastery) == 0, "a right click alone is the hop");
            for (int shots = 1; shots <= 7; shots++) {
                int ender = AyatoRules.ender(shots, mastery);
                check(ender >= 1 && ender <= Math.min(5, shots) && mastery >= AyatoRules.enderMastery(ender), "a chain ends in an unlocked ender");
                check(ender >= AyatoRules.ender(shots - 1, mastery), "more shots never give a weaker ender");
                check(AyatoRules.ender(shots, mastery + 10) >= ender, "more mastery never gives a weaker ender");
            }
        }
        check(AyatoRules.ender(1, 0) == 1 && AyatoRules.ender(2, 100) == 2 && AyatoRules.ender(3, 250) == 3 && AyatoRules.ender(4, 350) == 4
            && AyatoRules.ender(5, 500) == 5, "each count has its own ender once unlocked");
        check(AyatoRules.ender(5, 0) == 1 && AyatoRules.ender(5, 499) == 4 && AyatoRules.ender(2, 99) == 1, "locked enders fall back to the strongest unlocked");
        check(AyatoRules.enderCooldown(0) > 0 && AyatoRules.enderCooldown(5) >= AyatoRules.enderCooldown(4), "the hop and the big moves have cooldowns");
        check(AyatoRules.ENDER_WINDOW_TICKS >= 15, "there is time to right click after the last shot");
        // 5.0.18: the Steel Blade reaches as far as it is long, swings wide with the right arm, and comes to a needle point.
        check(Math.abs(com.hex.kagune.control.ayato.SteelBlade.REACH - (com.hex.kagune.control.ayato.SteelBlade.ARM
            + com.hex.kagune.control.ayato.SteelBlade.LENGTH + 0.4)) < 1.0E-9 && com.hex.kagune.control.ayato.SteelBlade.REACH > 3.0,
            "the blade's reach is the arm and all of its length");
        boolean right = true;
        for (int i = 0; i < ComboStyle.AYATO_STEEL_BLADE.stageCount(); i++) {
            right &= ComboStyle.AYATO_STEEL_BLADE.stage(i).limbs()[0] == 0 && ComboStyle.AYATO_STEEL_BLADE.stage(i).shapeParam() >= 200.0F;
        }
        check(right, "the blade swings wide, from the right");
        check(com.hex.kagune.control.ayato.SteelBlade.reach(1.0) == 0.0 && com.hex.kagune.control.ayato.SteelBlade.radius(1.0) == 0.0
            && !com.hex.kagune.control.ayato.SteelBlade.build(1.0, 0.0, false).isEmpty()
            && com.hex.kagune.control.ayato.SteelBlade.build(0.0, 0.0, false).isEmpty(), "the cone and its edge meet at the point; open, nothing");
        for (ComboStyle style : ComboStyle.values()) {
            boolean listed = java.util.Arrays.asList(AYATO_STYLES).contains(style);
            check(style.ayato() == listed, "only the Ayato styles are Ayato styles: " + style);
        }
        for (int mastery = 0; mastery < 1000; mastery += 10) {
            check(AyatoRules.flameCadence(mastery + 10) <= AyatoRules.flameCadence(mastery) && AyatoRules.flameCadence(mastery) <= 7,
                "shots are fast and only get faster");
            check(AyatoRules.flameSpeed(mastery + 10) >= AyatoRules.flameSpeed(mastery) && AyatoRules.flameRange(mastery) >= 14.0,
                "shots fly fast and far");
            for (WingForm form : WingForm.values()) {
                for (int side = -1; side <= 1; side += 2) {
                    double[] tip = WingRig.aimTip(mastery, form, side, WingRig.FORWARD, false);
                    double[] up = WingRig.aimTip(mastery, form, side, new double[] {0.0, 0.6, 0.8}, false);
                    double[] down = WingRig.aimTip(mastery, form, side, new double[] {0.0, -0.6, 0.8}, false);
                    check(tip[2] > 0.4 && tip[1] > 1.4 && tip[1] < 3.4, "an aiming wing's tip is ahead, above the shoulders: " + mastery + " " + form);
                    check(Math.abs(tip[0]) > 0.25 && Math.abs(tip[0]) < 2.4, "out to the side, never in front of the face: " + mastery + " " + form);
                    check(form.usable(mastery).rightWing() ? tip[0] * side > 0.0 : tip[0] < 0.0, "each wing aims from its own side: " + mastery + " " + form);
                    check(up[1] > tip[1] && down[1] < tip[1], "the wing follows the aim up and down: " + mastery + " " + form);
                }
            }
        }
        // Aiming bends the wing; it never folds it thin or pulls it in.
        for (int mastery : new int[] {100, 150, 300, 500, 1000}) {
            if (!WingForm.TWIN.usable(mastery).rightWing()) {
                continue;
            }
            double rest = rightWingArea(mastery, 0.0F);
            double aimed = rightWingArea(mastery, 1.0F);
            check(rest > 0.0 && aimed > 0.9 * rest && aimed < 1.15 * rest, "an aiming wing keeps its size and spread: " + mastery + " " + aimed / rest);
        }
        check(AyatoRules.flameRange(500) > AyatoRules.flameRange(499) && AyatoRules.flamePierce(250) == 1 && AyatoRules.flamePierce(249) == 0
            && AyatoRules.flameEmbers(450) && !AyatoRules.flameEmbers(449), "the mastery table: pierce at 250, embers at 450, range at 500");
        for (ComboStyle style : AYATO_STYLES) {
            for (int index = 0; index < style.stageCount(); index++) {
                ComboStage stage = style.stage(index);
                String name = style + " " + stage.name();
                check(AyatoBody.poses(stage.pattern()), "every Ayato stage has a body pose: " + name);
                check(stage.limbs().length > 0, "every Ayato stage names its limbs: " + name);
                boolean flame = stage.pattern().flame();
                boolean kakuja = stage.pattern().kakuja();
                boolean anyTentacle = false;
                for (int limb : stage.limbs()) {
                    boolean leg = limb == 8 || limb == 9;
                    boolean wing = limb == 0 || limb == 1;
                    boolean hand = limb == 10 || limb == 11;
                    boolean tentacle = limb >= 20 && limb <= 25;
                    anyTentacle |= tentacle;
                    check(stage.pattern() == StrikePattern.KICK ? leg : kakuja ? tentacle || hand : flame ? wing || hand : wing, "limbs fit the strike: " + name);
                }
                if (kakuja) {
                    check(anyTentacle, "the tentacles fire every kakuja move: " + name);
                } else if (flame) {
                    check(stage.usesLimb(0) || stage.usesLimb(1), "the wings fire every shot and ender: " + name);
                    if (stage.pattern() == StrikePattern.FLAME_SHOT && index > 0) {
                        check(stage.usesLimb(10) != style.stage(index - 1).usesLimb(10), "the hands alternate: " + name);
                    }
                }
                if (style == ComboStyle.AYATO_RABBIT || style == ComboStyle.AYATO_RABBIT_BLADES
                    || style == ComboStyle.AYATO_ONE_WING) {
                    check(!stage.usesLimb(0), "one-sided forms never use the right wing: " + name);
                }
                for (int mastery = 0; mastery <= 1000; mastery += 50) {
                    for (WingForm form : WingForm.values()) {
                        double range = AyatoRules.strikeRange(mastery, form, stage);
                        double most = stage.pattern().crystal() ? 36.0 : flame ? 18.0 : 15.0;
                        check(range >= AyatoRules.KICK_REACH && range <= most && Double.isFinite(range),
                            "no strike falls short of melee reach or runs away: " + name);
                        if (stage.pattern() == StrikePattern.KICK) {
                            check(range == AyatoRules.KICK_REACH, "kicks reach as far as legs do: " + name);
                        }
                    }
                }
            }
        }
        check(AyatoRules.KICK_REACH >= 3.0, "a kick reaches at least as far as a vanilla melee hit");
        ComboStage sweep = ComboStyle.AYATO_BLADES.stage(0);
        check(AyatoRules.strikeRange(1000, WingForm.TWIN, sweep) > AyatoRules.strikeRange(0, WingForm.TWIN, sweep), "wing reach grows");
        ComboStage behead = ComboStyle.AYATO_ONE_WING.stage(2);
        check(AyatoRules.strikeRange(750, WingForm.ONE_WING, behead) > AyatoRules.strikeRange(100, WingForm.TWIN, sweep) + 1.5,
            "the One-Winged Kagune cuts from much farther away");
        check(AyatoRules.strikeRange(500, WingForm.ONE_WING, behead) == AyatoRules.strikeRange(500, WingForm.TWIN, behead),
            "a locked One-Wing reaches like the twin wings");
        check(behead.damageMul() >= ComboStyle.AYATO_ONE_WING.stage(0).damageMul() && behead.name().startsWith("Beheading"),
            "the beheading cut is the One-Wing's strongest hit");
    }

    /** The crystal mode (V): its gate, its M1 chain of spikes and its enders, and the numbers behind them. */
    private static void crystalMode() {
        check(AyatoRules.style(AyatoRules.CRYSTAL_MASTERY, WingForm.TWIN, false, true) == ComboStyle.AYATO_CRYSTAL
            && AyatoRules.style(1000, WingForm.RABBIT, true, true) == ComboStyle.AYATO_CRYSTAL, "in the crystal mode the M1 is the crystal spikes");
        check(AyatoRules.style(AyatoRules.CRYSTAL_MASTERY - 1, WingForm.TWIN, false, true) == ComboStyle.AYATO_FIRE,
            "below its mastery the crystal mode doesn't take");
        check(AyatoRules.style(1000, WingForm.ONE_WING, false, true) == ComboStyle.AYATO_ONE_WING && !AyatoRules.crystalModeAllowed(1000, WingForm.ONE_WING),
            "the One-Winged Kagune is crystal already: no crystal mode");
        check(AyatoRules.style(1000, WingForm.TWIN, false, false) == ComboStyle.AYATO_FIRE, "out of the crystal mode the M1 is the fire again");
        check(AyatoRules.style(500, WingForm.TWIN, false) == AyatoRules.style(500, WingForm.TWIN, false, false), "the old call is the fire's");
        check(ComboStyle.AYATO_CRYSTAL.stageCount() == AyatoRules.CHAIN_SHOTS, "five spikes to a chain, like the fire");
        for (int index = 0; index < 5; index++) {
            ComboStage spike = ComboStyle.AYATO_CRYSTAL.stage(index);
            ComboStage shot = ComboStyle.AYATO_FIRE.stage(index);
            check(spike.pattern() == StrikePattern.CRYSTAL_SHOT && java.util.Arrays.equals(spike.limbs(), shot.limbs()),
                "spike " + index + " uses the same wing and hand as fire shot " + index);
        }
        StrikePattern[] enders = {StrikePattern.RABBIT_HOP, StrikePattern.CRYSTAL_LANCE, StrikePattern.SPIKE_FAN, StrikePattern.SPIKE_RAIN,
            StrikePattern.SPIKE_BARRAGE, StrikePattern.CRYSTAL_BLOOM};
        check(ComboStyle.AYATO_CRYSTAL_ENDERS.stageCount() == ComboStyle.AYATO_ENDERS.stageCount(), "one crystal ender for each count of spikes");
        for (int index = 0; index < enders.length; index++) {
            StrikePattern pattern = ComboStyle.AYATO_CRYSTAL_ENDERS.stage(index).pattern();
            check(pattern == enders[index] && pattern.ender() && pattern.flame(), "crystal ender " + index + " is " + enders[index]);
            check(pattern.crystal() == (index > 0), "every crystal ender but the hop is crystal: " + pattern);
        }
        check(StrikePattern.CRYSTAL_SHOT.shot() && StrikePattern.FLAME_SHOT.shot() && !StrikePattern.CRYSTAL_SHOT.ender()
            && StrikePattern.CRYSTAL_SHOT.crystal() && StrikePattern.CRYSTAL_SHOT.flame(), "a spike is a ranged shot, not an ender");
        check(!StrikePattern.BLACK_RABBIT.crystal() && !StrikePattern.FLAME_SHOT.crystal(), "the fire is not crystal");
        check(ComboStyle.AYATO_CRYSTAL.ayatoShots() && ComboStyle.AYATO_FIRE.ayatoShots() && !ComboStyle.AYATO_ENDERS.ayatoShots()
            && ComboStyle.AYATO_CRYSTAL.ayatoEnders() == ComboStyle.AYATO_CRYSTAL_ENDERS && ComboStyle.AYATO_FIRE.ayatoEnders() == ComboStyle.AYATO_ENDERS
            && ComboStyle.AYATO_CRYSTAL_ENDERS.ayatoEnderSet() && ComboStyle.AYATO_ENDERS.ayatoEnderSet(), "each chain ends in its own enders");
        for (int mastery = 0; mastery <= 1000; mastery += 10) {
            check(AyatoRules.crystalRange(mastery) >= 1.5 * AyatoRules.flameRange(mastery) && AyatoRules.crystalRange(mastery) >= 24.0,
                "spikes fly much further than the fire");
            check(AyatoRules.crystalSpeed(mastery) > AyatoRules.flameSpeed(mastery) && AyatoRules.crystalSpeed(mastery + 10) >= AyatoRules.crystalSpeed(mastery),
                "spikes are faster than the fire, and faster with mastery");
            check(AyatoRules.crystalCadence(mastery) == AyatoRules.flameCadence(mastery) + 1
                && AyatoRules.shotCadence(StrikePattern.CRYSTAL_SHOT, mastery) == AyatoRules.crystalCadence(mastery)
                && AyatoRules.shotCadence(StrikePattern.FLAME_SHOT, mastery) == AyatoRules.flameCadence(mastery),
                "spikes trade a beat of pace for their reach and bleeding");
            int delay = AyatoRules.burstDelay(mastery);
            check(delay >= 300 && delay == AyatoRules.STUCK_TICKS, "a spike stays stuck in a body for at least fifteen seconds before it bursts");
            for (WingForm form : WingForm.values()) {
                if (form.usable(mastery) == WingForm.ONE_WING) {
                    continue;
                }
                check(AyatoRules.strikeRange(mastery, form, ComboStyle.AYATO_CRYSTAL.stage(0)) == AyatoRules.crystalRange(mastery)
                    && AyatoRules.strikeRange(mastery, form, ComboStyle.AYATO_CRYSTAL_ENDERS.stage(1)) > AyatoRules.crystalRange(mastery),
                    "a spike reaches as far as it flies, the lance further");
            }
        }
        for (int stacks = 1; stacks <= AyatoRules.MAX_BLEED_STACKS; stacks++) {
            check(AyatoRules.bleedAmplifier(stacks) >= AyatoRules.bleedAmplifier(stacks - 1) && AyatoRules.bleedAmplifier(stacks) <= 3
                && AyatoRules.bleedDuration(stacks) >= AyatoRules.bleedDuration(stacks - 1) && AyatoRules.bleedDuration(stacks) <= 220,
                "more spikes bleed harder and longer, within limits");
        }
        check(AyatoRules.bleedAmplifier(1) == 0 && AyatoRules.bleedAmplifier(AyatoRules.MAX_BLEED_STACKS) == 3, "the bleeding runs from I to IV");
        check(AyatoRules.burstDamage(5.0F) > 1.0F && AyatoRules.burstDamage(5.0F) < 5.0F, "a burst is a smaller hit than the spike's");
        check(AyatoRules.BLOOM_MULTIPLIER > 1.0F && AyatoRules.MAX_STUCK_PER_TARGET >= 5 && AyatoRules.MAX_STUCK >= AyatoRules.MAX_STUCK_PER_TARGET,
            "the Bloom bursts harder, and enough spikes stick to be worth it");
        check(AyatoRules.CRYSTALLIZE_TICKS >= 16 && AyatoRules.CRYSTALLIZE_TICKS <= 40 && AyatoRules.THAW_TICKS > 0,
            "crystallizing is quick but not instant");
    }

    /** The crystallization climbs the wing from the shoulder blade to the tip: crystal below the front, fire above it. */
    private static void sweep() {
        for (boolean reEra : new boolean[] {false, true}) {
            // A full-size wing (4.3 blocks): distances below are in wing lengths.
            double length = 4.3;
            UkakuModel.Frame frame = new UkakuModel.Frame(new double[] {0.0, 0.0, 0.0}, new double[] {1.0, 0.0, 0.0}, new double[] {0.0, 1.0, 0.0},
                new double[] {0.0, 0.0, 1.0}, length);
            int mastery = reEra ? 800 : 300;
            UkakuModel.Params params = new UkakuModel.Params(1.0, 1.0F, 1.0F, false, reEra, 0.0F, 0.0F, 1.0F, 1.0F, 1, 1, 0.0F, 0.0F, mastery, null);
            double previous = 0.0;
            for (double front : new double[] {0.15, 0.3, 0.6, 1.0}) {
                double[] reach = {0.0};
                UkakuCrystal.wing((a, b, c, d, n, ca, cb, cc, cd) -> {
                    for (double[] v : new double[][] {a, b, c, d}) {
                        reach[0] = Math.max(reach[0], Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]) / length);
                    }
                }, frame, params, 1.0, front);
                check(reach[0] > previous, "the crystal climbs further up the wing as the front rises (" + front + ")");
                check(reach[0] < front * 1.15 + 0.25, "no crystal runs far ahead of the front (" + front + ": " + reach[0] + ")");
                previous = reach[0];
            }
            check(previous > 0.95, "at the end the crystal reaches the very tip");
            for (double front : new double[] {0.3, 0.6}) {
                double[] nearest = {Double.MAX_VALUE};
                int[] count = {0};
                UkakuFlame.wing((a, b, c, d, n, ca, cb, cc, cd) -> {
                    double far = 0.0;
                    for (double[] v : new double[][] {a, b, c, d}) {
                        far = Math.max(far, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]) / length);
                    }
                    nearest[0] = Math.min(nearest[0], far);
                    count[0]++;
                }, frame, params, 1.0, front);
                check(count[0] > 0 && nearest[0] > front * 0.4, "the fire only burns above the crystal front (" + front + ": " + nearest[0] + ")");
            }
            int[] gone = {0};
            UkakuFlame.wing((a, b, c, d, n, ca, cb, cc, cd) -> gone[0]++, frame, params, 1.0, 1.0);
            check(gone[0] == 0, "fully crystallized, no fire is left");
        }
        // Mid-climb both are drawn: still within twice the budget.
        for (float front : new float[] {0.1F, 0.3F, 0.5F, 0.7F, 0.9F}) {
            int quads = WingRig.build(new WingRig.Input(WingForm.TWIN, 1000, 1.0F, 1.0, front, front, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1,
                false)).size();
            check(quads < 2 * 3200, "crystallizing stays within the budget at every height (" + front + ": " + quads + ")");
        }
    }

    /** The crystal spike's geometry: well formed, as long as it says, the burst's glow and cracks only when it is due. */
    private static void spikes() {
        for (double size : new double[] {0.85, 1.0, 2.2}) {
            for (double heat : new double[] {0.0, 0.5, 1.0}) {
                List<double[]> points = new java.util.ArrayList<>();
                int[] quads = {0};
                boolean[] good = {true};
                CrystalSpike.build((a, b, c, d, ca, cb, cc, cd) -> {
                    quads[0]++;
                    for (double[] v : new double[][] {a, b, c, d}) {
                        good[0] &= Double.isFinite(v[0]) && Double.isFinite(v[1]) && Double.isFinite(v[2]);
                        points.add(v);
                    }
                    good[0] &= (ca >>> 24) <= 255;
                }, new double[] {0.0, 0.0, 0.0}, new double[] {0.0, 0.0, 1.0}, size, new double[] {3.0, 2.0, -4.0}, 1.0, heat, 1.0, 7, true, 1.0);
                check(good[0] && quads[0] > 0 && quads[0] <= CrystalSpike.QUADS, "a spike is well formed within its quad budget (" + size + ", " + heat + ")");
                double ahead = -Double.MAX_VALUE;
                double behind = Double.MAX_VALUE;
                for (double[] v : points) {
                    ahead = Math.max(ahead, v[2]);
                    behind = Math.min(behind, v[2]);
                }
                check(ahead <= 0.05 * size + 0.2 * size && behind < -0.9 * CrystalSpike.LENGTH * size, "the spike runs back from its point (" + size + ")");
            }
        }
        int[] cool = {0};
        int[] hot = {0};
        CrystalSpike.build((a, b, c, d, ca, cb, cc, cd) -> cool[0]++, new double[] {0.0, 0.0, 0.0}, new double[] {0.0, 0.0, 1.0}, 1.0,
            new double[] {3.0, 2.0, -4.0}, 0.0, 0.0, 1.0, 7, true, 1.0);
        CrystalSpike.build((a, b, c, d, ca, cb, cc, cd) -> hot[0]++, new double[] {0.0, 0.0, 0.0}, new double[] {0.0, 0.0, 1.0}, 1.0,
            new double[] {3.0, 2.0, -4.0}, 0.0, 1.0, 1.0, 7, true, 1.0);
        check(hot[0] > cool[0], "a spike about to burst glows and cracks");
        int[] faded = {0};
        CrystalSpike.build((a, b, c, d, ca, cb, cc, cd) -> faded[0]++, new double[] {0.0, 0.0, 0.0}, new double[] {0.0, 0.0, 1.0}, 1.0,
            new double[] {3.0, 2.0, -4.0}, 1.0, 0.0, 1.0, 7, true, 0.0);
        check(faded[0] == 0, "a faded spike draws nothing");
    }

    /** The Wing Vortex (Z): which wing goes, how the back looks while it is gone, the vortex itself and the throw. */
    private static void vortex() {
        check(AyatoRules.vortexSide(false, false, false, 0.2F) == -1 && AyatoRules.vortexSide(false, false, false, 0.8F) == -1,
            "one wing alone throws itself (the left)");
        check(AyatoRules.vortexSide(true, true, false, 0.2F) == -1 && AyatoRules.vortexSide(true, false, true, 0.8F) == 1,
            "never a wing that is still growing back");
        check(AyatoRules.vortexSide(true, false, false, 0.2F) == 1 && AyatoRules.vortexSide(true, false, false, 0.8F) == -1, "otherwise a random wing");
        long launch = 1000L;
        long regrow = launch + 20L;
        float[] before = AyatoRules.vortexWing(launch - AyatoRules.VORTEX_WINDUP - 1, launch, regrow);
        check(before[0] == 0.0F && before[1] == 1.0F, "before the wind-up the wing is just there");
        float previous = 0.0F;
        for (int tick = -AyatoRules.VORTEX_WINDUP; tick < 0; tick++) {
            float[] wing = AyatoRules.vortexWing(launch + tick + 0.5, launch, regrow);
            check(wing[1] == 1.0F && wing[0] >= previous && wing[0] <= 1.0F, "the wing twirls up through the wind-up, still there");
            previous = wing[0];
        }
        check(previous > 0.8F, "it is all but fully coiled by the throw");
        for (long tick = launch; tick < regrow; tick++) {
            check(AyatoRules.vortexWing(tick, launch, regrow)[1] == 0.0F, "while the vortex flies the shoulder blade is bare");
        }
        previous = -1.0F;
        for (int tick = 0; tick <= AyatoRules.REGROW_TICKS; tick++) {
            float grow = AyatoRules.vortexWing(regrow + tick, launch, regrow)[1];
            check(grow >= previous, "the new wing only grows");
            previous = grow;
        }
        check(previous == 1.0F && AyatoRules.vortexWing(regrow + AyatoRules.REGROW_TICKS * 3, launch, regrow)[1] == 1.0F, "and is whole again");
        check(AyatoRules.vortexWing(launch, Long.MIN_VALUE, Long.MIN_VALUE)[1] == 1.0F, "no vortex yet: both wings there");
        for (int mastery = 0; mastery <= 1000; mastery += 10) {
            int flight = AyatoRules.vortexFlight(mastery);
            check(flight * AyatoRules.vortexSpeed(mastery) >= AyatoRules.VORTEX_RANGE && AyatoRules.VORTEX_RANGE == 25.0, "the vortex flies 25 blocks");
            check(AyatoRules.vortexCooldown(mastery) > AyatoRules.VORTEX_WINDUP + flight + AyatoRules.VORTEX_FADE_TICKS + AyatoRules.REGROW_TICKS,
                "he never throws a wing that hasn't grown back");
            check(AyatoRules.vortexBusy(mastery) > AyatoRules.VORTEX_WINDUP + flight + AyatoRules.VORTEX_FADE_TICKS + AyatoRules.REGROW_TICKS
                && AyatoRules.vortexBusy(mastery) <= AyatoRules.vortexCooldown(mastery), "cooldowns off, still never a wing that hasn't grown back");
            check(AyatoRules.vortexDamage(mastery) > AyatoRules.meleeDamage(mastery) && AyatoRules.vortexBurnSeconds(mastery) >= 5
                && AyatoRules.vortexRadius(mastery) >= 1.0F, "it hits hard, burns and is wide");
        }
        check(AyatoRules.VORTEX_KNOCKBACK >= 1.5 && AyatoRules.VORTEX_LIFT > 0.3, "it throws what it hits a long way");

        // The back: a thrown wing is gone from its shoulder blade, a coiling one twirls, a growing one is smaller.
        for (int mastery : new int[] {100, 300, 800}) {
            WingRig.Input full = new WingRig.Input(WingForm.TWIN, mastery, 1.0F, 1.0, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1, false,
                0, 0, WingRig.FORWARD, 0, 0, 0.0F, 0.0F, 1.0F, 1.0F);
            WingRig.Input gone = new WingRig.Input(WingForm.TWIN, mastery, 1.0F, 1.0, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1, false,
                0, 0, WingRig.FORWARD, 0, 0, 0.0F, 0.0F, 0.0F, 1.0F);
            WingRig.Input half = new WingRig.Input(WingForm.TWIN, mastery, 1.0F, 1.0, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1, false,
                0, 0, WingRig.FORWARD, 0, 0, 0.0F, 0.0F, 0.5F, 1.0F);
            WingRig.Input coiled = new WingRig.Input(WingForm.TWIN, mastery, 1.0F, 1.0, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1, false,
                0, 0, WingRig.FORWARD, 0, 0, 0.8F, 0.0F, 1.0F, 1.0F);
            boolean noRight = true;
            for (WingRig.Quad quad : WingRig.build(gone)) {
                float[] v = quad.xyz();
                for (int k = 0; k < 4; k++) {
                    noRight &= v[k * 3] < 0.25F;
                }
            }
            check(noRight && !WingRig.build(gone).isEmpty(), "a thrown right wing leaves only the left on his back: " + mastery);
            double rest = rightWingArea(WingRig.build(full));
            double growing = rightWingArea(WingRig.build(half));
            check(growing > 0.0 && growing < rest, "a wing growing back is smaller than a whole one: " + mastery);
            List<WingRig.Quad> twirl = WingRig.build(coiled);
            float[] e = extent(twirl);
            boolean finite = true;
            for (WingRig.Quad quad : twirl) {
                for (float c : quad.xyz()) {
                    finite &= Float.isFinite(c);
                }
            }
            check(finite && !sameQuads(twirl, WingRig.build(full)) && e[1] > 0.0F && Math.abs(e[0]) < 7.0F && Math.abs(e[3]) < 7.0F,
                "a coiling wing twirls, near its host: " + mastery);
        }

        // The vortex itself: from the shoulder blade (root) to its point (head), still attached.
        for (double size : new double[] {0.6, 1.0, 1.3}) {
            for (double unwind : new double[] {0.0, 1.0}) {
                for (double length : new double[] {1.5, 10.0, 25.0}) {
                    List<double[]> points = new ArrayList<>();
                    int[] quads = {0};
                    boolean[] good = {true};
                    WingVortex.build((a, b, c, d, ca, cb, cc, cd) -> {
                        quads[0]++;
                        for (double[] v : new double[][] {a, b, c, d}) {
                            good[0] &= Double.isFinite(v[0]) && Double.isFinite(v[1]) && Double.isFinite(v[2]);
                            points.add(v);
                        }
                    }, new double[] {0.0, 0.0, 0.0}, new double[] {0.0, 0.0, length}, size, 1.3, 5, new double[] {4.0, 3.0, -6.0}, 1.0, unwind, 0.5,
                        true);
                    String name = "(" + size + ", " + unwind + ", " + length + ")";
                    check(good[0] && quads[0] > 0 && quads[0] <= WingVortex.QUADS, "the vortex is well formed within its budget " + name);
                    double front = -Double.MAX_VALUE;
                    double back = Double.MAX_VALUE;
                    double wide = 0.0;
                    double atRoot = 0.0;
                    for (double[] v : points) {
                        front = Math.max(front, v[2]);
                        back = Math.min(back, v[2]);
                        double r = Math.hypot(v[0], v[1]);
                        if (v[2] > 0.3 && v[2] < length - 0.3) {
                            wide = Math.max(wide, r);
                        }
                        if (v[2] < 0.15) {
                            atRoot = Math.max(atRoot, r);
                        }
                    }
                    // Its glow reaches a little ahead of the point (more as it spreads, unwinding).
                    check(front <= length + size * (0.4 + 1.0 * (1.0 + 1.3 * unwind)) && back > -size * (1.0 + 1.3 * unwind),
                        "the vortex runs from the shoulder blade to its point " + name + ": " + back + " .. " + front);
                    check(wide > 0.3 * WingVortex.RADIUS * size && wide < (1.9 + 2.4 * unwind) * WingVortex.RADIUS * size,
                        "it is a vortex, not a line, and never a cloud " + name + ": " + wide);
                    if (unwind == 0.0 && length >= 10.0) {
                        check(atRoot < 0.5 * wide, "it leaves the shoulder blade as a narrow jet and opens out " + name + ": " + atRoot + " of " + wide);
                    }
                }
            }
        }
        List<Double> turnA = new ArrayList<>();
        List<Double> turnB = new ArrayList<>();
        WingVortex.build((a, b, c, d, ca, cb, cc, cd) -> turnA.add(a[0]), new double[] {0, 0, 0}, new double[] {0, 0, 8}, 1.0, 1.0, 5,
            new double[] {4, 3, -6}, 1.0, 0.0, 0.0, true);
        WingVortex.build((a, b, c, d, ca, cb, cc, cd) -> turnB.add(a[0]), new double[] {0, 0, 0}, new double[] {0, 0, 8}, 1.0, 1.05, 5,
            new double[] {4, 3, -6}, 1.0, 0.0, 0.0, true);
        check(!turnA.equals(turnB), "the vortex spins");
        int[] faded = {0};
        WingVortex.build((a, b, c, d, ca, cb, cc, cd) -> faded[0]++, new double[] {0, 0, 0}, new double[] {0, 0, 8}, 1.0, 1.0, 5,
            new double[] {4, 3, -6}, 0.0, 0.0, 0.0, true);
        WingVortex.build((a, b, c, d, ca, cb, cc, cd) -> faded[0]++, new double[] {0, 0, 0}, new double[] {0, 0, 0.01}, 1.0, 1.0, 5,
            new double[] {4, 3, -6}, 1.0, 0.0, 0.0, true);
        check(faded[0] == 0, "a faded vortex, or one not out yet, draws nothing");

        // The throw.
        float release = AyatoRules.VORTEX_WINDUP / (float)(AyatoRules.VORTEX_WINDUP + 12);
        for (boolean right : new boolean[] {true, false}) {
            check(allZero(AyatoBody.vortex(right, 0.0F, release).channels()) && allZero(AyatoBody.vortex(right, 1.0F, release).channels()),
                "the throw starts and ends at rest");
            float[] previousChannels = AyatoBody.vortex(right, 0.0F, release).channels();
            boolean smooth = true;
            for (int step = 1; step <= 400; step++) {
                float[] channels = AyatoBody.vortex(right, step / 400.0F, release).channels();
                for (int index = 0; index < channels.length; index++) {
                    smooth &= Math.abs(channels[index] - previousChannels[index]) <= 0.05F;
                }
                previousChannels = channels;
            }
            check(smooth, "the throw never snaps between frames");
        }
        // The arm finishes its whip just after the wing has left it.
        AyatoBody.Pose cocked = AyatoBody.vortex(true, release * 0.6F, release);
        AyatoBody.Pose thrown = AyatoBody.vortex(true, release + AyatoBody.VORTEX_FOLLOW, release);
        check(cocked.rightArmX > 0.3F && thrown.rightArmX < -1.4F && thrown.twist < -0.3F, "the right arm draws back, then whips the wing forward");
        AyatoBody.Pose left = AyatoBody.vortex(false, release + AyatoBody.VORTEX_FOLLOW, release);
        check(near(left.leftArmX, thrown.rightArmX) && near(left.twist, -thrown.twist), "a left throw mirrors a right one");
    }

    /** The Gatling (J): spin-up, a stream that speeds up, a drain that grows, the wings curled and spinning, the body braced. */
    private static void gatling() {
        for (long held = 0; held < AyatoRules.GATLING_SPINUP; held++) {
            check(AyatoRules.gatlingRate(held) == 0.0, "nothing fires while the wings spin up");
        }
        double previous = 0.0;
        for (long held = AyatoRules.GATLING_SPINUP; held <= 200; held++) {
            double rate = AyatoRules.gatlingRate(held);
            check(rate >= previous && rate >= AyatoRules.GATLING_START_RATE && rate <= AyatoRules.GATLING_FULL_RATE, "the stream only speeds up");
            previous = rate;
        }
        check(previous == AyatoRules.GATLING_FULL_RATE && AyatoRules.GATLING_FULL_RATE * 20 >= 10, "at full spin it fires ten or more a second");
        for (int mastery : new int[] {200, 500, 1000}) {
            check(AyatoRules.gatlingDrain(200, mastery) > AyatoRules.gatlingDrain(20, mastery) && AyatoRules.gatlingDrain(0, mastery) > 0.0F,
                "the longer it runs, the more it drains");
            check(AyatoRules.gatlingDrain(100, 1000) < AyatoRules.gatlingDrain(100, 200), "mastery makes it cheaper");
            check(AyatoRules.gatlingDamage(mastery) < AyatoRules.meleeDamage(mastery) * 0.5F && AyatoRules.gatlingDamage(mastery) > 0.3F,
                "each spike is light: the stream is what hurts");
        }
        check(AyatoRules.gatlingAim(100.0, false, Long.MIN_VALUE) == 0.0F, "never fired: wings at rest");
        float up = 0.0F;
        for (int tick = 0; tick <= AyatoRules.GATLING_SPINUP; tick++) {
            float aim = AyatoRules.gatlingAim(1000 + tick, true, 1000L);
            check(aim >= up, "the wings curl forward as it spins up");
            up = aim;
        }
        check(up == 1.0F && AyatoRules.gatlingAim(1000 + AyatoRules.GATLING_SPINDOWN, false, 1000L) == 0.0F
            && AyatoRules.gatlingAim(1002, false, 1000L) > 0.0F, "they ease back after the trigger is let go");
        // The shudder: the same wing, rocked about its centre line, never noticeably bigger or smaller (the user's rule).
        for (int mastery : new int[] {300, 800}) {
            WingRig.Input still = new WingRig.Input(WingForm.TWIN, mastery, 1.0F, 1.0, 1.0F, 1.0F, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1,
                false, 1.0F, 1.0F, WingRig.FORWARD, 0, 0, 0, 0, 1, 1, 0.0F);
            WingRig.Input spun = new WingRig.Input(WingForm.TWIN, mastery, 1.0F, 1.0, 1.0F, 1.0F, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1,
                false, 1.0F, 1.0F, WingRig.FORWARD, 0, 0, 0, 0, 1, 1, 0.28F);
            List<WingRig.Quad> a = WingRig.build(still);
            List<WingRig.Quad> b = WingRig.build(spun);
            boolean finite = true;
            for (WingRig.Quad quad : b) {
                for (float c : quad.xyz()) {
                    finite &= Float.isFinite(c);
                }
            }
            double areaA = totalArea(a);
            double areaB = totalArea(b);
            check(finite && !sameQuads(a, b) && Math.abs(areaB - areaA) < 0.03 * areaA, "the Gatling's wings shudder, keeping their size: "
                + mastery + " " + areaB / areaA);
        }
        check(allZero(AyatoBody.gatling(0.0F, 10.0).channels()), "no Gatling, no pose");
        AyatoBody.Pose braced = AyatoBody.gatling(1.0F, 10.0);
        check(braced.rightArmX < -1.3F && braced.leftArmX < -1.3F && braced.torsoX > 0.0F, "both palms held out at the target, braced");
        boolean small = true;
        for (int step = 1; step <= 100; step++) {
            float[] before = AyatoBody.gatling((step - 1) / 100.0F, 10.0 + (step - 1) * 0.05).channels();
            float[] after = AyatoBody.gatling(step / 100.0F, 10.0 + step * 0.05).channels();
            for (int index = 0; index < after.length; index++) {
                small &= Math.abs(after[index] - before[index]) <= 0.05F;
            }
        }
        check(small, "the Gatling pose eases in without snapping");
    }

    /** Area of every quad. */
    private static double totalArea(List<WingRig.Quad> quads) {
        double total = 0.0;
        for (WingRig.Quad quad : quads) {
            float[] v = quad.xyz();
            for (int t = 0; t < 2; t++) {
                int i = t == 0 ? 3 : 6;
                int j = t == 0 ? 6 : 9;
                double[] p = {v[i] - v[0], v[i + 1] - v[1], v[i + 2] - v[2]};
                double[] q = {v[j] - v[0], v[j + 1] - v[1], v[j + 2] - v[2]};
                double x = p[1] * q[2] - p[2] * q[1];
                double y = p[2] * q[0] - p[0] * q[2];
                double z = p[0] * q[1] - p[1] * q[0];
                total += 0.5 * Math.sqrt(x * x + y * y + z * z);
            }
        }
        return total;
    }

    /** Area of the right wing's quads (x > 0.25), for size comparisons. */
    private static double rightWingArea(List<WingRig.Quad> quads) {
        double total = 0.0;
        for (WingRig.Quad quad : quads) {
            float[] v = quad.xyz();
            if (v[0] <= 0.25F && v[3] <= 0.25F && v[6] <= 0.25F) {
                continue;
            }
            double[] p = {v[3] - v[0], v[4] - v[1], v[5] - v[2]};
            double[] q = {v[6] - v[0], v[7] - v[1], v[8] - v[2]};
            double x = p[1] * q[2] - p[2] * q[1];
            double y = p[2] * q[0] - p[0] * q[2];
            double z = p[0] * q[1] - p[1] * q[0];
            total += 0.5 * Math.sqrt(x * x + y * y + z * z);
        }
        return total;
    }

    private static void body() {
        StrikePattern[] patterns = {StrikePattern.KICK, StrikePattern.WING_SLASH, StrikePattern.WING_CROSS, StrikePattern.SHARD_BURST,
            StrikePattern.ONE_WING_SLASH, StrikePattern.ONE_WING_RETURN, StrikePattern.BEHEADING, StrikePattern.FLAME_SHOT, StrikePattern.RABBIT_HOP,
            StrikePattern.FLARE, StrikePattern.RISING_FLAME, StrikePattern.FIRE_FAN, StrikePattern.RABBIT_DASH, StrikePattern.BLACK_RABBIT,
            StrikePattern.CRYSTAL_SHOT, StrikePattern.CRYSTAL_LANCE, StrikePattern.SPIKE_FAN, StrikePattern.SPIKE_RAIN, StrikePattern.SPIKE_BARRAGE,
            StrikePattern.CRYSTAL_BLOOM};
        for (StrikePattern pattern : patterns) {
            check(AyatoBody.poses(pattern), "Ayato poses " + pattern);
            for (float impact : new float[] {0.3F, 0.36F, 0.42F, 0.46F}) {
                for (boolean both : new boolean[] {false, true}) {
                    for (boolean right : new boolean[] {true, false}) {
                        float[] previous = AyatoBody.pose(pattern, right, both, 0.0F, impact).channels();
                        boolean rest = allZero(previous) && allZero(AyatoBody.pose(pattern, right, both, 1.0F, impact).channels());
                        check(rest, "every Ayato pose starts and ends at rest: " + pattern);
                        boolean finite = true;
                        boolean smooth = true;
                        boolean limits = true;
                        for (int step = 1; step <= 400; step++) {
                            AyatoBody.Pose pose = AyatoBody.pose(pattern, right, both, step / 400.0F, impact);
                            float[] channels = pose.channels();
                            for (int index = 0; index < channels.length; index++) {
                                finite &= Float.isFinite(channels[index]);
                                double change = channels[index] - previous[index];
                                if (index == channels.length - 1) {
                                    // The flip channel: a full turn is the same as none.
                                    change = Math.IEEEremainder(change, Math.PI * 2.0);
                                }
                                smooth &= Math.abs(change) <= 0.05F || index == channels.length - 1 && Math.abs(change) <= 0.12F;
                            }
                            limits &= Math.abs(pose.rightLegX) <= AyatoBody.MAX_LEG_X && Math.abs(pose.leftLegX) <= AyatoBody.MAX_LEG_X
                                && Math.abs(pose.rightLegZ) <= AyatoBody.MAX_LEG_Z && Math.abs(pose.leftLegZ) <= AyatoBody.MAX_LEG_Z
                                && Math.abs(pose.twist) <= 1.2F && Math.abs(pose.rightArmX) <= 3.0F && Math.abs(pose.leftArmX) <= 3.0F
                                && Math.abs(pose.torsoX) <= 1.0F;
                            previous = channels;
                        }
                        check(finite, "Ayato poses are finite: " + pattern);
                        check(smooth, "Ayato poses never snap between frames: " + pattern + " impact " + impact);
                        check(limits, "Ayato poses stay inside the body layer's limits: " + pattern);
                    }
                    for (float p = 0.0F; p <= 1.0F; p += 0.05F) {
                        AyatoBody.Pose r = AyatoBody.pose(pattern, true, both, p, impact);
                        AyatoBody.Pose l = AyatoBody.pose(pattern, false, both, p, impact);
                        boolean mirrored = near(r.twist, -l.twist) && near(r.headY, -l.headY) && near(r.torsoX, l.torsoX)
                            && near(r.rightArmX, l.leftArmX) && near(r.rightArmY, -l.leftArmY) && near(r.rightArmZ, -l.leftArmZ)
                            && near(r.leftArmX, l.rightArmX) && near(r.rightLegX, l.leftLegX) && near(r.rightLegZ, -l.leftLegZ)
                            && near(r.rightArmBend, l.leftArmBend);
                        check(mirrored, "left-side strikes mirror right-side ones: " + pattern);
                    }
                }
            }
        }
        float impact = 0.36F;
        AyatoBody.Pose kick = AyatoBody.pose(StrikePattern.KICK, true, false, impact, impact);
        check(kick.rightLegX <= -1.3F && kick.leftLegX >= 0.0F, "at the impact the kicking leg is fully up, the other one planted");
        check(kick.leftArmX < -0.5F && kick.leftArmBend > 0.5F, "the free arm stays up in guard through a kick");
        AyatoBody.Pose leftKick = AyatoBody.pose(StrikePattern.KICK, false, false, impact, impact);
        check(leftKick.leftLegX <= -1.3F && leftKick.rightLegX >= 0.0F, "a left kick raises the left leg");
        AyatoBody.Pose cut = AyatoBody.pose(StrikePattern.WING_SLASH, true, false, 0.38F, 0.38F);
        check(cut.rightArmY < -0.5F && cut.twist < -0.2F, "a right wing cut carries the arm and chest across to the left");
        AyatoBody.Pose windup = AyatoBody.pose(StrikePattern.BEHEADING, false, false, 0.55F * 0.46F, 0.46F);
        check(windup.leftArmX <= -2.0F && windup.torsoX < -0.2F, "the beheading cut rises high before it drives through");
        AyatoBody.Pose behead = AyatoBody.pose(StrikePattern.BEHEADING, false, false, 0.46F, 0.46F);
        check(behead.twist > 0.6F && behead.rightLegX < -0.3F && behead.leftLegX > 0.2F, "the beheading cut turns the chest right and lunges onto the right leg");
        AyatoBody.Pose burst = AyatoBody.pose(StrikePattern.SHARD_BURST, true, true, 0.42F, 0.42F);
        check(near(burst.rightArmX, burst.leftArmX) && near(burst.rightArmZ, -burst.leftArmZ) && burst.torsoX < -0.2F,
            "a twin shard burst throws both arms open and recoils");
        check(!AyatoBody.poses(StrikePattern.LASH) && allZero(AyatoBody.pose(StrikePattern.LASH, true, true, 0.4F, 0.4F).channels()),
            "Kaneki patterns get no Ayato pose");
        AyatoBody.Pose shot = AyatoBody.pose(StrikePattern.FLAME_SHOT, true, false, 0.38F, 0.38F);
        check(shot.rightArmX < -1.2F && shot.rightArmBend < 0.3F && shot.leftArmBend > 0.6F, "a right shot thrusts the right hand out, the left stays tucked");
        AyatoBody.Pose flipping = AyatoBody.pose(StrikePattern.BLACK_RABBIT, true, true, 0.24F, 0.1F);
        check(flipping.flip > 2.5F && flipping.flip < 3.8F && flipping.rightKnee > 1.0F, "halfway through the backflip he is upside down, tucked");
        AyatoBody.Pose hanging = AyatoBody.pose(StrikePattern.BLACK_RABBIT, true, true, 0.55F, 0.1F);
        check(Math.abs(Math.IEEEremainder(hanging.flip, Math.PI * 2.0)) < 1.0E-3 && hanging.rightArmZ > 0.8F,
            "the flip is done by the barrage: upright, arms flung wide");
        AyatoBody.Pose dash = AyatoBody.pose(StrikePattern.RABBIT_DASH, true, false, 0.5F, 0.15F);
        check(dash.torsoX > 0.5F && dash.rightArmX > 0.5F, "the dash runs low, arms trailing like the wings");
    }

    private static boolean allZero(float[] values) {
        for (float value : values) {
            if (value != 0.0F) {
                return false;
            }
        }
        return true;
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 1.0E-5F;
    }

    private static void volley() {
        double[] root = UkakuWing.bladePoint(0.0);
        check(Math.abs(root[0]) < 1.0E-9 && Math.abs(root[1]) < 1.0E-9, "shards measure from the shoulder blade");
        float[] blade = UkakuWing.bounds(UkakuWing.wing(1.0, 1.0F, 1.0F, 7, 1.0F));
        double previous = -1.0;
        for (int step = 0; step <= 20; step++) {
            double[] point = UkakuWing.bladePoint(step / 20.0);
            check(point[0] >= blade[0] - 0.02 && point[0] <= blade[2] + 0.02 && point[1] >= blade[1] - 0.02 && point[1] <= blade[3] + 0.02,
                "every launch point lies on the drawn crystallized blade");
            double distance = Math.hypot(point[0], point[1]);
            check(distance > previous, "launch points run from the root to the tip");
            previous = distance;
        }
        check(previous > 0.9, "the blade spine reaches the tip");

        // Launch points sit on the drawn wings in the volley pose, on the right side of the body.
        for (int mastery : new int[] {0, 100, 250, 450, 750, 1000}) {
            for (WingForm form : new WingForm[] {WingForm.TWIN, WingForm.RABBIT}) {
                if (!form.unlocked(mastery)) {
                    continue;
                }
                List<WingRig.Quad> quads = WingRig.build(new WingRig.Input(form, mastery, 1.0F, 1.0, 1.0F, 1.0F, WingRig.Motion.NONE, 0.0F,
                    0.4F, 1.0F, 0.0F, false, 1.0F, 1.0F, false));
                for (int side : form.rightWing() ? new int[] {1, -1} : new int[] {-1}) {
                    float[] box = extent(quads, side);
                    for (double t = 0.3; t <= 0.95; t += 0.05) {
                        double[] point = WingRig.launchPoint(mastery, form, side, t, false);
                        boolean finite = Double.isFinite(point[0]) && Double.isFinite(point[1]) && Double.isFinite(point[2]);
                        check(finite && point[0] >= box[0] - 0.05 && point[0] <= box[3] + 0.05 && point[1] >= box[1] - 0.05
                            && point[1] <= box[4] + 0.05 && point[2] >= box[2] - 0.05 && point[2] <= box[5] + 0.05,
                            "a shard leaves from the drawn wing: " + form + " " + mastery + " side " + side + " t " + t);
                        check(side > 0 ? point[0] > 0.2 : point[0] < -0.2, "each wing fires from its own side");
                        check(point[1] > WingRig.ROOT_Y, "shards leave above the shoulder blades");
                    }
                }
            }
        }
        double[] standing = WingRig.launchPoint(0, WingForm.TWIN, 1, 0.6, false);
        double[] crouched = WingRig.launchPoint(0, WingForm.TWIN, 1, 0.6, true);
        check(Math.abs(standing[1] - crouched[1] - WingRig.CROUCH_DROP) < 1.0E-9, "crouching lowers the launch points with the wings");

        java.util.Random random = new java.util.Random(7);
        double[][] aims = {{0, 0, 1}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0.3, -0.2, -0.9}, {0, 0, 0}, {1e-12, 0, 0}};
        for (double[] aim : aims) {
            for (float spread : new float[] {0.0F, 8.0F, 13.0F}) {
                double length = Math.sqrt(aim[0] * aim[0] + aim[1] * aim[1] + aim[2] * aim[2]);
                double[] unit = length > 1.0E-9 ? new double[] {aim[0] / length, aim[1] / length, aim[2] / length} : new double[] {0, 0, 1};
                boolean within = true;
                boolean units = true;
                double minDot = 1.0;
                for (int sample = 0; sample < 400; sample++) {
                    double[] d = AyatoRules.spreadDirection(aim[0], aim[1], aim[2], spread, random.nextDouble(), random.nextDouble());
                    double norm = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
                    units &= Math.abs(norm - 1.0) < 1.0E-9;
                    double dot = d[0] * unit[0] + d[1] * unit[1] + d[2] * unit[2];
                    within &= dot >= Math.cos(Math.toRadians(spread)) - 1.0E-9;
                    minDot = Math.min(minDot, dot);
                }
                check(units, "scatter directions are unit vectors");
                check(within, "the scatter stays inside its cone");
                if (spread > 0.0F) {
                    check(minDot < Math.cos(Math.toRadians(spread * 0.5)), "the scatter fills the cone, not just its centre");
                }
            }
        }
    }

    /** {minX, minY, minZ, maxX, maxY, maxZ} of the quads on one side of the body (x sign). */
    private static float[] extent(List<WingRig.Quad> quads, int side) {
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (WingRig.Quad quad : quads) {
            float[] xyz = quad.xyz();
            float centre = (xyz[0] + xyz[3] + xyz[6] + xyz[9]) / 4.0F;
            if (side > 0 ? centre < 0.0F : centre > 0.0F) {
                continue;
            }
            for (int corner = 0; corner < 4; corner++) {
                for (int axis = 0; axis < 3; axis++) {
                    box[axis] = Math.min(box[axis], xyz[corner * 3 + axis]);
                    box[axis + 3] = Math.max(box[axis + 3], xyz[corner * 3 + axis]);
                }
            }
        }
        return box;
    }

    /** Quads of the kakuja for one frame at rest (or moving with {@code body}), mastery 800. */
    private static List<WingRig.Quad> kakujaQuads(double time, KakujaRig.Body body, float deploy, float detail, boolean crouching) {
        return KakujaRig.build(new WingRig.Input(WingForm.KAKUJA, 800, deploy, time, 0.0F, 0.0F, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F,
            crouching, detail, 1.0F, false), null, body, null);
    }

    private static void kakuja() {
        // The model: finite, unit normals, the size of the art, off the floor, coming out in turn.
        List<WingRig.Quad> rest = kakujaQuads(1.3, KakujaRig.Body.STILL, 1.0F, 1.0F, false);
        boolean finite = !rest.isEmpty();
        boolean unit = true;
        for (WingRig.Quad quad : rest) {
            for (float v : quad.xyz()) {
                finite &= Float.isFinite(v);
            }
            unit &= Math.abs(Math.sqrt(quad.nx() * quad.nx() + quad.ny() * quad.ny() + quad.nz() * quad.nz()) - 1.0) < 1.0E-3;
        }
        check(finite && unit, "the kakuja is finite with unit normals");
        float[] e = extent(rest);
        check(e[4] > 2.4F && e[4] < 5.0F && e[0] < -2.3F && e[3] > 2.3F, "six big tentacles spread over and beside him: " + java.util.Arrays.toString(e));
        check(rest.size() < 2800, "the kakuja stays within its quad budget: " + rest.size());
        check(kakujaQuads(1.3, KakujaRig.Body.STILL, 1.0F, 0.3F, false).size() < rest.size() / 2, "far away it costs far fewer quads");
        check(kakujaQuads(1.3, KakujaRig.Body.STILL, 0.0F, 1.0F, false).isEmpty(), "nothing out, nothing drawn");
        check(KakujaRig.deployOf(0.12F, 1, KakujaRig.UPPER) > 0.0 && KakujaRig.deployOf(0.12F, 1, KakujaRig.LOWER) == 0.0
            && KakujaRig.deployOf(1.0F, -1, KakujaRig.LOWER) == 1.0, "the upper pair comes out first, every one fully out at the end");
        // 5.0.9: coming out, they push out of his back. Two stubs side by side at their sockets were thrown apart by the clearance
        // of full-grown tentacles, the newer one flung about a block away, down by his feet (in the air, below them).
        float lowestOut = Float.MAX_VALUE;
        for (float deploy = 0.005F; deploy <= 0.35F; deploy += 0.0025F) {
            for (double time : new double[] {1.3, 5.6, 17.0, 41.2}) {
                for (boolean finalForm : new boolean[] {false, true}) {
                    WingRig.Input in = new WingRig.Input(WingForm.KAKUJA, 1000, deploy, time, 0.0F, 0.0F, WingRig.Motion.NONE, 0.0F, 0.4F,
                        0.0F, 0.0F, false, 1.0F, 1.0F, false);
                    KakujaRig.Limb[] limbs = KakujaRig.pose(in, KakujaRig.Body.STILL);
                    com.hex.kagune.control.ayato.KakujaModel.Spine[] spines = KakujaRig.spines(in, KakujaRig.Body.STILL, limbs);
                    List<WingRig.Quad> drawn = new ArrayList<>();
                    KakujaRig.draw(drawn, in, spines, limbs, null, false, null, finalForm);
                    lowestOut = Math.min(lowestOut, extent(drawn)[1]);
                }
            }
        }
        check(lowestOut > 0.8F, "coming out, every tentacle stays at his back, never down by his feet: " + lowestOut);
        KakujaRig.Limb emerging = new KakujaRig.Limb(1, KakujaRig.UPPER, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0);
        com.hex.kagune.control.ayato.KakujaModel.Spine whole = KakujaRig.spine(emerging, false, 1.0);
        com.hex.kagune.control.ayato.KakujaModel.Spine half = KakujaRig.spine(emerging, false, 0.5);
        double[] halfTip = half.p()[half.samples()];
        double[] wholeMiddle = whole.p()[whole.samples() / 2];
        double[] gap = {halfTip[0] - wholeMiddle[0], halfTip[1] - wholeMiddle[1], halfTip[2] - wholeMiddle[2]};
        check(length(gap) < 0.03, "half out, a tentacle is the first half of its own curve (it slides out along it): " + length(gap));
        com.hex.kagune.control.ayato.KakujaModel.Spine stub = KakujaRig.spine(emerging, false, 0.1);
        double[] chord = {stub.p()[stub.samples()][0] - stub.p()[0][0], stub.p()[stub.samples()][1] - stub.p()[0][1],
            stub.p()[stub.samples()][2] - stub.p()[0][2]};
        double[] out = stub.t()[0];
        double bend = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0,
            (chord[0] * out[0] + chord[1] * out[1] + chord[2] * out[2]) / length(chord)))));
        check(bend < 8.0, "a tentacle just coming out points straight out of his back, not curled up: " + bend + " degrees");
        float lowest = Float.MAX_VALUE;
        for (double time = 0.0; time < 30.0; time += 0.53) {
            for (float run : new float[] {0.0F, 0.5F, 1.0F}) {
                KakujaRig.Body body = new KakujaRig.Body(0.0F, 0.0F, 0.0F, 0.0F, run, (float)(time * 5.0), 0.0F, false);
                lowest = Math.min(lowest, extent(kakujaQuads(time, body, 1.0F, 0.6F, false))[1]);
            }
        }
        check(lowest > -0.12F, "standing or running, no tentacle goes into the floor: " + lowest);

        // Alive: every tentacle moves on its own, and every one stretches now and then (but rests most of the time).
        int[] stretched = new int[KakujaRig.TENTACLES];
        int samples = 0;
        boolean bounded = true;
        for (double time = 0.0; time < 90.0; time += 0.05) {
            samples++;
            for (int i = 0; i < KakujaRig.TENTACLES; i++) {
                double s = KakujaRig.stretch(i, time);
                bounded &= s >= 0.0 && s <= 1.0;
                if (s > 0.5) {
                    stretched[i]++;
                }
            }
        }
        boolean everyone = true;
        boolean mostlyStill = true;
        for (int count : stretched) {
            everyone &= count > 0;
            mostlyStill &= count < samples * 0.25;
        }
        check(bounded && everyone && mostlyStill, "each tentacle stretches now and then: " + java.util.Arrays.toString(stretched) + " of " + samples);
        KakujaRig.Limb a = KakujaRig.alive(1, KakujaRig.UPPER, 10.0, 1.0);
        KakujaRig.Limb later = KakujaRig.alive(1, KakujaRig.UPPER, 11.0, 1.0);
        KakujaRig.Limb other = KakujaRig.alive(-1, KakujaRig.UPPER, 10.0, 1.0);
        check(Math.abs(a.curlDown() - later.curlDown()) + Math.abs(a.elevation() - later.elevation()) > 0.5, "a tentacle at rest keeps moving");
        check(Math.abs(a.curlDown() - other.curlDown()) + Math.abs(a.sweep() - other.sweep()) > 0.5, "the two sides don't move in lockstep");

        // Answering the body: running streams them back, the torso carries them, the floor holds them up.
        KakujaRig.Limb still = KakujaRig.moving(a, KakujaRig.Body.STILL, false);
        KakujaRig.Limb running = KakujaRig.moving(a, new KakujaRig.Body(0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, false), false);
        check(running.sweep() > still.sweep() + 15.0, "running sweeps them back");
        double[] root = {0.12, 1.36, -0.2};
        double[] leaning = KakujaRig.torsoPoint(root, new KakujaRig.Body(0.4F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, false), 0.0);
        double[] turned = KakujaRig.torsoPoint(root, new KakujaRig.Body(0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, false), 0.0);
        check(leaning[2] > root[2] + 0.02 && leaning[1] > root[1] - 0.02, "leaning forward carries the roots forward with the shoulders");
        check(turned[2] < root[2] - 0.03, "the chest turning right takes the right shoulder blade back");
        KakujaRig.Limb sunk = new KakujaRig.Limb(1, KakujaRig.LOWER, -60.0, 0.0, 0.0, 60.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0);
        com.hex.kagune.control.ayato.KakujaModel.Spine deep = KakujaRig.spine(sunk, false, 1.0);
        com.hex.kagune.control.ayato.KakujaModel.Spine held = KakujaRig.floor(deep, 1);
        double deepest = Double.MAX_VALUE;
        double heldLowest = Double.MAX_VALUE;
        for (int i = 0; i < deep.p().length; i++) {
            deepest = Math.min(deepest, deep.p()[i][1]);
            heldLowest = Math.min(heldLowest, held.p()[i][1]);
        }
        check(deepest < 0.0 && heldLowest > 0.1, "a tentacle driven into the ground is pressed along it instead: " + deepest + " -> " + heldLowest);

        // Neighbours never pass through each other: idle (stretches included), running, jumping, falling, crouching.
        double closest = Double.MAX_VALUE;
        for (double time = 0.0; time < 60.0; time += 0.2) {
            for (int mode = 0; mode < 5; mode++) {
                KakujaRig.Body body = new KakujaRig.Body(0.0F, 0.0F, 0.0F, 0.0F, mode == 1 ? 1.0F : 0.0F, (float)(time * 5.0),
                    mode == 2 ? 0.42F : mode == 3 ? -0.6F : 0.0F, mode == 2 || mode == 3);
                WingRig.Input in = new WingRig.Input(WingForm.KAKUJA, 800, 1.0F, time, 0.0F, 0.0F, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F,
                    mode == 4, 1.0F, 1.0F, false);
                KakujaRig.Limb[] limbs = KakujaRig.pose(in, body);
                com.hex.kagune.control.ayato.KakujaModel.Spine[] spines = KakujaRig.spines(in, body, limbs);
                KakujaRig.separate(spines, limbs);
                closest = Math.min(closest, kakujaGap(spines));
            }
        }
        check(closest > -0.03, "neighbouring tentacles never pass through each other (spike rows included): " + closest);

        // The kit: volleys of needles from tentacle pairs, faster than any wing, the rows turned on the target.
        check(AyatoRules.style(800, WingForm.KAKUJA, false, false) == ComboStyle.AYATO_KAKUJA
            && AyatoRules.style(800, WingForm.KAKUJA, false, true) == ComboStyle.AYATO_KAKUJA, "the kakuja fights with its needles");
        check(ComboStyle.AYATO_KAKUJA.ayatoShots() && ComboStyle.AYATO_KAKUJA.stageCount() == 5, "five volleys, then a right click");
        check(ComboStyle.AYATO_KAKUJA.ayatoEnders() == ComboStyle.AYATO_KAKUJA_ENDERS && ComboStyle.AYATO_KAKUJA_ENDERS.ayatoEnderSet()
            && ComboStyle.AYATO_KAKUJA_ENDERS.stageCount() == 6, "the right click after the volleys is one of the kakuja's six enders");
        com.hex.kagune.control.StrikePattern[] enders = {com.hex.kagune.control.StrikePattern.HARROW, com.hex.kagune.control.StrikePattern.PIERCING_LINE,
            com.hex.kagune.control.StrikePattern.THORN_FAN, com.hex.kagune.control.StrikePattern.HAIL_OF_THORNS,
            com.hex.kagune.control.StrikePattern.SIX_TAIL_BARRAGE, com.hex.kagune.control.StrikePattern.CRIMSON_BLOOM};
        boolean enderSet = true;
        for (int shots = 0; shots <= 5; shots++) {
            com.hex.kagune.control.StrikePattern pattern = ComboStyle.AYATO_KAKUJA_ENDERS.stage(AyatoRules.ender(shots, 1000, true)).pattern();
            enderSet &= pattern == enders[shots] && pattern.kakuja() && pattern.ender() && pattern.crystal() && !pattern.shot();
        }
        check(enderSet, "at 1000 every volley count has its own kakuja ender (Harrow .. Crimson Bloom)");
        boolean order = true;
        int[] seen = new int[KakujaRig.TENTACLES];
        for (int shot = 0; shot < 8; shot++) {
            int index = AyatoRules.sixTailTentacle(shot);
            seen[index]++;
            order &= shot == 0 || KakujaRig.sideOf(index) != KakujaRig.sideOf(AyatoRules.sixTailTentacle(shot - 1));
        }
        for (int count : seen) {
            order &= count > 0;
        }
        check(order, "the barrage goes round all six, the sides taking turns (never all at once)");
        // Rushing Death: a move of its own; a full backflip up to the throw at the top, twelve needles, a cooldown.
        com.hex.kagune.control.ComboStage rush = ComboStyle.AYATO_RUSHING_DEATH.stage(0);
        check(ComboStyle.AYATO_RUSHING_DEATH.stageCount() == 1 && !ComboStyle.AYATO_RUSHING_DEATH.ayatoShots()
            && rush.pattern() == com.hex.kagune.control.StrikePattern.RUSHING_DEATH && rush.pattern().kakuja() && rush.pattern().ender()
            && Math.abs(rush.impactFraction() - AyatoBody.RUSH_THROW) < 1.0E-6, "Rushing Death is one move, its throw on the body's throw");
        float midFlip = AyatoBody.pose(com.hex.kagune.control.StrikePattern.RUSHING_DEATH, true, true, 0.22F, rush.impactFraction()).flip;
        float atThrow = AyatoBody.pose(com.hex.kagune.control.StrikePattern.RUSHING_DEATH, true, true, AyatoBody.RUSH_THROW, rush.impactFraction()).flip;
        check(midFlip > 1.0F && midFlip < (float)(Math.PI * 2.0) && atThrow == 0.0F, "a full backflip, done by the throw at the top: " + midFlip);
        check(AyatoRules.RUSH_NEEDLES_PER_TENTACLE * KakujaRig.TENTACLES == 12 && AyatoRules.RUSH_COOLDOWN >= 100,
            "twelve needles at once, two per tentacle, on a cooldown");
        // Six blocks back and up: the launch carries him well clear (air drag 0.91 a tick, gravity 0.08).
        double vx = AyatoRules.RUSH_BACK;
        double vy = AyatoRules.RUSH_UP;
        double x = 0.0;
        double y = 0.0;
        double top = 0.0;
        for (int tick = 0; tick < 60 && y >= 0.0; tick++) {
            x += vx;
            y += vy;
            top = Math.max(top, y);
            vx *= 0.91;
            vy = (vy - 0.08) * 0.98;
        }
        check(x >= 6.0 && top >= 4.0, "the backflip carries him at least six blocks back and high up: " + x + " back, " + top + " up");
        // The Needle Gatling: two needles a salvo, one tentacle at a time, faster and faster, dearer the longer it runs.
        check(AyatoRules.needleGatlingRate(AyatoRules.GATLING_SPINUP - 1) == 0.0
            && AyatoRules.needleGatlingRate(AyatoRules.GATLING_SPINUP) >= AyatoRules.NEEDLE_GATLING_START_RATE - 1.0E-9
            && AyatoRules.needleGatlingRate(400) == AyatoRules.NEEDLE_GATLING_FULL_RATE
            && AyatoRules.needleGatlingRate(30) > AyatoRules.needleGatlingRate(10), "the Needle Gatling spins up, then speeds up to full");
        check(AyatoRules.NEEDLE_GATLING_PER_SALVO == 2 && AyatoRules.NEEDLE_GATLING_FULL_RATE * AyatoRules.NEEDLE_GATLING_PER_SALVO
            > AyatoRules.GATLING_FULL_RATE, "two needles per tentacle a salvo, and more needles than the wings' stream at full spin");
        check(AyatoRules.needleGatlingDrain(100, 800) > AyatoRules.needleGatlingDrain(10, 800)
            && AyatoRules.needleGatlingDrain(10, 800) > AyatoRules.gatlingDrain(10, 800)
            && AyatoRules.needleGatlingDamage(800) < AyatoRules.gatlingDamage(800), "it drains more the longer it runs; each needle is lighter");
        boolean pairs = true;
        for (int stage = 0; stage < 5; stage++) {
            com.hex.kagune.control.ComboStage volley = ComboStyle.AYATO_KAKUJA.stage(stage);
            int tier = stage % 3;
            pairs &= volley.pattern() == com.hex.kagune.control.StrikePattern.NEEDLE_VOLLEY && volley.usesLimb(20 + tier) && volley.usesLimb(23 + tier)
                && volley.pattern().shot() && volley.pattern().crystal() && volley.pattern().kakuja() && !volley.pattern().ender();
        }
        check(pairs, "each volley fires from a pair of tentacles: upper, middle, lower, upper, middle");
        check(AyatoRules.kakujaCadence(800) < AyatoRules.crystalCadence(800) && AyatoRules.kakujaCadence(800) < AyatoRules.flameCadence(800)
            && AyatoRules.needleSpeed(800) > AyatoRules.crystalSpeed(1000) && AyatoRules.NEEDLE_RANGE > AyatoRules.crystalRange(1000),
            "needles fly faster and further, and the volleys come faster, than anything the wings fire");
        boolean facing = true;
        boolean ahead = true;
        for (int i = 0; i < KakujaRig.TENTACLES; i++) {
            KakujaRig.Limb steady = new KakujaRig.Limb(KakujaRig.sideOf(i), KakujaRig.tierOf(i), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0);
            com.hex.kagune.control.ayato.KakujaModel.Spine spine = KakujaRig.spine(KakujaRig.aiming(steady, 1.0F, WingRig.FORWARD, 0.0F), false, 1.0);
            // The plates' back (dorsal) faces away from the target over the middle of the tentacle: the spike rows face it.
            double[] dorsal = spine.n()[spine.n().length / 2];
            facing &= dorsal[2] < -0.5;
            for (double[] muzzle : KakujaRig.muzzles(i, WingRig.FORWARD, false, 3)) {
                ahead &= Double.isFinite(muzzle[0]) && Double.isFinite(muzzle[1]) && Double.isFinite(muzzle[2]) && muzzle[2] > -0.6 && muzzle[1] > 0.0;
            }
        }
        check(facing, "turned to fire, every tentacle's spike rows face where he looks");
        check(ahead, "needles leave from the spike points, around and in front of him, never from inside him or the floor");
        int[] needleQuads = {0};
        boolean needleFinite = true;
        List<double[]> needlePoints = new ArrayList<>();
        com.hex.kagune.control.ayato.KakujaNeedle.build((p, q, r, t, ca, cb, cc, cd) -> {
            needleQuads[0]++;
            needlePoints.add(p);
            needlePoints.add(r);
        }, new double[] {0.0, 0.0, 0.0}, new double[] {0.0, 0.0, 1.0}, 1.0, new double[] {2.0, 1.0, -3.0}, 1.0, 0.5, 1.0, 3, true, 1.0);
        double longest = 0.0;
        for (double[] point : needlePoints) {
            needleFinite &= Double.isFinite(point[0]) && Double.isFinite(point[1]) && Double.isFinite(point[2]);
            longest = Math.max(longest, Math.abs(point[2]));
        }
        check(needleFinite && needleQuads[0] > 0 && needleQuads[0] <= com.hex.kagune.control.ayato.KakujaNeedle.QUADS,
            "a needle is well formed within its quad budget (" + needleQuads[0] + ")");
        check(com.hex.kagune.control.ayato.KakujaNeedle.RADIUS < CrystalSpike.RADIUS * 0.5
            && com.hex.kagune.control.ayato.KakujaNeedle.LENGTH < CrystalSpike.LENGTH, "a needle is thinner and smaller than a crystal spike");

        // The spring chain: still is still, a jolt runs out to the claw and settles, a teleport doesn't fling them.
        KakujaMotion motion = new KakujaMotion();
        double[][][] goal = KakujaRig.nodes(KakujaRig.spines(new WingRig.Input(WingForm.KAKUJA, 800, 1.0F, 1.0, 0.0F, 0.0F, WingRig.Motion.NONE, 0.0F,
            0.4F, 0.0F, 0.0F, false, 1.0F, 1.0F, false), KakujaRig.Body.STILL, KakujaRig.pose(new WingRig.Input(WingForm.KAKUJA, 800, 1.0F, 1.0,
            0.0F, 0.0F, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, 1.0F, 1.0F, false), KakujaRig.Body.STILL)));
        double[][][] lag = motion.step(goal, 0.0);
        lag = motion.step(goal, 20.0);
        check(largest(lag) < 1.0E-6, "a body standing still leaves the tentacles where the pose has them");
        double[][][] moved = shifted(goal, 1.0);
        lag = motion.step(moved, 21.0);
        double tipLag = length(lag[0][KakujaRig.NODES]);
        double rootLag = length(lag[0][0]);
        check(rootLag < 1.0E-9 && tipLag > 0.3, "a sudden step leaves the claws behind, the root stays on his back: " + tipLag);
        double worst = 0.0;
        boolean sane = true;
        for (int tick = 22; tick < 220; tick++) {
            lag = motion.step(moved, tick);
            for (double[][] limb : lag) {
                for (double[] point : limb) {
                    sane &= Double.isFinite(point[0]) && Double.isFinite(point[1]) && Double.isFinite(point[2]);
                }
            }
            worst = Math.max(worst, largest(lag));
        }
        check(sane && worst < 2.4, "the chains stay finite and bounded: " + worst);
        check(largest(lag) < 0.02, "and settle back into the pose: " + largest(lag));
        lag = motion.step(shifted(goal, 40.0), 221.0);
        check(largest(lag) < 1.0E-9, "a teleport starts the chains again instead of flinging them");

        // The armor: horns, a mask with eye holes where the eyes are, the plated chest.
        List<double[]> centres = new ArrayList<>();
        com.hex.kagune.control.ayato.UkakuModel.Sink collect = (p, q, r, t, n, ca, cb, cc, cd) -> centres.add(new double[] {
            (p[0] + q[0] + r[0] + t[0]) / 4.0, (p[1] + q[1] + r[1] + t[1]) / 4.0, (p[2] + q[2] + r[2] + t[2]) / 4.0});
        com.hex.kagune.control.ayato.KakujaModel.mask(collect, new int[] {1, 4, 5, 4}, 1.0, 1.0);
        boolean holes = !centres.isEmpty();
        for (double[] c : centres) {
            // The eyes' centres (default skin): face columns 1-2 and 5-6, rows 4-5.
            holes &= !(Math.abs(c[1] + 3.0) < 0.6 && (Math.abs(c[0] + 2.0) < 0.6 || Math.abs(c[0] - 2.0) < 0.6) && c[2] < -4.0);
        }
        check(holes, "the mask leaves the eyes uncovered (the kakugan shows through)");
        centres.clear();
        com.hex.kagune.control.ayato.KakujaModel.horns(collect, 1.0, 1.0);
        boolean above = !centres.isEmpty();
        for (double[] c : centres) {
            above &= c[1] < -6.5 && Double.isFinite(c[0]);
        }
        check(above, "the horns rise from the top of the head");
        centres.clear();
        com.hex.kagune.control.ayato.KakujaModel.chest(collect, 1.0, 1.0);
        boolean onTorso = centres.size() > 40;
        for (double[] c : centres) {
            onTorso &= Math.abs(c[0]) < 4.6 && c[1] > -0.5 && c[1] < 12.5 && Math.abs(c[2]) < 3.5;
        }
        check(onTorso, "the chest plates sit on the torso, front and back");
        finalKakuja();
        finalKit();
        kakujaLocomotion();
    }

    /** The final kakuja's model: armored right, fire left, within budget, and the half mask under the eyes. */
    private static void finalKakuja() {
        WingRig.Input in = new WingRig.Input(WingForm.KAKUJA, 1000, 1.0F, 1.3, 0.0F, 0.0F, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false,
            1.0F, 1.0F, false);
        double[] eye = {1.5, 2.0, 8.0};
        List<WingRig.Quad> quads = KakujaRig.build(in, eye, KakujaRig.Body.STILL, KakujaRig.Action.NONE, null, true);
        boolean finite = !quads.isEmpty();
        for (WingRig.Quad quad : quads) {
            for (float v : quad.xyz()) {
                finite &= Float.isFinite(v);
            }
            finite &= Math.abs(Math.sqrt(quad.nx() * quad.nx() + quad.ny() * quad.ny() + quad.nz() * quad.nz()) - 1.0) < 1.0E-3;
        }
        check(finite, "the final kakuja is finite with unit normals");
        check(quads.size() < 3200, "the final kakuja stays within its quad budget: " + quads.size());
        WingRig.Input far = new WingRig.Input(WingForm.KAKUJA, 1000, 1.0F, 1.3, 0.0F, 0.0F, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false,
            0.3F, 1.0F, false);
        check(KakujaRig.build(far, eye, KakujaRig.Body.STILL, KakujaRig.Action.NONE, null, true).size() < quads.size() * 0.6,
            "far away the final kakuja costs far fewer quads");
        float[] e = extent(quads);
        check(e[0] < -2.0F && e[3] > 2.0F && e[4] > 2.4F, "both halves spread beside and over him: " + java.util.Arrays.toString(e));
        // The fire is translucent gas (every fire quad glows, lit from straight above); the armor is shaded plate.
        int leftGlow = 0;
        int left = 0;
        int rightShaded = 0;
        for (WingRig.Quad quad : quads) {
            float[] v = quad.xyz();
            float x = (v[0] + v[3] + v[6] + v[9]) / 4.0F;
            boolean glow = quad.ny() == 1.0F && quad.nx() == 0.0F && quad.nz() == 0.0F;
            if (x < -0.6F) {
                left++;
                leftGlow += glow ? 1 : 0;
            } else if (x > 0.6F && !glow) {
                rightShaded++;
            }
        }
        check(left > 200 && leftGlow > left * 0.95 && rightShaded > 300, "fire tendrils on his left, armored plate on his right (" + leftGlow + "/" + left
            + " glowing, " + rightShaded + " plates)");
        check(FireTendril.widthAt(0.0) > 0.0 && FireTendril.widthAt(1.0) < 0.02 && FireTendril.widthAt(0.4) > FireTendril.widthAt(0.9),
            "a fire tendril tapers to a wisp");
        check(KakujaRig.build(in, null, KakujaRig.Body.STILL, KakujaRig.Action.NONE, null, true).size() == quads.size(),
            "without a camera it draws the same geometry (shaded half on)");

        // The half mask: below the eyes wherever they are, in front of the face, on the head; the cheek wings sweep back.
        com.hex.kagune.control.ayato.UkakuModel.Sink[] holder = new com.hex.kagune.control.ayato.UkakuModel.Sink[1];
        for (int[] eyes : new int[][] {{1, 4, 5, 4}, {2, 5, 4, 5}, {0, 3, 6, 3}}) {
            List<double[]> centres = new ArrayList<>();
            double[] back = {-9.0};
            holder[0] = (p, q, r, t, n, ca, cb, cc, cd) -> {
                centres.add(new double[] {(p[0] + q[0] + r[0] + t[0]) / 4.0, (p[1] + q[1] + r[1] + t[1]) / 4.0, (p[2] + q[2] + r[2] + t[2]) / 4.0});
                back[0] = Math.max(back[0], Math.max(Math.max(p[2], q[2]), Math.max(r[2], t[2])));
            };
            com.hex.kagune.control.ayato.KakujaModel.halfMask(holder[0], eyes, 1.0, 1.0);
            boolean clear = centres.size() > 150;
            boolean outside = true;
            for (double[] c : centres) {
                for (int k = 0; k < 2; k++) {
                    // Never over an eye (two pixels each, as KakuganEyes).
                    double ex = eyes[k * 2] - 4.0 + 1.0;
                    double ey = eyes[k * 2 + 1] - 8.0 + 1.0;
                    clear &= !(Math.abs(c[0] - ex) < 1.0 && Math.abs(c[1] - ey) < 1.0 && c[2] < -3.0);
                }
                // Outside the head and its skin's outer layer (-4.5 .. 4.5) wherever it is in front of the face.
                outside &= !(Math.abs(c[0]) < 4.5 && c[2] > -4.55 && c[2] < 0.0 && c[1] > -8.0 && c[1] < 0.0);
            }
            check(clear, "the half mask leaves the eyes bare " + java.util.Arrays.toString(eyes));
            check(outside, "the half mask sits outside the face " + java.util.Arrays.toString(eyes));
            check(back[0] > 1.0, "its cheek wings sweep back along the side of the head " + java.util.Arrays.toString(eyes) + ": " + back[0]);
        }
        // 6.0.2: fitted to the face. Over the bare face, the outer layer or 3D Skin Layers' voxels, nothing of it goes inside what it
        // covers, its front lies just off it, and it is no wider than that (the cheek wings aside, which sweep back from the jaw).
        for (double shell : new double[] {com.hex.kagune.control.ayato.KakujaModel.FACE, com.hex.kagune.control.ayato.KakujaModel.OUTER_LAYER,
            com.hex.kagune.control.ayato.KakujaModel.VOXEL_LAYER}) {
            double half = shell + com.hex.kagune.control.ayato.KakujaModel.MASK_GAP;
            for (int[] eyes : new int[][] {{1, 4, 5, 4}, {2, 5, 4, 5}, {0, 3, 6, 3}}) {
                // Every corner and every facet's middle (a facet cutting a corner shows in its middle).
                List<double[]> corners = new ArrayList<>();
                com.hex.kagune.control.ayato.KakujaModel.halfMask((p, q, r, t, n, ca, cb, cc, cd) -> {
                    corners.add(p);
                    corners.add(q);
                    corners.add(r);
                    corners.add(t);
                    corners.add(new double[] {(p[0] + q[0] + r[0] + t[0]) / 4.0, (p[1] + q[1] + r[1] + t[1]) / 4.0, (p[2] + q[2] + r[2] + t[2]) / 4.0});
                }, eyes, 1.0, 1.0, shell);
                boolean outside = !corners.isEmpty();
                boolean hugs = true;
                int front = 0;
                double widest = 0.0;
                for (double[] c : corners) {
                    double chin = shell - 4.0;
                    outside &= !(Math.abs(c[0]) < shell - 0.02 && c[2] > -shell + 0.02 && c[2] < shell && c[1] > -8.0 && c[1] < chin - 0.02);
                    if (Math.abs(c[0]) > 2.7 && Math.abs(c[0]) < 3.6 && c[1] < Math.min(chin, 0.0) - 0.3 && c[2] < 0.0) {
                        front++;
                        // (Within a tenth of a pixel of its surface: the vents' glow stands a hair proud of it.)
                        hugs &= c[2] >= -(half + 0.1) && c[2] <= -shell - 0.02;
                    }
                    if (c[2] < -2.8) {
                        widest = Math.max(widest, Math.abs(c[0]));
                    }
                }
                String at = shell + " " + java.util.Arrays.toString(eyes);
                check(outside, "the half mask never goes inside what it is fitted over (" + at + ")");
                check(front > 20 && hugs, "the half mask's front lies just off the face it covers (" + at + ")");
                check(widest <= half + 1.0E-6, "the half mask is no wider than the head it covers (" + at + ": " + widest + ")");
            }
        }
        int[] lowEyes = {1, 4, 5, 4};
        long hair = 0L;
        for (int row = 0; row < 5; row++) {
            hair |= 0xFFL << (row * 8);
        }
        long beard = com.hex.kagune.control.kakugan.KakuganSkin.bit(3, 6);
        check(com.hex.kagune.control.ayato.KakujaModel.halfMaskShell(lowEyes, 0L, true, false) == com.hex.kagune.control.ayato.KakujaModel.FACE
            && com.hex.kagune.control.ayato.KakujaModel.halfMaskShell(lowEyes, hair, true, true) == com.hex.kagune.control.ayato.KakujaModel.FACE,
            "a lower face the second layer leaves bare: the half mask on the face itself (hair and brows above it don't count)");
        check(com.hex.kagune.control.ayato.KakujaModel.halfMaskShell(lowEyes, beard, true, false) == com.hex.kagune.control.ayato.KakujaModel.OUTER_LAYER
            && com.hex.kagune.control.ayato.KakujaModel.halfMaskShell(lowEyes, beard, true, true) == com.hex.kagune.control.ayato.KakujaModel.VOXEL_LAYER,
            "a second layer over the lower face: over it, as vanilla or 3D Skin Layers draws it");
        check(com.hex.kagune.control.ayato.KakujaModel.halfMaskShell(lowEyes, 0L, false, false) == com.hex.kagune.control.ayato.KakujaModel.OUTER_LAYER
            && com.hex.kagune.control.ayato.KakujaModel.halfMaskShell(lowEyes, 0L, false, true) == com.hex.kagune.control.ayato.KakujaModel.VOXEL_LAYER,
            "a skin not read yet counts as covered");
        List<double[]> grown = new ArrayList<>();
        com.hex.kagune.control.ayato.KakujaModel.halfMask((p, q, r, t, n, ca, cb, cc, cd) -> grown.add(p), new int[] {1, 4, 5, 4}, 0.3, 1.0);
        List<double[]> full = new ArrayList<>();
        com.hex.kagune.control.ayato.KakujaModel.halfMask((p, q, r, t, n, ca, cb, cc, cd) -> full.add(p), new int[] {1, 4, 5, 4}, 1.0, 1.0);
        check(!grown.isEmpty() && grown.size() < full.size(), "the half mask spreads as it grows");
    }

    /** The final kakuja's own kit: needles from the right three, fire from the left three, every number from AyatoRules. */
    private static void finalKit() {
        ComboStyle shots = ComboStyle.AYATO_FINAL_KAKUJA;
        ComboStyle enders = ComboStyle.AYATO_FINAL_ENDERS;
        StrikePattern[] m1 = {StrikePattern.NEEDLE_PAIR, StrikePattern.FIRE_LASH, StrikePattern.NEEDLE_FAN, StrikePattern.FIREBALL, StrikePattern.CROSSFIRE};
        StrikePattern[] m2 = {StrikePattern.EMBER_HOP, StrikePattern.SEARING_HARROW, StrikePattern.TWIN_SERPENTS, StrikePattern.PYRE_LANCE,
            StrikePattern.HELLRAIN, StrikePattern.CRIMSON_PYRE};
        check(shots.stageCount() == m1.length && enders.stageCount() == m2.length, "five shots and six enders");
        check(shots.ayatoShots() && shots.kakujaShots() && shots.ayatoEnders() == enders && enders.ayatoEnderSet() && !shots.ayatoEnderSet(),
            "the shots chain into their own enders, with the kakuja's mastery table");
        // Which sides fire in each shot: needles right (0..2), fire left (3..5), both in the Crossfire.
        boolean[][] sides = {{true, false}, {false, true}, {true, false}, {false, true}, {true, true}};
        int[] needles = {2 * AyatoRules.VOLLEY_NEEDLES, 0, AyatoRules.NEEDLE_FAN_NEEDLES, 0, 3 * AyatoRules.CROSSFIRE_NEEDLES};
        int[] fire = {0, AyatoRules.LASH_SHARDS, 0, 2, 3};
        for (int i = 0; i < m1.length; i++) {
            ComboStage stage = shots.stage(i);
            check(stage.pattern() == m1[i] && m1[i].shot() && m1[i].finalKakuja() && m1[i].kakuja() && m1[i].flame() && !m1[i].ender(),
                "shot " + i + " is " + m1[i]);
            int mask = 0;
            for (int limb : stage.limbs()) {
                mask |= 1 << limb;
            }
            int right = 0;
            int left = 0;
            boolean before = true;
            for (int index = 0; index < KakujaRig.TENTACLES; index++) {
                int[] ticks = AyatoRules.finalFireTicks(m1[i], index, mask);
                for (int tick : ticks) {
                    before &= tick >= 0 && tick < AyatoRules.finalCadence(m1[i]);
                }
                if (index < 3) {
                    right += ticks.length;
                } else {
                    left += ticks.length;
                }
            }
            check(right == needles[i] && left == fire[i], m1[i] + " fires " + right + " needles right and " + left + " fire left");
            check((right > 0) == sides[i][0] && (left > 0) == sides[i][1], m1[i] + " fires from the side the pitch says");
            check(before, m1[i] + ": every needle and shard is out before the next shot may start");
            check(AyatoRules.shotCadence(m1[i], 1000) == AyatoRules.finalCadence(m1[i]) && AyatoRules.finalCadence(m1[i]) >= 3,
                m1[i] + " keeps its own pace");
            check(AyatoRules.strikeRange(1000, WingForm.FINAL_KAKUJA, stage) == AyatoRules.NEEDLE_RANGE, m1[i] + " reaches as far as a needle");
        }
        for (int i = 0; i < m2.length; i++) {
            check(enders.stage(i).pattern() == m2[i] && m2[i].ender() && m2[i].finalKakuja() && !m2[i].shot(), "ender " + i + " is " + m2[i]);
            check(AyatoRules.ender(i, 1000, true) == i, "at 1000 the ender after " + i + " shots is " + m2[i]);
        }
        // Scorched wounds: a burning body bleeds twice as hard, only from the final kakuja.
        check(AyatoRules.scorchedStacks(1, true, true) == 2 && AyatoRules.scorchedStacks(2, true, true) == 4 && AyatoRules.scorchedStacks(1, false, true) == 1
            && AyatoRules.scorchedStacks(1, true, false) == 1, "Scorched wounds doubles the stacks on a burning body");
        // The Pyre Gatling: a needle salvo from a right tentacle, then fire from a tendril, round both sides.
        int[] used = new int[KakujaRig.TENTACLES];
        int fires = 0;
        for (int shot = 0; shot < 12; shot++) {
            int index = AyatoRules.pyreGatlingTentacle(shot);
            boolean flame = AyatoRules.pyreGatlingFire(shot);
            check(flame == (shot % 2 == 1) && (flame ? index >= 3 && index < 6 : index >= 0 && index < 3), "Pyre Gatling shot " + shot + " from " + index);
            used[index]++;
            fires += flame ? 1 : 0;
        }
        boolean everyOne = true;
        for (int count : used) {
            everyOne &= count == 2;
        }
        check(everyOne && fires == 6, "the Pyre Gatling takes turns: needles and fire alike, every tentacle in turn");
        check(AyatoRules.pyreGatlingFireDamage(1000) > AyatoRules.needleGatlingDamage(1000), "a Pyre Gatling fireball outweighs a needle");
        // Where the fire leaves: the left tendrils' tips, beside and above him, toward the aim.
        double[] ahead = {0.0, 0.0, 1.0};
        double[] rightward = {0.8, 0.0, 0.6};
        for (int index = 3; index < KakujaRig.TENTACLES; index++) {
            double[] mouth = KakujaRig.mouth(index, ahead, false);
            double[] turned = KakujaRig.mouth(index, rightward, false);
            double fromBack = Math.sqrt(mouth[0] * mouth[0] + (mouth[1] - WingRig.ROOT_Y) * (mouth[1] - WingRig.ROOT_Y) + mouth[2] * mouth[2]);
            check(mouth[0] < 0.0 && mouth[1] > 0.3 && fromBack > 1.0 && fromBack < 4.5, "fire tendril " + index + "'s mouth is out on his left: "
                + java.util.Arrays.toString(mouth));
            check(Math.abs(turned[0] - mouth[0]) + Math.abs(turned[2] - mouth[2]) > 0.05, "fire tendril " + index + "'s mouth follows the aim");
            check(KakujaRig.mouth(index, ahead, true)[1] < mouth[1], "crouching lowers fire tendril " + index + "'s mouth");
        }
        // The left three can aim on their own (the fire tendrils sweep and beat while the needles hold).
        WingRig.Input in = new WingRig.Input(WingForm.KAKUJA, 1000, 1.0F, 1.3, 0.0F, 0.0F, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false,
            1.0F, 1.0F, false);
        float[] all = new float[KakujaRig.TENTACLES];
        java.util.Arrays.fill(all, 1.0F);
        float[] none = new float[KakujaRig.TENTACLES];
        KakujaRig.Limb[] same = KakujaRig.pose(in, KakujaRig.Body.STILL, new KakujaRig.Action(ahead, all, none));
        KakujaRig.Limb[] split = KakujaRig.pose(in, KakujaRig.Body.STILL, new KakujaRig.Action(ahead, all, none, 0.0F, new double[] {0.0, -0.9, 0.44}));
        boolean rightSame = true;
        boolean leftMoved = true;
        for (int index = 0; index < KakujaRig.TENTACLES; index++) {
            if (index < 3) {
                rightSame &= same[index].equals(split[index]);
            } else {
                leftMoved &= split[index].elevation() < same[index].elevation() - 5.0;
            }
        }
        check(rightSame && leftMoved, "a separate left aim moves only the fire tendrils (here: beating down)");
    }

    /** The kakuja's body: runs, jumps and lands (ayato/KakujaBody), and the torso the tentacles and chest ride. */
    private static void kakujaLocomotion() {
        com.hex.kagune.control.ayato.KakujaBody.Pose still = com.hex.kagune.control.ayato.KakujaBody.pose(
            com.hex.kagune.control.ayato.KakujaBody.Motion.STILL);
        check(still.lean == 0.0F && still.drop == 0.0F && still.rightKnee == 0.0F && still.leftArmX == 0.0F, "standing still adds nothing");
        float leanRun = 0.0F;
        float kneeRun = 0.0F;
        float kneeWalk = 0.0F;
        boolean planted = true;
        for (int i = 0; i < 64; i++) {
            float stride = (float)(Math.PI * 2.0 * i / 64.0);
            com.hex.kagune.control.ayato.KakujaBody.Pose run = com.hex.kagune.control.ayato.KakujaBody.pose(
                new com.hex.kagune.control.ayato.KakujaBody.Motion(stride, 1.0F, 1.0F, false, 0.0F, 1000.0F, 0.0F));
            com.hex.kagune.control.ayato.KakujaBody.Pose walk = com.hex.kagune.control.ayato.KakujaBody.pose(
                new com.hex.kagune.control.ayato.KakujaBody.Motion(stride, 0.6F, 0.0F, false, 0.0F, 1000.0F, 0.0F));
            leanRun = Math.max(leanRun, run.lean);
            kneeRun = Math.max(kneeRun, Math.max(run.rightKnee, run.leftKnee));
            kneeWalk = Math.max(kneeWalk, Math.max(walk.rightKnee, walk.leftKnee));
            planted &= run.drop >= 0.0F && run.drop < 4.0F && Float.isFinite(run.twist);
        }
        check(leanRun > 0.2F && kneeRun > 1.2F && kneeWalk < kneeRun, "running leans in and drives the knees higher than walking");
        check(planted, "the body sinks only by what the bent standing leg takes up");
        com.hex.kagune.control.ayato.KakujaBody.Pose rising = com.hex.kagune.control.ayato.KakujaBody.pose(
            new com.hex.kagune.control.ayato.KakujaBody.Motion(0.0F, 0.0F, 0.0F, true, 0.4F, 1000.0F, 0.0F));
        com.hex.kagune.control.ayato.KakujaBody.Pose falling = com.hex.kagune.control.ayato.KakujaBody.pose(
            new com.hex.kagune.control.ayato.KakujaBody.Motion(0.0F, 0.0F, 0.0F, true, -0.6F, 1000.0F, 0.0F));
        check(rising.rightKnee > 1.0F && rising.rightArmX < -0.8F, "on the way up the legs tuck and the arms drive up");
        check(falling.rightArmZ > 0.6F && falling.leftArmZ < -0.6F && falling.rightKnee < rising.rightKnee, "on the way down the arms spread and the legs reach");
        com.hex.kagune.control.ayato.KakujaBody.Pose soft = com.hex.kagune.control.ayato.KakujaBody.pose(
            new com.hex.kagune.control.ayato.KakujaBody.Motion(0.0F, 0.0F, 0.0F, false, 0.0F, 2.0F, 0.3F));
        com.hex.kagune.control.ayato.KakujaBody.Pose hard = com.hex.kagune.control.ayato.KakujaBody.pose(
            new com.hex.kagune.control.ayato.KakujaBody.Motion(0.0F, 0.0F, 0.0F, false, 0.0F, 2.0F, 0.9F));
        com.hex.kagune.control.ayato.KakujaBody.Pose after = com.hex.kagune.control.ayato.KakujaBody.pose(
            new com.hex.kagune.control.ayato.KakujaBody.Motion(0.0F, 0.0F, 0.0F, false, 0.0F, com.hex.kagune.control.ayato.KakujaBody.LAND_TICKS, 0.9F));
        check(soft.rightKnee > 0.3F && hard.rightKnee > soft.rightKnee && hard.drop > soft.drop && hard.lean > 0.0F,
            "a landing folds the knees and sinks him, harder the faster he came down");
        check(after.rightKnee == 0.0F && after.drop == 0.0F, "and he is back up once it is soaked up");
        // The tentacles (rig space) and the chest (model space) ride the same bend.
        double[] v = {0.3, 0.7, -0.2};
        com.hex.kagune.control.ayato.KakujaRig.Body body = new com.hex.kagune.control.ayato.KakujaRig.Body(0.4F, 0.15F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
            false);
        double[] rig = KakujaRig.torsoDirection(v, body);
        double[] model = KakujaRig.bendModel(new double[] {-v[0], -v[1], -v[2]}, 0.4F, 0.15F);
        check(Math.abs(rig[0] + model[0]) + Math.abs(rig[1] + model[1]) + Math.abs(rig[2] + model[2]) < 1.0E-9,
            "the chest armor bends exactly as the tentacles' roots do");
        double[] up = KakujaRig.torsoDirection(new double[] {0.0, 1.0, 0.0}, new com.hex.kagune.control.ayato.KakujaRig.Body(0.5F, 0.0F, 0.0F, 0.0F,
            0.0F, 0.0F, 0.0F, false));
        check(up[2] > 0.4 && up[1] > 0.8, "leaning forward tips the upper body forward: " + java.util.Arrays.toString(up));
    }

    /** The smallest margin between neighbouring tentacles' centre lines beyond the clearance they need (negative: overlapping). */
    private static double kakujaGap(com.hex.kagune.control.ayato.KakujaModel.Spine[] spines) {
        double worst = Double.MAX_VALUE;
        for (int side = 1; side >= -1; side -= 2) {
            for (int tier = 0; tier < 2; tier++) {
                com.hex.kagune.control.ayato.KakujaModel.Spine a = spines[KakujaRig.index(side, tier)];
                com.hex.kagune.control.ayato.KakujaModel.Spine b = spines[KakujaRig.index(side, tier + 1)];
                for (int i = 0; i < a.p().length; i++) {
                    for (int j = 0; j < b.p().length; j++) {
                        double need = KakujaRig.clearance(tier, i / (double)(a.p().length - 1), tier + 1, j / (double)(b.p().length - 1));
                        if (need > 0.0) {
                            double[] d = {a.p()[i][0] - b.p()[j][0], a.p()[i][1] - b.p()[j][1], a.p()[i][2] - b.p()[j][2]};
                            worst = Math.min(worst, length(d) - need);
                        }
                    }
                }
            }
        }
        return worst;
    }

    private static double[][][] shifted(double[][][] goal, double dx) {
        double[][][] out = new double[goal.length][][];
        for (int i = 0; i < goal.length; i++) {
            out[i] = new double[goal[i].length][];
            for (int k = 0; k < goal[i].length; k++) {
                out[i][k] = new double[] {goal[i][k][0] + dx, goal[i][k][1], goal[i][k][2]};
            }
        }
        return out;
    }

    private static double largest(double[][][] lag) {
        double most = 0.0;
        for (double[][] limb : lag) {
            if (limb == null) {
                continue;
            }
            for (double[] point : limb) {
                most = Math.max(most, length(point));
            }
        }
        return most;
    }

    private static double length(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static void unlocks() {
        com.hex.kagune.control.ayato.AyatoUnlock[] all = com.hex.kagune.control.ayato.AyatoUnlock.values();
        boolean ordered = true;
        for (int i = 1; i < all.length; i++) {
            ordered &= all[i].mastery() > all[i - 1].mastery();
        }
        check(ordered, "the unlock track climbs: every step needs more mastery than the one before");
        check(all[0].mastery() == 0 && all[0].reached(0) && all[all.length - 1] == com.hex.kagune.control.ayato.AyatoUnlock.FINAL_KAKUJA
            && all[all.length - 1].mastery() == 1000, "the track runs from a new Ayato to the final kakuja at 1000");
        boolean texts = true;
        for (com.hex.kagune.control.ayato.AyatoUnlock unlock : all) {
            texts &= unlock.text().length() <= 42;
        }
        check(texts, "every step's line fits the menu column");
        // The track's numbers are the game's own gates.
        check(com.hex.kagune.control.ayato.AyatoUnlock.WING_VORTEX.mastery() == AyatoRules.VORTEX_MASTERY
            && com.hex.kagune.control.ayato.AyatoUnlock.CRYSTAL.mastery() == AyatoRules.CRYSTAL_MASTERY
            && com.hex.kagune.control.ayato.AyatoUnlock.GATLING.mastery() == AyatoRules.GATLING_MASTERY
            && com.hex.kagune.control.ayato.AyatoUnlock.KAKUJA.mastery() == WingForm.KAKUJA.requiredMastery()
            && com.hex.kagune.control.ayato.AyatoUnlock.RUSHING_DEATH.mastery() == AyatoRules.RUSH_MASTERY
            && com.hex.kagune.control.ayato.AyatoUnlock.NEEDLE_GATLING.mastery() == AyatoRules.NEEDLE_GATLING_MASTERY, "Z, V, J and K unlock where the track says");
        check(AyatoRules.speedAmplifier(com.hex.kagune.control.ayato.AyatoUnlock.UKAKU_SPEED.mastery() - 1) == 0
            && AyatoRules.speedAmplifier(com.hex.kagune.control.ayato.AyatoUnlock.UKAKU_SPEED.mastery()) == 1, "Ukaku speed II comes where the track says");
        // The wings' enders land on the steps that name them.
        check(AyatoRules.enderMastery(2) <= com.hex.kagune.control.ayato.AyatoUnlock.TWIN_WINGS.mastery()
            && AyatoRules.enderMastery(3) == com.hex.kagune.control.ayato.AyatoUnlock.WING_BLADES.mastery()
            && AyatoRules.enderMastery(4) == com.hex.kagune.control.ayato.AyatoUnlock.RABBIT_DASH.mastery()
            && AyatoRules.enderMastery(5) == com.hex.kagune.control.ayato.AyatoUnlock.FULL_WINGS.mastery(), "Rising Flame, Fire Fan, Rabbit Dash, Black Rabbit on their steps");
        // The kakuja's enders: two with the transformation, the rest spread to 1000.
        check(AyatoRules.enderMastery(0, true) == WingForm.KAKUJA.requiredMastery() && AyatoRules.enderMastery(1, true) == WingForm.KAKUJA.requiredMastery()
            && AyatoRules.enderMastery(2, true) == com.hex.kagune.control.ayato.AyatoUnlock.RUSHING_DEATH.mastery()
            && AyatoRules.enderMastery(3, true) == com.hex.kagune.control.ayato.AyatoUnlock.NEEDLE_GATLING.mastery()
            && AyatoRules.enderMastery(4, true) == com.hex.kagune.control.ayato.AyatoUnlock.SIX_TAIL.mastery()
            && AyatoRules.enderMastery(5, true) == com.hex.kagune.control.ayato.AyatoUnlock.CRIMSON_BLOOM.mastery(), "Harrow and the Line at 750, Thorn Fan 800 .. Crimson Bloom 950");
        boolean kakujaEnders = true;
        for (int mastery = WingForm.KAKUJA.requiredMastery(); mastery <= 1000; mastery += 10) {
            kakujaEnders &= AyatoRules.ender(0, mastery, true) == 0;
            for (int shots = 1; shots <= 5; shots++) {
                int ender = AyatoRules.ender(shots, mastery, true);
                kakujaEnders &= ender >= 1 && ender <= shots && mastery >= AyatoRules.enderMastery(ender, true);
                kakujaEnders &= ender >= AyatoRules.ender(shots - 1, mastery, true) && AyatoRules.ender(shots, mastery + 10, true) >= ender;
            }
        }
        check(kakujaEnders, "a kakuja chain always ends in an unlocked ender, never weaker for more volleys or mastery");
        check(AyatoRules.ender(5, 750, true) == 1 && AyatoRules.ender(2, 799, true) == 1 && AyatoRules.ender(2, 800, true) == 2
            && AyatoRules.ender(5, 949, true) == 4 && AyatoRules.ender(5, 950, true) == 5, "locked kakuja enders fall back to the strongest unlocked");
        // 5.0.13: the Wing Block is gone, so the first step after the start is the twin wings.
        check(com.hex.kagune.control.ayato.AyatoUnlock.next(0) == com.hex.kagune.control.ayato.AyatoUnlock.TWIN_WINGS
            && com.hex.kagune.control.ayato.AyatoUnlock.next(760) == com.hex.kagune.control.ayato.AyatoUnlock.RUSHING_DEATH
            && com.hex.kagune.control.ayato.AyatoUnlock.next(999) == com.hex.kagune.control.ayato.AyatoUnlock.FINAL_KAKUJA
            && com.hex.kagune.control.ayato.AyatoUnlock.FINAL_KAKUJA.reached(1000) && com.hex.kagune.control.ayato.AyatoUnlock.next(1000) == null,
            "the next step at 0, 760 and 999; at 1000 the final kakuja and nothing left");
    }

    /** K timed: a tap is the first kakuja, a hold the final kakuja, once per press. */
    private static void kakujaKey() {
        com.hex.kagune.control.ayato.KakujaKey key = new com.hex.kagune.control.ayato.KakujaKey();
        int hold = com.hex.kagune.control.ayato.KakujaKey.HOLD_TICKS;
        check(key.tick(true, false, true) == com.hex.kagune.control.ayato.KakujaKey.TAP, "pressed and let go within a tick: a tap");
        check(key.tick(false, false, true) == com.hex.kagune.control.ayato.KakujaKey.NONE, "nothing more after it");
        int sentAt = -1;
        int taps = 0;
        int holds = 0;
        for (int t = 0; t < hold * 3; t++) {
            int sent = key.tick(t == 0, true, true);
            if (sent == com.hex.kagune.control.ayato.KakujaKey.HOLD) {
                holds++;
                sentAt = t;
            } else if (sent == com.hex.kagune.control.ayato.KakujaKey.TAP) {
                taps++;
            }
        }
        check(holds == 1 && taps == 0 && sentAt == hold - 1, "held down: one hold, as soon as it is held long enough (" + sentAt + ")");
        check(key.tick(false, false, true) == com.hex.kagune.control.ayato.KakujaKey.NONE, "letting go after a hold sends nothing");
        int[] quick = new int[hold];
        for (int t = 0; t < hold - 2; t++) {
            quick[t] = key.tick(t == 0, true, true);
        }
        check(java.util.Arrays.stream(quick).allMatch(s -> s == com.hex.kagune.control.ayato.KakujaKey.NONE)
            && key.tick(false, false, true) == com.hex.kagune.control.ayato.KakujaKey.TAP, "let go before the hold: a tap, sent on letting go");
        check(key.tick(true, true, false) == com.hex.kagune.control.ayato.KakujaKey.TAP, "before the final kakuja a press is a tap at once");
        boolean quiet = true;
        for (int t = 0; t < hold * 2; t++) {
            quiet &= key.tick(false, true, false) == com.hex.kagune.control.ayato.KakujaKey.NONE;
        }
        check(quiet, "and holding it then sends nothing more");
    }

    private static void mutations() {
        com.hex.kagune.control.ayato.AyatoMutation[] all = com.hex.kagune.control.ayato.AyatoMutation.values();
        check(all.length == 5 && com.hex.kagune.control.ayato.AyatoMutation.MILESTONES.length == all.length,
            "five mutations (1.0.41: Furuta's revive the fifth), a milestone for each: he can have them all");
        check(all[4] == com.hex.kagune.control.ayato.AyatoMutation.NOT_THAT_EASILY
            && all[4].description().equals(com.hex.kagune.control.furuta.FurutaMutation.NOT_THAT_EASILY.description()),
            "the fifth is Furuta's revive, word for word (the first four untouched, ordinals and all)");
        check(com.hex.kagune.control.ayato.AyatoMutation.earned(0) == 0 && com.hex.kagune.control.ayato.AyatoMutation.earned(299) == 0
            && com.hex.kagune.control.ayato.AyatoMutation.earned(300) == 1 && com.hex.kagune.control.ayato.AyatoMutation.earned(999) == 4
            && com.hex.kagune.control.ayato.AyatoMutation.earned(1000) == 5, "a point at each milestone: 300, 500, 700, 900, 1000");
        int mask = 0;
        boolean spend = true;
        for (com.hex.kagune.control.ayato.AyatoMutation mutation : all) {
            spend &= com.hex.kagune.control.ayato.AyatoMutation.canUnlock(1000, mask, mutation);
            mask |= mutation.bit();
            spend &= !com.hex.kagune.control.ayato.AyatoMutation.canUnlock(1000, mask, mutation) && mutation.in(mask);
        }
        check(spend && mask == com.hex.kagune.control.ayato.AyatoMutation.all() && com.hex.kagune.control.ayato.AyatoMutation.available(1000, mask) == 0,
            "at full mastery every mutation can be unlocked, each once");
        int one = com.hex.kagune.control.ayato.AyatoMutation.FIRE_DANCE.bit();
        check(com.hex.kagune.control.ayato.AyatoMutation.available(450, one) == 0
            && !com.hex.kagune.control.ayato.AyatoMutation.canUnlock(450, one, com.hex.kagune.control.ayato.AyatoMutation.AGILITY_BOOST)
            && com.hex.kagune.control.ayato.AyatoMutation.canUnlock(500, one, com.hex.kagune.control.ayato.AyatoMutation.AGILITY_BOOST),
            "a mutation needs a point left to spend; any order");
        check(com.hex.kagune.control.ayato.AyatoMutation.nextMilestone(0) == 300 && com.hex.kagune.control.ayato.AyatoMutation.nextMilestone(899) == 900
            && com.hex.kagune.control.ayato.AyatoMutation.nextMilestone(900) == 1000 && com.hex.kagune.control.ayato.AyatoMutation.nextMilestone(1000) == -1,
            "the menu knows when the next point comes");
        check(com.hex.kagune.control.ayato.AyatoMutation.byOrdinal(7) == null && com.hex.kagune.control.ayato.AyatoMutation.byOrdinal(-1) == null
            && com.hex.kagune.control.ayato.AyatoMutation.unlocked(0xFF) == 5, "bad requests and stray bits are ignored");
        check(AyatoRules.FIRE_DASH_BACK > 1.2 && AyatoRules.FIRE_DANCE_RADIUS > 1.4 && AyatoRules.FIRE_DANCE_DAMAGE > 1.2
            && AyatoRules.AGILITY_SPEED >= 1 && AyatoRules.AGILITY_JUMP >= 1 && AyatoRules.SHARD_EXPLOSION_SHARE > 0.0F,
            "each mutation makes a real difference");
    }
}

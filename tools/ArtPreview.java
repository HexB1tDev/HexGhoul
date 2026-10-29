import com.hex.kagune.control.ayato.WingForm;
import com.hex.kagune.control.ayato.WingRig;
import com.hex.kagune.control.client.animation.IdlePose;
import com.hex.kagune.control.client.animation.RigPose;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Ayato's moves off-game: the body posed through the game's exact PlayerAnimator/bendy-lib transforms ({@link PoseRender})
 * with the wings from {@link WingRig}, in {@link WingPreview}'s translucent rasterizer. Rig space throughout (x to the
 * player's right, y up from the feet, z forward). Usage: {@code ArtPreview <out-dir> [sheet ...]}; sheets: aim, shot, moves,
 * crystal, spikes, vortex, gatling, kakuja.
 */
public final class ArtPreview {
    /** One panel: what to draw, from where. */
    record Shot(String label, RigPose pose, WingRig.Input wings, double[] eye, double[] target, List<WingPreview.Tri> extra) {
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/preview");
        out.mkdirs();
        java.util.Set<String> wanted = new java.util.HashSet<>(java.util.Arrays.asList(args).subList(Math.min(1, args.length), args.length));
        if (wanted.isEmpty() || wanted.contains("aim")) {
            aim(new File(out, "ayato-aim.png"));
        }
        if (wanted.isEmpty() || wanted.contains("shot")) {
            shot(new File(out, "ayato-shot.png"));
        }
        if (wanted.isEmpty() || wanted.contains("moves")) {
            moves(new File(out, "ayato-moves.png"));
        }
        if (wanted.isEmpty() || wanted.contains("crystal")) {
            crystal(new File(out, "ayato-crystal.png"));
        }
        if (wanted.isEmpty() || wanted.contains("spikes")) {
            spikes(new File(out, "ayato-spikes.png"));
        }
        if (wanted.isEmpty() || wanted.contains("vortex")) {
            vortex(new File(out, "ayato-vortex.png"));
        }
        if (wanted.isEmpty() || wanted.contains("gatling")) {
            gatling(new File(out, "ayato-gatling.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kakuja")) {
            kakuja(new File(out, "ayato-kakuja.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kakuja-aim")) {
            kakujaAim(new File(out, "ayato-kakuja-aim.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kakuja-kit")) {
            kakujaKit(new File(out, "ayato-kakuja-kit.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kakuja-motion")) {
            kakujaMotion(new File(out, "ayato-kakuja-motion.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kakuja-final")) {
            kakujaFinal(new File(out, "ayato-kakuja-final.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kakuja-final-kit")) {
            kakujaFinalKit(new File(out, "ayato-kakuja-final-kit.png"));
        }
        if (wanted.isEmpty() || wanted.contains("wounds")) {
            wounds(new File(out, "wounds.png"));
        }
        if (wanted.isEmpty() || wanted.contains("centipede")) {
            centipede(new File(out, "kaneki-centipede.png"));
        }
        if (wanted.isEmpty() || wanted.contains("centipede-grab")) {
            centipedeGrab(new File(out, "kaneki-centipede-grab.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kaneki-kakuja")) {
            kanekiKakuja(new File(out, "kaneki-kakuja-forms.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kakuja-burst")) {
            kakujaBurst(new File(out, "kaneki-kakuja-burst.png"));
        }
        if (wanted.isEmpty() || wanted.contains("centipede-game")) {
            centipedeInGame(new File(out, "kaneki-centipede-ingame.png"));
        }
        if (wanted.isEmpty() || wanted.contains("final-legs")) {
            finalLegs(new File(out, "kaneki-final-legs.png"));
        }
        if (wanted.isEmpty() || wanted.contains("centipede-mask")) {
            centipedeMask(new File(out, "kaneki-mask-centipede.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kaneki-mask")) {
            kanekiMask(new File(out, "kaneki-mask.png"));
        }
        if (wanted.isEmpty() || wanted.contains("kakugan-lean")) {
            kakuganLean(new File(out, "kakugan-lean.png"));
        }
        if (wanted.isEmpty() || wanted.contains("tendril-guard")) {
            tendrilGuard(new File(out, "kaneki-tendril-guard.png"));
        }
        if (wanted.isEmpty() || wanted.contains("steel-blade")) {
            steelBlade(new File(out, "ayato-steel-blade.png"));
        }
        if (wanted.isEmpty() || wanted.contains("steel-blade-detail")) {
            steelBladeDetail(new File(out, "ayato-steel-blade-detail.png"));
        }
        if (wanted.isEmpty() || wanted.contains("wounds-anatomy")) {
            woundsAnatomy(new File(out, "wounds-anatomy.png"));
        }
        if (wanted.isEmpty() || wanted.contains("crystal-thorns")) {
            crystalThorns(new File(out, "ayato-crystal-thorns.png"));
        }
        if (wanted.contains("war")) {
            war(new File(out, "king-war.png"));
        }
        if (wanted.contains("war-combo")) {
            warStrikes(new File(out, "king-war-combo.png"), true);
        }
        if (wanted.contains("war-strikes")) {
            warStrikes(new File(out, "king-war-strikes.png"), false);
        }
        if (wanted.contains("king-marks")) {
            kingMarks(new File(out, "king-marks.png"));
        }
        if (wanted.contains("king-leaving")) {
            kingLeaving(new File(out, "king-leaving.png"));
        }
        if (wanted.contains("king-coming")) {
            kingComing(new File(out, "king-coming.png"));
        }
        if (wanted.contains("war-cloth")) {
            warCloth(new File(out, "king-war-cloth.png"));
        }
        if (wanted.contains("war-fx")) {
            warFx(new File(out, "king-war-fx.png"));
        }
        if (wanted.contains("war-sun")) {
            warSun(new File(out, "king-war-sun.png"));
        }
        if (wanted.contains("war-fp")) {
            warFirstPersonBlows(new File(out, "king-war-fp.png"));
        }
        if (wanted.contains("war-dominion")) {
            warDominion(new File(out, "king-war-dominion.png"));
        }
        if (wanted.contains("war-eyes")) {
            warEyes(new File(out, "king-war-eyes.png"));
        }
        if (wanted.contains("turning")) {
            turning(new File(out, "dragon-turning.png"));
        }
        if (wanted.contains("king-first-person")) {
            kingFirstPerson(new File(out, "king-first-person.png"));
        }
        if (wanted.contains("warhold")) {
            warhold(new File(out, "warhold.png"));
        }
        if (wanted.contains("warpath")) {
            warpath(new File(out, "warpath.png"));
        }
        if (wanted.contains("king-runs")) {
            kingRuns(new File(out, "king-runs.png"));
        }
        if (wanted.contains("peace-alive")) {
            peaceAlive(out);
        }
        if (wanted.contains("peace-moves")) {
            peaceMoves(new File(out, "king-peace-moves.png"));
        }
        if (wanted.contains("peace-rework")) {
            peaceRework(new File(out, "king-peace-rework.png"));
        }
        if (wanted.contains("clash")) {
            clash(new File(out, "kagune-clash.png"));
        }
        if (wanted.contains("peace-pierce")) {
            peacePierce(new File(out, "king-peace-pierce.png"));
        }
        if (wanted.contains("peace-combo")) {
            peaceCombo(new File(out, "king-peace-combo.png"));
        }
        if (wanted.contains("peace-views")) {
            peaceViews(new File(out, "king-peace-views.png"));
        }
        if (wanted.contains("peace")) {
            peace(new File(out, "king-peace.png"));
        }
        if (wanted.contains("war-moves")) {
            warMoves(new File(out, "king-war-moves.png"));
        }
        if (wanted.contains("war-night")) {
            warNight(new File(out, "king-war-night.png"));
        }
        if (wanted.contains("war-views")) {
            warViews(new File(out, "king-war-views.png"));
        }
    }

    /**
     * 5.0.15: the crystal thorns (CrystalThorns, the game's own layout and growth, each a CrystalSpike as AyatoWingRenderer
     * draws it) breaking out of an Ayato in the crystal mode through a Kaneki tail wrapped round him; a second later it lets go.
     */
    static void crystalThorns(File file) throws Exception {
        RigPose still = new RigPose();
        com.hex.kagune.control.ayato.CrystalThorns.Thorn[] thorns = com.hex.kagune.control.ayato.CrystalThorns.thorns(4242,
            com.hex.kagune.control.ayato.CrystalThorns.COUNT);
        // A Kaneki tail coming in from his right front and wrapping round his chest (preview tentacle, not the game's path).
        List<double[]> coil = new ArrayList<>();
        coil.add(new double[] {-6.0, 2.6, 4.0});
        coil.add(new double[] {-3.2, 1.9, 2.0});
        coil.add(new double[] {-1.2, 1.35, 0.7});
        for (int k = 0; k <= 10; k++) {
            double a = Math.PI * 0.75 + k * Math.PI * 2.2 / 10.0;
            coil.add(new double[] {Math.sin(a) * 0.42, 1.32 - 0.03 * k, Math.cos(a) * 0.36});
        }
        double[][] tail = coil.toArray(new double[0][]);
        double[] front34 = {-3.6, 2.4, 4.4};
        double[] wide = {-5.4, 3.0, 6.6};
        double[] look = {0.0, 1.05, 0.0};
        Object[][] cases = {
            {"Held in the crystal mode: the thorns break out (1 tick)", 1.0, Double.NaN, front34, true},
            {"2 ticks", 2.0, Double.NaN, front34, true},
            {"4 ticks", 4.0, Double.NaN, front34, true},
            {"All out (10 ticks): at 20, the tail lets go", 10.0, Double.NaN, wide, true},
            {"Free, from behind: they stay out a moment", 18.0, Double.NaN, new double[] {4.8, 3.2, -6.8}, false},
            {"Then sink back in (or shatter if the crystal thaws)", 40.0, 3.0, wide, false},
        };
        List<Shot> shots = new ArrayList<>();
        for (Object[] c : cases) {
            double age = (Double)c[1];
            double sunk = (Double)c[2];
            double[] eye = (double[])c[3];
            List<WingPreview.Tri> tris = new ArrayList<>();
            if ((Boolean)c[4]) {
                tris.addAll(rinkaku(tail, 0.2, 0xA50F37, 0x18030B));
            }
            double glow = 0.15 + 0.6 * Math.max(0.0, 1.0 - age / 10.0);
            for (com.hex.kagune.control.ayato.CrystalThorns.Thorn thorn : thorns) {
                double out = com.hex.kagune.control.ayato.CrystalThorns.out(thorn, age, sunk);
                if (out <= 0.001) {
                    continue;
                }
                tris.addAll(spike(com.hex.kagune.control.ayato.CrystalThorns.tip(thorn, out), thorn.direction(),
                    com.hex.kagune.control.ayato.CrystalThorns.drawnSize(thorn, out), eye, glow, 0.0, thorn.seed()));
            }
            shots.add(new Shot((String)c[0], still, crystalWings(300, 1.0F, 0, 0, WingRig.FORWARD, 0, 1.3), eye, look, tris));
        }
        sheet(file, "Crystal thorns: grabbed in the crystal mode, dozens of crystal spikes break out of him and the kagune lets go", shots, 3, 520, 460);
    }

    /**
     * 5.0.14: Auto Projectile's guard (TendrilShield.pose, the game's own curve) on the final kakuja's four tentacles: the one on
     * the side the shots come from swings across their line; shots from two sides take two. A thin red line is the incoming shot.
     */
    static void tendrilGuard(File file) throws Exception {
        RigPose still = new RigPose();
        double[][] upper = gameTentacle(1.88, 1.72, 0.5, 1.2, 1);
        double[][] lower = gameTentacle(2.34, -0.44, 0.72, 1.34, -1);
        double[][][] rest = {upper, mirrored(upper), lower, mirrored(lower)};
        double[] centre = {0.0, 1.8 * 0.55, 0.0};
        double[] forward = {0.0, 0.0, 1.0};
        Object[][] cases = {
            {"An arrow from the front: one tentacle guards", new double[][] {{0.0, 0.05, 1.0}}, new double[] {3.0, 2.4, 4.6}},
            {"From his left", new double[][] {{-1.0, 0.05, 0.1}}, new double[] {-4.2, 2.4, 3.0}},
            {"From behind", new double[][] {{0.1, 0.05, -1.0}}, new double[] {-3.0, 2.6, -4.6}},
            {"Two sides at once: two tentacles", new double[][] {{0.0, 0.05, 1.0}, {1.0, 0.05, 0.2}}, new double[] {4.0, 2.6, 4.0}},
            {"Spike Rain from above", new double[][] {{0.15, 1.0, 0.35}}, new double[] {3.4, 3.4, 4.4}},
            {"From a rooftop in front", new double[][] {{0.2, 0.55, 1.0}}, new double[] {3.0, 2.2, 4.8}},
        };
        List<Shot> shots = new ArrayList<>();
        for (Object[] c : cases) {
            double[][] dirs = (double[][])c[1];
            boolean[] used = new boolean[rest.length];
            List<WingPreview.Tri> tris = new ArrayList<>();
            for (double[] raw : dirs) {
                double[] dir = PoseRender.norm(raw);
                // The free tentacle on the side the shots come from (upper pair first), as TendrilShieldServer picks.
                int pick = -1;
                for (int i = 0; i < rest.length && pick < 0; i++) {
                    if (!used[i] && (rest[i][rest[i].length - 1][0] >= 0.0) == (dir[0] >= 0.0)) {
                        pick = i;
                    }
                }
                for (int i = 0; i < rest.length && pick < 0; i++) {
                    if (!used[i]) {
                        pick = i;
                    }
                }
                used[pick] = true;
                double[][] guard = com.hex.kagune.control.TendrilShield.pose(rest[pick][0], centre, dir, forward, 24);
                tris.addAll(rinkaku(guard, pick < 2 ? 0.22 : 0.2, 0xA50F37, 0x18030B));
                double[] stop = PoseRender.add(centre, scale(dir, com.hex.kagune.control.TendrilShield.DISTANCE + 0.2));
                tris.addAll(rinkaku(new double[][] {PoseRender.add(centre, scale(dir, 7.0)), PoseRender.add(centre, scale(dir, 4.0)), stop},
                    0.05, 0xFF3020, 0xFFB0A0));
            }
            for (int i = 0; i < rest.length; i++) {
                if (!used[i]) {
                    tris.addAll(rinkaku(rest[i], i < 2 ? 0.22 : 0.2, 0xA50F37, 0x18030B));
                }
            }
            shots.add(new Shot((String)c[0], still, NO_WINGS, (double[])c[2], new double[] {0.0, 1.2, 0.0}, tris));
        }
        sheet(file, "Auto Projectile: a tentacle swings across the shots' line to guard him (one per direction, two at most)", shots, 3, 520, 420);
    }

    /** The kakugan as a head layer, placed on {@code placeOn}'s head (PoseRender.point: with the upper body's turn when it bends). */
    static List<WingPreview.Tri> kakuganTris(RigPose placeOn) {
        List<WingPreview.Tri> out = new ArrayList<>();
        for (com.hex.kagune.control.kakugan.KakuganModel.Quad q : com.hex.kagune.control.kakugan.KakuganModel.build(
                com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT, 7, 1.0, 1.0F, true, 0.003)) {
            float[] x = q.xyz();
            double[][] c = new double[4][];
            for (int i = 0; i < 4; i++) {
                c[i] = onPart(placeOn, "head", new double[] {x[i * 3], x[i * 3 + 1], x[i * 3 + 2]});
            }
            double[] n = q.glow() ? new double[] {0, 1, 0} : PoseRender.norm(PoseRender.cross(PoseRender.sub(c[1], c[0]), PoseRender.sub(c[2], c[0])));
            int[] a = q.argb();
            out.add(new WingPreview.Tri(new double[][] {c[0], c[1], c[2]}, new int[] {a[0], a[1], a[2]}, n, false, q.glow()));
            out.add(new WingPreview.Tri(new double[][] {c[0], c[2], c[3]}, new int[] {a[0], a[2], a[3]}, n, false, q.glow()));
        }
        return out;
    }

    /**
     * 5.0.13: the kakugan on a leaning body. PlayerAnimator draws the head turned with the upper body's bend, which the head part's
     * own transform never carries: placed on the part alone (5.0.12), a forward lean buries it in the head and nothing glows.
     */
    static void kakuganLean(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        Object[][] stances = {{"Ayato's kakuja stance", IdlePose.Style.AYATO_KAKUJA}, {"Kaneki's crouch", IdlePose.Style.KANEKI_FERAL}};
        for (Object[] stance : stances) {
            RigPose pose = IdlePose.pose((IdlePose.Style)stance[1], 1.0F, 0.4);
            RigPose unbent = new RigPose().add(pose, 1.0F);
            unbent.lean = 0.0F;
            unbent.side = 0.0F;
            double[] face = onPart(pose, "head", new double[] {0, -4, -4});
            double[] eye = {face[0] + 0.55, face[1] + 0.1, face[2] + 1.15};
            shots.add(new Shot(stance[0] + ", 5.0.12: no glow", pose, NO_WINGS, eye, face, kakuganTris(unbent)));
            shots.add(new Shot(stance[0] + ", 5.0.13: glowing", pose, NO_WINGS, eye, face, kakuganTris(pose)));
        }
        sheet(file, "Kakugan on a leaning body: placed on the head part alone (left) vs turned with the upper body (right)", shots, 2, 560, 420);
    }

    /** The concept centipede legs (CentipedeTail) as in the user's in-game shot: one arched over his head, one out to his left. */
    static final double[][] CENTIPEDE_RIGHT = {{0.12, 1.05, -0.2}, {0.35, 1.2, -0.7}, {0.75, 1.9, -1.25}, {1.05, 3.0, -1.15}, {0.95, 3.85, -0.35},
        {0.6, 3.8, 0.55}, {0.35, 3.1, 1.0}};

    static List<WingPreview.Tri> centipedeTris(boolean fine, double jaw, double flare, double ripple, double phase) {
        double[][] right = CENTIPEDE_RIGHT;
        double[][] left = {{-0.12, 1.05, -0.2}, {-0.5, 1.25, -0.75}, {-1.3, 1.95, -1.05}, {-2.35, 2.5, -0.85}, {-3.25, 2.35, -0.25}, {-3.7, 1.6, 0.3}};
        List<WingPreview.Tri> out = new ArrayList<>();
        out.addAll(centipedeLeg(right, 1, new double[] {1.0, 0.0, 0.0}, new com.hex.kagune.control.ayato.CentipedeTail.Look(0.3, 7, fine, 1.0, 1,
            jaw, flare, ripple, phase)));
        out.addAll(centipedeLeg(left, -1, new double[] {0.0, 0.0, -1.0}, new com.hex.kagune.control.ayato.CentipedeTail.Look(0.3, 7, fine, 1.0, 2,
            jaw, flare, ripple, phase + 1.3)));
        return out;
    }

    /** Both legs up and out like the user's accessory picture (ram horns, fangs hooking down at his sides). */
    static List<WingPreview.Tri> centipedeHorns() {
        double[][] right = {{0.12, 1.05, -0.2}, {0.4, 1.3, -0.7}, {1.0, 2.2, -1.0}, {1.7, 3.0, -0.8}, {2.45, 3.15, -0.3}, {2.95, 2.45, 0.1},
            {3.0, 1.55, 0.25}};
        List<WingPreview.Tri> out = new ArrayList<>();
        for (int side = -1; side <= 1; side += 2) {
            double[][] c = new double[right.length][];
            for (int i = 0; i < right.length; i++) {
                c[i] = new double[] {right[i][0] * side, right[i][1], right[i][2]};
            }
            out.addAll(centipedeLeg(c, side, new double[] {0.0, 0.0, -1.0}, new com.hex.kagune.control.ayato.CentipedeTail.Look(0.3, 7, true, 1.0,
                side > 0 ? 1 : 2)));
        }
        return out;
    }

    static List<WingPreview.Tri> centipedeLeg(double[][] controls, int side, double[] dorsal, com.hex.kagune.control.ayato.CentipedeTail.Look look) {
        com.hex.kagune.control.ayato.KakujaModel.Spine spine = com.hex.kagune.control.ayato.CentipedeTail.through(controls, 120, side, dorsal);
        return placed(sink -> com.hex.kagune.control.ayato.CentipedeTail.tail(sink, spine, look), q -> q);
    }

    /**
     * A rinkaku tentacle for previews (an approximation of how the game draws Kaneki's tendrils, not the game's code): a
     * twelve-sided tube through {@code controls}, grown out of the back, swelling a little in the middle and tapering to a point,
     * crimson with dark vein lanes.
     */
    static List<WingPreview.Tri> rinkaku(double[][] controls, double radius, int colour, int vein) {
        int stations = 48;
        int sides = 12;
        double[][] p = new double[stations + 1][];
        for (int i = 0; i <= stations; i++) {
            p[i] = catmull(controls, i / (double)stations);
        }
        double[][][] ring = new double[stations + 1][sides][];
        double[][][] normal = new double[stations + 1][sides][];
        double[] n = null;
        for (int i = 0; i <= stations; i++) {
            double[] tan = PoseRender.norm(PoseRender.sub(p[Math.min(stations, i + 1)], p[Math.max(0, i - 1)]));
            double[] ref = n == null ? new double[] {0.0, 1.0, 0.0} : n;
            n = PoseRender.norm(PoseRender.sub(ref, scale(tan, PoseRender.dot(ref, tan))));
            double[] b = crossOf(tan, n);
            double u = i / (double)stations;
            double grow = 0.3 + 0.7 * smoothstep(Math.min(1.0, u / 0.18));
            double r = radius * grow * (1.0 + 0.18 * Math.sin(Math.PI * u)) * (1.0 - 0.62 * Math.pow(u, 1.6)) * (1.0 - 0.85 * smoothstep(Math.max(0.0,
                (u - 0.8) / 0.2)));
            r = Math.max(radius * 0.04, r);
            for (int k = 0; k < sides; k++) {
                double a0 = Math.PI * 2.0 * k / sides;
                double wobble = 1.0 + 0.03 * Math.sin(u * 12.0 + k * 1.93);
                double[] radial = {n[0] * Math.cos(a0) + b[0] * Math.sin(a0), n[1] * Math.cos(a0) + b[1] * Math.sin(a0), n[2] * Math.cos(a0) + b[2] * Math.sin(a0)};
                normal[i][k] = radial;
                ring[i][k] = new double[] {p[i][0] + radial[0] * r * wobble, p[i][1] + radial[1] * r * wobble, p[i][2] + radial[2] * r * wobble};
            }
        }
        List<WingPreview.Tri> out = new ArrayList<>();
        for (int i = 0; i < stations; i++) {
            for (int k = 0; k < sides; k++) {
                int j = (k + 1) % sides;
                int c0 = laneColour(colour, vein, k, i / (double)stations);
                int c1 = laneColour(colour, vein, j, i / (double)stations);
                double[] nn = PoseRender.norm(new double[] {normal[i][k][0] + normal[i][j][0], normal[i][k][1] + normal[i][j][1], normal[i][k][2] + normal[i][j][2]});
                out.add(new WingPreview.Tri(new double[][] {ring[i][k], ring[i + 1][k], ring[i + 1][j]}, new int[] {c0, c0, c1}, nn, true));
                out.add(new WingPreview.Tri(new double[][] {ring[i][k], ring[i + 1][j], ring[i][j]}, new int[] {c0, c1, c1}, nn, true));
            }
        }
        return out;
    }

    static int laneColour(int colour, int vein, int side, double u) {
        double mix = side % 3 == 0 ? 0.35 + 0.35 * (0.5 + 0.5 * Math.sin(u * 24.0)) : 0.0;
        int r = (int)Math.round((colour >> 16 & 255) * (1 - mix) + (vein >> 16 & 255) * mix);
        int g = (int)Math.round((colour >> 8 & 255) * (1 - mix) + (vein >> 8 & 255) * mix);
        int b = (int)Math.round((colour & 255) * (1 - mix) + (vein & 255) * mix);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    static double[] catmull(double[][] c, double u) {
        int segments = c.length - 1;
        double x = Math.max(0.0, Math.min(1.0, u)) * segments;
        int i = Math.min(segments - 1, (int)Math.floor(x));
        double t = x - i;
        double[] p0 = c[Math.max(0, i - 1)], p1 = c[i], p2 = c[i + 1], p3 = c[Math.min(segments, i + 2)];
        double[] out = new double[3];
        for (int k = 0; k < 3; k++) {
            out[k] = 0.5 * (2.0 * p1[k] + (-p0[k] + p2[k]) * t + (2.0 * p0[k] - 5.0 * p1[k] + 4.0 * p2[k] - p3[k]) * t * t
                + (-p0[k] + 3.0 * p1[k] - 3.0 * p2[k] + p3[k]) * t * t * t);
        }
        return out;
    }

    static double[] scale(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    static double[] crossOf(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double smoothstep(double x) {
        double t = Math.max(0.0, Math.min(1.0, x));
        return t * t * (3.0 - 2.0 * t);
    }

    static double[][] mirrored(double[][] c) {
        double[][] out = new double[c.length][];
        for (int i = 0; i < c.length; i++) {
            out[i] = new double[] {-c[i][0], c[i][1], c[i][2]};
        }
        return out;
    }

    /** The final kakuja's four rinkaku tentacles (default kagune colours): two rising high behind him, two sweeping out low. */
    static List<WingPreview.Tri> kanekiTentacles() {
        // The final kakuja's four tentacles at rest, as the original lays them out (TendrilChain: the cubic from the pelvis
        // through its two controls to restTarget; mastery 1000, default appearance): the upper pair (fan 1.88, height 1.72,
        // back 0.5, length 1.2) and the lower pair (2.34, -0.44, 0.72, 1.34).
        double[][] upper = gameTentacle(1.88, 1.72, 0.5, 1.2, 1);
        double[][] lower = gameTentacle(2.34, -0.44, 0.72, 1.34, -1);
        int crimson = 0xA50F37;
        int vein = 0x18030B;
        List<WingPreview.Tri> out = new ArrayList<>();
        out.addAll(rinkaku(upper, 0.22, crimson, vein));
        out.addAll(rinkaku(mirrored(upper), 0.22, crimson, vein));
        out.addAll(rinkaku(lower, 0.2, crimson, vein));
        out.addAll(rinkaku(mirrored(lower), 0.2, crimson, vein));
        return out;
    }

    /** One tentacle's rest curve (rig space, his right side), sampled from the original's cubic; {@code lane} 1 upper, -1 lower. */
    static double[][] gameTentacle(double fan, double height, double back, double lengthScale, int lane) {
        double length = (8.0 + 1000 * 0.0016) * 0.6 * lengthScale;
        double[] root = {0.0, 1.8 * 0.43, -0.18};
        double restArch = lane > 0 ? length * 0.09 : -length * 0.025;
        double[] rest = {fan, root[1] + height + restArch, root[2] + length * (0.03 + back * 0.04)};
        double[] a = {fan * 0.16, root[1] + height * 0.42 + (lane > 0 ? length * 0.15 : -length * 0.08), root[2] - length * 0.22};
        double[] b = {rest[0] + fan * 0.05, rest[1] + (lane > 0 ? length * 0.12 : -length * 0.04), rest[2] - length * 0.16};
        double[][] out = new double[7][];
        for (int i = 0; i < out.length; i++) {
            double t = i / (double)(out.length - 1);
            double u = 1.0 - t;
            out[i] = new double[3];
            for (int k = 0; k < 3; k++) {
                out[i][k] = u * u * u * root[k] + 3.0 * u * u * t * a[k] + 3.0 * u * t * t * b[k] + t * t * t * rest[k];
            }
        }
        return out;
    }

    /** A leg as the game draws it (3.1.0): CentipedeMotion's springs, CentipedeTail's plates, at {@code mastery}. */
    static List<WingPreview.Tri> gameLeg(com.hex.kagune.control.ayato.CentipedeMotion.Leg leg, int side, int mastery) {
        double length = com.hex.kagune.control.ayato.CentipedeMotion.length(mastery);
        double width = com.hex.kagune.control.ayato.CentipedeMotion.width(length);
        int plates = com.hex.kagune.control.ayato.CentipedeMotion.plates(length, width);
        com.hex.kagune.control.ayato.KakujaModel.Spine spine = com.hex.kagune.control.ayato.CentipedeTail.through(leg.points(), 64, side, leg.dorsal());
        return placed(sink -> com.hex.kagune.control.ayato.CentipedeTail.tail(sink, spine, new com.hex.kagune.control.ayato.CentipedeTail.Look(width,
            plates, true, 1.0, side > 0 ? 1 : 2, leg.jaw(), leg.flare(), 0.35, leg.phase())), q -> q);
    }

    static com.hex.kagune.control.ayato.CentipedeMotion.Input motionInput(int side, boolean finalForm, int mastery,
                                                                          com.hex.kagune.control.ayato.CentipedeMotion.Act act, double[] target,
                                                                          double radius, boolean grab, double time) {
        return new com.hex.kagune.control.ayato.CentipedeMotion.Input(side, finalForm, com.hex.kagune.control.ayato.CentipedeMotion.length(mastery), 0.99,
            act, target, radius, grab, 0.0, 0.0, 0.0, time);
    }

    /** A leg settled in a pose. */
    static com.hex.kagune.control.ayato.CentipedeMotion.Leg settled(com.hex.kagune.control.ayato.CentipedeMotion.Input input) {
        com.hex.kagune.control.ayato.CentipedeMotion.Leg leg = new com.hex.kagune.control.ayato.CentipedeMotion.Leg();
        leg.follow(com.hex.kagune.control.ayato.CentipedeMotion.pose(input), input.time(), 0.0);
        return leg;
    }

    /** What the 3.1.0 build draws: the legs on their springs through CentipedeHunt's strike and grab, tick by tick. */
    static void centipedeInGame(File file) throws Exception {
        RigPose still = new RigPose();
        RigPose dangling = new RigPose();
        dangling.leftArmX = -0.5F;
        dangling.rightArmX = -0.35F;
        dangling.rightArmZ = 0.45F;
        dangling.leftArmZ = -0.45F;
        dangling.settle(1.0F);
        com.hex.kagune.control.ayato.CentipedeMotion.Act rest = com.hex.kagune.control.ayato.CentipedeMotion.Act.REST;
        com.hex.kagune.control.ayato.CentipedeMotion.Act lock = com.hex.kagune.control.ayato.CentipedeMotion.Act.LOCK;
        com.hex.kagune.control.ayato.CentipedeMotion.Act strike = com.hex.kagune.control.ayato.CentipedeMotion.Act.STRIKE;
        com.hex.kagune.control.ayato.CentipedeMotion.Act coil = com.hex.kagune.control.ayato.CentipedeMotion.Act.COIL;
        List<Shot> shots = new ArrayList<>();
        List<WingPreview.Tri> first = new ArrayList<>(maskTris(still));
        for (int side = -1; side <= 1; side += 2) {
            first.addAll(gameLeg(settled(motionInput(side, false, 750, rest, null, 0.3, false, 0.0)), side, 750));
        }
        shots.add(new Shot("First kakuja at rest (mastery 750, 6.5-block legs)", still, NO_WINGS, new double[] {4.8, 3.2, 9.4},
            new double[] {-0.6, 2.3, -0.3}, first));
        List<WingPreview.Tri> last = new ArrayList<>(maskTris(still));
        for (int side = -1; side <= 1; side += 2) {
            last.addAll(gameLeg(settled(motionInput(side, true, 1000, rest, null, 0.3, false, 0.0)), side, 1000));
        }
        last.addAll(kanekiTentacles());
        shots.add(new Shot("Final kakuja at rest (1000, 7.5-block legs; tentacles approximated)", still, NO_WINGS, new double[] {0.0, 3.0, 12.5},
            new double[] {0.0, 2.6, -0.6}, last));
        double length = com.hex.kagune.control.ayato.CentipedeMotion.length(750);
        double reach = com.hex.kagune.control.ayato.CentipedeMotion.reach(length);
        // The strike: the nearest threat at the edge of reach, rear back to tick 3, the hit on tick 5.
        double[] prey = {Math.sin(0.45) * reach * 0.95, 1.1, Math.cos(0.45) * reach * 0.95};
        double[] preyFeet = {prey[0], prey[1] - 0.94 + 1.0, prey[2]};
        double[][] frames = {{1.5, 0}, {3.5, 0}, {5.0, 0}};
        String[] labels = {"Strike, tick 1.5: rearing back", "Strike, tick 3.5: the arc comes over",
            String.format("Strike, tick 5: the hit lands (reach %.1f blocks)", reach)};
        com.hex.kagune.control.ayato.CentipedeMotion.Leg right = settled(motionInput(1, false, 750, rest, null, 0.3, false, 0.0));
        com.hex.kagune.control.ayato.CentipedeMotion.Leg left = settled(motionInput(-1, false, 750, rest, null, 0.3, false, 0.0));
        double time = 0.0;
        for (int f = 0; f < frames.length; f++) {
            while (time < frames[f][0] - 1.0E-9) {
                time = Math.min(frames[f][0], time + 0.25);
                right.follow(com.hex.kagune.control.ayato.CentipedeMotion.pose(motionInput(1, false, 750, time < 3.0 ? lock : strike, prey, 0.3, false,
                    time)), time, 0.0);
                left.follow(com.hex.kagune.control.ayato.CentipedeMotion.pose(motionInput(-1, false, 750, rest, null, 0.3, false, time)), time, 0.0);
            }
            List<WingPreview.Tri> tris = new ArrayList<>(maskTris(still));
            tris.addAll(gameLeg(right, 1, 750));
            tris.addAll(gameLeg(left, -1, 750));
            tris.addAll(victim(still, preyFeet, Math.PI, 0.0, 1.0));
            shots.add(new Shot(labels[f], still, NO_WINGS, new double[] {6.6, 3.3, 8.8}, new double[] {1.0, 1.9, 1.6}, tris));
        }
        // The grab: rear to tick 4, strike, the bite on 7, hoisted over to beside him by 13, crushed there, then slammed.
        double[] held = com.hex.kagune.control.ayato.CentipedeMotion.holdPoint(1, length);
        double radius = 0.3 + com.hex.kagune.control.ayato.CentipedeMotion.width(length) * 0.85;
        right = settled(motionInput(1, false, 750, rest, null, 0.3, false, 0.0));
        left = settled(motionInput(-1, false, 750, rest, null, 0.3, true, 0.0));
        time = 0.0;
        double[][] grabFrames = {{8.0}, {12.0}, {16.0}};
        String[] grabLabels = {"Grab, tick 8: bitten, the coil closes", "Grab, tick 12: hoisted over to beside him", "Crush: held beside him, where he sees it"};
        for (int f = 0; f < grabFrames.length; f++) {
            double[] body = null;
            while (time < grabFrames[f][0] - 1.0E-9) {
                time = Math.min(grabFrames[f][0], time + 0.25);
                double hoist = smoothstep((time - 7.0) / 6.0);
                body = time < 7.0 ? prey : new double[] {prey[0] + (held[0] - prey[0]) * hoist, prey[1] + (held[1] - prey[1]) * hoist,
                    prey[2] + (held[2] - prey[2]) * hoist};
                com.hex.kagune.control.ayato.CentipedeMotion.Act act = time < 4.0 ? lock : time < 7.0 ? strike : coil;
                right.follow(com.hex.kagune.control.ayato.CentipedeMotion.pose(motionInput(1, false, 750, act, body, radius, true, time)), time, 0.0);
                left.follow(com.hex.kagune.control.ayato.CentipedeMotion.pose(motionInput(-1, false, 750, rest, null, 0.3, false, time)), time, 0.0);
            }
            List<WingPreview.Tri> tris = new ArrayList<>(maskTris(still));
            tris.addAll(gameLeg(right, 1, 750));
            tris.addAll(gameLeg(left, -1, 750));
            tris.addAll(victim(time < 7.5 ? still : dangling, new double[] {body[0], body[1] - 0.94 + 1.0, body[2]}, Math.PI * 0.85, 0.0, 1.0));
            shots.add(new Shot(grabLabels[f], still, NO_WINGS, new double[] {6.6, 3.3, 8.8}, new double[] {1.0, 1.9, 1.6}, tris));
        }
        sheet(file, "3.1.0 in game: the new legs and mask; the hunt strikes what a leg reaches and holds its catch beside him", shots, 3, 600, 470);
    }

    /**
     * The final kakuja's legs at rest (3.1.0, lowered): out of his hips between the upper and lower tentacle pairs, heads
     * hooking forward at his sides; from the third-person camera, the front, the side and above.
     */
    static void finalLegs(File file) throws Exception {
        RigPose still = new RigPose();
        com.hex.kagune.control.ayato.CentipedeMotion.Act rest = com.hex.kagune.control.ayato.CentipedeMotion.Act.REST;
        List<WingPreview.Tri> tris = new ArrayList<>(maskTris(still));
        for (int side = -1; side <= 1; side += 2) {
            tris.addAll(gameLeg(settled(motionInput(side, true, 1000, rest, null, 0.3, false, 0.0)), side, 1000));
        }
        tris.addAll(kanekiTentacles());
        double[] at = {0.0, 1.6, 0.0};
        List<Shot> shots = new ArrayList<>();
        shots.add(new Shot("Third person (behind, above)", still, NO_WINGS, new double[] {1.4, 6.2, -13.5}, at, tris));
        shots.add(new Shot("Front, three quarters", still, NO_WINGS, new double[] {9.5, 3.4, 13.0}, at, tris));
        shots.add(new Shot("Side", still, NO_WINGS, new double[] {15.0, 2.0, 0.6}, at, tris));
        shots.add(new Shot("Above", still, NO_WINGS, new double[] {0.0, 17.0, -1.0}, at, tris));
        sheet(file, "Final kakuja at rest (1000, 7.5-block legs): the legs between the upper and lower tentacles", shots, 2, 600, 470);
    }

    /** Both of Kaneki's kakuja with the new legs and mask: the first (750) and the final (1000). */
    static void kanekiKakuja(File file) throws Exception {
        RigPose still = new RigPose();
        com.hex.kagune.control.ayato.CentipedeMotion.Act rest = com.hex.kagune.control.ayato.CentipedeMotion.Act.REST;
        List<WingPreview.Tri> first = new ArrayList<>(maskTris(still));
        List<WingPreview.Tri> last = new ArrayList<>(maskTris(still));
        for (int side = -1; side <= 1; side += 2) {
            first.addAll(gameLeg(settled(motionInput(side, false, 750, rest, null, 0.3, false, 0.0)), side, 750));
            last.addAll(gameLeg(settled(motionInput(side, true, 1000, rest, null, 0.3, false, 0.0)), side, 1000));
        }
        last.addAll(kanekiTentacles());
        double[] face = {0.0, 1.55, 0.0};
        List<Shot> shots = new ArrayList<>();
        shots.add(new Shot("First kakuja (Centipede Form, 750): front, three quarters", still, NO_WINGS, new double[] {4.5, 3.0, 9.0},
            new double[] {-0.9, 2.2, -0.3}, first));
        shots.add(new Shot("First kakuja: back, three quarters", still, NO_WINGS, new double[] {-6.0, 4.6, -8.0}, new double[] {-0.9, 2.2, -0.3}, first));
        shots.add(new Shot("First kakuja: the mask up close", still, NO_WINGS, new double[] {-0.55, 1.95, 2.7}, new double[] {0.05, 1.7, 0.0}, first));
        shots.add(new Shot("Final kakuja (Full Kakuja, 1000): front", still, NO_WINGS, new double[] {0.0, 2.6, 15.0}, new double[] {0.0, 1.8, -0.3}, last));
        shots.add(new Shot("Final kakuja: back, three quarters", still, NO_WINGS, new double[] {-8.5, 5.5, -11.5}, new double[] {0.0, 1.8, -0.3}, last));
        shots.add(new Shot("Final kakuja: front, three quarters", still, NO_WINGS, new double[] {9.5, 3.4, 12.0}, new double[] {0.0, 1.8, -0.3}, last));
        sheet(file, "Kaneki's kakuja in game (3.1.0): first kakuja (top), final kakuja with its legs between the tentacles (bottom)", shots, 3, 600,
            470);
    }

    /**
     * 5.0.0: Kaneki's final kakuja bursting out of his back (the legs tear out one after the other and overshoot, the tentacles
     * erupt, the mask creeps over his face from the lens) and shattering (legs, tentacles and mask break into pieces that fly off
     * him, bounce and fade), with the game's own timing ({@code KakujaBurst}).
     */
    static void kakujaBurst(File file) throws Exception {
        RigPose still = new RigPose();
        com.hex.kagune.control.ayato.CentipedeMotion.Act rest = com.hex.kagune.control.ayato.CentipedeMotion.Act.REST;
        double[][] upper = gameTentacle(1.88, 1.72, 0.5, 1.2, 1);
        double[][] lower = gameTentacle(2.34, -0.44, 0.72, 1.34, -1);
        double[][][] tentacles = {upper, mirrored(upper), lower, mirrored(lower)};
        double[] radii = {0.22, 0.22, 0.2, 0.2};
        double[] back = {-6.0, 4.4, -8.5};
        double[] middle = {0.0, 2.0, -0.4};
        List<Shot> shots = new ArrayList<>();
        for (double age : new double[] {2.0, 3.5, 5.0, 7.0, 10.0, 17.0}) {
            List<WingPreview.Tri> tris = new ArrayList<>(burstMask(still, age));
            for (int side = -1, order = 0; side <= 1; side += 2, order++) {
                tris.addAll(burstLeg(settled(motionInput(side, true, 1000, rest, null, 0.3, false, 0.0)), side, order, age));
            }
            for (int t = 0; t < 4; t++) {
                double out = com.hex.kagune.control.KakujaBurst.tentacleOut(age, t);
                if (out > 0.01) {
                    tris.addAll(rinkaku(com.hex.kagune.control.KakujaBurst.extrude(tentacles[t], out, 1.0), radii[t], 0xA50F37, 0x18030B));
                }
            }
            shots.add(new Shot(String.format("Burst, tick %.1f", age), still, NO_WINGS, back, middle, tris));
        }
        for (double age : new double[] {8.0, 18.0, 26.0, 34.0, 42.0, 50.0}) {
            shots.add(new Shot(String.format("The mask growing in, tick %.0f", age), still, NO_WINGS, new double[] {-0.55, 1.95, 2.7},
                new double[] {0.05, 1.7, 0.0}, burstMask(still, age)));
        }
        List<com.hex.kagune.control.KakujaBurst.Quad> whole = new ArrayList<>();
        whole.addAll(quads(sink -> com.hex.kagune.control.kakugan.CentipedeMask.build(sink, com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT),
            q -> onPart(still, "head", q)));
        for (int s = -1; s <= 1; s += 2) {
            int side = s;
            com.hex.kagune.control.ayato.CentipedeMotion.Leg leg = settled(motionInput(side, true, 1000, rest, null, 0.3, false, 0.0));
            double length = com.hex.kagune.control.ayato.CentipedeMotion.length(1000);
            double width = com.hex.kagune.control.ayato.CentipedeMotion.width(length);
            int plates = com.hex.kagune.control.ayato.CentipedeMotion.plates(length, width);
            com.hex.kagune.control.ayato.KakujaModel.Spine spine = com.hex.kagune.control.ayato.CentipedeTail.through(leg.points(), 40, side, leg.dorsal());
            whole.addAll(quads(sink -> com.hex.kagune.control.ayato.CentipedeTail.tail(sink, spine, new com.hex.kagune.control.ayato.CentipedeTail.Look(
                width, plates, false, 1.0, side > 0 ? 1 : 2, 0.0, 1.0, 0.35, 0.0)), q -> q));
        }
        List<com.hex.kagune.control.KakujaBurst.Quad> tubes = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            for (WingPreview.Tri tri : rinkaku(tentacles[t], radii[t], 0xA50F37, 0x18030B)) {
                double[][] v = tri.p();
                tubes.add(new com.hex.kagune.control.KakujaBurst.Quad(v[0], v[1], v[2], v[2], tri.n(), tri.argb()[0], tri.argb()[1], tri.argb()[2],
                    tri.argb()[2], false));
            }
        }
        List<com.hex.kagune.control.KakujaBurst.Shard> shards = new ArrayList<>();
        List<com.hex.kagune.control.KakujaBurst.Quad> mask = new ArrayList<>();
        List<com.hex.kagune.control.KakujaBurst.Quad> legs = new ArrayList<>();
        int maskQuads = quads(sink -> com.hex.kagune.control.kakugan.CentipedeMask.build(sink, com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT),
            q -> q).size();
        mask.addAll(whole.subList(0, maskQuads));
        legs.addAll(whole.subList(maskQuads, whole.size()));
        double[] body = {0.0, 1.8 * 0.55, 0.0};
        shards.addAll(com.hex.kagune.control.KakujaBurst.shatter(legs, 0.55, body, 0.0, 1.0, 7L));
        shards.addAll(com.hex.kagune.control.KakujaBurst.shatter(tubes, 0.45, body, 0.0, 1.1, 8L));
        shards.addAll(com.hex.kagune.control.KakujaBurst.shatter(mask, 0.085, body, 0.0, 0.55, 9L));
        System.out.printf("  shatter: %d quads in %d pieces%n", whole.size() + tubes.size(), shards.size());
        int age = 0;
        for (int at : new int[] {1, 4, 8, 14, 24, 34}) {
            while (age < at) {
                shards.forEach(com.hex.kagune.control.KakujaBurst.Shard::step);
                age++;
            }
            shots.add(new Shot("Shatter, tick " + at, still, NO_WINGS, new double[] {7.5, 4.2, 10.5}, new double[] {0.0, 1.6, 0.0}, shardTris(shards)));
        }
        sheet(file, "Kaneki's final kakuja (5.0.0): bursting out of his back (top), the mask growing in (middle), shattering (bottom)", shots, 6,
            420, 360);
    }

    /** One leg tearing out at {@code age} ticks into the burst (CentipedeRenderer's own steps). */
    static List<WingPreview.Tri> burstLeg(com.hex.kagune.control.ayato.CentipedeMotion.Leg leg, int side, int order, double age) {
        double out = com.hex.kagune.control.KakujaBurst.legOut(age, order);
        if (out <= 0.01) {
            return List.of();
        }
        double length = com.hex.kagune.control.ayato.CentipedeMotion.length(1000);
        double width = com.hex.kagune.control.ayato.CentipedeMotion.width(length);
        int plates = com.hex.kagune.control.ayato.CentipedeMotion.plates(length, width);
        double[][] points = com.hex.kagune.control.KakujaBurst.extrude(leg.points(), out, com.hex.kagune.control.KakujaBurst.legStretch(age, order));
        com.hex.kagune.control.ayato.KakujaModel.Spine spine = com.hex.kagune.control.ayato.CentipedeTail.through(points, 64, side, leg.dorsal());
        double swell = com.hex.kagune.control.KakujaBurst.legSwell(age, order);
        double jaw = Math.max(leg.jaw(), com.hex.kagune.control.KakujaBurst.legJaw(age, order));
        double flare = Math.max(leg.flare(), 1.0 + 0.5 * (1.0 - out));
        int shown = Math.max(2, (int)Math.round(plates * out));
        return placed(sink -> com.hex.kagune.control.ayato.CentipedeTail.tail(sink, spine, new com.hex.kagune.control.ayato.CentipedeTail.Look(
            width * swell, shown, true, 1.0, side > 0 ? 1 : 2, jaw, flare, 0.35, leg.phase())), q -> q);
    }

    /** The mask at {@code age} ticks into the burst. */
    static List<WingPreview.Tri> burstMask(RigPose pose, double age) {
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT;
        double[] lens = com.hex.kagune.control.kakugan.CentipedeMask.lens(eyes);
        double front = com.hex.kagune.control.KakujaBurst.maskFront(age);
        return placed(sink -> com.hex.kagune.control.kakugan.CentipedeMask.build(com.hex.kagune.control.KakujaBurst.growingMask(sink, lens, front), eyes),
            q -> onPart(pose, "head", q));
    }

    /** Quads (placed) for the shatter. */
    static List<com.hex.kagune.control.KakujaBurst.Quad> quads(java.util.function.Consumer<com.hex.kagune.control.ayato.UkakuModel.Sink> build,
                                                               java.util.function.UnaryOperator<double[]> place) {
        List<com.hex.kagune.control.KakujaBurst.Quad> out = new ArrayList<>();
        build.accept((a, b, c, d, n, ca, cb, cc, cd) -> {
            double[] pa = place.apply(a);
            double[] tip = place.apply(new double[] {a[0] + n[0] * 0.25, a[1] + n[1] * 0.25, a[2] + n[2] * 0.25});
            boolean glow = n[1] == 1.0 && n[0] == 0.0 && n[2] == 0.0;
            double[] pn = glow ? new double[] {0, 1, 0} : PoseRender.norm(PoseRender.sub(tip, pa));
            out.add(new com.hex.kagune.control.KakujaBurst.Quad(pa, place.apply(b), place.apply(c), place.apply(d), pn, ca, cb, cc, cd, glow));
        });
        return out;
    }

    /** The flying pieces as triangles, turned, shrunk and faded as the game draws them. */
    static List<WingPreview.Tri> shardTris(List<com.hex.kagune.control.KakujaBurst.Shard> shards) {
        List<WingPreview.Tri> out = new ArrayList<>();
        for (com.hex.kagune.control.KakujaBurst.Shard shard : shards) {
            double fade = shard.fade(0.0);
            if (fade <= 0.01) {
                continue;
            }
            double[] at = shard.position(1.0);
            double[] rotation = shard.rotation(1.0);
            double scale = 0.35 + 0.65 * fade;
            for (int q = 0; q < shard.quads(); q++) {
                double[][] v = new double[4][];
                int[] c = new int[4];
                for (int k = 0; k < 4; k++) {
                    double[] p = shard.corner(q, k, rotation, scale);
                    v[k] = new double[] {at[0] + p[0], at[1] + p[1], at[2] + p[2]};
                    int colour = shard.colour(q, k);
                    c[k] = (int)Math.round((colour >>> 24) * fade) << 24 | (colour & 0xFFFFFF);
                }
                double[] n = shard.glow(q) ? new double[] {0, 1, 0} : shard.normal(q, rotation);
                out.add(new WingPreview.Tri(new double[][] {v[0], v[1], v[2]}, new int[] {c[0], c[1], c[2]}, n, false));
                out.add(new WingPreview.Tri(new double[][] {v[0], v[2], v[3]}, new int[] {c[0], c[2], c[3]}, n, false));
            }
        }
        return out;
    }

    /** A body recoloured as a zombie (green skin, teal shirt, blue trousers), turned by {@code yaw}, tipped by {@code tilt}, its middle at {@code at}. */
    static List<WingPreview.Tri> victim(RigPose pose, double[] at, double yaw, double tilt, double squash) {
        List<WingPreview.Tri> out = new ArrayList<>();
        double cy = Math.cos(yaw), sy = Math.sin(yaw), ct = Math.cos(tilt), st = Math.sin(tilt);
        for (WingPreview.Tri tri : body(pose)) {
            double[][] p = new double[3][];
            double mid = (tri.p()[0][1] + tri.p()[1][1] + tri.p()[2][1]) / 3.0;
            for (int k = 0; k < 3; k++) {
                double[] v = tri.p()[k];
                double x = v[0] * squash, y = v[1] - 1.0, z = v[2] * squash;
                double y1 = y * ct - z * st, z1 = y * st + z * ct;
                p[k] = new double[] {at[0] + x * cy + z1 * sy, at[1] + y1, at[2] - x * sy + z1 * cy};
            }
            int rgb = tri.argb()[0] & 0xFFFFFF;
            int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
            double lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255.0;
            int base = r > g + 12 && r > 110 ? 0x4F8A3C : mid < 0.75 ? 0x3B3E9C : mid > 1.5 ? 0x2C4A22 : 0x2A9A9A;
            double shade = Math.max(0.55, Math.min(1.15, 0.55 + lum * 1.1));
            int c = 0xFF000000 | (int)Math.min(255, (base >> 16 & 255) * shade) << 16 | (int)Math.min(255, (base >> 8 & 255) * shade) << 8
                | (int)Math.min(255, (base & 255) * shade);
            double[] n = tri.n();
            double ny1 = n[1] * ct - n[2] * st, nz1 = n[1] * st + n[2] * ct;
            out.add(new WingPreview.Tri(p, new int[] {c, c, c}, new double[] {n[0] * cy + nz1 * sy, ny1, -n[0] * sy + nz1 * cy}, true));
        }
        return out;
    }

    /** Control points of a coil round the vertical axis through {@code centre}: from {@code a0} (radians, 0 = +z) over {@code sweep}, dropping {@code drop}. */
    static double[][] coil(double[] centre, double radius, double top, double drop, double a0, double sweep, int points) {
        double[][] out = new double[points][];
        for (int i = 0; i < points; i++) {
            double f = (i + 1) / (double)points;
            double a = a0 + sweep * f;
            out[i] = new double[] {centre[0] + Math.sin(a) * radius, top - drop * f, centre[2] + Math.cos(a) * radius};
        }
        return out;
    }

    static double[][] join(double[][] a, double[][] b) {
        double[][] out = new double[a.length + b.length][];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /** One leg with the plate count following its length (plates keep their size however it is posed). */
    static List<WingPreview.Tri> posedLeg(double[][] controls, int side, double[] dorsal, int seed, double jaw, double flare) {
        com.hex.kagune.control.ayato.KakujaModel.Spine spine = com.hex.kagune.control.ayato.CentipedeTail.through(controls, 140, side, dorsal);
        int plates = (int)Math.max(5, Math.round(spine.length() / 0.8));
        System.out.printf("  leg %d: %.2f blocks, %d plates%n", seed, spine.length(), plates);
        return placed(sink -> com.hex.kagune.control.ayato.CentipedeTail.tail(sink, spine,
            new com.hex.kagune.control.ayato.CentipedeTail.Look(0.3, plates, true, 1.0, seed, jaw, flare, 0.0, 0.0)), q -> q);
    }

    /** The left leg at rest through the whole grab (only the right one hunts). */
    static List<WingPreview.Tri> restingLeft() {
        double[][] left = {{-0.12, 1.05, -0.2}, {-0.5, 1.25, -0.75}, {-1.3, 1.95, -1.05}, {-2.35, 2.5, -0.85}, {-3.25, 2.35, -0.25}, {-3.7, 1.6, 0.3}};
        return posedLeg(left, -1, new double[] {0.0, 0.0, -1.0}, 2, 0.0, 1.0);
    }

    /** Storyboard: the proposed centipede grab, key poses on a zombie (the user picked the full coil). */
    static void centipedeGrab(File file) throws Exception {
        RigPose still = new RigPose();
        RigPose dangling = new RigPose();
        dangling.leftArmX = -0.5F;
        dangling.rightArmX = -0.35F;
        dangling.rightArmZ = 0.45F;
        dangling.leftArmZ = -0.45F;
        dangling.settle(1.0F);
        double[] root = {0.12, 1.05, -0.2};
        double[] out = {1.0, 0.0, 0.0};
        double[] eye = {5.6, 3.4, 7.4};
        double[] look = {0.9, 1.9, 1.4};
        double[] stand = {1.3, 1.0, 3.3};
        List<Shot> shots = new ArrayList<>();
        // 1. Lock-on: rears back over his shoulder like a cobra, jaw opening, claws bristling.
        List<WingPreview.Tri> f1 = new ArrayList<>(restingLeft());
        f1.addAll(posedLeg(new double[][] {root, {0.4, 1.4, -0.8}, {0.95, 2.5, -1.3}, {1.3, 3.6, -0.95}, {1.35, 4.25, -0.05}, {1.3, 3.95, 0.75}},
            1, out, 1, 0.65, 1.35));
        f1.addAll(victim(still, stand, Math.PI, 0.0, 1.0));
        shots.add(new Shot("1  Lock-on (4 ticks): rears back over his shoulder, jaw opens, claws bristle", still, NO_WINGS, eye, look, f1));
        // 2. Strike: one smooth arc, head first, jaw wide.
        List<WingPreview.Tri> f2 = new ArrayList<>(restingLeft());
        f2.addAll(posedLeg(new double[][] {root, {0.4, 1.4, -0.75}, {0.95, 2.6, -0.8}, {1.3, 3.2, 0.5}, {1.35, 2.5, 1.95}, {1.32, 1.55, 2.75}},
            1, out, 1, 1.0, 1.25));
        f2.addAll(victim(still, stand, Math.PI, 0.0, 1.0));
        shots.add(new Shot("2  Strike (3 ticks): one smooth arc, head first, jaw wide", still, NO_WINGS, eye, look, f2));
        // 3. Bite and coil: the jaw clamps his chest, the front of the leg wraps once round him, claws dig in.
        double[][] approach3 = {root, {0.4, 1.4, -0.7}, {0.95, 2.45, -0.6}, {1.35, 2.75, 1.1}, {1.25, 2.05, 2.55}};
        List<WingPreview.Tri> f3 = new ArrayList<>(restingLeft());
        f3.addAll(posedLeg(join(approach3, coil(stand, 0.55, 1.85, 0.75, Math.PI + 0.25, Math.PI * 1.75, 7)), 1, new double[] {0.0, 1.0, 0.0}, 1, 0.0, 0.55));
        f3.addAll(victim(still, stand, Math.PI, 0.0, 1.0));
        shots.add(new Shot("3  Bite + coil (4 ticks): jaw clamps, one wrap round the body, claws dig in", still, NO_WINGS, eye, look, f3));
        // 4. Hoist: lifted off its feet, drawn in to the hold point beside him, carried smoothly.
        double[] held = {2.0, 2.15, 1.75};
        double[][] approach4 = {root, {0.45, 1.5, -0.6}, {1.05, 2.65, -0.55}, {1.75, 3.35, 0.45}, {1.95, 3.1, 1.2}};
        List<WingPreview.Tri> f4 = new ArrayList<>(restingLeft());
        f4.addAll(posedLeg(join(approach4, coil(held, 0.52, 2.95, 0.75, Math.PI + 0.1, Math.PI * 1.75, 7)), 1, new double[] {0.0, 1.0, 0.0}, 1, 0.0, 0.55));
        f4.addAll(victim(dangling, held, Math.PI * 0.85, 0.12, 1.0));
        shots.add(new Shot("4  Hoist (6 ticks): off its feet, drawn in beside him, never snagging", still, NO_WINGS, eye, look, f4));
        // 5. Crush: the coil cinches, the claws close in.
        List<WingPreview.Tri> f5 = new ArrayList<>(restingLeft());
        f5.addAll(posedLeg(join(approach4, coil(held, 0.43, 2.9, 0.7, Math.PI + 0.1, Math.PI * 1.85, 7)), 1, new double[] {0.0, 1.0, 0.0}, 1, 0.0, 0.25));
        f5.addAll(victim(dangling, held, Math.PI * 0.85, 0.12, 0.84));
        shots.add(new Shot("5  Crush (x1-3, 9 ticks each): the coil cinches, claws close in", still, NO_WINGS, eye, look, f5));
        // 6. Slam: swung up and over and driven into the ground in front of him; the jaw lets go at the impact.
        List<WingPreview.Tri> f6 = new ArrayList<>(restingLeft());
        f6.addAll(posedLeg(new double[][] {root, {0.4, 1.6, -0.6}, {0.95, 3.25, -0.4}, {1.35, 3.95, 1.05}, {1.4, 2.6, 2.6}, {1.35, 0.95, 3.25}},
            1, out, 1, 0.55, 1.2));
        f6.addAll(victim(dangling, new double[] {1.35, 0.3, 3.55}, Math.PI, Math.PI * 0.5, 1.0));
        shots.add(new Shot("6  Slam (or Throw): swung over, driven into the ground, let go at impact", still, NO_WINGS, eye, look, f6));
        sheet(file, "CONCEPT: the centipede grab, key poses (right leg hunts, the left stays at rest)", shots, 3, 600, 470);
    }

    /** Concept sheet: Kaneki's centipede legs rebuilt after the user's reference. */
    static void centipede(File file) throws Exception {
        RigPose still = new RigPose();
        double[] look = {-0.9, 2.2, -0.3};
        List<WingPreview.Tri> rest = centipedeTris(true, 0.0, 1.0, 0.0, 0.0);
        List<Shot> shots = new ArrayList<>();
        shots.add(new Shot("Front, three quarters (your in-game shot's angle)", still, NO_WINGS, new double[] {4.5, 3.0, 9.0}, look, rest));
        shots.add(new Shot("Back, three quarters", still, NO_WINGS, new double[] {-6.0, 4.6, -8.0}, look, rest));
        shots.add(new Shot("Side", still, NO_WINGS, new double[] {10.0, 2.6, 0.0}, look, rest));
        shots.add(new Shot("Plates up close: rims, flesh, one claw pair a plate", still, NO_WINGS, new double[] {-2.9, 1.7, 1.6},
            new double[] {-1.7, 2.2, -0.9}, rest));
        double[][] head = com.hex.kagune.control.ayato.CentipedeTail.head(com.hex.kagune.control.ayato.CentipedeTail.through(CENTIPEDE_RIGHT, 120, 1,
            new double[] {1.0, 0.0, 0.0}), new com.hex.kagune.control.ayato.CentipedeTail.Look(0.3, 7, true, 1.0, 1));
        // Looking at the curl's face, from a little behind the point.
        double[] headEye = {head[0][0] + head[2][0] * 3.8 - head[1][0] * 0.6, head[0][1] + head[2][1] * 3.8 - head[1][1] * 0.6 + 0.4,
            head[0][2] + head[2][2] * 3.8 - head[1][2] * 0.6};
        shots.add(new Shot("The head: square fang, jaw claws shut", still, NO_WINGS, headEye, head[0], rest));
        shots.add(new Shot("Jaw open, claws bristling (the grab)", still, NO_WINGS, headEye, head[0], centipedeTris(true, 1.0, 1.35, 0.0, 0.0)));
        shots.add(new Shot("Like your accessory picture: both up, fangs down", still, NO_WINGS, new double[] {0.0, 2.6, 10.0},
            new double[] {0.0, 2.2, -0.3}, centipedeHorns()));
        shots.add(new Shot("Idle: the claws ripple in a wave, head to root", still, NO_WINGS, new double[] {-2.4, 3.8, 2.6},
            new double[] {-2.2, 2.2, -0.6}, centipedeTris(true, 0.0, 1.0, 0.55, 0.0)));
        shots.add(new Shot("Far (low detail)", still, NO_WINGS, new double[] {6.0, 4.0, 13.0}, look, centipedeTris(false, 0.0, 1.0, 0.0, 0.0)));
        System.out.println("centipede quads: " + rest.size() / 2 + " fine, " + centipedeTris(false, 0.0, 1.0, 0.0, 0.0).size() / 2 + " far");
        sheet(file, "CONCEPT: Kaneki's centipede legs after your reference (one sickle claw pair a plate, boxy rimmed plates, fang + jaw)", shots,
            3, 600, 470);
    }

    /** Concept sheet: Kaneki's kakuja mask after the user's reference (one eye bare), in the centipede legs' colours. */
    static void centipedeMask(File file) throws Exception {
        RigPose still = new RigPose();
        RigPose turned = new RigPose();
        turned.headY = 0.6F;
        turned.headX = 0.35F;
        turned.side = 0.25F;
        turned.settle(1.0F);
        List<Shot> shots = new ArrayList<>();
        Object[][] views = {
            {"Front: one eye bare, the lens over the other", still, new double[] {0.0, 1.75, 2.2}, new double[] {0.0, 1.55, 0.0}},
            {"Three quarters, the lens side", still, new double[] {1.4, 1.8, 1.7}, new double[] {0.0, 1.55, 0.0}},
            {"Three quarters, the bare eye's side", still, new double[] {-1.4, 1.8, 1.7}, new double[] {0.0, 1.55, 0.0}},
            {"Side: the shell's crumples and the beak", still, new double[] {2.3, 1.7, 0.2}, new double[] {0.0, 1.55, 0.0}},
            {"Up close: lens, petals, gloss", still, new double[] {0.45, 1.78, 1.05}, new double[] {0.0, 1.66, 0.0}},
            {"It rides the head (turned and nodding)", turned, new double[] {1.2, 1.8, 2.2}, new double[] {0.0, 1.5, 0.0}},
        };
        for (Object[] view : views) {
            RigPose pose = (RigPose)view[1];
            shots.add(new Shot((String)view[0], pose, NO_WINGS, (double[])view[2], (double[])view[3], maskTris(pose)));
        }
        List<WingPreview.Tri> whole = new ArrayList<>(maskTris(still));
        whole.addAll(centipedeTris(true, 0.0, 1.0, 0.0, 0.0));
        shots.add(new Shot("With the legs (front, three quarters)", still, NO_WINGS, new double[] {3.2, 2.6, 5.6}, new double[] {-0.4, 2.2, -0.2}, whole));
        whole = new ArrayList<>(maskTris(still));
        whole.addAll(centipedeHorns());
        shots.add(new Shot("With the legs up like your accessory picture", still, NO_WINGS, new double[] {0.0, 2.3, 6.8}, new double[] {0.0, 2.1, -0.3},
            whole));
        shots.add(new Shot("Before: the original's patch and beak", still, NO_WINGS, new double[] {0.0, 1.75, 2.2}, new double[] {0.0, 1.55, 0.0},
            placed(sink -> com.hex.kagune.control.kakugan.KanekiMask.build(sink, com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT, 0xA51B37, 0x18030B),
                q -> onPart(still, "head", q))));
        sheet(file, "CONCEPT: Kaneki's mask after your reference: one eye bare, a lens over the other, the centipede's plum", shots, 3, 460, 430);
    }

    static List<WingPreview.Tri> maskTris(RigPose pose) {
        return placed(sink -> com.hex.kagune.control.kakugan.CentipedeMask.build(sink, com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT),
            q -> onPart(pose, "head", q));
    }

    /** Kaneki's kakuja mask on the head model part (3.0.0): it stays on his face through leans, turns and nods. */
    static void kanekiMask(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        RigPose still = new RigPose();
        RigPose lean = new RigPose();
        lean.lean = 0.55F;
        lean.headX = -0.35F;
        lean.rightArmX = -1.2F;
        lean.settle(1.0F);
        RigPose turned = new RigPose();
        turned.headY = 0.6F;
        turned.headX = 0.35F;
        turned.side = 0.25F;
        turned.settle(1.0F);
        RigPose swing = new RigPose();
        swing.twist = 0.5F;
        swing.lean = 0.3F;
        swing.rightArmX = -1.6F;
        swing.rightArmY = 0.4F;
        swing.drop = 0.25F;
        swing.settle(1.0F);
        Object[][] views = {
            {"Front", still, new double[] {0.0, 1.75, 2.2}, new double[] {0.0, 1.6, 0.0}},
            {"Three quarters", still, new double[] {1.4, 1.8, 1.7}, new double[] {0.0, 1.6, 0.0}},
            {"Side", still, new double[] {2.3, 1.7, 0.2}, new double[] {0.0, 1.6, 0.0}},
            {"Leaning into a strike (body bend)", lean, new double[] {1.8, 1.9, 2.6}, new double[] {0.0, 1.4, 0.3}},
            {"Head turned and nodding", turned, new double[] {1.2, 1.8, 2.2}, new double[] {0.0, 1.55, 0.0}},
            {"Twisting swing, crouched", swing, new double[] {-1.0, 1.7, 2.6}, new double[] {0.0, 1.35, 0.2}},
        };
        for (Object[] view : views) {
            RigPose pose = (RigPose)view[1];
            shots.add(new Shot((String)view[0], pose, NO_WINGS, (double[])view[2], (double[])view[3],
                placed(sink -> com.hex.kagune.control.kakugan.KanekiMask.build(sink, com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT, 0xA51B37,
                    0x18030B), q -> onPart(pose, "head", q))));
        }
        sheet(file, "Kaneki's kakuja mask on the head model: it rides every lean, turn and swing", shots, 3, 460, 400);
    }

    static RigPose braced(float amount, double time) {
        com.hex.kagune.control.ayato.AyatoBody.Pose pose = com.hex.kagune.control.ayato.AyatoBody.gatling(amount, time);
        RigPose p = new RigPose();
        p.headX = pose.headX - 0.6F * pose.torsoX;
        p.lean = pose.torsoX;
        p.rightArmX = pose.rightArmX;
        p.rightArmY = pose.rightArmY;
        p.rightArmZ = pose.rightArmZ;
        p.rightElbow = pose.rightArmBend;
        p.leftArmX = pose.leftArmX;
        p.leftArmY = pose.leftArmY;
        p.leftArmZ = pose.leftArmZ;
        p.leftElbow = pose.leftArmBend;
        p.rightLegX = pose.rightLegX;
        p.leftLegX = pose.leftLegX;
        p.settle(1.0F);
        return p;
    }

    /** No twin wings (the kakuja is drawn through the extras). */
    static final WingRig.Input NO_WINGS = new WingRig.Input(WingForm.TWIN, 500, 0.0F, 1.3, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1,
        false);

    /** Quads from a model sink as preview triangles, each corner sent through {@code place} (and its normal with it). */
    static List<WingPreview.Tri> placed(java.util.function.Consumer<com.hex.kagune.control.ayato.UkakuModel.Sink> build,
                                        java.util.function.UnaryOperator<double[]> place) {
        List<WingPreview.Tri> out = new ArrayList<>();
        build.accept((a, b, c, d, n, ca, cb, cc, cd) -> {
            double[] pa = place.apply(a), pb = place.apply(b), pc = place.apply(c), pd = place.apply(d);
            double[] tip = place.apply(new double[] {a[0] + n[0] * 0.25, a[1] + n[1] * 0.25, a[2] + n[2] * 0.25});
            double[] pn = n[1] == 1.0 && n[0] == 0.0 && n[2] == 0.0 ? new double[] {0, 1, 0} : PoseRender.norm(PoseRender.sub(tip, pa));
            out.add(new WingPreview.Tri(new double[][] {pa, pb, pc}, new int[] {ca, cb, cc}, pn, false));
            out.add(new WingPreview.Tri(new double[][] {pa, pc, pd}, new int[] {ca, cc, cd}, pn, false));
        });
        return out;
    }

    /** {@link #placed}, the glowing quads drawn as the game's layers draw them: added on at full brightness (the eyes type). */
    static List<WingPreview.Tri> placedLit(java.util.function.Consumer<com.hex.kagune.control.ayato.UkakuModel.Sink> build,
                                           java.util.function.UnaryOperator<double[]> place) {
        List<WingPreview.Tri> out = new ArrayList<>();
        build.accept((a, b, c, d, n, ca, cb, cc, cd) -> {
            double[] pa = place.apply(a), pb = place.apply(b), pc = place.apply(c), pd = place.apply(d);
            boolean glow = n[1] == 1.0 && n[0] == 0.0 && n[2] == 0.0;
            double[] tip = place.apply(new double[] {a[0] + n[0] * 0.25, a[1] + n[1] * 0.25, a[2] + n[2] * 0.25});
            double[] pn = glow ? new double[] {0, 1, 0} : PoseRender.norm(PoseRender.sub(tip, pa));
            out.add(new WingPreview.Tri(new double[][] {pa, pb, pc}, new int[] {ca, cb, cc}, pn, false, glow));
            out.add(new WingPreview.Tri(new double[][] {pa, pc, pd}, new int[] {ca, cc, cd}, pn, false, glow));
        });
        return out;
    }

    /** A point of a body part (its own pixels) in rig space (PoseRender's world has x to the player's left). */
    static double[] onPart(RigPose pose, String part, double[] local) {
        double[] w = PoseRender.point(pose, part, local);
        return new double[] {-w[0], w[1], w[2]};
    }

    /** The whole kakuja on a posed body: the six tentacles and the chest armor (no mask or horns since 3.0.0). */
    static List<WingPreview.Tri> kakujaTris(RigPose pose, int mastery, double time, double[] eye, float deploy, float detail) {
        WingRig.Input in = new WingRig.Input(WingForm.TWIN, mastery, deploy, time, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, detail, 1,
            false);
        List<WingPreview.Tri> out = new ArrayList<>();
        for (WingRig.Quad q : com.hex.kagune.control.ayato.KakujaRig.build(in, eye)) {
            float[] v = q.xyz();
            double[] n = {q.nx(), q.ny(), q.nz()};
            double[][] c = new double[4][];
            for (int k = 0; k < 4; k++) {
                c[k] = new double[] {v[k * 3], v[k * 3 + 1], v[k * 3 + 2]};
            }
            int[] a = q.argb();
            out.add(new WingPreview.Tri(new double[][] {c[0], c[1], c[2]}, new int[] {a[0], a[1], a[2]}, n, false));
            out.add(new WingPreview.Tri(new double[][] {c[0], c[2], c[3]}, new int[] {a[0], a[2], a[3]}, n, false));
        }
        double grow = Math.min(1.0, deploy * 1.4);
        out.addAll(placed(sink -> com.hex.kagune.control.ayato.KakujaModel.chest(sink, grow, 1.0), q -> onPart(pose, "torso", q)));
        return out;
    }

    /** The kakuja's tentacles for one frame of a moving body: pose, spring lag (world), back to rig space. */
    static List<WingPreview.Tri> livingTentacles(com.hex.kagune.control.ayato.KakujaMotion motion, com.hex.kagune.control.ayato.KakujaRig.Body body,
                                                 double[] position, double yaw, double ticks, double[] eye) {
        WingRig.Input in = new WingRig.Input(WingForm.TWIN, 500, 1.0F, ticks / 20.0, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1.0F, 1,
            false);
        com.hex.kagune.control.ayato.KakujaRig.Limb[] limbs = com.hex.kagune.control.ayato.KakujaRig.pose(in, body);
        com.hex.kagune.control.ayato.KakujaModel.Spine[] spines = com.hex.kagune.control.ayato.KakujaRig.spines(in, body, limbs);
        double[][][] nodes = com.hex.kagune.control.ayato.KakujaRig.nodes(spines);
        double c = Math.cos(yaw), sn = Math.sin(yaw);
        // Rig x right, z forward; the body faces +z turned by yaw (radians, + turns right).
        double[][][] world = new double[nodes.length][][];
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] == null) {
                continue;
            }
            world[i] = new double[nodes[i].length][];
            for (int k = 0; k < nodes[i].length; k++) {
                double[] r = nodes[i][k];
                world[i][k] = new double[] {position[0] + r[0] * c + r[2] * sn, position[1] + r[1], position[2] - r[0] * sn + r[2] * c};
            }
        }
        double[][][] lag = motion.step(world, ticks);
        double[][][] offsets = new double[lag.length][][];
        for (int i = 0; i < lag.length; i++) {
            if (lag[i] == null) {
                continue;
            }
            offsets[i] = new double[lag[i].length][];
            for (int k = 0; k < lag[i].length; k++) {
                double[] w = lag[i][k];
                offsets[i][k] = new double[] {w[0] * c - w[2] * sn, w[1], w[0] * sn + w[2] * c};
            }
        }
        List<WingRig.Quad> quads = new ArrayList<>();
        com.hex.kagune.control.ayato.KakujaRig.draw(quads, in, spines, limbs, offsets, !body.airborne());
        List<WingPreview.Tri> out = new ArrayList<>();
        for (WingRig.Quad q : quads) {
            float[] v = q.xyz();
            double[] n = {q.nx(), q.ny(), q.nz()};
            double[][] cc = new double[4][];
            for (int k = 0; k < 4; k++) {
                cc[k] = new double[] {v[k * 3], v[k * 3 + 1], v[k * 3 + 2]};
            }
            int[] a = q.argb();
            out.add(new WingPreview.Tri(new double[][] {cc[0], cc[1], cc[2]}, new int[] {a[0], a[1], a[2]}, n, false));
            out.add(new WingPreview.Tri(new double[][] {cc[0], cc[2], cc[3]}, new int[] {a[0], a[2], a[3]}, n, false));
        }
        return out;
    }

    /**
     * The tentacles alive: a little scene run through the spring chains tick by tick (stand, sprint off, stop dead, whip
     * round, jump), with frames picked out where the motion shows.
     */
    static void kakujaMotion(File file) throws Exception {
        com.hex.kagune.control.ayato.KakujaMotion motion = new com.hex.kagune.control.ayato.KakujaMotion();
        double[] position = {0.0, 0.0, 0.0};
        double yaw = 0.0;
        double velocity = 0.0;
        double vertical = 0.0;
        double stride = 0.0;
        double[] eyeSide = {7.5, 3.4, 2.5};
        double[] eyeBack = {-5.0, 4.0, -7.5};
        Object[][] wanted = {
            {30, "Standing: each one writhing on its own", eyeBack},
            {44, "Sprinting off: they trail behind", eyeSide},
            {62, "Full run: streaming back, bobbing with the stride", eyeSide},
            {72, "Stopped dead: they swing on past him", eyeSide},
            {80, "...and settle back", eyeSide},
            {98, "Whipping round (fast turn right)", eyeBack},
            {116, "Jumping: they sag as he rises", eyeSide},
            {128, "Top of the jump: they whip up and over", eyeSide},
            {138, "Falling: spread like wings", eyeSide},
            {146, "Landing: they slam down and bounce", eyeSide},
            {170, "A stretch: one reaches out, spikes bristling", eyeBack},
            {200, "Settled again", eyeBack},
        };
        List<Shot> shots = new ArrayList<>();
        int next = 0;
        boolean wasAirborne = false;
        double lastVertical = 0.0;
        int landedAt = -1000;
        float landSpeed = 0.0F;
        for (int tick = 0; tick <= 200 && next < wanted.length; tick++) {
            // The script.
            double targetSpeed = tick >= 36 && tick < 66 ? 0.34 : 0.0;
            velocity += (targetSpeed - velocity) * (targetSpeed > velocity ? 0.25 : 0.6);
            if (tick >= 90 && tick < 96) {
                yaw += Math.toRadians(15.0);
            }
            boolean airborne = position[1] > 1.0E-6 || tick == 110;
            if (tick == 110) {
                vertical = 0.42 * 1.25;
            }
            if (airborne) {
                position[1] += vertical;
                vertical = (vertical - 0.08) * 0.98;
                if (position[1] <= 0.0) {
                    position[1] = 0.0;
                    vertical = 0.0;
                }
            }
            position[0] += Math.sin(yaw) * velocity;
            position[2] += Math.cos(yaw) * velocity;
            stride += velocity * 4.0 * 0.6662 * 1.6;
            float run = (float)Math.min(1.0, velocity / 0.32);
            boolean inAir = position[1] > 1.0E-6;
            if (wasAirborne && !inAir) {
                landedAt = tick;
                landSpeed = (float)Math.max(0.0, -lastVertical);
            }
            wasAirborne = inAir;
            lastVertical = vertical;
            // The body as the game's body layer moves it in the kakuja (KakujaBody on top of vanilla's walk), and the tentacles riding it.
            float swing = (float)Math.min(1.0, velocity * 4.0);
            float sprint = (float)Math.max(0.0, Math.min(1.0, (velocity - 0.17) / 0.11));
            com.hex.kagune.control.ayato.KakujaBody.Pose kb = com.hex.kagune.control.ayato.KakujaBody.pose(new com.hex.kagune.control.ayato.KakujaBody.Motion(
                (float)stride, swing, sprint, inAir, (float)vertical, tick - landedAt, landSpeed));
            com.hex.kagune.control.ayato.KakujaRig.Body body = new com.hex.kagune.control.ayato.KakujaRig.Body(kb.lean, 0.0F, kb.twist,
                kb.drop / 16.0F * 0.9375F, run, (float)stride, (float)vertical, inAir);
            int frame = (int)wanted[next][0];
            double ticks = tick;
            if (tick == frame) {
                double[] eye = (double[])wanted[next][2];
                RigPose pose = new RigPose();
                float vanillaLeg = (float)(1.4 * Math.cos(stride) * swing);
                float vanillaArm = (float)(Math.cos(stride + Math.PI) * swing);
                pose.lean = kb.lean;
                pose.twist = kb.twist;
                pose.headX = kb.headX;
                pose.rightLegX = vanillaLeg + kb.rightLegX;
                pose.leftLegX = -vanillaLeg + kb.leftLegX;
                pose.rightLegZ = kb.rightLegZ;
                pose.leftLegZ = kb.leftLegZ;
                pose.rightKnee = kb.rightKnee;
                pose.leftKnee = kb.leftKnee;
                pose.rightArmX = vanillaArm + kb.rightArmX;
                pose.leftArmX = -vanillaArm + kb.leftArmX;
                pose.rightArmZ = kb.rightArmZ;
                pose.leftArmZ = kb.leftArmZ;
                pose.rightElbow = kb.rightElbow;
                pose.leftElbow = kb.leftElbow;
                pose.drop = kb.drop + (float)(-position[1] * 16.0);
                List<WingPreview.Tri> tris = livingTentacles(motion, body, position, yaw, ticks, eye);
                // Draw the scene around the body: shift everything down by the jump height so the camera stays put.
                List<WingPreview.Tri> lifted = new ArrayList<>();
                for (WingPreview.Tri t : tris) {
                    double[][] p = new double[3][];
                    for (int k = 0; k < 3; k++) {
                        p[k] = new double[] {t.p()[k][0], t.p()[k][1] + position[1], t.p()[k][2]};
                    }
                    lifted.add(new WingPreview.Tri(p, t.argb(), t.n(), t.opaque()));
                }
                // The chest is placed on the posed body, which is already raised by the jump.
                lifted.addAll(placed(sink -> com.hex.kagune.control.ayato.KakujaModel.chest(sink, 1.0, 1.0), q -> onPart(pose, "torso", q)));
                shots.add(new Shot((String)wanted[next][1], pose, NO_WINGS, eye, new double[] {0.0, 2.0 + position[1] * 0.5, -0.4}, lifted));
                next++;
            } else {
                livingTentacles(motion, body, position, yaw, ticks, null);
            }
        }
        sheet(file, "The kakuja alive: the body runs, jumps and lands (KakujaBody), the tentacles and chest ride it on spring chains", shots, 4, 460,
            400);
    }

    /** Quads as preview triangles. */
    static List<WingPreview.Tri> tris(List<WingRig.Quad> quads) {
        List<WingPreview.Tri> out = new ArrayList<>();
        for (WingRig.Quad q : quads) {
            float[] v = q.xyz();
            double[] n = {q.nx(), q.ny(), q.nz()};
            double[][] cc = new double[4][];
            for (int k = 0; k < 4; k++) {
                cc[k] = new double[] {v[k * 3], v[k * 3 + 1], v[k * 3 + 2]};
            }
            int[] a = q.argb();
            out.add(new WingPreview.Tri(new double[][] {cc[0], cc[1], cc[2]}, new int[] {a[0], a[1], a[2]}, n, false));
            out.add(new WingPreview.Tri(new double[][] {cc[0], cc[2], cc[3]}, new int[] {a[0], a[2], a[3]}, n, false));
        }
        return out;
    }

    /** Needles in flight from tentacle {@code index}'s muzzles toward {@code target}, {@code travel} blocks out. */
    static List<WingPreview.Tri> needles(int index, double[] aim, double[] target, double travel, double[] eye, int count) {
        List<WingPreview.Tri> out = new ArrayList<>();
        double[][] from = com.hex.kagune.control.ayato.KakujaRig.muzzles(index, aim, false, count);
        for (int i = 0; i < from.length; i++) {
            double[] d = {target[0] - from[i][0], target[1] - from[i][1], target[2] - from[i][2]};
            double l = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            double[] u = {d[0] / l, d[1] / l, d[2] / l};
            double go = travel * (0.75 + 0.5 * (i % 3) / 2.0);
            double[] tip = {from[i][0] + u[0] * go, from[i][1] + u[1] * go, from[i][2] + u[2] * go};
            com.hex.kagune.control.ayato.KakujaNeedle.build((a, b, c, e, ca, cb, cc, ce) -> {
                out.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {ca, cb, cc}, new double[] {0, 1, 0}, false));
                out.add(new WingPreview.Tri(new double[][] {a, c, e}, new int[] {ca, cc, ce}, new double[] {0, 1, 0}, false));
            }, tip, u, 1.0, eye, 1.0, 0.0, 1.0, index * 7 + i, true, 1.0);
        }
        return out;
    }

    /** The kakuja firing: the tentacles turned so their spike rows face where he looks, needles leaving from many spots. */
    static void kakujaAim(File file) throws Exception {
        RigPose still = new RigPose();
        List<Shot> shots = new ArrayList<>();
        double[] ahead = {0.0, 0.0, 1.0};
        double[] target = {0.0, 1.6, 14.0};
        Object[][] views = {
            {"All six turned to fire (rows facing forward)", new double[] {6.5, 3.2, 8.5}, new double[] {0.0, 2.0, 0.5}, 1.0F, 0F},
            {"From the front: a wall of spikes", new double[] {0.0, 2.2, 10.0}, new double[] {0.0, 2.0, 0.0}, 1.0F, 0F},
            {"From behind", new double[] {-5.5, 4.2, -7.5}, new double[] {0.0, 2.0, 0.5}, 1.0F, 0F},
            {"M1: the upper pair fires (needles from many spots)", new double[] {7.5, 3.2, 3.5}, new double[] {0.0, 2.0, 3.0}, 0.35F, 1F},
            {"Looking right", new double[] {6.5, 3.2, 8.5}, new double[] {0.0, 2.0, 0.5}, 1.0F, 0F},
            {"Looking up", new double[] {7.0, 1.5, 6.0}, new double[] {0.0, 2.4, 0.5}, 1.0F, 0F},
        };
        for (int v = 0; v < views.length; v++) {
            double[] eye = (double[])views[v][1];
            double[] look = v == 4 ? norm3(new double[] {0.6, 0.0, 0.8}) : v == 5 ? norm3(new double[] {0.0, 0.55, 0.83}) : ahead;
            float[] amount = new float[6];
            float[] kick = new float[6];
            for (int i = 0; i < 6; i++) {
                amount[i] = (float)views[v][3];
            }
            List<WingPreview.Tri> extra = new ArrayList<>();
            if ((float)views[v][4] > 0F) {
                amount[0] = 1.0F;
                amount[3] = 1.0F;
                kick[0] = 0.8F;
                kick[3] = 0.8F;
                extra.addAll(needles(0, look, target, 3.5, eye, 3));
                extra.addAll(needles(3, look, target, 3.0, eye, 3));
            }
            WingRig.Input in = new WingRig.Input(WingForm.KAKUJA, 800, 1.0F, 1.3, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1.0F, 1, false);
            com.hex.kagune.control.ayato.KakujaRig.Action action = new com.hex.kagune.control.ayato.KakujaRig.Action(look, amount, kick);
            extra.addAll(tris(com.hex.kagune.control.ayato.KakujaRig.build(in, eye, com.hex.kagune.control.ayato.KakujaRig.Body.STILL, action, null)));
            shots.add(new Shot((String)views[v][0], still, NO_WINGS, eye, (double[])views[v][2], extra));
        }
        sheet(file, "Kakuja firing pose: the tentacles roll so their spike rows face the aim; needles leave from the rows", shots, 3, 600, 470);
    }

    static double[] norm3(double[] v) {
        double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / l, v[1] / l, v[2] / l};
    }

    /** An Ayato body pose as a RigPose (the body layer's way of adding it). */
    static RigPose posed(com.hex.kagune.control.ayato.AyatoBody.Pose pose) {
        RigPose p = new RigPose();
        p.twist = pose.twist;
        p.headX = pose.headX - 0.6F * (pose.torsoX + pose.torsoBend);
        p.headY = pose.headY;
        p.lean = pose.torsoX + pose.torsoBend;
        p.rightArmX = pose.rightArmX;
        p.rightArmY = pose.rightArmY;
        p.rightArmZ = pose.rightArmZ;
        p.rightElbow = pose.rightArmBend;
        p.leftArmX = pose.leftArmX;
        p.leftArmY = pose.leftArmY;
        p.leftArmZ = pose.leftArmZ;
        p.leftElbow = pose.leftArmBend;
        p.rightLegX = pose.rightLegX;
        p.rightLegZ = pose.rightLegZ;
        p.leftLegX = pose.leftLegX;
        p.leftLegZ = pose.leftLegZ;
        p.rightKnee = pose.rightKnee;
        p.leftKnee = pose.leftKnee;
        p.settle(1.0F);
        return p;
    }

    /** The kakuja's kit: the Needle Gatling, Rushing Death's throw, Harrow, the Thorn Fan. */
    static void kakujaKit(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        double[] ahead = {0.0, 0.0, 1.0};
        double[] target = {0.0, 1.6, 14.0};
        double[] eye = {7.0, 3.4, 5.0};
        double[] look = {0.0, 2.0, 2.5};
        WingRig.Input in = new WingRig.Input(WingForm.KAKUJA, 800, 1.0F, 1.3, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1.0F, 1, false);
        // J: all six on the target, one tentacle's salvo leaving.
        float[] all = {1, 1, 1, 1, 1, 1};
        float[] kick = new float[6];
        kick[1] = 0.9F;
        kick[3] = 0.4F;
        List<WingPreview.Tri> gun = new ArrayList<>(tris(com.hex.kagune.control.ayato.KakujaRig.build(in, eye, com.hex.kagune.control.ayato.KakujaRig.Body.STILL,
            new com.hex.kagune.control.ayato.KakujaRig.Action(ahead, all, kick), null)));
        gun.addAll(needles(1, ahead, target, 2.5, eye, 2));
        gun.addAll(needles(3, ahead, target, 5.5, eye, 2));
        gun.addAll(needles(0, ahead, target, 8.5, eye, 2));
        shots.add(new Shot("J held: the Needle Gatling (two a salvo, round the six)", braced(1.0F, 10.0), NO_WINGS, eye, look, gun));
        // Z: Rushing Death at the top of the jump, the flip done, all six snapping forward and throwing.
        float[] snapKick = {1, 1, 1, 1, 1, 1};
        RigPose thrown = posed(com.hex.kagune.control.ayato.AyatoBody.pose(com.hex.kagune.control.StrikePattern.RUSHING_DEATH, true, true, 0.45F, 0.42F));
        List<WingPreview.Tri> rush = new ArrayList<>(tris(com.hex.kagune.control.ayato.KakujaRig.build(in, eye,
            com.hex.kagune.control.ayato.KakujaRig.Body.STILL, new com.hex.kagune.control.ayato.KakujaRig.Action(norm3(new double[] {0.0, -0.35, 0.94}),
            all, snapKick), null)));
        double[] below = {0.0, -3.5, 12.0};
        for (int i = 0; i < 6; i++) {
            rush.addAll(needles(i, norm3(new double[] {0.0, -0.35, 0.94}), below, 2.0 + 0.4 * i, eye, 2));
        }
        shots.add(new Shot("Z: Rushing Death, the throw at the top (12 needles)", thrown, NO_WINGS, eye, look, rush));
        RigPose tucked = posed(com.hex.kagune.control.ayato.AyatoBody.pose(com.hex.kagune.control.StrikePattern.RUSHING_DEATH, true, true, 0.2F, 0.42F));
        shots.add(new Shot("Z: tucked through the backflip", tucked, NO_WINGS, eye, look, tris(com.hex.kagune.control.ayato.KakujaRig.build(in, eye,
            com.hex.kagune.control.ayato.KakujaRig.Body.STILL, new com.hex.kagune.control.ayato.KakujaRig.Action(ahead, new float[6], new float[6], 1.0F),
            null))));
        // R: Harrow, a point-blank cone.
        List<WingPreview.Tri> harrow = new ArrayList<>(tris(com.hex.kagune.control.ayato.KakujaRig.build(in, eye,
            com.hex.kagune.control.ayato.KakujaRig.Body.STILL, new com.hex.kagune.control.ayato.KakujaRig.Action(ahead, all, snapKick), null)));
        for (int i = 0; i < 6; i++) {
            double[] spreadTarget = {(i - 2.5) * 1.6, 1.6 + (i % 3 - 1) * 1.2, 9.0};
            harrow.addAll(needles(i, ahead, spreadTarget, 2.2 + 0.3 * i, eye, 3));
        }
        RigPose open = posed(com.hex.kagune.control.ayato.AyatoBody.pose(com.hex.kagune.control.StrikePattern.HARROW, true, true, 0.4F, 0.35F));
        shots.add(new Shot("R: Harrow (a point-blank cone that shoves)", open, NO_WINGS, eye, look, harrow));
        sheet(file, "First kakuja: the Needle Gatling (J), Rushing Death (Z), Harrow (R)", shots, 2, 640, 470);
    }

    /** The first kakuja: six armored tentacles with spike rows and the plated chest (bare-faced since 3.0.0). */
    static void kakuja(File file) throws Exception {
        RigPose still = new RigPose();
        double[] look = {0.0, 2.0, -0.3};
        List<Shot> shots = new ArrayList<>();
        Object[][] views = {
            {"Back, three quarters", new double[] {-7.0, 4.6, -8.0}, look},
            {"Front, three quarters", new double[] {6.5, 3.4, 8.0}, look},
            {"Front", new double[] {0.0, 2.4, 10.5}, look},
            {"Side", new double[] {11.0, 2.4, 0.4}, look},
            {"The spike rows, from below", new double[] {4.2, 0.35, 3.6}, new double[] {1.9, 2.0, -0.8}},
            {"Back: the six sockets", new double[] {-1.4, 2.2, -3.6}, new double[] {0.0, 1.35, 0.0}},
            {"Bare face, chest plates", new double[] {0.9, 1.85, 2.6}, new double[] {0.0, 1.45, 0.0}},
            {"Low, from the front (the kakuja art's angle)", new double[] {3.2, 0.9, 6.0}, new double[] {0.0, 2.1, 0.0}},
            {"Coming out (deploy 0.5)", new double[] {-6.0, 3.6, -7.0}, look},
        };
        for (int i = 0; i < views.length; i++) {
            double[] eye = (double[])views[i][1];
            float deploy = i == views.length - 1 ? 0.5F : 1.0F;
            shots.add(new Shot((String)views[i][0], still, NO_WINGS, eye, (double[])views[i][2], kakujaTris(still, 500, 1.3 + i * 0.37, eye, deploy, 1.0F)));
        }
        System.out.println("kakuja quads: " + com.hex.kagune.control.ayato.KakujaRig.build(new WingRig.Input(WingForm.TWIN, 500, 1.0F, 1.3, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1, false), null).size()
            + " fine, " + com.hex.kagune.control.ayato.KakujaRig.build(new WingRig.Input(WingForm.TWIN, 500, 1.0F, 1.3, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 0.3F, 1, false), null).size() + " far");
        sheet(file, "First kakuja (mastery 500): six armored tentacles, spike rows on the belly, plated chest, bare face", shots, 3, 600, 470);
    }

    /** The final kakuja on a posed body: three armored tentacles (right), three fire tendrils (left), the half mask, the chest. */
    static List<WingPreview.Tri> finalKakujaTris(RigPose pose, int mastery, double time, double[] eye, float deploy, float detail) {
        WingRig.Input in = new WingRig.Input(WingForm.TWIN, mastery, deploy, time, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, detail, 1,
            false);
        List<WingPreview.Tri> out = new ArrayList<>(tris(com.hex.kagune.control.ayato.KakujaRig.build(in, eye,
            com.hex.kagune.control.ayato.KakujaRig.Body.STILL, com.hex.kagune.control.ayato.KakujaRig.Action.NONE, null, true)));
        double grow = Math.min(1.0, deploy * 1.4);
        out.addAll(placed(sink -> com.hex.kagune.control.ayato.KakujaModel.halfMask(sink, new int[] {1, 4, 5, 4}, grow, 1.0), q -> onPart(pose, "head", q)));
        out.addAll(placed(sink -> com.hex.kagune.control.ayato.KakujaModel.chest(sink, grow, 1.0, true), q -> onPart(pose, "torso", q)));
        return out;
    }

    /** The final kakuja (for approval before its combat): half armored spike tentacles, half fire tendrils, Ayato's half mask. */
    static void kakujaFinal(File file) throws Exception {
        RigPose still = new RigPose();
        double[] look = {0.0, 2.0, -0.3};
        List<Shot> shots = new ArrayList<>();
        Object[][] views = {
            {"Back, three quarters", new double[] {-7.0, 4.6, -8.0}, look},
            {"Front, three quarters", new double[] {6.5, 3.4, 8.0}, look},
            {"Front", new double[] {0.0, 2.4, 10.5}, look},
            {"Back", new double[] {0.0, 3.2, -10.5}, look},
            {"Half mask, front", new double[] {0.0, 1.78, 1.9}, new double[] {0.0, 1.72, 0.0}},
            {"Half mask, three quarters", new double[] {1.25, 1.85, 1.45}, new double[] {0.0, 1.72, 0.0}},
            {"Half mask, side (the cheek wing)", new double[] {2.0, 1.8, 0.35}, new double[] {0.0, 1.74, 0.0}},
            {"Low, from the front", new double[] {3.2, 0.9, 6.0}, new double[] {0.0, 2.1, 0.0}},
            {"Coming out (deploy 0.5)", new double[] {-6.0, 3.6, -7.0}, look},
        };
        for (int i = 0; i < views.length; i++) {
            double[] eye = (double[])views[i][1];
            float deploy = i == views.length - 1 ? 0.5F : 1.0F;
            shots.add(new Shot((String)views[i][0], still, NO_WINGS, eye, (double[])views[i][2], finalKakujaTris(still, 1000, 1.3 + i * 0.37, eye, deploy,
                1.0F)));
        }
        WingRig.Input fine = new WingRig.Input(WingForm.TWIN, 1000, 1.0F, 1.3, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1, false);
        WingRig.Input far = new WingRig.Input(WingForm.TWIN, 1000, 1.0F, 1.3, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 0.3F, 1, false);
        System.out.println("final kakuja quads: " + com.hex.kagune.control.ayato.KakujaRig.build(fine, new double[] {0, 2, 8},
            com.hex.kagune.control.ayato.KakujaRig.Body.STILL, com.hex.kagune.control.ayato.KakujaRig.Action.NONE, null, true).size() + " fine, "
            + com.hex.kagune.control.ayato.KakujaRig.build(far, new double[] {0, 2, 8}, com.hex.kagune.control.ayato.KakujaRig.Body.STILL,
            com.hex.kagune.control.ayato.KakujaRig.Action.NONE, null, true).size() + " far");
        sheet(file, "Final kakuja (mastery 1000): three armored spike tentacles, three fire tendrils, Ayato's half mask", shots, 3, 600, 470);
    }

    /** The final kakuja on a posed body doing {@code action}: tentacles and tendrils, the half mask and the chest. */
    static List<WingPreview.Tri> finalKit(RigPose pose, double[] eye, com.hex.kagune.control.ayato.KakujaRig.Action action) {
        WingRig.Input in = new WingRig.Input(WingForm.TWIN, 1000, 1.0F, 1.3, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1.0F, 1, false);
        List<WingPreview.Tri> out = new ArrayList<>(tris(com.hex.kagune.control.ayato.KakujaRig.build(in, eye,
            com.hex.kagune.control.ayato.KakujaRig.Body.STILL, action, null, true)));
        out.addAll(placed(sink -> com.hex.kagune.control.ayato.KakujaModel.halfMask(sink, new int[] {1, 4, 5, 4}, 1.0, 1.0), q -> onPart(pose, "head", q)));
        out.addAll(placed(sink -> com.hex.kagune.control.ayato.KakujaModel.chest(sink, 1.0, 1.0, true), q -> onPart(pose, "torso", q)));
        return out;
    }

    /** Fire shards (RcShard) leaving fire tendril {@code index}'s mouth toward {@code target}, {@code distances} blocks out. */
    static List<WingPreview.Tri> fireFrom(int index, double[] aim, double[] target, double size, double[] eye, double... distances) {
        double[] from = com.hex.kagune.control.ayato.KakujaRig.mouth(index, aim, false);
        double[] u = norm3(new double[] {target[0] - from[0], target[1] - from[1], target[2] - from[2]});
        List<WingPreview.Tri> out = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) {
            double d = distances[i];
            out.addAll(shard(new double[] {from[0] + u[0] * d, from[1] + u[1] * d, from[2] + u[2] * d}, u, size, 1.3 + i * 0.41, index * 5 + i, eye));
        }
        return out;
    }

    /** {@link #needles} wrapped in flame: a small fire shard riding each needle (the game wraps them in Photon fire). */
    static List<WingPreview.Tri> burningNeedles(int index, double[] aim, double[] target, double travel, double[] eye, int count) {
        List<WingPreview.Tri> out = new ArrayList<>(needles(index, aim, target, travel, eye, count));
        double[][] from = com.hex.kagune.control.ayato.KakujaRig.muzzles(index, aim, false, count);
        for (int i = 0; i < from.length; i++) {
            double[] u = norm3(new double[] {target[0] - from[i][0], target[1] - from[i][1], target[2] - from[i][2]});
            double go = travel * (0.75 + 0.5 * (i % 3) / 2.0) - 0.25;
            out.addAll(shard(new double[] {from[i][0] + u[0] * go, from[i][1] + u[1] * go, from[i][2] + u[2] * go}, u, 0.55, 1.1 + i * 0.3, index * 9 + i,
                eye));
        }
        return out;
    }

    static double[] turned(double[] aim, double degrees) {
        double yaw = Math.toRadians(degrees);
        return new double[] {aim[0] * Math.cos(yaw) + aim[2] * Math.sin(yaw), aim[1], -aim[0] * Math.sin(yaw) + aim[2] * Math.cos(yaw)};
    }

    /** The final kakuja's own kit (3.0.2): needles from the right, fire from the left, its enders, Z and J. */
    static void kakujaFinalKit(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        double[] ahead = {0.0, 0.0, 1.0};
        double[] target = {0.0, 1.6, 14.0};
        double[] eye = {9.0, 4.2, 8.0};
        double[] look = {0.6, 2.0, 2.2};
        double[] left = {-9.0, 4.2, 8.0};
        double[] leftLook = {-0.6, 2.0, 2.2};
        float[] none = new float[6];
        float[] all = {1, 1, 1, 1, 1, 1};
        // 1: Needle Pair (the upper two right tentacles).
        float[] pairAmount = {1, 1, 0.35F, 0.35F, 0.35F, 0.35F};
        float[] pairKick = {0.8F, 0.8F, 0, 0, 0, 0};
        List<WingPreview.Tri> pair = finalKit(posed(com.hex.kagune.control.ayato.AyatoBody.pose(com.hex.kagune.control.StrikePattern.NEEDLE_PAIR, true,
            false, 0.4F, 0.3F)), eye, new com.hex.kagune.control.ayato.KakujaRig.Action(ahead, pairAmount, pairKick));
        pair.addAll(needles(0, ahead, target, 3.5, eye, 3));
        pair.addAll(needles(1, ahead, target, 2.5, eye, 3));
        shots.add(new Shot("M1 1: Needle Pair (bleed)", posed(com.hex.kagune.control.ayato.AyatoBody.pose(com.hex.kagune.control.StrikePattern.NEEDLE_PAIR,
            true, false, 0.4F, 0.3F)), NO_WINGS, eye, look, pair));
        // 2: Fire Lash (the upper fire tendril whipping three shards across, mid sweep).
        float[] lashAmount = {0.35F, 0.35F, 0.35F, 1, 0.35F, 0.35F};
        float[] lashKick = {0, 0, 0, 0.8F, 0, 0};
        RigPose lashBody = posed(com.hex.kagune.control.ayato.AyatoBody.pose(com.hex.kagune.control.StrikePattern.FIRE_LASH, false, false, 0.45F, 0.35F));
        List<WingPreview.Tri> lash = finalKit(lashBody, left, new com.hex.kagune.control.ayato.KakujaRig.Action(ahead, lashAmount, lashKick, 0.0F,
            turned(ahead, 0.0)));
        lash.addAll(fireFrom(3, ahead, new double[] {-2.2, 1.6, 14.0}, 1.0, left, 6.0));
        lash.addAll(fireFrom(3, ahead, new double[] {0.0, 1.6, 14.0}, 1.0, left, 3.5));
        lash.addAll(fireFrom(3, ahead, new double[] {2.2, 1.6, 14.0}, 1.0, left, 1.2));
        shots.add(new Shot("M1 2: Fire Lash, swept left to right (burn)", lashBody, NO_WINGS, left, leftLook, lash));
        // 4: Fireball (from between the upper two tendrils).
        float[] ballAmount = {0.35F, 0.35F, 0.35F, 1, 1, 0.35F};
        float[] ballKick = {0, 0, 0, 1, 1, 0};
        RigPose ballBody = posed(com.hex.kagune.control.ayato.AyatoBody.pose(com.hex.kagune.control.StrikePattern.FIREBALL, false, false, 0.5F, 0.4F));
        List<WingPreview.Tri> ball = finalKit(ballBody, left, new com.hex.kagune.control.ayato.KakujaRig.Action(ahead, ballAmount, ballKick));
        double[] m3 = com.hex.kagune.control.ayato.KakujaRig.mouth(3, ahead, false);
        double[] m4 = com.hex.kagune.control.ayato.KakujaRig.mouth(4, ahead, false);
        double[] between = {(m3[0] + m4[0]) * 0.5, (m3[1] + m4[1]) * 0.5, (m3[2] + m4[2]) * 0.5};
        double[] ballDir = norm3(new double[] {target[0] - between[0], target[1] - between[1], target[2] - between[2]});
        ball.addAll(shard(new double[] {between[0] + ballDir[0] * 6.5, between[1] + ballDir[1] * 6.5, between[2] + ballDir[2] * 6.5}, ballDir,
            2.2 * com.hex.kagune.control.ayato.AyatoRules.FIREBALL_SIZE * 1.2, 1.7, 11, left));
        shots.add(new Shot("M1 4: Fireball (bursts, leaves the ground burning)", ballBody, NO_WINGS, left, leftLook, ball));
        // 5: Crossfire (burning needles from the right, fire from the left, crossing on the target).
        double[] close = {0.0, 1.6, 9.0};
        float[] crossKick = {0.8F, 0.8F, 0.8F, 0.8F, 0.8F, 0.8F};
        RigPose crossBody = posed(com.hex.kagune.control.ayato.AyatoBody.pose(com.hex.kagune.control.StrikePattern.CROSSFIRE, true, true, 0.45F, 0.4F));
        List<WingPreview.Tri> cross = finalKit(crossBody, eye, new com.hex.kagune.control.ayato.KakujaRig.Action(ahead, all, crossKick));
        for (int i = 0; i < 3; i++) {
            cross.addAll(burningNeedles(i, ahead, close, 3.0 + i, eye, 2));
            cross.addAll(fireFrom(3 + i, ahead, close, 1.0, eye, 2.5 + i * 1.2));
        }
        shots.add(new Shot("M1 5: Crossfire (needles in flame + fire, bleed + burn)", crossBody, NO_WINGS, eye, look, cross));
        // M2: Twin Serpents, mid sweep: needles sweeping right to left, fire left to right.
        double[] needleWay = turned(ahead, 25.0);
        double[] fireWay = turned(ahead, -20.0);
        RigPose serpentBody = posed(com.hex.kagune.control.ayato.AyatoBody.pose(com.hex.kagune.control.StrikePattern.TWIN_SERPENTS, true, true, 0.5F,
            0.35F));
        List<WingPreview.Tri> serpents = finalKit(serpentBody, eye, new com.hex.kagune.control.ayato.KakujaRig.Action(needleWay, all,
            new float[] {0.7F, 0.7F, 0.7F, 0.7F, 0.7F, 0.7F}, 0.0F, fireWay));
        for (int k = 0; k < 6; k++) {
            double yaw = 60.0 - 14.0 * k;
            double[] way = turned(ahead, yaw);
            serpents.addAll(needles(k % 3, needleWay, new double[] {way[0] * 12.0, 1.6, way[2] * 12.0}, 2.0 + 1.3 * k, eye, 1));
        }
        for (int k = 0; k < 4; k++) {
            double yaw = -50.0 + 16.0 * k;
            double[] way = turned(ahead, yaw);
            serpents.addAll(fireFrom(3 + k % 3, fireWay, new double[] {way[0] * 12.0, 1.6, way[2] * 12.0}, 0.9, eye, 6.5 - 1.5 * k));
        }
        shots.add(new Shot("M2 after 2: Twin Serpents (two fans sweeping opposite ways)", serpentBody, NO_WINGS, eye, look, serpents));
        // M2: Ember Hop: the fire tendrils beat down and throw him back.
        RigPose hopBody = posed(com.hex.kagune.control.ayato.AyatoBody.pose(com.hex.kagune.control.StrikePattern.EMBER_HOP, true, true, 0.3F, 0.2F));
        float[] beat = {0, 0, 0, 1, 1, 1};
        List<WingPreview.Tri> hop = finalKit(hopBody, left, new com.hex.kagune.control.ayato.KakujaRig.Action(ahead, beat, beat, 0.0F,
            norm3(new double[] {0.0, -0.92, 0.38})));
        for (int k = 0; k < 5; k++) {
            double a = Math.PI * 2.0 * k / 5.0;
            hop.addAll(shard(new double[] {Math.cos(a) * 1.4, 0.25, 0.6 + Math.sin(a) * 1.4}, norm3(new double[] {Math.cos(a), 0.4, Math.sin(a)}), 0.9, 1.3 + k,
                20 + k, left));
        }
        shots.add(new Shot("M2 first: Ember Hop (tendrils beat down, burst of fire)", hopBody, NO_WINGS, left, leftLook, hop));
        // M2: Hellrain, thrown up: needles and fireballs both going up.
        double[] up = norm3(new double[] {0.0, 0.85, 0.5});
        RigPose rainBody = posed(com.hex.kagune.control.ayato.AyatoBody.pose(com.hex.kagune.control.StrikePattern.HELLRAIN, true, true, 0.45F, 0.4F));
        List<WingPreview.Tri> rain = finalKit(rainBody, eye, new com.hex.kagune.control.ayato.KakujaRig.Action(up, all, all));
        double[] sky = {0.0, 12.0, 4.0};
        for (int i = 0; i < 3; i++) {
            rain.addAll(needles(i, up, sky, 2.5 + i, eye, 2));
            rain.addAll(fireFrom(3 + i, up, sky, 1.2, eye, 2.0 + i * 1.3));
        }
        shots.add(new Shot("M2 after 4: Hellrain (all thrown up, then down on them)", rainBody, NO_WINGS, eye, look, rain));
        // Z: the fire cyclone leaving the fire side, the tendrils whipped forward.
        float release = com.hex.kagune.control.ayato.AyatoRules.VORTEX_WINDUP / (float)(com.hex.kagune.control.ayato.AyatoRules.VORTEX_WINDUP + 12);
        com.hex.kagune.control.ayato.AyatoBody.Pose throwPose = com.hex.kagune.control.ayato.AyatoBody.vortex(false,
            release + com.hex.kagune.control.ayato.AyatoBody.VORTEX_FOLLOW, release);
        RigPose throwBody = new RigPose();
        throwBody.twist = throwPose.twist;
        throwBody.headX = throwPose.headX - 0.6F * (throwPose.torsoX + throwPose.torsoBend);
        throwBody.headY = throwPose.headY;
        throwBody.lean = throwPose.torsoX + throwPose.torsoBend;
        throwBody.rightArmX = throwPose.rightArmX;
        throwBody.rightArmY = throwPose.rightArmY;
        throwBody.rightArmZ = throwPose.rightArmZ;
        throwBody.rightElbow = throwPose.rightArmBend;
        throwBody.leftArmX = throwPose.leftArmX;
        throwBody.leftArmY = throwPose.leftArmY;
        throwBody.leftArmZ = throwPose.leftArmZ;
        throwBody.leftElbow = throwPose.leftArmBend;
        throwBody.rightLegX = throwPose.rightLegX;
        throwBody.leftLegX = throwPose.leftLegX;
        throwBody.settle(1.0F);
        float[] whip = {0, 0, 0, 1, 1, 1};
        double[] vortexEye = {-9.0, 4.8, -7.5};
        List<WingPreview.Tri> cyclone = finalKit(throwBody, vortexEye, new com.hex.kagune.control.ayato.KakujaRig.Action(ahead, whip, whip));
        cyclone.addAll(vortexTris(new double[] {-WingRig.ROOT_X, WingRig.ROOT_Y, WingRig.ROOT_Z}, new double[] {-0.6, 2.0, 7.0}, 1.3, vortexEye, 0.0, 1.0,
            1.4));
        shots.add(new Shot("Z: the fire cyclone, thrown from the fire side", throwBody, NO_WINGS, vortexEye, new double[] {-0.3, 2.0, 2.5}, cyclone));
        // J: the Pyre Gatling: a needle salvo, then a fireball, in turn.
        float[] gunKick = {0, 0.9F, 0, 0, 0.6F, 0};
        List<WingPreview.Tri> gun = finalKit(braced(1.0F, 10.0), eye, new com.hex.kagune.control.ayato.KakujaRig.Action(ahead, all, gunKick));
        gun.addAll(needles(1, ahead, target, 2.5, eye, 2));
        gun.addAll(needles(0, ahead, target, 7.5, eye, 2));
        gun.addAll(fireFrom(4, ahead, target, 1.5, eye, 4.5));
        gun.addAll(fireFrom(3, ahead, target, 1.5, eye, 9.0));
        shots.add(new Shot("J held: the Pyre Gatling (needles, fireball, in turn)", braced(1.0F, 10.0), NO_WINGS, eye, look, gun));
        sheet(file, "Final kakuja kit: needles from the right (bleed), fire from the left (burn), a burning body bleeds twice as hard", shots, 3, 600,
            470);
    }

    /** Both crystal wings curled forward firing (the Gatling), with a shudder of {@code barrel} radians and shot kicks. */
    static WingRig.Input gatlingWings(int mastery, float barrel, float kickRight, float kickLeft) {
        return new WingRig.Input(WingForm.TWIN, mastery, 1.0F, 1.3, 1.0F, 1.0F, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, 1.0F, 1.0F,
            false, 1.0F, 1.0F, dir(0, -3), kickRight, kickLeft, 0.0F, 0.0F, 1.0F, 1.0F, barrel);
    }

    /** J: the Gatling, both crystal wings streaming spikes. */
    static void gatling(File file) throws Exception {
        double[] ahead = dir(0, -3);
        double[] eye = {7.5, 4.2, -6.5};
        double[] look = {0.3, 2.1, 3.0};
        List<Shot> shots = new ArrayList<>();
        List<WingPreview.Tri> stream = concat(spikes(300, 1, ahead, 0.85, eye, 0.8, 4.5, 8.5, 12.5), spikes(300, -1, ahead, 0.85, eye, 2.6, 6.5, 10.5));
        shots.add(new Shot("J held: both crystal wings fire a stream (300)", braced(1.0F, 10.0), gatlingWings(300, 0.2F, 0.6F, 0.0F), eye, look, stream));
        double[] side = {10.0, 2.6, 3.5};
        List<WingPreview.Tri> streamSide = concat(spikes(300, 1, ahead, 0.85, side, 0.8, 4.5, 8.5), spikes(300, -1, ahead, 0.85, side, 2.6, 6.5));
        shots.add(new Shot("From the side", braced(1.0F, 11.0), gatlingWings(300, -0.2F, 0.0F, 0.6F), side, look, streamSide));
        double[] front = {2.5, 3.0, 12.0};
        List<WingPreview.Tri> streamFront = concat(spikes(800, 1, ahead, 0.85, front, 1.2, 5.0), spikes(800, -1, ahead, 0.85, front, 3.0, 7.0));
        shots.add(new Shot(":re (800), coming at you", braced(1.0F, 12.0), gatlingWings(800, 0.25F, 0.5F, 0.0F), front, new double[] {0.0, 2.2, 2.0},
            streamFront));
        sheet(file, "J: Gatling. Hold J in the crystal mode: both wings curl forward and stream spikes, draining RC faster and faster", shots, 3, 460, 400);
    }

    /** Twin wings with the right one coiling ({@code coil}) or growing back ({@code grow}). */
    static WingRig.Input thrownWings(int mastery, float coil, float grow) {
        return new WingRig.Input(WingForm.TWIN, mastery, 1.0F, 1.3, 0.0F, 0.0F, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, 1.0F, 1.0F,
            false, 0.0F, 0.0F, WingRig.FORWARD, 0.0F, 0.0F, coil, 0.0F, grow, 1.0F);
    }

    static List<WingPreview.Tri> vortexTris(double[] root, double[] head, double size, double[] eye, double unwind, double dusk, double time) {
        List<WingPreview.Tri> out = new ArrayList<>();
        com.hex.kagune.control.ayato.WingVortex.build((a, b, c, d, ca, cb, cc, cd) -> {
            out.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {ca, cb, cc}, new double[] {0, 1, 0}, false));
            out.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {ca, cc, cd}, new double[] {0, 1, 0}, false));
        }, root, head, size, time, 5, eye, 1.0, unwind, dusk, true);
        return out;
    }

    /** The right shoulder blade the vortex grows from (rig space). */
    static double[] rightRoot() {
        return new double[] {WingRig.ROOT_X, WingRig.ROOT_Y, WingRig.ROOT_Z};
    }

    static RigPose throwing(float progress) {
        float release = com.hex.kagune.control.ayato.AyatoRules.VORTEX_WINDUP / (float)(com.hex.kagune.control.ayato.AyatoRules.VORTEX_WINDUP + 12);
        com.hex.kagune.control.ayato.AyatoBody.Pose pose = com.hex.kagune.control.ayato.AyatoBody.vortex(true, progress * release, release);
        RigPose p = new RigPose();
        p.twist = pose.twist;
        p.headX = pose.headX - 0.6F * (pose.torsoX + pose.torsoBend);
        p.headY = pose.headY;
        p.lean = pose.torsoX + pose.torsoBend;
        p.rightArmX = pose.rightArmX;
        p.rightArmY = pose.rightArmY;
        p.rightArmZ = pose.rightArmZ;
        p.rightElbow = pose.rightArmBend;
        p.leftArmX = pose.leftArmX;
        p.leftArmY = pose.leftArmY;
        p.leftArmZ = pose.leftArmZ;
        p.leftElbow = pose.leftArmBend;
        p.rightLegX = pose.rightLegX;
        p.leftLegX = pose.leftLegX;
        p.settle(1.0F);
        return p;
    }

    /** Z: one wing twirls up and shoots forward as a spinning vortex, attached until it ends; then it detaches and a new one grows. */
    static void vortex(File file) throws Exception {
        double[] eye = {7.5, 4.2, -6.5};
        double[] look = {0.3, 2.0, 2.5};
        float release = com.hex.kagune.control.ayato.AyatoRules.VORTEX_WINDUP / (float)(com.hex.kagune.control.ayato.AyatoRules.VORTEX_WINDUP + 12);
        float follow = (release + com.hex.kagune.control.ayato.AyatoBody.VORTEX_FOLLOW) / release;
        double[] root = rightRoot();
        List<Shot> shots = new ArrayList<>();
        shots.add(new Shot("Z: the right wing twirls up (wind-up)", throwing(0.6F), thrownWings(300, 0.55F, 1.0F), eye, look, null));
        shots.add(new Shot("Fully coiled, the moment before it shoots out", throwing(0.97F), thrownWings(300, 1.0F, 1.0F), eye, look, null));
        shots.add(new Shot("It shoots forward, still on his back", throwing(follow), thrownWings(300, 0.0F, 0.0F), eye, look,
            vortexTris(root, new double[] {0.6, 2.0, 5.5}, 0.75, eye, 0.0, 0.25, 1.3)));
        double[] far = {13.5, 6.0, -7.0};
        double[] farLook = {0.5, 1.8, 6.0};
        shots.add(new Shot("Shooting on, 12 blocks out (300)", throwing(1.0F), thrownWings(300, 0.0F, 0.0F), far, farLook,
            vortexTris(root, new double[] {1.0, 1.8, 12.0}, 0.75, far, 0.0, 0.25, 1.6)));
        shots.add(new Shot(":re (800): the full wing as a vortex", throwing(1.0F), thrownWings(800, 0.0F, 0.0F), far, farLook,
            vortexTris(root, new double[] {1.0, 1.8, 12.0}, 1.3, far, 0.0, 1.0, 1.6)));
        double[] front = {2.0, 2.6, 16.0};
        shots.add(new Shot("Coming at you", throwing(1.0F), thrownWings(800, 0.0F, 0.0F), front, new double[] {0.3, 1.9, 6.0},
            vortexTris(root, new double[] {0.6, 1.9, 9.0}, 1.3, front, 0.0, 1.0, 1.9)));
        shots.add(new Shot("It ends: detached, unwinding and fading", new RigPose(), thrownWings(300, 0.0F, 0.0F), far, farLook,
            vortexTris(new double[] {root[0] + 0.3, root[1], root[2] + 1.0}, new double[] {1.0, 1.8, 12.0}, 0.75, far, 0.55, 0.25, 1.6)));
        double[] behind = {2.5, 3.0, -4.5};
        shots.add(new Shot("A new wing grows back (40%)", new RigPose(), thrownWings(300, 0.0F, 0.4F), behind, new double[] {0.3, 2.0, 0.0}, null));
        shots.add(new Shot("Grown back (80%)", new RigPose(), thrownWings(300, 0.0F, 0.8F), behind, new double[] {0.3, 2.0, 0.0}, null));
        sheet(file, "Z: Wing Vortex. The wing shoots forward as a spinning vortex, attached until it ends, then detaches; a new one grows", shots, 3, 460, 400);
    }

    /** Twin wings crystallized up to {@code front} (0 fire .. 1 crystal to the tips), optionally aiming. */
    static WingRig.Input crystalWings(int mastery, float front, float aimRight, float aimLeft, double[] aim, float kick, double time) {
        return new WingRig.Input(WingForm.TWIN, mastery, 1.0F, time, front, front, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, 1.0F,
            1.0F, false, aimRight, aimLeft, aim, aimRight > 0.5F ? kick : 0.0F, aimLeft > 0.5F ? kick : 0.0F);
    }

    /** V: the crystal climbing the wings from the shoulder blades to the tips. */
    static void crystal(File file) throws Exception {
        RigPose still = new RigPose();
        double[] back34 = {4.0, 3.0, -5.4};
        double[] backTarget = {0.3, 2.1, 0.0};
        double[] back34Far = {6.6, 3.9, -8.6};
        double[] farTarget = {0.4, 2.5, 0.0};
        List<Shot> shots = new ArrayList<>();
        for (float front : new float[] {0.0F, 0.3F, 0.6F, 1.0F}) {
            shots.add(new Shot("Wings (300), crystal at " + Math.round(front * 100) + "%", still, crystalWings(300, front, 0, 0, WingRig.FORWARD, 0, 1.3),
                back34, backTarget, null));
        }
        for (float front : new float[] {0.0F, 0.3F, 0.6F, 1.0F}) {
            shots.add(new Shot(":re wings (800), crystal at " + Math.round(front * 100) + "%", still, crystalWings(800, front, 0, 0, WingRig.FORWARD, 0, 1.3),
                back34Far, farTarget, null));
        }
        sheet(file, "V: crystallization climbs the wings from the shoulder blades to the very tips (about 1.2 s)", shots, 4, 460, 440);
    }

    /** Crystal spikes (CrystalSpike) along the shot from the muzzle of wing {@code side}. */
    static List<WingPreview.Tri> spikes(int mastery, int side, double[] aim, double size, double[] eye, double... distances) {
        double[] t = WingRig.aimTip(mastery, WingForm.TWIN, side, aim, false);
        List<WingPreview.Tri> out = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) {
            double d = distances[i];
            double[] head = {t[0] + aim[0] * d, t[1] + aim[1] * d, t[2] + aim[2] * d};
            out.addAll(spike(head, aim, size, eye, 1.0, 0.0, i + 3));
        }
        return out;
    }

    static List<WingPreview.Tri> spike(double[] tip, double[] dir, double size, double[] eye, double glow, double heat, int seed) {
        List<WingPreview.Tri> out = new ArrayList<>();
        com.hex.kagune.control.ayato.CrystalSpike.build((a, b, c, d, ca, cb, cc, cd) -> {
            out.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {ca, cb, cc}, new double[] {0, 1, 0}, false));
            out.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {ca, cc, cd}, new double[] {0, 1, 0}, false));
        }, tip, dir, size, eye, glow, heat, 1.0, seed, true, 1.0);
        return out;
    }

    /** How deep a stuck spike's point sits in the body (blocks), as AyatoWingRenderer draws it. */
    static final double STUCK = com.hex.kagune.control.ayato.CrystalSpike.LENGTH * 1.25 * 0.2;

    /** A standing body (the same player model) {@code ahead} blocks in front, facing back toward Ayato. */
    static List<WingPreview.Tri> target(double ahead) {
        List<WingPreview.Tri> out = new ArrayList<>();
        for (WingPreview.Tri t : body(new RigPose())) {
            double[][] p = new double[3][];
            for (int k = 0; k < 3; k++) {
                p[k] = new double[] {-t.p()[k][0], t.p()[k][1], ahead - t.p()[k][2]};
            }
            int c = 0xFF000000 | (t.argb()[0] & 0xFFFFFF);
            out.add(new WingPreview.Tri(p, new int[] {c, c, c}, new double[] {-t.n()[0], t.n()[1], -t.n()[2]}, true));
        }
        return out;
    }

    /** The crystal mode's M1 and what the spikes do: flying, the lance, stuck in a body and about to burst. */
    static void spikes(File file) throws Exception {
        double[] ahead = dir(0, -4);
        double[] eye = {7.5, 4.0, -7.0};
        double[] look = {0.0, 2.0, 2.6};
        List<Shot> shots = new ArrayList<>();
        RigPose firing = body(com.hex.kagune.control.StrikePattern.CRYSTAL_SHOT, true, false, 0.42F, 0.38F, 0.25F);
        shots.add(new Shot("Crystal M1: the right wing fires a spike", firing, crystalWings(300, 1.0F, 1, 0.35F, ahead, 0.6F, 1.3), eye, look,
            spikes(300, 1, ahead, 1.0, eye, 0.6, 4.0, 8.5)));
        RigPose lance = body(com.hex.kagune.control.StrikePattern.CRYSTAL_LANCE, true, true, 0.47F, 0.45F, 0.6F);
        List<WingPreview.Tri> lanceTris = spike(new double[] {0.0, 2.2, 5.5}, WingRig.FORWARD, 2.2, eye, 1.0, 0.0, 9);
        shots.add(new Shot("L + R: Crystal Lance (runs through everything in its line)", lance, crystalWings(300, 1.0F, 1, 1, ahead, 0.6F, 1.3), eye,
            look, lanceTris));
        // Stuck spikes in a body six blocks ahead: points inside it, the backs sticking out toward Ayato.
        double[] near = {3.6, 2.4, 3.2};
        double[] onTarget = {0.0, 1.3, 6.0};
        List<WingPreview.Tri> stuck = new ArrayList<>(target(6.0));
        double[][] wounds = {{-0.15, 1.45, 5.82}, {0.12, 1.1, 5.85}, {0.05, 1.62, 5.84}, {-0.2, 0.9, 5.86}};
        double[] heats = {0.0, 0.2, 0.55, 1.0};
        for (int i = 0; i < wounds.length; i++) {
            double[] w = wounds[i];
            double[] d = {0.04 * (i - 1.5), -0.08 + 0.03 * i, 1.0};
            double[] tip = {w[0] + d[0] * STUCK, w[1] + d[1] * STUCK, w[2] + d[2] * STUCK};
            stuck.addAll(spike(tip, d, 1.25, near, 0.0, heats[i] * heats[i], 20 + i));
        }
        shots.add(new Shot("Stuck in a body (the right one about to burst)", null, crystalWings(300, 0.0F, 0, 0, ahead, 0, 1.3), near, onTarget, stuck));
        double[] close = {0.9, 1.6, 4.3};
        List<WingPreview.Tri> one = new ArrayList<>(target(6.0));
        one.addAll(spike(new double[] {0.05, 1.35, 6.0 - 0.18 + STUCK}, new double[] {0.0, -0.05, 1.0}, 1.25, close, 0.0, 0.0, 31));
        shots.add(new Shot("A spike in the chest, close up", null, crystalWings(300, 0.0F, 0, 0, ahead, 0, 1.3), close, new double[] {0.0, 1.35, 5.8}, one));
        double[] closeHot = {0.9, 1.6, 4.3};
        List<WingPreview.Tri> hot = new ArrayList<>(target(6.0));
        hot.addAll(spike(new double[] {0.05, 1.35, 6.0 - 0.18 + STUCK}, new double[] {0.0, -0.05, 1.0}, 1.25, closeHot, 0.0, 1.0, 31));
        shots.add(new Shot("The same spike a moment before it bursts", null, crystalWings(300, 0.0F, 0, 0, ahead, 0, 1.3), closeHot,
            new double[] {0.0, 1.35, 5.8}, hot));
        double[] side = {9.0, 2.2, 3.0};
        RigPose barrage = body(com.hex.kagune.control.StrikePattern.SPIKE_BARRAGE, true, true, 0.5F, 0.2F, 0.8F);
        shots.add(new Shot("LLLL + R: Spike Barrage (both wings machine-gun)", barrage, crystalWings(800, 1.0F, 1, 1, ahead, 0.6F, 1.3),
            new double[] {8.0, 4.2, -7.0}, look, concat(spikes(800, 1, ahead, 0.85, eye, 1.5, 5.0, 9.0), spikes(800, -1, ahead, 0.85, eye, 3.2, 7.0))));
        sheet(file, "Crystal mode: spikes fly far, stick in what they hit, bleed it, then burst", shots, 3, 460, 400);
    }

    @SafeVarargs
    static List<WingPreview.Tri> concat(List<WingPreview.Tri>... lists) {
        List<WingPreview.Tri> out = new ArrayList<>();
        for (List<WingPreview.Tri> list : lists) {
            out.addAll(list);
        }
        return out;
    }

    static WingRig.Input aiming(int mastery, float right, float left, double[] aim, float kick) {
        WingForm form = WingForm.TWIN;
        return new WingRig.Input(form, mastery, 1.0F, 1.3, 0.0F, 0.0F, WingRig.Motion.NONE, 0.0F, 0.4F, 0.0F, 0.0F, false, 1.0F, 1.0F,
            false, right, left, aim, right > 0.5F ? kick : 0.0F, left > 0.5F ? kick : 0.0F);
    }

    static double[] dir(double yawDegrees, double pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees), pitch = Math.toRadians(pitchDegrees);
        return new double[] {-Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)};
    }

    static void aim(File file) throws Exception {
        RigPose still = new RigPose();
        double[] ahead = dir(0, -6);
        double[] head = {0.0, 2.0, 0.6};
        double[] front = {1.0, 2.4, 11.0};
        double[] front34 = {7.0, 3.0, 8.5};
        double[] side = {12.0, 2.2, 1.0};
        double[] top = {0.01, 13.0, 0.8};
        double[] topTarget = {0.0, 1.2, 0.8};
        double[] behind = {4.0, 3.2, -9.5};
        List<Shot> shots = new ArrayList<>();
        shots.add(new Shot("Twin wings (300) at rest", still, aiming(300, 0, 0, ahead, 0), front34, head, null));
        shots.add(new Shot("Right wing aiming, front", still, aiming(300, 1, 0, ahead, 0), front, head, tip(300, 1, ahead)));
        shots.add(new Shot("Right wing aiming, front 3/4", still, aiming(300, 1, 0, ahead, 0), front34, head, tip(300, 1, ahead)));
        shots.add(new Shot("Right wing aiming, side", still, aiming(300, 1, 0, ahead, 0), side, head, tip(300, 1, ahead)));
        shots.add(new Shot("Right wing aiming, top (forward is right)", still, aiming(300, 1, 0, ahead, 0), top, topTarget, tip(300, 1, ahead)));
        shots.add(new Shot("Right wing aiming, behind", still, aiming(300, 1, 0, ahead, 0), behind, head, tip(300, 1, ahead)));
        shots.add(new Shot("Halfway (curling to aim)", still, aiming(300, 0.5F, 0, ahead, 0), front34, head, null));
        shots.add(new Shot("Just fired (the wing flares)", still, aiming(300, 1, 0, ahead, 1), front34, head, null));
        shots.add(new Shot("Ready between shots", still, aiming(300, 0.35F, 0.35F, ahead, 0), front34, head, null));
        shots.add(new Shot("Aiming up at a flyer", still, aiming(300, 1, 0, dir(0, 35), 0), side, head, tip(300, 1, dir(0, 35))));
        shots.add(new Shot("Aiming down at the ground", still, aiming(300, 1, 0, dir(0, -35), 0), side, head, tip(300, 1, dir(0, -35))));
        shots.add(new Shot("One small wing (0) aiming", still, aiming(0, 1, 0, ahead, 0), WingPreview.cam("back34", 6.0), head, tip(0, 1, ahead)));
        shots.add(new Shot("Full wings (500), right aiming, top", still, aiming(500, 1, 0, ahead, 0), top, topTarget, tip(500, 1, ahead)));
        shots.add(new Shot("Full wings (500), right aiming, side", still, aiming(500, 1, 0, ahead, 0), side, head, tip(500, 1, ahead)));
        shots.add(new Shot("Full wings (500), both aiming", still, aiming(500, 1, 1, ahead, 0), front34, head, null));
        sheet(file, "Ayato M1: the wing keeps its size and spread and curls until its tip points forward", shots, 3, 420, 380);
    }

    /** Fire shards (RcShard) at {@code distances} blocks along the shot from the muzzle of wing {@code side}. */
    static List<WingPreview.Tri> shards(int mastery, int side, double[] aim, double size, double... distances) {
        double[] t = WingRig.aimTip(mastery, WingForm.TWIN, side, aim, false);
        List<WingPreview.Tri> out = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) {
            double d = distances[i];
            double[] head = {t[0] + aim[0] * d, t[1] + aim[1] * d, t[2] + aim[2] * d};
            out.addAll(shard(head, aim, size, 1.3 + i * 0.37, i + 3, new double[] {3.4, 2.7, -6.0}));
        }
        return out;
    }

    static List<WingPreview.Tri> shard(double[] head, double[] dir, double size, double time, int seed, double[] eye) {
        List<WingPreview.Tri> out = new ArrayList<>();
        com.hex.kagune.control.ayato.RcShard.build((a, b, c, d, ca, cb, cc, cd) -> {
            out.add(new WingPreview.Tri(new double[][] {a, b, c}, new int[] {ca, cb, cc}, new double[] {0, 1, 0}, false));
            out.add(new WingPreview.Tri(new double[][] {a, c, d}, new int[] {ca, cc, cd}, new double[] {0, 1, 0}, false));
        }, head, dir, size, time, seed, eye, 1.0);
        return out;
    }

    static void shot(File file) throws Exception {
        RigPose still = new RigPose();
        double[] ahead = dir(0, -4);
        double[] head = {0.0, 1.9, 2.0};
        double[] behind = {5.2, 3.6, -8.5};
        List<Shot> shots = new ArrayList<>();
        shots.add(new Shot("Right wing fires: shards in flight", still, aiming(300, 1, 0.3F, ahead, 0.6F), behind, head,
            shards(300, 1, ahead, 1.0, 0.4, 3.0, 6.5)));
        shots.add(new Shot("Same, from the side", still, aiming(300, 1, 0.3F, ahead, 0.6F), new double[] {9.0, 1.9, 2.5}, head,
            shards(300, 1, ahead, 1.0, 0.4, 3.0, 6.5)));
        double[] near = {1.2, 1.7, 3.0};
        shots.add(new Shot("A shard close up", null, aiming(300, 0, 0, ahead, 0), new double[] {2.2, 2.0, 2.0}, near,
            shard(new double[] {1.2, 1.7, 3.0}, new double[] {-0.15, -0.05, 1.0}, 1.0, 2.0, 5, new double[] {2.2, 2.0, 2.0})));
        shots.add(new Shot("Flare fireball (size 2.2)", null, aiming(300, 0, 0, ahead, 0), new double[] {2.6, 2.0, 1.4}, near,
            shard(new double[] {1.2, 1.7, 3.0}, new double[] {-0.15, -0.05, 1.0}, 2.2, 2.0, 9, new double[] {2.6, 2.0, 1.4})));
        sheet(file, "Ayato M1: shards of the wing's fire", shots, 2, 520, 420);
    }

    /** The body for an Ayato strike frame, mapped exactly as KaguneBodyAnimation.ayatoStrike does. */
    static RigPose body(com.hex.kagune.control.StrikePattern pattern, boolean right, boolean both, float progress, float impact, float weight) {
        com.hex.kagune.control.ayato.AyatoBody.Pose pose = com.hex.kagune.control.ayato.AyatoBody.pose(pattern, right, both, progress, impact);
        float amp = 0.92F + 0.16F * weight;
        RigPose p = new RigPose();
        p.twist = pose.twist * amp;
        p.headX = pose.headX * amp - 0.6F * (pose.torsoX + pose.torsoBend) * amp;
        p.headY = pose.headY * amp;
        p.lean = (pose.torsoX + pose.torsoBend) * amp;
        p.rightArmX = pose.rightArmX * amp;
        p.rightArmY = pose.rightArmY * amp;
        p.rightArmZ = pose.rightArmZ * amp;
        p.rightElbow = pose.rightArmBend * amp;
        p.leftArmX = pose.leftArmX * amp;
        p.leftArmY = pose.leftArmY * amp;
        p.leftArmZ = pose.leftArmZ * amp;
        p.leftElbow = pose.leftArmBend * amp;
        p.rightLegX = pose.rightLegX * amp;
        p.rightLegZ = pose.rightLegZ * amp;
        p.leftLegX = pose.leftLegX * amp;
        p.leftLegZ = pose.leftLegZ * amp;
        p.rightKnee = pose.rightKnee * amp;
        p.leftKnee = pose.leftKnee * amp;
        p.flip = pose.flip;
        p.settle(1.0F);
        return p;
    }

    static WingRig.Input wings(int mastery, WingRig.Motion motion, float progress, float impact, float aimRight, float aimLeft, double[] aim,
                               float kickRight, float kickLeft) {
        return new WingRig.Input(WingForm.TWIN, mastery, 1.0F, 1.3, 0.0F, 0.0F, motion, progress, impact, 0.0F, 0.0F, false, 1.0F, 1.0F,
            false, aimRight, aimLeft, aim, kickRight, kickLeft);
    }

    record Frame(String label, RigPose pose, WingRig.Input wings, float flip, List<WingPreview.Tri> extra) {
    }

    static void moves(File file) throws Exception {
        double[] ahead = dir(0, -4);
        com.hex.kagune.control.StrikePattern P = null;
        List<Frame> frames = new ArrayList<>();
        frames.add(new Frame("Shot, right: cocking back", body(com.hex.kagune.control.StrikePattern.FLAME_SHOT, true, false, 0.18F, 0.38F, 0.25F),
            wings(300, WingRig.Motion.NONE, 0.18F, 0.38F, 0.6F, 0.35F, ahead, 0, 0), 0, null));
        frames.add(new Frame("Shot, right: fired", body(com.hex.kagune.control.StrikePattern.FLAME_SHOT, true, false, 0.42F, 0.38F, 0.25F),
            wings(300, WingRig.Motion.NONE, 0.42F, 0.38F, 1, 0.35F, ahead, 0.7F, 0), 0, shards(300, 1, ahead, 1.0, 1.2)));
        frames.add(new Frame("Shot, left (the chain alternates)", body(com.hex.kagune.control.StrikePattern.FLAME_SHOT, false, false, 0.42F, 0.38F, 0.25F),
            wings(300, WingRig.Motion.NONE, 0.42F, 0.38F, 0.35F, 1, ahead, 0, 0.7F), 0, shards(300, -1, ahead, 1.0, 1.2)));
        frames.add(new Frame("L + R: Flare", body(com.hex.kagune.control.StrikePattern.FLARE, true, true, 0.47F, 0.45F, 0.6F),
            wings(300, WingRig.Motion.NONE, 0.47F, 0.45F, 1, 1, ahead, 0.6F, 0.6F), 0, shardAhead(1.0, 2.6, 2.2)));
        frames.add(new Frame("LL + R: Rising Flame", body(com.hex.kagune.control.StrikePattern.RISING_FLAME, true, true, 0.5F, 0.45F, 0.7F),
            wings(300, WingRig.Motion.RISE, 0.5F, 0.45F, 0, 0, ahead, 0, 0), 0, null));
        frames.add(new Frame("LLL + R: Fire Fan", body(com.hex.kagune.control.StrikePattern.FIRE_FAN, true, true, 0.45F, 0.42F, 0.6F),
            wings(300, WingRig.Motion.NONE, 0.45F, 0.42F, 1, 1, dir(-10, -4), 0.5F, 0.5F), 0, null));
        frames.add(new Frame("LLLL + R: Rabbit Dash", body(com.hex.kagune.control.StrikePattern.RABBIT_DASH, true, false, 0.4F, 0.15F, 0.8F),
            wings(300, WingRig.Motion.DASH, 0.4F, 0.15F, 0, 0, ahead, 0, 0), 0, null));
        frames.add(new Frame("R alone: Rabbit Hop", body(com.hex.kagune.control.StrikePattern.RABBIT_HOP, true, true, 0.25F, 0.2F, 0.3F),
            wings(300, WingRig.Motion.HOP, 0.25F, 0.2F, 0, 0, ahead, 0, 0), 0, null));
        RigPose flipping = body(com.hex.kagune.control.StrikePattern.BLACK_RABBIT, true, true, 0.24F, 0.1F, 0.9F);
        frames.add(new Frame("LLLLL + R: Black Rabbit, the backflip", flipping, wings(300, WingRig.Motion.FLIP, 0.24F, 0.1F, 0, 0, ahead, 0, 0),
            flipping.flip, null));
        frames.add(new Frame("Black Rabbit: the hovering barrage", body(com.hex.kagune.control.StrikePattern.BLACK_RABBIT, true, true, 0.55F, 0.1F, 0.9F),
            wings(300, WingRig.Motion.FLIP, 0.55F, 0.1F, 1, 1, dir(0, -30), 0.6F, 0), 0, shards(300, 1, dir(0, -30), 0.8, 1.0, 3.5)));
        List<Shot> shots = new ArrayList<>();
        double[] eye = {7.0, 3.4, 5.4};
        double[] target = {0.0, 1.7, 0.6};
        for (Frame f : frames) {
            List<WingPreview.Tri> extra = new ArrayList<>();
            if (f.extra() != null) {
                extra.addAll(f.extra());
            }
            shots.add(new Shot(f.label(), f.pose(), f.wings(), eye, target, extra));
        }
        sheetFlipped(file, "Ayato: the M1 chain and the right-click enders (body through PlayerAnimator's real transforms)", shots, frames, 3, 420, 400);
    }

    static List<WingPreview.Tri> shardAhead(double z, double y, double size) {
        return shard(new double[] {0.0, y - 0.9, z + 1.5}, WingRig.FORWARD, size, 1.0, 4, new double[] {4.6, 2.6, 3.4});
    }

    /** Like {@link #sheet} but turning each frame's wings with the body's flip, as AyatoWingRenderer does. */
    static void sheetFlipped(File file, String title, List<Shot> shots, List<Frame> frames, int columns, int pw, int ph) throws Exception {
        List<Shot> turned = new ArrayList<>();
        for (int i = 0; i < shots.size(); i++) {
            Shot shot = shots.get(i);
            float flip = frames.get(i).flip();
            if (flip == 0.0F) {
                turned.add(shot);
                continue;
            }
            List<WingPreview.Tri> extra = new ArrayList<>(shot.extra());
            double c = Math.cos(-flip), s = Math.sin(-flip);
            for (WingRig.Quad q : WingRig.build(shot.wings(), shot.eye())) {
                float[] v = q.xyz();
                double[][] p = new double[4][];
                for (int k = 0; k < 4; k++) {
                    double y = v[k * 3 + 1] - 0.7, z = v[k * 3 + 2];
                    p[k] = new double[] {v[k * 3], 0.7 + y * c - z * s, y * s + z * c};
                }
                int[] a = q.argb();
                extra.add(new WingPreview.Tri(new double[][] {p[0], p[1], p[2]}, new int[] {a[0], a[1], a[2]}, new double[] {0, 1, 0}, false));
                extra.add(new WingPreview.Tri(new double[][] {p[0], p[2], p[3]}, new int[] {a[0], a[2], a[3]}, new double[] {0, 1, 0}, false));
            }
            // No unturned wings: draw them only through the extras.
            WingRig.Input none = new WingRig.Input(WingForm.TWIN, 300, 0.0F, 1.3, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1, false);
            turned.add(new Shot(shot.label(), shot.pose(), none, shot.eye(), shot.target(), extra));
        }
        sheet(file, title, turned, columns, pw, ph);
    }

    /** A bright marker at the muzzle (where a shot leaves the wing) and a line along the shot. */
    static List<WingPreview.Tri> tip(int mastery, int side, double[] aim) {
        double[] t = WingRig.aimTip(mastery, WingForm.TWIN, side, aim, false);
        List<WingPreview.Tri> out = new ArrayList<>();
        double r = 0.06;
        int c = 0xFF66FFEE;
        for (int k = 0; k < 12; k++) {
            double a0 = Math.PI * 2 * k / 12, a1 = Math.PI * 2 * (k + 1) / 12;
            double[] p0 = {t[0] + Math.cos(a0) * r, t[1] + Math.sin(a0) * r, t[2]};
            double[] p1 = {t[0] + Math.cos(a1) * r, t[1] + Math.sin(a1) * r, t[2]};
            out.add(new WingPreview.Tri(new double[][] {t, p0, p1}, new int[] {c, c, c}, new double[] {0, 1, 0}, true));
            double[] q0 = {t[0], t[1] + Math.cos(a0) * r, t[2] + Math.sin(a0) * r};
            double[] q1 = {t[0], t[1] + Math.cos(a1) * r, t[2] + Math.sin(a1) * r};
            out.add(new WingPreview.Tri(new double[][] {t, q0, q1}, new int[] {c, c, c}, new double[] {0, 1, 0}, true));
        }
        double[] end = {t[0] + aim[0] * 3, t[1] + aim[1] * 3, t[2] + aim[2] * 3};
        double w = 0.015;
        out.add(new WingPreview.Tri(new double[][] {{t[0], t[1] - w, t[2]}, {t[0], t[1] + w, t[2]}, {end[0], end[1] + w, end[2]}},
            new int[] {c, c, c}, new double[] {0, 1, 0}, true));
        out.add(new WingPreview.Tri(new double[][] {{t[0] - w, t[1], t[2]}, {t[0] + w, t[1], t[2]}, {end[0] + w, end[1], end[2]}},
            new int[] {c, c, c}, new double[] {0, 1, 0}, true));
        return out;
    }

    /**
     * The scale the body is drawn at: 1 (PoseRender's, the model's own pixels) for the older sheets; the game's player scale (15/16,
     * BodyFrame.SCALE) where what is drawn on the body must fit it exactly as in game (the Owl's mask on her head).
     */
    static double bodyScale = 1.0;

    /** The posed body as opaque triangles in rig space (PoseRender's world has x to the player's left). */
    static List<WingPreview.Tri> body(RigPose pose) {
        List<WingPreview.Tri> out = new ArrayList<>();
        for (PoseRender.Tri t : PoseRender.model(pose)) {
            double[][] p = new double[3][];
            for (int k = 0; k < 3; k++) {
                p[k] = new double[] {-t.v()[k][0] * bodyScale, t.v()[k][1] * bodyScale, t.v()[k][2] * bodyScale};
            }
            int c = 0xFF000000 | t.rgb();
            out.add(new WingPreview.Tri(p, new int[] {c, c, c}, new double[] {-t.n()[0], t.n()[1], t.n()[2]}, true));
        }
        return out;
    }

    static BufferedImage render(Shot shot, int w, int h) {
        WingPreview.Panel panel = new WingPreview.Panel(w, h, shot.eye(), shot.target(), fov > 0.0 ? fov : shot.pose() == null ? 70.0 : 40.0, sky);
        panel.dim = dim;
        PoseRender.skin = SKINS.get(shot);
        PoseRender.slim = SLIM.contains(shot);
        PoseRender.stiff = STIFF.contains(shot);
        List<WingPreview.Tri> tris = new ArrayList<>(shot.pose() == null ? List.of() : body(shot.pose()));
        PoseRender.skin = null;
        PoseRender.slim = false;
        PoseRender.stiff = false;
        for (WingRig.Quad q : WingRig.build(shot.wings(), shot.eye())) {
            float[] v = q.xyz();
            double[] n = {q.nx(), q.ny(), q.nz()};
            double[][] c = new double[4][];
            for (int k = 0; k < 4; k++) {
                c[k] = new double[] {v[k * 3], v[k * 3 + 1], v[k * 3 + 2]};
            }
            int[] a = q.argb();
            tris.add(new WingPreview.Tri(new double[][] {c[0], c[1], c[2]}, new int[] {a[0], a[1], a[2]}, n, false));
            tris.add(new WingPreview.Tri(new double[][] {c[0], c[2], c[3]}, new int[] {a[0], a[2], a[3]}, n, false));
        }
        if (shot.extra() != null) {
            tris.addAll(shot.extra());
        }
        List<WingPreview.Tri>[] carve = CARVES.get(shot);
        WingPreview.CARVED.set(carve == null ? new ArrayList<>() : carve[0]);
        WingPreview.OPENINGS.set(carve == null ? new ArrayList<>() : carve[1]);
        panel.draw(tris);
        WingPreview.CARVED.set(new ArrayList<>());
        WingPreview.OPENINGS.set(new ArrayList<>());
        return panel.image();
    }

    /** 7.0.2: the skin a shot's body wears (a ghoul NPC's vanilla skin); none, the stand-in suit. */
    static final java.util.Map<Shot, java.awt.image.BufferedImage> SKINS = new java.util.IdentityHashMap<>();
    /** 1.0.41: the shots whose skin is painted for the slim arms (a girl ghoul's). */
    static final java.util.Set<Shot> SLIM = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    /** 1.0.41: the shots drawn without bent elbows or knees (a ghoul NPC on vanilla's player model). */
    static final java.util.Set<Shot> STIFF = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** Wounds carved into a shot's body: {flesh, openings} (drawn before the body, see WingPreview.Panel.draw). */
    static final java.util.Map<Shot, List<WingPreview.Tri>[]> CARVES = new java.util.IdentityHashMap<>();

    /** The PoseRender part a wound sits on. */
    static String partName(com.hex.kagune.control.wound.Wound.Part part) {
        return switch (part) {
            case HEAD -> "head";
            case BODY -> "torso";
            case RIGHT_ARM -> "rightArm";
            case LEFT_ARM -> "leftArm";
            case RIGHT_LEG -> "rightLeg";
            case LEFT_LEG -> "leftLeg";
        };
    }

    /** A shot of a body with its wounds: flesh and openings carved, rims and blood laid over. */
    @SuppressWarnings("unchecked")
    static Shot wounded(String label, RigPose pose, com.hex.kagune.control.wound.WoundBook book, double[] eye, double[] target, double light) {
        List<WingPreview.Tri> flesh = new ArrayList<>();
        List<WingPreview.Tri> openings = new ArrayList<>();
        List<WingPreview.Tri> over = new ArrayList<>();
        for (com.hex.kagune.control.wound.Wound w : book.wounds()) {
            double open = book.openness(w);
            String part = partName(w.part());
            com.hex.kagune.control.wound.WoundShape.build(w, open, w.part().box(false), (role, points, argb) -> {
                double[][] world = new double[points.length][];
                for (int i = 0; i < points.length; i++) {
                    world[i] = onPart(pose, part, points[i]);
                }
                for (int i = 1; i + 1 < world.length; i++) {
                    double[][] tri = {world[0], world[i], world[i + 1]};
                    int[] colour = {lit(argb[0], light), lit(argb[i], light), lit(argb[i + 1], light)};
                    switch (role) {
                        case WALL, FLOOR, BONE -> flesh.add(new WingPreview.Tri(tri, colour, new double[] {0, 1, 0}, true, false));
                        case OPENING -> openings.add(new WingPreview.Tri(tri, colour, new double[] {0, 1, 0}, true, false));
                        default -> over.add(new WingPreview.Tri(tri, new int[] {argb[0], argb[i], argb[i + 1]}, new double[] {0, 1, 0}, false, false));
                    }
                }
            });
        }
        Shot shot = new Shot(label, pose, NO_WINGS, eye, target, over);
        CARVES.put(shot, new List[] {flesh, openings});
        return shot;
    }

    static int lit(int argb, double light) {
        int r = (int)(((argb >> 16) & 255) * light);
        int g = (int)(((argb >> 8) & 255) * light);
        int b = (int)((argb & 255) * light);
        return argb & 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** 3.0.0: wounds that follow the health, carved into the body, closing as a ghoul regenerates. */
    static void wounds(File file) throws Exception {
        com.hex.kagune.control.wound.WoundBook hurt = new com.hex.kagune.control.wound.WoundBook();
        hurt.health(20, 20, 1);
        hurt.health(15, 20, 11);
        hurt.health(9, 20, 23);
        hurt.health(5, 20, 37);
        com.hex.kagune.control.wound.WoundBook light = new com.hex.kagune.control.wound.WoundBook();
        light.health(20, 20, 1);
        light.health(16, 20, 11);
        com.hex.kagune.control.wound.WoundBook closing = new com.hex.kagune.control.wound.WoundBook();
        closing.health(20, 20, 1);
        closing.health(15, 20, 11);
        closing.health(9, 20, 23);
        closing.health(5, 20, 37);
        closing.health(11, 20, 41);
        com.hex.kagune.control.wound.WoundBook spiked = new com.hex.kagune.control.wound.WoundBook();
        spiked.health(20, 20, 1);
        spiked.health(12, 20, 5);
        spiked.puncture(com.hex.kagune.control.wound.Wound.Part.BODY, com.hex.kagune.control.wound.Wound.Face.FRONT, 2.6, 4.0, 3);
        spiked.puncture(com.hex.kagune.control.wound.Wound.Part.BODY, com.hex.kagune.control.wound.Wound.Face.FRONT, 5.5, 6.5, 4);
        spiked.puncture(com.hex.kagune.control.wound.Wound.Part.RIGHT_ARM, com.hex.kagune.control.wound.Wound.Face.FRONT, 2.0, 5.0, 5);
        spiked.puncture(com.hex.kagune.control.wound.Wound.Part.BODY, com.hex.kagune.control.wound.Wound.Face.FRONT, 4.0, 9.0, 6);
        RigPose still = new RigPose();
        double[] front = {0.6, 1.4, 2.8};
        double[] back = {-0.8, 1.5, -2.8};
        double[] look = {0.0, 1.05, 0.0};
        double[] close = {0.5, 1.25, 1.3};
        double[] closeLook = {0.0, 1.1, 0.0};
        List<Shot> shots = new ArrayList<>();
        shots.add(wounded("A light hit (4 of 20 health lost)", still, light, front, look, 1.0));
        shots.add(wounded("Badly hurt (15 of 20 lost), front", still, hurt, front, look, 1.0));
        shots.add(wounded("Badly hurt, back", still, hurt, back, look, 1.0));
        shots.add(wounded("Up close: tears down to the muscle, cuts", still, hurt, close, closeLook, 1.0));
        shots.add(wounded("Regenerating: the newest wounds close first, pink", still, closing, front, look, 1.0));
        shots.add(wounded("Ayato's spikes: deep punctures", still, spiked, close, closeLook, 1.0));
        // 3.0.3: Kaneki's tails shape the wounds: a lash's gash where it struck, a thrust's hole, a crush, a slam.
        com.hex.kagune.control.wound.WoundBook.Spot chest = new com.hex.kagune.control.wound.WoundBook.Spot(
            com.hex.kagune.control.wound.Wound.Part.BODY, com.hex.kagune.control.wound.Wound.Face.FRONT, 4.0, 5.0);
        com.hex.kagune.control.wound.WoundBook lashed = blown(com.hex.kagune.control.wound.WoundBook.Blow.SLASH, chest, 0.35, 14);
        com.hex.kagune.control.wound.WoundBook stabbed = blown(com.hex.kagune.control.wound.WoundBook.Blow.STAB,
            new com.hex.kagune.control.wound.WoundBook.Spot(com.hex.kagune.control.wound.Wound.Part.BODY, com.hex.kagune.control.wound.Wound.Face.FRONT,
            3.2, 6.0), 0.0, 14);
        com.hex.kagune.control.wound.WoundBook crushed = blown(com.hex.kagune.control.wound.WoundBook.Blow.CRUSH, null, 0.0, 13);
        com.hex.kagune.control.wound.WoundBook slammed = blown(com.hex.kagune.control.wound.WoundBook.Blow.SLAM, null, 0.0, 11);
        shots.add(wounded("Kaneki's lash: a gash where the tail struck", still, lashed, close, closeLook, 1.0));
        shots.add(wounded("Kaneki's thrust: a hole", still, stabbed, close, closeLook, 1.0));
        shots.add(wounded("Kaneki's crush: tears round the torso and arms", still, crushed, front, look, 1.0));
        shots.add(wounded("Kaneki's crush, from behind", still, crushed, back, look, 1.0));
        shots.add(wounded("Kaneki's slam: big tears down the back", still, slammed, back, look, 1.0));
        sheet(file, "Wounds: torn open by every hit, carved into the body, closing as a ghoul regenerates", shots, 3, 460, 430);
    }

    /**
     * 5.0.19: the Steel Blade's carry on {@code p} the way the game puts it (KaguneBodyAnimation.carry): the arm all the blade's,
     * en garde as far as it is {@code formed} and not taken by a {@code swing}, and guarded off the ground.
     */
    static RigPose carried(RigPose p, float formed, float time) {
        return carried(p, formed, com.hex.kagune.control.ayato.SteelSwing.hold(time, 0.0, 0.0, 0.0));
    }

    /** The Steel Blade arm turned {@code arm} (SteelSwing's, chest frame) on {@code p}, as KaguneBodyAnimation.carry puts it. */
    static RigPose carried(RigPose p, float formed, double[] arm) {
        float[] turn = com.hex.kagune.control.ayato.SteelSwing.euler(arm);
        p.rightArmHold = formed;
        p.rightArmX += formed * turn[0];
        p.rightArmY += formed * turn[1];
        p.rightArmZ += formed * turn[2];
        p.rightElbow *= 1.0F - formed;
        p.blade = (float)com.hex.kagune.control.ayato.SteelBlade.extend(formed);
        return p;
    }

    /** A Steel Blade swing at {@code progress} as the game poses it: the body's part, and the blade arm from the hold back to it. */
    static RigPose steelSwing(com.hex.kagune.control.StrikePattern pattern, float progress, float impact) {
        double[] hold = com.hex.kagune.control.ayato.SteelSwing.hold(0.0, 0.0, 0.0, 0.0);
        com.hex.kagune.control.ayato.SteelSwing.Pose s = com.hex.kagune.control.ayato.SteelSwing.swing(pattern, progress, impact, hold, hold);
        RigPose p = new RigPose();
        p.twist = s.twist;
        p.lean = s.torsoX;
        p.headX = s.headX;
        p.headY = s.headY;
        p.leftArmX = s.leftArmX;
        p.leftArmY = s.leftArmY;
        p.leftArmZ = s.leftArmZ;
        p.leftElbow = s.leftArmBend;
        p.rightLegX = s.rightLegX;
        p.leftLegX = s.leftLegX;
        p.rightKnee = s.rightKnee;
        p.leftKnee = s.leftKnee;
        p.settle(1.0F);
        return carried(p, 1.0F, s.arm);
    }

    /** 5.0.18: the final kakuja's Steel Blade (SteelBlade, the game's own model) on the posed right arm. */
    static List<WingPreview.Tri> blade(RigPose pose, double formed, double time) {
        List<WingPreview.Tri> out = new ArrayList<>();
        for (com.hex.kagune.control.ayato.SteelBlade.Quad q : com.hex.kagune.control.ayato.SteelBlade.build(formed, time, false)) {
            double[][] c = new double[4][];
            double[][] n = new double[4][];
            for (int k = 0; k < 4; k++) {
                double[] p = q.corners()[k];
                double[] m = q.normals()[k];
                c[k] = onPart(pose, "rightArm", p);
                n[k] = PoseRender.norm(PoseRender.sub(onPart(pose, "rightArm", new double[] {p[0] + m[0] * 0.05, p[1] + m[1] * 0.05,
                    p[2] + m[2] * 0.05}), c[k]));
            }
            int[] a = q.argb();
            // The rasterizer lights each triangle flat: by its corners' normals averaged (the model's smooth normals).
            out.add(new WingPreview.Tri(new double[][] {c[0], c[1], c[2]}, new int[] {a[0], a[1], a[2]}, average(n[0], n[1], n[2]), true));
            out.add(new WingPreview.Tri(new double[][] {c[0], c[2], c[3]}, new int[] {a[0], a[2], a[3]}, average(n[0], n[2], n[3]), true));
        }
        return out;
    }

    /** The ground under his feet (a dark slab at y 0), to see the blade clear it. */
    static List<WingPreview.Tri> ground() {
        double r = 4.0;
        int c = 0xFF30353D;
        double[] up = {0.0, 1.0, 0.0};
        List<WingPreview.Tri> out = new ArrayList<>();
        out.add(new WingPreview.Tri(new double[][] {{-r, 0.0, -r}, {r, 0.0, -r}, {r, 0.0, r}}, new int[] {c, c, c}, up, true));
        out.add(new WingPreview.Tri(new double[][] {{-r, 0.0, -r}, {r, 0.0, r}, {-r, 0.0, r}}, new int[] {c, c, c}, up, true));
        return out;
    }

    static double[] average(double[] a, double[] b, double[] c) {
        return PoseRender.norm(new double[] {a[0] + b[0] + c[0], a[1] + b[1] + c[1], a[2] + b[2] + c[2]});
    }

    /** The final kakuja with its steel tentacles drawn back {@code fold} of the way (the Steel Blade), the half mask and the chest. */
    static List<WingPreview.Tri> finalKitBlade(RigPose pose, double[] eye, double fold, double time) {
        WingRig.Input in = new WingRig.Input(WingForm.TWIN, 1000, 1.0F, time, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1.0F, 1, false);
        com.hex.kagune.control.ayato.KakujaRig.Body body = com.hex.kagune.control.ayato.KakujaRig.Body.STILL;
        com.hex.kagune.control.ayato.KakujaRig.Limb[] limbs = com.hex.kagune.control.ayato.KakujaRig.pose(in, body,
            com.hex.kagune.control.ayato.KakujaRig.Action.NONE);
        for (int i = 0; i < limbs.length; i++) {
            if (limbs[i].side() > 0) {
                limbs[i] = com.hex.kagune.control.ayato.KakujaRig.folded(limbs[i], fold);
            }
        }
        List<WingRig.Quad> quads = new ArrayList<>();
        com.hex.kagune.control.ayato.KakujaRig.draw(quads, in, com.hex.kagune.control.ayato.KakujaRig.spines(in, body, limbs), limbs, null, true, eye,
            true);
        List<WingPreview.Tri> out = new ArrayList<>(tris(quads));
        out.addAll(placed(sink -> com.hex.kagune.control.ayato.KakujaModel.halfMask(sink, new int[] {1, 4, 5, 4}, 1.0, 1.0), q -> onPart(pose, "head", q)));
        out.addAll(placed(sink -> com.hex.kagune.control.ayato.KakujaModel.chest(sink, 1.0, 1.0, true), q -> onPart(pose, "torso", q)));
        return out;
    }

    static List<WingPreview.Tri> with(List<WingPreview.Tri> a, List<WingPreview.Tri> b) {
        List<WingPreview.Tri> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }

    /**
     * 5.0.19: the final kakuja's Steel Blade, rebuilt after the manga: the steel tentacles drawing back into his back as plates
     * surface down his arm and the pauldron fans in, the blade driving out and unfurling, its barbs springing last; the full
     * kakuja with it out; its stance and its three swings.
     */
    static void steelBlade(File file) throws Exception {
        RigPose stance = carried(new RigPose(), 1.0F, 0.0F);
        double[] front = {-3.6, 2.0, 4.4};
        double[] look = {-0.7, 1.0, 1.0};
        double[] wideFront = {-5.2, 2.6, 7.4};
        double[] wideBack = {4.2, 3.4, -7.8};
        double[] wideLook = {-0.3, 1.5, 0.4};
        List<Shot> shots = new ArrayList<>();
        for (double formed : new double[] {0.12, 0.3, 0.5, 0.68, 0.84, 1.0}) {
            double fold = com.hex.kagune.control.ayato.SteelBlade.fold(formed);
            RigPose forming = carried(new RigPose(), (float)formed, 0.0F);
            shots.add(new Shot("C: forming (" + Math.round(formed * 100) + "%)", forming, NO_WINGS, wideFront, wideLook,
                with(blade(forming, formed, 0.0), finalKitBlade(forming, wideFront, fold, 1.3))));
        }
        shots.add(new Shot("The full final kakuja, Steel Blade out (front)", stance, NO_WINGS, wideFront, wideLook,
            with(blade(stance, 1.0, 0.4), finalKitBlade(stance, wideFront, 1.0, 1.7))));
        shots.add(new Shot("The full final kakuja, Steel Blade out (back)", stance, NO_WINGS, wideBack, wideLook,
            with(blade(stance, 1.0, 0.4), finalKitBlade(stance, wideBack, 1.0, 1.7))));
        shots.add(new Shot("Up close: the blade's flank, its barbs, the edge", stance, NO_WINGS, new double[] {-0.9, 2.5, 2.9},
            new double[] {1.15, 1.8, 1.0}, blade(stance, 1.0, 1.2)));
        shots.add(new Shot("Up close: the swallowed arm and the pauldron", stance, NO_WINGS, new double[] {1.9, 2.5, -1.6},
            new double[] {0.5, 1.55, 0.3}, blade(stance, 1.0, 1.2)));
        // The hold: relaxed, low at his right side; and running, trailing behind him.
        RigPose running = carried(new RigPose(), 1.0F, com.hex.kagune.control.ayato.SteelSwing.hold(0.0, 1.0, 1.2, 1.0));
        shots.add(new Shot("The hold, side on: relaxed, its point just off the ground", stance, NO_WINGS, new double[] {4.6, 1.6, 0.9},
            new double[] {0.6, 0.85, -0.2}, with(blade(stance, 1.0, 0.4), ground())));
        shots.add(new Shot("Running: it trails behind him", running, NO_WINGS, new double[] {4.6, 1.6, 0.9}, new double[] {0.6, 0.85, -0.3},
            with(blade(running, 1.0, 0.4), ground())));
        com.hex.kagune.control.StrikePattern sweep = com.hex.kagune.control.StrikePattern.STEEL_SWEEP;
        com.hex.kagune.control.StrikePattern back = com.hex.kagune.control.StrikePattern.STEEL_RETURN;
        com.hex.kagune.control.StrikePattern execution = com.hex.kagune.control.StrikePattern.STEEL_EXECUTION;
        com.hex.kagune.control.StrikePattern[] swings = {sweep, sweep, sweep, sweep, back, back, back, execution, execution, execution};
        float[] at = {0.24F, 0.34F, 0.4F, 0.56F, 0.24F, 0.4F, 0.56F, 0.27F, 0.45F, 0.63F};
        float[] impacts = {0.4F, 0.4F, 0.4F, 0.4F, 0.4F, 0.4F, 0.4F, 0.45F, 0.45F, 0.45F};
        String[] names = {"M1 Steel Sweep: cocked back, high", "the cut: edge leading", "impact: dead ahead, fastest", "carried through to his left",
            "Steel Return: wound across to his left", "the backhand: impact ahead", "carried out past his right", "Steel Execution: raised behind his head",
            "driven down: impact ahead", "into the lunge (stopped above the ground)"};
        for (int i = 0; i < swings.length; i++) {
            RigPose swing = steelSwing(swings[i], at[i], impacts[i]);
            shots.add(new Shot(names[i], swing, NO_WINGS, new double[] {2.6, 4.0, 6.6}, new double[] {0.0, 1.0, 1.2},
                with(blade(swing, 1.0, 0.3), ground())));
        }
        // The guard: the chop's follow-through would bury the blade; guarded it stops just above the ground.
        RigPose unguarded = steelSwing(execution, 0.63F, 0.45F);
        unguarded.blade = 0.0F;
        double[] low = {4.2, 1.1, 4.6};
        double[] lowLook = {0.6, 0.5, 0.6};
        shots.add(new Shot("Unguarded: the chop would bury it", unguarded, NO_WINGS, low, lowLook, with(blade(unguarded, 1.0, 0.3), ground())));
        RigPose guarded = steelSwing(execution, 0.63F, 0.45F);
        shots.add(new Shot("Guarded: stopped just above the ground", guarded, NO_WINGS, low, lowLook, with(blade(guarded, 1.0, 0.3), ground())));
        sheet(file, "Final kakuja, C: the Steel Blade (the steel wings closed round the right arm; reach = arm + blade)", shots, 3, 560, 480);
    }

    /**
     * 5.0.19: the Steel Blade up close: held up like the manga's panel, its flank, down its point (the cone and its edge), its
     * barbs from above, the pauldron, and the forming step by step.
     */
    static void steelBladeDetail(File file) throws Exception {
        RigPose stance = carried(new RigPose(), 1.0F, 0.0F);
        // The panel: the arm raised up and out to his right, the blade high over him.
        RigPose panel = new RigPose();
        panel.rightArmX = -0.35F;
        panel.rightArmZ = 2.05F;
        panel.twist = -0.1F;
        // Straight out to his right: its flank faces front and back, its edge down, its barbs up.
        RigPose out = new RigPose();
        out.rightArmZ = 1.57F;
        List<Shot> shots = new ArrayList<>();
        shots.add(new Shot("Like the panel: raised up and out", panel, NO_WINGS, new double[] {1.1, 2.2, 5.2}, new double[] {1.25, 2.15, 0.0},
            blade(panel, 1.0, 0.6)));
        shots.add(new Shot("Its flank: a cone with one long edge", out, NO_WINGS, new double[] {1.75, 1.45, 3.9}, new double[] {1.75, 1.45, 0.0},
            blade(out, 1.0, 0.6)));
        shots.add(new Shot("Down its point: the cone and its edge", out, NO_WINGS, new double[] {4.9, 1.75, 1.1}, new double[] {0.6, 1.4, 0.0},
            blade(out, 1.0, 0.6)));
        shots.add(new Shot("From above: the barbs down its back", out, NO_WINGS, new double[] {1.7, 4.4, 1.3}, new double[] {1.7, 1.4, 0.0},
            blade(out, 1.0, 0.6)));
        shots.add(new Shot("The pauldron and the swallowed arm", stance, NO_WINGS, new double[] {-0.9, 2.6, 2.3}, new double[] {0.45, 1.5, 0.25},
            blade(stance, 1.0, 1.1)));
        shots.add(new Shot("From behind", stance, NO_WINGS, new double[] {1.7, 2.6, -2.7}, new double[] {0.6, 1.5, 0.4}, blade(stance, 1.0, 1.1)));
        for (double formed : new double[] {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9}) {
            RigPose forming = carried(new RigPose(), (float)formed, 0.0F);
            shots.add(new Shot("Forming " + Math.round(formed * 100) + "%", forming, NO_WINGS, new double[] {-2.6, 2.1, 3.0}, new double[] {0.8, 1.45, 0.7},
                blade(forming, formed, 0.0)));
        }
        sheet(file, "The Steel Blade up close", shots, 3, 640, 520);
    }

    /** A body hit by {@code blows} in turn: {Blow, Spot (or null), angle, health after}. */
    static com.hex.kagune.control.wound.WoundBook hits(Object[]... blows) {
        com.hex.kagune.control.wound.WoundBook book = new com.hex.kagune.control.wound.WoundBook();
        book.health(20, 20, 1, 100L);
        long time = 100L;
        int seed = 17;
        for (Object[] hit : blows) {
            time += 20L;
            book.blow((com.hex.kagune.control.wound.WoundBook.Blow)hit[0], (com.hex.kagune.control.wound.WoundBook.Spot)hit[1], (Double)hit[2], time);
            book.health((Double)hit[3], 20, seed += 13, time + 1L);
        }
        return book;
    }

    static com.hex.kagune.control.wound.WoundBook.Spot spot(com.hex.kagune.control.wound.Wound.Part part, com.hex.kagune.control.wound.Wound.Face face,
                                                           double u, double v) {
        return new com.hex.kagune.control.wound.WoundBook.Spot(part, face, u, v);
    }

    /**
     * 5.0.17: the wounds rebuilt: much bigger, each hit's own kind where it landed, and the inside of the body under them: the
     * ribs and breastbone through the chest, the bone down a limb, the spine through the back, a shin snapped through the skin.
     */
    static void woundsAnatomy(File file) throws Exception {
        com.hex.kagune.control.wound.Wound.Part body = com.hex.kagune.control.wound.Wound.Part.BODY;
        com.hex.kagune.control.wound.Wound.Face front = com.hex.kagune.control.wound.Wound.Face.FRONT;
        com.hex.kagune.control.wound.WoundBook.Blow slash = com.hex.kagune.control.wound.WoundBook.Blow.SLASH;
        // The user's sketch: the chest torn open to the ribs, the outer arm and the outer leg laid open to the bone.
        com.hex.kagune.control.wound.WoundBook sketch = hits(
            new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.BURST, spot(body, front, 4.0, 4.2), 0.0, 14.0},
            new Object[] {slash, spot(com.hex.kagune.control.wound.Wound.Part.RIGHT_ARM, com.hex.kagune.control.wound.Wound.Face.RIGHT, 2.0, 4.5), 1.5, 10.0},
            new Object[] {slash, spot(com.hex.kagune.control.wound.Wound.Part.LEFT_LEG, com.hex.kagune.control.wound.Wound.Face.LEFT, 2.0, 7.0), 1.5, 6.0});
        com.hex.kagune.control.wound.WoundBook sword = hits(new Object[] {slash, spot(body, front, 4.0, 4.5), 0.25, 12.0});
        com.hex.kagune.control.wound.WoundBook armClaw = hits(new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.MAUL,
            spot(com.hex.kagune.control.wound.Wound.Part.RIGHT_ARM, front, 2.0, 5.0), 1.5, 14.0});
        com.hex.kagune.control.wound.WoundBook fall = hits(new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.FALL, null, 0.0, 9.0});
        com.hex.kagune.control.wound.WoundBook slam = hits(new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.SLAM, null, 0.0, 11.0});
        com.hex.kagune.control.wound.WoundBook burned = hits(new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.BURN, spot(body, front, 3.0, 6.0), 0.0, 13.0},
            new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.BURN, spot(com.hex.kagune.control.wound.Wound.Part.LEFT_ARM, front, 2.0, 5.0), 0.0, 11.0});
        com.hex.kagune.control.wound.WoundBook punched = hits(new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.BLUNT, spot(body, front, 5.0, 3.0), 0.0, 18.0},
            new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.BLUNT, spot(body, front, 2.5, 8.0), 0.0, 16.5},
            new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.BLUNT, spot(com.hex.kagune.control.wound.Wound.Part.LEFT_ARM, front, 2.0, 4.0), 0.0, 10.0});
        com.hex.kagune.control.wound.WoundBook blast = hits(new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.BLAST, spot(body, front, 4.5, 5.0), 0.0, 6.0});
        com.hex.kagune.control.wound.WoundBook gutted = hits(new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.BURST, spot(body, front, 4.0, 9.3), 0.0, 13.0});
        com.hex.kagune.control.wound.WoundBook arrows = hits(new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.STAB, spot(body, front, 2.5, 4.0), 0.0, 16.0},
            new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.STAB, spot(com.hex.kagune.control.wound.Wound.Part.LEFT_LEG, front, 2.0, 4.0), 0.0, 12.0});
        RigPose still = new RigPose();
        double[] look = {0.0, 1.05, 0.0};
        double[] frontEye = {0.9, 1.4, 2.9};
        double[] leftEye = {-2.6, 1.3, 1.8};
        double[] rightEye = {2.6, 1.3, 1.8};
        double[] backEye = {-0.8, 1.5, -2.8};
        double[] chestEye = {0.35, 1.35, 1.25};
        double[] chestLook = {0.0, 1.15, 0.0};
        double[] legEye = {0.4, 0.6, 1.4};
        double[] legLook = {0.0, 0.45, 0.0};
        List<Shot> shots = new ArrayList<>();
        shots.add(wounded("Healed: nothing to see", still, new com.hex.kagune.control.wound.WoundBook(), frontEye, look, 1.0));
        shots.add(wounded("The sketch: chest to the ribs, arm and leg to the bone", still, sketch, frontEye, look, 1.0));
        shots.add(wounded("The same from his right (the arm)", still, sketch, rightEye, look, 1.0));
        shots.add(wounded("The same from his left (the leg)", still, sketch, leftEye, look, 1.0));
        shots.add(wounded("Up close: ribs and breastbone over the lungs", still, sketch, chestEye, chestLook, 1.0));
        shots.add(wounded("Up close: the arm laid open to the bone", still, sketch, new double[] {1.45, 1.35, 0.5}, new double[] {0.35, 1.15, 0.0}, 1.0));
        shots.add(wounded("Up close: the leg laid open to the bone", still, sketch, new double[] {-1.3, 0.75, 0.6}, new double[] {-0.12, 0.45, 0.0}, 1.0));
        shots.add(wounded("The broken shin from the side", still, fall, new double[] {1.4, 0.7, 0.9}, legLook, 1.0));
        shots.add(wounded("A sword across the chest: a gash to the ribs", still, sword, chestEye, chestLook, 1.0));
        shots.add(wounded("A beast's claws down the arm: torn to the bone, raked", still, armClaw, frontEye, look, 1.0));
        shots.add(wounded("A long fall: the shin snaps through the skin", still, fall, legEye, legLook, 1.0));
        shots.add(wounded("Slammed: the back torn open to the spine", still, slam, backEye, look, 1.0));
        shots.add(wounded("The belly torn open: the guts", still, gutted, chestEye, new double[] {0.0, 0.95, 0.0}, 1.0));
        shots.add(wounded("An explosion in front of him", still, blast, frontEye, look, 1.0));
        shots.add(wounded("Arrows: holes", still, arrows, frontEye, look, 1.0));
        shots.add(wounded("Fire: charred, blistered burns", still, burned, frontEye, look, 1.0));
        shots.add(wounded("Fists: bruises, then a broken arm", still, punched, frontEye, look, 1.0));
        // Healing: the flesh grows back over the bone first, pink, then the skin closes.
        com.hex.kagune.control.wound.WoundBook healing = hits(new Object[] {com.hex.kagune.control.wound.WoundBook.Blow.BURST, spot(body, front, 4.0, 4.2), 0.0, 8.0});
        for (double health : new double[] {11.0, 13.5, 15.5}) {
            healing.health(health, 20, 5, 300L);
            shots.add(wounded("Healing (" + (int)health + " of 20)", still, healing, chestEye, chestLook, 1.0));
        }
        sheet(file, "Wounds 5.0.17: bigger, where each hit landed, down to the bone", shots, 3, 460, 430);
    }

    /** A body hit once by one of Kaneki's blows, down to {@code health} of 20. */
    static com.hex.kagune.control.wound.WoundBook blown(com.hex.kagune.control.wound.WoundBook.Blow blow,
                                                       com.hex.kagune.control.wound.WoundBook.Spot spot, double angle, double health) {
        com.hex.kagune.control.wound.WoundBook book = new com.hex.kagune.control.wound.WoundBook();
        book.health(20, 20, 1, 100L);
        book.blow(blow, spot, angle, 100L);
        book.health(health, 20, 17, 102L);
        return book;
    }

    /** The sky the shots are drawn against, how bright the world's light is (1 day .. low at night), the lens (0: as the shot has it). */
    static int sky = 0x3E4A58;
    static double dim = 1.0;
    static double fov = 0.0;

    /** War on a posed body: every part of {@code WarModel} on its own body part. */
    static List<WingPreview.Tri> warTris(RigPose pose, double time, double grow) {
        List<WingPreview.Tri> out = new ArrayList<>();
        String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
        com.hex.kagune.control.king.WarModel.Part[] parts = com.hex.kagune.control.king.WarModel.Part.values();
        for (int i = 0; i < parts.length; i++) {
            com.hex.kagune.control.king.WarModel.Part part = parts[i];
            String name = names[i];
            out.addAll(placedLit(sink -> com.hex.kagune.control.king.WarModel.build(sink, part, time, grow), q -> onPart(pose, name, q)));
        }
        return out;
    }

    /** 1.0.17: War on a posed body with his cape and skirt left to the cloth ({@link #clothTris}). */
    static List<WingPreview.Tri> warBodyTris(RigPose pose, double time) {
        List<WingPreview.Tri> out = new ArrayList<>();
        String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
        com.hex.kagune.control.king.WarModel.Part[] parts = com.hex.kagune.control.king.WarModel.Part.values();
        for (int i = 0; i < parts.length; i++) {
            com.hex.kagune.control.king.WarModel.Part part = parts[i];
            String name = names[i];
            out.addAll(placedLit(sink -> com.hex.kagune.control.king.WarModel.build(sink, part, time, 1.0, 0.0, com.hex.kagune.control.king.WarModel.Motion.STILL,
                null, true), q -> onPart(pose, name, q)));
        }
        return out;
    }

    /** A body for the cloth: {@code pose} turned {@code yaw} about the vertical and moved to {@code at} (rig space, blocks). */
    static com.hex.kagune.control.king.WarCloth.Pose clothBody(RigPose pose, double yaw, double[] at) {
        java.util.function.Function<String, com.hex.kagune.control.king.WarCloth.Frame> part = name -> (x, y, z) -> {
            double[] q = onPart(pose, name, new double[] {x, y, z});
            double c = Math.cos(yaw);
            double sn = Math.sin(yaw);
            return new double[] {q[0] * c + q[2] * sn + at[0], q[1] + at[1], -q[0] * sn + q[2] * c + at[2]};
        };
        return new com.hex.kagune.control.king.WarCloth.Pose(part.apply("torso"), part.apply("rightArm"), part.apply("leftArm"), part.apply("rightLeg"),
            part.apply("leftLeg"), 0.0, 0.9375 / 16.0);
    }

    /** The cloth's quads as the shot's triangles, both sides, out of the world it ran in and into the body's own frame. */
    static List<WingPreview.Tri> clothTris(com.hex.kagune.control.king.WarCloth cloth, double yaw, double[] at) {
        return clothTris(cloth, yaw, at, 0);
    }

    /** {@link #clothTris}, every quad {@code debug} (0: its own black) so the cloth's shape reads on its own. */
    static List<WingPreview.Tri> clothTris(com.hex.kagune.control.king.WarCloth cloth, double yaw, double[] at, int debug) {
        List<WingPreview.Tri> out = new ArrayList<>();
        java.util.function.UnaryOperator<double[]> local = p -> {
            double x = p[0] - at[0];
            double z = p[2] - at[2];
            double c = Math.cos(-yaw);
            double sn = Math.sin(-yaw);
            return new double[] {x * c + z * sn, p[1] - at[1], -x * sn + z * c};
        };
        cloth.draw(1.0, new double[] {0.0, 0.0, 0.0}, 1.0, (a, b, c, d, colour) -> {
            double[] la = local.apply(a), lb = local.apply(b), lc = local.apply(c), ld = local.apply(d);
            double[] n = PoseRender.norm(cross3(PoseRender.sub(lc, la), PoseRender.sub(ld, lb)));
            double[] back = {-n[0], -n[1], -n[2]};
            int shade = debug == 0 ? colour : debug;
            int[] cs = {shade, shade, shade};
            out.add(new WingPreview.Tri(new double[][] {la, lb, lc}, cs, n, false, false));
            out.add(new WingPreview.Tri(new double[][] {la, lc, ld}, cs, n, false, false));
            out.add(new WingPreview.Tri(new double[][] {la, lc, lb}, cs, back, false, false));
            out.add(new WingPreview.Tri(new double[][] {la, ld, lc}, cs, back, false, false));
        });
        return out;
    }

    static double[] cross3(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    /** 1.0.17: War's black cloth as it moves (WarCloth, the same the game runs): at rest, walking, sprinting, turned hard, crouched, in a gale, burst out. */
    static void warCloth(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        double[] look = {0.0, 0.95, 0.0};
        // {label, ticks, speed (blocks a second, along his front), turn (radians, over the last 6 ticks), stride, crouch, gale, burst tick, eye}
        Object[][] runs = {
            {"At rest (back, three quarters)", 60, 0.0, 0.0, 0.0, false, 0.0, -1, new double[] {-3.4, 1.7, -4.4}},
            {"At rest (front)", 60, 0.0, 0.0, 0.0, false, 0.0, -1, new double[] {-1.6, 1.3, 4.6}},
            {"At rest (side)", 60, 0.0, 0.0, 0.0, false, 0.0, -1, new double[] {5.2, 1.1, 0.4}},
            {"Walking (side)", 48, 2.6, 0.0, 0.55, false, 0.0, -1, new double[] {5.2, 1.1, 0.4}},
            {"Sprinting (side)", 48, 5.6, 0.0, 1.0, false, 0.0, -1, new double[] {5.2, 1.1, 0.4}},
            {"Sprinting (back)", 48, 5.6, 0.0, 1.0, false, 0.0, -1, new double[] {-2.4, 1.7, -5.0}},
            {"Sprinting (front)", 48, 5.6, 0.0, 1.0, false, 0.0, -1, new double[] {-2.0, 1.3, 4.6}},
            {"Turned hard (just after)", 44, 0.0, 2.4, 0.0, false, 0.0, -1, new double[] {-3.4, 1.7, -4.4}},
            {"Crouched (side)", 60, 0.0, 0.0, 0.0, true, 0.0, -1, new double[] {5.0, 1.0, -1.0}},
            {"In a gale from the front", 80, 0.0, 0.0, 0.0, false, 7.0, -1, new double[] {-4.4, 1.5, -2.6}},
            {"The burst: thrown out (2 ticks)", 62, 0.0, 0.0, 0.0, false, 0.0, 60, new double[] {-3.8, 1.6, -4.4}},
            {"The burst: falling back (8 ticks)", 68, 0.0, 0.0, 0.0, false, 0.0, 60, new double[] {-3.8, 1.6, -4.4}},
        };
        List<Shot> bare = new ArrayList<>();
        for (Object[] run : runs) {
            int ticks = (int)run[1];
            double speed = (double)run[2];
            double turn = (double)run[3];
            double stride = (double)run[4];
            boolean crouch = (boolean)run[5];
            double gale = (double)run[6];
            int burst = (int)run[7];
            com.hex.kagune.control.king.WarCloth cloth = new com.hex.kagune.control.king.WarCloth();
            double[] at = {0.0, 0.0, 0.0};
            double yaw = 0.0;
            RigPose pose = clothPose(0.0, 0.0, crouch);
            com.hex.kagune.control.king.WarCloth.Pose last = clothBody(pose, yaw, at);
            cloth.reset(last);
            for (int t = 0; t < ticks; t++) {
                double ramp = Math.min(1.0, t / 6.0);
                if (turn != 0.0 && t >= ticks - 6) {
                    yaw += turn / 6.0;
                }
                at = new double[] {at[0] + Math.sin(yaw) * speed / 20.0, 0.0, at[2] + Math.cos(yaw) * speed / 20.0};
                pose = clothPose(t + 1, stride * ramp, crouch);
                com.hex.kagune.control.king.WarCloth.Pose now = clothBody(pose, yaw, at);
                com.hex.kagune.control.king.WarCloth.Wind wind = gale > 0.0 ? new com.hex.kagune.control.king.WarCloth.Wind(0.0, 0.0, -gale * (0.8 + 0.2 * Math.sin(t * 0.7)))
                    : com.hex.kagune.control.king.WarCloth.Wind.breeze(t);
                cloth.step(last, now, com.hex.kagune.control.king.WarCloth.SUBSTEPS_PER_TICK, wind);
                if (t == burst) {
                    cloth.burst(now.torso().at(0.0, 5.0, 0.0), 9.0);
                }
                last = now;
            }
            List<WingPreview.Tri> tris = warBodyTris(pose, ticks);
            tris.addAll(clothTris(cloth, yaw, at));
            double[] eye = (double[])run[8];
            shots.add(new Shot((String)run[0], pose, NO_WINGS, eye, look, tris));
            bare.add(new Shot((String)run[0], pose, NO_WINGS, eye, look, clothTris(cloth, yaw, at, 0xFFB8B4C8)));
        }
        sheet(file, "War 1.0.17: pure black, real cloth (the cape and the coat's skirt simulated)", shots, 3, 430, 470);
        sheet(new File(file.getParentFile(), "king-war-cloth-bare.png"), "War 1.0.17: the cloth alone on the body (pale so its shape reads)", bare, 3, 430, 470);
    }

    /**
     * 1.0.17: the transformations (KingTransform) frame by frame, as the game draws them: the body's pose, the kakuja growing out of him
     * (KingGrowth), War's cloth thrown out by the burst, and what each leaves in the world (KingComingFx).
     */
    /** 1.0.19: the marks blows leave (KingMarkFx): each style a moment after, a little later, and fading (the motes are not drawn here). */
    static void kingMarks(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        String[] names = {"BLOOD", "GORE", "QUAKE", "FLARE", "GLINT"};
        int[] styles = {com.hex.kagune.control.king.KingMarkFx.BLOOD, com.hex.kagune.control.king.KingMarkFx.GORE, com.hex.kagune.control.king.KingMarkFx.QUAKE,
            com.hex.kagune.control.king.KingMarkFx.FLARE, com.hex.kagune.control.king.KingMarkFx.GLINT};
        double[] ages = {1.0, 3.0, 7.0};
        RigPose pose = new RigPose();
        for (int s = 0; s < styles.length; s++) {
            for (double age : ages) {
                int style = styles[s];
                double[] eye = {-1.2, 2.4, 4.2};
                double[] at = style == com.hex.kagune.control.king.KingMarkFx.QUAKE ? new double[] {0.0, 0.0, 1.6} : new double[] {0.0, 1.2, 1.6};
                double[] way = style == com.hex.kagune.control.king.KingMarkFx.QUAKE ? new double[] {0.0, 0.0, 0.0} : new double[] {0.0, 0.15, 0.99};
                List<WingPreview.Tri> tris = new ArrayList<>(placedLit(sink -> com.hex.kagune.control.king.KingMarkFx.draw(sink, style, at, way, 1.2, age, 77L, eye),
                    q -> q));
                shots.add(new Shot(names[s] + " " + (int)age + "t", pose, NO_WINGS, eye, new double[] {0.0, 1.0, 1.4}, tris));
            }
        }
        sheet(file, "1.0.19: what blows leave (no vanilla dust): blood, gore, quake, flare, glint", shots, 3, 400, 320);
    }

    /** 1.0.40: two players' M1s meeting (KaguneClash): the wind of it between them, moment by moment, from the side and from behind one. */
    static void clash(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        RigPose[] posed = new RigPose[1];
        double[] at = {0.0, 1.1, 1.6};
        double[] way = {0.0, 0.0, 1.0};
        double[] ages = {0.5, 2.0, 4.0, 7.0};
        double[][][] views = {{{-6.5, 2.2, 1.6}, {0.0, 1.1, 1.6}}, {{2.2, 2.6, -3.4}, {0.0, 1.1, 1.8}}};
        for (double[][] view : views) {
            for (double age : ages) {
                List<WingPreview.Tri> tris = new ArrayList<>(peaceIn(com.hex.kagune.control.king.KingMove.BLADES, 0, 3.0, null, 0.0, posed));
                tris.addAll(dummy(new double[] {0.0, 0.0, 3.2}, 0.6, 1.8, 0xFF2E4A2A));
                tris.addAll(placedLit(sink -> com.hex.kagune.control.king.KingMarkFx.draw(sink, com.hex.kagune.control.king.KingMarkFx.CLASH, at, way, 1.2, age, 77L,
                    view[0]), q -> q));
                tris.addAll(field(0.0, 10.0, null, 0.0, 0.0));
                shots.add(new Shot("clash, " + age + " ticks", posed[0], NO_WINGS, view[0], view[1], tris));
            }
        }
        sheet(file, "1.0.40: kagune meeting kagune: both blows stopped, the wind of it between them", shots, 4, 420, 340);
    }

    /** 1.0.19: the final kakujas leaving him: War crumbling to ash (KingShed), Peace folding in, withering and receding. */
    static void kingLeaving(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        double[] war = {0.0, 5.0, 9.0, 13.0, 17.0, 21.0, 25.0, 30.0};
        double[] peace = {0.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 34.0};
        String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
        com.hex.kagune.control.king.WarModel.Part[] parts = com.hex.kagune.control.king.WarModel.Part.values();
        // War as it was the moment he took it off, kept as the layer keeps it.
        List<List<float[]>> worn = new ArrayList<>();
        for (com.hex.kagune.control.king.WarModel.Part part : parts) {
            List<float[]> quads = new ArrayList<>();
            com.hex.kagune.control.king.WarModel.build((a, b, c, d, n, ca, cb, cc, cd) -> quads.add(new float[] {(float)a[0], (float)a[1], (float)a[2], (float)b[0],
                (float)b[1], (float)b[2], (float)c[0], (float)c[1], (float)c[2], (float)d[0], (float)d[1], (float)d[2], (float)n[0], (float)n[1], (float)n[2], ca, cb, cc,
                cd, n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0 ? 1.0F : 0.0F}), part, 400.0, 1.0, 0.0, com.hex.kagune.control.king.WarModel.Motion.STILL, null, false);
            worn.add(quads);
        }
        for (int pass = 0; pass < 2; pass++) {
            boolean isWar = pass == 0;
            double[] ages = isWar ? war : peace;
            for (double age : ages) {
                double time = 400.0 + age;
                RigPose pose = new RigPose();
                float stance = (float)Math.max(0.0, 1.0 - age / 4.0);
                if (isWar) {
                    com.hex.kagune.control.client.animation.WarPose.apply(pose, time, 7, stance, 0.0F);
                } else {
                    com.hex.kagune.control.client.animation.WarPose.peace(pose, time, stance);
                }
                com.hex.kagune.control.client.animation.WarPose.going(pose, isWar, age, time);
                List<WingPreview.Tri> tris = new ArrayList<>();
                for (int k = 0; k < parts.length; k++) {
                    com.hex.kagune.control.king.WarModel.Part part = parts[k];
                    String name = names[k];
                    if (isWar) {
                        List<float[]> quads = worn.get(k);
                        tris.addAll(placedLit(sink -> {
                            float[] s = new float[20];
                            for (float[] q : quads) {
                                if (com.hex.kagune.control.king.KingShed.shed(q, part, age, s)) {
                                    sink.quad(new double[] {s[0], s[1], s[2]}, new double[] {s[3], s[4], s[5]}, new double[] {s[6], s[7], s[8]},
                                        new double[] {s[9], s[10], s[11]}, new double[] {s[12], s[13], s[14]}, (int)s[15], (int)s[16], (int)s[17], (int)s[18]);
                                }
                            }
                        }, q -> onPart(pose, name, q)));
                    } else {
                        com.hex.kagune.control.king.PeaceMotion.State state = new com.hex.kagune.control.king.PeaceMotion.State(
                            com.hex.kagune.control.king.KingMove.RECEDE, 0, age, 0.0, null);
                        double[] growth = com.hex.kagune.control.king.KingTransform.growth(com.hex.kagune.control.king.KingForm.PEACE, false, age);
                        tris.addAll(placedLit(sink -> com.hex.kagune.control.king.PeaceModel.build(sink, part, time, 1.0, state, false, growth),
                            q -> onPart(pose, name, q)));
                    }
                }
                double[] eye = isWar ? new double[] {-3.6, 1.8, 4.6} : new double[] {-3.4, 1.9, 5.0};
                shots.add(new Shot((isWar ? "War " : "Peace ") + (int)age + "t", pose, NO_WINGS, eye, new double[] {0.0, 1.0, 0.0}, tris));
            }
        }
        sheet(file, "1.0.19: leaving him: War crumbles to ash, Peace folds in and recedes", shots, 4, 400, 430);
    }

    static void kingComing(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        double[] war = {3.0, 9.0, 14.5, 16.4, 17.5, 20.0, 30.0, 70.0};
        double[] peace = {3.0, 12.0, 20.0, 26.0, 32.0, 40.0, 47.0, 62.0};
        String[] warLabels = {"darkness pours in", "doubled over", "coiled", "THE BURST", "torn out", "roaring", "settled", "the blood left"};
        String[] peaceLabels = {"head bowed", "rising, arms opening", "the wings unfurl", "spread wide", "the armour grows", "the crown folds",
            "the light", "feathers drifting"};
        for (int pass = 0; pass < 2; pass++) {
            boolean isWar = pass == 0;
            double[] ages = isWar ? war : peace;
            for (int i = 0; i < ages.length; i++) {
                double age = ages[i];
                double time = 400.0 + age;
                com.hex.kagune.control.king.KingForm form = isWar ? com.hex.kagune.control.king.KingForm.WAR : com.hex.kagune.control.king.KingForm.PEACE;
                RigPose pose = comingPose(isWar, age, time);
                List<WingPreview.Tri> tris = new ArrayList<>();
                double grow = com.hex.kagune.control.king.KingTransform.grow(form, true, age);
                double[] growth = com.hex.kagune.control.king.KingTransform.growth(form, true, age);
                String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
                com.hex.kagune.control.king.WarModel.Part[] parts = com.hex.kagune.control.king.WarModel.Part.values();
                com.hex.kagune.control.king.PeaceMotion.State state = com.hex.kagune.control.king.KingTransform.coming(form, true, age)
                    ? new com.hex.kagune.control.king.PeaceMotion.State(com.hex.kagune.control.king.KingMove.EMERGE, 0, age, 0.0, null, com.hex.kagune.control.king.KingMove.NONE,
                        0, 0.0, 0.0, 0.0)
                    : com.hex.kagune.control.king.PeaceMotion.State.REST;
                for (int k = 0; k < parts.length; k++) {
                    com.hex.kagune.control.king.WarModel.Part part = parts[k];
                    String name = names[k];
                    if (isWar && grow > 0.003) {
                        tris.addAll(placedLit(sink -> com.hex.kagune.control.king.WarModel.build(com.hex.kagune.control.king.KingGrowth.wrap(sink, part, growth), part, time,
                            grow, 0.0, com.hex.kagune.control.king.WarModel.Motion.STILL, null, true), q -> onPart(pose, name, q)));
                    } else if (!isWar) {
                        tris.addAll(placedLit(sink -> com.hex.kagune.control.king.PeaceModel.build(sink, part, time, grow, state, false, growth), q -> onPart(pose, name, q)));
                    }
                }
                double[] eye = isWar ? new double[] {-3.6, 1.7, 4.6} : new double[] {-3.2, 1.9, 5.2};
                if (isWar && age >= com.hex.kagune.control.king.KingTransform.WAR_BURST) {
                    // The cloth from the burst on: hung as it comes out of him, thrown wide, left to fall.
                    com.hex.kagune.control.king.WarCloth cloth = new com.hex.kagune.control.king.WarCloth();
                    com.hex.kagune.control.king.WarCloth.Pose body = clothBody(pose, 0.0, new double[] {0.0, 0.0, 0.0});
                    cloth.reset(body);
                    cloth.burst(body.torso().at(0.0, 5.0, 0.0), 9.0);
                    int steps = (int)Math.round((age - com.hex.kagune.control.king.KingTransform.WAR_BURST) * com.hex.kagune.control.king.WarCloth.SUBSTEPS_PER_TICK);
                    for (int k = 0; k < steps; k++) {
                        cloth.step(body, body, 1, com.hex.kagune.control.king.WarCloth.Wind.STILL);
                    }
                    tris.addAll(clothTris(cloth, 0.0, new double[] {0.0, 0.0, 0.0}));
                }
                final double fxAge = age;
                tris.addAll(placedLit(sink -> {
                    if (isWar) {
                        com.hex.kagune.control.king.KingComingFx.war(sink, new double[] {0.0, 0.0, 0.0}, fxAge, 1234L, eye, time);
                    } else {
                        com.hex.kagune.control.king.KingComingFx.peace(sink, new double[] {0.0, 0.0, 0.0}, fxAge, 1234L, eye, time);
                    }
                }, q -> q));
                String label = (isWar ? "War " : "Peace ") + (int)Math.round(age * 10) / 10.0 + "t: " + (isWar ? warLabels[i] : peaceLabels[i]);
                shots.add(new Shot(label, pose, NO_WINGS, eye, new double[] {0.0, 1.0, 0.0}, tris));
            }
        }
        sheet(file, "1.0.17: the transformations: War (violent: blood and darkness) and Peace (graceful: wings and armour like muscle)", shots, 4, 400, 430);
    }

    /** The body as it comes over him {@code age} ticks in: the transformation's pose, his stance coming in under it at the end. */
    static RigPose comingPose(boolean war, double age, double time) {
        RigPose p = new RigPose();
        com.hex.kagune.control.king.KingForm form = war ? com.hex.kagune.control.king.KingForm.WAR : com.hex.kagune.control.king.KingForm.PEACE;
        double ticks = com.hex.kagune.control.king.KingTransform.ticks(form);
        if (age >= ticks) {
            if (war) {
                com.hex.kagune.control.client.animation.WarPose.apply(p, time, 7, 1.0F, 0.0F);
            } else {
                com.hex.kagune.control.client.animation.WarPose.peace(p, time, 1.0F);
            }
            return p;
        }
        float stance = (float)Math.max(0.0, Math.min(1.0, (age - (ticks - 10.0)) / 10.0));
        if (war) {
            com.hex.kagune.control.client.animation.WarPose.apply(p, time, 7, stance, 0.0F);
        } else {
            com.hex.kagune.control.client.animation.WarPose.peace(p, time, stance);
        }
        RigPose coming = new RigPose();
        com.hex.kagune.control.client.animation.WarPose.coming(coming, war, age, time);
        p.add(coming, 1.0F - stance * stance);
        return p;
    }

    /** War's stance for the cloth, the legs and arms swinging as far as {@code stride} at {@code tick}; crouched or not. */
    static RigPose clothPose(double tick, double stride, boolean crouch) {
        RigPose pose = new RigPose();
        com.hex.kagune.control.client.animation.WarPose.apply(pose, tick, 7, 1.0F, (float)stride);
        float swing = (float)(Math.cos(tick * 0.6662) * 1.0 * stride);
        pose.rightLegX += swing;
        pose.leftLegX -= swing;
        pose.rightArmX -= swing * 0.8F;
        pose.leftArmX += swing * 0.8F;
        if (crouch) {
            pose.lean += 0.5F;
        }
        return pose;
    }

    /** The final evolution's War form (1.0.5) beside the reference sheet's three views: front, right side, back (near orthographic). */
    static void warViews(File file) throws Exception {
        RigPose still = new RigPose();
        double[] look = {0.0, 1.12, 0.0};
        double saved = fov;
        int savedSky = sky;
        fov = 24.0;
        sky = 0xE6E6E6;
        List<Shot> shots = new ArrayList<>();
        // 1.0.7: in War's own stance (WarPose at rest, the head between tilts).
        RigPose stance = new RigPose();
        com.hex.kagune.control.client.animation.WarPose.apply(stance, 2.0, 7, 1.0F, 0.0F);
        shots.add(new Shot("Front", stance, NO_WINGS, new double[] {0.0, 1.12, 10.0}, look, warTris(stance, 0.0, 1.0)));
        shots.add(new Shot("Side (right)", stance, NO_WINGS, new double[] {10.0, 1.12, 0.0}, look, warTris(stance, 0.0, 1.0)));
        shots.add(new Shot("Back", stance, NO_WINGS, new double[] {0.0, 1.12, -10.0}, look, warTris(stance, 0.0, 1.0)));
        sheet(file, "War: front, right, back", shots, 3, 500, 640);
        fov = saved;
        sky = savedSky;
    }

    /** War from all round and close up, alive (the rinkaku and the cape move with time) and coming out. */
    static void war(File file) throws Exception {
        RigPose still = new RigPose();
        double[] look = {0.0, 1.05, 0.0};
        List<Shot> shots = new ArrayList<>();
        Object[][] views = {
            {"Front, three quarters (right)", new double[] {-4.2, 1.6, 5.2}, look, 1.0},
            {"Front, three quarters (left)", new double[] {4.2, 1.5, 5.2}, look, 1.0},
            {"Back, three quarters", new double[] {-4.0, 2.0, -5.4}, look, 1.0},
            {"Mask", new double[] {0.35, 1.78, 1.9}, new double[] {0.0, 1.72, 0.0}, 1.0},
            {"Mask, three quarters", new double[] {1.4, 1.85, 1.4}, new double[] {0.0, 1.72, 0.0}, 1.0},
            {"The arm and the scythe", new double[] {-2.6, 1.0, 2.6}, new double[] {-0.6, 0.8, 0.0}, 1.0},
            {"Low, from the front", new double[] {-1.6, 0.4, 4.6}, new double[] {0.0, 1.2, 0.0}, 1.0},
            {"Coming out (grow 0.5)", new double[] {-4.2, 1.6, 5.2}, look, 0.5},
            {"Coming out (grow 0.2)", new double[] {-4.2, 1.6, 5.2}, look, 0.2},
        };
        for (int i = 0; i < views.length; i++) {
            shots.add(new Shot((String)views[i][0], still, NO_WINGS, (double[])views[i][1], (double[])views[i][2], warTris(still, 20.0 + i * 11.0,
                (double)views[i][3])));
        }
        int quads = 0;
        for (com.hex.kagune.control.king.WarModel.Part part : com.hex.kagune.control.king.WarModel.Part.values()) {
            int[] n = {0};
            com.hex.kagune.control.king.WarModel.build((a, b, c, d, nn, ca, cb, cc, cd) -> n[0]++, part, 0.0, 1.0);
            System.out.println("war " + part + ": " + n[0] + " quads");
            quads += n[0];
        }
        System.out.println("war: " + quads + " quads");
        sheet(file, "War, the One-Eyed King (final evolution, evil path): smooth living kakuja after the reference sheet", shots, 3, 600, 520);
    }

    /** War as the game draws it at {@code night} (0..1): the plates dimmed, the eye's halo, and the bloom sized for {@code distance}. */
    static List<WingPreview.Tri> warLit(RigPose pose, double time, double grow, double night, double distance) {
        List<WingPreview.Tri> out = new ArrayList<>();
        String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
        com.hex.kagune.control.king.WarModel.Part[] parts = com.hex.kagune.control.king.WarModel.Part.values();
        double plates = 1.0 - 0.68 * night;
        for (int i = 0; i < parts.length; i++) {
            com.hex.kagune.control.king.WarModel.Part part = parts[i];
            String name = names[i];
            out.addAll(placedLit(sink -> com.hex.kagune.control.king.WarModel.build((a, b, c, d, n, ca, cb, cc, cd) -> {
                boolean glow = n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0;
                sink.quad(a, b, c, d, n, glow ? nightGlow(ca, night) : dimmed(ca, plates), glow ? nightGlow(cb, night) : dimmed(cb, plates),
                    glow ? nightGlow(cc, night) : dimmed(cc, plates), glow ? nightGlow(cd, night) : dimmed(cd, plates));
            }, part, time, grow, night), q -> onPart(pose, name, q)));
        }
        // The bloom (KingLayer.bloom): rings of light round the eye, wider with the distance.
        double reach = Math.max(1.0, Math.min(18.0, distance / 5.0));
        double strength = (0.55 + 0.45 * night) * grow;
        double[] at = com.hex.kagune.control.king.WarModel.eye(grow);
        double z = at[2] - 0.02 * reach;
        double[] radii = {0.0, 0.4, 0.9, 1.8, 3.2};
        double[] light = {1.0, 0.65, 0.3, 0.1, 0.0};
        out.addAll(placedLit(sink -> {
            for (int r = 0; r + 1 < radii.length; r++) {
                int in = greyLight(light[r] * strength);
                int outer = greyLight(light[r + 1] * strength);
                for (int k = 0; k < 20; k++) {
                    double a0 = Math.PI * 2.0 * k / 20;
                    double a1 = Math.PI * 2.0 * (k + 1) / 20;
                    double i0 = radii[r] * reach;
                    double o0 = radii[r + 1] * reach;
                    sink.quad(new double[] {at[0] + Math.cos(a0) * i0, at[1] + Math.sin(a0) * i0, z},
                        new double[] {at[0] + Math.cos(a0) * o0, at[1] + Math.sin(a0) * o0, z},
                        new double[] {at[0] + Math.cos(a1) * o0, at[1] + Math.sin(a1) * o0, z},
                        new double[] {at[0] + Math.cos(a1) * i0, at[1] + Math.sin(a1) * i0, z}, new double[] {0, 1, 0}, in, outer, outer, in);
                }
            }
        }, q -> onPart(pose, "head", q)));
        return out;
    }

    /** KingLayer.nightGlow: by night the reds go low, the eye's white keeps its light. */
    static int nightGlow(int argb, double night) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return r == g && g == b ? argb : dimmed(argb, 1.0 - 0.72 * night);
    }

    static int dimmed(int argb, double share) {
        int r = (int)Math.round(((argb >> 16) & 0xFF) * share);
        int g = (int)Math.round(((argb >> 8) & 0xFF) * share);
        int b = (int)Math.round((argb & 0xFF) * share);
        return argb & 0xFF000000 | r << 16 | g << 8 | b;
    }

    static int greyLight(double amount) {
        int v = (int)Math.round(255.0 * Math.max(0.0, Math.min(1.0, amount)));
        return 0xFF000000 | v << 16 | v << 8 | v;
    }

    /** 1.0.5: War's twitching, head-tilting body (WarPose) through a few seconds, and War at night, near and far. */
    static void warNight(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        double[] look = {0.0, 1.15, 0.0};
        int seed = 1234567;
        double[] times = {20.0, 85.0, 150.0, 214.0, 262.0, 333.0};
        for (int i = 0; i < times.length; i++) {
            RigPose pose = new RigPose();
            com.hex.kagune.control.client.animation.WarPose.apply(pose, times[i], seed, 1.0F, 0.0F);
            double[] eye = {-1.4, 1.75, 3.6};
            shots.add(new Shot(String.format("Day, %.1f s: the head tilts and twitches", times[i] / 20.0), pose, NO_WINGS, eye, new double[] {0.0, 1.45, 0.0},
                warLit(pose, times[i], 1.0, 0.0, 4.0)));
        }
        double savedDim = dim;
        int savedSky = sky;
        RigPose nightPose = new RigPose();
        com.hex.kagune.control.client.animation.WarPose.apply(nightPose, 85.0, seed, 1.0F, 0.0F);
        Object[][] nights = {{"Night, 4 blocks", new double[] {-0.8, 1.7, 3.8}, new double[] {0.0, 1.5, 0.0}, 4.0},
            {"Night, 16 blocks", new double[] {-1.0, 1.8, 16.0}, new double[] {0.0, 1.3, 0.0}, 16.0},
            {"Night, 48 blocks", new double[] {-2.0, 2.0, 48.0}, new double[] {0.0, 1.3, 0.0}, 48.0}};
        List<Shot> dark = new ArrayList<>();
        for (Object[] n : nights) {
            dark.add(new Shot((String)n[0], nightPose, NO_WINGS, (double[])n[1], (double[])n[2], warLit(nightPose, 85.0, 1.0, 1.0, (double)n[3])));
        }
        sheet(file, "War: the body (WarPose) by day, and by night (the body dark, the eye burning, its bloom sized for the distance)", shots, 3, 600, 470);
        dim = 0.12;
        sky = 0x05070C;
        fov = 40.0;
        sheet(new File(file.getParentFile(), "king-war-night-only.png"), "War at night", dark, 3, 600, 470);
        fov = 0.0;
        dim = savedDim;
        sky = savedSky;
    }

    /** 1.0.6: War's abilities on the body (WarPose.act) with the rinkaku in each (WarModel.Motion), part way through each move. */
    static void warMoves(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        Object[][] moves = {
            {com.hex.kagune.control.king.KingMove.GRASP, 18.0, 0, "King's Execution: the hand out, crushing"},
            {com.hex.kagune.control.king.KingMove.DASH, 3.0, 0, "Black Reaper: the dash"},
            {com.hex.kagune.control.king.KingMove.CLEAVE, 2.5, 0, "Black Reaper: the cleave raised"},
            {com.hex.kagune.control.king.KingMove.CLEAVE, 6.0, 0, "Black Reaper: the cleave landed"},
            {com.hex.kagune.control.king.KingMove.SPIRE, 9.0, 0, "Spire Wave: driven into the ground"},
            {com.hex.kagune.control.king.KingMove.DEFIANCE, 8.0, 0, "King's Defiance: the guard"},
            {com.hex.kagune.control.king.KingMove.COCOON, 30.0, 0, "King's Defiance: the cocoon"},
            {com.hex.kagune.control.king.KingMove.BURST, 4.0, 0, "King's Defiance: the burst"},
            {com.hex.kagune.control.king.KingMove.SUN, 50.0, 0, "Black Sun: the eye open"},
        };
        double[] look = {0.0, 1.1, 0.0};
        for (Object[] m : moves) {
            com.hex.kagune.control.king.KingMove move = (com.hex.kagune.control.king.KingMove)m[0];
            double age = (double)m[1];
            RigPose pose = new RigPose();
            com.hex.kagune.control.client.animation.WarPose.apply(pose, 100.0 + age, 7, 0.5F, 0.0F);
            com.hex.kagune.control.client.animation.WarPose.act(pose, move, age, (int)m[2], 1.0F);
            com.hex.kagune.control.king.WarModel.Motion motion = com.hex.kagune.control.king.WarModel.Motion.of(move, move.progress(age));
            List<WingPreview.Tri> tris = new ArrayList<>();
            String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
            com.hex.kagune.control.king.WarModel.Part[] parts = com.hex.kagune.control.king.WarModel.Part.values();
            for (int i = 0; i < parts.length; i++) {
                com.hex.kagune.control.king.WarModel.Part part = parts[i];
                String name = names[i];
                tris.addAll(placedLit(sink -> com.hex.kagune.control.king.WarModel.build(sink, part, 100.0 + age, 1.0, 0.0, motion), q -> onPart(pose, name, q)));
            }
            shots.add(new Shot((String)m[3], pose, NO_WINGS, new double[] {-3.6, 1.9, 4.6}, look, tris));
        }
        sheet(file, "War's abilities on the body (1.0.13)", shots, 3, 600, 470);
    }

    /**
     * 1.0.14: War's M1, blow by blow: each wound up, landing on its impact tick, and followed through, on the body (WarPose) with the
     * rinkaku as each has them, the blade's arc and the shock of a fist or a foot (KingFxModel) round a body standing where he stops.
     */
    static void warStrikes(File file, boolean grammar) throws Exception {
        List<Shot> shots = new ArrayList<>();
        double[] chest = {0.0, 0.9, 0.0};
        double[] facing = {0.0, 0.0, 1.0};
        String[] names = {"Jab", "Reap", "Side kick", "Rising slash", "Spinning heel", "King's Verdict", "Ascension (launcher)", "Downfall (air spike)",
            "Cleaver (RMB)", "Black Calamity (RMB held)", "Rend (RMB ender)"};
        if (grammar) {
            for (double age : new double[] {3.0, 14.0}) {
                RigPose pose = new RigPose();
                List<WingPreview.Tri> tris = warIn(pose, com.hex.kagune.control.king.KingMove.CHARGE, age);
                shots.add(new Shot("RMB held: charging (" + (int)age + "t)", pose, NO_WINGS, new double[] {-4.6, 2.2, 2.6}, new double[] {0.0, 1.0, 0.5}, tris));
            }
        }
        for (com.hex.kagune.control.king.WarStrike blow : com.hex.kagune.control.king.WarStrike.values()) {
            if (blow.special() != grammar) {
                continue;
            }
            double stand = blow.stand(0.6);
            double[] at = {0.0, 0.0, stand};
            double[][] frames = {{blow.impact - 2.0, 0}, {blow.impact, 1}, {blow.impact + 2.0, 2}};
            String[] when = {"wound up", "landing", "through"};
            for (double[] frame : frames) {
                double age = frame[0];
                RigPose pose = new RigPose();
                com.hex.kagune.control.client.animation.WarPose.apply(pose, 100.0 + age, 7, 0.5F, 0.0F);
                com.hex.kagune.control.client.animation.WarPose.act(pose, com.hex.kagune.control.king.KingMove.STRIKE, age, blow.ordinal(), 1.0F);
                double px = 16.0 / 0.9375;
                com.hex.kagune.control.king.WarModel.Motion motion = com.hex.kagune.control.king.WarModel.Motion.of(
                    com.hex.kagune.control.king.KingMove.STRIKE, blow.ordinal(), com.hex.kagune.control.king.KingMove.STRIKE.progress(age, blow.ordinal()))
                    .aiming(com.hex.kagune.control.king.WarModel.strikeAims(blow.ordinal(), age, new double[] {0.0, 0.9 * px, -stand * px}));
                List<WingPreview.Tri> tris = new ArrayList<>();
                String[] parts = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
                com.hex.kagune.control.king.WarModel.Part[] all = com.hex.kagune.control.king.WarModel.Part.values();
                for (int i = 0; i < all.length; i++) {
                    com.hex.kagune.control.king.WarModel.Part part = all[i];
                    String name = parts[i];
                    tris.addAll(placedLit(sink -> com.hex.kagune.control.king.WarModel.build(sink, part, 100.0 + age, 1.0, 0.0, motion), q -> onPart(pose, name, q)));
                }
                tris.addAll(dummy(at, 0.6, 1.8, 0xFF2E4A2A));
                tris.addAll(placedLit(sink -> com.hex.kagune.control.king.KingFxModel.swing(sink, blow, chest, facing, age), q -> q));
                if (age >= blow.impact) {
                    double height = blow == com.hex.kagune.control.king.WarStrike.SIDE_KICK ? 0.5 : 0.72;
                    double[] contact = {0.0, 1.8 * height, stand - 0.3};
                    tris.addAll(placedLit(sink -> com.hex.kagune.control.king.KingFxModel.impact(sink, blow, contact, facing, age - blow.impact), q -> q));
                    if (blow == com.hex.kagune.control.king.WarStrike.VERDICT) {
                        tris.addAll(placedLit(sink -> com.hex.kagune.control.king.KingFxModel.verdict(sink, at, facing, age - blow.impact + 2.0, 100.0 + age),
                            q -> q));
                    }
                }
                // From the side the blow is thrown from (the free hand and the heel are his left, +x), the Verdict from farther off.
                boolean left = blow == com.hex.kagune.control.king.WarStrike.JAB || blow == com.hex.kagune.control.king.WarStrike.HEEL;
                double far = blow == com.hex.kagune.control.king.WarStrike.VERDICT ? 1.6 : 1.0;
                double[] eye = {(left ? 4.6 : -4.6) * far, 2.2 * far, 2.6 * far};
                shots.add(new Shot(names[blow.ordinal()] + ": " + when[(int)frame[1]], pose, NO_WINGS, eye, new double[] {0.0, 1.0, 1.0}, tris));
            }
        }
        sheet(file, grammar ? "War's new blows (1.0.20): the launcher, the air spike, the heavy" : "War's M1 (1.0.14), blow by blow", shots, 3, 520, 420);
    }

    /** 1.0.13: War on a body posed in {@code move}, {@code age} ticks in, with the rinkaku as that move has them. */
    static List<WingPreview.Tri> warIn(RigPose pose, com.hex.kagune.control.king.KingMove move, double age) {
        com.hex.kagune.control.client.animation.WarPose.apply(pose, 100.0 + age, 7, 0.5F, 0.0F);
        com.hex.kagune.control.client.animation.WarPose.act(pose, move, age, 0, 1.0F);
        com.hex.kagune.control.king.WarModel.Motion motion = com.hex.kagune.control.king.WarModel.Motion.of(move, move.progress(age));
        List<WingPreview.Tri> tris = new ArrayList<>();
        String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
        com.hex.kagune.control.king.WarModel.Part[] parts = com.hex.kagune.control.king.WarModel.Part.values();
        for (int i = 0; i < parts.length; i++) {
            com.hex.kagune.control.king.WarModel.Part part = parts[i];
            String name = names[i];
            tris.addAll(placedLit(sink -> com.hex.kagune.control.king.WarModel.build(sink, part, 100.0 + age, 1.0, 0.0, motion), q -> onPart(pose, name, q)));
        }
        return tris;
    }

    /**
     * 1.0.33: War's M1 in first person exactly as the game draws it (EtoFleshRenderer.warArms and warRinkaku): his arms posed as third
     * person poses them, pulled in before the eyes ({@code FirstPersonArms}), War's arm pieces on them, the rinkaku; from his eyes, a 70
     * degree lens. {@code still} the arm pieces as they were drawn (unmoving) or as third person moves them.
     */
    static List<WingPreview.Tri> warSeen(RigPose pose, com.hex.kagune.control.king.WarModel.Motion motion, double time, boolean still) {
        double[] eye = {0.0, 1.62, 0.0};
        double[] shift = com.hex.kagune.control.client.animation.EtoOwnLeg.shift(pose);
        List<WingPreview.Tri> tris = new ArrayList<>();
        int skin = 0xFFC69C7A;
        tris.addAll(placedLit(sink -> com.hex.kagune.control.client.animation.EtoOwnArm.both(pose, shift, false, (a, b, c, d, n, uv, trousers) -> {
            if (!trousers) {
                sink.quad(com.hex.kagune.control.client.animation.FirstPersonArms.adjust(a, eye, 0.0),
                    com.hex.kagune.control.client.animation.FirstPersonArms.adjust(b, eye, 0.0), com.hex.kagune.control.client.animation.FirstPersonArms.adjust(c, eye, 0.0),
                    com.hex.kagune.control.client.animation.FirstPersonArms.adjust(d, eye, 0.0), n, skin, skin, skin, skin);
            }
        }), q -> q));
        com.hex.kagune.control.king.WarModel.Motion pieces = still ? com.hex.kagune.control.king.WarModel.Motion.STILL : motion;
        for (com.hex.kagune.control.king.WarModel.Part part : new com.hex.kagune.control.king.WarModel.Part[] {
            com.hex.kagune.control.king.WarModel.Part.RIGHT_ARM, com.hex.kagune.control.king.WarModel.Part.LEFT_ARM}) {
            String name = part == com.hex.kagune.control.king.WarModel.Part.RIGHT_ARM ? "rightArm" : "leftArm";
            tris.addAll(placedLit(sink -> com.hex.kagune.control.king.WarModel.build(sink, part, time, 1.0, 0.0, pieces), q -> {
                double[] r = com.hex.kagune.control.client.animation.BodyFrame.world(pose, com.hex.kagune.control.client.animation.BodyFrame.model(pose, name, q));
                return com.hex.kagune.control.client.animation.FirstPersonArms.adjust(new double[] {r[0] + shift[0], r[1] + shift[1], r[2] + shift[2]}, eye, 0.0);
            }));
        }
        tris.addAll(placedLit(sink -> com.hex.kagune.control.king.WarModel.rinkaku(sink, time, 1.0, motion), q -> {
            double[] r = com.hex.kagune.control.client.animation.BodyFrame.world(pose, com.hex.kagune.control.client.animation.BodyFrame.model(pose, "torso", q));
            return new double[] {r[0] + shift[0], r[1] + shift[1], r[2] + shift[2]};
        }));
        return seenFrom(tris, eye, 0.2);
    }

    /** 1.0.33: War's M1 blows from his eyes (as the game had them, and as now) beside the same moment in third person. */
    static void warFirstPersonBlows(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        double[] eye = {0.0, 1.62, 0.0};
        double[] ahead = {0.0, 1.45, 6.0};
        com.hex.kagune.control.king.KingMove strike = com.hex.kagune.control.king.KingMove.STRIKE;
        String only = System.getProperty("blows");
        for (com.hex.kagune.control.king.WarStrike blow : com.hex.kagune.control.king.WarStrike.values()) {
            if (only != null && !only.contains(blow.name())) {
                continue;
            }
            for (double age : new double[] {blow.impact - 2.0, blow.impact, blow.impact + 2.0}) {
                double time = 100.0 + age;
                RigPose pose = new RigPose();
                com.hex.kagune.control.client.animation.WarPose.apply(pose, time, 7, 0.5F, 0.0F);
                com.hex.kagune.control.client.animation.WarPose.act(pose, strike, age, blow.ordinal(), 1.0F);
                com.hex.kagune.control.king.WarModel.Motion motion = com.hex.kagune.control.king.WarModel.Motion.of(strike, blow.ordinal(), strike.progress(age, blow.ordinal()))
                    .aiming(com.hex.kagune.control.king.WarModel.strikeAims(blow.ordinal(), age, null));
                String at = blow.name() + " " + (age < blow.impact ? "wound up" : age > blow.impact ? "through" : "landing");
                shots.add(new Shot(at + ": first person", null, NO_WINGS, eye, ahead, warSeen(pose, motion, time, Boolean.getBoolean("still"))));
                RigPose third = new RigPose().add(pose, 1.0F);
                List<WingPreview.Tri> body = warIn(new RigPose(), strike, age);
                shots.add(new Shot(at + ": third person", third, NO_WINGS, new double[] {-3.2, 2.2, 3.6}, new double[] {0.0, 1.1, 0.6},
                    warBlow(third, motion, time)));
            }
        }
        double saved = fov;
        fov = 70.0;
        sheet(file, "War's M1: first person (left) against third person (right)", shots, 4, 420, 300);
        fov = saved;
    }

    /** War's whole model on a posed body for a blow. */
    static List<WingPreview.Tri> warBlow(RigPose pose, com.hex.kagune.control.king.WarModel.Motion motion, double time) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
        com.hex.kagune.control.king.WarModel.Part[] parts = com.hex.kagune.control.king.WarModel.Part.values();
        for (int i = 0; i < parts.length; i++) {
            com.hex.kagune.control.king.WarModel.Part part = parts[i];
            String name = names[i];
            tris.addAll(placedLit(sink -> com.hex.kagune.control.king.WarModel.build(sink, part, time, 1.0, 0.0, motion), q -> onPart(pose, name, q)));
        }
        return tris;
    }

    /** 1.0.33: Centipede Dominion's little turning on War's body: doubled over, shaking harder, then thrown back as it tears out. */
    static void warDominion(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        double prelude = com.hex.kagune.control.king.WarRules.WYRM_PRELUDE;
        double[] ages = {2.0, 6.0, prelude - 3.5, prelude - 1.0};
        String[] labels = {"doubled over", "shaking harder", "about to tear", "thrown back: it tears out of his back"};
        for (int i = 0; i < ages.length; i++) {
            RigPose pose = new RigPose();
            List<WingPreview.Tri> tris = warIn(pose, com.hex.kagune.control.king.KingMove.WYRM, ages[i]);
            shots.add(new Shot("Dominion " + String.format(java.util.Locale.ROOT, "%.1f", ages[i]) + ": " + labels[i], pose, NO_WINGS,
                new double[] {-3.4, 1.8, 4.0}, new double[] {0.0, 1.0, 0.0}, tris));
        }
        sheet(file, "Centipede Dominion 1.0.33: the little turning before it", shots, 2, 600, 470);
    }

    /**
     * 1.0.33: Black Sun's punch beat by beat: beside it, drawn back, coiled tight, the punch landing with the ring of air standing up
     * square to it (drawn a third its size to fit), the body hanging on the fist feeling it, and sent flying.
     */
    static void warSun(File file) throws Exception {
        List<WingPreview.Tri> none = new ArrayList<>();
        List<Shot> shots = new ArrayList<>();
        int beside = com.hex.kagune.control.king.WarRules.SUNDER_BESIDE;
        int punch = com.hex.kagune.control.king.WarRules.SUNDER_PUNCH;
        int launch = com.hex.kagune.control.king.WarRules.SUNDER_LAUNCH;
        Object[][] beats = {
            {(double)beside, "beside it at the top: the bow"},
            {(beside + punch) * 0.5, "drawn back (" + (punch - beside) + " ticks of it)"},
            {punch - 1.5, "coiled tight, shaking"},
            {(double)punch, "the punch lands: the ring stands up square to it"},
            {punch + com.hex.kagune.control.king.WarRules.SUNDER_FELT * 0.6, "it hangs on the fist, feeling it"},
            {launch + 1.5, "sent down steep, limp"},
        };
        double ringTicks = com.hex.kagune.control.king.WarRules.RING_TICKS;
        for (Object[] b : beats) {
            double age = (double)b[0];
            RigPose pose = new RigPose();
            List<WingPreview.Tri> tris = warIn(pose, com.hex.kagune.control.king.KingMove.SUNDER, age);
            double since = age - punch;
            double in = since < 0.0 ? 0.0 : Math.min(com.hex.kagune.control.king.WarRules.SUNDER_SINK,
                com.hex.kagune.control.king.WarRules.SUNDER_SINK * since / com.hex.kagune.control.king.WarRules.SUNDER_FELT);
            // It (over it and behind it, his left shoulder in line): driven in, then down steep (75 degrees) and on.
            double flown = age > launch ? (age - launch) * 0.8 : 0.0;
            double dive = com.hex.kagune.control.king.WarRules.SUNDER_DIVE;
            double sinkAlong = in + flown;
            tris.addAll(dummy(new double[] {-0.35, -0.6 - sinkAlong * Math.sin(dive), 1.25 + sinkAlong * Math.cos(dive)}, 0.6, 1.8, 0xFF2E4A2A));
            if (since >= 0.0 && since < ringTicks + 6.0) {
                double k = Math.min(1.0, (since + 1.0) / ringTicks);
                double r = com.hex.kagune.control.king.WarRules.RING_RADIUS / 3.0 * (1.0 - Math.pow(1.0 - k, 3.0));
                double fade = 1.0 - Math.min(1.0, (since + 1.0) / (ringTicks + 6.0));
                tris.addAll(placedLit(sink -> com.hex.kagune.control.king.KingFxModel.airRing(sink, new double[] {-0.35, 0.7, 0.75}, r, 0.6 + 0.04 * r,
                    Math.max(0.35, fade), new double[] {-1, 0, 0}, new double[] {0, 1, 0}, new double[] {0, 0, 1}, -14.0), q -> q));
            }
            shots.add(new Shot("Black Sun " + String.format(java.util.Locale.ROOT, "%.1f", age) + ": " + b[1], pose, NO_WINGS,
                new double[] {-8.5, 2.2, 0.4}, new double[] {-0.2, 0.9, 0.9}, tris));
        }
        sheet(file, "Black Sun 1.0.33: the long draw, the ring upright, the hang, the launch", shots, 3, 600, 470);
    }

    /** A stand-in for something hit: a body-sized dark box at {@code at} (its feet), {@code w} wide and {@code h} tall. */
    static List<WingPreview.Tri> dummy(double[] at, double w, double h, int colour) {
        return placedLit(sink -> {
            double[][] c = new double[8][];
            for (int k = 0; k < 8; k++) {
                c[k] = new double[] {at[0] + ((k & 1) == 0 ? -w / 2 : w / 2), at[1] + ((k & 2) == 0 ? 0 : h), at[2] + ((k & 4) == 0 ? -w / 2 : w / 2)};
            }
            int[][] faces = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
            double[][] normals = {{0, 0, -1}, {0, 0, 1}, {0, -1.0E-3, 0}, {0, 0.999, 0.01}, {-1, 0, 0}, {1, 0, 0}};
            for (int f = 0; f < 6; f++) {
                sink.quad(c[faces[f][0]], c[faces[f][1]], c[faces[f][2]], c[faces[f][3]], normals[f], colour, colour, colour, colour);
            }
        }, q -> q);
    }

    /** 1.0.13: War's abilities as they are now, out in the world: the centipedes, the cleave, the wave, the cocoon, the Black Sun. */
    static void warFx(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        com.hex.kagune.control.king.KingMove grasp = com.hex.kagune.control.king.KingMove.GRASP;
        // King's Execution: coiled round it, squeezing.
        {
            RigPose pose = new RigPose();
            double age = 18.0;
            List<WingPreview.Tri> tris = warIn(pose, grasp, age);
            double[] hand = onPart(pose, "leftArm", new double[] {1, 10, 0});
            double[] target = {1.2, 0.9, 7.0};
            tris.addAll(dummy(new double[] {1.2, 0.0, 7.0}, 0.6, 1.8, 0xFF2E4A2A));
            for (int i = 0; i < com.hex.kagune.control.king.WarRules.GRASP_ARMS; i++) {
                int index = i;
                double[][] path = com.hex.kagune.control.king.KingFxModel.grasp(hand, target, 0.3, 1.8, i, com.hex.kagune.control.king.WarRules.GRASP_ARMS,
                    1.0, 1.0, 0.6, age);
                tris.addAll(placedLit(sink -> com.hex.kagune.control.king.KingFxModel.centipede(sink, path, 0.2, 31 + index, 0.4, true, 1.0, age), q -> q));
            }
            shots.add(new Shot("Z King's Execution: five centipedes out of the arm, coiled, crushing", pose, NO_WINGS, new double[] {7.5, 3.4, -2.5},
                new double[] {0.6, 1.2, 3.5}, tris));
        }
        // Black Reaper: the cleave landing.
        {
            RigPose pose = new RigPose();
            double age = com.hex.kagune.control.king.WarRules.CLEAVE_LANDS + 0.5;
            List<WingPreview.Tri> tris = warIn(pose, com.hex.kagune.control.king.KingMove.CLEAVE, age);
            tris.addAll(dummy(new double[] {-0.4, 0.0, 2.6}, 0.6, 1.8, 0xFF2E4A2A));
            tris.addAll(placedLit(sink -> com.hex.kagune.control.king.KingFxModel.slash(sink, new double[] {-0.25, 1.45, 0.3}, new double[] {0, 1, 0},
                new double[] {0, 0, 1}, 2.2, 0.35, 0.2, 2.5, 0.95), q -> q));
            shots.add(new Shot("J Black Reaper: at the end of the dash, the cleave", pose, NO_WINGS, new double[] {-4.5, 2.2, 4.5}, new double[] {0.0, 1.0, 1.2},
                tris));
        }
        // Spire Wave: twelve blocks out.
        {
            RigPose pose = new RigPose();
            double age = 20.0;
            List<WingPreview.Tri> tris = warIn(pose, com.hex.kagune.control.king.KingMove.SPIRE, 10.0);
            for (int step = 0; step < 13; step++) {
                double since = 12 - step;
                double out = Math.min(1.0, since / 3.0);
                for (int k = 0; k < 2; k++) {
                    double[] at = com.hex.kagune.control.king.KingFxModel.spireAt(step, k);
                    if (at == null) {
                        continue;
                    }
                    int seed = step * 3 + k;
                    double[] base = {at[0], 0.0, 1.6 + step + at[1]};
                    double[] lean = {at[2], 0.0, at[3]};
                    double[][] path = com.hex.kagune.control.king.KingFxModel.spire(base, lean, at[4], out, age, seed);
                    tris.addAll(placedLit(sink -> com.hex.kagune.control.king.KingFxModel.spike(sink, path, at[5], seed, age, 1.0), q -> q));
                }
            }
            tris.addAll(dummy(new double[] {0.0, 1.6, 9.0}, 0.6, 1.8, 0xFF2E4A2A));
            shots.add(new Shot("B Spire Wave: centipedes bursting up a line ahead", pose, NO_WINGS, new double[] {8.0, 4.5, -1.5}, new double[] {0.0, 1.0, 7.0},
                tris));
        }
        // King's Defiance: the cocoon.
        {
            RigPose pose = new RigPose();
            List<WingPreview.Tri> tris = warIn(pose, com.hex.kagune.control.king.KingMove.COCOON, 30.0);
            shots.add(new Shot("C King's Defiance: no blow came, the cocoon", pose, NO_WINGS, new double[] {-3.4, 1.8, 4.4}, new double[] {0.0, 1.1, 0.0}, tris));
        }
        // 1.0.15: Black Sun out of the ground ahead of him: rising with what it seized, and held high before the slam.
        for (double age : new double[] {30.0, 56.0}) {
            RigPose pose = new RigPose();
            List<WingPreview.Tri> tris = warIn(pose, com.hex.kagune.control.king.KingMove.SUN, age);
            double[] spot = {0.0, 0.0, 9.0};
            double lift = com.hex.kagune.control.king.WarRules.lift(age) * com.hex.kagune.control.king.WarRules.SUN_HEIGHT;
            tris.addAll(placedLit(sink -> com.hex.kagune.control.king.KingFxModel.crescent(sink, new double[] {spot[0], -0.25, spot[2]},
                com.hex.kagune.control.king.WarRules.SUN_RADIUS * 0.9, 0.45, 0.8, age), q -> q));
            double[][] bodies = {{3.0, 0.0, 11.0}, {-4.0, 0.0, 8.0}, {1.0, 0.0, 15.0}, {-2.0, 0.0, 12.5}, {5.5, 0.0, 6.5}};
            for (int i = 0; i < bodies.length; i++) {
                int seed = 40 + i;
                double[] feet = {bodies[i][0], lift * (0.85 + 0.05 * i), bodies[i][2]};
                tris.addAll(dummy(feet, 0.6, 1.8, 0xFF2E4A2A));
                double[] centre = {feet[0], feet[1] + 0.9, feet[2]};
                double turn = i * 2.39996;
                double[] base = {centre[0] + Math.cos(turn) * 1.1, 0.0, centre[2] + Math.sin(turn) * 1.1};
                double[][] path = com.hex.kagune.control.king.KingFxModel.seize(base, centre, 0.3, 1.8, 1.0, age, seed);
                tris.addAll(placedLit(sink -> com.hex.kagune.control.king.KingFxModel.centipede(sink, path, 0.34, seed, 0.4, true, 1.0, age * 0.4),
                    q -> q));
            }
            shots.add(new Shot(age < 40.0 ? "V Black Sun: up out of the ground, lifting them" : "V Black Sun: held high, before the slam", pose, NO_WINGS,
                new double[] {-17.0, 11.0, -6.0}, new double[] {0.0, 6.0, 9.0}, tris));
        }
        sheet(file, "War's abilities (1.0.15), out in the world", shots, 3, 600, 460);
    }

    /** 1.0.11: War's one eye on the kakugan wherever the player placed it: Steve's left eye, a left eye set higher, a one-eyed right. */
    static void warEyes(File file) throws Exception {
        Object[][] placements = {
            {com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT, "Usual placement: the left eye"},
            {new com.hex.kagune.control.kakugan.KakuganEyes(1, 3, 4, 3, 1, true, false), "Eyes set high, one pixel tall, closer"},
            {new com.hex.kagune.control.kakugan.KakuganEyes(0, 5, 6, 5, 2, true, false), "Eyes set low and wide"},
            {com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT.withGhoul(com.hex.kagune.control.kakugan.KakuganEyes.RIGHT), "One-eyed, the right: the mask mirrored"},
        };
        RigPose still = new RigPose();
        List<Shot> shots = new ArrayList<>();
        for (Object[] placement : placements) {
            double[] eye = com.hex.kagune.control.king.WarModel.eyeOn((com.hex.kagune.control.kakugan.KakuganEyes)placement[0]);
            List<WingPreview.Tri> tris = new ArrayList<>();
            String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
            com.hex.kagune.control.king.WarModel.Part[] parts = com.hex.kagune.control.king.WarModel.Part.values();
            for (int i = 0; i < 2; i++) {
                com.hex.kagune.control.king.WarModel.Part part = parts[i];
                String name = names[i];
                tris.addAll(placedLit(sink -> com.hex.kagune.control.king.WarModel.build(sink, part, 40.0, 1.0, 0.0,
                    com.hex.kagune.control.king.WarModel.Motion.STILL, eye), q -> onPart(still, name, q)));
            }
            shots.add(new Shot((String)placement[1] + String.format(" (eye at %.1f, %.1f)", eye[0], eye[1]), still, NO_WINGS, new double[] {0.0, 1.75, 2.3},
                new double[] {0.0, 1.72, 0.0}, tris));
        }
        sheet(file, "War's eye on the kakugan (1.0.12)", shots, 2, 560, 560);
    }

    /** 1.0.11: Kaneki turning into the Dragon ({@code TurningModel}) on his body, posed as the game poses it ({@code WarPose.turning}). */
    static void turning(File file) throws Exception {
        Object[][] beats = {
            {0.07, "Clutching his chest (0.4 s)"},
            {0.15, "It tears out of his chest, he screams"},
            {0.30, "The plates spread from the wound"},
            {0.42, "It tears out of his head"},
            {0.64, "Thrown about: the tail, the cracks"},
            {0.70, "Side: the tail out of his back"},
            {0.83, "The last scream"},
            {0.93, "Curled in, about to burst"},
            {0.99, "Bursting open: the Dragon comes"},
        };
        double[] look = {0.0, 1.15, 0.0};
        List<Shot> shots = new ArrayList<>();
        for (Object[] beat : beats) {
            double p = (double)beat[0];
            double time = p * com.hex.kagune.control.king.TurningModel.PRELUDE_TICKS;
            RigPose pose = new RigPose();
            com.hex.kagune.control.client.animation.WarPose.turning(pose, p, time, com.hex.kagune.control.king.TurningModel.scream(p),
                com.hex.kagune.control.king.TurningModel.burst(p));
            List<WingPreview.Tri> tris = new ArrayList<>();
            String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
            com.hex.kagune.control.king.WarModel.Part[] parts = com.hex.kagune.control.king.WarModel.Part.values();
            for (int i = 0; i < parts.length; i++) {
                com.hex.kagune.control.king.WarModel.Part part = parts[i];
                String name = names[i];
                tris.addAll(placedLit(sink -> com.hex.kagune.control.king.TurningModel.build(sink, part, time, p), q -> onPart(pose, name, q)));
            }
            boolean side = ((String)beat[1]).startsWith("Side");
            double[] eye = side ? new double[] {4.8, 1.6, -0.6} : new double[] {-2.6, 1.7, 4.4};
            shots.add(new Shot((String)beat[1], pose, NO_WINGS, eye, look, tris));
        }
        int quads = 0;
        for (com.hex.kagune.control.king.WarModel.Part part : com.hex.kagune.control.king.WarModel.Part.values()) {
            int[] n = {0};
            com.hex.kagune.control.king.TurningModel.build((a, b, c, d, nn, ca, cb, cc, cd) -> n[0]++, part, 170.0, 0.95);
            quads += n[0];
        }
        System.out.println("turning: " + quads + " quads at 0.95");
        sheet(file, "The Dragon coming: Kaneki turning (1.0.11, nine seconds, immortal)", shots, 3, 560, 470);
    }

    /**
     * 1.0.15: Peace's blades at work (PeaceMotion), part way through each of its M1's blows and its abilities, on a body standing
     * still (forward is +z here), dummies where they would be struck.
     */
    static void peaceMoves(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        com.hex.kagune.control.king.KingMove blades = com.hex.kagune.control.king.KingMove.BLADES;
        com.hex.kagune.control.king.KingMove waltz = com.hex.kagune.control.king.KingMove.WALTZ;
        com.hex.kagune.control.king.KingMove horizon = com.hex.kagune.control.king.KingMove.HORIZON;
        Object[][] moves = {
            {com.hex.kagune.control.king.KingMove.NONE, 0, 0.0, "At rest: six, points ahead", 0},
            {blades, 0, 3.0, "M1 Lance: drawn back, coiled", 0},
            {blades, 0, 5.0, "M1 Lance: driven out", 0},
            {blades, 1, 5.0, "M1 Sweep: across the front", 4},
            {blades, 2, 6.0, "M1 Scissor: crossing", 4},
            {blades, 3, 6.5, "M1 Crucifix: the star", 5},
            {blades, 3, 9.0, "M1 Crucifix: impaled", 0},
            {waltz, 0, 7.0, "Z Waltz: the great pair sweep in", 4},
            {waltz, 0, 16.0, "Z Waltz: the lower pair cross", 0},
            {waltz, 0, 30.0, "Z Waltz: all six wheel round him", 4},
            {waltz, 0, 43.0, "Z Waltz: the final cross", 5},
            {com.hex.kagune.control.king.KingMove.AEGIS, 0, 20.0, "J Aegis: the wall (front)", 5},
            {com.hex.kagune.control.king.KingMove.AEGIS, 0, 20.0, "J Aegis: the wall (side)", 0},
            {com.hex.kagune.control.king.KingMove.AEGIS_OPEN, 0, 5.0, "J Aegis: flung open", 5},
            {com.hex.kagune.control.king.KingMove.ASCENT, 0, 20.0, "G Ascent: the living wings", 6},
            {com.hex.kagune.control.king.KingMove.DIVE, 0, 8.0, "G Ascent: the dive", 1},
            {com.hex.kagune.control.king.KingMove.LANDING, 0, 3.0, "G Ascent: landed", 0},
            {com.hex.kagune.control.king.KingMove.PIERCE, 0, 6.0, "B Piercing: drawn back", 0},
            {com.hex.kagune.control.king.KingMove.PIERCE, 0, 13.0, "B Piercing Salvation", 2},
            {com.hex.kagune.control.king.KingMove.REBIRTH, 0, 45.0, "C Rebirth: the crown, pulsing", 6},
            {horizon, 0, 22.0, "V Sanctuary: the wheel (front)", 5},
            {horizon, 0, 22.0, "V Sanctuary: the wheel (side)", 0},
            {horizon, 0, 113.0, "V The Final Cross: the star (front)", 5},
            {horizon, 0, 120.0, "V The Final Cross: through", 0},
        };
        for (Object[] m : moves) {
            com.hex.kagune.control.king.KingMove move = (com.hex.kagune.control.king.KingMove)m[0];
            double age = (double)m[2];
            int view = (int)m[4];
            double pitch = view == 1 ? -0.6 : 0.0;
            com.hex.kagune.control.king.PeaceMotion.State state = new com.hex.kagune.control.king.PeaceMotion.State(move, (int)m[1], age, pitch, null);
            RigPose pose = new RigPose();
            com.hex.kagune.control.client.animation.WarPose.peace(pose, 100.0 + age, 1.0F);
            List<WingPreview.Tri> tris = peaceTris(pose, 100.0 + age, 1.0, state);
            double[] eye;
            double[] look;
            if (view == 2) {
                eye = new double[] {-12.0, 7.0, 3.0};
                look = new double[] {0.0, 1.5, 7.0};
            } else if (view == 3) {
                eye = new double[] {-16.0, 9.0, 2.0};
                look = new double[] {0.0, 1.5, 11.0};
            } else if (view == 4) {
                eye = new double[] {-0.6, 11.0, 2.2};
                look = new double[] {0.0, 1.0, 2.0};
            } else if (view == 5) {
                eye = new double[] {1.5, 2.0, 9.0};
                look = new double[] {0.0, 1.5, 0.0};
            } else if (view == 6) {
                eye = new double[] {-3.0, 3.5, -7.0};
                look = new double[] {0.0, 1.5, 0.0};
            } else {
                eye = new double[] {-7.5, 4.0, 8.5};
                look = new double[] {0.0, 1.4, 2.5};
            }
            shots.add(new Shot((String)m[3], pose, NO_WINGS, eye, look, tris));
        }
        sheet(file, "Peace's six blades at work (1.0.18)", shots, 4, 480, 400);
    }

    // ------------------------------------------------------------------ 1.0.34: Peace reworked

    /** A blade's aim (PeaceMotion.State's) at {@code at} (preview frame: x his right, z ahead), its way in {@code way} (null: from its root). */
    static double[] peaceAim(double[] at, double out, double[] way, double back, double size) {
        double b = com.hex.kagune.control.king.PeaceMotion.BLOCK;
        double[] w = way == null ? new double[] {Double.NaN, Double.NaN, Double.NaN} : new double[] {-way[0] * b, way[1] * b, -way[2] * b};
        return new double[] {-at[0] * b, at[1] * b, -at[2] * b, out, w[0], w[1], w[2], back * b, size};
    }

    static double[] unit3(double[] v) {
        double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / l, v[1] / l, v[2] / l};
    }

    static double smooth01(double x) {
        double t = Math.max(0.0, Math.min(1.0, x));
        return t * t * (3.0 - 2.0 * t);
    }

    /** World shapes (the game's frame, him at the origin facing +z: his right is -x) into the preview's frame (his right is +x). */
    static List<WingPreview.Tri> game(java.util.function.Consumer<com.hex.kagune.control.ayato.UkakuModel.Sink> build) {
        return placedLit(build, q -> new double[] {-q[0], q[1], q[2]});
    }

    /** Peace posed in {@code move} at {@code age}, its blades as {@code aims} send them, lifted {@code up} blocks. */
    static List<WingPreview.Tri> peaceIn(com.hex.kagune.control.king.KingMove move, int level, double age, double[][] aims, double up, RigPose[] posed) {
        RigPose pose = new RigPose();
        com.hex.kagune.control.client.animation.WarPose.peace(pose, 300.0 + age, 1.0F);
        if (move != com.hex.kagune.control.king.KingMove.NONE) {
            com.hex.kagune.control.client.animation.PeacePose.act(pose, move, age, level, 1.0F);
        }
        pose.drop -= (float)(up * 16.0);
        posed[0] = pose;
        com.hex.kagune.control.king.PeaceMotion.State state = new com.hex.kagune.control.king.PeaceMotion.State(move, level, age, 0.0, aims);
        return peaceTris(pose, 300.0 + age, 1.0, state);
    }

    /** A wide flat ground at {@code y}, with a square hole {@code hole} across round (x, z) if {@code hole} > 0 (its walls going down). */
    static List<WingPreview.Tri> field(double y, double r, double[] at, double hole, double deep) {
        int c = 0xFF3A4034;
        int wall = 0xFF2A2620;
        List<WingPreview.Tri> out = new ArrayList<>();
        double[] up = {0.0, 1.0, 0.0};
        double[][] rects = hole <= 0.0 ? new double[][] {{-r, -r, r, r}}
            : new double[][] {{-r, -r, r, at[1] - hole}, {-r, at[1] + hole, r, r}, {-r, at[1] - hole, at[0] - hole, at[1] + hole},
                {at[0] + hole, at[1] - hole, r, at[1] + hole}};
        for (double[] q : rects) {
            out.add(new WingPreview.Tri(new double[][] {{q[0], y, q[1]}, {q[2], y, q[1]}, {q[2], y, q[3]}}, new int[] {c, c, c}, up, true));
            out.add(new WingPreview.Tri(new double[][] {{q[0], y, q[1]}, {q[2], y, q[3]}, {q[0], y, q[3]}}, new int[] {c, c, c}, up, true));
        }
        if (hole > 0.0) {
            double x0 = at[0] - hole, x1 = at[0] + hole, z0 = at[1] - hole, z1 = at[1] + hole, b = y - deep;
            double[][][] walls = {{{x0, y, z1}, {x1, y, z1}, {x1, b, z1}, {x0, b, z1}}, {{x0, y, z0}, {x0, y, z1}, {x0, b, z1}, {x0, b, z0}},
                {{x1, y, z1}, {x1, y, z0}, {x1, b, z0}, {x1, b, z1}}, {{x1, y, z0}, {x0, y, z0}, {x0, b, z0}, {x1, b, z0}}};
            for (double[][] q : walls) {
                double[] n = unit3(new double[] {at[0] - (q[0][0] + q[1][0]) * 0.5, 0.0, at[1] - (q[0][2] + q[1][2]) * 0.5});
                out.add(new WingPreview.Tri(new double[][] {q[0], q[1], q[2]}, new int[] {wall, wall, wall}, n, true));
                out.add(new WingPreview.Tri(new double[][] {q[0], q[2], q[3]}, new int[] {wall, wall, wall}, n, true));
            }
        }
        return out;
    }

    /** The blades in a body at {@code centre} (preview frame), each blade's tier at its own height, out {@code out[blade]} (NaN: not). */
    static double[][] impaled(double[] feet, double height, double[] out, int mode, double tear) {
        double[][] aims = new double[com.hex.kagune.control.king.PeaceMotion.BLADES][];
        for (int blade = 0; blade < aims.length; blade++) {
            if (Double.isNaN(out[blade])) {
                continue;
            }
            int tier = com.hex.kagune.control.king.PeaceMotion.tier(blade);
            double[] at = {feet[0], feet[1] + height * (0.62 - 0.14 * tier), feet[2]};
            double[] way = null;
            if (mode == 0) {
                // (the game's frame mirrored: his right is +x here, -x there)
                double[] g = com.hex.kagune.control.king.PeaceFxModel.waltzEntry(blade, new double[] {-feet[0], feet[1], feet[2]}, 0.6, height,
                    new double[] {-1.0, 0.0, 0.0});
                at = new double[] {-g[0], g[1], g[2]};
                double[] gw = com.hex.kagune.control.king.PeaceFxModel.waltzWay(blade, new double[] {0.0, 0.0, 1.0}, new double[] {-1.0, 0.0, 0.0});
                way = new double[] {-gw[0], gw[1], gw[2]};
            } else if (mode == 1) {
                double a = Math.toRadians(blade * 60.0 + 30.0);
                double tilt = Math.toRadians(tier == 0 ? 40.0 : tier == 1 ? 5.0 : -35.0);
                way = new double[] {Math.cos(a) * Math.cos(tilt), -Math.sin(tilt), -Math.sin(a) * Math.cos(tilt)};
            } else if (mode == 2) {
                way = unit3(new double[] {0.0, 1.0, 0.3});
            } else if (mode == 3) {
                way = unit3(new double[] {0.0, -1.0, 0.2});
            }
            if (tear > 0.0) {
                int r = com.hex.kagune.control.king.PeaceMotion.right(blade);
                double[] outward = unit3(new double[] {r, tier == 0 ? 0.8 : tier == 2 ? -0.6 : 0.1, 0.0});
                double k = smooth01(tear / 3.0) * 1.6;
                at = new double[] {at[0] + outward[0] * k, at[1] + outward[1] * k, at[2] + outward[2] * k};
            }
            aims[blade] = peaceAim(at, out[blade], way, 0.45, 1.1);
        }
        return aims;
    }

    /** 1.0.34: Peace's six rebuilt, frame by frame, as the server runs them and every client draws them. */
    /**
     * 1.0.40: Piercing Salvation alone, from every side: the six closed into one cone round the lance's line (built as the game builds it,
     * {@code PeaceFxModel.lanceCone} and the same aims {@code PeaceFx.lanceAims} gives), driven through a body, and parting again as it is
     * called back.
     */
    static void peacePierce(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        RigPose[] posed = new RigPose[1];
        int blades = com.hex.kagune.control.king.PeaceMotion.BLADES;
        int dummy = 0xFF2E4A2A;
        double[] way = {0.0, 0.0, 1.0};
        // {charge (0..1), out (1: the lance; less: parting back to him), where its point is (0: before him; else this far out), eye, look, label}
        Object[][] frames = {
            {0.35, 1.0, 0.0, new double[] {-8.0, 3.0, 3.0}, new double[] {0.0, 1.3, 3.5}, "charging: the six closing into a cone"},
            {1.0, 1.0, 0.0, new double[] {-9.0, 3.0, 4.5}, new double[] {0.0, 1.3, 5.0}, "held to the full, from his left"},
            {1.0, 1.0, 0.0, new double[] {4.0, 2.4, 14.0}, new double[] {0.0, 1.3, 5.0}, "held, from in front of the point"},
            {1.0, 1.0, 0.0, new double[] {3.2, 3.6, -6.0}, new double[] {0.0, 1.3, 6.0}, "held, from behind him"},
            {1.0, 1.0, 0.0, new double[] {0.01, 15.0, 5.0}, new double[] {0.0, 1.3, 5.0}, "held, from above"},
            {1.0, 1.0, 0.0, new double[] {-6.5, 2.8, 11.5}, new double[] {0.0, 1.32, 7.6}, "the spearhead, close"},
            {1.0, 1.0, 0.0, new double[] {0.6, 1.8, 14.5}, new double[] {0.0, 1.32, 8.6}, "the spearhead, head on"},
            {1.0, 1.0, 12.4, new double[] {-10.0, 4.5, 4.0}, new double[] {0.0, 1.2, 7.0}, "fired: through a body, carrying it"},
            {1.0, 0.55, 12.4, new double[] {-10.0, 4.5, 4.0}, new double[] {0.0, 1.2, 6.0}, "called back: parting, half way"},
            {1.0, 0.2, 12.4, new double[] {-10.0, 4.5, 4.0}, new double[] {0.0, 1.2, 4.0}, "called back: nearly home"}};
        for (Object[] f : frames) {
            double k = (double) f[0];
            double out = (double) f[1];
            double far = (double) f[2];
            double length = com.hex.kagune.control.king.PeaceRules.lance(k);
            double[] point = far <= 0.0 ? new double[] {0.0, 1.32, 1.1 + length} : new double[] {0.0, 1.3, far};
            double size = 1.1 + 0.8 * k;
            double[][] aims = new double[blades][];
            double[][] cone = com.hex.kagune.control.king.PeaceFxModel.lanceCone(way, size);
            for (int b = 0; b < blades; b++) {
                double[] c = cone[b];
                double[] a = peaceAim(point, out, new double[] {c[0], c[1], c[2]}, c[6], size);
                double px = com.hex.kagune.control.king.PeaceMotion.BLOCK;
                aims[b] = new double[] {a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], -c[3] * px, c[4] * px, -c[5] * px, 1.0};
            }
            List<WingPreview.Tri> tris = new ArrayList<>(peaceIn(com.hex.kagune.control.king.KingMove.PIERCE, far > 0.0 ? 1 : 0, far > 0.0 ? 8.0 : 60.0, aims, 0.0, posed));
            if (far > 0.0 && out >= 1.0) {
                tris.addAll(dummy(new double[] {0.0, 0.0, 12.0}, 0.6, 1.8, dummy));
            }
            tris.addAll(field(0.0, 16.0, null, 0.0, 0.0));
            shots.add(new Shot((String) f[5], posed[0], NO_WINGS, (double[]) f[3], (double[]) f[4], tris));
        }
        sheet(file, "Piercing Salvation (Peace B): the lance", shots, 3, 560, 420);
    }

    static void peaceRework(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        RigPose[] posed = new RigPose[1];
        int blades = com.hex.kagune.control.king.PeaceMotion.BLADES;
        int dummy = 0xFF2E4A2A;
        double[] chest = {0.0, 1.25, 0.0};
        double[] ahead = {0.0, 0.0, 1.0};
        double[] right = {-1.0, 0.0, 0.0};
        // Z: Crossblade Waltz, at a body 4.5 blocks before him.
        double[] prey = {0.0, 0.0, 4.5};
        for (double age : new double[] {com.hex.kagune.control.king.PeaceRules.WALTZ_FIRST + 2 * com.hex.kagune.control.king.PeaceRules.WALTZ_EVERY + 4.0,
            com.hex.kagune.control.king.PeaceRules.WALTZ_IN + 2.0, com.hex.kagune.control.king.PeaceRules.WALTZ_RIP + 2.0}) {
            double[] out = new double[blades];
            for (int b = 0; b < blades; b++) {
                double sent = com.hex.kagune.control.king.PeaceRules.waltzSent(b);
                out[b] = age < sent ? Double.NaN : smooth01((age - sent) / com.hex.kagune.control.king.PeaceRules.WALTZ_REACH);
            }
            double tear = age >= com.hex.kagune.control.king.PeaceRules.WALTZ_RIP ? age - com.hex.kagune.control.king.PeaceRules.WALTZ_RIP : 0.0;
            List<WingPreview.Tri> tris = new ArrayList<>(peaceIn(com.hex.kagune.control.king.KingMove.WALTZ, 0, age, impaled(prey, 1.8, out, 0, tear), 0.0, posed));
            tris.addAll(dummy(prey, 0.6, 1.8, dummy));
            tris.addAll(field(0.0, 8.0, null, 0.0, 0.0));
            String what = tear > 0.0 ? "Z Waltz: all six torn out at once" : age > com.hex.kagune.control.king.PeaceRules.WALTZ_IN ? "Z Waltz: all six in it"
                : "Z Waltz: one at a time, left in it";
            shots.add(new Shot(what, posed[0], NO_WINGS, new double[] {-6.5, 3.4, 1.0}, new double[] {0.0, 1.2, 2.6}, tris));
        }
        // J: Aegis of :Re, at rest and meeting a blow from his right.
        for (int frame = 0; frame < 2; frame++) {
            List<WingPreview.Tri> tris = new ArrayList<>(peaceIn(com.hex.kagune.control.king.KingMove.NONE, 0, 20.0, null, 0.0, posed));
            int working = frame;
            tris.addAll(game(sink -> {
                for (int i = 0; i < com.hex.kagune.control.king.PeaceRules.SHIELDS; i++) {
                    double[] anchor = com.hex.kagune.control.king.PeaceFxModel.shieldRest(i, chest, ahead, right, 40.0);
                    if (working == 1 && i == com.hex.kagune.control.king.PeaceRules.SHIELD_RIGHT) {
                        double[] from = unit3(new double[] {-2.0, 0.3, 2.0});
                        anchor = new double[] {chest[0] + from[0] * 1.35, chest[1] + from[1] * 1.35, chest[2] + from[2] * 1.35};
                    }
                    com.hex.kagune.control.king.PeaceFxModel.aegisShield(sink, i, chest, ahead, right, anchor, 1.0, 40.0, true, 8);
                }
            }));
            if (frame == 1) {
                tris.addAll(dummy(new double[] {2.4, 0.0, 2.4}, 0.6, 1.8, dummy));
            }
            tris.addAll(field(0.0, 8.0, null, 0.0, 0.0));
            shots.add(new Shot(frame == 0 ? "J Aegis: the three shields" : "J Aegis: one swung out to meet a blow", posed[0], NO_WINGS,
                frame == 0 ? new double[] {-3.6, 2.6, 4.6} : new double[] {4.4, 2.8, 5.4}, new double[] {0.0, 1.3, 0.3}, tris));
        }
        // G: White Wing Ascent: lifted on the two tendrils; the landing, the flesh torn up all round.
        {
            double h = 8.0;
            List<WingPreview.Tri> tris = new ArrayList<>(peaceIn(com.hex.kagune.control.king.KingMove.ASCENT, 0, 20.0, null, h, posed));
            double[] lifted = {0.0, h + 1.25, 0.0};
            tris.addAll(game(sink -> {
                com.hex.kagune.control.king.PeaceFxModel.liftTendril(sink, -1, lifted, ahead, right, new double[] {1.8, 0.0, -0.4}, 1.0, 50.0, 8);
                com.hex.kagune.control.king.PeaceFxModel.liftTendril(sink, 1, lifted, ahead, right, new double[] {-1.8, 0.0, -0.4}, 1.0, 50.0, 8);
            }));
            tris.addAll(field(0.0, 10.0, null, 0.0, 0.0));
            shots.add(new Shot("G Ascent: lifted on two tendrils", posed[0], NO_WINGS, new double[] {-12.0, 7.0, 12.0}, new double[] {0.0, 5.2, 0.0}, tris));
        }
        for (double age : new double[] {6.0, 16.0}) {
            List<WingPreview.Tri> tris = new ArrayList<>(peaceIn(com.hex.kagune.control.king.KingMove.LANDING, 0, Math.min(age, 8.0), null, 0.0, posed));
            double[][] layout = com.hex.kagune.control.king.PeaceFxModel.eruption(20341L, com.hex.kagune.control.king.PeaceRules.ERUPT_COUNT,
                com.hex.kagune.control.king.PeaceRules.ERUPT_RADIUS);
            tris.addAll(game(sink -> {
                for (int i = 0; i < layout.length; i++) {
                    double local = age - layout[i][7] / com.hex.kagune.control.king.PeaceRules.ERUPT_SPREAD;
                    if (local < 0.0) {
                        continue;
                    }
                    double[] base = {layout[i][0], 0.0, layout[i][1]};
                    double[][] path = com.hex.kagune.control.king.PeaceFxModel.eruptionPath(base, layout[i], 8);
                    com.hex.kagune.control.king.PeaceFxModel.flesh(sink, path, layout[i][3], smooth01(local / com.hex.kagune.control.king.PeaceRules.ERUPT_GROW),
                        null, 0.0, 60.0 + age, i * 31, 7);
                }
            }));
            tris.addAll(field(0.0, 34.0, null, 0.0, 0.0));
            shots.add(new Shot(age < 10.0 ? "G Ascent: the landing, the flesh breaking out" : "G Ascent: the eruption, all round (30 blocks)", posed[0], NO_WINGS,
                age < 10.0 ? new double[] {-4.5, 2.2, 4.5} : new double[] {-30.0, 26.0, 34.0}, age < 10.0 ? new double[] {0.0, 0.8, 0.0} : new double[] {0.0, 0.0, 2.0},
                tris));
        }
        // B: Piercing Salvation: gathered (held to the full), then through a body, carrying it.
        for (int frame = 0; frame < 2; frame++) {
            double k = 1.0;
            double length = com.hex.kagune.control.king.PeaceRules.lance(k);
            double[] way = {0.0, 0.0, 1.0};
            double[] point = frame == 0 ? new double[] {0.0, 1.32, 1.1 + length} : new double[] {0.0, 0.9 + 0.4, 12.0 + 0.4};
            double size = 1.1 + 0.8 * k;
            int[] order = {com.hex.kagune.control.king.PeaceMotion.GREAT, com.hex.kagune.control.king.PeaceMotion.ML, com.hex.kagune.control.king.PeaceMotion.UR,
                com.hex.kagune.control.king.PeaceMotion.UL, com.hex.kagune.control.king.PeaceMotion.LR, com.hex.kagune.control.king.PeaceMotion.LL};
            double[][] aims = new double[blades][];
            double[][] cone = com.hex.kagune.control.king.PeaceFxModel.lanceCone(way, size);
            for (int b = 0; b < blades; b++) {
                double[] c = cone[b];
                double[] a = peaceAim(point, 1.0, new double[] {c[0], c[1], c[2]}, c[6], size);
                double px = com.hex.kagune.control.king.PeaceMotion.BLOCK;
                aims[b] = new double[] {a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], -c[3] * px, c[4] * px, -c[5] * px, 1.0};
            }
            List<WingPreview.Tri> tris = new ArrayList<>(peaceIn(com.hex.kagune.control.king.KingMove.PIERCE, frame, frame == 0 ? 60.0 : 8.0, aims, 0.0, posed));
            if (frame == 1) {
                tris.addAll(dummy(new double[] {0.0, 0.0, 12.0}, 0.6, 1.8, dummy));
            }
            tris.addAll(field(0.0, 14.0, null, 0.0, 0.0));
            shots.add(new Shot(frame == 0 ? "B Piercing: the lance, held to the full" : "B Piercing: through it, carrying it on", posed[0], NO_WINGS,
                frame == 0 ? new double[] {-9.0, 3.6, 3.0} : new double[] {-10.0, 4.5, 4.0}, frame == 0 ? new double[] {0.0, 1.3, 5.0} : new double[] {0.0, 1.2, 7.0}, tris));
        }
        // C: Rebirth Instinct: the blades in the ground, four coming up; then the four at his back, one cutting.
        double[][] spots = {{-1.6, 3.2}, {1.7, 3.0}, {-3.6, 2.2}, {3.4, 2.6}};
        for (int frame = 0; frame < 3; frame++) {
            double[][] aims = null;
            if (frame == 0) {
                aims = new double[blades][];
                for (int b = 0; b < blades; b++) {
                    int r = com.hex.kagune.control.king.PeaceMotion.right(b);
                    int tier = com.hex.kagune.control.king.PeaceMotion.tier(b);
                    double[] at = {r * (0.7 + 0.5 * tier), -0.75, 1.3 + 0.55 * (2 - tier)};
                    aims[b] = peaceAim(at, 1.0, unit3(new double[] {0.0, -1.0, 0.25}), 0.2, 1.1);
                }
            }
            List<WingPreview.Tri> body = peaceIn(com.hex.kagune.control.king.KingMove.REBIRTH, frame == 0 ? 0 : 1, frame == 0 ? 40.0 : 30.0, aims, 0.0, posed);
            List<WingPreview.Tri> tris = new ArrayList<>(frame == 2 ? List.of() : body);
            int f = frame;
            tris.addAll(game(sink -> {
                for (int i = 0; i < spots.length; i++) {
                    double[] feet = f == 0 ? new double[] {-spots[i][0], 0.0, spots[i][1]}
                        : new double[] {-(i % 2 == 0 ? -1.0 : 1.0) * (1.3 + 0.9 * (i / 2)), 0.0, -2.6 - 0.5 * (i % 2)};
                    double rise = f == 0 ? 0.35 + 0.18 * i : 1.0;
                    double swing = f >= 1 && i == 1 ? 0.42 : Double.NaN;
                    double bladesOut = f >= 1 && i % 2 == 1 ? 1.0 : 0.0;
                    double yaw = f == 0 ? 0.0 : Math.toRadians(-10.0 + 20.0 * i);
                    if (f == 2 && i != 1) {
                        continue;
                    }
                    com.hex.kagune.control.king.PeaceFxModel.thrall(sink, feet, yaw, 1.3 * i, f >= 1 && i == 2 ? 1.0 : 0.0, bladesOut, swing, 1, rise, 0.0, 77.0,
                        i * 13);
                }
            }));
            tris.addAll(field(0.0, 10.0, null, 0.0, 0.0));
            String what = frame == 0 ? "C Rebirth: the blades in the ground, four coming up" : frame == 1 ? "C Rebirth: the four at his back, apart" : "C Rebirth: one, blades out, cutting";
            double[] eye = frame == 0 ? new double[] {-5.0, 4.0, 9.0} : frame == 1 ? new double[] {-6.0, 4.0, -9.0} : new double[] {-0.6, 1.9, -0.4};
            double[] look = frame == 0 ? new double[] {0.0, 0.8, 2.0} : frame == 1 ? new double[] {0.0, 1.0, -2.0} : new double[] {1.3, 1.2, -3.1};
            shots.add(new Shot(what, frame == 2 ? null : posed[0], NO_WINGS, eye, look, tris));
        }
        // V: The Final Horizon: the uppercut; up under it, five in from all round; the great one's blow; driven down under the ground.
        {
            double[] front = {0.0, 0.0, 1.5};
            double[] out = new double[blades];
            java.util.Arrays.fill(out, Double.NaN);
            out[com.hex.kagune.control.king.PeaceMotion.GREAT] = 1.0;
            List<WingPreview.Tri> tris = new ArrayList<>(peaceIn(com.hex.kagune.control.king.KingMove.HORIZON, 0, com.hex.kagune.control.king.PeaceRules.HORIZON_UPPER + 1.0,
                impaled(new double[] {0.0, 1.2, 1.5}, 1.8, out, 2, 0.0), 0.0, posed));
            tris.addAll(dummy(new double[] {front[0], 1.2, front[2]}, 0.6, 1.8, dummy));
            tris.addAll(field(0.0, 8.0, null, 0.0, 0.0));
            shots.add(new Shot("V Horizon: the great blade's uppercut", posed[0], NO_WINGS, new double[] {-6.0, 2.6, 2.5}, new double[] {0.0, 1.6, 1.0}, tris));
        }
        for (int frame = 0; frame < 2; frame++) {
            double age = frame == 0 ? com.hex.kagune.control.king.PeaceRules.HORIZON_STRIKE - 4.0 : com.hex.kagune.control.king.PeaceRules.HORIZON_STRIKE + 1.0;
            double[] out = new double[blades];
            for (int b = 0; b < blades; b++) {
                out[b] = b == com.hex.kagune.control.king.PeaceMotion.GREAT && frame == 0 ? Double.NaN : 1.0;
            }
            double[] body = {0.0, 2.9, 0.9};
            double[][] aims = impaled(body, 1.8, out, 1, 0.0);
            if (frame == 1) {
                aims[com.hex.kagune.control.king.PeaceMotion.GREAT] = impaled(body, 1.8, out, 3, 0.0)[com.hex.kagune.control.king.PeaceMotion.GREAT];
            }
            List<WingPreview.Tri> tris = new ArrayList<>(peaceIn(com.hex.kagune.control.king.KingMove.HORIZON, 0, age, aims, 0.0, posed));
            tris.addAll(dummy(body, 0.6, 1.8, dummy));
            shots.add(new Shot(frame == 0 ? "V Horizon: up under it, five in from all round" : "V Horizon: the great one's blow from over it", posed[0], NO_WINGS,
                new double[] {-6.5, 3.0, 6.0}, new double[] {0.0, 2.8, 0.6}, tris));
        }
        {
            double hover = 4.5;
            double depth = 6.0;
            double[] body = {0.0, -depth, 1.6};
            double[] out = new double[blades];
            java.util.Arrays.fill(out, 1.0);
            double[][] aims = impaled(new double[] {body[0], body[1] - hover, body[2]}, 1.8, out, 1, 0.0);
            aims[com.hex.kagune.control.king.PeaceMotion.GREAT] = impaled(new double[] {body[0], body[1] - hover, body[2]}, 1.8, out, 3, 0.0)[
                com.hex.kagune.control.king.PeaceMotion.GREAT];
            List<WingPreview.Tri> tris = new ArrayList<>(peaceIn(com.hex.kagune.control.king.KingMove.HORIZON, 0, com.hex.kagune.control.king.PeaceRules.HORIZON_STRIKE
                + 40.0, aims, hover, posed));
            tris.addAll(dummy(body, 0.6, 1.8, dummy));
            tris.addAll(field(0.0, 10.0, new double[] {body[0], body[2]}, 0.9, depth + 0.5));
            shots.add(new Shot("V Horizon: the blades driving it down through the ground", posed[0], NO_WINGS, new double[] {-9.0, 8.0, 9.0},
                new double[] {0.0, 0.5, 1.2}, tris));
        }
        sheet(file, "Peace reworked (1.0.34): the six", shots, 4, 480, 400);
    }

    /** 1.0.20: Peace's new blows (the launcher, the air spike, the heavy's three, the charge), each wound up, landing and through. */
    static void peaceCombo(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        com.hex.kagune.control.king.PeaceStrike[] blows = {com.hex.kagune.control.king.PeaceStrike.ASCEND, com.hex.kagune.control.king.PeaceStrike.JUDGMENT,
            com.hex.kagune.control.king.PeaceStrike.CROSS_CLEAVE, com.hex.kagune.control.king.PeaceStrike.SERAPH, com.hex.kagune.control.king.PeaceStrike.HALO};
        String[] names = {"Ascend (launcher)", "Judgment (air spike)", "Cross Cleave (RMB)", "Seraph Thrust (RMB held)", "Halo Rend (RMB ender)"};
        for (double age : new double[] {3.0, 14.0}) {
            com.hex.kagune.control.king.PeaceMotion.State state = new com.hex.kagune.control.king.PeaceMotion.State(com.hex.kagune.control.king.KingMove.CHARGE, 0, age,
                0.0, null);
            RigPose pose = new RigPose();
            com.hex.kagune.control.client.animation.WarPose.peace(pose, 100.0 + age, 1.0F);
            com.hex.kagune.control.client.animation.PeacePose.act(pose, com.hex.kagune.control.king.KingMove.CHARGE, age, 0, 1.0F);
            shots.add(new Shot("RMB held: charging (" + (int)age + "t)", pose, NO_WINGS, new double[] {-7.5, 4.0, 8.5}, new double[] {0.0, 1.4, 2.5},
                peaceTris(pose, 100.0 + age, 1.0, state)));
        }
        for (int i = 0; i < blows.length; i++) {
            com.hex.kagune.control.king.PeaceStrike blow = blows[i];
            double[] ages = {blow.impact - 2.0, blow.impact, blow.impact + 2.0};
            String[] when = {"wound up", "landing", "through"};
            for (int k = 0; k < ages.length; k++) {
                double age = ages[k];
                com.hex.kagune.control.king.PeaceMotion.State state = new com.hex.kagune.control.king.PeaceMotion.State(com.hex.kagune.control.king.KingMove.BLADES,
                    blow.ordinal(), age, 0.0, null);
                RigPose pose = new RigPose();
                com.hex.kagune.control.client.animation.WarPose.peace(pose, 100.0 + age, 1.0F);
                com.hex.kagune.control.client.animation.PeacePose.act(pose, com.hex.kagune.control.king.KingMove.BLADES, age, blow.ordinal(), 1.0F);
                double[] eye = blow == com.hex.kagune.control.king.PeaceStrike.HALO ? new double[] {-0.6, 11.0, 2.2}
                    : blow == com.hex.kagune.control.king.PeaceStrike.SERAPH ? new double[] {-12.0, 7.0, 3.0} : new double[] {-7.5, 4.0, 8.5};
                double[] look = blow == com.hex.kagune.control.king.PeaceStrike.HALO ? new double[] {0.0, 1.0, 0.5}
                    : blow == com.hex.kagune.control.king.PeaceStrike.SERAPH ? new double[] {0.0, 1.5, 4.0} : new double[] {0.0, 1.4, 2.5};
                shots.add(new Shot(names[i] + ": " + when[k], pose, NO_WINGS, eye, look, peaceTris(pose, 100.0 + age, 1.0, state)));
            }
        }
        sheet(file, "Peace's new blows (1.0.20): launcher, air spike, the heavy", shots, 3, 480, 400);
    }

    /** 1.0.26: {@code tris} as first person draws them: nothing of the body, and no shape with a corner this close to the eyes (blocks). */
    static List<WingPreview.Tri> seenFrom(List<WingPreview.Tri> tris, double[] eye, double clear) {
        List<WingPreview.Tri> out = new ArrayList<>();
        for (WingPreview.Tri t : tris) {
            boolean near = false;
            for (double[] v : t.p()) {
                double dx = v[0] - eye[0];
                double dy = v[1] - eye[1];
                double dz = v[2] - eye[2];
                near |= dx * dx + dy * dy + dz * dz < clear * clear;
            }
            if (!near) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * 1.0.26: what each final kakuja's wearer sees of it in first person (the game draws there Peace's blades on their arms and War's
     * rinkaku, on the body as posed, never the body itself): from his eyes, a 70 degree lens, looking ahead and down, at rest, at work
     * and running.
     */
    static void kingFirstPerson(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        double[] eye = {0.0, 1.62, 0.0};
        double[] ahead = {0.0, 1.5, 6.0};
        double[] down = {0.0, -1.2, 5.0};
        com.hex.kagune.control.king.KingMove none = com.hex.kagune.control.king.KingMove.NONE;
        com.hex.kagune.control.king.KingMove blades = com.hex.kagune.control.king.KingMove.BLADES;
        // Peace: the blades on their arms (PeaceModel's blades-only build, as first person has it).
        Object[][] peace = {{none, 0, 0.0, 0.0, "Peace at rest, ahead", ahead}, {none, 0, 0.0, 0.0, "Peace at rest, looking down", down},
            {none, 0, 0.0, 1.0, "Peace running", ahead}, {blades, 0, 4.0, 0.0, "Peace Lance, on its way", ahead},
            {blades, 0, 5.0, 0.0, "Peace Lance, landing", ahead}, {blades, 1, 5.0, 0.0, "Peace Sweep", ahead}, {blades, 2, 6.0, 0.0, "Peace Scissor", ahead},
            {blades, 3, 9.0, 0.0, "Peace Crucifix", ahead}};
        for (Object[] m : peace) {
            com.hex.kagune.control.king.KingMove move = (com.hex.kagune.control.king.KingMove)m[0];
            double age = (double)m[2];
            double t = 700.0 + age;
            com.hex.kagune.control.king.PeaceMotion.State state = new com.hex.kagune.control.king.PeaceMotion.State(move, (int)m[1], age, 0.0, null, none, 0,
                0.0, 0.0, (double)m[3], 0.0);
            RigPose pose = new RigPose();
            com.hex.kagune.control.client.animation.WarPose.peace(pose, t, 1.0F);
            if (move != none) {
                RigPose b = new RigPose();
                com.hex.kagune.control.client.animation.PeacePose.act(b, move, age, (int)m[1], 1.0F);
                pose.add(b.legs(0.0F, 0.9F), 1.0F);
            }
            List<WingPreview.Tri> tris = new ArrayList<>(placedLit(sink -> com.hex.kagune.control.king.PeaceModel.build(sink,
                com.hex.kagune.control.king.WarModel.Part.TORSO, t, 1.0, state, true), q -> onPart(pose, "torso", q)));
            shots.add(new Shot((String)m[4], null, NO_WINGS, eye, (double[])m[5], seenFrom(tris, eye, 0.45)));
        }
        // War: his rinkaku (WarModel.rinkaku, as first person has it), at rest and through moves that send them out.
        com.hex.kagune.control.king.KingMove strike = com.hex.kagune.control.king.KingMove.STRIKE;
        Object[][] war = {{none, 0, 0.0, "War at rest, ahead", ahead}, {none, 0, 0.0, "War at rest, looking down", down},
            {none, 0, 0.0, "War at rest, looking up", new double[] {0.0, 4.5, 5.0}}, {strike, 5, 7.0, "War Verdict", ahead},
            {strike, 10, 6.0, "War Rend", ahead}, {strike, 9, 8.0, "War Calamity", down}, {com.hex.kagune.control.king.KingMove.COUNTER, 0, 4.0,
            "War Defiance: the counter", ahead}, {com.hex.kagune.control.king.KingMove.SPIRE, 0, 8.0, "War Spire Wave", down}};
        for (Object[] m : war) {
            com.hex.kagune.control.king.KingMove move = (com.hex.kagune.control.king.KingMove)m[0];
            double age = (double)m[2];
            double t = 900.0 + age;
            int level = (int)m[1];
            RigPose pose = new RigPose();
            com.hex.kagune.control.client.animation.WarPose.apply(pose, t, 7, 1.0F, 0.0F);
            com.hex.kagune.control.king.WarModel.Motion motion = com.hex.kagune.control.king.WarModel.Motion.of(move, level, move.progress(age, level));
            List<WingPreview.Tri> tris = new ArrayList<>(placedLit(sink -> com.hex.kagune.control.king.WarModel.rinkaku(sink, t, 1.0, motion),
                q -> onPart(pose, "torso", q)));
            shots.add(new Shot((String)m[3], null, NO_WINGS, eye, (double[])m[4], seenFrom(tris, eye, 0.45)));
        }
        double saved = fov;
        fov = 70.0;
        sheet(file, "1.0.26: first person: what each wearer sees of it (no body, nothing against the eyes)", shots, 4, 420, 300);
        fov = saved;
    }

    /** 1.0.26: Peace's body sprinting at {@code phase}, as the game poses it, a blow of its own ({@code move}) on top from the hips up. */
    static RigPose peaceRun(double phase, double time, com.hex.kagune.control.king.KingMove move, int level, double age) {
        RigPose p = new RigPose();
        com.hex.kagune.control.client.animation.WarPose.peace(p, time, 0.3F);
        boolean blow = move != com.hex.kagune.control.king.KingMove.NONE;
        com.hex.kagune.control.client.animation.KingRun.peace(p, phase, 1.0F, 1.0F, blow ? 0.0F : 1.0F, time);
        if (blow) {
            RigPose b = new RigPose();
            com.hex.kagune.control.client.animation.PeacePose.act(b, move, age, level, 1.0F);
            p.add(b.legs(0.0F, 0.9F), 1.0F);
        }
        p.settle(1.0F);
        return p;
    }

    /** 1.0.26: War's body on the warpath at {@code phase}, a blow of his M1 ({@code level}, -1: none) on top from the hips up. */
    static RigPose warRun(double phase, double time, int level, double age) {
        RigPose p = new RigPose();
        boolean blow = level >= 0;
        com.hex.kagune.control.client.animation.WarPose.apply(p, time, 7, 0.4F * (blow ? 0.5F : 1.0F), 1.0F);
        com.hex.kagune.control.client.animation.KingRun.war(p, phase, 1.0F, 1.0F, blow ? 0.0F : 1.0F, time, 7);
        if (blow) {
            RigPose b = new RigPose();
            com.hex.kagune.control.client.animation.WarPose.strikes(b, level, age, -1, 0.0, 1.0F);
            p.add(b.legs(0.0F, 0.9F), 1.0F);
        }
        p.settle(1.0F);
        return p;
    }

    /**
     * 1.0.26: the final kakujas' runs (KingRun): Peace's (Naoya's) and War's (the warpath) through a stride from the side, then from the
     * front three-quarters, and each running on through blows (the legs never the blow's).
     */
    static void kingRuns(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        double[] side = {-9.0, 1.7, 0.9};
        double[] sideLook = {0.0, 1.05, 0.9};
        double[] front = {-6.0, 3.0, 7.5};
        double[] frontLook = {0.0, 1.1, 0.5};
        com.hex.kagune.control.king.KingMove none = com.hex.kagune.control.king.KingMove.NONE;
        com.hex.kagune.control.king.PeaceMotion.State running = new com.hex.kagune.control.king.PeaceMotion.State(none, 0, 0.0, 0.0, null, none, 0, 0.0, 0.0,
            1.0, 0.0);
        for (int k = 0; k < 6; k++) {
            double phase = k * Math.PI / 3.0;
            double t = 300.0 + k * 2.0;
            RigPose pose = peaceRun(phase, t, none, 0, 0.0);
            shots.add(new Shot("Peace sprint " + (k * 60) + " deg", pose, NO_WINGS, side, sideLook, peaceTris(pose, t, 1.0, running)));
        }
        Object[][] peaceBlows = {{-1, 0.0, "Peace sprint, front"}, {0, 4.0, "running, Lance"}, {1, 5.0, "running, Sweep"}, {2, 6.0, "running, Scissor"},
            {3, 9.0, "running, Crucifix"}, {6, 5.0, "running, Cross Cleave"}};
        for (int k = 0; k < peaceBlows.length; k++) {
            int level = (int)peaceBlows[k][0];
            double age = (double)peaceBlows[k][1];
            double phase = 0.7 + k * 1.1;
            double t = 400.0 + k * 3.0;
            com.hex.kagune.control.king.KingMove move = level < 0 ? none : com.hex.kagune.control.king.KingMove.BLADES;
            RigPose pose = peaceRun(phase, t, move, Math.max(0, level), age);
            com.hex.kagune.control.king.PeaceMotion.State state = level < 0 ? running : new com.hex.kagune.control.king.PeaceMotion.State(move, level, age, 0.0,
                null, none, 0, 0.0, 0.0, 1.0, 0.0);
            shots.add(new Shot((String)peaceBlows[k][2], pose, NO_WINGS, front, frontLook, peaceTris(pose, t, 1.0, state)));
        }
        for (int k = 0; k < 6; k++) {
            double phase = k * Math.PI / 3.0;
            double t = 500.0 + k * 2.0;
            RigPose pose = warRun(phase, t, -1, 0.0);
            shots.add(new Shot("War warpath " + (k * 60) + " deg", pose, NO_WINGS, side, sideLook, warTris(pose, t, 1.0)));
        }
        Object[][] warBlows = {{-1, 0.0, "War warpath, front"}, {0, 3.0, "running, Jab"}, {1, 4.0, "running, Reap"}, {2, 4.0, "running, Side Kick"},
            {6, 5.0, "running, Ascension"}, {8, 7.0, "running, Cleaver"}};
        for (int k = 0; k < warBlows.length; k++) {
            int level = (int)warBlows[k][0];
            double age = (double)warBlows[k][1];
            double phase = 0.7 + k * 1.1;
            double t = 600.0 + k * 3.0;
            RigPose pose = warRun(phase, t, level, age);
            shots.add(new Shot((String)warBlows[k][2], pose, NO_WINGS, front, frontLook, warTris(pose, t, 1.0)));
        }
        sheet(file, "1.0.26: the runs: Peace (Naoya's) and War (the warpath), and running on through blows", shots, 6, 360, 380);
        // The bodies alone, close, from the side: the run itself.
        List<Shot> bodies = new ArrayList<>();
        double[] close = {-5.2, 1.2, 0.9};
        double[] closeLook = {0.0, 1.0, 0.9};
        for (int k = 0; k < 8; k++) {
            double phase = k * Math.PI / 4.0;
            RigPose pose = peaceRun(phase, 300.0 + k, none, 0, 0.0);
            bodies.add(new Shot("Peace " + (k * 45) + " deg", pose, NO_WINGS, close, closeLook, new ArrayList<>()));
        }
        for (int k = 0; k < 8; k++) {
            double phase = k * Math.PI / 4.0;
            RigPose pose = warRun(phase, 500.0 + k, -1, 0.0);
            bodies.add(new Shot("War " + (k * 45) + " deg", pose, NO_WINGS, close, closeLook, new ArrayList<>()));
        }
        sheet(new File(file.getParentFile(), "king-runs-body.png"), "1.0.26: the runs, the body alone, from his left", bodies, 8, 260, 330);
        // Both strides as films: a frame every sixteenth of a stride, the whole form on, from his left and a little ahead.
        File frames = new File(file.getParentFile(), "king-runs");
        frames.mkdirs();
        double[] quarter = {-7.5, 2.6, 4.0};
        double[] quarterLook = {0.0, 1.1, 0.6};
        for (int k = 0; k < 16; k++) {
            double phase = k * Math.PI / 8.0;
            double t = 800.0 + k;
            RigPose peace = peaceRun(phase, t, none, 0, 0.0);
            List<Shot> one = new ArrayList<>();
            one.add(new Shot("Peace", peace, NO_WINGS, quarter, quarterLook, peaceTris(peace, t, 1.0, running)));
            RigPose war = warRun(phase, t, -1, 0.0);
            one.add(new Shot("War", war, NO_WINGS, quarter, quarterLook, warTris(war, t, 1.0)));
            sheet(new File(frames, String.format(java.util.Locale.ROOT, "run-%02d.png", k)), "1.0.26: the runs", one, 2, 360, 340);
        }
    }

    /** 1.0.27: War's body in one of the warpath's moves, {@code age} ticks in (all of him: the legs are the move's, as in the air). */
    static RigPose warAct(com.hex.kagune.control.king.KingMove move, double age, double time, boolean legs) {
        RigPose p = new RigPose();
        com.hex.kagune.control.client.animation.WarPose.apply(p, time, 7, 0.5F, 0.0F);
        RigPose b = new RigPose();
        com.hex.kagune.control.client.animation.WarPose.act(b, move, age, 0, 1.0F);
        p.add(b.legs(legs ? 1.0F : 0.0F, 0.9F), 1.0F);
        p.settle(1.0F);
        return p;
    }

    /** 1.0.27: the cone's test ground: a step up, a ledge off to one side, a drop on the other (block tops, blocks). */
    static double coneGround(double x, double z) {
        double h = 0.0;
        if (z >= 5.0) h += 1.0;
        if (z >= 9.0 && x > 1.0) h += 1.0;
        if (z >= 11.0 && x < -2.0) h -= 2.0;
        return h;
    }

    /** That ground as blocks: their tops, and their sides where the next one is lower. */
    static List<WingPreview.Tri> coneTerrain() {
        List<WingPreview.Tri> out = new ArrayList<>();
        for (int x = -9; x < 9; x++) {
            for (int z = -3; z < 18; z++) {
                double h = coneGround(x + 0.5, z + 0.5);
                int top = (x + z & 1) == 0 ? 0xFF3A4048 : 0xFF343A42;
                double[] up = {0, 1, 0};
                out.add(new WingPreview.Tri(new double[][] {{x, h, z}, {x + 1, h, z}, {x + 1, h, z + 1}}, new int[] {top, top, top}, up, true));
                out.add(new WingPreview.Tri(new double[][] {{x, h, z}, {x + 1, h, z + 1}, {x, h, z + 1}}, new int[] {top, top, top}, up, true));
                int side = 0xFF262B31;
                double[][] next = {{x - 0.5, z + 0.5, -1, 0}, {x + 1.5, z + 0.5, 1, 0}, {x + 0.5, z - 0.5, 0, -1}, {x + 0.5, z + 1.5, 0, 1}};
                for (double[] nb : next) {
                    double low = coneGround(nb[0], nb[1]);
                    if (low >= h) continue;
                    double ex = nb[2] < 0 ? x : nb[2] > 0 ? x + 1 : -1;
                    double ez = nb[3] < 0 ? z : nb[3] > 0 ? z + 1 : -1;
                    double[] n = {nb[2], 0, nb[3]};
                    double[][] q = nb[2] != 0
                        ? new double[][] {{ex, low, z}, {ex, h, z}, {ex, h, z + 1}, {ex, low, z + 1}}
                        : new double[][] {{x, low, ez}, {x, h, ez}, {x + 1, h, ez}, {x + 1, low, ez}};
                    out.add(new WingPreview.Tri(new double[][] {q[0], q[1], q[2]}, new int[] {side, side, side}, n, true));
                    out.add(new WingPreview.Tri(new double[][] {q[0], q[2], q[3]}, new int[] {side, side, side}, n, true));
                }
            }
        }
        return out;
    }

    /**
     * 1.0.27: the cone's spikes laid out as the server lays them ({@code WarPath.cone}: row by row out from his hand, each row on the
     * ground where it stands, following it on from the row before), {@code age} ticks into the swipe, on {@link #coneGround}.
     */
    static List<WingPreview.Tri> coneSpikes(double age) {
        List<WingPreview.Tri> out = new ArrayList<>();
        long seed = 77L;
        int index = 0;
        for (int row = 0; row < com.hex.kagune.control.king.WarRules.CONE_ROWS; row++) {
            int n = com.hex.kagune.control.king.WarRules.coneSpikes(row);
            for (int k = 0; k < n; k++) {
                double j1 = unit(seed, row * 17 + k) * 2.0 - 1.0;
                double j2 = unit(seed, row * 17 + k + 7) * 2.0 - 1.0;
                double across = com.hex.kagune.control.king.WarRules.coneAcross(row, k, j1);
                double along = com.hex.kagune.control.king.WarRules.coneAlong(row) + 0.35 * j2;
                double y = coneGround(across, along);
                double side = across == 0.0 ? 0.0 : Math.signum(across);
                double[] lean = {0.25 * side, 0.0, 0.35};
                double height = 1.1 + 0.11 * row + 0.4 * unit(seed, row * 17 + k + 3);
                double width = 0.24 + 0.012 * row;
                double since = age - (com.hex.kagune.control.king.WarRules.CONE_SWIPE + row);
                int s = index++ * 7 + 3;
                if (since <= 0.0) continue;
                double grow = 1.6;
                double sink = 6.0;
                double up = Math.min(1.0, since / grow) * Math.max(0.0, Math.min(1.0, (grow + com.hex.kagune.control.king.WarRules.CONE_STAND + sink - since) / sink));
                if (up <= 0.01) continue;
                double[] base = {across, y, along};
                out.addAll(placedLit(sk -> com.hex.kagune.control.king.KingFxModel.needle(sk, base, lean, height, width * 0.45, up, s), q -> q));
            }
        }
        return out;
    }

    /** 1.0.27: King's Execution at nothing: the rings of spikes round him ({@code WarPath.groundSpikes}), {@code since} ticks up. */
    static List<WingPreview.Tri> riseSpikes(double since) {
        List<WingPreview.Tri> out = new ArrayList<>();
        double[][] rings = {{1.6, 6}, {3.0, 10}, {4.4, 14}};
        long seed = 31L;
        for (int ring = 0; ring < rings.length; ring++) {
            int n = (int)rings[ring][1];
            for (int k = 0; k < n; k++) {
                double a = (k + 0.5 * unit(seed, ring * 40 + k)) / n * Math.PI * 2.0 + ring * 0.4;
                double r = rings[ring][0] + 0.4 * (unit(seed, ring * 40 + k + 20) - 0.5);
                double[] dir = {Math.cos(a), 0.0, Math.sin(a)};
                double height = 1.5 + 0.45 * ring + 0.5 * unit(seed, ring * 40 + k + 9);
                double width = 0.26 + 0.04 * ring;
                double t = since - ring * 2;
                if (t <= 0.0) continue;
                double up = Math.min(1.0, t / 1.6);
                int s = ring * 40 + k;
                double[] base = {dir[0] * r, 0.0, dir[2] * r};
                double[] lean = {dir[0] * 0.45, 0.0, dir[2] * 0.45};
                out.addAll(placedLit(sk -> com.hex.kagune.control.king.KingFxModel.needle(sk, base, lean, height, width * 0.45, up, s), q -> q));
            }
        }
        return out;
    }

    static double unit(long seed, int k) {
        long h = seed * 0x9E3779B97F4A7C15L + k * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    /**
     * 1.0.27: the warpath's reworked moves: the RMB's swipe and the cone it sends over the ground (a step up, a ledge, a drop), King's
     * Execution at nothing (both hands up, the rings of spikes) and at a body (the blitz, beat by beat), and Black Sun.
     */
    static void warpath(File file) throws Exception {
        com.hex.kagune.control.king.KingMove cone = com.hex.kagune.control.king.KingMove.CONE;
        List<Shot> shots = new ArrayList<>();
        double[] behind = {-9.0, 11.0, -7.0};
        double[] behindLook = {0.0, 0.0, 7.5};
        for (double age : new double[] {2.0, 4.0, 8.0, 12.0, 17.0, 24.0}) {
            RigPose pose = warAct(cone, Math.min(age, cone.ticks() + 1.0), 300.0 + age, false);
            List<WingPreview.Tri> extra = coneTerrain();
            extra.addAll(coneSpikes(age));
            extra.addAll(warTris(pose, 300.0 + age, 1.0));
            shots.add(new Shot(String.format(java.util.Locale.ROOT, "RMB cone, %.0f t", age), pose, NO_WINGS, behind, behindLook, extra));
        }
        double[] side = {-3.5, 2.6, 6.0};
        double[] sideLook = {0.0, 1.0, 0.6};
        for (double age : new double[] {2.0, 3.5, 7.0}) {
            RigPose pose = warAct(cone, age, 320.0 + age, false);
            shots.add(new Shot(String.format(java.util.Locale.ROOT, "the swipe, %.1f t", age), pose, NO_WINGS, side, sideLook, warTris(pose, 320.0 + age, 1.0)));
        }
        com.hex.kagune.control.king.KingMove rise = com.hex.kagune.control.king.KingMove.RISE;
        double[] high = {-7.0, 5.0, 7.0};
        double[] highLook = {0.0, 0.8, 0.0};
        for (double age : new double[] {1.0, 4.0, 9.0}) {
            RigPose pose = warAct(rise, age, 340.0 + age, false);
            List<WingPreview.Tri> extra = new ArrayList<>(ground());
            extra.addAll(riseSpikes(age - com.hex.kagune.control.king.WarRules.RISE_SPIKES));
            extra.addAll(warTris(pose, 340.0 + age, 1.0));
            shots.add(new Shot(String.format(java.util.Locale.ROOT, "Execution at nothing, %.0f t", age), pose, NO_WINGS, high, highLook, extra));
        }
        com.hex.kagune.control.king.KingMove blitz = com.hex.kagune.control.king.KingMove.BLITZ;
        double[] quarter = {-5.0, 2.0, 4.5};
        double[] quarterLook = {0.0, 1.0, 0.5};
        Object[][] blitzBeats = {{1.5, "blitz: in"}, {3.0, "the punch"}, {5.5, "the cut, drawn"}, {7.0, "the cut"}, {10.4, "the kick, chambered"},
            {11.6, "the kick"}, {16.0, "the last, wound"}, {25.5, "held..."}, {27.0, "into the gut"}, {30.0, "through"}};
        for (Object[] beat : blitzBeats) {
            double age = (double)beat[0];
            RigPose pose = warAct(blitz, age, 360.0 + age, true);
            shots.add(new Shot((String)beat[1], pose, NO_WINGS, quarter, quarterLook, warTris(pose, 360.0 + age, 1.0)));
        }
        com.hex.kagune.control.king.KingMove sun = com.hex.kagune.control.king.KingMove.SUNDER;
        Object[][] sunBeats = {{1.5, "Black Sun: in"}, {3.0, "the uppercut"}, {4.6, "crouched"}, {7.0, "sprung up"}, {14.0, "beside it"},
            {32.0, "the fist cocked"}, {34.0, "the punch"}, {38.0, "after"}, {48.0, "falling"}};
        for (Object[] beat : sunBeats) {
            double age = (double)beat[0];
            RigPose pose = warAct(sun, age, 400.0 + age, true);
            shots.add(new Shot((String)beat[1], pose, NO_WINGS, quarter, quarterLook, warTris(pose, 400.0 + age, 1.0)));
        }
        // The spikes themselves, close, low by the ground: shooting up, all out, going back down.
        List<Shot> close = new ArrayList<>();
        double[] low = {-3.0, 1.4, 1.2};
        double[] lowLook = {0.6, 0.9, 5.5};
        for (double age : new double[] {5.0, 7.0, 12.0, 30.0}) {
            RigPose pose = warAct(cone, Math.min(age, cone.ticks() + 1.0), 300.0 + age, false);
            List<WingPreview.Tri> extra = coneTerrain();
            extra.addAll(coneSpikes(age));
            close.add(new Shot(String.format(java.util.Locale.ROOT, "cone spikes close, %.0f t", age), pose, NO_WINGS, low, lowLook, extra));
        }
        for (double since : new double[] {1.0, 6.0}) {
            RigPose pose = warAct(rise, since + com.hex.kagune.control.king.WarRules.RISE_SPIKES, 340.0, false);
            List<WingPreview.Tri> extra = new ArrayList<>(ground());
            extra.addAll(riseSpikes(since));
            extra.addAll(warTris(pose, 340.0, 1.0));
            close.add(new Shot(String.format(java.util.Locale.ROOT, "Execution rings close, %.0f t", since), pose, NO_WINGS, new double[] {-4.5, 2.4, 4.5},
                new double[] {0.0, 0.9, 0.0}, extra));
        }
        sheet(new File(file.getParentFile(), "warpath-spikes.png"), "1.0.27: the ground spikes: thin, straight, sharp", close, 3, 520, 420);
        sheet(file, "1.0.27: the warpath: the RMB cone over a step, a ledge and a drop; King's Execution; Black Sun", shots, 6, 360, 360);
    }

    /** 1.0.28: War posed in a held move (its level, {@code age} ticks in), the rinkaku as the layer has them in it (the cocoon's sphere). */
    static RigPose warHeld(com.hex.kagune.control.king.KingMove move, int level, double age, int from, double time, boolean legs) {
        RigPose p = new RigPose();
        com.hex.kagune.control.client.animation.WarPose.apply(p, time, 7, 0.5F, 0.0F);
        RigPose b = new RigPose();
        if (move == com.hex.kagune.control.king.KingMove.REAPER) {
            com.hex.kagune.control.client.animation.WarPose.reaper(b, level, age, from, 1.0);
        } else if (move == com.hex.kagune.control.king.KingMove.SPIRE_SEND) {
            com.hex.kagune.control.client.animation.WarPose.spireSend(b, age, 26.0, 48.0, 1.0);
        } else {
            com.hex.kagune.control.client.animation.WarPose.act(b, move, age, level, 1.0F);
        }
        p.add(b.legs(legs ? 1.0F : 0.0F, 0.9F), 1.0F);
        p.settle(1.0F);
        return p;
    }

    /** War's whole form on {@code pose} with his rinkaku in {@code motion}. */
    static List<WingPreview.Tri> warMoving(RigPose pose, double time, com.hex.kagune.control.king.WarModel.Motion motion) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
        com.hex.kagune.control.king.WarModel.Part[] parts = com.hex.kagune.control.king.WarModel.Part.values();
        for (int i = 0; i < parts.length; i++) {
            com.hex.kagune.control.king.WarModel.Part part = parts[i];
            String name = names[i];
            tris.addAll(placedLit(sink -> com.hex.kagune.control.king.WarModel.build(sink, part, time, 1.0, 0.0, motion), q -> onPart(pose, name, q)));
        }
        return tris;
    }

    /**
     * 1.0.28: War's held moves: Black Reaper (out of his back at a body, biting, dragging it in, the blade through it, back), King's
     * Defiance (the arm over his face, the cocoon closing, a spike out of it at what struck it, opening) and the Spire Wave (charging
     * round the arm, let go, the mass out along the ground, the crush, back).
     */
    static void warhold(File file) throws Exception {
        List<Shot> shots = new ArrayList<>();
        com.hex.kagune.control.king.KingMove reaper = com.hex.kagune.control.king.KingMove.REAPER;
        double[] side = {-10.5, 4.0, 3.6};
        double[] sideLook = {0.0, 1.5, 3.4};
        double[] forward = {0.0, 0.0, 1.0};
        double[] right = {1.0, 0.0, 0.0};
        // {phase, age, from, where the body stands (blocks ahead), label}
        Object[][] reaperBeats = {{0, 2.0, -1, 7.0, "Reaper: out"}, {0, 5.0, -1, 7.0, "they reach it"}, {1, 3.0, 0, 6.0, "biting, dragging"},
            {1, 12.0, 0, 3.6, "dragged in"}, {2, 3.0, 1, 1.45, "the blade through it"}, {3, 4.0, 2, 2.6, "back into him"}};
        for (Object[] beat : reaperBeats) {
            int phase = (int)beat[0];
            double age = (double)beat[1];
            int from = (int)beat[2];
            double ahead = (double)beat[3];
            double t = 300.0 + age;
            RigPose pose = warHeld(reaper, phase, age, from, t, false);
            double lifted = phase == 2 ? 0.35 : 0.0;
            List<WingPreview.Tri> extra = new ArrayList<>(ground());
            extra.addAll(warTris(pose, t, 1.0));
            extra.addAll(dummy(new double[] {0.0, lifted, ahead}, 0.6, 1.8, 0xFF6C7A86));
            double[][] roots = new double[com.hex.kagune.control.king.WarRules.REAPER_ARMS][];
            for (int k = 0; k < roots.length; k++) {
                roots[k] = onPart(pose, "torso", com.hex.kagune.control.king.WarHoldModel.reaperRoot(k));
            }
            List<com.hex.kagune.control.king.WarHoldModel.Arm> arms = com.hex.kagune.control.king.WarHoldModel.reaper(roots, forward, right,
                new double[] {0.0, lifted + 1.0, ahead}, 0.3, phase, age, t);
            extra.addAll(placedLit(sink -> com.hex.kagune.control.king.WarHoldModel.draw(sink, arms, true), q -> q));
            shots.add(new Shot((String)beat[4], pose, NO_WINGS, side, sideLook, extra));
        }
        com.hex.kagune.control.king.KingMove defy = com.hex.kagune.control.king.KingMove.DEFY;
        double[] quarter = {-4.0, 2.2, 4.5};
        double[] quarterLook = {0.0, 1.0, 0.2};
        Object[][] defyBeats = {{0, 1.5, "Defiance: arm up"}, {0, 4.0, "closing"}, {0, 14.0, "the cocoon"}, {0, 30.0, "struck: a spike out"}, {1, 4.0, "opening"}};
        for (Object[] beat : defyBeats) {
            int level = (int)beat[0];
            double age = (double)beat[1];
            double t = 320.0 + age;
            RigPose pose = warHeld(defy, level, age, -1, t, true);
            double closed = com.hex.kagune.control.king.WarRules.defyClosed(age, level == com.hex.kagune.control.king.WarRules.DEFY_OPENING);
            List<WingPreview.Tri> extra = new ArrayList<>(ground());
            extra.addAll(warMoving(pose, t, com.hex.kagune.control.king.WarModel.Motion.orbed(closed)));
            if (age == 30.0) {
                double[] centre = {0.0, 1.05, 0.0};
                double[] dir = {-0.85, 0.2, -0.3};
                double l = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
                double[] u = {dir[0] / l, dir[1] / l, dir[2] / l};
                double[] from = {centre[0] + u[0] * com.hex.kagune.control.king.WarRules.DEFY_RADIUS, centre[1] + u[1] * com.hex.kagune.control.king.WarRules.DEFY_RADIUS,
                    centre[2] + u[2] * com.hex.kagune.control.king.WarRules.DEFY_RADIUS};
                double reach = com.hex.kagune.control.king.WarRules.DEFY_SPIKE;
                double[] tip = {from[0] + u[0] * reach, from[1] + u[1] * reach, from[2] + u[2] * reach};
                extra.addAll(placedLit(sink -> com.hex.kagune.control.king.WarHoldModel.defySpike(sink, from, tip, 3.0, 9, t), q -> q));
                extra.addAll(dummy(new double[] {u[0] * 3.6, 0.0, u[2] * 3.6}, 0.6, 1.8, 0xFF6C7A86));
            }
            shots.add(new Shot((String)beat[2], pose, NO_WINGS, quarter, quarterLook, extra));
        }
        com.hex.kagune.control.king.KingMove aim = com.hex.kagune.control.king.KingMove.SPIRE_AIM;
        double[] close = {-3.6, 1.9, 1.4};
        double[] closeLook = {-0.2, 1.3, 1.0};
        for (double age : new double[] {4.0, 20.0, 60.0}) {
            double t = 340.0 + age;
            RigPose pose = warHeld(aim, 0, age, -1, t, false);
            List<WingPreview.Tri> extra = new ArrayList<>(warTris(pose, t, 1.0));
            List<com.hex.kagune.control.king.WarHoldModel.Arm> arms = com.hex.kagune.control.king.WarHoldModel.coils(
                onPart(pose, "leftArm", com.hex.kagune.control.king.WarHoldModel.ELBOW), onPart(pose, "leftArm", com.hex.kagune.control.king.WarHoldModel.HAND), age, t);
            extra.addAll(placedLit(sink -> com.hex.kagune.control.king.WarHoldModel.draw(sink, arms, true), q -> q));
            shots.add(new Shot(String.format(java.util.Locale.ROOT, "Spire: charging %.0f%%", 100.0 * com.hex.kagune.control.king.WarRules.spireCharge(age)), pose,
                NO_WINGS, close, closeLook, extra));
        }
        com.hex.kagune.control.king.KingMove send = com.hex.kagune.control.king.KingMove.SPIRE_SEND;
        double charge = 1.0;
        double reach = com.hex.kagune.control.king.WarRules.spireReach(charge);
        double size = com.hex.kagune.control.king.WarRules.spireSize(charge);
        double speed = com.hex.kagune.control.king.WarRules.spireSpeed(charge);
        int armsN = com.hex.kagune.control.king.WarRules.spireArms(charge);
        int arrive = com.hex.kagune.control.king.WarRules.spireArrive(reach, speed);
        double[] high = {-9.0, 7.0, -3.0};
        double[] highLook = {0.0, 0.8, 10.0};
        for (double age : new double[] {3.0, 10.0, 18.0, arrive + 1.0, arrive + 4.0, arrive + 14.0}) {
            double t = 360.0 + age;
            RigPose pose = warHeld(send, 0, age, -1, t, false);
            List<WingPreview.Tri> extra = new ArrayList<>(coneTerrain());
            extra.addAll(warTris(pose, t, 1.0));
            double[] hand = onPart(pose, "leftArm", com.hex.kagune.control.king.WarHoldModel.HAND);
            double[] from = {hand[0], 1.15, 0.7};
            double front = com.hex.kagune.control.king.WarRules.spireFront(age, reach, speed);
            for (int b = 0; b < 3; b++) {
                double at = age >= arrive ? reach : Math.min(front, 6.0 + b * 7.0);
                if (front >= 6.0 + b * 7.0 - 0.5) {
                    extra.addAll(dummy(new double[] {(b - 1) * 0.8, 0.0 + (at > 5.0 ? 1.0 : 0.0) * 0.0, at + 0.3}, 0.6, 1.8, 0xFF6C7A86));
                }
            }
            List<com.hex.kagune.control.king.WarHoldModel.Arm> arms = com.hex.kagune.control.king.WarHoldModel.mass(from, new double[] {0.0, 0.0, 1.0}, hand,
                reach, size, speed, charge, armsN, 5, age, t);
            extra.addAll(placedLit(sink -> com.hex.kagune.control.king.WarHoldModel.draw(sink, arms, true), q -> q));
            shots.add(new Shot(String.format(java.util.Locale.ROOT, "Spire let go, %.0f t%s", age, age >= arrive && age < arrive + 6 ? " (crush)" : ""), pose, NO_WINGS,
                high, highLook, extra));
        }
        sheet(file, "1.0.28: War's held moves: Black Reaper, King's Defiance, the Spire Wave", shots, 6, 360, 360);
    }

    /** 1.0.25: when each move of the {@link #peaceAlive} timeline begins, and which (BLADES level, or -1 for a run and turn). */
    static final int[][] ALIVE_TIMELINE = {{100, -1, 30}, {150, 0, 10}, {160, 1, 11}, {171, 2, 12}, {183, 3, 18}};

    /** The timeline's state at tick {@code t}: at rest, running and turning, or a blow of the M1 chain (handed on from the one before). */
    static com.hex.kagune.control.king.PeaceMotion.State aliveState(long t) {
        com.hex.kagune.control.king.KingMove none = com.hex.kagune.control.king.KingMove.NONE;
        com.hex.kagune.control.king.KingMove blades = com.hex.kagune.control.king.KingMove.BLADES;
        int[] was = null;
        for (int[] m : ALIVE_TIMELINE) {
            if (t >= m[0] && t < m[0] + m[2]) {
                if (m[1] < 0) {
                    return new com.hex.kagune.control.king.PeaceMotion.State(none, 0, 0.0, 0.0, null, none, 0, 0.0, 0.3, 1.0, 0.0);
                }
                boolean handed = was != null && was[1] >= 0 && was[0] + was[2] == m[0];
                return new com.hex.kagune.control.king.PeaceMotion.State(blades, m[1], t - m[0], 0.0, null, handed ? blades : none,
                    handed ? was[1] : 0, handed ? was[2] : 0.0, 0.0, 0.0, 0.0);
            }
            was = m;
        }
        return com.hex.kagune.control.king.PeaceMotion.State.REST;
    }

    /** The timeline's body motion at tick {@code t}: {turn radians, right, up, ahead (blocks)}: turning right and running in its run. */
    static double[] aliveMotion(long t) {
        int[] run = ALIVE_TIMELINE[0];
        return t >= run[0] && t < run[0] + run[2] ? new double[] {0.09, 0.0, 0.0, 0.24} : new double[] {0.0, 0.0, 0.0, 0.0};
    }

    /**
     * 1.0.25: Peace's arms alive ({@code PeaceArms}) along a timeline: at rest, a run turning right, at rest again, then the M1 chain
     * (Lance, Sweep, Scissor, Crucifix): a sheet of moments (two drawn still, as before 1.0.25, beside them) and every tick as its own
     * frame ({@code peace-alive/frame-NNN.png}) to be put together as a film.
     */
    static void peaceAlive(File out) throws Exception {
        com.hex.kagune.control.king.PeaceArms arms = new com.hex.kagune.control.king.PeaceArms(12345L);
        com.hex.kagune.control.king.PeaceArms.Source source = new com.hex.kagune.control.king.PeaceArms.Source() {
            @Override
            public com.hex.kagune.control.king.PeaceMotion.State state(long tick) {
                return aliveState(tick);
            }

            @Override
            public double grow(long tick) {
                return 1.0;
            }
        };
        File frames = new File(out, "peace-alive");
        frames.mkdirs();
        long[] moments = {20, 45, 70, 95, 108, 118, 128, 140, 152, 155, 157, 163, 166, 176, 178, 191};
        java.util.Set<Long> wanted = new java.util.HashSet<>();
        for (long m : moments) {
            wanted.add(m);
        }
        List<Shot> sheet = new ArrayList<>();
        double[] eye = {-8.0, 3.6, -3.2};
        double[] look = {0.0, 1.5, 1.0};
        double[] front = {-7.5, 4.0, 8.5};
        double[] frontLook = {0.0, 1.4, 2.5};
        for (long t = 0; t <= 205; t++) {
            com.hex.kagune.control.king.PeaceMotion.State still = aliveState(t);
            double[][][] lines = arms.lines(t, source, aliveMotion(t));
            com.hex.kagune.control.king.PeaceMotion.State alive = still.withArms(lines);
            RigPose pose = new RigPose();
            com.hex.kagune.control.client.animation.WarPose.peace(pose, (double)t, 1.0F);
            if (still.move() != com.hex.kagune.control.king.KingMove.NONE) {
                com.hex.kagune.control.client.animation.PeacePose.act(pose, still.move(), still.age(), still.level(), 1.0F);
            }
            String what = still.move() == com.hex.kagune.control.king.KingMove.BLADES ? com.hex.kagune.control.king.PeaceStrike.byOrdinal(still.level()).name()
                + " +" + (int)still.age() : aliveMotion(t)[0] != 0.0 ? "running, turning right" : "at rest";
            if (t >= 60) {
                List<Shot> one = new ArrayList<>();
                one.add(new Shot(what, pose, NO_WINGS, t < 150 ? eye : front, t < 150 ? look : frontLook, peaceTris(pose, (double)t, 1.0, alive)));
                sheet(new File(frames, String.format(java.util.Locale.ROOT, "frame-%03d.png", t)), "Peace alive (1.0.25), tick " + t, one, 1, 420, 380);
            }
            if (wanted.contains(t)) {
                boolean fromFront = t >= 150;
                sheet.add(new Shot(t + "t: " + what, pose, NO_WINGS, fromFront ? front : eye, fromFront ? frontLook : look, peaceTris(pose, (double)t, 1.0, alive)));
                if (t == 20 || t == 157) {
                    sheet.add(new Shot(t + "t: " + what + " (still, as before)", pose, NO_WINGS, fromFront ? front : eye, fromFront ? frontLook : look,
                        peaceTris(pose, (double)t, 1.0, still)));
                }
            }
        }
        sheet(new File(out, "king-peace-alive.png"), "1.0.25: Peace's arms alive: at rest, a run turning right, the M1 chain", sheet, 6, 400, 380);
        // The same blow, begun at three moments: each goes its own way (the seeded arcs), the hits where they always were.
        List<Shot> ways = new ArrayList<>();
        long[] starts = {1000L, 2003L, 3011L};
        Object[][] blows = {{0, 4.0, "Lance, on its way"}, {1, 4.0, "Sweep, on its way"}, {2, 4.5, "Scissor, closing"}};
        for (Object[] b : blows) {
            for (long start : starts) {
                double age = (double)b[1];
                double t = start + age;
                com.hex.kagune.control.king.PeaceMotion.State state = new com.hex.kagune.control.king.PeaceMotion.State(
                    com.hex.kagune.control.king.KingMove.BLADES, (int)b[0], age, 0.0, null);
                RigPose pose = new RigPose();
                com.hex.kagune.control.client.animation.WarPose.peace(pose, t, 1.0F);
                com.hex.kagune.control.client.animation.PeacePose.act(pose, com.hex.kagune.control.king.KingMove.BLADES, age, (int)b[0], 1.0F);
                ways.add(new Shot(b[2] + ", begun at " + start, pose, NO_WINGS, new double[] {-0.6, 11.0, 2.2}, new double[] {0.0, 1.0, 2.0},
                    peaceTris(pose, t, 1.0, state)));
            }
        }
        sheet(new File(out, "king-peace-ways.png"), "1.0.25: the same blow never goes the same way twice (from above)", ways, 3, 400, 380);
    }

    /** Peace on a posed body: every part of {@code PeaceModel} on its own body part. */
    static List<WingPreview.Tri> peaceTris(RigPose pose, double time, double grow) {
        return peaceTris(pose, time, grow, com.hex.kagune.control.king.PeaceMotion.State.REST);
    }

    /** 1.0.15: as above, the blades as {@code state} has them. */
    static List<WingPreview.Tri> peaceTris(RigPose pose, double time, double grow, com.hex.kagune.control.king.PeaceMotion.State state) {
        List<WingPreview.Tri> out = new ArrayList<>();
        String[] names = {"head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg"};
        com.hex.kagune.control.king.WarModel.Part[] parts = com.hex.kagune.control.king.WarModel.Part.values();
        for (int i = 0; i < parts.length; i++) {
            com.hex.kagune.control.king.WarModel.Part part = parts[i];
            String name = names[i];
            out.addAll(placedLit(sink -> com.hex.kagune.control.king.PeaceModel.build(sink, part, time, grow, state), q -> onPart(pose, name, q)));
        }
        return out;
    }

    /** Peace beside the reference sheet's views: front, right side, back (near orthographic). */
    static void peaceViews(File file) throws Exception {
        RigPose still = new RigPose();
        double[] look = {0.0, 1.55, 0.0};
        double saved = fov;
        int savedSky = sky;
        fov = 26.0;
        sky = 0xE6E6E6;
        List<Shot> shots = new ArrayList<>();
        shots.add(new Shot("Front", still, NO_WINGS, new double[] {0.0, 1.55, 10.0}, look, peaceTris(still, 0.0, 1.0)));
        shots.add(new Shot("Side (right)", still, NO_WINGS, new double[] {10.0, 1.55, 0.0}, look, peaceTris(still, 0.0, 1.0)));
        shots.add(new Shot("Back", still, NO_WINGS, new double[] {0.0, 1.55, -10.0}, look, peaceTris(still, 0.0, 1.0)));
        sheet(file, "Peace: front, right, back", shots, 3, 640, 560);
        fov = saved;
        sky = savedSky;
    }

    /** Peace from all round and close up, alive (the wings breathe, the blades turn) and coming out. */
    static void peace(File file) throws Exception {
        RigPose still = new RigPose();
        double[] look = {0.0, 1.35, 0.0};
        List<Shot> shots = new ArrayList<>();
        Object[][] views = {
            {"Front, three quarters (right)", new double[] {-5.4, 2.0, 6.6}, look, 1.0},
            {"Front, three quarters (left)", new double[] {5.4, 1.9, 6.6}, look, 1.0},
            {"Back, three quarters", new double[] {-5.2, 2.4, -6.8}, look, 1.0},
            {"The chest", new double[] {0.6, 1.4, 2.4}, new double[] {0.0, 1.25, 0.0}, 1.0},
            {"A cross blade", new double[] {-2.6, 1.6, 3.2}, new double[] {-1.4, 1.5, 0.4}, 1.0},
            {"Coming out (grow 0.5)", new double[] {-5.4, 2.0, 6.6}, look, 0.5},
        };
        for (int i = 0; i < views.length; i++) {
            shots.add(new Shot((String)views[i][0], still, NO_WINGS, (double[])views[i][1], (double[])views[i][2], peaceTris(still, 20.0 + i * 11.0,
                (double)views[i][3])));
        }
        int quads = 0;
        for (com.hex.kagune.control.king.WarModel.Part part : com.hex.kagune.control.king.WarModel.Part.values()) {
            int[] n = {0};
            com.hex.kagune.control.king.PeaceModel.build((a, b, c, d, nn, ca, cb, cc, cd) -> n[0]++, part, 0.0, 1.0);
            quads += n[0];
        }
        System.out.println("peace: " + quads + " quads");
        sheet(file, "Peace, Kaneki :Re (the other final kakuja): after the reference sheet", shots, 3, 600, 520);
    }

    static void sheet(File file, String title, List<Shot> shots, int columns, int pw, int ph) throws Exception {
        int rows = (shots.size() + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(columns * pw, rows * ph + 36, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0x101014));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.setColor(new Color(0xEDE3D6));
        g.drawString(title, 12, 25);
        for (int i = 0; i < shots.size(); i++) {
            int x = (i % columns) * pw, y = (i / columns) * ph + 36;
            g.drawImage(render(shots.get(i), pw, ph), x, y, null);
            g.setColor(new Color(0x101014));
            g.fillRect(x, y + ph - 26, pw, 26);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.setColor(new Color(0xEDE3D6));
            g.drawString(shots.get(i).label(), x + 8, y + ph - 8);
        }
        g.dispose();
        ImageIO.write(sheet, "png", file);
        System.out.println("wrote " + file);
    }
}

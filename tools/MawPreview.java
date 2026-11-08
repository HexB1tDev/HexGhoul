import com.hex.kagune.control.bikaku.BikakuMove;
import com.hex.kagune.control.bikaku.BikakuRules;
import com.hex.kagune.control.bikaku.MawMotion;
import com.hex.kagune.control.bikaku.MawWorm;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.ukaku.UkakuModel;
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
 * 3.4.9: the Bikaku path's worm off-game, exactly as MawMotion poses it and MawWorm builds it, on the player model (PoseRender).
 * <pre>
 * javac -nowarn -cp build/quick/classes -d build/maw-preview tools/PoseRender.java tools/MawPreview.java
 * java -Djava.awt.headless=true -cp build/maw-preview:build/quick/classes MawPreview &lt;out-dir&gt; [look|mouth|bite|colours|bites|firstperson|grow|all]
 * </pre>
 */
public class MawPreview {
    static final int SEED = 7;

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/maw-preview");
        String which = args.length > 1 ? args[1] : "all";
        out.mkdirs();
        if (which.equals("look") || which.equals("all")) {
            look(new File(out, "maw-look.png"));
        }
        if (which.equals("mouth") || which.equals("all")) {
            mouth(new File(out, "maw-mouth.png"));
        }
        if (which.equals("bite") || which.equals("all")) {
            bite(new File(out, "maw-bite.png"));
        }
        if (which.equals("colours") || which.equals("all")) {
            colours(new File(out, "maw-colours.png"));
        }
        if (which.equals("bites") || which.equals("all")) {
            bites(new File(out, "maw-bites.png"));
        }
        if (which.equals("firstperson") || which.equals("all")) {
            firstPerson(new File(out, "maw-firstperson.png"));
        }
        if (which.equals("grabs") || which.equals("all")) {
            grabs(new File(out, "maw-grabs.png"));
        }
        if (which.equals("pursuit") || which.equals("all")) {
            pursuit(new File(out, "maw-pursuit.png"));
        }
        if (which.equals("eruption") || which.equals("all")) {
            eruption(new File(out, "maw-eruption.png"));
        }
        if (which.equals("regen") || which.equals("all")) {
            regen(new File(out, "maw-regen.png"));
        }
        if (which.equals("kakujamoves") || which.equals("all")) {
            kakujaMoves(new File(out, "maw-kakuja-moves.png"));
        }
        if (which.equals("kakuja") || which.equals("all")) {
            kakuja(new File(out, "maw-kakuja.png"));
        }
        if (which.equals("grow") || which.equals("all")) {
            grow(new File(out, "maw-grow.png"));
        }
    }

    // ---------------------------------------------------------------- 3.4.9: as the game draws it

    /** Where the bitten thing stands (body frame: its feet) and its middle, as the server aims at it. */
    static final double[] PREY_FEET = {0.1, 0.0, 3.6};
    static final double[] PREY = {0.1, 0.99, 3.6};

    /** Something to bite: a stand-in figure at {@link #PREY_FEET}, facing him, a zombie's green. */
    static List<PoseRender.Tri> prey() {
        return prey(PREY_FEET);
    }

    /** 3.5.0: the stand-in with its feet at {@code feet} (the body's frame). */
    static List<PoseRender.Tri> prey(double[] feet) {
        List<PoseRender.Tri> out = new ArrayList<>();
        for (PoseRender.Tri t : PoseRender.model(new RigPose())) {
            double[][] v = new double[3][];
            for (int k = 0; k < 3; k++) {
                v[k] = new double[] {-t.v()[k][0] - feet[0], t.v()[k][1] + feet[1], -t.v()[k][2] + feet[2]};
            }
            int rgb = mix(t.rgb(), 0x4E8A3E, 0.55);
            out.add(new PoseRender.Tri(v, rgb, new double[] {-t.n()[0], t.n()[1], -t.n()[2]}));
        }
        return out;
    }

    static int mix(int a, int b, double t) {
        int r = (int)Math.round((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = (int)Math.round((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int bl = (int)Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    /** The M1 string's three bites (BikakuRules), frame by frame, at something 3.6 blocks ahead; the first again from his right. */
    static void bites(File file) throws Exception {
        int fw = 250;
        int fh = 300;
        BikakuRules.Bite[] bites = {BikakuRules.Bite.SNAP, BikakuRules.Bite.LUNGE, BikakuRules.Bite.MAUL, BikakuRules.Bite.SNAP};
        int cols = 9;
        BufferedImage img = new BufferedImage(fw * cols, fh * bites.length + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.4.9: the M1 string (Snap, Lunge from above, Maul hooked from the side and shaken), then Snap from his right side");
        for (int r = 0; r < bites.length; r++) {
            BikakuRules.Bite bite = bites[r];
            boolean side = r == 3;
            for (int i = 0; i < cols; i++) {
                double age = bite.ticks() * i / (double)(cols - 1);
                if (i == 4) {
                    age = bite.land(); // the frame it shuts
                }
                double time = 30 + age;
                MawMotion.Shape s = MawMotion.of(time, SEED, bite, age, PREY);
                List<PoseRender.Tri> tris = worm(s, 1.0, time, 0xA50F37, true);
                tris.addAll(prey());
                PoseRender.Camera cam = side ? framed(-90, 6, 12.5) : framed(42, 14, 12.5);
                g.drawImage(PoseRender.render(new RigPose(), cam, fw, fh, tris), i * fw, 34 + r * fh, null);
                label(g, String.format("%s %.1ft  mouth %.2f%s", bite.name().charAt(0) + bite.name().substring(1).toLowerCase(), age, s.aperture(),
                    i == 4 ? "  SHUT" : ""), i * fw, 34 + r * fh);
            }
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }

    /** A camera orbiting the middle between him and what he bites (yaw 0 at his face, 90 at his left), so both are in the frame. */
    static PoseRender.Camera framed(double yawDegrees, double pitchDegrees, double distance) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double[] target = {-PREY[0] * 0.5, 1.5, PREY[2] * 0.45};
        double[] eye = {target[0] + Math.sin(yaw) * Math.cos(pitch) * distance, target[1] + Math.sin(pitch) * distance,
            target[2] + Math.cos(yaw) * Math.cos(pitch) * distance};
        return new PoseRender.Camera(eye, target, 34);
    }

    /** 3.5.0: Devouring Maw (top two rows) and Predatory Drag (bottom two), frame by frame, the catch held where the server holds it. */
    static void grabs(File file) throws Exception {
        int fw = 250;
        int fh = 300;
        int cols = 10;
        BufferedImage img = new BufferedImage(fw * cols, fh * 4 + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.5.0: Devouring Maw (G: thrown wide, clamped, pulled up, shaken, flung right) and Predatory Drag (J: along the ground, dragged in, lifted, slammed)");
        double[] mawAges = {0, 4, 7, 9, 12, 16, 21, 27, 30, 38};
        double[] dragAges = {0, 4, 8, 11, 16, 22, 28, 31, 34, 42};
        double[] far = {0.2, 0.99, 6.5};
        for (int r = 0; r < 4; r++) {
            boolean maw = r < 2;
            boolean side = r % 2 == 1;
            double[] target = maw ? PREY : far;
            for (int i = 0; i < cols; i++) {
                double age = maw ? mawAges[i] : dragAges[i];
                double time = 30 + age;
                BikakuMove move = maw ? BikakuMove.MAW : BikakuMove.DRAG;
                double[] held = age < move.land() ? target : maw ? MawMotion.mawHeld(BikakuRules.reach(target), age)
                    : MawMotion.dragHeld(MawMotion.dragReach(target), Math.min(age, BikakuMove.DRAG_SLAM));
                MawMotion.Shape s = maw ? MawMotion.maw(time, SEED, target, age >= move.land() ? held : null, age, true)
                    : MawMotion.drag(time, SEED, target, age >= move.land() ? held : null, age, true);
                List<PoseRender.Tri> tris = worm(s, 1.0, time, 0xA50F37, true);
                boolean let = maw ? age > BikakuMove.MAW_RELEASE : age > BikakuMove.DRAG_SLAM;
                if (!let) {
                    tris.addAll(prey(new double[] {held[0], held[1] - 0.99, held[2]}));
                }
                PoseRender.Camera cam = side ? framedAt(-90, 6, 14.5, maw ? 0.45 : 0.6) : framedAt(40, 14, 14.5, maw ? 0.45 : 0.6);
                g.drawImage(PoseRender.render(new RigPose(), cam, fw, fh, tris), i * fw, 34 + r * fh, null);
                label(g, String.format("%s %.0ft  mouth %.2f%s", maw ? "Maw" : "Drag", age, s.aperture(), age == move.land() ? "  BITES"
                    : !maw && age == BikakuMove.DRAG_SLAM ? "  SLAM" : maw && age == BikakuMove.MAW_RELEASE ? "  FLUNG" : ""), i * fw, 34 + r * fh);
            }
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }

    /** The worms of a move drawn at once (each its own thickness, growth and seed), as the renderer draws them. */
    static List<PoseRender.Tri> worms(MawMotion.Worm[] worms, double time) {
        List<PoseRender.Tri> tris = new ArrayList<>();
        for (MawMotion.Worm worm : worms) {
            if (worm.grown() <= 0.002) {
                continue;
            }
            double[][] path = MawMotion.grown(MawMotion.around(worm.shape().path(), worm.girth(), worm.shape().aperture()), worm.grown());
            UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
                int rgb = avg(ca, cb, cc, cd);
                tris.add(new PoseRender.Tri(new double[][] {w(a), w(b), w(c)}, rgb, w(n)));
                tris.add(new PoseRender.Tri(new double[][] {w(a), w(c), w(d)}, rgb, w(n)));
            };
            MawWorm.build(sink, new MawWorm.Pose(path, worm.shape().dorsal(), worm.grown(), worm.shape().aperture(), worm.girth(), worm.seed(), time),
                0xA50F37, true);
        }
        return tris;
    }

    /** 3.5.1: Ravenous Pursuit at three bodies (top two rows, two views) and all three worms on one (bottom). */
    static void pursuit(File file) throws Exception {
        int fw = 250;
        int fh = 300;
        double[] ages = {0, 4, 8, 12, 15, 17, 22, 26, 33, 40};
        double[][] three = {{2.6, 0.99, 3.2}, {-2.3, 0.99, 3.0}, {0.2, 0.99, 4.3}};
        double[][] one = {PREY, PREY, PREY};
        BufferedImage img = new BufferedImage(fw * ages.length, fh * 4 + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.5.1: Ravenous Pursuit (Z): three thinner worms from one root, each in its own lane well apart, lunging in snakes at its own body (bites at 12, 17, 22)");
        for (int r = 0; r < 4; r++) {
            double[][] targets = r < 2 || r == 3 ? three : one;
            for (int i = 0; i < ages.length; i++) {
                double time = 30 + ages[i];
                List<PoseRender.Tri> tris = worms(MawMotion.pursuit(time, SEED, targets, ages[i]), time);
                java.util.Set<String> placed = new java.util.HashSet<>();
                for (double[] t : targets) {
                    if (placed.add(t[0] + "," + t[2])) {
                        tris.addAll(prey(new double[] {t[0], 0.0, t[2]}));
                    }
                }
                PoseRender.Camera cam = r == 1 ? framedAt(170, 22, 15.5, 0.4) : r == 3 ? framedAt(180, 4, 13.0, 0.1) : framedAt(30, 18, 15.5, 0.45);
                g.drawImage(PoseRender.render(new RigPose(), cam, fw, fh, tris), i * fw, 34 + r * fh, null);
                label(g, String.format("%s %.0ft%s", r == 2 ? "one body" : r == 1 ? "from behind" : r == 3 ? "straight behind" : "three bodies", ages[i],
                    ages[i] == 12 || ages[i] == 17 || ages[i] == 22 ? "  BITES" : ""), i * fw, 34 + r * fh);
            }
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }

    /** 3.5.1: Carrion Eruption: into the ground, under it (the ground heaving: brown marks), up under its mark, the mark thrown up. */
    static void eruption(File file) throws Exception {
        int fw = 250;
        int fh = 320;
        double[] ages = {0, 4, 7, 11, 16, 20, 22, 24, 28, 36, 44, 50};
        double[] mark = {0.4, 0.99, 5.5};
        BufferedImage img = new BufferedImage(fw * ages.length, fh * 2 + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.5.1: Carrion Eruption (B): into the ground before him, tunnelling under it (brown: where the ground heaves), bursting up under its mark, the mark thrown up, back the same way");
        for (int r = 0; r < 2; r++) {
            for (int i = 0; i < ages.length; i++) {
                double age = ages[i];
                double time = 30 + age;
                MawMotion.Shape s = MawMotion.eruption(time, SEED, mark, age);
                List<PoseRender.Tri> tris = worm(s, 1.0, time, 0xA50F37, true);
                double up = 0.0;
                int land = BikakuMove.ERUPTION.land();
                // Thrown up as Minecraft moves a body: its speed, then 0.08 a tick of gravity, 2% drag.
                double v = BikakuRules.ERUPTION_LAUNCH;
                for (int t = land; t < age; t++) {
                    up = Math.max(0.0, up + v);
                    v = (v - 0.08) * 0.98;
                }
                tris.addAll(prey(new double[] {mark[0], up, mark[2]}));
                double[] bulge = MawMotion.eruptBulge(mark, age);
                if (bulge != null) {
                    tris.addAll(mound(bulge));
                }
                PoseRender.Camera cam = r == 1 ? framedAt(-90, 8, 20.0, 0.75) : framedAt(35, 24, 18.0, 0.75);
                g.drawImage(big(cam, fw, fh, tris), i * fw, 34 + r * fh, null);
                label(g, String.format("%.0ft  mouth %.2f%s", age, s.aperture(), age == land ? "  SHUT" : age == BikakuMove.ERUPT_RISE ? "  BURSTS" : ""),
                    i * fw, 34 + r * fh);
            }
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }

    /** As PoseRender.render on a wider floor (what is under it hidden, as the ground hides it in game). */
    static BufferedImage big(PoseRender.Camera cam, int w, int h, List<PoseRender.Tri> extra) {
        PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, cam, 0x3A4450);
        canvas.floor(16, 0x58626C, 0x4E5862);
        for (PoseRender.Tri t : PoseRender.model(new RigPose())) {
            canvas.tri(t, true);
        }
        for (PoseRender.Tri t : extra) {
            canvas.tri(t, true);
        }
        return canvas.image();
    }

    /** A low brown heap on the ground at {@code at} (the body's frame): where the ground heaves over the worm (a stand-in for the dust). */
    static List<PoseRender.Tri> mound(double[] at) {
        List<PoseRender.Tri> tris = new ArrayList<>();
        double[] top = w(new double[] {at[0], at[1] + 0.35, at[2]});
        for (int k = 0; k < 6; k++) {
            double a0 = Math.PI * 2.0 * k / 6.0;
            double a1 = Math.PI * 2.0 * (k + 1) / 6.0;
            double[] p0 = w(new double[] {at[0] + 0.6 * Math.cos(a0), at[1] + 0.02, at[2] + 0.6 * Math.sin(a0)});
            double[] p1 = w(new double[] {at[0] + 0.6 * Math.cos(a1), at[1] + 0.02, at[2] + 0.6 * Math.sin(a1)});
            tris.add(new PoseRender.Tri(new double[][] {p0, p1, top}, 0x6E5A44, new double[] {0, 1, 0}));
        }
        return tris;
    }

    /** 3.5.3: Monstrous Regeneration's strands crawling over him (the worm at rest), at times through it, from the front and behind. */
    static void regen(File file) throws Exception {
        int fw = 260;
        int fh = 320;
        double[] ages = {2, 8, 20, 40, 70, 100, 140, 152, 158};
        BufferedImage img = new BufferedImage(fw * ages.length, fh * 2 + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.5.3: Monstrous Regeneration (C): strands of the kagune crawling over his body and knitting it (8 s), then withering (bottom: from behind, the worm left out)");
        for (int r = 0; r < 2; r++) {
            for (int i = 0; i < ages.length; i++) {
                double age = ages[i];
                double time = 30 + age;
                List<PoseRender.Tri> tris = r == 0 ? worm(MawMotion.idle(time, SEED), 1.0, time, 0xA50F37, false) : new ArrayList<>();
                tris.addAll(tissue(time, age));
                PoseRender.Camera cam = r == 0 ? PoseRender.orbit(25, 8, 4.6, 1.05) : PoseRender.orbit(200, 8, 4.6, 1.05);
                g.drawImage(PoseRender.render(new RigPose(), cam, fw, fh, tris), i * fw, 34 + r * fh, null);
                label(g, String.format("%.0ft (%.1f s)  shows %.2f", age, age / 20.0, com.hex.kagune.control.bikaku.MawTissue.shown(age)), i * fw, 34 + r * fh);
            }
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }

    /** The strands, placed on the body at rest as the renderer places them (BodyFrame), as PoseRender triangles. */
    static List<PoseRender.Tri> tissue(double time, double age) {
        RigPose rest = new RigPose();
        List<double[][]> placed = new ArrayList<>();
        List<Double> radii = new ArrayList<>();
        for (com.hex.kagune.control.bikaku.MawTissue.Strand strand : com.hex.kagune.control.bikaku.MawTissue.strands(time, age, SEED)) {
            double[][] points = new double[strand.local().length][];
            for (int i = 0; i < points.length; i++) {
                points[i] = com.hex.kagune.control.client.animation.BodyFrame.world(rest,
                    com.hex.kagune.control.client.animation.BodyFrame.model(rest, strand.part(), strand.local()[i]));
            }
            placed.add(points);
            radii.add(strand.radius());
        }
        List<PoseRender.Tri> tris = new ArrayList<>();
        com.hex.kagune.control.bikaku.MawTissue.build((a, b, c, d, n, ca, cb, cc, cd) -> {
            int rgb = avg(ca, cb, cc, cd);
            tris.add(new PoseRender.Tri(new double[][] {w(a), w(b), w(c)}, rgb, w(n)));
            tris.add(new PoseRender.Tri(new double[][] {w(a), w(c), w(d)}, rgb, w(n)));
        }, placed, radii, 0xA50F37);
        return tris;
    }

    /** 3.5.4: the kakuja, grown (from round about), growing into it, and coming out of it. */
    static void kakuja(File file) throws Exception {
        int fw = 330;
        int fh = 330;
        int cols = 6;
        BufferedImage img = new BufferedImage(fw * cols, fh * 4 + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.5.4: the kakuja: a lopsided heaving mass round him (about 4.8 high) covered in grinning mouths and a great maw, six tentacles; growing into it (3 s), out (1.5 s); M1: a tentacle's bite");
        double[][] views = {{20, 12}, {70, 18}, {140, 10}, {200, 15}, {-70, 8}, {-20, 35}};
        for (int i = 0; i < cols; i++) {
            double time = 40 + i * 9;
            List<PoseRender.Tri> tris = kakujaTris(com.hex.kagune.control.bikaku.MawKakuja.growth(true, 200), time, null);
            g.drawImage(bigAt(views[i][0], views[i][1], 34.0, 3.6, fw, fh, tris), i * fw, 34, null);
            label(g, String.format("grown, from %.0f°", views[i][0]), i * fw, 34);
        }
        double[] ins = {6, 16, 26, 36, 46, 60};
        for (int i = 0; i < cols; i++) {
            double time = 40 + ins[i];
            com.hex.kagune.control.bikaku.MawKakuja.Growth gr = com.hex.kagune.control.bikaku.MawKakuja.growth(true, ins[i]);
            g.drawImage(bigAt(30, 12, 30.0, 3.2, fw, fh, kakujaTris(gr, time, gr)), i * fw, 34 + fh, null);
            label(g, String.format("growing into it %.0ft", ins[i]), i * fw, 34 + fh);
        }
        double[] outs = {0, 5, 10, 16, 22, 30};
        for (int i = 0; i < cols; i++) {
            double time = 140 + outs[i];
            com.hex.kagune.control.bikaku.MawKakuja.Growth gr = com.hex.kagune.control.bikaku.MawKakuja.growth(false, outs[i]);
            g.drawImage(bigAt(30, 12, 30.0, 3.2, fw, fh, kakujaTris(gr, time, gr)), i * fw, 34 + 2 * fh, null);
            label(g, String.format("coming out %.0ft", outs[i]), i * fw, 34 + 2 * fh);
        }
        double[] bites = {0, 3, 6, 9, 13, 18};
        double[] prey = {6.5, 0.99, 7.5};
        for (int i = 0; i < cols; i++) {
            double age = bites[i];
            double time = 240 + age;
            List<PoseRender.Tri> tris = new ArrayList<>();
            UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
                int rgb = avg(ca, cb, cc, cd);
                tris.add(new PoseRender.Tri(new double[][] {w(a), w(b), w(c)}, rgb, w(n)));
                tris.add(new PoseRender.Tri(new double[][] {w(a), w(c), w(d)}, rgb, w(n)));
            };
            int biter = com.hex.kagune.control.bikaku.MawKakuja.biter(prey, time);
            com.hex.kagune.control.bikaku.MawKakuja.build(sink, com.hex.kagune.control.bikaku.MawKakuja.growth(true, 200), time, SEED, 0xA50F37, true, biter,
                prey, com.hex.kagune.control.bikaku.MawKakuja.lunge(age), com.hex.kagune.control.bikaku.MawKakuja.lungeMouth(age));
            tris.addAll(prey(new double[] {prey[0], 0.0, prey[2]}));
            g.drawImage(bigAt(-10, 14, 30.0, 3.2, fw, fh, tris), i * fw, 34 + 3 * fh, null);
            label(g, String.format("tentacle bite %.0ft%s", age, age == com.hex.kagune.control.bikaku.BikakuRules.Bite.TENTACLE.land() ? "  BITES" : ""),
                i * fw, 34 + 3 * fh);
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }

    /** 3.5.7: the kakuja's own moves (Thousand Jaws, Endless Hunger, Flesh Cataclysm, Undying Abomination) and its bites on its own. */
    static void kakujaMoves(File file) throws Exception {
        int fw = 300;
        int fh = 300;
        int cols = 7;
        BufferedImage img = new BufferedImage(fw * cols, fh * 5 + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.5.7: the kakuja's moves: Thousand Jaws (Z), Endless Hunger (G), Flesh Cataclysm (B), Undying Abomination (C), and its tentacles biting on their own");
        com.hex.kagune.control.bikaku.MawKakuja.Growth grown = com.hex.kagune.control.bikaku.MawKakuja.growth(true, 200);
        com.hex.kagune.control.bikaku.BikakuMove jaws = com.hex.kagune.control.bikaku.BikakuMove.JAWS;
        // Thousand Jaws: five bodies round it, one close.
        double[][] round = {{7.0, 0.99, 6.0}, {-7.5, 0.99, 4.0}, {2.0, 0.99, -8.0}, {-5.0, 0.99, -6.5}, {1.0, 0.99, 3.2}};
        double[][] perTentacle = new double[6][];
        int[] who = {0, 1, 2, -1, 4, 3};
        for (int i = 0; i < 6; i++) {
            perTentacle[i] = who[i] < 0 ? null : round[who[i]];
        }
        double[] jawsAges = {0, 6, 10, 14, 24, 30, 54};
        for (int i = 0; i < cols; i++) {
            double age = jawsAges[i];
            double time = 300 + age;
            com.hex.kagune.control.bikaku.MawKakuja.Act act = com.hex.kagune.control.bikaku.KakujaActs.act(jaws, age, time, perTentacle, null, null, true);
            List<PoseRender.Tri> tris = actTris(grown, time, act);
            for (double[] body : round) {
                tris.addAll(prey(new double[] {body[0], 0.0, body[2]}));
            }
            g.drawImage(bigAt(25, 38, 40.0, 2.0, fw, fh, tris), i * fw, 34, null);
            label(g, String.format("Thousand Jaws %.0ft%s", age, snaps(age) ? "  GRINS SNAP" : ""), i * fw, 34);
        }
        // Endless Hunger on one body before it.
        double[] caught = {2.5, 0.99, 7.0};
        com.hex.kagune.control.bikaku.BikakuMove hunger = com.hex.kagune.control.bikaku.BikakuMove.HUNGER;
        double[] hungerAges = {0, 6, 12, 22, 40, 46, 56};
        for (int i = 0; i < cols; i++) {
            double age = hungerAges[i];
            double time = 400 + age;
            double[] held = com.hex.kagune.control.bikaku.KakujaActs.hungerHeld(caught, age);
            com.hex.kagune.control.bikaku.MawKakuja.Act act = com.hex.kagune.control.bikaku.KakujaActs.act(hunger, age, time, null, held, caught, true);
            List<PoseRender.Tri> tris = actTris(grown, time, act);
            if (age < com.hex.kagune.control.bikaku.BikakuMove.HUNGER_RELEASE) {
                tris.addAll(prey(new double[] {held[0], held[1] - 0.99, held[2]}));
            }
            g.drawImage(bigAt(-70, 32, 26.0, 2.0, fw, fh, tris), i * fw, 34 + fh, null);
            label(g, String.format("Endless Hunger %.0ft%s", age, age == hunger.land() ? "  CAUGHT" : age == com.hex.kagune.control.bikaku.BikakuMove.HUNGER_CRUSH
                ? "  THE MAW SHUTS" : bitesAt(age) ? "  BITES" : ""), i * fw, 34 + fh);
        }
        // Flesh Cataclysm.
        com.hex.kagune.control.bikaku.BikakuMove cata = com.hex.kagune.control.bikaku.BikakuMove.CATACLYSM;
        double[] cataAges = {0, 8, 13, 16, 22, 30, 42};
        for (int i = 0; i < cols; i++) {
            double age = cataAges[i];
            double time = 500 + age;
            com.hex.kagune.control.bikaku.MawKakuja.Act act = com.hex.kagune.control.bikaku.KakujaActs.act(cata, age, time, null, null, null, true);
            List<PoseRender.Tri> tris = actTris(grown, time, act);
            for (double[] body : new double[][] {{3.5, 0.99, 3.5}, {-9.0, 0.99, 2.0}, {6.0, 0.99, -7.0}}) {
                tris.addAll(prey(new double[] {body[0], 0.0, body[2]}));
            }
            g.drawImage(bigAt(25, 42, 42.0, 2.0, fw, fh, tris), i * fw, 34 + 2 * fh, null);
            label(g, String.format("Flesh Cataclysm %.0ft%s", age, age == cata.land() ? "  BURSTS, ROARS" : age < com.hex.kagune.control.bikaku.BikakuMove.CATA_DRAWN
                && age > 0 ? "  DRAWING IN" : age > cata.land() && age < com.hex.kagune.control.bikaku.BikakuMove.CATA_SWEPT ? "  SWEEPING" : ""), i * fw, 34 + 2 * fh);
        }
        // Undying Abomination: up close.
        com.hex.kagune.control.bikaku.BikakuMove undying = com.hex.kagune.control.bikaku.BikakuMove.UNDYING;
        double[] mendAges = {0, 6, 30, 70, 120, 175, 196};
        for (int i = 0; i < cols; i++) {
            double age = mendAges[i];
            double time = 600 + age;
            com.hex.kagune.control.bikaku.MawKakuja.Act act = com.hex.kagune.control.bikaku.KakujaActs.act(undying, age, time, null, null, null, true);
            g.drawImage(bigAt(30, 16, 24.0, 2.6, fw, fh, actTris(grown, time, act)), i * fw, 34 + 3 * fh, null);
            label(g, String.format("Undying Abomination %.0ft  mending %.0f%%", age, act.mend() * 100), i * fw, 34 + 3 * fh);
        }
        // Autonomous Predation: two bodies near it, two tentacles going for them on their own, at their own times.
        double[] near1 = {5.5, 0.99, 5.0};
        double[] near2 = {-6.0, 0.99, -3.0};
        double[] autoAges = {0, 4, 9, 12, 16, 20, 26};
        for (int i = 0; i < cols; i++) {
            double t = autoAges[i];
            double time = 700 + t;
            com.hex.kagune.control.bikaku.MawKakuja.Act act = com.hex.kagune.control.bikaku.MawKakuja.Act.still();
            act = com.hex.kagune.control.bikaku.KakujaActs.withAuto(act, 4, near1, t);
            act = com.hex.kagune.control.bikaku.KakujaActs.withAuto(act, 1, near2, t - 8);
            List<PoseRender.Tri> tris = actTris(grown, time, act);
            tris.addAll(prey(new double[] {near1[0], 0.0, near1[2]}));
            tris.addAll(prey(new double[] {near2[0], 0.0, near2[2]}));
            g.drawImage(bigAt(10, 30, 38.0, 2.0, fw, fh, tris), i * fw, 34 + 4 * fh, null);
            label(g, String.format("on its own %.0ft%s", t, t == 9 || t == 17 ? "  BITES" : ""), i * fw, 34 + 4 * fh);
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }

    private static boolean snaps(double age) {
        for (int snap : com.hex.kagune.control.bikaku.BikakuMove.JAWS_SNAPS) {
            if (age == snap) {
                return true;
            }
        }
        return false;
    }

    private static boolean bitesAt(double age) {
        for (int bite : com.hex.kagune.control.bikaku.BikakuMove.HUNGER_BITES) {
            if (age == bite) {
                return true;
            }
        }
        return false;
    }

    /** The kakuja grown, doing {@code act}. */
    static List<PoseRender.Tri> actTris(com.hex.kagune.control.bikaku.MawKakuja.Growth growth, double time, com.hex.kagune.control.bikaku.MawKakuja.Act act) {
        List<PoseRender.Tri> tris = new ArrayList<>();
        UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
            int rgb = avg(ca, cb, cc, cd);
            tris.add(new PoseRender.Tri(new double[][] {w(a), w(b), w(c)}, rgb, w(n)));
            tris.add(new PoseRender.Tri(new double[][] {w(a), w(c), w(d)}, rgb, w(n)));
        };
        com.hex.kagune.control.bikaku.MawKakuja.build(sink, growth, time, SEED, 0xA50F37, true, act);
        return tris;
    }

    /** The kakuja as grown, and (through a transformation) the worm swollen and showing as much as it still does. */
    static List<PoseRender.Tri> kakujaTris(com.hex.kagune.control.bikaku.MawKakuja.Growth growth, double time, com.hex.kagune.control.bikaku.MawKakuja.Growth worm) {
        List<PoseRender.Tri> tris = new ArrayList<>();
        UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
            int rgb = avg(ca, cb, cc, cd);
            tris.add(new PoseRender.Tri(new double[][] {w(a), w(b), w(c)}, rgb, w(n)));
            tris.add(new PoseRender.Tri(new double[][] {w(a), w(c), w(d)}, rgb, w(n)));
        };
        com.hex.kagune.control.bikaku.MawKakuja.build(sink, growth, time, SEED, 0xA50F37, true, -1, null, 0.0, 0.0);
        if (worm != null && worm.worm() > 0.002) {
            MawMotion.Shape s = MawMotion.idle(time * (1.0 + worm.swell()), SEED);
            MawWorm.build(sink, new MawWorm.Pose(MawMotion.grown(MawMotion.around(s.path(), 1.0, s.aperture()), worm.worm()), s.dorsal(), worm.worm(), s.aperture(), 1.0 + worm.swell(), SEED,
                time), 0xA50F37, true);
        }
        return tris;
    }

    /** A camera orbiting him at {@code yaw}/{@code pitch}, {@code distance} out, looking at {@code y}; the wide floor. */
    static BufferedImage bigAt(double yawDegrees, double pitchDegrees, double distance, double y, int w, int h, List<PoseRender.Tri> tris) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double[] target = {0.0, y, 0.0};
        double[] eye = {Math.sin(yaw) * Math.cos(pitch) * distance, y + Math.sin(pitch) * distance, Math.cos(yaw) * Math.cos(pitch) * distance};
        return big(new PoseRender.Camera(eye, target, 40), w, h, tris);
    }

    /** As {@link #framed}, looking at {@code share} of the way out to what he bites. */
    static PoseRender.Camera framedAt(double yawDegrees, double pitchDegrees, double distance, double share) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double[] target = {-0.3, 1.6, 4.0 * share};
        double[] eye = {target[0] + Math.sin(yaw) * Math.cos(pitch) * distance, target[1] + Math.sin(pitch) * distance,
            target[2] + Math.cos(yaw) * Math.cos(pitch) * distance};
        return new PoseRender.Camera(eye, target, 34);
    }

    /** As the renderer does in first person: the line kept 1.25 from the eyes, no quad with a corner within 0.6. */
    static List<PoseRender.Tri> firstPersonWorm(MawMotion.Shape shape, double time, int tint) {
        double[] eye = {0.0, 1.62, 0.0};
        double[][] path = MawMotion.firstPerson(MawMotion.around(shape.path(), 1.0, shape.aperture()), eye);
        List<PoseRender.Tri> tris = new ArrayList<>();
        UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
            for (double[] q : new double[][] {a, b, c, d}) {
                double dx = q[0] - eye[0];
                double dy = q[1] - eye[1];
                double dz = q[2] - eye[2];
                if (dx * dx + dy * dy + dz * dz < 0.36) {
                    return;
                }
            }
            int rgb = avg(ca, cb, cc, cd);
            tris.add(new PoseRender.Tri(new double[][] {w(a), w(b), w(c)}, rgb, w(n)));
            tris.add(new PoseRender.Tri(new double[][] {w(a), w(c), w(d)}, rgb, w(n)));
        };
        MawWorm.build(sink, new MawWorm.Pose(path, shape.dorsal(), 1.0, shape.aperture(), 1.0, SEED, time), tint, true);
        return tris;
    }

    /** What he sees in first person (Minecraft's 70 degree view): at rest looking up and to the right, and a bite on what is ahead. */
    static void firstPerson(File file) throws Exception {
        int fw = 400;
        int fh = 240;
        double[] eye = {0.0, 1.62, 0.0};
        BufferedImage img = new BufferedImage(fw * 3, fh * 2 + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.4.9, first person: at rest it is above you (look up and right); a bite comes over your right shoulder");
        String[] names = {"rest, looking ahead", "rest, looking up-right", "rest, looking up", "Snap 3t (thrown)", "Snap 5t", "Snap 6t (shut on it)"};
        for (int i = 0; i < 6; i++) {
            double time = i < 3 ? 14 : 30 + (i == 3 ? 3 : i == 4 ? 5 : 6);
            MawMotion.Shape s = i < 3 ? MawMotion.idle(14, SEED) : MawMotion.of(time, SEED, BikakuRules.Bite.SNAP, i == 3 ? 3 : i == 4 ? 5 : 6, PREY);
            double[] look = i == 0 ? new double[] {0, 1.62, 1} : i == 1 ? new double[] {-0.8, 2.6, 0.9} : i == 2 ? new double[] {-0.15, 3.0, 0.5}
                : new double[] {0, 1.45, 1};
            PoseRender.Canvas canvas = new PoseRender.Canvas(fw, fh, 2, new PoseRender.Camera(eye, look, 70), 0x3A4450);
            canvas.floor(8, 0x58626C, 0x4E5862);
            for (PoseRender.Tri t : prey()) {
                canvas.tri(t, true);
            }
            for (PoseRender.Tri t : firstPersonWorm(s, time, 0xA50F37)) {
                canvas.tri(t, true);
            }
            g.drawImage(canvas.image(), (i % 3) * fw, 34 + (i / 3) * fh, null);
            label(g, names[i], (i % 3) * fw, 34 + (i / 3) * fh);
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }

    /** Coming out (Alt + R): drawn out of his back mouth first along its own line, as the renderer cuts it (MawMotion.grown). */
    static void grow(File file) throws Exception {
        int fw = 260;
        int fh = 320;
        double[] deploys = {0.08, 0.2, 0.35, 0.5, 0.7, 0.85, 1.0};
        BufferedImage img = new BufferedImage(fw * deploys.length, fh + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.4.9: coming out of his back (how far the kagune is out)");
        for (int i = 0; i < deploys.length; i++) {
            double grown = deploys[i] * deploys[i] * (3 - 2 * deploys[i]);
            MawMotion.Shape rest = MawMotion.idle(14, SEED);
            MawMotion.Shape s = new MawMotion.Shape(MawMotion.grown(rest.path(), grown), rest.dorsal(), rest.aperture());
            g.drawImage(PoseRender.render(new RigPose(), PoseRender.orbit(150, 14, 13.5, 2.1), fw, fh, worm(s, grown, 14, 0xA50F37, true)), i * fw, 34, null);
            label(g, String.format("out %.0f%%", deploys[i] * 100), i * fw, 34);
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }

    /** The worm's quads as PoseRender triangles (the body frame's x is to the right; PoseRender's to the left). */
    static List<PoseRender.Tri> worm(MawMotion.Shape shape, double grown, double time, int tint, boolean fine) {
        List<PoseRender.Tri> tris = new ArrayList<>();
        // 3.5.5: drawn along the curve the game draws it along (round his body).
        double[][] path = MawMotion.around(shape.path(), 1.0, shape.aperture());
        UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
            double[] pa = w(a);
            double[] pb = w(b);
            double[] pc = w(c);
            double[] pd = w(d);
            double[] nn = w(n);
            int rgb = avg(ca, cb, cc, cd);
            tris.add(new PoseRender.Tri(new double[][] {pa, pb, pc}, rgb, nn));
            tris.add(new PoseRender.Tri(new double[][] {pa, pc, pd}, rgb, nn));
        };
        MawWorm.build(sink, new MawWorm.Pose(path, shape.dorsal(), grown, shape.aperture(), 1.0, SEED, time), tint, fine);
        return tris;
    }

    static double[] w(double[] v) {
        return new double[] {-v[0], v[1], v[2]};
    }

    static int avg(int a, int b, int c, int d) {
        int r = ((a >> 16 & 255) + (b >> 16 & 255) + (c >> 16 & 255) + (d >> 16 & 255)) / 4;
        int g = ((a >> 8 & 255) + (b >> 8 & 255) + (c >> 8 & 255) + (d >> 8 & 255)) / 4;
        int bl = ((a & 255) + (b & 255) + (c & 255) + (d & 255)) / 4;
        return r << 16 | g << 8 | bl;
    }

    static BufferedImage frame(MawMotion.Shape shape, double time, int tint, PoseRender.Camera cam, int fw, int fh) {
        return PoseRender.render(new RigPose(), cam, fw, fh, worm(shape, 1.0, time, tint, true));
    }

    static Graphics2D sheet(BufferedImage img, String title) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(26, 26, 32));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString(title, 10, 24);
        return g;
    }

    static void label(Graphics2D g, String text, int x, int y) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.setColor(new Color(225, 225, 225));
        g.drawString(text, x + 6, y + 15);
    }

    /** At rest over a few seconds, from the front quarter; then from behind, the side and above. */
    static void look(File file) throws Exception {
        int fw = 300;
        int fh = 360;
        BufferedImage img = new BufferedImage(fw * 5, fh * 2 + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.4.9: at rest, about 8 blocks of zigzag coils (it slithers, scans in snaps, now and then gapes)");
        double[] times = {0, 40, 95, 160, 215};
        for (int i = 0; i < times.length; i++) {
            MawMotion.Shape s = MawMotion.idle(times[i], SEED);
            g.drawImage(frame(s, times[i], 0xA50F37, PoseRender.orbit(35, 10, 9.5, 1.7), fw, fh), i * fw, 34, null);
            label(g, String.format("%.0ft  mouth %.2f", times[i], s.aperture()), i * fw, 34);
        }
        double[][] views = {{150, 12}, {-100, 6}, {-30, 55}, {200, 25}, {60, -5}};
        String[] names = {"behind", "right side", "above", "behind left", "low front"};
        for (int i = 0; i < views.length; i++) {
            MawMotion.Shape s = MawMotion.idle(14, SEED);
            g.drawImage(frame(s, 14, 0xA50F37, PoseRender.orbit(views[i][0], views[i][1], 9.5, 1.7), fw, fh), i * fw, 34 + fh, null);
            label(g, names[i], i * fw, 34 + fh);
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }

    /** Its mouth close up, shut to thrown wide, from ahead and from the side. */
    static void mouth(File file) throws Exception {
        int fw = 340;
        int fh = 340;
        double[] opens = {0.0, 0.12, 0.35, 0.7, 1.0};
        BufferedImage img = new BufferedImage(fw * opens.length, fh * 2 + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.4.9: the mouth, shut to thrown wide (shut: puckered lip, fangs folded over it; open: fangs splayed, head flared)");
        for (int i = 0; i < opens.length; i++) {
            MawMotion.Shape rest = MawMotion.idle(14, SEED);
            MawMotion.Shape s = new MawMotion.Shape(rest.path(), rest.dorsal(), opens[i]);
            double[] head = w(rest.path()[rest.path().length - 1]);
            double[] neck = w(rest.path()[rest.path().length - 2]);
            double[] dir = PoseRender.norm(PoseRender.sub(head, neck));
            double[] eye = PoseRender.add(head, PoseRender.add(PoseRender.scale(dir, 1.25), new double[] {0.15, 0.2, 0.0}));
            BufferedImage a = PoseRender.render(new RigPose(), new PoseRender.Camera(eye, head, 40), fw, fh, worm(s, 1.0, 14, 0xA50F37, true));
            g.drawImage(a, i * fw, 34, null);
            label(g, String.format("ahead, mouth %.2f", opens[i]), i * fw, 34);
            double[] side = PoseRender.norm(PoseRender.cross(dir, new double[] {0, 1, 0}));
            double[] eye2 = PoseRender.add(head, PoseRender.add(PoseRender.scale(side, 1.2), PoseRender.scale(dir, 0.35)));
            BufferedImage b = PoseRender.render(new RigPose(), new PoseRender.Camera(eye2, PoseRender.add(head, PoseRender.scale(dir, 0.1)), 40), fw, fh,
                worm(s, 1.0, 14, 0xA50F37, true));
            g.drawImage(b, i * fw, 34 + fh, null);
            label(g, "side", i * fw, 34 + fh);
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }

    /** A bite at something ahead of him (a body's chest 3 blocks out), frame by frame. */
    static void bite(File file) throws Exception {
        int fw = 260;
        int fh = 320;
        double total = 12.0;
        double land = 6.0;
        double[] ages = {0, 1.5, 3, 4.5, 5.5, 6, 7, 8.5, 10, 12};
        BufferedImage img = new BufferedImage(fw * ages.length, fh * 2 + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.4.9: a bite (drawn back, thrown out gaping, snapped shut at 6 ticks, shaken, back)");
        double[] target = {0.1, 1.2, 3.0};
        for (int i = 0; i < ages.length; i++) {
            MawMotion.Shape s = MawMotion.bite(30 + ages[i], SEED, target, ages[i], total, land);
            g.drawImage(frame(s, 30 + ages[i], 0xA50F37, PoseRender.orbit(55, 10, 10.5, 1.6), fw, fh), i * fw, 34, null);
            label(g, String.format("%.1ft  mouth %.2f", ages[i], s.aperture()), i * fw, 34);
            g.drawImage(frame(s, 30 + ages[i], 0xA50F37, PoseRender.orbit(-95, 8, 10.5, 1.6), fw, fh), i * fw, 34 + fh, null);
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }

    /** In other kagune colours: the flesh follows, the teeth and throat keep theirs. */
    static void colours(File file) throws Exception {
        int fw = 300;
        int fh = 360;
        int[] tints = {0xA50F37, 0x111014, 0xE8E2D6, 0x2350D8, 0x2FB34A, 0x8A2BE2};
        String[] names = {"crimson (default)", "black", "bone white", "blue", "green", "violet"};
        BufferedImage img = new BufferedImage(fw * tints.length, fh + 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet(img, "Bikaku 3.4.9: other kagune colours (flesh and veins follow; teeth, spurs, throat keep theirs)");
        for (int i = 0; i < tints.length; i++) {
            MawMotion.Shape rest = MawMotion.idle(14, SEED);
            MawMotion.Shape s = new MawMotion.Shape(rest.path(), rest.dorsal(), 0.8);
            g.drawImage(frame(s, 14, tints[i], PoseRender.orbit(35, 10, 9.5, 1.7), fw, fh), i * fw, 34, null);
            label(g, names[i], i * fw, 34);
        }
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println(file);
    }
}

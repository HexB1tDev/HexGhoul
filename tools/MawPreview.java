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
        List<PoseRender.Tri> out = new ArrayList<>();
        for (PoseRender.Tri t : PoseRender.model(new RigPose())) {
            double[][] v = new double[3][];
            for (int k = 0; k < 3; k++) {
                v[k] = new double[] {-t.v()[k][0] - PREY_FEET[0], t.v()[k][1], -t.v()[k][2] + PREY_FEET[2]};
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

    /** As the renderer does in first person: the line kept 1.25 from the eyes, no quad with a corner within 0.6. */
    static List<PoseRender.Tri> firstPersonWorm(MawMotion.Shape shape, double time, int tint) {
        double[] eye = {0.0, 1.62, 0.0};
        double[][] path = MawMotion.firstPerson(shape.path(), eye);
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
        double[][] path = new double[shape.path().length][];
        for (int i = 0; i < path.length; i++) {
            path[i] = shape.path()[i];
        }
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

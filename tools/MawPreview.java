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
 * java -Djava.awt.headless=true -cp build/maw-preview:build/quick/classes MawPreview &lt;out-dir&gt; [look|mouth|bite|colours|all]
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
        Graphics2D g = sheet(img, "Bikaku worm (WIP): at rest, about 8 blocks of zigzag coils (it slithers, scans in snaps, now and then gapes)");
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
        Graphics2D g = sheet(img, "Bikaku worm (WIP): the mouth, shut to thrown wide (shut: puckered lip, fangs folded over it; open: fangs splayed, head flared)");
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
        Graphics2D g = sheet(img, "Bikaku worm (WIP): a bite (drawn back, thrown out gaping, snapped shut at 6 ticks, shaken, back)");
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
        Graphics2D g = sheet(img, "Bikaku worm (WIP): other kagune colours (flesh and veins follow; teeth, spurs, throat keep theirs)");
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

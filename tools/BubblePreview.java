import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.family.ChildTemper;
import com.hex.kagune.control.family.Parenting;
import com.hex.kagune.control.family.Siblings;
import com.hex.kagune.control.npc.BubbleLayout;
import com.hex.kagune.control.npc.BubbleShape;
import com.hex.kagune.control.npc.Speech;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * 1.3.38: the speech bubble and the thought cloud off-game ({@link BubbleShape}'s quads, the same the game draws), at 6 screen pixels a
 * text pixel, over a sky-ish background, the words in a stand-in font a Minecraft character's width. Usage after a build:
 * {@code java -cp build/classes tools/BubblePreview.java <out-dir>}. 1.3.52: also {@code bubbles-scene.png}: speakers in the world as the
 * camera sees them (a block a 0.025-pixel name tag scale, six blocks off), real lines from a squabble and a parent's talk, before (1.3.51:
 * every line up as long as it takes to read, the reply sooner) and after (each line down as the reply goes up; other bubbles kept apart by
 * {@link BubbleLayout}).
 */
public final class BubblePreview {
    static final int K = 6;

    record Quad(float[][] p, int argb, float depth) {
    }

    static void panel(Graphics2D g, int ox, int oy, String[] lines, boolean thought) {
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, line.length() * 6 - 1);
        }
        BubbleShape shape = BubbleShape.of(width, lines.length, thought);
        List<Quad> quads = new ArrayList<>();
        shape.fill((a, b, c, d, argb, depth) -> quads.add(new Quad(new float[][] {a, b, c, d}, argb, depth)));
        quads.sort((x, y) -> Float.compare(y.depth(), x.depth()));
        for (Quad q : quads) {
            Polygon poly = new Polygon();
            for (float[] p : q.p()) {
                poly.addPoint(ox + Math.round(p[0] * K), oy + Math.round(p[1] * K));
            }
            g.setColor(new Color(q.argb(), true));
            g.fillPolygon(poly);
        }
        g.setFont(new Font(Font.MONOSPACED, thought ? Font.ITALIC | Font.BOLD : Font.BOLD, 9 * K - 6));
        g.setColor(thought ? new Color(0x55, 0x55, 0x55) : Color.BLACK);
        for (int i = 0; i < lines.length; i++) {
            int w = lines[i].length() * 6 * K;
            g.drawString(lines[i], ox - w / 2, oy + Math.round(shape.lineY(i) * K) + 8 * K);
        }
        g.setColor(new Color(0xD8, 0x3A, 0x3A));
        g.setStroke(new BasicStroke(3));
        g.drawOval(ox - 6, oy - 6, 12, 12); // where its point is (the head)
    }

    // ---------------------------------------------------------------- 1.3.52: scenes in the world

    static final int W = 1280;
    static final int H = 560;
    static final double FOCAL = (720 / 2.0) / Math.tan(Math.toRadians(35)); // a 70 degree view at 720 high
    static final double[] CAM = {0.0, 1.62, -6.0};
    static final float SCALE = 0.025F;

    /** Someone in the scene: where (feet), how tall, how wide, coloured. */
    record Body(double x, double y, double z, double height, double width, Color colour) {
        double top() {
            return this.y + this.height;
        }
    }

    /** A bubble in the scene: over whom, what, how long it has been up and has left. */
    record Said(int id, Body body, String text, double age, double left) {
    }

    static double[] project(double x, double y, double z) {
        double rx = x - CAM[0];
        double ry = y - CAM[1];
        double rz = z - CAM[2];
        return new double[] {W / 2.0 - FOCAL * rx / rz, H * 0.62 - FOCAL * ry / rz, rz};
    }

    static void ground(Graphics2D g, String title) {
        g.setColor(new Color(0x7A, 0xA6, 0xD8));
        g.fillRect(0, 0, W, H);
        int horizon = (int)project(0.0, 0.0, 0.0)[1];
        g.setColor(new Color(0x5E, 0x9B, 0x3A));
        g.fillRect(0, horizon, W, H - horizon);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        g.drawString(title, 18, 28);
    }

    static void figure(Graphics2D g, Body b) {
        double[] feet = project(b.x(), b.y(), b.z());
        double[] top = project(b.x(), b.top(), b.z());
        double k = FOCAL / feet[2];
        int w = (int)Math.round(b.width() * k);
        g.setColor(b.colour());
        g.fillRect((int)Math.round(feet[0] - w / 2.0), (int)Math.round(top[1]), w, (int)Math.round(feet[1] - top[1]));
        int head = (int)Math.round(Math.min(b.width() * 0.9, b.height() * 0.3) * k);
        g.setColor(new Color(0xE8, 0xC4, 0xA4));
        g.fillRect((int)Math.round(feet[0] - head / 2.0), (int)Math.round(top[1]), head, head);
    }

    static BubbleShape shape(String text, float shift, float lift) {
        List<String> lines = Speech.wrap(text);
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, line.length() * 6 - 1);
        }
        return BubbleShape.of(width, lines.size(), Speech.action(text), shift, lift);
    }

    /** The bubbles over a scene, laid out ({@code layout}) as the game does, nearest drawn last. */
    static void bubbles(Graphics2D g, List<Said> said, boolean layout) {
        Map<Integer, BubbleLayout.Place> places = new HashMap<>();
        if (layout) {
            List<BubbleLayout.Seen> seen = new ArrayList<>();
            for (Said s : said) {
                if (s.left() < BubbleShape.GOES) {
                    continue;
                }
                double fx = s.body().x() - CAM[0];
                double fy = s.body().top() + 0.3 - CAM[1];
                double fz = s.body().z() - CAM[2];
                double flat = Math.sqrt(fx * fx + fz * fz);
                double far = Math.sqrt(flat * flat + fy * fy);
                BubbleShape home = shape(s.text(), 0.0F, 0.0F);
                seen.add(BubbleLayout.Seen.of(s.id(), s.age(), -Math.atan2(fx, fz), Math.atan2(fy, flat), SCALE / far, home));
            }
            places.putAll(BubbleLayout.arrange(seen));
        }
        List<Said> order = new ArrayList<>(said);
        order.sort((a, b) -> Double.compare(b.body().z(), a.body().z()));
        for (Said s : order) {
            BubbleLayout.Place place = places.getOrDefault(s.id(), BubbleLayout.Place.HOME);
            float pop = BubbleShape.pop(s.age(), s.left());
            if (pop <= 0.01F) {
                continue;
            }
            double[] at = project(s.body().x(), s.body().top() + 0.3, s.body().z());
            double k = FOCAL * SCALE / at[2] * pop;
            BubbleShape shape = shape(s.text(), place.shift(), place.lift());
            List<Quad> quads = new ArrayList<>();
            shape.fill((a, b, c, d, argb, depth) -> quads.add(new Quad(new float[][] {a, b, c, d}, argb, depth)));
            quads.sort((x, y) -> Float.compare(y.depth(), x.depth()));
            for (Quad q : quads) {
                Polygon poly = new Polygon();
                for (float[] p : q.p()) {
                    poly.addPoint((int)Math.round(at[0] + p[0] * k), (int)Math.round(at[1] + p[1] * k));
                }
                g.setColor(new Color(q.argb(), true));
                g.fillPolygon(poly);
            }
            List<String> lines = Speech.wrap(s.text());
            boolean thought = Speech.action(s.text());
            g.setFont(new Font(Font.MONOSPACED, thought ? Font.ITALIC | Font.BOLD : Font.BOLD, (int)Math.round(10 * k)));
            g.setColor(thought ? new Color(0x55, 0x55, 0x55) : Color.BLACK);
            for (int i = 0; i < lines.size(); i++) {
                int w = (int)Math.round(lines.get(i).length() * 6 * k);
                g.drawString(lines.get(i), (int)Math.round(at[0] + shape.shift * k - w / 2.0), (int)Math.round(at[1] + (shape.lineY(i) + 8) * k));
            }
        }
    }

    /** 1.3.51's rule: the reply after this long, each line up as long as it takes to read. */
    static int pause151(String text) {
        return Math.max(36, Math.min(110, 30 + text.length() * 13 / 10));
    }

    static void scenes(File dir) throws Exception {
        int panels = 8;
        BufferedImage img = new BufferedImage(W, H * panels, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double kid = 1.8 * ChildStage.CHILD.box;
        Body hana = new Body(-0.8, 0.0, 0.0, kid, 0.42, new Color(0x8A, 0x3A, 0x5A));
        Body ren = new Body(0.8, 0.0, 0.0, kid, 0.42, new Color(0x3A, 0x4A, 0x8A));
        double size = 0.94;
        Body mom = new Body(0.0, 0.0, 0.0, 1.8 * size, 0.55 * size, new Color(0x40, 0x40, 0x48));
        Body tod = new Body(0.0, 1.02 * size, -0.34 * size, 1.8 * ChildStage.TODDLER.box, 0.36, new Color(0xC0, 0x60, 0x60));
        List<Siblings.Line> squabble = Siblings.talk(Siblings.Kind.SQUABBLE, ChildTemper.BOLD, ChildStage.CHILD, "Hana", "Mama", true, ChildTemper.MISCHIEF,
            ChildStage.CHILD, "Ren", "Mama", false, 11);
        String a = squabble.get(0).text();
        String b = squabble.get(1).text();
        List<Parenting.Line> fetch = Parenting.talk(Parenting.Why.WANDERED, "touka", ChildStage.TODDLER, ChildTemper.BOLD, "Hana", "Mama", 7);
        String m = fetch.get(0).text();
        String t = fetch.get(1).text();
        // 1: the squabble, 12 ticks into the reply, as 1.3.51 had it.
        int at = 12;
        ground(g, "1.3.51  a squabble, " + at + " ticks into the reply: the first line still up (" + (Speech.ticks(a) - pause151(a)) + " ticks both up)");
        figure(g, hana);
        figure(g, ren);
        bubbles(g, List.of(new Said(1, hana, a, pause151(a) + at, Speech.ticks(a) - pause151(a) - at), new Said(2, ren, b, at, Speech.ticks(b) - at)), false);
        // 2: the same moment now: the first line came down as the reply went up.
        g.translate(0, H);
        int life = Speech.life(a, Speech.turn(1, 2, Parenting.pause(a)));
        ground(g, "1.3.52  the same moment: the first line was up " + life + " ticks and came down as the reply went up");
        figure(g, hana);
        figure(g, ren);
        bubbles(g, List.of(new Said(1, hana, a, life + at, -at), new Said(2, ren, b, at, Speech.ticks(b) - at)), true);
        // 3: the turn itself: the line shrinking away in its last tick as the reply pops up.
        g.translate(0, H);
        ground(g, "1.3.52  the turn, frame by frame: the line shrinks away in its last " + (int)BubbleShape.GOES + " ticks, then the reply pops up");
        figure(g, hana);
        figure(g, ren);
        bubbles(g, List.of(new Said(1, hana, a, life - 1.2, 1.2)), true);
        // 4: a toddler on its mother's hip: 1.3.51 against 1.3.52.
        g.translate(0, H);
        ground(g, "1.3.51  a toddler carried home answers while its mother's line is still up (" + (Speech.ticks(m) - pause151(m)) + " ticks)");
        figure(g, mom);
        figure(g, tod);
        bubbles(g, List.of(new Said(1, mom, m, pause151(m) + at, Speech.ticks(m) - pause151(m) - at), new Said(2, tod, t, at, Speech.ticks(t) - at)), false);
        g.translate(0, H);
        ground(g, "1.3.52  the same: her line came down as it answered");
        figure(g, mom);
        figure(g, tod);
        int momLife = Speech.life(m, Speech.turn(1, 2, Parenting.pause(m)));
        bubbles(g, List.of(new Said(1, mom, m, momLife + at, -at), new Said(2, tod, t, at, Speech.ticks(t) - at)), true);
        // 6: two things said at once that are not one talk (a word across a squabble): the newer kept clear of the older.
        g.translate(0, H);
        ground(g, "1.3.52  two lines up at once that aren't one talk's turns: the newer kept clear, its tail to its own speaker");
        figure(g, hana);
        figure(g, ren);
        bubbles(g, List.of(new Said(1, hana, a, 40, 60), new Said(2, ren, "*sticks out his tongue*", 6, 80)), true);
        // 7: two long lines at once, a block and a half apart.
        g.translate(0, H);
        ground(g, "1.3.52  two long lines at once, a block and a half apart");
        figure(g, hana);
        figure(g, ren);
        bubbles(g, List.of(new Said(1, hana, "You ALWAYS get the window seat and it's not fair, Ren, it's my turn.", 40, 60),
            new Said(2, ren, "Mama said whoever's first gets it. I was first. Go cry about it.", 6, 80)), true);
        // 8: a toddler on its mother's hip, her own remark up as it says something of its own.
        g.translate(0, H);
        ground(g, "1.3.52  on its mother's hip: her own remark up, the toddler's line beside it");
        figure(g, mom);
        figure(g, tod);
        bubbles(g, List.of(new Said(1, mom, "*picks Hana up and holds them on her hip*", 30, 60), new Said(2, tod, "Mama! Look! A birdie!", 5, 80)), true);
        g.dispose();
        File out = new File(dir, "bubbles-scene.png");
        ImageIO.write(img, "png", out);
        System.out.println("wrote " + out);
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : ".");
        dir.mkdirs();
        scenes(dir);
        BufferedImage img = new BufferedImage(1500, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x7A, 0xA6, 0xD8));
        g.fillRect(0, 0, 1500, 720);
        panel(g, 380, 330, new String[] {"Welcome home. I kept", "you some dinner."}, false);
        panel(g, 1100, 330, new String[] {"*stirs, mumbling,", "and snuggles in*"}, true);
        panel(g, 380, 680, new String[] {"Hey."}, false);
        panel(g, 1100, 680, new String[] {BubbleShape.zees(40)}, true);
        g.dispose();
        ImageIO.write(img, "png", new File(dir, "bubbles.png"));
        System.out.println("wrote " + new File(dir, "bubbles.png"));
    }
}

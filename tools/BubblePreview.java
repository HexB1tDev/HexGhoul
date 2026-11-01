import com.hex.kagune.control.npc.BubbleShape;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 1.3.38: the speech bubble and the thought cloud off-game ({@link BubbleShape}'s quads, the same the game draws), at 6 screen pixels a
 * text pixel, over a sky-ish background, the words in a stand-in font a Minecraft character's width. Usage after a build:
 * {@code java -cp build/classes tools/BubblePreview.java <out-dir>}.
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

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : ".");
        dir.mkdirs();
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

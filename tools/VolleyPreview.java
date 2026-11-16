import com.hex.kagune.control.quinque.DawnVolley;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * 2.0.48: the White Reaper's volley ({@code DawnVolley}) off-game: its five balls tick by tick, seen from behind him down the line he
 * aimed along (the ring opening and closing) and from the side (leaving the point, apart, meeting on the quarry, apart again past it).
 * Usage: {@code java -cp <classes> VolleyPreview <out.png>}.
 */
public final class VolleyPreview {
    public static void main(String[] args) throws Exception {
        int w = 900;
        int h = 460;
        BufferedImage img = new BufferedImage(w * 2, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x1C2028));
        g.fillRect(0, 0, w * 2, h);
        double aimed = 14.0;
        double speed = 1.5;
        double[] from = {0, 0, 0};
        double[] way = {0, 0, 1};
        Color[] colours = {new Color(0xFFF6C8), new Color(0xFFD23C), new Color(0xFFB000), new Color(0xFFE680), new Color(0xFFC020)};
        // From behind: x right, y up, the ring at each tick (fainter further along).
        double scale = 90;
        int cx = w / 2;
        int cy = h / 2 + 10;
        g.setColor(new Color(0x2E3440));
        g.drawOval(cx - (int)(DawnVolley.RADIUS * scale), cy - (int)(DawnVolley.RADIUS * scale), (int)(2 * DawnVolley.RADIUS * scale), (int)(2 * DawnVolley.RADIUS * scale));
        g.setColor(new Color(0x7A2030));
        g.fillOval(cx - 14, cy - 28, 28, 56); // the quarry, where it was aimed at
        for (double t = 0; t <= aimed / speed + 0.01; t += 0.5) {
            for (int i = 0; i < DawnVolley.COUNT; i++) {
                double[] p = DawnVolley.at(from, way, i, aimed, t * speed);
                double fade = 1.0 - 0.6 * t / (aimed / speed);
                Color c = colours[i];
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(255 * fade)));
                int r = (int)(5 + 7 * fade);
                // Seen from behind him: his right is the picture's right (the ring's right is -x here for way +z).
                g.fillOval(cx + (int)(-p[0] * scale) - r, cy - (int)(p[1] * scale) - r, 2 * r, 2 * r);
            }
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.drawString("from behind him, down the line he aimed: five from one point open into a ring and close on the quarry", 10, 24);
        // From the side: along (z) to the right, up (y) up.
        double sx = (w - 80) / (aimed * 1.6);
        int ox = w + 40;
        int oy = h / 2 + 10;
        g.setColor(new Color(0x7A2030));
        g.fillRect(ox + (int)(aimed * sx) - 10, oy - 28, 20, 56);
        g.setColor(new Color(0x404858));
        g.setStroke(new BasicStroke(1.5F));
        g.drawLine(ox, oy, ox + (int)(aimed * 1.55 * sx), oy);
        for (int i = 0; i < DawnVolley.COUNT; i++) {
            Color c = colours[i];
            double[] last = null;
            for (double s = 0; s <= aimed * 1.55; s += 0.1) {
                double[] p = DawnVolley.at(from, way, i, aimed, s);
                if (last != null) {
                    g.setColor(c);
                    g.setStroke(new BasicStroke(2.5F));
                    g.drawLine(ox + (int)(last[2] * sx), oy - (int)(last[1] * sx * 1.6), ox + (int)(p[2] * sx), oy - (int)(p[1] * sx * 1.6));
                }
                last = p;
            }
            for (double s = 0; s <= aimed * 1.55; s += speed) {
                double[] p = DawnVolley.at(from, way, i, aimed, s);
                g.fillOval(ox + (int)(p[2] * sx) - 5, oy - (int)(p[1] * sx * 1.6) - 5, 10, 10);
            }
        }
        g.setColor(Color.WHITE);
        g.drawString("from the side (each dot a tick): out of Dawn's point, apart, meeting on the quarry 14 blocks off, apart again", w + 10, 24);
        g.drawString("Dawn's point", w + 10, oy + 46);
        g.drawString("the quarry", ox + (int)(aimed * sx) - 40, oy + 52);
        g.dispose();
        ImageIO.write(img, "png", new File(args[0]));
        System.out.println("wrote " + args[0]);
    }
}

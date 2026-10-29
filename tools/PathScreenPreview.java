import com.hex.kagune.control.client.PathCards;
import com.hex.kagune.control.client.PathChoiceLayout;
import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.client.PathSilhouette;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Off-game mock of PathChoiceScreen for design review: the real layout and silhouette geometry from build/classes,
 * with a stand-in font (Minecraft's own font and textures are not available here).
 * Usage after {@code python3 build.py}: {@code java -cp build/classes tools/PathScreenPreview.java docs/images}
 */
public class PathScreenPreview {
    static final int BG = 0x0B0608, CARD = 0x130B0D, BUTTON = 0x1A1012, BONE = 0xE8DCCB, MUTED = 0x9C8F84;
    static Graphics2D g; static Font font;

    public static void main(String[] a) throws Exception {
        render(480, 270, 3, "UKAKU", 1.25, a[0] + "/path-choice-1080p.png");
        render(320, 240, 3, "NONE", 0.4, a[0] + "/path-choice-smallest.png");
    }

    static Color c(int rgb) { return new Color(rgb); }
    static void fill(int x1, int y1, int x2, int y2, int rgb) { g.setColor(c(rgb)); g.fillRect(x1, y1, x2 - x1, y2 - y1); }
    static int w(String s) { return g.getFontMetrics().stringWidth(s); }
    static void text(String s, int x, int y, int rgb) { g.setColor(c(rgb)); g.drawString(s, x, y + 7); }
    static void center(String s, int cx, int y, int rgb) { text(s, cx - w(s) / 2, y, rgb); }
    static String fit(String s, int width) { if (w(s) <= width) return s; String t = s; while (!t.isEmpty() && w(t + "...") > width) t = t.substring(0, t.length() - 1); return t.strip() + "..."; }

    static void render(int W, int H, int scale, String selected, double time, String out) throws Exception {
        BufferedImage img = new BufferedImage(W * scale, H * scale, BufferedImage.TYPE_INT_RGB);
        g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.scale(scale, scale);
        font = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
        g.setFont(font);
        PathChoiceLayout L = PathChoiceLayout.of(W, H);
        fill(0, 0, W, H, BG);
        for (int i = 0; i < H / 3; i++) fill(0, i, W, i + 1, PathSilhouette.blend(BG, 0, 0.45f * (1 - i / (H / 3f))));
        Font title = font.deriveFont(Font.BOLD, 12f); g.setFont(title);
        center("YOUR RC CELLS ARE WAKING UP", W / 2, L.titleY, BONE);
        g.setFont(font);
        String sub = w("What tears out of your back decides how you fight.") <= W - 32 ? "What tears out of your back decides how you fight." : "Your kagune decides how you fight.";
        center(sub, W / 2, L.subtitleY, MUTED);
        if (L.subtitleLines > 1) center("This choice is permanent. Choose with your gut.", W / 2, L.subtitleY + 10, MUTED);
        List<PathCards.Card> cards = PathCards.choosable();
        for (int i = 0; i < cards.size(); i++) {
            PathCards.Card c = cards.get(i);
            card(L, L.cards.get(i), c, c.path().name().equals(selected), time);
        }
        Box b = L.confirm; boolean en = !selected.equals("NONE"); int acc = en ? PathCards.of(com.hex.kagune.control.KagunePath.valueOf(selected)).accent() : MUTED;
        fill(b.x() - 1, b.y() - 1, b.right() + 1, b.bottom() + 1, en ? acc : PathSilhouette.blend(BG, MUTED, 0.35f));
        fill(b.x(), b.y(), b.right(), b.bottom(), BUTTON);
        if (en) fill(b.x(), b.y(), b.x() + (int)(b.width() * 0.55), b.bottom(), PathSilhouette.blend(BUTTON, acc, 0.6f));
        center(en ? "Awakening..." : "Choose a path", b.centerX(), b.y() + 6, en ? BONE : MUTED);
        center("Decide later", L.later.centerX(), L.later.y() + 2, MUTED);
        if (L.hintY >= 0) center("Arrows: choose   Hold Enter: awaken   Esc: decide later", W / 2, L.hintY, PathSilhouette.blend(BG, MUTED, 0.7f));
        ImageIO.write(img, "png", new File(out));
    }

    static void card(PathChoiceLayout L, Box slot, PathCards.Card card, boolean chosen, double time) {
        String name = card.name(), type = card.type(), tags = card.tags();
        List<String> bullets = card.bullets();
        int accent = card.accent();
        int[] strip = card.stageMastery().stream().mapToInt(Integer::intValue).toArray();
        int lift = chosen ? 2 : 0;
        Box box = new Box(slot.x(), slot.y() - lift, slot.width(), slot.height());
        if (chosen) fill(box.x() - 3, box.y() - 3, box.right() + 3, box.bottom() + 3, PathSilhouette.blend(BG, accent, 0.25f));
        fill(box.x() - 1, box.y() - 1, box.right() + 1, box.bottom() + 1, chosen ? accent : PathSilhouette.blend(CARD, accent, 0.35f));
        fill(box.x(), box.y(), box.right(), box.bottom(), CARD);
        PathChoiceLayout.Card in = L.card(box);
        float intensity = chosen ? 1f : 0.35f;
        List<PathSilhouette.Mark> marks = card.silhouette().marks(in.silhouette(), time, intensity, chosen, CARD);
        for (PathSilhouette.Mark m : marks) draw(m);
        Font bold = font.deriveFont(Font.BOLD); g.setFont(bold);
        text(fit(name, in.textWidth()), in.textX(), in.nameY(), chosen ? accent : PathSilhouette.blend(CARD, accent, 0.8f));
        g.setFont(font);
        text(fit(type, in.textWidth()), in.textX(), in.typeY(), MUTED);
        for (int i = 0; i < Math.min(in.maxBullets(), bullets.size()); i++) text(fit("• " + bullets.get(i), in.textWidth()), in.textX(), in.bulletsY() + i * 10, PathSilhouette.blend(CARD, BONE, chosen ? 1f : 0.8f));
        if (in.tagsY() >= 0) text(fit(tags, in.textWidth()), in.textX(), in.tagsY(), PathSilhouette.blend(CARD, accent, 0.75f));
        if (in.stripY() >= 0) {
            int left = in.textX() + 6, right = in.textX() + in.textWidth() - 6, y = in.stripY() + 2;
            fill(left, y, right, y + 1, PathSilhouette.blend(CARD, accent, 0.4f));
            Font small = font.deriveFont(5.6f); g.setFont(small);
            for (int i = 0; i < strip.length; i++) {
                int x = left + (right - left) * i / (strip.length - 1);
                fill(x - 1, y - 1, x + 2, y + 2, PathSilhouette.blend(CARD, accent, 0.75f));
                String n = Integer.toString(strip[i]); g.setColor(c(MUTED)); g.drawString(n, x - g.getFontMetrics().stringWidth(n) / 2f, y + 10);
            }
            g.setFont(font);
        }
    }

    static void draw(PathSilhouette.Mark mark) {
        if (mark instanceof PathSilhouette.Stroke s) {
            float dx = s.x1() - s.x0(), dy = s.y1() - s.y0();
            int steps = Math.max(1, (int)Math.ceil(Math.sqrt(dx * dx + dy * dy) / Math.max(0.75f, Math.max(0.6f, Math.min(s.t0(), s.t1())) * 0.6f)));
            for (int i = 0; i <= steps; i++) {
                float t = i / (float)steps, half = (s.t0() + (s.t1() - s.t0()) * t) / 2f, x = s.x0() + dx * t, y = s.y0() + dy * t;
                fill(Math.round(x - half), Math.round(y - half), Math.max(Math.round(x - half) + 1, Math.round(x + half)), Math.max(Math.round(y - half) + 1, Math.round(y + half)), s.rgb());
            }
        } else if (mark instanceof PathSilhouette.Shape sh) {
            int n = sh.x().length;
            int[] xs = new int[n], ys = new int[n];
            for (int i = 0; i < n; i++) { xs[i] = Math.round(sh.x()[i]); ys[i] = Math.round(sh.y()[i]); }
            int argb = sh.argb();
            g.setColor(new Color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255));
            g.fillPolygon(xs, ys, n);
        } else if (mark instanceof PathSilhouette.Disc d) {
            int r = Math.max(1, Math.round(d.r()));
            for (int dy = -r; dy < r; dy++) { float row = dy + 0.5f; int half = Math.max(1, Math.round((float)Math.sqrt(Math.max(0, d.r() * d.r() - row * row)))); fill(Math.round(d.cx()) - half, Math.round(d.cy()) + dy, Math.round(d.cx()) + half, Math.round(d.cy()) + dy + 1, d.rgb()); }
        }
    }
}

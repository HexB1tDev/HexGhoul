package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.king.KingForm;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.0.10: the two cards after the Dragon (pure, on any {@link UiCanvas}, 960 x 520 scaled to the window): "What kind of King will you
 * become?" War on the left, black with red veins and its one white eye; Peace on the right, white with feathers and its red cross
 * blade. One is picked with a click and chosen with the bar held: once, for good. A card that is not built yet is sealed.
 *
 * <p>1.0.11: drawn as a page of Kaneki's own kagune menu ({@link KanekiMenuView}, in {@link KingTheme#KANEKI}): the same organic board
 * with its veins, the spaced-out title over the subtitle and the hint, headers and rows in the menu's colours, and the bar held to
 * choose filling as the mastery bar does.
 */
public final class KingChoiceView {
    public static final int UI_WIDTH = AyatoMenuView.UI_WIDTH;
    public static final int UI_HEIGHT = AyatoMenuView.UI_HEIGHT;
    static final int PANEL_W = 930;
    static final int PANEL_H = 500;
    static final int CARD_W = 330;
    static final int CARD_H = 300;
    static final int CARD_Y = 96;

    /** What the screen shows: the card picked (NONE: none yet), how far the bar is held (0..1), and whether Peace can be chosen. */
    public record Model(KingForm picked, double hold, boolean peaceBuilt) {
    }

    private KingChoiceView() {
    }

    /** The card's box for {@code form}. */
    static AyatoMenuView.Rect card(KingForm form) {
        int x = form == KingForm.WAR ? UI_WIDTH / 2 - 34 - CARD_W : UI_WIDTH / 2 + 34;
        return new AyatoMenuView.Rect(x, CARD_Y, CARD_W, CARD_H);
    }

    /** Draws the screen and returns its clickable areas: "card:war", "card:peace", "choose", "later". */
    public static Map<String, AyatoMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        KingTheme t = KingTheme.KANEKI;
        Map<String, AyatoMenuView.Rect> hits = new LinkedHashMap<>();
        int centerX = UI_WIDTH / 2;
        int centerY = UI_HEIGHT / 2;
        int left = centerX - PANEL_W / 2;
        int top = centerY - PANEL_H / 2;
        c.batch(() -> {
            AyatoMenuView.organicPanel(c, centerX, centerY, PANEL_W / 2, PANEL_H / 2, t.panelFill(), t.panelEdge());
            AyatoMenuView.organicPanel(c, centerX, centerY, PANEL_W / 2 - 5, PANEL_H / 2 - 5, t.innerFill(), t.innerEdge());
            // The menu's veins: the arc under the title, and one down the middle between the two ways.
            AyatoMenuView.drawVein(c, left + 18, top + 74, centerX, top + 32, left + PANEL_W - 18, top + 74, t.veinBright(), 2);
            AyatoMenuView.drawVein(c, centerX, top + 82, centerX - 14, centerY, centerX + 6, CARD_Y + CARD_H + 4, t.vein(), 2);
        });
        c.centered("W H A T   K I N D   O F   K I N G   W I L L   Y O U   B E C O M E ?", centerX, top + 18, t.title());
        c.centered("THE DRAGON IS SPENT", centerX, top + 34, t.subtitle());
        c.centered("Click a card  •  hold the bar (or Enter) to become it  •  once, and never again", centerX, top + 51, t.hint());
        for (KingForm form : new KingForm[] {KingForm.WAR, KingForm.PEACE}) {
            AyatoMenuView.Rect r = card(form);
            boolean sealed = form == KingForm.PEACE && !model.peaceBuilt();
            boolean picked = model.picked() == form;
            boolean hover = r.contains(mouseX, mouseY) && !sealed;
            if (form == KingForm.WAR) {
                war(c, t, r, picked, hover, time);
            } else {
                peace(c, t, r, picked, hover, sealed, time);
            }
            hits.put("card:" + form.id(), r);
        }
        // The bar, held to choose: the mastery bar's capsule, filling.
        boolean enabled = model.picked() != KingForm.NONE;
        int barX = centerX - 150;
        int barY = CARD_Y + CARD_H + 34;
        AyatoMenuView.Rect bar = new AyatoMenuView.Rect(barX - 6, barY - 18, 312, 30);
        boolean over = bar.contains(mouseX, mouseY);
        String label = !enabled ? "PICK A CARD" : model.hold() > 0.0 ? "BECOMING " + model.picked().displayName().toUpperCase() + "..."
            : "HOLD TO BECOME " + model.picked().displayName().toUpperCase();
        c.text(label, barX, barY - 15, enabled ? (over ? t.title() : t.header()) : t.faint(), false);
        String share = enabled ? Math.round(100.0 * Math.min(1.0, model.hold())) + "%" : "";
        c.text(share, barX + 300 - c.width(share), barY - 15, t.value(), false);
        AyatoMenuView.capsule(c, barX, barY, 300, 8, over && enabled ? t.buttonHover() : t.barBack());
        if (model.hold() > 0.0) {
            AyatoMenuView.capsule(c, barX, barY, Math.max(16, (int)Math.round(300 * Math.min(1.0, model.hold()))), 8, t.bar());
        }
        hits.put("choose", bar);
        AyatoMenuView.Rect later = new AyatoMenuView.Rect(centerX - 60, top + PANEL_H - 22, 120, 14);
        c.centered("DECIDE LATER", centerX, top + PANEL_H - 18, later.contains(mouseX, mouseY) ? t.on() : t.off());
        hits.put("later", later);
        return hits;
    }

    /** A card's frame: the menu's button fill, its edge lit when hovered and red when picked. */
    static int frame(UiCanvas c, KingTheme t, AyatoMenuView.Rect r, int fill, boolean picked, boolean hover) {
        int lift = picked ? 2 : 0;
        int top = r.y() - lift;
        int edge = picked ? t.bar() : hover ? t.buttonHover() : t.innerEdge();
        c.fill(r.x() - 2, top - 2, r.x() + r.width() + 2, top + r.height() + 2, edge);
        c.fill(r.x(), top, r.x() + r.width(), top + r.height(), fill);
        return top;
    }

    static void war(UiCanvas c, KingTheme t, AyatoMenuView.Rect r, boolean picked, boolean hover, double time) {
        int top = frame(c, t, r, 0xFF040203, picked, hover);
        c.batch(() -> veins(c, r.x() + 4, top + 4, r.width() - 8, 150, 0xFF6E0C16, 0xFF34070E, time));
        // The one eye: black, a white point burning in it, red veins out of it.
        int cx = r.x() + r.width() / 2;
        int cy = top + 78;
        c.batch(() -> {
            AyatoMenuView.organicPanel(c, cx, cy, 64, 46, 0xFF0A0506, 0xFF3A0810);
            for (int k = 0; k < 8; k++) {
                double a = Math.PI * 2.0 * k / 8 + 0.3;
                AyatoMenuView.drawVein(c, cx + (int)(Math.cos(a) * 26), cy + (int)(Math.sin(a) * 20), cx + (int)(Math.cos(a + 0.4) * 36),
                    cy + (int)(Math.sin(a + 0.4) * 28), cx + (int)(Math.cos(a - 0.2) * 50), cy + (int)(Math.sin(a - 0.2) * 36), 0xFFB0101E, 1);
            }
            AyatoMenuView.fillCircle(c, cx, cy, 24, 0xFF1A0E10);
            AyatoMenuView.fillCircle(c, cx, cy, 18, 0xFF030203);
            double pulse = 0.5 + 0.5 * Math.sin(time * 3.0);
            AyatoMenuView.fillCircle(c, cx, cy, 8 + (int)Math.round(2 * pulse), 0x406A6A6A);
            AyatoMenuView.fillCircle(c, cx, cy, 4, 0xFFFFFFFF);
        });
        text(c, r, top, KingTheme.WAR, "WAR", "THE ONE-EYED KING", List.of(
            new String[] {"Look", "Black, a torn cape, one eye"},
            new String[] {"Rinkaku", "Six that hunt; a Black Sun"},
            new String[] {"Moves", "Centipede arms, a small Dragon"},
            new String[] {"Body", "+45% melee, +35% speed"},
            new String[] {"Health", "+80%, 25% of blows taken off"}), true);
    }

    static void peace(UiCanvas c, KingTheme t, AyatoMenuView.Rect r, boolean picked, boolean hover, boolean sealed, double time) {
        int top = frame(c, t, r, sealed ? 0xFFCDD0D6 : 0xFFF4F4F7, picked, hover);
        c.batch(() -> KingTheme.feathers(c, r.x() + 4, top + 4, r.width() - 8, r.height() - 8, 0xFFDCE0E8, 0xFFC4C9D2, time));
        // The cross blade: four points, two long, a red core and a burning centre; it turns slowly.
        int cx = r.x() + r.width() / 2;
        int cy = top + 78;
        c.batch(() -> {
            AyatoMenuView.organicPanel(c, cx, cy, 64, 46, 0xFFE6E9EE, 0xFFB4B9C4);
            double turn = Math.toRadians(30.0) + time * 0.3;
            for (int k = 0; k < 4; k++) {
                double a = turn + k * Math.PI / 2.0;
                double length = k % 2 == 0 ? 40.0 : 22.0;
                double width = length * (k % 2 == 0 ? 0.2 : 0.28);
                blade(c, cx, cy, a, length + 3, width + 2, 0xFF4E6478);
                blade(c, cx, cy, a, length, width, 0xFFE8EEF4);
                blade(c, cx, cy, a, length * 0.8, width * 0.22, 0xFFC4121F);
            }
            AyatoMenuView.fillCircle(c, cx, cy, 6, 0xFFFF2234);
            AyatoMenuView.fillCircle(c, cx, cy, 2, 0xFFFFFFFF);
        });
        text(c, r, top, KingTheme.PEACE, "PEACE", "KANEKI :RE", List.of(
            new String[] {"Look", "White armour, kagune wings"},
            new String[] {"Blades", "Six cross blades on arms"},
            new String[] {"Moves", "Graceful, long combos"},
            new String[] {"Body", "+30% melee, +40% attack speed"},
            new String[] {"Health", "+100%, 35% of blows taken off"}), false);
        if (sealed) {
            c.fill(r.x(), top + r.height() - 26, r.x() + r.width(), top + r.height() - 8, 0xE01E2026);
            c.centered("SEALED: NOT READY YET. IT IS COMING.", r.x() + r.width() / 2, top + r.height() - 21, 0xFFF0E6E0);
        }
    }

    /** The card's text as the menu sets it: the name as a header, the title under it, then label and value rows. */
    static void text(UiCanvas c, AyatoMenuView.Rect r, int top, KingTheme t, String name, String subtitle, List<String[]> rows, boolean shadow) {
        int x = r.x() + 20;
        c.centered(name, r.x() + r.width() / 2, top + 140, t.title(), shadow);
        c.centered(subtitle, r.x() + r.width() / 2, top + 154, t.header(), shadow);
        for (int i = 0; i < rows.size(); i++) {
            int y = top + 180 + i * 18;
            c.text(rows.get(i)[0], x, y, t.label(), false);
            c.text(c.fit(rows.get(i)[1], r.width() - 110), x + 80, y, t.value(), false);
        }
    }

    /** War's veins, kept inside the box {@code left, top, width, height}: they crawl in from its edges and branch. */
    static void veins(UiCanvas c, int left, int top, int width, int height, int bright, int dark, double time) {
        java.util.Random r = new java.util.Random(23);
        for (int i = 0; i < 12; i++) {
            boolean side = i % 2 == 0;
            int x0 = side ? (i % 4 == 0 ? left + 2 : left + width - 2) : left + 20 + r.nextInt(width - 40);
            int y0 = side ? top + 10 + r.nextInt(height - 20) : (i % 4 == 1 ? top + 2 : top + height - 2);
            int x1 = clamp(x0 + (side ? (x0 < left + width / 2 ? 1 : -1) * (40 + r.nextInt(70)) : r.nextInt(80) - 40), left + 2, left + width - 2);
            int y1 = clamp(y0 + (side ? r.nextInt(60) - 30 : (y0 < top + height / 2 ? 1 : -1) * (30 + r.nextInt(50))), top + 2, top + height - 2);
            int cx = clamp((x0 + x1) / 2 + r.nextInt(30) - 15, left + 2, left + width - 2);
            int cy = clamp((y0 + y1) / 2 + r.nextInt(30) - 15, top + 2, top + height - 2);
            AyatoMenuView.drawVein(c, x0, y0, cx, cy, x1, y1, (int)(time * 2.0 + i) % 5 == 0 ? bright : dark, i % 3 == 0 ? 2 : 1);
        }
    }

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** One point of a cross blade, from the centre out along angle {@code a}: a long thin diamond. */
    static void blade(UiCanvas c, int cx, int cy, double a, double length, double width, int colour) {
        double dx = Math.cos(a);
        double dy = Math.sin(a);
        double nx = -dy;
        double ny = dx;
        float[] xs = {(float)cx, (float)(cx + dx * length * 0.28 + nx * width), (float)(cx + dx * length), (float)(cx + dx * length * 0.28 - nx * width)};
        float[] ys = {(float)cy, (float)(cy + dy * length * 0.28 + ny * width), (float)(cy + dy * length), (float)(cy + dy * length * 0.28 - ny * width)};
        c.polygon(xs, ys, colour);
    }
}

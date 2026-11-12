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
 * <p>1.0.11: drawn as a page of Rinkaku's own kagune menu ({@link RinkakuMenuView}, in {@link KingTheme#RINKAKU}): the same organic board
 * with its veins, the spaced-out title over the subtitle and the hint, headers and rows in the menu's colours, and the bar held to
 * choose filling as the mastery bar does.
 */
public final class KingChoiceView {
    public static final int UI_WIDTH = UkakuMenuView.UI_WIDTH;
    public static final int UI_HEIGHT = UkakuMenuView.UI_HEIGHT;
    static final int PANEL_W = 930;
    static final int PANEL_H = 500;
    static final int CARD_W = 330;
    static final int CARD_H = 300;
    static final int CARD_Y = 66;
    /** 4.1.65: the quest menu's box (the mastery menus' size), everything inside it. */
    public static final int BOX_WIDTH = UkakuMenuView.BOX_WIDTH;
    public static final int BOX_HEIGHT = UkakuMenuView.BOX_HEIGHT;

    /** What the screen shows: the card picked (NONE: none yet), how far the bar is held (0..1), and whether Peace can be chosen. */
    public record Model(KingForm picked, double hold, boolean peaceBuilt) {
    }

    private KingChoiceView() {
    }

    /** The card's box for {@code form}. */
    static UkakuMenuView.Rect card(KingForm form) {
        int x = form == KingForm.WAR ? BOX_WIDTH / 2 - 14 - CARD_W : BOX_WIDTH / 2 + 14;
        return new UkakuMenuView.Rect(x, CARD_Y, CARD_W, CARD_H);
    }

    /**
     * Draws the screen and returns its clickable areas: "card:war", "card:peace", "choose", "later". 4.1.65 (the owner: "new Menus: Every
     * single menu in the game is to mimic and look like the quest menu, and be as interactable as it is, Dialog, mastery menus ALL OF THEM"):
     * the quest menu's box ({@link #BOX_WIDTH} x {@link #BOX_HEIGHT}) over the world, its title, lines and rule; the two cards as they were;
     * the bar held to choose as the quest menu's button, filling.
     */
    public static Map<String, UkakuMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        KingTheme t = KingTheme.RINKAKU;
        Map<String, UkakuMenuView.Rect> hits = new LinkedHashMap<>();
        int centerX = BOX_WIDTH / 2;
        int ruleY = CARD_Y - 8;
        QuestStyle.backdrop(c, 0, 0, BOX_WIDTH, BOX_HEIGHT, time, BOX_WIDTH - 200, 6, 186, ruleY - 12);
        c.push();
        c.translate(16, 11);
        c.scale(1.4F);
        c.text("What kind of King will you become?", 0, 0, QuestJournalView.TITLE, true);
        c.pop();
        c.text("THE DRAGON IS SPENT", 16, 29, QuestJournalView.SUBTITLE, false);
        c.text("Click a card, then hold the button (or Enter) to become it: once, and never again", 16, 41, QuestFrame.MUTED, false);
        c.batch(() -> {
            for (int i = 0; i < 40; i++) {
                float f = i / 39.0F;
                int alpha = Math.round(190 * (1.0F - Math.abs(f - 0.5F) * 2.0F));
                int x0 = 12 + Math.round((BOX_WIDTH - 24) * i / 40.0F);
                c.fill(x0, ruleY, x0 + (BOX_WIDTH - 24) / 40 + 1, ruleY + 1, alpha << 24 | 0xE8323E);
            }
        });
        for (KingForm form : new KingForm[] {KingForm.WAR, KingForm.PEACE}) {
            UkakuMenuView.Rect r = card(form);
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
        // The button held to choose: the quest menu's, filling red as it is held.
        boolean enabled = model.picked() != KingForm.NONE;
        UkakuMenuView.Rect bar = new UkakuMenuView.Rect(centerX - 160, CARD_Y + CARD_H + 10, 320, 22);
        boolean over = bar.contains(mouseX, mouseY);
        boolean holding = model.hold() > 0.0;
        int lift = enabled && over && !holding ? -1 : 0;
        int edge = !enabled ? QuestStyle.GREY_DARK : over || holding ? QuestStyle.RED : QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED,
            0.15F + 0.15F * (float)Math.sin(time * 2.5));
        int fill = !enabled ? 0xFF140A0C : over ? QuestJournalView.ROW_PICKED : QuestJournalView.ROW;
        c.batch(() -> {
            QuestStyle.box(c, bar.x(), bar.y() + lift, bar.width(), bar.height(), fill, edge);
            if (holding) {
                int filled = (int)Math.round((bar.width() - 2) * Math.min(1.0, model.hold()));
                c.fill(bar.x() + 1, bar.y() + 1, bar.x() + 1 + filled, bar.y() + bar.height() - 1, QuestStyle.mix(QuestJournalView.ROW, QuestStyle.RED, 0.6F));
            }
            if (enabled && over) {
                c.fill(bar.x() + 4, bar.y() + bar.height() + 1, bar.x() + bar.width() - 4, bar.y() + bar.height() + 2, QuestStyle.RED);
            }
        });
        String label = !enabled ? "PICK A CARD" : holding ? "BECOMING " + model.picked().displayName().toUpperCase() + "...  "
            + Math.round(100.0 * Math.min(1.0, model.hold())) + "%" : "HOLD TO BECOME " + model.picked().displayName().toUpperCase();
        c.centered(c.fit(label, bar.width() - 8), centerX, bar.y() + 8 + lift, !enabled ? QuestStyle.GREY : over || holding ? QuestStyle.WHITE
            : QuestFrame.VALUE);
        hits.put("choose", bar);
        UkakuMenuView.Rect later = new UkakuMenuView.Rect(centerX - 50, BOX_HEIGHT - 20, 100, 12);
        boolean overLater = later.contains(mouseX, mouseY);
        c.centered("Decide later", centerX, later.y() + 2, overLater ? QuestFrame.VALUE : QuestFrame.MUTED);
        if (overLater) {
            int half = c.width("Decide later") / 2;
            c.fill(centerX - half, later.y() + 11, centerX + half, later.y() + 12, QuestStyle.RED);
        }
        hits.put("later", later);
        return hits;
    }

    /** A card's frame: the menu's button fill, its edge lit when hovered and red when picked. */
    static int frame(UiCanvas c, KingTheme t, UkakuMenuView.Rect r, int fill, boolean picked, boolean hover) {
        int lift = picked ? 2 : 0;
        int top = r.y() - lift;
        int edge = picked ? t.bar() : hover ? t.buttonHover() : t.innerEdge();
        c.fill(r.x() - 2, top - 2, r.x() + r.width() + 2, top + r.height() + 2, edge);
        c.fill(r.x(), top, r.x() + r.width(), top + r.height(), fill);
        return top;
    }

    static void war(UiCanvas c, KingTheme t, UkakuMenuView.Rect r, boolean picked, boolean hover, double time) {
        int top = frame(c, t, r, 0xFF040203, picked, hover);
        c.batch(() -> veins(c, r.x() + 4, top + 4, r.width() - 8, 150, 0xFF6E0C16, 0xFF34070E, time));
        // The one eye: black, a white point burning in it, red veins out of it.
        warEye(c, r.x() + r.width() / 2, top + 78, time);
        text(c, r, top, KingTheme.WAR, "WAR", "THE ONE-EYED KING", WAR_ROWS, true);
    }

    /** 4.1.58: War's card's rows (the Rinkaku menu shows them too). */
    static final List<String[]> WAR_ROWS = List.of(
        new String[] {"Look", "Black, a torn cape, one eye"},
        new String[] {"Rinkaku", "Six that hunt; a Black Sun"},
        new String[] {"Moves", "Centipede arms, a small Dragon"},
        new String[] {"Body", "+45% melee, +35% speed"},
        new String[] {"Toughness", "25% of blows off, then half the rest (2.0: no extra health)"});
    /** 4.1.58: Peace's card's rows. */
    static final List<String[]> PEACE_ROWS = List.of(
        new String[] {"Look", "White armour, kagune wings"},
        new String[] {"Blades", "Six cross blades on arms"},
        new String[] {"Moves", "Graceful, long combos"},
        new String[] {"Body", "+30% melee, +40% attack speed"},
        new String[] {"Toughness", "35% of blows off, then half the rest (2.0: no extra health)"});

    /** 4.1.58: War's one eye about ({@code cx}, {@code cy}): black, a white point burning in it, red veins out of it (its card's, the menu's). */
    static void warEye(UiCanvas c, int cx, int cy, double time) {
        c.batch(() -> {
            UkakuMenuView.organicPanel(c, cx, cy, 64, 46, 0xFF0A0506, 0xFF3A0810);
            for (int k = 0; k < 8; k++) {
                double a = Math.PI * 2.0 * k / 8 + 0.3;
                UkakuMenuView.drawVein(c, cx + (int)(Math.cos(a) * 26), cy + (int)(Math.sin(a) * 20), cx + (int)(Math.cos(a + 0.4) * 36),
                    cy + (int)(Math.sin(a + 0.4) * 28), cx + (int)(Math.cos(a - 0.2) * 50), cy + (int)(Math.sin(a - 0.2) * 36), 0xFFB0101E, 1);
            }
            UkakuMenuView.fillCircle(c, cx, cy, 24, 0xFF1A0E10);
            UkakuMenuView.fillCircle(c, cx, cy, 18, 0xFF030203);
            double pulse = 0.5 + 0.5 * Math.sin(time * 3.0);
            UkakuMenuView.fillCircle(c, cx, cy, 8 + (int)Math.round(2 * pulse), 0x406A6A6A);
            UkakuMenuView.fillCircle(c, cx, cy, 4, 0xFFFFFFFF);
        });
    }

    static void peace(UiCanvas c, KingTheme t, UkakuMenuView.Rect r, boolean picked, boolean hover, boolean sealed, double time) {
        int top = frame(c, t, r, sealed ? 0xFFCDD0D6 : 0xFFF4F4F7, picked, hover);
        c.batch(() -> KingTheme.feathers(c, r.x() + 4, top + 4, r.width() - 8, r.height() - 8, 0xFFDCE0E8, 0xFFC4C9D2, time));
        // The cross blade: four points, two long, a red core and a burning centre; it turns slowly.
        peaceCross(c, r.x() + r.width() / 2, top + 78, true, time);
        text(c, r, top, KingTheme.PEACE, "PEACE", "RINKAKU :RE", PEACE_ROWS, false);
        if (sealed) {
            c.fill(r.x(), top + r.height() - 26, r.x() + r.width(), top + r.height() - 8, 0xE01E2026);
            c.centered("SEALED: NOT READY YET. IT IS COMING.", r.x() + r.width() / 2, top + r.height() - 21, 0xFFF0E6E0);
        }
    }

    /** 4.1.58: Peace's cross blade about ({@code cx}, {@code cy}), turning slowly, on its pale plate if {@code plate}. */
    static void peaceCross(UiCanvas c, int cx, int cy, boolean plate, double time) {
        c.batch(() -> {
            if (plate) {
                UkakuMenuView.organicPanel(c, cx, cy, 64, 46, 0xFFE6E9EE, 0xFFB4B9C4);
            }
            double turn = Math.toRadians(30.0) + time * 0.3;
            for (int k = 0; k < 4; k++) {
                double a = turn + k * Math.PI / 2.0;
                double length = k % 2 == 0 ? 40.0 : 22.0;
                double width = length * (k % 2 == 0 ? 0.2 : 0.28);
                blade(c, cx, cy, a, length + 3, width + 2, 0xFF4E6478);
                blade(c, cx, cy, a, length, width, 0xFFE8EEF4);
                blade(c, cx, cy, a, length * 0.8, width * 0.22, 0xFFC4121F);
            }
            UkakuMenuView.fillCircle(c, cx, cy, 6, 0xFFFF2234);
            UkakuMenuView.fillCircle(c, cx, cy, 2, 0xFFFFFFFF);
        });
    }

    /** The card's text as the menu sets it: the name as a header, the title under it, then label and value rows. */
    static void text(UiCanvas c, UkakuMenuView.Rect r, int top, KingTheme t, String name, String subtitle, List<String[]> rows, boolean shadow) {
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
            UkakuMenuView.drawVein(c, x0, y0, cx, cy, x1, y1, (int)(time * 2.0 + i) % 5 == 0 ? bright : dark, i % 3 == 0 ? 2 : 1);
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

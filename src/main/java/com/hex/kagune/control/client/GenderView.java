package com.hex.kagune.control.client;

import com.hex.kagune.control.FemaleBody;
import com.hex.kagune.control.Gender;
import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.client.ui.UiCanvas;

/**
 * 1.3.9: the gender screen (pure drawing on {@link UiCanvas}), in the eye screen's colours: who you are, Male or Female, each a card
 * with the body it gives (the woman's Eto's own: smaller, slim arms, wider hips, the bust), and Done; Back returns to the eyes.
 */
public final class GenderView {
    static final String TITLE = "Who are you?";
    static final String SUBTITLE = "A woman is a little smaller, with Eto's own figure.";
    static final String HINT = "You can change it later: /kagune eyes, then Next.";

    /**
     * Where everything goes on a screen {@code width} x {@code height} (GUI pixels). 4.1.63: inside the quest menu's box ({@link #box}, a
     * small one: two cards and two buttons), the world seen round it.
     */
    public static final class Layout {
        public static final int BOX_WIDTH = 440;
        public static final int BOX_HEIGHT = 300;
        public static final int OUTSIDE = 8;
        public final int width;
        public final int height;
        public final Box box;
        public final int titleY;
        public final int subtitleY;
        public final int ruleY;
        public final Box male;
        public final Box female;
        public final Box done;
        public final Box back;
        public final int hintY;

        public Layout(int width, int height) {
            this.width = width;
            this.height = height;
            int w = Math.max(0, Math.min(BOX_WIDTH, width - 2 * OUTSIDE));
            int h = Math.max(0, Math.min(BOX_HEIGHT, height - 2 * OUTSIDE));
            int ox = (width - w) / 2;
            int oy = (height - h) / 2;
            this.box = new Box(ox, oy, w, h);
            boolean compact = h < 230;
            this.titleY = oy + (compact ? 6 : 10);
            this.subtitleY = this.titleY + 18;
            this.ruleY = this.subtitleY + 12;
            this.hintY = compact ? -1 : oy + h - 16;
            int buttonsY = (this.hintY < 0 ? oy + h - 8 : this.hintY - 6) - 20;
            int top = this.ruleY + (compact ? 5 : 8);
            int cardHeight = Math.max(60, Math.min(170, buttonsY - 10 - top));
            int cardWidth = Math.max(70, Math.min(130, (w - 48) / 2));
            int centre = ox + w / 2;
            int y = top + Math.max(0, (buttonsY - 10 - top - cardHeight) / 2);
            this.male = new Box(centre - cardWidth - 8, y, cardWidth, cardHeight);
            this.female = new Box(centre + 8, y, cardWidth, cardHeight);
            this.done = new Box(centre - 108, buttonsY, 110, 20);
            this.back = new Box(centre + 10, buttonsY, 80, 20);
        }

        /** What is under ({@code x}, {@code y}): "male", "female", "done", "back" or null. */
        public String at(double x, double y) {
            return this.male.contains(x, y) ? "male" : this.female.contains(x, y) ? "female" : this.done.contains(x, y) ? "done"
                : this.back.contains(x, y) ? "back" : null;
        }
    }

    private GenderView() {
    }

    public static void draw(UiCanvas c, Layout layout, Gender picked, String hovered, double time) {
        // 4.1.63: the quest menu's box over the world (no overlay round it), its title and line, a rule under them.
        Box frame = layout.box;
        QuestStyle.backdrop(c, frame.x(), frame.y(), frame.width(), frame.height(), time, frame.right() - 150, frame.y() + 6, 130,
            Math.max(10, layout.ruleY - frame.y() - 10));
        c.push();
        c.translate(frame.x() + 16, layout.titleY);
        c.scale(1.4F);
        c.text(TITLE, 0, 0, QuestJournalView.TITLE, true);
        c.pop();
        c.text(c.fit(SUBTITLE, frame.width() - 32), frame.x() + 16, layout.subtitleY, QuestJournalView.SUBTITLE, false);
        int ruleY = layout.ruleY;
        c.batch(() -> {
            for (int i = 0; i < 40; i++) {
                float t = i / 39.0F;
                int alpha = Math.round(190 * (1.0F - Math.abs(t - 0.5F) * 2.0F));
                int x0 = frame.x() + 12 + Math.round((frame.width() - 24) * i / 40.0F);
                c.fill(x0, ruleY, x0 + (frame.width() - 24) / 40 + 1, ruleY + 1, alpha << 24 | 0xE8323E);
            }
        });
        card(c, layout.male, "Male", false, picked == Gender.MALE, "male".equals(hovered), time);
        card(c, layout.female, "Female", true, picked == Gender.FEMALE, "female".equals(hovered), time);
        button(c, layout.done, picked.choice() ? "Done" : "Pick one", picked.choice(), picked.choice() && "done".equals(hovered));
        button(c, layout.back, "Back", false, "back".equals(hovered));
        if (layout.hintY >= 0) {
            c.centered(c.fit(HINT, frame.width() - 16), frame.centerX(), layout.hintY, QuestFrame.FAINT);
        }
    }

    /** One card: its figure, standing, breathing; its name under it; outlined red when picked. */
    private static void card(UiCanvas c, Box box, String name, boolean female, boolean picked, boolean hovered, double time) {
        // 4.1.63: a quest card: lifted under the mouse, lit red and its bar beating once picked.
        int lift = hovered && !picked ? -1 : 0;
        int edge = picked ? QuestStyle.RED : hovered ? QuestStyle.RED_DARK : QuestStyle.TILE_EDGE;
        int fill = picked ? QuestJournalView.ROW_PICKED : hovered ? QuestJournalView.ROW_HOVER : QuestJournalView.ROW;
        int top = box.y() + lift;
        c.batch(() -> {
            QuestStyle.box(c, box.x(), top, box.width(), box.height(), fill, edge);
            if (picked) {
                float pulse = 0.6F + 0.4F * (float)Math.sin(time * 4.0);
                c.fill(box.x() + 3, box.bottom() + lift - 3, box.right() - 3, box.bottom() + lift - 2, QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, pulse));
            }
        });
        int label = box.bottom() + lift - 14;
        int room = label - top - 8;
        // A player's body is 32 pixels tall (head 8, torso 12, legs 12); hers is FemaleBody.SIZE of that.
        float unit = Math.max(1.0F, (float)Math.floor(room / 34.0F));
        figure(c, box.centerX(), label - 4, unit * (female ? FemaleBody.SIZE : 1.0F), female, time,
            KakuganView.opaque(picked ? KakuganView.BONE : KakuganView.blend(KakuganView.PANEL, KakuganView.BONE, hovered ? 0.85F : 0.65F)));
        c.centered(name, box.centerX(), label, picked || hovered ? QuestStyle.WHITE : QuestFrame.MUTED);
    }

    /** The body's silhouette, its feet at ({@code x}, {@code feet}), {@code unit} GUI pixels to a skin pixel; each part its own shade. */
    static void figure(UiCanvas c, int x, int feet, float unit, boolean female, double time, int colour) {
        float breath = (float)Math.sin(time * 2.2) * 0.25F * unit;
        float legWidth = 4.0F * (female ? FemaleBody.HIPS : 1.0F);
        float legX = female ? FemaleBody.LEG_X : FemaleBody.VANILLA_LEG_X;
        float armWidth = female ? 3.0F : 4.0F;
        float hip = feet - 12.0F * unit;
        float shoulder = hip - 12.0F * unit + breath;
        float head = shoulder - 8.0F * unit;
        int rgb = colour & 0xFFFFFF;
        int legs = KakuganView.opaque(KakuganView.blend(rgb, 0x000000, 0.28F));
        int arms = KakuganView.opaque(KakuganView.blend(rgb, 0x000000, 0.16F));
        int seam = KakuganView.opaque(KakuganView.blend(rgb, 0x000000, 0.5F));
        int light = KakuganView.opaque(KakuganView.blend(rgb, 0xFFFFFF, 0.25F));
        // Legs (a seam between them), then the torso, the arms (a sliver of shadow at the body), the head.
        for (int side : new int[] {-1, 1}) {
            float centre = x + side * legX * unit;
            rect(c, centre - legWidth * unit / 2.0F, hip, centre + legWidth * unit / 2.0F, feet, legs);
        }
        rect(c, x - 0.5F, hip + unit, x + 0.5F, feet, seam);
        rect(c, x - 4.0F * unit, shoulder, x + 4.0F * unit, hip + 0.5F * unit, colour);
        for (int side : new int[] {-1, 1}) {
            float inner = x + side * 4.0F * unit;
            float outer = inner + side * armWidth * unit;
            float top = shoulder + (female ? 0.5F : 0.0F) * unit;
            rect(c, Math.min(inner, outer), top, Math.max(inner, outer), shoulder + 12.0F * unit, arms);
            rect(c, Math.min(inner, inner + side), top, Math.max(inner, inner + side), shoulder + 12.0F * unit, seam);
        }
        rect(c, x - 4.0F * unit, head, x + 4.0F * unit, shoulder, light);
        if (female) {
            // The bust, from the front: light catching its curve across the chest, a soft shade under it.
            float tip = shoulder + FemaleBody.BUST_TIP * unit;
            for (int side : new int[] {-1, 1}) {
                float cx = x + side * 1.9F * unit;
                c.polygon(new float[] {cx - 1.6F * unit, cx - 0.8F * unit, cx + 0.8F * unit, cx + 1.6F * unit, cx + 0.8F * unit, cx - 0.8F * unit},
                    new float[] {tip - 0.6F * unit, tip - 1.6F * unit, tip - 1.6F * unit, tip - 0.6F * unit, tip + 0.4F * unit, tip + 0.4F * unit}, light);
            }
            rect(c, x - 3.4F * unit, tip + 0.4F * unit, x + 3.4F * unit, tip + 0.9F * unit, arms);
        }
    }

    private static void rect(UiCanvas c, float x0, float y0, float x1, float y1, int colour) {
        c.fill(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1), colour);
    }

    /** 4.1.63: a button as the quest menu's: lifted under the mouse with a red bar under it, {@code primary} lit red. */
    private static void button(UiCanvas c, Box box, String label, boolean primary, boolean hovered) {
        int lift = hovered ? -1 : 0;
        int edge = hovered ? QuestStyle.RED : primary ? QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, 0.45F) : QuestStyle.TILE_EDGE;
        int fill = hovered || primary ? QuestJournalView.ROW_PICKED : QuestJournalView.ROW;
        c.batch(() -> {
            QuestStyle.box(c, box.x(), box.y() + lift, box.width(), box.height(), fill, edge);
            if (hovered) {
                c.fill(box.x() + 4, box.bottom() + 1, box.right() - 4, box.bottom() + 2, QuestStyle.RED);
            }
        });
        c.centered(c.fit(label, box.width() - 6), box.centerX(), box.y() + (box.height() - 8) / 2 + 1 + lift, hovered ? QuestStyle.WHITE : primary
            ? QuestFrame.VALUE : QuestFrame.MUTED);
    }
}

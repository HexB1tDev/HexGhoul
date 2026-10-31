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

    /** Where everything goes on a screen {@code width} x {@code height} (GUI pixels). */
    public static final class Layout {
        public final int width;
        public final int height;
        public final int titleY;
        public final int subtitleY;
        public final Box male;
        public final Box female;
        public final Box done;
        public final Box back;
        public final int hintY;

        public Layout(int width, int height) {
            this.width = width;
            this.height = height;
            boolean compact = height < 230;
            this.titleY = compact ? 8 : 18;
            this.subtitleY = this.titleY + 20;
            this.hintY = compact ? -1 : height - 16;
            int buttonsY = (this.hintY < 0 ? height - 8 : this.hintY - 6) - 20;
            int top = this.subtitleY + 16;
            int cardHeight = Math.max(60, Math.min(170, buttonsY - 10 - top));
            int cardWidth = Math.max(70, Math.min(130, (width - 48) / 2));
            int x = width / 2 - cardWidth - 8;
            int y = top + Math.max(0, (buttonsY - 10 - top - cardHeight) / 2);
            this.male = new Box(x, y, cardWidth, cardHeight);
            this.female = new Box(width / 2 + 8, y, cardWidth, cardHeight);
            this.done = new Box(width / 2 - 108, buttonsY, 110, 20);
            this.back = new Box(width / 2 + 10, buttonsY, 80, 20);
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
        int width = layout.width;
        int height = layout.height;
        c.fill(0, 0, width, height, 0xF0000000 | KakuganView.BACKGROUND);
        c.gradient(0, 0, width, height / 3, 0x70000000, 0x00000000);
        c.gradient(0, height * 2 / 3, width, height, 0x00000000, 0x90000000);
        c.push();
        c.translate(width / 2.0F, layout.titleY);
        c.scale(1.5F);
        c.centered(TITLE, 0, 0, KakuganView.opaque(KakuganView.BONE));
        c.pop();
        c.centered(c.fit(SUBTITLE, width - 16), width / 2, layout.subtitleY, KakuganView.opaque(KakuganView.MUTED));
        card(c, layout.male, "Male", false, picked == Gender.MALE, "male".equals(hovered), time);
        card(c, layout.female, "Female", true, picked == Gender.FEMALE, "female".equals(hovered), time);
        button(c, layout.done, picked.choice() ? "Done" : "Pick one", picked.choice(), picked.choice() && "done".equals(hovered));
        button(c, layout.back, "Back", false, "back".equals(hovered));
        if (layout.hintY >= 0) {
            c.centered(c.fit(HINT, width - 16), width / 2, layout.hintY, KakuganView.opaque(KakuganView.blend(KakuganView.BACKGROUND, KakuganView.MUTED, 0.7F)));
        }
    }

    /** One card: its figure, standing, breathing; its name under it; outlined red when picked. */
    private static void card(UiCanvas c, Box box, String name, boolean female, boolean picked, boolean hovered, double time) {
        int border = picked ? KakuganView.ACCENT : hovered ? KakuganView.BONE : KakuganView.blend(KakuganView.BACKGROUND, KakuganView.MUTED, 0.45F);
        c.fill(box.x() - 1, box.y() - 1, box.right() + 1, box.bottom() + 1, KakuganView.opaque(border));
        c.fill(box.x(), box.y(), box.right(), box.bottom(), KakuganView.opaque(picked ? KakuganView.blend(KakuganView.PANEL, KakuganView.ACCENT, 0.16F)
            : KakuganView.PANEL));
        int label = box.bottom() - 14;
        int room = label - box.y() - 8;
        // A player's body is 32 pixels tall (head 8, torso 12, legs 12); hers is FemaleBody.SIZE of that.
        float unit = Math.max(1.0F, (float)Math.floor(room / 34.0F));
        figure(c, box.centerX(), label - 4, unit * (female ? FemaleBody.SIZE : 1.0F), female, time,
            KakuganView.opaque(picked ? KakuganView.BONE : KakuganView.blend(KakuganView.PANEL, KakuganView.BONE, hovered ? 0.85F : 0.65F)));
        c.centered(name, box.centerX(), label, KakuganView.opaque(picked || hovered ? KakuganView.BONE : KakuganView.MUTED));
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

    private static void button(UiCanvas c, Box box, String label, boolean primary, boolean hovered) {
        int border = primary ? (hovered ? KakuganView.ACCENT : KakuganView.blend(KakuganView.BACKGROUND, KakuganView.ACCENT, 0.75F))
            : (hovered ? KakuganView.BONE : KakuganView.blend(KakuganView.BACKGROUND, KakuganView.MUTED, 0.45F));
        c.fill(box.x() - 1, box.y() - 1, box.right() + 1, box.bottom() + 1, KakuganView.opaque(border));
        c.fill(box.x(), box.y(), box.right(), box.bottom(), KakuganView.opaque(primary ? KakuganView.blend(KakuganView.BUTTON, KakuganView.ACCENT, 0.18F)
            : KakuganView.BUTTON));
        c.centered(c.fit(label, box.width() - 6), box.centerX(), box.y() + (box.height() - 8) / 2,
            KakuganView.opaque(hovered || primary ? KakuganView.BONE : KakuganView.MUTED));
    }
}

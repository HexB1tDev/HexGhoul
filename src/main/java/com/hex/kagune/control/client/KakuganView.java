package com.hex.kagune.control.client;

import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.kakugan.KakuganModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Draws the kakugan screen on any {@link UiCanvas} (the game's GUI or the preview tool's image): the player's own face
 * from their skin (inner layer only), the kakugan exactly as {@link KakuganModel} builds it laid over the eyes, and the
 * controls. Only the eyes move; nothing else about the face is edited.
 */
public final class KakuganView {
    static final int BACKGROUND = 0x0B0608;
    static final int PANEL = 0x130B0D;
    static final int BUTTON = 0x1A1012;
    static final int BONE = 0xE8DCCB;
    static final int MUTED = 0x9C8F84;
    static final int ACCENT = 0xE0302A;
    static final String TITLE = "YOUR KAKUGAN";
    static final String SUBTITLE = "When your kagune tears out, your eyes turn black and red.";
    static final String SUBTITLE_TWO = "Drag each eye onto the eyes of your own skin.";
    static final String HINT = "Drag an eye, or click it and use the arrow keys. Find my eyes reads them off your skin.";

    /**
     * What the screen shows this frame. {@code hovered} is the control or eye under the mouse ("mirror", "right", ...), or
     * null; {@code covered} the face cells the skin's second layer covers ({@code glass}: the see-through ones); {@code canFind}
     * whether eyes were found on the skin; {@code choosing} the "which eye?" box is open (its buttons hover as "pick-right",
     * "pick-left", "pick-both"); {@code bothAllowed} false on a path that is one-eyed by nature (Pale's): no "Both eyes"; 1.3.0:
     * {@code oneAllowed} false until Eto has made them like her: no "One eyed".
     */
    public record State(KakuganEyes eyes, int seed, boolean mirror, boolean selectedRight, String hovered, boolean dragging, double time,
                        long covered, long glass, boolean canFind, boolean choosing, boolean bothAllowed, boolean oneAllowed) {
        public State(KakuganEyes eyes, int seed, boolean mirror, boolean selectedRight, String hovered, boolean dragging, double time,
                     long covered, long glass, boolean canFind, boolean choosing, boolean bothAllowed) {
            this(eyes, seed, mirror, selectedRight, hovered, dragging, time, covered, glass, canFind, choosing, bothAllowed, true);
        }

        public State(KakuganEyes eyes, int seed, boolean mirror, boolean selectedRight, String hovered, boolean dragging, double time,
                     long covered, long glass, boolean canFind, boolean choosing) {
            this(eyes, seed, mirror, selectedRight, hovered, dragging, time, covered, glass, canFind, choosing, true);
        }

        public State(KakuganEyes eyes, int seed, boolean mirror, boolean selectedRight, String hovered, boolean dragging, double time,
                     long covered, long glass, boolean canFind) {
            this(eyes, seed, mirror, selectedRight, hovered, dragging, time, covered, glass, canFind, false, true);
        }
    }

    static final String CHOICE_TITLE = "Which is your ghoul eye?";
    static final String CHOICE_SUBTITLE = "The other stays human.";

    private KakuganView() {
    }

    public static void draw(UiCanvas c, KakuganScreenLayout layout, State state) {
        int width = layout.width;
        int height = layout.height;
        c.fill(0, 0, width, height, 0xF0000000 | BACKGROUND);
        c.gradient(0, 0, width, height / 3, 0x70000000, 0x00000000);
        c.gradient(0, height * 2 / 3, width, height, 0x00000000, 0x90000000);
        c.push();
        c.translate(width / 2.0F, layout.titleY);
        c.scale(1.5F);
        c.centered(TITLE, 0, 0, opaque(BONE));
        c.pop();
        if (layout.subtitleLines > 1) {
            c.centered(c.fit(SUBTITLE, width - 2 * KakuganScreenLayout.MARGIN), width / 2, layout.subtitleY, opaque(MUTED));
            c.centered(c.fit(SUBTITLE_TWO, width - 2 * KakuganScreenLayout.MARGIN), width / 2, layout.subtitleY + KakuganScreenLayout.LINE,
                opaque(MUTED));
        } else {
            c.centered(c.fit(SUBTITLE_TWO, width - 2 * KakuganScreenLayout.MARGIN), width / 2, layout.subtitleY, opaque(MUTED));
        }

        Box face = layout.face;
        c.fill(face.x() - 3, face.y() - 3, face.right() + 3, face.bottom() + 3, opaque(blend(BACKGROUND, ACCENT, 0.35F)));
        c.fill(face.x() - 2, face.y() - 2, face.right() + 2, face.bottom() + 2, opaque(PANEL));
        c.face(face.x(), face.y(), face.width());
        // As others will see it: the kakugan on the face, the skin's second layer over it (glass blending over the ghoul
        // eyes), and the kakugan on that layer's opaque pixels over an eye.
        State shown = previewed(state);
        kakugan(c, layout, shown, false);
        c.hat(face.x(), face.y(), face.width(), null);
        kakugan(c, layout, shown, true);
        c.batch(() -> {
            for (int i = 1; i < 8; i++) {
                int x = face.x() + i * layout.cell;
                int y = face.y() + i * layout.cell;
                c.fill(x, face.y(), x + 1, face.bottom(), 0x22FFFFFF);
                c.fill(face.x(), y, face.right(), y + 1, 0x22FFFFFF);
            }
        });
        for (boolean right : new boolean[] {true, false}) {
            Box eye = layout.eye(state.eyes(), right);
            boolean selected = state.selectedRight() == right;
            boolean hovered = (right ? "right" : "left").equals(state.hovered());
            if (!state.choosing() && (selected || hovered)) {
                int colour = selected ? opaque(state.dragging() ? BONE : ACCENT) : opaque(blend(BACKGROUND, BONE, 0.6F));
                outline(c, eye.x() - 2, eye.y() - 2, eye.right() + 2, eye.bottom() + 2, colour);
            }
        }

        button(c, layout.mirror, "Mirror eyes: " + (state.mirror() ? "ON" : "OFF"), state.mirror(), "mirror".equals(state.hovered()));
        button(c, layout.eyeHeight, "Eye height: " + state.eyes().height() + " px", false, "height".equals(state.hovered()));
        button(c, layout.iris, "Iris: " + (state.eyes().irisInside() ? "inner side" : "outer side"), false, "iris".equals(state.hovered()));
        KakuganEyes eyes = state.eyes();
        String one = !eyes.oneEyed() ? "OFF" : eyes.ghoul() == KakuganEyes.LEFT ? "left eye" : "right eye";
        if (state.oneAllowed()) {
            button(c, layout.oneEye, "One eyed: " + one, eyes.oneEyed(), "one".equals(state.hovered()));
        } else {
            button(c, layout.oneEye, "One eyed: Eto only", false, false);
        }
        button(c, layout.outer, state.canFind() ? "Find my eyes" : "No eyes found on skin", false, state.canFind() && "outer".equals(state.hovered()));
        button(c, layout.reset, "Reset", false, "reset".equals(state.hovered()));
        button(c, layout.done, "Next", true, "done".equals(state.hovered()));
        button(c, layout.later, "Later", false, "later".equals(state.hovered()));
        if (layout.hintY >= 0) {
            c.centered(c.fit(HINT, width - 2 * KakuganScreenLayout.MARGIN), width / 2, layout.hintY, opaque(blend(BACKGROUND, MUTED, 0.7F)));
        }
        if (state.choosing()) {
            choice(c, layout, state);
        }
    }

    /** While the "which eye?" box is open, the face shows the pick under the mouse: that eye turning alone (or both). */
    static State previewed(State s) {
        String hovered = s.hovered();
        if (!s.choosing() || hovered == null || !hovered.startsWith("pick-")) {
            return s;
        }
        int ghoul = "pick-right".equals(hovered) ? KakuganEyes.RIGHT : "pick-left".equals(hovered) ? KakuganEyes.LEFT : KakuganEyes.BOTH;
        return new State(s.eyes().withGhoul(ghoul), s.seed(), s.mirror(), s.selectedRight(), hovered, s.dragging(), s.time(), s.covered(),
            s.glass(), s.canFind(), true, s.bothAllowed(), s.oneAllowed());
    }

    /**
     * The "which eye?" box (One eyed): the screen dimmed under it, the two eyes' buttons laid out as the eyes are on the face
     * picture (the player's right eye on its left), the hovered one outlined on the face, and a way back to both eyes.
     */
    private static void choice(UiCanvas c, KakuganScreenLayout layout, State state) {
        // Everything dimmed but the face, which stays lit so the pick shows on it.
        Box face = layout.face;
        int dim = 0xB0000000;
        c.fill(0, 0, layout.width, face.y() - 3, dim);
        c.fill(0, face.bottom() + 3, layout.width, layout.height, dim);
        c.fill(0, face.y() - 3, face.x() - 3, face.bottom() + 3, dim);
        c.fill(face.right() + 3, face.y() - 3, layout.width, face.bottom() + 3, dim);
        KakuganEyes eyes = state.eyes();
        String hovered = state.hovered();
        // The face's eyes stay visible through the dimming, the one under the mouse ringed.
        for (boolean right : new boolean[] {true, false}) {
            Box eye = layout.eye(eyes, right);
            boolean pointed = (right ? "pick-right" : "pick-left").equals(hovered);
            boolean chosen = eyes.oneEyed() && eyes.ghoulEye(right);
            if (pointed || chosen) {
                outline(c, eye.x() - 2, eye.y() - 2, eye.right() + 2, eye.bottom() + 2, opaque(pointed ? ACCENT : blend(BACKGROUND, BONE, 0.6F)));
            }
        }
        Box box = layout.choice;
        c.fill(box.x() - 1, box.y() - 1, box.right() + 1, box.bottom() + 1, opaque(blend(BACKGROUND, ACCENT, 0.6F)));
        c.fill(box.x(), box.y(), box.right(), box.bottom(), opaque(PANEL));
        c.centered(c.fit(CHOICE_TITLE, box.width() - 8), box.centerX(), box.y() + 6, opaque(BONE));
        c.centered(c.fit(CHOICE_SUBTITLE, box.width() - 8), box.centerX(), box.y() + 17, opaque(MUTED));
        button(c, layout.choiceRight, "Right eye", eyes.ghoul() == KakuganEyes.RIGHT, "pick-right".equals(hovered));
        button(c, layout.choiceLeft, "Left eye", eyes.ghoul() == KakuganEyes.LEFT, "pick-left".equals(hovered));
        if (state.bothAllowed()) {
            button(c, layout.choiceBoth, "Both eyes", !eyes.oneEyed(), "pick-both".equals(hovered));
        }
    }

    /**
     * The kakugan over the face ({@code outer}: only its copies on the skin's second layer, laid back onto the face square):
     * solid parts in depth order, then the glow (approximated as light laid over the skin).
     */
    private static void kakugan(UiCanvas c, KakuganScreenLayout layout, State state, boolean outer) {
        List<KakuganModel.Quad> solid = new ArrayList<>();
        List<KakuganModel.Quad> glow = new ArrayList<>();
        for (KakuganModel.Quad quad : KakuganModel.build(state.eyes(), state.seed(), state.time(), 1.0F, true, KakuganModel.DEFAULT_LAYER,
            state.covered() & ~state.glass())) {
            if (quad.outer() == outer) {
                (quad.glow() ? glow : solid).add(outer ? flattened(quad) : quad);
            }
        }
        // Further off the face is drawn later (z is -4 minus the lift, so larger z first).
        solid.sort(Comparator.comparingDouble(q -> -q.xyz()[2]));
        c.batch(() -> {
            for (KakuganModel.Quad quad : solid) {
                polygon(c, layout, quad, quad.argb()[0]);
            }
            for (KakuganModel.Quad quad : glow) {
                polygon(c, layout, quad, light(quad.argb()[0]));
            }
        });
    }

    /** A quad on the second layer, laid back onto the face's own pixels (the screen shows both layers on one square). */
    private static KakuganModel.Quad flattened(KakuganModel.Quad quad) {
        float[] xyz = quad.xyz().clone();
        for (int i = 0; i < 4; i++) {
            xyz[i * 3] = (float)(xyz[i * 3] / KakuganModel.OUTER_SCALE);
            xyz[i * 3 + 1] = (float)(-4.0 + (xyz[i * 3 + 1] + 4.0) / KakuganModel.OUTER_SCALE);
        }
        return new KakuganModel.Quad(xyz, quad.nx(), quad.ny(), quad.nz(), quad.argb(), quad.glow(), true);
    }

    private static void polygon(UiCanvas c, KakuganScreenLayout layout, KakuganModel.Quad quad, int argb) {
        if ((argb >>> 24) < 3) {
            return;
        }
        float[] v = quad.xyz();
        float[] xs = {layout.guiX(v[0]), layout.guiX(v[3]), layout.guiX(v[6]), layout.guiX(v[9])};
        float[] ys = {layout.guiY(v[1]), layout.guiY(v[4]), layout.guiY(v[7]), layout.guiY(v[10])};
        c.polygon(xs, ys, argb);
    }

    /** Added light (its brightness is its strength) as a translucent colour a GUI can blend. */
    static int light(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        if (max == 0) {
            return 0;
        }
        float scale = 255.0F / max;
        return Math.round(max * 0.8F) << 24 | Math.round(r * scale) << 16 | Math.round(g * scale) << 8 | Math.round(b * scale);
    }

    private static void button(UiCanvas c, Box box, String label, boolean primary, boolean hovered) {
        int border = primary ? (hovered ? ACCENT : blend(BACKGROUND, ACCENT, 0.75F)) : (hovered ? BONE : blend(BACKGROUND, MUTED, 0.45F));
        c.fill(box.x() - 1, box.y() - 1, box.right() + 1, box.bottom() + 1, opaque(border));
        c.fill(box.x(), box.y(), box.right(), box.bottom(), opaque(primary ? blend(BUTTON, ACCENT, 0.18F) : BUTTON));
        c.centered(c.fit(label, box.width() - 6), box.centerX(), box.y() + (box.height() - 8) / 2, opaque(hovered || primary ? BONE : MUTED));
    }

    private static void outline(UiCanvas c, int x0, int y0, int x1, int y1, int argb) {
        c.fill(x0, y0, x1, y0 + 1, argb);
        c.fill(x0, y1 - 1, x1, y1, argb);
        c.fill(x0, y0, x0 + 1, y1, argb);
        c.fill(x1 - 1, y0, x1, y1, argb);
    }

    static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    static int blend(int bg, int fg, float alpha) {
        return PathSilhouette.blend(bg, fg, alpha);
    }
}

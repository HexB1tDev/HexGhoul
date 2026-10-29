package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.UiCanvas;

/**
 * The ghoul's flesh reserve, drawn where the food bar was (pure, so the preview tool draws it too; 3.0.3). It is a rinkaku tail
 * full of blood: the tip on the left (it empties from there, as the food bar did), the root in a kakuhou on the right, a kakugan
 * that watches the player and blinks now and then. Fed, the blood is bright, chevron scales drift down it toward the tip and RC
 * cells glint through it; the hungrier the ghoul, the harder the tip writhes and the wider the pupil. Eating raw flesh flashes
 * it white; healing leaves a pale ghost of what it cost, fading where the blood was. Low, the kakuhou beats like a heart; empty,
 * the tail is a dry, cracked husk and the eye goes dull.
 */
public final class ReserveBarView {
    /** The food bar's footprint: ten 9 px icons 8 px apart. */
    public static final int WIDTH = 81;
    public static final int HEIGHT = 9;
    /** The tail runs over columns 0 to TAIL - 1, the kakuhou over the rest. */
    static final int TAIL = 72;
    private static final float BULB_X = 76.5F;
    private static final float BULB_Y = 4.5F;
    private static final float BULB_R = 4.6F;

    private ReserveBarView() {
    }

    /**
     * One frame. {@code fill} and {@code ghost} are fractions of a full reserve ({@code ghost} at least {@code fill}: what it was
     * before a heal drew on it, shown at {@code ghostAlpha}); {@code flash} (0 to 1) is a meal going in; {@code hunger} (0 to 1)
     * how starved the ghoul is; {@code time} in ticks.
     */
    public record Frame(float fill, float ghost, float ghostAlpha, float flash, float hunger, double time) {
    }

    public static void draw(UiCanvas c, int x, int y, Frame frame) {
        c.batch(() -> {
            tail(c, x, y, frame);
            kakuhou(c, x, y, frame);
        });
    }

    private static void tail(UiCanvas c, int x, int y, Frame f) {
        float fill = clamp(f.fill());
        boolean empty = fill <= 0.001F;
        float front = (1.0F - fill) * TAIL;
        float ghostFront = (1.0F - Math.max(fill, clamp(f.ghost()))) * TAIL;
        float beat = beat(f);
        float writhe = 0.55F + 1.3F * clamp(f.hunger());
        int flow = (int)Math.floor(f.time() * 0.22);
        for (int i = 0; i < TAIL; i++) {
            float u = i / (float)(TAIL - 1);
            float half = 1.1F + 2.65F * (float)Math.pow(u, 0.8);
            float centre = BULB_Y + writhe * (float)Math.sin(i * 0.17 - f.time() * 0.11) * (float)Math.pow(1.0F - u, 1.6);
            int top = Math.round(centre - half);
            int bottom = Math.round(centre + half) - 1;
            top = Math.max(0, top);
            bottom = Math.min(HEIGHT - 1, bottom);
            if (bottom < top) {
                continue;
            }
            int middle = Math.round(centre - 0.5F);
            for (int r = top; r <= bottom; r++) {
                int colour;
                if (r == top || r == bottom || i == 0) {
                    colour = rim(r == top && r != bottom, beat, empty);
                } else if (empty) {
                    boolean crack = ((i * 7 + r * 13) % 11 == 0) || ((i + r * 3) % 17 == 0 && i % 2 == 0);
                    colour = crack ? 0xFF0A0607 : (TAIL - 1 - i) % 6 == 0 ? 0xFF2C2326 : 0xFF221A1D;
                } else if (i + meniscus(r, f) >= front) {
                    colour = blood(i, r, top, bottom, middle, flow, i + meniscus(r, f) - front < 1.0F && fill < 0.999F);
                    if (f.flash() > 0.0F) {
                        colour = blend(colour, 0xFFFFF4F4, 0.6F * clamp(f.flash()));
                    }
                } else {
                    colour = (TAIL - 1 - i) % 6 == 0 ? 0xFF2A0B13 : 0xFF13050A;
                    if (f.ghostAlpha() > 0.0F && i + meniscus(r, f) >= ghostFront) {
                        colour = blend(colour, 0xFFFFC2CE, 0.85F * clamp(f.ghostAlpha()));
                    }
                }
                c.fill(x + i, y + r, x + i + 1, y + r + 1, colour);
            }
        }
        if (!empty) {
            cells(c, x, y, f, front);
        }
    }

    /** How far each row's edge of the blood leans (a sloshing, ragged meniscus while the tail is not full). */
    private static float meniscus(int row, Frame f) {
        return (float)Math.sin(row * 1.9 + f.time() * 0.35) * 0.9F;
    }

    /** The blood: lit from above, dark beneath, chevron scales drifting toward the tip, its edge glowing. */
    private static int blood(int i, int r, int top, int bottom, int middle, int flow, boolean edge) {
        if (edge) {
            return 0xFFFF8E9E;
        }
        if (r == top + 1 && bottom - top > 2) {
            return 0xFFFF5A6E;
        }
        if (r == bottom - 1 && bottom - top > 2) {
            return 0xFF7A0717;
        }
        boolean scale = Math.floorMod(i - Math.abs(r - middle) + flow, 5) == 0;
        return scale ? 0xFFA20E26 : 0xFFD01632;
    }

    /** The membrane: lit on top, shadowed below, glowing red with each heartbeat when the reserve runs low. */
    private static int rim(boolean upper, float beat, boolean empty) {
        int base = empty ? (upper ? 0xFF3A2E31 : 0xFF151012) : upper ? 0xFF55101C : 0xFF1C0409;
        return beat > 0.0F ? blend(base, 0xFFFF3048, beat) : base;
    }

    /** Bright RC cells carried down the blood toward the tip. */
    private static void cells(UiCanvas c, int x, int y, Frame f, float front) {
        float length = TAIL - 1 - front;
        if (length < 3.0F) {
            return;
        }
        int[] rows = {4, 3, 5, 4};
        for (int k = 0; k < rows.length; k++) {
            float speed = 0.16F + 0.07F * k;
            float along = (float)((f.time() * speed + k * 17.3) % length);
            int i = Math.round(TAIL - 2 - along);
            if (i > front + 1.0F && i < TAIL - 1) {
                c.fill(x + i, y + rows[k], x + i + 1, y + rows[k] + 1, k % 2 == 0 ? 0xFFFFD2DA : 0xFFFFA8B6);
            }
        }
    }

    /** The kakuhou on the tail's root, and its kakugan. */
    private static void kakuhou(UiCanvas c, int x, int y, Frame f) {
        float fill = clamp(f.fill());
        float hunger = clamp(f.hunger());
        float beat = beat(f);
        boolean empty = fill <= 0.001F;
        // Where the eye looks: mostly down the tail, now and then at the player.
        float look = (float)Math.sin(f.time() * 0.021) > 0.6F ? 0.6F : -0.7F;
        float irisX = BULB_X + look;
        float iris = 2.35F;
        int irisColour = empty ? 0xFF4A2026 : blend(0xFF8A1424, 0xFFFF2440, fill);
        // The pupil: a slit when fed, wide and round when starving.
        float pupilW = 0.45F + 1.05F * hunger;
        float pupilH = 1.6F + 0.4F * hunger;
        double blink = f.time() % 170.0;
        float lid = blink < 5.0 ? (float)(blink < 2.5 ? blink / 2.5 : (5.0 - blink) / 2.5) : 0.0F;
        for (int col = TAIL; col < WIDTH; col++) {
            for (int r = 0; r < HEIGHT; r++) {
                float dx = col + 0.5F - BULB_X;
                float dy = r + 0.5F - BULB_Y;
                float d = (float)Math.sqrt(dx * dx + dy * dy);
                if (d > BULB_R) {
                    continue;
                }
                int colour;
                if (d > BULB_R - 1.0F) {
                    colour = rim(dy < 0.0F, beat, empty);
                } else if (Math.abs(dy) > (1.0F - lid) * 3.6F) {
                    // The lids closing over a blink.
                    colour = dy < 0.0F ? 0xFF4A0E18 : 0xFF2A0710;
                } else {
                    float ix = col + 0.5F - irisX;
                    float id = (float)Math.sqrt(ix * ix + dy * dy);
                    if (Math.abs(ix) < pupilW && Math.abs(dy) < pupilH) {
                        colour = 0xFF050102;
                    } else if (id < iris) {
                        colour = id < iris - 0.9F ? irisColour : blend(irisColour, 0xFF000000, 0.35F);
                        if (!empty && col == Math.round(irisX - 1.5F) && r == 3) {
                            colour = 0xFFFFC4CC;
                        }
                    } else {
                        boolean vein = hunger > 0.35F && ((col + r) % 3 == 0) && Math.abs(dy) < 2.0F && Math.abs(dx) > 2.0F;
                        colour = vein ? blend(0xFF0C0306, 0xFFB01428, Math.min(1.0F, (hunger - 0.35F) * 2.5F)) : 0xFF0C0306;
                    }
                }
                c.fill(x + col, y + r, x + col + 1, y + r + 1, colour);
            }
        }
    }

    /** A double heartbeat below a third full, faster and stronger the emptier, 0 otherwise. */
    private static float beat(Frame f) {
        float fill = clamp(f.fill());
        if (fill >= 0.3F) {
            return 0.0F;
        }
        float urgency = 1.0F - fill / 0.3F;
        double period = 30.0 - 12.0 * urgency;
        double phase = f.time() % period / period;
        float lub = (float)Math.max(0.0, 1.0 - Math.abs(phase - 0.06) / 0.06);
        float dub = (float)Math.max(0.0, 1.0 - Math.abs(phase - 0.24) / 0.06) * 0.7F;
        return Math.max(lub, dub) * (0.35F + 0.55F * urgency);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    static int blend(int from, int to, float t) {
        float k = clamp(t);
        int a = Math.round(((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * k);
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * k);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * k);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * k);
        return a << 24 | r << 16 | g << 8 | b;
    }
}

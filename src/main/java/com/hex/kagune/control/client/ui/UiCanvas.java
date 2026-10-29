package com.hex.kagune.control.client.ui;

/**
 * What a screen draws on, without Minecraft types: the game hands its GUI to a screen through {@code GuiCanvas},
 * and the preview tool hands it an image, so the same drawing code makes both (only the font differs).
 */
public interface UiCanvas {
    void fill(int x0, int y0, int x1, int y1, int argb);

    void gradient(int x0, int y0, int x1, int y1, int top, int bottom);

    void text(String text, int x, int y, int argb, boolean shadow);

    /** Text centred on {@code centerX}, with a shadow (as Minecraft draws centred strings). */
    void centered(String text, int centerX, int y, int argb);

    /** 1.0.10: centred, with or without the shadow (dark text on a pale board reads clean only without). */
    default void centered(String text, int centerX, int y, int argb, boolean shadow) {
        if (shadow) {
            this.centered(text, centerX, y, argb);
        } else {
            this.text(text, centerX - this.width(text) / 2, y, argb, false);
        }
    }

    int width(String text);

    /** The viewing player's face from their skin, inner layer only (the layer the kakugan replaces), {@code size} square. */
    void face(int x, int y, int size);

    /**
     * The skin's outer (second) layer of the face over the same square, see-through where it is empty, leaving out
     * {@code hole} ({column from, row from, column to, row to} in skin pixels, the "to" ends exclusive) or nothing if null.
     */
    void hat(int x, int y, int size, int[] hole);

    void push();

    void translate(float x, float y);

    void scale(float factor);

    void pop();

    /** Many small fills flushed together. */
    void batch(Runnable draw);

    /** Fills a convex polygon one pixel row at a time (GUI fills are axis-aligned rectangles). */
    default void polygon(float[] xs, float[] ys, int argb) {
        float top = Float.MAX_VALUE;
        float bottom = -Float.MAX_VALUE;
        for (float y : ys) {
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
        }
        int n = xs.length;
        for (int row = (int)Math.floor(top); row < (int)Math.ceil(bottom); row++) {
            float centre = row + 0.5F;
            float left = Float.MAX_VALUE;
            float right = -Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                if (ys[i] <= centre && ys[j] > centre || ys[j] <= centre && ys[i] > centre) {
                    float x = xs[i] + (centre - ys[i]) / (ys[j] - ys[i]) * (xs[j] - xs[i]);
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                }
            }
            int from = Math.round(left);
            int to = Math.round(right);
            if (to > from) {
                this.fill(from, row, to, row + 1, argb);
            }
        }
    }

    /** {@code text}, shortened with "..." until it fits {@code width} pixels. */
    default String fit(String text, int width) {
        if (this.width(text) <= width) {
            return text;
        }
        String shortened = text;
        while (!shortened.isEmpty() && this.width(shortened + "...") > width) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened.stripTrailing() + "...";
    }
}

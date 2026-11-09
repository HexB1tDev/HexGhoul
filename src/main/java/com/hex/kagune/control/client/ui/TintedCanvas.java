package com.hex.kagune.control.client.ui;

import com.hex.kagune.control.KaguneTint;

/**
 * 3.6.6: a canvas that draws everything drawn on it in another colour ({@link KaguneTint#recolour}: a red palette's hues turned to
 * {@code target}'s, its lights and darks kept), over another canvas. Potential Unlock's purple RC bar is the bar as it always was, drawn
 * through this.
 */
public final class TintedCanvas implements UiCanvas {
    private final UiCanvas canvas;
    private final int target;

    public TintedCanvas(UiCanvas canvas, int target) {
        this.canvas = canvas;
        this.target = target;
    }

    private int tint(int argb) {
        return KaguneTint.recolour(argb, this.target);
    }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int argb) {
        this.canvas.fill(x0, y0, x1, y1, this.tint(argb));
    }

    @Override
    public void gradient(int x0, int y0, int x1, int y1, int top, int bottom) {
        this.canvas.gradient(x0, y0, x1, y1, this.tint(top), this.tint(bottom));
    }

    @Override
    public void text(String text, int x, int y, int argb, boolean shadow) {
        this.canvas.text(text, x, y, this.tint(argb), shadow);
    }

    @Override
    public void centered(String text, int centerX, int y, int argb) {
        this.canvas.centered(text, centerX, y, this.tint(argb));
    }

    @Override
    public int width(String text) {
        return this.canvas.width(text);
    }

    @Override
    public void face(int x, int y, int size) {
        this.canvas.face(x, y, size);
    }

    @Override
    public void hat(int x, int y, int size, int[] hole) {
        this.canvas.hat(x, y, size, hole);
    }

    @Override
    public void push() {
        this.canvas.push();
    }

    @Override
    public void translate(float x, float y) {
        this.canvas.translate(x, y);
    }

    @Override
    public void scale(float factor) {
        this.canvas.scale(factor);
    }

    @Override
    public void pop() {
        this.canvas.pop();
    }

    @Override
    public void batch(Runnable draw) {
        this.canvas.batch(draw);
    }
}

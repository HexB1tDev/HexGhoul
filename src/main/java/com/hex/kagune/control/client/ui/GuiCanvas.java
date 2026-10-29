package com.hex.kagune.control.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** The game's GUI as a {@link UiCanvas}. {@code skin} is the viewing player's skin texture (for {@link #face}). */
public final class GuiCanvas implements UiCanvas {
    private final GuiGraphics graphics;
    private final Font font;
    private final ResourceLocation skin;

    public GuiCanvas(GuiGraphics graphics, Font font, ResourceLocation skin) {
        this.graphics = graphics;
        this.font = font;
        this.skin = skin;
    }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int argb) {
        this.graphics.m_280509_(x0, y0, x1, y1, argb);
    }

    @Override
    public void gradient(int x0, int y0, int x1, int y1, int top, int bottom) {
        this.graphics.m_280024_(x0, y0, x1, y1, top, bottom);
    }

    @Override
    public void text(String text, int x, int y, int argb, boolean shadow) {
        this.graphics.m_280056_(this.font, text, x, y, argb, shadow);
    }

    @Override
    public void centered(String text, int centerX, int y, int argb) {
        this.graphics.m_280137_(this.font, text, centerX, y, argb);
    }

    @Override
    public int width(String text) {
        return this.font.m_92895_(text);
    }

    /** The face of the skin texture (u 8..16, v 8..16 of 64 x 64): the inner layer only, never the outer (hat) layer. */
    @Override
    public void face(int x, int y, int size) {
        if (this.skin != null) {
            this.graphics.m_280411_(this.skin, x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
        }
    }

    /** The outer layer of the face (u 40..48, v 8..16), blended so its empty pixels stay see-through, minus the hole. */
    @Override
    public void hat(int x, int y, int size, int[] hole) {
        if (this.skin == null) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (hole == null) {
            this.hatPart(x, y, size, 0, 0, 8, 8);
        } else {
            this.hatPart(x, y, size, 0, 0, 8, hole[1]);
            this.hatPart(x, y, size, 0, hole[3], 8, 8);
            this.hatPart(x, y, size, 0, hole[1], hole[0], hole[3]);
            this.hatPart(x, y, size, hole[2], hole[1], 8, hole[3]);
        }
        RenderSystem.disableBlend();
    }

    /** Columns c0..c1 and rows r0..r1 (skin pixels) of the outer face layer. */
    private void hatPart(int x, int y, int size, int c0, int r0, int c1, int r1) {
        if (c1 <= c0 || r1 <= r0) {
            return;
        }
        int cell = size / 8;
        this.graphics.m_280411_(this.skin, x + c0 * cell, y + r0 * cell, (c1 - c0) * cell, (r1 - r0) * cell, 40.0F + c0, 8.0F + r0, c1 - c0, r1 - r0,
            64, 64);
    }

    @Override
    public void push() {
        this.graphics.m_280168_().m_85836_();
    }

    @Override
    public void translate(float x, float y) {
        this.graphics.m_280168_().m_85837_(x, y, 0.0);
    }

    @Override
    public void scale(float factor) {
        this.graphics.m_280168_().m_85841_(factor, factor, 1.0F);
    }

    @Override
    public void pop() {
        PoseStack pose = this.graphics.m_280168_();
        pose.m_85849_();
    }

    @Override
    public void batch(Runnable draw) {
        this.graphics.m_286007_(draw);
    }
}

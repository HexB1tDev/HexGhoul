package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.client.PathCards.Card;
import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.network.C2SChoosePath;
import com.hex.kagune.control.network.ControlNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The starting screen: choose a kagune path, one card per choosable path ({@link PathCards}; docs/AYATO_PATH.md,
 * "Starting GUI"). Pick a card with the mouse or arrow keys, then hold the Awaken button (or Enter) to confirm,
 * because the choice is permanent. The client only asks; the server checks and stores the choice.
 */
public final class PathChoiceScreen extends Screen {
    private static final int BACKGROUND = 0x0B0608;
    private static final int CARD = 0x130B0D;
    private static final int BUTTON = 0x1A1012;
    private static final int BONE = 0xE8DCCB;
    private static final int MUTED = 0x9C8F84;
    private static final long HOLD_MILLIS = 900L;
    private static final String TITLE = "YOUR RC CELLS ARE WAKING UP";
    private static final String SUBTITLE = "What tears out of your back decides how you fight.";
    private static final String SUBTITLE_SHORT = "Your kagune decides how you fight.";
    private static final String PERMANENT = "This choice is permanent. Choose with your gut.";
    private static final String HINT = "Arrows: choose   Hold Enter: awaken   Esc: decide later";

    private final List<Card> cards = PathCards.choosable();
    private final long openedAt = System.currentTimeMillis();
    private KagunePath selected = KagunePath.NONE;
    private long holdStarted = -1L;
    private boolean holdByMouse;
    private int holdKey = -1;
    private boolean sent;
    private String tooltip;
    private int tooltipX;
    private int tooltipY;

    public PathChoiceScreen() {
        super(Component.m_237115_("screen.hexkagune.path_choice"));
    }

    private PathChoiceLayout layout() {
        return PathChoiceLayout.of(this.f_96543_, this.f_96544_);
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.sent && (PathClient.self().chosen() || !KaguneClientState.self().unlocked())) {
            // Chosen elsewhere (an operator, another window) or the kagune was taken away: nothing left to decide.
            Minecraft.m_91087_().m_91152_(null);
            return;
        }
        PathChoiceLayout layout = this.layout();
        Font font = Minecraft.m_91087_().f_91062_;
        long now = System.currentTimeMillis();
        double time = (now - this.openedAt) / 1000.0;
        int width = this.f_96543_;
        int height = this.f_96544_;
        this.tooltip = null;

        graphics.m_280509_(0, 0, width, height, 0xF0000000 | BACKGROUND);
        graphics.m_280024_(0, 0, width, height / 3, 0x70000000, 0x00000000);
        graphics.m_280024_(0, height * 2 / 3, width, height, 0x00000000, 0x90000000);

        PoseStack pose = graphics.m_280168_();
        pose.m_85836_();
        pose.m_85837_(width / 2.0, layout.titleY, 0.0);
        pose.m_85841_(1.5F, 1.5F, 1.0F);
        graphics.m_280137_(font, TITLE, 0, 0, opaque(BONE));
        pose.m_85849_();
        String subtitle = font.m_92895_(SUBTITLE) <= width - 2 * PathChoiceLayout.MARGIN ? SUBTITLE : SUBTITLE_SHORT;
        graphics.m_280137_(font, subtitle, width / 2, layout.subtitleY, opaque(MUTED));
        if (layout.subtitleLines > 1) {
            graphics.m_280137_(font, fit(font, PERMANENT, width - 2 * PathChoiceLayout.MARGIN), width / 2,
                layout.subtitleY + PathChoiceLayout.LINE, opaque(MUTED));
        }

        for (int i = 0; i < this.cards.size() && i < layout.cards.size(); i++) {
            this.drawCard(graphics, font, layout, this.cards.get(i), layout.cards.get(i), mouseX, mouseY, time);
        }
        this.drawButtons(graphics, font, layout, mouseX, mouseY, now);
        if (layout.hintY >= 0) {
            graphics.m_280137_(font, fit(font, HINT, width - 2 * PathChoiceLayout.MARGIN), width / 2, layout.hintY,
                opaque(PathSilhouette.blend(BACKGROUND, MUTED, 0.7F)));
        }
        if (this.tooltip != null) {
            int tooltipWidth = font.m_92895_(this.tooltip) + 6;
            int x = Math.max(2, Math.min(width - tooltipWidth - 2, this.tooltipX - tooltipWidth / 2));
            int y = Math.max(2, this.tooltipY - 14);
            graphics.m_280509_(x - 1, y - 1, x + tooltipWidth + 1, y + 12, opaque(PathSilhouette.blend(CARD, MUTED, 0.5F)));
            graphics.m_280509_(x, y, x + tooltipWidth, y + 11, opaque(CARD));
            graphics.m_280056_(font, this.tooltip, x + 3, y + 2, opaque(BONE), false);
        }

        if (this.holdStarted >= 0L) {
            if (this.holdByMouse && !layout.confirm.contains(mouseX, mouseY)) {
                this.cancelHold();
            } else if (now - this.holdStarted >= HOLD_MILLIS) {
                this.awaken();
            }
        }
    }

    private void drawCard(GuiGraphics graphics, Font font, PathChoiceLayout layout, Card card, Box slot, int mouseX, int mouseY,
                          double time) {
        boolean hovered = slot.contains(mouseX, mouseY);
        boolean chosen = this.selected == card.path();
        int lift = chosen ? 2 : hovered ? 1 : 0;
        Box box = new Box(slot.x(), slot.y() - lift, slot.width(), slot.height());
        if (chosen) {
            graphics.m_280509_(box.x() - 3, box.y() - 3, box.right() + 3, box.bottom() + 3, 0x40000000 | card.accent());
        }
        int border = chosen ? card.accent() : PathSilhouette.blend(CARD, card.accent(), hovered ? 0.7F : 0.35F);
        graphics.m_280509_(box.x() - 1, box.y() - 1, box.right() + 1, box.bottom() + 1, opaque(border));
        graphics.m_280509_(box.x(), box.y(), box.right(), box.bottom(), opaque(CARD));

        PathChoiceLayout.Card inner = layout.card(box);
        float intensity = chosen ? 1.0F : hovered ? 0.7F : 0.35F;
        List<PathSilhouette.Mark> marks = card.silhouette().marks(inner.silhouette(), time, intensity, chosen, CARD);
        // Thousands of small fills: batch them into one flush instead of one draw call each.
        graphics.m_286007_(() -> {
            for (PathSilhouette.Mark mark : marks) {
                draw(graphics, mark);
            }
        });

        graphics.m_280056_(font, fit(font, card.name(), inner.textWidth()), inner.textX(), inner.nameY(),
            opaque(chosen || hovered ? card.accent() : PathSilhouette.blend(CARD, card.accent(), 0.8F)), true);
        graphics.m_280056_(font, fit(font, card.type(), inner.textWidth()), inner.textX(), inner.typeY(), opaque(MUTED), false);
        int bullets = Math.min(inner.maxBullets(), card.bullets().size());
        for (int i = 0; i < bullets; i++) {
            graphics.m_280056_(font, fit(font, "• " + card.bullets().get(i), inner.textWidth()), inner.textX(),
                inner.bulletsY() + i * PathChoiceLayout.LINE, opaque(PathSilhouette.blend(CARD, BONE, chosen ? 1.0F : 0.8F)), false);
        }
        if (inner.tagsY() >= 0) {
            graphics.m_280056_(font, fit(font, card.tags(), inner.textWidth()), inner.textX(), inner.tagsY(),
                opaque(PathSilhouette.blend(CARD, card.accent(), 0.75F)), false);
        }
        if (inner.stripY() >= 0) {
            this.drawStrip(graphics, font, card, inner, mouseX, mouseY);
        }
    }

    /** Stage nodes along the bottom of a card, evenly spaced, with mastery thresholds; hovering a node names it. */
    private void drawStrip(GuiGraphics graphics, Font font, Card card, PathChoiceLayout.Card inner, int mouseX, int mouseY) {
        List<String> names = card.stageNames();
        List<Integer> thresholds = card.stageMastery();
        int left = inner.textX() + 6;
        int right = inner.textX() + inner.textWidth() - 6;
        int y = inner.stripY() + 2;
        int line = opaque(PathSilhouette.blend(CARD, card.accent(), 0.4F));
        graphics.m_280509_(left, y, right, y + 1, line);
        PoseStack pose = graphics.m_280168_();
        for (int i = 0; i < names.size(); i++) {
            int x = names.size() == 1 ? left : left + (right - left) * i / (names.size() - 1);
            boolean over = Math.abs(mouseX - x) <= 4 && mouseY >= y - 4 && mouseY <= y + 12;
            int node = over ? card.accent() : PathSilhouette.blend(CARD, card.accent(), 0.75F);
            graphics.m_280509_(x - (over ? 2 : 1), y - (over ? 2 : 1), x + (over ? 3 : 2), y + (over ? 3 : 2), opaque(node));
            pose.m_85836_();
            pose.m_85837_(x, y + 5, 0.0);
            pose.m_85841_(0.7F, 0.7F, 1.0F);
            graphics.m_280137_(font, Integer.toString(thresholds.get(i)), 0, 0, opaque(over ? BONE : MUTED));
            pose.m_85849_();
            if (over) {
                this.tooltip = names.get(i) + " (mastery " + thresholds.get(i) + ")";
                this.tooltipX = x;
                this.tooltipY = y;
            }
        }
    }

    private void drawButtons(GuiGraphics graphics, Font font, PathChoiceLayout layout, int mouseX, int mouseY, long now) {
        Box confirm = layout.confirm;
        boolean enabled = this.selected.chosen();
        int accent = enabled ? PathCards.of(this.selected).accent() : MUTED;
        boolean hovered = confirm.contains(mouseX, mouseY);
        int border = !enabled ? PathSilhouette.blend(BACKGROUND, MUTED, 0.35F)
            : hovered || this.holdStarted >= 0L ? accent : PathSilhouette.blend(BACKGROUND, accent, 0.7F);
        graphics.m_280509_(confirm.x() - 1, confirm.y() - 1, confirm.right() + 1, confirm.bottom() + 1, opaque(border));
        graphics.m_280509_(confirm.x(), confirm.y(), confirm.right(), confirm.bottom(), opaque(BUTTON));
        String label;
        if (!enabled) {
            label = "Choose a path";
        } else if (this.holdStarted >= 0L) {
            float progress = Math.min(1.0F, (now - this.holdStarted) / (float)HOLD_MILLIS);
            graphics.m_280509_(confirm.x(), confirm.y(), confirm.x() + Math.round(confirm.width() * progress), confirm.bottom(),
                opaque(PathSilhouette.blend(BUTTON, accent, 0.6F)));
            label = "Awakening...";
        } else {
            label = "Hold to awaken: " + PathCards.of(this.selected).awaken();
        }
        graphics.m_280137_(font, fit(font, label, confirm.width() - 8), confirm.centerX(), confirm.y() + 6,
            opaque(enabled ? BONE : MUTED));

        Box later = layout.later;
        boolean overLater = later.contains(mouseX, mouseY);
        graphics.m_280137_(font, "Decide later", later.centerX(), later.y() + 2, opaque(overLater ? BONE : MUTED));
        if (overLater) {
            int half = font.m_92895_("Decide later") / 2;
            graphics.m_280509_(later.centerX() - half, later.y() + 11, later.centerX() + half, later.y() + 12, opaque(MUTED));
        }
    }

    /** Draws a tapered stroke as overlapping squares (opaque colours, so overlaps never darken) or a disc as rows. */
    private static void draw(GuiGraphics graphics, PathSilhouette.Mark mark) {
        if (mark instanceof PathSilhouette.Stroke stroke) {
            float dx = stroke.x1() - stroke.x0();
            float dy = stroke.y1() - stroke.y0();
            float thinnest = Math.max(0.6F, Math.min(stroke.t0(), stroke.t1()));
            int steps = Math.max(1, (int)Math.ceil(Math.sqrt(dx * dx + dy * dy) / Math.max(0.75F, thinnest * 0.6F)));
            int color = opaque(stroke.rgb());
            for (int i = 0; i <= steps; i++) {
                float t = i / (float)steps;
                float half = (stroke.t0() + (stroke.t1() - stroke.t0()) * t) / 2.0F;
                float x = stroke.x0() + dx * t;
                float y = stroke.y0() + dy * t;
                graphics.m_280509_(Math.round(x - half), Math.round(y - half), Math.max(Math.round(x - half) + 1, Math.round(x + half)),
                    Math.max(Math.round(y - half) + 1, Math.round(y + half)), color);
            }
        } else if (mark instanceof PathSilhouette.Shape shape) {
            fillPolygon(graphics, shape.x(), shape.y(), shape.argb());
        } else if (mark instanceof PathSilhouette.Disc disc) {
            int color = opaque(disc.rgb());
            int radius = Math.max(1, Math.round(disc.r()));
            for (int dy = -radius; dy < radius; dy++) {
                float row = dy + 0.5F;
                int half = Math.round((float)Math.sqrt(Math.max(0.0F, disc.r() * disc.r() - row * row)));
                if (half > 0 || radius == 1) {
                    int cx = Math.round(disc.cx());
                    int cy = Math.round(disc.cy());
                    graphics.m_280509_(cx - Math.max(half, 1), cy + dy, cx + Math.max(half, 1), cy + dy + 1, color);
                }
            }
        }
    }

    /** Fills a small convex polygon one pixel row at a time (GUI fills are axis-aligned rectangles). */
    private static void fillPolygon(GuiGraphics graphics, float[] xs, float[] ys, int argb) {
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
                graphics.m_280509_(from, row, to, row + 1, argb);
            }
        }
    }

    /** {@code text}, shortened with "..." until it fits {@code width} pixels. */
    private static String fit(Font font, String text, int width) {
        if (font.m_92895_(text) <= width) {
            return text;
        }
        String shortened = text;
        while (!shortened.isEmpty() && font.m_92895_(shortened + "...") > width) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened.stripTrailing() + "...";
    }

    private void select(KagunePath path) {
        if (this.selected != path) {
            this.cancelHold();
            this.selected = path;
        }
    }

    /** Moves the selection one card left or right; the first press picks the outermost card on that side (as with two cards). */
    private void step(int direction) {
        int index = -1;
        for (int i = 0; i < this.cards.size(); i++) {
            if (this.cards.get(i).path() == this.selected) {
                index = i;
            }
        }
        int last = this.cards.size() - 1;
        int next = index < 0 ? (direction > 0 ? last : 0) : Math.max(0, Math.min(last, index + direction));
        this.select(this.cards.get(next).path());
    }

    private void startHold(boolean mouse, int key) {
        if (this.selected.chosen() && this.holdStarted < 0L && !this.sent) {
            this.holdStarted = System.currentTimeMillis();
            this.holdByMouse = mouse;
            this.holdKey = key;
        }
    }

    private void cancelHold() {
        this.holdStarted = -1L;
        this.holdKey = -1;
    }

    private void awaken() {
        this.sent = true;
        this.cancelHold();
        ControlNetwork.CHANNEL.sendToServer(new C2SChoosePath(this.selected));
        Minecraft.m_91087_().m_91152_(null);
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button != 0 || this.sent) {
            return false;
        }
        PathChoiceLayout layout = this.layout();
        int card = layout.cardAt(mouseX, mouseY);
        if (card >= 0 && card < this.cards.size()) {
            this.select(this.cards.get(card).path());
        } else if (layout.confirm.contains(mouseX, mouseY)) {
            this.startHold(true, -1);
        } else if (layout.later.contains(mouseX, mouseY)) {
            Minecraft.m_91087_().m_91152_(null);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean m_6348_(double mouseX, double mouseY, int button) {
        if (button == 0 && this.holdByMouse && this.holdStarted >= 0L) {
            this.cancelHold();
            return true;
        }
        return false;
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 263, 65, 265, 87 -> this.step(-1); // Left, A, Up, W
            case 262, 68, 264, 83 -> this.step(1);  // Right, D, Down, S
            case 257, 335, 32 -> this.startHold(false, keyCode); // Enter, keypad Enter, Space
            default -> {
                return super.m_7933_(keyCode, scanCode, modifiers); // Esc: decide later
            }
        }
        return true;
    }

    @Override
    public boolean m_7920_(int keyCode, int scanCode, int modifiers) {
        if (!this.holdByMouse && this.holdStarted >= 0L && keyCode == this.holdKey) {
            this.cancelHold();
            return true;
        }
        return false;
    }
}

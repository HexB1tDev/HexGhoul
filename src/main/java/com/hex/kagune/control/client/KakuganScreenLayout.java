package com.hex.kagune.control.client;

import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.kakugan.KakuganEyes;

/**
 * Geometry of the kakugan screen in GUI-scaled pixels (pure, tested at every GUI size): the player's own face drawn
 * big as a grid of skin pixels, the placement controls beside it (below it on narrow screens), Done / Later, and the
 * "which eye?" box the One eyed control opens. 4.1.62: all of it inside the quest menu's box ({@link #box}), the world seen round it.
 */
public final class KakuganScreenLayout {
    public static final int MARGIN = 12;
    public static final int LINE = 10;
    public static final int CONTROL_WIDTH = 128;
    public static final int CONTROL_HEIGHT = 18;
    public static final int GAP = 6;
    public static final int BUTTON_HEIGHT = 20;
    /** Narrower screens put the controls under the face. */
    public static final int SIDE_BY_SIDE_FROM = 380;
    public static final int COMPACT_BELOW_HEIGHT = 300;
    public static final int MIN_CELL = 5;
    public static final int MAX_CELL = 36;
    /** 4.1.62: the quest menu's box (the other menus' size at most) and the room left round it on a small screen. */
    public static final int BOX_WIDTH = UkakuMenuView.BOX_WIDTH;
    public static final int BOX_HEIGHT = UkakuMenuView.BOX_HEIGHT;
    public static final int OUTSIDE = 8;

    public final int width;
    public final int height;
    public final boolean sideBySide;
    public final boolean compact;
    public final int titleY;
    public final int subtitleY;
    public final int subtitleLines;
    /** 4.1.62: the box everything is in, and the rule under its header. */
    public final Box box;
    public final int ruleY;
    /** The face's frame (a tile round it). */
    public final Box tile;
    /** The face (8 x 8 skin pixels of {@link #cell} GUI pixels each). */
    public final Box face;
    public final int cell;
    public final Box mirror;
    public final Box eyeHeight;
    public final Box iris;
    /** "One eyed" (3.1.0): only one eye turns; opens {@link #choice}. */
    public final Box oneEye;
    /** "Find my eyes": put them where the eyes are on the skin. */
    public final Box outer;
    public final Box reset;
    public final Box done;
    public final Box later;
    /** Top of the hint line, or -1 when the screen is too short for it. */
    public final int hintY;
    /**
     * The "which eye?" box over the screen and its buttons: the eye on the left of the face picture (the player's right eye),
     * the one on its right (the left eye), and both eyes again.
     */
    public final Box choice;
    public final Box choiceRight;
    public final Box choiceLeft;
    public final Box choiceBoth;

    private KakuganScreenLayout(int width, int height) {
        this.width = width;
        this.height = height;
        // 4.1.62: the quest menu's box, as big as the screen lets it be (up to the menus' own size), everything inside it.
        int w = Math.max(0, Math.min(BOX_WIDTH, width - 2 * OUTSIDE));
        int h = Math.max(0, Math.min(BOX_HEIGHT, height - 2 * OUTSIDE));
        int ox = (width - w) / 2;
        int oy = (height - h) / 2;
        this.box = new Box(ox, oy, w, h);
        this.compact = h < COMPACT_BELOW_HEIGHT;
        this.sideBySide = w >= SIDE_BY_SIDE_FROM;
        this.titleY = oy + (this.compact ? 6 : 10);
        this.subtitleY = this.titleY + 18;
        this.subtitleLines = this.compact ? 1 : 2;
        this.ruleY = this.subtitleY + this.subtitleLines * LINE + (this.compact ? 2 : 4);
        int contentTop = this.ruleY + (this.compact ? 5 : 8);
        this.hintY = this.compact ? -1 : oy + h - 6 - LINE;
        int buttonsY = (this.hintY < 0 ? oy + h - 6 : this.hintY - 4) - BUTTON_HEIGHT;
        int contentBottom = buttonsY - (this.compact ? 6 : 10);
        int area = Math.max(0, contentBottom - contentTop);

        int doneWidth = 110;
        int laterWidth = 80;
        int buttonsX = ox + (w - (doneWidth + 8 + laterWidth)) / 2;
        this.done = new Box(buttonsX, buttonsY, doneWidth, BUTTON_HEIGHT);
        this.later = new Box(buttonsX + doneWidth + 8, buttonsY, laterWidth, BUTTON_HEIGHT);

        if (this.sideBySide) {
            int columnHeight = 6 * CONTROL_HEIGHT + 5 * GAP;
            this.cell = clamp(Math.min((w - 2 * MARGIN - CONTROL_WIDTH - 2 * GAP - 8) / 8, (area - 6) / 8), MIN_CELL, MAX_CELL);
            int size = 8 * this.cell;
            int x = ox + (w - (size + 2 * GAP + 8 + CONTROL_WIDTH)) / 2;
            int y = contentTop + (area - size) / 2;
            this.face = new Box(x, y, size, size);
            int cx = x + size + 2 * GAP + 8;
            int cy = y + (size - columnHeight) / 2;
            this.mirror = new Box(cx, cy, CONTROL_WIDTH, CONTROL_HEIGHT);
            this.eyeHeight = new Box(cx, cy + (CONTROL_HEIGHT + GAP), CONTROL_WIDTH, CONTROL_HEIGHT);
            this.iris = new Box(cx, cy + 2 * (CONTROL_HEIGHT + GAP), CONTROL_WIDTH, CONTROL_HEIGHT);
            this.oneEye = new Box(cx, cy + 3 * (CONTROL_HEIGHT + GAP), CONTROL_WIDTH, CONTROL_HEIGHT);
            this.outer = new Box(cx, cy + 4 * (CONTROL_HEIGHT + GAP), CONTROL_WIDTH, CONTROL_HEIGHT);
            this.reset = new Box(cx, cy + 5 * (CONTROL_HEIGHT + GAP), CONTROL_WIDTH, CONTROL_HEIGHT);
        } else {
            int controls = 3 * CONTROL_HEIGHT + 8;
            this.cell = clamp(Math.min((w - 2 * MARGIN - 6) / 8, (area - controls - GAP - 6) / 8), MIN_CELL, MAX_CELL);
            int size = 8 * this.cell;
            int y = contentTop + 3 + Math.max(0, (area - 6 - (size + GAP + controls)) / 2);
            this.face = new Box(ox + (w - size) / 2, y, size, size);
            int controlWidth = Math.min(CONTROL_WIDTH, (w - 2 * MARGIN - 4) / 2);
            int left = ox + (w - (2 * controlWidth + 4)) / 2;
            int top = y + size + GAP + 3;
            this.mirror = new Box(left, top, controlWidth, CONTROL_HEIGHT);
            this.eyeHeight = new Box(left + controlWidth + 4, top, controlWidth, CONTROL_HEIGHT);
            this.iris = new Box(left, top + CONTROL_HEIGHT + 4, controlWidth, CONTROL_HEIGHT);
            this.outer = new Box(left + controlWidth + 4, top + CONTROL_HEIGHT + 4, controlWidth, CONTROL_HEIGHT);
            this.reset = new Box(left, top + 2 * (CONTROL_HEIGHT + 4), controlWidth, CONTROL_HEIGHT);
            this.oneEye = new Box(left + controlWidth + 4, top + 2 * (CONTROL_HEIGHT + 4), controlWidth, CONTROL_HEIGHT);
        }
        // The face's frame: a tile round it, three pixels out.
        this.tile = new Box(this.face.x() - 3, this.face.y() - 3, this.face.width() + 6, this.face.height() + 6);
        // The "which eye?" box goes where the controls are, so the face (and the eyes it asks about) stays in sight: over the
        // column beside it, or under it on narrow screens.
        int choiceHeight = 84;
        int choiceWidth;
        int choiceX;
        int choiceY;
        if (this.sideBySide) {
            choiceX = this.tile.right() + GAP;
            choiceWidth = Math.min(236, ox + w - 6 - choiceX);
            choiceY = this.face.y() + (this.face.height() - choiceHeight) / 2;
        } else {
            choiceWidth = Math.min(236, w - 2 * MARGIN);
            choiceX = ox + (w - choiceWidth) / 2;
            choiceY = this.tile.bottom() + 4;
        }
        choiceY = Math.max(oy + 4, Math.min(choiceY, oy + h - 4 - choiceHeight));
        this.choice = new Box(choiceX, choiceY, choiceWidth, choiceHeight);
        int half = (choiceWidth - 3 * 8) / 2;
        this.choiceRight = new Box(choiceX + 8, choiceY + 30, half, BUTTON_HEIGHT);
        this.choiceLeft = new Box(choiceX + 16 + half, choiceY + 30, half, BUTTON_HEIGHT);
        this.choiceBoth = new Box(choiceX + 8, choiceY + 56, choiceWidth - 16, BUTTON_HEIGHT);
    }

    public static KakuganScreenLayout of(int width, int height) {
        return new KakuganScreenLayout(width, height);
    }

    /** The face pixel (column, row) under a point, or null outside the face. */
    public int[] cellAt(double x, double y) {
        if (!this.face.contains(x, y)) {
            return null;
        }
        return new int[] {Math.min(7, (int)((x - this.face.x()) / this.cell)), Math.min(7, (int)((y - this.face.y()) / this.cell))};
    }

    /** Where an eye sits on the drawn face. */
    public Box eye(KakuganEyes eyes, boolean right) {
        return new Box(this.face.x() + eyes.column(right) * this.cell, this.face.y() + eyes.row(right) * this.cell, KakuganEyes.WIDTH * this.cell,
            eyes.height() * this.cell);
    }

    /** GUI x of a head-model x (pixels, -4..4) and GUI y of a head-model y (-8..0). */
    public float guiX(float headX) {
        return this.face.x() + (headX + 4.0F) * this.cell;
    }

    public float guiY(float headY) {
        return this.face.y() + (headY + 8.0F) * this.cell;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

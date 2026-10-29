package com.hex.kagune.control.kakugan;

/**
 * Where a player's eyes are on their skin, so the kakugan lands on them: positions on the 8x8 face texture (columns
 * 0..7 left to right as the face is seen from the front, rows 0..7 top to bottom). Each eye is two pixels wide (the
 * white that turns black, and the iris that turns red) and one or two pixels tall. The right eye is the one on the left
 * of the face texture (the player's own right). Pure and validated, so a bad packet or tag can never place an eye off
 * the face.
 *
 * @param rightX     left column of the right eye (0..4)
 * @param rightY     top row of the right eye
 * @param leftX      left column of the left eye (at least two columns right of the right eye, at most 6)
 * @param leftY      top row of the left eye
 * @param height     eye height in pixels (1 or 2)
 * @param irisInside the iris is the pixel nearer the nose (most skins); otherwise it is the outer one
 * @param clearOuter unused since 3.0.0 (it cleared the skin's outer layer round the eyes; now the kakugan is drawn on the
 *                   outer layer wherever it covers an eye, see {@link KakuganSkin}); kept so saved placements still read
 * @param ghoul      which eyes turn (3.1.0): {@link #BOTH}, or one eyed: only the {@link #RIGHT} or only the {@link #LEFT}
 *                   (the player's own right and left), the other stays human
 */
public record KakuganEyes(int rightX, int rightY, int leftX, int leftY, int height, boolean irisInside, boolean clearOuter, int ghoul) {
    public static final int BOTH = 0;
    public static final int RIGHT = 1;
    public static final int LEFT = 2;

    /** Both eyes turning (every placement before 3.1.0). */
    public KakuganEyes(int rightX, int rightY, int leftX, int leftY, int height, boolean irisInside, boolean clearOuter) {
        this(rightX, rightY, leftX, leftY, height, irisInside, clearOuter, BOTH);
    }

    /** Steve's eyes and most skins: two by two, irises toward the nose, on rows 4 and 5, nothing over them. */
    public static final KakuganEyes DEFAULT = new KakuganEyes(1, 4, 5, 4, 2, true, false);
    public static final int FACE = 8;
    public static final int WIDTH = 2;
    /** Version 1 (before 3.1.0) had no ghoul eye: it reads as both eyes. */
    private static final int VERSION = 2;

    /** The same eyes moved onto the face: every field clamped so both eyes fit and never overlap. */
    public KakuganEyes validated() {
        int h = this.height <= 1 ? 1 : 2;
        int rx = clamp(this.rightX, 0, FACE - 2 * WIDTH);
        int lx = clamp(this.leftX, rx + WIDTH, FACE - WIDTH);
        return new KakuganEyes(rx, clamp(this.rightY, 0, FACE - h), lx, clamp(this.leftY, 0, FACE - h), h, this.irisInside, this.clearOuter,
            clamp(this.ghoul, BOTH, LEFT));
    }

    /** The right eye moved to ({@code x}, {@code y}); with {@code mirror}, the left eye follows as its mirror image. */
    public KakuganEyes withRight(int x, int y, boolean mirror) {
        int rx = clamp(x, 0, mirror ? FACE / 2 - WIDTH : FACE - 2 * WIDTH);
        return mirror ? new KakuganEyes(rx, y, FACE - WIDTH - rx, y, this.height, this.irisInside, this.clearOuter, this.ghoul).validated()
            : new KakuganEyes(rx, y, Math.max(this.leftX, rx + WIDTH), this.leftY, this.height, this.irisInside, this.clearOuter, this.ghoul).validated();
    }

    /** The left eye moved to ({@code x}, {@code y}); with {@code mirror}, the right eye follows as its mirror image. */
    public KakuganEyes withLeft(int x, int y, boolean mirror) {
        int lx = clamp(x, mirror ? FACE / 2 : WIDTH, FACE - WIDTH);
        return mirror ? new KakuganEyes(FACE - WIDTH - lx, y, lx, y, this.height, this.irisInside, this.clearOuter, this.ghoul).validated()
            : new KakuganEyes(Math.min(this.rightX, lx - WIDTH), this.rightY, lx, y, this.height, this.irisInside, this.clearOuter, this.ghoul).validated();
    }

    public KakuganEyes withHeight(int height) {
        return new KakuganEyes(this.rightX, this.rightY, this.leftX, this.leftY, height, this.irisInside, this.clearOuter, this.ghoul).validated();
    }

    public KakuganEyes withIrisInside(boolean inside) {
        return new KakuganEyes(this.rightX, this.rightY, this.leftX, this.leftY, this.height, inside, this.clearOuter, this.ghoul).validated();
    }

    public KakuganEyes withClearOuter(boolean over) {
        return new KakuganEyes(this.rightX, this.rightY, this.leftX, this.leftY, this.height, this.irisInside, over, this.ghoul).validated();
    }

    /** One eyed ({@link #RIGHT} or {@link #LEFT}) or {@link #BOTH}. */
    public KakuganEyes withGhoul(int ghoul) {
        return new KakuganEyes(this.rightX, this.rightY, this.leftX, this.leftY, this.height, this.irisInside, this.clearOuter, ghoul).validated();
    }

    /** True if this eye (the player's {@code right} or left) turns into a kakugan. */
    public boolean ghoulEye(boolean right) {
        return this.ghoul == BOTH || (this.ghoul == RIGHT) == right;
    }

    /** One eyed: only one eye turns. */
    public boolean oneEyed() {
        return this.ghoul != BOTH;
    }

    /** True when the two eyes mirror each other across the middle of the face. */
    public boolean mirrored() {
        return this.rightY == this.leftY && this.rightX + this.leftX == FACE - WIDTH;
    }

    /** Column of an eye's iris (the pixel that turns red). */
    public int irisColumn(boolean right) {
        int x = right ? this.rightX : this.leftX;
        return right == this.irisInside ? x + 1 : x;
    }

    /** Column of an eye's white (the pixel that turns black). */
    public int scleraColumn(boolean right) {
        int x = right ? this.rightX : this.leftX;
        return right == this.irisInside ? x : x + 1;
    }

    public int column(boolean right) {
        return right ? this.rightX : this.leftX;
    }

    public int row(boolean right) {
        return right ? this.rightY : this.leftY;
    }

    /** One int for the profile NBT and the network: a version tag and every field in a few bits. */
    public int pack() {
        KakuganEyes e = this.validated();
        return VERSION << 28 | e.ghoul << 15 | (e.clearOuter ? 1 : 0) << 14 | (e.irisInside ? 1 : 0) << 13 | (e.height - 1) << 12 | e.leftY << 9
            | e.leftX << 6 | e.rightY << 3 | e.rightX;
    }

    /** Anything unreadable (another version tag) gives {@link #DEFAULT}; anything readable is validated (version 1: both eyes). */
    public static KakuganEyes unpack(int packed) {
        int version = packed >>> 28;
        if (version != VERSION && version != 1) {
            return DEFAULT;
        }
        int ghoul = version == 1 ? BOTH : packed >> 15 & 3;
        return new KakuganEyes(packed & 7, packed >> 3 & 7, packed >> 6 & 7, packed >> 9 & 7, (packed >> 12 & 1) + 1, (packed >> 13 & 1) != 0, (packed >> 14 & 1) != 0,
            ghoul).validated();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

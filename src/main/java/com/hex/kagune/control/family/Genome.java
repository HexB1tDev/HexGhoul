package com.hex.kagune.control.family;

import com.hex.kagune.control.Gender;
import java.util.Random;

/**
 * 1.3.10: what a child is, decided the night it is conceived (pure): a boy or a girl; a ghoul if either parent is; its kakugan (both eyes,
 * as a ghoul's are, or, if a parent is one-eyed, a good chance of just the one); its hair the blend of both parents' hair; its eyes one
 * parent's; its skin between theirs; a seed for the rest of its looks.
 */
public record Genome(Gender gender, boolean ghoul, int kakugan, int hair, int eyes, int skin, long seed) {
    /** Its kakugan: none (human), its right eye, its left, or both. */
    public static final int NONE = 0;
    public static final int RIGHT = 1;
    public static final int LEFT = 2;
    public static final int BOTH = 3;
    /** The chance a ghoul child of a one-eyed parent has the one kakugan. */
    public static final double ONE_EYED_CHANCE = 0.6;
    /** A parent's colours when nothing better is known (brown hair, brown eyes, a light skin). */
    public static final int HAIR = 0x5A3A22;
    public static final int EYES = 0x4A3020;
    public static final int SKIN = 0xD9A07E;

    /** A parent, as far as a child goes: their colours, and whether they are a ghoul, with one kakugan. */
    public record Parent(int hair, int eyes, int skin, boolean ghoul, boolean oneEyed) {
    }

    public static Genome of(Parent mother, Parent father, Random random) {
        Gender gender = random.nextBoolean() ? Gender.FEMALE : Gender.MALE;
        boolean ghoul = mother.ghoul() || father.ghoul();
        int kakugan = NONE;
        if (ghoul) {
            boolean oneEyedParent = mother.oneEyed() || father.oneEyed();
            kakugan = oneEyedParent && random.nextDouble() < ONE_EYED_CHANCE ? (random.nextBoolean() ? RIGHT : LEFT) : BOTH;
        }
        int hair = jitter(blend(mother.hair(), father.hair(), 0.35 + random.nextDouble() * 0.3), random, 10);
        int eyes = random.nextBoolean() ? mother.eyes() : father.eyes();
        int skin = blend(mother.skin(), father.skin(), 0.3 + random.nextDouble() * 0.4);
        return new Genome(gender, ghoul, kakugan, hair, eyes, skin, random.nextLong());
    }

    public boolean oneEyed() {
        return this.kakugan == RIGHT || this.kakugan == LEFT;
    }

    /** {@code a} to {@code b} by {@code t} (0..1), each channel. */
    public static int blend(int a, int b, double t) {
        int r = (int)Math.round(((a >> 16) & 255) * (1.0 - t) + ((b >> 16) & 255) * t);
        int g = (int)Math.round(((a >> 8) & 255) * (1.0 - t) + ((b >> 8) & 255) * t);
        int bl = (int)Math.round((a & 255) * (1.0 - t) + (b & 255) * t);
        return clamp(r) << 16 | clamp(g) << 8 | clamp(bl);
    }

    private static int jitter(int rgb, Random random, int most) {
        int shift = random.nextInt(most * 2 + 1) - most;
        return clamp(((rgb >> 16) & 255) + shift) << 16 | clamp(((rgb >> 8) & 255) + shift) << 8 | clamp((rgb & 255) + shift);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public String save() {
        return this.gender.ordinal() + "," + (this.ghoul ? 1 : 0) + "," + this.kakugan + "," + Integer.toHexString(this.hair) + "," + Integer.toHexString(this.eyes)
            + "," + Integer.toHexString(this.skin) + "," + this.seed;
    }

    /** As {@link #save} wrote it; null if it can't be read. */
    public static Genome load(String saved) {
        if (saved == null || saved.isEmpty()) {
            return null;
        }
        String[] p = saved.split(",");
        if (p.length != 7) {
            return null;
        }
        try {
            return new Genome(Gender.byOrdinal(Integer.parseInt(p[0])), "1".equals(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3], 16),
                Integer.parseInt(p[4], 16), Integer.parseInt(p[5], 16), Long.parseLong(p[6]));
        } catch (NumberFormatException bad) {
            return null;
        }
    }
}

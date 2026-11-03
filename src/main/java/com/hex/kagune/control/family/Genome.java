package com.hex.kagune.control.family;

import com.hex.kagune.control.Gender;
import java.util.Random;

/**
 * 1.3.10: what a child is, decided the night it is conceived (pure): a boy or a girl; a ghoul if either parent is; its kakugan (both eyes,
 * as a ghoul's are, or, if a parent is one-eyed, a good chance of just the one); its hair the blend of both parents' hair; its eyes one
 * parent's; its skin between theirs; a seed for the rest of its looks. 1.3.43: its kagune ({@link KaguneGenes}: the kinds from both parents,
 * {@link KaguneGenes#UNKNOWN} for a child from before) and that kagune's colour (one parent's; -1 none or not known).
 */
public record Genome(Gender gender, boolean ghoul, int kakugan, int hair, int eyes, int skin, long seed, int kagune, int colour) {
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

    /** 1.3.43: the genome of a child from before its kagune was decided (worked out when it next thinks). */
    public Genome(Gender gender, boolean ghoul, int kakugan, int hair, int eyes, int skin, long seed) {
        this(gender, ghoul, kakugan, hair, eyes, skin, seed, KaguneGenes.UNKNOWN, -1);
    }

    /**
     * A parent, as far as a child goes: their colours, and whether they are a ghoul, with one kakugan; 1.3.43: the kagune they pass on
     * ({@link KaguneGenes} bits) and its colour.
     */
    public record Parent(int hair, int eyes, int skin, boolean ghoul, boolean oneEyed, int kagune, int colour) {
        /** A parent with no kagune to pass on (or not known). */
        public Parent(int hair, int eyes, int skin, boolean ghoul, boolean oneEyed) {
            this(hair, eyes, skin, ghoul, oneEyed, KaguneGenes.NONE, -1);
        }
    }

    /** 1.3.43: this genome with its kagune decided ({@code kagune} bits, {@code colour}). */
    public Genome withKagune(int kagune, int colour) {
        return new Genome(this.gender, this.ghoul, this.kakugan, this.hair, this.eyes, this.skin, this.seed, kagune, colour);
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
        long seed = random.nextLong();
        // 1.3.43 (drawn after the rest, so everything before stays as it was): its kagune, and that kagune's colour from one parent.
        // 2.0.6: of two kinds, a chance of one alone (its roll drawn last of all), its colour then that parent's.
        int kagune = ghoul ? KaguneGenes.inherit(mother.kagune(), father.kagune()) : KaguneGenes.NONE;
        int colour = kagune == KaguneGenes.NONE ? -1 : KaguneGenes.colour(mother.kagune(), mother.colour(), father.kagune(), father.colour(), random);
        if (ghoul) {
            kagune = KaguneGenes.inherit(mother.kagune(), father.kagune(), random.nextDouble());
            colour = KaguneGenes.colourOf(kagune, mother.kagune(), mother.colour(), father.kagune(), father.colour(), colour);
        }
        return new Genome(gender, ghoul, kakugan, hair, eyes, skin, seed, kagune, colour);
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
            + "," + Integer.toHexString(this.skin) + "," + this.seed + "," + this.kagune + "," + this.colour;
    }

    /** 1.3.43: true once its kagune is decided (a child from before has it worked out from its parents). */
    public boolean kaguneKnown() {
        return this.kagune != KaguneGenes.UNKNOWN;
    }

    /** As {@link #save} wrote it; null if it can't be read. */
    public static Genome load(String saved) {
        if (saved == null || saved.isEmpty()) {
            return null;
        }
        String[] p = saved.split(",");
        if (p.length != 7 && p.length != 9) {
            return null;
        }
        try {
            // 1.3.43: one saved before its kagune was decided has seven parts: its kagune not known yet.
            int kagune = p.length == 9 ? Integer.parseInt(p[7]) : KaguneGenes.UNKNOWN;
            int colour = p.length == 9 ? Integer.parseInt(p[8]) : -1;
            return new Genome(Gender.byOrdinal(Integer.parseInt(p[0])), "1".equals(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3], 16),
                Integer.parseInt(p[4], 16), Integer.parseInt(p[5], 16), Long.parseLong(p[6]), kagune, colour);
        } catch (NumberFormatException bad) {
            return null;
        }
    }
}

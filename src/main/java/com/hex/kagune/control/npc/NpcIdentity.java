package com.hex.kagune.control.npc;

/**
 * 7.0.2: who a ghoul NPC is (pure, tested): its kagune, its temper, how strong it is ({@code tier} 0 to 2), the face it wears
 * ({@code skin}: 1.0.41, one of its own, {@link NpcLook}, a boy's or a girl's), its name and a seed for everything else drawn at
 * random about it. Rolled once when it is spawned and kept in its saved data as one number ({@link #pack}), sent to every client as
 * it is.
 */
public record NpcIdentity(Kind kind, NpcTemper temper, int tier, int skin, int name, int seed, int mask, boolean aogiri, int gang) {
    /**
     * Its kagune: the transplant's rinkaku tails, or the ukaku's twin wings; 1.3.36 (appended: sent and saved by ordinal) the arm blade, a
     * koukaku no player can take ({@link BladeArm}).
     */
    public enum Kind {
        RINKAKU, UKAKU, KOUKAKU;

        public static Kind byOrdinal(int ordinal) {
            Kind[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : RINKAKU;
        }
    }

    /** 4.0.0: its rank, D to SS ({@link NpcRules#RANKS}): six of them (three tiers before). */
    public static final int TIERS = 6;

    /** One without a mask, of no faction (every one before 4.0.0). */
    public NpcIdentity(Kind kind, NpcTemper temper, int tier, int skin, int name, int seed) {
        this(kind, temper, tier, skin, name, seed, 0, false, 0);
    }

    /** 4.0.0: one of no gang (every one before 4.1.45). */
    public NpcIdentity(Kind kind, NpcTemper temper, int tier, int skin, int name, int seed, int mask, boolean aogiri) {
        this(kind, temper, tier, skin, name, seed, mask, aogiri, 0);
    }

    public NpcIdentity {
        tier = Math.max(0, Math.min(TIERS - 1, tier));
        mask = Math.max(0, Math.min(15, mask));
        gang = Math.max(0, Math.min(3, gang));
        skin = Math.floorMod(skin, NpcLook.SKINS.length);
        name = Math.floorMod(name, NpcNames.count());
        seed &= 0xFFFF;
    }

    /**
     * Everything in one number: kind 1 bit, temper 3, tier 2, skin 4, name 8, seed 16; 1.0.41: the skin's fifth bit at 34, and bit 35
     * set (the skin is one of its own: before, it was one of vanilla's nine); 1.3.36: the kind's second bit at 36 (one saved before
     * reads as it was: the bit is clear).
     */
    public long pack() {
        int kind = this.kind.ordinal();
        return (long)(kind & 1) | (long)this.temper.ordinal() << 1 | (long)(this.tier & 3) << 4 | (long)(this.skin & 15) << 6 | (long)this.name << 10
            | (long)this.seed << 18 | (long)(this.skin >> 4 & 1) << 34 | OWN_SKIN | (long)(kind >> 1 & 1) << 36
            | (long)(this.tier >> 2 & 1) << 37 | RANKED | (long)this.mask << 39 | (this.aogiri ? AOGIRI : 0L) | (long)this.gang << 44;
    }

    /**
     * 4.0.0: set in a packed identity saved with its rank (D to SS: the rank's third bit at 37, its mask at 39 to 42, Aogiri at 43). One
     * saved before had three tiers: 0, 1 and 2 are C, B and A now (as strong as they were), with no mask. 4.1.45: its gang at 44 and 45
     * ({@link Gang}, 0 none: every one saved before).
     */
    private static final long RANKED = 1L << 38;
    private static final long AOGIRI = 1L << 43;

    /** 1.0.41: set in a packed identity whose skin is one of {@link NpcLook}'s own. */
    private static final long OWN_SKIN = 1L << 35;

    public static NpcIdentity unpack(long packed) {
        int name = (int)(packed >> 10 & 255L);
        int seed = (int)(packed >> 18 & 0xFFFFL);
        // 1.0.41: one saved before it wore one of its own skins is given one now, the same every time.
        int skin = (packed & OWN_SKIN) != 0L ? (int)(packed >> 6 & 15L) | (int)(packed >> 34 & 1L) << 4 : seed * 7 + name;
        int kind = (int)(packed & 1L) | (int)(packed >> 36 & 1L) << 1;
        boolean ranked = (packed & RANKED) != 0L;
        int tier = ranked ? (int)(packed >> 4 & 3L) | (int)(packed >> 37 & 1L) << 2 : (int)(packed >> 4 & 3L) + 1;
        int mask = ranked ? (int)(packed >> 39 & 15L) : 0;
        return new NpcIdentity(Kind.byOrdinal(kind), NpcTemper.byOrdinal((int)(packed >> 1 & 7L)), tier, skin, name, seed, mask,
            ranked && (packed & AOGIRI) != 0L, ranked ? (int)(packed >> 44 & 3L) : 0);
    }

    /** 1.0.41: a girl (her skin on the slim arms, a girl's name) or a boy. */
    public boolean girl() {
        return NpcLook.girl(this.skin);
    }

    /** Its given name alone. */
    public String given() {
        return NpcNames.given(this.name, girl());
    }

    /**
     * A new ghoul met by someone at {@code mastery}: its temper by weight, its kagune even odds, a stronger one more likely the
     * stronger the one who meets it; {@code roll} gives six numbers in 0..1.
     */
    public static NpcIdentity roll(int mastery, java.util.function.DoubleSupplier roll) {
        NpcTemper temper = NpcTemper.pick(roll.getAsDouble());
        // 1.3.36: a little over a quarter carry the arm blade.
        double which = roll.getAsDouble();
        Kind kind = which < 0.37 ? Kind.RINKAKU : which < 0.72 ? Kind.UKAKU : Kind.KOUKAKU;
        int tier = NpcRules.tier(mastery, roll.getAsDouble());
        int skin = (int)(roll.getAsDouble() * NpcLook.SKINS.length);
        int name = (int)(roll.getAsDouble() * NpcNames.count());
        int seed = (int)(roll.getAsDouble() * 65536.0);
        // 4.0.0: a chance of a mask, one of the generic ones (never a character's or a faction's).
        int mask = com.hex.kagune.control.outfit.Outfits.wildMask(roll.getAsDouble());
        return new NpcIdentity(kind, temper, tier, skin, name, seed, mask, false);
    }

    /** 4.0.0: the same, of rank {@code tier}. */
    public NpcIdentity ranked(int tier) {
        return new NpcIdentity(this.kind, this.temper, tier, this.skin, this.name, this.seed, this.mask, this.aogiri, this.gang);
    }

    /**
     * 4.1.45: one of a roaming gang's ({@code gang}), in its mask {@code mask} (1-based, each member a different one), its temper a drifter's
     * (it starts nothing: whoever hurts one of them has the gang on him).
     */
    public NpcIdentity ganged(Gang gang, int mask) {
        return new NpcIdentity(this.kind, NpcTemper.DRIFTER, this.tier, this.skin, this.name, this.seed, mask, false, gang.ordinal());
    }

    /** 4.1.45: its gang ({@link Gang#NONE} for none). */
    public Gang gangOf() {
        return Gang.byOrdinal(this.gang);
    }

    /**
     * 4.0.0: an Aogiri Tree member (its faction's mask, {@link com.hex.kagune.control.outfit.Outfits#AOGIRI_MASKS}, 1-based), its temper
     * Aogiri's own (the pack: violent, loyal to its own), of rank C to S.
     */
    public NpcIdentity member(int mask, int tier) {
        return new NpcIdentity(this.kind, this.temper, tier, this.skin, this.name, this.seed, mask, true);
    }

    /** 4.0.0: the mask it wears (an item id), or null: an Aogiri member's faction mask, a wild ghoul's generic one. */
    public String maskItem() {
        if (this.gang != 0) {
            return gangOf().mask(this.mask); // 4.1.45: its gang's, drawn on its face (no item: Outfits.geometryOf)
        }
        return this.aogiri ? com.hex.kagune.control.outfit.Outfits.aogiri(this.mask) : com.hex.kagune.control.outfit.Outfits.generic(this.mask);
    }

    /** 4.0.0: its rank's name ("B"). */
    public String rank() {
        return NpcRules.rankName(this.tier);
    }

    /** What it is called: a given name, and for the ones who hunt, the name the streets gave it. */
    public String displayName() {
        return NpcNames.name(this.name, this.temper, girl());
    }

    /** 4.0.0: its name over its head: its name and its rank ("Kaito, B-rank"); Aogiri's own say so. */
    public String tagName() {
        return displayName() + ", " + rank() + "-rank" + (this.aogiri ? " (Aogiri)" : this.gang != 0 ? " (" + gangOf().title() + ")" : "");
    }

    /** The mastery its kagune is drawn at (how big and how many tails, how big the wings; the arm blade is its own size). */
    public int lookMastery() {
        return this.kind == Kind.RINKAKU || this.kind == Kind.KOUKAKU ? NpcRules.tailMastery(this.tier) : NpcRules.wingMastery(this.tier);
    }
}

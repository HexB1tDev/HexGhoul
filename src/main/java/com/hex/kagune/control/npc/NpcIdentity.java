package com.hex.kagune.control.npc;

/**
 * 7.0.2: who a ghoul NPC is (pure, tested): its kagune, its temper, how strong it is ({@code tier} 0 to 2), the face it wears
 * ({@code skin}: 1.0.41, one of its own, {@link NpcLook}, a boy's or a girl's), its name and a seed for everything else drawn at
 * random about it. Rolled once when it is spawned and kept in its saved data as one number ({@link #pack}), sent to every client as
 * it is.
 */
public record NpcIdentity(Kind kind, NpcTemper temper, int tier, int skin, int name, int seed) {
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

    public static final int TIERS = 3;

    public NpcIdentity {
        tier = Math.max(0, Math.min(TIERS - 1, tier));
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
        return (long)(kind & 1) | (long)this.temper.ordinal() << 1 | (long)this.tier << 4 | (long)(this.skin & 15) << 6 | (long)this.name << 10
            | (long)this.seed << 18 | (long)(this.skin >> 4 & 1) << 34 | OWN_SKIN | (long)(kind >> 1 & 1) << 36;
    }

    /** 1.0.41: set in a packed identity whose skin is one of {@link NpcLook}'s own. */
    private static final long OWN_SKIN = 1L << 35;

    public static NpcIdentity unpack(long packed) {
        int name = (int)(packed >> 10 & 255L);
        int seed = (int)(packed >> 18 & 0xFFFFL);
        // 1.0.41: one saved before it wore one of its own skins is given one now, the same every time.
        int skin = (packed & OWN_SKIN) != 0L ? (int)(packed >> 6 & 15L) | (int)(packed >> 34 & 1L) << 4 : seed * 7 + name;
        int kind = (int)(packed & 1L) | (int)(packed >> 36 & 1L) << 1;
        return new NpcIdentity(Kind.byOrdinal(kind), NpcTemper.byOrdinal((int)(packed >> 1 & 7L)), (int)(packed >> 4 & 3L), skin, name, seed);
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
        return new NpcIdentity(kind, temper, tier, skin, name, seed);
    }

    /** What it is called: a given name, and for the ones who hunt, the name the streets gave it. */
    public String displayName() {
        return NpcNames.name(this.name, this.temper, girl());
    }

    /** The mastery its kagune is drawn at (how big and how many tails, how big the wings; the arm blade is its own size). */
    public int lookMastery() {
        return this.kind == Kind.RINKAKU || this.kind == Kind.KOUKAKU ? NpcRules.tailMastery(this.tier) : NpcRules.wingMastery(this.tier);
    }
}

package com.hex.kagune.control.npc;

import java.util.List;

/**
 * 4.1.45 (the owner: "Next up we're adding roaming gangs, such as the clowns, 3-5 roaming ghouls walking together. ... Clowns (all ghouls
 * wear different clown masks (nothing else) and protect eachother) (Non-hostile unless provoked) ... Gas Masks (... they wear nothing but
 * gas masks)"): the roaming gangs of ghouls (pure, tested). Each gang walks together, {@link #SMALLEST} to {@link #LARGEST} of them, every
 * one in a different mask of its gang's ({@link #masks}) and nothing else; they start nothing, but whoever hurts one of them (or goes for
 * one) has all of them on him. Sent and saved by ordinal (in the ghoul's identity: 0 is none).
 *
 * <p>4.1.46 (the owner: "New Re CCG Ghoul division (Normal ccg agents (HUMANS not ghouls), they have RC cells which means they get a
 * random kagune, they are not ghouls and are members of the CCG, they are never attacked by the ccg, have no kakugan, they are basically
 * wild ghouls with CCG armor and a Kagune (NO kakugan), they spawn in trios"): {@link #CCG_DIVISION}, three humans of the CCG's in its
 * Formal Outfit, each with a kagune of its own and no kakugan, going for what an investigator goes for ({@link #ccg}).
 */
public enum Gang {
    NONE("", List.of()),
    CLOWNS("Clowns", List.of("mask_clown", "mask_clown_v2", "mask_clown_v3", "mask_clown_v4", "mask_clown_v5")),
    GAS_MASKS("Gas Masks", List.of("mask_gas", "mask_gas_v2", "mask_gas_v3", "mask_gas_v4", "mask_gas_v5")),
    /** 4.1.46: the CCG's ghoul division: no mask (the Formal Outfit), never a roaming gang ({@link #roaming}). */
    CCG_DIVISION("CCG Ghoul Division", List.of());

    /** 4.1.46: how many of the CCG's ghoul division walk together. */
    public static final int TRIO = 3;

    /** How many walk together. */
    public static final int SMALLEST = 3;
    public static final int LARGEST = 5;

    private final String title;
    private final List<String> masks;

    Gang(String title, List<String> masks) {
        this.title = title;
        this.masks = masks;
    }

    public String title() {
        return this.title;
    }

    /** 4.1.46: true for the CCG's own: never attacked by the CCG nor attacking it, no kakugan, the Formal Outfit. */
    public boolean ccg() {
        return this == CCG_DIVISION;
    }

    /** Its masks, each a different one (a variant draws its base's model in its own paint: Outfits.geometryOf). */
    public List<String> masks() {
        return this.masks;
    }

    /** Its mask {@code index} (1-based, as an identity keeps it), or null. */
    public String mask(int index) {
        return index >= 1 && index <= this.masks.size() ? this.masks.get(index - 1) : null;
    }

    public static Gang byOrdinal(int ordinal) {
        Gang[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }

    /** A roaming gang ({@code roll} in 0..1): the clowns or the gas masks, even odds. */
    public static Gang roaming(double roll) {
        return roll < 0.5 ? CLOWNS : GAS_MASKS;
    }

    /** How many walk together ({@code roll} in 0..1): {@link #SMALLEST} to {@link #LARGEST}. */
    public static int size(double roll) {
        return SMALLEST + Math.min(LARGEST - SMALLEST, (int)(Math.max(0.0, roll) * (LARGEST - SMALLEST + 1)));
    }

    /**
     * Each of {@code size} members' masks (1-based indexes), all different ({@code roll} gives numbers in 0..1): a shuffle of the gang's own,
     * so no two of them wear the same one.
     */
    public int[] faces(int size, java.util.function.DoubleSupplier roll) {
        int n = this.masks.size();
        if (n == 0) {
            return new int[Math.max(0, size)]; // 4.1.46: no masks (the CCG's division): none for each
        }
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i + 1;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = Math.min(i, (int)(roll.getAsDouble() * (i + 1)));
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        int[] out = new int[size];
        for (int i = 0; i < size; i++) {
            out[i] = order[i % n];
        }
        return out;
    }
}

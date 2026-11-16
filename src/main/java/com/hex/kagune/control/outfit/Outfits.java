package com.hex.kagune.control.outfit;

import java.util.List;

/**
 * 4.0.0 (The Great Exploration): every outfit piece and mask there is as an item (pure, tested): its id, the model it is drawn from
 * ({@link OutfitModels}) and the slot it is worn in. The owner's two outfits (the CCG's Formal Outfit, Aogiri Tree's Crimson Outfit), four
 * pieces each (4.0.3: three of the Crimson, its hood gone), and their 24 masks; and the generic masks (made for 4.0.0: a clown's, a gas
 * mask, a skull, a fox, an oni, a cloth wrap, a cracked porcelain face, a bandage wrap, a kabuki face), the only ones a wild ghoul is ever
 * found in. Every mask is a helmet: worn in the head slot (a cap and a mask, one at a time, as the owner's own mod has it).
 */
public final class Outfits {
    /** One item: its id, its model, its slot ({@link OutfitModels#HELMET} ...), and whether it is a mask. */
    public record Piece(String id, String model, int slot, boolean mask) {
        /** Its texture's path in the mod's assets. */
        public String texture() {
            return "textures/outfit/" + this.model + ".png";
        }
    }

    public static final String FORMAL = "formal";
    public static final String CRIMSON = "crimson";

    /** The Formal Outfit (every CCG investigator's but the White Reaper's): cap, overcoat, trousers, shoes. */
    public static final List<Piece> FORMAL_SET = set(FORMAL, true);
    /**
     * The Crimson Outfit (every Aogiri Tree member's): coat (with its high collar), coat tails and trousers, boots. 4.0.3 (the owner: "On
     * the Ougiri outfit, no more hood, make it less bulky around the shoulders and head to be similar to the formal outfit, and, give it a
     * high collar on the sides of the head pointing outwards"): no hood; the head is left to a mask.
     */
    public static final List<Piece> CRIMSON_SET = set(CRIMSON, false);

    /** The owner's 24 masks, in their reference sheet's order (the first is Eyepatch's: its folder's name kept out of the mod). */
    public static final List<String> CHARACTER_MASKS = List.of("mask_eyepatch", "mask_touka", "mask_ayato", "mask_noro", "mask_yamori", "mask_eto",
        "mask_tatara", "mask_bin_brothers", "mask_roma", "mask_shachi", "mask_rize", "mask_uta", "mask_itori", "mask_yomo", "mask_dotted",
        "mask_nishiki", "mask_tsukiyama", "mask_kanae", "mask_donato", "mask_koori", "mask_amon", "mask_seidou", "mask_akira", "mask_arima");

    /**
     * The generic masks: belonging to no one (the owner: "clown masks, gas masks, and other non-faction-specific masks"). The only ones a
     * wild ghoul ever wears.
     */
    public static final List<String> GENERIC_MASKS = List.of("mask_clown", "mask_gas", "mask_skull", "mask_fox", "mask_oni", "mask_cloth",
        "mask_porcelain", "mask_bandage", "mask_kabuki"); // the last three since the owner found the first "very basic": a saved ghoul's index keeps its mask

    /**
     * Aogiri Tree's own masks a member wears (the owner: "the appropriate faction masks"): the Laughing Mask and the Fanged
     * Mask, the faction's rank and file; never an executive's (Ayato's, Noro's, Eto's, the Hockey Mask, the Red Muzzle, the Jester).
     */
    public static final List<String> AOGIRI_MASKS = List.of("mask_bin_brothers", "mask_shachi");

    /**
     * 4.0.7 (the owner: "Full masks (only full masks not half masks) prevent CCG agents and humans to tell you're a ghoul when your eyes are
     * in Kakugan."): the masks that leave the eyes and the face's top bare, over the mouth only (the owner's eight half masks, the cloth
     * wrap); every other mask covers the whole face.
     */
    public static final java.util.Set<String> HALF_MASKS = java.util.Set.of("mask_eyepatch", "mask_ayato", "mask_tatara", "mask_shachi", "mask_rize",
        "mask_yomo", "mask_dotted", "mask_nishiki", "mask_cloth");

    /** 4.0.7: whether {@code id} is a mask over the whole face (its wearer's kakugan unseen behind it). */
    public static boolean fullMask(String id) {
        Piece piece = of(id);
        return piece != null && piece.mask() && !HALF_MASKS.contains(id);
    }

    /** Every piece, outfits first, then every mask. */
    public static final List<Piece> ALL;

    static {
        java.util.ArrayList<Piece> all = new java.util.ArrayList<>(FORMAL_SET);
        all.addAll(CRIMSON_SET);
        for (String mask : CHARACTER_MASKS) {
            all.add(new Piece(mask, mask, OutfitModels.HELMET, true));
        }
        for (String mask : GENERIC_MASKS) {
            all.add(new Piece(mask, mask, OutfitModels.HELMET, true));
        }
        ALL = List.copyOf(all);
    }

    private Outfits() {
    }

    /** A style's pieces: its head's ({@code head}: the Formal Outfit's cap; the Crimson has none), its coat, its legs', its feet's. */
    private static List<Piece> set(String style, boolean head) {
        java.util.ArrayList<Piece> pieces = new java.util.ArrayList<>();
        if (head) {
            pieces.add(new Piece(style + "_helmet", style, OutfitModels.HELMET, false));
        }
        pieces.add(new Piece(style + "_chestplate", style, OutfitModels.CHESTPLATE, false));
        pieces.add(new Piece(style + "_leggings", style, OutfitModels.LEGGINGS, false));
        pieces.add(new Piece(style + "_boots", style, OutfitModels.BOOTS, false));
        return List.copyOf(pieces);
    }

    /** The piece with {@code id}, or null. */
    public static Piece of(String id) {
        for (Piece piece : ALL) {
            if (piece.id().equals(id)) {
                return piece;
            }
        }
        return null;
    }

    /** The mask {@code index} names among {@link #GENERIC_MASKS} (1-based: 0 is none), or null. */
    /**
     * 4.1.45: the model a mask {@code id} is drawn with: a gang's variant ("mask_clown_v3") its base's ("mask_clown"), in its own paint
     * (textures/outfit/{@code id}.png); any other mask its own.
     */
    public static String geometryOf(String id) {
        if (id == null) {
            return null;
        }
        int v = id.lastIndexOf("_v");
        if (id.startsWith("mask_") && v > 0 && v + 2 < id.length() && id.substring(v + 2).chars().allMatch(Character::isDigit)) {
            return id.substring(0, v);
        }
        return id;
    }

    public static String generic(int index) {
        return index >= 1 && index <= GENERIC_MASKS.size() ? GENERIC_MASKS.get(index - 1) : null;
    }

    /** The mask {@code index} names among {@link #AOGIRI_MASKS} (1-based: 0 is none), or null. */
    public static String aogiri(int index) {
        return index >= 1 && index <= AOGIRI_MASKS.size() ? AOGIRI_MASKS.get(index - 1) : null;
    }

    /**
     * A wild ghoul's mask for a roll {@code r} in 0..1 (1-based among the generic masks; 0: none). The owner: "a chance to spawn wearing
     * randomly selected masks": {@link #WILD_MASK_CHANCE} of them wear one, each mask as likely.
     */
    public static int wildMask(double r) {
        if (r >= WILD_MASK_CHANCE) {
            return 0;
        }
        return 1 + Math.min(GENERIC_MASKS.size() - 1, (int)(r / WILD_MASK_CHANCE * GENERIC_MASKS.size()));
    }

    /** How many wild ghouls wear a mask. */
    public static final double WILD_MASK_CHANCE = 0.6;

    /**
     * Each piece's own chance of falling when an investigator (or an Aogiri member, or a masked ghoul) is killed by a player (the owner:
     * "a small chance to drop individual pieces ... calculated separately for each piece ... low enough to make collecting the full set
     * rewarding"): one in twenty-five, each piece its own roll.
     */
    public static final float DROP_CHANCE = 0.04F;

    /** The armour the Formal and Crimson outfits give together (iron's: 2 + 6 + 5 + 2), taken off an NPC's own so a dressed one fights as before. */
    public static final double SET_ARMOR = 15.0;
    /** And a mask alone (an iron helmet's). */
    public static final double MASK_ARMOR = 2.0;
}

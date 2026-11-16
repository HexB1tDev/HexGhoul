package com.hex.kagune.control.family;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 2.0.9 (the owner: "spouse will sort items from chests all of them in the house perimeter, separating items in different chests from NBT
 * data, food in one chest, blocks in another, etc. If it doesnt know what to do with them it puts them in a "Whatever" chest"; asked, they
 * chose that she labels the chests herself): what kind of thing an item is, by its id and what the game says of it (pure, tested), and
 * which chest of the home holds which kind. A chest named after a kind (by her, or by the player with an anvil: "Food", "food", "Armour"...)
 * keeps that kind; a chest the player named anything else is theirs, left alone (nothing taken from it or put in it); an unnamed chest is
 * given a kind the home needs (the kinds with the most things first) and named, and one is made the "Whatever" chest.
 */
public final class ChestSort {
    /** The kinds, each with the name written on its chest. */
    public enum Kind {
        FOOD("Food"),
        BLOCKS("Blocks"),
        TOOLS("Tools & Weapons"),
        ARMOR("Armor"),
        ORES("Ores & Minerals"),
        PLANTS("Plants & Seeds"),
        DROPS("Mob Drops"),
        WHATEVER("Whatever");

        public final String label;

        Kind(String label) {
            this.label = label;
        }
    }

    private static final String[] ARMOR = {"_helmet", "_chestplate", "_leggings", "_boots", "_horse_armor", "elytra", "_cap", "_tunic", "_pants"};
    private static final String[] TOOLS = {"_pickaxe", "_axe", "_shovel", "_hoe", "_sword", "bow", "trident", "shield", "fishing_rod", "shears",
        "flint_and_steel", "arrow", "spyglass", "compass", "clock", "brush", "mace", "_knife", "_dagger", "_spear"};
    private static final String[] ORE = {"_ore", "raw_", "ancient_debris"};
    private static final String[] PLANTS = {"seeds", "_sapling", "_propagule", "sugar_cane", "bamboo", "cactus", "vine", "lily_pad", "kelp", "wheat",
        "fern", "dead_bush", "nether_wart", "cocoa_beans", "pitcher_pod", "torchflower", "_tulip", "poppy", "dandelion", "orchid", "allium",
        "azure_bluet", "oxeye_daisy", "cornflower", "lily_of_the_valley", "wither_rose", "sunflower", "lilac", "rose_bush", "peony", "_mushroom",
        "spore_blossom", "azalea", "sea_pickle", "_fungus", "_roots", "tall_grass", "hanging_roots", "glow_lichen", "moss_carpet", "bone_meal"};
    private static final String[] MINERALS = {"_ingot", "_nugget", "diamond", "emerald", "lapis_lazuli", "redstone", "coal", "quartz",
        "amethyst_shard", "netherite_scrap", "glowstone_dust", "flint", "echo_shard", "copper", "clay_ball"};
    private static final String[] DROPS = {"bone", "string", "leather", "feather", "gunpowder", "ender_pearl", "slime_ball", "blaze_rod",
        "blaze_powder", "ghast_tear", "ink_sac", "rabbit_hide", "rabbit_foot", "phantom_membrane", "shulker_shell", "prismarine_shard",
        "prismarine_crystals", "nautilus_shell", "scute", "magma_cream", "egg", "honeycomb", "breeze_rod", "spider_eye", "ender_eye", "fermented"};

    private ChestSort() {
    }

    /**
     * What kind {@code id} ("minecraft:iron_pickaxe", any mod's too) is: {@code food} the game says it can be eaten (or it is ghoul meat),
     * {@code block} it places a block. Food first; armour; tools and weapons; ores; plants and seeds; other blocks; minerals; mob drops; else
     * Whatever.
     */
    public static Kind kind(String id, boolean food, boolean block) {
        String path = id == null ? "" : id.toLowerCase(Locale.ROOT);
        int colon = path.indexOf(':');
        path = colon >= 0 ? path.substring(colon + 1) : path;
        if (food) {
            return Kind.FOOD;
        }
        if (has(path, ARMOR) && !block) {
            return Kind.ARMOR;
        }
        if (has(path, TOOLS) && !block && !path.equals("bowl")) {
            return Kind.TOOLS;
        }
        if (has(path, ORE)) {
            return Kind.ORES;
        }
        if (has(path, PLANTS) || path.equals("grass")) {
            return Kind.PLANTS;
        }
        if (block) {
            return Kind.BLOCKS;
        }
        if (has(path, MINERALS)) {
            return Kind.ORES;
        }
        if (has(path, DROPS)) {
            return Kind.DROPS;
        }
        return Kind.WHATEVER;
    }

    private static boolean has(String path, String[] words) {
        for (String word : words) {
            if (path.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** The kind a chest's name gives it (its label, or a word for it, any case), or null: not a kind's name. */
    public static Kind named(String name) {
        String n = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (n.isEmpty()) {
            return null;
        }
        for (Kind kind : Kind.values()) {
            if (n.equals(kind.label.toLowerCase(Locale.ROOT)) || n.equals(kind.name().toLowerCase(Locale.ROOT))) {
                return kind;
            }
        }
        return switch (n) {
            case "tools", "weapons", "tools and weapons" -> Kind.TOOLS;
            case "armour" -> Kind.ARMOR;
            case "ores", "minerals", "ores and minerals" -> Kind.ORES;
            case "plants", "seeds", "plants and seeds" -> Kind.PLANTS;
            case "drops", "mob drops" -> Kind.DROPS;
            case "misc", "other", "stuff" -> Kind.WHATEVER;
            default -> null;
        };
    }

    /** What a chest of the home is to her: its kind, and whether it is the player's own (named something else: left alone). */
    public record Box(Kind kind, boolean theirs) {
        public boolean free() {
            return this.kind == null && !this.theirs;
        }
    }

    /** A chest as its name has it ({@code name}: its custom name, empty for none). */
    public static Box box(String name) {
        Kind kind = named(name);
        boolean named = name != null && !name.trim().isEmpty();
        return new Box(kind, kind == null && named);
    }

    /**
     * Each chest's kind for the home ({@code boxes}: the chests (a double chest once) as their names have them, nearest the home's spot
     * first; {@code counts}: how many things of each kind its chests hold, in chests not of that kind already): a named kind's chest keeps
     * it; the kinds still without a chest that have things are given free chests, the most things first; a Whatever chest is always kept,
     * the last free chest when there are fewer free chests than kinds wanting one. Null for a chest left as it is (the player's own, or
     * one spare).
     */
    public static List<Kind> assign(List<Box> boxes, Map<Kind, Integer> counts) {
        List<Kind> out = new ArrayList<>();
        List<Integer> free = new ArrayList<>();
        boolean[] has = new boolean[Kind.values().length];
        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            out.add(box.kind());
            if (box.kind() != null) {
                has[box.kind().ordinal()] = true;
            }
            if (box.free()) {
                free.add(i);
            }
        }
        List<Kind> wanted = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            if (kind != Kind.WHATEVER && !has[kind.ordinal()] && counts.getOrDefault(kind, 0) > 0) {
                wanted.add(kind);
            }
        }
        wanted.sort(Comparator.comparingInt((Kind k) -> -counts.getOrDefault(k, 0)).thenComparingInt(Enum::ordinal));
        boolean whatever = has[Kind.WHATEVER.ordinal()];
        int room = free.size() - (whatever ? 0 : 1); // one kept for Whatever, unless there is one
        int next = 0;
        for (Kind kind : wanted) {
            if (next >= room) {
                break;
            }
            out.set(free.get(next++), kind);
        }
        if (!whatever && next < free.size()) {
            out.set(free.get(next), Kind.WHATEVER);
        }
        return out;
    }

    /** Where a thing of {@code kind} goes: its kind's chest, else the Whatever chest, else -1 (it stays where it is). */
    public static int target(List<Kind> kinds, Kind kind) {
        int whatever = -1;
        for (int i = 0; i < kinds.size(); i++) {
            if (kinds.get(i) == kind) {
                return i;
            }
            if (kinds.get(i) == Kind.WHATEVER && whatever < 0) {
                whatever = i;
            }
        }
        return whatever;
    }

    /** The custom name the game saved ({"text":"Food"} as an anvil writes it, or plain), as words; empty for none. */
    public static String nameOf(String saved) {
        if (saved == null || saved.isEmpty()) {
            return "";
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(saved);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            out.append(m.group(1).replace("\\\"", "\"").replace("\\\\", "\\"));
        }
        if (out.length() > 0) {
            return out.toString();
        }
        String plain = saved.trim();
        return plain.startsWith("\"") && plain.endsWith("\"") && plain.length() >= 2 ? plain.substring(1, plain.length() - 1) : plain.startsWith("{") ? "" : plain;
    }

    /** {@code label} as the game saves a custom name. */
    public static String saved(String label) {
        return "{\"text\":\"" + label.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    /** The two halves of a double chest ({@code state}: the block state as the game writes it, "...[facing=north,type=left,...]"): "left", "right", or "" (single). */
    public static String half(String state) {
        String s = state == null ? "" : state;
        return s.contains("type=left") ? "left" : s.contains("type=right") ? "right" : "";
    }
}

package com.hex.kagune.control.family;

import com.hex.kagune.control.npc.EtoGesture;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.3.29: a spouse's day at the home they share (pure, tested; the world's side is {@code HomeChores}). By day, at a declared home, it
 * keeps house instead of strolling to the door and back: it puts a chest in order (its things sorted and stacked for real), sweeps the
 * floor with the brush (and puts whatever lies about on it into a chest), wipes the tables, goes round the house watering the flowers and
 * the crops (watered, a crop or a sapling grows a step now and then), takes a book off the shelf a while, looks out of a window. What a
 * block is for is read from its registry name ({@link #of}); which chore comes next is weighed by what the home has ({@link #pick}).
 */
public final class Chores {
    /** A chore: the gesture it is done with, how close it stands to do it (blocks, to the block's middle). */
    public enum Kind {
        TIDY(EtoGesture.TIDY, 2.3),
        SWEEP(EtoGesture.SWEEP, 1.4),
        WIPE(EtoGesture.WIPE, 2.3),
        WATER(EtoGesture.WATER, 2.6),
        READ(EtoGesture.READ, 2.5),
        LOOK(EtoGesture.LOOK, 2.2),
        /** 1.3.31: cooking at the stove (Kitchen): raw food from the chests cooked, a meal kept for its spouse. */
        COOK(EtoGesture.COOK, 2.3),
        /** 1.3.31: smelting at the furnace: ores, sand, cobblestone from the chests, the ingots and glass put away. */
        SMELT(EtoGesture.TIDY, 2.3),
        /** 1.3.31: ripe crops picked (the plants left to grow again), what they give put away. */
        HARVEST(EtoGesture.TIDY, 1.9);

        public final EtoGesture gesture;
        public final double reach;

        Kind(EtoGesture gesture, double reach) {
            this.gesture = gesture;
            this.reach = reach;
        }
    }

    /** How long it walks to a chore before it gives that one up (ticks), and how long without getting anywhere counts as stuck. */
    public static final long WALK = 300L;
    public static final long STUCK = 60L;
    /** Set on its way again this many times without getting anywhere, it gives that block up (1.3.32: it is never put anywhere). */
    public static final int REPATHS = 3;
    /** How far it must have got in {@link #STUCK} ticks not to be stuck (blocks). */
    public static final double PROGRESS = 0.6;
    /** How many blocks of one chore it does in a go (chests, plants, tables...), at most. */
    public static final int ROUND = 3;
    /** Items on the floor are swept up only once they have lain there this long (ticks): never what was just dropped. */
    public static final int SETTLED = 200;

    private static final String[] FLOWERS = {"dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "_tulip", "oxeye_daisy", "cornflower",
        "lily_of_the_valley", "sunflower", "lilac", "rose_bush", "peony", "torchflower", "pitcher_plant", "pink_petals", "potted_"};
    /** Named exactly (an azalea bush is watered, never the leaves of an azalea tree). */
    private static final String[] BUSHES = {"azalea", "flowering_azalea"};
    private static final String[] GROWS = {"wheat", "carrots", "potatoes", "beetroots", "melon_stem", "pumpkin_stem", "sweet_berry_bush",
        "torchflower_crop", "pitcher_crop", "_sapling", "cocoa"};

    private Chores() {
    }

    /** The registry name in a block's text ({@code "Block{minecraft:oak_planks}"} gives {@code "minecraft:oak_planks"}); else the text. */
    public static String id(String blockText) {
        if (blockText == null) {
            return "";
        }
        int open = blockText.indexOf('{');
        int close = blockText.indexOf('}', open + 1);
        return open >= 0 && close > open ? blockText.substring(open + 1, close) : blockText;
    }

    /** The name without its namespace ({@code "minecraft:chest"} gives {@code "chest"}). */
    static String path(String blockText) {
        String id = id(blockText);
        return id.substring(id.indexOf(':') + 1);
    }

    /**
     * The chore a block is for: TIDY a chest or a barrel, WIPE a table, WATER a flower, a crop, a sapling or a potted plant, READ at a
     * bookshelf or a lectern, LOOK out of a window (a glass pane); null for anything else (the floor's sweeping is found another way).
     */
    public static Kind of(String blockText) {
        String path = path(blockText);
        if (path.isEmpty()) {
            return null;
        }
        if (path.endsWith("chest") && !path.contains("ender") || path.equals("barrel")) {
            return Kind.TIDY;
        }
        if (path.equals("crafting_table") || path.endsWith("_table") && !path.equals("enchanting_table")) {
            return Kind.WIPE;
        }
        if (path.equals("bookshelf") || path.equals("chiseled_bookshelf") || path.equals("lectern")) {
            return Kind.READ;
        }
        if (path.endsWith("glass_pane")) {
            return Kind.LOOK;
        }
        if (path.equals("furnace") || path.equals("smoker") || path.equals("blast_furnace") || path.endsWith("campfire")) {
            return Kind.COOK; // 1.3.31: a station (cooking and smelting both look for them here)
        }
        if (path.equals("flower_pot")) {
            return null; // an empty pot: nothing in it to water
        }
        for (String flower : FLOWERS) {
            if (path.contains(flower)) {
                return Kind.WATER;
            }
        }
        for (String bush : BUSHES) {
            if (path.equals(bush)) {
                return Kind.WATER;
            }
        }
        return grows(blockText) ? Kind.WATER : null;
    }

    /** True if watering it helps it grow (it is given one of the world's random ticks: a crop, a stem, a berry bush, a sapling). */
    public static boolean grows(String blockText) {
        String path = path(blockText);
        if (path.startsWith("potted_")) {
            return false;
        }
        for (String grows : GROWS) {
            if (path.equals(grows) || grows.startsWith("_") && path.endsWith(grows)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if what is in it is really put in order: a chest or a barrel of its own (a trapped chest only looks tidied: its things are
     * often a redstone build's; an ender chest is never touched).
     */
    public static boolean sortable(String blockText) {
        Kind kind = of(blockText);
        String path = path(blockText);
        return kind == Kind.TIDY && !path.contains("trapped");
    }

    /** True for a chest whose lid opens while it is tidied: vanilla's own (a barrel's doesn't move; a mod's block event may mean anything). */
    public static boolean lid(String blockText) {
        String path = path(blockText);
        return of(blockText) == Kind.TIDY && path.endsWith("chest") && id(blockText).startsWith("minecraft:");
    }

    /**
     * How long until the next chore (ticks): 15 to 35 seconds home alone; 20 to 45 with its spouse about (1.3.30, the owner: it should be
     * busy about the house, not hanging on them).
     */
    /**
     * 2.0.8: how long a step at {@code gesture} lasts: the stove's whole round of cooking (2.4.9: {@link #COOKING}, the fire fed, stirred,
     * seasoned, tasted), anything else once.
     */
    public static long stepTicks(EtoGesture gesture) {
        return (gesture == EtoGesture.COOK ? cookingTicks() : (long)gesture.ticks) - 4L;
    }

    /**
     * 2.4.9 (the owner: "Spouse when cooking food has more animations"): what its hands do at the stove, in turn: the fire fed, stirred,
     * a pinch of something shaken in, stirred again, a taste off the spoon, stirred once more. Each for its gesture's length.
     */
    public static final EtoGesture[] COOKING = {EtoGesture.STOKE, EtoGesture.COOK, EtoGesture.SEASON, EtoGesture.COOK, EtoGesture.TASTE,
        EtoGesture.COOK};

    /** 2.4.9: how long the whole of {@link #COOKING} takes (ticks). */
    public static long cookingTicks() {
        long sum = 0L;
        for (EtoGesture g : COOKING) {
            sum += g.ticks;
        }
        return sum;
    }

    /** 2.4.9: what its hands do {@code age} ticks into its time at the stove (the last of {@link #COOKING} once it has run through). */
    public static EtoGesture cooking(long age) {
        long at = Math.max(0L, age);
        for (EtoGesture g : COOKING) {
            if (at < g.ticks) {
                return g;
            }
            at -= g.ticks;
        }
        return COOKING[COOKING.length - 1];
    }

    /** 2.4.9: when (ticks into its time at the stove) the gesture it is at began. */
    public static long cookingFrom(long age) {
        long start = 0L;
        long at = Math.max(0L, age);
        for (EtoGesture g : COOKING) {
            if (at < g.ticks) {
                return start;
            }
            at -= g.ticks;
            start += g.ticks;
        }
        return start - COOKING[COOKING.length - 1].ticks;
    }

    /**
     * 2.4.9 (the owner: "go directly infront of the furnace"): the way a stove faces, as a step from its block to the one in front of it
     * ({@code state} as the game writes it: "Block{minecraft:furnace}[facing=north,lit=false]"): {dx, dz}, or null if it faces no way
     * (a campfire faces one too, but has no front to stand at; it is not a stove now).
     */
    public static int[] front(String state) {
        if (state == null) {
            return null;
        }
        int i = state.indexOf("facing=");
        if (i < 0) {
            return null;
        }
        String rest = state.substring(i + 7);
        if (rest.startsWith("north")) {
            return new int[] {0, -1};
        }
        if (rest.startsWith("south")) {
            return new int[] {0, 1};
        }
        if (rest.startsWith("west")) {
            return new int[] {-1, 0};
        }
        if (rest.startsWith("east")) {
            return new int[] {1, 0};
        }
        return null;
    }

    /** 2.4.9: how close to the spot in front of the stove counts as there (blocks, to its middle). */
    public static final double FRONT = 0.6;
    /** 2.4.9: how long it tries for the spot in front before it cooks from where it got to (ticks), as long as it is within reach. */
    public static final long FRONT_TRY = 120L;

    /**
     * 2.0.8 (the owner: "with sizzling sounds"; vanilla's crackle, by their word, the one sound not from the hexkagune pack): the sound a
     * stove makes while it is cooked at ({@code block} as the game writes it: "Block{minecraft:furnace}"; a sound of minecraft:), or null
     * (a table: no fire).
     */
    public static String crackle(String block) {
        String b = block == null ? "" : block;
        if (b.contains("smoker")) {
            return "block.smoker.smoke";
        }
        if (b.contains("campfire")) {
            return "block.campfire.crackle";
        }
        return b.contains("furnace") ? "block.furnace.fire_crackle" : null;
    }

    public static long gap(boolean partnerNear, double roll) {
        double r = Math.max(0.0, Math.min(1.0, roll));
        return partnerNear ? 400L + (long)(r * 500.0) : 300L + (long)(r * 400.0);
    }

    /**
     * Which chore to do, weighed by what the home has ({@code counts}, by {@link Kind#ordinal}: how many of each it found; {@code items}:
     * things lying about on the floor, which make the sweeping likelier). The one just done is less likely again; null if nothing at all.
     */
    public static Kind pick(int[] counts, Kind last, boolean items, double roll) {
        double[] weights = new double[Kind.values().length];
        double total = 0.0;
        for (Kind kind : Kind.values()) {
            int count = counts == null || kind.ordinal() >= counts.length ? 0 : counts[kind.ordinal()];
            double w = switch (kind) {
                case TIDY -> count > 0 ? 3.0 : 0.0;
                case SWEEP -> items ? 9.0 : count > 0 ? 2.5 : 0.0;
                case WIPE -> count > 0 ? 2.0 : 0.0;
                case WATER -> count > 0 ? 3.5 : 0.0;
                case READ -> count > 0 ? 1.0 : 0.0;
                case LOOK -> count > 0 ? 0.8 : 0.0;
                case COOK -> count > 0 ? 4.0 : 0.0;
                case SMELT -> count > 0 ? 3.5 : 0.0;
                case HARVEST -> count > 0 ? 5.0 : 0.0;
            };
            if (kind == last && !(kind == Kind.SWEEP && items)) {
                w *= 0.25;
            }
            weights[kind.ordinal()] = w;
            total += w;
        }
        if (total <= 0.0) {
            return null;
        }
        double at = Math.max(0.0, Math.min(0.999999, roll)) * total;
        for (Kind kind : Kind.values()) {
            at -= weights[kind.ordinal()];
            if (at < 0.0 && weights[kind.ordinal()] > 0.0) {
                return kind;
            }
        }
        for (int i = weights.length - 1; i >= 0; i--) {
            if (weights[i] > 0.0) {
                return Kind.values()[i];
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- a chest put in order

    /** One stack in a chest as the sorting sees it: what it is (its item and its data: only equal keys stack), how many, how many stack. */
    public record Pile(String key, int count, int max) {
    }

    /**
     * A chest's stacks put in order ({@code piles}: its slots, null or 0-count for empty ones; {@code size}: its slots): the same things
     * stacked together as far as they stack, sorted by name, then the rest empty. Nothing is ever lost or made: every key's total is
     * kept. Null if it would not fit (a stack already past its own limit) or there is nothing in it.
     */
    public static List<Pile> sort(List<Pile> piles, int size) {
        Map<String, int[]> totals = new LinkedHashMap<>();
        for (Pile pile : piles) {
            if (pile == null || pile.count() <= 0 || pile.key() == null) {
                continue;
            }
            int[] total = totals.computeIfAbsent(pile.key(), k -> new int[] {0, Math.max(1, pile.max())});
            total[0] += pile.count();
            total[1] = Math.max(1, Math.min(total[1], pile.max()));
        }
        if (totals.isEmpty()) {
            return null;
        }
        List<String> keys = new ArrayList<>(totals.keySet());
        keys.sort(Comparator.naturalOrder());
        List<Pile> out = new ArrayList<>();
        for (String key : keys) {
            int[] total = totals.get(key);
            int left = total[0];
            while (left > 0) {
                int take = Math.min(left, total[1]);
                out.add(new Pile(key, take, total[1]));
                left -= take;
            }
        }
        return out.size() <= size ? out : null;
    }

    /** True if {@code piles} are already as {@link #sort} would leave them (nothing to do). */
    public static boolean sorted(List<Pile> piles, int size) {
        List<Pile> want = sort(piles, size);
        if (want == null) {
            return true;
        }
        int i = 0;
        for (Pile pile : piles) {
            boolean empty = pile == null || pile.count() <= 0;
            if (i < want.size()) {
                if (empty || !want.get(i).key().equals(pile.key()) || want.get(i).count() != pile.count()) {
                    return false;
                }
            } else if (!empty) {
                return false;
            }
            i++;
        }
        return i >= want.size();
    }

    // ---------------------------------------------------------------- its words

    /** What it says starting a chore ({@code key}: the companion's), to its spouse near it; {@code seed} picks among its lines. */
    public static String start(String key, Kind kind, int seed) {
        String[] lines = switch (household(key)) {
            case "touka" -> switch (kind) {
                case TIDY -> new String[] {"*opens the chest* Seriously, how do you find anything in here? ...Don't touch it. I'm doing it.",
                    "*digs through the chest* Who throws dirt in with the food? You. Only you."};
                case SWEEP -> new String[] {"*sweeps* You track half the forest in here every single day.",
                    "*sweeps hard* Wipe your feet. I'm not saying it again. ...I'm saying it again."};
                case WIPE -> new String[] {"*wipes the table down* We're not animals. Use a plate.", "*scrubs a ring off the table* Coasters exist."};
                case WATER -> new String[] {"*waters the plants* ...What? They were dying. Somebody had to.",
                    "*tips the bucket over the flowers* Don't look at me like that. I like flowers. So what."};
                case READ -> new String[] {"*takes a book off the shelf* ...It's for studying. Shut up.", "*pulls out a book* Don't talk to me for ten minutes."};
                case LOOK -> new String[] {"*looks out the window* ...Just checking nobody's out there.", "*stares out the window* Quiet. Too quiet. ...I'm fine."};
                case COOK -> new String[] {"*ties her hair back and lights the stove* Don't hover.", "*at the stove* I can't taste it. I just watch it. Shut up."};
                case SMELT -> new String[] {"*loads the furnace* Who leaves raw ore lying in a chest for a week?", "*feeds the furnace* Fine. I'll do it."};
                case HARVEST -> new String[] {"*crouches in the field* These are ready. Of course nobody noticed.", "*picks the crops* Hand me a basket. ...Never mind."};
            };
            case "ken" -> switch (kind) {
                case TIDY -> new String[] {"*opens the chest* I'll sort this out. ...Do you mind if I put the books together?",
                    "*kneels by the chest* It's calming, putting things where they belong."};
                case SWEEP -> new String[] {"*sweeps the floor* I like doing this. It keeps my hands busy.", "*sweeps* Dust gets everywhere, doesn't it."};
                case WIPE -> new String[] {"*wipes the table* I'll make coffee after this. You want some?", "*wipes the table slowly* There. Like new."};
                case WATER -> new String[] {"*waters the flowers* They're doing well. They just needed someone to notice them.",
                    "*waters the plants* Somebody has to look after them. I don't mind."};
                case READ -> new String[] {"*takes down a book* I've read this one before. It's better the second time.",
                    "*runs a finger along the shelf* So many I haven't read yet."};
                case LOOK -> new String[] {"*looks out the window* It's peaceful here. I didn't think I'd ever have that.",
                    "*watches the sky through the glass* ...Sorry. I was miles away."};
                case COOK -> new String[] {"*at the stove* I'll make something. You should eat properly.", "*stirs the pot* I used to watch my mother do this."};
                case SMELT -> new String[] {"*loads the furnace* I'll smelt this ore for you. It's no trouble.", "*feeds the furnace with coal* Almost meditative."};
                case HARVEST -> new String[] {"*kneels in the field* These are ripe. I'll bring them in.", "*picks the crops carefully* Look how well they grew."};
            };
            default -> switch (kind) {
                case TIDY -> new String[] {"*opens the chest* ...Who puts rotten flesh next to the bread? A monster. You. You're the monster.",
                    "*rummages in the chest* Let me sort this. Alphabetically. No: by how much I like each thing."};
                case SWEEP -> new String[] {"*sweeps* Dust, dust, dust. Where does it all come from? Is it you? It's you.",
                    "*sweeps and hums* A broom and a book. Every girl's dream, apparently."};
                case WIPE -> new String[] {"*wipes the table* Leave one more ring on this table and I'm writing you into a tragedy.",
                    "*polishes the table* Clean enough to eat off. Not that I eat off tables. Mostly."};
                case WATER -> new String[] {"*waters the flowers* Drink up, little ones. Grow tall.",
                    "*tips the bucket over the garden* I'm a very good gardener. Ask the flowers. They can't argue."};
                case READ -> new String[] {"*pulls a book off the shelf* Just one chapter. ...Okay, three.",
                    "*takes down a book* Your shelf is a crime. Half of these are unread. Tragic."};
                case LOOK -> new String[] {"*looks out the window* Quiet out there today. I like it. Don't tell anyone I like quiet.",
                    "*leans on the window* Somebody's out there. ...No. Just a cow. Disappointing."};
                case COOK -> new String[] {"*at the stove* Cooking. For you. Write this day down.", "*stirs something* I read twelve cookbooks. Theory is everything."};
                case SMELT -> new String[] {"*loads the furnace* Ore in a chest is a crime against productivity.", "*feeds the furnace* Burn, little rocks."};
                case HARVEST -> new String[] {"*in the field* Ripe. Mine now. Well, ours.", "*picks the crops* Farming. Me. If my readers could see me."};
            };
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** What it says when it is done ({@code more}: the sweeping put things from the floor away; the tidying put a chest in order). */
    public static String done(String key, Kind kind, boolean more, int seed) {
        String[] lines = switch (household(key)) {
            case "touka" -> switch (kind) {
                case TIDY -> more ? new String[] {"*shuts the lid* Done. Mess it up again and I'm putting you in the chest."}
                    : new String[] {"*shuts the lid* ...Huh. You kept it tidy. Don't let it go to your head."};
                case SWEEP -> more ? new String[] {"Your junk was all over the floor. It's in the chest. Don't make it a habit."}
                    : new String[] {"*leans the brush on the wall* There. Floor's clean. Try to keep it that way."};
                case WATER -> new String[] {"*sets the bucket down* They'll live. ...Not that I care."};
                default -> new String[] {"*dusts off her hands* There."};
            };
            case "ken" -> switch (kind) {
                case TIDY -> more ? new String[] {"*closes the chest* There. It's easier to find things now. I hope you don't mind."}
                    : new String[] {"*closes the chest* It was already tidy. You're better at this than you think."};
                case SWEEP -> more ? new String[] {"I found a few of your things on the floor. They're in the chest now."}
                    : new String[] {"*puts the brush away* Done. It's nice, a clean floor."};
                case WATER -> new String[] {"*sets the bucket down* That's all of them. They'll bloom soon, I think."};
                default -> new String[] {"*straightens up* That's that."};
            };
            default -> switch (kind) {
                case TIDY -> more ? new String[] {"*shuts the lid* There. Everything where it belongs. Like you: at home, with me."}
                    : new String[] {"*shuts the lid* Already tidy? Who are you and what did you do with my spouse?"};
                case SWEEP -> more ? new String[] {"Things all over the floor again. I put them in the chest. You're welcome, slob."}
                    : new String[] {"*twirls the brush* Spotless. You may now worship me."};
                case WATER -> new String[] {"*swings the empty bucket* Done. If they don't bloom I'll have a word with them."};
                default -> new String[] {"*dusts off her hands* Done. I'm a wonderful wife. Write that down."};
            };
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /**
     * What it says when asked to go home ({@code married}: to the home they share; else back to where it belongs, {@code place}: its own
     * home's name).
     */
    public static String goingHome(String key, boolean married, String place, int seed) {
        String[] lines = switch (household(key)) {
            case "touka" -> married ? new String[] {"Fine. I'm going. ...Don't stay out too late, idiot.", "Home. Right. I'll leave a light on. Not for you."}
                : new String[] {"Fine. Back to " + place + ". Somebody has to work.", "Whatever. I'm going. ...Come by sometime."};
            case "ken" -> married ? new String[] {"Okay. I'll head home. I'll have something warm waiting.", "Alright. Be careful out here. I'll see you at home."}
                : new String[] {"Alright. I'll go back to " + place + ". Come find me.", "Okay. I'll be at " + place + " if you need me."};
            default -> married ? new String[] {"Home it is. I'll put the kettle on. Don't dawdle.", "Sending me home? Fine. I'll be in bed with a book. Hint."}
                : new String[] {"Back to " + place + ", then. Visit me. Bring a book.", "Sent home like a cat. Rude. ...I'll go."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** 4.0.0: whose household words {@code key} speaks: Ayato Ken's (the owner: his romance "quite similar to kaneki"), the rest their own. */
    static String household(String key) {
        return "ayato".equals(key) ? "ken" : key;
    }
}

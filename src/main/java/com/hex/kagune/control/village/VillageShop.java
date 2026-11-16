package com.hex.kagune.control.village;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 2.0.13 (the owner: "check how the trading works with the Human NPCS I couldn't find a way", then: "they mostly trade basic stuff like
 * villagers, but a lot more food, the ones needed for our recipes on touka (not meat)"): what a village human sells a player and buys from
 * them, for emeralds (pure, tested). Each sells seven things for life (from its id; 2.1.3: by its trade, {@link HumanLife.Job}: five of
 * its trade's own wares, one of the cookbook's foods, one farming or basic good; never meat, never fish); and it buys the field's crops and
 * plain goods, as a village would. Paid with the emeralds
 * in the player's hand; sold from what is in their hand.
 */
public final class VillageShop {
    /** {@code count} of {@code item} ("minecraft:wheat"; {@code name} as said) for {@code price} emeralds. */
    public record Offer(String item, String name, int count, int price) {
        /** What the page says for buying it: "12 wheat: 1 emerald". */
        public String label() {
            return this.count + " " + this.name + ": " + this.price + (this.price == 1 ? " emerald" : " emeralds");
        }
    }

    public static final String EMERALD = "minecraft:emerald";

    /** The staples, one of which every seller has (the cookbook's most used). */
    public static final Offer[] STAPLES = {
        new Offer("minecraft:wheat", "wheat", 12, 1),
        new Offer("minecraft:egg", "eggs", 8, 1),
        new Offer("minecraft:sugar", "sugar", 12, 1),
        new Offer("minecraft:bread", "bread", 6, 1),
    };

    /** The cookbook's other foods (never meat, never fish). */
    public static final Offer[] FOODS = {
        new Offer("minecraft:beetroot", "beetroots", 12, 1),
        new Offer("minecraft:brown_mushroom", "brown mushrooms", 6, 1),
        new Offer("minecraft:red_mushroom", "red mushrooms", 6, 1),
        new Offer("minecraft:carrot", "carrots", 12, 1),
        new Offer("minecraft:potato", "potatoes", 12, 1),
        new Offer("minecraft:baked_potato", "baked potatoes", 8, 1),
        new Offer("minecraft:cocoa_beans", "cocoa beans", 8, 1),
        new Offer("minecraft:dried_kelp", "dried kelp", 12, 1),
        new Offer("minecraft:kelp", "kelp", 12, 1),
        new Offer("minecraft:sweet_berries", "sweet berries", 12, 1),
        new Offer("minecraft:glow_berries", "glow berries", 8, 1),
        new Offer("minecraft:honey_bottle", "honey bottles", 2, 1),
        new Offer("minecraft:milk_bucket", "bucket of milk", 1, 2),
        new Offer("minecraft:apple", "apples", 6, 1),
        new Offer("minecraft:pumpkin", "pumpkins", 4, 1),
        new Offer("minecraft:golden_apple", "golden apple", 1, 10),
        new Offer("minecraft:wheat", "wheat", 12, 1),
        new Offer("minecraft:egg", "eggs", 8, 1),
        new Offer("minecraft:sugar", "sugar", 12, 1),
        new Offer("minecraft:bread", "bread", 6, 1),
    };

    /** 2.0.14 (the owner: "seeds, farming materials, food etc"): the field's things: seeds, bone meal, saplings, cane, hay, tools. */
    public static final Offer[] FARMING = {
        new Offer("minecraft:wheat_seeds", "wheat seeds", 16, 1),
        new Offer("minecraft:beetroot_seeds", "beetroot seeds", 16, 1),
        new Offer("minecraft:pumpkin_seeds", "pumpkin seeds", 8, 1),
        new Offer("minecraft:melon_seeds", "melon seeds", 8, 1),
        new Offer("minecraft:bone_meal", "bone meal", 12, 1),
        new Offer("minecraft:oak_sapling", "oak saplings", 4, 1),
        new Offer("minecraft:sugar_cane", "sugar cane", 8, 1),
        new Offer("minecraft:hay_block", "hay bales", 2, 1),
        new Offer("minecraft:stone_hoe", "stone hoe", 1, 1),
        new Offer("minecraft:iron_hoe", "iron hoe", 1, 3),
        new Offer("minecraft:bucket", "bucket", 1, 2),
        new Offer("minecraft:composter", "composter", 1, 2),
    };

    /** Basic goods, as a village's: the kitchen's bowls, bottles and odds and ends, and plain things. */
    public static final Offer[] BASICS = {
        new Offer("minecraft:bowl", "bowls", 8, 1),
        new Offer("minecraft:glass_bottle", "glass bottles", 6, 1),
        new Offer("minecraft:water_bucket", "bucket of water", 1, 2),
        new Offer("minecraft:stick", "sticks", 32, 1),
        new Offer("minecraft:bone", "bones", 8, 1),
        new Offer("minecraft:ink_sac", "ink sacs", 4, 1),
        new Offer("minecraft:slime_ball", "slimeballs", 2, 1),
        new Offer("minecraft:snowball", "snowballs", 16, 1),
        new Offer("minecraft:blaze_powder", "blaze powder", 1, 3),
        new Offer("minecraft:coal", "coal", 8, 1),
        new Offer("minecraft:torch", "torches", 16, 1),
        new Offer("minecraft:paper", "paper", 16, 1),
        new Offer("minecraft:arrow", "arrows", 16, 1),
        new Offer("minecraft:string", "string", 8, 1),
        new Offer("minecraft:leather", "leather", 4, 1),
        new Offer("minecraft:flint", "flint", 10, 1),
        new Offer("minecraft:iron_ingot", "iron ingot", 1, 2),
    };

    /** What it buys from a player: {@code count} of it for {@code price} emeralds (a village's crops and plain goods). */
    public static final Offer[] BUYS = {
        new Offer("minecraft:wheat", "wheat", 20, 1),
        new Offer("minecraft:potato", "potatoes", 26, 1),
        new Offer("minecraft:carrot", "carrots", 22, 1),
        new Offer("minecraft:beetroot", "beetroots", 15, 1),
        new Offer("minecraft:pumpkin", "pumpkins", 6, 1),
        new Offer("minecraft:melon", "melons", 4, 1),
        new Offer("minecraft:sugar_cane", "sugar cane", 18, 1),
        new Offer("minecraft:sweet_berries", "sweet berries", 24, 1),
        new Offer("minecraft:apple", "apples", 10, 1),
        new Offer("minecraft:egg", "eggs", 16, 1),
        new Offer("minecraft:paper", "paper", 24, 1),
        new Offer("minecraft:string", "string", 14, 1),
        new Offer("minecraft:coal", "coal", 15, 1),
        new Offer("minecraft:stick", "sticks", 32, 1),
        new Offer("minecraft:leather", "leather", 6, 1),
        new Offer("minecraft:feather", "feathers", 24, 1),
        new Offer("minecraft:flint", "flint", 26, 1),
        new Offer("minecraft:iron_ingot", "iron ingots", 4, 1),
    };

    /** How many foods (the staple among them), farming goods and basic goods each one sells (2.0.14: seven in all). */
    public static final int FOOD_OFFERS = 3;
    public static final int FARMING_OFFERS = 2;
    public static final int BASIC_OFFERS = 2;

    private VillageShop() {
    }

    /** 2.1.3: how many of its trade's own wares each one sells (its trade's first always among them); the rest from the village's goods. */
    public static final int TRADE_OFFERS = 5;

    /** What one sells ({@code seed}: its own, always the same), by the job its life gives it ({@link HumanLife#of}). */
    public static List<Offer> stock(long seed) {
        return stock(seed, HumanLife.of(seed, null).job());
    }

    /**
     * 2.1.3 (the owner: "different trades per NPC"): what one sells by its trade: its trade's first ware and four more of its own (a baker
     * bread, cookies, cake...; a smith iron, buckets, shears...), then one of the cookbook's foods and one farming or basic good, none twice:
     * seven in all, the same for life.
     */
    public static List<Offer> stock(long seed, HumanLife.Job job) {
        Random random = new Random(seed ^ 0x5A0B5L);
        List<Offer> out = new ArrayList<>();
        out.add(job.wares.get(0));
        pick(out, job.wares.subList(1, job.wares.size()).toArray(new Offer[0]), TRADE_OFFERS - 1, random);
        pick(out, FOODS, 1, random);
        Offer[] rest = new Offer[FARMING.length + BASICS.length];
        System.arraycopy(FARMING, 0, rest, 0, FARMING.length);
        System.arraycopy(BASICS, 0, rest, FARMING.length, BASICS.length);
        pick(out, rest, FOOD_OFFERS + FARMING_OFFERS + BASIC_OFFERS - out.size(), random);
        return out;
    }

    private static void pick(List<Offer> out, Offer[] from, int want, Random random) {
        List<Offer> left = new ArrayList<>();
        for (Offer offer : from) {
            if (out.stream().noneMatch(o -> o.item().equals(offer.item()))) {
                left.add(offer);
            }
        }
        for (int i = 0; i < want && !left.isEmpty(); i++) {
            out.add(left.remove(random.nextInt(left.size())));
        }
    }

    /** What it pays for {@code item} ("minecraft:wheat"), or null: it doesn't buy that. */
    public static Offer buys(String item) {
        for (Offer offer : BUYS) {
            if (offer.item().equals(item)) {
                return offer;
            }
        }
        return null;
    }

    /** How many lots of {@code offer} {@code held} of it make (what it would buy now), and the emeralds paid for them. */
    public static int lots(Offer offer, int held) {
        return offer == null || offer.count() <= 0 ? 0 : held / offer.count();
    }

    /** As the wares are shown. */
    public static String hello(int seed) {
        return pick(seed, "Take a look. Fresh today.", "Something for your kitchen?", "Emeralds, and it's yours.", "I've a bit of everything. Mostly food.",
            "Hungry? Or cooking for someone?");
    }

    /** Back to talking. */
    public static String done(int seed) {
        return pick(seed, "Anything else on your mind?", "Sure. What is it?", "Business can wait.");
    }

    /** What they have to pay with, as the page says it after its words. */
    public static String purse(int emeralds) {
        return emeralds <= 0 ? "(Hold emeralds in your hand to buy. Hold something it wants to sell it.)"
            : "(In your hand: " + emeralds + (emeralds == 1 ? " emerald.)" : " emeralds.)");
    }

    /** They can't pay for {@code offer} ({@code emeralds} in their hand). */
    public static String short_(Offer offer, int emeralds, int seed) {
        String need = offer.price() + (offer.price() == 1 ? " emerald" : " emeralds");
        return emeralds <= 0 ? pick(seed, "That's " + need + ". Hold them out and it's yours.", "Emeralds first, friend. " + need + ".")
            : pick(seed, "That's " + need + ", and you've only " + emeralds + ".", "Not quite enough. " + need + ", please.");
    }

    /** It sold them {@code offer}. */
    public static String sold(Offer offer, int seed) {
        return pick(seed, "There you go. " + capital(offer.count() + " " + offer.name()) + ".", "Pleasure doing business.", "Enjoy it. Come back anytime.",
            "Here. Cook something nice with it.");
    }

    /** It bought {@code lots} of {@code offer} from them. */
    public static String bought(Offer offer, int lots, int seed) {
        int emeralds = lots * offer.price();
        return pick(seed, "Good " + offer.name() + ". Here's " + emeralds + (emeralds == 1 ? " emerald." : " emeralds."), "Fair's fair. "
            + emeralds + (emeralds == 1 ? " emerald" : " emeralds") + " for you.", "I'll take those. Thank you.");
    }

    private static String capital(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** What the page says for selling what is held: "Sell 40 wheat: 2 emeralds". */
    public static String sellLabel(Offer offer, int held) {
        int lots = lots(offer, held);
        int emeralds = lots * offer.price();
        return "Sell " + lots * offer.count() + " " + offer.name() + ": " + emeralds + (emeralds == 1 ? " emerald" : " emeralds");
    }
}

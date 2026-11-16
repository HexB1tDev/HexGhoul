package com.hex.kagune.control.village;

import java.util.List;

/**
 * 1.3.72: what the Brick Village's humans say to each other (pure, tested): a trade in the streets (the owner: "Humans often trade in the
 * streets"), four or five lines between a seller and a buyer, the goods and the coin changing hands on the line marked for it; and a
 * word as two of them pass (a greeting, a bit of gossip, worry about ghouls, praise for the CCG), and on the way home at dusk.
 */
public final class VillageTalk {
    private VillageTalk() {
    }

    /** What is sold: its item (as 1.20.1 has it), what it is called, what it costs. */
    public record Goods(String item, String name, String price) {
    }

    /** One line of a trade: the seller's or the buyer's; {@code hands}: the goods and the coin change hands as it is said. */
    public record Line(boolean seller, String text, boolean hands) {
    }

    /** What changes hands for the goods. */
    public static final String COIN = "minecraft:emerald";

    public static final Goods[] GOODS = {
        new Goods("minecraft:bread", "bread", "two emeralds"),
        new Goods("minecraft:apple", "apples", "one emerald"),
        new Goods("minecraft:carrot", "carrots", "one emerald"),
        new Goods("minecraft:baked_potato", "baked potatoes", "one emerald"),
        new Goods("minecraft:cookie", "cookies", "an emerald a dozen"),
        new Goods("minecraft:pumpkin_pie", "pumpkin pie", "three emeralds"),
        new Goods("minecraft:cod", "fish", "two emeralds"),
        new Goods("minecraft:sweet_berries", "berries", "one emerald"),
        new Goods("minecraft:honey_bottle", "honey", "two emeralds"),
        new Goods("minecraft:poppy", "flowers", "one emerald"),
        new Goods("minecraft:paper", "paper", "one emerald"),
        new Goods("minecraft:candle", "candles", "two emeralds"),
    };

    /** The goods by {@code seed}. */
    public static Goods goods(int seed) {
        return GOODS[Math.floorMod(seed, GOODS.length)];
    }

    /** A trade of {@code goods}: the seller calls it, the buyer asks, a price, the coin and the goods change hands, a word to part on. */
    public static List<Line> trade(Goods goods, int seed) {
        String name = goods.name();
        String price = goods.price();
        String upper = Character.toUpperCase(price.charAt(0)) + price.substring(1);
        return List.of(
            new Line(true, pick(seed, "Fresh " + name + ", best in the village!", capital(name) + "! Who wants " + name + "?",
                "Get your " + name + " here, still good!", "Last of the " + name + " today, come and see!"), false),
            new Line(false, pick(seed >> 2, "How much for the " + name + "?", "I'll take some " + name + ". What's the damage?",
                "Are the " + name + " fresh? Really fresh?", "My little one loves " + name + ". How much?"), false),
            new Line(true, pick(seed >> 4, "For you? " + price + ".", upper + ", and I'm robbing myself.",
                upper + ". Don't tell my neighbour I gave you that price.", upper + ". Same as always."), false),
            new Line(false, pick(seed >> 6, "Deal. *hands over the coin*", "Fine, fine. *pays*", "You drive a hard bargain. Here.",
                "*counts out the coin* There."), true),
            new Line(true, pick(seed >> 8, "Pleasure! Come back tomorrow.", "Stay safe out there. Ghouls about at night.", "Thank you kindly!",
                "Get home before dark, alright?"), false));
    }

    /** A word as two pass each other in the streets. */
    public static String greet(int seed) {
        return pick(seed, "Morning!", "Lovely day, isn't it?", "Lock your door tonight. They say a ghoul was seen near the fields.",
            "The CCG walked by again. I feel safer already.", "Have you eaten? You look pale.", "*waves*",
            "Did you hear? Someone's moving into the empty house.", "Busy day at the market!", "Don't forget the bell if you see anything strange.",
            "*tips an imaginary hat*");
    }

    /** A word on the way home as it gets dark. */
    public static String dusk(int seed) {
        return pick(seed, "Getting dark. Home I go.", "Don't stay out late!", "Doors locked, lights on. Good night.",
            "*hurries home, glancing over a shoulder*");
    }

    static String capital(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}

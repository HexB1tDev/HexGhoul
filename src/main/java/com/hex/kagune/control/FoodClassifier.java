package com.hex.kagune.control;

import java.util.Locale;
import java.util.Set;

/**
 * Name-based fallback used when an edible item is in none of the {@code hexkagune:ghoul_food},
 * {@code hexkagune:ghoul_tolerated} or {@code hexkagune:not_ghoul_food} item tags. This lets raw
 * meat from other mods work without a compatibility patch while cooked and prepared food is rejected.
 */
public final class FoodClassifier {
    public enum Diet {
        /** Raw flesh: satisfies a ghoul, with bonus nourishment. */
        GHOUL_FOOD,
        /** Coffee and water-like drinks: harmless, but no nourishment. */
        TOLERATED,
        /** Ordinary prepared food: rejected by a ghoul's body. */
        HUMAN_FOOD
    }

    private static final Set<String> PREPARED = Set.of(
        "cooked", "roasted", "roast", "baked", "fried", "grilled", "smoked", "boiled", "steamed", "seared",
        "stew", "soup", "pie", "sandwich", "burger", "hamburger", "wrap", "kebab", "skewer", "curry",
        "dumpling", "jerky", "dried", "sushi", "bread", "cake", "cookie", "salad", "pasta", "noodle",
        "noodles", "rice", "steak", "sausage", "meatball", "meatballs", "nugget", "nuggets", "pizza",
        "taco", "omelette", "omelet", "pudding", "porridge", "casserole", "risotto", "barbecue", "bbq",
        "glazed", "stuffed", "toast", "bowl", "plate", "platter", "feast", "candy", "chocolate", "honey",
        "jam", "juice", "pastry", "tart", "muffin", "pancake", "waffle", "cereal");
    private static final Set<String> FLESH = Set.of(
        "beef", "porkchop", "pork", "chicken", "mutton", "rabbit", "venison", "meat", "flesh", "fillet",
        "filet", "cutlet", "slice", "ribs", "rib", "loin", "brisket", "bacon", "ham", "shank", "heart",
        "liver", "kidney", "organ", "organs", "brain", "tongue", "eye", "gizzard", "drumstick", "wing",
        "thigh", "breast", "leg", "haunch", "patty", "mince", "minced", "goat", "lamb", "veal", "duck",
        "turkey", "goose", "horse", "boar", "deer", "frog", "snail", "bug", "insect", "tentacle");
    private static final Set<String> FISH = Set.of(
        "fish", "cod", "salmon", "tuna", "trout", "bass", "carp", "eel", "squid", "shrimp", "prawn",
        "crab", "lobster", "clam", "oyster", "octopus", "calamari", "roe", "caviar", "pufferfish",
        "anchovy", "sardine", "herring", "mackerel", "catfish", "perch", "pike", "tilapia", "minnow",
        "piranha", "shark", "jellyfish", "scallop", "mussel");
    private static final Set<String> TOLERATED = Set.of("coffee", "espresso", "latte", "cappuccino", "water");

    private FoodClassifier() {
    }

    /**
     * @param path   item registry path, e.g. {@code cooked_beef} or {@code raw_venison}
     * @param isMeat {@code FoodProperties#isMeat()} for the item
     */
    public static Diet classify(String path, boolean isMeat) {
        if (path == null || path.isBlank()) {
            return isMeat ? Diet.GHOUL_FOOD : Diet.HUMAN_FOOD;
        }
        String[] tokens = path.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        boolean raw = false;
        boolean prepared = false;
        boolean flesh = false;
        boolean tolerated = false;
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            raw |= token.equals("raw") || token.equals("uncooked") || token.equals("fresh") || token.equals("rotten");
            prepared |= PREPARED.contains(token);
            flesh |= FLESH.contains(token) || FISH.contains(token);
            tolerated |= TOLERATED.contains(token);
        }
        if (tolerated && !flesh) {
            return Diet.TOLERATED;
        }
        if (raw && !prepared) {
            return isMeat || flesh ? Diet.GHOUL_FOOD : Diet.HUMAN_FOOD;
        }
        if (prepared) {
            return Diet.HUMAN_FOOD;
        }
        return isMeat || flesh ? Diet.GHOUL_FOOD : Diet.HUMAN_FOOD;
    }
}

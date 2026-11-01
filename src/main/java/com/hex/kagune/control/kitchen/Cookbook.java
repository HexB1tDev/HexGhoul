package com.hex.kagune.control.kitchen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Home Living (1.3.41): the ghoul cookbook's pages (pure, tested): Touka's book, traded for {@link #PRICE} cocoa beans once she likes you
 * ({@link #REGARD}). It opens on its cover (a picture, its title, the contents); each chapter has its own spread (its picture, a few
 * words, every dish in it as a grid of icons to pick from), then one spread a dish (its picture, its recipe, what it fills and what it
 * does); at the back, the kitchen's own four effects explained. Read in order a page at a time, or jumped about by the contents, the
 * chapter ribbons and the dishes' icons.
 */
public final class Cookbook {
    /** The cocoa beans she asks for it. */
    public static final int PRICE = 2;
    /** How much she has to like you before she'll part with it (NpcBonds.FRIENDLY). */
    public static final int REGARD = 20;
    /** The book's item path. */
    public static final String ID = "ghoul_cookbook";
    /** What you ask her, on her first page. */
    public static final String ASK = "Got any recipes? For ghouls.";
    /** What she says when you haven't the beans in your hand. */
    static final String[] PRICE_LINES = {
        "Two cocoa beans. In your hand. That's the price, idiot. Beans aren't free.",
        "My recipes cost two cocoa beans. The good ones. Hold them out like a normal person.",
        "You want my cookbook for nothing? Two cocoa beans. I'm not running a charity. ...Mostly.",
    };
    /** And when you have. */
    static final String[] GIVEN_LINES = {
        "...Fine. Here. My recipes. Don't spill blood on the pages. Well. Not too much.",
        "Take it. I wrote it for the cafe. If you burn the stew, don't tell me. And don't tell anyone it was me.",
        "Here. The coffee page is the important one. Read that first. The rest is just food.",
    };

    /** What a spread shows. */
    public enum Kind {
        COVER, CHAPTER, DISH, EFFECTS
    }

    /** One spread: what it is, the chapter it is in (null for the cover and the effects), the dish on it (DISH only). */
    public record Spread(Kind kind, String chapter, Dish dish) {
    }

    private static final List<Spread> SPREADS;

    static {
        List<Spread> out = new ArrayList<>();
        out.add(new Spread(Kind.COVER, null, null));
        for (String chapter : Menu.CHAPTERS) {
            out.add(new Spread(Kind.CHAPTER, chapter, null));
            for (Dish dish : Dishes.chapter(chapter)) {
                out.add(new Spread(Kind.DISH, chapter, dish));
            }
        }
        out.add(new Spread(Kind.EFFECTS, null, null));
        SPREADS = Collections.unmodifiableList(out);
    }

    private Cookbook() {
    }

    public static List<Spread> spreads() {
        return SPREADS;
    }

    /** Spread {@code index}, kept inside the book. */
    public static Spread spread(int index) {
        return SPREADS.get(clamp(index));
    }

    public static int clamp(int index) {
        return Math.max(0, Math.min(SPREADS.size() - 1, index));
    }

    /** Where {@code chapter} begins (its own spread), or 0. */
    public static int chapterAt(String chapter) {
        for (int i = 0; i < SPREADS.size(); i++) {
            if (SPREADS.get(i).kind() == Kind.CHAPTER && SPREADS.get(i).chapter().equals(chapter)) {
                return i;
            }
        }
        return 0;
    }

    /** The spread of the dish {@code id}, or -1. */
    public static int dishAt(String id) {
        for (int i = 0; i < SPREADS.size(); i++) {
            Dish dish = SPREADS.get(i).dish();
            if (dish != null && dish.id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /** The effects' spread (the last). */
    public static int effectsAt() {
        return SPREADS.size() - 1;
    }

    /** The page number printed at the foot of spread {@code index}'s left page (two to a spread, from 1). */
    public static int pageNumber(int index) {
        return clamp(index) * 2 + 1;
    }

    /** True if a player with {@code regard} and {@code beans} cocoa beans can have it from her now. */
    public static boolean affordable(int regard, int beans) {
        return regard >= REGARD && beans >= PRICE;
    }

    /** Her line when you ask without the beans ({@code seed} picks one). */
    public static String price(int seed) {
        return PRICE_LINES[Math.floorMod(seed, PRICE_LINES.length)];
    }

    /** Her line as she hands it over. */
    public static String given(int seed) {
        return GIVEN_LINES[Math.floorMod(seed, GIVEN_LINES.length)];
    }

    /** {@code text} broken into lines no wider than {@code width} (as {@code measure} measures), at the spaces; a word too long alone. */
    public static List<String> wrap(String text, int width, ToIntFunction<String> measure) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String tried = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && measure.applyAsInt(tried) > width) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(tried);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}

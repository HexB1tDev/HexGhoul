package com.hex.kagune.control.npc;

import java.util.Map;

/**
 * 4.1.81 (the plan the owner approved, "whatever you think is best": "real dates at real places"): where each date of the talk scripts
 * ({@code date <id>}) is had, and when (pure, tested). A date chosen is no longer a scene at once: the companion follows them there, and its
 * scene plays when they arrive together ({@code CourtTrials}). The places are the world's: the companions' own (the Library, Anteiku, the
 * circus, the hideout, the den, the CCG tower and its roof) and kinds of place (somewhere high, the water, a field under the open sky, out in
 * the rain, a village).
 */
public final class DateSpot {
    private DateSpot() {
    }

    /** A kind of place: an official's own ({@link #home}: whose home it is), or one found by what is round the player. */
    public enum Place {
        LIBRARY("eto", "the Library"),
        CAFE("touka", "Anteiku"),
        CIRCUS("roma", "the circus"),
        HIDEOUT("ayato", "Aogiri's hideout"),
        DEN("banjou", "the Gas Masks' den"),
        TOWER("general", "the CCG tower"),
        ROOF("general", "the CCG tower's roof"),
        HIGH(null, "somewhere high"),
        WATER(null, "the water (a river, a lake, the sea)"),
        FIELD(null, "a field under the open sky"),
        RAIN(null, "out in the rain"),
        VILLAGE(null, "a village");

        /** Whose home it is (null: a kind of place, anywhere). */
        public final String home;
        public final String name;

        Place(String home, String name) {
            this.home = home;
            this.name = name;
        }
    }

    /** When: any time, by day, by night. */
    public enum When {
        ANY(""), DAY(" by day"), NIGHT(" at night");

        public final String words;

        When(String words) {
            this.words = words;
        }

        /** True at {@code time} (the day's ticks). */
        public boolean at(long time) {
            return switch (this) {
                case ANY -> true;
                case DAY -> Courtship.daylight(time);
                case NIGHT -> EtoRules.night(time);
            };
        }
    }

    /** Where and when one date is had. */
    public record Spot(Place place, When when) {
        /** "the water by day", "Anteiku at night". */
        public String words() {
            return this.place.name + this.when.words;
        }
    }

    /** Somewhere high: this high or more, with the sky over them. */
    public static final int HIGH = 90;
    /** How near a companion's home (blocks) is at their place. */
    public static final double AT_HOME = 30.0;
    /** How near the water (blocks) is at the water. */
    public static final int WATER_NEAR = 3;
    /** How long a date waits for them to get there (ticks: ten minutes). */
    public static final long WAITS = 12000L;
    /** How near (blocks) the companion must be, along with them, when they arrive. */
    public static final double ALONG = 16.0;

    /** Every date in the talk scripts, by its id: where and when it is had. */
    public static final Map<String, Spot> SPOTS = Map.ofEntries(
        Map.entry("eto_bookshop", new Spot(Place.LIBRARY, When.ANY)),
        Map.entry("eto_rooftop", new Spot(Place.HIGH, When.NIGHT)),
        Map.entry("eto_graveyard", new Spot(Place.VILLAGE, When.NIGHT)),
        Map.entry("eto_rain_reading", new Spot(Place.RAIN, When.ANY)),
        Map.entry("eto_stars", new Spot(Place.FIELD, When.NIGHT)),
        Map.entry("eto_kitchen", new Spot(Place.LIBRARY, When.NIGHT)),
        Map.entry("eto_museum", new Spot(Place.VILLAGE, When.DAY)),
        Map.entry("eto_tea", new Spot(Place.CAFE, When.ANY)),
        Map.entry("touka_movie", new Spot(Place.VILLAGE, When.NIGHT)),
        Map.entry("touka_beach", new Spot(Place.WATER, When.DAY)),
        Map.entry("touka_arcade", new Spot(Place.VILLAGE, When.DAY)),
        Map.entry("touka_roof_again", new Spot(Place.HIGH, When.ANY)),
        Map.entry("touka_cafe_closed", new Spot(Place.CAFE, When.NIGHT)),
        Map.entry("touka_study_park", new Spot(Place.FIELD, When.DAY)),
        Map.entry("touka_fireworks", new Spot(Place.FIELD, When.NIGHT)),
        Map.entry("touka_konbini", new Spot(Place.VILLAGE, When.NIGHT)),
        Map.entry("ken_bookstore", new Spot(Place.LIBRARY, When.DAY)),
        Map.entry("ken_river", new Spot(Place.WATER, When.ANY)),
        Map.entry("ken_rain_bench", new Spot(Place.RAIN, When.ANY)),
        Map.entry("ken_cook", new Spot(Place.CAFE, When.NIGHT)),
        Map.entry("ken_night_library", new Spot(Place.LIBRARY, When.NIGHT)),
        Map.entry("ken_stars", new Spot(Place.FIELD, When.NIGHT)),
        Map.entry("ken_picnic", new Spot(Place.FIELD, When.DAY)),
        Map.entry("ken_aquarium", new Spot(Place.WATER, When.DAY)),
        Map.entry("ayato_rooftop", new Spot(Place.HIGH, When.NIGHT)),
        Map.entry("ayato_arcade", new Spot(Place.VILLAGE, When.ANY)),
        Map.entry("ayato_ramen", new Spot(Place.VILLAGE, When.NIGHT)),
        Map.entry("ayato_park", new Spot(Place.FIELD, When.ANY)),
        Map.entry("ayato_spar_date", new Spot(Place.HIDEOUT, When.ANY)),
        Map.entry("ayato_stars", new Spot(Place.HIDEOUT, When.NIGHT)),
        Map.entry("general_roof", new Spot(Place.ROOF, When.ANY)),
        Map.entry("general_spar", new Spot(Place.TOWER, When.ANY)),
        Map.entry("general_dinner", new Spot(Place.VILLAGE, When.NIGHT)),
        Map.entry("roma_rooftops", new Spot(Place.HIGH, When.NIGHT)),
        Map.entry("roma_show_private", new Spot(Place.CIRCUS, When.NIGHT)),
        Map.entry("roma_graveyard", new Spot(Place.VILLAGE, When.NIGHT)),
        Map.entry("banjou_garden", new Spot(Place.DEN, When.NIGHT)),
        Map.entry("banjou_stars", new Spot(Place.DEN, When.NIGHT)),
        Map.entry("banjou_tea", new Spot(Place.DEN, When.ANY)));

    /** Where and when the date {@code id} is had (null: a date with no place, which plays where they stand, as before). */
    public static Spot of(String id) {
        return id == null ? null : SPOTS.get(id);
    }

    /** What a field is underfoot: grass (a meadow, a park, a lawn), moss, podzol. */
    public static boolean field(String under) {
        return "minecraft:grass_block".equals(under) || "minecraft:moss_block".equals(under) || "minecraft:podzol".equals(under);
    }

    /** True if somewhere high: at {@code y} or over {@link #HIGH}, nothing over them ({@code topHere}: the first open block over their column). */
    public static boolean high(double y, int topHere) {
        return y >= HIGH && y >= topHere - 0.5;
    }
}

package com.hex.kagune.control.npc;

/**
 * 4.1.82 (the owner: "the clown girl is not in the circus tent, either that or multiple of the tents are spawning one of the 2, fix it, now"):
 * the one-of-a-kind place each official NPC belongs to (pure, tested), and when a body of hers at that place takes her place in the world
 * from one somewhere else.
 *
 * <p>There is one of each in a world ({@code UniqueNpcs}): the first body to claim her is her, and, once anyone has met her, any other body of
 * hers leaves on its first tick. So a Roma met anywhere but her tent (made by an egg or a command, or carried off and lost) left the tent's
 * own Roma to vanish as the tent loaded, and she came back, after dying or going missing, where that other one had been: never in her
 * ring. Now the body at her own place is her (4.0.5, the owner: official NPCs "should always stay there unless married to a player and set
 * home in another place"): unless she is married, a body of hers in her place takes her slot from one whose home is not there.
 */
public final class HomePlace {
    private HomePlace() {
    }

    /** The structure (its id, as PlaceClaims keeps it) official NPC {@code key} lives in, or null for none of theirs. */
    public static String of(String key) {
        return switch (key == null ? "" : key) {
            case "eto" -> "library";
            case "touka", "ken" -> "anteiku";
            case "ayato", "noro" -> "aogiri_hideout";
            case "general" -> "ccg_building";
            case "roma" -> "circus";
            case "banjou" -> "gas_hideout";
            default -> null;
        };
    }

    /**
     * How far (blocks) from its claimed start chunk's middle {@code place} reaches: the chunk the game built it from, moved up to two chunks
     * onto its flattest ground (ControlStructures), and half its footprint (the mansion's grounds are its own: 180).
     */
    public static double reach(String place) {
        return switch (place == null ? "" : place) {
            case "aogiri_hideout" -> 180.0;
            case "circus" -> 80.0;
            case "ccg_building" -> 60.0;
            case "gas_hideout" -> 48.0;
            default -> 64.0;
        };
    }

    /** True if a body ({@code dx}, {@code dz} from {@code place}'s claimed chunk's middle) is at the place. */
    public static boolean within(String place, double dx, double dz) {
        double r = reach(place);
        return place != null && dx * dx + dz * dz <= r * r;
    }

    /**
     * True if official {@code key} stands in the very middle of her place (Roma, in the middle of the ring: her spot in the tent's template is
     * its footprint's middle, whichever way the tent is turned): where she is made if her place has none of her.
     */
    public static boolean middle(String key) {
        return "roma".equals(key);
    }

    /** The middle of a box ({x, y, z}: its middle across, the floor's height over its lowest layer). */
    public static int[] middle(int minX, int maxX, int minZ, int maxZ, int minY) {
        return new int[] {Math.floorDiv(minX + maxX, 2), minY + 1, Math.floorDiv(minZ + maxZ, 2)};
    }

    /**
     * 4.1.82: true if official {@code key} is made at her place now: someone has been there {@code waited} ticks (UniqueNpc.MISSING or
     * more), none of her is in it ({@code inPlace}), she is not out with someone ({@code along}: following or holding a hand) and not
     * married.
     */
    public static boolean remade(boolean inPlace, boolean along, boolean married, long waited, long missing) {
        return !inPlace && !along && !married && waited >= missing;
    }

    /**
     * True if a body of hers at her own place ({@code bodyHome}) takes her slot from the one the world has ({@code slotHome}: whether
     * that one's home is her place too); never from a married one (she lives where her spouse made their home).
     */
    public static boolean takesOver(boolean bodyHome, boolean slotHome, boolean married) {
        return bodyHome && !slotHome && !married;
    }
}

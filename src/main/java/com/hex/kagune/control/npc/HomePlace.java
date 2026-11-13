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
     * 4.1.83 (the owner: "Gas mask guy is ALSO not spawning in his room, or anywhere"; "and I'm 99% sure the general of the CCg isnt spawning
     * either"): official {@code key}'s own spot in her place's template, where it puts her ({x, feet y, z, yaw}: a jigsaw place's own; the
     * Aogiri hideout's and the CCG tower's in the whole site, from its first tile's corner), or null for none of theirs. As the built
     * templates have them (CompanionTests reads them back), but Banjou: in his bedroom upstairs by the magenta beds, facing into it (the
     * owner, asked where his room is: "Upstairs bedroom (Recommended)"); the template is written so (tools/companion_structures.py).
     */
    public static int[] spot(String key) {
        return switch (key == null ? "" : key) {
            case "eto" -> new int[] {6, 3, 5, 90};
            case "touka" -> new int[] {17, 10, 8, 0};
            case "ken" -> new int[] {17, 30, 17, 0};
            case "ayato" -> new int[] {238, 14, 105, 0};
            case "noro" -> new int[] {94, 26, 88, 0};
            case "general" -> new int[] {23, 58, 25, 0};
            case "roma" -> new int[] {38, 1, 24, 0};
            case "banjou" -> new int[] {5, 7, 10, -90};
            default -> null;
        };
    }

    /**
     * 4.1.83: where {@code spot} (a template's {x, feet y, z, yaw}) stands in the world ({x, feet y, z, yaw}) with the template's corner at
     * ({@code x}, {@code y}, {@code z}) turned {@code turns} quarter turns clockwise (Rotation's order: NONE, CLOCKWISE_90, CLOCKWISE_180,
     * COUNTERCLOCKWISE_90), as the game turns a template's blocks and people about its corner (village.VillageRules#turn) and their facing.
     */
    public static int[] placed(int[] spot, int x, int y, int z, int turns) {
        int[] turned = com.hex.kagune.control.village.VillageRules.turn(spot[0], spot[2], turns);
        int yaw = Math.floorMod(spot[3] + 90 * Math.floorMod(turns, 4) + 180, 360) - 180;
        return new int[] {x + turned[0], y + spot[1], z + turned[1], yaw};
    }

    /** 4.1.83: how near (blocks, across) her spot a player keeps her place: half a minute there with none of her in it, she is made there. */
    public static final double KEEP_NEAR = 24.0;
    /** 4.1.83: how far (blocks, any way) her home may be from her spot's before it is moved there (a step or two of her own is hers). */
    public static final int ASTRAY = 2;

    /**
     * 4.1.83: true if a home ({@code ax}, {@code ay}, {@code az}: a block one over her feet) is not the one at her spot's ({@code x}, {@code y},
     * {@code z}): more than {@link #ASTRAY} from it any way (Banjou downstairs before his bedroom upstairs).
     */
    public static boolean astray(int ax, int ay, int az, int x, int y, int z) {
        return Math.abs(ax - x) > ASTRAY || Math.abs(ay - y) > ASTRAY || Math.abs(az - z) > ASTRAY;
    }

    /** The middle of a box ({x, y, z}: its middle across, the floor's height over its lowest layer). */
    public static int[] middle(int minX, int maxX, int minZ, int maxZ, int minY) {
        return new int[] {Math.floorDiv(minX + maxX, 2), minY + 1, Math.floorDiv(minZ + maxZ, 2)};
    }

    /**
     * 4.1.82: true if official {@code key} is made at her place now: someone has been there {@code waited} ticks (UniqueNpc.MISSING or
     * more), none of her is in it ({@code inPlace}), she is not out with someone ({@code along}: following or holding a hand; 4.1.83: or left
     * waiting somewhere by a player: she goes home herself after a day alone, UniqueNpc#dueHome) and not married.
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

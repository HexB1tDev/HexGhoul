package com.hex.kagune.control.village;

/**
 * 1.3.71: the Brick Village's people (pure, tested): the village is placed turned a quarter turn at a time, and so is each of its people
 * (placed facing south in the template, yaw 0), so one tells the village's turn from its own yaw the first time it ticks
 * ({@link #turns}) and turns the village's map ({@link BrickVillage}) with it ({@link #turn}). Its humans live in its houses (their homes,
 * where they were placed), walk its streets by day and stay home by night; its investigators walk the loop round it ({@code ROUTE}) in
 * order; none of them ever goes past its perimeter ({@link #inside}, {@link #clamp}).
 */
public final class VillageRules {
    private VillageRules() {
    }

    /** How long (ticks) a human waits between walks on the streets, at least and at most more. */
    public static final int STROLL_EVERY = 140;
    public static final int STROLL_MORE = 200;
    /** How far (blocks) from where it stands a human picks the next street spot to walk to. */
    public static final double STROLL_REACH = 22.0;
    /** How near (blocks) a waypoint counts as reached. */
    public static final double REACHED = 1.8;
    /** Walking paces (a mob's movement modifier): a human's stroll, its way home, an investigator's patrol. */
    public static final double STROLL = 0.55;
    public static final double HOMEWARD = 0.75;
    public static final double PATROL = 0.62;

    /** Quarter turns clockwise (0..3) of something placed facing south (yaw 0) and turned with the village, from its {@code yaw} now. */
    public static int turns(float yaw) {
        return Math.floorMod(Math.round(yaw / 90.0F), 4);
    }

    /**
     * The template's offset ({@code dx}, {@code dz}) turned {@code turns} quarter turns clockwise, as the game turns a template's blocks
     * (StructureTemplate's CLOCKWISE_90: x, z to -z, x).
     */
    public static int[] turn(int dx, int dz, int turns) {
        return switch (Math.floorMod(turns, 4)) {
            case 1 -> new int[] {-dz, dx};
            case 2 -> new int[] {-dx, -dz};
            case 3 -> new int[] {dz, -dx};
            default -> new int[] {dx, dz};
        };
    }

    /** As {@link #turn}, back: the world's offset in the template's frame. */
    public static int[] unturn(int dx, int dz, int turns) {
        return turn(dx, dz, 4 - Math.floorMod(turns, 4));
    }

    /** True if ({@code dx}, {@code dz}) from the village's middle is within its perimeter (it is square: a turn changes nothing). */
    public static boolean inside(double dx, double dz, int perimeter) {
        return Math.abs(dx) <= perimeter && Math.abs(dz) <= perimeter;
    }

    /** ({@code dx}, {@code dz}) from the middle brought back within the perimeter (less {@code margin}). */
    public static double[] clamp(double dx, double dz, int perimeter, double margin) {
        double most = Math.max(0.0, perimeter - margin);
        return new double[] {Math.max(-most, Math.min(most, dx)), Math.max(-most, Math.min(most, dz))};
    }

    /** The patrol's next waypoint after {@code at} on a loop of {@code length}, walking it {@code forward} or back. */
    public static int next(int at, int length, boolean forward) {
        return length <= 0 ? 0 : Math.floorMod(at + (forward ? 1 : -1), length);
    }

    /** The waypoint of {@code route} nearest ({@code dx}, {@code dz}) (both in the template's frame), where a patrol joins the loop. */
    public static int nearest(int[][] route, double dx, double dz) {
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < route.length; i++) {
            double d = Math.pow(route[i][0] - dx, 2) + Math.pow(route[i][2] - dz, 2);
            if (d < bestDistance) {
                bestDistance = d;
                best = i;
            }
        }
        return best;
    }
}

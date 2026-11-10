package com.hex.kagune.control.npc;

/**
 * 4.1.4 (the owner: "CCG building, tons of ccg agents guard and patrol the area, sit in their offices"): the CCG building's people (pure,
 * tested). Its investigators are placed with it ({@code tools/exploration_structures.py}): each carries the building's map (HexVillage)
 * and, in its saved data, what it does there: {@link #POST} (it stands guard where it was placed, looking the way {@link #YAW} says),
 * {@link #SEAT} (it sits on the chair it was placed in, a stair, facing {@link #YAW}), or neither (it walks the patrol round the building).
 * {@link #YAW} is in the building's own frame (whole degrees: south 0, west 90, north 180, east -90); the building is turned as a whole.
 * Whatever it goes for, it gets up and fights as any investigator, then goes back.
 */
public final class HqRules {
    public static final String SEAT = "HexCcgSeat";
    public static final String POST = "HexCcgPost";
    public static final String YAW = "HexCcgYaw";
    /** A chair's seat over its block's floor (a stair's low step), and how far toward its open side one sits (off its tall side). */
    public static final double SEAT_RISE = 0.5;
    public static final double SEAT_FORWARD = 0.2;
    /** How near its chair (blocks, on the level) it sits down from, and how far up or down. */
    public static final double SEAT_NEAR = 1.6;
    public static final double SEAT_HEIGHT = 1.25;
    /** How near its post it stops and turns to its watch. */
    public static final double POST_NEAR = 1.2;
    /** How fast it walks back to its chair or post (a walk: the village patrol's pace). */
    public static final double PACE = com.hex.kagune.control.village.VillageRules.PATROL;
    /** How far the body comes down sitting (sixteenths of a block: the hips to just over the seat). */
    public static final float SEAT_DROP = 10.0F;

    private HqRules() {
    }

    /** A yaw of the building's frame in the world, the building turned {@code turns} quarter turns clockwise: within -180 .. 180. */
    public static float yaw(int template, int turns) {
        float yaw = (template + 90.0F * Math.floorMod(turns, 4)) % 360.0F;
        return yaw > 180.0F ? yaw - 360.0F : yaw <= -180.0F ? yaw + 360.0F : yaw;
    }

    /**
     * Where one sits on the chair in the block ({@code x}, {@code y}, {@code z}) facing {@code yaw} (the world's): on its low step, toward
     * its open side: {x, y, z} of the feet.
     */
    public static double[] seat(int x, int y, int z, float yaw) {
        double turn = Math.toRadians(yaw);
        return new double[] {x + 0.5 - Math.sin(turn) * SEAT_FORWARD, y + SEAT_RISE, z + 0.5 + Math.cos(turn) * SEAT_FORWARD};
    }

    /** True if feet at ({@code dx}, {@code dy}, {@code dz}) from the seat are near enough to sit down. */
    public static boolean sits(double dx, double dy, double dz) {
        return dx * dx + dz * dz <= SEAT_NEAR * SEAT_NEAR && Math.abs(dy) <= SEAT_HEIGHT;
    }

    /** True if feet at ({@code dx}, {@code dz}) from the post are on it. */
    public static boolean posted(double dx, double dz) {
        return dx * dx + dz * dz <= POST_NEAR * POST_NEAR;
    }
}

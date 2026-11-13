package com.hex.kagune.control.npc;

/**
 * 4.1.69: where Ken sits down at home. His legs lie out along the floor ({@code EtoPose} SIT), reaching {@link #REACH} blocks ahead of
 * his middle; on Anteiku's roof his spot is one block inside the parapet, whose face is half a block ahead of the spot's middle, so
 * sitting on the spot itself would put his boots through the wall. He sits back from it as far as he must, never further than
 * {@link #MOST_BACK} (still on his own block), and slides there from wherever his walk home stopped, {@link #STEP} a tick.
 */
public final class SitSpot {
    /** How far ahead of his middle his boots reach when he sits (blocks: the 12-pixel legs, a hair more for the toes). */
    public static final double REACH = 0.82;
    /** The furthest back of his spot he sits (blocks). */
    public static final double MOST_BACK = 0.4;
    /** How far he slides a tick, settling onto the spot (blocks). */
    public static final double STEP = 0.08;
    /** Close enough to the spot to sit (blocks). */
    public static final double THERE = 0.05;
    /** How finely the floor ahead of him is looked along for a wall (blocks). */
    public static final double LOOK = 0.05;

    private SitSpot() {
    }

    /** How far back of his spot he sits, the floor ahead of it clear for {@code clear} blocks (the first solid point along his facing). */
    public static double back(double clear) {
        if (!(clear >= 0.0)) {
            return MOST_BACK;
        }
        return Math.min(MOST_BACK, Math.max(0.0, REACH - clear));
    }

    /** The way he faces, as (x, z) on the ground, for a yaw in degrees (Minecraft's: 0 faces south, +z). */
    public static double[] facing(float yaw) {
        double r = Math.toRadians(yaw);
        return new double[] {-Math.sin(r), Math.cos(r)};
    }

    /** One slide toward (tx, tz) from (x, z): where he is next tick. */
    public static double[] slide(double x, double z, double tx, double tz) {
        double dx = tx - x;
        double dz = tz - z;
        double far = Math.sqrt(dx * dx + dz * dz);
        if (far <= STEP) {
            return new double[] {tx, tz};
        }
        return new double[] {x + dx / far * STEP, z + dz / far * STEP};
    }
}

package com.hex.kagune.control.village;

/**
 * 4.1.51: the CCG tower's lift (pure, tested). The owner's tower (art/exploration/CCG.schematic) has no stairs: on every floor a 3 x 3
 * cabin over one shaft, a stone button inside and a sign over it ("Current Floor: 1 / Dest: 2" ... "Current Floor: 10 / Dest: 1"); the
 * command blocks that ran it were never in the file. So the game does what its signs say ({@code server/CcgLift}): pressing a cabin's
 * button takes whoever stands in the cabin to the next floor up, and from the top floor back down to the lobby. A floor is a cabin's
 * button {@link #STOREY} blocks over or under the one pressed, in the same spot; the lift's floors are the unbroken run of them.
 */
public final class LiftRules {
    /** Blocks between one floor's button and the next's (the tower's floors, one every 6 blocks). */
    public static final int STOREY = 6;
    /** The most floors a lift is looked for over or under the button pressed (the tower has ten). */
    public static final int MOST = 16;
    /** How near (blocks, from the button's middle to the feet, on the level) one must stand to ride: inside the 3 x 3 cabin. */
    public static final double CABIN = 2.4;
    /** How long (ticks) after a ride a press does nothing (a held click, the button still pressed). */
    public static final int REST = 20;

    private LiftRules() {
    }

    /**
     * How far (blocks, up positive) a press takes the rider: {@code above} buttons follow in an unbroken run over the one pressed and
     * {@code below} under it. One floor up while there is one; from the top, down to the lowest; 0 if the button is no lift's (alone).
     */
    public static int ride(int above, int below) {
        if (above > 0) {
            return STOREY;
        }
        return below > 0 ? -STOREY * below : 0;
    }

    /** The floor (1 the lowest) of a button with {@code below} floors under it, as the cabins' signs number them. */
    public static int floor(int below) {
        return below + 1;
    }

    /** True if feet ({@code dx}, {@code dz}) from the button's middle are in its cabin. */
    public static boolean aboard(double dx, double dz) {
        return dx * dx + dz * dz <= CABIN * CABIN;
    }
}

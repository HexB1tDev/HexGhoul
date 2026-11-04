package com.hex.kagune.control;

/**
 * 2.1.7 (pure, tested; the owner: "Ayato Kakuja Final Variant: Gets access to Arima's dashes (3 dashes, after using one, it will show in
 * the action bar 2/3 and another will regenerate after 5 seconds and return it to 3/3). Dash is activated when double tapping sprint."):
 * the White Reaper's dash for a player: a short burst the way they are moving ({@link #direction}: W, S, A, D and between; no direction,
 * the way they look), at his speed, from {@link #CHARGES} charges that come back one every {@link #RECHARGE} ticks ({@link Charges}).
 * Started by the sprint key pressed twice within {@link #DOUBLE_TAP} ticks ({@link Taps}).
 */
public final class ReaperDash {
    private ReaperDash() {
    }

    /** How many dashes are held, and how long each takes to come back (ticks: five seconds). */
    public static final int CHARGES = 3;
    public static final long RECHARGE = 100L;
    /** The burst: this fast (blocks a tick, along the ground: the Reaper's own pace, a little less for a player's lighter frame) for this long. */
    public static final double SPEED = 1.45;
    public static final int TICKS = 4;
    /** The sprint key pressed twice within this many ticks is a double tap. */
    public static final int DOUBLE_TAP = 7;

    /** A player's dashes: how many are ready and when the next comes back ({@code Long.MIN_VALUE}: all are back). */
    public static final class Charges {
        int left = CHARGES;
        long refillAt = Long.MIN_VALUE;

        /** How many are ready at {@code now}, any that came back counted in. */
        public int ready(long now) {
            this.refill(now);
            return this.left;
        }

        /** True (and one fewer) if one is ready at {@code now}; the next refill counted from now if none was on its way. */
        public boolean spend(long now) {
            this.refill(now);
            if (this.left <= 0) {
                return false;
            }
            this.left--;
            if (this.refillAt == Long.MIN_VALUE) {
                this.refillAt = now + RECHARGE;
            }
            return true;
        }

        /** True if one came back since {@code before} (by {@code now}). */
        public boolean refilled(long before, long now) {
            int was = this.left;
            this.refill(now);
            return this.left > was;
        }

        /** Every one ready (cooldowns off). */
        public void fill() {
            this.left = CHARGES;
            this.refillAt = Long.MIN_VALUE;
        }

        private void refill(long now) {
            while (this.refillAt != Long.MIN_VALUE && now >= this.refillAt && this.left < CHARGES) {
                this.left++;
                this.refillAt = this.left < CHARGES ? this.refillAt + RECHARGE : Long.MIN_VALUE;
            }
            if (this.left >= CHARGES) {
                this.refillAt = Long.MIN_VALUE;
            }
        }
    }

    /** What the action bar says: "Dash 2/3". */
    public static String bar(int ready) {
        return "Dash " + Math.max(0, Math.min(CHARGES, ready)) + "/" + CHARGES;
    }

    /**
     * The way a dash goes (unit, world x and z) for a body facing {@code yawDegrees} (Minecraft's: 0 south, 90 west) with the movement keys
     * at {@code forward} (W +1, S -1) and {@code left} (A +1, D -1); no keys: forward.
     */
    public static double[] direction(double yawDegrees, double forward, double left) {
        double f = Math.abs(forward) < 0.05 && Math.abs(left) < 0.05 ? 1.0 : forward;
        double l = Math.abs(forward) < 0.05 && Math.abs(left) < 0.05 ? 0.0 : left;
        double yaw = Math.toRadians(yawDegrees);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        // His left, facing (fx, fz): (fz, -fx) (Minecraft's x east, z south).
        double x = fx * f + fz * l;
        double z = fz * f - fx * l;
        double length = Math.hypot(x, z);
        return length < 1.0E-6 ? new double[] {fx, fz} : new double[] {x / length, z / length};
    }

    /** The way as sent (tenths of a degree of world yaw), and back. */
    public static int encode(double[] way) {
        double degrees = Math.toDegrees(Math.atan2(-way[0], way[1]));
        return (int)Math.round(Math.floorMod(Math.round(degrees * 10.0), 3600L));
    }

    public static double[] decode(int tenths) {
        double yaw = Math.toRadians(Math.floorMod(tenths, 3600) / 10.0);
        return new double[] {-Math.sin(yaw), Math.cos(yaw)};
    }

    /** The sprint key's presses, for a double tap: true on the second press within {@link #DOUBLE_TAP} ticks of the first. */
    public static final class Taps {
        boolean down;
        long lastPress = Long.MIN_VALUE;

        /** The key {@code down} at tick {@code now}: true if this tick is a double tap's second press (and the tap is used up). */
        public boolean tick(boolean down, long now) {
            boolean pressed = down && !this.down;
            this.down = down;
            if (!pressed) {
                return false;
            }
            if (this.lastPress != Long.MIN_VALUE && now - this.lastPress <= DOUBLE_TAP) {
                this.lastPress = Long.MIN_VALUE;
                return true;
            }
            this.lastPress = now;
            return false;
        }
    }
}

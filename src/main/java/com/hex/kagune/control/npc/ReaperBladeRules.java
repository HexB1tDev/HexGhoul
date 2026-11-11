package com.hex.kagune.control.npc;

/**
 * 4.1.29: the White Reaper's blade string's timing (pure; shared by his brain and how he is drawn, client.animation.ReaperBladePose): each
 * blow's length and the tick it lands.
 */
public final class ReaperBladeRules {
    private ReaperBladeRules() {
    }

    /** A blow: how long (ticks), when it lands. */
    public record Blow(int total, int impact) {
    }

    /** A cleave from over his head; a rising cut from low on his left; a sweep across; a thrust, all his weight behind it. */
    public static final Blow[] STRING = {new Blow(16, 8), new Blow(15, 7), new Blow(17, 8), new Blow(18, 8)};
    public static final int STEPS = STRING.length;

    /**
     * 4.1.30 (the owner: "He can dash more precisely ... He will always try to, get into range and kill you. He'll dodge moves"): his rush in
     * (blocks a tick, how far off he starts one, the longest, how soon again) and his sidestep (how fast, how long, how soon again, the
     * chance he meets a blow coming with one).
     */
    public static final double RUSH_SPEED = 1.15;
    public static final double RUSH_FROM = 12.0;
    public static final int RUSH_LONGEST = 10;
    public static final int RUSH_EVERY = 36;
    public static final double DODGE_SPEED = 0.95;
    public static final int DODGE_TICKS = 4;
    public static final int DODGE_EVERY = 22;
    public static final double DODGE_BLOW = 0.65;
    /** 4.1.30: how far short of his prey a rush stops (blocks, edge to edge): in his blade's reach, with room to swing. */
    public static final double RUSH_STOP = 2.4;
    /** 4.1.30: the ticks of a blow after which he is committed to it (no dodge out of it): two before it lands. */
    public static boolean committed(int step, long age) {
        return age >= blow(step).impact() - 2;
    }

    /** 4.1.30: how far a rush goes this tick ({@code left} blocks still to go): its speed, never past where it stops. */
    public static double rushStep(double left) {
        return Math.max(0.0, Math.min(RUSH_SPEED, left));
    }

    /**
     * 4.1.30: the way he sidesteps a shot or a blow coming at him along {@code (vx, vz)} from where it is ({@code dx, dz}: it to him): square
     * to its way, to whichever side of its line he already stands (so he leaves it, not crosses it), a unit vector.
     */
    public static double[] dodge(double vx, double vz, double dx, double dz) {
        double n = Math.hypot(vx, vz);
        if (n < 1.0E-6) {
            vx = dx;
            vz = dz;
            n = Math.hypot(vx, vz);
            if (n < 1.0E-6) {
                return new double[] {1.0, 0.0};
            }
        }
        double ux = vx / n;
        double uz = vz / n;
        double side = ux * dz - uz * dx; // which side of its line he stands
        double sx = -uz;
        double sz = ux;
        return side >= 0.0 ? new double[] {sx, sz} : new double[] {-sx, -sz};
    }

    /** Blow {@code step} (wrapped onto the string). */
    public static Blow blow(int step) {
        return STRING[Math.floorMod(step, STEPS)];
    }
}

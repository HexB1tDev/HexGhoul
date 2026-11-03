package com.hex.kagune.control.npc;

/**
 * 2.0.33 (pure, tested): the White Reaper's ground tendril (the owner: "he shoves the stick in the ground and then like a black tendril
 * comes out of the ground where the user was. It was still possibility to dodge, but it's a pretty big tendril"). He raises Dawn and
 * drives it point first into the ground ({@link #PLANT} ticks); where his prey stood at that moment is marked on the ground (dark cracks
 * and red glints) for {@link #WARN} ticks, time enough to get off it; then a great black tendril bursts up out of the mark, catching
 * everyone standing on it ({@link #hits}), and whips there a moment before it sinks back ({@link #LIFE}). He holds the lance in the
 * ground the whole time and pulls it out after ({@link #DURATION}): the moment to punish him.
 */
public final class ReaperTendril {
    private ReaperTendril() {
    }

    /** From the start: the lance in the ground; the mark's warning; the lance still in until; the whole move (ticks). */
    public static final int PLANT = 9;
    public static final int WARN = 16;
    public static final int HOLD = 30;
    public static final int DURATION = 36;
    /** Not again for this long, and only on prey this near (blocks), and seen. */
    public static final long COOLDOWN = 170L;
    public static final double NEAREST = 3.5;
    public static final double FARTHEST = 18.0;
    /** The chance each tick it is ready and its prey in range that it goes for it. */
    public static final double CHANCE = 0.05;
    /** What it catches: within this of the mark's middle (blocks, along the ground), from a little under it up to its height. */
    public static final double RADIUS = 1.6;
    public static final double HEIGHT = 5.5;
    /** Its blow (raw, before the target's defences) and how hard it throws them up. */
    public static final float DAMAGE = 24.0F;
    public static final double THROW_UP = 0.95;
    /** How long it is drawn: bursting up over {@link #RISE}, whipping, then sinking back over the last {@link #SINK}. */
    public static final int LIFE = 26;
    public static final int RISE = 4;
    public static final int SINK = 8;
    /** Its thickness where it leaves the ground (blocks across), thinning to its point. */
    public static final float THICKNESS = 0.62F;
    /** Its colours: black, with dark red veins. */
    public static final int COLOUR = 0x120E10;
    public static final int VEINS = 0x4E0A14;

    /** When it rises (ticks from the start). */
    public static int erupts() {
        return PLANT + WARN;
    }

    /** True if a body whose feet are {@code dx, dy, dz} from the mark's middle is caught as it rises. */
    public static boolean hits(double dx, double dy, double dz) {
        return Math.sqrt(dx * dx + dz * dz) <= RADIUS && dy >= -0.6 && dy <= HEIGHT;
    }

    /** How much of it is out of the ground {@code age} ticks after it burst up (0..1): out fast, back in at the end. */
    public static double out(double age) {
        if (age < 0.0 || age >= LIFE) {
            return 0.0;
        }
        if (age < RISE) {
            double t = age / RISE;
            return 1.0 - (1.0 - t) * (1.0 - t) * (1.0 - t);
        }
        double left = LIFE - age;
        if (left < SINK) {
            double t = left / SINK;
            return t * t * (3.0 - 2.0 * t);
        }
        return 1.0;
    }

    /**
     * Its line {@code age} ticks after it burst up, from where it leaves the ground (a little under it) to its point: {@code n} points
     * {x, y, z} from the mark's middle. It leans and whips about its own seed (two waves along it, stronger toward the point, the point
     * curling over), only as much of it as is out ({@link #out}).
     */
    public static double[][] line(double age, int seed, int n) {
        double shown = out(age);
        double length = HEIGHT * Math.max(0.03, shown);
        java.util.Random random = new java.util.Random(seed * 31L + 7L);
        double lean = random.nextDouble() * Math.PI * 2.0;
        double phase = random.nextDouble() * Math.PI * 2.0;
        double[][] points = new double[n][];
        for (int i = 0; i < n; i++) {
            double f = i / (double)(n - 1);
            double y = -0.3 + length * f;
            double reach = f * f;
            double whip = Math.sin(age * 0.55 + phase + f * 3.2) * 0.55 * reach + Math.sin(age * 0.9 + phase * 2.0 + f * 5.0) * 0.18 * reach;
            double curl = 0.9 * f * f * f;
            double x = Math.cos(lean) * (curl + whip) - Math.sin(lean) * 0.35 * Math.sin(age * 0.4 + f * 2.0) * reach;
            double z = Math.sin(lean) * (curl + whip) + Math.cos(lean) * 0.35 * Math.sin(age * 0.4 + f * 2.0) * reach;
            points[i] = new double[] {x, y, z};
        }
        return points;
    }
}

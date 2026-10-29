package com.hex.kagune.control.client.render;

import com.hex.kagune.control.ComboStage;
import com.hex.kagune.control.ControlMath;
import com.hex.kagune.control.HitShape;
import com.hex.kagune.control.StrikePattern;

/**
 * Pure strike-path math for one kagune limb: doubles only, no Minecraft types, so it is unit-testable.
 *
 * <p>A strike is a short keyframed timeline (rest, windup, impact, follow-through, rest) laid out in a frame built on
 * the horizontal strike axis from the limb root to the aim point. Sweeping patterns key cylindrical coordinates around
 * the root so their arcs stay round; straight patterns key along/side/up offsets. Keys are joined by cubic Hermite
 * segments (C1 continuous). The aim point is reached exactly at {@link #impact}, when the server resolves damage, and
 * the path ends exactly at rest. Low control deepens, widens and slows the windup, lengthens the overshoot and adds
 * smooth deterministic wobble; weight makes arcs bigger and windups heavier; involuntary strikes thrash like a whip.
 *
 * <p>Fill the public inputs, call {@link #sample(double)}, read {@link #x}, {@link #y}, {@link #z}. An instance keeps
 * scratch state (no allocation per sample), so use one per thread.
 */
public final class StrikeCurve {
    /** TendrilChain's combat reach: goals never leave this distance from the root. */
    public static final double MAX_REACH = 15.25;
    private static final double TAU = Math.PI * 2.0;
    private static final int MAX_KEYS = 10;
    private static final double MIN_KEY_GAP = 0.01;
    /** Half-width (in progress) of the window around impact where wobble fades out so the strike connects cleanly. */
    private static final double IMPACT_CALM = 0.11;
    private static final double[] NOISE_FREQUENCY = {1.35, 2.6, 4.1};
    private static final double[] NOISE_AMPLITUDE = {0.55, 0.3, 0.15};

    // Inputs: world geometry.
    public double rootX, rootY, rootZ;
    public double restX, restY, restZ;
    public double targetX, targetY, targetZ;
    /** Horizontal body forward: fallback strike axis, and tells targets behind the user apart. */
    public double forwardX, forwardZ = 1.0;
    /** +1 if the limb sits on the side {@code (forward.z, 0, -forward.x)} points to, otherwise -1. */
    public double sideSign = 1.0;
    /** -1 lower tail .. +1 upper tail or blade, see {@link #laneOf(double)}. */
    public double lane;
    /** Limb size 0.08..1, see {@link #sizeScale(double)}. */
    public double size = 1.0;

    // Inputs: the strike.
    public StrikePattern pattern = StrikePattern.THRUST;
    /** 0 light and whippy .. 1 heavy and committed. */
    public double weight;
    /** Fraction of the strike at which the tip must be on the aim point. */
    public double impact = 0.4;
    /** Width of a sweeping stage's hit arc in degrees, or 0 when the stage is not a sweep. */
    public double arcDegrees;
    /** Learned kagune control 0..1. */
    public double control = 1.0;
    public boolean involuntary;
    /** Server yaw deviation in degrees: rotates the aim point around the root like the server's hit direction. */
    public double deviation;
    /** Game tick the strike started: seeds the wobble so every client draws the same strike. */
    public long strikeSeed;
    public int limbIndex;
    /** This limb's position among the participating limbs (ordered by index), and how many take part. */
    public int rank;
    public int count = 1;

    // Outputs.
    public double x, y, z;
    /** The point the strike connects with at impact (the target turned by {@link #deviation}). */
    public double aimX, aimY, aimZ;

    // Strike frame and derived shaping, rebuilt by every sample.
    private boolean valid;
    private double axisX, axisZ, latX, latZ, own, distance, height;
    private double restAlong, restSide, restUp;
    private double wildness, heavy, scale, deep, overshoot, hang, jitter, wobble, rankShift, heightOffset;
    private double hit, windupShare, follow;
    private boolean cylindrical;
    private double sigma;
    private double restA, restB, restC;
    private long state;
    private final double[] phase = new double[12];
    private final double[] frequency = new double[3];

    // Keyframes: time, three channels (angle/radius/height or along/side/up), and tangent tension.
    private int keys;
    private final double[] time = new double[MAX_KEYS];
    private final double[] keyA = new double[MAX_KEYS];
    private final double[] keyB = new double[MAX_KEYS];
    private final double[] keyC = new double[MAX_KEYS];
    private final double[] tension = new double[MAX_KEYS];

    /** Same mapping as TendrilChain's layout scale: limb length to a 0.08..1 size factor. */
    public static double sizeScale(double limbLength) {
        return clamp(limbLength / 4.0, 0.08, 1.0);
    }

    /** Layout fan height to a vertical lane: upper limbs toward +1, lower tails toward -1. */
    public static double laneOf(double fanHeight) {
        return fanHeight >= 0.0 ? Math.min(1.0, fanHeight / 1.5) : Math.max(-1.0, fanHeight / 0.38);
    }

    /** Copies the stage's path family and weight; {@code impactFraction} is the exact client impact fraction. */
    public void stage(ComboStage stage, double impactFraction) {
        this.pattern = stage.pattern();
        this.weight = stage.weight();
        this.impact = impactFraction;
        this.arcDegrees = stage.shape() == HitShape.SWEEP ? stage.shapeParam() : 0.0;
    }

    /** Writes the goal at {@code progress} (0..1) into {@link #x}, {@link #y}, {@link #z}. */
    public void sample(double progress) {
        this.prepare();
        if (!this.valid || !(progress > 0.0) || progress >= 1.0) {
            this.rest();
            return;
        }
        double a = this.channel(this.keyA, progress);
        double b = this.channel(this.keyB, progress);
        double c = this.channel(this.keyC, progress);
        double along;
        double side;
        if (this.cylindrical) {
            double radius = Math.max(0.0, b);
            along = radius * Math.cos(a);
            side = radius * Math.sin(a) * this.sigma;
        } else {
            along = a;
            side = b * this.own;
        }
        double up = c;
        if (this.wobble > 0.0) {
            double arc = Math.sin(Math.PI * progress);
            double envelope = this.wobble * arc * arc * smooth(Math.abs(progress - this.hit) / IMPACT_CALM);
            along += envelope * 0.6 * this.noise(0, progress);
            side += envelope * this.noise(1, progress);
            up += envelope * 0.8 * this.noise(2, progress);
        }
        double offsetX = along * this.axisX + side * this.latX;
        double offsetZ = along * this.axisZ + side * this.latZ;
        double length = Math.sqrt(offsetX * offsetX + up * up + offsetZ * offsetZ);
        if (length > MAX_REACH) {
            double shrink = MAX_REACH / length;
            offsetX *= shrink;
            up *= shrink;
            offsetZ *= shrink;
        }
        this.x = this.rootX + offsetX;
        this.y = this.rootY + up;
        this.z = this.rootZ + offsetZ;
        if (!Double.isFinite(this.x) || !Double.isFinite(this.y) || !Double.isFinite(this.z)) {
            this.rest();
        }
    }

    private void rest() {
        boolean restFinite = Double.isFinite(this.restX) && Double.isFinite(this.restY) && Double.isFinite(this.restZ);
        this.x = restFinite ? this.restX : this.rootX;
        this.y = restFinite ? this.restY : this.rootY;
        this.z = restFinite ? this.restZ : this.rootZ;
    }

    private void prepare() {
        this.valid = finite(this.rootX, this.rootY, this.rootZ) && finite(this.restX, this.restY, this.restZ)
            && finite(this.targetX, this.targetY, this.targetZ);
        if (!this.valid) {
            this.aimX = this.targetX;
            this.aimY = this.targetY;
            this.aimZ = this.targetZ;
            return;
        }

        // Aim: the target turned around the root by the server's deviation, kept inside combat reach.
        double dx = this.targetX - this.rootX;
        double dy = this.targetY - this.rootY;
        double dz = this.targetZ - this.rootZ;
        if (Double.isFinite(this.deviation) && Math.abs(this.deviation) > 1.0E-4) {
            double[] turned = ControlMath.rotateYaw(dx, dz, (float)this.deviation);
            dx = turned[0];
            dz = turned[1];
        }
        double reach = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (reach > MAX_REACH) {
            double shrink = MAX_REACH / reach;
            dx *= shrink;
            dy *= shrink;
            dz *= shrink;
        }
        this.aimX = this.rootX + dx;
        this.aimY = this.rootY + dy;
        this.aimZ = this.rootZ + dz;

        // Frame: horizontal axis toward the aim (blending to body forward when the aim is straight above or below).
        double forwardLength = Math.sqrt(this.forwardX * this.forwardX + this.forwardZ * this.forwardZ);
        double fx = forwardLength > 1.0E-6 ? this.forwardX / forwardLength : 0.0;
        double fz = forwardLength > 1.0E-6 ? this.forwardZ / forwardLength : 1.0;
        this.distance = Math.sqrt(dx * dx + dz * dz);
        this.height = dy;
        double toward = smooth((this.distance - 0.1) / 0.4);
        double ax = fx * (1.0 - toward) + (this.distance > 1.0E-9 ? dx / this.distance : 0.0) * toward;
        double az = fz * (1.0 - toward) + (this.distance > 1.0E-9 ? dz / this.distance : 0.0) * toward;
        double axisLength = Math.sqrt(ax * ax + az * az);
        this.axisX = axisLength > 1.0E-6 ? ax / axisLength : fx;
        this.axisZ = axisLength > 1.0E-6 ? az / axisLength : fz;
        this.latX = this.axisZ;
        this.latZ = -this.axisX;
        // The limb's own side in the strike frame; mirrored when the target is behind the user.
        double side = this.sideSign >= 0.0 ? 1.0 : -1.0;
        this.own = fx * this.axisX + fz * this.axisZ < -0.2 ? -side : side;
        double restX = this.restX - this.rootX;
        double restZ = this.restZ - this.rootZ;
        this.restAlong = restX * this.axisX + restZ * this.axisZ;
        this.restSide = restX * this.latX + restZ * this.latZ;
        this.restUp = this.restY - this.rootY;

        // Shaping: wildness falls smoothly with control; weight makes everything bigger and heavier.
        double control = ControlMath.clamp01((float)this.control);
        double weight = ControlMath.clamp01((float)this.weight);
        this.hit = clamp(Double.isFinite(this.impact) ? this.impact : 0.4, 0.15, 0.8);
        this.wildness = 1.0 - control;
        double wild = Math.pow(this.wildness, 1.3);
        this.heavy = this.involuntary ? weight * 0.6 : weight;
        this.scale = 0.55 + 0.45 * clamp(Double.isFinite(this.size) ? this.size : 1.0, 0.08, 1.0);
        this.deep = 1.0 + 0.75 * this.wildness + 0.6 * this.heavy;
        this.overshoot = 1.0 + 0.8 * this.wildness + (this.involuntary ? 0.45 : 0.0);
        this.hang = 0.45 - 0.25 * this.heavy;
        this.jitter = 0.32 * wild + (this.involuntary ? 0.55 : 0.0);
        this.wobble = this.scale * (0.38 * wild + (this.involuntary ? 0.45 : 0.0));
        int limbs = Math.max(1, this.count);
        int order = Math.max(0, Math.min(limbs - 1, this.rank));
        this.rankShift = limbs > 1 ? order / (limbs - 1.0) - 0.5 : 0.0;
        double lane = clamp(Double.isFinite(this.lane) ? this.lane : 0.0, -1.0, 1.0);
        this.heightOffset = clamp(lane * 0.14 + this.rankShift * 0.16, -0.28, 0.28);
        this.windupShare = clamp(0.36 + 0.14 * this.wildness + 0.1 * this.heavy, 0.3, 0.6);
        this.follow = clamp(this.hit + (1.0 - this.hit) * clamp(0.3 + 0.16 * this.wildness + 0.1 * this.heavy, 0.2, 0.6)
            + 0.05 * this.rankShift, this.hit + 0.06, 0.94);

        // Seeded randomness: always drawn in the same order so a strike never changes shape between frames.
        this.state = this.strikeSeed * 0xD1B54A32D192ED03L + (long)this.limbIndex * 0x9E3779B97F4A7C15L + 0x2545F4914F6CDD1DL;
        for (int i = 0; i < this.phase.length; i++) {
            this.phase[i] = (this.random() + 1.0) * Math.PI;
        }
        for (int i = 0; i < this.frequency.length; i++) {
            this.frequency[i] = 1.0 + 0.15 * this.random();
        }

        this.build(lane, order, limbs);
    }

    private void build(double lane, int order, int limbs) {
        this.keys = 0;
        StrikePattern family = this.pattern == null ? StrikePattern.THRUST : this.pattern;
        double stagger = limbs > 1 ? clamp(0.3 * this.hit, 0.06, 0.13) * (limbs - 1 - order) / (limbs - 1.0) : 0.0;
        switch (family) {
            case LASH -> this.sweep(1.0, false, lane);
            case BACKHAND -> this.sweep(-1.0, false, lane);
            case SCISSOR -> this.sweep(1.0, true, lane);
            case SPIN -> this.spin();
            case OVERHEAD -> this.overhead(stagger * 0.6);
            case RISING -> this.rising();
            case IMPALE -> this.impale(lane, limbs);
            default -> this.thrust(stagger, lane);
        }
        if (this.involuntary) {
            this.twitch();
        }
        this.spaceKeys();
    }

    /**
     * LASH, BACKHAND and SCISSOR: a round horizontal arc around the root. LASH pulls back and out to the limb's own
     * side and sweeps across to the far side; BACKHAND whips over the top to the far side and sweeps back; SCISSOR
     * blades open wide on their own sides, tilt opposite ways and cross at the target.
     */
    private void sweep(double direction, boolean scissor, double lane) {
        this.cylinder(this.own, -0.75 * Math.PI);
        double d = this.distance;
        double h = this.height;
        double half = Math.toRadians(clamp(this.arcDegrees > 0.0 ? this.arcDegrees * 0.5 : 45.0, 30.0, 75.0))
            * (1.0 + 0.25 * this.wildness + 0.15 * this.heavy);
        half = Math.min(half, Math.toRadians(85.0));
        double windAngle = direction * Math.min(Math.toRadians(150.0),
            half * 1.1 + Math.toRadians(30.0 + 28.0 * this.wildness + 18.0 * this.heavy + (scissor ? 12.0 : 0.0)));
        double windRadius = clamp(0.38 * d + 0.9, 1.3, 4.6) * this.scale * (0.92 + 0.2 * this.wildness);
        double bodySide = this.sideSign >= 0.0 ? 1.0 : -1.0;
        double tilt = scissor ? (0.45 + 0.35 * this.heavy) * this.scale * bodySide : 0.0;
        double windHeight = Math.max(-0.55, h + (0.3 + 0.3 * this.heavy) * this.deep * this.scale + lane * 0.3 * this.scale + tilt);
        double windAt = this.windAt(this.hit, this.windupShare, !scissor);
        this.key(0.0, this.restA, this.restB, this.restC, 0.0);
        if (direction < 0.0) {
            this.key(windAt * 0.5, (this.restA + windAngle) * 0.5, Math.max(0.9, 0.35 * windRadius),
                Math.max(windHeight, h) + (0.9 + 0.35 * this.deep) * this.scale, 0.8);
        }
        this.key(windAt, windAngle + this.jitterAngle(0.35), windRadius + this.jitterLength(0.6), windHeight + this.jitterLength(0.5), this.hang);
        this.key(this.hit, 0.0, d, h + (scissor ? 0.1 * bodySide : this.heightOffset), 1.15);
        double followAngle = -direction * Math.min(Math.toRadians(120.0), half * (0.85 + 0.6 * (this.overshoot - 1.0)) * (scissor ? 0.75 : 1.0));
        this.key(this.follow, followAngle + this.jitterAngle(0.2), lerp(d, windRadius, 0.3),
            h - 0.12 * this.heavy - lane * 0.1 - tilt * 0.8 * this.overshoot, 0.75);
        this.key(1.0, this.restA, this.restB, this.restC, 0.0);
    }

    /** SPIN: every limb orbits the user once in the same direction, passing the target together at impact. */
    private void spin() {
        double spin = (this.strikeSeed * 0x9E3779B97F4A7C15L) >>> 63 == 0L ? 1.0 : -1.0;
        // Cut directly ahead: no spinning form keeps a limb at rest there.
        this.cylinder(spin, 0.0);
        double coil = Math.toRadians(22.0 + 26.0 * this.wildness + 16.0 * this.heavy) + this.jitterAngle(0.25);
        // Coil early: a limb resting just past the target has almost a full turn to make before impact.
        double windAt = this.windAt(this.hit, clamp(0.24 + 0.08 * this.wildness + 0.06 * this.heavy, 0.2, 0.4), true);
        double home = this.restA - TAU;
        // Carry through at least a minimum arc, even when home lies just past the target (it then swings back).
        double carry = Math.max(0.6 * -home, Math.toRadians(70.0 + 40.0 * this.wildness));
        this.key(0.0, this.restA, this.restB, this.restC, 0.0);
        this.key(windAt, this.restA + coil, Math.max(1.1 * this.scale, 0.72 * this.restB) + this.jitterLength(0.4),
            lerp(this.restC, this.height, 0.5) + 0.3 * this.scale + this.jitterLength(0.4), this.hang);
        this.key(this.hit, 0.0, this.distance, this.height + this.heightOffset, 1.1);
        this.key(this.follow, -carry + this.jitterAngle(0.2), lerp(this.distance, this.restB, 0.35),
            this.height + this.heightOffset * 0.5 + this.jitterLength(0.3), 0.9);
        this.key(1.0, home, this.restB, this.restC, 0.0);
    }

    /** OVERHEAD: rise high above and behind, arc over, crash down onto the target, press, lift off and recover. */
    private void overhead(double stagger) {
        this.line();
        double d = this.distance;
        double h = this.height;
        double arrive = this.hit - stagger;
        double top = Math.max(h, 0.3) + (1.55 + 0.5 * this.heavy + 0.35 * this.wildness) * this.scale;
        double windAt = this.windAt(arrive, this.windupShare, true);
        this.key(0.0, this.restA, this.restB, this.restC, 0.0);
        this.key(windAt, -(0.35 + 0.45 * this.deep) * this.scale + this.jitterLength(0.5),
            (0.35 + 0.25 * this.wildness) * this.scale + this.jitterLength(0.5), top + this.jitterLength(0.4), this.hang);
        this.key(lerp(windAt, arrive, 0.5), d - clamp(0.45 * d, 0.8, 3.0), 0.15 * this.scale, top + 0.15 * this.scale, 1.0);
        if (stagger > 0.01) {
            // The first hammer lands a moment early and keeps crushing until the impact tick.
            this.key(arrive, d, 0.0, h + this.heightOffset + 0.1, 1.0);
            this.key(this.hit, d + 0.1, 0.0, h - 0.18, 0.6);
        } else {
            this.key(this.hit, d, 0.0, h + this.heightOffset, 1.1);
        }
        double press = this.hit + 0.05 + 0.07 * this.heavy;
        this.key(press, d + 0.2 * this.scale, 0.0, h - (0.3 + 0.35 * this.heavy) * this.scale, 0.3);
        this.key(Math.max(this.follow, press + 0.05), 0.75 * d, 0.3 * this.scale, h + 0.5 * this.scale, 0.8);
        this.key(1.0, this.restA, this.restB, this.restC, 0.0);
    }

    /** THRUST: coil back along the strike axis, then stab straight through and past the target. */
    private void thrust(double stagger, double lane) {
        this.line();
        double d = this.distance;
        double h = this.height;
        double arrive = this.hit - stagger;
        double windAt = this.windAt(arrive, this.windupShare, true);
        this.key(0.0, this.restA, this.restB, this.restC, 0.0);
        this.key(windAt, -(0.45 + 0.55 * this.deep) * this.scale + this.jitterLength(0.5),
            (0.35 + 0.35 * this.wildness) * this.scale + this.jitterLength(0.5),
            clamp(h * 0.3, -0.4, 0.8) + (0.3 + lane * 0.35) * this.scale + this.jitterLength(0.4), this.hang);
        if (stagger > 0.01) {
            // Paired jabs alternate: this one lands first and stays lodged until the impact tick.
            this.key(arrive, d - 0.1, 0.0, h + this.heightOffset, 1.2);
            this.key(this.hit, d + 0.3, 0.0, h + this.heightOffset * 0.5, 0.5);
        } else {
            this.key(this.hit, d, 0.0, h + this.heightOffset, 1.4);
        }
        double through = this.hit + (1.0 - this.hit) * (0.16 + 0.1 * this.wildness);
        this.key(through, d + (0.6 + 0.45 * this.wildness + 0.3 * this.heavy) * this.overshoot * this.scale,
            -0.1 * this.scale + this.jitterLength(0.3), h - 0.12 + this.jitterLength(0.25), 0.8);
        this.key(1.0, this.restA, this.restB, this.restC, 0.0);
    }

    /** RISING: drop low in front near the ground on the own side, then slash upward through the target to the far side. */
    private void rising() {
        this.line();
        double d = this.distance;
        double h = this.height;
        double windAt = this.windAt(this.hit, this.windupShare, true);
        double low = Math.min(-0.6, h - 1.1) - 0.1 * this.wildness;
        this.key(0.0, this.restA, this.restB, this.restC, 0.0);
        this.key(windAt, clamp(0.45 * d, 0.9, 3.0) + this.jitterLength(0.4),
            (0.55 + 0.35 * this.wildness + 0.2 * this.heavy) * this.scale + this.jitterLength(0.4),
            low + Math.abs(this.jitterLength(0.2)), this.hang);
        this.key(this.hit, d, 0.0, h + this.heightOffset, 1.2);
        this.key(this.follow, d + 0.35 * this.scale, -0.7 * this.overshoot * this.scale + this.jitterLength(0.4),
            h + (1.4 + 0.5 * this.heavy) * this.overshoot * this.scale + this.jitterLength(0.3), 0.8);
        this.key(1.0, this.restA, this.restB, this.restC, 0.0);
    }

    /**
     * IMPALE: limbs bloom outward one after another in a ring (by side and lane), then converge into a single thrust
     * through the target. Very heavy stages twist the ring as it closes, like a drill.
     */
    private void impale(double lane, int limbs) {
        this.line();
        double d = this.distance;
        double h = this.height;
        double side = 1.0;
        double up = limbs > 1 ? lane * 0.85 + 0.15 : 0.35;
        double norm = Math.sqrt(side * side + up * up);
        side /= norm;
        up /= norm;
        // Own-side coordinates are mirrored per side, so the local twist is flipped to turn every limb the same way.
        double twist = 2.4 * clamp((this.weight - 0.6) / 0.4, 0.0, 1.0) * this.own;
        double radius = (1.5 + 0.5 * this.heavy + 0.5 * this.wildness) * this.scale + this.jitterLength(0.4);
        // Bloom one after another, then close together.
        double spreadAt = clamp(this.hit * this.windupShare * (0.8 + 0.4 * (this.rankShift + 0.5)), 0.05, this.hit * 0.65);
        double center = clamp(h, -0.3, 1.2) * 0.4 + 0.45 * this.scale;
        double through = this.hit + (1.0 - this.hit) * (0.18 + 0.1 * this.wildness);
        this.key(0.0, this.restA, this.restB, this.restC, 0.0);
        this.ring(spreadAt, -(0.25 + 0.3 * this.deep) * this.scale + this.jitterLength(0.4), center, radius, side, up, 0.0, this.hang);
        this.ring(lerp(spreadAt, this.hit, 0.55), 0.4 * d, lerp(center, h, 0.6), radius * 0.45, side, up, twist * 0.5, 1.0);
        this.ring(this.hit, d, h, 0.2, side, up, twist, 1.15);
        this.ring(through, d + (0.9 * this.overshoot + 0.4 * this.heavy) * this.scale, h - 0.1, 0.15, side, up, twist * 1.3, 0.8);
        this.key(1.0, this.restA, this.restB, this.restC, 0.0);
    }

    /** Windup apex time: a share of the run-up to {@code arrive}, optionally rank-staggered, always leaving room to strike. */
    private double windAt(double arrive, double share, boolean staggered) {
        double at = arrive * share * (staggered ? 1.0 + 0.3 * this.rankShift : 1.0);
        return clamp(at, 0.05, arrive - Math.max(0.07, 0.3 * arrive));
    }

    private void ring(double at, double along, double centerHeight, double radius, double side, double up, double angle, double tension) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        this.key(at, along, (side * cos - up * sin) * radius, centerHeight + (side * sin + up * cos) * radius, tension);
    }

    /** Involuntary strikes jerk somewhere unplanned on the way into the windup. */
    private void twitch() {
        if (this.keys < 3 || this.keys >= MAX_KEYS) {
            return;
        }
        double at = clamp(this.time[1] * 0.5, 0.02, this.time[1] - 0.02);
        double room = Math.min(1.0, this.time[1] / 0.12);
        double a = lerp(this.keyA[0], this.keyA[1], 0.4);
        double b = lerp(this.keyB[0], this.keyB[1], 0.4);
        double c = lerp(this.keyC[0], this.keyC[1], 0.4);
        if (this.cylindrical) {
            a += this.random() * 0.35 * room;
            b = Math.max(0.3, b + this.random() * 0.7 * this.scale * room);
        } else {
            a += this.random() * 0.7 * this.scale * room;
            b += this.random() * 0.7 * this.scale * room;
        }
        c += this.random() * 0.7 * this.scale * room;
        for (int i = this.keys; i > 1; i--) {
            this.time[i] = this.time[i - 1];
            this.keyA[i] = this.keyA[i - 1];
            this.keyB[i] = this.keyB[i - 1];
            this.keyC[i] = this.keyC[i - 1];
            this.tension[i] = this.tension[i - 1];
        }
        this.time[1] = at;
        this.keyA[1] = a;
        this.keyB[1] = b;
        this.keyC[1] = c;
        this.tension[1] = 1.0;
        this.keys++;
    }

    /** Linear channels: along the axis, toward the limb's own side, up; rest converted exactly. */
    private void line() {
        this.cylindrical = false;
        this.restA = this.restAlong;
        this.restB = this.restSide * this.own;
        this.restC = this.restUp;
    }

    /** Cylindrical channels around the root: angle (positive toward {@code sigma * lateral}), radius, height. */
    private void cylinder(double sigma, double cut) {
        this.cylindrical = true;
        this.sigma = sigma;
        double angle = Math.atan2(this.restSide * sigma, this.restAlong);
        this.restA = angle <= cut ? angle + TAU : angle;
        this.restB = Math.sqrt(this.restAlong * this.restAlong + this.restSide * this.restSide);
        this.restC = this.restUp;
    }

    private void key(double at, double a, double b, double c, double tangentScale) {
        if (this.keys >= MAX_KEYS) {
            return;
        }
        this.time[this.keys] = at;
        this.keyA[this.keys] = a;
        this.keyB[this.keys] = b;
        this.keyC[this.keys] = c;
        this.tension[this.keys] = tangentScale;
        this.keys++;
    }

    /** Safety net: key times strictly increase, start at 0 and end at 1 (construction already keeps impact clear). */
    private void spaceKeys() {
        this.time[0] = 0.0;
        this.time[this.keys - 1] = 1.0;
        for (int i = 1; i < this.keys - 1; i++) {
            this.time[i] = Math.max(this.time[i], this.time[i - 1] + MIN_KEY_GAP);
        }
        for (int i = this.keys - 2; i > 0; i--) {
            this.time[i] = Math.min(this.time[i], this.time[i + 1] - MIN_KEY_GAP);
        }
    }

    /** Cubic Hermite through the keys of one channel. */
    private double channel(double[] values, double progress) {
        int k = 0;
        while (k < this.keys - 2 && progress >= this.time[k + 1]) {
            k++;
        }
        double span = this.time[k + 1] - this.time[k];
        double s = clamp((progress - this.time[k]) / span, 0.0, 1.0);
        double m0 = this.slope(values, k) * span;
        double m1 = this.slope(values, k + 1) * span;
        double s2 = s * s;
        double s3 = s2 * s;
        return (2.0 * s3 - 3.0 * s2 + 1.0) * values[k] + (s3 - 2.0 * s2 + s) * m0 + (3.0 * s2 - 2.0 * s3) * values[k + 1] + (s3 - s2) * m1;
    }

    /** Tangent at a key: the end keys ease (zero), interior keys use the three-point (Bessel) slope times tension. */
    private double slope(double[] values, int k) {
        if (k <= 0 || k >= this.keys - 1) {
            return 0.0;
        }
        double before = this.time[k] - this.time[k - 1];
        double after = this.time[k + 1] - this.time[k];
        double incoming = (values[k] - values[k - 1]) / before;
        double outgoing = (values[k + 1] - values[k]) / after;
        return this.tension[k] * (incoming * after + outgoing * before) / (before + after);
    }

    /** Smooth, seeded wobble around 0 (roughly -1..1): a few sines in progress, so it never jitters between frames. */
    private double noise(int axis, double progress) {
        double sum = 0.0;
        for (int i = 0; i < NOISE_FREQUENCY.length; i++) {
            sum += NOISE_AMPLITUDE[i] * Math.sin(TAU * NOISE_FREQUENCY[i] * this.frequency[axis] * progress + this.phase[axis * 4 + i]);
        }
        if (this.involuntary) {
            sum += 0.35 * Math.sin(TAU * 6.3 * progress + this.phase[axis * 4 + 3]);
        }
        return sum;
    }

    private double jitterLength(double amount) {
        return this.random() * this.jitter * this.scale * amount;
    }

    private double jitterAngle(double radians) {
        return this.random() * this.jitter * radians;
    }

    /** SplitMix64 step mapped to -1..1. */
    private double random() {
        long z = this.state += 0x9E3779B97F4A7C15L;
        z = (z ^ z >>> 30) * 0xBF58476D1CE4E5B9L;
        z = (z ^ z >>> 27) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 11) * 0x1.0p-52 - 1.0;
    }

    private static boolean finite(double a, double b, double c) {
        return Double.isFinite(a) && Double.isFinite(b) && Double.isFinite(c);
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(max, value);
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private static double smooth(double value) {
        double t = clamp(value, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }
}

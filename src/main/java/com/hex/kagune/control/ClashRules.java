package com.hex.kagune.control;

/**
 * 1.0.40: two players' M1s meeting (pure). A blow that lands on a player whose own M1 is still on its way, aimed back at the one who
 * struck, meets it: both blows stop, neither hurts, the two are thrown apart, and the wind of it blows out between them. The other
 * blow need not have begun at the same moment: anywhere from its start to its landing, it is met. 1.0.42: an M1 on its way also
 * meets a shard flying at the one swinging it ({@link #swats}): the shard bursts there, hurting nothing. 1.0.43: and the blow is spent
 * on it ({@link #spent}): it lands on nothing, so swatting is defending instead of attacking.
 */
public final class ClashRules {
    /** How square to the striker the other must face: the cosine of the most the two may be off (60 degrees). */
    public static final double FACING = 0.5;
    /** Blocks past the other blow's own reach that it still meets (the two kagune cross before either is fully out). */
    public static final double SLACK = 1.5;
    /** How hard each is thrown back from the other: blocks a tick across, and up. */
    public static final double PUSH = 0.75;
    public static final double LIFT = 0.3;
    /** Ticks before either may strike again. */
    public static final int STAGGER = 10;

    /**
     * 1.3.62: how hard a blow is when nothing better is known (its damage), and how hard two blows are between them for the clash to look
     * as it always has (the wind at {@link #ORDINARY} power); the most and least a clash may be; the power from which the ground shakes.
     */
    public static final double HIT = 7.0;
    public static final double REFERENCE = 12.0;
    public static final float ORDINARY = 1.2F;
    public static final float LEAST = 0.7F;
    public static final float MOST = 3.0F;
    public static final float QUAKES = 1.55F;

    /**
     * 1.3.62 (the owner: "depending on how hard the player / NPC hit the impact should be bigger, even shake the floor / camera"): a
     * clash's power from both blows' damage: {@link #ORDINARY} for two ordinary blows, growing with the root of how much more they carry
     * between them, from {@link #LEAST} to {@link #MOST}.
     */
    public static float impact(double hitA, double hitB) {
        double sum = Math.max(0.0, hitA) + Math.max(0.0, hitB);
        return (float)Math.max(LEAST, Math.min(MOST, ORDINARY * Math.sqrt(sum / REFERENCE)));
    }

    /** 1.3.62: true if a clash of {@code power} shakes the ground. */
    public static boolean quakes(float power) {
        return power >= QUAKES;
    }

    /** 1.3.62: how hard the ground shakes the view at the heart of a clash of {@code power} (0 below {@link #QUAKES}, up to 1). */
    public static float rumble(float power) {
        return power < QUAKES ? 0.0F : Math.min(1.0F, 0.35F + (power - QUAKES) / (MOST - QUAKES) * 0.65F);
    }

    /** 1.3.62: how long (ticks) the ground shakes for a clash of {@code power}, and how far off (blocks) it is felt. */
    public static double rumbleTicks(float power) {
        return 8.0 + 6.0 * power;
    }

    public static double felt(float power) {
        return 10.0 + 8.0 * power;
    }

    /** 1.3.62: how hard each is thrown back from a clash of {@code power} (blocks a tick across): {@link #PUSH} for an ordinary one. */
    public static double push(float power) {
        return PUSH * (0.7 + 0.25 * power);
    }

    /** 1.3.62: the clash's sound: louder (heard further) and deeper the harder it is. */
    public static float volume(float power) {
        return 0.95F + 0.3F * power;
    }

    public static float pitch(float power) {
        return Math.max(0.65F, 1.0F + (ORDINARY - power) * 0.15F);
    }

    /** 1.0.42: blocks past a blow's reach that a shard is still met at, and how many shards one blow meets. */
    public static final double SHARD_SLACK = 0.75;
    public static final int SHARDS_PER_BLOW = 3;

    private ClashRules() {
    }

    /** 1.0.43: a blow begun at {@code began}, due by {@code until}, that met shards on its way, is still that blow {@code now}: it lands on nothing. */
    public static boolean spent(long began, long until, long now) {
        return now >= began && now <= until + 1;
    }

    /** The other blow, begun at {@code began} and due to land by {@code until}, still on its way {@code now}. */
    public static boolean onItsWay(long began, long until, long now) {
        return now >= began && now <= until;
    }

    /** The other, looking along ({@code lookX}, {@code lookZ}), faces the striker ({@code dx}, {@code dz} off it across the ground). */
    public static boolean faces(double lookX, double lookZ, double dx, double dz) {
        double look = Math.sqrt(lookX * lookX + lookZ * lookZ);
        double away = Math.sqrt(dx * dx + dz * dz);
        if (away < 1.0E-6) {
            return true; // (on top of each other: whichever way it looks, it is at him)
        }
        if (look < 1.0E-6) {
            return false; // (looking straight up or down: not at him)
        }
        return (lookX * dx + lookZ * dz) / (look * away) >= FACING;
    }

    /** The striker, {@code distance} blocks off, within the other blow's {@code reach}. */
    public static boolean reaches(double distance, double reach) {
        return distance <= reach + SLACK;
    }

    /** All of it: the other blow on its way, aimed at the striker, and reaching him. */
    public static boolean meets(long began, long until, long now, double lookX, double lookZ, double dx, double dz, double distance, double reach) {
        return onItsWay(began, until, now) && faces(lookX, lookZ, dx, dz) && reaches(distance, reach);
    }

    /**
     * 1.0.42: a shard flying this tick from {@code from} to {@code to} ({x, y, z}) meets a blow on its way (begun at {@code began}, due
     * by {@code until}) swung by one whose chest is at {@code chest}, looking along ({@code lookX}, {@code lookZ}), {@code reach} long:
     * how far along this tick's flight it is met (0 to 1), or -1 if it is not. It must be coming at him, and is met where it comes
     * into his reach (or at once, already in it), in front of him.
     */
    public static double swats(long began, long until, long now, double lookX, double lookZ, double[] chest, double[] from, double[] to, double reach) {
        if (!onItsWay(began, until, now)) {
            return -1.0;
        }
        double vx = to[0] - from[0];
        double vy = to[1] - from[1];
        double vz = to[2] - from[2];
        double cx = chest[0] - from[0];
        double cy = chest[1] - from[1];
        double cz = chest[2] - from[2];
        double a = vx * vx + vy * vy + vz * vz;
        double toward = vx * cx + vy * cy + vz * cz;
        if (a < 1.0E-9 || toward <= 0.0) {
            return -1.0; // (still, or already past him, or flying away)
        }
        double r = reach + SHARD_SLACK;
        double c = cx * cx + cy * cy + cz * cz - r * r;
        double t;
        if (c <= 0.0) {
            t = 0.0;
        } else {
            // |v t - (chest - from)| = r: the first time it comes within reach.
            double disc = toward * toward - a * c;
            if (disc < 0.0) {
                return -1.0;
            }
            t = (toward - Math.sqrt(disc)) / a;
            if (t < 0.0 || t > 1.0) {
                return -1.0;
            }
        }
        double px = vx * t - cx;
        double pz = vz * t - cz;
        return faces(lookX, lookZ, px, pz) ? t : -1.0;
    }
}

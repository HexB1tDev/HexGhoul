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

    /**
     * 1.3.62: the clash's sound: louder (heard further) and deeper the harder it is. 2.0.11 (the owner: "Increase the volume of the clash
     * sounds, the stronger the users the louder and even bigger the impact, current still too small"): louder again, and laid over itself
     * {@link #layers} times (one sound can be no louder than full: two or three at once are).
     */
    public static float volume(float power) {
        return 1.3F + 0.55F * power;
    }

    /** 2.0.11: how many times the clash's sound is laid over itself: once, twice from a hard clash, three times from a huge one. */
    public static int layers(float power) {
        return power >= 2.2F ? 3 : power >= 1.5F ? 2 : 1;
    }

    /**
     * 2.0.12 (the owner: "I dont think clashes are shaking them screen at all"; it was a 1.6-degree jolt gone in a quarter second; then
     * "increase it further"): how hard a clash shakes the view at its heart ({@code shown}: its drawn power, {@link #shown}), 1 about 4
     * degrees (an ordinary clash about 1.6), up to {@link #SHAKE_MOST}; how long (ticks) and how far off (blocks) it is felt.
     */
    public static final float SHAKE_MOST = 2.6F;

    public static float shake(float shown) {
        return Math.max(0.0F, Math.min(SHAKE_MOST, 0.7F + 0.55F * shown));
    }

    public static double shakeTicks(float shown) {
        return 14.0 + 6.0 * shown;
    }

    public static double shakeReach(float shown) {
        return 20.0 + 5.0 * shown;
    }

    /** 2.0.11: how big the clash's wind is drawn (its puffs, sparks and kick of the view): bigger than its power, the harder the bigger. */
    public static float shown(float power) {
        return Math.min(MOST * 1.5F, power * 1.4F);
    }

    /**
     * 2.0.11 (the owner: "the weakest player/entity gets pushed further back, if the difference is too vast (gap in power too big) the
     * entity/player get pushed far back a ton, stunned and ragdolled"): how much harder the other's blow was than its own ({@code own},
     * {@code other}: their damage): 1 even, above 1 the other was stronger.
     */
    public static double ratio(double own, double other) {
        return Math.max(0.1, other) / Math.max(0.1, own);
    }

    /**
     * 2.0.11: the gap from which the weaker is overwhelmed (the owner: "its pretty much the same effect entirely of the kick without the
     * animation, send them through blocks"): flown off as the Speed Kick sends a body, through what it can break, down and stunned.
     */
    public static final double GAP = 2.0; // 3.4.3: 2.5 before (a maxed ghoul flung only Rank 0 to 3: "If you're stronger than investigators, then you should send them flying")

    public static boolean overwhelmed(double ratio) {
        return ratio >= GAP;
    }

    /**
     * 3.4.3 (the owner: "no matter how strong you are, what CCG investigators are just gonna send you flying during clashes ... the master
     * should be more reflective of how strong the clash is ... If you're max mastery, I don't think that the Reaper should be able to even
     * send you flying"): how hard one is thrown back from a clash ({@code ratio}: how much harder the other's blow was than its own, as
     * {@link #ratio}; {@code power}: how big the clash was). Before, both were thrown by the size of the clash, which is both blows together:
     * the stronger you were, the harder you were thrown yourself, even winning (a maxed ghoul and the Reaper flew a block a tick each way),
     * and the stronger never threw less than a third of it. Now an even clash throws both a short way apart ({@link #EVEN_PUSH}, a little
     * more for a huge one); the stronger is thrown less by the square of the gap and stands its ground once it is twice as strong
     * ({@link #STANDS}); the weaker further, by the gap; and past {@link #GAP} the weaker is flung as by the kick ({@link #overwhelmed}).
     */
    public static double thrown(float power, double ratio) {
        double r = Math.max(0.0, ratio);
        if (r <= STANDS) {
            return 0.0;
        }
        double even = EVEN_PUSH + EVEN_GROWS * (Math.max(LEAST, Math.min(MOST, power)) - ORDINARY);
        return even * (r < 1.0 ? r * r : Math.min(2.4, Math.pow(r, 1.2))); // (2.4: past GAP's, so it never stops growing before the fling)
    }

    /** 3.4.3: how far up one is thrown with it: {@link #EVEN_LIFT} for an even clash, less for the stronger, more for the weaker (to twice). */
    public static double lifted(double ratio) {
        double r = Math.max(0.0, ratio);
        if (r <= STANDS) {
            return 0.0;
        }
        return EVEN_LIFT * (r < 1.0 ? r * r : Math.min(2.0, r));
    }

    /** 3.4.3: an even clash's throw (blocks a tick across) and rise; how much a huge clash adds to it; the gap from which the stronger stands. */
    public static final double EVEN_PUSH = 0.42;
    public static final double EVEN_GROWS = 0.06;
    public static final double EVEN_LIFT = 0.15;
    public static final double STANDS = 0.5;

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

package com.hex.kagune.control.client.fx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

/**
 * 6.2.0: the Owl's giant blades leave sharp trails as they whip through its moves: wherever a blade's point (and the middle of its
 * edge) goes faster than {@link #FAST}, a thin pale streak is left all along the way it went, gone in a few ticks, so the fastest
 * slash is seen as one clean arc rather than the blade skipping from frame to frame. Fed every frame by the drawing, from where the
 * blades are drawn; nothing is sent.
 */
public final class OwlTrails {
    /** Faster than this (blocks a tick), a part of a blade leaves a trail. */
    static final double FAST = 1.2;
    /** A streak every so far along the way it went (blocks), and at most so many a part a frame. */
    static final double STEP = 0.2;
    static final int MOST = 18;
    /** None this close to the eye in first person (it would wipe across the view). */
    static final double CLEARANCE = 1.2;
    private static final int EDGE = 0xD8F6F1E6;
    private static final int EDGE_FADE = 0x00E2D8C6;

    /** Each player's blade parts as last drawn ({right point, right edge, left point, left edge}) and when (PaleClient's clock). */
    private static final Map<UUID, double[][]> LAST = new HashMap<>();
    private static final Map<UUID, Double> LAST_AT = new HashMap<>();
    private static final java.util.Random RANDOM = new java.util.Random();

    private OwlTrails() {
    }

    /**
     * {@code player}'s blade parts drawn now (world space: {right point, right edge, left point, left edge}) at {@code time};
     * {@code eye} the viewer's own eye in first person (null otherwise).
     */
    public static void blades(UUID player, double[][] parts, double time, Vec3 eye) {
        double[][] last = LAST.get(player);
        Double lastAt = LAST_AT.get(player);
        LAST.put(player, parts);
        LAST_AT.put(player, time);
        if (last == null || lastAt == null) {
            return;
        }
        double dt = time - lastAt;
        if (!(dt > 1.0E-4) || dt > 1.5) {
            return; // the same frame, or not drawn a while: nothing to trail from
        }
        for (int i = 0; i < parts.length; i++) {
            double[] from = last[i];
            double[] to = parts[i];
            double dx = to[0] - from[0];
            double dy = to[1] - from[1];
            double dz = to[2] - from[2];
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length / dt < FAST || length > 12.0) {
                continue;
            }
            Vec3 along = new Vec3(dx / length, dy / length, dz / length);
            int steps = Math.min(MOST, Math.max(1, (int)Math.ceil(length / STEP * RcFx.density())));
            boolean point = i % 2 == 0;
            for (int s = 0; s < steps; s++) {
                double u = (s + RANDOM.nextDouble()) / steps;
                Vec3 at = new Vec3(from[0] + dx * u, from[1] + dy * u, from[2] + dz * u);
                if (eye != null && at.m_82557_(eye) < CLEARANCE * CLEARANCE) {
                    continue;
                }
                RcFx.at(RcFx.STREAK, true, (point ? 5 : 3) + RANDOM.nextInt(2), at).velocity(along.m_82490_(0.03)).drag(0.6F).colors(EDGE, EDGE_FADE)
                    .size(point ? 0.13F : 0.09F, 0.02F).bloom(0.0F).spawn();
            }
        }
    }

    /** Everything forgotten (a world left). */
    public static void clear() {
        LAST.clear();
        LAST_AT.clear();
    }
}

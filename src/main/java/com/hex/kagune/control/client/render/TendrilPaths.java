package com.hex.kagune.control.client.render;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

/**
 * 5.0.14: where each player's tentacles were last drawn (world points from the root to the tip, as {@link StrikePaths#clearOwner}
 * leaves them), so what is stuck in a tentacle rides it: the spikes a Kaneki guard stopped ({@code AyatoSpikes.place}).
 */
public final class TendrilPaths {
    /** One tentacle's drawn path and the world time (ticks, partial included) it was drawn at. */
    private record Drawn(double[][] path, double time) {
    }

    private static final int LIMBS = 16;
    /** A path older than this (ticks) is stale: the tentacle isn't being drawn (in, or out of sight). */
    private static final double STALE = 3.0;
    private static final Map<UUID, Drawn[]> PATHS = new HashMap<>();

    private TendrilPaths() {
    }

    static void record(UUID player, int limb, double[][] path, double time) {
        if (limb < 0 || limb >= LIMBS || path == null || path.length < 2) {
            return;
        }
        if (PATHS.size() > 64) {
            PATHS.values().removeIf(drawn -> {
                for (Drawn d : drawn) {
                    if (d != null && time - d.time() <= 200.0) {
                        return false;
                    }
                }
                return true;
            });
        }
        PATHS.computeIfAbsent(player, ignored -> new Drawn[LIMBS])[limb] = new Drawn(path, time);
    }

    /** The path {@code player}'s tentacle {@code limb} was last drawn on, or null if it hasn't been drawn lately. */
    public static double[][] path(UUID player, int limb, double time) {
        Drawn[] drawn = PATHS.get(player);
        if (drawn == null || limb < 0 || limb >= LIMBS || drawn[limb] == null || Math.abs(time - drawn[limb].time()) > STALE) {
            return null;
        }
        return drawn[limb].path();
    }

    /** How far along {@code path} (0 root .. 1 tip) the point nearest {@code point} is. */
    public static double nearest(double[][] path, Vec3 point) {
        double best = Double.MAX_VALUE;
        double at = 0.0;
        for (int i = 0; i + 1 < path.length; i++) {
            double[] a = path[i];
            double[] b = path[i + 1];
            double[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
            double length = ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2];
            double t = length < 1.0E-9 ? 0.0
                : ((point.f_82479_ - a[0]) * ab[0] + (point.f_82480_ - a[1]) * ab[1] + (point.f_82481_ - a[2]) * ab[2]) / length;
            t = Math.max(0.0, Math.min(1.0, t));
            double dx = a[0] + ab[0] * t - point.f_82479_;
            double dy = a[1] + ab[1] * t - point.f_82480_;
            double dz = a[2] + ab[2] * t - point.f_82481_;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < best) {
                best = distance;
                at = (i + t) / (path.length - 1);
            }
        }
        return at;
    }

    /** The point {@code u} of the way along {@code path} (0 root .. 1 tip). */
    public static Vec3 at(double[][] path, double u) {
        double x = Math.max(0.0, Math.min(1.0, u)) * (path.length - 1);
        int i = Math.min(path.length - 2, (int)Math.floor(x));
        double t = x - i;
        double[] a = path[i];
        double[] b = path[i + 1];
        return new Vec3(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t);
    }

    public static void clear() {
        PATHS.clear();
    }
}

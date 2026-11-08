package com.hex.kagune.control.bikaku;

/**
 * 3.5.9 (the owner: "It should adapt to blocks, and that's where I say flexibility, like go around blocks like a worm, sort of, and
 * actually go around them. It's being able to adapt to the terrain itself and pulling people back"): the way the worm takes to what it
 * goes after, round the blocks between (pure: the server gives it the world's blocks, the tests a made-up one). A straight line if nothing
 * is in the way; else a short search through the open blocks (it keeps a little off walls, never squeezes through a gap only a corner
 * wide), pulled taut into a few points. Never longer than the reach it is given; null if there is no way in reach.
 */
public final class WormRoute {
    private WormRoute() {
    }

    /** Whether a block (by its whole coordinates) lets the worm through: nothing solid in it. */
    public interface Open {
        boolean open(int x, int y, int z);
    }

    /** How many blocks the search looks at, at most (the server's cost); how many points a way keeps, at most. */
    public static final int MOST_LOOKED = 6000;
    public static final int MOST_POINTS = 6;
    /** What a step beside a solid block costs over its length (the worm keeps a little off walls). */
    static final double WALL_COST = 0.35;
    /** How finely a straight line is checked for blocks (blocks). */
    static final double STEP = 0.2;

    /**
     * The way from {@code from} to {@code to} (world points) through {@code open} blocks, no longer than {@code most}: {@code {from, to}} if
     * the straight line is clear; else up to {@link #MOST_POINTS} points round what is in the way; null if there is none in reach.
     */
    public static double[][] find(double[] from, double[] to, double most, Open open) {
        if (clear(from, to, open)) {
            return length(from, to) <= most ? new double[][] {from.clone(), to.clone()} : null;
        }
        int[] start = cell(from);
        int[] goal = cell(to);
        if (!open.open(start[0], start[1], start[2]) || !open.open(goal[0], goal[1], goal[2])) {
            return null;
        }
        java.util.List<int[]> cells = search(start, goal, most, open);
        if (cells == null) {
            return null;
        }
        java.util.List<double[]> points = new java.util.ArrayList<>();
        points.add(from.clone());
        for (int i = 1; i < cells.size() - 1; i++) {
            int[] c = cells.get(i);
            points.add(new double[] {c[0] + 0.5, c[1] + 0.5, c[2] + 0.5});
        }
        points.add(to.clone());
        double[][] taut = taut(points, open);
        return taut != null && length(taut) <= most ? taut : null;
    }

    /** True if the straight line from {@code a} to {@code b} crosses no solid block. */
    public static boolean clear(double[] a, double[] b, Open open) {
        double d = length(a, b);
        int steps = Math.max(1, (int)Math.ceil(d / STEP));
        int lx = Integer.MIN_VALUE;
        int ly = 0;
        int lz = 0;
        for (int i = 0; i <= steps; i++) {
            double f = i / (double)steps;
            int x = (int)Math.floor(a[0] + (b[0] - a[0]) * f);
            int y = (int)Math.floor(a[1] + (b[1] - a[1]) * f);
            int z = (int)Math.floor(a[2] + (b[2] - a[2]) * f);
            if (x == lx && y == ly && z == lz) {
                continue;
            }
            lx = x;
            ly = y;
            lz = z;
            if (!open.open(x, y, z)) {
                return false;
            }
        }
        return true;
    }

    /** How long the line through {@code points} is. */
    public static double length(double[][] points) {
        double total = 0.0;
        for (int i = 1; i < points.length; i++) {
            total += length(points[i - 1], points[i]);
        }
        return total;
    }

    /** The point {@code along} blocks along the line through {@code points} (its end past its length). */
    public static double[] at(double[][] points, double along) {
        double left = Math.max(0.0, along);
        for (int i = 1; i < points.length; i++) {
            double step = length(points[i - 1], points[i]);
            if (left <= step || i == points.length - 1) {
                double f = step < 1.0E-9 ? 1.0 : Math.min(1.0, left / step);
                return new double[] {points[i - 1][0] + (points[i][0] - points[i - 1][0]) * f, points[i - 1][1] + (points[i][1] - points[i - 1][1]) * f,
                    points[i - 1][2] + (points[i][2] - points[i - 1][2]) * f};
            }
            left -= step;
        }
        return points[points.length - 1].clone();
    }

    /** The line through {@code points} as far as {@code along} blocks along it (its first point, the point there). */
    public static double[][] upTo(double[][] points, double along) {
        java.util.List<double[]> out = new java.util.ArrayList<>();
        out.add(points[0].clone());
        double left = Math.max(0.0, along);
        for (int i = 1; i < points.length; i++) {
            double step = length(points[i - 1], points[i]);
            if (left <= step) {
                out.add(at(points, along));
                return out.toArray(new double[0][]);
            }
            out.add(points[i].clone());
            left -= step;
        }
        return out.toArray(new double[0][]);
    }

    // ---------------------------------------------------------------- the search

    /** A* over the blocks, each step to one of the 26 round it (no corner cutting), within {@code most} of its length. */
    private static java.util.List<int[]> search(int[] start, int[] goal, double most, Open open) {
        java.util.Map<Long, Double> cost = new java.util.HashMap<>();
        java.util.Map<Long, Long> came = new java.util.HashMap<>();
        java.util.Map<Long, Boolean> seen = new java.util.HashMap<>();
        java.util.PriorityQueue<double[]> queue = new java.util.PriorityQueue<>(java.util.Comparator.comparingDouble(e -> e[0]));
        long first = key(start[0], start[1], start[2]);
        cost.put(first, 0.0);
        queue.add(new double[] {distance(start, goal), start[0], start[1], start[2]});
        int looked = 0;
        Open cached = (x, y, z) -> seen.computeIfAbsent(key(x, y, z), k -> open.open(x, y, z));
        while (!queue.isEmpty() && looked < MOST_LOOKED) {
            double[] e = queue.poll();
            int x = (int)e[1];
            int y = (int)e[2];
            int z = (int)e[3];
            long here = key(x, y, z);
            double g = cost.get(here);
            if (e[0] > g + distance(new int[] {x, y, z}, goal) + 1.0E-9) {
                continue; // a stale entry
            }
            looked++;
            if (x == goal[0] && y == goal[1] && z == goal[2]) {
                java.util.List<int[]> out = new java.util.ArrayList<>();
                long at = here;
                while (true) {
                    out.add(0, unkey(at));
                    Long back = came.get(at);
                    if (back == null) {
                        break;
                    }
                    at = back;
                }
                return out;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        int nx = x + dx;
                        int ny = y + dy;
                        int nz = z + dz;
                        if (!cached.open(nx, ny, nz)) {
                            continue;
                        }
                        // No corner cutting: every block a diagonal step passes beside must be open too.
                        if ((dx != 0 && !cached.open(x + dx, y, z)) || (dy != 0 && !cached.open(x, y + dy, z)) || (dz != 0 && !cached.open(x, y, z + dz))) {
                            continue;
                        }
                        double step = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        double next = g + step + (besideWall(nx, ny, nz, cached) ? WALL_COST * step : 0.0);
                        if (next > most * 1.25) {
                            continue;
                        }
                        long k = key(nx, ny, nz);
                        Double had = cost.get(k);
                        if (had == null || next < had - 1.0E-9) {
                            cost.put(k, next);
                            came.put(k, here);
                            int[] n = {nx, ny, nz};
                            queue.add(new double[] {next + distance(n, goal), nx, ny, nz});
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean besideWall(int x, int y, int z, Open open) {
        return !open.open(x + 1, y, z) || !open.open(x - 1, y, z) || !open.open(x, y + 1, z) || !open.open(x, y - 1, z) || !open.open(x, y, z + 1)
            || !open.open(x, y, z - 1);
    }

    /** The points pulled taut: from each, straight to the farthest later one in a clear line (at most {@link #MOST_POINTS}; null if more). */
    private static double[][] taut(java.util.List<double[]> points, Open open) {
        java.util.List<double[]> out = new java.util.ArrayList<>();
        int i = 0;
        out.add(points.get(0));
        while (i < points.size() - 1) {
            int j = points.size() - 1;
            while (j > i + 1 && !clear(points.get(i), points.get(j), open)) {
                j--;
            }
            out.add(points.get(j));
            i = j;
        }
        return out.size() > MOST_POINTS ? null : out.toArray(new double[0][]);
    }

    private static int[] cell(double[] p) {
        return new int[] {(int)Math.floor(p[0]), (int)Math.floor(p[1]), (int)Math.floor(p[2])};
    }

    private static double distance(int[] a, int[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static double length(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static long key(int x, int y, int z) {
        return ((long)(x & 0x3FFFFF) << 42) | ((long)(y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
    }

    private static int[] unkey(long k) {
        int x = (int)(k >>> 42 & 0x3FFFFF);
        int y = (int)(k >>> 22 & 0xFFFFF);
        int z = (int)(k & 0x3FFFFF);
        return new int[] {x >= 0x200000 ? x - 0x400000 : x, y >= 0x80000 ? y - 0x100000 : y, z >= 0x200000 ? z - 0x400000 : z};
    }
}

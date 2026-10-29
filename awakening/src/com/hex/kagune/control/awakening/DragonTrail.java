package com.hex.kagune.control.awakening;

/**
 * Path history of the Dragon's lead point. Body segments are laid along it at fixed arc-length
 * spacing, so the colossal body follows exactly where the front has been, like a centipede.
 */
final class DragonTrail {
    private static final int CAPACITY = 2048;
    private static final double MIN_STEP = 0.2;
    private final double[] xs = new double[CAPACITY];
    private final double[] ys = new double[CAPACITY];
    private final double[] zs = new double[CAPACITY];
    /** Cumulative arc length from the oldest retained point. */
    private final double[] arc = new double[CAPACITY];
    private int start;
    private int size;
    private final double keep;

    DragonTrail(V3 origin, double keepLength) {
        this.keep = keepLength;
        put(0, origin, 0.0);
        size = 1;
    }

    private void put(int logical, V3 point, double length) {
        int slot = (start + logical) % CAPACITY;
        xs[slot] = point.x();
        ys[slot] = point.y();
        zs[slot] = point.z();
        arc[slot] = length;
    }

    private int slot(int logical) {
        return (start + logical) % CAPACITY;
    }

    V3 newest() {
        int s = slot(size - 1);
        return new V3(xs[s], ys[s], zs[s]);
    }

    /** Total retained arc length. */
    double length() {
        return arc[slot(size - 1)] - arc[slot(0)];
    }

    void record(V3 point) {
        V3 last = newest();
        double step = point.distance(last);
        if (step < MIN_STEP) return;
        if (size == CAPACITY) drop();
        put(size, point, arc[slot(size - 1)] + step);
        size++;
        while (size > 2 && arc[slot(size - 1)] - arc[slot(1)] > keep) drop();
    }

    private void drop() {
        start = (start + 1) % CAPACITY;
        size--;
    }

    /** Point at the given arc distance behind the newest point; clamps to the oldest point. */
    V3 behind(double distance) {
        double target = arc[slot(size - 1)] - distance;
        if (target <= arc[slot(0)]) {
            int s = slot(0);
            return new V3(xs[s], ys[s], zs[s]);
        }
        int low = 0;
        int high = size - 1;
        while (high - low > 1) {
            int mid = (low + high) >>> 1;
            if (arc[slot(mid)] <= target) low = mid; else high = mid;
        }
        int a = slot(low);
        int b = slot(high);
        double span = arc[b] - arc[a];
        double t = span < 1.0E-9 ? 0.0 : (target - arc[a]) / span;
        return new V3(xs[a] + (xs[b] - xs[a]) * t, ys[a] + (ys[b] - ys[a]) * t, zs[a] + (zs[b] - zs[a]) * t);
    }
}

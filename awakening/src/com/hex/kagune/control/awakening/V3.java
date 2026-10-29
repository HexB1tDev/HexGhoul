package com.hex.kagune.control.awakening;

/** Small immutable vector so the Dragon's kinematics stay independent of Minecraft classes. */
public record V3(double x, double y, double z) {
    public static final V3 ZERO = new V3(0.0, 0.0, 0.0);
    public static final V3 UP = new V3(0.0, 1.0, 0.0);

    public V3 add(V3 o) {
        return new V3(x + o.x, y + o.y, z + o.z);
    }

    public V3 add(double dx, double dy, double dz) {
        return new V3(x + dx, y + dy, z + dz);
    }

    public V3 sub(V3 o) {
        return new V3(x - o.x, y - o.y, z - o.z);
    }

    public V3 scale(double s) {
        return new V3(x * s, y * s, z * s);
    }

    public double dot(V3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public V3 cross(V3 o) {
        return new V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double horizontalLength() {
        return Math.sqrt(x * x + z * z);
    }

    public double distance(V3 o) {
        return sub(o).length();
    }

    public V3 normalize() {
        double length = length();
        return length < 1.0E-9 ? ZERO : scale(1.0 / length);
    }

    public V3 lerp(V3 o, double t) {
        return new V3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t);
    }

    public V3 withY(double ny) {
        return new V3(x, ny, z);
    }

    /** Shortest distance from this point to the segment a-b. */
    public double distanceToSegment(V3 a, V3 b) {
        V3 ab = b.sub(a);
        double lengthSquared = ab.dot(ab);
        double t = lengthSquared < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, sub(a).dot(ab) / lengthSquared));
        return distance(a.add(ab.scale(t)));
    }
}

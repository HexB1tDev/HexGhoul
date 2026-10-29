package com.hex.kagune.control.wound;

/**
 * One wound on a player's body (pure; 3.0.0): which part and which side of it, what kind, where on that side (pixels from its
 * top-left corner as seen from outside: {@code u} across, {@code v} down), turned by {@code angle} (radians), {@code length}
 * and {@code width} in pixels, and its own {@code seed}. It is fully open while the player misses at least {@code threshold}
 * of their health and closes as they regenerate the {@code size} before it ({@link WoundBook}).
 */
public record Wound(Part part, Face face, Kind kind, double u, double v, double angle, double length, double width, int seed, double threshold,
                    double size) {
    /** What made it. */
    public enum Kind {
        /** A thin slit: a blade, a claw, a swipe. */
        CUT,
        /** A torn opening down to the muscle. */
        TEAR,
        /** A deep round puncture: one of Ayato's spikes or needles, pulled out or burst, an arrow, a stab. */
        HOLE,
        /** 5.0.17: a long, deep, wide-open laceration (a sword, a lash) down to whatever bone lies along it. */
        GASH,
        /** 5.0.17: flesh torn out (a blast, a crush, a spike bursting): a big ragged crater showing the ribs, the spine, the guts. */
        CAVITY,
        /** 5.0.17: a compound fracture: the skin split and the broken bone standing out of it (a fall, a crushing blow). */
        FRACTURE,
        /** 5.0.17: a burn: raw, blistered flesh in charred skin. */
        BURN,
        /** 5.0.17: a bruise: the skin unbroken, dark purple (yellowing as it heals). */
        BRUISE
    }

    /** The player model's parts, each a box in its own pixels (x to the model's left, y down, z backward). */
    public enum Part {
        HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG;

        /** {x0, y0, z0, x1, y1, z1}: the part's box; {@code slim} for the three-pixel arms. */
        public double[] box(boolean slim) {
            return switch (this) {
                case HEAD -> new double[] {-4, -8, -4, 4, 0, 4};
                case BODY -> new double[] {-4, 0, -2, 4, 12, 2};
                case RIGHT_ARM -> slim ? new double[] {-2, -2, -2, 1, 10, 2} : new double[] {-3, -2, -2, 1, 10, 2};
                case LEFT_ARM -> slim ? new double[] {-1, -2, -2, 2, 10, 2} : new double[] {-1, -2, -2, 3, 10, 2};
                case RIGHT_LEG, LEFT_LEG -> new double[] {-2, 0, -2, 2, 12, 2};
            };
        }

        /** How far the skin's outer layer stands off this part (the hat half a pixel, the jacket, sleeves and trousers a quarter). */
        public double outer() {
            return this == HEAD ? 0.5 : 0.25;
        }
    }

    /** A side of a part: the front (the face, the chest), the back, and the two sides. */
    public enum Face {
        FRONT, BACK, RIGHT, LEFT;

        /** The side's width (across) and height (down) in pixels on a part's box. */
        public double[] size(double[] box) {
            double across = this == FRONT || this == BACK ? box[3] - box[0] : box[5] - box[2];
            return new double[] {across, box[4] - box[1]};
        }

        /** The outward normal in part pixels. */
        public double[] normal() {
            return switch (this) {
                case FRONT -> new double[] {0, 0, -1};
                case BACK -> new double[] {0, 0, 1};
                case RIGHT -> new double[] {-1, 0, 0};
                case LEFT -> new double[] {1, 0, 0};
            };
        }

        /** How deep the part is under this side (pixels). */
        public double thickness(double[] box) {
            return this == FRONT || this == BACK ? box[5] - box[2] : box[3] - box[0];
        }

        /** The point ({@code u}, {@code v}) on this side of {@code box}, {@code out} pixels along its normal, in part pixels. */
        public double[] point(double[] box, double u, double v, double out) {
            double y = box[1] + v;
            return switch (this) {
                case FRONT -> new double[] {box[0] + u, y, box[2] - out};
                case BACK -> new double[] {box[3] - u, y, box[5] + out};
                case RIGHT -> new double[] {box[0] - out, y, box[5] - u};
                case LEFT -> new double[] {box[3] + out, y, box[2] + u};
            };
        }
    }
}

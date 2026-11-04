package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.quinque.Quinque;

/**
 * 1.3.33: each two-handed quinque's stance and the third person transform its model is drawn with in it, solved together by
 * tools/quinque_models.py (generated: never edit by hand; run the script). The chest's twist; the right arm's pitch, yaw and roll
 * (QuinqueGrip.aim); the left arm reaching its grip (x, y, z: RigPose's, as the stance's hint); the model's thirdperson_righthand
 * rotation (XYZ degrees), translation (pixels) and scale; its right and left grips (model pixels, as drawn); the stretch of it the
 * left hand may slide along (the spear's shaft; the others' grip itself).
 */
public final class QuinqueHolds {
    private QuinqueHolds() {
    }

    public record Hold(float twist, float pitch, float yaw, float roll, float[] left, float[] rotation, float[] translation, float scale,
                       float[] rightGrip, float[] leftGrip, float[] slideFrom, float[] slideTo) {
    }

    /** {@code q}'s hold, or null for the one-handed blade. */
    public static Hold of(Quinque q) {
        return switch (q) {
            case BLADE -> new Hold(0.00000F, -0.70000F, -0.25000F, 0.00000F,
                new float[] {-0.47787F, -0.09377F, 1.07421F},
                new float[] {5.00000F, 90.00000F, 0.00000F}, new float[] {-0.00800F, -0.09400F, -0.59900F}, 0.85000F,
                new float[] {5.87868F, 5.87868F, 8.00000F}, new float[] {5.87868F, 5.87868F, 8.00000F},
                new float[] {5.87868F, 5.87868F, 8.00000F}, new float[] {5.87868F, 5.87868F, 8.00000F});
            case SPEAR -> new Hold(1.00000F, -1.00000F, -1.05000F, 0.10472F,
                new float[] {-1.54211F, -0.52794F, 1.18336F},
                new float[] {119.40000F, 5.06000F, -135.08000F}, new float[] {-0.11800F, -0.64300F, -1.41000F}, 0.85000F,
                new float[] {5.52513F, 5.52513F, 8.00000F}, new float[] {11.18198F, 11.18198F, 8.00000F},
                new float[] {8.70711F, 8.70711F, 8.00000F}, new float[] {14.71751F, 14.71751F, 8.00000F});
            case KATANA -> new Hold(0.20000F, -1.15000F, -0.75000F, 0.48869F,
                new float[] {-0.34806F, 0.28368F, 0.71402F},
                new float[] {-23.93000F, 58.54000F, 45.18000F}, new float[] {-0.06200F, 1.60300F, -0.42100F}, 0.85000F,
                new float[] {4.74731F, 4.74731F, 8.00000F}, new float[] {1.42391F, 1.42391F, 8.00000F},
                new float[] {1.42391F, 1.42391F, 8.00000F}, new float[] {1.42391F, 1.42391F, 8.00000F});
            case MACE -> new Hold(0.25000F, -1.15000F, -0.75000F, 0.27925F,
                new float[] {-0.42262F, 0.44243F, 0.53996F},
                new float[] {-6.99000F, 63.84000F, 44.99000F}, new float[] {-0.05500F, 1.78000F, 0.68900F}, 0.85000F,
                new float[] {4.81802F, 4.81802F, 8.00000F}, new float[] {1.28249F, 1.28249F, 8.00000F},
                new float[] {-2.96016F, -2.96016F, 8.00000F}, new float[] {3.05025F, 3.05025F, 8.00000F});
            case LAUNCHER -> new Hold(0.85000F, -1.62000F, -1.12000F, 0.00000F,
                new float[] {-1.59215F, -0.18128F, -0.00216F},
                new float[] {-2.82000F, 74.53000F, 0.00000F}, new float[] {0.96700F, -0.32800F, -2.86500F}, 0.62000F,
                new float[] {1.30000F, 5.00000F, 8.00000F}, new float[] {11.10000F, 4.60000F, 8.00000F},
                new float[] {11.10000F, 4.60000F, 8.00000F}, new float[] {11.10000F, 4.60000F, 8.00000F});
            case DAWN -> new Hold(0.36000F, -1.05000F, -0.42000F, 0.00000F,
                new float[] {-1.14837F, 0.87238F, 0.02808F},
                new float[] {29.84000F, 86.56000F, 1.15000F}, new float[] {0.71800F, 4.28700F, -9.46200F}, 0.90000F,
                new float[] {-5.70000F, 8.00000F, 8.00000F}, new float[] {-5.70000F, 8.00000F, 8.00000F},
                new float[] {-5.70000F, 8.00000F, 8.00000F}, new float[] {-5.70000F, 8.00000F, 8.00000F});
            default -> null;
        };
    }
}

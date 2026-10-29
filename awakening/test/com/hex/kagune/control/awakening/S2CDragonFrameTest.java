package com.hex.kagune.control.awakening;

import java.util.List;
import java.util.UUID;

/** The frame clients render must be the rig the server hits with, to float precision, far from the origin. */
public final class S2CDragonFrameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        DragonRig.Ground flat = new DragonRig.Ground() {
            @Override
            public double surface(double x, double z) {
                return 70.0;
            }

            @Override
            public double minY() {
                return -64.0;
            }

            @Override
            public double maxY() {
                return 320.0;
            }
        };
        V3 anchor = new V3(29_999_000.5, 70.0, -29_999_000.5);
        DragonRig rig = new DragonRig(anchor, 99L);
        for (int tick = 0; tick < 400; tick++) rig.step(tick, List.of(), flat);
        UUID owner = new UUID(1L, 2L);
        S2CDragonFrame frame = S2CDragonFrame.capture(owner, rig, null);
        check(frame.owner().equals(owner) && frame.tick() == 399, "identity and tick");
        check(frame.segments().length == DragonRig.SEGMENTS * 3 && frame.heads().length == DragonRig.HEADS * 4, "fixed layout");
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            V3 s = rig.segment(i);
            double dx = frame.anchorX() + frame.segments()[i * 3] - s.x();
            double dz = frame.anchorZ() + frame.segments()[i * 3 + 2] - s.z();
            check(Math.abs(dx) < 1.0E-3 && Math.abs(dz) < 1.0E-3, "anchor-relative floats stay precise at the world border");
            check(Math.abs(frame.radii()[i] - rig.radius(i)) < 1.0E-4, "radius carried");
        }
        float[] e = frame.emergence();
        check(e[0] == 0.0F && e[1] == 0.0F && e[2] == 0.0F, "no emergence point before the shatter");
        System.out.println("Verified: frames mirror the server rig with float precision anywhere in the world.");
    }
}

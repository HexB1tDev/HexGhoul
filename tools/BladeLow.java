import com.hex.kagune.control.client.animation.BladeArmPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.BladeArm;
/** 4.1.47: the arm blade's lowest point in a pose (blocks above the feet), for BladeArmMoves. */
public final class BladeLow {
    static double low(RigPose p, double wrap) {
        double[] m = {99};
        com.hex.kagune.control.npc.BladeArmModel.build(wrap, false, 0x7A1E5C, (a, b, c, d, n, ca, cb, cc, cd) -> {
            for (double[] q : new double[][] {a, b, c, d}) m[0] = Math.min(m[0], ArtPreview.onPart(p, "rightArm", q)[1]);
        });
        return m[0];
    }
}

import com.hex.kagune.control.client.animation.IdlePose;
import com.hex.kagune.control.client.animation.RigPose;
import java.util.Random;

/**
 * Finds arm angles that put a hand and an elbow where a reference picture has them, through the game's own transforms
 * ({@link PoseRender#point}). Targets are world blocks with the player at the origin facing +z (the player's right is
 * -x). Usage (dev only): {@code ArmSolve <ayato|kaneki> <right|left> hx hy hz ex ey ez}.
 */
public final class ArmSolve {
    public static void main(String[] args) {
        IdlePose.Style style = args[0].equals("ayato") ? IdlePose.Style.AYATO_RELAXED : IdlePose.Style.KANEKI_FERAL;
        if (args[1].endsWith("Leg")) {
            leg(style, args);
            return;
        }
        boolean right = args[1].equals("right");
        double[] hand = {Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4])};
        double[] elbow = {Double.parseDouble(args[5]), Double.parseDouble(args[6]), Double.parseDouble(args[7])};
        RigPose base = IdlePose.pose(style, 1.0F, 0.0);
        Random random = new Random(7);
        double[] best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 400000; i++) {
            double[] c = best != null && i > 150000
                ? new double[] {best[0] + random.nextGaussian() * 0.05, best[1] + random.nextGaussian() * 0.05, best[2] + random.nextGaussian() * 0.05,
                    best[3] + random.nextGaussian() * 0.05}
                : new double[] {-2.2 + random.nextDouble() * 3.2, -1.5 + random.nextDouble() * 3.0, (right ? 1 : -1) * (-0.4 + random.nextDouble() * 1.9),
                    random.nextDouble() * 2.2};
            double score = score(base, right, c, hand, elbow);
            if (score < bestScore) {
                bestScore = score;
                best = c;
            }
        }
        RigPose pose = apply(base, right, best);
        String arm = right ? "rightArm" : "leftArm";
        double cx = right ? -1 : 1;
        double[] h = PoseRender.point(pose, arm, new double[] {cx, 9, 0});
        double[] e = PoseRender.point(pose, arm, new double[] {cx, 4, 0});
        double[] s = PoseRender.point(pose, arm, new double[] {cx, -2, 0});
        System.out.printf("%s arm: X %.3f  Y %.3f  Z %.3f  elbow %.3f   (error %.4f blocks)%n", arm, best[0], best[1], best[2], best[3], Math.sqrt(bestScore));
        System.out.printf("  shoulder top (%.3f %.3f %.3f)  elbow (%.3f %.3f %.3f)  hand (%.3f %.3f %.3f)%n", s[0], s[1], s[2], e[0], e[1], e[2], h[0], h[1], h[2]);
    }

    /** Legs: {@code ArmSolve <style> rightLeg|leftLeg kx ky kz fx fz} - knee target, and the sole's x z (its y is the ground). */
    static void leg(IdlePose.Style style, String[] args) {
        boolean right = args[1].startsWith("right");
        double[] knee = {Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4])};
        double[] foot = {Double.parseDouble(args[5]), 0.0, Double.parseDouble(args[6])};
        RigPose base = IdlePose.pose(style, 1.0F, 0.0);
        Random random = new Random(11);
        double[] best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 300000; i++) {
            double[] c = best != null && i > 120000
                ? new double[] {best[0] + random.nextGaussian() * 0.04, best[1] + random.nextGaussian() * 0.04, best[2] + random.nextGaussian() * 0.04,
                    best[3] + random.nextGaussian() * 0.04}
                : new double[] {-1.2 + random.nextDouble() * 1.4, -0.8 + random.nextDouble() * 1.6, (right ? 1 : -1) * random.nextDouble() * 0.6,
                    random.nextDouble() * 1.6};
            RigPose p = legPose(base, right, c);
            String name = right ? "rightLeg" : "leftLeg";
            double[] k = PoseRender.point(p, name, new double[] {0, 6, 0});
            double[] f = PoseRender.point(p, name, new double[] {0, 12, 0});
            double score = dist2(k, knee) + 2.0 * dist2(f, foot) + 0.002 * (c[0] * c[0] + c[1] * c[1] + c[2] * c[2]);
            if (score < bestScore) {
                bestScore = score;
                best = c;
            }
        }
        RigPose p = legPose(base, right, best);
        String name = right ? "rightLeg" : "leftLeg";
        double[] k = PoseRender.point(p, name, new double[] {0, 6, 0});
        double[] f = PoseRender.point(p, name, new double[] {0, 12, 0});
        System.out.printf("%s: X %.3f  Y %.3f  Z %.3f  knee %.3f   (error %.4f)  knee at (%.3f %.3f %.3f) sole at (%.3f %.3f %.3f), body drop %.2f px%n",
            name, best[0], best[1], best[2], best[3], Math.sqrt(bestScore), k[0], k[1], k[2], f[0], f[1], f[2], p.drop);
    }

    static RigPose legPose(RigPose base, boolean right, double[] c) {
        RigPose p = new RigPose().add(base, 1.0F);
        if (right) {
            p.rightLegX = (float)c[0];
            p.rightLegY = (float)c[1];
            p.rightLegZ = (float)c[2];
            p.rightKnee = (float)c[3];
        } else {
            p.leftLegX = (float)c[0];
            p.leftLegY = (float)c[1];
            p.leftLegZ = (float)c[2];
            p.leftKnee = (float)c[3];
        }
        return p;
    }

    static RigPose apply(RigPose base, boolean right, double[] c) {
        RigPose p = new RigPose().add(base, 1.0F);
        if (right) {
            p.rightArmX = (float)c[0];
            p.rightArmY = (float)c[1];
            p.rightArmZ = (float)c[2];
            p.rightElbow = (float)c[3];
        } else {
            p.leftArmX = (float)c[0];
            p.leftArmY = (float)c[1];
            p.leftArmZ = (float)c[2];
            p.leftElbow = (float)c[3];
        }
        return p;
    }

    static double score(RigPose base, boolean right, double[] c, double[] hand, double[] elbow) {
        RigPose p = apply(base, right, c);
        String arm = right ? "rightArm" : "leftArm";
        double cx = right ? -1 : 1;
        double[] h = PoseRender.point(p, arm, new double[] {cx, 9, 0});
        double[] e = PoseRender.point(p, arm, new double[] {cx, 4, 0});
        // Small preference for gentle angles, so equal fits pick the natural one.
        double effort = 0.002 * (c[0] * c[0] + c[1] * c[1] + c[2] * c[2]);
        return dist2(h, hand) + 0.6 * dist2(e, elbow) + effort;
    }

    static double dist2(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return dx * dx + dy * dy + dz * dz;
    }
}

package com.hex.kagune.control.king;

import java.util.ArrayList;
import java.util.List;

/**
 * 3.2.8 (the owner: War's Spire Wave, "the move that charges centipedes from the left arm and shoots them forwards in a beam", janky in
 * first person): his free left arm as he sees it himself while B is held and as the mass goes out. First person never drew that arm
 * (vanilla draws an empty off hand as nothing), so the charge's centipedes wound round the third-person body's arm, under and in front
 * of the eyes and swinging with the body's lagging turn, and the mass came out of that hand through the view. Now the arm is drawn in
 * the hand pass where vanilla puts a left arm, in its skin with War's sleeve on it, and the centipedes coil round it there.
 *
 * <p>Pure: the arm's placement as steps (each a move or a turn, applied in order as a PoseStack applies them, so a point drawn after
 * them is carried out through them last to first), shared by the game ({@code KingFirstPerson}), the tests and the preview.
 */
public final class SpireArm {
    private SpireArm() {
    }

    /** A move (by {@code x y z}, blocks) or a turn (by {@code a} radians about the axis {@code x y z}). */
    public record Step(boolean turn, double a, double x, double y, double z) {
    }

    /** The left arm part's pivot in the model (pixels) and the little sway outward the model gives it (radians about z). */
    public static final double SHOULDER_X = 5.0;
    public static final double SHOULDER_Y = 2.0;
    public static final double SWAY = -0.1;
    /** Where the centipedes coil along the arm (its own pixels down from the shoulder): the elbow to the hand, as third person. */
    public static final double ELBOW = 3.5;
    public static final double HAND = 10.5;
    /** How far past the hand their heads gather in first person, against third person's {@code 1.1} (blocks at full charge): kept out of the middle of the view. */
    public static final double REACH = 0.45;
    /** How thick the coils are in first person (of third person's), half a block from the eyes; and the mass's {@code near}. */
    public static final double THICK = 0.5;
    public static final double NEAR = 0.55;

    static Step move(double x, double y, double z) {
        return new Step(false, 0.0, x, y, z);
    }

    static Step turn(double degrees, double x, double y, double z) {
        return new Step(true, Math.toRadians(degrees), x, y, z);
    }

    /**
     * The left arm placed in the hand pass: vanilla's own (ItemInHandRenderer's empty arm, mirrored to the left, no swing, fully
     * equipped), pushed forward and up a little with {@code charge} (0..1, B held) and further, toward the middle, with {@code thrust}
     * (0..1, the mass going out).
     */
    public static List<Step> place(double charge, double thrust) {
        double f = -1.0;
        double c = clamp01(charge);
        double t = clamp01(thrust);
        List<Step> out = new ArrayList<>();
        out.add(move(f * 0.64 + 0.06 * t, -0.6 + 0.05 * c + 0.08 * t, -0.72 - 0.1 * c - 0.2 * t));
        out.add(turn(f * 45.0, 0.0, 1.0, 0.0));
        out.add(move(f * -1.0, 3.6, 3.5));
        out.add(turn(f * 120.0, 0.0, 0.0, 1.0));
        out.add(turn(200.0, 1.0, 0.0, 0.0));
        out.add(turn(f * -135.0, 0.0, 1.0, 0.0));
        out.add(move(f * 5.6, 0.0, 0.0));
        return out;
    }

    /** Then to the arm part's shoulder, with its sway: where the model draws the arm, and War's sleeve on it. */
    public static List<Step> shoulder() {
        return List.of(move(SHOULDER_X / 16.0, SHOULDER_Y / 16.0, 0.0), turn(Math.toDegrees(SWAY), 0.0, 0.0, 1.0));
    }

    /** Then the coils' own frame: down the middle of the arm (its box is 4 pixels wide, from -1), the arm along +z. */
    public static List<Step> along() {
        return List.of(move(1.0 / 16.0, 0.0, 0.0), turn(-90.0, 1.0, 0.0, 0.0));
    }

    /** Where the coils run in {@link #along}'s frame (blocks): from the elbow to the hand. */
    public static double[] elbow() {
        return new double[] {0.0, 0.0, ELBOW / 16.0};
    }

    public static double[] hand() {
        return new double[] {0.0, 0.0, HAND / 16.0};
    }

    /** All three at once: {@code place}, then the shoulder, then the coils' frame. */
    public static List<Step> coilFrame(double charge, double thrust) {
        List<Step> out = new ArrayList<>(place(charge, thrust));
        out.addAll(shoulder());
        out.addAll(along());
        return out;
    }

    /** {@code p} (in the frame after {@code steps}) carried out into the frame before them (the camera's, for the hand pass). */
    public static double[] apply(List<Step> steps, double[] p) {
        double[] q = p.clone();
        for (int i = steps.size() - 1; i >= 0; i--) {
            Step s = steps.get(i);
            if (s.turn()) {
                q = rotate(q, s.a(), s.x(), s.y(), s.z());
            } else {
                q = new double[] {q[0] + s.x(), q[1] + s.y(), q[2] + s.z()};
            }
        }
        return q;
    }

    /** A direction (no moves) carried out the same way. */
    public static double[] turnOut(List<Step> steps, double[] v) {
        double[] q = v.clone();
        for (int i = steps.size() - 1; i >= 0; i--) {
            Step s = steps.get(i);
            if (s.turn()) {
                q = rotate(q, s.a(), s.x(), s.y(), s.z());
            }
        }
        return q;
    }

    /**
     * 3.4.5 (the owner: "Spirewave now is kind of bugged. It detaches from your hand if you move"): {@code p}, a point in the hand pass's
     * frame (+x right, +y up, -z ahead), carried out into the world from the eyes at {@code eye} looking {@code yaw}, {@code pitch}
     * (Minecraft's degrees: yaw 0 south, pitch down positive). The hand pass draws in the camera's frame, so this is where its arm is.
     */
    public static double[] toWorld(double[] eye, double yaw, double pitch, double[] p) {
        double y = Math.toRadians(yaw);
        double x = Math.toRadians(pitch);
        double[] f = {-Math.sin(y) * Math.cos(x), -Math.sin(x), Math.cos(y) * Math.cos(x)};
        double[] r = {-Math.cos(y), 0.0, -Math.sin(y)};
        double[] u = {r[1] * f[2] - r[2] * f[1], r[2] * f[0] - r[0] * f[2], r[0] * f[1] - r[1] * f[0]};
        return new double[] {eye[0] + r[0] * p[0] + u[0] * p[1] - f[0] * p[2], eye[1] + r[1] * p[0] + u[1] * p[1] - f[1] * p[2],
            eye[2] + r[2] * p[0] + u[2] * p[1] - f[2] * p[2]};
    }

    /**
     * 3.4.5: the fist of the arm the hand pass draws ({@link #coilFrame} at full charge, {@code thrust} out; the coils' {@link #hand} end)
     * in the world this frame, from the eyes as {@link #toWorld}: where the mass leaves from as he sees it, so it stays on his hand as he
     * moves (3.2.8 left it where it set out, on the server's spot, and walking off left it hanging there).
     */
    public static double[] fist(double[] eye, double yaw, double pitch, double thrust) {
        return toWorld(eye, yaw, pitch, apply(coilFrame(1.0, thrust), hand()));
    }

    /** {@code p} turned by {@code a} radians about the unit axis {@code (x, y, z)} (right-handed, as JOML's rotateAxis). */
    static double[] rotate(double[] p, double a, double x, double y, double z) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        double dot = x * p[0] + y * p[1] + z * p[2];
        double cx = y * p[2] - z * p[1];
        double cy = z * p[0] - x * p[2];
        double cz = x * p[1] - y * p[0];
        return new double[] {p[0] * c + cx * s + x * dot * (1.0 - c), p[1] * c + cy * s + y * dot * (1.0 - c), p[2] * c + cz * s + z * dot * (1.0 - c)};
    }

    /**
     * How far the arm is thrust out {@code age} ticks after letting go, the mass arriving at {@code arrive} and all back by {@code back}:
     * out with it, held through the crush, drawn back as the centipedes come home.
     */
    public static double thrust(double age, double arrive, double back) {
        if (age <= 0.0) {
            return 0.0;
        }
        double out = clamp01(age / 4.0);
        double home = back <= arrive ? 1.0 : clamp01((age - arrive) / (back - arrive));
        return out * (1.0 - home * home);
    }

    static double clamp01(double v) {
        return Double.isFinite(v) ? Math.max(0.0, Math.min(1.0, v)) : 0.0;
    }
}

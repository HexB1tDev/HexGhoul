import com.hex.kagune.control.client.animation.AgentPose;
import com.hex.kagune.control.client.animation.RigPose;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 2.0.43: the White Reaper standing about, off-game, in his own skin through {@link PoseRender}'s exact transforms: an ordinary
 * investigator's idle ({@code AgentPose.idle}) beside his ({@code AgentPose.reaperIdle}), each with the case's arm hanging plumb
 * ({@code AgentPose.carry}), from several sides. Usage: {@code java -cp <classes> ReaperPreview <skin.png> <out.png> [bulk]}.
 */
public final class ReaperPreview {
    public static void main(String[] args) throws Exception {
        PoseRender.skin = ImageIO.read(new File(args[0]));
        PoseRender.slim = true;
        PoseRender.stiff = true; // an investigator is a mob on vanilla's player model: no elbows or knees bend
        File out = new File(args[1]);
        record Shot(String label, boolean reaper, boolean build, double yaw, double pitch) {
        }
        boolean builds = args.length > 2 && args[2].equals("build");
        List<Shot> shots = new ArrayList<>(builds ? List.of(
            new Shot("before, front", true, false, 0, 4),
            new Shot("his build, front", true, true, 0, 4),
            new Shot("before, side", true, false, 90, 2),
            new Shot("his build, side", true, true, 90, 2),
            new Shot("his build, three-quarter", true, true, 35, 8),
            new Shot("his build, other three-quarter", true, true, -40, 8),
            new Shot("his build, behind", true, true, 160, 10),
            new Shot("his build, close (chest)", true, true, 20, 4),
            new Shot("standing straight, before, right side", false, false, -90, 2),
            new Shot("standing straight, his build, right side", false, true, -90, 2),
            new Shot("standing straight, his build, front", false, true, 0, 2),
            new Shot("standing straight, before, right side, arm hidden", false, false, -90, 2),
            new Shot("standing straight, his build, right side, arm hidden", false, true, -90, 2),
            new Shot("standing straight, his build, three-quarter", false, true, -40, 6),
            new Shot("standing straight, his build, from above", false, true, 30, 40))
            : List.of(
            new Shot("investigators' idle, front", false, false, 0, 6),
            new Shot("the Reaper's idle, front", true, false, 0, 6),
            new Shot("the Reaper's idle, three-quarter", true, false, 35, 8),
            new Shot("the Reaper's idle, other three-quarter", true, false, -40, 8),
            new Shot("the Reaper's idle, side", true, false, 90, 4),
            new Shot("the Reaper's idle, from below (your eyes, close)", true, false, 15, -8),
            new Shot("investigators' idle, side", false, false, 90, 4),
            new Shot("the Reaper's idle, behind", true, false, 165, 10)));
        int w = 420;
        int h = 560;
        int cols = 4;
        int rows = (shots.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(w * cols, h * rows, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int i = 0; i < shots.size(); i++) {
            Shot s = shots.get(i);
            RigPose pose = new RigPose();
            if (s.reaper) {
                AgentPose.reaperIdle(pose, 0.0, 7, 1.0F);
                AgentPose.carry(pose, 0.0F);
            } else if (!s.label.startsWith("standing straight")) {
                AgentPose.idle(pose, 0.0, 7, 1.0F);
                AgentPose.carry(pose, 0.0F);
            }
            PoseRender.armOut = s.build ? com.hex.kagune.control.npc.ReaperBuild.ARM_OUT : 0.0;
            PoseRender.noRightArm = s.label.endsWith("arm hidden");
            List<PoseRender.Tri> extra = new ArrayList<>();
            if (s.build) {
                chest(extra, pose, false);
                chest(extra, pose, true);
            }
            double distance = s.label.contains("close") ? 2.4 : 4.0;
            double target = s.label.contains("close") ? 1.3 : 1.0;
            BufferedImage img = PoseRender.render(pose, PoseRender.orbit(s.yaw, s.pitch, distance, target), w, h, extra);
            g.drawImage(img, (i % cols) * w, (i / cols) * h, w, h, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.drawString(s.label, (i % cols) * w + 10, (i / cols) * h + 22);
        }
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", out);
        System.out.println("wrote " + out);
    }

    /** ReaperBuild's chest on {@code pose}'s torso, each quad cut into small cells painted from the skin (see-through texels left out). */
    static void chest(List<PoseRender.Tri> tris, RigPose pose, boolean jacket) {
        int cuts = 10;
        com.hex.kagune.control.npc.ReaperBuild.chest(jacket, (c, uv, n) -> {
            for (int a = 0; a < cuts; a++) {
                for (int b = 0; b < cuts; b++) {
                    double s0 = a / (double)cuts, s1 = (a + 1) / (double)cuts, t0 = b / (double)cuts, t1 = (b + 1) / (double)cuts;
                    double sm = (s0 + s1) / 2, tm = (t0 + t1) / 2;
                    double u = lerp(uv[0], uv[2], uv[4], uv[6], sm, tm);
                    double v = lerp(uv[1], uv[3], uv[5], uv[7], sm, tm);
                    int argb = PoseRender.skin.getRGB((int)Math.max(0, Math.min(63, Math.floor(u))), (int)Math.max(0, Math.min(63, Math.floor(v))));
                    if ((argb >>> 24) < 128) {
                        continue;
                    }
                    double[][] q = {at(c, s0, t0), at(c, s1, t0), at(c, s1, t1), at(c, s0, t1)};
                    double[][] w = new double[4][];
                    for (int k = 0; k < 4; k++) {
                        w[k] = PoseRender.point(pose, "torso", q[k]);
                    }
                    double[] wn = PoseRender.norm(PoseRender.cross(PoseRender.sub(w[1], w[0]), PoseRender.sub(w[3], w[0])));
                    double[] probe = PoseRender.point(pose, "torso", new double[] {q[0][0] + n[0], q[0][1] + n[1], q[0][2] + n[2]});
                    if (PoseRender.dot(wn, PoseRender.sub(probe, w[0])) < 0) {
                        wn = PoseRender.scale(wn, -1);
                    }
                    tris.add(new PoseRender.Tri(new double[][] {w[0], w[1], w[2]}, argb & 0xFFFFFF, wn));
                    tris.add(new PoseRender.Tri(new double[][] {w[0], w[2], w[3]}, argb & 0xFFFFFF, wn));
                }
            }
        });
    }

    /** The point at ({@code s} along the first edge, {@code t} down the sides) of the quad {@code c}. */
    static double[] at(double[][] c, double s, double t) {
        double[] top = {c[0][0] + (c[1][0] - c[0][0]) * s, c[0][1] + (c[1][1] - c[0][1]) * s, c[0][2] + (c[1][2] - c[0][2]) * s};
        double[] bottom = {c[3][0] + (c[2][0] - c[3][0]) * s, c[3][1] + (c[2][1] - c[3][1]) * s, c[3][2] + (c[2][2] - c[3][2]) * s};
        return new double[] {top[0] + (bottom[0] - top[0]) * t, top[1] + (bottom[1] - top[1]) * t, top[2] + (bottom[2] - top[2]) * t};
    }

    static double lerp(double a, double b, double c, double d, double s, double t) {
        double top = a + (b - a) * s;
        double bottom = d + (c - d) * s;
        return top + (bottom - top) * t;
    }
}

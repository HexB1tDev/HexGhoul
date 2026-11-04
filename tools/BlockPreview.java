import com.hex.kagune.control.RinkakuBlock;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.client.animation.RinkakuBlockPose;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 2.0.42: Rinkaku's block off-game: the body's guard ({@code RinkakuBlockPose}, through {@link PoseRender}'s exact transforms) with the
 * tails in theirs ({@code RinkakuBlock.pose}) as plain tubes, from several sides, for two and four tails, plus a blow, a perfect block and
 * a broken guard. Usage: {@code java -cp <classes> BlockPreview <out.png>}.
 */
public final class BlockPreview {
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/block-preview.png");
        record Shot(String label, int tails, double yaw, double pitch, double recoil, boolean perfect, double thrown, float jolt, float throwBody) {
        }
        List<Shot> shots = List.of(
            new Shot("body alone, front", 0, 0, 6, 0, false, 0, 0, 0),
            new Shot("body alone, three-quarter", 0, 40, 8, 0, false, 0, 0, 0),
            new Shot("body alone, side", 0, 90, 4, 0, false, 0, 0, 0),
            new Shot("2 tails, front", 2, 0, 8, 0, false, 0, 0, 0),
            new Shot("2 tails, three-quarter", 2, 35, 10, 0, false, 0, 0, 0),
            new Shot("2 tails, behind (F5)", 2, 160, 18, 0, false, 0, 0, 0),
            new Shot("4 tails, front", 4, 0, 8, 0, false, 0, 0, 0),
            new Shot("4 tails, three-quarter", 4, -35, 12, 0, false, 0, 0, 0),
            new Shot("1 tail, front", 1, 0, 8, 0, false, 0, 0, 0),
            new Shot("a blow lands (pressed back)", 4, 50, 8, RinkakuBlock.recoil(1.0, false), false, 0, (float)RinkakuBlock.recoil(1.0, false), 0),
            new Shot("perfect block (snapped out)", 4, 50, 8, RinkakuBlock.recoil(1.0, true), true, 0, (float)RinkakuBlock.recoil(1.0, true), 0),
            new Shot("guard broken (thrown back)", 4, 60, 10, 0, false, 1.0, 0, 1.0F),
            new Shot("own eyes, 2 tails (first person)", 2, 180, 0, 0, false, 0, 0, 0),
            new Shot("own eyes, 4 tails (first person)", 4, 180, 0, 0, false, 0, 0, 0),
            new Shot("own eyes, 4 tails, looking down", 4, 181, 0, 0, false, 0, 0, 0),
            new Shot("3 tails, front", 3, 0, 8, 0, false, 0, 0, 0));
        int w = 420;
        int h = 520;
        int cols = 4;
        int rows = (shots.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(w * cols, h * rows, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int i = 0; i < shots.size(); i++) {
            Shot s = shots.get(i);
            RigPose pose = new RigPose();
            RinkakuBlockPose.apply(pose, s.thrown > 0 ? 0.0F : 1.0F, s.jolt, s.throwBody);
            List<PoseRender.Tri> extra = new ArrayList<>();
            for (int limb = 0; limb < s.tails; limb++) {
                double[][] path = RinkakuBlock.pose(limb, s.tails, 0.0, 24, 1.8, s.label.startsWith("own eyes"));
                if (s.thrown > 0) {
                    path = RinkakuBlock.flung(limb, 0.0, 24, 1.8);
                }
                if (s.recoil != 0) {
                    path = RinkakuBlock.push(path, new double[] {0, 0, 1}, s.recoil);
                }
                tube(extra, path, 0.17, limb % 2 == 0 ? 0xC81634 : 0xA8102C);
            }
            BufferedImage img;
            if (s.label.startsWith("own eyes")) {
                // Just in front of his face (the stand-in has a head where the camera is), vanilla's 70 degrees (the vertical here).
                double down = s.yaw == 181 ? 0.9 : 0.0;
                PoseRender.Camera cam = new PoseRender.Camera(new double[] {0, 1.62, 0.0}, new double[] {0, 1.62 - down * 2.0, 2.0}, 70);
                // His own body isn't drawn in first person (vanilla draws its own arms), only the tails.
                PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, cam, 0x3A4450);
                canvas.floor(6, 0x58626C, 0x4E5862);
                for (PoseRender.Tri t : extra) canvas.tri(t, true);
                img = canvas.image();
            } else {
                img = PoseRender.render(pose, PoseRender.orbit(s.yaw, s.pitch, 4.2, 1.05), w, h, extra);
            }
            g.drawImage(img, (i % cols) * w, (i / cols) * h, w, h, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString(s.label, (i % cols) * w + 10, (i / cols) * h + 22);
        }
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", out);
        System.out.println("wrote " + out);
    }

    /** A round tube along {@code path} (owner's frame: x right), tapering to its tip, into PoseRender's world (x left). */
    static void tube(List<PoseRender.Tri> tris, double[][] path, double radius, int rgb) {
        int n = path.length;
        int sides = 10;
        double[][][] rings = new double[n][sides][];
        double[][][] normals = new double[n][sides][];
        double[] prevSide = null;
        for (int i = 0; i < n; i++) {
            double[] p = {-path[i][0], path[i][1], path[i][2]};
            double[] a = i == 0 ? p : new double[] {-path[i - 1][0], path[i - 1][1], path[i - 1][2]};
            double[] b = i == n - 1 ? p : new double[] {-path[i + 1][0], path[i + 1][1], path[i + 1][2]};
            double[] t = PoseRender.norm(PoseRender.sub(b, a));
            double[] side = prevSide == null ? PoseRender.cross(t, new double[] {0, 1, 0}) : prevSide;
            side = PoseRender.norm(PoseRender.sub(side, PoseRender.scale(t, PoseRender.dot(side, t))));
            prevSide = side;
            double[] up = PoseRender.cross(side, t);
            double u = i / (double)(n - 1);
            double r = radius * (u < 0.1 ? 0.85 + 1.5 * u : 1.0 - 0.75 * Math.pow(Math.max(0, u - 0.55) / 0.45, 1.3));
            for (int k = 0; k < sides; k++) {
                double ang = 2 * Math.PI * k / sides;
                double[] nrm = PoseRender.add(PoseRender.scale(side, Math.cos(ang)), PoseRender.scale(up, Math.sin(ang)));
                normals[i][k] = nrm;
                rings[i][k] = PoseRender.add(p, PoseRender.scale(nrm, r));
            }
        }
        for (int i = 0; i < n - 1; i++) {
            for (int k = 0; k < sides; k++) {
                int k2 = (k + 1) % sides;
                double[] nrm = PoseRender.norm(PoseRender.add(normals[i][k], normals[i][k2]));
                tris.add(new PoseRender.Tri(new double[][] {rings[i][k], rings[i + 1][k], rings[i + 1][k2]}, rgb, nrm));
                tris.add(new PoseRender.Tri(new double[][] {rings[i][k], rings[i + 1][k2], rings[i][k2]}, rgb, nrm));
            }
        }
    }
}

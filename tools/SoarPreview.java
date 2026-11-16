import com.hex.kagune.control.client.animation.AgentPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.ReaperTactics;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.quinque.DawnVolley;
import com.hex.kagune.control.quinque.Quinque;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 2.0.49: the White Reaper's soar off-game, as a strobe from the side, in his skin through {@link PoseRender}'s exact transforms (no bends:
 * an investigator's model): pressed by a quarry three blocks off, his backflip launched as Up We Go launches one ({@code ReaperTactics.soar},
 * the flight as a mob flies), turning over by the top, hanging there aiming Dawn down, and the volley's five paths ({@code DawnVolley})
 * closing on the quarry. Usage: {@code java -cp <classes> SoarPreview <skin.png> <out.png>}.
 */
public final class SoarPreview {
    public static void main(String[] args) throws Exception {
        PoseRender.skin = ImageIO.read(new File(args[0]));
        PoseRender.slim = true;
        PoseRender.stiff = true;
        // The flight (z: forward is toward the quarry; he flies back, -z).
        int ticks = 40;
        double[] ys = new double[ticks];
        double[] zs = new double[ticks];
        double y = 0.0, z = 0.0, vy = 0.0, vz = 0.0;
        int apexTick = -1;
        for (int t = 0; t < ticks; t++) {
            if (t < ReaperTactics.RAMP) {
                double[] launch = ReaperTactics.soar(t);
                vz = -launch[0];
                vy = launch[1];
            } else if (apexTick < 0 && vy <= ReaperTactics.APEX) {
                apexTick = t;
            }
            if (apexTick >= 0 && t < apexTick + ReaperTactics.AIR_AIM) {
                vy = Math.max(vy, ReaperTactics.HANG_FALL);
                vz *= ReaperTactics.HANG_DRIFT;
            }
            ys[t] = y;
            zs[t] = z;
            y = Math.max(0.0, y + vy);
            z += vz;
            vy = (vy - 0.08) * 0.98;
            vz *= 0.91;
        }
        int fire = apexTick + ReaperTactics.AIR_AIM;
        double[] quarry = {0.0, 0.95, 3.0};
        List<PoseRender.Tri> tris = new ArrayList<>();
        int[] frames = {0, 2, 4, 6, 8, 10, apexTick, fire};
        for (int frame : frames) {
            RigPose pose;
            double eyeY = ys[frame] + 1.62;
            double pitch = Math.toDegrees(Math.atan2(eyeY - quarry[1], quarry[2] - zs[frame]));
            if (frame < apexTick) {
                pose = AgentPose.soar(PaleRules.Flip.BACK, frame, -1.0);
            } else {
                pose = AgentPose.aimDawn(frame - apexTick, (float)pitch);
            }
            double ox = 0.0, oy = ys[frame], oz = zs[frame];
            for (PoseRender.Tri t : PoseRender.model(pose)) {
                double[][] v = new double[3][];
                for (int k = 0; k < 3; k++) {
                    v[k] = new double[] {t.v()[k][0] + ox, t.v()[k][1] + oy, t.v()[k][2] + oz};
                }
                tris.add(new PoseRender.Tri(v, t.rgb(), t.n()));
            }
        }
        // The quarry, a dark stand-in where it was pressing him.
        RigPose standing = new RigPose();
        PoseRender.slim = false;
        BufferedImage reaperSkin = PoseRender.skin;
        PoseRender.skin = null;
        for (PoseRender.Tri t : PoseRender.model(standing)) {
            double[][] v = new double[3][];
            for (int k = 0; k < 3; k++) {
                // Turned to face him (-z), three blocks off.
                v[k] = new double[] {-t.v()[k][0], t.v()[k][1], -t.v()[k][2] + quarry[2]};
            }
            tris.add(new PoseRender.Tri(v, 0x5A2030, new double[] {-t.n()[0], t.n()[1], -t.n()[2]}));
        }
        PoseRender.skin = reaperSkin;
        PoseRender.Camera cam = new PoseRender.Camera(new double[] {13.0, 3.6, -1.6}, new double[] {0.0, 2.6, -1.6}, 48);
        int w = 1500, h = 900;
        PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, cam, 0x2A303A);
        canvas.floor(10, 0x46505C, 0x3E4752);
        for (PoseRender.Tri t : tris) canvas.tri(t, true);
        // The volley's paths from Dawn's point (his eye's height, along his aim) to the quarry.
        double[] eye = {0.0, ys[fire] + 1.1, zs[fire]};
        double[] to = {quarry[0] - eye[0], quarry[1] - eye[1], quarry[2] - eye[2]};
        double length = Math.sqrt(to[0] * to[0] + to[1] * to[1] + to[2] * to[2]);
        double[] way = {to[0] / length, to[1] / length, to[2] / length};
        double[] tip = {eye[0] + way[0] * Quinque.DAWN_TIP, eye[1] + way[1] * Quinque.DAWN_TIP, eye[2] + way[2] * Quinque.DAWN_TIP};
        double aimed = length - Quinque.DAWN_TIP;
        for (int i = 0; i < DawnVolley.COUNT; i++) {
            for (double s = 0.0; s <= aimed; s += 0.12) {
                double[] p = DawnVolley.at(tip, way, i, aimed, s);
                double r = Math.abs((s / 1.5) - Math.round(s / 1.5)) < 0.05 ? 0.13 : 0.035;
                int c = Math.abs((s / 1.5) - Math.round(s / 1.5)) < 0.05 ? 0xFFF2A8 : 0xFFC21A;
                double[][] q = {{p[0], p[1] - r, p[2] - r}, {p[0], p[1] - r, p[2] + r}, {p[0], p[1] + r, p[2] + r}, {p[0], p[1] + r, p[2] - r}};
                canvas.tri(new PoseRender.Tri(new double[][] {q[0], q[1], q[2]}, c, new double[] {1, 0, 0}), false);
                canvas.tri(new PoseRender.Tri(new double[][] {q[0], q[2], q[3]}, c, new double[] {1, 0, 0}), false);
            }
        }
        BufferedImage img = canvas.image();
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("The White Reaper's soar (side view, every other tick): pressed 3 blocks off, he backflips as Up We Go launches one,", 12, 26);
        g.drawString(String.format("turns over by the top (%.1f blocks up, %.1f back, tick %d), hangs there aiming Dawn down, and the volley closes on the quarry.",
            ys[apexTick], -zs[apexTick], apexTick), 12, 50);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString("(Dawn itself isn't drawn here: the five paths start at its point, about two blocks out along his aim.)", 12, 74);
        g.dispose();
        ImageIO.write(img, "png", new File(args[1]));
        System.out.println("wrote " + args[1] + " apex " + ys[apexTick] + " at tick " + apexTick + ", back " + (-zs[apexTick]));
    }
}

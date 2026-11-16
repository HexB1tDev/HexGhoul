import com.hex.kagune.control.client.animation.BladeArmPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.BladeArmModel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 1.3.36: the wild ghouls' arm blade off-game: its quads ({@link BladeArmModel}, the same the game draws) on the right arm of a body
 * posed by {@link BladeArmPose} ({@link PoseRender}, stiff limbs: a ghoul NPC's vanilla model). Rows: the kagune winding out, its
 * guard and sprint, each of its three cuts. After a build:
 * <pre>
 * javac -nowarn -cp build/classes:build/base.jar -d build/blade-preview tools/PoseRender.java tools/BladeArmPreview.java
 * java -Djava.awt.headless=true -cp build/blade-preview:build/classes:build/base.jar BladeArmPreview &lt;out-dir&gt;
 * </pre>
 */
public final class BladeArmPreview {
    static List<PoseRender.Tri> blade(RigPose pose, double wrap, int colour) {
        List<PoseRender.Tri> tris = new ArrayList<>();
        BladeArmModel.build(wrap, false, colour, (a, b, c, d, n, ca, cb, cc, cd) -> {
            double[] wa = PoseRender.point(pose, "rightArm", a);
            double[] wb = PoseRender.point(pose, "rightArm", b);
            double[] wc = PoseRender.point(pose, "rightArm", c);
            double[] wd = PoseRender.point(pose, "rightArm", d);
            double[] normal = PoseRender.norm(PoseRender.cross(PoseRender.sub(wb, wa), PoseRender.sub(wd, wa)));
            boolean glow = n == BladeArmModel.GLOW;
            int rgb = ca & 0xFFFFFF;
            // Glowing quads lit as if from straight above (full bright in game).
            double[] lit = glow ? new double[] {0.0, 1.0, 0.0} : normal;
            tris.add(new PoseRender.Tri(new double[][] {wa, wb, wc}, rgb, lit));
            tris.add(new PoseRender.Tri(new double[][] {wa, wc, wd}, rgb, lit));
        });
        return tris;
    }

    record Shot(String label, RigPose pose, double wrap) {
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : ".");
        dir.mkdirs();
        PoseRender.stiff = true;
        int colour = args.length > 1 ? Integer.parseInt(args[1], 16) : 0x7A1E5C;
        List<List<Shot>> rows = new ArrayList<>();
        List<Shot> wrap = new ArrayList<>();
        for (double w : new double[] {0.12, 0.3, 0.5, 0.68, 0.84, 1.0}) {
            wrap.add(new Shot("wrap " + w, BladeArmPose.wrap(w), w));
        }
        rows.add(wrap);
        rows.add(List.of(new Shot("guard", BladeArmPose.guard(), 1.0), new Shot("run a", BladeArmPose.run(0.0, 1.0F), 1.0),
            new Shot("run b", BladeArmPose.run(Math.PI / 2.0, 1.0F), 1.0), new Shot("run c", BladeArmPose.run(Math.PI, 1.0F), 1.0)));
        for (int step = 0; step < 3; step++) {
            List<Shot> row = new ArrayList<>();
            for (double t : new double[] {0.3, 0.4, 0.48, 0.58, 0.7}) {
                row.add(new Shot("cut " + (step + 1) + " t" + t, BladeArmPose.swing(step, t), 1.0));
            }
            rows.add(row);
        }
        double[][] views = {{-35, 8}, {-95, 4}, {40, 10}};
        int cell = 300;
        int widest = 0;
        for (List<Shot> row : rows) {
            widest = Math.max(widest, row.size());
        }
        BufferedImage sheet = new BufferedImage(cell * widest, cell * views.length * rows.size(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int r = 0; r < rows.size(); r++) {
            for (int i = 0; i < rows.get(r).size(); i++) {
                Shot shot = rows.get(r).get(i);
                List<PoseRender.Tri> extra = blade(shot.pose(), shot.wrap(), colour);
                for (int v = 0; v < views.length; v++) {
                    PoseRender.Camera cam = PoseRender.orbit(views[v][0], views[v][1], 5.0, 1.05);
                    g.drawImage(PoseRender.render(shot.pose(), cam, cell, cell, extra), i * cell, (r * views.length + v) * cell, null);
                }
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                g.drawString(shot.label(), i * cell + 6, r * views.length * cell + 16);
            }
        }
        g.dispose();
        File out = new File(dir, "blade-arm.png");
        ImageIO.write(sheet, "png", out);
        System.out.println("wrote " + out);
    }
}

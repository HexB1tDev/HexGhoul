import com.hex.kagune.control.client.animation.BladeArmPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.BladeArm;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 4.1.47: the wild ghouls' arm blade off-game as the game draws it (BladeArmModel's quads on the right arm, GhoulNpcRenderer.BladeArmLayer,
 * its gleam full bright) on a ghoul NPC in its own skin, kakugan open, in ArtPreview's rasterizer: its guard from in front, three quarters,
 * the side, behind and above; close on the blade; winding out; each of its four cuts as it lands. A floor at the feet shows where the
 * blade would meet the ground.
 *
 * <pre>
 * javac -nowarn -encoding UTF-8 -cp build/quick/classes:build/quick/base.jar -sourcepath tools -d build/blade-showcase tools/BladeArmShowcase.java
 * java -Djava.awt.headless=true -cp build/blade-showcase:build/quick/classes:build/quick/base.jar BladeArmShowcase &lt;out.png&gt; [colour hex]
 * </pre>
 */
public final class BladeArmShowcase {
    public static void main(String[] args) throws Exception {
        PoseRender.stiff = true;
        int colour = args.length > 1 ? Integer.parseInt(args[1], 16) : 0x7A1E5C;
        int skin = 9;
        List<ArtPreview.Shot> shots = new ArrayList<>();
        RigPose guard = BladeArmPose.guard();
        double[] at = {0.0, 1.1, 0.4};
        Object[][] views = {{"Guard: front", new double[] {0.6, 1.5, 4.6}}, {"Guard: three quarters", new double[] {-3.2, 1.8, 3.4}},
            {"Guard: side", new double[] {-4.6, 1.3, 0.6}}, {"Guard: behind", new double[] {2.2, 2.0, -3.8}}, {"Guard: above", new double[] {0.4, 5.2, 1.6}}};
        for (Object[] v : views) {
            shots.add(shot((String)v[0], guard, 1.0, (double[])v[1], at, colour, skin));
        }
        // Close on the blade, from its edge side and its flat.
        shots.add(shot("Close: the edge and its gleam", guard, 1.0, new double[] {-1.9, 1.3, 2.4}, new double[] {-0.3, 0.9, 1.0}, colour, skin));
        shots.add(shot("Close: the flat and the veins", guard, 1.0, new double[] {-2.8, 1.0, 0.9}, new double[] {-0.4, 0.85, 0.9}, colour, skin));
        shots.add(shot("Close: the ribbons round the arm", guard, 1.0, new double[] {-1.4, 1.6, 1.9}, new double[] {-0.35, 1.25, 0.45}, colour, skin));
        // Winding out.
        for (double w : new double[] {0.35, 0.6, 0.8, 0.92}) {
            shots.add(shot("Winding out: " + (int)Math.round(w * 100) + "%", BladeArmPose.wrap(w), w, new double[] {-2.4, 1.6, 3.6}, new double[] {0.0, 1.1, 0.2},
                colour, skin));
        }
        // Each cut as it lands.
        String[] cuts = {"Cut 1 (chop down) lands", "Cut 2 (rising backhand) lands", "Cut 3 (sweep) lands", "Cut 4 (cleave) lands"};
        for (int step = 0; step < BladeArm.STEPS; step++) {
            double t = BladeArm.lands(step) / (double)BladeArm.ticks(step);
            shots.add(shot(cuts[step], BladeArmPose.swing(step, t), 1.0, new double[] {-3.6, 2.2, 4.2}, new double[] {0.0, 1.0, 0.8}, colour, skin));
        }
        File out = new File(args[0]);
        out.getAbsoluteFile().getParentFile().mkdirs();
        ArtPreview.sheet(out, "The ghouls' arm blade (4.1.47): a great blade grown through the forearm", shots, 4, 420, 380);
        System.out.println("wrote " + out);
    }

    static ArtPreview.Shot shot(String label, RigPose pose, double wrap, double[] eye, double[] at, int colour, int skin) throws Exception {
        List<WingPreview.Tri> tris = new ArrayList<>(GraftPreview.kakugan(pose, skin));
        tris.addAll(blade(pose, wrap, colour));
        tris.addAll(floor());
        return GraftPreview.ghoul(label, pose, ArtPreview.NO_WINGS, eye, at, tris, skin);
    }

    static List<WingPreview.Tri> blade(RigPose pose, double wrap, int colour) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        com.hex.kagune.control.npc.BladeArmModel.build(wrap, false, colour, (a, b, c, d, n, ca, cb, cc, cd) -> {
            double[][] w = {ArtPreview.onPart(pose, "rightArm", a), ArtPreview.onPart(pose, "rightArm", b), ArtPreview.onPart(pose, "rightArm", c),
                ArtPreview.onPart(pose, "rightArm", d)};
            boolean glow = n == com.hex.kagune.control.npc.BladeArmModel.GLOW;
            double[] normal = glow ? new double[] {0, 1, 0} : PoseRender.norm(PoseRender.cross(PoseRender.sub(w[1], w[0]), PoseRender.sub(w[3], w[0])));
            if (!glow) {
                // The quad's own normal, turned the way the model's points (the model's is outward).
                double[] mn = ArtPreview.onPart(pose, "rightArm", new double[] {a[0] + n[0], a[1] + n[1], a[2] + n[2]});
                if (PoseRender.dot(normal, PoseRender.sub(mn, w[0])) < 0) {
                    normal = PoseRender.scale(normal, -1);
                }
            }
            tris.add(new WingPreview.Tri(new double[][] {w[0], w[1], w[2]}, new int[] {ca, cb, cc}, normal, true, glow));
            tris.add(new WingPreview.Tri(new double[][] {w[0], w[2], w[3]}, new int[] {ca, cc, cd}, normal, true, glow));
        });
        return tris;
    }

    /** A thin checked floor at the feet (y 0), so a blade through the ground shows. */
    static List<WingPreview.Tri> floor() {
        List<WingPreview.Tri> tris = new ArrayList<>();
        for (int i = -4; i < 4; i++) {
            for (int j = -4; j < 4; j++) {
                int c = ((i + j) & 1) == 0 ? 0xFF58626C : 0xFF4E5862;
                double x0 = i * 0.5;
                double z0 = j * 0.5;
                double[][] q = {{x0, 0, z0}, {x0 + 0.5, 0, z0}, {x0 + 0.5, 0, z0 + 0.5}, {x0, 0, z0 + 0.5}};
                tris.add(new WingPreview.Tri(new double[][] {q[0], q[1], q[2]}, new int[] {c, c, c}, new double[] {0, 1, 0.001}, true));
                tris.add(new WingPreview.Tri(new double[][] {q[0], q[2], q[3]}, new int[] {c, c, c}, new double[] {0, 1, 0.001}, true));
            }
        }
        return tris;
    }
}

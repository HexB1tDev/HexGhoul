import com.hex.kagune.control.FemaleBody;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.outfit.OutfitFit;
import com.hex.kagune.control.outfit.OutfitModels;
import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.control.ukaku.WingRig;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * 4.1.89 (the owner: "when women are wearing our armor, our CCG armor or the ghoul armor, their breasts literally stick out from the
 * armor ... So you got to fix that so the armor adapts to their chest."): a woman in each outfit's coat, as OutfitLayer draws it on her:
 * BEFORE, the coat as made with her bust (FemaleFigure.bust's wedge, in skin) drawn under it; NOW, the coat fitted to her bust
 * (OutfitFit) and her skin's bust not drawn. From in front, each side, three quarters and from below; then a slimmer figure (an
 * investigator's, AgentRules.FEMALE_CURVES), her bust bounced up and down, and the coat's front close up. Usage (after
 * tools/fast_tests.sh, which builds build/fast):
 * <pre>
 * javac -nowarn -cp build/fast/classes:build/fast/base.jar -d &lt;dir&gt; tools/PoseRender.java tools/WingPreview.java tools/ArtPreview.java tools/ArmorFitPreview.java
 * java -Djava.awt.headless=true -cp &lt;dir&gt;:build/fast/classes:build/fast/base.jar ArmorFitPreview &lt;out.png&gt;
 * </pre>
 */
public final class ArmorFitPreview {
    /** A plain skin tone for her bust in the BEFORE shots (what came through the coat). */
    static final int SKIN = 0xFFE9CDB8;

    private ArmorFitPreview() {
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        Map<String, OutfitModels.Model> all;
        try (java.io.InputStream in = new java.io.FileInputStream("src/main/resources" + OutfitModels.RESOURCE)) {
            all = OutfitModels.read(in);
        }
        WingRig.Input none = new WingRig.Input(WingForm.TWIN, 300, 0.0F, 1.3, 0, 0, WingRig.Motion.NONE, 0, 0.4F, 0, 0, false, 1, 1, false);
        double[] target = {0.0, 1.05, 0.0};
        double[][] eyes = {{0.0, 1.25, 2.6}, {2.6, 1.2, 0.0}, {-2.6, 1.2, 0.0}, {1.85, 1.35, 1.85}, {-1.2, 0.55, 2.2}};
        String[] views = {"front", "side", "other side", "three quarters", "from below"};
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (String id : new String[] {"formal", "crimson"}) {
            OutfitModels.Model model = all.get(id);
            BufferedImage texture = ImageIO.read(new File("art/outfits/" + id + ".png"));
            String name = id.equals("formal") ? "Formal Outfit (CCG)" : "Crimson Outfit (ghoul)";
            RigPose p = new RigPose();
            p.settle(1.0F);
            for (int k = 0; k < eyes.length; k++) {
                List<WingPreview.Tri> before = ArtPreview.outfit(p, model, texture);
                before.addAll(bust(p, 1.0F, 0.0F));
                shots.add(new ArtPreview.Shot("BEFORE, " + name + ": " + views[k], p, none, eyes[k], target, before));
            }
            for (int k = 0; k < eyes.length; k++) {
                shots.add(new ArtPreview.Shot("NOW, " + name + ": " + views[k], p, none, eyes[k], target,
                    ArtPreview.outfit(p, fitted(model, 1.0F, 0.0F), texture)));
            }
            // A slimmer figure (an investigator's), her bust bounced up and down (as she lands), and walking.
            float slim = com.hex.kagune.control.npc.AgentRules.FEMALE_CURVES;
            shots.add(new ArtPreview.Shot(String.format(java.util.Locale.ROOT, "NOW, %s: curves %.2f, side", name, slim), p, none, eyes[1], target,
                ArtPreview.outfit(p, fitted(model, slim, 0.0F), texture)));
            float most = FemaleBody.jigglePixels(FemaleBody.JIGGLE_MOST);
            for (float bounce : new float[] {most, -most}) {
                shots.add(new ArtPreview.Shot(String.format(java.util.Locale.ROOT, "NOW, %s: bounced %+.1f px, side", name, bounce), p, none, eyes[1], target,
                    ArtPreview.outfit(p, fitted(model, 1.0F, bounce), texture)));
            }
            RigPose walk = new RigPose();
            walk.rightLegX = -0.55F;
            walk.leftLegX = 0.55F;
            walk.rightArmX = 0.5F;
            walk.leftArmX = -0.5F;
            walk.settle(1.0F);
            shots.add(new ArtPreview.Shot("NOW, " + name + ": walking, three quarters", walk, none, eyes[3], target,
                ArtPreview.outfit(walk, fitted(model, 1.0F, 0.0F), texture)));
            shots.add(new ArtPreview.Shot("NOW, " + name + ": close up", p, none, new double[] {0.9, 1.45, 1.2}, new double[] {0.0, 1.2, 0.0},
                ArtPreview.outfit(p, fitted(model, 1.0F, 0.0F), texture)));
        }
        ArtPreview.sheet(out, "A woman's coat takes her bust's shape (4.1.89): before, her bust came through it; now the coat itself is shaped over it",
            shots, 5, 300, 340);
    }

    /** {@code model} with its coat's body cubes fitted to a bust of {@code curves}, its tip {@code bounce} up, as OutfitLayer draws it on her. */
    static OutfitModels.Model fitted(OutfitModels.Model model, float curves, float bounce) {
        List<OutfitModels.Cube> cubes = new ArrayList<>();
        for (OutfitModels.Cube cube : model.cubes()) {
            if (cube.bone() != OutfitModels.BODY || cube.slot() != OutfitModels.CHESTPLATE) {
                cubes.add(cube);
            }
        }
        // The very call OutfitLayer makes (its key, the cut at the bounce's tip, the cache).
        cubes.addAll(OutfitFit.fitted(model.id() + '/' + OutfitModels.CHESTPLATE, model.on(OutfitModels.BODY, OutfitModels.CHESTPLATE), curves, bounce));
        return new OutfitModels.Model(model.id(), model.textureWidth(), model.textureHeight(), cubes);
    }

    /** FemaleFigure.bust's wedge on {@code pose}'s torso (skin, flat), as the game drew it under the coat before. */
    static List<WingPreview.Tri> bust(RigPose pose, float curves, float bounce) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        double top = FemaleBody.BUST_TOP;
        double tip = FemaleBody.BUST_TIP - bounce;
        double bottom = FemaleBody.BUST_BOTTOM;
        double front = FemaleBody.BUST_FRONT;
        double out = FemaleBody.BUST_FRONT + (FemaleBody.BUST_OUT - FemaleBody.BUST_FRONT) * curves;
        quad(tris, pose, new double[][] {{-4, top, front}, {4, top, front}, {4, tip, out}, {-4, tip, out}});
        quad(tris, pose, new double[][] {{-4, tip, out}, {4, tip, out}, {4, bottom, front}, {-4, bottom, front}});
        for (double x : new double[] {-4.0, 4.0}) {
            quad(tris, pose, new double[][] {{x, top, front}, {x, tip, out}, {x, bottom, front}, {x, bottom, front}});
        }
        return tris;
    }

    static void quad(List<WingPreview.Tri> tris, RigPose pose, double[][] local) {
        double[][] w = new double[4][];
        for (int k = 0; k < 4; k++) {
            double[] at = PoseRender.point(pose, "torso", local[k]);
            w[k] = new double[] {-at[0], at[1], at[2]};
        }
        double[] n = PoseRender.norm(PoseRender.cross(PoseRender.sub(w[1], w[0]), PoseRender.sub(w[2], w[0])));
        tris.add(new WingPreview.Tri(new double[][] {w[0], w[1], w[2]}, new int[] {SKIN, SKIN, SKIN}, n, true));
        tris.add(new WingPreview.Tri(new double[][] {w[0], w[2], w[3]}, new int[] {SKIN, SKIN, SKIN}, n, true));
    }
}

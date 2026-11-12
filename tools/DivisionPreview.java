import com.hex.kagune.control.client.animation.BladeArmPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.npc.BladeArmModel;
import com.hex.kagune.control.outfit.OutfitModels;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 4.1.46: the CCG's ghoul division off-game (npc.Gang.CCG_DIVISION): three of them as the game draws them, each in the CCG's Formal Outfit
 * (OutfitLayer: the outfit's cubes on the posed body, its own texture) over a ghoul NPC's skin, with its kagune out (the transplant's two
 * tails, the twin wings, the arm blade in its guard, each as GraftPreview and BladeArmPreview draw the game's own) and NO kakugan
 * (GhoulNpcRenderer.Eyes draws none for it): from in front, the side and behind; then a division member's face beside a wild ghoul's
 * (kakugan open) at the same moment.
 *
 * <pre>
 * javac -nowarn -encoding UTF-8 -cp build/quick/classes:build/quick/base.jar -sourcepath tools -d build/division-preview tools/DivisionPreview.java
 * java -Djava.awt.headless=true -cp build/division-preview:build/quick/classes:build/quick/base.jar DivisionPreview &lt;out.png&gt;
 * </pre>
 */
public final class DivisionPreview {
    public static void main(String[] args) throws Exception {
        PoseRender.stiff = true;
        OutfitModels.Model formal;
        try (java.io.InputStream in = new java.io.FileInputStream("src/main/resources" + OutfitModels.RESOURCE)) {
            formal = OutfitModels.read(in).get("formal");
        }
        BufferedImage paint = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/outfit/formal.png"));
        List<ArtPreview.Shot> shots = new ArrayList<>();
        int rinkaku = 2;
        int ukaku = 6;
        int koukaku = 9;
        RigPose still = new RigPose();
        // The transplant's tails (two, as a stronger one's), from in front, the side and behind.
        int mastery = 200;
        double size = GraftStage.TRANSPLANT.length(mastery);
        double[] tailLook = {0.0, 1.4 + size * 0.15, -0.4};
        double[][] tailEyes = {{size * 0.75, 1.9 + size * 0.25, size * 1.35}, {size * 1.45, 1.8, -0.2}, {-size * 0.9, 2.4 + size * 0.35, -size * 1.25}};
        String[] views = {"front", "side", "behind"};
        for (int v = 0; v < 3; v++) {
            List<WingPreview.Tri> tris = new ArrayList<>(ArtPreview.outfit(still, formal, paint));
            tris.addAll(GraftPreview.whole(still, GraftStage.TRANSPLANT, mastery, GraftMove.NONE, 0.0, null, 1.0, tailEyes[v]));
            shots.add(GraftPreview.ghoul("Division, rinkaku (two tails): " + views[v], still, ArtPreview.NO_WINGS, tailEyes[v], tailLook, tris, rinkaku));
        }
        // The twin wings.
        double[][] wingEyes = {{2.4, 2.1, 4.2}, {4.6, 1.9, -0.3}, {-2.8, 2.7, -3.8}};
        for (int v = 0; v < 3; v++) {
            shots.add(GraftPreview.ghoul("Division, ukaku (twin wings): " + views[v], still, GraftPreview.npcWings(150), wingEyes[v],
                new double[] {0.0, 1.45, -0.3}, ArtPreview.outfit(still, formal, paint), ukaku));
        }
        // The arm blade, wound out, in its guard.
        RigPose guard = BladeArmPose.guard();
        double[][] bladeEyes = {{1.2, 1.6, 3.6}, {3.7, 1.5, 0.4}, {-1.4, 1.9, -3.4}};
        for (int v = 0; v < 3; v++) {
            List<WingPreview.Tri> tris = new ArrayList<>(ArtPreview.outfit(guard, formal, paint));
            tris.addAll(blade(guard, 0x7A1E5C));
            shots.add(GraftPreview.ghoul("Division, koukaku (arm blade, guard): " + views[v], guard, ArtPreview.NO_WINGS, bladeEyes[v],
                new double[] {0.0, 1.1, 0.2}, tris, koukaku));
        }
        // Faces, kagune out: the division's (no kakugan) and a wild ghoul's (kakugan open), same skin, same moment.
        double[] faceEye = {0.55, 1.82, 1.55};
        double[] faceAt = {0.0, 1.72, 0.0};
        shots.add(GraftPreview.ghoul("Division member, kagune out: NO kakugan", still, ArtPreview.NO_WINGS, faceEye, faceAt,
            ArtPreview.outfit(still, formal, paint), rinkaku));
        List<WingPreview.Tri> wild = new ArrayList<>(GraftPreview.kakugan(still, rinkaku));
        shots.add(GraftPreview.ghoul("A wild ghoul, kagune out (for comparison): kakugan", still, ArtPreview.NO_WINGS, faceEye, faceAt, wild, rinkaku));
        File out = new File(args[0]);
        out.getAbsoluteFile().getParentFile().mkdirs();
        ArtPreview.sheet(out, "The CCG's ghoul division: the Formal Outfit, a kagune of their own, no kakugan", shots, 3, 440, 400);
        System.out.println("wrote " + out);
    }

    /** The arm blade's quads (BladeArmModel, as GhoulNpcRenderer.BladeArmLayer draws them) on {@code pose}'s right arm, its seams lit full. */
    static List<WingPreview.Tri> blade(RigPose pose, int colour) {
        List<WingPreview.Tri> tris = new ArrayList<>();
        BladeArmModel.build(1.0, false, colour, (a, b, c, d, n, ca, cb, cc, cd) -> {
            double[][] w = {ArtPreview.onPart(pose, "rightArm", a), ArtPreview.onPart(pose, "rightArm", b), ArtPreview.onPart(pose, "rightArm", c),
                ArtPreview.onPart(pose, "rightArm", d)};
            boolean glow = n == BladeArmModel.GLOW;
            double[] normal = glow ? new double[] {0, 1, 0} : PoseRender.norm(PoseRender.cross(PoseRender.sub(w[1], w[0]), PoseRender.sub(w[3], w[0])));
            tris.add(new WingPreview.Tri(new double[][] {w[0], w[1], w[2]}, new int[] {ca, cb, cc}, normal, true, glow));
            tris.add(new WingPreview.Tri(new double[][] {w[0], w[2], w[3]}, new int[] {ca, cc, cd}, normal, true, glow));
        });
        return tris;
    }
}

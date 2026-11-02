import com.hex.kagune.control.KaguneTint;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.family.ChildKaguneRules;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.family.KaguneGenes;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.npc.WingRules;
import com.hex.kagune.control.ukaku.WingRig;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 1.3.43 off-game: a ghoul child of a Rinkaku father and an Ukaku mother (Touka) with its kagune out, at its real size: one of the owner's
 * child skins (read only, scaled down to 64 x 64 in memory, its kakugan layers laid over as the game draws them), the body and the
 * kagune drawn at {@link ChildKaguneRules#scale} about its feet, the two tails at {@link ChildKaguneRules#tailMastery}, the one wing at
 * {@link ChildKaguneRules#wingMastery} ({@link WingRules#child}), recoloured as the game recolours them ({@link KaguneTint#recolour}),
 * Touka beside it for scale. Usage after a build: {@code javac -cp build/classes -sourcepath tools -d <cls> tools/ChildKagunePreview.java},
 * then {@code java -cp build/classes:<cls> ChildKagunePreview <out-dir>}.
 */
public final class ChildKagunePreview {
    private static final String TEXTURES = "src/main/resources/assets/hexkagune/textures/entity/";

    private ChildKagunePreview() {
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/preview");
        out.mkdirs();
        BufferedImage kid = kakugan("son_04");
        BufferedImage touka = small(ImageIO.read(new File(TEXTURES + "touka/touka.png")));
        double toukaSize = com.hex.kagune.control.npc.Persona.TOUKA.size();
        Object[][] rows = {
            {ChildStage.CHILD, 0xA50F37, "child (5-8 days): Touka's crimson"},
            {ChildStage.TEEN, 0x2E6BD8, "teen (9+ days): its father's colour (a blue, for the example)"}};
        double[][][] views = {
            {{1.2, 1.3, 5.6}, {-0.8, 0.95, 0.0}},
            {{0.6, 2.0, -5.6}, {-0.8, 1.0, 0.0}},
            {{5.4, 1.3, 1.2}, {-0.6, 0.9, 0.0}}};
        String[] names = {"front", "behind", "side"};
        ArtPreview.fov = 40.0;
        List<ArtPreview.Shot> shots = new ArrayList<>();
        for (Object[] row : rows) {
            ChildStage stage = (ChildStage)row[0];
            int colour = (Integer)row[1];
            double s = ChildKaguneRules.scale(stage);
            for (int v = 0; v < views.length; v++) {
                double[] eye = views[v][0];
                List<WingPreview.Tri> tris = new ArrayList<>();
                RigPose still = new RigPose();
                // The child (its kakugan open: it is fighting), then Touka a little way off to its left, as tall as she is.
                tris.addAll(placed(body(still, kid, false), s, 0.0, 0.0));
                tris.addAll(placed(body(still, touka, true), toukaSize, -1.9, 0.0));
                // Its two tails, as the game draws them at its size, in its colour.
                double[] rigEye = {eye[0] / s, eye[1] / s, eye[2] / s};
                for (WingPreview.Tri t : GraftPreview.whole(still, GraftStage.TRANSPLANT, ChildKaguneRules.tailMastery(stage), GraftMove.NONE, 0.0, null, 1.0,
                    rigEye)) {
                    tris.add(tinted(scaled(t, s, 0.0, 0.0), colour));
                }
                // Its one wing, on the right (KaguneGenes.side picks it per child), at its size, in its colour.
                WingRig.Input wing = WingRules.child(ChildKaguneRules.wingMastery(stage), KaguneGenes.wings(KaguneGenes.TAILS | KaguneGenes.WING), 1, 1.0F,
                    40.0, false, 0.0F, WingRig.FORWARD, 0.0F, 0.0F, 1.0F);
                for (WingRig.Quad q : WingRig.build(wing, rigEye)) {
                    float[] x = q.xyz();
                    double[][] c = new double[4][];
                    for (int k = 0; k < 4; k++) {
                        c[k] = new double[] {x[k * 3] * s, x[k * 3 + 1] * s, x[k * 3 + 2] * s};
                    }
                    int[] a = KaguneTint.recolour(q.argb(), colour);
                    double[] n = {q.nx(), q.ny(), q.nz()};
                    tris.add(new WingPreview.Tri(new double[][] {c[0], c[1], c[2]}, new int[] {a[0], a[1], a[2]}, n, false));
                    tris.add(new WingPreview.Tri(new double[][] {c[0], c[2], c[3]}, new int[] {a[0], a[2], a[3]}, n, false));
                }
                shots.add(new ArtPreview.Shot(row[2] + ": " + names[v], null, ArtPreview.NO_WINGS, eye, views[v][1], tris));
            }
        }
        ArtPreview.sheet(new File(out, "child-rinkaku-ukaku.png"),
            "A Rinkaku father and Touka's child: two tails and one wing, at its real size (Touka beside it)", shots, 3, 460, 400);
    }

    /** The posed player model in {@code skin} (slim arms or wide) as opaque triangles, feet at the origin. */
    static List<WingPreview.Tri> body(RigPose pose, BufferedImage skin, boolean slim) {
        PoseRender.skin = skin;
        PoseRender.slim = slim;
        PoseRender.stiff = true;
        try {
            return ArtPreview.body(pose);
        } finally {
            PoseRender.skin = null;
            PoseRender.slim = false;
            PoseRender.stiff = false;
        }
    }

    static List<WingPreview.Tri> placed(List<WingPreview.Tri> tris, double scale, double dx, double dz) {
        List<WingPreview.Tri> out = new ArrayList<>();
        for (WingPreview.Tri t : tris) {
            out.add(scaled(t, scale, dx, dz));
        }
        return out;
    }

    static WingPreview.Tri scaled(WingPreview.Tri t, double scale, double dx, double dz) {
        double[][] p = new double[t.p().length][];
        for (int k = 0; k < p.length; k++) {
            p[k] = new double[] {t.p()[k][0] * scale + dx, t.p()[k][1] * scale, t.p()[k][2] * scale + dz};
        }
        return new WingPreview.Tri(p, t.argb(), t.n(), t.opaque(), t.additive(), t.uv(), t.tex());
    }

    static WingPreview.Tri tinted(WingPreview.Tri t, int colour) {
        return new WingPreview.Tri(t.p(), KaguneTint.recolour(t.argb(), colour), t.n(), t.opaque(), t.additive(), t.uv(), t.tex());
    }

    /** One of the owner's child skins scaled to 64 x 64, its kakugan layers (both eyes) laid over it. Nothing is written back. */
    static BufferedImage kakugan(String key) throws Exception {
        BufferedImage skin = small(ImageIO.read(new File(TEXTURES + "child/skins/" + key + ".png")));
        for (String layer : new String[] {"_kr", "_kl", "_gr", "_gl"}) {
            File f = new File(TEXTURES + "child/skins/" + key + layer + ".png");
            if (f.exists()) {
                BufferedImage over = small(ImageIO.read(f));
                for (int y = 0; y < 64; y++) {
                    for (int x = 0; x < 64; x++) {
                        int o = over.getRGB(x, y);
                        if ((o >>> 24) > 32) {
                            skin.setRGB(x, y, 0xFF000000 | (o & 0xFFFFFF));
                        }
                    }
                }
            }
        }
        return skin;
    }

    /** {@code image} at 64 x 64 (nearest neighbour, as an HD skin's pixels map onto the model's). */
    static BufferedImage small(BufferedImage image) {
        if (image.getWidth() == 64 && image.getHeight() == 64) {
            BufferedImage copy = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            copy.getGraphics().drawImage(image, 0, 0, null);
            return copy;
        }
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, 0, 0, 64, 64, null);
        g.dispose();
        return out;
    }
}

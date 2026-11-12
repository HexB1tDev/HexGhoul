import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.Gang;
import com.hex.kagune.control.outfit.OutfitModels;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 4.1.45: the roaming gangs off-game (npc.Gang): each gang's masks on a ghoul's head (as OutfitLayer draws a mask: the mask's model on the
 * head, in its own paint; a variant on its base's model: Outfits.geometryOf), close from the front and three-quarter, then each gang
 * standing together, every one in a different mask and nothing else.
 *
 * <pre>
 * javac -nowarn -cp build/quick/classes:build/quick/base.jar -d build/gang-preview tools/PoseRender.java tools/GangPreview.java
 * java -Djava.awt.headless=true -cp build/gang-preview:build/quick/classes:build/quick/base.jar:src/main/resources GangPreview &lt;out.png&gt;
 * </pre>
 */
public final class GangPreview {
    static final String SKINS = "src/main/resources/assets/hexkagune/textures/entity/ghoul/";
    static final String MASKS = "src/main/resources/assets/hexkagune/textures/outfit/";

    public static void main(String[] args) throws Exception {
        PoseRender.stiff = true;
        List<BufferedImage> frames = new ArrayList<>();
        String[] skins = {"boy_01", "girl_02", "boy_03", "girl_04", "boy_05"};
        for (Gang gang : new Gang[] {Gang.CLOWNS, Gang.GAS_MASKS}) {
            for (int i = 0; i < gang.masks().size(); i++) {
                String mask = gang.masks().get(i);
                frames.add(one(skins[i], mask, 0, 0, 2.2, 1.55, 240, 240, mask + ", front"));
                frames.add(one(skins[i], mask, 35, 6, 2.2, 1.55, 240, 240, mask + ", three-quarter"));
            }
        }
        for (Gang gang : new Gang[] {Gang.CLOWNS, Gang.GAS_MASKS}) {
            frames.add(group(gang, skins, gang.title() + ", walking together"));
        }
        sheet(new File(args[0]), frames);
    }

    /** One ghoul in {@code skin} wearing {@code mask}, seen from {@code yaw}, {@code pitch}. */
    static BufferedImage one(String skin, String mask, double yaw, double pitch, double distance, double at, int w, int h, String text) throws Exception {
        List<PoseRender.Tri> tris = body(skin, mask, 0.0, 0.0, 0.0);
        PoseRender.Camera cam = PoseRender.orbit(yaw, pitch, distance, at);
        PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, cam, 0x3A4450);
        for (PoseRender.Tri t : tris) {
            canvas.tri(t, true);
        }
        return label(canvas.image(), text);
    }

    /** The whole gang in a loose line, each in a different mask. */
    static BufferedImage group(Gang gang, String[] skins, String text) throws Exception {
        List<PoseRender.Tri> tris = new ArrayList<>();
        int n = gang.masks().size();
        for (int i = 0; i < n; i++) {
            double x = (i - (n - 1) / 2.0) * 1.1;
            double z = (i % 2) * 0.6;
            tris.addAll(body(skins[i], gang.masks().get(i), x, 0.0, z));
        }
        PoseRender.Camera cam = PoseRender.orbit(18, 8, 5.2, 1.1);
        PoseRender.Canvas canvas = new PoseRender.Canvas(960, 400, 2, cam, 0x3A4450);
        canvas.floor(6, 0x58626C, 0x4E5862);
        for (PoseRender.Tri t : tris) {
            canvas.tri(t, true);
        }
        return label(canvas.image(), text);
    }

    /** A ghoul's body in {@code skin} and its mask's faces, moved to ({@code dx}, {@code dy}, {@code dz}) (blocks). */
    static List<PoseRender.Tri> body(String skin, String mask, double dx, double dy, double dz) throws Exception {
        PoseRender.skin = ImageIO.read(new File(SKINS + skin + ".png"));
        PoseRender.slim = skin.startsWith("girl");
        RigPose pose = new RigPose();
        List<PoseRender.Tri> out = new ArrayList<>();
        for (PoseRender.Tri t : PoseRender.model(pose)) {
            out.add(moved(t, dx, dy, dz));
        }
        BufferedImage paint = ImageIO.read(new File(MASKS + mask + ".png"));
        OutfitModels.Model model = OutfitModels.get(mask);
        for (OutfitModels.Cube cube : model.on(OutfitModels.HEAD, -1)) {
            for (OutfitModels.Face face : cube.faces()) {
                double[][] w = new double[4][];
                for (int k = 0; k < 4; k++) {
                    w[k] = PoseRender.point(pose, "head", new double[] {face.corners()[k * 3], face.corners()[k * 3 + 1], face.corners()[k * 3 + 2]});
                }
                float[] uv = face.uvs();
                double u = (uv[0] + uv[2] + uv[4] + uv[6]) / 4.0;
                double v = (uv[1] + uv[3] + uv[5] + uv[7]) / 4.0;
                int argb = paint.getRGB((int)Math.min(paint.getWidth() - 1, u * paint.getWidth()), (int)Math.min(paint.getHeight() - 1, v * paint.getHeight()));
                if ((argb >>> 24) < 16) {
                    continue;
                }
                double[] n = PoseRender.norm(PoseRender.cross(PoseRender.sub(w[1], w[0]), PoseRender.sub(w[3], w[0])));
                double[] probe = PoseRender.point(pose, "head", new double[] {face.corners()[0] + face.normal()[0], face.corners()[1] + face.normal()[1],
                    face.corners()[2] + face.normal()[2]});
                if (PoseRender.dot(n, PoseRender.sub(probe, w[0])) < 0) {
                    n = PoseRender.scale(n, -1);
                }
                out.add(moved(new PoseRender.Tri(new double[][] {w[0], w[1], w[2]}, argb & 0xFFFFFF, n), dx, dy, dz));
                out.add(moved(new PoseRender.Tri(new double[][] {w[0], w[2], w[3]}, argb & 0xFFFFFF, n), dx, dy, dz));
            }
        }
        return out;
    }

    static PoseRender.Tri moved(PoseRender.Tri t, double dx, double dy, double dz) {
        double[][] v = new double[3][];
        for (int i = 0; i < 3; i++) {
            v[i] = new double[] {t.v()[i][0] + dx, t.v()[i][1] + dy, t.v()[i][2] + dz};
        }
        return new PoseRender.Tri(v, t.rgb(), t.n());
    }

    static BufferedImage label(BufferedImage img, String text) {
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.drawString(text, 5, 14);
        g.dispose();
        return img;
    }

    static void sheet(File out, List<BufferedImage> frames) throws Exception {
        int cols = 4;
        int cell = 240;
        int heads = frames.size() - 2;
        int rows = (heads + cols - 1) / cols;
        BufferedImage s = new BufferedImage(cols * cell, rows * cell + 2 * 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = s.createGraphics();
        for (int i = 0; i < heads; i++) {
            g.drawImage(frames.get(i), i % cols * cell, i / cols * cell, null);
        }
        g.drawImage(frames.get(heads), 0, rows * cell, null);
        g.drawImage(frames.get(heads + 1), 0, rows * cell + 400, null);
        g.dispose();
        out.getAbsoluteFile().getParentFile().mkdirs();
        ImageIO.write(s, "png", out);
        System.out.println("wrote " + out);
    }
}

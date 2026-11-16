import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.Persona;
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
 * 4.1.54: Banjou off-game: his game texture (the owner's skin, its arms narrowed) on the slim arms with no muscle (the owner: "just alex
 * model by the way, not muscled at all"), by day and at night, his gas mask on only at night (the owner: "puts on the mask at night"):
 * the mask OutfitLayer.faction gives him for that hour (Persona.maskAtNight, EtoRules.night), drawn as OutfitLayer draws a mask (its model
 * on the head, in its own paint). From in front, three quarters, his side and behind.
 * <pre>
 * javac -nowarn -cp build/quick/classes:build/quick/base.jar -d build/banjou-preview tools/PoseRender.java tools/GangPreview.java tools/BanjouPreview.java
 * java -Djava.awt.headless=true -cp build/banjou-preview:build/quick/classes:build/quick/base.jar:src/main/resources BanjouPreview &lt;out.png&gt;
 * </pre>
 */
public final class BanjouPreview {
    public static void main(String[] args) throws Exception {
        Persona p = Persona.BANJOU;
        BufferedImage skin = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/entity/banjou/banjou.png"));
        double[] yaws = {0, 35, 90, 180};
        String[] views = {"front", "three quarters", "side", "behind"};
        long[] times = {6000L, 18000L};
        String[] when = {"day (6000)", "night (18000)"};
        int w = 240;
        int h = 300;
        BufferedImage sheet = new BufferedImage(yaws.length * w, times.length * h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int r = 0; r < times.length; r++) {
            // As OutfitLayer.faction: his mask at night, none by day.
            String mask = p.maskAtNight() && EtoRules.night(times[r]) ? p.mask() : null;
            for (int c = 0; c < yaws.length; c++) {
                PoseRender.skin = skin;
                PoseRender.slim = p.slim();
                PoseRender.stiff = false;
                RigPose pose = new RigPose();
                List<PoseRender.Tri> tris = new ArrayList<>(PoseRender.model(pose));
                if (mask != null) {
                    tris.addAll(mask(pose, mask));
                }
                PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, PoseRender.orbit(yaws[c], 6, 3.9, 1.05), r == 0 ? 0x5A6A7C : 0x1E2433);
                for (PoseRender.Tri t : tris) {
                    canvas.tri(t, true);
                }
                BufferedImage img = canvas.image();
                Graphics2D gi = img.createGraphics();
                gi.setColor(Color.WHITE);
                gi.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                gi.drawString("Banjou, " + when[r] + ": " + views[c], 5, 14);
                gi.drawString(mask == null ? "no mask" : "mask: " + mask, 5, 28);
                gi.dispose();
                g.drawImage(img, c * w, r * h, null);
            }
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(args[0]));
        System.out.println("wrote " + args[0]);
    }

    /** {@code mask}'s faces on the head of {@code pose}, in its own paint (as GangPreview draws a gang's). */
    static List<PoseRender.Tri> mask(RigPose pose, String mask) throws Exception {
        List<PoseRender.Tri> out = new ArrayList<>();
        BufferedImage paint = ImageIO.read(new File(GangPreview.MASKS + mask + ".png"));
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
                out.add(new PoseRender.Tri(new double[][] {w[0], w[1], w[2]}, argb & 0xFFFFFF, n));
                out.add(new PoseRender.Tri(new double[][] {w[0], w[2], w[3]}, argb & 0xFFFFFF, n));
            }
        }
        return out;
    }
}

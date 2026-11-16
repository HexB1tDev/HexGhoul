import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.HumanRules;
import com.hex.kagune.control.village.HumanSkins;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 2.0.34: each village woman (HumanSkins with {@code girl}) as HumanRenderer draws her now: the female figure as the CCG's women have it
 * (HumanRules.WOMAN_*: legs shaped and spread, a wide skin's arms slim, the bust), at her height and width; before it, the plain body she
 * had. A man's skin beside for scale. Run tools/quick_check.py first, then:
 * <pre>
 * javac -nowarn -cp build/quick/classes:build/quick/base.jar -d build/women-preview tools/PoseRender.java tools/VillagerPreview.java tools/GrownDaughterPreview.java tools/VillageWomenPreview.java
 * java -Djava.awt.headless=true -cp build/women-preview:build/quick/classes:build/quick/base.jar VillageWomenPreview &lt;out.png&gt;
 * </pre>
 */
public final class VillageWomenPreview {
    static final String ROOT = "src/main/resources/assets/hexkagune/";

    public static void main(String[] args) throws Exception {
        List<Integer> women = new ArrayList<>();
        int man = -1;
        for (int i = 0; i < HumanSkins.ALL.length; i++) {
            if (HumanSkins.ALL[i].girl()) {
                women.add(i);
            } else if (man < 0) {
                man = i;
            }
        }
        int w = 170;
        int h = 260;
        String[] heads = {"before", "now: front", "now: 3/4", "now: side", "a man, to scale"};
        BufferedImage sheet = new BufferedImage(heads.length * w, women.size() * (h + 20) + 24, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x20262C));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g.setColor(Color.WHITE);
        for (int c = 0; c < heads.length; c++) {
            g.drawString(heads[c], c * w + 6, 16);
        }
        BufferedImage manSkin = VillagerPreview.small(ImageIO.read(new File(ROOT + HumanSkins.ALL[man].path())));
        for (int r = 0; r < women.size(); r++) {
            HumanSkins.Skin skin = HumanSkins.ALL[women.get(r)];
            BufferedImage image = VillagerPreview.small(ImageIO.read(new File(ROOT + skin.path())));
            for (int c = 0; c < heads.length; c++) {
                boolean other = c == 4;
                PoseRender.skin = other ? manSkin : image;
                PoseRender.slim = other ? HumanSkins.ALL[man].slim() : skin.slim();
                boolean figure = c >= 1 && c <= 3;
                double yaw = c == 2 ? 35.0 : c == 3 ? 90.0 : 0.0;
                List<PoseRender.Tri> tris = figure ? GrownDaughterPreview.grown(new RigPose(), image, skin.slim(), HumanRules.WOMAN_LEGS, HumanRules.WOMAN_CURVES)
                    : PoseRender.model(new RigPose());
                double size = figure ? HumanRules.WOMAN_SIZE : 1.0;
                double wide = figure ? HumanRules.WOMAN_SIZE * HumanRules.WOMAN_BUILD : 1.0;
                PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, PoseRender.orbit(yaw, 8.0, 3.6, 0.95), 0x3A4450);
                canvas.floor(6, 0x58626C, 0x4E5862);
                for (PoseRender.Tri t : tris) {
                    double[][] v = new double[3][];
                    for (int i = 0; i < 3; i++) {
                        double[] p = PoseRender.scale(t.v()[i], size);
                        v[i] = new double[] {p[0] * wide / size, p[1], p[2] * wide / size};
                    }
                    canvas.tri(new PoseRender.Tri(v, t.rgb(), t.n()), true);
                }
                g.drawImage(canvas.image(), c * w, 24 + r * (h + 20), null);
            }
            g.setColor(Color.WHITE);
            g.drawString(skin.path().replaceAll(".*/", "") + (skin.slim() ? " (slim)" : " (wide: arms drawn slim)"), 6, 24 + r * (h + 20) + h + 14);
        }
        ImageIO.write(sheet, "png", new File(args[0]));
    }
}

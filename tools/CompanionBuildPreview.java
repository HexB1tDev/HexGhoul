import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.ReaperBuild;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 4.1.48: the companions' builds off-game (EtoRenderer.Build: ReaperBuild.chest of the persona's muscle, its arms set out by
 * ReaperBuild.armOut), on the slim arms in their own game textures: Ayato and Noro at the shares given, from in front, three quarters and
 * the side, with a male player's for comparison.
 *
 * <pre>
 * javac -nowarn -cp build/quick/classes:build/quick/base.jar -sourcepath tools -d build/build-preview tools/CompanionBuildPreview.java
 * java -Djava.awt.headless=true -cp build/build-preview:build/quick/classes:build/quick/base.jar CompanionBuildPreview &lt;out.png&gt; key:share:slim ...
 * </pre>
 */
public final class CompanionBuildPreview {
    public static void main(String[] args) throws Exception {
        PoseRender.stiff = true;
        List<BufferedImage> frames = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String[] a = args[i].split(":");
            String key = a[0];
            double share = Double.parseDouble(a[1]);
            boolean slim = a.length < 3 || Boolean.parseBoolean(a[2]);
            for (double yaw : new double[] {0, 40, 90}) {
                String path = key.startsWith("ghoul/") ? "src/main/resources/assets/hexkagune/textures/entity/" + key + ".png"
                    : "src/main/resources/assets/hexkagune/textures/entity/" + key + "/" + key + ".png";
                PoseRender.skin = ImageIO.read(new File(path));
                PoseRender.slim = slim;
                PoseRender.armOut = ReaperBuild.armOut(share);
                RigPose pose = new RigPose();
                List<PoseRender.Tri> tris = new ArrayList<>(PoseRender.model(pose));
                if (share > 0.0) {
                    ReaperPreview.chest(tris, pose, false, ReaperBuild.of(share));
                    ReaperPreview.chest(tris, pose, true, ReaperBuild.of(share));
                }
                PoseRender.Canvas canvas = new PoseRender.Canvas(240, 300, 2, PoseRender.orbit(yaw, 6, 3.7, 1.05), 0x3A4450);
                for (PoseRender.Tri t : tris) {
                    canvas.tri(t, true);
                }
                BufferedImage img = canvas.image();
                Graphics2D g = img.createGraphics();
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                g.drawString(key + " build " + share + (slim ? " (slim)" : " (wide)"), 5, 14);
                g.dispose();
                frames.add(img);
            }
        }
        BufferedImage sheet = new BufferedImage(3 * 240, (frames.size() / 3) * 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int i = 0; i < frames.size(); i++) {
            g.drawImage(frames.get(i), i % 3 * 240, i / 3 * 300, null);
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(args[0]));
        System.out.println("wrote " + args[0]);
    }
}

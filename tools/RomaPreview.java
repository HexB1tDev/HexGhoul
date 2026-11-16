import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.Persona;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 4.1.49: Roma off-game (the owner's skin, her game texture): her figure (Persona.ROMA's curves and legs: skinny) on the slim arms, from
 * in front, three quarters, the side and behind; Touka beside her in the same views for her size and build.
 * <pre>
 * javac -nowarn -cp build/quick/classes:build/quick/base.jar -d build/roma-preview tools/PoseRender.java tools/VillagerPreview.java tools/GrownDaughterPreview.java tools/RomaPreview.java
 * java -Djava.awt.headless=true -cp build/roma-preview:build/quick/classes:build/quick/base.jar RomaPreview &lt;out.png&gt;
 * </pre>
 */
public final class RomaPreview {
    public static void main(String[] args) throws Exception {
        PoseRender.stiff = true;
        double[] yaws = {0, 35, 90, 180};
        String[] views = {"front", "three quarters", "side", "behind"};
        Persona[] who = {Persona.ROMA, Persona.TOUKA};
        int w = 230;
        int h = 300;
        BufferedImage sheet = new BufferedImage(yaws.length * w, who.length * h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int r = 0; r < who.length; r++) {
            Persona p = who[r];
            BufferedImage skin = ImageIO.read(new File("src/main/resources/assets/hexkagune/textures/entity/" + p.key() + "/" + p.key() + ".png"));
            for (int c = 0; c < yaws.length; c++) {
                PoseRender.skin = skin;
                PoseRender.slim = p.slim();
                PoseRender.armOut = 0.0;
                List<PoseRender.Tri> tris = GrownDaughterPreview.grown(new RigPose(), skin, p.slim(), p.legs(), p.curves());
                PoseRender.Canvas canvas = new PoseRender.Canvas(w, h, 2, PoseRender.orbit(yaws[c], 6, 3.7, 1.05), 0x3A4450);
                for (PoseRender.Tri t : tris) {
                    canvas.tri(t, true);
                }
                BufferedImage img = canvas.image();
                Graphics2D gi = img.createGraphics();
                gi.setColor(Color.WHITE);
                gi.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                gi.drawString(p.name() + ": " + views[c] + " (curves " + p.curves() + ", legs " + p.legs() + ")", 5, 14);
                gi.dispose();
                g.drawImage(img, c * w, r * h, null);
            }
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(args[0]));
        System.out.println("wrote " + args[0]);
    }
}

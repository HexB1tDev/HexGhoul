import com.hex.kagune.control.client.animation.EtoPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.EtoGesture;
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
 * 1.3.14: the companions posed off-game ({@link PoseRender}, the game's own transforms; a mob's stiff limbs, no bust drawn): Touka's
 * strict, pouty idles, Ken's quiet ones, and Ken's way of holding someone (the kiss, making out, the forehead kiss, the hug, an arm round
 * them). Run {@code python3 build.py} first, then:
 * <pre>
 * javac -nowarn -cp build/classes:build/base.jar -d build/companion-preview tools/PoseRender.java tools/CompanionPreview.java
 * java -Djava.awt.headless=true -cp build/companion-preview:build/classes:build/base.jar CompanionPreview &lt;out-dir&gt;
 * </pre>
 */
public final class CompanionPreview {
    record Shot(String label, RigPose pose, double yaw) {
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/preview");
        out.mkdirs();
        if (args.length > 1 && args[1].equals("frames")) {
            frames(out);
            return;
        }
        String textures = "src/main/resources/assets/hexkagune/textures/entity/";
        List<Shot> touka = new ArrayList<>();
        for (Object[] g : new Object[][] {{EtoGesture.CROSS, 40.0}, {EtoGesture.TAP, 20.0}, {EtoGesture.HUFF, 14.0}, {EtoGesture.SIP, 50.0},
            {EtoGesture.WIPE, 30.0}, {EtoGesture.KICKS, 10.0}, {EtoGesture.SCOLD, 20.0}, {EtoGesture.SHY, 40.0}, {EtoGesture.PECK, 15.0}}) {
            EtoGesture gesture = (EtoGesture)g[0];
            touka.add(new Shot(gesture.name().toLowerCase(), EtoPose.gesture(gesture, (Double)g[1], false), 25.0));
        }
        sheet(new File(out, "touka-poses.png"), "Touka", textures + "touka/touka.png", true, touka);
        List<Shot> ken = new ArrayList<>();
        for (Object[] g : new Object[][] {{EtoGesture.CHIN, 40.0}, {EtoGesture.CRACK, 24.0}, {EtoGesture.BOW, 18.0}, {EtoGesture.READ, 40.0},
            {EtoGesture.SIP, 50.0}}) {
            EtoGesture gesture = (EtoGesture)g[0];
            ken.add(new Shot(gesture.name().toLowerCase(), EtoPose.gesture(gesture, (Double)g[1], true), 12.0));
        }
        for (Object[] g : new Object[][] {{EtoGesture.KISS, 30.0, "kiss (waiting)"}, {EtoGesture.PULL, 30.0, "making out"},
            {EtoGesture.PECK, 15.0, "forehead kiss"}, {EtoGesture.HUG, 25.0, "hug"}, {EtoGesture.CLING, 40.0, "arm round them"},
            {EtoGesture.KISSED, 15.0, "after a kiss"}}) {
            ken.add(new Shot((String)g[2], EtoPose.gesture((EtoGesture)g[0], (Double)g[1], true), 12.0));
            ken.add(new Shot((String)g[2] + ", side", EtoPose.gesture((EtoGesture)g[0], (Double)g[1], true), 80.0));
        }
        sheet(new File(out, "ken-poses.png"), "Ken", textures + "ken/ken.png", true, ken);
    }

    /**
     * Animation frames (every other tick, ten a second): each gesture looping as it plays, one sheet per frame per companion; a GIF is made
     * of them by {@code tools/companion-gifs.py}.
     */
    static void frames(File out) throws Exception {
        String textures = "src/main/resources/assets/hexkagune/textures/entity/";
        Object[][] touka = {{EtoGesture.CROSS, "arms folded"}, {EtoGesture.TAP, "foot tapping"}, {EtoGesture.HUFF, "huff"},
            {EtoGesture.SIP, "sipping coffee"}, {EtoGesture.WIPE, "drying a cup"}, {EtoGesture.KICKS, "practice kick"}, {EtoGesture.SCOLD, "telling you off"},
            {EtoGesture.SHY, "shy, turned away"}, {EtoGesture.PECK, "a quick kiss"}, {EtoGesture.KISS, "waiting for a kiss"}};
        Object[][] ken = {{EtoGesture.CHIN, "hand to chin"}, {EtoGesture.CRACK, "cracking a finger"}, {EtoGesture.BOW, "a bow"},
            {EtoGesture.READ, "reading"}, {EtoGesture.SIP, "coffee"}, {EtoGesture.KISS, "kiss (waiting)"}, {EtoGesture.PULL, "making out"},
            {EtoGesture.PECK, "forehead kiss"}, {EtoGesture.HUG, "hug"}, {EtoGesture.KISSED, "after a kiss"}};
        frames(new File(out, "touka-frames"), textures + "touka/touka.png", false, touka);
        frames(new File(out, "ken-frames"), textures + "ken/ken.png", true, ken);
    }

    static void frames(File folder, String skin, boolean leads, Object[][] gestures) throws Exception {
        folder.mkdirs();
        PoseRender.skin = ImageIO.read(new File(skin));
        PoseRender.slim = true;
        PoseRender.stiff = true;
        int w = 200;
        int h = 260;
        int columns = 5;
        int rows = (gestures.length + columns - 1) / columns;
        for (int frame = 0; frame < 60; frame++) {
            double tick = frame * 2.0;
            BufferedImage sheet = new BufferedImage(columns * w, rows * (h + 20), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sheet.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(34, 34, 40));
            g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            for (int i = 0; i < gestures.length; i++) {
                EtoGesture gesture = (EtoGesture)gestures[i][0];
                double length = gesture.held() ? 120.0 : gesture.ticks + 10.0;
                RigPose pose = EtoPose.gesture(gesture, tick % length, leads);
                BufferedImage image = PoseRender.render(pose == null ? new RigPose() : pose, PoseRender.orbit(22.0, 8.0, 4.4, 0.95), w, h, null);
                int x = i % columns * w;
                int y = i / columns * (h + 20);
                g.drawImage(image, x, y, null);
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                g.drawString((String)gestures[i][1], x + 8, y + h + 15);
            }
            g.dispose();
            ImageIO.write(sheet, "png", new File(folder, String.format("frame_%03d.png", frame)));
        }
    }

    static void sheet(File file, String title, String skin, boolean slim, List<Shot> shots) throws Exception {
        PoseRender.skin = ImageIO.read(new File(skin));
        PoseRender.slim = slim;
        PoseRender.stiff = true;
        int w = 260;
        int h = 330;
        int columns = 5;
        int rows = (shots.size() + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(columns * w, rows * (h + 22) + 36, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(34, 34, 40));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString(title, 10, 24);
        for (int i = 0; i < shots.size(); i++) {
            Shot shot = shots.get(i);
            BufferedImage image = PoseRender.render(shot.pose() == null ? new RigPose() : shot.pose(), PoseRender.orbit(shot.yaw(), 8.0, 4.2, 0.95), w, h, null);
            int x = i % columns * w;
            int y = 36 + i / columns * (h + 22);
            g.drawImage(image, x, y, null);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            g.drawString(shot.label(), x + 10, y + h + 16);
        }
        g.dispose();
        ImageIO.write(sheet, "png", file);
    }
}

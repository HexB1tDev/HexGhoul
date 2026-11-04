import com.hex.kagune.control.client.animation.AgentPose;
import com.hex.kagune.control.client.animation.RigPose;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 2.0.43: the White Reaper standing about, off-game, in his own skin through {@link PoseRender}'s exact transforms: an ordinary
 * investigator's idle ({@code AgentPose.idle}) beside his ({@code AgentPose.reaperIdle}), each with the case's arm hanging plumb
 * ({@code AgentPose.carry}), from several sides. Usage: {@code java -cp <classes> ReaperPreview <skin.png> <out.png> [bulk]}.
 */
public final class ReaperPreview {
    public static void main(String[] args) throws Exception {
        PoseRender.skin = ImageIO.read(new File(args[0]));
        PoseRender.slim = true;
        PoseRender.stiff = true; // an investigator is a mob on vanilla's player model: no elbows or knees bend
        File out = new File(args[1]);
        record Shot(String label, boolean reaper, double yaw, double pitch) {
        }
        List<Shot> shots = new ArrayList<>(List.of(
            new Shot("investigators' idle, front", false, 0, 6),
            new Shot("the Reaper's idle, front", true, 0, 6),
            new Shot("the Reaper's idle, three-quarter", true, 35, 8),
            new Shot("the Reaper's idle, other three-quarter", true, -40, 8),
            new Shot("the Reaper's idle, side", true, 90, 4),
            new Shot("the Reaper's idle, from below (your eyes, close)", true, 15, -8),
            new Shot("investigators' idle, side", false, 90, 4),
            new Shot("the Reaper's idle, behind", true, 165, 10)));
        int w = 420;
        int h = 560;
        int cols = 4;
        int rows = (shots.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(w * cols, h * rows, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int i = 0; i < shots.size(); i++) {
            Shot s = shots.get(i);
            RigPose pose = new RigPose();
            if (s.reaper) {
                AgentPose.reaperIdle(pose, 0.0, 7, 1.0F);
            } else {
                AgentPose.idle(pose, 0.0, 7, 1.0F);
            }
            AgentPose.carry(pose, 0.0F);
            double distance = s.label.contains("close") ? 3.0 : 4.0;
            BufferedImage img = PoseRender.render(pose, PoseRender.orbit(s.yaw, s.pitch, distance, 1.0), w, h, null);
            g.drawImage(img, (i % cols) * w, (i / cols) * h, w, h, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.drawString(s.label, (i % cols) * w + 10, (i / cols) * h + 22);
        }
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", out);
        System.out.println("wrote " + out);
    }
}

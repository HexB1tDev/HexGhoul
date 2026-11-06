import com.hex.kagune.control.client.animation.AgentPose;
import com.hex.kagune.control.client.animation.QuinqueGrip;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.client.animation.WeaponPose;
import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.ArmMesh;
import com.hex.kagune.control.quinque.WhipPath;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 3.1.0: offline carry checks with the game's pose composition, real weapon meshes, vanilla walking input and stiff NPC limbs.
 * <pre>
 * javac -cp build/quick/classes -d build/carry-preview tools/PoseRender.java tools/WeaponPosePreview.java tools/WeaponCarryPreview.java
 * java -Djava.awt.headless=true -cp build/carry-preview:build/quick/classes WeaponCarryPreview &lt;out.png&gt; [yaw]
 * </pre>
 */
public final class WeaponCarryPreview {
    static RigPose pose(Arm arm, double speed, double phase) {
        float run = AgentPose.running(speed);
        RigPose p = AgentPose.run(phase, run, arm.twoHanded ? AgentPose.Carry.STANCE : AgentPose.Carry.BLADE);
        if (arm.twoHanded && run > 0.0F) {
            WeaponPose.carryRun(p, arm, phase, run);
        } else {
            AgentPose.withWeapon(p, WeaponPose.carry(arm, speed, phase));
        }
        if (arm.twoHanded) {
            QuinqueGrip.reach(p, WeaponPose.base(arm), false);
        }
        // PoseRender begins with standing input. Bake in the walk AgentModel receives from vanilla before its pose is applied.
        float stride = (float)(Math.cos(phase) * 0.77 * Math.min(1.0, speed / 0.1));
        float legs = 1.0F - p.legHold;
        p.rightLegX += stride * legs;
        p.leftLegX -= stride * legs;
        p.legHold = 1.0F;
        float left = 1.0F - Math.max(p.armHold, p.leftArmHold);
        p.leftArmX += stride * 0.5F * left;
        p.leftArmZ -= PoseRender.BOB * left;
        p.leftArmHold = 1.0F;
        return p;
    }

    public static void main(String[] args) throws Exception {
        PoseRender.stiff = true; // AgentModel is vanilla's PlayerModel, with no bendable elbows/knees.
        double yaw = args.length > 1 ? Double.parseDouble(args[1]) : 35.0;
        int w = 300, h = 350, top = 62;
        String[] labels = {"Stopped", "Walk: left", "Walk: passing", "Walk: right", "Walk: passing", "Sprint"};
        double[] phases = {0, 0, Math.PI * 0.5, Math.PI, Math.PI * 1.5, Math.PI * 0.5};
        double[] speeds = {0, 0.1, 0.1, 0.1, 0.1, 0.4};
        BufferedImage sheet = new BufferedImage(w * labels.length, top + h * Arm.values().length, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(26, 26, 32));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        g.drawString("3.1.0: investigator weapon carries - offline geometry preview, not an in-game test", 12, 24);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        g.drawString("Vanilla walk under the held weapon; the existing sprint; both hands on two-handed grips; whip curled above the floor.", 12, 47);
        for (Arm arm : Arm.values()) {
            for (int col = 0; col < labels.length; col++) {
                RigPose p = pose(arm, speeds[col], phases[col]);
                List<PoseRender.Tri> extra = new ArrayList<>();
                WeaponPosePreview.weapon(extra, p, arm, 1.0F);
                if (arm == Arm.WHIP) {
                    double[] k = WeaponPose.onArm(WeaponPose.meshToArm(arm), new double[] {0, 0, -ArmMesh.WHIP_ROOT});
                    double[] knot = PoseRender.point(p, "rightArm", new double[] {k[0] * 16, k[1] * 16, k[2] * 16});
                    WeaponPosePreview.tendril(extra, p, WhipPath.rest(WhipPath.length(false), Math.max(0.2, knot[1]), col * 6.0));
                }
                PoseRender.Camera centered = PoseRender.orbit(yaw, 10, 7.8, 0.95);
                double[] shift = {-0.35, 0.0, 0.65};
                PoseRender.Camera camera = new PoseRender.Camera(PoseRender.add(centered.eye(), shift), PoseRender.add(centered.target(), shift), 42);
                BufferedImage cell = PoseRender.render(p, camera, w, h, extra);
                int x = col * w, y = top + arm.ordinal() * h;
                g.drawImage(cell, x, y, null);
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                g.drawString(arm.title + " / " + labels[col], x + 9, y + 20);
            }
        }
        g.dispose();
        File out = new File(args.length > 0 ? args[0] : "build/carry-preview/weapon-carry-3.1.0.png");
        out.getAbsoluteFile().getParentFile().mkdirs();
        ImageIO.write(sheet, "png", out);
        System.out.println(out);
    }
}

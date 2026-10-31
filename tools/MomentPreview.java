import com.hex.kagune.control.client.animation.ChildMomentPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.family.ChildMoment;
import com.hex.kagune.control.family.ChildRules;
import com.hex.kagune.control.family.ChildStage;
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
 * 1.3.20: the parent-child moments posed off-game ({@link PoseRender}, the game's own transforms): the parent (a stand-in player) and the
 * child (one of the owner's skins) at a moment's telling frame, both on the poses the game gives them ({@link ChildMomentPose}). The child
 * is drawn as a scaled-down player (PoseRender has no little one's big head), at its stage's height. Run {@code python3 build.py} first:
 * <pre>
 * javac -nowarn -cp build/classes:build/base.jar -d build/moment-preview tools/PoseRender.java tools/MomentPreview.java
 * java -Djava.awt.headless=true -cp build/moment-preview:build/classes:build/base.jar MomentPreview &lt;out-dir&gt; &lt;kid skin png&gt;
 * </pre>
 */
public final class MomentPreview {
    record Shot(String label, ChildMoment moment, ChildStage stage, double age, int ride, double camYaw) {
    }

    /** The child's height against a player's (it is drawn as a scaled player). */
    static double height(ChildStage stage) {
        return stage.young ? 20.0 * stage.draw / 32.0 : stage.draw;
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/preview");
        out.mkdirs();
        BufferedImage kidSkin = small(ImageIO.read(new File(args[1])));
        List<Shot> shots = List.of(
            new Shot("hug (toddler)", ChildMoment.HUG, ChildStage.TODDLER, 30, -1, 65),
            new Shot("hug (teen)", ChildMoment.HUG, ChildStage.TEEN, 30, -1, 70),
            new Shot("head pat (child)", ChildMoment.PAT, ChildStage.CHILD, 24, -1, 65),
            new Shot("high five (child)", ChildMoment.HIGH_FIVE, ChildStage.CHILD, 12, -1, 70),
            new Shot("tickle (toddler)", ChildMoment.TICKLE, ChildStage.TODDLER, 30, -1, 55),
            new Shot("peekaboo: hidden", ChildMoment.PEEKABOO, ChildStage.BABY, 10, -1, 60),
            new Shot("peekaboo: boo!", ChildMoment.PEEKABOO, ChildStage.BABY, 28, -1, 60),
            new Shot("toss: thrown up", ChildMoment.TOSS, ChildStage.TODDLER, 11, ChildRules.RIDE_ARMS, 70),
            new Shot("toss: caught", ChildMoment.TOSS, ChildStage.TODDLER, 21, ChildRules.RIDE_ARMS, 70),
            new Shot("airplane spin", ChildMoment.SPIN, ChildStage.TODDLER, 32, ChildRules.RIDE_SPIN, 60),
            new Shot("on the shoulders", ChildMoment.NONE, ChildStage.CHILD, 0, ChildRules.RIDE_SHOULDERS, 25),
            new Shot("arms up: pick me up!", ChildMoment.ARMS_UP, ChildStage.TODDLER, 20, -1, 65),
            new Shot("wave (child)", ChildMoment.WAVE, ChildStage.CHILD, 14, -1, 60),
            new Shot("clap (baby)", ChildMoment.CLAP, ChildStage.BABY, 9, -1, 60),
            new Shot("a flower for you", ChildMoment.FLOWER, ChildStage.CHILD, 20, -1, 65));
        int w = 300;
        int h = 340;
        int columns = 5;
        int rows = (shots.size() + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(columns * w, rows * (h + 22) + 36, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(34, 34, 40));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString("Parent and child moments (the parent a stand-in player; the child a scaled player in a kid's skin)", 10, 24);
        for (int i = 0; i < shots.size(); i++) {
            Shot shot = shots.get(i);
            BufferedImage image = render(shot, kidSkin, w, h);
            int x = i % columns * w;
            int y = 36 + i / columns * (h + 22);
            g.drawImage(image, x, y, null);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            g.drawString(shot.label(), x + 10, y + h + 16);
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(out, "moments.png"));
    }

    static BufferedImage small(BufferedImage hd) {
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int s = hd.getWidth() / 64;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                out.setRGB(x, y, hd.getRGB(x * s + s / 2, y * s + s / 2));
            }
        }
        return out;
    }

    static BufferedImage render(Shot shot, BufferedImage kidSkin, int w, int h) {
        // The parent's pose: its side of the moment (a player's: knees bent), or the ride's.
        RigPose parent = new RigPose();
        if (shot.ride() == ChildRules.RIDE_SHOULDERS) {
            ChildMomentPose.blend(parent, ChildMomentPose.shouldersParent(), 1.0F);
        } else if (shot.ride() == ChildRules.RIDE_ARMS) {
            parent.rightArmX = -0.95F;
            parent.leftArmX = -0.95F;
            parent.rightArmY = -0.45F;
            parent.leftArmY = 0.45F;
            parent.armHold = 1.0F;
        }
        if (shot.moment() != ChildMoment.NONE && !shot.moment().solo) {
            ChildMomentPose.blend(parent, ChildMomentPose.parent(shot.moment(), shot.age(), shot.stage(), true), 1.0F);
        }
        // The child's: sitting when held in the arms, its moment's or its ride's over that.
        RigPose child = new RigPose();
        if (shot.ride() == ChildRules.RIDE_ARMS || shot.stage().sits()) {
            child.rightLegX = -1.41F;
            child.leftLegX = -1.41F;
            child.rightLegY = 0.31F;
            child.leftLegY = -0.31F;
            child.legHold = 1.0F;
        }
        RigPose moment = shot.moment() == ChildMoment.NONE ? ChildMomentPose.ride(shot.ride(), 40.0)
            : ChildMomentPose.child(shot.moment(), shot.age(), shot.stage());
        if (moment != null) {
            ChildMomentPose.blend(child, moment, 1.0F);
        }
        java.awt.image.BufferedImage parentSkin = PoseRender.skin;
        PoseRender.skin = kidSkin;
        PoseRender.slim = false;
        List<PoseRender.Tri> kid = PoseRender.model(child);
        PoseRender.skin = parentSkin;
        // Where the child is: facing the parent its stand away, or held (in the arms, on the shoulders, out spinning, thrown up).
        double k = height(shot.stage());
        double sat = shot.ride() >= 0 || shot.stage().sits() ? 0.75 * k : 0.0;
        double[] at;
        double turn;
        if (shot.ride() == ChildRules.RIDE_SHOULDERS) {
            at = new double[] {0.0, ChildRules.SHOULDERS_UP - sat, -ChildRules.SHOULDERS_BACK};
            turn = 0.0;
        } else if (shot.ride() == ChildRules.RIDE_SPIN) {
            double a = ChildMoment.spinAngle(shot.age());
            at = new double[] {-Math.sin(a) * ChildRules.SPIN_AHEAD, ChildRules.SPIN_UP - 0.45 * k, Math.cos(a) * ChildRules.SPIN_AHEAD};
            turn = Math.PI - a;
        } else if (shot.ride() == ChildRules.RIDE_ARMS) {
            at = new double[] {0.0, ChildRules.CARRY_UP + ChildMoment.tossLift(shot.age()) - sat, ChildRules.CARRY_AHEAD};
            turn = Math.PI;
        } else {
            double stand = shot.moment().stand > 0.0 ? shot.moment().stand : 1.1;
            at = new double[] {0.0, -sat, stand};
            turn = Math.PI;
        }
        List<PoseRender.Tri> placed = new ArrayList<>();
        double cos = Math.cos(turn);
        double sin = Math.sin(turn);
        for (PoseRender.Tri t : kid) {
            double[][] v = new double[t.v().length][];
            for (int i = 0; i < v.length; i++) {
                double[] q = t.v()[i];
                double x = q[0] * k;
                double y = q[1] * k;
                double z = q[2] * k;
                v[i] = new double[] {x * cos + z * sin + at[0], y + at[1], -x * sin + z * cos + at[2]};
            }
            double[] n = t.n();
            double[] nn = n == null ? null : new double[] {n[0] * cos + n[2] * sin, n[1], -n[0] * sin + n[2] * cos};
            placed.add(new PoseRender.Tri(v, t.rgb(), nn));
        }
        return PoseRender.render(parent, PoseRender.orbit(shot.camYaw(), 12.0, 4.6, 0.95), w, h, placed);
    }
}
